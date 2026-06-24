package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.components.CalendarMarker
import com.example.ui.components.MonthCalendar
import com.example.ui.viewmodel.BookishViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreordersScreen(
    viewModel: BookishViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Observe flows
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

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                TopAppBar(
                    title = {
                        Text("Book Preorders", fontWeight = FontWeight.Bold, fontSize = 20.sp)
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
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Stats Timeframe Selector: 7 Days, Monthly, Yearly
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val timeframes = listOf("7 Days", "Monthly", "Yearly")
                timeframes.forEachIndexed { index, label ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (currentStatsTimeframe == index) MaterialTheme.colorScheme.tertiaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (currentStatsTimeframe == index) MaterialTheme.colorScheme.tertiary
                                else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { currentStatsTimeframe = index }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (currentStatsTimeframe == index) MaterialTheme.colorScheme.onTertiaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Math for counters based on timeframe
            val timeframeDays = when (currentStatsTimeframe) {
                0 -> 7
                1 -> 30
                else -> 365
            }
            val now = System.currentTimeMillis()
            val dayMs = 24 * 60 * 60 * 1000L
            val limitTime = now + timeframeDays * dayMs

            // Filter preorders falling inside range
            val preordersInScope = preordersList.filter {
                it.preorder.rangedSaleDateStart in now..limitTime
            }

            val upcoming = preordersInScope.count { it.preorder.status.lowercase() == "upcoming" }
            val released = preordersInScope.count { it.preorder.status.lowercase() == "released" }
            val preordered = preordersInScope.count { it.preorder.status.lowercase() == "preordered" }
            val shipped = preordersInScope.count { it.preorder.status.lowercase() == "shipped" }

            // Display Upper Stats grid with four counters
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.05f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Preordered", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$preordered", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                    }
                    Divider(modifier = Modifier.height(30.dp).width(1.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Shipped", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$shipped", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Divider(modifier = Modifier.height(30.dp).width(1.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Released", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$released", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    }
                    Divider(modifier = Modifier.height(30.dp).width(1.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Upcoming", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$upcoming", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
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
                                PreorderItemRow(item, viewModel)
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
                // LIST VIEW Grouped by Release Date (rangedSaleDateStart)
                val dateFormatGroup = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())
                val groupedPreorders = remember(preordersList) {
                    preordersList.groupBy { dateFormatGroup.format(Date(it.preorder.rangedSaleDateStart)) }
                }

                if (groupedPreorders.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No preorders found matching search or filters.")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        groupedPreorders.keys.sorted().forEach { dateHeader ->
                            item {
                                Text(
                                    text = dateHeader,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                            items(groupedPreorders[dateHeader] ?: emptyList()) { item ->
                                PreorderItemRow(item, viewModel)
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

@Composable
fun PreorderItemRow(item: PreorderWithBookstore, viewModel: BookishViewModel) {
    val currency = viewModel.userState.collectAsState().value?.currency ?: "$"
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    var showEditDialog by remember { mutableStateOf(false) }

    val accentColor = when (item.preorder.status.lowercase()) {
        "preordered" -> MaterialTheme.colorScheme.tertiary
        "shipped" -> MaterialTheme.colorScheme.primary
        "received", "released" -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.outline
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showEditDialog = true }
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
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
                    .height(64.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentColor)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.preorder.bookTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = "By ${item.preorder.bookAuthor}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${item.bookstore?.name ?: "Unknown Bookstore"} • Release: ${dateFormat.format(Date(item.preorder.rangedSaleDateStart))}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 2.dp)
                )
                if (item.preorder.description.isNotEmpty()) {
                    Text(
                        text = item.preorder.description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 4.dp),
                        maxLines = 2
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "$currency${String.format("%.2f", item.preorder.price)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when (item.preorder.status.lowercase()) {
                                "preordered" -> MaterialTheme.colorScheme.tertiaryContainer
                                "shipped" -> MaterialTheme.colorScheme.primaryContainer
                                "received", "released" -> MaterialTheme.colorScheme.secondaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = item.preorder.status,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (item.preorder.status.lowercase()) {
                            "preordered" -> MaterialTheme.colorScheme.onTertiaryContainer
                            "shipped" -> MaterialTheme.colorScheme.onPrimaryContainer
                            "received", "released" -> MaterialTheme.colorScheme.onSecondaryContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
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
    var selectedBookstoreId by remember { mutableStateOf(bookstores.firstOrNull()?.id ?: 0) }
    var bookTitle by remember { mutableStateOf("") }
    var bookAuthor by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priceStr by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Preordered") }

    val calendar = Calendar.getInstance()
    var saleDateStart by remember { mutableStateOf(calendar.timeInMillis) }
    var saleDateEnd by remember { mutableStateOf(calendar.timeInMillis + 2 * 24 * 60 * 60 * 1000L) }

    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Preorder", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("Select Bookstore", fontWeight = FontWeight.Medium)
                    if (bookstores.isEmpty()) {
                        Text("No bookstores registered! Add one first.", color = Color.Red, fontSize = 12.sp)
                    } else {
                        var expandedStore by remember { mutableStateOf(false) }
                        val currentStore = bookstores.find { it.id == selectedBookstoreId } ?: bookstores.first()
                        Box {
                            OutlinedButton(onClick = { expandedStore = true }) {
                                Text(currentStore.name)
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
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        label = { Text("Price") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth().testTag("add_preorder_price")
                    )
                }

                item {
                    var expandedStatus by remember { mutableStateOf(false) }
                    Column {
                        Text("Status", fontSize = 12.sp)
                        OutlinedButton(onClick = { expandedStatus = true }) {
                            Text(status)
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

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Release Start: ${dateFormat.format(Date(saleDateStart))}", fontSize = 13.sp)
                            TextButton(onClick = {
                                val c = Calendar.getInstance()
                                DatePickerDialog(context, { _, year, month, day ->
                                    val newCal = Calendar.getInstance()
                                    newCal.set(year, month, day)
                                    saleDateStart = newCal.timeInMillis
                                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                            }) {
                                Text("Pick")
                            }
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Release End: ${dateFormat.format(Date(saleDateEnd))}", fontSize = 13.sp)
                            TextButton(onClick = {
                                val c = Calendar.getInstance()
                                DatePickerDialog(context, { _, year, month, day ->
                                    val newCal = Calendar.getInstance()
                                    newCal.set(year, month, day)
                                    saleDateEnd = newCal.timeInMillis
                                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                            }) {
                                Text("Pick")
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
                    if (bookTitle.isNotEmpty() && selectedBookstoreId > 0) {
                        viewModel.addPreorder(
                            bookstoreId = selectedBookstoreId,
                            bookTitle = bookTitle,
                            bookAuthor = bookAuthor,
                            description = description,
                            price = parsedPrice,
                            saleDateStart = saleDateStart,
                            saleDateEnd = saleDateEnd,
                            status = status
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

    var selectedBookstoreId by remember { mutableStateOf(pr.bookstoreId) }
    var bookTitle by remember { mutableStateOf(pr.bookTitle) }
    var bookAuthor by remember { mutableStateOf(pr.bookAuthor) }
    var description by remember { mutableStateOf(pr.description) }
    var priceStr by remember { mutableStateOf(pr.price.toString()) }
    var status by remember { mutableStateOf(pr.status) }

    var saleDateStart by remember { mutableStateOf(pr.rangedSaleDateStart) }
    var saleDateEnd by remember { mutableStateOf(pr.rangedSaleDateEnd) }

    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Preorder", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("Select Bookstore", fontWeight = FontWeight.Medium)
                    var expandedStore by remember { mutableStateOf(false) }
                    val currentStore = bookstores.find { it.id == selectedBookstoreId } ?: bookstores.firstOrNull()
                    Box {
                        OutlinedButton(onClick = { expandedStore = true }) {
                            Text(currentStore?.name ?: "Unknown")
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
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        label = { Text("Price") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    var expandedStatus by remember { mutableStateOf(false) }
                    Column {
                        Text("Status", fontSize = 12.sp)
                        OutlinedButton(onClick = { expandedStatus = true }) {
                            Text(status)
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

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Release Start: ${dateFormat.format(Date(saleDateStart))}", fontSize = 13.sp)
                            TextButton(onClick = {
                                val c = Calendar.getInstance().apply { timeInMillis = saleDateStart }
                                DatePickerDialog(context, { _, year, month, day ->
                                    val newCal = Calendar.getInstance()
                                    newCal.set(year, month, day)
                                    saleDateStart = newCal.timeInMillis
                                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                            }) {
                                Text("Pick")
                            }
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Release End: ${dateFormat.format(Date(saleDateEnd))}", fontSize = 13.sp)
                            TextButton(onClick = {
                                val c = Calendar.getInstance().apply { timeInMillis = saleDateEnd }
                                DatePickerDialog(context, { _, year, month, day ->
                                    val newCal = Calendar.getInstance()
                                    newCal.set(year, month, day)
                                    saleDateEnd = newCal.timeInMillis
                                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                            }) {
                                Text("Pick")
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
                                    status = status
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
