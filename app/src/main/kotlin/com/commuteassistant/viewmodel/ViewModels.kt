package com.commuteassistant.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.commuteassistant.data.repository.CommuteRepository
import com.commuteassistant.domain.model.*
import com.commuteassistant.domain.usecase.GetDepartureRecommendationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val destinationName: String = "",
    val destinationLat: Double = 0.0,
    val destinationLng: Double = 0.0,
    val selectedDay: DayOfWeek = DayOfWeek.MONDAY,
    val departureHour: Int = 8,
    val departureMinute: Int = 0,
    val isSaving: Boolean = false,
    val saved: Boolean = false
)

@HiltViewModel
class RoutineFormViewModel @Inject constructor(
    private val repository: CommuteRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RoutineFormState())
    val state: StateFlow<RoutineFormState> = _state.asStateFlow()

    fun updateOrigin(name: String, lat: Double, lng: Double) =
        _state.update { it.copy(originName = name, originLat = lat, originLng = lng) }

    fun updateDestination(name: String, lat: Double, lng: Double) =
        _state.update { it.copy(destinationName = name, destinationLat = lat, destinationLng = lng) }

    fun updateDay(day: DayOfWeek) = _state.update { it.copy(selectedDay = day) }

    fun updateTime(hour: Int, minute: Int) =
        _state.update { it.copy(departureHour = hour, departureMinute = minute) }

    fun save() {
        val s = _state.value
        if (s.originName.isBlank() || s.destinationName.isBlank()) return
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
