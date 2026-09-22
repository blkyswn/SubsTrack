package com.example.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.*

data class CalendarMarker(
    val id: String,
    val title: String,
    val color: Color,
    val imageUrl: String? = null
)

@Composable
fun MonthCalendar(
    modifier: Modifier = Modifier,
    markerDates: Map<String, List<CalendarMarker>>, // Date string in "yyyy-MM-dd" format to markers
    selectedDateStr: String? = null,
    onDayClick: (String, List<CalendarMarker>) -> Unit,
    lazyListState: LazyListState? = null,
    initiallyCollapsed: Boolean = false
) {
    val dateFormatKey = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val todayStr = remember { dateFormatKey.format(Date()) }

    var currentSelectedDateStr by remember(selectedDateStr) {
        mutableStateOf(selectedDateStr ?: todayStr)
    }

    val selectedCalendar = remember(currentSelectedDateStr) {
        Calendar.getInstance().apply {
            try {
                val parsed = dateFormatKey.parse(currentSelectedDateStr)
                if (parsed != null) time = parsed
            } catch (_: Exception) {}
        }
    }

    var calendarMonth by remember {
        mutableStateOf(
            Calendar.getInstance().apply {
                try {
                    val parsed = dateFormatKey.parse(currentSelectedDateStr)
                    if (parsed != null) time = parsed
                } catch (_: Exception) {}
            }
        )
    }

    val isListScrolled by remember {
        derivedStateOf {
            if (lazyListState == null) false
            else (lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 30)
        }
    }

    var manualCollapsedState by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(isListScrolled) {
        if (!isListScrolled) {
            manualCollapsedState = null
        }
    }

    val isCollapsed = manualCollapsedState ?: (isListScrolled || initiallyCollapsed)

    val monthYearFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val fullDateFormat = remember { SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()) }

    val headerText = if (isCollapsed) {
        fullDateFormat.format(selectedCalendar.time)
    } else {
        monthYearFormat.format(calendarMonth.time)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .animateContentSize(
                animationSpec = spring(
                    stiffness = Spring.StiffnessMediumLow
                )
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Calendar Header: Nav Buttons + Selected Date / Month
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (isCollapsed) {
                            val newCal = selectedCalendar.clone() as Calendar
                            newCal.add(Calendar.DAY_OF_MONTH, -7)
                            val newStr = dateFormatKey.format(newCal.time)
                            currentSelectedDateStr = newStr
                            calendarMonth = newCal.clone() as Calendar
                            onDayClick(newStr, markerDates[newStr] ?: emptyList())
                        } else {
                            val newCal = calendarMonth.clone() as Calendar
                            newCal.add(Calendar.MONTH, -1)
                            calendarMonth = newCal
                        }
                    },
                    modifier = Modifier.size(36.dp).testTag("cal_prev")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = headerText,
                    fontSize = if (isCollapsed) 15.sp else 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = {
                        if (isCollapsed) {
                            val newCal = selectedCalendar.clone() as Calendar
                            newCal.add(Calendar.DAY_OF_MONTH, 7)
                            val newStr = dateFormatKey.format(newCal.time)
                            currentSelectedDateStr = newStr
                            calendarMonth = newCal.clone() as Calendar
                            onDayClick(newStr, markerDates[newStr] ?: emptyList())
                        } else {
                            val newCal = calendarMonth.clone() as Calendar
                            newCal.add(Calendar.MONTH, 1)
                            calendarMonth = newCal
                        }
                    },
                    modifier = Modifier.size(36.dp).testTag("cal_next")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = { manualCollapsedState = !isCollapsed },
                    modifier = Modifier.size(36.dp).testTag("cal_toggle_collapse")
                ) {
                    Icon(
                        imageVector = if (isCollapsed) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                        contentDescription = if (isCollapsed) "Expand Calendar" else "Collapse Calendar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Weekday Headers
            val weekDays = listOf("S", "M", "T", "W", "T", "F", "S")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weekDays.forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (isCollapsed) {
                // COLLAPSED MODE: Single week row for selected date's week
                val weekStartCal = selectedCalendar.clone() as Calendar
                val dayOfWeek = weekStartCal.get(Calendar.DAY_OF_WEEK) // 1 = Sun, 2 = Mon ...
                weekStartCal.add(Calendar.DAY_OF_MONTH, -(dayOfWeek - 1))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (c in 0 until 7) {
                        val dayCal = weekStartCal.clone() as Calendar
                        dayCal.add(Calendar.DAY_OF_MONTH, c)
                        val dateStr = dateFormatKey.format(dayCal.time)
                        val dayNum = dayCal.get(Calendar.DAY_OF_MONTH)
                        val markersForDay = markerDates[dateStr] ?: emptyList()

                        val isToday = dateStr == todayStr
                        val isSelected = dateStr == currentSelectedDateStr

                        CalendarDayCell(
                            dayNumber = dayNum,
                            isToday = isToday,
                            isSelected = isSelected,
                            markers = markersForDay,
                            onClick = {
                                currentSelectedDateStr = dateStr
                                onDayClick(dateStr, markersForDay)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else {
                // EXPANDED MODE: Full month grid
                val daysInMonth = calendarMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
                val tempCal = calendarMonth.clone() as Calendar
                tempCal.set(Calendar.DAY_OF_MONTH, 1)
                val firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK)
                val leadingEmptyDays = firstDayOfWeek - 1

                val totalCells = leadingEmptyDays + daysInMonth
                val rows = (totalCells + 6) / 7

                for (r in 0 until rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (c in 0 until 7) {
                            val cellIndex = r * 7 + c
                            val dayNumber = cellIndex - leadingEmptyDays + 1

                            if (cellIndex < leadingEmptyDays || dayNumber > daysInMonth) {
                                Spacer(modifier = Modifier.weight(1f).aspectRatio(0.72f))
                            } else {
                                val dateCal = calendarMonth.clone() as Calendar
                                dateCal.set(Calendar.DAY_OF_MONTH, dayNumber)
                                val dateStr = dateFormatKey.format(dateCal.time)
                                val markersForDay = markerDates[dateStr] ?: emptyList()

                                val isToday = dateStr == todayStr
                                val isSelected = dateStr == currentSelectedDateStr

                                CalendarDayCell(
                                    dayNumber = dayNumber,
                                    isToday = isToday,
                                    isSelected = isSelected,
                                    markers = markersForDay,
                                    onClick = {
                                        currentSelectedDateStr = dateStr
                                        onDayClick(dateStr, markersForDay)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Bottom pill handle
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    .clickable { manualCollapsedState = !isCollapsed }
            )
        }
    }
}

@Composable
private fun CalendarDayCell(
    dayNumber: Int,
    isToday: Boolean,
    isSelected: Boolean,
    markers: List<CalendarMarker>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasMarkers = markers.isNotEmpty()
    val isDark = isSystemInDarkTheme()

    val activeBgColor = if (isDark) {
        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
    } else {
        Color(0xFFFF80AB).copy(alpha = 0.22f)
    }

    val cellBgColor = when {
        hasMarkers -> activeBgColor
        isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
    }

    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .aspectRatio(0.72f)
            .padding(1.5.dp)
            .clip(shape)
            .background(cellBgColor)
            .then(
                if (isSelected) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape)
                } else if (hasMarkers) {
                    Modifier.border(0.5.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f), shape)
                } else {
                    Modifier
                }
            )
            .clickable { onClick() },
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 3.dp, bottom = 3.dp, start = 1.dp, end = 1.dp)
        ) {
            // Day Number Header
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .then(
                        if (isToday) {
                            Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = dayNumber.toString(),
                    fontSize = 11.sp,
                    fontWeight = if (hasMarkers || isToday || isSelected) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(
                            includeFontPadding = false
                        )
                    ),
                    color = when {
                        isToday -> MaterialTheme.colorScheme.onPrimary
                        isSelected -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                    }
                )
            }

            // Image container / Stacked pictures
            if (hasMarkers) {
                val maxShow = 3
                val displayMarkers = markers.take(maxShow)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(bottom = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    displayMarkers.forEachIndexed { index, marker ->
                        val count = displayMarkers.size
                        val offsetX = when (count) {
                            1 -> 0.dp
                            2 -> if (index == 0) (-2.5).dp else 2.5.dp
                            else -> when (index) {
                                0 -> (-3.5).dp
                                1 -> 0.dp
                                else -> 3.5.dp
                            }
                        }
                        val offsetY = when (count) {
                            1 -> 0.dp
                            2 -> if (index == 0) (-2).dp else 2.dp
                            else -> when (index) {
                                0 -> (-2.5).dp
                                1 -> 0.dp
                                else -> 2.5.dp
                            }
                        }

                        BookCoverThumbnail(
                            imageUrl = marker.imageUrl,
                            title = marker.title,
                            accentColor = marker.color,
                            modifier = Modifier
                                .offset(x = offsetX, y = offsetY)
                                .width(20.dp)
                                .height(28.dp)
                                .shadow(elevation = (index + 1).dp, shape = RoundedCornerShape(3.dp))
                                .clip(RoundedCornerShape(3.dp))
                                .border(
                                    width = 0.8.dp,
                                    color = if (isDark) Color(0xFF333333) else Color.White,
                                    shape = RoundedCornerShape(3.dp)
                                )
                        )
                    }

                    if (markers.size > maxShow) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = (-4).dp, y = (-1).dp)
                                .shadow(1.dp, CircleShape)
                                .background(MaterialTheme.colorScheme.tertiary, CircleShape)
                                .border(0.5.dp, if (isDark) Color(0xFF333333) else Color.White, CircleShape)
                                .padding(horizontal = 3.dp, vertical = 1.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+${markers.size - maxShow}",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 8.5.sp,
                                style = TextStyle(
                                    platformStyle = PlatformTextStyle(
                                        includeFontPadding = false
                                    )
                                ),
                                color = MaterialTheme.colorScheme.onTertiary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun BookCoverThumbnail(
    imageUrl: String?,
    title: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    if (!imageUrl.isNullOrBlank()) {
        AsyncImage(
            model = imageUrl,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        val firstChar = title.trim().firstOrNull()?.uppercase() ?: "B"
        Box(
            modifier = modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            accentColor,
                            accentColor.copy(alpha = 0.75f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Book,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(10.dp)
                )
                Text(
                    text = firstChar,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}


