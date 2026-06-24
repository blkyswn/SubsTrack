package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.viewmodel.BookishViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: BookishViewModel,
    modifier: Modifier = Modifier
) {
    val user by viewModel.userState.collectAsState()
    val scheduled by viewModel.rawScheduledState.collectAsState()
    val subscriptions by viewModel.rawSubscriptionsState.collectAsState()
    val preorders by viewModel.rawPreordersState.collectAsState()

    val currency = user?.currency ?: "$"

    // Bottom sheet / Dialog details state
    var selectedStatType by remember { mutableStateOf<String?>(null) } // "scheduled", "subscriptions", "preorders"
    var showDetailsSheet by remember { mutableStateOf(false) }

    val upcomingEvents = remember(scheduled, preorders) {
        val scheduledEvents = scheduled.map {
            UpcomingEvent(
                id = it.scheduled.id.toString(),
                title = it.scheduled.bookTitle,
                author = it.scheduled.bookAuthor,
                date = it.scheduled.dueDate,
                type = "scheduled",
                status = it.scheduled.status,
                isSkipped = it.scheduled.isSkipped,
                subTitle = it.subscriptionType?.title ?: "Scheduled Delivery"
            )
        }
        val preorderEvents = preorders.map {
            UpcomingEvent(
                id = it.preorder.id.toString(),
                title = it.preorder.bookTitle,
                author = it.preorder.bookAuthor,
                date = it.preorder.rangedSaleDateStart,
                type = "preorder",
                status = it.preorder.status,
                isSkipped = false,
                subTitle = it.bookstore?.name ?: "Preorder Book"
            )
        }
        (scheduledEvents + preorderEvents)
            .filter { !it.isSkipped && it.status != "Paid" && it.status != "Received" && it.status != "Released" }
            .sortedBy { it.date }
            .take(5)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoStories,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Bookish Home", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.testTag("home_top_bar")
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Welcome Header
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Welcome back,",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = user?.username ?: "Reader",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Keep track of all your upcoming book renewals and preorders in one secure place.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                                    lineHeight = 18.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            // Sleek initials avatar bubble
                            val initials = (user?.username ?: "R").take(2).uppercase()
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.tertiary
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = initials,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Bento Row 1: Scheduled Deliveries (Hero - full width)
            item {
                val activeCount = scheduled.count { !it.scheduled.isSkipped }
                val skippedCount = scheduled.count { it.scheduled.isSkipped }
                val totalDue = scheduled.filter { !it.scheduled.isSkipped && it.scheduled.status != "Paid" && it.scheduled.status != "Received" }
                    .sumOf { it.subscriptionType?.price ?: 0.0 }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedStatType = "scheduled"
                            showDetailsSheet = true
                        }
                        .testTag("home_stat_scheduled"),
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
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Scheduled Deliveries",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Active issues from subscriptions",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            
                            // Sleek price bubble
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${currency}${String.format("%.2f", totalDue)} Due",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Stats counters
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Active stats box
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)), RoundedCornerShape(16.dp))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column {
                                        Text("Active", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("$activeCount issues", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Done, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }

                            // Skipped stats box
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.05f))
                                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.15f)), RoundedCornerShape(16.dp))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column {
                                        Text("Skipped", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("$skippedCount issues", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Block, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bento Row 2: Subscriptions Spends & Book Preorders (Side-by-side)
            item {
                val activeSubs = subscriptions.filter { it.subscription.status == "Active" }
                var monthlySpend = 0.0
                var yearlySpend = 0.0

                for (s in activeSubs) {
                    val price = s.subscription.price
                    when (s.subscription.frequency.lowercase()) {
                        "weekly" -> {
                            monthlySpend += price * 4.33
                            yearlySpend += price * 52.0
                        }
                        "monthly" -> {
                            monthlySpend += price
                            yearlySpend += price * 12.0
                        }
                        "bi-monthly" -> {
                            monthlySpend += price / 2.0
                            yearlySpend += price * 6.0
                        }
                        "quarterly" -> {
                            monthlySpend += price / 3.0
                            yearlySpend += price * 4.0
                        }
                        "yearly" -> {
                            monthlySpend += price / 12.0
                            yearlySpend += price
                        }
                    }
                }

                val upcoming = preorders.count { it.preorder.status == "Upcoming" }
                val released = preorders.count { it.preorder.status == "Released" }
                val preordered = preorders.count { it.preorder.status == "Preordered" }
                val shipped = preorders.count { it.preorder.status == "Shipped" }
                val totalPreorders = preordered + shipped + upcoming

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Left bento tile (Subscription Spends)
                    Card(
                        modifier = Modifier
                            .weight(1f)
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
                                .padding(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "${currency}${String.format("%.2f", monthlySpend)}",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                text = "Monthly Spend",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f))
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Yearly: ${currency}${String.format("%.0f", yearlySpend)}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }

                    // Right bento tile (Book Preorders)
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                selectedStatType = "preorders"
                                showDetailsSheet = true
                            }
                            .testTag("home_stat_preorders"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "$totalPreorders",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                text = "Active Preorders",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Mini indicator list for statuses
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MiniStatusDot(color = Color(0xFF2196F3), count = preordered, label = "Preordered")
                                MiniStatusDot(color = Color(0xFF4CAF50), count = shipped, label = "Shipped")
                                MiniStatusDot(color = Color(0xFFFF9800), count = upcoming, label = "Upcoming")
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
                        selectedStatType = event.type
                        showDetailsSheet = true
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
                                                    text = date,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = MaterialTheme.colorScheme.primary,
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
                                                                Text(item.scheduled.bookTitle, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                                                Text(item.scheduled.bookAuthor, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                            }
                                                            Box(
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(8.dp))
                                                                    .background(
                                                                        if (item.scheduled.isSkipped) MaterialTheme.colorScheme.errorContainer
                                                                        else MaterialTheme.colorScheme.secondaryContainer
                                                                    )
                                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                                            ) {
                                                                Text(
                                                                    text = if (item.scheduled.isSkipped) "Skipped" else item.scheduled.status,
                                                                    fontSize = 10.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = if (item.scheduled.isSkipped) MaterialTheme.colorScheme.onErrorContainer
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
                                                    text = "Next Billing: $date",
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
                                                    text = "Release Date: $date",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = MaterialTheme.colorScheme.tertiary,
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
    }
}

data class UpcomingEvent(
    val id: String,
    val title: String,
    val author: String,
    val date: Long,
    val type: String, // "scheduled" or "preorder"
    val status: String,
    val isSkipped: Boolean = false,
    val subTitle: String = "" // subscription type name or bookstore name
)

@Composable
fun MiniStatusDot(color: Color, count: Int, label: String) {
    if (count > 0) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(color.copy(alpha = 0.12f))
                .padding(horizontal = 6.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(color)
                )
                Text(
                    text = "$count",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
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
            // Asymmetric Left Indicator Bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Text Info Column
            Column(modifier = Modifier.weight(1f)) {
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
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = event.status,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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

            Spacer(modifier = Modifier.width(12.dp))

            // Calendar-style Date Badge
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = accentColor.copy(alpha = 0.08f)
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.2f)),
                modifier = Modifier.width(52.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(vertical = 6.dp)
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
                }
            }
        }
    }
}
