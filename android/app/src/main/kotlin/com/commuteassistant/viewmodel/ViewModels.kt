package com.commuteassistant.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.commuteassistant.data.repository.CommuteRepository
import com.commuteassistant.domain.model.*
import com.commuteassistant.domain.usecase.GetDepartureRecommendationUseCase
import com.commuteassistant.data.TomTomApiService
import com.commuteassistant.data.ApiKeyProvider
import com.commuteassistant.data.Prediction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import android.location.Geocoder
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.Locale
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.commuteassistant.util.TimeUtils
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class HomeUiState(
    val routines: List<CommuteRoutine> = emptyList(),
    val recommendations: Map<Long, DepartureRecommendation> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: CommuteRepository,
    private val recommendationUseCase: GetDepartureRecommendationUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeRoutines()
    }

    private fun observeRoutines() {
        viewModelScope.launch {
            repository.observeRoutines()
                .catch { e -> _uiState.update { it.copy(error = e.message, isLoading = false) } }
                .collect { routines ->
                    _uiState.update { it.copy(routines = routines, isLoading = false) }
                    refreshRecommendations(routines)
                }
        }
    }

    fun refreshRecommendations(routines: List<CommuteRoutine> = _uiState.value.routines) {
        viewModelScope.launch {
            val recs = routines.associate { routine ->
                routine.id to recommendationUseCase(routine)
            }
            _uiState.update { it.copy(recommendations = recs) }
        }
    }

    fun deleteRoutine(routine: CommuteRoutine) {
        viewModelScope.launch { repository.deleteRoutine(routine) }
    }
}

// ─── Add/Edit Routine ViewModel ───────────────────────────────────────────────

data class RoutineFormState(
    val originName: String = "",
    val originLat: Double = 0.0,
    val originLng: Double = 0.0,
    val originSuggestions: List<Prediction> = emptyList(),
    val destinationName: String = "",
    val destinationLat: Double = 0.0,
    val destinationLng: Double = 0.0,
    val destinationSuggestions: List<Prediction> = emptyList(),
    val selectedDay: DayOfWeek = DayOfWeek.MONDAY,
    val departureHour: Int = 8,
    val departureMinute: Int = 0,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val etaText: String? = null,
    val isEtaLoading: Boolean = false,
    val isNotificationEnabled: Boolean = true,
    val isPriorityAlert: Boolean = false,
    val notificationOffsetMins: Int = 15,
    val notificationCount: Int = 1,
    val isLoadingLocation: Boolean = false,   // ← new
    val locationError: String? = null          // ← new
)

