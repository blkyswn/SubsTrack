package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.viewmodel.BookishViewModel
import com.example.utils.TrackingUtils

@Composable
fun PackageTrackingQuickButton(
    originTable: String,
    originId: Int,
    viewModel: BookishViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val user by viewModel.userState.collectAsState()
    val pkg by viewModel.getPackageFlow(originTable, originId).collectAsState(initial = null)
    val companies by viewModel.shippingCompaniesState.collectAsState()
    val contacts by viewModel.shippingCompanyContactsState.collectAsState()

    val trackingOptions = remember(pkg, companies, contacts, user?.defaultTrackingUrl) {
        TrackingUtils.getTrackingOptions(pkg, companies, contacts, user?.defaultTrackingUrl)
    }

    // Hide button if no tracking numbers are informed
    if (trackingOptions.isEmpty()) {
        return
    }

    var showMenu by remember { mutableStateOf(false) }

    Box {
        IconButton(
            onClick = { showMenu = true },
            modifier = modifier
                .size(36.dp)
                .testTag("package_tracking_quick_button")
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_delivery_truck_speed),
                contentDescription = "Track Package",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            trackingOptions.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = option.title,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "${option.trackingNumber} (${option.companyName})",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.TravelExplore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    onClick = {
                        showMenu = false
                        TrackingUtils.openUrl(context, option.url)
                    },
                    modifier = Modifier.testTag("quick_track_option_${option.title.lowercase().replace(" ", "_")}")
                )
            }
        }
    }
}
