with open("app/src/main/java/com/example/ui/screens/HomeScreen.kt", "r") as f:
    content = f.read()

target_data = """data class GroupedEventsPopupData("""
replacement_data = """data class DisplayedDayGroupInfo(
    val dayStartTs: Long,
    val shownEvents: List<UpcomingEvent>,
    val leftoverEvents: List<UpcomingEvent>,
    val fullDayEvents: List<UpcomingEvent>
)

data class GroupedEventsPopupData("""

assert target_data in content, "target_data not found"
content = content.replace(target_data, replacement_data, 1)

target = """                                if (todayEvents.isNotEmpty()) {
                                    val displayedTodayEvents = todayEvents.take(2)
                                    val leftoverTodayCount = (todayEvents.size - displayedTodayEvents.size).coerceAtLeast(0)
                                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                        displayedTodayEvents.forEach { event ->
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
                                        if (leftoverTodayCount > 0) {
                                            Row(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .clickable {
                                                        groupedEventsPopupData = GroupedEventsPopupData("Today's Events (${todayEvents.size})", todayEvents)
                                                    }
                                                    .padding(vertical = 2.dp, horizontal = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                            ) {
                                                Text(
                                                    text = "+$leftoverTodayCount more",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                                                )
                                            }
                                        }
                                    }
                                }"""

replacement = """                                if (todayEvents.isNotEmpty()) {
                                    val sortedTodayEvents = remember(todayEvents) {
                                        todayEvents.sortedWith(
                                            compareByDescending<UpcomingEvent> { it.hasTime || it.reminderHour != null }
                                                .thenBy { it.reminderHour ?: 24 }
                                                .thenBy { it.reminderMinute ?: 60 }
                                        )
                                    }
                                    val displayedTodayEvents = sortedTodayEvents.take(2)
                                    val leftoverTodayEvents = sortedTodayEvents.drop(2)
                                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                        displayedTodayEvents.forEach { event ->
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
                                        if (leftoverTodayEvents.isNotEmpty()) {
                                            Row(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .clickable {
                                                        groupedEventsPopupData = GroupedEventsPopupData("Today's Events (${todayEvents.size})", todayEvents)
                                                    }
                                                    .padding(vertical = 2.dp, horizontal = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                            ) {
                                                Text(
                                                    text = "+${leftoverTodayEvents.size} more",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                                                )
                                            }
                                        }
                                    }
                                }"""

assert target in content, "target today block not found"
content = content.replace(target, replacement, 1)

target_right = """                                    val groupedFuture = futureEventsList.groupBy { event ->
                                        val startDay = getDayStart(event.startDate)
                                        if (startDay >= tomorrowStart) startDay else tomorrowStart
                                    }.toSortedMap()

                                    val maxRightItems = 2
                                    var itemsRemaining = maxRightItems
                                    val displayGroups = mutableListOf<Pair<Long, List<UpcomingEvent>>>()

                                    for ((dayStartTs, eventsForDay) in groupedFuture) {
                                        if (itemsRemaining <= 0) break
                                        val countToTake = eventsForDay.size.coerceAtMost(itemsRemaining)
                                        displayGroups.add(dayStartTs to eventsForDay.take(countToTake))
                                        itemsRemaining -= countToTake
                                    }

                                    val totalDisplayedRightCount = displayGroups.sumOf { it.second.size }
                                    val leftOverCount = (futureEventsList.size - totalDisplayedRightCount).coerceAtLeast(0)

                                    displayGroups.forEach { (dayStartTs, eventsForDay) ->
                                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                            val groupHeaderStr = when {
                                                dayStartTs == tomorrowStart -> "TOMORROW"
                                                dayStartTs < sevenDaysOutStart -> dayOfWeekFormat.format(Date(dayStartTs)).uppercase(Locale.getDefault())
                                                else -> dayMonthFormat.format(Date(dayStartTs)).uppercase(Locale.getDefault())
                                            }

                                            val fullDayEvents = groupedFuture[dayStartTs] ?: eventsForDay
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .then(
                                                        if (fullDayEvents.size > 1) {
                                                            Modifier
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .clickable {
                                                                    groupedEventsPopupData = GroupedEventsPopupData("Events for $groupHeaderStr (${fullDayEvents.size})", fullDayEvents)
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

                                                if (fullDayEvents.size > 1) {
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
                                                            text = "${fullDayEvents.size} items",
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
                                        val leftoverList = futureEventsList.drop(totalDisplayedRightCount)
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
                                                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                                                        RoundedCornerShape(2.dp)
                                                    )
                                            )
                                            Text(
                                                text = "+$leftOverCount more upcoming events",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                                            )
                                        }
                                    }"""

