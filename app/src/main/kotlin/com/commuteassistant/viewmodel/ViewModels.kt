package com.commuteassistant.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.commuteassistant.data.repository.CommuteRepository
import com.commuteassistant.domain.model.*
import com.commuteassistant.domain.usecase.GetDepartureRecommendationUseCase
import com.commuteassistant.data.GoogleMapsApiService
import com.commuteassistant.data.ApiKeyProvider
import com.commuteassistant.data.Prediction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime
import javax.inject.Inject

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
    val isEtaLoading: Boolean = false
)

@HiltViewModel
class RoutineFormViewModel @Inject constructor(
    private val repository: CommuteRepository,
    private val apiService: GoogleMapsApiService,
    private val apiKeyProvider: ApiKeyProvider
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
                val key = apiKeyProvider.getMapsApiKey()
                if (key.isNotEmpty()) {
                    val response = apiService.getPlaceAutocomplete(input = query, apiKey = key)
                    val predictions = response.predictions ?: emptyList()
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
        _state.update { it.copy(originName = prediction.description, originSuggestions = emptyList()) }
        fetchPlaceDetails(prediction.placeId, isOrigin = true)
    }

    fun onDestinationSelected(prediction: Prediction) {
        _state.update { it.copy(destinationName = prediction.description, destinationSuggestions = emptyList()) }
        fetchPlaceDetails(prediction.placeId, isOrigin = false)
    }

    private fun fetchPlaceDetails(placeId: String, isOrigin: Boolean) {
        viewModelScope.launch {
            try {
                val key = apiKeyProvider.getMapsApiKey()
                if (key.isNotEmpty()) {
                    val response = apiService.getPlaceDetails(placeId, key)
                    val location = response.result?.geometry?.location
                    if (location != null) {
                        _state.update {
                            if (isOrigin) it.copy(originLat = location.lat, originLng = location.lng)
                            else it.copy(destinationLat = location.lat, destinationLng = location.lng)
                        }
                        calculateEta()
                    }
                }
            } catch (e: Exception) {
                // Ignore or log
            }
        }
    }

    private fun calculateEta() {
        val s = _state.value
        if (s.originLat != 0.0 && s.originLng != 0.0 && s.destinationLat != 0.0 && s.destinationLng != 0.0) {
            viewModelScope.launch {
                _state.update { it.copy(isEtaLoading = true) }
                try {
                    val key = apiKeyProvider.getMapsApiKey()
                    if (key.isNotEmpty()) {
                        val response = apiService.getDirections(
                            origin = "${s.originLat},${s.originLng}",
                            destination = "${s.destinationLat},${s.destinationLng}",
                            apiKey = key
                        )
                        val text = response.routes?.firstOrNull()?.legs?.firstOrNull()?.durationInTraffic?.text
                            ?: response.routes?.firstOrNull()?.legs?.firstOrNull()?.duration?.text
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

    fun save() {
        val s = _state.value
        if (s.originName.isBlank() || s.destinationName.isBlank() ||
            s.originLat == 0.0 || s.destinationLat == 0.0) {
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            repository.saveRoutine(
                CommuteRoutine(
                    dayOfWeek = s.selectedDay,
                    usualDepartureTime = LocalTime.of(s.departureHour, s.departureMinute),
                    originLat = s.originLat, originLng = s.originLng, originName = s.originName,
                    destinationLat = s.destinationLat, destinationLng = s.destinationLng,
                    destinationName = s.destinationName
                )
            )
            _state.update { it.copy(isSaving = false, saved = true) }
        }
    }
}
