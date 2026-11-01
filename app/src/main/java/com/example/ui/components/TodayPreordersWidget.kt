package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.*
import com.example.ui.viewmodel.BookishViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * A widget that scrolls vertically through all preorders for the day.
 * If user swipes up or down, scrolls to next or previous preorder when multiple preorders are available.
 * Shows "No preorders today" when no preorders exist.
 * Styled using the preorder tile style used in preorders screen list items.
 */
@Composable
fun TodayPreordersWidget(
    viewModel: BookishViewModel,
    onPreorderClick: (PreorderWithBookstore) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val preorders by viewModel.rawPreordersState.collectAsState()
    val userState by viewModel.userState.collectAsState()
    val userAddresses by viewModel.userAddressesState.collectAsState()
    val forwardingServices by viewModel.forwardingServicesState.collectAsState()
    val allPackages by viewModel.allPackagesState.collectAsState()

    val currency = userState?.currency ?: "$"
    val isDark = isSystemInDarkTheme()

    // Filter all preorders for today
    val todayPreorders = remember(preorders) {
        val now = System.currentTimeMillis()
        val getDayStart = { ms: Long ->
            Calendar.getInstance().apply {
                timeInMillis = ms
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
        val todayStart = getDayStart(now)
        val dateFormatKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayKey = dateFormatKey.format(Date(now))

        preorders.filter { item ->
            val s = getDayStart(item.preorder.rangedSaleDateStart)
            val e = getDayStart(
                if (item.preorder.rangedSaleDateEnd > item.preorder.rangedSaleDateStart) item.preorder.rangedSaleDateEnd
                else item.preorder.rangedSaleDateStart
            )
            val isToday = (todayStart in s..e) || (dateFormatKey.format(Date(item.preorder.rangedSaleDateStart)) == todayKey)
            val status = item.preorder.status.trim()
            val isAllowedStatus = status.equals("Upcoming", ignoreCase = true) ||
                    status.equals("Released", ignoreCase = true)
            isToday && isAllowedStatus
        }.sortedBy { it.preorder.rangedSaleDateStart }
    }

    if (todayPreorders.isEmpty()) {
        // Empty State: "No preorders today"
        Card(
            modifier = modifier
                .fillMaxWidth()
                .testTag("today_preorders_empty_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.18f else 0.35f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp, horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "No preorders today",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    } else {
        val isInfinite = todayPreorders.size > 1
        val infinitePageCount = if (isInfinite) 10000 else 1
        val initialPage = if (isInfinite) 5000 - (5000 % todayPreorders.size) else 0
        val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { infinitePageCount })
        val coroutineScope = rememberCoroutineScope()

        Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(122.dp)
                    .pointerInput(pagerState, todayPreorders.size) {
                        if (todayPreorders.size > 1) {
                            var totalVerticalDelta = 0f
                            detectVerticalDragGestures(
                                onDragStart = { totalVerticalDelta = 0f },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    totalVerticalDelta += dragAmount
                                    coroutineScope.launch {
                                        pagerState.dispatchRawDelta(-dragAmount)
                                    }
                                },
                                onDragEnd = {
                                    coroutineScope.launch {
                                        val threshold = 30f
                                        if (totalVerticalDelta < -threshold) {
                                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                        } else if (totalVerticalDelta > threshold) {
                                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                        } else {
                                            pagerState.animateScrollToPage(pagerState.currentPage)
                                        }
                                    }
                                },
                                onDragCancel = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage)
                                    }
                                }
                            )
                        }
                    }
            ) {
                VerticalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("today_preorders_vertical_pager"),
                    pageSpacing = 8.dp
                ) { page ->
                    val actualIndex = page % todayPreorders.size
                    val item = todayPreorders[actualIndex]
                    PreorderTileView(
                        item = item,
                        currency = currency,
                        isDark = isDark,
                        userAddresses = userAddresses,
                        forwardingServices = forwardingServices,
                        allPackages = allPackages,
                        onClick = { onPreorderClick(item) }
                    )
                }
            }

            // Slider dots on the RIGHT of the widget with Up/Down buttons when multiple preorders exist
            if (todayPreorders.size > 1) {
                Column(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .testTag("today_preorders_dots_right"),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExpandLess,
                            contentDescription = "Previous preorder",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    val currentCycleIndex = pagerState.currentPage % todayPreorders.size
                    repeat(todayPreorders.size) { index ->
                        val isSelected = currentCycleIndex == index
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(if (isSelected) 16.dp else 5.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                )
                                .clickable {
                                    coroutineScope.launch {
                                        val diff = index - currentCycleIndex
                                        pagerState.animateScrollToPage(pagerState.currentPage + diff)
                                    }
                                }
                        )
                    }

                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = "Next preorder",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Preorder tile styled identically to preorders screen list items.
 */
@Composable
fun PreorderTileView(
    item: PreorderWithBookstore,
    currency: String,
    isDark: Boolean,
    userAddresses: List<UserAddress>,
    forwardingServices: List<ForwardingService>,
    allPackages: List<PackageItem>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val preorderAddress = remember(item.preorder.shippingAddressId, userAddresses) {
        userAddresses.find { it.id == item.preorder.shippingAddressId }
    }
    val forwardingService = remember(preorderAddress, forwardingServices) {
        preorderAddress?.forwardingServiceId?.let { fsId -> forwardingServices.find { it.id == fsId } }
    }
    val packageItem = remember(allPackages, item.preorder.id) {
        allPackages.find { it.originTable == "preorders" && it.originId == item.preorder.id }
    }

    val curStatus = item.preorder.status.lowercase().trim()
    val isStoredAtForwarder = forwardingService != null &&
            curStatus !in listOf("shipped", "received") &&
            packageItem?.forwarderShippedDate == null &&
            (curStatus == "in suite" || packageItem?.forwarderReceivedDate != null)

    val effectiveReceivedDate = packageItem?.forwarderReceivedDate
        ?: if (curStatus == "in suite") System.currentTimeMillis() else null

    val accentColor = when (curStatus) {
        "upcoming" -> Color(0xFF64748B) // Grey
        "released" -> Color(0xFFF97316) // Orange
        "wishlist" -> Color(0xFFEC4899) // Pink / Rose
        "preordered" -> Color(0xFF2563EB) // Blue
        "forwarded" -> Color(0xFF0EA5E9) // Sky Blue
        "in suite" -> Color(0xFFD97706) // Amber
        "shipped" -> Color(0xFF8B5CF6) // Purple
        "received" -> Color(0xFF10B981) // Green
        else -> Color(0xFF64748B)
    }

    val activeImageModel = item.preorder.picturePath.takeIf { !it.isNullOrEmpty() }
        ?: item.bookstore?.profilePic.takeIf { !it.isNullOrEmpty() && it != "ic_launcher_foreground" }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("today_preorder_tile_${item.preorder.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (curStatus) {
                "upcoming" -> Color(0xFF64748B).copy(alpha = if (isDark) 0.12f else 0.06f)
                "released" -> Color(0xFFF97316).copy(alpha = if (isDark) 0.12f else 0.06f)
                "wishlist" -> Color(0xFFEC4899).copy(alpha = if (isDark) 0.12f else 0.06f)
                "preordered" -> Color(0xFF2563EB).copy(alpha = if (isDark) 0.12f else 0.06f)
                "forwarded" -> Color(0xFF0EA5E9).copy(alpha = if (isDark) 0.12f else 0.06f)
                "in suite" -> Color(0xFFD97706).copy(alpha = if (isDark) 0.12f else 0.06f)
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
                .padding(horizontal = 14.dp, vertical = 10.dp),
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

            Spacer(modifier = Modifier.width(12.dp))

            // Cover / Bookstore Picture Box (Fixed size matching all fields height)
            Box(
                modifier = Modifier
                    .size(82.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                if (activeImageModel != null) {
                    AsyncImage(
                        model = activeImageModel,
                        contentDescription = "${item.preorder.bookTitle} Cover",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    val storeName = item.bookstore?.name?.takeIf { it.isNotBlank() } ?: item.preorder.bookTitle
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

                if (isStoredAtForwarder) {
                    StorageCountdownPill(
                        forwarderReceivedDate = effectiveReceivedDate,
                        storageDays = forwardingService?.storageDays,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text Info & Status Column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Compact Text Info with clean spacing
                Column(verticalArrangement = Arrangement.spacedBy(2.5.dp)) {
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

                Spacer(modifier = Modifier.height(5.dp))

                // Status Badge with icon before text & Price with dot spacer
                val statusContainerColor = when (curStatus) {
                    "upcoming" -> if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9)
                    "released" -> if (isDark) Color(0xFF7C2D12) else Color(0xFFFFEDD5)
                    "wishlist" -> if (isDark) Color(0xFF831843) else Color(0xFFFCE7F3)
                    "preordered" -> if (isDark) Color(0xFF1E3A8A) else Color(0xFFDBEAFE)
                    "forwarded" -> if (isDark) Color(0xFF0C4A6E) else Color(0xFFE0F2FE)
                    "in suite" -> if (isDark) Color(0xFF78350F) else Color(0xFFFEF3C7)
                    "shipped" -> if (isDark) Color(0xFF4C1D95) else Color(0xFFF3E8FF)
                    "received" -> if (isDark) Color(0xFF064E3B) else Color(0xFFD1FAE5)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
                val statusOnContainerColor = when (curStatus) {
                    "upcoming" -> if (isDark) Color(0xFFF1F5F9) else Color(0xFF334155)
                    "released" -> if (isDark) Color(0xFFFFEDD5) else Color(0xFF9A3412)
                    "wishlist" -> if (isDark) Color(0xFFFCE7F3) else Color(0xFF9D174D)
                    "preordered" -> if (isDark) Color(0xFFDBEAFE) else Color(0xFF1E40AF)
                    "forwarded" -> if (isDark) Color(0xFFE0F2FE) else Color(0xFF0369A1)
                    "in suite" -> if (isDark) Color(0xFFFEF3C7) else Color(0xFF92400E)
                    "shipped" -> if (isDark) Color(0xFFF3E8FF) else Color(0xFF5B21B6)
                    "received" -> if (isDark) Color(0xFFD1FAE5) else Color(0xFF065F46)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                val statusIcon = when (curStatus) {
                    "upcoming", "preordered" -> Icons.Default.ShoppingBag
                    "forwarded" -> Icons.Default.AltRoute
                    "in suite" -> Icons.Default.Warehouse
                    "released" -> Icons.Default.NewReleases
                    "wishlist" -> Icons.Default.Favorite
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
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(statusContainerColor)
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = null,
                            modifier = Modifier.size(9.dp),
                            tint = statusOnContainerColor
                        )
                        Text(
                            text = item.preorder.status,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusOnContainerColor,
                            maxLines = 1
                        )
                    }

                    Text(
                        text = "•",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val formattedPrice = String.format(Locale.getDefault(), "%.2f", item.preorder.price)
                    Text(
                        text = "$currency$formattedPrice",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Right Column: Date Square Card
            val dateFormatMonth = remember { SimpleDateFormat("MMM", Locale.getDefault()) }
            val dateFormatDay = remember { SimpleDateFormat("dd", Locale.getDefault()) }
            val eventDate = Date(item.preorder.rangedSaleDateStart)
            val monthStr = dateFormatMonth.format(eventDate).uppercase(Locale.getDefault())
            val dayStr = dateFormatDay.format(eventDate)

            val startCal = remember(item.preorder.rangedSaleDateStart) {
                Calendar.getInstance().apply { timeInMillis = item.preorder.rangedSaleDateStart }
            }
            val formattedTime = String.format(
                Locale.getDefault(),
                "%02d:%02d",
                startCal.get(Calendar.HOUR_OF_DAY),
                startCal.get(Calendar.MINUTE)
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = accentColor.copy(alpha = 0.08f)
                ),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.2f)),
                modifier = Modifier
                    .fillMaxHeight()
                    .width(60.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 6.dp, horizontal = 5.dp),
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
                    Text(
                        text = formattedTime,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
            }
        }
    }
}
