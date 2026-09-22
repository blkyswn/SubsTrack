package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun InlineReminderSelector(
    reminderDDayOffset: Int? = null,
    onDDayOffsetChange: ((Int) -> Unit)? = null,
    reminderHour: Int,
    onHourChange: (Int) -> Unit,
    reminderMinute: Int,
    onMinuteChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                val timeLabel = String.format("%02d:%02d", reminderHour, reminderMinute)
                val ddayLabel = if (reminderDDayOffset != null) {
                    if (reminderDDayOffset == 0) "D-Day" else "$reminderDDayOffset days before"
                } else null

                val headerText = if (ddayLabel != null) "Alert on $ddayLabel at $timeLabel" else "Delivery Time: $timeLabel"

                Text(
                    text = headerText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (reminderDDayOffset != null && onDDayOffsetChange != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Alert Day",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        RevolverWheelPicker(
                            items = (0..7).toList(),
                            selectedItem = reminderDDayOffset,
                            onItemSelected = onDDayOffsetChange,
                            modifier = Modifier.width(95.dp),
                            label = { dday -> if (dday == 0) "D-Day" else "$dday days before" }
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Hour",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    RevolverWheelPicker(
                        items = (0..23).toList(),
                        selectedItem = reminderHour,
                        onItemSelected = onHourChange,
                        modifier = Modifier.width(55.dp),
                        label = { hr -> String.format("%02d", hr) }
                    )
                }

                Text(
                    text = ":",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Min",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    RevolverWheelPicker(
                        items = (0..59).toList(),
                        selectedItem = reminderMinute,
                        onItemSelected = onMinuteChange,
                        modifier = Modifier.width(55.dp),
                        label = { mn -> String.format("%02d", mn) }
                    )
                }
            }
        }
    }
}

@Composable
fun <T> RevolverWheelPicker(
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

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialListIndex
    )
    val flingBehavior = rememberSnapFlingBehavior(
        lazyListState = listState,
        snapPosition = SnapPosition.Center
    )

    LaunchedEffect(selectedItem) {
        val targetSelIdx = items.indexOf(selectedItem).coerceAtLeast(0)
        val currentListIdx = listState.firstVisibleItemIndex
        val currentSelIdx = currentListIdx % items.size
        if (currentSelIdx != targetSelIdx) {
            val diff = targetSelIdx - currentSelIdx
            listState.scrollToItem(currentListIdx + diff)
        }
    }

    val isScrollInProgress = listState.isScrollInProgress
    LaunchedEffect(isScrollInProgress) {
        if (!isScrollInProgress) {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isNotEmpty()) {
                val viewportMiddle = layoutInfo.viewportStartOffset + (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset) / 2
                val closestItem = visibleItems.minByOrNull {
                    val itemMiddle = it.offset + it.size / 2
                    abs(itemMiddle - viewportMiddle)
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

        LazyColumn(
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
                            MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    )
                }
            }
        }
    }
}