@HiltViewModel
class RoutineFormViewModel @Inject constructor(
    private val repository: CommuteRepository,
    private val apiService: TomTomApiService,
    private val apiKeyProvider: ApiKeyProvider,
    private val notificationScheduler: com.commuteassistant.notifications.NotificationScheduler,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(RoutineFormState())
    val state: StateFlow<RoutineFormState> = _state.asStateFlow()

    fun onOriginSearchChange(query: String) {
        _state.update { it.copy(originName = query, originLat = 0.0, originLng = 0.0, etaText = null) }
        fetchSuggestions(query, isOrigin = true)
    }

    fun onDestinationSearchChange(query: String) {
        _state.update { it.copy(destinationName = query, destinationLat = 0.0, destinationLng = 0.0, etaText = null) }
        fetchSuggestions(query, isOrigin = false)
    }

    private fun fetchSuggestions(query: String, isOrigin: Boolean) {
        if (query.isBlank()) {
            _state.update {
                if (isOrigin) it.copy(originSuggestions = emptyList())
                else it.copy(destinationSuggestions = emptyList())
            }
            return
        }
        viewModelScope.launch {
            delay(300) // debounce
            try {
                val key = apiKeyProvider.getTomTomApiKey()
                if (key.isNotEmpty()) {
                    val response = apiService.fuzzySearch(query = query, apiKey = key)
                    val predictions = response.results?.map { result ->
                        Prediction(
                            description = result.address?.freeformAddress ?: "",
                            placeId = result.id,
                            lat = result.position?.lat,
                            lng = result.position?.lon
                        )
                    } ?: emptyList()
                    _state.update {
                        if (isOrigin) it.copy(originSuggestions = predictions)
                        else it.copy(destinationSuggestions = predictions)
                    }
                }
            } catch (e: Exception) {
                // Ignore or log
            }
        }
    }

    fun onOriginSelected(prediction: Prediction) {
        _state.update { 
            it.copy(
                originName = prediction.description, 
                originLat = prediction.lat ?: 0.0,
                originLng = prediction.lng ?: 0.0,
                originSuggestions = emptyList()
            ) 
        }
        calculateEta()
    }

    fun onDestinationSelected(prediction: Prediction) {
        _state.update { 
            it.copy(
                destinationName = prediction.description, 
                destinationLat = prediction.lat ?: 0.0,
                destinationLng = prediction.lng ?: 0.0,
                destinationSuggestions = emptyList()
            ) 
        }
        calculateEta()
    }

    // fetchPlaceDetails is no longer needed as TomTom fuzzy search returns positions directly.

    // ── Current Location ──────────────────────────────────────────────────────

    fun useCurrentLocationAsOrigin() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingLocation = true, locationError = null) }
            try {
                val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                val cts = CancellationTokenSource()

                val location = suspendCancellableCoroutine<android.location.Location?> { cont ->
                    fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                        .addOnSuccessListener { cont.resume(it) }
                        .addOnFailureListener { cont.resumeWithException(it) }
                    cont.invokeOnCancellation { cts.cancel() }
                }

                if (location == null) {
                    _state.update { it.copy(isLoadingLocation = false, locationError = "Location unavailable") }
                    return@launch
                }

                @Suppress("DEPRECATION")
                val addresses = Geocoder(context, Locale.getDefault())
                    .getFromLocation(location.latitude, location.longitude, 1)
                val addressLine = addresses?.firstOrNull()?.getAddressLine(0) ?: "Current Location"

                _state.update {
                    it.copy(
                        originName = addressLine,
                        originLat = location.latitude,
                        originLng = location.longitude,
                        originSuggestions = emptyList(),
                        isLoadingLocation = false,
                        locationError = null
                    )
                }
                calculateEta()

            } catch (e: SecurityException) {
                _state.update { it.copy(isLoadingLocation = false, locationError = "Location permission denied") }
            } catch (e: Exception) {
                _state.update { it.copy(isLoadingLocation = false, locationError = "Could not get location") }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    private fun calculateEta() {
        val s = _state.value
        if (s.originLat != 0.0 && s.originLng != 0.0 && s.destinationLat != 0.0 && s.destinationLng != 0.0) {
            viewModelScope.launch {
                _state.update { it.copy(isEtaLoading = true) }
                try {
                    val key = apiKeyProvider.getTomTomApiKey()
                    if (key.isNotEmpty()) {
                        val locations = "${s.originLat},${s.originLng}:${s.destinationLat},${s.destinationLng}"
                        
                        val nextOccurrence = TimeUtils.calculateNextOccurrence(
                            s.selectedDay, 
                            LocalTime.of(s.departureHour, s.departureMinute)
                        )
                        val arriveAtIso = nextOccurrence.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

                        val response = apiService.calculateRoute(
                            locations = locations,
                            apiKey = key,
                            arriveAt = arriveAtIso
                        )
                        val summary = response.routes?.firstOrNull()?.summary
                        val minutes = summary?.travelTimeInSeconds?.let { it / 60 }
                        val text = minutes?.let { "${it} min" } ?: "N/A"
                        _state.update { it.copy(etaText = text, isEtaLoading = false) }
                    } else {
                        _state.update { it.copy(isEtaLoading = false) }
                    }
                } catch (e: Exception) {
                    _state.update { it.copy(isEtaLoading = false) }
                }
            }
        }
    }

    fun updateDay(day: DayOfWeek) = _state.update { it.copy(selectedDay = day) }

    fun updateTime(hour: Int, minute: Int) =
        _state.update { it.copy(departureHour = hour, departureMinute = minute) }

    fun updateNotificationEnabled(enabled: Boolean) = _state.update { it.copy(isNotificationEnabled = enabled) }
    fun updatePriorityAlert(priority: Boolean) = _state.update { it.copy(isPriorityAlert = priority) }
    fun updateNotificationOffset(mins: Int) = _state.update { it.copy(notificationOffsetMins = mins) }
    fun updateNotificationCount(count: Int) = _state.update { it.copy(notificationCount = count) }

    fun save() {
        val s = _state.value
        if (s.originName.isBlank() || s.destinationName.isBlank() ||
            s.originLat == 0.0 || s.destinationLat == 0.0) {
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val routine = CommuteRoutine(
                dayOfWeek = s.selectedDay,
                usualDepartureTime = LocalTime.of(s.departureHour, s.departureMinute),
                originLat = s.originLat, originLng = s.originLng, originName = s.originName,
                destinationLat = s.destinationLat, destinationLng = s.destinationLng,
                destinationName = s.destinationName,
                isNotificationEnabled = s.isNotificationEnabled,
                isPriorityAlert = s.isPriorityAlert,
                notificationOffsetMins = s.notificationOffsetMins,
                notificationCount = s.notificationCount
            )
            val id = repository.saveRoutine(routine)
            val savedRoutine = routine.copy(id = id)
            notificationScheduler.scheduleAlertsFor(savedRoutine)

            if (savedRoutine.isNotificationEnabled) {
                com.commuteassistant.notifications.TrafficCheckWorker.scheduleForRoutine(
                    context, id, savedRoutine.notificationOffsetMins
                )
            } else {
                androidx.work.WorkManager.getInstance(context).cancelUniqueWork("traffic_check_$id")
            }

            _state.update { it.copy(isSaving = false, saved = true) }
        }
    }
}
