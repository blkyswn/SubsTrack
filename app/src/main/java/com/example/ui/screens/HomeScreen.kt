package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.example.R
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.viewmodel.BookishViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private data class SubGraphData(
    val title: String,
    val countOther: Int,
    val countSkipped: Int,
    val countUpcoming: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: BookishViewModel,
    onNavigateToSubscriptions: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val user by viewModel.userState.collectAsState()
    val scheduled by viewModel.rawScheduledState.collectAsState()
    val subscriptions by viewModel.rawSubscriptionsState.collectAsState()
    val preorders by viewModel.rawPreordersState.collectAsState()

    val currency = user?.currency ?: "$"
    val bookstores by viewModel.bookstoresState.collectAsState()

    // Bottom sheet / Dialog details state
    var selectedStatType by remember { mutableStateOf<String?>(null) } // "scheduled", "subscriptions", "preorders"
    var showDetailsSheet by remember { mutableStateOf(false) }

    var statusFilterTitle by remember { mutableStateOf<String?>(null) }
    var statusFilterScheduledItems by remember { mutableStateOf<List<ScheduledWithDetails>?>(null) }
    var statusFilterSubItems by remember { mutableStateOf<List<SubscriptionWithBookstore>?>(null) }

    var selectedPreorderForEdit by remember { mutableStateOf<PreorderWithBookstore?>(null) }
    var selectedScheduledForEdit by remember { mutableStateOf<ScheduledWithDetails?>(null) }
    var groupedEventsPopupData by remember { mutableStateOf<GroupedEventsPopupData?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val statsPagerState = rememberPagerState(pageCount = { 2 })
    val topActiveSubsPagerState = rememberPagerState(pageCount = { 2 })
    val spendsPagerState = rememberPagerState(pageCount = { 2 })

    var discreteToastMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(discreteToastMessage) {
        if (discreteToastMessage != null) {
            kotlinx.coroutines.delay(1800)
            discreteToastMessage = null
        }
    }

    val allActiveUpcomingEvents = remember(scheduled, preorders) {
        val now = System.currentTimeMillis()
        val calToday = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayStart = calToday.timeInMillis

        val scheduledEvents = scheduled.filter {
            val status = it.scheduled.status
            !it.scheduled.isSkipped &&
                    !status.equals("Skipped", ignoreCase = true) &&
                    !status.equals("Received", ignoreCase = true) &&
                    !status.equals("Cancelled", ignoreCase = true) &&
                    it.scheduled.dueDate >= todayStart
        }.map {
            val subTypeName = it.subscriptionType?.title?.ifBlank { null } ?: it.scheduled.bookTitle
            val startDt = it.scheduled.dueDate
            val endDt = it.scheduled.dueDate
            val reminderHour = if (it.subscriptionType?.reminderEnabled == true) it.subscriptionType?.reminderHour else null
            val reminderMinute = if (it.subscriptionType?.reminderEnabled == true) it.subscriptionType?.reminderMinute else null

            UpcomingEvent(
                id = "sched_${it.scheduled.id}",
                title = subTypeName,
                author = it.scheduled.bookAuthor,
                date = startDt,
                startDate = startDt,
                endDate = endDt,
                type = "scheduled",
                status = it.scheduled.status,
                isSkipped = false,
                subTitle = subTypeName,
                price = it.subscriptionType?.price,
                rawScheduled = it,
                rawPreorder = null,
                reminderHour = reminderHour,
                reminderMinute = reminderMinute,
                hasTime = reminderHour != null
            )
        }

        val preorderEvents = preorders.filter {
            val status = it.preorder.status
            val endDt = if (it.preorder.rangedSaleDateEnd > 0L) it.preorder.rangedSaleDateEnd else it.preorder.rangedSaleDateStart
            !status.equals("Received", ignoreCase = true) &&
                    !status.equals("Cancelled", ignoreCase = true) &&
                    endDt >= todayStart
        }.map {
            val bookstoreName = it.bookstore?.name?.trim() ?: ""
            val titleText = if (bookstoreName.isNotBlank()) "${it.preorder.bookTitle} ($bookstoreName)" else it.preorder.bookTitle
            val startDt = it.preorder.rangedSaleDateStart
            val endDt = if (it.preorder.rangedSaleDateEnd > 0L) it.preorder.rangedSaleDateEnd else startDt

            val calStart = Calendar.getInstance().apply { timeInMillis = startDt }
            val startHour: Int? = if (calStart.get(Calendar.HOUR_OF_DAY) != 0 || calStart.get(Calendar.MINUTE) != 0) {
                calStart.get(Calendar.HOUR_OF_DAY)
            } else null
            val startMinute: Int? = if (calStart.get(Calendar.HOUR_OF_DAY) != 0 || calStart.get(Calendar.MINUTE) != 0) {
                calStart.get(Calendar.MINUTE)
            } else null

            val preorderReminderHour: Int? = if (it.preorder.reminderEnabled) it.preorder.reminderHour else null
            val preorderReminderMinute: Int? = if (it.preorder.reminderEnabled) it.preorder.reminderMinute else null

            val finalReminderHour = startHour ?: preorderReminderHour
            val finalReminderMinute = startMinute ?: preorderReminderMinute

            UpcomingEvent(
                id = "pre_${it.preorder.id}",
                title = titleText,
                author = it.preorder.bookAuthor,
                date = startDt,
                startDate = startDt,
                endDate = endDt,
                type = "preorder",
                status = it.preorder.status,
                isSkipped = false,
                subTitle = bookstoreName,
                price = it.preorder.price,
                rawScheduled = null,
                rawPreorder = it,
                reminderHour = finalReminderHour,
                reminderMinute = finalReminderMinute,
                hasTime = finalReminderHour != null
            )
        }

        (scheduledEvents + preorderEvents)
            .sortedBy { it.startDate }
    }

    val upcomingEvents = remember(allActiveUpcomingEvents) {
        allActiveUpcomingEvents.take(5)
    }

    var parentBoxWindowY by remember { mutableFloatStateOf(0f) }
    var currentTargetY by remember { mutableFloatStateOf(-1f) }
    val density = LocalDensity.current
    val defaultTargetY = with(density) { 380.dp.toPx() }
    val curveHeightPx = with(density) { 380.dp.toPx() }

    val isDark = isSystemInDarkTheme()
    val backgroundColor = MaterialTheme.colorScheme.background
    val calendarCardBgColor = MaterialTheme.colorScheme.primary

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .padding(innerPadding)
                .onGloballyPositioned { coordinates ->
                    parentBoxWindowY = coordinates.positionInWindow().y
                }
        ) {
            val activeTargetY = if (currentTargetY != -1f) currentTargetY else defaultTargetY

            if (activeTargetY > 0f) {
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val extraWidth = size.width * 0.25f
                    val arcWidth = size.width + extraWidth * 2f
                    val arcRadiusY = arcWidth / 2f
                    val rectHeight = (activeTargetY - arcRadiusY).coerceAtLeast(0f)

                    val archBrush = Brush.verticalGradient(
                        0.0f to backgroundColor,
                        1.0f to calendarCardBgColor,
                        startY = 0f,
                        endY = activeTargetY
                    )

                    // Fill rectangle above the half circle diameter line
                    if (rectHeight > 0f) {
                        drawRect(
                            brush = archBrush,
                            topLeft = Offset(-extraWidth, 0f),
                            size = Size(arcWidth, rectHeight)
                        )
                    }

                    // Draw wider half circle arc ending at activeTargetY
                    drawArc(
                        brush = archBrush,
                        startAngle = 0f,
                        sweepAngle = 180f,
                        useCenter = true,
                        topLeft = Offset(-extraWidth, activeTargetY - 2f * arcRadiusY),
                        size = Size(arcWidth, 2f * arcRadiusY)
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 52.dp, bottom = 4.dp)
                        .testTag("home_top_bar"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoStories,
                        contentDescription = "SubsTrack Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "SubsTrack",
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                }
            }

            // Welcome Header & Calendar Overview Card
            item {
                val now = System.currentTimeMillis()
                val getDayStart = { timestamp: Long ->
                    val cal = Calendar.getInstance()
                    cal.timeInMillis = timestamp
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    cal.timeInMillis
                }

                val todayStart = getDayStart(now)
                val tomorrowStart = todayStart + 24 * 60 * 60 * 1000L
                val dayAfterTomorrowStart = tomorrowStart + 24 * 60 * 60 * 1000L

                val dayNameFormat = remember { SimpleDateFormat("EEEE", Locale.getDefault()) }
                val dayNumberFormat = remember { SimpleDateFormat("d", Locale.getDefault()) }
                val dayOfWeekFormat = remember { SimpleDateFormat("EEEE", Locale.getDefault()) }
                val dayMonthFormat = remember { SimpleDateFormat("d MMM", Locale.getDefault()) }
                val sevenDaysOutStart = todayStart + 7 * 24 * 60 * 60 * 1000L

                val todayDate = Date(now)
                val todayDayName = dayNameFormat.format(todayDate).uppercase(Locale.getDefault())
                val todayDayNumber = dayNumberFormat.format(todayDate)

                fun eventCoversDay(event: UpcomingEvent, targetDayStart: Long): Boolean {
                    val s = getDayStart(event.startDate)
                    val e = getDayStart(event.endDate)
                    return targetDayStart in s..e
                }

                val todayEvents = allActiveUpcomingEvents.filter { eventCoversDay(it, todayStart) }
                val tomorrowEvents = allActiveUpcomingEvents.filter { eventCoversDay(it, tomorrowStart) }

                val futureEvents = allActiveUpcomingEvents.filter { getDayStart(it.startDate) >= dayAfterTomorrowStart }
                val nextDayStart = futureEvents.firstOrNull()?.let { getDayStart(it.startDate) }
                val nextDayEvents = if (nextDayStart != null) {
                    allActiveUpcomingEvents.filter { eventCoversDay(it, nextDayStart) }
                } else {
                    emptyList()
                }

                val displayedToday = todayEvents
                val displayedTomorrow = tomorrowEvents
                val displayedNextDay = nextDayEvents

                val totalDisplayed = displayedToday.size + displayedTomorrow.size + displayedNextDay.size
                val remainingCount = (allActiveUpcomingEvents.size - totalDisplayed).coerceAtLeast(0)

                val isDark = isSystemInDarkTheme()
                val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                val (greetingPrefix, greetingIcon) = when {
                    currentHour in 5..11 -> "Good morning" to Icons.Default.WbSunny
                    currentHour in 12..17 -> "Good afternoon" to Icons.Default.WbTwilight
                    else -> "Good night" to Icons.Default.NightsStay
                }
                val userName = user?.username?.ifBlank { "User" } ?: "User"
                val greetingText = "$greetingPrefix, $userName!"

                val greetingBgColor = backgroundColor
                val greetingBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .testTag("home_greeting_box"),
                    shape = RoundedCornerShape(12.dp),
                    color = greetingBgColor,
                    border = BorderStroke(1.dp, greetingBorderColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = greetingIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = greetingText,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("home_greeting_text")
                        )
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            val cardWindowY = coordinates.positionInWindow().y
                            val cardHeight = coordinates.size.height
                            currentTargetY = cardWindowY + (cardHeight * 0.80f) - parentBoxWindowY
                        }
                        .testTag("welcome_calendar_box"),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        // Calendar Widget Layout
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // LEFT COLUMN: Today Date & Today's Events
                            Column(
                                modifier = Modifier.weight(0.48f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .then(
                                            if (todayEvents.size > 1) {
                                                Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .clickable {
                                                        groupedEventsPopupData = GroupedEventsPopupData("Today's Events (${todayEvents.size})", todayEvents)
                                                    }
                                            } else Modifier
                                        ),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = todayDayName,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                        )
                                        Text(
                                            text = todayDayNumber,
                                            fontSize = 38.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            lineHeight = 40.sp
                                        )
                                    }
                                    if (todayEvents.size > 1) {
                                        Icon(
                                            imageVector = Icons.Default.Layers,
                                            contentDescription = "Grouped events",
                                            tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                                            modifier = Modifier
                                                .size(18.dp)
                                                .padding(end = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))

                                if (todayEvents.isNotEmpty()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                        todayEvents.forEach { event ->
                                            CalendarBoxEventItem(
                                                event = event,
                                                targetDayStart = todayStart,
                                                userDisplayAmounts = user?.displayAmounts == true,
                                                currency = currency,
                                                onClick = {
                                                    if (event.type == "preorder" && event.rawPreorder != null) {
                                                        selectedPreorderForEdit = event.rawPreorder
                                                    } else if (event.type == "scheduled" && event.rawScheduled != null) {
                                                        selectedScheduledForEdit = event.rawScheduled
                                                    }
                                                }
                                            )
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "No events today",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                                    )
                                }
                            }

                            // RIGHT COLUMN: Grouped Future Events
                            Column(
                                modifier = Modifier.weight(0.52f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val futureEventsList = allActiveUpcomingEvents.filter {
                                    getDayStart(it.startDate) >= tomorrowStart || getDayStart(it.endDate) >= tomorrowStart
                                }

                                if (futureEventsList.isEmpty()) {
                                    Text(
                                        text = "No upcoming events",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                                    )
                                } else {
                                    val groupedFuture = futureEventsList.groupBy { event ->
                                        val startDay = getDayStart(event.startDate)
                                        if (startDay >= tomorrowStart) startDay else tomorrowStart
                                    }.toSortedMap()

                                    val topDayGroups = groupedFuture.entries.take(2)
                                    val displayedFutureCount = topDayGroups.sumOf { it.value.size }
                                    val totalDisplayedCount = todayEvents.size + displayedFutureCount
                                    val leftOverCount = (allActiveUpcomingEvents.size - totalDisplayedCount).coerceAtLeast(0)

                                    topDayGroups.forEach { (dayStartTs, eventsForDay) ->
                                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                            val groupHeaderStr = when {
                                                dayStartTs == tomorrowStart -> "TOMORROW"
                                                dayStartTs < sevenDaysOutStart -> dayOfWeekFormat.format(Date(dayStartTs)).uppercase(Locale.getDefault())
                                                else -> dayMonthFormat.format(Date(dayStartTs)).uppercase(Locale.getDefault())
                                            }

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .then(
                                                        if (eventsForDay.size > 1) {
                                                            Modifier
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .clickable {
                                                                    groupedEventsPopupData = GroupedEventsPopupData("Events for $groupHeaderStr (${eventsForDay.size})", eventsForDay)
                                                                }
                                                        } else Modifier
                                                    ),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = groupHeaderStr,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                                                    style = TextStyle(
                                                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                                                    )
                                                )

                                                if (eventsForDay.size > 1) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Layers,
                                                            contentDescription = "Grouped events",
                                                            tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                                                            modifier = Modifier.size(11.dp)
                                                        )
                                                        Text(
                                                            text = "${eventsForDay.size} items",
                                                            fontSize = 9.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                                                        )
                                                    }
                                                }
                                            }

                                            eventsForDay.forEach { event ->
                                                CalendarBoxEventItem(
                                                    event = event,
                                                    targetDayStart = dayStartTs,
                                                    userDisplayAmounts = user?.displayAmounts == true,
                                                    currency = currency,
                                                    onClick = {
                                                        if (event.type == "preorder" && event.rawPreorder != null) {
                                                            selectedPreorderForEdit = event.rawPreorder
                                                        } else if (event.type == "scheduled" && event.rawScheduled != null) {
                                                            selectedScheduledForEdit = event.rawScheduled
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    if (leftOverCount > 0) {
                                        val leftoverList = futureEventsList.drop(displayedFutureCount)
                                        Row(
                                            modifier = Modifier
                                                .height(IntrinsicSize.Min)
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable {
                                                    groupedEventsPopupData = GroupedEventsPopupData(
                                                        "Grouped Upcoming Events (${if (leftoverList.isNotEmpty()) leftoverList.size else allActiveUpcomingEvents.size})",
                                                        if (leftoverList.isNotEmpty()) leftoverList else allActiveUpcomingEvents
                                                    )
                                                }
                                                .padding(vertical = 2.dp, horizontal = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .width(2.5.dp)
                                                    .fillMaxHeight()
                                                    .background(
                                                        MaterialTheme.colorScheme.onPrimary,
                                                        shape = RoundedCornerShape(2.dp)
                                                    )
                                            )
                                            Text(
                                                text = "+$leftOverCount more event${if (leftOverCount > 1) "s" else ""}",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bento Row 1: Subscription Types Overview (Hero - full width)
            item {
                val isDark = isSystemInDarkTheme()
                val contrastColor = if (!isDark) Color(0xFFFF5722) else MaterialTheme.colorScheme.primary
                val allSkips by viewModel.allSubscriptionSkipsState.collectAsState()

                if (subscriptions.isEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_stat_subscription_types"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No subscription types available",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    val pageCount = subscriptions.size
                    val heroPagerState = rememberPagerState(pageCount = { pageCount })
                    val safeIndex = heroPagerState.currentPage.coerceIn(0, pageCount - 1)
                    val currentSubItem = subscriptions[safeIndex]

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_stat_subscription_types"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(contrastColor.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Autorenew,
                                            contentDescription = null,
                                            tint = contrastColor
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = currentSubItem.subscription.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${currentSubItem.bookstore?.name ?: "Subscription"} • ${currency}${String.format("%.2f", currentSubItem.subscription.price)} (${currentSubItem.subscription.frequency})",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                if (pageCount > 1) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(start = 8.dp)
                                    ) {
                                        (0 until pageCount).forEach { index ->
                                            Box(
                                                modifier = Modifier
                                                    .size(if (safeIndex == index) 7.dp else 5.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (safeIndex == index) contrastColor
                                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                                    )
                                                    .clickable { coroutineScope.launch { heroPagerState.animateScrollToPage(index) } }
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            HorizontalPager(
                                state = heroPagerState,
                                modifier = Modifier.fillMaxWidth()
                            ) { pageIdx ->
                                val item = subscriptions[pageIdx]
                                val now = System.currentTimeMillis()
                                val subSkips = allSkips.filter { it.subscriptionTypeId == item.subscription.id }
                                val activeRegister = subSkips.firstOrNull { skip ->
                                    skip.skipStartDate != null && skip.skipEndDate != null && now >= skip.skipStartDate && now <= skip.skipEndDate
                                } ?: subSkips.firstOrNull { skip ->
                                    skip.skipStartDate == null || skip.skipEndDate == null
                                }

                                val skipType = item.subscription.skipType
                                val skipsText = when (skipType) {
                                    "Unlimited" -> "Unlimited"
                                    "None" -> "None"
                                    else -> {
                                        val totalSkips = activeRegister?.numberOfSkips ?: item.subscription.numberOfSkips ?: 0
                                        val skipsLeft = activeRegister?.skipsLeft ?: totalSkips
                                        if (totalSkips > 0) "$skipsLeft / $totalSkips left" else "$skipsLeft left"
                                    }
                                }

                                val subScheduledList = scheduled.filter {
                                    it.subscriptionType?.id == item.subscription.id || it.scheduled.subscriptionTypeId == item.subscription.id
                                }
                                val nextScheduled = subScheduledList
                                    .filter { it.scheduled.dueDate >= now - 86400000L }
                                    .minByOrNull { it.scheduled.dueDate }
                                    ?: subScheduledList.minByOrNull { it.scheduled.dueDate }

                                val isNextSkipped = nextScheduled?.scheduled?.isSkipped == true ||
                                        nextScheduled?.scheduled?.status.equals("Skipped", ignoreCase = true)

                                val renewalText = if (isNextSkipped) {
                                    "Renewal skipped!"
                                } else {
                                    val targetDueDate = nextScheduled?.scheduled?.dueDate ?: item.subscription.dueDate
                                    val diffMs = targetDueDate - now
                                    val daysLeft = kotlin.math.ceil(diffMs.toDouble() / (1000 * 60 * 60 * 24)).toLong().coerceAtLeast(0)
                                    val daysStr = if (daysLeft == 1L) "1 day" else "$daysLeft days"
                                    "Renews in $daysStr"
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Min),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(contrastColor.copy(alpha = 0.05f))
                                            .border(BorderStroke(1.dp, contrastColor.copy(alpha = 0.15f)), RoundedCornerShape(16.dp))
                                            .padding(horizontal = 9.dp, vertical = 7.dp)
                                    ) {
                                        Column {
                                            Text("Skips Available", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(modifier = Modifier.height(1.dp))
                                            Text(skipsText, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                            if (skipType != null && !skipType.equals("Unlimited", ignoreCase = true) && !skipType.equals("None", ignoreCase = true)) {
                                                val skipEndDate = activeRegister?.skipEndDate ?: run {
                                                    when (skipType) {
                                                        "Each calendar year" -> Calendar.getInstance().apply {
                                                            set(Calendar.MONTH, Calendar.DECEMBER)
                                                            set(Calendar.DAY_OF_MONTH, 31)
                                                            set(Calendar.HOUR_OF_DAY, 23)
                                                            set(Calendar.MINUTE, 59)
                                                            set(Calendar.SECOND, 59)
                                                            set(Calendar.MILLISECOND, 999)
                                                        }.timeInMillis
                                                        "Every certain months" -> Calendar.getInstance().apply {
                                                            val months = item.subscription.numberOfMonths ?: 1
                                                            add(Calendar.MONTH, months)
                                                            set(Calendar.HOUR_OF_DAY, 23)
                                                            set(Calendar.MINUTE, 59)
                                                            set(Calendar.SECOND, 59)
                                                            set(Calendar.MILLISECOND, 999)
                                                        }.timeInMillis
                                                        else -> null
                                                    }
                                                }
                                                if (skipEndDate != null) {
                                                    val diffMs = skipEndDate - now
                                                    val daysUntilSkipsRenew = kotlin.math.ceil(diffMs.toDouble() / (1000 * 60 * 60 * 24)).toLong().coerceAtLeast(0)
                                                    Spacer(modifier = Modifier.height(1.dp))
                                                    Text(
                                                        text = "Skips renew in $daysUntilSkipsRenew days",
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(
                                                if (isNextSkipped) MaterialTheme.colorScheme.error.copy(alpha = 0.05f)
                                                else MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f)
                                            )
                                            .border(
                                                BorderStroke(
                                                    1.dp,
                                                    if (isNextSkipped) MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                                    else MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                                ),
                                                RoundedCornerShape(16.dp)
                                            )
                                            .padding(horizontal = 9.dp, vertical = 7.dp)
                                    ) {
                                        Column {
                                            Text("Next Renewal", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(modifier = Modifier.height(1.dp))
                                            Text(
                                                renewalText,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isNextSkipped) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bento Row 2: Subscriptions Spends & Book Preorders (Side-by-side)
            item {
                val activeSubs = subscriptions.filter { it.subscription.status.equals("Active", ignoreCase = true) }
                val pausedSubs = subscriptions.filter { it.subscription.status.equals("Paused", ignoreCase = true) }
                val totalActiveSubs = activeSubs.size
                val totalPausedSubs = pausedSubs.size

                val isDark = isSystemInDarkTheme()
                val contrastColor = if (!isDark) Color(0xFFFF5722) else MaterialTheme.colorScheme.primary

                val currentCal = Calendar.getInstance()
                val currentYear = currentCal.get(Calendar.YEAR)
                val currentMonth = currentCal.get(Calendar.MONTH)

                val preordersThisMonth = preorders.filter { item ->
                    val cal = Calendar.getInstance().apply { timeInMillis = item.preorder.rangedSaleDateStart }
                    cal.get(Calendar.YEAR) == currentYear && cal.get(Calendar.MONTH) == currentMonth
                }

                val preordersThisYear = preorders.filter { item ->
                    val cal = Calendar.getInstance().apply { timeInMillis = item.preorder.rangedSaleDateStart }
                    cal.get(Calendar.YEAR) == currentYear
                }

                val scheduledThisMonth = scheduled.filter { item ->
                    val cal = Calendar.getInstance().apply { timeInMillis = item.scheduled.dueDate }
                    cal.get(Calendar.YEAR) == currentYear && cal.get(Calendar.MONTH) == currentMonth
                }

                val scheduledThisYear = scheduled.filter { item ->
                    val cal = Calendar.getInstance().apply { timeInMillis = item.scheduled.dueDate }
                    cal.get(Calendar.YEAR) == currentYear
                }

                // Monthly spend: preorders in month (active) + scheduled subs in month (active/renewed/shipped/received)
                val monthlySpend = preordersThisMonth
                    .filter { !it.preorder.status.equals("Canceled", ignoreCase = true) && !it.preorder.status.equals("Cancelled", ignoreCase = true) && !it.preorder.status.equals("Skipped", ignoreCase = true) }
                    .sumOf { it.preorder.price } +
                    scheduledThisMonth
                    .filter { !it.scheduled.status.equals("Upcoming", ignoreCase = true) && !it.scheduled.status.equals("Skipped", ignoreCase = true) }
                    .sumOf { it.subscriptionType?.price ?: 0.0 }

                // Monthly preview: all status
                val monthlyPreview = preordersThisMonth.sumOf { it.preorder.price } +
                    scheduledThisMonth.sumOf { it.subscriptionType?.price ?: 0.0 }

                // Yearly spend: preorders in year (active) + scheduled subs in year (active/renewed/shipped/received)
                val yearlySpend = preordersThisYear
                    .filter { !it.preorder.status.equals("Canceled", ignoreCase = true) && !it.preorder.status.equals("Cancelled", ignoreCase = true) && !it.preorder.status.equals("Skipped", ignoreCase = true) }
                    .sumOf { it.preorder.price } +
                    scheduledThisYear
                    .filter { !it.scheduled.status.equals("Upcoming", ignoreCase = true) && !it.scheduled.status.equals("Skipped", ignoreCase = true) }
                    .sumOf { it.subscriptionType?.price ?: 0.0 }

                // Yearly preview: all status
                val yearlyPreview = preordersThisYear.sumOf { it.preorder.price } +
                    scheduledThisYear.sumOf { it.subscriptionType?.price ?: 0.0 }

                val monthScheduledList = if (scheduledThisMonth.isNotEmpty()) scheduledThisMonth else scheduled
                val yearScheduledList = if (scheduledThisYear.isNotEmpty()) scheduledThisYear else scheduled

                val upcomingSched = monthScheduledList.count { it.scheduled.status.equals("Upcoming", ignoreCase = true) }
                val renewedSched = monthScheduledList.count { it.scheduled.status.equals("Renewed", ignoreCase = true) || it.scheduled.status.equals("Paid", ignoreCase = true) }
                val shippedSched = monthScheduledList.count { it.scheduled.status.equals("Shipped", ignoreCase = true) }
                val receivedSched = monthScheduledList.count { it.scheduled.status.equals("Received", ignoreCase = true) }
                val skippedSched = monthScheduledList.count { it.scheduled.status.equals("Skipped", ignoreCase = true) }

                val yearUpcomingSched = yearScheduledList.count { it.scheduled.status.equals("Upcoming", ignoreCase = true) }
                val yearRenewedSched = yearScheduledList.count { it.scheduled.status.equals("Renewed", ignoreCase = true) || it.scheduled.status.equals("Paid", ignoreCase = true) }
                val yearShippedSched = yearScheduledList.count { it.scheduled.status.equals("Shipped", ignoreCase = true) }
                val yearReceivedSched = yearScheduledList.count { it.scheduled.status.equals("Received", ignoreCase = true) }
                val yearSkippedSched = yearScheduledList.count { it.scheduled.status.equals("Skipped", ignoreCase = true) }

                val displaySubs = remember(subscriptions, activeSubs) { if (activeSubs.isNotEmpty()) activeSubs else subscriptions }

                val subGraphDataList = remember(displaySubs, scheduled) {
                    displaySubs.map { sub ->
                        val subSched = scheduled.filter {
                            it.scheduled.subscriptionTypeId == sub.subscription.id ||
                            it.subscriptionType?.id == sub.subscription.id
                        }
                        val countOther = subSched.count {
                            !it.scheduled.status.equals("Upcoming", ignoreCase = true) &&
                            !it.scheduled.status.equals("Skipped", ignoreCase = true)
                        }
                        val countSkipped = subSched.count {
                            it.scheduled.status.equals("Skipped", ignoreCase = true)
                        }
                        val countUpcoming = subSched.count {
                            it.scheduled.status.equals("Upcoming", ignoreCase = true)
                        }
                        val title = sub.subscription.title.ifEmpty { sub.bookstore?.name ?: "Sub" }
                        SubGraphData(
                            title = title,
                            countOther = countOther,
                            countSkipped = countSkipped,
                            countUpcoming = countUpcoming
                        )
                    }
                }

                val maxSubGraphCount = remember(subGraphDataList) {
                    subGraphDataList.flatMap {
                        listOf(it.countOther, it.countSkipped, it.countUpcoming)
                    }.maxOrNull()?.coerceAtLeast(1) ?: 1
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Left bento tile (Subscription Spends - 1 column)
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(180.dp)
                            .clickable {
                                selectedStatType = "subscriptions"
                                showDetailsSheet = true
                            }
                            .testTag("home_stat_subscriptions"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            HorizontalPager(
                                state = spendsPagerState,
                                modifier = Modifier.fillMaxSize()
                            ) { spendPage ->
                                if (spendPage == 0) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(contrastColor.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Payments,
                                                    contentDescription = null,
                                                    tint = contrastColor,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(if (spendsPagerState.currentPage == 0) 7.dp else 5.dp)
                                                        .clip(CircleShape)
                                                        .background(if (spendsPagerState.currentPage == 0) contrastColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                                                        .clickable { coroutineScope.launch { spendsPagerState.animateScrollToPage(0) } }
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .size(if (spendsPagerState.currentPage == 1) 7.dp else 5.dp)
                                                        .clip(CircleShape)
                                                        .background(if (spendsPagerState.currentPage == 1) contrastColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                                                        .clickable { coroutineScope.launch { spendsPagerState.animateScrollToPage(1) } }
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Text(
                                                text = "${currency}${String.format("%.2f", monthlySpend)}",
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                letterSpacing = (-0.5).sp
                                            )

                                            Text(
                                                text = "Monthly Spend",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            Spacer(modifier = Modifier.height(6.dp))

                                            Text(
                                                text = "${currency}${String.format("%.2f", monthlyPreview)}",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = contrastColor,
                                                letterSpacing = (-0.5).sp
                                            )

                                            Text(
                                                text = "Monthly Preview",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(contrastColor.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Payments,
                                                    contentDescription = null,
                                                    tint = contrastColor,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(if (spendsPagerState.currentPage == 0) 7.dp else 5.dp)
                                                        .clip(CircleShape)
                                                        .background(if (spendsPagerState.currentPage == 0) contrastColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                                                        .clickable { coroutineScope.launch { spendsPagerState.animateScrollToPage(0) } }
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .size(if (spendsPagerState.currentPage == 1) 7.dp else 5.dp)
                                                        .clip(CircleShape)
                                                        .background(if (spendsPagerState.currentPage == 1) contrastColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                                                        .clickable { coroutineScope.launch { spendsPagerState.animateScrollToPage(1) } }
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Text(
                                                text = "${currency}${String.format("%.2f", yearlySpend)}",
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                letterSpacing = (-0.5).sp
                                            )

                                            Text(
                                                text = "Yearly Spend",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            Spacer(modifier = Modifier.height(6.dp))

                                            Text(
                                                text = "${currency}${String.format("%.2f", yearlyPreview)}",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = contrastColor,
                                                letterSpacing = (-0.5).sp
                                            )

                                            Text(
                                                text = "Yearly Preview",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Right bento tile (Active Subscriptions - 2 columns)
                    Card(
                        modifier = Modifier
                            .weight(2f)
                            .height(180.dp)
                            .then(
                                if (topActiveSubsPagerState.currentPage == 0 && totalActiveSubs > 0) {
                                    Modifier.clickable { onNavigateToSubscriptions() }
                                } else Modifier
                            )
                            .testTag("home_stat_subscriptions_active"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            HorizontalPager(
                                state = topActiveSubsPagerState,
                                modifier = Modifier.fillMaxSize()
                            ) { topPage ->
                                if (topPage == 0) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(36.dp)
                                                            .clip(RoundedCornerShape(10.dp))
                                                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.AutoStories,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.tertiary,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }

                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Text(
                                                            text = "$totalActiveSubs",
                                                            fontSize = 20.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            letterSpacing = (-0.5).sp,
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .clickable {
                                                                    if (totalActiveSubs > 0) {
                                                                        statusFilterTitle = "Active Subscriptions ($totalActiveSubs)"
                                                                        statusFilterSubItems = activeSubs
                                                                        statusFilterScheduledItems = null
                                                                    } else {
                                                                        onNavigateToSubscriptions()
                                                                    }
                                                                }
                                                                .padding(horizontal = 2.dp, vertical = 2.dp)
                                                        )
                                                        Text(
                                                            text = "•",
                                                            fontSize = 14.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.secondary
                                                        )
                                                        Text(
                                                            text = "$totalPausedSubs paused",
                                                            fontSize = 16.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = MaterialTheme.colorScheme.secondary,
                                                            letterSpacing = (-0.5).sp,
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .clickable {
                                                                    if (totalPausedSubs > 0) {
                                                                        statusFilterTitle = "Paused Subscriptions ($totalPausedSubs)"
                                                                        statusFilterSubItems = pausedSubs
                                                                        statusFilterScheduledItems = null
                                                                    } else {
                                                                        onNavigateToSubscriptions()
                                                                    }
                                                                }
                                                                .padding(horizontal = 2.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }

                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(if (topActiveSubsPagerState.currentPage == 0) 7.dp else 5.dp)
                                                            .clip(CircleShape)
                                                            .background(if (topActiveSubsPagerState.currentPage == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                                                            .clickable { coroutineScope.launch { topActiveSubsPagerState.animateScrollToPage(0) } }
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .size(if (topActiveSubsPagerState.currentPage == 1) 7.dp else 5.dp)
                                                            .clip(CircleShape)
                                                            .background(if (topActiveSubsPagerState.currentPage == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                                                            .clickable { coroutineScope.launch { topActiveSubsPagerState.animateScrollToPage(1) } }
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = "Active Subscriptions",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Column(
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Text(
                                                        text = if (statsPagerState.currentPage == 0) "This month" else "This year",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }

                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(if (statsPagerState.currentPage == 0) 7.dp else 5.dp)
                                                            .clip(CircleShape)
                                                            .background(if (statsPagerState.currentPage == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                                                            .clickable { coroutineScope.launch { statsPagerState.animateScrollToPage(0) } }
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .size(if (statsPagerState.currentPage == 1) 7.dp else 5.dp)
                                                            .clip(CircleShape)
                                                            .background(if (statsPagerState.currentPage == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                                                            .clickable { coroutineScope.launch { statsPagerState.animateScrollToPage(1) } }
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))

                                            HorizontalPager(
                                                state = statsPagerState,
                                                modifier = Modifier.fillMaxWidth()
                                            ) { page ->
                                            if (page == 0) {
                                                // Status color indicators for current month
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    MiniStatusDot(
                                                        color = Color(0xFFEF4444),
                                                        count = skippedSched,
                                                        label = "Skipped",
                                                        icon = Icons.Default.Block,
                                                        onClick = {
                                                            statusFilterTitle = "Skipped Deliveries ($skippedSched)"
                                                            statusFilterScheduledItems = monthScheduledList.filter { it.scheduled.status.equals("Skipped", ignoreCase = true) }
                                                            statusFilterSubItems = null
                                                        },
                                                        onZeroClick = { discreteToastMessage = it },
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    MiniStatusDot(
                                                        color = Color(0xFFFF9800),
                                                        count = upcomingSched,
                                                        label = "Upcoming",
                                                        icon = Icons.Default.Schedule,
                                                        onClick = {
                                                            statusFilterTitle = "Upcoming Deliveries ($upcomingSched)"
                                                            statusFilterScheduledItems = monthScheduledList.filter { it.scheduled.status.equals("Upcoming", ignoreCase = true) }
                                                            statusFilterSubItems = null
                                                        },
                                                        onZeroClick = { discreteToastMessage = it },
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    MiniStatusDot(
                                                        color = Color(0xFF8B5CF6),
                                                        count = renewedSched,
                                                        label = "Renewed",
                                                        icon = Icons.Default.Payments,
                                                        onClick = {
                                                            statusFilterTitle = "Renewed Deliveries ($renewedSched)"
                                                            statusFilterScheduledItems = monthScheduledList.filter { it.scheduled.status.equals("Renewed", ignoreCase = true) || it.scheduled.status.equals("Paid", ignoreCase = true) }
                                                            statusFilterSubItems = null
                                                        },
                                                        onZeroClick = { discreteToastMessage = it },
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    MiniStatusDot(
                                                        color = Color(0xFF2196F3),
                                                        count = shippedSched,
                                                        label = "Shipped",
                                                        icon = Icons.Default.LocalShipping,
                                                        onClick = {
                                                            statusFilterTitle = "Shipped Deliveries ($shippedSched)"
                                                            statusFilterScheduledItems = monthScheduledList.filter { it.scheduled.status.equals("Shipped", ignoreCase = true) }
                                                            statusFilterSubItems = null
                                                        },
                                                        onZeroClick = { discreteToastMessage = it },
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    MiniStatusDot(
                                                        color = Color(0xFF4CAF50),
                                                        count = receivedSched,
                                                        label = "Received",
                                                        icon = Icons.Default.CheckCircle,
                                                        onClick = {
                                                            statusFilterTitle = "Received Deliveries ($receivedSched)"
                                                            statusFilterScheduledItems = monthScheduledList.filter { it.scheduled.status.equals("Received", ignoreCase = true) }
                                                            statusFilterSubItems = null
                                                        },
                                                        onZeroClick = { discreteToastMessage = it },
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            } else {
                                                // Status color indicators for current year
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    MiniStatusDot(
                                                        color = Color(0xFFEF4444),
                                                        count = yearSkippedSched,
                                                        label = "Skipped",
                                                        icon = Icons.Default.Block,
                                                        onClick = {
                                                            statusFilterTitle = "Skipped Deliveries ($yearSkippedSched)"
                                                            statusFilterScheduledItems = yearScheduledList.filter { it.scheduled.status.equals("Skipped", ignoreCase = true) }
                                                            statusFilterSubItems = null
                                                        },
                                                        onZeroClick = { discreteToastMessage = it },
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    MiniStatusDot(
                                                        color = Color(0xFFFF9800),
                                                        count = yearUpcomingSched,
                                                        label = "Upcoming",
                                                        icon = Icons.Default.Schedule,
                                                        onClick = {
                                                            statusFilterTitle = "Upcoming Deliveries ($yearUpcomingSched)"
                                                            statusFilterScheduledItems = yearScheduledList.filter { it.scheduled.status.equals("Upcoming", ignoreCase = true) }
                                                            statusFilterSubItems = null
                                                        },
                                                        onZeroClick = { discreteToastMessage = it },
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    MiniStatusDot(
                                                        color = Color(0xFF8B5CF6),
                                                        count = yearRenewedSched,
                                                        label = "Renewed",
                                                        icon = Icons.Default.Payments,
                                                        onClick = {
                                                            statusFilterTitle = "Renewed Deliveries ($yearRenewedSched)"
                                                            statusFilterScheduledItems = yearScheduledList.filter { it.scheduled.status.equals("Renewed", ignoreCase = true) || it.scheduled.status.equals("Paid", ignoreCase = true) }
                                                            statusFilterSubItems = null
                                                        },
                                                        onZeroClick = { discreteToastMessage = it },
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    MiniStatusDot(
                                                        color = Color(0xFF2196F3),
                                                        count = yearShippedSched,
                                                        label = "Shipped",
                                                        icon = Icons.Default.LocalShipping,
                                                        onClick = {
                                                            statusFilterTitle = "Shipped Deliveries ($yearShippedSched)"
                                                            statusFilterScheduledItems = yearScheduledList.filter { it.scheduled.status.equals("Shipped", ignoreCase = true) }
                                                            statusFilterSubItems = null
                                                        },
                                                        onZeroClick = { discreteToastMessage = it },
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    MiniStatusDot(
                                                        color = Color(0xFF4CAF50),
                                                        count = yearReceivedSched,
                                                        label = "Received",
                                                        icon = Icons.Default.CheckCircle,
                                                        onClick = {
                                                            statusFilterTitle = "Received Deliveries ($yearReceivedSched)"
                                                            statusFilterScheduledItems = yearScheduledList.filter { it.scheduled.status.equals("Received", ignoreCase = true) }
                                                            statusFilterSubItems = null
                                                        },
                                                        onZeroClick = { discreteToastMessage = it },
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                } else {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "This year",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )

                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(if (topActiveSubsPagerState.currentPage == 0) 7.dp else 5.dp)
                                                        .clip(CircleShape)
                                                        .background(if (topActiveSubsPagerState.currentPage == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                                                        .clickable { coroutineScope.launch { topActiveSubsPagerState.animateScrollToPage(0) } }
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .size(if (topActiveSubsPagerState.currentPage == 1) 7.dp else 5.dp)
                                                        .clip(CircleShape)
                                                        .background(if (topActiveSubsPagerState.currentPage == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                                                        .clickable { coroutineScope.launch { topActiveSubsPagerState.animateScrollToPage(1) } }
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF4CAF50)))
                                                Text("Renewed", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFEF4444)))
                                                Text("Skipped", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFFF9800)))
                                                Text("Upcoming", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        if (subGraphDataList.isEmpty()) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(85.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "No subscriptions",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                )
                                            }
                                        } else {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(85.dp),
                                                horizontalArrangement = Arrangement.SpaceEvenly,
                                                verticalAlignment = Alignment.Bottom
                                            ) {
                                                subGraphDataList.take(6).forEach { subData ->
                                                    Column(
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.Bottom,
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Row(
                                                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                            verticalAlignment = Alignment.Bottom
                                                        ) {
                                                            // 1. Other status bar
                                                            val hOther = if (subData.countOther > 0) {
                                                                (subData.countOther.toFloat() / maxSubGraphCount * 50).dp.coerceAtLeast(6.dp)
                                                            } else 2.dp
                                                            Box(
                                                                modifier = Modifier
                                                                    .width(5.dp)
                                                                    .height(hOther)
                                                                    .clip(RoundedCornerShape(3.dp))
                                                                    .background(if (subData.countOther > 0) Color(0xFF4CAF50) else Color(0xFF4CAF50).copy(alpha = 0.2f))
                                                                    .clickable {
                                                                        discreteToastMessage = "Total Renewed: ${subData.countOther}"
                                                                    }
                                                            )

                                                            // 2. Skipped status bar
                                                            val hSkipped = if (subData.countSkipped > 0) {
                                                                (subData.countSkipped.toFloat() / maxSubGraphCount * 50).dp.coerceAtLeast(6.dp)
                                                            } else 2.dp
                                                            Box(
                                                                modifier = Modifier
                                                                    .width(5.dp)
                                                                    .height(hSkipped)
                                                                    .clip(RoundedCornerShape(3.dp))
                                                                    .background(if (subData.countSkipped > 0) Color(0xFFEF4444) else Color(0xFFEF4444).copy(alpha = 0.2f))
                                                                    .clickable {
                                                                        discreteToastMessage = "Total Skipped: ${subData.countSkipped}"
                                                                    }
                                                            )

                                                            // 3. Upcoming status bar
                                                            val hUpcoming = if (subData.countUpcoming > 0) {
                                                                (subData.countUpcoming.toFloat() / maxSubGraphCount * 50).dp.coerceAtLeast(6.dp)
                                                            } else 2.dp
                                                            Box(
                                                                modifier = Modifier
                                                                    .width(5.dp)
                                                                    .height(hUpcoming)
                                                                    .clip(RoundedCornerShape(3.dp))
                                                                    .background(if (subData.countUpcoming > 0) Color(0xFFFF9800) else Color(0xFFFF9800).copy(alpha = 0.2f))
                                                                    .clickable {
                                                                        discreteToastMessage = "Total Upcoming: ${subData.countUpcoming}"
                                                                    }
                                                            )
                                                        }

                                                        Spacer(modifier = Modifier.height(6.dp))

                                                        Box(
                                                            modifier = Modifier
                                                                .rotate(-35f),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = if (subData.title.length > 7) subData.title.take(6) + "…" else subData.title,
                                                                fontSize = 8.sp,
                                                                fontWeight = FontWeight.Medium,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                                                maxLines = 1,
                                                                softWrap = false
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bento Row 3: Upcoming Timeline Section Header
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Upcoming Timeline",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Next 5 events",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Bento Row 4: Upcoming Timeline Items (the bento-styled list)
            if (upcomingEvents.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoStories,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "All clear on the horizon!",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "No pending scheduled subscription renewals or upcoming preorders detected.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(upcomingEvents) { event ->
                    BentoTimelineItem(event = event, currency = currency) {
                        if (event.type == "preorder" && event.rawPreorder != null) {
                            selectedPreorderForEdit = event.rawPreorder
                        } else if (event.type == "scheduled" && event.rawScheduled != null) {
                            selectedScheduledForEdit = event.rawScheduled
                        } else {
                            selectedStatType = event.type
                            showDetailsSheet = true
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Details Popup Dialog / Bottom Sheet
        if (showDetailsSheet && selectedStatType != null) {
            val title = when (selectedStatType) {
                "scheduled" -> "Scheduled Deliveries"
                "subscriptions" -> "Subscription Services"
                else -> "Pre-ordered Books"
            }

            AlertDialog(
                onDismissRequest = {
                    showDetailsSheet = false
                    selectedStatType = null
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when (selectedStatType) {
                                "scheduled" -> Icons.Default.Schedule
                                "subscriptions" -> Icons.Default.Payments
                                else -> Icons.Default.Bookmark
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(text = title, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                        Text(
                            text = "All items grouped by date:",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                        when (selectedStatType) {
                            "scheduled" -> {
                                val grouped = scheduled.groupBy { dateFormat.format(Date(it.scheduled.dueDate)) }
                                val sortedDates = grouped.keys.sorted()

                                if (sortedDates.isEmpty()) {
                                    Text("No deliveries registered.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                } else {
                                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        items(sortedDates) { date ->
                                            Column {
                                                Text(
                                                    text = formatHomeScreenDate(date),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(bottom = 4.dp)
                                                )
                                                grouped[date]?.forEach { item ->
                                                    Card(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 4.dp)
                                                            .clickable {
                                                                selectedScheduledForEdit = item
                                                                showDetailsSheet = false
                                                            },
                                                        colors = CardDefaults.cardColors(
                                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                        )
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(12.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text(item.scheduled.bookTitle, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                                                Text(item.scheduled.bookAuthor, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                            }
                                                             Box(
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(8.dp))
                                                                    .background(
                                                                        if (item.scheduled.status.equals("Skipped", ignoreCase = true)) MaterialTheme.colorScheme.errorContainer
                                                                        else MaterialTheme.colorScheme.secondaryContainer
                                                                    )
                                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                                            ) {
                                                                Text(
                                                                    text = if (item.scheduled.status.equals("Skipped", ignoreCase = true)) "Skipped" else item.scheduled.status,
                                                                    fontSize = 10.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = if (item.scheduled.status.equals("Skipped", ignoreCase = true)) MaterialTheme.colorScheme.onErrorContainer
                                                                            else MaterialTheme.colorScheme.onSecondaryContainer
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            "subscriptions" -> {
                                val grouped = subscriptions.groupBy { dateFormat.format(Date(it.subscription.dueDate)) }
                                val sortedDates = grouped.keys.sorted()

                                if (sortedDates.isEmpty()) {
                                    Text("No subscriptions found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                } else {
                                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        items(sortedDates) { date ->
                                            Column {
                                                Text(
                                                    text = "Next Billing: ${formatHomeScreenDate(date)}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.padding(bottom = 4.dp)
                                                )
                                                grouped[date]?.forEach { item ->
                                                    Card(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 4.dp),
                                                        colors = CardDefaults.cardColors(
                                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                        )
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(12.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text(item.subscription.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                                                Text(item.bookstore?.name ?: "Direct", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                            }
                                                            Text(
                                                                text = "$currency${item.subscription.price}",
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 14.sp
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            else -> {
                                val grouped = preorders.groupBy { dateFormat.format(Date(it.preorder.rangedSaleDateStart)) }
                                val sortedDates = grouped.keys.sorted()

                                if (sortedDates.isEmpty()) {
                                    Text("No preorders found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                } else {
                                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        items(sortedDates) { date ->
                                            Column {
                                                Text(
                                                    text = "Release Date: ${formatHomeScreenDate(date)}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = MaterialTheme.colorScheme.tertiary,
                                                    modifier = Modifier.padding(bottom = 4.dp)
                                                )
                                                grouped[date]?.forEach { item ->
                                                    Card(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 4.dp)
                                                            .clickable {
                                                                selectedPreorderForEdit = item
                                                                showDetailsSheet = false
                                                            },
                                                        colors = CardDefaults.cardColors(
                                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                        )
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(12.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text(item.preorder.bookTitle, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                                                Text(item.preorder.bookAuthor, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                            }
                                                            Box(
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(8.dp))
                                                                    .background(MaterialTheme.colorScheme.tertiaryContainer)
                                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                                            ) {
                                                                Text(
                                                                    text = item.preorder.status,
                                                                    fontSize = 10.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showDetailsSheet = false
                        selectedStatType = null
                    }) {
                        Text("Close")
                    }
                }
            )
        }

        // Status Filter Pop Up Dialog (Counted Items List View)
        if (statusFilterTitle != null) {
            AlertDialog(
                onDismissRequest = {
                    statusFilterTitle = null
                    statusFilterScheduledItems = null
                    statusFilterSubItems = null
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatListBulleted,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = statusFilterTitle ?: "Counted Items",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                    ) {
                        val isDark = isSystemInDarkTheme()
                        val userDateFormatPattern = viewModel.userState.collectAsState().value?.dateFormat ?: "yyyy-MM-dd"
                        val displayFormat = remember(userDateFormatPattern) { SimpleDateFormat(userDateFormatPattern, Locale.getDefault()) }
                        val schedItems = statusFilterScheduledItems
                        val subItems = statusFilterSubItems

                        if (!schedItems.isNullOrEmpty()) {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(schedItems) { item ->
                                    val statusColor = when (item.scheduled.status.lowercase()) {
                                        "upcoming" -> Color(0xFF64748B)
                                        "renewed", "paid" -> Color(0xFF2563EB)
                                        "shipped" -> Color(0xFF8B5CF6)
                                        "received" -> Color(0xFF10B981)
                                        "skipped" -> Color(0xFFEF4444)
                                        else -> Color(0xFF64748B)
                                    }
                                    val statusBgColor = when (item.scheduled.status.lowercase()) {
                                        "upcoming" -> if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9)
                                        "renewed", "paid" -> if (isDark) Color(0xFF1E3A8A) else Color(0xFFDBEAFE)
                                        "skipped" -> if (isDark) Color(0xFF7F1D1D) else Color(0xFFFEE2E2)
                                        "shipped" -> if (isDark) Color(0xFF4C1D95) else Color(0xFFF3E8FF)
                                        "received" -> if (isDark) Color(0xFF064E3B) else Color(0xFFD1FAE5)
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                    val statusTextColor = when (item.scheduled.status.lowercase()) {
                                        "upcoming" -> if (isDark) Color(0xFFF1F5F9) else Color(0xFF334155)
                                        "renewed", "paid" -> if (isDark) Color(0xFFDBEAFE) else Color(0xFF1E40AF)
                                        "skipped" -> if (isDark) Color(0xFFFEE2E2) else Color(0xFF991B1B)
                                        "shipped" -> if (isDark) Color(0xFFF3E8FF) else Color(0xFF5B21B6)
                                        "received" -> if (isDark) Color(0xFFD1FAE5) else Color(0xFF065F46)
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedScheduledForEdit = item
                                                statusFilterTitle = null
                                                statusFilterScheduledItems = null
                                                statusFilterSubItems = null
                                            },
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        ),
                                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.scheduled.bookTitle.ifEmpty { item.subscriptionType?.title ?: "Delivery Issue" },
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                if (item.scheduled.bookAuthor.isNotEmpty()) {
                                                    Text(
                                                        text = "by ${item.scheduled.bookAuthor}",
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "Due: ${displayFormat.format(Date(item.scheduled.dueDate))}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(statusBgColor)
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = item.scheduled.status,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = statusTextColor
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (!subItems.isNullOrEmpty()) {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(subItems) { item ->
                                    val subStatusBgColor = when (item.subscription.status.lowercase()) {
                                        "active" -> if (isDark) Color(0xFF1E3A8A) else Color(0xFFDBEAFE)
                                        "waitlist" -> if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9)
                                        "paused" -> if (isDark) Color(0xFF7C2D12) else Color(0xFFFFEDD5)
                                        "wishlist" -> if (isDark) Color(0xFF4C1D95) else Color(0xFFF3E8FF)
                                        "canceled", "cancelled" -> if (isDark) Color(0xFF7F1D1D) else Color(0xFFFEE2E2)
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                    val subStatusTextColor = when (item.subscription.status.lowercase()) {
                                        "active" -> if (isDark) Color(0xFFDBEAFE) else Color(0xFF1E40AF)
                                        "waitlist" -> if (isDark) Color(0xFFF1F5F9) else Color(0xFF334155)
                                        "paused" -> if (isDark) Color(0xFFFFEDD5) else Color(0xFF9A3412)
                                        "wishlist" -> if (isDark) Color(0xFFF3E8FF) else Color(0xFF5B21B6)
                                        "canceled", "cancelled" -> if (isDark) Color(0xFFFEE2E2) else Color(0xFF991B1B)
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                statusFilterTitle = null
                                                statusFilterScheduledItems = null
                                                statusFilterSubItems = null
                                                onNavigateToSubscriptions()
                                            },
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        ),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.subscription.title,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                if (item.bookstore != null) {
                                                    Text(
                                                        text = item.bookstore.name,
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Text(
                                                    text = "${currency}${String.format("%.2f", item.subscription.price)} • ${item.subscription.frequency}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(subStatusBgColor)
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = item.subscription.status,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = subStatusTextColor
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No items found for this status.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            statusFilterTitle = null
                            statusFilterScheduledItems = null
                            statusFilterSubItems = null
                        }
                    ) {
                        Text("Close")
                    }
                }
            )
        }

        // Edit Preorder Dialog
        selectedPreorderForEdit?.let { preorder ->
            EditPreorderDialog(
                preorderWithBookstore = preorder,
                viewModel = viewModel,
                bookstores = bookstores,
                onDismiss = { selectedPreorderForEdit = null }
            )
        }

        // Edit Scheduled Subscription Dialog
        selectedScheduledForEdit?.let { scheduledItem ->
            EditScheduledSubscriptionDialog(
                scheduledWithDetails = scheduledItem,
                viewModel = viewModel,
                onDismiss = { selectedScheduledForEdit = null }
            )
        }

        // Grouped Events Detail Popup
        groupedEventsPopupData?.let { popupData ->
            AlertDialog(
                onDismissRequest = { groupedEventsPopupData = null },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = popupData.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                    ) {
                        val isDark = isSystemInDarkTheme()
                        val userDateFormatPattern = viewModel.userState.collectAsState().value?.dateFormat ?: "yyyy-MM-dd"
                        val displayFormat = remember(userDateFormatPattern) { SimpleDateFormat(userDateFormatPattern, Locale.getDefault()) }

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(popupData.events) { event ->
                                val statusLower = event.status.lowercase()
                                val statusColor = when (statusLower) {
                                    "upcoming" -> Color(0xFF64748B)
                                    "renewed", "paid", "preordered" -> Color(0xFF2563EB)
                                    "released" -> Color(0xFFEA580C)
                                    "skipped" -> Color(0xFFEF4444)
                                    "shipped" -> Color(0xFF8B5CF6)
                                    "received" -> Color(0xFF10B981)
                                    else -> Color(0xFF64748B)
                                }
                                val statusBgColor = when (statusLower) {
                                    "upcoming" -> if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9)
                                    "renewed", "paid", "preordered" -> if (isDark) Color(0xFF1E3A8A) else Color(0xFFDBEAFE)
                                    "released" -> if (isDark) Color(0xFF7C2D12) else Color(0xFFFFEDD5)
                                    "skipped" -> if (isDark) Color(0xFF7F1D1D) else Color(0xFFFEE2E2)
                                    "shipped" -> if (isDark) Color(0xFF4C1D95) else Color(0xFFF3E8FF)
                                    "received" -> if (isDark) Color(0xFF064E3B) else Color(0xFFD1FAE5)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                                val statusTextColor = when (statusLower) {
                                    "upcoming" -> if (isDark) Color(0xFFF1F5F9) else Color(0xFF334155)
                                    "renewed", "paid", "preordered" -> if (isDark) Color(0xFFDBEAFE) else Color(0xFF1E40AF)
                                    "released" -> if (isDark) Color(0xFFFFEDD5) else Color(0xFF9A3412)
                                    "skipped" -> if (isDark) Color(0xFFFEE2E2) else Color(0xFF991B1B)
                                    "shipped" -> if (isDark) Color(0xFFF3E8FF) else Color(0xFF5B21B6)
                                    "received" -> if (isDark) Color(0xFFD1FAE5) else Color(0xFF065F46)
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            groupedEventsPopupData = null
                                            if (event.type == "preorder" && event.rawPreorder != null) {
                                                selectedPreorderForEdit = event.rawPreorder
                                            } else if (event.type == "scheduled" && event.rawScheduled != null) {
                                                selectedScheduledForEdit = event.rawScheduled
                                            }
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = event.title.ifBlank { event.subTitle },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (event.author.isNotBlank()) {
                                                Text(
                                                    text = "by ${event.author}" + if (event.subTitle.isNotBlank()) " • ${event.subTitle}" else "",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            } else if (event.subTitle.isNotBlank()) {
                                                Text(
                                                    text = event.subTitle,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            val rH = event.reminderHour
                                            val rM = event.reminderMinute
                                            val timeStr = if (rH != null && rM != null) String.format("%d:%02d", rH, rM) else null
                                            val dateLabel = displayFormat.format(Date(event.startDate))
                                            Text(
                                                text = if (timeStr != null) "Due: $dateLabel at $timeStr" else "Due: $dateLabel",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(statusBgColor)
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = event.status.ifEmpty { if (event.type == "preorder") "Preorder" else "Scheduled" },
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = statusTextColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { groupedEventsPopupData = null }) {
                        Text("Close")
                    }
                }
            )
        }

        // Discrete Toast Helper Pill
        AnimatedVisibility(
            visible = discreteToastMessage != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        ) {
            discreteToastMessage?.let { msg ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    shadowElevation = 6.dp
                ) {
                    Text(
                        text = msg,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}
}

data class GroupedEventsPopupData(
    val title: String,
    val events: List<UpcomingEvent>
)

data class UpcomingEvent(
    val id: String,
    val title: String,
    val author: String,
    val date: Long,
    val startDate: Long = date,
    val endDate: Long = date,
    val type: String, // "scheduled" or "preorder"
    val status: String,
    val isSkipped: Boolean = false,
    val subTitle: String = "", // subscription type name or bookstore name
    val price: Double? = null,
    val rawScheduled: ScheduledWithDetails? = null,
    val rawPreorder: PreorderWithBookstore? = null,
    val reminderHour: Int? = null,
    val reminderMinute: Int? = null,
    val hasTime: Boolean = false
)

@Composable
fun MiniStatusDot(
    color: Color,
    count: Int,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: (() -> Unit)? = null,
    onZeroClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .then(
                if (onClick != null || onZeroClick != null) {
                    Modifier.clickable {
                        if (count > 0) {
                            onClick?.invoke()
                        } else {
                            onZeroClick?.invoke(label)
                        }
                    }
                } else Modifier
            )
            .padding(vertical = 5.dp, horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(15.dp),
                    tint = color
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(color)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$count",
                fontSize = 11.sp,
                lineHeight = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    platformStyle = PlatformTextStyle(
                        includeFontPadding = false
                    )
                )
            )
        }
    }
}

@Composable
private fun CalendarBoxEventItem(
    event: UpcomingEvent,
    targetDayStart: Long,
    userDisplayAmounts: Boolean,
    currency: String,
    onClick: (() -> Unit)? = null
) {
    val displayTitle = event.title.ifBlank { event.subTitle }
    val priceStr = if (userDisplayAmounts && event.price != null && event.price > 0) " (${currency}${String.format("%.2f", event.price)})" else ""

    val barColor = MaterialTheme.colorScheme.onPrimary
    val titleColor = MaterialTheme.colorScheme.onPrimary
    val subColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f)
    val containerBg = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f)

    fun getDayStartMs(ts: Long): Long {
        val c = Calendar.getInstance().apply {
            timeInMillis = ts
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return c.timeInMillis
    }

    val eventStartDay = getDayStartMs(event.startDate)
    val eventEndDay = getDayStartMs(event.endDate)
    val hasTime = event.hasTime || event.reminderHour != null
    val reminderH = event.reminderHour ?: 8
    val reminderM = event.reminderMinute ?: 0
    val timeStr = String.format("%d:%02d", reminderH, reminderM)

    val displaySubtitle = if (eventStartDay == eventEndDay) {
        if (hasTime) "Starts at $timeStr" else "All day"
    } else {
        when (targetDayStart) {
            eventStartDay -> if (hasTime) "Starts at $timeStr" else "Starts at 8:00"
            eventEndDay -> if (hasTime) "Until $timeStr" else "Until 20:00"
            else -> "All day"
        }
    }

    val fullTitle = "$displayTitle$priceStr"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(containerBg)
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick.invoke() }
                } else Modifier
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .fillMaxHeight()
                    .padding(vertical = 1.5.dp)
                    .background(
                        barColor,
                        shape = RoundedCornerShape(1.dp)
                    )
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = fullTitle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.Both
                        )
                    )
                )
                Text(
                    text = displaySubtitle,
                    fontSize = 9.5.sp,
                    color = subColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.Both
                        )
                    )
                )
            }
        }
    }
}

@Composable
fun BentoTimelineItem(
    event: UpcomingEvent,
    currency: String,
    onClick: () -> Unit
) {
    val dateFormatMonth = remember { SimpleDateFormat("MMM", Locale.getDefault()) }
    val dateFormatDay = remember { SimpleDateFormat("dd", Locale.getDefault()) }
    val eventDate = Date(event.date)
    val monthStr = dateFormatMonth.format(eventDate).uppercase()
    val dayStr = dateFormatDay.format(eventDate)

    val accentColor = if (event.type == "scheduled") {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.tertiary
    }

    val isDark = isSystemInDarkTheme()
    val statusLower = event.status.lowercase()

    val statusBgColor = when (statusLower) {
        "upcoming" -> if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9)
        "renewed", "paid", "preordered" -> if (isDark) Color(0xFF1E3A8A) else Color(0xFFDBEAFE)
        "released" -> if (isDark) Color(0xFF7C2D12) else Color(0xFFFFEDD5)
        "skipped" -> if (isDark) Color(0xFF7F1D1D) else Color(0xFFFEE2E2)
        "shipped" -> if (isDark) Color(0xFF4C1D95) else Color(0xFFF3E8FF)
        "received" -> if (isDark) Color(0xFF064E3B) else Color(0xFFD1FAE5)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val statusTextColor = when (statusLower) {
        "upcoming" -> if (isDark) Color(0xFFF1F5F9) else Color(0xFF334155)
        "renewed", "paid", "preordered" -> if (isDark) Color(0xFFDBEAFE) else Color(0xFF1E40AF)
        "released" -> if (isDark) Color(0xFFFFEDD5) else Color(0xFF9A3412)
        "skipped" -> if (isDark) Color(0xFFFEE2E2) else Color(0xFF991B1B)
        "shipped" -> if (isDark) Color(0xFFF3E8FF) else Color(0xFF5B21B6)
        "received" -> if (isDark) Color(0xFFD1FAE5) else Color(0xFF065F46)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val statusIcon = when (statusLower) {
        "upcoming" -> if (event.type == "scheduled") Icons.Default.Schedule else Icons.Default.ShoppingBag
        "preordered" -> Icons.Default.ShoppingBag
        "renewed", "paid" -> Icons.Default.Payments
        "released" -> Icons.Default.NewReleases
        "shipped" -> Icons.Default.LocalShipping
        "received" -> Icons.Default.CheckCircle
        "skipped" -> Icons.Default.Block
        else -> Icons.Default.Info
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
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
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Text Info Column with Left Indicator Bar drawn on it
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                        .drawWithContent {
                            drawContent()
                            drawRoundRect(
                                color = accentColor,
                                topLeft = Offset(-12.dp.toPx(), 0f),
                                size = Size(4.dp.toPx(), size.height),
                                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                            )
                        }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Mini chip representing the category
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(accentColor.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (event.type == "scheduled") "Subscription" else "Preorder",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                        }

                        // Mini status badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(statusBgColor)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = statusIcon,
                                    contentDescription = null,
                                    modifier = Modifier.size(10.dp),
                                    tint = statusTextColor
                                )
                                Text(
                                    text = event.status,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = statusTextColor
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = event.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )

                    Text(
                        text = "By ${event.author} • ${event.subTitle}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Calendar-style Date Badge
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = accentColor.copy(alpha = 0.08f)
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.2f)),
                modifier = Modifier.width(60.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(vertical = 6.dp, horizontal = 2.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = monthStr,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = accentColor,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = dayStr,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 18.sp
                    )
                    if (event.type == "preorder") {
                        val startCal = Calendar.getInstance().apply { timeInMillis = event.date }
                        val formattedTime = String.format(
                            Locale.getDefault(),
                            "%02d:%02d",
                            startCal.get(Calendar.HOUR_OF_DAY),
                            startCal.get(Calendar.MINUTE)
                        )
                        Text(
                            text = formattedTime,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatHomeScreenDate(dateStr: String): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return try {
        val date = sdf.parse(dateStr)
        if (date != null) {
            formatRelativeDate(date.time, isHeader = false)
        } else {
            dateStr
        }
    } catch (e: Exception) {
        dateStr
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
