package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.PopupProperties
import coil.compose.AsyncImage
import com.example.ui.components.ImageViewerDialog
import com.example.data.Bookstore
import com.example.data.ForwardingService
import com.example.data.SubscriptionType
import com.example.data.User
import com.example.data.UserAddress
import com.example.ui.viewmodel.BookishViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: BookishViewModel,
    modifier: Modifier = Modifier
) {
    val user by viewModel.userState.collectAsState()
    val context = LocalContext.current

    // Local form states
    var editUsername by remember { mutableStateOf("") }
    var editCountry by remember { mutableStateOf("") }
    var editCurrency by remember { mutableStateOf("$") }
    var editLanguage by remember { mutableStateOf("English") }
    var editProfilePic by remember { mutableStateOf("avatar_classic") }
    var editDefaultCountStr by remember { mutableStateOf("6") }
    var editThemeMode by remember { mutableStateOf("system") }
    var editThemeCombo by remember { mutableStateOf("default") }
    var editDateFormat by remember { mutableStateOf("yyyy-MM-dd") }
    var editDisplayAmounts by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    var showSavedToast by remember { mutableStateOf(false) }
    var showGeneratePastSubsDialog by remember { mutableStateOf(false) }

    val userAddresses by viewModel.userAddressesState.collectAsState()
    val forwardingServices by viewModel.forwardingServicesState.collectAsState()
    var showAddAddressDialog by remember { mutableStateOf(false) }
    var editingAddress by remember { mutableStateOf<UserAddress?>(null) }
    var previewImageUrl by remember { mutableStateOf<String?>(null) }

    fun autoSave(
        username: String = editUsername,
        currency: String = editCurrency,
        language: String = editLanguage,
        profilePic: String = editProfilePic,
        defaultCountStr: String = editDefaultCountStr,
        themeMode: String = editThemeMode,
        themeCombo: String = editThemeCombo,
        dateFormat: String = editDateFormat,
        displayAmounts: Boolean = editDisplayAmounts,
        country: String = editCountry
    ) {
        if (username.isNotEmpty()) {
            val parsedCount = defaultCountStr.toIntOrNull() ?: 6
            viewModel.updateProfile(
                username = username,
                currency = currency,
                language = language,
                profilePic = profilePic,
                defaultScheduledSubCount = parsedCount,
                themeMode = themeMode,
                themeCombo = themeCombo,
                dateFormat = dateFormat,
                displayAmounts = displayAmounts,
                country = country
            )
            showSavedToast = true
        }
    }

    LaunchedEffect(showSavedToast) {
        if (showSavedToast) {
            kotlinx.coroutines.delay(2500)
            showSavedToast = false
        }
    }

    // Sync form with user state when loaded
    LaunchedEffect(user) {
        user?.let {
            editUsername = it.username
            editCountry = it.country
            editCurrency = it.currency
            editLanguage = it.language
            editProfilePic = it.profilePic ?: "avatar_classic"
            editDefaultCountStr = it.defaultScheduledSubCount.toString()
            editThemeMode = it.themeMode
            editThemeCombo = it.themeCombo
            editDateFormat = it.dateFormat
            editDisplayAmounts = it.displayAmounts
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Profile & Settings", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            floatingActionButton = {
                if (selectedTab == 0) {
                    FloatingActionButton(
                        onClick = { showAddAddressDialog = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("floating_add_address_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Address")
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Profile Card Header with presets
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Display Avatar Icon based on preset
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (editProfilePic) {
                                        "avatar_fantasy" -> Icons.Default.AutoStories
                                        "avatar_scifi" -> Icons.Default.RocketLaunch
                                        "avatar_cozy" -> Icons.Default.Coffee
                                        else -> Icons.Default.Person
                                    },
                                    contentDescription = "Profile Pic",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(40.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = editUsername.ifEmpty { "Reader" },
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (editCountry.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Public,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = editCountry,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Selection of Avatar presets
                            Text("Select Reading Vibe", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val avatars = listOf(
                                    "avatar_classic" to Icons.Default.Person,
                                    "avatar_fantasy" to Icons.Default.AutoStories,
                                    "avatar_scifi" to Icons.Default.RocketLaunch,
                                    "avatar_cozy" to Icons.Default.Coffee
                                )

                                avatars.forEach { (key, icon) ->
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (editProfilePic == key) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                            .clickable {
                                                editProfilePic = key
                                                autoSave(profilePic = key)
                                            }
                                            .padding(8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = if (editProfilePic == key) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Profile Tabs for Addresses, Settings, Theme & Colors, and Advanced Settings
                item {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            modifier = Modifier.testTag("tab_addresses")
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .padding(horizontal = 2.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Addresses Tab",
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "Addresses",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            modifier = Modifier.testTag("tab_settings")
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .padding(horizontal = 2.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Settings Tab",
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "Settings",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            modifier = Modifier.testTag("tab_theme")
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .padding(horizontal = 2.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = "Theme Tab",
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "Theme & Colors",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp,
                                    maxLines = 2,
                                    softWrap = true,
                                    lineHeight = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        Tab(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            modifier = Modifier.testTag("tab_advanced")
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .padding(horizontal = 2.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Advanced Tab",
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "Advanced",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Main Section based on selected tab
                if (selectedTab == 0) {
                    // ADDRESSES TAB - Title removed as requested

                    if (userAddresses.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Place,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    Text(
                                        text = "No addresses saved yet",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = "Tap the + button to register your shipping addresses or forwarding service boxes.",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Button(
                                        onClick = { showAddAddressDialog = true },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.testTag("add_first_address_btn")
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Add Address")
                                    }
                                }
                            }
                        }
                    } else {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                for (address in userAddresses) {
                                    val linkedService = forwardingServices.find { it.id == address.forwardingServiceId }
                                    val isDark = isSystemInDarkTheme()
                                    val hasPicture = linkedService != null && !linkedService.profilePic.isNullOrEmpty() && linkedService.profilePic != "ic_launcher_foreground"

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { editingAddress = address }
                                            .testTag("address_item_${address.id}"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = if (address.isDefault) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(IntrinsicSize.Min)
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Left: Forwarding service picture or local shipping icon
                                            Box(
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                                    .then(
                                                        if (hasPicture) {
                                                            Modifier.clickable { previewImageUrl = linkedService?.profilePic }
                                                        } else {
                                                            Modifier
                                                        }
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (hasPicture) {
                                                    AsyncImage(
                                                        model = linkedService!!.profilePic,
                                                        contentDescription = "${linkedService.name} Picture",
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Default.LocalShipping,
                                                        contentDescription = linkedService?.name ?: "Shipping",
                                                        tint = if (linkedService != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(10.dp))

                                            // Middle Column: Address information
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(1.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = address.streetAddress1,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.5.sp,
                                                        lineHeight = 18.sp
                                                    )
                                                    if (address.isDefault) {
                                                        Surface(
                                                            color = MaterialTheme.colorScheme.primary,
                                                            shape = RoundedCornerShape(4.dp)
                                                        ) {
                                                            Text(
                                                                text = "DEFAULT",
                                                                color = MaterialTheme.colorScheme.onPrimary,
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                            )
                                                        }
                                                    }
                                                }

                                                if (address.streetAddress2.isNotBlank()) {
                                                    Text(
                                                        text = address.streetAddress2,
                                                        fontSize = 12.5.sp,
                                                        lineHeight = 16.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }

                                                if (address.city.isNotBlank()) {
                                                    Text(
                                                        text = address.city,
                                                        fontSize = 12.sp,
                                                        lineHeight = 15.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }

                                                val regionZipLine = buildString {
                                                    append(address.stateProvinceRegion)
                                                    if (address.postalCode.isNotBlank()) {
                                                        if (isNotEmpty()) append(" ")
                                                        append(address.postalCode)
                                                    }
                                                    if (address.country.isNotBlank()) {
                                                        if (isNotEmpty()) append(", ")
                                                        append(address.country)
                                                    }
                                                }
                                                if (regionZipLine.isNotBlank()) {
                                                    Text(
                                                        text = regionZipLine,
                                                        fontSize = 12.sp,
                                                        lineHeight = 15.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            // Right Column: Edit button on top, forwarding service tag at bottom right
                                            Column(
                                                modifier = Modifier.fillMaxHeight(),
                                                horizontalAlignment = Alignment.End,
                                                verticalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                IconButton(
                                                    onClick = { editingAddress = address },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "Edit Address",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }

                                                if (linkedService != null) {
                                                    val tagContainerColor = if (isDark) Color(0xFF4C1D95) else Color(0xFFF3E8FF)
                                                    val tagOnContainerColor = if (isDark) Color(0xFFF3E8FF) else Color(0xFF5B21B6)

                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                        modifier = Modifier
                                                            .testTag("address_forwarding_tag_${address.id}")
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(tagContainerColor)
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.LocalShipping,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(11.dp),
                                                            tint = tagOnContainerColor
                                                        )
                                                        Text(
                                                            text = linkedService.name,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = tagOnContainerColor
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(72.dp))
                        }
                    }
                } else if (selectedTab == 1) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text("Personal Information", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                                // Username textfield
                                OutlinedTextField(
                                    value = editUsername,
                                    onValueChange = { newValue ->
                                        editUsername = newValue
                                        autoSave(username = newValue)
                                    },
                                    label = { Text("Display Username", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    modifier = Modifier.fillMaxWidth().testTag("profile_username"),
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                                )

                                // Country textfield with filterable dropdown
                                var expandedCountryDropdown by remember { mutableStateOf(false) }
                                val allCountries = remember {
                                    Locale.getISOCountries()
                                        .map { Locale("", it).getDisplayCountry(Locale.ENGLISH) }
                                        .filter { it.isNotBlank() }
                                        .distinct()
                                        .sorted()
                                }
                                val filteredCountries = remember(editCountry, allCountries) {
                                    if (editCountry.isBlank()) {
                                        allCountries
                                    } else {
                                        val query = editCountry.trim()
                                        allCountries.filter { it.contains(query, ignoreCase = true) }
                                    }
                                }

                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = editCountry,
                                        onValueChange = { newValue ->
                                            editCountry = newValue
                                            expandedCountryDropdown = true
                                            autoSave(country = newValue)
                                        },
                                        label = { Text("Country", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                        placeholder = { Text("Search or select country...") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("profile_country")
                                            .onFocusChanged { focusState ->
                                                if (focusState.isFocused) {
                                                    expandedCountryDropdown = true
                                                }
                                            },
                                        leadingIcon = { Icon(Icons.Default.Public, contentDescription = null) },
                                        trailingIcon = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (editCountry.isNotEmpty()) {
                                                    IconButton(
                                                        onClick = {
                                                            editCountry = ""
                                                            expandedCountryDropdown = true
                                                            autoSave(country = "")
                                                        }
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Clear,
                                                            contentDescription = "Clear country",
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                                IconButton(
                                                    onClick = { expandedCountryDropdown = !expandedCountryDropdown }
                                                ) {
                                                    Icon(
                                                        imageVector = if (expandedCountryDropdown) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                                        contentDescription = "Toggle country list",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                        keyboardActions = KeyboardActions(onDone = { expandedCountryDropdown = false })
                                    )

                                    DropdownMenu(
                                        expanded = expandedCountryDropdown,
                                        onDismissRequest = { expandedCountryDropdown = false },
                                        properties = PopupProperties(focusable = false),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 280.dp)
                                    ) {
                                        if (filteredCountries.isEmpty()) {
                                            DropdownMenuItem(
                                                text = { Text("No matching countries", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                                onClick = { },
                                                enabled = false
                                            )
                                        } else {
                                            filteredCountries.take(80).forEach { countryName ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            text = countryName,
                                                            fontWeight = if (countryName.equals(editCountry, ignoreCase = true)) FontWeight.Bold else FontWeight.Normal
                                                        )
                                                    },
                                                    onClick = {
                                                        editCountry = countryName
                                                        expandedCountryDropdown = false
                                                        autoSave(country = countryName)
                                                    },
                                                    leadingIcon = {
                                                        Icon(
                                                            imageVector = Icons.Default.LocationOn,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(18.dp),
                                                            tint = if (countryName.equals(editCountry, ignoreCase = true)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                )
                                            }
                                            if (filteredCountries.size > 80) {
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            "Type to filter ${filteredCountries.size - 80} more...",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    },
                                                    onClick = {},
                                                    enabled = false
                                                )
                                            }
                                        }
                                    }
                                }

                                // Currency dropdown
                                var expandedCurrency by remember { mutableStateOf(false) }
                                val currencyOptions = listOf(
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
                                val selectedCurrencyMatch = currencyOptions.firstOrNull { it.first == editCurrency }
                                val currencyDisplayText = if (selectedCurrencyMatch != null) "${selectedCurrencyMatch.first} — ${selectedCurrencyMatch.second}" else editCurrency

                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = currencyDisplayText,
                                        onValueChange = {},
                                        label = { Text("App Currency", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                        readOnly = true,
                                        leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null) },
                                        trailingIcon = {
                                            IconButton(onClick = { expandedCurrency = true }) {
                                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().testTag("profile_currency")
                                    )
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .clickable { expandedCurrency = true }
                                    )
                                    DropdownMenu(
                                        expanded = expandedCurrency,
                                        onDismissRequest = { expandedCurrency = false },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        currencyOptions.forEach { (currSymbol, currDesc) ->
                                            DropdownMenuItem(
                                                text = {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Text(currSymbol, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                        Text("—", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        Text(currDesc, fontSize = 14.sp)
                                                    }
                                                },
                                                onClick = {
                                                    editCurrency = currSymbol
                                                    expandedCurrency = false
                                                    autoSave(currency = currSymbol)
                                                }
                                            )
                                        }
                                    }
                                }

                                // Language dropdown
                                var expandedLang by remember { mutableStateOf(false) }
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = editLanguage,
                                        onValueChange = {},
                                        label = { Text("App Language", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                        readOnly = true,
                                        leadingIcon = { Icon(Icons.Default.Language, contentDescription = null) },
                                        trailingIcon = {
                                            IconButton(onClick = { expandedLang = true }) {
                                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().testTag("profile_language")
                                    )
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .clickable { expandedLang = true }
                                    )
                                    DropdownMenu(
                                        expanded = expandedLang,
                                        onDismissRequest = { expandedLang = false },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        listOf("English", "Spanish", "French", "German", "Japanese", "Portuguese").forEach { lang ->
                                            DropdownMenuItem(
                                                text = { Text(lang) },
                                                onClick = {
                                                    editLanguage = lang
                                                    expandedLang = false
                                                    autoSave(language = lang)
                                                }
                                            )
                                        }
                                    }
                                }

                                // Date Format dropdown
                                var expandedDateFormat by remember { mutableStateOf(false) }
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = editDateFormat,
                                        onValueChange = {},
                                        label = { Text("App Date Format", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                        readOnly = true,
                                        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                                        trailingIcon = {
                                            IconButton(onClick = { expandedDateFormat = true }) {
                                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().testTag("profile_date_format")
                                    )
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .clickable { expandedDateFormat = true }
                                    )
                                    DropdownMenu(
                                        expanded = expandedDateFormat,
                                        onDismissRequest = { expandedDateFormat = false },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        listOf(
                                            "yyyy-MM-dd",
                                            "MM/dd/yyyy",
                                            "dd/MM/yyyy",
                                            "dd-MM-yyyy",
                                            "MMM dd, yyyy",
                                            "d MMM yyyy"
                                        ).forEach { formatOption ->
                                            DropdownMenuItem(
                                                text = { Text(formatOption) },
                                                onClick = {
                                                    editDateFormat = formatOption
                                                    expandedDateFormat = false
                                                    autoSave(dateFormat = formatOption)
                                                }
                                            )
                                        }
                                    }
                                }

                                // Scheduled Subs count field
                                OutlinedTextField(
                                    value = editDefaultCountStr,
                                    onValueChange = { newValue ->
                                        editDefaultCountStr = newValue
                                        autoSave(defaultCountStr = newValue)
                                    },
                                    label = { Text("Default Scheduled Subs to Create", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    placeholder = { Text("6") },
                                    leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth().testTag("profile_scheduled_count")
                                )

                                // Display Amounts toggle setting
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Visibility,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Column {
                                            Text("Display amounts", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                            Text(
                                                "Default visibility for costs in stats view",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Switch(
                                        checked = editDisplayAmounts,
                                        onCheckedChange = { newValue ->
                                            editDisplayAmounts = newValue
                                            autoSave(displayAmounts = newValue)
                                        },
                                        modifier = Modifier.testTag("profile_display_amounts")
                                    )
                                }
                            }
                        }
                    }
                } else if (selectedTab == 2) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Theme Mode selection
                                Text("App Theme Mode", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val options = listOf(
                                        Triple("system", "System", Icons.Default.Settings),
                                        Triple("light", "Light", Icons.Default.LightMode),
                                        Triple("dark", "Dark", Icons.Default.DarkMode)
                                    )
                                    options.forEach { (mode, label, icon) ->
                                        val isSelected = editThemeMode == mode
                                        Card(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(72.dp)
                                                .clickable {
                                                    editThemeMode = mode
                                                    viewModel.updateThemeMode(mode)
                                                    autoSave(themeMode = mode)
                                                }
                                                .testTag("theme_mode_$mode"),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                                        ) {
                                            Column(
                                                modifier = Modifier.fillMaxSize().padding(8.dp),
                                                verticalArrangement = Arrangement.Center,
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(24.dp))
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // App Theme Color Combination Selection
                                Text("App Color Scheme", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                val isDarkPreview = when (editThemeMode) {
                                    "dark" -> true
                                    "light" -> false
                                    else -> isSystemInDarkTheme()
                                }

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val combos = listOf(
                                        ThemeComboInfo("default", "Classic Purple", "Original Material theme colors",
                                                       Color(0xFF6650A4), Color(0xFF625B71), Color(0xFF7D5260),
                                                       Color(0xFFD0BCFF), Color(0xFFCCC2DC), Color(0xFFEFB8C8)),
                                        ThemeComboInfo("summer", "Golden Summer Fields", "Sandy golden light tones with dark golden variant",
                                                       Color(0xFFD4A373), Color(0xFFCCD5AE), Color(0xFFFAEDCD),
                                                       Color(0xFFE0B48A), Color(0xFFCCD5AE), Color(0xFF3B4422)),
                                        ThemeComboInfo("cozy", "Cozy Pastel Home", "Soft peach warm tones with cozy deep dark variant",
                                                       Color(0xFFD19E82), Color(0xFFEDB791), Color(0xFFF7D3AD),
                                                       Color(0xFFEBAF94), Color(0xFFF2CBB2), Color(0xFF5E3A25)),
                                        ThemeComboInfo("winter", "Winter Wonderland", "Cream and warm sand tones with charcoal frost dark variant",
                                                       Color(0xFFC4B69E), Color(0xFFE7DECD), Color(0xFFEFE8DB),
                                                       Color(0xFFDDD3C1), Color(0xFFC4B69E), Color(0xFF3A3427)),
                                        ThemeComboInfo("serenity", "Soft Pastel Serenity", "Gentle cream light tones with deep serenity dark variant",
                                                       Color(0xFFD4C294), Color(0xFFEEE4C3), Color(0xFFF3ECD3),
                                                       Color(0xFFE6D7AB), Color(0xFFD4C294), Color(0xFF423B1E)),
                                        ThemeComboInfo("green_serenity", "Green Serenity", "Sage green light tones with dark green teal variant",
                                                       Color(0xFF588157), Color(0xFF709775), Color(0xFFA1CCA5),
                                                       Color(0xFFA1CCA5), Color(0xFF8FB996), Color(0xFF1C3A23)),
                                        ThemeComboInfo("blood_moon", "Blood Moon Serenity", "Crimson warm tones with blood moon teal-dark variant",
                                                       Color(0xFF6A3937), Color(0xFF706563), Color(0xFF748386),
                                                       Color(0xFF9DC7C8), Color(0xFF748386), Color(0xFF3B0D11)),
                                        ThemeComboInfo("earthy_harmony", "Earthy Harmony", "Warm dusty rose tones with dark earthy berry variant",
                                                       Color(0xFF896A67), Color(0xFF6B4D57), Color(0xFFDDC8C4),
                                                       Color(0xFFDDC8C4), Color(0xFF6B4D57), Color(0xFF24151C)),
                                        ThemeComboInfo("chocolate_sunset", "Chocolate Sunset", "Earthy sunset light tones with deep berry dark variant",
                                                       Color(0xFF8E443D), Color(0xFFCB9173), Color(0xFFE0D68A),
                                                       Color(0xFFCB9173), Color(0xFFE0D68A), Color(0xFF511730)),
                                        ThemeComboInfo("red_sunburst", "Red Sunburst", "Fiery sunburst light tones with crimson sunset dark variant",
                                                       Color(0xFFCE4257), Color(0xFFFF7F51), Color(0xFFFF9B54),
                                                       Color(0xFFCE4257), Color(0xFFFF7F51), Color(0xFF5E121E)),
                                        ThemeComboInfo("earthy_forest", "Earthy Forest Hues", "Earthy forest greens with deep dark sage variant",
                                                       Color(0xFF344E41), Color(0xFF588157), Color(0xFFDAD7CD),
                                                       Color(0xFFA3B18A), Color(0xFF588157), Color(0xFF263D31)),
                                        ThemeComboInfo("deep_sea", "Deep Sea Blue", "Ice blue light tones with deep navy dark variant",
                                                       Color(0xFF415A77), Color(0xFF778DA9), Color(0xFF1B263B),
                                                       Color(0xFF778DA9), Color(0xFF415A77), Color(0xFF1B263B)),
                                        ThemeComboInfo("soft_lavender", "Soft Lavender", "Muted lavender with deep lavender-navy dark variant",
                                                       Color(0xFF4A4E69), Color(0xFF9A8C98), Color(0xFFC9ADA7),
                                                       Color(0xFFC9ADA7), Color(0xFF9A8C98), Color(0xFF22223B))
                                    )
                                    combos.forEach { item ->
                                        val isSelected = editThemeCombo == item.id
                                        val primColor = if (isDarkPreview) item.darkPrimary else item.lightPrimary
                                        val secColor = if (isDarkPreview) item.darkSecondary else item.lightSecondary
                                        val tertColor = if (isDarkPreview) item.darkTertiary else item.lightTertiary

                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    editThemeCombo = item.id
                                                    viewModel.updateThemeCombo(item.id)
                                                    autoSave(themeCombo = item.id)
                                                }
                                                .testTag("theme_combo_${item.id}"),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                                else MaterialTheme.colorScheme.onSurface
                                            ),
                                            border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(item.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                    Text(item.desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(modifier = Modifier.size(16.dp).background(primColor, shape = CircleShape))
                                                    Box(modifier = Modifier.size(16.dp).background(secColor, shape = CircleShape))
                                                    Box(modifier = Modifier.size(16.dp).background(tertColor, shape = CircleShape))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (selectedTab == 3) {
                    // ADVANCED SETTINGS TAB
                    // 1. Button on top to generate past scheduled subscriptions
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DateRange,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                    Text(
                                        text = "Generate Past Subscriptions",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Retroactively generate scheduled deliveries for your subscriptions between a start date and end date based on their recurrence schedule.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = { showGeneratePastSubsDialog = true },
                                    modifier = Modifier.fillMaxWidth().testTag("profile_generate_past_subs"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(imageVector = Icons.Default.AutoMode, contentDescription = null)
                                        Text("Generate Past Scheduled Subscriptions")
                                    }
                                }
                            }
                        }
                    }

                    // 2. Excel Spreadsheet Backup Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.secondary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(imageVector = Icons.Default.GridOn, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondary)
                                    }
                                    Text(
                                        text = "Excel Spreadsheet Backup",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Export your entire offline library (bookstores, subscriptions, deliveries, and preorders) to a standard spreadsheet file (.csv) that you can open instantly in Excel or Google Sheets.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = { viewModel.exportToExcelSpreadsheet(context) },
                                    modifier = Modifier.fillMaxWidth().testTag("profile_export_excel"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary
                                    )
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(imageVector = Icons.Default.Share, contentDescription = null)
                                        Text("Export and Share Spreadsheet")
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        AnimatedVisibility(
            visible = showSavedToast,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .testTag("saved_changes_toast")
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFFE8F5E9),
                contentColor = Color(0xFF2E7D32),
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, Color(0xFFA5D6A7))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Saved changes",
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        if (showGeneratePastSubsDialog) {
            GeneratePastSubsDialog(
                viewModel = viewModel,
                dateFormat = editDateFormat,
                onDismiss = { showGeneratePastSubsDialog = false }
            )
        }

        if (showAddAddressDialog) {
            AddressFormDialog(
                initialAddress = null,
                forwardingServices = forwardingServices,
                onDismiss = { showAddAddressDialog = false },
                onSave = { newAddress ->
                    viewModel.insertUserAddress(newAddress)
                    showAddAddressDialog = false
                    Toast.makeText(context, "Address saved", Toast.LENGTH_SHORT).show()
                }
            )
        }

        editingAddress?.let { addressToEdit ->
            AddressFormDialog(
                initialAddress = addressToEdit,
                forwardingServices = forwardingServices,
                onDismiss = { editingAddress = null },
                onSave = { updatedAddress ->
                    viewModel.updateUserAddress(updatedAddress)
                    editingAddress = null
                    Toast.makeText(context, "Address updated", Toast.LENGTH_SHORT).show()
                },
                onDelete = { addressToDelete ->
                    viewModel.deleteUserAddress(addressToDelete)
                    editingAddress = null
                    Toast.makeText(context, "Address deleted", Toast.LENGTH_SHORT).show()
                }
            )
        }

        previewImageUrl?.let { url ->
            ImageViewerDialog(
                imageUrl = url,
                onDismiss = { previewImageUrl = null }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GeneratePastSubsDialog(
    viewModel: BookishViewModel,
    dateFormat: String,
    onDismiss: () -> Unit
) {
    val bookstores by viewModel.bookstoresState.collectAsState()
    val rawSubs by viewModel.rawSubscriptionsState.collectAsState()
    val context = LocalContext.current

    var selectedBookstore by remember { mutableStateOf<Bookstore?>(null) }
    var selectedSubscription by remember { mutableStateOf<SubscriptionType?>(null) }
    var expandedBookstore by remember { mutableStateOf(false) }
    var expandedSubscription by remember { mutableStateOf(false) }

    // Default start date: 6 months ago
    var startDateMs by remember {
        mutableStateOf(
            Calendar.getInstance().apply {
                add(Calendar.MONTH, -6)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        )
    }

    // Default end date: TODAY
    var endDateMs by remember {
        mutableStateOf(
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        )
    }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val formatter = remember(dateFormat) {
        try {
            SimpleDateFormat(dateFormat, Locale.getDefault())
        } catch (e: Exception) {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        }
    }

    val availableSubs = remember(selectedBookstore, rawSubs) {
        if (selectedBookstore == null) {
            rawSubs.map { it.subscription }
        } else {
            rawSubs.filter { it.subscription.bookstoreId == selectedBookstore?.id }.map { it.subscription }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Generate Past Subscriptions",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "Select a bookstore and subscription type to generate missing past scheduled deliveries up to the end date.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 1. Bookstore Selection
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedBookstore?.name ?: "All Bookstores",
                        onValueChange = {},
                        label = { Text("Bookstore", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        readOnly = true,
                        leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { expandedBookstore = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("dialog_bookstore_select")
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { expandedBookstore = true }
                    )
                    DropdownMenu(
                        expanded = expandedBookstore,
                        onDismissRequest = { expandedBookstore = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Bookstores", fontWeight = FontWeight.Bold) },
                            onClick = {
                                selectedBookstore = null
                                selectedSubscription = null
                                expandedBookstore = false
                            }
                        )
                        bookstores.forEach { b ->
                            DropdownMenuItem(
                                text = { Text(b.name) },
                                onClick = {
                                    selectedBookstore = b
                                    selectedSubscription = null
                                    expandedBookstore = false
                                }
                            )
                        }
                    }
                }

                // 2. Subscription Type Selection
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedSubscription?.title ?: "All Subscription Types",
                        onValueChange = {},
                        label = { Text("Subscription Type", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        readOnly = true,
                        leadingIcon = { Icon(Icons.Default.Bookmark, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { expandedSubscription = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("dialog_subscription_select")
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { expandedSubscription = true }
                    )
                    DropdownMenu(
                        expanded = expandedSubscription,
                        onDismissRequest = { expandedSubscription = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Subscription Types", fontWeight = FontWeight.Bold) },
                            onClick = {
                                selectedSubscription = null
                                expandedSubscription = false
                            }
                        )
                        availableSubs.forEach { sub ->
                            DropdownMenuItem(
                                text = { Text("${sub.title} (${sub.frequency})") },
                                onClick = {
                                    selectedSubscription = sub
                                    expandedSubscription = false
                                }
                            )
                        }
                    }
                }

                // 3. Start Date Selection
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = formatter.format(Date(startDateMs)),
                        onValueChange = {},
                        label = { Text("Start Date", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        readOnly = true,
                        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { showStartDatePicker = true }) {
                                Icon(Icons.Default.EditCalendar, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("dialog_start_date")
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showStartDatePicker = true }
                    )
                }

                // 4. End Date Selection
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = formatter.format(Date(endDateMs)),
                        onValueChange = {},
                        label = { Text("End Date (Default: Today)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        readOnly = true,
                        leadingIcon = { Icon(Icons.Default.Event, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { showEndDatePicker = true }) {
                                Icon(Icons.Default.EditCalendar, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("dialog_end_date")
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showEndDatePicker = true }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.generatePastScheduledSubscriptions(
                        bookstoreId = selectedBookstore?.id,
                        subscriptionTypeId = selectedSubscription?.id,
                        startDate = startDateMs,
                        endDate = endDateMs
                    )
                    Toast.makeText(context, "Past scheduled subscriptions generated!", Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
                modifier = Modifier.testTag("dialog_btn_generate"),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Generate")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dialog_btn_cancel")
            ) {
                Text("Cancel")
            }
        }
    )

    if (showStartDatePicker) {
        ProfileDatePickerDialog(
            initialTimeMs = startDateMs,
            onDateSelected = { selectedLocalMs ->
                startDateMs = selectedLocalMs
                showStartDatePicker = false
            },
            onDismiss = { showStartDatePicker = false }
        )
    }

    if (showEndDatePicker) {
        ProfileDatePickerDialog(
            initialTimeMs = endDateMs,
            onDateSelected = { selectedLocalMs ->
                endDateMs = selectedLocalMs
                showEndDatePicker = false
            },
            onDismiss = { showEndDatePicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileDatePickerDialog(
    initialTimeMs: Long,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val initialUtc = localTimeToUtcMidnight(initialTimeMs)
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialUtc)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { selectedUtc ->
                    onDateSelected(utcMidnightToLocalStartOfDay(selectedUtc))
                }
                onDismiss()
            }) {
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

private fun utcMidnightToLocalStartOfDay(timeMs: Long): Long {
    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = timeMs
    }
    val localCal = Calendar.getInstance().apply {
        set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
        set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
        set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return localCal.timeInMillis
}

private fun localTimeToUtcMidnight(timeMs: Long): Long {
    val localCal = Calendar.getInstance().apply {
        timeInMillis = timeMs
    }
    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        set(Calendar.YEAR, localCal.get(Calendar.YEAR))
        set(Calendar.MONTH, localCal.get(Calendar.MONTH))
        set(Calendar.DAY_OF_MONTH, localCal.get(Calendar.DAY_OF_MONTH))
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return utcCal.timeInMillis
}

private data class ThemeComboInfo(
    val id: String,
    val name: String,
    val desc: String,
    val lightPrimary: Color,
    val lightSecondary: Color,
    val lightTertiary: Color,
    val darkPrimary: Color,
    val darkSecondary: Color,
    val darkTertiary: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddressFormDialog(
    initialAddress: UserAddress? = null,
    forwardingServices: List<ForwardingService>,
    onDismiss: () -> Unit,
    onSave: (UserAddress) -> Unit,
    onDelete: ((UserAddress) -> Unit)? = null
) {
    val context = LocalContext.current
    var streetAddress1 by remember { mutableStateOf(initialAddress?.streetAddress1 ?: "") }
    var streetAddress2 by remember { mutableStateOf(initialAddress?.streetAddress2 ?: "") }
    var city by remember { mutableStateOf(initialAddress?.city ?: "") }
    var stateProvinceRegion by remember { mutableStateOf(initialAddress?.stateProvinceRegion ?: "") }
    var postalCode by remember { mutableStateOf(initialAddress?.postalCode ?: "") }
    var country by remember { mutableStateOf(initialAddress?.country ?: "") }
    var selectedForwardingServiceId by remember { mutableStateOf(initialAddress?.forwardingServiceId) }
    var isDefault by remember { mutableStateOf(initialAddress?.isDefault ?: false) }

    var street1Error by remember { mutableStateOf(false) }
    var cityError by remember { mutableStateOf(false) }
    var postalCodeError by remember { mutableStateOf(false) }
    var countryError by remember { mutableStateOf(false) }

    var expandedForwardingDropdown by remember { mutableStateOf(false) }
    var expandedCountryDropdown by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val allCountries = remember {
        val isoCountries = Locale.getISOCountries()
            .map { Locale("", it).displayCountry }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
        if (isoCountries.isNotEmpty()) isoCountries else listOf(
            "Australia", "Austria", "Belgium", "Brazil", "Canada", "China", "Denmark",
            "Finland", "France", "Germany", "Hong Kong", "Ireland", "Italy", "Japan",
            "Mexico", "Netherlands", "New Zealand", "Norway", "Poland", "Portugal",
            "Singapore", "South Korea", "Spain", "Sweden", "Switzerland", "Taiwan",
            "United Kingdom", "United States"
        )
    }

    val filteredCountries = remember(country, allCountries) {
        if (country.isBlank()) allCountries
        else allCountries.filter { it.contains(country, ignoreCase = true) }
    }

    val selectedForwardingService = forwardingServices.find { it.id == selectedForwardingServiceId }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (initialAddress == null) Icons.Default.Add else Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (initialAddress == null) "Add Address" else "Edit Address",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Forwarding Service Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedForwardingService?.name ?: "None (Direct Delivery)",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Forwarding Service (Optional)") },
                        leadingIcon = { Icon(Icons.Default.LocalShipping, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { expandedForwardingDropdown = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("address_form_forwarding_service")
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { expandedForwardingDropdown = true }
                    )
                    DropdownMenu(
                        expanded = expandedForwardingDropdown,
                        onDismissRequest = { expandedForwardingDropdown = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("None (Direct Delivery)") },
                            leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
                            onClick = {
                                selectedForwardingServiceId = null
                                expandedForwardingDropdown = false
                            }
                        )
                        for (service in forwardingServices) {
                            DropdownMenuItem(
                                text = { Text(service.name) },
                                leadingIcon = { Icon(Icons.Default.LocalShipping, contentDescription = null) },
                                onClick = {
                                    selectedForwardingServiceId = service.id
                                    expandedForwardingDropdown = false
                                }
                            )
                        }
                    }
                }

                // Street Address 1
                OutlinedTextField(
                    value = streetAddress1,
                    onValueChange = {
                        streetAddress1 = it
                        if (street1Error && it.isNotBlank()) {
                            street1Error = false
                        }
                    },
                    label = { Text("Street Address 1 *") },
                    placeholder = { Text("e.g. 123 Main St") },
                    leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
                    isError = street1Error,
                    supportingText = if (street1Error) {
                        { Text("Street address is mandatory", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    trailingIcon = if (street1Error) {
                        { Icon(Icons.Default.Error, contentDescription = "Error", tint = MaterialTheme.colorScheme.error) }
                    } else null,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("address_form_street1")
                )

                // Street Address 2
                OutlinedTextField(
                    value = streetAddress2,
                    onValueChange = { streetAddress2 = it },
                    label = { Text("Street Address 2 (Apt, Suite, Unit)") },
                    placeholder = { Text("e.g. Apt 4B, Suite 200") },
                    leadingIcon = { Icon(Icons.Default.LocationCity, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("address_form_street2")
                )

                // City
                OutlinedTextField(
                    value = city,
                    onValueChange = {
                        city = it
                        if (cityError && it.isNotBlank()) {
                            cityError = false
                        }
                    },
                    label = { Text("City *") },
                    placeholder = { Text("City") },
                    leadingIcon = { Icon(Icons.Default.LocationCity, contentDescription = null) },
                    isError = cityError,
                    supportingText = if (cityError) {
                        { Text("City is mandatory", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    trailingIcon = if (cityError) {
                        { Icon(Icons.Default.Error, contentDescription = "Error", tint = MaterialTheme.colorScheme.error) }
                    } else null,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("address_form_city")
                )

                // State / Region
                OutlinedTextField(
                    value = stateProvinceRegion,
                    onValueChange = { stateProvinceRegion = it },
                    label = { Text("State / Region") },
                    placeholder = { Text("State") },
                    leadingIcon = { Icon(Icons.Default.Map, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("address_form_state")
                )

                // Postal Code
                OutlinedTextField(
                    value = postalCode,
                    onValueChange = {
                        postalCode = it
                        if (postalCodeError && it.isNotBlank()) {
                            postalCodeError = false
                        }
                    },
                    label = { Text("Postal / ZIP *") },
                    placeholder = { Text("Postal Code") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    isError = postalCodeError,
                    supportingText = if (postalCodeError) {
                        { Text("Postal / ZIP code is mandatory", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    trailingIcon = if (postalCodeError) {
                        { Icon(Icons.Default.Error, contentDescription = "Error", tint = MaterialTheme.colorScheme.error) }
                    } else null,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("address_form_postal")
                )

                // Country with Dropdown Menu
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = country,
                        onValueChange = {
                            country = it
                            expandedCountryDropdown = true
                            if (countryError && it.isNotBlank()) {
                                countryError = false
                            }
                        },
                        label = { Text("Country *") },
                        placeholder = { Text("Select or type country") },
                        leadingIcon = { Icon(Icons.Default.Public, contentDescription = null) },
                        isError = countryError,
                        supportingText = if (countryError) {
                            { Text("Country is mandatory", color = MaterialTheme.colorScheme.error) }
                        } else null,
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (countryError) {
                                    Icon(
                                        Icons.Default.Error,
                                        contentDescription = "Error",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                }
                                IconButton(onClick = { expandedCountryDropdown = !expandedCountryDropdown }) {
                                    Icon(
                                        imageVector = if (expandedCountryDropdown) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = "Select country"
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("address_form_country")
                    )
                    DropdownMenu(
                        expanded = expandedCountryDropdown,
                        onDismissRequest = { expandedCountryDropdown = false },
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .heightIn(max = 280.dp)
                    ) {
                        if (filteredCountries.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No matching country found", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                onClick = {}
                            )
                        } else {
                            filteredCountries.take(50).forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(item) },
                                    leadingIcon = { Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    onClick = {
                                        country = item
                                        expandedCountryDropdown = false
                                        countryError = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Default Address Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (isDefault) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Column {
                            Text("Default Address", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(
                                "Use as preferred shipping address",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = isDefault,
                        onCheckedChange = { isDefault = it },
                        modifier = Modifier.testTag("address_form_default_switch")
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (initialAddress != null && onDelete != null) {
                    Button(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("address_form_delete")
                    ) {
                        Text("Delete")
                    }
                }

                Button(
                    onClick = {
                        val hasStreetError = streetAddress1.isBlank()
                        val hasCityError = city.isBlank()
                        val hasPostalError = postalCode.isBlank()
                        val hasCountryError = country.isBlank()

                        if (hasStreetError || hasCityError || hasPostalError || hasCountryError) {
                            street1Error = hasStreetError
                            cityError = hasCityError
                            postalCodeError = hasPostalError
                            countryError = hasCountryError
                            Toast.makeText(context, "Please fill in all mandatory fields", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        onSave(
                            UserAddress(
                                id = initialAddress?.id ?: 0,
                                userId = 1,
                                streetAddress1 = streetAddress1.trim(),
                                streetAddress2 = streetAddress2.trim(),
                                city = city.trim(),
                                stateProvinceRegion = stateProvinceRegion.trim(),
                                postalCode = postalCode.trim(),
                                country = country.trim(),
                                forwardingServiceId = selectedForwardingServiceId,
                                isDefault = isDefault
                            )
                        )
                    },
                    modifier = Modifier.testTag("address_form_save")
                ) {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("address_form_cancel")
            ) {
                Text("Cancel")
            }
        }
    )

    if (showDeleteConfirm && initialAddress != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Address?") },
            text = { Text("Are you sure you want to delete '${initialAddress.streetAddress1}'? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(initialAddress)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

