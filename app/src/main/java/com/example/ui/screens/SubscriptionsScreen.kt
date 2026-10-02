package com.example.ui.screens

import com.example.ui.components.InlineReminderSelector
import com.example.ui.components.MultiSelectChipGroup
import com.example.ui.components.OtherFormTabContent
import com.example.ui.components.PackageDetailsDialog
import com.example.ui.components.PackageTrackingQuickButton
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.res.painterResource
import com.example.R
 
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.example.utils.saveImageToInternalStorage
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.components.InteractiveImagePicker
import com.example.ui.components.ImageOptionsDialog
import com.example.ui.components.ImageViewerDialog
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
    val selectedTab by viewModel.subSelectedTab.collectAsState() // 0 = Overview (Renewals), 1 = Subscriptions
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()
    val snackbarHostState = remember { SnackbarHostState() }

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

    // Observe flows
    val userState by viewModel.userState.collectAsState()
    val scheduledList by viewModel.filteredScheduledState.collectAsState()
    val subscriptionsList by viewModel.filteredSubscriptionsState.collectAsState()
    val rawSubscriptions by viewModel.rawSubscriptionsState.collectAsState()
    val bookstores by viewModel.bookstoresState.collectAsState()
    val allSkipMethods by viewModel.allSubscriptionSkipMethodsState.collectAsState()
    val rawScheduled by viewModel.rawScheduledState.collectAsState()
    val allPackages by viewModel.allPackagesState.collectAsState()
    val userAddresses by viewModel.userAddressesState.collectAsState()

    // Dialog trigger states
    var showAddSubscriptionDialog by remember { mutableStateOf(false) }
    var showAddScheduledDialog by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var itemToPromptSkip by remember { mutableStateOf<ScheduledWithDetails?>(null) }

    // Search bar state
    val searchOverviewQuery by viewModel.subOverviewSearch.collectAsState()
    val searchSubQuery by viewModel.subSearch.collectAsState()
    var showSearchRow by remember { mutableStateOf(false) }

    // Calendar toggles
    val isOverviewCalendar by viewModel.subOverviewIsCalendarView.collectAsState()
    val isSubCalendar by viewModel.subIsCalendarView.collectAsState()

    // Filter states for active filter badge
    val overviewFilterStoreIds by viewModel.subOverviewFilterBookstores.collectAsState()
    val overviewFilterTypeIds by viewModel.subOverviewFilterSubTypeIds.collectAsState()
    val overviewFilterTypes by viewModel.subOverviewFilterTypes.collectAsState()
    val overviewFilterAuthor by viewModel.subOverviewFilterAuthor.collectAsState()
    val overviewFilterBook by viewModel.subOverviewFilterBook.collectAsState()

    val subFilterStoreIds by viewModel.subFilterBookstores.collectAsState()
    val subFilterStatuses by viewModel.subFilterStatuses.collectAsState()
    val subFilterFrequencies by viewModel.subFilterFrequencies.collectAsState()
    val subFilterSubTypeIds by viewModel.subFilterSubTypeIds.collectAsState()

    val isFilterActive = if (selectedTab == 0) {
        overviewFilterStoreIds.isNotEmpty() || overviewFilterTypeIds.isNotEmpty() || overviewFilterTypes.isNotEmpty() || !overviewFilterAuthor.isNullOrBlank() || !overviewFilterBook.isNullOrBlank()
    } else {
        subFilterStoreIds.isNotEmpty() || subFilterStatuses.isNotEmpty() || subFilterFrequencies.isNotEmpty() || subFilterSubTypeIds.isNotEmpty()
    }

    // Active timeframe for Overview counters (7 days, monthly, yearly)
    var currentOverviewTimeframe by remember { mutableStateOf(0) } // 0 = 7 days, 1 = monthly (30d), 2 = yearly (365d)

    var subListViewTab by remember { mutableStateOf(0) } // 0 = Upcoming, 1 = Past, 2 = All
    val todayStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    LaunchedEffect(scheduledList, todayStart) {
        val upcomingScheduled = scheduledList.filter { it.scheduled.dueDate >= todayStart }
        if (upcomingScheduled.isNotEmpty()) {
            val closest = upcomingScheduled.minByOrNull { it.scheduled.dueDate }
            if (closest != null) {
                val diffMs = closest.scheduled.dueDate - todayStart
                val diffDays = diffMs / (24 * 60 * 60 * 1000f)
                currentOverviewTimeframe = when {
                    diffDays < 7f -> 0    // 7 Days
                    diffDays < 30f -> 1   // Monthly
                    else -> 2             // Yearly
                }
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.testTag("subs_snackbar_host")
            )
        },
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                TopAppBar(
                    title = {
                        Text(
                            text = "Subscriptions",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    },
                    actions = {
                        // Action 1: Toggle View (Calendar / List) for Overview, or Refresh for Subscriptions
                        if (selectedTab == 0) {
                            IconButton(
                                onClick = {
                                    viewModel.subOverviewIsCalendarView.value = !isOverviewCalendar
                                },
                                modifier = Modifier.testTag("sub_toggle_view")
                            ) {
                                Icon(
                                    imageVector = if (isOverviewCalendar) Icons.Default.List else Icons.Default.CalendarMonth,
                                    contentDescription = "Toggle View"
                                )
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    viewModel.recalculateSubscriptionSkips()
                                },
                                modifier = Modifier.testTag("sub_refresh_skips")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Recalculate Skips"
                                )
                            }
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
                            BadgedBox(
                                badge = {
                                    if (isFilterActive) {
                                        Badge(containerColor = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = "Filter",
                                    tint = if (isFilterActive) MaterialTheme.colorScheme.primary else LocalContentColor.current
                                )
                            }
                        }

                        // Action 4: Add Button
                        IconButton(
                            onClick = {
                                showAddSubscriptionDialog = true
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

                val contrastColor = if (!isDark) Color(0xFFFF5722) else MaterialTheme.colorScheme.primary

                // Tab Row for Subscriptions Screen
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = contrastColor
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { viewModel.subSelectedTab.value = 0 },
                        text = { 
                            Text(
                                text = "Renewals",
                                color = if (selectedTab == 0) contrastColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                            ) 
                        },
                        modifier = Modifier.testTag("sub_tab_overview")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { viewModel.subSelectedTab.value = 1 },
                        text = { 
                            Text(
                                text = "Subscriptions",
                                color = if (selectedTab == 1) contrastColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                            ) 
                        },
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
                val timeframes = remember(subListViewTab) {
                    when (subListViewTab) {
                        1 -> listOf("Last Week", "Last Month", "Last Year")
                        2 -> listOf("Last Year", "Last Month", "Last Week", "This Week", "This Month", "Next Week & Month", "This Year")
                        else -> listOf("This Week", "This Month", "Next Week & Month", "This Year")
                    }
                }
                val subOverviewPagerState = rememberPagerState(
                    initialPage = if (subListViewTab == 2) 3 else 0,
                    pageCount = { timeframes.size }
                )

                LaunchedEffect(subListViewTab) {
                    val targetPage = if (subListViewTab == 2) 3 else 0
                    if (subOverviewPagerState.currentPage != targetPage) {
                        subOverviewPagerState.scrollToPage(targetPage)
                    }
                }
                var showCosts by remember(userState?.displayAmounts) { mutableStateOf(userState?.displayAmounts ?: false) }
                val cardContrastColor = if (!isDark) Color(0xFFFF5722) else MaterialTheme.colorScheme.primary
                val cardContrastBg = if (!isDark) Color(0xFFFF5722).copy(alpha = 0.05f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                val cardBorder = if (!isDark) BorderStroke(1.dp, Color(0xFFFF5722).copy(alpha = 0.15f)) else null

                Spacer(modifier = Modifier.height(12.dp))

                if (!isOverviewCalendar) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = cardContrastBg
                    ),
                    border = cardBorder
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        val currentLabel = timeframes.getOrElse(subOverviewPagerState.currentPage) { timeframes.first() }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp, start = 4.dp, end = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currentLabel,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = cardContrastColor,
                                modifier = Modifier.testTag("stats_timeframe_title")
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (showCosts) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (showCosts) "Hide costs" else "Show costs",
                                    tint = cardContrastColor,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable { showCosts = !showCosts }
                                        .testTag("toggle_show_costs")
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    timeframes.indices.forEach { index ->
                                        Box(
                                            modifier = Modifier
                                                .size(if (subOverviewPagerState.currentPage == index) 7.dp else 5.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (subOverviewPagerState.currentPage == index) cardContrastColor
                                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                                )
                                                .clickable { coroutineScope.launch { subOverviewPagerState.animateScrollToPage(index) } }
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalPager(
                            state = subOverviewPagerState,
                            modifier = Modifier.fillMaxWidth()
                        ) { page ->
                            val pageLabel = timeframes.getOrElse(page) { timeframes.first() }
                            val currency = userState?.currency ?: "$"

                            if (pageLabel == "Next Week & Month") {
                                val thisWeekStart = java.util.Calendar.getInstance().apply {
                                    timeInMillis = todayStart
                                    firstDayOfWeek = java.util.Calendar.MONDAY
                                    set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
                                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                                    set(java.util.Calendar.MINUTE, 0)
                                    set(java.util.Calendar.SECOND, 0)
                                    set(java.util.Calendar.MILLISECOND, 0)
                                    if (timeInMillis > todayStart) {
                                        add(java.util.Calendar.WEEK_OF_YEAR, -1)
                                    }
                                }.timeInMillis
                                val startNW = java.util.Calendar.getInstance().apply {
                                    timeInMillis = thisWeekStart
                                    add(java.util.Calendar.WEEK_OF_YEAR, 1)
                                }.timeInMillis
                                val endNW = java.util.Calendar.getInstance().apply {
                                    timeInMillis = startNW
                                    add(java.util.Calendar.DAY_OF_YEAR, 7)
                                    add(java.util.Calendar.MILLISECOND, -1)
                                }.timeInMillis

                                val startNM = java.util.Calendar.getInstance().apply {
                                    timeInMillis = todayStart
                                    set(java.util.Calendar.DAY_OF_MONTH, 1)
                                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                                    set(java.util.Calendar.MINUTE, 0)
                                    set(java.util.Calendar.SECOND, 0)
                                    set(java.util.Calendar.MILLISECOND, 0)
                                    add(java.util.Calendar.MONTH, 1)
                                }.timeInMillis
                                val endNM = java.util.Calendar.getInstance().apply {
                                    timeInMillis = startNM
                                    add(java.util.Calendar.MONTH, 1)
                                    add(java.util.Calendar.MILLISECOND, -1)
                                }.timeInMillis

                                val upcomingNW = scheduledList.filter {
                                    it.scheduled.status.equals("Upcoming", ignoreCase = true) && it.scheduled.dueDate in startNW..endNW
                                }
                                val skippedNW = scheduledList.filter {
                                    it.scheduled.status.equals("Skipped", ignoreCase = true) && it.scheduled.dueDate in startNW..endNW
                                }
                                val dueCostNW = upcomingNW.sumOf { it.subscriptionType?.price ?: 0.0 }

                                val upcomingNM = scheduledList.filter {
                                    it.scheduled.status.equals("Upcoming", ignoreCase = true) && it.scheduled.dueDate in startNM..endNM
                                }
                                val skippedNM = scheduledList.filter {
                                    it.scheduled.status.equals("Skipped", ignoreCase = true) && it.scheduled.dueDate in startNM..endNM
                                }
                                val dueCostNM = upcomingNM.sumOf { it.subscriptionType?.price ?: 0.0 }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Min),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Left Side: Next Week
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(IntrinsicSize.Min),
                                            horizontalArrangement = Arrangement.SpaceAround,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = "Upcoming\nNext Week",
                                                    fontSize = 9.5.sp,
                                                    lineHeight = 10.5.sp,
                                                    textAlign = TextAlign.Center,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.height(3.5.dp))
                                                Text(
                                                    text = "${upcomingNW.size}",
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF64748B)
                                                )
                                            }
                                            Divider(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .padding(vertical = 2.dp)
                                                    .width(1.dp),
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                            )
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = "Skipped\nNext Week",
                                                    fontSize = 9.5.sp,
                                                    lineHeight = 10.5.sp,
                                                    textAlign = TextAlign.Center,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.height(3.5.dp))
                                                Text(
                                                    text = "${skippedNW.size}",
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFEF4444)
                                                )
                                            }
                                        }
                                        if (showCosts) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Divider(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 8.dp),
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = "Due Cost\nNext Week",
                                                    fontSize = 9.5.sp,
                                                    lineHeight = 10.5.sp,
                                                    textAlign = TextAlign.Center,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.height(3.5.dp))
                                                Text(
                                                    text = "${currency}${String.format("%.2f", dueCostNW)}",
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (!isDark) Color(0xFFE64A19) else MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                        }
                                    }

                                    Divider(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .padding(vertical = 4.dp)
                                            .width(1.dp),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                                    )

                                    // Right Side: Next Month
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(IntrinsicSize.Min),
                                            horizontalArrangement = Arrangement.SpaceAround,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = "Upcoming\nNext Month",
                                                    fontSize = 9.5.sp,
                                                    lineHeight = 10.5.sp,
                                                    textAlign = TextAlign.Center,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.height(3.5.dp))
                                                Text(
                                                    text = "${upcomingNM.size}",
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF64748B)
                                                )
                                            }
                                            Divider(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .padding(vertical = 2.dp)
                                                    .width(1.dp),
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                            )
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = "Skipped\nNext Month",
                                                    fontSize = 9.5.sp,
                                                    lineHeight = 10.5.sp,
                                                    textAlign = TextAlign.Center,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.height(3.5.dp))
                                                Text(
                                                    text = "${skippedNM.size}",
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFEF4444)
                                                )
                                            }
                                        }
                                        if (showCosts) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Divider(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 8.dp),
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = "Due Cost\nNext Month",
                                                    fontSize = 9.5.sp,
                                                    lineHeight = 10.5.sp,
                                                    textAlign = TextAlign.Center,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.height(3.5.dp))
                                                Text(
                                                    text = "${currency}${String.format("%.2f", dueCostNM)}",
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (!isDark) Color(0xFFE64A19) else MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                val (startTime, endTime) = when (pageLabel) {
                                    "Last Week" -> {
                                        val thisWeekStart = java.util.Calendar.getInstance().apply {
                                            timeInMillis = todayStart
                                            firstDayOfWeek = java.util.Calendar.MONDAY
                                            set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
                                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                                            set(java.util.Calendar.MINUTE, 0)
                                            set(java.util.Calendar.SECOND, 0)
                                            set(java.util.Calendar.MILLISECOND, 0)
                                            if (timeInMillis > todayStart) {
                                                add(java.util.Calendar.WEEK_OF_YEAR, -1)
                                            }
                                        }.timeInMillis
                                        val start = java.util.Calendar.getInstance().apply {
                                            timeInMillis = thisWeekStart
                                            add(java.util.Calendar.WEEK_OF_YEAR, -1)
                                        }.timeInMillis
                                        val end = java.util.Calendar.getInstance().apply {
                                            timeInMillis = thisWeekStart
                                            add(java.util.Calendar.MILLISECOND, -1)
                                        }.timeInMillis
                                        Pair(start, end)
                                    }
                                    "Last Month" -> {
                                        val start = java.util.Calendar.getInstance().apply {
                                            set(java.util.Calendar.DAY_OF_MONTH, 1)
                                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                                            set(java.util.Calendar.MINUTE, 0)
                                            set(java.util.Calendar.SECOND, 0)
                                            set(java.util.Calendar.MILLISECOND, 0)
                                            add(java.util.Calendar.MONTH, -1)
                                        }.timeInMillis
                                        val end = java.util.Calendar.getInstance().apply {
                                            timeInMillis = start
                                            add(java.util.Calendar.MONTH, 1)
                                            add(java.util.Calendar.MILLISECOND, -1)
                                        }.timeInMillis
                                        Pair(start, end)
                                    }
                                    "Last Year" -> {
                                        val start = java.util.Calendar.getInstance().apply {
                                            set(java.util.Calendar.DAY_OF_YEAR, 1)
                                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                                            set(java.util.Calendar.MINUTE, 0)
                                            set(java.util.Calendar.SECOND, 0)
                                            set(java.util.Calendar.MILLISECOND, 0)
                                            add(java.util.Calendar.YEAR, -1)
                                        }.timeInMillis
                                        val end = java.util.Calendar.getInstance().apply {
                                            timeInMillis = start
                                            add(java.util.Calendar.YEAR, 1)
                                            add(java.util.Calendar.MILLISECOND, -1)
                                        }.timeInMillis
                                        Pair(start, end)
                                    }
                                    "This Week" -> {
                                        val start = java.util.Calendar.getInstance().apply {
                                            timeInMillis = todayStart
                                            firstDayOfWeek = java.util.Calendar.MONDAY
                                            set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
                                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                                            set(java.util.Calendar.MINUTE, 0)
                                            set(java.util.Calendar.SECOND, 0)
                                            set(java.util.Calendar.MILLISECOND, 0)
                                            if (timeInMillis > todayStart) {
                                                add(java.util.Calendar.WEEK_OF_YEAR, -1)
                                            }
                                        }.timeInMillis
                                        val end = java.util.Calendar.getInstance().apply {
                                            timeInMillis = start
                                            add(java.util.Calendar.DAY_OF_YEAR, 7)
                                            add(java.util.Calendar.MILLISECOND, -1)
                                        }.timeInMillis
                                        Pair(start, end)
                                    }
                                    "This Month" -> {
                                        val start = java.util.Calendar.getInstance().apply {
                                            set(java.util.Calendar.DAY_OF_MONTH, 1)
                                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                                            set(java.util.Calendar.MINUTE, 0)
                                            set(java.util.Calendar.SECOND, 0)
                                            set(java.util.Calendar.MILLISECOND, 0)
                                        }.timeInMillis
                                        val end = java.util.Calendar.getInstance().apply {
                                            timeInMillis = start
                                            add(java.util.Calendar.MONTH, 1)
                                            add(java.util.Calendar.MILLISECOND, -1)
                                        }.timeInMillis
                                        Pair(start, end)
                                    }
                                    else -> { // "This Year"
                                        val start = java.util.Calendar.getInstance().apply {
                                            set(java.util.Calendar.DAY_OF_YEAR, 1)
                                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                                            set(java.util.Calendar.MINUTE, 0)
                                            set(java.util.Calendar.SECOND, 0)
                                            set(java.util.Calendar.MILLISECOND, 0)
                                        }.timeInMillis
                                        val end = java.util.Calendar.getInstance().apply {
                                            timeInMillis = start
                                            add(java.util.Calendar.YEAR, 1)
                                            add(java.util.Calendar.MILLISECOND, -1)
                                        }.timeInMillis
                                        Pair(start, end)
                                    }
                                }

                                val packagesByScheduledId = remember(allPackages) {
                                    allPackages.filter { it.originTable == "scheduled_subs" }.associateBy { it.originId }
                                }
                                val addressesById = remember(userAddresses) {
                                    userAddresses.associateBy { it.id }
                                }

                                val upcomingScheduled = scheduledList.filter {
                                    it.scheduled.status.equals("Upcoming", ignoreCase = true) && !it.scheduled.isSkipped && it.scheduled.dueDate in startTime..endTime
                                }
                                val skippedScheduled = scheduledList.filter {
                                    (it.scheduled.status.equals("Skipped", ignoreCase = true) || it.scheduled.isSkipped) && it.scheduled.dueDate in startTime..endTime
                                }
                                val renewedScheduled = scheduledList.filter {
                                    val status = it.scheduled.status.lowercase().trim()
                                    if ((status == "renewed" || status == "paid") && !it.scheduled.isSkipped) {
                                        val pkg = packagesByScheduledId[it.scheduled.id]
                                        val date = pkg?.purchaseDate ?: it.scheduled.dueDate
                                        date in startTime..endTime
                                    } else false
                                }
                                val forwardedScheduled = scheduledList.filter {
                                    val status = it.scheduled.status.lowercase().trim()
                                    if (status == "forwarded" && !it.scheduled.isSkipped) {
                                        val pkg = packagesByScheduledId[it.scheduled.id]
                                        val date = pkg?.storeShippingDate
                                        date != null && date in startTime..endTime
                                    } else false
                                }
                                val inSuiteScheduled = scheduledList.filter {
                                    val status = it.scheduled.status.lowercase().trim()
                                    if (status == "in suite" && !it.scheduled.isSkipped) {
                                        val pkg = packagesByScheduledId[it.scheduled.id]
                                        val date = pkg?.forwarderReceivedDate
                                        date != null && date in startTime..endTime
                                    } else false
                                }
                                val shippedScheduled = scheduledList.filter {
                                    val status = it.scheduled.status.lowercase().trim()
                                    if (status == "shipped" && !it.scheduled.isSkipped) {
                                        val pkg = packagesByScheduledId[it.scheduled.id]
                                        val hasForwarding = it.subscriptionType?.shippingAddressId?.let { addrId ->
                                            addressesById[addrId]?.forwardingServiceId != null
                                        } ?: false
                                        val date = if (hasForwarding) pkg?.forwarderShippedDate else pkg?.storeShippingDate
                                        date != null && date in startTime..endTime
                                    } else false
                                }
                                val receivedScheduled = scheduledList.filter {
                                    val status = it.scheduled.status.lowercase().trim()
                                    if (status == "received" && !it.scheduled.isSkipped) {
                                        val pkg = packagesByScheduledId[it.scheduled.id]
                                        val date = pkg?.receivedDate
                                        date != null && date in startTime..endTime
                                    } else false
                                }

                                val dueCost = upcomingScheduled.sumOf { it.subscriptionType?.price ?: 0.0 }
                                val totalSpentScheduled = scheduledList.filter { item ->
                                    val status = item.scheduled.status.lowercase().trim()
                                    if (status == "upcoming" || status == "skipped" || item.scheduled.isSkipped) {
                                        false
                                    } else {
                                        val pkg = packagesByScheduledId[item.scheduled.id]
                                        val date = when (status) {
                                            "renewed", "paid" -> pkg?.purchaseDate ?: item.scheduled.dueDate
                                            "forwarded" -> pkg?.storeShippingDate
                                            "in suite" -> pkg?.forwarderReceivedDate
                                            "shipped" -> {
                                                val hasForwarding = item.subscriptionType?.shippingAddressId?.let { addrId ->
                                                    addressesById[addrId]?.forwardingServiceId != null
                                                } ?: false
                                                if (hasForwarding) pkg?.forwarderShippedDate else pkg?.storeShippingDate
                                            }
                                            "received" -> pkg?.receivedDate
                                            else -> item.scheduled.dueDate
                                         }
                                        date != null && date in startTime..endTime
                                    }
                                }
                                val totalSpent = totalSpentScheduled.sumOf { it.subscriptionType?.price ?: 0.0 }

                                val isPastTimeframe = pageLabel in listOf("Last Week", "Last Month", "Last Year")

                                Column(modifier = Modifier.fillMaxWidth()) {
                                    // Top Row: Upcoming, Skipped, Renewed
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceAround,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Upcoming", fontSize = 11.sp, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                 "${upcomingScheduled.size}",
                                                 fontSize = 18.sp,
                                                 fontWeight = FontWeight.Bold,
                                                 color = Color(0xFF64748B)
                                             )
                                         }
                                         Divider(
                                             modifier = Modifier
                                                 .height(28.dp)
                                                 .width(1.dp),
                                             color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                         )
                                         Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                             Text("Skipped", fontSize = 11.sp, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                             Text(
                                                 "${skippedScheduled.size}",
                                                 fontSize = 18.sp,
                                                 fontWeight = FontWeight.Bold,
                                                 color = Color(0xFFEF4444)
                                             )
                                         }
                                         Divider(
                                             modifier = Modifier
                                                 .height(28.dp)
                                                 .width(1.dp),
                                             color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                         )
                                         Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                             Text("Renewed", fontSize = 11.sp, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                             Text(
                                                 "${renewedScheduled.size}",
                                                 fontSize = 18.sp,
                                                 fontWeight = FontWeight.Bold,
                                                 color = Color(0xFF2563EB)
                                             )
                                         }
                                    }

                                    Divider(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                    )

                                    // Bottom Row: Forwarded, In Suite, Shipped, Received
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceAround,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Forwarded", fontSize = 11.sp, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                "${forwardedScheduled.size}",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0EA5E9)
                                            )
                                        }
                                        Divider(
                                            modifier = Modifier
                                                .height(28.dp)
                                                .width(1.dp),
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                        )
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("In Suite", fontSize = 11.sp, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                "${inSuiteScheduled.size}",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFD97706)
                                            )
                                        }
                                        Divider(
                                            modifier = Modifier
                                                .height(28.dp)
                                                .width(1.dp),
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                        )
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Shipped", fontSize = 11.sp, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                "${shippedScheduled.size}",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF8B5CF6)
                                            )
                                        }
                                        Divider(
                                            modifier = Modifier
                                                .height(28.dp)
                                                .width(1.dp),
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                        )
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Received", fontSize = 11.sp, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                "${receivedScheduled.size}",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF10B981)
                                            )
                                        }
                                    }

                                    if (showCosts) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Divider(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp),
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceAround,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (!isPastTimeframe) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("Due Cost", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Text(
                                                        "${currency}${String.format("%.2f", dueCost)}",
                                                        fontSize = 17.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (!isDark) Color(0xFFE64A19) else MaterialTheme.colorScheme.secondary
                                                    )
                                                }
                                                Divider(
                                                    modifier = Modifier
                                                        .height(28.dp)
                                                        .width(1.dp),
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                                )
                                            }
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("Total Spent", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text(
                                                    "${currency}${String.format("%.2f", totalSpent)}",
                                                    fontSize = 17.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (!isDark) Color(0xFF059669) else Color(0xFF10B981)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Three tab controls (Upcoming, Past, All)
                val listTabContrastColor = if (!isDark) Color(0xFFFF5722) else MaterialTheme.colorScheme.primary
                TabRow(
                    selectedTabIndex = subListViewTab,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    contentColor = listTabContrastColor,
                    divider = {}
                ) {
                    Tab(
                        selected = subListViewTab == 0,
                        onClick = { subListViewTab = 0 },
                        text = { Text("Upcoming", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                        selectedContentColor = listTabContrastColor,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Tab(
                        selected = subListViewTab == 1,
                        onClick = { subListViewTab = 1 },
                        text = { Text("Past", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                        selectedContentColor = listTabContrastColor,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Tab(
                        selected = subListViewTab == 2,
                        onClick = { subListViewTab = 2 },
                        text = { Text("All", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                        selectedContentColor = listTabContrastColor,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            if (isOverviewCalendar) {
                // CALENDAR VIEW (TAB 1)
                val overviewCalendarLazyListState = rememberLazyListState()
                val dateFormatKey = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
                val todayStr = remember { dateFormatKey.format(Date()) }

                var focusedDateStr by remember { mutableStateOf(todayStr) }
                val markerMap = remember(scheduledList) {
                    scheduledList.groupBy { dateFormatKey.format(Date(it.scheduled.dueDate)) }
                        .mapValues { entry ->
                            entry.value.map { item ->
                                CalendarMarker(
                                    id = item.scheduled.id.toString(),
                                    title = item.scheduled.bookTitle,
                                    color = when (item.scheduled.status.lowercase()) {
                                        "skipped" -> Color(0xFFEF4444)
                                        "upcoming" -> Color(0xFF64748B)
                                        "renewed", "paid" -> Color(0xFF2563EB)
                                        "forwarded" -> Color(0xFF0EA5E9)
                                        "in suite" -> Color(0xFFD97706)
                                        "shipped" -> Color(0xFF8B5CF6)
                                        "received" -> Color(0xFF10B981)
                                        else -> Color(0xFF2563EB)
                                    },
                                    imageUrl = item.scheduled.picturePath
                                )
                            }
                        }
                }

                var focusedDayItems by remember(focusedDateStr, scheduledList) {
                    mutableStateOf(scheduledList.filter { dateFormatKey.format(Date(it.scheduled.dueDate)) == focusedDateStr })
                }

                MonthCalendar(
                    markerDates = markerMap,
                    selectedDateStr = focusedDateStr,
                    onDayClick = { dateStr, markers ->
                        focusedDateStr = dateStr
                        focusedDayItems = scheduledList.filter { dateFormatKey.format(Date(it.scheduled.dueDate)) == dateStr }
                    },
                    lazyListState = overviewCalendarLazyListState,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    state = overviewCalendarLazyListState,
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

                        Text(
                            text = displayDateText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    if (focusedDayItems.isEmpty()) {
                        item {
                            Text(
                                "No deliveries scheduled for this day.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }
                    } else {
                        items(focusedDayItems, key = { it.scheduled.id }) { item ->
                            ScheduledItemRow(
                                item = item,
                                viewModel = viewModel,
                                onPromptSkip = { itemToPromptSkip = it },
                                onStateChanged = onScheduledStateChanged
                            )
                        }
                    }


                }

                } else {
                    // LIST VIEW Tabbed (Upcoming, Past, All)
                    val monthHeaderFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
                    val monthHeaderNoYearFormat = remember { SimpleDateFormat("MMMM", Locale.getDefault()) }
                    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }

                    fun formatSubGroupHeader(timeMs: Long): String {
                        val cal = Calendar.getInstance().apply { timeInMillis = timeMs }
                        return if (cal.get(Calendar.YEAR) == currentYear) {
                            monthHeaderNoYearFormat.format(Date(timeMs))
                        } else {
                            monthHeaderFormat.format(Date(timeMs))
                        }
                    }

                    val upcomingList = remember(scheduledList, todayStart) {
                        scheduledList.filter { it.scheduled.dueDate >= todayStart }
                            .sortedBy { it.scheduled.dueDate }
                    }
                    val groupedUpcoming = remember(upcomingList, currentYear) {
                        upcomingList.groupBy { formatSubGroupHeader(it.scheduled.dueDate) }
                    }

                    val pastList = remember(scheduledList, todayStart) {
                        scheduledList.filter { it.scheduled.dueDate < todayStart }
                            .sortedByDescending { it.scheduled.dueDate }
                    }
                    val groupedPast = remember(pastList, currentYear) {
                        pastList.groupBy { formatSubGroupHeader(it.scheduled.dueDate) }
                    }

                    val allSorted = remember(scheduledList) {
                        scheduledList.sortedByDescending { it.scheduled.dueDate }
                    }
                    val groupedAll = remember(allSorted, currentYear) {
                        allSorted.groupBy { formatSubGroupHeader(it.scheduled.dueDate) }
                    }

                    if (subListViewTab == 0) {
                        if (groupedUpcoming.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No upcoming scheduled subscription deliveries.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                groupedUpcoming.keys.forEach { dateHeader ->
                                    item {
                                        Text(
                                            text = dateHeader,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                                        )
                                    }
                                    items(
                                        items = groupedUpcoming[dateHeader] ?: emptyList(),
                                        key = { it.scheduled.id }
                                    ) { item ->
                                        ScheduledItemRow(
                                            item = item,
                                            viewModel = viewModel,
                                            onPromptSkip = { itemToPromptSkip = it },
                                            onStateChanged = onScheduledStateChanged
                                        )
                                    }
                                }
                            }
                        }
                    } else if (subListViewTab == 1) {
                        if (groupedPast.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No past scheduled subscription deliveries.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                groupedPast.keys.forEach { monthHeader ->
                                    item {
                                        Text(
                                            text = monthHeader,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                                        )
                                    }
                                    items(
                                        items = groupedPast[monthHeader] ?: emptyList(),
                                        key = { it.scheduled.id }
                                    ) { item ->
                                        ScheduledItemRow(
                                            item = item,
                                            viewModel = viewModel,
                                            onPromptSkip = { itemToPromptSkip = it },
                                            onStateChanged = onScheduledStateChanged
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        if (groupedAll.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No scheduled subscription deliveries matching search/filters.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                groupedAll.keys.forEach { dateHeader ->
                                    item {
                                        Text(
                                            text = dateHeader,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                                        )
                                    }
                                    items(
                                        items = groupedAll[dateHeader] ?: emptyList(),
                                        key = { it.scheduled.id }
                                    ) { item ->
                                        ScheduledItemRow(
                                            item = item,
                                            viewModel = viewModel,
                                            onPromptSkip = { itemToPromptSkip = it },
                                            onStateChanged = onScheduledStateChanged
                                        )
                                    }
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
                                "${userState?.currency ?: "$"}${String.format("%.2f", monthlyTotal)}",
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
                                "${userState?.currency ?: "$"}${String.format("%.2f", yearlyTotal)}",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

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
                    val groupedSubsByStore = remember(subscriptionsList) {
                        subscriptionsList
                            .sortedWith(
                                compareBy<SubscriptionWithBookstore> { it.bookstore?.name?.lowercase() ?: "zzzz" }
                                    .thenBy { it.subscription.startDate }
                            )
                            .groupBy { it.bookstore?.name?.takeIf { name -> name.isNotBlank() } ?: "Other" }
                    }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        groupedSubsByStore.forEach { (storeName, storeSubs) ->
                            item(key = "header_$storeName") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp, bottom = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Storefront,
                                        contentDescription = null,
                                        tint = if (!isDark) Color(0xFFFF5722) else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = storeName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (!isDark) Color(0xFFFF5722) else MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = CircleShape,
                                        color = (if (!isDark) Color(0xFFFF5722) else MaterialTheme.colorScheme.primary).copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = "${storeSubs.size}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (!isDark) Color(0xFFFF5722) else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            items(
                                items = storeSubs,
                                key = { it.subscription.id }
                            ) { item ->
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
                subscriptionTypes = rawSubscriptions.map { it.subscription },
                selectedTab = selectedTab,
                onDismiss = { showFilterDialog = false }
            )
        }

        // Hoisted Skip Action Prompt Dialog
        itemToPromptSkip?.let { scheduledItem ->
            SkipActionPromptDialog(
                scheduledWithDetails = scheduledItem,
                allSkipMethods = allSkipMethods,
                allScheduled = rawScheduled,
                viewModel = viewModel,
                onDismiss = { itemToPromptSkip = null }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduledItemRow(
    item: ScheduledWithDetails,
    viewModel: BookishViewModel,
    onPromptSkip: ((ScheduledWithDetails) -> Unit)? = null,
    onStateChanged: ((oldScheduled: ScheduledSubscription, newScheduled: ScheduledSubscription, message: String) -> Unit)? = null
) {
    val userState by viewModel.userState.collectAsState()
    val currency = userState?.currency ?: "$"
    val userDateFormatPattern = userState?.dateFormat ?: "yyyy-MM-dd"
    val dateFormat = remember(userDateFormatPattern) { SimpleDateFormat(userDateFormatPattern, Locale.getDefault()) }
    val displayTitle = if (item.scheduled.bookTitle.isBlank()) {
        item.subscriptionType?.title ?: "Subscription"
    } else {
        item.scheduled.bookTitle
    }

    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current
    var showEditDialog by remember { mutableStateOf(false) }
    var showViewerDialog by remember { mutableStateOf(false) }

    val activeImageModel = item.scheduled.picturePath.takeIf { !it.isNullOrEmpty() }

    if (showViewerDialog && activeImageModel != null) {
        ImageViewerDialog(
            imageUrl = activeImageModel,
            onDismiss = { showViewerDialog = false }
        )
    }

    val userAddresses by viewModel.userAddressesState.collectAsState()
    val forwardingServices by viewModel.forwardingServicesState.collectAsState()
    val allPackages by viewModel.allPackagesState.collectAsState()

    val subAddress = remember(item.subscriptionType?.shippingAddressId, userAddresses) {
        userAddresses.find { it.id == item.subscriptionType?.shippingAddressId }
    }
    val forwardingService = remember(subAddress, forwardingServices) {
        subAddress?.forwardingServiceId?.let { fsId -> forwardingServices.find { it.id == fsId } }
    }
    val packageItem = remember(allPackages, item.scheduled.id) {
        allPackages.find { it.originTable == "scheduled_subs" && it.originId == item.scheduled.id }
    }

    val curSubStatus = item.scheduled.status.lowercase().trim()
    val isStoredAtForwarder = forwardingService != null &&
        curSubStatus !in listOf("shipped", "received") &&
        packageItem?.forwarderShippedDate == null &&
        (curSubStatus == "in suite" || packageItem?.forwarderReceivedDate != null)

    val effectiveReceivedDate = packageItem?.forwarderReceivedDate ?: if (curSubStatus == "in suite") System.currentTimeMillis() else null

    val hasForwardingAddress = remember(item.subscriptionType?.shippingAddressId, userAddresses) {
        userAddresses.find { it.id == item.subscriptionType?.shippingAddressId }?.forwardingServiceId != null
    }

    val accentColor = when (item.scheduled.status.lowercase()) {
        "upcoming" -> Color(0xFF64748B) // Grey
        "renewed", "paid" -> Color(0xFF2563EB) // Blue
        "forwarded" -> Color(0xFF0EA5E9) // Sky Blue
        "in suite" -> Color(0xFFD97706) // Amber
        "skipped" -> Color(0xFFEF4444) // Red
        "shipped" -> Color(0xFF8B5CF6) // Purple
        "received" -> Color(0xFF10B981) // Green
        else -> Color(0xFF64748B)
    }

    val isUpcoming = item.scheduled.status.equals("Upcoming", ignoreCase = true)
    val isSkippedStatus = item.scheduled.status.equals("Skipped", ignoreCase = true)
    val isDueDateToCome = item.scheduled.dueDate >= (System.currentTimeMillis() - 86400000L)
    val isRenewed = item.scheduled.status.equals("Renewed", ignoreCase = true) || item.scheduled.status.equals("Paid", ignoreCase = true)
    val isForwarded = item.scheduled.status.equals("Forwarded", ignoreCase = true)
    val isInSuite = item.scheduled.status.equals("In Suite", ignoreCase = true)
    val isShipped = item.scheduled.status.equals("Shipped", ignoreCase = true)
    val isReceived = item.scheduled.status.equals("Received", ignoreCase = true)

    val canSwipeRight = isSkippedStatus || isUpcoming || isRenewed || isForwarded || isInSuite || isShipped
    val canSwipeLeft = isReceived || isShipped || isInSuite || isForwarded || isRenewed || isUpcoming || (isSkippedStatus && isDueDateToCome)
    val density = LocalDensity.current
    val thresholdPx = remember(density) { with(density) { 48.dp.toPx() } }

    key(item.scheduled.id, item.scheduled.status, item.scheduled.isSkipped) {
        val dismissState = rememberSwipeToDismissBoxState(
            positionalThreshold = { totalDistance -> thresholdPx.coerceAtMost(totalDistance * 0.35f) },
            confirmValueChange = { dismissValue ->
                when (dismissValue) {
                    SwipeToDismissBoxValue.StartToEnd -> {
                        if (canSwipeRight) {
                            val newStatus = when {
                                isSkippedStatus -> "Upcoming"
                                isUpcoming -> "Renewed"
                                isRenewed -> if (hasForwardingAddress) "Forwarded" else "Shipped"
                                isForwarded -> "In Suite"
                                isInSuite -> "Shipped"
                                isShipped -> "Received"
                                else -> item.scheduled.status
                            }
                            val isSkippedNew = if (isSkippedStatus) false else item.scheduled.isSkipped
                            if (newStatus != item.scheduled.status || isSkippedNew != item.scheduled.isSkipped) {
                                val newScheduled = item.scheduled.copy(status = newStatus, isSkipped = isSkippedNew)
                                val msg = when {
                                    isSkippedStatus -> "$displayTitle: Unskipped"
                                    else -> "$displayTitle: Status changed to $newStatus"
                                }
                                if (onStateChanged != null) {
                                    onStateChanged(item.scheduled, newScheduled, msg)
                                } else {
                                    viewModel.updateScheduledSubscription(newScheduled)
                                }
                            }
                        }
                        false
                    }
                    SwipeToDismissBoxValue.EndToStart -> {
                        if (canSwipeLeft) {
                            when {
                                isReceived -> {
                                    val newScheduled = item.scheduled.copy(status = "Shipped")
                                    val msg = "$displayTitle: Status changed to Shipped"
                                    if (onStateChanged != null) {
                                        onStateChanged(item.scheduled, newScheduled, msg)
                                    } else {
                                        viewModel.updateScheduledSubscription(newScheduled)
                                    }
                                }
                                isShipped -> {
                                    val prevStatus = if (hasForwardingAddress) "In Suite" else "Renewed"
                                    val newScheduled = item.scheduled.copy(status = prevStatus)
                                    val msg = "$displayTitle: Status changed to $prevStatus"
                                    if (onStateChanged != null) {
                                        onStateChanged(item.scheduled, newScheduled, msg)
                                    } else {
                                        viewModel.updateScheduledSubscription(newScheduled)
                                    }
                                }
                                isInSuite -> {
                                    val newScheduled = item.scheduled.copy(status = "Forwarded")
                                    val msg = "$displayTitle: Status changed to Forwarded"
                                    if (onStateChanged != null) {
                                        onStateChanged(item.scheduled, newScheduled, msg)
                                    } else {
                                        viewModel.updateScheduledSubscription(newScheduled)
                                    }
                                }
                                isForwarded -> {
                                    val newScheduled = item.scheduled.copy(status = "Renewed")
                                    val msg = "$displayTitle: Status changed to Renewed"
                                    if (onStateChanged != null) {
                                        onStateChanged(item.scheduled, newScheduled, msg)
                                    } else {
                                        viewModel.updateScheduledSubscription(newScheduled)
                                    }
                                }
                                isRenewed -> {
                                    val newScheduled = item.scheduled.copy(status = "Upcoming")
                                    val msg = "$displayTitle: Status changed to Upcoming"
                                    if (onStateChanged != null) {
                                        onStateChanged(item.scheduled, newScheduled, msg)
                                    } else {
                                        viewModel.updateScheduledSubscription(newScheduled)
                                    }
                                }
                                isUpcoming -> {
                                    val newScheduled = item.scheduled.copy(status = "Skipped", isSkipped = true)
                                    val msg = "$displayTitle: Skipped"
                                    onPromptSkip?.invoke(item)
                                    if (onStateChanged != null) {
                                        onStateChanged(item.scheduled, newScheduled, msg)
                                    } else {
                                        viewModel.updateScheduledSubscription(newScheduled)
                                    }
                                }
                                isSkippedStatus -> {
                                    val newScheduled = item.scheduled.copy(status = "Upcoming", isSkipped = false)
                                    val msg = "$displayTitle: Unskipped"
                                    if (onStateChanged != null) {
                                        onStateChanged(item.scheduled, newScheduled, msg)
                                    } else {
                                        viewModel.updateScheduledSubscription(newScheduled)
                                    }
                                }
                            }
                        }
                        false
                    }
                    else -> false
                }
            }
        )

        SwipeToDismissBox(
            state = dismissState,
            enableDismissFromEndToStart = canSwipeLeft,
            enableDismissFromStartToEnd = canSwipeRight,
            backgroundContent = {
                val direction = dismissState.dismissDirection
                val target = dismissState.targetValue
                val current = dismissState.currentValue

                val isStartToEnd = direction == SwipeToDismissBoxValue.StartToEnd || 
                                  target == SwipeToDismissBoxValue.StartToEnd || 
                                  current == SwipeToDismissBoxValue.StartToEnd
                                  
                val isEndToStart = direction == SwipeToDismissBoxValue.EndToStart || 
                                  target == SwipeToDismissBoxValue.EndToStart || 
                                  current == SwipeToDismissBoxValue.EndToStart

                val color = when {
                    isStartToEnd -> {
                        when {
                            isSkippedStatus -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            isUpcoming -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                            isRenewed -> if (hasForwardingAddress) Color(0xFF0EA5E9).copy(alpha = 0.15f) else Color(0xFF8B5CF6).copy(alpha = 0.15f)
                            isForwarded -> Color(0xFFD97706).copy(alpha = 0.15f)
                            isInSuite -> Color(0xFF8B5CF6).copy(alpha = 0.15f)
                            isShipped -> Color(0xFF10B981).copy(alpha = 0.15f)
                            else -> Color.Transparent
                        }
                    }
                    isEndToStart -> {
                        when {
                            isReceived -> Color(0xFF8B5CF6).copy(alpha = 0.15f)
                            isShipped -> if (hasForwardingAddress) Color(0xFFD97706).copy(alpha = 0.15f) else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                            isInSuite -> Color(0xFF0EA5E9).copy(alpha = 0.15f)
                            isForwarded -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                            isRenewed -> Color(0xFF64748B).copy(alpha = 0.15f)
                            isUpcoming -> Color(0xFFEF4444).copy(alpha = 0.15f)
                            isSkippedStatus -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else -> Color.Transparent
                        }
                    }
                    else -> Color.Transparent
                }
                val alignment = when {
                    isStartToEnd -> Alignment.CenterStart
                    isEndToStart -> Alignment.CenterEnd
                    else -> Alignment.Center
                }
                val icon = when {
                    isStartToEnd -> {
                        when {
                            isSkippedStatus -> Icons.AutoMirrored.Filled.Undo
                            isUpcoming -> Icons.Default.Autorenew
                            isRenewed -> if (hasForwardingAddress) Icons.Default.AltRoute else Icons.Default.LocalShipping
                            isForwarded -> Icons.Default.Warehouse
                            isInSuite -> Icons.Default.LocalShipping
                            isShipped -> Icons.Default.CheckCircle
                            else -> null
                        }
                    }
                    isEndToStart -> {
                        when {
                            isReceived -> Icons.Default.LocalShipping
                            isShipped -> if (hasForwardingAddress) Icons.Default.Warehouse else Icons.Default.Autorenew
                            isInSuite -> Icons.Default.AltRoute
                            isForwarded -> Icons.Default.Autorenew
                            isRenewed -> Icons.Default.Schedule
                            isUpcoming -> Icons.Default.Block
                            isSkippedStatus -> Icons.AutoMirrored.Filled.Undo
                            else -> null
                        }
                    }
                    else -> null
                }
                val iconTint = when {
                    isStartToEnd -> {
                        when {
                            isSkippedStatus -> MaterialTheme.colorScheme.primary
                            isUpcoming -> MaterialTheme.colorScheme.tertiary
                            isRenewed -> if (hasForwardingAddress) Color(0xFF0EA5E9) else Color(0xFF8B5CF6)
                            isForwarded -> Color(0xFFD97706)
                            isInSuite -> Color(0xFF8B5CF6)
                            isShipped -> Color(0xFF10B981)
                            else -> Color.Transparent
                        }
                    }
                    isEndToStart -> {
                        when {
                            isReceived -> Color(0xFF8B5CF6)
                            isShipped -> if (hasForwardingAddress) Color(0xFFD97706) else MaterialTheme.colorScheme.tertiary
                            isInSuite -> Color(0xFF0EA5E9)
                            isForwarded -> MaterialTheme.colorScheme.tertiary
                            isRenewed -> Color(0xFF64748B)
                            isUpcoming -> Color(0xFFEF4444)
                            isSkippedStatus -> MaterialTheme.colorScheme.primary
                            else -> Color.Transparent
                        }
                    }
                    else -> Color.Transparent
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(color)
                        .padding(horizontal = 20.dp),
                    contentAlignment = alignment
                ) {
                    icon?.let {
                        Icon(
                            imageVector = it,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showEditDialog = true }
                    .padding(vertical = 6.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when (item.scheduled.status.lowercase()) {
                        "upcoming" -> Color(0xFF64748B).copy(alpha = if (isDark) 0.12f else 0.06f)
                        "renewed", "paid" -> Color(0xFF2563EB).copy(alpha = if (isDark) 0.12f else 0.06f)
                        "forwarded" -> Color(0xFF0EA5E9).copy(alpha = if (isDark) 0.12f else 0.06f)
                        "in suite" -> Color(0xFFD97706).copy(alpha = if (isDark) 0.12f else 0.06f)
                        "skipped" -> Color(0xFFEF4444).copy(alpha = if (isDark) 0.12f else 0.06f)
                        "shipped" -> Color(0xFF8B5CF6).copy(alpha = if (isDark) 0.12f else 0.06f)
                        "received" -> Color(0xFF10B981).copy(alpha = if (isDark) 0.12f else 0.06f)
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    }
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = accentColor.copy(alpha = 0.35f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Asymmetric Left Indicator Bar
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .background(accentColor)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    // Cover / Bookstore Picture Box (Fixed size matching all fields height)
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(accentColor.copy(alpha = 0.12f))
                            .then(
                                if (activeImageModel != null) {
                                    Modifier.clickable { showViewerDialog = true }
                                } else {
                                    Modifier
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val imageModel = item.scheduled.picturePath.takeIf { !it.isNullOrEmpty() }
                            ?: item.subscriptionType?.picturePath.takeIf { !it.isNullOrEmpty() }
                            ?: item.bookstore?.profilePic.takeIf { !it.isNullOrEmpty() && it != "ic_launcher_foreground" }
                        if (imageModel != null) {
                            AsyncImage(
                                model = imageModel,
                                contentDescription = "$displayTitle Cover",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            val storeName = item.bookstore?.name?.takeIf { it.isNotBlank() } ?: displayTitle
                            val initial = (storeName.firstOrNull { it.isLetterOrDigit() } ?: 'B').uppercaseChar().toString()
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(accentColor.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = initial,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    color = accentColor
                                )
                            }
                        }

                        if (isStoredAtForwarder) {
                            com.example.ui.components.StorageCountdownPill(
                                forwarderReceivedDate = effectiveReceivedDate,
                                storageDays = forwardingService?.storageDays,
                                modifier = Modifier.align(Alignment.BottomCenter)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Text Info & Status Column
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Compact Text Info
                        Column(verticalArrangement = Arrangement.spacedBy(1.5.dp)) {
                            Text(
                                text = displayTitle,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                lineHeight = 16.sp,
                                color = if (item.scheduled.status.equals("Skipped", ignoreCase = true)) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            if (!item.scheduled.bookAuthor.isNullOrBlank()) {
                                Text(
                                    text = "By ${item.scheduled.bookAuthor}",
                                    fontSize = 11.5.sp,
                                    lineHeight = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                            if (item.subscriptionType != null) {
                                Text(
                                    text = "${item.subscriptionType.title} • ${item.bookstore?.name ?: "Unknown"}",
                                    fontSize = 10.5.sp,
                                    lineHeight = 13.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                            if (item.scheduled.rating > 0.0) {
                                com.example.ui.components.StarRatingBar(
                                    rating = item.scheduled.rating,
                                    starSize = 12.dp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        // Status Badge with icon before text & Price with dot spacer
                        val statusContainerColor = when (item.scheduled.status.lowercase()) {
                            "upcoming" -> if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9)
                            "renewed", "paid" -> if (isDark) Color(0xFF1E3A8A) else Color(0xFFDBEAFE)
                            "forwarded" -> if (isDark) Color(0xFF0C4A6E) else Color(0xFFE0F2FE)
                            "in suite" -> if (isDark) Color(0xFF78350F) else Color(0xFFFEF3C7)
                            "skipped" -> if (isDark) Color(0xFF7F1D1D) else Color(0xFFFEE2E2)
                            "shipped" -> if (isDark) Color(0xFF4C1D95) else Color(0xFFF3E8FF)
                            "received" -> if (isDark) Color(0xFF064E3B) else Color(0xFFD1FAE5)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                        val statusOnContainerColor = when (item.scheduled.status.lowercase()) {
                            "upcoming" -> if (isDark) Color(0xFFF1F5F9) else Color(0xFF334155)
                            "renewed", "paid" -> if (isDark) Color(0xFFDBEAFE) else Color(0xFF1E40AF)
                            "forwarded" -> if (isDark) Color(0xFFE0F2FE) else Color(0xFF0369A1)
                            "in suite" -> if (isDark) Color(0xFFFEF3C7) else Color(0xFF92400E)
                            "skipped" -> if (isDark) Color(0xFFFEE2E2) else Color(0xFF991B1B)
                            "shipped" -> if (isDark) Color(0xFFF3E8FF) else Color(0xFF5B21B6)
                            "received" -> if (isDark) Color(0xFFD1FAE5) else Color(0xFF065F46)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        val statusIcon = when (item.scheduled.status.lowercase()) {
                            "upcoming" -> Icons.Default.Schedule
                            "renewed", "paid" -> Icons.Default.Payments
                            "forwarded" -> Icons.Default.AltRoute
                            "in suite" -> Icons.Default.Warehouse
                            "shipped" -> Icons.Default.LocalShipping
                            "received" -> Icons.Default.CheckCircle
                            "skipped" -> Icons.Default.Block
                            else -> Icons.Default.Info
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .testTag("sub_status_tag_${item.scheduled.id}")
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(statusContainerColor)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = statusIcon,
                                    contentDescription = null,
                                    modifier = Modifier.size(11.dp),
                                    tint = statusOnContainerColor
                                )
                                Text(
                                    text = item.scheduled.status,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = statusOnContainerColor
                                )
                            }

                            if (item.subscriptionType != null) {
                                Text(
                                    text = "•",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Text(
                                    text = "$currency${String.format("%.2f", item.subscriptionType.price)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Right Column: Date Square Card
                    val dateFormatMonth = remember { SimpleDateFormat("MMM", Locale.getDefault()) }
                    val dateFormatDay = remember { SimpleDateFormat("dd", Locale.getDefault()) }
                    val eventDate = Date(item.scheduled.dueDate)
                    val monthStr = dateFormatMonth.format(eventDate).uppercase()
                    val dayStr = dateFormatDay.format(eventDate)

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = accentColor.copy(alpha = 0.08f)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.2f)),
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(52.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 4.dp, horizontal = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = monthStr,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = accentColor,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = dayStr,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
    }
}

    if (showEditDialog) {
        EditScheduledSubscriptionDialog(
            scheduledWithDetails = item,
            viewModel = viewModel,
            onDismiss = { showEditDialog = false },
            onPromptSkip = onPromptSkip
        )
    }
}

@Composable
fun SkipActionPromptDialog(
    scheduledWithDetails: ScheduledWithDetails,
    allSkipMethods: List<SubscriptionSkipMethod> = emptyList(),
    allScheduled: List<ScheduledWithDetails> = emptyList(),
    viewModel: BookishViewModel? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val bookstoreName = scheduledWithDetails.bookstore?.name?.takeIf { it.isNotBlank() }
        ?: scheduledWithDetails.subscriptionType?.title?.takeIf { it.isNotBlank() }
        ?: "bookstore"
    val subType = scheduledWithDetails.subscriptionType
    val subTypeId = scheduledWithDetails.scheduled.subscriptionTypeId

    val vmSkipMethods = viewModel?.allSubscriptionSkipMethodsState?.collectAsState()?.value ?: emptyList()
    val sourceSkipMethods = if (allSkipMethods.isNotEmpty()) allSkipMethods else vmSkipMethods

    var directMethods by remember(subTypeId) { mutableStateOf<List<SubscriptionSkipMethod>?>(null) }
    var isCheckingDb by remember(subTypeId) { mutableStateOf(viewModel != null) }

    LaunchedEffect(subTypeId) {
        if (viewModel != null) {
            val fromDb = viewModel.repository.getSkipMethodsForSubscriptionTypeDirect(subTypeId)
            directMethods = fromDb
        }
        isCheckingDb = false
    }

    val typeSkipMethods = remember(sourceSkipMethods, directMethods, subTypeId) {
        val filtered = sourceSkipMethods.filter { it.subscriptionTypeId == subTypeId }
        if (filtered.isNotEmpty()) {
            filtered.sortedBy { it.skipMethodOrder }
        } else {
            directMethods?.sortedBy { it.skipMethodOrder } ?: emptyList()
        }
    }

    val vmScheduled = viewModel?.rawScheduledState?.collectAsState()?.value ?: emptyList()
    val sourceScheduled = if (allScheduled.isNotEmpty()) allScheduled else vmScheduled

    val priorScheduledForType = remember(sourceScheduled, scheduledWithDetails) {
        sourceScheduled
            .filter { it.scheduled.subscriptionTypeId == subTypeId }
            .map { it.scheduled }
            .filter { sched ->
                sched.dueDate < scheduledWithDetails.scheduled.dueDate ||
                        (sched.dueDate == scheduledWithDetails.scheduled.dueDate && sched.id < scheduledWithDetails.scheduled.id)
            }
            .sortedBy { it.dueDate }
    }

    // Skip number: how many scheduled subscriptions were skipped immediately before skipping the current one
    val skipNumber = remember(priorScheduledForType) {
        var count = 0
        for (item in priorScheduledForType.reversed()) {
            if (item.isSkipped || item.status.equals("Skipped", ignoreCase = true)) {
                count++
            } else {
                break
            }
        }
        count
    }

    // Filter skip methods by consecutiveSkips <= skipNumber,
    // order by consecutiveSkips descending, default check (order == 1) descending, skipMethodOrder ascending,
    // and grab the first register
    val selectedMethod = remember(typeSkipMethods, skipNumber) {
        val eligible = typeSkipMethods.filter { it.consecutiveSkips <= skipNumber }
        eligible.sortedWith(
            compareByDescending<SubscriptionSkipMethod> { it.consecutiveSkips }
                .thenByDescending { it.skipMethodOrder == 1 }
                .thenBy { it.skipMethodOrder }
        ).firstOrNull() ?: typeSkipMethods.firstOrNull()
    }

    val methodType: String
    val methodValue: String
    val methodText: String

    if (selectedMethod != null) {
        methodType = selectedMethod.skipMethodType.trim()
        methodValue = selectedMethod.skipMethodValue.trim()
        methodText = selectedMethod.skipMethodText.trim()
    } else {
        // Fallback to legacy fields if no SkipMethod records exist
        methodType = subType?.skipMethod?.trim() ?: ""
        methodValue = subType?.skipLink?.trim() ?: ""
        methodText = subType?.skipText?.trim() ?: ""
    }

    val isEmail = methodType.equals("Email", ignoreCase = true)
    val isWebsite = methodType.equals("Website", ignoreCase = true)
    val isPhone = methodType.equals("Phone", ignoreCase = true)
    val isCustom = !isEmail && !isWebsite && !isPhone && methodType.isNotBlank()

    val hasValidMethod = methodValue.isNotBlank() && (isEmail || isWebsite || isPhone || isCustom)

    if (!hasValidMethod) {
        if (!isCheckingDb) {
            LaunchedEffect(Unit) {
                onDismiss()
            }
        }
        return
    }

    val dialogMessage = when {
        isEmail -> "Would you like to email $bookstoreName to skip?"
        isWebsite -> "Would you like to open $bookstoreName website to skip?"
        isPhone -> "Would you like to call $bookstoreName to skip?"
        else -> "Would you like to skip subscription for $bookstoreName via $methodType?"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Skip Subscription",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = dialogMessage,
                    fontSize = 15.sp
                )
                if (isCustom && methodValue.isNotBlank()) {
                    Text(
                        text = "Contact: $methodValue",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    try {
                        when {
                            isEmail -> {
                                val emailAddress = methodValue.removePrefix("mailto:").trim()
                                val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:")
                                    putExtra(Intent.EXTRA_EMAIL, arrayOf(emailAddress))
                                    putExtra(Intent.EXTRA_SUBJECT, "Skip Subscription - ${subType?.title ?: ""}")
                                    if (methodText.isNotBlank()) {
                                        putExtra(Intent.EXTRA_TEXT, methodText)
                                    }
                                }
                                context.startActivity(emailIntent)
                            }
                            isWebsite -> {
                                val fullUrl = if (!methodValue.startsWith("http://") && !methodValue.startsWith("https://")) {
                                    "https://$methodValue"
                                } else {
                                    methodValue
                                }
                                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl))
                                context.startActivity(webIntent)
                            }
                            isPhone -> {
                                val cleanPhone = methodValue.removePrefix("tel:").trim()
                                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPhone"))
                                context.startActivity(dialIntent)
                            }
                            else -> {
                                // Custom method: check format
                                val cleanVal = methodValue.trim()
                                when {
                                    cleanVal.startsWith("http://") || cleanVal.startsWith("https://") || cleanVal.startsWith("www.") -> {
                                        val fullUrl = if (cleanVal.startsWith("www.")) "https://$cleanVal" else cleanVal
                                        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl))
                                        context.startActivity(webIntent)
                                    }
                                    cleanVal.contains("@") && !cleanVal.contains(" ") -> {
                                        val emailAddress = cleanVal.removePrefix("mailto:").trim()
                                        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                            data = Uri.parse("mailto:")
                                            putExtra(Intent.EXTRA_EMAIL, arrayOf(emailAddress))
                                            putExtra(Intent.EXTRA_SUBJECT, "Skip Subscription - ${subType?.title ?: ""}")
                                            if (methodText.isNotBlank()) {
                                                putExtra(Intent.EXTRA_TEXT, methodText)
                                            }
                                        }
                                        context.startActivity(emailIntent)
                                    }
                                    cleanVal.all { it.isDigit() || it == '+' || it == '-' || it == '(' || it == ')' || it == ' ' } && cleanVal.length >= 5 -> {
                                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanVal"))
                                        context.startActivity(dialIntent)
                                    }
                                    else -> {
                                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, if (methodText.isNotBlank()) "$cleanVal\n\n$methodText" else cleanVal)
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Skip Subscription"))
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            ) {
                Text("Yes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("No")
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SubscriptionItemRow(item: SubscriptionWithBookstore, viewModel: BookishViewModel) {
    val userState by viewModel.userState.collectAsState()
    val currency = userState?.currency ?: "$"
    var showEditDialog by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }
    var totalDragX by remember { mutableFloatStateOf(0f) }
    var showViewerDialog by remember { mutableStateOf(false) }

    val activeSubPic = item.subscription.picturePath.takeIf { !it.isNullOrEmpty() }

    if (showViewerDialog && activeSubPic != null) {
        ImageViewerDialog(
            imageUrl = activeSubPic,
            onDismiss = { showViewerDialog = false }
        )
    }

    val isDark = isSystemInDarkTheme()
    val allSkips by viewModel.allSubscriptionSkipsState.collectAsState()
    val itemSkips = remember(allSkips, item.subscription.id) {
        allSkips.filter { it.subscriptionTypeId == item.subscription.id }
    }

    val accentColor = when (item.subscription.status.lowercase()) {
        "active" -> Color(0xFF2563EB) // Blue
        "waitlist" -> Color(0xFF64748B) // Grey
        "paused" -> Color(0xFFF97316) // Orange
        "wishlist" -> Color(0xFF8B5CF6) // Purple
        "canceled", "cancelled" -> Color(0xFFEF4444) // Red
        else -> Color(0xFF64748B)
    }

    val statusContainerColor = when (item.subscription.status.lowercase()) {
        "active" -> if (isDark) Color(0xFF1E3A8A) else Color(0xFFDBEAFE)
        "waitlist" -> if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9)
        "paused" -> if (isDark) Color(0xFF7C2D12) else Color(0xFFFFEDD5)
        "wishlist" -> if (isDark) Color(0xFF4C1D95) else Color(0xFFF3E8FF)
        "canceled", "cancelled" -> if (isDark) Color(0xFF7F1D1D) else Color(0xFFFEE2E2)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val statusOnContainerColor = when (item.subscription.status.lowercase()) {
        "active" -> if (isDark) Color(0xFFDBEAFE) else Color(0xFF1E40AF)
        "waitlist" -> if (isDark) Color(0xFFF1F5F9) else Color(0xFF334155)
        "paused" -> if (isDark) Color(0xFFFFEDD5) else Color(0xFF9A3412)
        "wishlist" -> if (isDark) Color(0xFFF3E8FF) else Color(0xFF5B21B6)
        "canceled", "cancelled" -> if (isDark) Color(0xFFFEE2E2) else Color(0xFF991B1B)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val statusIcon = when (item.subscription.status.lowercase()) {
        "active" -> Icons.Default.CheckCircle
        "waitlist" -> Icons.Default.Schedule
        "paused" -> Icons.Default.PauseCircle
        "canceled", "cancelled" -> Icons.Default.Block
        "wishlist" -> Icons.Default.Bookmark
        else -> Icons.Default.Info
    }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (totalDragX > 50f) {
                                showEditDialog = true
                            }
                            totalDragX = 0f
                        },
                        onDragCancel = { totalDragX = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            if (dragAmount > 0) {
                                totalDragX += dragAmount
                            }
                        }
                    )
                }
                .combinedClickable(
                    onClick = { isExpanded = !isExpanded },
                    onLongClick = { showEditDialog = true }
                ),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = when (item.subscription.status.lowercase()) {
                    "active" -> Color(0xFF2563EB).copy(alpha = if (isDark) 0.12f else 0.06f)
                    "waitlist" -> Color(0xFF64748B).copy(alpha = if (isDark) 0.12f else 0.06f)
                    "paused" -> Color(0xFFF97316).copy(alpha = if (isDark) 0.12f else 0.06f)
                    "wishlist" -> Color(0xFF8B5CF6).copy(alpha = if (isDark) 0.12f else 0.06f)
                    "canceled", "cancelled" -> Color(0xFFEF4444).copy(alpha = if (isDark) 0.12f else 0.06f)
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                }
            ),
            border = BorderStroke(
                width = 1.dp,
                color = accentColor.copy(alpha = 0.35f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Asymmetric Left Indicator Bar
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(accentColor)
                )

                Spacer(modifier = Modifier.width(10.dp))

                // Subscription Picture Box
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.12f))
                        .then(
                            if (activeSubPic != null) {
                                Modifier.clickable { showViewerDialog = true }
                            } else {
                                Modifier
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val subPic = item.subscription.picturePath
                    if (!subPic.isNullOrEmpty()) {
                        AsyncImage(
                            model = subPic,
                            contentDescription = "${item.subscription.title} Image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (item.bookstore != null && !item.bookstore.profilePic.isNullOrEmpty() && item.bookstore.profilePic != "ic_launcher_foreground") {
                        AsyncImage(
                            model = item.bookstore.profilePic,
                            contentDescription = "${item.bookstore.name} Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        val storeName = item.bookstore?.name?.takeIf { it.isNotBlank() } ?: item.subscription.title
                        val initial = (storeName.firstOrNull { it.isLetterOrDigit() } ?: 'B').uppercaseChar().toString()
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(accentColor.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initial,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = accentColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                val rawScheduled by viewModel.rawScheduledState.collectAsState()

                val statusLower = item.subscription.status.lowercase()
                val frequencySubtitle = when {
                    statusLower in listOf("waitlist", "canceled", "cancelled", "wishlist") -> {
                        item.subscription.frequency
                    }
                    statusLower == "paused" -> {
                        "${item.subscription.frequency} • No upcoming renewals"
                    }
                    else -> {
                        val scheduledList = rawScheduled.filter {
                            it.subscriptionType?.id == item.subscription.id || it.scheduled.subscriptionTypeId == item.subscription.id
                        }
                        val now = System.currentTimeMillis()
                        val nextScheduled = scheduledList
                            .filter { it.scheduled.dueDate >= now - 86400000L }
                            .minByOrNull { it.scheduled.dueDate }
                            ?: scheduledList.minByOrNull { it.scheduled.dueDate }

                        val isSkipped = nextScheduled?.scheduled?.isSkipped == true ||
                                nextScheduled?.scheduled?.status.equals("Skipped", ignoreCase = true)

                        if (isSkipped) {
                            "${item.subscription.frequency} • Renewal skipped"
                        } else {
                            val targetDueDate = nextScheduled?.scheduled?.dueDate ?: item.subscription.dueDate
                            val diffMs = targetDueDate - now
                            val daysLeft = kotlin.math.ceil(diffMs.toDouble() / (1000 * 60 * 60 * 24)).toLong().coerceAtLeast(0)
                            val daysText = if (daysLeft == 1L) "1 day" else "$daysLeft days"
                            "${item.subscription.frequency} • Renews in $daysText"
                        }
                    }
                }

                // Text Info Column
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = item.subscription.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.bookstore?.name ?: "Unknown",
                        fontSize = 11.5.sp,
                        lineHeight = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = frequencySubtitle,
                        fontSize = 10.5.sp,
                        lineHeight = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    item.subscription.skipType?.let { st ->
                        if (st.isNotBlank()) {
                            val baseSkipText = when (st) {
                                "Each calendar year" -> "Skips: ${item.subscription.numberOfSkips ?: 0}/yr"
                                "Every certain months" -> "Skips: ${item.subscription.numberOfSkips ?: 0} / ${item.subscription.numberOfMonths ?: 1}mo"
                                "Unlimited" -> "Skips: Unlimited"
                                "None" -> "Skips: None"
                                else -> "Skips: $st"
                            }
                            val skipText = if (st != "Unlimited" && st != "None") {
                                val now = System.currentTimeMillis()
                                val activeRegister = itemSkips.firstOrNull { skip ->
                                    skip.skipStartDate != null && skip.skipEndDate != null && now >= skip.skipStartDate && now <= skip.skipEndDate
                                } ?: itemSkips.firstOrNull { skip ->
                                    skip.skipStartDate == null || skip.skipEndDate == null
                                }
                                val skipsLeft = activeRegister?.skipsLeft ?: activeRegister?.numberOfSkips ?: item.subscription.numberOfSkips
                                if (skipsLeft != null) {
                                    "$baseSkipText • $skipsLeft left"
                                } else {
                                    baseSkipText
                                }
                            } else {
                                baseSkipText
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = skipText,
                                fontSize = 10.sp,
                                lineHeight = 12.sp,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Normal,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Right Column: Status, Price & Fold Indicator
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(statusContainerColor)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = null,
                            modifier = Modifier.size(11.dp),
                            tint = statusOnContainerColor
                        )
                        Text(
                            text = item.subscription.status,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusOnContainerColor
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "$currency${String.format("%.2f", item.subscription.price)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Fold" else "Unfold",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Unfolded Table for subscription_skips
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Skips Register",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${itemSkips.size} register${if (itemSkips.size != 1) "s" else ""}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (itemSkips.isEmpty()) {
                        Text(
                            text = "No registers in skips table for this subscription.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        var currentPage by remember { mutableIntStateOf(0) }
                        val pageSize = 5
                        val totalPages = (itemSkips.size + pageSize - 1) / pageSize
                        val safePage = currentPage.coerceIn(0, (totalPages - 1).coerceAtLeast(0))
                        val pageSkips = itemSkips.drop(safePage * pageSize).take(pageSize)
                        val userDateFormatPattern = viewModel.userState.collectAsState().value?.dateFormat ?: "yyyy-MM-dd"
                        val dateFormat = remember(userDateFormatPattern) { SimpleDateFormat(userDateFormatPattern, Locale.getDefault()) }

                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Table Header Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                                    .padding(vertical = 4.dp, horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Skip Type", modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold, fontSize = 10.5.sp)
                                Text("Skips", modifier = Modifier.weight(0.7f), fontWeight = FontWeight.Bold, fontSize = 10.5.sp, textAlign = TextAlign.Center)
                                Text("Left", modifier = Modifier.weight(0.7f), fontWeight = FontWeight.Bold, fontSize = 10.5.sp, textAlign = TextAlign.Center)
                                Text("Start Date", modifier = Modifier.weight(1.1f), fontWeight = FontWeight.Bold, fontSize = 10.5.sp, textAlign = TextAlign.Center)
                                Text("End Date", modifier = Modifier.weight(1.1f), fontWeight = FontWeight.Bold, fontSize = 10.5.sp, textAlign = TextAlign.Center)
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            // Table Data Rows
                            pageSkips.forEachIndexed { idx, skipRecord ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (idx % 2 == 0) MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                        )
                                        .padding(vertical = 4.dp, horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = skipRecord.subscriptionSkipType,
                                        modifier = Modifier.weight(1.2f),
                                        fontSize = 10.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = skipRecord.numberOfSkips?.toString() ?: "-",
                                        modifier = Modifier.weight(0.7f),
                                        fontSize = 10.5.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = skipRecord.skipsLeft?.toString() ?: "-",
                                        modifier = Modifier.weight(0.7f),
                                        fontSize = 10.5.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = skipRecord.skipStartDate?.let { dateFormat.format(Date(it)) } ?: "-",
                                        modifier = Modifier.weight(1.1f),
                                        fontSize = 10.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = skipRecord.skipEndDate?.let { dateFormat.format(Date(it)) } ?: "-",
                                        modifier = Modifier.weight(1.1f),
                                        fontSize = 10.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                if (idx < pageSkips.size - 1) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                                }
                            }

                            // Pagination controls if > 5 registers
                            if (itemSkips.size > 5) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { if (safePage > 0) currentPage = safePage - 1 },
                                        enabled = safePage > 0,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowBack, contentDescription = "Previous Page", modifier = Modifier.size(14.dp))
                                    }

                                    Text(
                                        text = "Page ${safePage + 1} of $totalPages (${itemSkips.size} total)",
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    IconButton(
                                        onClick = { if (safePage < totalPages - 1) currentPage = safePage + 1 },
                                        enabled = safePage < totalPages - 1,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowForward, contentDescription = "Next Page", modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
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

@Composable
fun SubscriptionSkipMethodsSection(
    skipMethods: List<SubscriptionSkipMethod>,
    bookstoreContacts: List<BookstoreContact>,
    subTitle: String,
    userName: String,
    onSkipMethodsChanged: (List<SubscriptionSkipMethod>) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var editingMethodIndex by remember { mutableStateOf<Int?>(null) }
    var addingMethodType by remember { mutableStateOf<String?>(null) } // "Website", "Email", "Phone", "Custom"

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Skip Methods",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )

            Box {
                FilledTonalButton(
                    onClick = { showMenu = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("add_skip_method_menu_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Method", fontSize = 13.sp)
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Add website") },
                        leadingIcon = { Icon(Icons.Default.Language, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            addingMethodType = "Website"
                        },
                        modifier = Modifier.testTag("add_website_option")
                    )
                    DropdownMenuItem(
                        text = { Text("Add email") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            addingMethodType = "Email"
                        },
                        modifier = Modifier.testTag("add_email_option")
                    )
                    DropdownMenuItem(
                        text = { Text("Add phone") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            addingMethodType = "Phone"
                        },
                        modifier = Modifier.testTag("add_phone_option")
                    )
                    DropdownMenuItem(
                        text = { Text("Add custom field") },
                        leadingIcon = { Icon(Icons.Default.AddCircle, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            addingMethodType = "Custom"
                        },
                        modifier = Modifier.testTag("add_custom_option")
                    )
                }
            }
        }

        if (skipMethods.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "No skip methods configured. Use the + button above to add Website, Email, Phone, or Custom skip methods.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp)
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                skipMethods.forEachIndexed { index, method ->
                    SkipMethodItemRow(
                        index = index,
                        totalCount = skipMethods.size,
                        method = method,
                        onEdit = {
                            editingMethodIndex = index
                        },
                        onDelete = {
                            val updated = skipMethods.toMutableList().apply { removeAt(index) }
                            onSkipMethodsChanged(updated.mapIndexed { idx, m -> m.copy(skipMethodOrder = idx + 1) })
                        },
                        onMoveUp = {
                            if (index > 0) {
                                val updated = skipMethods.toMutableList()
                                val temp = updated[index]
                                updated[index] = updated[index - 1]
                                updated[index - 1] = temp
                                onSkipMethodsChanged(updated.mapIndexed { idx, m -> m.copy(skipMethodOrder = idx + 1) })
                            }
                        },
                        onMoveDown = {
                            if (index < skipMethods.size - 1) {
                                val updated = skipMethods.toMutableList()
                                val temp = updated[index]
                                updated[index] = updated[index + 1]
                                updated[index + 1] = temp
                                onSkipMethodsChanged(updated.mapIndexed { idx, m -> m.copy(skipMethodOrder = idx + 1) })
                            }
                        }
                    )
                }
            }
        }
    }

    // Popup Editor Dialog for Add
    addingMethodType?.let { type ->
        SkipMethodEditorDialog(
            initialMethod = null,
            initialType = type,
            bookstoreContacts = bookstoreContacts,
            defaultSubTitle = subTitle,
            defaultUserName = userName,
            onSave = { newMethod ->
                val newOrder = skipMethods.size + 1
                val updated = skipMethods + newMethod.copy(skipMethodOrder = newOrder)
                onSkipMethodsChanged(updated)
                addingMethodType = null
            },
            onDismiss = { addingMethodType = null }
        )
    }

    // Popup Editor Dialog for Edit
    editingMethodIndex?.let { index ->
        if (index in skipMethods.indices) {
            val methodToEdit = skipMethods[index]
            SkipMethodEditorDialog(
                initialMethod = methodToEdit,
                initialType = methodToEdit.skipMethodType,
                bookstoreContacts = bookstoreContacts,
                defaultSubTitle = subTitle,
                defaultUserName = userName,
                onSave = { updatedMethod ->
                    val updated = skipMethods.toMutableList()
                    updated[index] = updatedMethod.copy(skipMethodOrder = index + 1)
                    onSkipMethodsChanged(updated)
                    editingMethodIndex = null
                },
                onDismiss = { editingMethodIndex = null }
            )
        } else {
            editingMethodIndex = null
        }
    }
}

@Composable
fun SkipMethodItemRow(
    index: Int,
    totalCount: Int,
    method: SubscriptionSkipMethod,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val isDefault = index == 0
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDefault) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                             else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        modifier = Modifier.fillMaxWidth().testTag("skip_method_item_$index")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reorder controls
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    IconButton(
                        onClick = onMoveUp,
                        enabled = index > 0,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowUp,
                            contentDescription = "Move Up",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onMoveDown,
                        enabled = index < totalCount - 1,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = "Move Down",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                val typeIcon = when (method.skipMethodType.lowercase()) {
                    "website" -> Icons.Default.Language
                    "email" -> Icons.Default.Email
                    "phone" -> Icons.Default.Phone
                    else -> Icons.Default.BookmarkBorder
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (isDefault) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        typeIcon,
                        contentDescription = null,
                        tint = if (isDefault) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = method.skipMethodType,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        if (isDefault) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 2.dp)
                            ) {
                                Text(
                                    "Default",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = method.skipMethodValue,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp).testTag("edit_skip_method_btn_$index")) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit skip method",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp).testTag("delete_skip_method_btn_$index")) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Delete skip method",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun SkipMethodEditorDialog(
    initialMethod: SubscriptionSkipMethod?,
    initialType: String,
    bookstoreContacts: List<BookstoreContact>,
    defaultSubTitle: String,
    defaultUserName: String,
    onSave: (SubscriptionSkipMethod) -> Unit,
    onDismiss: () -> Unit
) {
    val isCustomType = initialType == "Custom" || (initialMethod != null && initialMethod.skipMethodType !in listOf("Website", "Email", "Phone"))
    var customLabel by remember {
        mutableStateOf(
            if (isCustomType) {
                if (initialMethod != null && initialMethod.skipMethodType != "Custom") initialMethod.skipMethodType else ""
            } else ""
        )
    }

    val methodType = if (isCustomType) {
        customLabel.ifBlank { "Custom" }
    } else {
        initialMethod?.skipMethodType ?: initialType
    }

    var value by remember { mutableStateOf(initialMethod?.skipMethodValue ?: "") }
    var text by remember {
        mutableStateOf(
            initialMethod?.skipMethodText ?: if ((initialType == "Email" || methodType == "Email") && initialMethod == null) {
                "Hi there,\nI would like to skip my upcoming ${defaultSubTitle.ifBlank { "Subscription" }} related to this account.\nThank you so much!\n${defaultUserName.ifBlank { "User" }}"
            } else ""
        )
    }

    var consecutiveSkips by remember { mutableIntStateOf(initialMethod?.consecutiveSkips ?: 0) }
    var consecutiveSkipsInputStr by remember { mutableStateOf(if ((initialMethod?.consecutiveSkips ?: 0) > 0) (initialMethod?.consecutiveSkips ?: 0).toString() else "") }
    var isEditingConsecutiveSkips by remember { mutableStateOf((initialMethod?.consecutiveSkips ?: 0) > 0) }

    val matchingContacts = remember(bookstoreContacts, initialType, customLabel) {
        when {
            initialType == "Website" || methodType.equals("Website", true) ->
                bookstoreContacts.filter { it.contactType.equals("Website", true) }
            initialType == "Email" || methodType.equals("Email", true) ->
                bookstoreContacts.filter { it.contactType.equals("Email", true) }
            initialType == "Phone" || methodType.equals("Phone", true) ->
                bookstoreContacts.filter { it.contactType.equals("Phone", true) }
            else -> {
                val matchingCustom = if (customLabel.isNotBlank()) {
                    bookstoreContacts.filter { it.contactType.equals(customLabel, true) }
                } else emptyList()
                matchingCustom.ifEmpty {
                    bookstoreContacts.filter { it.contactType !in listOf("Website", "Email", "Phone") }
                }
            }
        }
    }

    val showTextField = isCustomType || methodType.equals("Email", true) || methodType.equals("Phone", true)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialMethod != null) "Edit Skip Method" else "Add Skip Method",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // If Custom field, show Label field on top of value
                if (isCustomType) {
                    OutlinedTextField(
                        value = customLabel,
                        onValueChange = { customLabel = it },
                        label = { Text("Custom Field Label", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        placeholder = { Text("e.g. Chat Support, WhatsApp") },
                        modifier = Modifier.fillMaxWidth().testTag("skip_method_custom_label")
                    )
                }

                // Helper bookstore contacts suggestions
                if (matchingContacts.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Suggestions from Bookstore Contacts:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            matchingContacts.forEach { contact ->
                                AssistChip(
                                    onClick = { value = contact.contactValue },
                                    label = { Text(contact.contactValue, maxLines = 1) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.ContactPage,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                val valueLabel = when {
                    methodType.equals("Website", true) -> "Website URL"
                    methodType.equals("Email", true) -> "Email Address"
                    methodType.equals("Phone", true) -> "Phone Number"
                    else -> "Value"
                }
                val placeholderText = when {
                    methodType.equals("Website", true) -> "https://example.com/skip"
                    methodType.equals("Email", true) -> "support@example.com"
                    methodType.equals("Phone", true) -> "+1 555-0199"
                    else -> "Enter skip method value"
                }

                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(valueLabel, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    placeholder = { Text(placeholderText) },
                    keyboardOptions = if (methodType.equals("Phone", true)) KeyboardOptions(keyboardType = KeyboardType.Phone)
                                      else if (methodType.equals("Email", true)) KeyboardOptions(keyboardType = KeyboardType.Email)
                                      else if (methodType.equals("Website", true)) KeyboardOptions(keyboardType = KeyboardType.Uri)
                                      else KeyboardOptions.Default,
                    modifier = Modifier.fillMaxWidth().testTag("skip_method_value")
                )

                if (showTextField) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text("Skip Method Text", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        placeholder = {
                            if (methodType.equals("Email", true)) Text("Email body template")
                            else if (methodType.equals("Phone", true)) Text("Calling script or notes")
                            else Text("Message or instructions")
                        },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth().testTag("skip_method_text")
                    )
                }

                // Consecutive Skips Button / Input
                if (!isEditingConsecutiveSkips) {
                    TextButton(
                        onClick = { isEditingConsecutiveSkips = true },
                        modifier = Modifier.testTag("toggle_consecutive_skips_btn")
                    ) {
                        if (consecutiveSkips == 0) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add consecutive skips")
                        } else {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Used after $consecutiveSkips consecutive skips")
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Consecutive Skips",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            TextButton(
                                onClick = {
                                    consecutiveSkips = 0
                                    consecutiveSkipsInputStr = ""
                                    isEditingConsecutiveSkips = false
                                }
                            ) {
                                Text("Remove", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                            }
                        }

                        OutlinedTextField(
                            value = consecutiveSkipsInputStr,
                            onValueChange = { input ->
                                val cleanDigits = input.filter { it.isDigit() }
                                consecutiveSkipsInputStr = cleanDigits
                                consecutiveSkips = (cleanDigits.toIntOrNull() ?: 0).coerceAtLeast(0)
                            },
                            label = { Text("Used after (skips)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            placeholder = { Text("0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            trailingIcon = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            val current = consecutiveSkipsInputStr.toIntOrNull() ?: consecutiveSkips
                                            if (current > 0) {
                                                val next = current - 1
                                                consecutiveSkips = next
                                                consecutiveSkipsInputStr = next.toString()
                                            }
                                        },
                                        enabled = (consecutiveSkipsInputStr.toIntOrNull() ?: consecutiveSkips) > 0,
                                        modifier = Modifier.size(32.dp).testTag("consecutive_skips_decrement")
                                    ) {
                                        Icon(
                                            Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Decrease skips",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            val current = consecutiveSkipsInputStr.toIntOrNull() ?: consecutiveSkips
                                            val next = (current + 1).coerceAtLeast(1)
                                            consecutiveSkips = next
                                            consecutiveSkipsInputStr = next.toString()
                                        },
                                        modifier = Modifier.size(32.dp).testTag("consecutive_skips_increment")
                                    ) {
                                        Icon(
                                            Icons.Default.KeyboardArrowUp,
                                            contentDescription = "Increase skips",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("consecutive_skips_input")
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (value.isNotBlank()) {
                        val finalConsecutive = consecutiveSkipsInputStr.toIntOrNull() ?: consecutiveSkips
                        onSave(
                            SubscriptionSkipMethod(
                                id = initialMethod?.id ?: 0,
                                subscriptionTypeId = initialMethod?.subscriptionTypeId ?: 0,
                                skipMethodOrder = initialMethod?.skipMethodOrder ?: 1,
                                skipMethodType = methodType,
                                skipMethodValue = value.trim(),
                                skipMethodText = text.trim(),
                                consecutiveSkips = finalConsecutive
                            )
                        )
                    }
                },
                enabled = value.isNotBlank() && (!isCustomType || customLabel.isNotBlank()),
                modifier = Modifier.testTag("save_skip_method_btn")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSubscriptionTypeDialog(
    viewModel: BookishViewModel,
    bookstores: List<Bookstore>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val userState by viewModel.userState.collectAsState()
    val currency = userState?.currency ?: "$"
    val userAddresses by viewModel.userAddressesState.collectAsState()
    val forwardingServices by viewModel.forwardingServicesState.collectAsState()
    var selectedShippingAddressId by remember { mutableStateOf<Int?>(null) }
    var selectedCurrency by remember { mutableStateOf(userState?.currency ?: "$") }
    var basePriceStr by remember { mutableStateOf("") }
    var discountedAmountStr by remember { mutableStateOf("") }
    var shippingPriceStr by remember { mutableStateOf("") }
    var taxPriceStr by remember { mutableStateOf("") }
    var forwardShippingPriceStr by remember { mutableStateOf("") }
    var forwardTaxPriceStr by remember { mutableStateOf("") }

    var selectedBookstoreId by remember { mutableStateOf(bookstores.firstOrNull()?.id ?: 0) }
    var title by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Active") }
    var priceStr by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("Monthly") }
    var reminderEnabled by remember { mutableStateOf(false) }
    var reminderDDayOffset by remember { mutableStateOf(0) }
    var reminderHour by remember { mutableStateOf(8) }
    var reminderMinute by remember { mutableStateOf(0) }
    var skipType by remember { mutableStateOf("None") }
    var numberOfSkipsStr by remember { mutableStateOf("") }
    var numberOfMonthsStr by remember { mutableStateOf("") }
    var skipMethodsList by remember { mutableStateOf<List<SubscriptionSkipMethod>>(emptyList()) }
    var imageUrl by remember { mutableStateOf("") }
    var showDueDatePicker by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    val calendar = Calendar.getInstance()
    var dueDate by remember { mutableStateOf(calendar.timeInMillis) }
    var startDate by remember { mutableStateOf(calendar.timeInMillis) }
    var finishDate by remember { mutableStateOf(0L) }

    val bookstoreContacts = viewModel.bookstoreContactsState.collectAsState().value
    val contactsForStore = remember(bookstoreContacts, selectedBookstoreId) {
        bookstoreContacts.filter { it.bookstoreId == selectedBookstoreId }
    }

    val userDateFormatPattern = viewModel.userState.collectAsState().value?.dateFormat ?: "yyyy-MM-dd"
    val dateFormat = remember(userDateFormatPattern) { SimpleDateFormat(userDateFormatPattern, Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Subscription", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                ScheduledSubscriptionImagePicker(
                    imageUrl = imageUrl,
                    onImageSelected = { imageUrl = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Details", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("add_sub_tab_details")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Skips", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("add_sub_tab_skips")
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Other", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("add_sub_tab_other")
                    )
                }

                if (selectedTab == 0) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            // Bookstore select
                            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Bookstore", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                if (bookstores.isEmpty()) {
                                    Text("No bookstores registered! Add one first.", color = Color.Red, fontSize = 12.sp)
                                } else {
                                    var expandedStore by remember { mutableStateOf(false) }
                                    val currentStore = bookstores.find { it.id == selectedBookstoreId } ?: bookstores.first()
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        OutlinedButton(
                                            onClick = { expandedStore = true },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(currentStore.name, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
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
                        }

                        item {
                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                label = { Text("Subscription Title", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                modifier = Modifier.fillMaxWidth().testTag("add_sub_title")
                            )
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                // Status select
                                var expandedStatus by remember { mutableStateOf(false) }
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Status", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        OutlinedButton(
                                            onClick = { expandedStatus = true },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(status)
                                        }
                                        DropdownMenu(expanded = expandedStatus, onDismissRequest = { expandedStatus = false }) {
                                            listOf("Active", "Waitlist", "Paused", "Canceled", "Wishlist").forEach { st ->
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

                                OutlinedTextField(
                                    value = priceStr,
                                    onValueChange = { priceStr = it },
                                    label = { Text("Price ($currency)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    leadingIcon = { Text(currency, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f).testTag("add_sub_price")
                                )
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                val showDueDateField = !status.equals("Wishlist", ignoreCase = true)
                                // Frequency select
                                var expandedFreq by remember { mutableStateOf(false) }
                                Column(modifier = if (showDueDateField) Modifier.weight(1f) else Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Frequency", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        OutlinedButton(
                                            onClick = { expandedFreq = true },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
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

                                if (showDueDateField) {
                                    // Due Date select
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Box(modifier = Modifier.fillMaxWidth().clickable { showDueDatePicker = true }) {
                                            OutlinedTextField(
                                                value = formatMonthDay(dueDate),
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text("Due Date", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                                modifier = Modifier.fillMaxWidth(),
                                                enabled = false,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (!status.equals("Wishlist", ignoreCase = true)) {
                            item {
                                GoogleCalendarStyleSubscriptionPeriodPicker(
                                    initialStartDate = startDate,
                                    initialEndDate = finishDate,
                                    onPeriodChanged = { start, end ->
                                        startDate = start
                                        finishDate = end
                                    }
                                )
                            }
                        }

                        item {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("Reminder Notification", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        }
                                    }
                                    Switch(
                                        checked = reminderEnabled,
                                        onCheckedChange = { reminderEnabled = it }
                                    )
                                }

                                if (reminderEnabled) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    InlineReminderSelector(
                                        reminderDDayOffset = reminderDDayOffset,
                                        onDDayOffsetChange = { reminderDDayOffset = it },
                                        reminderHour = reminderHour,
                                        onHourChange = { reminderHour = it },
                                        reminderMinute = reminderMinute,
                                        onMinuteChange = { reminderMinute = it }
                                    )
                                }
                            }
                        }
                    }
                } else if (selectedTab == 1) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Skip Type", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                var expandedSkipType by remember { mutableStateOf(false) }
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = { expandedSkipType = true },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(skipType)
                                    }
                                    DropdownMenu(expanded = expandedSkipType, onDismissRequest = { expandedSkipType = false }) {
                                        listOf("None", "Each calendar year", "Every certain months", "Unlimited").forEach { st ->
                                            DropdownMenuItem(
                                                text = { Text(st) },
                                                onClick = {
                                                    skipType = st
                                                    expandedSkipType = false
                                                }
                                            )
                                        }
                                    }
                                }
                                if (skipType == "Each calendar year" || skipType == "Every certain months") {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    if (skipType == "Every certain months") {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = numberOfMonthsStr,
                                                onValueChange = { numberOfMonthsStr = it.filter { ch -> ch.isDigit() } },
                                                label = { Text("Months", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true,
                                                modifier = Modifier.weight(1f).height(64.dp).testTag("add_sub_number_of_months")
                                            )
                                            OutlinedTextField(
                                                value = numberOfSkipsStr,
                                                onValueChange = { numberOfSkipsStr = it.filter { ch -> ch.isDigit() } },
                                                label = { Text("Num. Skips", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                                                placeholder = { Text("0") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true,
                                                trailingIcon = {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.padding(end = 4.dp)
                                                    ) {
                                                        IconButton(
                                                            onClick = {
                                                                val current = numberOfSkipsStr.toIntOrNull() ?: 0
                                                                if (current > 0) {
                                                                    numberOfSkipsStr = (current - 1).toString()
                                                                }
                                                            },
                                                            enabled = (numberOfSkipsStr.toIntOrNull() ?: 0) > 0,
                                                            modifier = Modifier.size(32.dp).testTag("add_sub_skips_decrement")
                                                        ) {
                                                            Icon(
                                                                Icons.Default.KeyboardArrowDown,
                                                                contentDescription = "Decrease skips",
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }
                                                        IconButton(
                                                            onClick = {
                                                                val current = numberOfSkipsStr.toIntOrNull() ?: 0
                                                                numberOfSkipsStr = (current + 1).toString()
                                                            },
                                                            modifier = Modifier.size(32.dp).testTag("add_sub_skips_increment")
                                                        ) {
                                                            Icon(
                                                                Icons.Default.KeyboardArrowUp,
                                                                contentDescription = "Increase skips",
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.weight(1f).height(64.dp).testTag("add_sub_number_of_skips")
                                            )
                                        }
                                    } else {
                                        OutlinedTextField(
                                            value = numberOfSkipsStr,
                                            onValueChange = { numberOfSkipsStr = it.filter { ch -> ch.isDigit() } },
                                            label = { Text("Num. Skips", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                                            placeholder = { Text("0") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            trailingIcon = {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(end = 4.dp)
                                                ) {
                                                    IconButton(
                                                        onClick = {
                                                            val current = numberOfSkipsStr.toIntOrNull() ?: 0
                                                            if (current > 0) {
                                                                numberOfSkipsStr = (current - 1).toString()
                                                            }
                                                        },
                                                        enabled = (numberOfSkipsStr.toIntOrNull() ?: 0) > 0,
                                                        modifier = Modifier.size(32.dp).testTag("add_sub_skips_decrement")
                                                    ) {
                                                        Icon(
                                                            Icons.Default.KeyboardArrowDown,
                                                            contentDescription = "Decrease skips",
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            val current = numberOfSkipsStr.toIntOrNull() ?: 0
                                                            numberOfSkipsStr = (current + 1).toString()
                                                        },
                                                        modifier = Modifier.size(32.dp).testTag("add_sub_skips_increment")
                                                    ) {
                                                        Icon(
                                                            Icons.Default.KeyboardArrowUp,
                                                            contentDescription = "Increase skips",
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth().height(64.dp).testTag("add_sub_number_of_skips")
                                        )
                                    }
                                }

                                if (skipType != "None") {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    SubscriptionSkipMethodsSection(
                                        skipMethods = skipMethodsList,
                                        bookstoreContacts = contactsForStore,
                                        subTitle = title,
                                        userName = userState?.username ?: "User",
                                        onSkipMethodsChanged = { skipMethodsList = it }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            OtherFormTabContent(
                                userAddresses = userAddresses,
                                forwardingServices = forwardingServices,
                                selectedShippingAddressId = selectedShippingAddressId,
                                onShippingAddressChange = { selectedShippingAddressId = it },
                                selectedCurrency = selectedCurrency,
                                onCurrencyChange = { selectedCurrency = it },
                                basePriceStr = basePriceStr,
                                onBasePriceChange = {
                                    basePriceStr = it
                                    if (priceStr.isBlank() || priceStr == "0" || priceStr == "0.0") {
                                        priceStr = it
                                    }
                                },
                                discountedAmountStr = discountedAmountStr,
                                onDiscountedAmountChange = { discountedAmountStr = it },
                                shippingPriceStr = shippingPriceStr,
                                onShippingPriceChange = { shippingPriceStr = it },
                                taxPriceStr = taxPriceStr,
                                onTaxPriceChange = { taxPriceStr = it },
                                forwardShippingPriceStr = forwardShippingPriceStr,
                                onForwardShippingPriceChange = { forwardShippingPriceStr = it },
                                forwardTaxPriceStr = forwardTaxPriceStr,
                                onForwardTaxPriceChange = { forwardTaxPriceStr = it }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedBase = basePriceStr.toDoubleOrNull()
                    val parsedPrice = priceStr.toDoubleOrNull() ?: (parsedBase ?: 0.0)
                    val selectedAddr = userAddresses.find { it.id == selectedShippingAddressId }
                    val hasForwarding = selectedAddr?.forwardingServiceId != null

                    if (title.isNotEmpty() && selectedBookstoreId > 0) {
                         viewModel.addSubscriptionType(
                             bookstoreId = selectedBookstoreId,
                             title = title,
                             status = status,
                             price = parsedPrice,
                             dueDate = dueDate,
                             startDate = startDate,
                             finishDate = finishDate,
                             notificationAlertDays = null,
                             frequency = frequency,
                             reminderEnabled = reminderEnabled,
                             reminderDDayOffset = reminderDDayOffset,
                             reminderHour = reminderHour,
                             reminderMinute = reminderMinute,
                             skipType = skipType,
                             numberOfSkips = numberOfSkipsStr.toIntOrNull(),
                             numberOfMonths = numberOfMonthsStr.toIntOrNull(),
                             picturePath = imageUrl.ifEmpty { null },
                             skipMethods = if (skipType != "None") skipMethodsList else emptyList(),
                             shippingAddressId = selectedShippingAddressId,
                             currency = selectedCurrency,
                             basePrice = parsedBase,
                             discountedAmount = discountedAmountStr.toDoubleOrNull(),
                             shippingPrice = shippingPriceStr.toDoubleOrNull(),
                            taxPrice = taxPriceStr.toDoubleOrNull(),
                            forwardShippingPrice = if (hasForwarding) forwardShippingPriceStr.toDoubleOrNull() else null,
                            forwardTaxPrice = if (hasForwarding) forwardTaxPriceStr.toDoubleOrNull() else null
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

    if (showDueDatePicker) {
        MonthDayPickerDialog(
            initialDateMillis = dueDate,
            onDateSelected = { dueDate = it },
            onDismiss = { showDueDatePicker = false }
        )
    }
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
    val userState by viewModel.userState.collectAsState()
    val currency = userState?.currency ?: "$"
    val userAddresses by viewModel.userAddressesState.collectAsState()
    val forwardingServices by viewModel.forwardingServicesState.collectAsState()
    var selectedShippingAddressId by remember { mutableStateOf<Int?>(sub.shippingAddressId) }
    var selectedCurrency by remember { mutableStateOf(sub.currency ?: (userState?.currency ?: "$")) }
    var basePriceStr by remember { mutableStateOf(sub.basePrice?.toString() ?: "") }
    var discountedAmountStr by remember { mutableStateOf(sub.discountedAmount?.toString() ?: "") }
    var shippingPriceStr by remember { mutableStateOf(sub.shippingPrice?.toString() ?: "") }
    var taxPriceStr by remember { mutableStateOf(sub.taxPrice?.toString() ?: "") }
    var forwardShippingPriceStr by remember { mutableStateOf(sub.forwardShippingPrice?.toString() ?: "") }
    var forwardTaxPriceStr by remember { mutableStateOf(sub.forwardTaxPrice?.toString() ?: "") }

    var selectedBookstoreId by remember { mutableStateOf(sub.bookstoreId) }
    var title by remember { mutableStateOf(sub.title) }
    var status by remember { mutableStateOf(sub.status) }
    var priceStr by remember { mutableStateOf(sub.price.toString()) }
    var frequency by remember { mutableStateOf(sub.frequency) }
    var reminderEnabled by remember { mutableStateOf(sub.reminderEnabled) }
    var reminderDDayOffset by remember { mutableStateOf(sub.reminderDDayOffset) }
    var reminderHour by remember { mutableStateOf(sub.reminderHour) }
    var reminderMinute by remember { mutableStateOf(sub.reminderMinute) }
    var skipType by remember { mutableStateOf(sub.skipType ?: "None") }
    var numberOfSkipsStr by remember { mutableStateOf(sub.numberOfSkips?.toString() ?: "") }
    var numberOfMonthsStr by remember { mutableStateOf(sub.numberOfMonths?.toString() ?: "") }

    val allSkipMethods = viewModel.allSubscriptionSkipMethodsState.collectAsState().value
    var skipMethodsList by remember(sub.id) {
        val existing = allSkipMethods.filter { it.subscriptionTypeId == sub.id }.sortedBy { it.skipMethodOrder }
        if (existing.isNotEmpty()) {
            mutableStateOf(existing)
        } else if (!sub.skipMethod.isNullOrBlank() && !sub.skipLink.isNullOrBlank()) {
            mutableStateOf(
                listOf(
                    SubscriptionSkipMethod(
                        subscriptionTypeId = sub.id,
                        skipMethodOrder = 1,
                        skipMethodType = sub.skipMethod ?: "Website",
                        skipMethodValue = sub.skipLink ?: "",
                        skipMethodText = sub.skipText ?: "",
                        consecutiveSkips = 0
                    )
                )
            )
        } else {
            mutableStateOf(emptyList<SubscriptionSkipMethod>())
        }
    }

    var imageUrl by remember { mutableStateOf(sub.picturePath ?: "") }
    var showDueDatePicker by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    var dueDate by remember { mutableStateOf(sub.dueDate) }
    var startDate by remember { mutableStateOf(sub.startDate) }
    var finishDate by remember { mutableStateOf(sub.finishDate) }

    val bookstoreContacts = viewModel.bookstoreContactsState.collectAsState().value
    val contactsForStore = remember(bookstoreContacts, selectedBookstoreId) {
        bookstoreContacts.filter { it.bookstoreId == selectedBookstoreId }
    }

    val userDateFormatPattern = viewModel.userState.collectAsState().value?.dateFormat ?: "yyyy-MM-dd"
    val dateFormat = remember(userDateFormatPattern) { SimpleDateFormat(userDateFormatPattern, Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Subscription", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                ScheduledSubscriptionImagePicker(
                    imageUrl = imageUrl,
                    onImageSelected = { imageUrl = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Details", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("edit_sub_tab_details")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Skips", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("edit_sub_tab_skips")
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Other", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("edit_sub_tab_other")
                    )
                }

                if (selectedTab == 0) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            // Bookstore select
                            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Bookstore", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                var expandedStore by remember { mutableStateOf(false) }
                                val currentStore = bookstores.find { it.id == selectedBookstoreId } ?: bookstores.firstOrNull()
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = { expandedStore = true },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
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
                        }

                        item {
                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                label = { Text("Subscription Title", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                modifier = Modifier.fillMaxWidth().testTag("edit_sub_title")
                            )
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                // Status select
                                var expandedStatus by remember { mutableStateOf(false) }
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Status", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        OutlinedButton(
                                            onClick = { expandedStatus = true },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(status)
                                        }
                                        DropdownMenu(expanded = expandedStatus, onDismissRequest = { expandedStatus = false }) {
                                            listOf("Waitlist", "Active", "Paused", "Canceled", "Wishlist").forEach { st ->
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

                                OutlinedTextField(
                                    value = priceStr,
                                    onValueChange = { priceStr = it },
                                    label = { Text("Price ($currency)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    leadingIcon = { Text(currency, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f).testTag("edit_sub_price")
                                )
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                val showDueDateField = !status.equals("Wishlist", ignoreCase = true)
                                // Frequency select
                                var expandedFreq by remember { mutableStateOf(false) }
                                Column(modifier = if (showDueDateField) Modifier.weight(1f) else Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Frequency", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        OutlinedButton(
                                            onClick = { expandedFreq = true },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
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

                                if (showDueDateField) {
                                    // Due Date select
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Box(modifier = Modifier.fillMaxWidth().clickable { showDueDatePicker = true }) {
                                            OutlinedTextField(
                                                value = formatMonthDay(dueDate),
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text("Due Date", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                                modifier = Modifier.fillMaxWidth(),
                                                enabled = false,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (!status.equals("Wishlist", ignoreCase = true)) {
                            item {
                                GoogleCalendarStyleSubscriptionPeriodPicker(
                                    initialStartDate = startDate,
                                    initialEndDate = finishDate,
                                    onPeriodChanged = { start, end ->
                                        startDate = start
                                        finishDate = end
                                    }
                                )
                            }
                        }

                        item {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("Reminder Notification", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        }
                                    }
                                    Switch(
                                        checked = reminderEnabled,
                                        onCheckedChange = { reminderEnabled = it }
                                    )
                                }

                                if (reminderEnabled) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    InlineReminderSelector(
                                        reminderDDayOffset = reminderDDayOffset,
                                        onDDayOffsetChange = { reminderDDayOffset = it },
                                        reminderHour = reminderHour,
                                        onHourChange = { reminderHour = it },
                                        reminderMinute = reminderMinute,
                                        onMinuteChange = { reminderMinute = it }
                                    )
                                }
                            }
                        }
                    }
                } else if (selectedTab == 1) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Skip Type", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                var expandedSkipType by remember { mutableStateOf(false) }
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = { expandedSkipType = true },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(skipType)
                                    }
                                    DropdownMenu(expanded = expandedSkipType, onDismissRequest = { expandedSkipType = false }) {
                                        listOf("None", "Each calendar year", "Every certain months", "Unlimited").forEach { st ->
                                            DropdownMenuItem(
                                                text = { Text(st) },
                                                onClick = {
                                                    skipType = st
                                                    expandedSkipType = false
                                                }
                                            )
                                        }
                                    }
                                }
                                if (skipType == "Each calendar year" || skipType == "Every certain months") {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    if (skipType == "Every certain months") {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = numberOfMonthsStr,
                                                onValueChange = { numberOfMonthsStr = it.filter { ch -> ch.isDigit() } },
                                                label = { Text("Months", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true,
                                                modifier = Modifier.weight(1f).height(64.dp).testTag("edit_sub_number_of_months")
                                            )
                                            OutlinedTextField(
                                                value = numberOfSkipsStr,
                                                onValueChange = { numberOfSkipsStr = it.filter { ch -> ch.isDigit() } },
                                                label = { Text("Num. Skips", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                                                placeholder = { Text("0") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true,
                                                trailingIcon = {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.padding(end = 4.dp)
                                                    ) {
                                                        IconButton(
                                                            onClick = {
                                                                val current = numberOfSkipsStr.toIntOrNull() ?: 0
                                                                if (current > 0) {
                                                                    numberOfSkipsStr = (current - 1).toString()
                                                                }
                                                            },
                                                            enabled = (numberOfSkipsStr.toIntOrNull() ?: 0) > 0,
                                                            modifier = Modifier.size(32.dp).testTag("edit_sub_skips_decrement")
                                                        ) {
                                                            Icon(
                                                                Icons.Default.KeyboardArrowDown,
                                                                contentDescription = "Decrease skips",
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }
                                                        IconButton(
                                                            onClick = {
                                                                val current = numberOfSkipsStr.toIntOrNull() ?: 0
                                                                numberOfSkipsStr = (current + 1).toString()
                                                            },
                                                            modifier = Modifier.size(32.dp).testTag("edit_sub_skips_increment")
                                                        ) {
                                                            Icon(
                                                                Icons.Default.KeyboardArrowUp,
                                                                contentDescription = "Increase skips",
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.weight(1f).height(64.dp).testTag("edit_sub_number_of_skips")
                                            )
                                        }
                                    } else {
                                        OutlinedTextField(
                                            value = numberOfSkipsStr,
                                            onValueChange = { numberOfSkipsStr = it.filter { ch -> ch.isDigit() } },
                                            label = { Text("Num. Skips", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                                            placeholder = { Text("0") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            trailingIcon = {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(end = 4.dp)
                                                ) {
                                                    IconButton(
                                                        onClick = {
                                                            val current = numberOfSkipsStr.toIntOrNull() ?: 0
                                                            if (current > 0) {
                                                                numberOfSkipsStr = (current - 1).toString()
                                                            }
                                                        },
                                                        enabled = (numberOfSkipsStr.toIntOrNull() ?: 0) > 0,
                                                        modifier = Modifier.size(32.dp).testTag("edit_sub_skips_decrement")
                                                    ) {
                                                        Icon(
                                                            Icons.Default.KeyboardArrowDown,
                                                            contentDescription = "Decrease skips",
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            val current = numberOfSkipsStr.toIntOrNull() ?: 0
                                                            numberOfSkipsStr = (current + 1).toString()
                                                        },
                                                        modifier = Modifier.size(32.dp).testTag("edit_sub_skips_increment")
                                                    ) {
                                                        Icon(
                                                            Icons.Default.KeyboardArrowUp,
                                                            contentDescription = "Increase skips",
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth().height(64.dp).testTag("edit_sub_number_of_skips")
                                        )
                                    }
                                }

                                if (skipType != "None") {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    SubscriptionSkipMethodsSection(
                                        skipMethods = skipMethodsList,
                                        bookstoreContacts = contactsForStore,
                                        subTitle = title,
                                        userName = userState?.username ?: "User",
                                        onSkipMethodsChanged = { skipMethodsList = it }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            OtherFormTabContent(
                                userAddresses = userAddresses,
                                forwardingServices = forwardingServices,
                                selectedShippingAddressId = selectedShippingAddressId,
                                onShippingAddressChange = { selectedShippingAddressId = it },
                                selectedCurrency = selectedCurrency,
                                onCurrencyChange = { selectedCurrency = it },
                                basePriceStr = basePriceStr,
                                onBasePriceChange = {
                                    basePriceStr = it
                                    if (priceStr.isBlank() || priceStr == "0" || priceStr == "0.0") {
                                        priceStr = it
                                    }
                                },
                                discountedAmountStr = discountedAmountStr,
                                onDiscountedAmountChange = { discountedAmountStr = it },
                                shippingPriceStr = shippingPriceStr,
                                onShippingPriceChange = { shippingPriceStr = it },
                                taxPriceStr = taxPriceStr,
                                onTaxPriceChange = { taxPriceStr = it },
                                forwardShippingPriceStr = forwardShippingPriceStr,
                                onForwardShippingPriceChange = { forwardShippingPriceStr = it },
                                forwardTaxPriceStr = forwardTaxPriceStr,
                                onForwardTaxPriceChange = { forwardTaxPriceStr = it }
                            )
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
                        val parsedBase = basePriceStr.toDoubleOrNull()
                        val parsedPrice = priceStr.toDoubleOrNull() ?: (parsedBase ?: sub.price)
                        val selectedAddr = userAddresses.find { it.id == selectedShippingAddressId }
                        val hasForwarding = selectedAddr?.forwardingServiceId != null

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
                                    notificationAlertDays = null,
                                    frequency = frequency,
                                    reminderEnabled = reminderEnabled,
                                    reminderDDayOffset = reminderDDayOffset,
                                    reminderHour = reminderHour,
                                    reminderMinute = reminderMinute,
                                    skipType = skipType,
                                    numberOfSkips = numberOfSkipsStr.toIntOrNull(),
                                    numberOfMonths = numberOfMonthsStr.toIntOrNull(),
                                    picturePath = imageUrl.ifEmpty { null },
                                    shippingAddressId = selectedShippingAddressId,
                                    currency = selectedCurrency,
                                    basePrice = parsedBase,
                                    discountedAmount = discountedAmountStr.toDoubleOrNull(),
                                    shippingPrice = shippingPriceStr.toDoubleOrNull(),
                                    taxPrice = taxPriceStr.toDoubleOrNull(),
                                    forwardShippingPrice = if (hasForwarding) forwardShippingPriceStr.toDoubleOrNull() else null,
                                    forwardTaxPrice = if (hasForwarding) forwardTaxPriceStr.toDoubleOrNull() else null
                                ),
                                skipMethods = if (skipType != "None") skipMethodsList else emptyList()
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

    if (showDueDatePicker) {
        MonthDayPickerDialog(
            initialDateMillis = dueDate,
            onDateSelected = { dueDate = it },
            onDismiss = { showDueDatePicker = false }
        )
    }
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
    val userAddresses by viewModel.userAddressesState.collectAsState()
    val currentSub = subscriptionsList.find { it.subscription.id == selectedSubId } ?: subscriptionsList.firstOrNull()
    val isForwardingAddress = remember(currentSub?.subscription?.shippingAddressId, userAddresses) {
        userAddresses.find { it.id == currentSub?.subscription?.shippingAddressId }?.forwardingServiceId != null
    }
    val availableStatuses = remember(isForwardingAddress) {
        if (isForwardingAddress) {
            listOf("Upcoming", "Skipped", "Renewed", "Forwarded", "In Suite", "Shipped", "Received")
        } else {
            listOf("Upcoming", "Skipped", "Renewed", "Shipped", "Received")
        }
    }
    LaunchedEffect(isForwardingAddress) {
        if (!isForwardingAddress && status in listOf("Forwarded", "In Suite")) {
            status = "Renewed"
        }
    }
    var imageUrl by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf(0.0) }

    val calendar = Calendar.getInstance()
    var dueDate by remember { mutableStateOf(calendar.timeInMillis) }
    var showDueDatePicker by remember { mutableStateOf(false) }

    val userDateFormatPattern = viewModel.userState.collectAsState().value?.dateFormat ?: "yyyy-MM-dd"
    val dateFormat = remember(userDateFormatPattern) { SimpleDateFormat(userDateFormatPattern, Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Scheduled Delivery", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    ScheduledSubscriptionImagePicker(
                        imageUrl = imageUrl,
                        onImageSelected = { imageUrl = it }
                    )
                }

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
                        label = { Text("Book Title", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = bookAuthor,
                        onValueChange = { bookAuthor = it },
                        label = { Text("Book Author", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description / Notes", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Rating", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        com.example.ui.components.StarRatingBar(
                            rating = rating,
                            onRatingChanged = { rating = it }
                        )
                    }
                }

                item {
                    var expandedStatus by remember { mutableStateOf(false) }
                    Column {
                        Text("Status", fontSize = 12.sp)
                        OutlinedButton(onClick = { expandedStatus = true }) {
                            Text(status)
                        }
                        DropdownMenu(expanded = expandedStatus, onDismissRequest = { expandedStatus = false }) {
                            availableStatuses.forEach { st ->
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
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.fillMaxWidth().clickable { showDueDatePicker = true }) {
                            OutlinedTextField(
                                value = formatMonthDay(dueDate),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Due Date", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = false,
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
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
                            status = status,
                            picturePath = imageUrl.ifEmpty { null },
                            rating = rating
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

    if (showDueDatePicker) {
        MonthDayPickerDialog(
            initialDateMillis = dueDate,
            onDateSelected = { dueDate = it },
            onDismiss = { showDueDatePicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScheduledSubscriptionDialog(
    scheduledWithDetails: ScheduledWithDetails,
    viewModel: BookishViewModel,
    onDismiss: () -> Unit,
    onPromptSkip: ((ScheduledWithDetails) -> Unit)? = null
) {
    val sc = scheduledWithDetails.scheduled
    val subType = scheduledWithDetails.subscriptionType
    val context = LocalContext.current

    var bookTitle by remember { mutableStateOf(sc.bookTitle) }
    var bookAuthor by remember { mutableStateOf(sc.bookAuthor) }
    var description by remember { mutableStateOf(sc.description) }
    var status by remember { mutableStateOf(sc.status) }
    val userAddresses by viewModel.userAddressesState.collectAsState()
    val isForwardingAddress = remember(subType?.shippingAddressId, userAddresses) {
        userAddresses.find { it.id == subType?.shippingAddressId }?.forwardingServiceId != null
    }
    val availableStatuses = remember(isForwardingAddress) {
        if (isForwardingAddress) {
            listOf("Upcoming", "Skipped", "Renewed", "Forwarded", "In Suite", "Shipped", "Received")
        } else {
            listOf("Upcoming", "Skipped", "Renewed", "Shipped", "Received")
        }
    }
    LaunchedEffect(isForwardingAddress) {
        if (!isForwardingAddress && status in listOf("Forwarded", "In Suite")) {
            status = "Renewed"
        }
    }
    var imageUrl by remember { mutableStateOf(sc.picturePath ?: "") }
    var rating by remember { mutableStateOf(sc.rating) }

    var dueDate by remember { mutableStateOf(sc.dueDate) }
    var showDueDatePicker by remember { mutableStateOf(false) }
    var showPackageDialog by remember { mutableStateOf(false) }

    val userDateFormatPattern = viewModel.userState.collectAsState().value?.dateFormat ?: "yyyy-MM-dd"
    val dateFormat = remember(userDateFormatPattern) { SimpleDateFormat(userDateFormatPattern, Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Edit Scheduled Subscription",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                PackageTrackingQuickButton(
                    originTable = "scheduled_subs",
                    originId = sc.id,
                    viewModel = viewModel
                )
                IconButton(
                    onClick = { showPackageDialog = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_package_2),
                        contentDescription = "Package Details",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    ScheduledSubscriptionImagePicker(
                        imageUrl = imageUrl,
                        onImageSelected = { imageUrl = it }
                    )
                }

                item {
                    val subTypeName = subType?.title ?: "N/A"
                    OutlinedTextField(
                        value = subTypeName,
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text("Subscription Type", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        modifier = Modifier.fillMaxWidth().testTag("edit_scheduled_sub_type"),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Status select
                        var expandedStatus by remember { mutableStateOf(false) }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Status", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { expandedStatus = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(status)
                                }
                                DropdownMenu(expanded = expandedStatus, onDismissRequest = { expandedStatus = false }) {
                                    availableStatuses.forEach { st ->
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

                        // Due Date select
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.fillMaxWidth().clickable { showDueDatePicker = true }) {
                                OutlinedTextField(
                                    value = formatMonthDay(dueDate),
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Due Date", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = false,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }



                item {
                    OutlinedTextField(
                        value = bookTitle,
                        onValueChange = { bookTitle = it },
                        label = { Text("Book Title", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = bookAuthor,
                        onValueChange = { bookAuthor = it },
                        label = { Text("Book Author", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Rating", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        com.example.ui.components.StarRatingBar(
                            rating = rating,
                            onRatingChanged = { rating = it }
                        )
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
                        val wasSkipped = sc.status.equals("Skipped", ignoreCase = true) || sc.isSkipped
                        val isNowSkipped = status.equals("Skipped", ignoreCase = true)
                        val updatedScheduled = sc.copy(
                            bookTitle = bookTitle,
                            bookAuthor = bookAuthor,
                            description = description,
                            status = status,
                            isSkipped = isNowSkipped,
                            dueDate = dueDate,
                            picturePath = imageUrl.ifEmpty { null },
                            rating = rating
                        )

                        viewModel.updateScheduledSubscription(updatedScheduled)

                        if (!wasSkipped && isNowSkipped) {
                            onPromptSkip?.invoke(
                                scheduledWithDetails.copy(scheduled = updatedScheduled)
                            )
                        }
                        onDismiss()
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

    if (showDueDatePicker) {
        MonthDayPickerDialog(
            initialDateMillis = dueDate,
            onDateSelected = { dueDate = it },
            onDismiss = { showDueDatePicker = false }
        )
    }

    if (showPackageDialog) {
        PackageDetailsDialog(
            originTable = "scheduled_subs",
            originId = sc.id,
            viewModel = viewModel,
            hasForwardingAddress = isForwardingAddress,
            onDismiss = { showPackageDialog = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubscriptionFilterDialog(
    viewModel: BookishViewModel,
    bookstores: List<Bookstore>,
    subscriptionTypes: List<SubscriptionType>,
    selectedTab: Int,
    onDismiss: () -> Unit
) {
    if (selectedTab == 0) {
        // FILTER 1: Overview / Renewals Tab Filters
        val initialStoreIds by viewModel.subOverviewFilterBookstores.collectAsState()
        val initialTypeIds by viewModel.subOverviewFilterSubTypeIds.collectAsState()
        val initialStatuses by viewModel.subOverviewFilterTypes.collectAsState()
        val initialAuthor by viewModel.subOverviewFilterAuthor.collectAsState()
        val initialBook by viewModel.subOverviewFilterBook.collectAsState()

        val userAddresses by viewModel.userAddressesState.collectAsState()
        val hasAnyForwardingAddress = remember(userAddresses) {
            userAddresses.any { it.forwardingServiceId != null }
        }
        val statusFilterItems = remember(hasAnyForwardingAddress) {
            if (hasAnyForwardingAddress) {
                listOf("Upcoming", "Skipped", "Renewed", "Forwarded", "In Suite", "Shipped", "Received")
            } else {
                listOf("Upcoming", "Skipped", "Renewed", "Shipped", "Received")
            }
        }

        var selectedStoreIds by remember(initialStoreIds) { mutableStateOf(initialStoreIds) }
        var selectedTypeIds by remember(initialTypeIds) { mutableStateOf(initialTypeIds) }
        var selectedStatuses by remember(initialStatuses) { mutableStateOf(initialStatuses) }
        var authorText by remember(initialAuthor) { mutableStateOf(initialAuthor ?: "") }
        var bookText by remember(initialBook) { mutableStateOf(initialBook ?: "") }

        AlertDialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            title = { Text("Filter Renewals", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MultiSelectChipGroup(
                        title = "Status",
                        items = statusFilterItems,
                        selectedItems = selectedStatuses,
                        onSelectionChanged = { selectedStatuses = it },
                        labelProvider = { it }
                    )

                    if (bookstores.isNotEmpty()) {
                        MultiSelectChipGroup(
                            title = "Bookstore",
                            items = bookstores.map { it.id },
                            selectedItems = selectedStoreIds,
                            onSelectionChanged = { selectedStoreIds = it },
                            labelProvider = { id -> bookstores.find { it.id == id }?.name ?: "" }
                        )
                    }

                    if (subscriptionTypes.isNotEmpty()) {
                        MultiSelectChipGroup(
                            title = "Subscription Type",
                            items = subscriptionTypes.map { it.id },
                            selectedItems = selectedTypeIds,
                            onSelectionChanged = { selectedTypeIds = it },
                            labelProvider = { id -> subscriptionTypes.find { it.id == id }?.title ?: "" }
                        )
                    }

                    OutlinedTextField(
                        value = authorText,
                        onValueChange = { authorText = it },
                        label = { Text("Filter by Author Name", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = bookText,
                        onValueChange = { bookText = it },
                        label = { Text("Filter by Book Title", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.subOverviewFilterBookstores.value = selectedStoreIds
                        viewModel.subOverviewFilterSubTypeIds.value = selectedTypeIds
                        viewModel.subOverviewFilterTypes.value = selectedStatuses
                        viewModel.subOverviewFilterAuthor.value = if (authorText.isBlank()) null else authorText
                        viewModel.subOverviewFilterBook.value = if (bookText.isBlank()) null else bookText
                        onDismiss()
                    }
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.subOverviewFilterBookstores.value = emptySet()
                        viewModel.subOverviewFilterSubTypeIds.value = emptySet()
                        viewModel.subOverviewFilterTypes.value = emptySet()
                        viewModel.subOverviewFilterAuthor.value = null
                        viewModel.subOverviewFilterBook.value = null
                        onDismiss()
                    }
                ) {
                    Text("Clear All")
                }
            }
        )
    } else {
        // FILTER 2: Subscriptions Main Tab Filters
        val initialStoreIds by viewModel.subFilterBookstores.collectAsState()
        val initialStatuses by viewModel.subFilterStatuses.collectAsState()
        val initialFrequencies by viewModel.subFilterFrequencies.collectAsState()
        val initialTypeIds by viewModel.subFilterSubTypeIds.collectAsState()

        var selectedStoreIds by remember(initialStoreIds) { mutableStateOf(initialStoreIds) }
        var selectedStatuses by remember(initialStatuses) { mutableStateOf(initialStatuses) }
        var selectedFrequencies by remember(initialFrequencies) { mutableStateOf(initialFrequencies) }
        var selectedTypeIds by remember(initialTypeIds) { mutableStateOf(initialTypeIds) }

        AlertDialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            title = { Text("Filter Subscriptions", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MultiSelectChipGroup(
                        title = "Status",
                        items = listOf("Waitlist", "Active", "Paused", "Canceled", "Wishlist"),
                        selectedItems = selectedStatuses,
                        onSelectionChanged = { selectedStatuses = it },
                        labelProvider = { it }
                    )

                    if (bookstores.isNotEmpty()) {
                        MultiSelectChipGroup(
                            title = "Bookstore",
                            items = bookstores.map { it.id },
                            selectedItems = selectedStoreIds,
                            onSelectionChanged = { selectedStoreIds = it },
                            labelProvider = { id -> bookstores.find { it.id == id }?.name ?: "" }
                        )
                    }

                    if (subscriptionTypes.isNotEmpty()) {
                        MultiSelectChipGroup(
                            title = "Subscription Type",
                            items = subscriptionTypes.map { it.id },
                            selectedItems = selectedTypeIds,
                            onSelectionChanged = { selectedTypeIds = it },
                            labelProvider = { id -> subscriptionTypes.find { it.id == id }?.title ?: "" }
                        )
                    }

                    MultiSelectChipGroup(
                        title = "Frequency",
                        items = listOf("Weekly", "Monthly", "Bi-Monthly", "Quarterly", "Yearly"),
                        selectedItems = selectedFrequencies,
                        onSelectionChanged = { selectedFrequencies = it },
                        labelProvider = { it }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.subFilterBookstores.value = selectedStoreIds
                        viewModel.subFilterStatuses.value = selectedStatuses
                        viewModel.subFilterFrequencies.value = selectedFrequencies
                        viewModel.subFilterSubTypeIds.value = selectedTypeIds
                        onDismiss()
                    }
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.subFilterBookstores.value = emptySet()
                        viewModel.subFilterStatuses.value = emptySet()
                        viewModel.subFilterFrequencies.value = emptySet()
                        viewModel.subFilterSubTypeIds.value = emptySet()
                        onDismiss()
                    }
                ) {
                    Text("Clear All")
                }
            }
        )
    }
}

@Composable
fun ScheduledSubscriptionImagePicker(
    imageUrl: String,
    onImageSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = saveImageToInternalStorage(context, uri) ?: uri.toString()
            onImageSelected(savedPath)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    shape = CircleShape
                )
                .clickable { photoPickerLauncher.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            if (imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Selected Subscription Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.AddAPhoto,
                    contentDescription = "Default Subscription Icon",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        // Pencil Edit Button
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = 35.dp, y = 35.dp)
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                .clickable { photoPickerLauncher.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit Image",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun <T> RevolverWheelPicker(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    isLooped: Boolean = true,
    label: (T) -> String = { it.toString() }
) {
    val itemHeight = 36.dp
    val visibleItemsCount = 3
    val coroutineScope = rememberCoroutineScope()

    if (items.isEmpty()) return

    val totalCount = if (isLooped) items.size * 10000 else items.size

    val initialSelIndex = items.indexOf(selectedItem).coerceAtLeast(0)
    val initialListIndex = if (isLooped) {
        val middle = (totalCount / 2)
        middle - (middle % items.size) + initialSelIndex
    } else {
        initialSelIndex
    }

    val listState = androidx.compose.foundation.lazy.rememberLazyListState(
        initialFirstVisibleItemIndex = initialListIndex
    )
    val flingBehavior = rememberSnapFlingBehavior(
        lazyListState = listState,
        snapPosition = SnapPosition.Center
    )

    // Sync state when selectedItem changes externally
    LaunchedEffect(selectedItem) {
        val targetSelIdx = items.indexOf(selectedItem).coerceAtLeast(0)
        val currentListIdx = listState.firstVisibleItemIndex
        val currentSelIdx = currentListIdx % items.size
        if (currentSelIdx != targetSelIdx) {
            val diff = targetSelIdx - currentSelIdx
            listState.scrollToItem(currentListIdx + diff)
        }
    }

    // Capture scrolling finish & snap to center item
    val isScrollInProgress = listState.isScrollInProgress
    LaunchedEffect(isScrollInProgress) {
        if (!isScrollInProgress) {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isNotEmpty()) {
                val viewportMiddle = layoutInfo.viewportStartOffset + (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset) / 2
                val closestItem = visibleItems.minByOrNull {
                    val itemMiddle = it.offset + it.size / 2
                    kotlin.math.abs(itemMiddle - viewportMiddle)
                }
                if (closestItem != null) {
                    val actualIndex = closestItem.index % items.size
                    onItemSelected(items[actualIndex])
                }
            }
        }
    }

    Box(
        modifier = modifier
            .height(itemHeight * visibleItemsCount)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        // Selection overlay (highlight center)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(4.dp)
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(4.dp)
                )
        )

        androidx.compose.foundation.lazy.LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(vertical = itemHeight),
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(totalCount) { index ->
                val actualIndex = index % items.size
                val item = items[actualIndex]
                val isSelected = item == selectedItem
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .clickable {
                            coroutineScope.launch {
                                listState.animateScrollToItem(index)
                                onItemSelected(item)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label(item),
                        style = if (isSelected) {
                            MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 13.sp
                            )
                        } else {
                            MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontSize = 11.sp
                            )
                        }
                    )
                }
            }
        }
    }
}

private fun localTimeToUtcMidnight(localTimeMs: Long): Long {
    val localCal = Calendar.getInstance().apply {
        timeInMillis = localTimeMs
    }
    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(Calendar.YEAR, localCal.get(Calendar.YEAR))
        set(Calendar.MONTH, localCal.get(Calendar.MONTH))
        set(Calendar.DAY_OF_MONTH, localCal.get(Calendar.DAY_OF_MONTH))
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return utcCal.timeInMillis
}

private fun utcMidnightToLocalStartOfDay(timeMs: Long): Long {
    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = timeMs
    }
    val localCal = Calendar.getInstance().apply {
        set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
        set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
        set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return localCal.timeInMillis
}

private fun getOrdinalSuffix(day: Int): String {
    if (day in 11..13) {
        return "th"
    }
    return when (day % 10) {
        1 -> "st"
        2 -> "nd"
        3 -> "rd"
        else -> "th"
    }
}

private fun formatDateToWordsWithOrdinal(timeMs: Long): String {
    val cal = Calendar.getInstance().apply {
        timeInMillis = timeMs
    }
    val day = cal.get(Calendar.DAY_OF_MONTH)
    val suffix = getOrdinalSuffix(day)
    val sdf = SimpleDateFormat("EEEE, MMMM", Locale.getDefault())
    val formattedBase = sdf.format(Date(timeMs))
    return "$formattedBase $day$suffix"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoogleCalendarStyleSubscriptionPeriodPicker(
    initialStartDate: Long,
    initialEndDate: Long,
    onPeriodChanged: (Long, Long) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    var isExpanded by remember { mutableStateOf(false) }

    var startDateLong by remember { mutableStateOf(initialStartDate) }
    var endDateLong by remember { mutableStateOf(if (initialEndDate > 0L) initialEndDate else initialStartDate) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val areDatesSame = startDateLong == endDateLong || initialEndDate == 0L

    LaunchedEffect(startDateLong, endDateLong, areDatesSame) {
        val finalStart = startDateLong
        val finalEnd = if (areDatesSame) 0L else endDateLong
        onPeriodChanged(finalStart, finalEnd)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Subscription Period", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)

        OutlinedCard(
            onClick = { isExpanded = true },
            modifier = Modifier.fillMaxWidth().testTag("subscription_period_card")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    val startText = formatDateToWordsWithOrdinal(startDateLong)
                    val endText = formatDateToWordsWithOrdinal(endDateLong)
                    val displayText = if (areDatesSame) startText else "$startText - $endText"
                    Text(displayText, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    if (isExpanded) {
        AlertDialog(
            onDismissRequest = { isExpanded = false },
            title = {
                Text(
                    text = "Configure Subscription Period",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (areDatesSame) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Start Date", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedCard(
                                    onClick = { showStartDatePicker = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = dateFormat.format(Date(startDateLong)),
                                        modifier = Modifier.padding(10.dp),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(
                                onClick = { showEndDatePicker = true },
                                modifier = Modifier.align(Alignment.Bottom)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Set End Date", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Start Date", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedCard(
                                    onClick = { showStartDatePicker = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = dateFormat.format(Date(startDateLong)),
                                        modifier = Modifier.padding(10.dp),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("End Date", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    IconButton(
                                        onClick = {
                                            endDateLong = startDateLong
                                        },
                                        modifier = Modifier.size(16.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove end date",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedCard(
                                    onClick = { showEndDatePicker = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = dateFormat.format(Date(endDateLong)),
                                        modifier = Modifier.padding(10.dp),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { isExpanded = false }
                ) {
                    Text("Done")
                }
            }
        )
    }

    if (showStartDatePicker) {
        ComposeDatePickerDialog(
            initialDateMillis = startDateLong,
            onDateSelected = { selected: Long ->
                startDateLong = selected
                if (endDateLong < selected) {
                    endDateLong = selected
                }
            },
            onDismiss = { showStartDatePicker = false }
        )
    }

    if (showEndDatePicker) {
        ComposeDatePickerDialog(
            initialDateMillis = endDateLong,
            onDateSelected = { selected: Long ->
                if (selected >= startDateLong) {
                    endDateLong = selected
                }
            },
            onDismiss = { showEndDatePicker = false }
        )
    }
}

fun formatMonthDay(timeMs: Long): String {
    val sdf = SimpleDateFormat("MMMM d", Locale.getDefault())
    return sdf.format(Date(timeMs))
}

@Composable
fun MonthDayPickerDialog(
    initialDateMillis: Long,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val initialCal = remember(initialDateMillis) {
        Calendar.getInstance().apply { timeInMillis = initialDateMillis }
    }
    var selectedMonth by remember { mutableStateOf(initialCal.get(Calendar.MONTH)) } // 0..11
    var selectedDay by remember { mutableStateOf(initialCal.get(Calendar.DAY_OF_MONTH)) } // 1..31

    val months = remember {
        val monthFormat = SimpleDateFormat("MMMM", Locale.getDefault())
        val cal = Calendar.getInstance()
        (0..11).map { m ->
            cal.set(Calendar.MONTH, m)
            monthFormat.format(cal.time)
        }
    }

    val maxDaysInSelectedMonth = remember(selectedMonth) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.MONTH, selectedMonth)
        cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    // Keep selectedDay valid when month changes
    LaunchedEffect(maxDaysInSelectedMonth) {
        if (selectedDay > maxDaysInSelectedMonth) {
            selectedDay = maxDaysInSelectedMonth
        }
    }

    val days = (1..maxDaysInSelectedMonth).toList()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select Due Date",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header preview showing formatted month and day
                val calPreview = Calendar.getInstance().apply {
                    set(Calendar.MONTH, selectedMonth)
                    set(Calendar.DAY_OF_MONTH, selectedDay.coerceAtMost(maxDaysInSelectedMonth))
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Text(
                        text = SimpleDateFormat("MMMM d", Locale.getDefault()).format(calPreview.time),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Month Picker
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1.4f)
                    ) {
                        Text(
                            text = "Month",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        RevolverWheelPicker(
                            items = (0..11).toList(),
                            selectedItem = selectedMonth,
                            onItemSelected = { selectedMonth = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { months[it] }
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Day Picker
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(0.9f)
                    ) {
                        Text(
                            text = "Day",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        RevolverWheelPicker(
                            items = days,
                            selectedItem = selectedDay.coerceAtMost(maxDaysInSelectedMonth),
                            onItemSelected = { selectedDay = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { it.toString() }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cal = Calendar.getInstance().apply {
                        timeInMillis = initialDateMillis
                        set(Calendar.MONTH, selectedMonth)
                        val safeDay = selectedDay.coerceAtMost(getActualMaximum(Calendar.DAY_OF_MONTH))
                        set(Calendar.DAY_OF_MONTH, safeDay)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    onDateSelected(cal.timeInMillis)
                    onDismiss()
                }
            ) {
                Text("Select")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeDatePickerDialog(
    initialDateMillis: Long,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val initialUtc = localTimeToUtcMidnight(initialDateMillis)
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialUtc)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    datePickerState.selectedDateMillis?.let { selectedUtc ->
                        onDateSelected(utcMidnightToLocalStartOfDay(selectedUtc))
                    }
                    onDismiss()
                }
            ) {
                Text("Select")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComposeDateRangePickerDialog(
    initialStartDateMillis: Long,
    initialEndDateMillis: Long?,
    onDatesSelected: (Long, Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val initialStartUtc = localTimeToUtcMidnight(initialStartDateMillis)
    val initialEndUtc = initialEndDateMillis?.let { localTimeToUtcMidnight(it) }
    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialStartUtc,
        initialSelectedEndDateMillis = initialEndUtc
    )
    val formatter = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val startUtc = dateRangePickerState.selectedStartDateMillis
                    val endUtc = dateRangePickerState.selectedEndDateMillis
                    val startLocal = startUtc?.let { utcMidnightToLocalStartOfDay(it) }
                    val endLocal = endUtc?.let { utcMidnightToLocalStartOfDay(it) }
                    if (startLocal != null) {
                        onDatesSelected(startLocal, endLocal)
                    }
                    onDismiss()
                }
            ) {
                Text("Select")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .padding(16.dp)
            .widthIn(max = 500.dp)
    ) {
        DateRangePicker(
            state = dateRangePickerState,
            modifier = Modifier.weight(1f, fill = false),
            title = {
                Text(
                    text = "Select period",
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            headline = {
                val startText = dateRangePickerState.selectedStartDateMillis?.let { formatter.format(Date(it)) } ?: "Start"
                val endText = dateRangePickerState.selectedEndDateMillis?.let { formatter.format(Date(it)) } ?: "End"
                Text(
                    text = "$startText — $endText",
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 12.dp),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            },
            showModeToggle = true
        )
    }
}

private fun formatRelativeDate(timeMs: Long, isHeader: Boolean): String {
    val targetCal = Calendar.getInstance().apply { timeInMillis = timeMs }
    val todayCal = Calendar.getInstance()
    
    val isToday = targetCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                  targetCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)
                  
    val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val isYesterday = targetCal.get(Calendar.YEAR) == yesterdayCal.get(Calendar.YEAR) &&
                      targetCal.get(Calendar.DAY_OF_YEAR) == yesterdayCal.get(Calendar.DAY_OF_YEAR)
                      
    val tomorrowCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
    val isTomorrow = targetCal.get(Calendar.YEAR) == tomorrowCal.get(Calendar.YEAR) &&
                     targetCal.get(Calendar.DAY_OF_YEAR) == tomorrowCal.get(Calendar.DAY_OF_YEAR)
                     
    if (isToday) return "Today"
    if (isYesterday) return "Yesterday"
    if (isTomorrow) return "Tomorrow"
    
    val showYear = targetCal.get(Calendar.YEAR) != todayCal.get(Calendar.YEAR)
    
    val pattern = if (isHeader) {
        if (showYear) "EEEE, MMMM dd, yyyy" else "EEEE, MMMM dd"
    } else {
        if (showYear) "yyyy-MM-dd" else "MM-dd"
    }
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(timeMs))
}

