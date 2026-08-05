package com.example.ui.screens

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.example.R
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreordersScreen(
    viewModel: BookishViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val currency = "$"

    // Observe flows
    val userState by viewModel.userState.collectAsState()
    val preordersList by viewModel.filteredPreordersState.collectAsState()
    val bookstores by viewModel.bookstoresState.collectAsState()

    // Filter, Add, Search trigger states
    var showAddPreorderDialog by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    val searchQuery by viewModel.preorderSearch.collectAsState()
    var showSearchRow by remember { mutableStateOf(false) }

    // Calendar toggle
    val isCalendarView by viewModel.preorderIsCalendarView.collectAsState()

    // Timeframe selector for stats (0 = 7 days, 1 = Monthly, 2 = Yearly)
    var currentStatsTimeframe by remember { mutableStateOf(0) }

    var preorderListViewTab by remember { mutableStateOf(0) } // 0 = Upcoming, 1 = Past, 2 = All
    val todayStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    LaunchedEffect(preordersList, todayStart) {
        val upcomingPreorders = preordersList.filter { it.preorder.rangedSaleDateStart >= todayStart }
        if (upcomingPreorders.isNotEmpty()) {
            val closest = upcomingPreorders.minByOrNull { it.preorder.rangedSaleDateStart }
            if (closest != null) {
                val diffMs = closest.preorder.rangedSaleDateStart - todayStart
                val diffDays = diffMs / (24 * 60 * 60 * 1000f)
                currentStatsTimeframe = when {
                    diffDays < 7f -> 0    // 7 Days
                    diffDays < 30f -> 1   // Monthly
                    else -> 2             // Yearly
                }
            }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val onDeletePreorder: (Preorder) -> Unit = { preorder ->
        viewModel.deletePreorder(preorder)
        coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = "Deleted preorder: ${preorder.bookTitle}",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.restorePreorder(preorder)
            }
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                TopAppBar(
                    title = {
                        Text("Preorders", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    },
                    actions = {
                        // Action 1: Toggle View (Calendar / List)
                        IconButton(
                            onClick = { viewModel.preorderIsCalendarView.value = !isCalendarView },
                            modifier = Modifier.testTag("preorder_toggle_view")
                        ) {
                            Icon(
                                imageVector = if (isCalendarView) Icons.Default.List else Icons.Default.CalendarMonth,
                                contentDescription = "Toggle View"
                            )
                        }

                        // Action 2: Toggle Search
                        IconButton(
                            onClick = { showSearchRow = !showSearchRow },
                            modifier = Modifier.testTag("preorder_search")
                        ) {
                            Icon(
                                imageVector = if (showSearchRow) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Search"
                            )
                        }

                        // Action 3: Filter
                        IconButton(
                            onClick = { showFilterDialog = true },
                            modifier = Modifier.testTag("preorder_filter")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Filter"
                            )
                        }

                        // Action 4: Add Preorder
                        IconButton(
                            onClick = { showAddPreorderDialog = true },
                            modifier = Modifier.testTag("preorder_add")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add"
                            )
                        }
                    }
                )

                // Search Input Field
                AnimatedVisibility(visible = showSearchRow) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.preorderSearch.value = it },
                            placeholder = { Text("Search by book title or author...") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.preorderSearch.value = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = null)
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("preorder_search_input")
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val timeframes = listOf("7 Days", "This Month", "Next Month", "Yearly")
            val preorderStatsPagerState = rememberPagerState(initialPage = 0, pageCount = { timeframes.size })
            var showCosts by remember(userState?.displayAmounts) { mutableStateOf(userState?.displayAmounts ?: false) }
            val contrastColor = if (!isDark) Color(0xFFFF5722) else MaterialTheme.colorScheme.tertiary
            val statsCardBg = if (!isDark) Color(0xFFFF5722).copy(alpha = 0.05f) else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.05f)
            val statsCardBorder = if (!isDark) BorderStroke(1.dp, Color(0xFFFF5722).copy(alpha = 0.15f)) else null

            // Display Upper Stats grid with counters (supports swiping left/right to switch timeframe)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = statsCardBg
                ),
                border = statsCardBorder
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    val currentLabel = timeframes[preorderStatsPagerState.currentPage]
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
                            color = contrastColor,
                            modifier = Modifier.testTag("stats_timeframe_title")
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (showCosts) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (showCosts) "Hide costs" else "Show costs",
                                tint = contrastColor,
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
                                            .size(if (preorderStatsPagerState.currentPage == index) 7.dp else 5.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (preorderStatsPagerState.currentPage == index) contrastColor
                                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                            )
                                            .clickable { coroutineScope.launch { preorderStatsPagerState.animateScrollToPage(index) } }
                                    )
                                }
                            }
                        }
                    }

                    HorizontalPager(
                        state = preorderStatsPagerState,
                        modifier = Modifier.fillMaxWidth()
                    ) { page ->
                        val (startTime, endTime) = when (page) {
                            0 -> {
                                // 7 days is from today onwards
                                val start = todayStart
                                val end = Calendar.getInstance().apply {
                                    timeInMillis = todayStart
                                    add(Calendar.DAY_OF_YEAR, 7)
                                    set(Calendar.HOUR_OF_DAY, 23)
                                    set(Calendar.MINUTE, 59)
                                    set(Calendar.SECOND, 59)
                                    set(Calendar.MILLISECOND, 999)
                                }.timeInMillis
                                Pair(start, end)
                            }
                            1 -> {
                                // Monthly is for current month
                                val start = Calendar.getInstance().apply {
                                    set(Calendar.DAY_OF_MONTH, 1)
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }.timeInMillis
                                val end = Calendar.getInstance().apply {
                                    timeInMillis = start
                                    add(Calendar.MONTH, 1)
                                    add(Calendar.MILLISECOND, -1)
                                }.timeInMillis
                                Pair(start, end)
                            }
                            2 -> {
                                // Next Month
                                val start = Calendar.getInstance().apply {
                                    set(Calendar.DAY_OF_MONTH, 1)
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                    add(Calendar.MONTH, 1)
                                }.timeInMillis
                                val end = Calendar.getInstance().apply {
                                    timeInMillis = start
                                    add(Calendar.MONTH, 1)
                                    add(Calendar.MILLISECOND, -1)
                                }.timeInMillis
                                Pair(start, end)
                            }
                            else -> {
                                // Yearly is for current year
                                val start = Calendar.getInstance().apply {
                                    set(Calendar.DAY_OF_YEAR, 1)
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }.timeInMillis
                                val end = Calendar.getInstance().apply {
                                    timeInMillis = start
                                    add(Calendar.YEAR, 1)
                                    add(Calendar.MILLISECOND, -1)
                                }.timeInMillis
                                Pair(start, end)
                            }
                        }

                        val preordersInScope = preordersList.filter {
                            it.preorder.rangedSaleDateStart in startTime..endTime
                        }

                        val upcoming = preordersInScope.count { it.preorder.status.lowercase() == "upcoming" }
                        val released = preordersInScope.count { it.preorder.status.lowercase() == "released" }
                        val preordered = preordersInScope.count { it.preorder.status.lowercase() == "preordered" }
                        val shipped = preordersInScope.count { it.preorder.status.lowercase() == "shipped" }

                        val totalSpent = preordersInScope
                            .filter { it.preorder.status.lowercase() in listOf("preordered", "shipped", "received") }
                            .sumOf { it.preorder.price }
                        val totalUpcoming = preordersInScope.sumOf { it.preorder.price }

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Upcoming", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("$upcoming", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (!isDark) Color(0xFFD84315) else MaterialTheme.colorScheme.outline)
                                }
                                Divider(modifier = Modifier.height(30.dp).width(1.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Released", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("$released", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (!isDark) Color(0xFFE65100) else Color(0xFFFF9800))
                                }
                                Divider(modifier = Modifier.height(30.dp).width(1.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Preordered", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("$preordered", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (!isDark) Color(0xFF1976D2) else Color(0xFF2196F3))
                                }
                                Divider(modifier = Modifier.height(30.dp).width(1.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Shipped", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("$shipped", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (!isDark) Color(0xFF1565C0) else MaterialTheme.colorScheme.primary)
                                }
                            }

                            if (showCosts) {
                                Divider(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                        Text("Total Spent", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${currency}${String.format("%.2f", totalSpent)}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = contrastColor)
                                    }
                                    Divider(modifier = Modifier.height(28.dp).width(1.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                        Text("Total Upcoming", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${currency}${String.format("%.2f", totalUpcoming)}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isCalendarView) {
                // CALENDAR VIEW (PREORDERS)
                val dateFormatKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val markerColor = MaterialTheme.colorScheme.tertiary
                val markerMap = remember(preordersList, markerColor) {
                    preordersList.groupBy { dateFormatKey.format(Date(it.preorder.rangedSaleDateStart)) }
                        .mapValues { entry ->
                            entry.value.map { item ->
                                CalendarMarker(
                                    id = item.preorder.id.toString(),
                                    title = item.preorder.bookTitle,
                                    color = markerColor
                                )
                            }
                        }
                }

                var focusedDayPreorders by remember { mutableStateOf<List<PreorderWithBookstore>>(emptyList()) }
                var focusedDateStr by remember { mutableStateOf("") }

                MonthCalendar(
                    markerDates = markerMap,
                    onDayClick = { dateStr, markers ->
                        focusedDateStr = dateStr
                        focusedDayPreorders = preordersList.filter { dateFormatKey.format(Date(it.preorder.rangedSaleDateStart)) == dateStr }
                    },
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                if (focusedDateStr.isNotEmpty()) {
                    Text(
                        text = "Preorders releasing on $focusedDateStr:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    if (focusedDayPreorders.isEmpty()) {
                        Text(
                            "No books scheduled for release on this day.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    } else {
                        LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
                            items(focusedDayPreorders) { item ->
                                PreorderItemRow(item, viewModel, onDeleted = onDeletePreorder)
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Tap a calendar date to view preorders",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        fontWeight = FontWeight.Medium
                    )
                }

            } else {
                // LIST VIEW Tabbed (Upcoming, Past, All) - Grouped Monthly
                val monthHeaderFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
                val monthHeaderNoYearFormat = remember { SimpleDateFormat("MMMM", Locale.getDefault()) }
                val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }

                fun formatPreorderGroupHeader(timeMs: Long): String {
                    val cal = Calendar.getInstance().apply { timeInMillis = timeMs }
                    return if (cal.get(Calendar.YEAR) == currentYear) {
                        monthHeaderNoYearFormat.format(Date(timeMs))
                    } else {
                        monthHeaderFormat.format(Date(timeMs))
                    }
                }

                val upcomingList = remember(preordersList, todayStart) {
                    preordersList.filter { it.preorder.rangedSaleDateStart >= todayStart }
                        .sortedBy { it.preorder.rangedSaleDateStart }
                }
                val groupedUpcoming = remember(upcomingList, currentYear) {
                    upcomingList.groupBy { formatPreorderGroupHeader(it.preorder.rangedSaleDateStart) }
                }

                val pastList = remember(preordersList, todayStart) {
                    preordersList.filter { it.preorder.rangedSaleDateStart < todayStart }
                        .sortedByDescending { it.preorder.rangedSaleDateStart }
                }
                val groupedPast = remember(pastList, currentYear) {
                    pastList.groupBy { formatPreorderGroupHeader(it.preorder.rangedSaleDateStart) }
                }

                val allSorted = remember(preordersList) {
                    preordersList.sortedBy { it.preorder.rangedSaleDateStart }
                }
                val groupedAll = remember(allSorted, currentYear) {
                    allSorted.groupBy { formatPreorderGroupHeader(it.preorder.rangedSaleDateStart) }
                }

                // Three tab controls
                val tabContrastColor = if (!isDark) Color(0xFFFF5722) else MaterialTheme.colorScheme.primary
                TabRow(
                    selectedTabIndex = preorderListViewTab,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    contentColor = tabContrastColor,
                    divider = {}
                ) {
                    Tab(
                        selected = preorderListViewTab == 0,
                        onClick = { preorderListViewTab = 0 },
                        text = { Text("Upcoming", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                        selectedContentColor = tabContrastColor,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Tab(
                        selected = preorderListViewTab == 1,
                        onClick = { preorderListViewTab = 1 },
                        text = { Text("Past", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                        selectedContentColor = tabContrastColor,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Tab(
                        selected = preorderListViewTab == 2,
                        onClick = { preorderListViewTab = 2 },
                        text = { Text("All", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                        selectedContentColor = tabContrastColor,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (preorderListViewTab == 0) {
                    if (groupedUpcoming.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No upcoming preorders.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            groupedUpcoming.keys.forEach { dateHeader ->
                                item {
                                    Text(
                                        text = dateHeader,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                                items(groupedUpcoming[dateHeader] ?: emptyList(), key = { it.preorder.id }) { item ->
                                    PreorderItemRow(item, viewModel, onDeleted = onDeletePreorder)
                                }
                            }
                        }
                    }
                } else if (preorderListViewTab == 1) {
                    if (groupedPast.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No past preorders.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            groupedPast.keys.forEach { monthHeader ->
                                item {
                                    Text(
                                        text = monthHeader,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                                items(groupedPast[monthHeader] ?: emptyList(), key = { it.preorder.id }) { item ->
                                    PreorderItemRow(item, viewModel, onDeleted = onDeletePreorder)
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
                            Text("No preorders found.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            groupedAll.keys.forEach { dateHeader ->
                                item {
                                    Text(
                                        text = dateHeader,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                                items(groupedAll[dateHeader] ?: emptyList(), key = { it.preorder.id }) { item ->
                                    PreorderItemRow(item, viewModel, onDeleted = onDeletePreorder)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add Preorder Dialog
        if (showAddPreorderDialog) {
            AddPreorderDialog(
                viewModel = viewModel,
                bookstores = bookstores,
                onDismiss = { showAddPreorderDialog = false }
            )
        }

        // Filter Dialog
        if (showFilterDialog) {
            PreorderFilterDialog(
                viewModel = viewModel,
                bookstores = bookstores,
                onDismiss = { showFilterDialog = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreorderItemRow(
    item: PreorderWithBookstore,
    viewModel: BookishViewModel,
    onDeleted: (Preorder) -> Unit
) {
    val currency = viewModel.userState.collectAsState().value?.currency ?: "$"
    val userDateFormatPattern = viewModel.userState.collectAsState().value?.dateFormat ?: "yyyy-MM-dd"
    val dateFormat = remember(userDateFormatPattern) { SimpleDateFormat(userDateFormatPattern, Locale.getDefault()) }

    val isDark = isSystemInDarkTheme()
    var showEditDialog by remember { mutableStateOf(false) }

    val accentColor = when (item.preorder.status.lowercase()) {
        "upcoming" -> Color(0xFF64748B) // Grey
        "preordered" -> Color(0xFF2563EB) // Blue
        "released" -> Color(0xFFF97316) // Orange
        "shipped" -> Color(0xFF8B5CF6) // Purple
        "received" -> Color(0xFF10B981) // Green
        else -> Color(0xFF64748B)
    }

    val currentStatus = item.preorder.status.lowercase()
    val canSwipeRight = currentStatus in listOf("released", "preordered", "shipped")
    val canSwipeLeft = true

    key(item.preorder.id, item.preorder.status) {
        val dismissState = rememberSwipeToDismissBoxState(
            confirmValueChange = { dismissValue ->
                if (dismissValue == SwipeToDismissBoxValue.StartToEnd && canSwipeRight) {
                    val nextStatus = when (currentStatus) {
                        "released" -> "Preordered"
                        "preordered" -> "Shipped"
                        "shipped" -> "Received"
                        else -> item.preorder.status
                    }
                    viewModel.updatePreorder(item.preorder.copy(status = nextStatus))
                    false
                } else if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                    onDeleted(item.preorder)
                    true
                } else {
                    false
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
                        when (currentStatus) {
                            "released" -> Color(0xFF3B82F6).copy(alpha = 0.15f) // Blue
                            "preordered" -> Color(0xFF8B5CF6).copy(alpha = 0.15f) // Purple
                            "shipped" -> Color(0xFF10B981).copy(alpha = 0.15f) // Green for status change to Received
                            else -> Color.Transparent
                        }
                    }
                    isEndToStart -> Color(0xFFEF4444).copy(alpha = 0.15f) // Red for delete
                    else -> Color.Transparent
                }
                val alignment = when {
                    isStartToEnd -> Alignment.CenterStart
                    isEndToStart -> Alignment.CenterEnd
                    else -> Alignment.Center
                }
                val icon = when {
                    isStartToEnd -> {
                        when (currentStatus) {
                            "released" -> ImageVector.vectorResource(R.drawable.ic_shopping_bag_speed)
                            "preordered" -> Icons.Default.LocalShipping
                            "shipped" -> Icons.Default.CheckCircle
                            else -> null
                        }
                    }
                    isEndToStart -> Icons.Default.Delete
                    else -> null
                }
                val iconTint = when {
                    isStartToEnd -> {
                        when (currentStatus) {
                            "released" -> Color(0xFF3B82F6)
                            "preordered" -> Color(0xFF8B5CF6)
                            "shipped" -> Color(0xFF10B981) // Green for status change to Received
                            else -> Color.Transparent
                        }
                    }
                    isEndToStart -> Color(0xFFEF4444)
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
                    containerColor = when (item.preorder.status.lowercase()) {
                        "upcoming" -> Color(0xFF64748B).copy(alpha = if (isDark) 0.12f else 0.06f)
                        "preordered" -> Color(0xFF2563EB).copy(alpha = if (isDark) 0.12f else 0.06f)
                        "released" -> Color(0xFFF97316).copy(alpha = if (isDark) 0.12f else 0.06f)
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
                        val imageModel = item.preorder.picturePath.takeIf { !it.isNullOrEmpty() }
                            ?: item.bookstore?.profilePic.takeIf { !it.isNullOrEmpty() && it != "ic_launcher_foreground" }
                        if (imageModel != null) {
                            AsyncImage(
                                model = imageModel,
                                contentDescription = "${item.preorder.bookTitle} Cover",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            val storeName = item.bookstore?.name ?: item.preorder.bookTitle
                            val initial = (storeName.firstOrNull { it.isLetterOrDigit() } ?: 'P').uppercaseChar().toString()
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
                                text = item.preorder.bookTitle,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                lineHeight = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "By ${if (!item.preorder.bookAuthor.isNullOrBlank()) item.preorder.bookAuthor else "Unknown Author"}",
                                fontSize = 11.5.sp,
                                lineHeight = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (item.bookstore != null) {
                                Text(
                                    text = item.bookstore.name,
                                    fontSize = 10.5.sp,
                                    lineHeight = 13.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        // Status Badge with icon before text & Price with dot spacer
                        val statusContainerColor = when (item.preorder.status.lowercase()) {
                            "upcoming" -> if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9)
                            "preordered" -> if (isDark) Color(0xFF1E3A8A) else Color(0xFFDBEAFE)
                            "released" -> if (isDark) Color(0xFF7C2D12) else Color(0xFFFFEDD5)
                            "shipped" -> if (isDark) Color(0xFF4C1D95) else Color(0xFFF3E8FF)
                            "received" -> if (isDark) Color(0xFF064E3B) else Color(0xFFD1FAE5)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                        val statusOnContainerColor = when (item.preorder.status.lowercase()) {
                            "upcoming" -> if (isDark) Color(0xFFF1F5F9) else Color(0xFF334155)
                            "preordered" -> if (isDark) Color(0xFFDBEAFE) else Color(0xFF1E40AF)
                            "released" -> if (isDark) Color(0xFFFFEDD5) else Color(0xFF9A3412)
                            "shipped" -> if (isDark) Color(0xFFF3E8FF) else Color(0xFF5B21B6)
                            "received" -> if (isDark) Color(0xFFD1FAE5) else Color(0xFF065F46)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        val statusIcon = when (item.preorder.status.lowercase()) {
                            "upcoming", "preordered" -> Icons.Default.ShoppingBag
                            "released" -> Icons.Default.NewReleases
                            "shipped" -> Icons.Default.LocalShipping
                            "received" -> Icons.Default.CheckCircle
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
                                    text = item.preorder.status,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = statusOnContainerColor
                                )
                            }

                            Text(
                                text = "•",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = "$currency${String.format("%.2f", item.preorder.price)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Right Column: Date Square Card
                    val dateFormatMonth = remember { SimpleDateFormat("MMM", Locale.getDefault()) }
                    val dateFormatDay = remember { SimpleDateFormat("dd", Locale.getDefault()) }
                    val eventDate = Date(item.preorder.rangedSaleDateStart)
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
                            verticalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Text(
                                text = monthStr,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = accentColor,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = dayStr,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        EditPreorderDialog(
            preorderWithBookstore = item,
            viewModel = viewModel,
            bookstores = viewModel.bookstoresState.collectAsState().value,
            onDismiss = { showEditDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPreorderDialog(
    viewModel: BookishViewModel,
    bookstores: List<Bookstore>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currency = viewModel.userState.collectAsState().value?.currency ?: "$"
    var selectedBookstoreId by remember { mutableStateOf(bookstores.firstOrNull()?.id ?: 0) }
    var bookTitle by remember { mutableStateOf("") }
    var bookAuthor by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priceStr by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Preordered") }
    var imageUrl by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf(0.0) }
    var reminderEnabled by remember { mutableStateOf(false) }
    var reminderDDayOffset by remember { mutableStateOf(0) }
    var reminderHour by remember { mutableStateOf(8) }
    var reminderMinute by remember { mutableStateOf(0) }
    var showTimePicker by remember { mutableStateOf(false) }

    val calendar = Calendar.getInstance()
    var saleDateStart by remember { mutableStateOf(calendar.timeInMillis) }
    var saleDateEnd by remember { mutableStateOf(0L) }

    val userDateFormatPattern = viewModel.userState.collectAsState().value?.dateFormat ?: "yyyy-MM-dd"
    val dateFormat = remember(userDateFormatPattern) { SimpleDateFormat(userDateFormatPattern, Locale.getDefault()) }

    var selectedTab by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Preorder", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    PreorderImagePicker(
                        imageUrl = imageUrl,
                        onImageSelected = { imageUrl = it }
                    )
                }

                item {
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
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Notes & Rating", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                            selectedContentColor = MaterialTheme.colorScheme.primary,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (selectedTab == 0) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Select Bookstore", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
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
                            value = bookTitle,
                            onValueChange = { bookTitle = it },
                            label = { Text("Book Title") },
                            modifier = Modifier.fillMaxWidth().testTag("add_preorder_title")
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = bookAuthor,
                            onValueChange = { bookAuthor = it },
                            label = { Text("Book Author") },
                            modifier = Modifier.fillMaxWidth().testTag("add_preorder_author")
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            var expandedStatus by remember { mutableStateOf(false) }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Status", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = { expandedStatus = true },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(status, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                    }
                                    DropdownMenu(expanded = expandedStatus, onDismissRequest = { expandedStatus = false }) {
                                        listOf("Upcoming", "Released", "Preordered", "Shipped", "Received").forEach { st ->
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
                                modifier = Modifier.weight(1f).testTag("add_preorder_price")
                            )
                        }
                    }

                    // Date selectors: Google Calendar style preorder period field
                    item {
                        GoogleCalendarStylePreorderPeriodPicker(
                            initialStartDate = saleDateStart,
                            initialEndDate = saleDateEnd,
                            onPeriodChanged = { start, end ->
                                saleDateStart = start
                                saleDateEnd = end
                            }
                        )
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
                                                    "Choose when to be notified of preorder alerts.",
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
                } else {
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedPrice = priceStr.toDoubleOrNull() ?: 0.0
                    if (bookTitle.isNotEmpty() && selectedBookstoreId > 0) {
                        viewModel.addPreorder(
                            bookstoreId = selectedBookstoreId,
                            bookTitle = bookTitle,
                            bookAuthor = bookAuthor,
                            description = description,
                            price = parsedPrice,
                            saleDateStart = saleDateStart,
                            saleDateEnd = saleDateEnd,
                            status = status,
                            picturePath = imageUrl.ifEmpty { null },
                            reminderEnabled = reminderEnabled,
                            reminderDDayOffset = reminderDDayOffset,
                            reminderHour = reminderHour,
                            reminderMinute = reminderMinute,
                            rating = rating
                        )
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("confirm_add_preorder")
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
fun EditPreorderDialog(
    preorderWithBookstore: PreorderWithBookstore,
    viewModel: BookishViewModel,
    bookstores: List<Bookstore>,
    onDismiss: () -> Unit
) {
    val pr = preorderWithBookstore.preorder
    val context = LocalContext.current
    val currency = viewModel.userState.collectAsState().value?.currency ?: "$"

    var selectedBookstoreId by remember { mutableStateOf(pr.bookstoreId) }
    var bookTitle by remember { mutableStateOf(pr.bookTitle) }
    var bookAuthor by remember { mutableStateOf(pr.bookAuthor) }
    var description by remember { mutableStateOf(pr.description) }
    var priceStr by remember { mutableStateOf(pr.price.toString()) }
    var status by remember { mutableStateOf(pr.status) }
    var imageUrl by remember { mutableStateOf(pr.picturePath ?: "") }
    var rating by remember { mutableStateOf(pr.rating) }
    var reminderEnabled by remember { mutableStateOf(pr.reminderEnabled) }
    var reminderDDayOffset by remember { mutableStateOf(pr.reminderDDayOffset) }
    var reminderHour by remember { mutableStateOf(pr.reminderHour) }
    var reminderMinute by remember { mutableStateOf(pr.reminderMinute) }
    var showTimePicker by remember { mutableStateOf(false) }

    var saleDateStart by remember { mutableStateOf(pr.rangedSaleDateStart) }
    var saleDateEnd by remember { mutableStateOf(pr.rangedSaleDateEnd) }

    val userDateFormatPattern = viewModel.userState.collectAsState().value?.dateFormat ?: "yyyy-MM-dd"
    val dateFormat = remember(userDateFormatPattern) { SimpleDateFormat(userDateFormatPattern, Locale.getDefault()) }

    var selectedTab by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Preorder", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    PreorderImagePicker(
                        imageUrl = imageUrl,
                        onImageSelected = { imageUrl = it }
                    )
                }

                item {
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
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Notes & Rating", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                            selectedContentColor = MaterialTheme.colorScheme.primary,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (selectedTab == 0) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Select Bookstore", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            var expandedStore by remember { mutableStateOf(false) }
                            val currentStore = bookstores.find { it.id == selectedBookstoreId } ?: bookstores.firstOrNull()
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { expandedStore = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(currentStore?.name ?: "Unknown", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            var expandedStatus by remember { mutableStateOf(false) }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Status", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = { expandedStatus = true },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(status, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                    }
                                    DropdownMenu(expanded = expandedStatus, onDismissRequest = { expandedStatus = false }) {
                                        listOf("Upcoming", "Released", "Preordered", "Shipped", "Received").forEach { st ->
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

                    // Date selectors: Google Calendar style preorder period field
                    item {
                        GoogleCalendarStylePreorderPeriodPicker(
                            initialStartDate = saleDateStart,
                            initialEndDate = saleDateEnd,
                            onPeriodChanged = { start, end ->
                                saleDateStart = start
                                saleDateEnd = end
                            }
                        )
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
                                                    "Choose when to be notified of preorder alerts.",
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
                } else {
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
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        viewModel.deletePreorder(pr)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }

                Button(
                    onClick = {
                        val parsedPrice = priceStr.toDoubleOrNull() ?: 0.0
                        if (bookTitle.isNotEmpty()) {
                            viewModel.updatePreorder(
                                pr.copy(
                                    bookstoreId = selectedBookstoreId,
                                    bookTitle = bookTitle,
                                    bookAuthor = bookAuthor,
                                    description = description,
                                    price = parsedPrice,
                                    rangedSaleDateStart = saleDateStart,
                                    rangedSaleDateEnd = saleDateEnd,
                                    status = status,
                                    picturePath = imageUrl.ifEmpty { null },
                                    reminderEnabled = reminderEnabled,
                                    reminderDDayOffset = reminderDDayOffset,
                                    reminderHour = reminderHour,
                                    reminderMinute = reminderMinute,
                                    rating = rating
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
fun PreorderFilterDialog(
    viewModel: BookishViewModel,
    bookstores: List<Bookstore>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter Preorders", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Bookstore Filter
                val currentStoreId by viewModel.preorderFilterBookstore.collectAsState()
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
                                    viewModel.preorderFilterBookstore.value = null
                                    expandedStore = false
                                }
                            )
                            bookstores.forEach { store ->
                                DropdownMenuItem(
                                    text = { Text(store.name) },
                                    onClick = {
                                        viewModel.preorderFilterBookstore.value = store.id
                                        expandedStore = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Status Filter
                val currentStatus by viewModel.preorderFilterStatus.collectAsState()
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
                                    viewModel.preorderFilterStatus.value = null
                                    expandedStatus = false
                                }
                            )
                            listOf("Upcoming", "Released", "Preordered", "Shipped", "Received").forEach { st ->
                                DropdownMenuItem(
                                    text = { Text(st) },
                                    onClick = {
                                        viewModel.preorderFilterStatus.value = st
                                        expandedStatus = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Freeform Author / Book Filters
                var authorText by remember { mutableStateOf(viewModel.preorderFilterAuthor.value ?: "") }
                var bookText by remember { mutableStateOf(viewModel.preorderFilterBook.value ?: "") }

                OutlinedTextField(
                    value = authorText,
                    onValueChange = {
                        authorText = it
                        viewModel.preorderFilterAuthor.value = if (it.isBlank()) null else it
                    },
                    label = { Text("Filter by Author Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = bookText,
                    onValueChange = {
                        bookText = it
                        viewModel.preorderFilterBook.value = if (it.isBlank()) null else it
                    },
                    label = { Text("Filter by Book Title") },
                    modifier = Modifier.fillMaxWidth()
                )
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
                    viewModel.preorderFilterBookstore.value = null
                    viewModel.preorderFilterStatus.value = null
                    viewModel.preorderFilterAuthor.value = null
                    viewModel.preorderFilterBook.value = null
                    onDismiss()
                }
            ) {
                Text("Clear All")
            }
        }
    )
}

@Composable
fun PreorderImagePicker(
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
                    contentDescription = "Selected Preorder Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = "Default Preorder Icon",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        // Pencil Edit Button on the bottom-right corner of the circle
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComposeDateRangePickerDialog(
    initialStartDateMillis: Long,
    initialEndDateMillis: Long?,
    onDatesSelected: (Long, Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialStartDateMillis,
        initialSelectedEndDateMillis = initialEndDateMillis
    )
    val formatter = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val start = dateRangePickerState.selectedStartDateMillis
                    val end = dateRangePickerState.selectedEndDateMillis
                    if (start != null) {
                        onDatesSelected(start, end)
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

private fun combineDateAndTime(dateMs: Long, hour: Int, minute: Int): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = dateMs
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreordersDatePickerDialog(
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

@Composable
private fun ComposeTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    title: String,
    onTimeSelected: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var tempHour by remember { mutableStateOf(initialHour) }
    var tempMinute by remember { mutableStateOf(initialMinute) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                    onTimeSelected(tempHour, tempMinute)
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

@Composable
private fun GoogleCalendarStylePreorderPeriodPicker(
    initialStartDate: Long,
    initialEndDate: Long,
    onPeriodChanged: (Long, Long) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    
    val startCal = remember(initialStartDate) {
        Calendar.getInstance().apply { timeInMillis = initialStartDate }
    }
    val endCal = remember(initialEndDate, initialStartDate) {
        Calendar.getInstance().apply {
            if (initialEndDate > 0L) {
                timeInMillis = initialEndDate
            } else {
                timeInMillis = initialStartDate + 60 * 60 * 1000L
            }
        }
    }

    var startDateLong by remember { mutableStateOf(initialStartDate) }
    var startHour by remember { mutableStateOf(startCal.get(Calendar.HOUR_OF_DAY)) }
    var startMinute by remember { mutableStateOf(startCal.get(Calendar.MINUTE)) }

    var endDateLong by remember { mutableStateOf(if (initialEndDate > 0L) initialEndDate else initialStartDate + 60 * 60 * 1000L) }
    var endHour by remember { mutableStateOf(endCal.get(Calendar.HOUR_OF_DAY)) }
    var endMinute by remember { mutableStateOf(endCal.get(Calendar.MINUTE)) }

    val wasAllDay = startCal.get(Calendar.HOUR_OF_DAY) == 0 && startCal.get(Calendar.MINUTE) == 0 &&
                    endCal.get(Calendar.HOUR_OF_DAY) == 0 && endCal.get(Calendar.MINUTE) == 0 &&
                    initialEndDate > 0L

    var isAllDay by remember { mutableStateOf(wasAllDay) }
    var isExpanded by remember { mutableStateOf(false) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    val areDatesSame = run {
        val cal1 = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = startDateLong }
        val cal2 = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = endDateLong }
        cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
        cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
        cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
    }

    LaunchedEffect(startDateLong, startHour, startMinute, endDateLong, endHour, endMinute, isAllDay) {
        val finalStart = combineDateAndTime(startDateLong, if (isAllDay) 0 else startHour, if (isAllDay) 0 else startMinute)
        val finalEnd = combineDateAndTime(endDateLong, if (isAllDay) 0 else endHour, if (isAllDay) 0 else endMinute)
        onPeriodChanged(finalStart, finalEnd)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Preorder Period", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)

        OutlinedCard(
            onClick = { isExpanded = true },
            modifier = Modifier.fillMaxWidth().testTag("preorder_period_card")
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
                    val displayText = if (isAllDay) {
                        if (areDatesSame) "$startText (All Day)" else "$startText - $endText (All Day)"
                    } else {
                        val startTimeStr = String.format("%02d:%02d", startHour, startMinute)
                        val endTimeStr = String.format("%02d:%02d", endHour, endMinute)
                        if (areDatesSame) {
                            "$startText, $startTimeStr - $endTimeStr"
                        } else {
                            "$startText $startTimeStr - $endText $endTimeStr"
                        }
                    }
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
                    text = "Configure Preorder Period",
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
                    if (isAllDay) {
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
                                            text = dateFormat.format(Date(combineDateAndTime(startDateLong, 0, 0))),
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
                                            text = dateFormat.format(Date(combineDateAndTime(startDateLong, 0, 0))),
                                            modifier = Modifier.padding(10.dp),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("End Date", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedCard(
                                        onClick = { showEndDatePicker = true },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = dateFormat.format(Date(combineDateAndTime(endDateLong, 0, 0))),
                                            modifier = Modifier.padding(10.dp),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        if (!areDatesSame) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                                text = dateFormat.format(Date(combineDateAndTime(startDateLong, 0, 0))),
                                                modifier = Modifier.padding(10.dp),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Start Time", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        OutlinedCard(
                                            onClick = { showStartTimePicker = true },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = String.format("%02d:%02d", startHour, startMinute),
                                                modifier = Modifier.padding(10.dp),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("End Date", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        OutlinedCard(
                                            onClick = { showEndDatePicker = true },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = dateFormat.format(Date(combineDateAndTime(endDateLong, 0, 0))),
                                                modifier = Modifier.padding(10.dp),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("End Time", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        OutlinedCard(
                                            onClick = { showEndTimePicker = true },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = String.format("%02d:%02d", endHour, endMinute),
                                                modifier = Modifier.padding(10.dp),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                                text = dateFormat.format(Date(combineDateAndTime(startDateLong, 0, 0))),
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
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Start Time", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        OutlinedCard(
                                            onClick = { showStartTimePicker = true },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = String.format("%02d:%02d", startHour, startMinute),
                                                modifier = Modifier.padding(10.dp),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("End Time", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        OutlinedCard(
                                            onClick = { showEndTimePicker = true },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = String.format("%02d:%02d", endHour, endMinute),
                                                modifier = Modifier.padding(10.dp),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isAllDay,
                            onCheckedChange = { isAllDay = it },
                            modifier = Modifier.testTag("preorder_all_day_checkbox")
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Preorder lasts all day",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
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
        PreordersDatePickerDialog(
            initialDateMillis = startDateLong,
            onDateSelected = { selected: Long ->
                startDateLong = selected
                if (isAllDay) {
                    if (endDateLong < selected) {
                        endDateLong = selected
                    }
                } else {
                    if (areDatesSame) {
                        endDateLong = selected
                    } else if (endDateLong < selected) {
                        endDateLong = selected
                    }
                }
            },
            onDismiss = { showStartDatePicker = false }
        )
    }

    if (showEndDatePicker) {
        PreordersDatePickerDialog(
            initialDateMillis = endDateLong,
            onDateSelected = { selected: Long ->
                if (selected >= startDateLong) {
                    endDateLong = selected
                }
            },
            onDismiss = { showEndDatePicker = false }
        )
    }

    if (showStartTimePicker) {
        ComposeTimePickerDialog(
            initialHour = startHour,
            initialMinute = startMinute,
            title = "Set Start Time",
            onTimeSelected = { h, m ->
                startHour = h
                startMinute = m
                if (areDatesSame && (h > endHour || (h == endHour && m > endMinute))) {
                    endHour = (h + 1) % 24
                    endMinute = m
                }
            },
            onDismiss = { showStartTimePicker = false }
        )
    }

    if (showEndTimePicker) {
        ComposeTimePickerDialog(
            initialHour = endHour,
            initialMinute = endMinute,
            title = "Set End Time",
            onTimeSelected = { h, m ->
                endHour = h
                endMinute = m
            },
            onDismiss = { showEndTimePicker = false }
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




