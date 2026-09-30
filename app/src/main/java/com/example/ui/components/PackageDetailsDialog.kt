package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.data.PackageItem
import com.example.ui.viewmodel.BookishViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackageDetailsDialog(
    originTable: String, // "preorders" or "scheduled_subs"
    originId: Int,
    viewModel: BookishViewModel,
    hasForwardingAddress: Boolean = false,
    onDismiss: () -> Unit
) {
    val packageItemFlow = remember(originTable, originId) {
        viewModel.getPackageFlow(originTable, originId)
    }
    val currentPackage by packageItemFlow.collectAsState(initial = null)

    // Ensure register exists if it wasn't already created
    LaunchedEffect(originTable, originId) {
        viewModel.getOrCreatePackage(originTable, originId)
    }

    var purchaseDate by remember { mutableStateOf<Long?>(null) }
    var storeShippingDate by remember { mutableStateOf<Long?>(null) }
    var storeShippingCompany by remember { mutableStateOf("") }
    var storeTrackingNumber by remember { mutableStateOf("") }
    var forwarderReceivedDate by remember { mutableStateOf<Long?>(null) }
    var forwarderShippingCompany by remember { mutableStateOf("") }
    var forwarderTrackingNumber by remember { mutableStateOf("") }
    var forwarderShippedDate by remember { mutableStateOf<Long?>(null) }
    var receivedDate by remember { mutableStateOf<Long?>(null) }
    var isInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(currentPackage) {
        val pkg = currentPackage
        if (pkg != null && !isInitialized) {
            purchaseDate = pkg.purchaseDate
            storeShippingDate = pkg.storeShippingDate
            storeShippingCompany = pkg.storeShippingCompany ?: ""
            storeTrackingNumber = pkg.storeTrackingNumber ?: ""
            forwarderReceivedDate = pkg.forwarderReceivedDate
            forwarderShippingCompany = pkg.forwarderShippingCompany ?: ""
            forwarderTrackingNumber = pkg.forwarderTrackingNumber ?: ""
            forwarderShippedDate = pkg.forwarderShippedDate
            receivedDate = pkg.receivedDate
            isInitialized = true
        }
    }

    val userState by viewModel.userState.collectAsState()
    val dateFormat = remember(userState?.dateFormat) {
        SimpleDateFormat(userState?.dateFormat ?: "yyyy-MM-dd", Locale.getDefault())
    }

    val shippingCompanies by viewModel.shippingCompaniesState.collectAsState()

    var showDatePickerTarget by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_package_2),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Package Details",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Section 1: Store Delivery Information
                item {
                    Text(
                        text = "Store Delivery Information",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            DatePickerField(
                                label = "Purchase Date",
                                timestamp = purchaseDate,
                                dateFormat = dateFormat,
                                onClick = { showDatePickerTarget = "purchase" },
                                onClear = { purchaseDate = null }
                            )
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            DatePickerField(
                                label = "Store Shipping Date",
                                timestamp = storeShippingDate,
                                dateFormat = dateFormat,
                                onClick = { showDatePickerTarget = "storeShipping" },
                                onClear = { storeShippingDate = null }
                            )
                        }
                    }
                }

                item {
                    // Shipping Company autocomplete / dropdown
                    var expandedCompanies by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expandedCompanies,
                        onExpandedChange = { expandedCompanies = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = storeShippingCompany,
                            onValueChange = { storeShippingCompany = it },
                            label = { Text("Store Shipping Company") },
                            placeholder = { Text("e.g. Royal Mail, FedEx, DHL") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCompanies)
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors()
                        )

                        if (shippingCompanies.isNotEmpty()) {
                            val filtered = shippingCompanies.filter {
                                it.name.contains(storeShippingCompany, ignoreCase = true)
                            }
                            val displayList = if (filtered.isNotEmpty()) filtered else shippingCompanies
                            ExposedDropdownMenu(
                                expanded = expandedCompanies,
                                onDismissRequest = { expandedCompanies = false }
                            ) {
                                displayList.forEach { company ->
                                    DropdownMenuItem(
                                        text = { Text(company.name) },
                                        onClick = {
                                            storeShippingCompany = company.name
                                            expandedCompanies = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = storeTrackingNumber,
                        onValueChange = { storeTrackingNumber = it },
                        label = { Text("Store Tracking Number") },
                        placeholder = { Text("e.g. TRK123456789") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = {
                            if (storeTrackingNumber.isNotEmpty()) {
                                IconButton(onClick = { storeTrackingNumber = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        }
                    )
                }

                // Section 2: Forwarder Information (Only if address belongs to a forwarding service)
                if (hasForwardingAddress) {
                    item {
                        Text(
                            text = "Forwarder Information",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                DatePickerField(
                                    label = "Forwarder Received Date",
                                    timestamp = forwarderReceivedDate,
                                    dateFormat = dateFormat,
                                    onClick = { showDatePickerTarget = "forwarderReceived" },
                                    onClear = { forwarderReceivedDate = null }
                                )
                            }

                            Box(modifier = Modifier.weight(1f)) {
                                DatePickerField(
                                    label = "Forwarder Shipped Date",
                                    timestamp = forwarderShippedDate,
                                    dateFormat = dateFormat,
                                    onClick = { showDatePickerTarget = "forwarderShipped" },
                                    onClear = { forwarderShippedDate = null }
                                )
                            }
                        }
                    }

                    item {
                        // Forwarder Shipping Company autocomplete / dropdown
                        var expandedForwarderCompanies by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expandedForwarderCompanies,
                            onExpandedChange = { expandedForwarderCompanies = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = forwarderShippingCompany,
                                onValueChange = { forwarderShippingCompany = it },
                                label = { Text("Forwarder Shipping Company") },
                                placeholder = { Text("e.g. DHL Express, FedEx, UPS") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedForwarderCompanies)
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors()
                            )

                            if (shippingCompanies.isNotEmpty()) {
                                val filtered = shippingCompanies.filter {
                                    it.name.contains(forwarderShippingCompany, ignoreCase = true)
                                }
                                val displayList = if (filtered.isNotEmpty()) filtered else shippingCompanies
                                ExposedDropdownMenu(
                                    expanded = expandedForwarderCompanies,
                                    onDismissRequest = { expandedForwarderCompanies = false }
                                ) {
                                    displayList.forEach { company ->
                                        DropdownMenuItem(
                                            text = { Text(company.name) },
                                            onClick = {
                                                forwarderShippingCompany = company.name
                                                expandedForwarderCompanies = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = forwarderTrackingNumber,
                            onValueChange = { forwarderTrackingNumber = it },
                            label = { Text("Forwarder Tracking Number") },
                            placeholder = { Text("e.g. FWD987654321") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            trailingIcon = {
                                if (forwarderTrackingNumber.isNotEmpty()) {
                                    IconButton(onClick = { forwarderTrackingNumber = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear")
                                    }
                                }
                            }
                        )
                    }
                }

                // Section 3: Final Delivery
                item {
                    Text(
                        text = "Final Delivery",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                item {
                    DatePickerField(
                        label = "Received Date",
                        timestamp = receivedDate,
                        dateFormat = dateFormat,
                        onClick = { showDatePickerTarget = "received" },
                        onClear = { receivedDate = null }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val basePkg = currentPackage ?: PackageItem(
                        originTable = originTable,
                        originId = originId
                    )
                    val updated = basePkg.copy(
                        originTable = originTable, // Never allow changing origin table
                        originId = originId,       // Never allow changing origin id
                        purchaseDate = purchaseDate,
                        storeShippingDate = storeShippingDate,
                        storeShippingCompany = storeShippingCompany.trim().ifEmpty { null },
                        storeTrackingNumber = storeTrackingNumber.trim().ifEmpty { null },
                        forwarderReceivedDate = if (hasForwardingAddress) forwarderReceivedDate else null,
                        forwarderShippingCompany = if (hasForwardingAddress) forwarderShippingCompany.trim().ifEmpty { null } else null,
                        forwarderTrackingNumber = if (hasForwardingAddress) forwarderTrackingNumber.trim().ifEmpty { null } else null,
                        forwarderShippedDate = if (hasForwardingAddress) forwarderShippedDate else null,
                        receivedDate = receivedDate
                    )
                    viewModel.updatePackage(updated)
                    onDismiss()
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    // Sub-dialog DatePicker
    if (showDatePickerTarget != null) {
        val initialDate = when (showDatePickerTarget) {
            "purchase" -> purchaseDate
            "storeShipping" -> storeShippingDate
            "forwarderReceived" -> forwarderReceivedDate
            "forwarderShipped" -> forwarderShippedDate
            "received" -> receivedDate
            else -> null
        } ?: System.currentTimeMillis()

        PackageDatePickerDialog(
            initialDateMillis = initialDate,
            onDateSelected = { selectedMillis ->
                when (showDatePickerTarget) {
                    "purchase" -> purchaseDate = selectedMillis
                    "storeShipping" -> storeShippingDate = selectedMillis
                    "forwarderReceived" -> forwarderReceivedDate = selectedMillis
                    "forwarderShipped" -> forwarderShippedDate = selectedMillis
                    "received" -> receivedDate = selectedMillis
                }
                showDatePickerTarget = null
            },
            onDismiss = { showDatePickerTarget = null }
        )
    }
}

@Composable
private fun DatePickerField(
    label: String,
    timestamp: Long?,
    dateFormat: SimpleDateFormat,
    onClick: () -> Unit,
    onClear: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        OutlinedTextField(
            value = timestamp?.let { dateFormat.format(Date(it)) } ?: "",
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = {
                Text(
                    text = label,
                    maxLines = 2,
                    softWrap = true,
                    lineHeight = 13.sp,
                    fontSize = 12.sp
                )
            },
            placeholder = { Text("Select date", fontSize = 13.sp) },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                if (timestamp != null) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Default.Close, contentDescription = "Clear Date")
                    }
                } else {
                    IconButton(onClick = onClick) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Pick Date")
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PackageDatePickerDialog(
    initialDateMillis: Long,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    // Convert to UTC midnight to avoid timezone offset issues in DatePicker
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    calendar.timeInMillis = initialDateMillis
    val initialUtc = calendar.timeInMillis

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialUtc)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { utcMillis ->
                        // Convert UTC midnight to local timestamp
                        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                        utcCal.timeInMillis = utcMillis
                        val localCal = Calendar.getInstance()
                        localCal.set(
                            utcCal.get(Calendar.YEAR),
                            utcCal.get(Calendar.MONTH),
                            utcCal.get(Calendar.DAY_OF_MONTH),
                            12, 0, 0
                        )
                        onDateSelected(localCal.timeInMillis)
                    }
                }
            ) {
                Text("OK")
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