replacement_right = """                                    val groupedFuture = futureEventsList.groupBy { event ->
                                        val startDay = getDayStart(event.startDate)
                                        if (startDay >= tomorrowStart) startDay else tomorrowStart
                                    }.toSortedMap()

                                    val displayedDayGroups = mutableListOf<DisplayedDayGroupInfo>()
                                    var remainingItemBudget = 3

                                    for ((dayStartTs, eventsForDay) in groupedFuture) {
                                        if (remainingItemBudget <= 0) break

                                        val isFirstDay = displayedDayGroups.isEmpty()
                                        if (eventsForDay.size <= remainingItemBudget) {
                                            displayedDayGroups.add(
                                                DisplayedDayGroupInfo(
                                                    dayStartTs = dayStartTs,
                                                    shownEvents = eventsForDay,
                                                    leftoverEvents = emptyList(),
                                                    fullDayEvents = eventsForDay
                                                )
                                            )
                                            remainingItemBudget -= eventsForDay.size
                                        } else {
                                            val numToShow = if (isFirstDay) {
                                                remainingItemBudget.coerceAtMost(3)
                                            } else {
                                                1
                                            }
                                            val shown = eventsForDay.take(numToShow)
                                            val leftover = eventsForDay.drop(numToShow)
                                            displayedDayGroups.add(
                                                DisplayedDayGroupInfo(
                                                    dayStartTs = dayStartTs,
                                                    shownEvents = shown,
                                                    leftoverEvents = leftover,
                                                    fullDayEvents = eventsForDay
                                                )
                                            )
                                            remainingItemBudget = 0
                                        }
                                    }

                                    displayedDayGroups.forEach { group ->
                                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                            val groupHeaderStr = when {
                                                group.dayStartTs == tomorrowStart -> "TOMORROW"
                                                group.dayStartTs < sevenDaysOutStart -> dayOfWeekFormat.format(Date(group.dayStartTs)).uppercase(Locale.getDefault())
                                                else -> dayMonthFormat.format(Date(group.dayStartTs)).uppercase(Locale.getDefault())
                                            }

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .then(
                                                        if (group.fullDayEvents.size > 1) {
                                                            Modifier
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .clickable {
                                                                    groupedEventsPopupData = GroupedEventsPopupData("Events for $groupHeaderStr (${group.fullDayEvents.size})", group.fullDayEvents)
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

                                                if (group.fullDayEvents.size > 1) {
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
                                                            text = "${group.fullDayEvents.size} items",
                                                            fontSize = 9.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                                                        )
                                                    }
                                                }
                                            }

                                            group.shownEvents.forEach { event ->
                                                CalendarBoxEventItem(
                                                    event = event,
                                                    targetDayStart = group.dayStartTs,
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

                                            if (group.leftoverEvents.isNotEmpty()) {
                                                Row(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .clickable {
                                                            groupedEventsPopupData = GroupedEventsPopupData(
                                                                "Events for $groupHeaderStr (${group.fullDayEvents.size})",
                                                                group.fullDayEvents
                                                            )
                                                        }
                                                        .padding(vertical = 2.dp, horizontal = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                                ) {
                                                    Text(
                                                        text = "+${group.leftoverEvents.size} more",
                                                        fontSize = 10.5.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                                                    )
                                                }
                                            }
                                        }
                                    }"""

assert target_right in content, "target_right block not found"
content = content.replace(target_right, replacement_right, 1)

with open("app/src/main/java/com/example/ui/screens/HomeScreen.kt", "w") as f:
    f.write(content)

print("UPDATED HOMESCREEN SUCCESSFULLY")
