package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

data class CalendarMarker(
    val id: String,
    val title: String,
    val color: Color
)

@Composable
fun MonthCalendar(
    modifier: Modifier = Modifier,
    markerDates: Map<String, List<CalendarMarker>>, // Date string in "yyyy-MM-dd" format to markers
    onDayClick: (String, List<CalendarMarker>) -> Unit
) {
    var calendarInstance by remember { mutableStateOf(Calendar.getInstance()) }
    val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val dateFormatKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    val currentMonthYear = monthYearFormat.format(calendarInstance.time)

    // Calculate days for the grid
    val daysInMonth = calendarInstance.getActualMaximum(Calendar.DAY_OF_MONTH)
    
    val tempCal = calendarInstance.clone() as Calendar
    tempCal.set(Calendar.DAY_OF_MONTH, 1)
    val firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK) // 1 = Sunday, 2 = Monday, etc.

    // Adjust firstDayOfWeek for standard Mon-Sun grid (or Sun-Sat). Let's use Sunday-first grid for simplicity.
    val leadingEmptyDays = firstDayOfWeek - 1

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Calendar Header: Month + Nav Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val newCal = calendarInstance.clone() as Calendar
                        newCal.add(Calendar.MONTH, -1)
                        calendarInstance = newCal
                    },
                    modifier = Modifier.testTag("cal_prev_month")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous Month",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = currentMonthYear,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = {
                        val newCal = calendarInstance.clone() as Calendar
                        newCal.add(Calendar.MONTH, 1)
                        calendarInstance = newCal
                    },
                    modifier = Modifier.testTag("cal_next_month")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next Month",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Weekday Headers
            val weekDays = listOf("S", "M", "T", "W", "T", "F", "S")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                weekDays.forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Days Grid
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
                            // Empty placeholder cell
                            Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                        } else {
                            // Setup date key for checking markers
                            val dateCal = calendarInstance.clone() as Calendar
                            dateCal.set(Calendar.DAY_OF_MONTH, dayNumber)
                            val dateStr = dateFormatKey.format(dateCal.time)
                            val markersForDay = markerDates[dateStr] ?: emptyList()

                            val isToday = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) == dateStr

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isToday) MaterialTheme.colorScheme.primaryContainer
                                        else Color.Transparent
                                    )
                                    .clickable {
                                        onDayClick(dateStr, markersForDay)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        text = dayNumber.toString(),
                                        fontSize = 14.sp,
                                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isToday) MaterialTheme.colorScheme.onPrimaryContainer
                                                else MaterialTheme.colorScheme.onSurface
                                    )

                                    if (markersForDay.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        // Dots Row
                                        Row(
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            markersForDay.take(3).forEach { marker ->
                                                Box(
                                                    modifier = Modifier
                                                        .size(5.dp)
                                                        .padding(horizontal = 0.5.dp)
                                                        .clip(CircleShape)
                                                        .background(marker.color)
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
