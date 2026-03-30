package com.commuteassistant.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.commuteassistant.viewmodel.RoutineFormViewModel
import com.commuteassistant.notifications.TrafficCheckWorker
import com.commuteassistant.data.Prediction
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
        if (state.saved) {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Commute Route") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            AutocompleteTextField(
                query = state.originName,
                onQueryChange = viewModel::onOriginSearchChange,
                suggestions = state.originSuggestions,
                onSuggestionSelected = viewModel::onOriginSelected,
                label = "Home / Origin address",
                leadingIcon = Icons.Default.Home
            )

            // ── Destination ───────────────────────────────────────────────────
            SectionHeader("Destination")
            AutocompleteTextField(
                query = state.destinationName,
                onQueryChange = viewModel::onDestinationSearchChange,
                suggestions = state.destinationSuggestions,
                onSuggestionSelected = viewModel::onDestinationSelected,
                label = "Work / Destination address",
                leadingIcon = Icons.Default.Work
            )

            // ── Day picker ────────────────────────────────────────────────────
            SectionHeader("When")
            DayPicker(selected = state.selectedDay, onSelect = viewModel::updateDay)

            Spacer(Modifier.height(16.dp))

            // ── Time picker ───────────────────────────────────────────────────
            SectionHeader("Desired time of arrival")
            TimePicker(
                hour = state.departureHour, minute = state.departureMinute,
                onTimeChange = viewModel::updateTime
            )

            Spacer(Modifier.height(16.dp))

            // ── Notification Settings ─────────────────────────────────────────
            SectionHeader("Notification Settings")
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Enable Notifications", style = MaterialTheme.typography.titleMedium)
                    Text("Get active traffic updates", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = state.isNotificationEnabled,
                    onCheckedChange = viewModel::updateNotificationEnabled
                )
            }

            if (state.isNotificationEnabled) {
                Spacer(Modifier.height(8.dp))
                Text("Send traffic updates every ${state.notificationOffsetMins} minutes", style = MaterialTheme.typography.bodyMedium)
                androidx.compose.material3.Slider(
                    value = state.notificationOffsetMins.toFloat(),
                    onValueChange = { viewModel.updateNotificationOffset(it.toInt()) },
                    valueRange = 15f..60f,
                    steps = 2
                )
            }

            Spacer(Modifier.height(8.dp))

            // ── ETA Display ───────────────────────────────────────────────────
            if (state.isEtaLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Calculating ETA...", style = MaterialTheme.typography.bodyMedium)
                }
            } else if (state.etaText != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DirectionsCar, contentDescription = "ETA")
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Approximate ETA: ${state.etaText}", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            // ── Save ──────────────────────────────────────────────────────────
            Button(
                onClick = { viewModel.save() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !state.isSaving && 
                          state.originName.isNotBlank() && state.destinationName.isNotBlank() &&
                          state.originLat != 0.0 && state.destinationLat != 0.0
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutocompleteTextField(
    query: String,
    onQueryChange: (String) -> Unit,
    suggestions: List<Prediction>,
    onSuggestionSelected: (Prediction) -> Unit,
    label: String,
    leadingIcon: ImageVector
) {
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(suggestions) {
        expanded = suggestions.isNotEmpty()
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                onQueryChange(it)
                expanded = true
            },
            label = { Text(label) },
            leadingIcon = { Icon(leadingIcon, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        androidx.compose.animation.AnimatedVisibility(visible = expanded && suggestions.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
            ) {
                Column {
                    suggestions.forEach { prediction ->
                        ListItem(
                            headlineContent = { Text(prediction.description, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                            modifier = Modifier.clickable {
                                onSuggestionSelected(prediction)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
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
