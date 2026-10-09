package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.example.data.ForwardingService
import com.example.data.UserAddress
import java.util.Locale

val CURRENCY_LIST = listOf(
    "$" to "USD (US Dollar)",
    "€" to "EUR (Euro)",
    "£" to "GBP (British Pound)",
    "¥" to "JPY (Japanese Yen)",
    "₩" to "KRW (South Korean Won)",
    "₹" to "INR (Indian Rupee)",
    "C$" to "CAD (Canadian Dollar)",
    "A$" to "AUD (Australian Dollar)",
    "CHF" to "CHF (Swiss Franc)"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherFormTabContent(
    userAddresses: List<UserAddress>,
    forwardingServices: List<ForwardingService>,
    selectedShippingAddressId: Int?,
    onShippingAddressChange: (Int?) -> Unit,
    selectedCurrency: String,
    onCurrencyChange: (String) -> Unit,
    basePriceStr: String,
    onBasePriceChange: (String) -> Unit,
    discountedAmountStr: String = "",
    onDiscountedAmountChange: (String) -> Unit = {},
    shippingPriceStr: String,
    onShippingPriceChange: (String) -> Unit,
    taxPriceStr: String,
    onTaxPriceChange: (String) -> Unit,
    forwardShippingPriceStr: String,
    onForwardShippingPriceChange: (String) -> Unit,
    forwardTaxPriceStr: String,
    onForwardTaxPriceChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedAddress = remember(selectedShippingAddressId, userAddresses) {
        userAddresses.find { it.id == selectedShippingAddressId }
    }
    val forwardingService = remember(selectedAddress, forwardingServices) {
        if (selectedAddress?.forwardingServiceId != null) {
            forwardingServices.find { it.id == selectedAddress.forwardingServiceId }
        } else {
            null
        }
    }
    val hasForwardingService = forwardingService != null

    var expandedAddressDropdown by remember { mutableStateOf(false) }
    var expandedCurrencyDropdown by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- 1. Shipping Address Field ---
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Shipping Address",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { expandedAddressDropdown = true }
                        .testTag("other_tab_shipping_address_selector"),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )

                            if (selectedAddress == null) {
                                Text(
                                    text = "None (No address)",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Column {
                                    Text(
                                        text = selectedAddress.streetAddress1.ifBlank { "Address #${selectedAddress.id}" },
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (selectedAddress.city.isNotBlank()) {
                                        Text(
                                            text = buildString {
                                                append(selectedAddress.city)
                                                if (selectedAddress.stateProvinceRegion.isNotBlank()) {
                                                    append(", ").append(selectedAddress.stateProvinceRegion)
                                                }
                                                if (selectedAddress.country.isNotBlank()) {
                                                    append(" (${selectedAddress.country})")
                                                }
                                            },
                                            fontSize = 11.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        // Forwarding Service displayed next to the address
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (forwardingService != null) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.testTag("forwarding_service_badge")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocalShipping,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = forwardingService.name,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            } else {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "Forwarding: None",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Icon(
                                imageVector = if (expandedAddressDropdown) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                contentDescription = "Select address",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                DropdownMenu(
                    expanded = expandedAddressDropdown,
                    onDismissRequest = { expandedAddressDropdown = false },
                    properties = PopupProperties(focusable = true),
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .heightIn(max = 280.dp)
                ) {
                    // Option: None
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("None (No address)", fontWeight = if (selectedShippingAddressId == null) FontWeight.Bold else FontWeight.Normal)
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        "Forwarding: None",
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        onClick = {
                            onShippingAddressChange(null)
                            expandedAddressDropdown = false
                        }
                    )

                    if (userAddresses.isNotEmpty()) {
                        HorizontalDivider()
                        userAddresses.forEach { address ->
                            val fs = forwardingServices.find { it.id == address.forwardingServiceId }
                            val isSelected = address.id == selectedShippingAddressId

                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f, fill = false)) {
                                            Text(
                                                text = address.streetAddress1,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            val subLine = listOf(address.city, address.country).filter { it.isNotBlank() }.joinToString(", ")
                                            if (subLine.isNotBlank()) {
                                                Text(
                                                    text = subLine,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        // Display forwarding service next to the address in the dropdown
                                        if (fs != null) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                ) {
                                                    Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(11.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                                    Text(fs.name, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                                }
                                            }
                                        } else {
                                            Surface(
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    "Forwarding: None",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                onClick = {
                                    onShippingAddressChange(address.id)
                                    expandedAddressDropdown = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // --- 2. Currency (on top) ---
        val matchedCurrency = remember(selectedCurrency) {
            CURRENCY_LIST.firstOrNull { it.first == selectedCurrency }
        }
        val currencyLabel = if (matchedCurrency != null) {
            val code = matchedCurrency.second.substringBefore(" ")
            "${matchedCurrency.first} ($code)"
        } else {
            selectedCurrency
        }

        // Currency Dropdown on top
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = currencyLabel,
                onValueChange = {},
                readOnly = true,
                label = { Text("Currency", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("other_tab_currency_dropdown"),
                leadingIcon = {
                    Icon(Icons.Default.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                trailingIcon = {
                    IconButton(onClick = { expandedCurrencyDropdown = !expandedCurrencyDropdown }) {
                        Icon(
                            imageVector = if (expandedCurrencyDropdown) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = "Select currency"
                        )
                    }
                },
                singleLine = true
            )

            // Clickable overlay so tapping anywhere opens the dropdown
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { expandedCurrencyDropdown = true }
            )

            DropdownMenu(
                expanded = expandedCurrencyDropdown,
                onDismissRequest = { expandedCurrencyDropdown = false },
                properties = PopupProperties(focusable = true),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .heightIn(max = 260.dp)
            ) {
                CURRENCY_LIST.forEach { (symbol, description) ->
                    val isSelected = selectedCurrency == symbol
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "$symbol — $description",
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.5.sp
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        onClick = {
                            onCurrencyChange(symbol)
                            expandedCurrencyDropdown = false
                        }
                    )
                }
            }
        }

        // --- 3. Base Price & Discounted Amount (discounted amount to the right of base price) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Base Price
            OutlinedTextField(
                value = basePriceStr,
                onValueChange = { onBasePriceChange(it.filter { c -> c.isDigit() || c == '.' }) },
                label = { Text("Base Price", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingIcon = {
                    Text(selectedCurrency, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("other_tab_base_price")
            )

            // Discounted Amount (to the right of base price)
            OutlinedTextField(
                value = discountedAmountStr,
                onValueChange = { onDiscountedAmountChange(it.filter { c -> c.isDigit() || c == '.' }) },
                label = { Text("Discounted Amount", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingIcon = {
                    Text(selectedCurrency, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("other_tab_discounted_amount")
            )
        }

        // --- 3. Shipping Price & Tax Price ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = shippingPriceStr,
                onValueChange = { onShippingPriceChange(it.filter { c -> c.isDigit() || c == '.' }) },
                label = { Text("Shipping Price", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingIcon = {
                    Text(selectedCurrency, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("other_tab_shipping_price")
            )

            OutlinedTextField(
                value = taxPriceStr,
                onValueChange = { onTaxPriceChange(it.filter { c -> c.isDigit() || c == '.' }) },
                label = { Text("Tax Price", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingIcon = {
                    Text(selectedCurrency, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("other_tab_tax_price")
            )
        }

        // --- 4. Forward Shipping Price & Forward Tax Price (CONDITIONAL) ---
        // "if shipping address has a forwarding service, forward shipping price and forward tax price are displayed;
        //  if the forwarding service is none, they are hidden"
        AnimatedVisibility(
            visible = hasForwardingService,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FlightTakeoff,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Forwarding Service Charges (${forwardingService?.name ?: ""})",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = forwardShippingPriceStr,
                        onValueChange = { onForwardShippingPriceChange(it.filter { c -> c.isDigit() || c == '.' }) },
                        label = { Text("Forward Shipping", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        leadingIcon = {
                            Text(selectedCurrency, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("other_tab_forward_shipping_price")
                    )

                    OutlinedTextField(
                        value = forwardTaxPriceStr,
                        onValueChange = { onForwardTaxPriceChange(it.filter { c -> c.isDigit() || c == '.' }) },
                        label = { Text("Forward Tax", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        leadingIcon = {
                            Text(selectedCurrency, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("other_tab_forward_tax_price")
                    )
                }
            }
        }

        // --- 5. Total Price Summary ---
        val baseVal = basePriceStr.toDoubleOrNull()
        val discountVal = discountedAmountStr.toDoubleOrNull() ?: 0.0
        val shippingVal = shippingPriceStr.toDoubleOrNull() ?: 0.0
        val taxVal = taxPriceStr.toDoubleOrNull() ?: 0.0
        val forwardShippingVal = if (hasForwardingService) forwardShippingPriceStr.toDoubleOrNull() ?: 0.0 else 0.0
        val forwardTaxVal = if (hasForwardingService) forwardTaxPriceStr.toDoubleOrNull() ?: 0.0 else 0.0

        val formulaPrice = if (baseVal != null) baseVal + shippingVal - discountVal else null
        val totalWithTaxes = (formulaPrice ?: 0.0) + taxVal + forwardShippingVal + forwardTaxVal

        if (formulaPrice != null || totalWithTaxes > 0.0 || basePriceStr.isNotBlank() || discountedAmountStr.isNotBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().testTag("other_tab_price_summary")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (formulaPrice != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Calculated Price (Base + Ship - Disc)",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val formattedPrice = if (formulaPrice % 1.0 == 0.0) {
                                formulaPrice.toLong().toString()
                            } else {
                                String.format(Locale.US, "%.2f", formulaPrice)
                            }
                            Text(
                                text = "$selectedCurrency$formattedPrice",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (taxVal > 0.0 || forwardShippingVal > 0.0 || forwardTaxVal > 0.0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total with Taxes & Forwarding",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$selectedCurrency${String.format(Locale.US, "%.2f", totalWithTaxes)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (discountVal > 0.0) {
                        Text(
                            text = "Includes discount: -$selectedCurrency${String.format(Locale.US, "%.2f", discountVal)}",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
