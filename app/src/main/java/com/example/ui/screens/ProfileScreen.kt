package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

    // Sync form with user state when loaded
    LaunchedEffect(user) {
        user?.let {
            editUsername = it.username
            editCurrency = it.currency
            editLanguage = it.language
            editProfilePic = it.profilePic ?: "avatar_classic"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile & Settings", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
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
                                        .clickable { editProfilePic = key }
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
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = editCurrency,
                                onValueChange = {},
                                label = { Text("App Currency") },
                                readOnly = true,
                                leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null) },
                                trailingIcon = {
                                    IconButton(onClick = { expandedCurrency = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().clickable { expandedCurrency = true }
                            )
                            DropdownMenu(
                                expanded = expandedCurrency,
                                onDismissRequest = { expandedCurrency = false },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                listOf("$", "€", "£", "¥", "₩", "₹").forEach { curr ->
                                    DropdownMenuItem(
                                        text = { Text(curr) },
                                        onClick = {
                                            editCurrency = curr
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
                                modifier = Modifier.fillMaxWidth().clickable { expandedLang = true }
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

                        // Save Button
                        Button(
                            onClick = {
                                if (editUsername.isNotEmpty()) {
                                    viewModel.updateProfile(editUsername, editCurrency, editLanguage, editProfilePic)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("profile_save"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Save Changes")
                        }
                    }
                }
            }

            // SPREADSHEET EXPORT CARD (EXCEL BACKUP)
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

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
