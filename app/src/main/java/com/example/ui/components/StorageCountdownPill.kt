package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

@Composable
fun StorageCountdownPill(
    forwarderReceivedDate: Long?,
    storageDays: Int?,
    modifier: Modifier = Modifier
) {
    if (forwarderReceivedDate == null) return

    val dayMs = 24 * 60 * 60 * 1000L

    val todayStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val receivedStart = remember(forwarderReceivedDate) {
        Calendar.getInstance().apply {
            timeInMillis = forwarderReceivedDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val (pillText, containerColor, contentColor) = remember(storageDays, receivedStart, todayStart) {
        if (storageDays != null && storageDays > 0) {
            val limitDate = receivedStart + (storageDays * dayMs)
            val daysLeft = ((limitDate - todayStart) / dayMs).toInt()
            when {
                daysLeft < 0 -> {
                    Triple("Expired", Color(0xFFDC2626), Color.White)
                }
                daysLeft == 0 -> {
                    Triple("0 days left", Color(0xFFD97706), Color.White)
                }
                daysLeft == 1 -> {
                    Triple("1 day left", Color(0xFFD97706), Color.White)
                }
                daysLeft in 2..5 -> {
                    Triple("$daysLeft days left", Color(0xFFD97706), Color.White)
                }
                else -> {
                    Triple("$daysLeft days left", Color(0xFF0F172A).copy(alpha = 0.88f), Color.White)
                }
            }
        } else {
            val daysStored = ((todayStart - receivedStart) / dayMs).toInt().coerceAtLeast(0)
            val text = if (daysStored == 1) "1 day stored" else "$daysStored days stored"
            Triple(text, Color(0xFF0369A1).copy(alpha = 0.90f), Color.White)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 3.dp)
            .padding(bottom = 4.dp)
            .testTag("storage_countdown_pill"),
        shape = RoundedCornerShape(4.dp),
        color = containerColor,
        shadowElevation = 1.5.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 3.dp, vertical = 0.5.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = pillText,
                fontSize = 8.sp,
                lineHeight = 10.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
