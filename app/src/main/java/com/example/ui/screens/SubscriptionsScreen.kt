package com.example.ui.screens
 
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
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
    val coroutineScope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()

    // Observe flows
    val userState by viewModel.userState.collectAsState()
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
                        onClick = { selectedTab = 0 },
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
                        onClick = { selectedTab = 1 },
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
                val timeframes = listOf("7 Days", "This Month", "Next Month", "Yearly")
                val subOverviewPagerState = rememberPagerState(initialPage = 0, pageCount = { timeframes.size })
                var showCosts by remember(userState?.displayAmounts) { mutableStateOf(userState?.displayAmounts ?: false) }
                val cardContrastColor = if (!isDark) Color(0xFFFF5722) else MaterialTheme.colorScheme.primary
                val cardContrastBg = if (!isDark) Color(0xFFFF5722).copy(alpha = 0.05f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                val cardBorder = if (!isDark) BorderStroke(1.dp, Color(0xFFFF5722).copy(alpha = 0.15f)) else null

                Spacer(modifier = Modifier.height(12.dp))

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
                        val currentLabel = timeframes[subOverviewPagerState.currentPage]

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
                            val (startTime, endTime) = when (page) {
                                0 -> {
                                    val start = todayStart
                                    val end = java.util.Calendar.getInstance().apply {
                                        timeInMillis = todayStart
                                        add(java.util.Calendar.DAY_OF_YEAR, 7)
                                        set(java.util.Calendar.HOUR_OF_DAY, 23)
                                        set(java.util.Calendar.MINUTE, 59)
                                        set(java.util.Calendar.SECOND, 59)
                                        set(java.util.Calendar.MILLISECOND, 999)
                                    }.timeInMillis
                                    Pair(start, end)
                                }
                                1 -> {
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
                                2 -> {
                                    val start = java.util.Calendar.getInstance().apply {
                                        set(java.util.Calendar.DAY_OF_MONTH, 1)
                                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                                        set(java.util.Calendar.MINUTE, 0)
                                        set(java.util.Calendar.SECOND, 0)
                                        set(java.util.Calendar.MILLISECOND, 0)
                                        add(java.util.Calendar.MONTH, 1)
                                    }.timeInMillis
                                    val end = java.util.Calendar.getInstance().apply {
                                        timeInMillis = start
                                        add(java.util.Calendar.MONTH, 1)
                                        add(java.util.Calendar.MILLISECOND, -1)
                                    }.timeInMillis
                                    Pair(start, end)
                                }
                                else -> {
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

                            val upcomingScheduled = scheduledList.filter {
                                it.scheduled.status.equals("Upcoming", ignoreCase = true) && it.scheduled.dueDate in startTime..endTime
                            }
                            val skippedScheduled = scheduledList.filter {
                                it.scheduled.status.equals("Skipped", ignoreCase = true) && it.scheduled.dueDate in startTime..endTime
                            }
                            val renewedScheduled = scheduledList.filter {
                                (it.scheduled.status.equals("Renewed", ignoreCase = true) || it.scheduled.status.equals("Paid", ignoreCase = true)) && it.scheduled.dueDate in startTime..endTime
                            }
                            val shippedScheduled = scheduledList.filter {
                                (it.scheduled.status.equals("Shipped", ignoreCase = true) || it.scheduled.status.equals("Received", ignoreCase = true)) && it.scheduled.dueDate in startTime..endTime
                            }

                            val dueCost = upcomingScheduled.sumOf { it.subscriptionType?.price ?: 0.0 }
                            val totalSpentScheduled = scheduledList.filter {
                                it.scheduled.dueDate in startTime..endTime &&
                                !it.scheduled.status.equals("Upcoming", ignoreCase = true) &&
                                !it.scheduled.status.equals("Skipped", ignoreCase = true)
                            }
                            val totalSpent = totalSpentScheduled.sumOf { it.subscriptionType?.price ?: 0.0 }
                            val currency = userState?.currency ?: "$"

                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Upcoming", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            "${upcomingScheduled.size}",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                    Divider(
                                        modifier = Modifier
                                            .height(32.dp)
                                            .width(1.dp),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                    )
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Skipped", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            "${skippedScheduled.size}",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFEF4444)
                                        )
                                    }
                                    Divider(
                                        modifier = Modifier
                                            .height(32.dp)
                                            .width(1.dp),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                    )
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Renewed", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            "${renewedScheduled.size}",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2563EB)
                                        )
                                    }
                                    Divider(
                                        modifier = Modifier
                                            .height(32.dp)
                                            .width(1.dp),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                    )
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Shipped", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            "${shippedScheduled.size}",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF8B5CF6)
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
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Due Cost", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                "${currency}${String.format("%.2f", dueCost)}",
                                                fontSize = 17.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (!isDark) Color(0xFFE64A19) else MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                        if (page == 1 || page == 3) {
                                            Divider(
                                                modifier = Modifier
                                                    .height(28.dp)
                                                    .width(1.dp),
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                            )
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
                                        color = when (item.scheduled.status.lowercase()) {
                                            "skipped" -> Color(0xFFEF4444)
                                            "upcoming" -> Color(0xFF64748B)
                                            "renewed", "paid" -> Color(0xFF2563EB)
                                            "shipped" -> Color(0xFF8B5CF6)
                                            "received" -> Color(0xFF10B981)
                                            else -> Color(0xFF2563EB)
                                        }
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
                                items(
                                    items = focusedDayItems,
                                    key = { it.scheduled.id }
                                ) { item ->
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
                        scheduledList.sortedBy { it.scheduled.dueDate }
                    }
                    val groupedAll = remember(allSorted, currentYear) {
                        allSorted.groupBy { formatSubGroupHeader(it.scheduled.dueDate) }
                    }

                    // Three tab controls
                    val listTabContrastColor = if (!isDark) Color(0xFFFF5722) else MaterialTheme.colorScheme.primary
                    TabRow(
                        selectedTabIndex = subListViewTab,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
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
                                        ScheduledItemRow(item, viewModel)
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
                                        ScheduledItemRow(item, viewModel)
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
                                        ScheduledItemRow(item, viewModel)
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
                        val sortedSubs = remember(subscriptionsList) {
                            subscriptionsList.sortedBy { it.subscription.dueDate }
                        }
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(
                                items = sortedSubs,
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
                selectedTab = selectedTab,
                onDismiss = { showFilterDialog = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduledItemRow(item: ScheduledWithDetails, viewModel: BookishViewModel) {
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
    var showEditDialog by remember { mutableStateOf(false) }

    val accentColor = when (item.scheduled.status.lowercase()) {
        "upcoming" -> Color(0xFF64748B) // Grey
        "renewed", "paid" -> Color(0xFF2563EB) // Blue
        "skipped" -> Color(0xFFEF4444) // Red
        "shipped" -> Color(0xFF8B5CF6) // Purple
        "received" -> Color(0xFF10B981) // Green
        else -> Color(0xFF64748B)
    }

    val isUpcoming = item.scheduled.status.equals("Upcoming", ignoreCase = true)
    val isSkippedStatus = item.scheduled.status.equals("Skipped", ignoreCase = true)
    val isDueDateToCome = item.scheduled.dueDate >= (System.currentTimeMillis() - 86400000L)
    val canSwipeToSkipOrUnskip = isUpcoming || (isSkippedStatus && isDueDateToCome)
    val isRenewed = item.scheduled.status.equals("Renewed", ignoreCase = true) || item.scheduled.status.equals("Paid", ignoreCase = true)
    val isShipped = item.scheduled.status.equals("Shipped", ignoreCase = true)
    val canSwipeToStatusChange = isUpcoming || isRenewed || isShipped || isSkippedStatus

    key(item.scheduled.id, item.scheduled.status) {
        val dismissState = rememberSwipeToDismissBoxState(
            confirmValueChange = { dismissValue ->
                when (dismissValue) {
                    SwipeToDismissBoxValue.StartToEnd -> {
                        if (canSwipeToStatusChange) {
                            val newStatus = when {
                                isSkippedStatus -> "Upcoming"
                                isUpcoming -> "Renewed"
                                isRenewed -> "Shipped"
                                isShipped -> "Received"
                                else -> item.scheduled.status
                            }
                            val isSkippedNew = if (isSkippedStatus) false else item.scheduled.isSkipped
                            viewModel.updateScheduledSubscription(
                                item.scheduled.copy(status = newStatus, isSkipped = isSkippedNew)
                            )
                        }
                        false
                    }
                    SwipeToDismissBoxValue.EndToStart -> {
                        if (canSwipeToSkipOrUnskip) {
                            viewModel.toggleSkipScheduledSubscription(item.scheduled)
                        }
                        false
                    }
                    else -> false
                }
            }
        )

        SwipeToDismissBox(
            state = dismissState,
            enableDismissFromEndToStart = canSwipeToSkipOrUnskip,
            enableDismissFromStartToEnd = canSwipeToStatusChange,
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
                            isRenewed -> Color(0xFF8B5CF6).copy(alpha = 0.15f)
                            else -> Color(0xFF10B981).copy(alpha = 0.15f)
                        }
                    }
                    isEndToStart -> Color.Gray.copy(alpha = 0.15f)
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
                            isRenewed -> Icons.Default.LocalShipping
                            else -> Icons.Default.CheckCircle
                        }
                    }
                    isEndToStart -> Icons.AutoMirrored.Filled.Undo
                    else -> null
                }
                val iconTint = when {
                    isStartToEnd -> {
                        when {
                            isSkippedStatus -> MaterialTheme.colorScheme.primary
                            isUpcoming -> MaterialTheme.colorScheme.tertiary
                            isRenewed -> Color(0xFF8B5CF6)
                            else -> Color(0xFF10B981)
                        }
                    }
                    isEndToStart -> Color.Gray
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
                            .background(accentColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        val imageModel = item.scheduled.picturePath.takeIf { !it.isNullOrEmpty() }
                            ?: item.bookstore?.profilePic.takeIf { !it.isNullOrEmpty() && it != "ic_launcher_foreground" }
                        if (imageModel != null) {
                            AsyncImage(
                                model = imageModel,
                                contentDescription = "$displayTitle Cover",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            val storeName = item.bookstore?.name ?: displayTitle
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
                            "skipped" -> if (isDark) Color(0xFF7F1D1D) else Color(0xFFFEE2E2)
                            "shipped" -> if (isDark) Color(0xFF4C1D95) else Color(0xFFF3E8FF)
                            "received" -> if (isDark) Color(0xFF064E3B) else Color(0xFFD1FAE5)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                        val statusOnContainerColor = when (item.scheduled.status.lowercase()) {
                            "upcoming" -> if (isDark) Color(0xFFF1F5F9) else Color(0xFF334155)
                            "renewed", "paid" -> if (isDark) Color(0xFFDBEAFE) else Color(0xFF1E40AF)
                            "skipped" -> if (isDark) Color(0xFFFEE2E2) else Color(0xFF991B1B)
                            "shipped" -> if (isDark) Color(0xFFF3E8FF) else Color(0xFF5B21B6)
                            "received" -> if (isDark) Color(0xFFD1FAE5) else Color(0xFF065F46)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        val statusIcon = when (item.scheduled.status.lowercase()) {
                            "upcoming" -> Icons.Default.Schedule
                            "renewed", "paid" -> Icons.Default.Payments
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
            onDismiss = { showEditDialog = false }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SubscriptionItemRow(item: SubscriptionWithBookstore, viewModel: BookishViewModel) {
    val userState by viewModel.userState.collectAsState()
    val currency = userState?.currency ?: "$"
    var showEditDialog by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }
    var totalDragX by remember { mutableFloatStateOf(0f) }

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

                // Bookstore Profile Picture Box
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.bookstore != null && !item.bookstore.profilePic.isNullOrEmpty() && item.bookstore.profilePic != "ic_launcher_foreground") {
                        AsyncImage(
                            model = item.bookstore.profilePic,
                            contentDescription = "${item.bookstore.name} Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        val storeName = item.bookstore?.name ?: item.subscription.title
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
    var showTimePicker by remember { mutableStateOf(false) }
    var showDueDatePicker by remember { mutableStateOf(false) }

    val calendar = Calendar.getInstance()
    var dueDate by remember { mutableStateOf(calendar.timeInMillis) }
    var startDate by remember { mutableStateOf(calendar.timeInMillis) }
    var finishDate by remember { mutableStateOf(0L) }

    val userDateFormatPattern = viewModel.userState.collectAsState().value?.dateFormat ?: "yyyy-MM-dd"
    val dateFormat = remember(userDateFormatPattern) { SimpleDateFormat(userDateFormatPattern, Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Subscription", fontWeight = FontWeight.Bold) },
        text = {
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
                        label = { Text("Subscription Title") },
                        modifier = Modifier.fillMaxWidth().testTag("add_sub_title")
                    )
                }

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
                                        onValueChange = { numberOfMonthsStr = it },
                                        label = { Text("Months") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f).testTag("add_sub_number_of_months")
                                    )
                                    OutlinedTextField(
                                        value = numberOfSkipsStr,
                                        onValueChange = { numberOfSkipsStr = it },
                                        label = { Text("Number of Skips") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f).testTag("add_sub_number_of_skips")
                                    )
                                }
                            } else {
                                OutlinedTextField(
                                    value = numberOfSkipsStr,
                                    onValueChange = { numberOfSkipsStr = it },
                                    label = { Text("Number of Skips") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth().testTag("add_sub_number_of_skips")
                                )
                            }
                        }
                    }
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
                            label = { Text("Price ($currency)") },
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
                                        value = dateFormat.format(Date(dueDate)),
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Due Date") },
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
                            // Trigger Row/Box
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .clickable { showTimePicker = true }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    val timeLabel = String.format("%02d:%02d", reminderHour, reminderMinute)
                                    val ddayLabel = if (reminderDDayOffset == 0) "D-Day" else "$reminderDDayOffset days before"
                                    Text(
                                        text = "Alert on $ddayLabel at $timeLabel",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "Edit Time",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            if (showTimePicker) {
                                var tempHour by remember { mutableStateOf(reminderHour) }
                                var tempMinute by remember { mutableStateOf(reminderMinute) }
                                var tempDDayOffset by remember { mutableStateOf(reminderDDayOffset) }

                                AlertDialog(
                                    onDismissRequest = { showTimePicker = false },
                                    title = { Text("Set Reminder", fontWeight = FontWeight.Bold) },
                                    text = {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                "Choose when to be notified of renewal alerts.",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceEvenly,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // D-Day Offset Picker
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("Alert Day", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    RevolverWheelPicker(
                                                        items = (0..7).toList(),
                                                        selectedItem = tempDDayOffset,
                                                        onItemSelected = { dday -> tempDDayOffset = dday },
                                                        modifier = Modifier.width(90.dp),
                                                        label = { dday -> if (dday == 0) "D-Day" else "$dday days before" }
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(8.dp))

                                                // Hour Picker (Scrolling revolver)
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("Hour", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    RevolverWheelPicker(
                                                        items = (0..23).toList(),
                                                        selectedItem = tempHour,
                                                        onItemSelected = { hr -> tempHour = hr },
                                                        modifier = Modifier.width(55.dp),
                                                        label = { hr -> String.format("%02d", hr) }
                                                    )
                                                }

                                                Text(":", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)

                                                // Minute Picker (Scrolling revolver)
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("Min", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    RevolverWheelPicker(
                                                        items = (0..59).toList(),
                                                        selectedItem = tempMinute,
                                                        onItemSelected = { mn -> tempMinute = mn },
                                                        modifier = Modifier.width(55.dp),
                                                        label = { mn -> String.format("%02d", mn) }
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    confirmButton = {
                                        Button(
                                            onClick = {
                                                reminderDDayOffset = tempDDayOffset
                                                reminderHour = tempHour
                                                reminderMinute = tempMinute
                                                showTimePicker = false
                                            }
                                        ) {
                                            Text("Set")
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showTimePicker = false }) {
                                            Text("Cancel")
                                        }
                                    }
                                )
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
                            numberOfMonths = numberOfMonthsStr.toIntOrNull()
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
        ComposeDatePickerDialog(
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
    var showTimePicker by remember { mutableStateOf(false) }
    var showDueDatePicker by remember { mutableStateOf(false) }

    var dueDate by remember { mutableStateOf(sub.dueDate) }
    var startDate by remember { mutableStateOf(sub.startDate) }
    var finishDate by remember { mutableStateOf(sub.finishDate) }

    val userDateFormatPattern = viewModel.userState.collectAsState().value?.dateFormat ?: "yyyy-MM-dd"
    val dateFormat = remember(userDateFormatPattern) { SimpleDateFormat(userDateFormatPattern, Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Subscription", fontWeight = FontWeight.Bold) },
        text = {
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
                        label = { Text("Subscription Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

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
                                        onValueChange = { numberOfMonthsStr = it },
                                        label = { Text("Months") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = numberOfSkipsStr,
                                        onValueChange = { numberOfSkipsStr = it },
                                        label = { Text("Number of Skips") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            } else {
                                OutlinedTextField(
                                    value = numberOfSkipsStr,
                                    onValueChange = { numberOfSkipsStr = it },
                                    label = { Text("Number of Skips") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
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
                            label = { Text("Price ($currency)") },
                            leadingIcon = { Text(currency, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
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
                                        value = dateFormat.format(Date(dueDate)),
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Due Date") },
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
                            // Trigger Row/Box
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .clickable { showTimePicker = true }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    val timeLabel = String.format("%02d:%02d", reminderHour, reminderMinute)
                                    val ddayLabel = if (reminderDDayOffset == 0) "D-Day" else "$reminderDDayOffset days before"
                                    Text(
                                        text = "Alert on $ddayLabel at $timeLabel",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "Edit Time",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            if (showTimePicker) {
                                var tempHour by remember { mutableStateOf(reminderHour) }
                                var tempMinute by remember { mutableStateOf(reminderMinute) }
                                var tempDDayOffset by remember { mutableStateOf(reminderDDayOffset) }

                                AlertDialog(
                                    onDismissRequest = { showTimePicker = false },
                                    title = { Text("Set Reminder", fontWeight = FontWeight.Bold) },
                                    text = {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                "Choose when to be notified of renewal alerts.",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceEvenly,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // D-Day Offset Picker
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("Alert Day", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    RevolverWheelPicker(
                                                        items = (0..7).toList(),
                                                        selectedItem = tempDDayOffset,
                                                        onItemSelected = { dday -> tempDDayOffset = dday },
                                                        modifier = Modifier.width(90.dp),
                                                        label = { dday -> if (dday == 0) "D-Day" else "$dday days before" }
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(8.dp))

                                                // Hour Picker (Scrolling revolver)
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("Hour", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    RevolverWheelPicker(
                                                        items = (0..23).toList(),
                                                        selectedItem = tempHour,
                                                        onItemSelected = { hr -> tempHour = hr },
                                                        modifier = Modifier.width(55.dp),
                                                        label = { hr -> String.format("%02d", hr) }
                                                    )
                                                }

                                                Text(":", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)

                                                // Minute Picker (Scrolling revolver)
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("Min", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    RevolverWheelPicker(
                                                        items = (0..59).toList(),
                                                        selectedItem = tempMinute,
                                                        onItemSelected = { mn -> tempMinute = mn },
                                                        modifier = Modifier.width(55.dp),
                                                        label = { mn -> String.format("%02d", mn) }
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    confirmButton = {
                                        Button(
                                            onClick = {
                                                reminderDDayOffset = tempDDayOffset
                                                reminderHour = tempHour
                                                reminderMinute = tempMinute
                                                showTimePicker = false
                                            }
                                        ) {
                                            Text("Set")
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showTimePicker = false }) {
                                            Text("Cancel")
                                        }
                                    }
                                )
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
                                    numberOfMonths = numberOfMonthsStr.toIntOrNull()
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

    if (showDueDatePicker) {
        ComposeDatePickerDialog(
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
                            listOf("Upcoming", "Skipped", "Renewed", "Shipped", "Received").forEach { st ->
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
                                value = dateFormat.format(Date(dueDate)),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Due Date") },
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
        ComposeDatePickerDialog(
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
    onDismiss: () -> Unit
) {
    val sc = scheduledWithDetails.scheduled
    val subType = scheduledWithDetails.subscriptionType
    val context = LocalContext.current

    var bookTitle by remember { mutableStateOf(sc.bookTitle) }
    var bookAuthor by remember { mutableStateOf(sc.bookAuthor) }
    var description by remember { mutableStateOf(sc.description) }
    var status by remember { mutableStateOf(sc.status) }
    var imageUrl by remember { mutableStateOf(sc.picturePath ?: "") }
    var rating by remember { mutableStateOf(sc.rating) }

    var dueDate by remember { mutableStateOf(sc.dueDate) }
    var showDueDatePicker by remember { mutableStateOf(false) }

    var reminderHour by remember { mutableStateOf(subType?.reminderHour ?: 8) }
    var reminderMinute by remember { mutableStateOf(subType?.reminderMinute ?: 0) }
    var showTimePicker by remember { mutableStateOf(false) }

    val userDateFormatPattern = viewModel.userState.collectAsState().value?.dateFormat ?: "yyyy-MM-dd"
    val dateFormat = remember(userDateFormatPattern) { SimpleDateFormat(userDateFormatPattern, Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Scheduled Subscription", fontWeight = FontWeight.Bold) },
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
                        label = { Text("Subscription Type") },
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
                                    listOf("Upcoming", "Skipped", "Renewed", "Shipped", "Received").forEach { st ->
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
                                    value = dateFormat.format(Date(dueDate)),
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Due Date") },
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

                if (subType != null) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .clickable { showTimePicker = true }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                val timeLabel = String.format("%02d:%02d", reminderHour, reminderMinute)
                                Text(
                                    text = "Delivery Time: $timeLabel",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "Edit Time",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
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
                        label = { Text("Description") },
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
                        subType?.let { st ->
                            if (st.reminderHour != reminderHour || st.reminderMinute != reminderMinute) {
                                viewModel.updateSubscriptionType(
                                    st.copy(
                                        reminderHour = reminderHour,
                                        reminderMinute = reminderMinute
                                    )
                                )
                            }
                        }
                        viewModel.updateScheduledSubscription(
                            sc.copy(
                                bookTitle = bookTitle,
                                bookAuthor = bookAuthor,
                                description = description,
                                status = status,
                                isSkipped = status.equals("Skipped", ignoreCase = true),
                                dueDate = dueDate,
                                picturePath = imageUrl.ifEmpty { null },
                                rating = rating
                            )
                        )
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
        ComposeDatePickerDialog(
            initialDateMillis = dueDate,
            onDateSelected = { dueDate = it },
            onDismiss = { showDueDatePicker = false }
        )
    }

    if (showTimePicker) {
        var tempHour by remember { mutableStateOf(reminderHour) }
        var tempMinute by remember { mutableStateOf(reminderMinute) }

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Set Delivery Time", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Set the time for scheduled subscription events.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Hour", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            RevolverWheelPicker(
                                items = (0..23).toList(),
                                selectedItem = tempHour,
                                onItemSelected = { hr -> tempHour = hr },
                                modifier = Modifier.width(55.dp),
                                label = { hr -> String.format("%02d", hr) }
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))
                        Text(":", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Min", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            RevolverWheelPicker(
                                items = (0..59).toList(),
                                selectedItem = tempMinute,
                                onItemSelected = { mn -> tempMinute = mn },
                                modifier = Modifier.width(55.dp),
                                label = { mn -> String.format("%02d", mn) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        reminderHour = tempHour
                        reminderMinute = tempMinute
                        showTimePicker = false
                    }
                ) {
                    Text("Set")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
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
                    Text("Filter Scheduled Subscriptions", fontWeight = FontWeight.Bold, fontSize = 14.sp)

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
                                listOf("Upcoming", "Skipped", "Renewed", "Shipped", "Received").forEach { ty ->
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
                    Text("Filter Subscriptions", fontWeight = FontWeight.Bold, fontSize = 14.sp)

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
                                listOf("Waitlist", "Active", "Paused", "Canceled", "Wishlist").forEach { st ->
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

@Composable
fun ScheduledSubscriptionImagePicker(
    imageUrl: String,
    onImageSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onImageSelected(uri.toString())
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
                    listState.animateScrollToItem(closestItem.index)
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

