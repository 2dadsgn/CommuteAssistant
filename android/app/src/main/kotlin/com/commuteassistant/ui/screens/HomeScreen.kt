package com.commuteassistant.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.commuteassistant.domain.model.*
import com.commuteassistant.viewmodel.HomeViewModel
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddRoutine: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Commute Assistant", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Smart departure recommendations",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshRecommendations() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddRoutine,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Route") }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                uiState.routines.isEmpty() -> EmptyState(onAddRoutine)

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.routines, key = { it.id }) { routine ->
                        RoutineCard(
                            routine = routine,
                            recommendation = uiState.recommendations[routine.id],
                            onDelete = { viewModel.deleteRoutine(routine) }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) } // FAB clearance
                }
            }
        }
    }
}

@Composable
private fun RoutineCard(
    routine: CommuteRoutine,
    recommendation: DepartureRecommendation?,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(true) }

    Card(
        onClick = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        routine.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "${routine.originName}  →  ${routine.destinationName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(Modifier.height(8.dp))

            // Usual time
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    "Target arrival: ${routine.usualDepartureTime.format(timeFormatter)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Recommendation section
            AnimatedVisibility(expanded) {
                recommendation?.let { rec ->
                    Column {
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(12.dp))
                        RecommendationSection(routine = routine, rec = rec)
                    }
                } ?: run {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun RecommendationSection(routine: CommuteRoutine, rec: DepartureRecommendation) {
    val earlyByMin = java.time.Duration.between(
        rec.recommendedDepartureTime, routine.usualDepartureTime
    ).toMinutes()

    // Recommended departure highlight
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Recommended departure", style = MaterialTheme.typography.labelSmall)
                Text(
                    rec.recommendedDepartureTime.format(timeFormatter),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (earlyByMin > 0) {
                    Text(
                        "${earlyByMin} min earlier than usual",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            ConfidenceBadge(rec.confidencePercent)
        }
    }

    Spacer(Modifier.height(8.dp))

    // Reason
    Row(verticalAlignment = Alignment.Top) {
        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.width(6.dp))
        Text(rec.reason, style = MaterialTheme.typography.bodySmall)
    }

    // Est. travel time
    Spacer(Modifier.height(6.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("Est. travel: ${rec.estimatedTravelMinutes} min", style = MaterialTheme.typography.bodySmall)
    }

    // Alternatives
    if (rec.alternativeTimes.isNotEmpty()) {
        Spacer(Modifier.height(10.dp))
        Text("Alternatives", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            rec.alternativeTimes.forEach { alt ->
                AssistChip(
                    onClick = {},
                    label = { Text(alt.format(timeFormatter)) },
                    leadingIcon = { Icon(Icons.Default.AccessTime, null, Modifier.size(14.dp)) }
                )
            }
        }
    }
}

@Composable
private fun ConfidenceBadge(percent: Int) {
    val color = when {
        percent >= 80 -> Color(0xFF4CAF50)
        percent >= 55 -> Color(0xFFFFC107)
        else          -> Color(0xFFFF5722)
    }
    Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.15f)) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${percent}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
            Text("confidence", style = MaterialTheme.typography.labelSmall, color = color)
        }
    }
}

@Composable
private fun EmptyState(onAdd: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.DirectionsCar, contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
        Spacer(Modifier.height(16.dp))
        Text("No routes yet", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Add your commute route and the app will learn your traffic patterns to suggest the ideal departure time.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onAdd) {
            Icon(Icons.Default.Add, null)
            Spacer(Modifier.width(8.dp))
            Text("Add your first route")
        }
    }
}
