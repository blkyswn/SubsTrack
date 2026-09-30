package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Preorder
import com.example.data.ScheduledSubscription
import com.example.data.ScheduledWithDetails
import com.example.ui.components.CalendarMarker
import com.example.ui.components.MonthCalendar
import com.example.ui.viewmodel.BookishViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpcomingCalendarScreen(
    viewModel: BookishViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheduled by viewModel.rawScheduledState.collectAsState()
    val preorders by viewModel.rawPreordersState.collectAsState()
    val allSkipMethods by viewModel.allSubscriptionSkipMethodsState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var itemToPromptSkip by remember { mutableStateOf<ScheduledWithDetails?>(null) }

    val dateFormatKey = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val todayStr = remember { dateFormatKey.format(Date()) }
    var focusedDateStr by remember { mutableStateOf(todayStr) }

    val calendarLazyListState = rememberLazyListState()

    // Filter type for day list: "All", "Subscriptions", "Preorders"
    var filterType by remember { mutableStateOf("All") }

    // Combined markers for MonthCalendar
    val markerMap = remember(scheduled, preorders) {
        val map = mutableMapOf<String, MutableList<CalendarMarker>>()

        scheduled.forEach { item ->
            val dateStr = dateFormatKey.format(Date(item.scheduled.dueDate))
            val statusColor = when (item.scheduled.status.lowercase()) {
                "upcoming" -> Color(0xFF64748B)
                "renewed", "paid" -> Color(0xFF2563EB)
                "forwarded" -> Color(0xFF0EA5E9)
                "in suite" -> Color(0xFFD97706)
                "shipped" -> Color(0xFF8B5CF6)
                "received" -> Color(0xFF10B981)
                "skipped" -> Color(0xFFEF4444)
                else -> Color(0xFF2563EB)
            }
            val title = item.subscriptionType?.title?.ifBlank { null }
                ?: item.scheduled.bookTitle.ifBlank { "Subscription" }
            val marker = CalendarMarker(
                id = "sched_${item.scheduled.id}",
                title = title,
                color = statusColor,
                imageUrl = item.scheduled.picturePath.takeIf { !it.isNullOrEmpty() }
                    ?: item.bookstore?.profilePic.takeIf { !it.isNullOrEmpty() && it != "ic_launcher_foreground" }
            )
            map.getOrPut(dateStr) { mutableListOf() }.add(marker)
        }

        preorders.forEach { item ->
            val dateStr = dateFormatKey.format(Date(item.preorder.rangedSaleDateStart))
            val statusColor = when (item.preorder.status.lowercase()) {
                "upcoming" -> Color(0xFF64748B)
                "preordered" -> Color(0xFF2563EB)
                "forwarded" -> Color(0xFF0EA5E9)
                "in suite" -> Color(0xFFD97706)
                "released" -> Color(0xFFF97316)
                "shipped" -> Color(0xFF8B5CF6)
                "received" -> Color(0xFF10B981)
                "canceled", "cancelled" -> Color(0xFFEF4444)
                else -> Color(0xFF2563EB)
            }
            val marker = CalendarMarker(
                id = "pre_${item.preorder.id}",
                title = item.preorder.bookTitle.ifBlank { "Preorder" },
                color = statusColor,
                imageUrl = item.preorder.picturePath.takeIf { !it.isNullOrEmpty() }
                    ?: item.bookstore?.profilePic.takeIf { !it.isNullOrEmpty() && it != "ic_launcher_foreground" }
            )
            map.getOrPut(dateStr) { mutableListOf() }.add(marker)
        }

        map
    }

    val dayScheduled = remember(focusedDateStr, scheduled) {
        scheduled.filter { dateFormatKey.format(Date(it.scheduled.dueDate)) == focusedDateStr }
    }
    val dayPreorders = remember(focusedDateStr, preorders) {
        preorders.filter { dateFormatKey.format(Date(it.preorder.rangedSaleDateStart)) == focusedDateStr }
    }

    val onScheduledStateChanged: (ScheduledSubscription, ScheduledSubscription, String) -> Unit = { oldScheduled, newScheduled, message ->
        viewModel.updateScheduledSubscription(newScheduled)
        val oldStatus = oldScheduled.status.lowercase().trim()
        val newStatus = newScheduled.status.lowercase().trim()
        val wasSkipped = oldStatus == "skipped" || oldScheduled.isSkipped
        val isNowSkipped = newStatus == "skipped" || newScheduled.isSkipped
        val wasUpcoming = oldStatus == "upcoming" && !oldScheduled.isSkipped
        val isNowUpcoming = newStatus == "upcoming" && !newScheduled.isSkipped

        val isUpcomingToSkipped = wasUpcoming && isNowSkipped
        val isSkippedToUpcoming = wasSkipped && isNowUpcoming

        if (!isUpcomingToSkipped && !isSkippedToUpcoming) {
            coroutineScope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                val result = snackbarHostState.showSnackbar(
                    message = message,
                    actionLabel = "Undo",
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.updateScheduledSubscription(oldScheduled)
                }
            }
        }
    }

    val onPreorderStateChanged: (Preorder, Preorder, String) -> Unit = { oldPreorder, newPreorder, message ->
        viewModel.updatePreorder(newPreorder)
        val oldStatus = oldPreorder.status.lowercase().trim()
        val newStatus = newPreorder.status.lowercase().trim()
        val isUpcomingToSkipped = oldStatus == "upcoming" && newStatus == "skipped"
        val isSkippedToUpcoming = oldStatus == "skipped" && newStatus == "upcoming"

        if (!isUpcomingToSkipped && !isSkippedToUpcoming) {
            coroutineScope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                val result = snackbarHostState.showSnackbar(
                    message = message,
                    actionLabel = "Undo",
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.updatePreorder(oldPreorder)
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.testTag("upcoming_calendar_snackbar_host")
            )
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Upcoming Calendar",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("upcoming_calendar_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { focusedDateStr = todayStr },
                        modifier = Modifier.testTag("upcoming_calendar_today")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Today,
                            contentDescription = "Jump to Today"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            MonthCalendar(
                markerDates = markerMap,
                selectedDateStr = focusedDateStr,
                onDayClick = { dateStr, _ ->
                    focusedDateStr = dateStr
                },
                lazyListState = calendarLazyListState,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                state = calendarLazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                item {
                    val displayDateText = remember(focusedDateStr) {
                        try {
                            val date = dateFormatKey.parse(focusedDateStr)
                            if (date != null) {
                                val targetCal = Calendar.getInstance().apply { time = date }
                                val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                                val targetYear = targetCal.get(Calendar.YEAR)
                                val pattern = if (targetYear == currentYear) "MMMM d" else "MMMM d, yyyy"
                                SimpleDateFormat(pattern, Locale.getDefault()).format(date)
                            } else {
                                focusedDateStr
                            }
                        } catch (e: Exception) {
                            focusedDateStr
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = displayDateText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )

                        val totalCount = dayScheduled.size + dayPreorders.size
                        if (totalCount > 0) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (dayScheduled.isNotEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = "${dayScheduled.size} Subs",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                if (dayPreorders.isNotEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.tertiaryContainer
                                    ) {
                                        Text(
                                            text = "${dayPreorders.size} Preorders",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Filter chips if there are items from both categories
                    if (dayScheduled.isNotEmpty() && dayPreorders.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = filterType == "All",
                                onClick = { filterType = "All" },
                                label = { Text("All (${dayScheduled.size + dayPreorders.size})", fontSize = 12.sp) }
                            )
                            FilterChip(
                                selected = filterType == "Subscriptions",
                                onClick = { filterType = "Subscriptions" },
                                label = { Text("Subs (${dayScheduled.size})", fontSize = 12.sp) }
                            )
                            FilterChip(
                                selected = filterType == "Preorders",
                                onClick = { filterType = "Preorders" },
                                label = { Text("Preorders (${dayPreorders.size})", fontSize = 12.sp) }
                            )
                        }
                    }
                }

                val showSubs = (filterType == "All" || filterType == "Subscriptions") && dayScheduled.isNotEmpty()
                val showPreorders = (filterType == "All" || filterType == "Preorders") && dayPreorders.isNotEmpty()

                if (!showSubs && !showPreorders) {
                    item {
                        Text(
                            text = "No deliveries or preorders scheduled for this day.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                } else {
                    if (showSubs) {
                        if (filterType == "All" && showPreorders) {
                            item {
                                Text(
                                    text = "Scheduled Deliveries",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)
                                )
                            }
                        }
                        items(dayScheduled, key = { "sched_${it.scheduled.id}" }) { item ->
                            ScheduledItemRow(
                                item = item,
                                viewModel = viewModel,
                                onPromptSkip = { itemToPromptSkip = it },
                                onStateChanged = onScheduledStateChanged
                            )
                        }
                    }

                    if (showPreorders) {
                        if (filterType == "All" && showSubs) {
                            item {
                                Text(
                                    text = "Pre-orders",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
                                )
                            }
                        }
                        items(dayPreorders, key = { "pre_${it.preorder.id}" }) { item ->
                            PreorderItemRow(
                                item = item,
                                viewModel = viewModel,
                                onDeleted = { viewModel.deletePreorder(it) },
                                onStateChanged = onPreorderStateChanged
                            )
                        }
                    }
                }
            }
        }

        // Hoisted Skip Action Prompt Dialog for Scheduled Deliveries
        itemToPromptSkip?.let { scheduledItem ->
            SkipActionPromptDialog(
                scheduledWithDetails = scheduledItem,
                allSkipMethods = allSkipMethods,
                allScheduled = scheduled,
                viewModel = viewModel,
                onDismiss = { itemToPromptSkip = null }
            )
        }
    }
}
