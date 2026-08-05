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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.example.data.User
import com.example.ui.viewmodel.BookishViewModel

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
                                            if (editUsername.isNotEmpty()) {
                                                val parsedCount = editDefaultCountStr.toIntOrNull() ?: 6
                                                viewModel.updateProfile(
                                                    username = editUsername,
                                                    currency = editCurrency,
                                                    language = editLanguage,
                                                    profilePic = key,
                                                    defaultScheduledSubCount = parsedCount,
                                                    themeMode = editThemeMode,
                                                    themeCombo = editThemeCombo,
                                                    dateFormat = editDateFormat,
                                                    displayAmounts = editDisplayAmounts
                                                )
                                                showSavedToast = true
                                            }
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

            // Profile Tabs for Settings and Theme & Colors
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
                        text = { Text("Settings", fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings Tab") },
                        modifier = Modifier.testTag("tab_settings")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Theme & Colors", fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.Palette, contentDescription = "Theme Tab") },
                        modifier = Modifier.testTag("tab_theme")
                    )
                }
            }

            // Profile Fields Form Section
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
                        if (selectedTab == 0) {
                            Text("Personal Information", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                            // Username textfield
                            OutlinedTextField(
                                value = editUsername,
                                onValueChange = { editUsername = it },
                                label = { Text("Display Username") },
                                modifier = Modifier.fillMaxWidth().testTag("profile_username"),
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                            )

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
                                    label = { Text("App Currency") },
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
                                    label = { Text("App Language") },
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
                                    label = { Text("App Date Format") },
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
                                            }
                                        )
                                    }
                                }
                            }

                            // Scheduled Subs count field
                            OutlinedTextField(
                                value = editDefaultCountStr,
                                onValueChange = { editDefaultCountStr = it },
                                label = { Text("Default Scheduled Subs to Create") },
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
                                    onCheckedChange = {
                                        editDisplayAmounts = it
                                        if (editUsername.isNotEmpty()) {
                                            val parsedCount = editDefaultCountStr.toIntOrNull() ?: 6
                                            viewModel.updateProfile(editUsername, editCurrency, editLanguage, editProfilePic, parsedCount, editThemeMode, editThemeCombo, editDateFormat, it)
                                        }
                                    },
                                    modifier = Modifier.testTag("profile_display_amounts")
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Save Button
                            Button(
                                onClick = {
                                    if (editUsername.isNotEmpty()) {
                                        val parsedCount = editDefaultCountStr.toIntOrNull() ?: 6
                                        viewModel.updateProfile(editUsername, editCurrency, editLanguage, editProfilePic, parsedCount, editThemeMode, editThemeCombo, editDateFormat, editDisplayAmounts)
                                        showSavedToast = true
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().testTag("profile_save"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Save Settings")
                            }
                        } else {
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
                                            }
                                            .testTag("theme_mode_$mode"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                                                             else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer 
                                                           else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
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
                                            }
                                            .testTag("theme_combo_${item.id}"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                                                             else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                                           else MaterialTheme.colorScheme.onSurface
                                        ),
                                        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
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
            }

            // SPREADSHEET EXPORT CARD (EXCEL BACKUP) - Only show on Settings tab
            if (selectedTab == 0) {
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
    }
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
