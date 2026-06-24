package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.components.CalendarMarker
import com.example.ui.components.MonthCalendar
import com.example.ui.viewmodel.BookishViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionsScreen(
    viewModel: BookishViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Overview, 1 = Subscriptions
    val context = LocalContext.current

    // Observe flows
    val scheduledList by viewModel.filteredScheduledState.collectAsState()
    val subscriptionsList by viewModel.filteredSubscriptionsState.collectAsState()
    val bookstores by viewModel.bookstoresState.collectAsState()

    // Dialog trigger states
    var showAddSubscriptionDialog by remember { mutableStateOf(false) }
    var showAddScheduledDialog by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }

    // Search bar state
    val searchOverviewQuery by viewModel.subOverviewSearch.collectAsState()
    val searchSubQuery by viewModel.subSearch.collectAsState()
    var showSearchRow by remember { mutableStateOf(false) }

    // Calendar toggles
    val isOverviewCalendar by viewModel.subOverviewIsCalendarView.collectAsState()
    val isSubCalendar by viewModel.subIsCalendarView.collectAsState()

    // Active timeframe for Overview counters (7 days, monthly, yearly)
    var currentOverviewTimeframe by remember { mutableStateOf(0) } // 0 = 7 days, 1 = monthly (30d), 2 = yearly (365d)

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                TopAppBar(
                    title = {
                        Text(
                            text = if (selectedTab == 0) "Subscription Overview" else "My Subscriptions",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    },
                    actions = {
                        // Action 1: Toggle View (Calendar / List)
                        IconButton(
                            onClick = {
                                if (selectedTab == 0) {
                                    viewModel.subOverviewIsCalendarView.value = !isOverviewCalendar
                                } else {
                                    viewModel.subIsCalendarView.value = !isSubCalendar
                                }
                            },
                            modifier = Modifier.testTag("sub_toggle_view")
                        ) {
                            Icon(
                                imageVector = if (selectedTab == 0) {
                                    if (isOverviewCalendar) Icons.Default.List else Icons.Default.CalendarMonth
                                } else {
                                    if (isSubCalendar) Icons.Default.List else Icons.Default.CalendarMonth
                                },
                                contentDescription = "Toggle View"
                            )
                        }

                        // Action 2: Toggle Search Bar
                        IconButton(
                            onClick = { showSearchRow = !showSearchRow },
                            modifier = Modifier.testTag("sub_search")
                        ) {
                            Icon(
                                imageVector = if (showSearchRow) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Search"
                            )
                        }

                        // Action 3: Filter Button
                        IconButton(
                            onClick = { showFilterDialog = true },
                            modifier = Modifier.testTag("sub_filter")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Filter"
                            )
                        }

                        // Action 4: Add Button
                        IconButton(
                            onClick = {
                                if (selectedTab == 0) {
                                    showAddScheduledDialog = true
                                } else {
                                    showAddSubscriptionDialog = true
                                }
                            },
                            modifier = Modifier.testTag("sub_add")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add"
                            )
                        }
                    }
                )

                // Tab Row for Subscriptions Screen
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Overview") },
                        modifier = Modifier.testTag("sub_tab_overview")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Subscriptions") },
                        modifier = Modifier.testTag("sub_tab_list")
                    )
                }

                // Expandable Search Field
                AnimatedVisibility(visible = showSearchRow) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        OutlinedTextField(
                            value = if (selectedTab == 0) searchOverviewQuery else searchSubQuery,
                            onValueChange = {
                                if (selectedTab == 0) {
                                    viewModel.subOverviewSearch.value = it
                                } else {
                                    viewModel.subSearch.value = it
                                }
                            },
                            placeholder = {
                                Text(
                                    if (selectedTab == 0) "Search books or authors..."
                                    else "Search subscription title..."
                                )
                            },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if ((if (selectedTab == 0) searchOverviewQuery else searchSubQuery).isNotEmpty()) {
                                    IconButton(onClick = {
                                        if (selectedTab == 0) viewModel.subOverviewSearch.value = ""
                                        else viewModel.subSearch.value = ""
                                    }) {
                                        Icon(Icons.Default.Clear, contentDescription = null)
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sub_search_input")
                        )
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (selectedTab == 0) {
                // TAB 1: OVERVIEW SCREEN
                // Timeframe Selectors (7 Days, Monthly, Yearly) - mimics swiping/clicking navigation
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val timeframes = listOf("7 Days", "Monthly", "Yearly")
                    timeframes.forEachIndexed { index, label ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (currentOverviewTimeframe == index) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (currentOverviewTimeframe == index) MaterialTheme.colorScheme.primary
                                    else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { currentOverviewTimeframe = index }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (currentOverviewTimeframe == index) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Compute Counters based on Timeframe
                val timeframeDays = when (currentOverviewTimeframe) {
                    0 -> 7
                    1 -> 30
                    else -> 365
                }
                val now = System.currentTimeMillis()
                val dayMs = 24 * 60 * 60 * 1000L
                val maxTime = now + timeframeDays * dayMs

                // Filter the current list of scheduled renewals falling inside timeframe
                val activeScheduled = scheduledList.filter {
                    !it.scheduled.isSkipped && it.scheduled.dueDate in now..maxTime
                }
                val skippedScheduled = scheduledList.filter {
                    it.scheduled.isSkipped && it.scheduled.dueDate in now..maxTime
                }
                val totalAmountDue = activeScheduled.sumOf { it.subscriptionType?.price ?: 0.0 }

                // Display upper stats view with three counters
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Active", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "${activeScheduled.size}",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Divider(
                            modifier = Modifier
                                .height(40.dp)
                                .width(1.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Skipped", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "${skippedScheduled.size}",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Divider(
                            modifier = Modifier
                                .height(40.dp)
                                .width(1.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Due Cost", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "${viewModel.userState.value?.currency ?: "$"}${String.format("%.2f", totalAmountDue)}",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (isOverviewCalendar) {
                    // CALENDAR VIEW (TAB 1)
                    val dateFormatKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val markerMap = remember(scheduledList) {
                        scheduledList.groupBy { dateFormatKey.format(Date(it.scheduled.dueDate)) }
                            .mapValues { entry ->
                                entry.value.map { item ->
                                    CalendarMarker(
                                        id = item.scheduled.id.toString(),
                                        title = item.scheduled.bookTitle,
                                        color = if (item.scheduled.isSkipped) Color.Red else Color.Green
                                    )
                                }
                            }
                    }

                    var focusedDayItems by remember { mutableStateOf<List<ScheduledWithDetails>>(emptyList()) }
                    var focusedDateStr by remember { mutableStateOf("") }

                    MonthCalendar(
                        markerDates = markerMap,
                        onDayClick = { dateStr, markers ->
                            focusedDateStr = dateStr
                            focusedDayItems = scheduledList.filter { dateFormatKey.format(Date(it.scheduled.dueDate)) == dateStr }
                        },
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    if (focusedDateStr.isNotEmpty()) {
                        Text(
                            text = "Deliveries on $focusedDateStr:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )

                        if (focusedDayItems.isEmpty()) {
                            Text(
                                "No deliveries scheduled for this day.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        } else {
                            LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
                                items(focusedDayItems) { item ->
                                    ScheduledItemRow(item, viewModel)
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "Tap a calendar date to view day deliveries",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            fontWeight = FontWeight.Medium
                        )
                    }

                } else {
                    // LIST VIEW Grouped by Due Date (TAB 1)
                    val dateFormatGroup = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())
                    val groupedScheduled = remember(scheduledList) {
                        scheduledList.groupBy { dateFormatGroup.format(Date(it.scheduled.dueDate)) }
                    }

                    if (groupedScheduled.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No scheduled subscription deliveries matching search/filters.")
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            groupedScheduled.keys.forEach { dateHeader ->
                                item {
                                    Text(
                                        text = dateHeader,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                                items(groupedScheduled[dateHeader] ?: emptyList()) { item ->
                                    ScheduledItemRow(item, viewModel)
                                }
                            }
                        }
                    }
                }

            } else {
                // TAB 2: SUBSCRIPTIONS MAIN LIST
                // Upper stats view with monthly and yearly spent counters
                var monthlyTotal = 0.0
                var yearlyTotal = 0.0
                for (s in subscriptionsList) {
                    val sub = s.subscription
                    if (sub.status == "Active") {
                        val p = sub.price
                        when (sub.frequency.lowercase()) {
                            "weekly" -> {
                                monthlyTotal += p * 4.33
                                yearlyTotal += p * 52.0
                            }
                            "monthly" -> {
                                monthlyTotal += p
                                yearlyTotal += p * 12.0
                            }
                            "bi-monthly" -> {
                                monthlyTotal += p / 2.0
                                yearlyTotal += p * 6.0
                            }
                            "quarterly" -> {
                                monthlyTotal += p / 3.0
                                yearlyTotal += p * 4.0
                            }
                            "yearly" -> {
                                monthlyTotal += p / 12.0
                                yearlyTotal += p
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Est. Spend / Month", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "${viewModel.userState.value?.currency ?: "$"}${String.format("%.2f", monthlyTotal)}",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Divider(
                            modifier = Modifier
                                .height(40.dp)
                                .width(1.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Est. Spend / Year", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "${viewModel.userState.value?.currency ?: "$"}${String.format("%.2f", yearlyTotal)}",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (isSubCalendar) {
                    // CALENDAR VIEW (TAB 2)
                    val dateFormatKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val markerColor = MaterialTheme.colorScheme.secondary
                    val markerMap = remember(subscriptionsList, markerColor) {
                        subscriptionsList.groupBy { dateFormatKey.format(Date(it.subscription.dueDate)) }
                            .mapValues { entry ->
                                entry.value.map { item ->
                                    CalendarMarker(
                                        id = item.subscription.id.toString(),
                                        title = item.subscription.title,
                                        color = markerColor
                                    )
                                }
                            }
                    }

                    var focusedDaySubs by remember { mutableStateOf<List<SubscriptionWithBookstore>>(emptyList()) }
                    var focusedDateStr by remember { mutableStateOf("") }

                    MonthCalendar(
                        markerDates = markerMap,
                        onDayClick = { dateStr, markers ->
                            focusedDateStr = dateStr
                            focusedDaySubs = subscriptionsList.filter { dateFormatKey.format(Date(it.subscription.dueDate)) == dateStr }
                        },
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    if (focusedDateStr.isNotEmpty()) {
                        Text(
                            text = "Sub Renewals on $focusedDateStr:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )

                        if (focusedDaySubs.isEmpty()) {
                            Text(
                                "No subscriptions renew on this day.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        } else {
                            LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
                                items(focusedDaySubs) { item ->
                                    SubscriptionItemRow(item, viewModel)
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "Tap a calendar date to view renewals",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            fontWeight = FontWeight.Medium
                        )
                    }

                } else {
                    // LIST VIEW (TAB 2)
                    if (subscriptionsList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No active subscriptions matching search/filters.")
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(subscriptionsList) { item ->
                                SubscriptionItemRow(item, viewModel)
                            }
                        }
                    }
                }
            }
        }

        // Add Scheduled Delivery Dialog (Overview Tab)
        if (showAddScheduledDialog) {
            AddScheduledSubscriptionDialog(
                viewModel = viewModel,
                subscriptionsList = subscriptionsList,
                onDismiss = { showAddScheduledDialog = false }
            )
        }

        // Add Subscription Type Dialog (Subscriptions Tab)
        if (showAddSubscriptionDialog) {
            AddSubscriptionTypeDialog(
                viewModel = viewModel,
                bookstores = bookstores,
                onDismiss = { showAddSubscriptionDialog = false }
            )
        }

        // Shared Filter Dialog
        if (showFilterDialog) {
            SubscriptionFilterDialog(
                viewModel = viewModel,
                bookstores = bookstores,
                selectedTab = selectedTab,
                onDismiss = { showFilterDialog = false }
            )
        }
    }
}

@Composable
fun ScheduledItemRow(item: ScheduledWithDetails, viewModel: BookishViewModel) {
    val currency = viewModel.userState.collectAsState().value?.currency ?: "$"
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    var showEditDialog by remember { mutableStateOf(false) }

    val accentColor = if (item.scheduled.isSkipped) {
        MaterialTheme.colorScheme.error
    } else {
        when (item.scheduled.status.lowercase()) {
            "received" -> MaterialTheme.colorScheme.secondary
            "shipped" -> MaterialTheme.colorScheme.primary
            "paid" -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.primary
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showEditDialog = true }
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.scheduled.isSkipped) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Asymmetric Left Indicator Bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(64.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentColor)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = item.scheduled.bookTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (item.scheduled.isSkipped) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    if (item.scheduled.isSkipped) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.errorContainer)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Skipped", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
                Text(
                    text = "By ${item.scheduled.bookAuthor}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (item.subscriptionType != null) {
                    Text(
                        text = "${item.subscriptionType.title} • ${item.bookstore?.name ?: "Unknown"}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                if (item.scheduled.description.isNotEmpty()) {
                    Text(
                        text = item.scheduled.description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 4.dp),
                        maxLines = 2
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (item.subscriptionType != null) {
                    Text(
                        text = "$currency${String.format("%.2f", item.subscriptionType.price)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when (item.scheduled.status.lowercase()) {
                                "received" -> MaterialTheme.colorScheme.secondaryContainer
                                "shipped" -> MaterialTheme.colorScheme.primaryContainer
                                "paid" -> MaterialTheme.colorScheme.tertiaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = item.scheduled.status,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (item.scheduled.status.lowercase()) {
                            "received" -> MaterialTheme.colorScheme.onSecondaryContainer
                            "shipped" -> MaterialTheme.colorScheme.onPrimaryContainer
                            "paid" -> MaterialTheme.colorScheme.onTertiaryContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }

    if (showEditDialog) {
        EditScheduledSubscriptionDialog(
            scheduledWithDetails = item,
            viewModel = viewModel,
            onDismiss = { showEditDialog = false }
        )
    }
}

@Composable
fun SubscriptionItemRow(item: SubscriptionWithBookstore, viewModel: BookishViewModel) {
    val currency = viewModel.userState.collectAsState().value?.currency ?: "$"
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    var showEditDialog by remember { mutableStateOf(false) }

    val accentColor = when (item.subscription.status.lowercase()) {
        "active" -> MaterialTheme.colorScheme.primary
        "waitlist" -> MaterialTheme.colorScheme.tertiary
        "paused" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.outline
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showEditDialog = true }
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Asymmetric Left Indicator Bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(56.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentColor)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.subscription.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = "Bookstore: ${item.bookstore?.name ?: "Direct / Unknown"}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Freq: ${item.subscription.frequency}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Next Billing: ${dateFormat.format(Date(item.subscription.dueDate))}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "$currency${String.format("%.2f", item.subscription.price)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when (item.subscription.status.lowercase()) {
                                "active" -> MaterialTheme.colorScheme.primaryContainer
                                "waitlist" -> MaterialTheme.colorScheme.tertiaryContainer
                                "paused" -> MaterialTheme.colorScheme.errorContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = item.subscription.status,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (item.subscription.status.lowercase()) {
                            "active" -> MaterialTheme.colorScheme.onPrimaryContainer
                            "waitlist" -> MaterialTheme.colorScheme.onTertiaryContainer
                            "paused" -> MaterialTheme.colorScheme.onErrorContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }

    if (showEditDialog) {
        EditSubscriptionTypeDialog(
            subscriptionWithBookstore = item,
            viewModel = viewModel,
            bookstores = viewModel.bookstoresState.collectAsState().value,
            onDismiss = { showEditDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSubscriptionTypeDialog(
    viewModel: BookishViewModel,
    bookstores: List<Bookstore>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedBookstoreId by remember { mutableStateOf(bookstores.firstOrNull()?.id ?: 0) }
    var title by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Active") }
    var priceStr by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("Monthly") }
    var alertDaysStr by remember { mutableStateOf("3") }

    val calendar = Calendar.getInstance()
    var dueDate by remember { mutableStateOf(calendar.timeInMillis) }
    var startDate by remember { mutableStateOf(calendar.timeInMillis) }
    var finishDate by remember { mutableStateOf(calendar.timeInMillis + 365 * 24 * 60 * 60 * 1000L) }

    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Subscription", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("Select Bookstore", fontWeight = FontWeight.Medium, fontSize = 14.dp.value.sp)
                    if (bookstores.isEmpty()) {
                        Text("No bookstores registered! Add one first.", color = Color.Red, fontSize = 12.sp)
                    } else {
                        var expandedStore by remember { mutableStateOf(false) }
                        val currentStore = bookstores.find { it.id == selectedBookstoreId } ?: bookstores.first()
                        Box {
                            OutlinedButton(onClick = { expandedStore = true }) {
                                Text(currentStore.name)
                            }
                            DropdownMenu(expanded = expandedStore, onDismissRequest = { expandedStore = false }) {
                                bookstores.forEach { store ->
                                    DropdownMenuItem(
                                        text = { Text(store.name) },
                                        onClick = {
                                            selectedBookstoreId = store.id
                                            expandedStore = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Subscription Title") },
                        modifier = Modifier.fillMaxWidth().testTag("add_sub_title")
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Status select
                        var expandedStatus by remember { mutableStateOf(false) }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Status", fontSize = 12.sp)
                            OutlinedButton(onClick = { expandedStatus = true }) {
                                Text(status)
                            }
                            DropdownMenu(expanded = expandedStatus, onDismissRequest = { expandedStatus = false }) {
                                listOf("Active", "Waitlist", "Paused", "Canceled").forEach { st ->
                                    DropdownMenuItem(
                                        text = { Text(st) },
                                        onClick = {
                                            status = st
                                            expandedStatus = false
                                        }
                                    )
                                }
                            }
                        }

                        // Frequency select
                        var expandedFreq by remember { mutableStateOf(false) }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Frequency", fontSize = 12.sp)
                            OutlinedButton(onClick = { expandedFreq = true }) {
                                Text(frequency)
                            }
                            DropdownMenu(expanded = expandedFreq, onDismissRequest = { expandedFreq = false }) {
                                listOf("Weekly", "Monthly", "Bi-Monthly", "Quarterly", "Yearly").forEach { f ->
                                    DropdownMenuItem(
                                        text = { Text(f) },
                                        onClick = {
                                            frequency = f
                                            expandedFreq = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        label = { Text("Price") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth().testTag("add_sub_price")
                    )
                }

                item {
                    OutlinedTextField(
                        value = alertDaysStr,
                        onValueChange = { alertDaysStr = it },
                        label = { Text("Alert Days Before Due") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Date selectors using Native DatePickerDialog
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Start Date: ${dateFormat.format(Date(startDate))}", fontSize = 13.sp)
                            TextButton(onClick = {
                                val c = Calendar.getInstance()
                                DatePickerDialog(context, { _, year, month, day ->
                                    val newCal = Calendar.getInstance()
                                    newCal.set(year, month, day)
                                    startDate = newCal.timeInMillis
                                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                            }) {
                                Text("Pick")
                            }
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Next Due Date: ${dateFormat.format(Date(dueDate))}", fontSize = 13.sp)
                            TextButton(onClick = {
                                val c = Calendar.getInstance()
                                DatePickerDialog(context, { _, year, month, day ->
                                    val newCal = Calendar.getInstance()
                                    newCal.set(year, month, day)
                                    dueDate = newCal.timeInMillis
                                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                            }) {
                                Text("Pick")
                            }
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Finish Date: ${dateFormat.format(Date(finishDate))}", fontSize = 13.sp)
                            TextButton(onClick = {
                                val c = Calendar.getInstance()
                                DatePickerDialog(context, { _, year, month, day ->
                                    val newCal = Calendar.getInstance()
                                    newCal.set(year, month, day)
                                    finishDate = newCal.timeInMillis
                                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                            }) {
                                Text("Pick")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedPrice = priceStr.toDoubleOrNull() ?: 0.0
                    val parsedAlert = alertDaysStr.toIntOrNull() ?: 3
                    if (title.isNotEmpty() && selectedBookstoreId > 0) {
                        viewModel.addSubscriptionType(
                            bookstoreId = selectedBookstoreId,
                            title = title,
                            status = status,
                            price = parsedPrice,
                            dueDate = dueDate,
                            startDate = startDate,
                            finishDate = finishDate,
                            notificationAlertDays = parsedAlert,
                            frequency = frequency
                        )
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("confirm_add_sub")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSubscriptionTypeDialog(
    subscriptionWithBookstore: SubscriptionWithBookstore,
    viewModel: BookishViewModel,
    bookstores: List<Bookstore>,
    onDismiss: () -> Unit
) {
    val sub = subscriptionWithBookstore.subscription
    val context = LocalContext.current

    var selectedBookstoreId by remember { mutableStateOf(sub.bookstoreId) }
    var title by remember { mutableStateOf(sub.title) }
    var status by remember { mutableStateOf(sub.status) }
    var priceStr by remember { mutableStateOf(sub.price.toString()) }
    var frequency by remember { mutableStateOf(sub.frequency) }
    var alertDaysStr by remember { mutableStateOf((sub.notificationAlertDays ?: 3).toString()) }

    var dueDate by remember { mutableStateOf(sub.dueDate) }
    var startDate by remember { mutableStateOf(sub.startDate) }
    var finishDate by remember { mutableStateOf(sub.finishDate) }

    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Subscription", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("Select Bookstore", fontWeight = FontWeight.Medium)
                    var expandedStore by remember { mutableStateOf(false) }
                    val currentStore = bookstores.find { it.id == selectedBookstoreId } ?: bookstores.firstOrNull()
                    Box {
                        OutlinedButton(onClick = { expandedStore = true }) {
                            Text(currentStore?.name ?: "Direct")
                        }
                        DropdownMenu(expanded = expandedStore, onDismissRequest = { expandedStore = false }) {
                            bookstores.forEach { store ->
                                DropdownMenuItem(
                                    text = { Text(store.name) },
                                    onClick = {
                                        selectedBookstoreId = store.id
                                        expandedStore = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Subscription Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        var expandedStatus by remember { mutableStateOf(false) }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Status", fontSize = 12.sp)
                            OutlinedButton(onClick = { expandedStatus = true }) {
                                Text(status)
                            }
                            DropdownMenu(expanded = expandedStatus, onDismissRequest = { expandedStatus = false }) {
                                listOf("Active", "Waitlist", "Paused", "Canceled").forEach { st ->
                                    DropdownMenuItem(
                                        text = { Text(st) },
                                        onClick = {
                                            status = st
                                            expandedStatus = false
                                        }
                                    )
                                }
                            }
                        }

                        var expandedFreq by remember { mutableStateOf(false) }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Frequency", fontSize = 12.sp)
                            OutlinedButton(onClick = { expandedFreq = true }) {
                                Text(frequency)
                            }
                            DropdownMenu(expanded = expandedFreq, onDismissRequest = { expandedFreq = false }) {
                                listOf("Weekly", "Monthly", "Bi-Monthly", "Quarterly", "Yearly").forEach { f ->
                                    DropdownMenuItem(
                                        text = { Text(f) },
                                        onClick = {
                                            frequency = f
                                            expandedFreq = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        label = { Text("Price") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = alertDaysStr,
                        onValueChange = { alertDaysStr = it },
                        label = { Text("Alert Days") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Start: ${dateFormat.format(Date(startDate))}", fontSize = 13.sp)
                            TextButton(onClick = {
                                val c = Calendar.getInstance().apply { timeInMillis = startDate }
                                DatePickerDialog(context, { _, year, month, day ->
                                    val newCal = Calendar.getInstance()
                                    newCal.set(year, month, day)
                                    startDate = newCal.timeInMillis
                                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                            }) {
                                Text("Pick")
                            }
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Due: ${dateFormat.format(Date(dueDate))}", fontSize = 13.sp)
                            TextButton(onClick = {
                                val c = Calendar.getInstance().apply { timeInMillis = dueDate }
                                DatePickerDialog(context, { _, year, month, day ->
                                    val newCal = Calendar.getInstance()
                                    newCal.set(year, month, day)
                                    dueDate = newCal.timeInMillis
                                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                            }) {
                                Text("Pick")
                            }
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Finish: ${dateFormat.format(Date(finishDate))}", fontSize = 13.sp)
                            TextButton(onClick = {
                                val c = Calendar.getInstance().apply { timeInMillis = finishDate }
                                DatePickerDialog(context, { _, year, month, day ->
                                    val newCal = Calendar.getInstance()
                                    newCal.set(year, month, day)
                                    finishDate = newCal.timeInMillis
                                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                            }) {
                                Text("Pick")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        viewModel.deleteSubscriptionType(sub)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }

                Button(
                    onClick = {
                        val parsedPrice = priceStr.toDoubleOrNull() ?: 0.0
                        val parsedAlert = alertDaysStr.toIntOrNull() ?: 3
                        if (title.isNotEmpty()) {
                            viewModel.updateSubscriptionType(
                                sub.copy(
                                    bookstoreId = selectedBookstoreId,
                                    title = title,
                                    status = status,
                                    price = parsedPrice,
                                    dueDate = dueDate,
                                    startDate = startDate,
                                    finishDate = finishDate,
                                    notificationAlertDays = parsedAlert,
                                    frequency = frequency
                                )
                            )
                            onDismiss()
                        }
                    }
                ) {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScheduledSubscriptionDialog(
    viewModel: BookishViewModel,
    subscriptionsList: List<SubscriptionWithBookstore>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedSubId by remember { mutableStateOf(subscriptionsList.firstOrNull()?.subscription?.id ?: 0) }
    var bookTitle by remember { mutableStateOf("") }
    var bookAuthor by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Upcoming") }

    val calendar = Calendar.getInstance()
    var dueDate by remember { mutableStateOf(calendar.timeInMillis) }

    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Scheduled Delivery", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("Select Subscription", fontWeight = FontWeight.Medium)
                    if (subscriptionsList.isEmpty()) {
                        Text("No subscriptions active! Create one first.", color = Color.Red, fontSize = 12.sp)
                    } else {
                        var expandedSub by remember { mutableStateOf(false) }
                        val currentSub = subscriptionsList.find { it.subscription.id == selectedSubId } ?: subscriptionsList.first()
                        Box {
                            OutlinedButton(onClick = { expandedSub = true }) {
                                Text(currentSub.subscription.title)
                            }
                            DropdownMenu(expanded = expandedSub, onDismissRequest = { expandedSub = false }) {
                                subscriptionsList.forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text(s.subscription.title) },
                                        onClick = {
                                            selectedSubId = s.subscription.id
                                            expandedSub = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = bookTitle,
                        onValueChange = { bookTitle = it },
                        label = { Text("Book Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = bookAuthor,
                        onValueChange = { bookAuthor = it },
                        label = { Text("Book Author") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description / Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    var expandedStatus by remember { mutableStateOf(false) }
                    Column {
                        Text("Status", fontSize = 12.sp)
                        OutlinedButton(onClick = { expandedStatus = true }) {
                            Text(status)
                        }
                        DropdownMenu(expanded = expandedStatus, onDismissRequest = { expandedStatus = false }) {
                            listOf("Upcoming", "Paid", "Shipped", "Received").forEach { st ->
                                DropdownMenuItem(
                                    text = { Text(st) },
                                    onClick = {
                                        status = st
                                        expandedStatus = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Due Date: ${dateFormat.format(Date(dueDate))}", fontSize = 14.sp)
                        TextButton(onClick = {
                            val c = Calendar.getInstance()
                            DatePickerDialog(context, { _, year, month, day ->
                                val newCal = Calendar.getInstance()
                                newCal.set(year, month, day)
                                dueDate = newCal.timeInMillis
                            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                        }) {
                            Text("Pick Date")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (bookTitle.isNotEmpty() && selectedSubId > 0) {
                        viewModel.addScheduledSubscription(
                            subscriptionTypeId = selectedSubId,
                            bookTitle = bookTitle,
                            bookAuthor = bookAuthor,
                            description = description,
                            dueDate = dueDate,
                            status = status
                        )
                        onDismiss()
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScheduledSubscriptionDialog(
    scheduledWithDetails: ScheduledWithDetails,
    viewModel: BookishViewModel,
    onDismiss: () -> Unit
) {
    val sc = scheduledWithDetails.scheduled
    val context = LocalContext.current

    var bookTitle by remember { mutableStateOf(sc.bookTitle) }
    var bookAuthor by remember { mutableStateOf(sc.bookAuthor) }
    var description by remember { mutableStateOf(sc.description) }
    var status by remember { mutableStateOf(sc.status) }
    var isSkipped by remember { mutableStateOf(sc.isSkipped) }

    var dueDate by remember { mutableStateOf(sc.dueDate) }

    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Scheduled Delivery", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = bookTitle,
                        onValueChange = { bookTitle = it },
                        label = { Text("Book Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = bookAuthor,
                        onValueChange = { bookAuthor = it },
                        label = { Text("Book Author") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        var expandedStatus by remember { mutableStateOf(false) }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Status", fontSize = 12.sp)
                            OutlinedButton(onClick = { expandedStatus = true }) {
                                Text(status)
                            }
                            DropdownMenu(expanded = expandedStatus, onDismissRequest = { expandedStatus = false }) {
                                listOf("Upcoming", "Paid", "Shipped", "Received").forEach { st ->
                                    DropdownMenuItem(
                                        text = { Text(st) },
                                        onClick = {
                                            status = st
                                            expandedStatus = false
                                        }
                                    )
                                }
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Skip Issue", fontSize = 12.sp)
                            Switch(
                                checked = isSkipped,
                                onCheckedChange = { isSkipped = it }
                            )
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Due Date: ${dateFormat.format(Date(dueDate))}", fontSize = 14.sp)
                        TextButton(onClick = {
                            val c = Calendar.getInstance().apply { timeInMillis = dueDate }
                            DatePickerDialog(context, { _, year, month, day ->
                                val newCal = Calendar.getInstance()
                                newCal.set(year, month, day)
                                dueDate = newCal.timeInMillis
                            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                        }) {
                            Text("Pick Date")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        viewModel.deleteScheduledSubscription(sc)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }

                Button(
                    onClick = {
                        if (bookTitle.isNotEmpty()) {
                            viewModel.updateScheduledSubscription(
                                sc.copy(
                                    bookTitle = bookTitle,
                                    bookAuthor = bookAuthor,
                                    description = description,
                                    status = status,
                                    isSkipped = isSkipped,
                                    dueDate = dueDate
                                )
                            )
                            onDismiss()
                        }
                    }
                ) {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun SubscriptionFilterDialog(
    viewModel: BookishViewModel,
    bookstores: List<Bookstore>,
    selectedTab: Int,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter Subscriptions", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (selectedTab == 0) {
                    // FILTER 1: Overview Tab Filters
                    Text("Filter Overview Deliveries", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    // Bookstore Filter
                    val currentStoreId by viewModel.subOverviewFilterBookstore.collectAsState()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Bookstore: ", modifier = Modifier.weight(1f))
                        var expandedStore by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(onClick = { expandedStore = true }) {
                                Text(bookstores.find { it.id == currentStoreId }?.name ?: "All")
                            }
                            DropdownMenu(expanded = expandedStore, onDismissRequest = { expandedStore = false }) {
                                DropdownMenuItem(
                                    text = { Text("All") },
                                    onClick = {
                                        viewModel.subOverviewFilterBookstore.value = null
                                        expandedStore = false
                                    }
                                )
                                bookstores.forEach { store ->
                                    DropdownMenuItem(
                                        text = { Text(store.name) },
                                        onClick = {
                                            viewModel.subOverviewFilterBookstore.value = store.id
                                            expandedStore = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Status Filter
                    val currentType by viewModel.subOverviewFilterType.collectAsState()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Status: ", modifier = Modifier.weight(1f))
                        var expandedType by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(onClick = { expandedType = true }) {
                                Text(currentType ?: "All")
                            }
                            DropdownMenu(expanded = expandedType, onDismissRequest = { expandedType = false }) {
                                DropdownMenuItem(
                                    text = { Text("All") },
                                    onClick = {
                                        viewModel.subOverviewFilterType.value = null
                                        expandedType = false
                                    }
                                )
                                listOf("Upcoming", "Paid", "Shipped", "Received").forEach { ty ->
                                    DropdownMenuItem(
                                        text = { Text(ty) },
                                        onClick = {
                                            viewModel.subOverviewFilterType.value = ty
                                            expandedType = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Author / Book Freeform Filters
                    var authorText by remember { mutableStateOf(viewModel.subOverviewFilterAuthor.value ?: "") }
                    var bookText by remember { mutableStateOf(viewModel.subOverviewFilterBook.value ?: "") }

                    OutlinedTextField(
                        value = authorText,
                        onValueChange = {
                            authorText = it
                            viewModel.subOverviewFilterAuthor.value = if (it.isBlank()) null else it
                        },
                        label = { Text("Filter by Author Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = bookText,
                        onValueChange = {
                            bookText = it
                            viewModel.subOverviewFilterBook.value = if (it.isBlank()) null else it
                        },
                        label = { Text("Filter by Book Title") },
                        modifier = Modifier.fillMaxWidth()
                    )

                } else {
                    // FILTER 2: Subscriptions Main Tab Filters
                    Text("Filter Active Subscriptions", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    // Bookstore Filter
                    val currentStoreId by viewModel.subFilterBookstore.collectAsState()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Bookstore: ", modifier = Modifier.weight(1f))
                        var expandedStore by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(onClick = { expandedStore = true }) {
                                Text(bookstores.find { it.id == currentStoreId }?.name ?: "All")
                            }
                            DropdownMenu(expanded = expandedStore, onDismissRequest = { expandedStore = false }) {
                                DropdownMenuItem(
                                    text = { Text("All") },
                                    onClick = {
                                        viewModel.subFilterBookstore.value = null
                                        expandedStore = false
                                    }
                                )
                                bookstores.forEach { store ->
                                    DropdownMenuItem(
                                        text = { Text(store.name) },
                                        onClick = {
                                            viewModel.subFilterBookstore.value = store.id
                                            expandedStore = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Status Filter
                    val currentStatus by viewModel.subFilterStatus.collectAsState()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Status: ", modifier = Modifier.weight(1f))
                        var expandedStatus by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(onClick = { expandedStatus = true }) {
                                Text(currentStatus ?: "All")
                            }
                            DropdownMenu(expanded = expandedStatus, onDismissRequest = { expandedStatus = false }) {
                                DropdownMenuItem(
                                    text = { Text("All") },
                                    onClick = {
                                        viewModel.subFilterStatus.value = null
                                        expandedStatus = false
                                    }
                                )
                                listOf("Active", "Waitlist", "Paused", "Canceled").forEach { st ->
                                    DropdownMenuItem(
                                        text = { Text(st) },
                                        onClick = {
                                            viewModel.subFilterStatus.value = st
                                            expandedStatus = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Frequency Filter
                    val currentFreq by viewModel.subFilterFrequency.collectAsState()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Frequency: ", modifier = Modifier.weight(1f))
                        var expandedFreq by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(onClick = { expandedFreq = true }) {
                                Text(currentFreq ?: "All")
                            }
                            DropdownMenu(expanded = expandedFreq, onDismissRequest = { expandedFreq = false }) {
                                DropdownMenuItem(
                                    text = { Text("All") },
                                    onClick = {
                                        viewModel.subFilterFrequency.value = null
                                        expandedFreq = false
                                    }
                                )
                                listOf("Weekly", "Monthly", "Bi-Monthly", "Quarterly", "Yearly").forEach { f ->
                                    DropdownMenuItem(
                                        text = { Text(f) },
                                        onClick = {
                                            viewModel.subFilterFrequency.value = f
                                            expandedFreq = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (selectedTab == 0) {
                        viewModel.subOverviewFilterBookstore.value = null
                        viewModel.subOverviewFilterType.value = null
                        viewModel.subOverviewFilterAuthor.value = null
                        viewModel.subOverviewFilterBook.value = null
                    } else {
                        viewModel.subFilterBookstore.value = null
                        viewModel.subFilterStatus.value = null
                        viewModel.subFilterFrequency.value = null
                    }
                    onDismiss()
                }
            ) {
                Text("Clear All")
            }
        }
    )
}
