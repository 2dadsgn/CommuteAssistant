package com.commuteassistant.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.commuteassistant.viewmodel.RoutineFormViewModel
import java.time.DayOfWeek

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRoutineScreen(
    onBack: () -> Unit,
    viewModel: RoutineFormViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    // Navigate back when saved
    LaunchedEffect(state.saved) {
        if (state.saved) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Commute Route") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Origin ────────────────────────────────────────────────────────
            SectionHeader("Starting Point")
            OutlinedTextField(
                value = state.originName,
                onValueChange = { viewModel.updateOrigin(it, state.originLat, state.originLng) },
                label = { Text("Home / Origin address") },
                leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            // Placeholder for map picker — wire up Maps SDK here
            CoordinateRow(
                lat = state.originLat, lng = state.originLng,
                onLatChange = { viewModel.updateOrigin(state.originName, it, state.originLng) },
                onLngChange = { viewModel.updateOrigin(state.originName, state.originLat, it) }
            )

            // ── Destination ───────────────────────────────────────────────────
            SectionHeader("Destination")
            OutlinedTextField(
                value = state.destinationName,
                onValueChange = { viewModel.updateDestination(it, state.destinationLat, state.destinationLng) },
                label = { Text("Work / Destination address") },
                leadingIcon = { Icon(Icons.Default.Work, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            CoordinateRow(
                lat = state.destinationLat, lng = state.destinationLng,
                onLatChange = { viewModel.updateDestination(state.destinationName, it, state.destinationLng) },
                onLngChange = { viewModel.updateDestination(state.destinationName, state.destinationLat, it) }
            )

            // ── Day picker ────────────────────────────────────────────────────
            SectionHeader("Day of Week") //todo possibility to pick more than one day
            DayPicker(selected = state.selectedDay, onSelect = { viewModel.updateDay(it) })

            // ── Time picker ───────────────────────────────────────────────────
            SectionHeader("Usual Departure Time") //TODO: change this to desired arrival time
            TimePicker(
                hour = state.departureHour,
                minute = state.departureMinute,
                onTimeChange = viewModel::updateTime
            )

            Spacer(Modifier.height(8.dp))

            // ── Save ──────────────────────────────────────────────────────────
            Button(
                onClick = { viewModel.save() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !state.isSaving && state.originName.isNotBlank() && state.destinationName.isNotBlank()
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Default.Save, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Save Route")
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun CoordinateRow(
    lat: Double, lng: Double,
    onLatChange: (Double) -> Unit,
    onLngChange: (Double) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = if (lat == 0.0) "" else lat.toString(),
            onValueChange = { onLatChange(it.toDoubleOrNull() ?: 0.0) },
            label = { Text("Lat") },
            modifier = Modifier.weight(1f),
            singleLine = true
        )
        OutlinedTextField(
            value = if (lng == 0.0) "" else lng.toString(),
            onValueChange = { onLngChange(it.toDoubleOrNull() ?: 0.0) },
            label = { Text("Lng") },
            modifier = Modifier.weight(1f),
            singleLine = true
        )
    }
    Text(
        "💡 Tip: long-press on Google Maps to copy coordinates",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    )
}

@Composable
private fun DayPicker(selected: DayOfWeek, onSelect: (DayOfWeek) -> Unit) {
    val days = DayOfWeek.values()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        days.forEach { day ->
            FilterChip(
                selected = day == selected,
                onClick = { onSelect(day) },
                label = { Text(day.name.take(2)) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TimePicker(hour: Int, minute: Int, onTimeChange: (Int, Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Hour
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Hour", style = MaterialTheme.typography.labelSmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onTimeChange((hour - 1 + 24) % 24, minute) }) {
                    Icon(Icons.Default.Remove, null)
                }
                Text(hour.toString().padStart(2, '0'),
                    style = MaterialTheme.typography.headlineMedium)
                IconButton(onClick = { onTimeChange((hour + 1) % 24, minute) }) {
                    Icon(Icons.Default.Add, null)
                }
            }
        }
        Text(":", style = MaterialTheme.typography.headlineMedium)
        // Minute
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Minute", style = MaterialTheme.typography.labelSmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onTimeChange(hour, (minute - 5 + 60) % 60) }) {
                    Icon(Icons.Default.Remove, null)
                }
                Text(minute.toString().padStart(2, '0'),
                    style = MaterialTheme.typography.headlineMedium)
                IconButton(onClick = { onTimeChange(hour, (minute + 5) % 60) }) {
                    Icon(Icons.Default.Add, null)
                }
            }
        }
    }
}
