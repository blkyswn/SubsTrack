package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.Bookstore
import com.example.ui.viewmodel.BookishViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookstoresScreen(
    viewModel: BookishViewModel,
    modifier: Modifier = Modifier
) {
    val bookstores by viewModel.bookstoresState.collectAsState()
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedBookstore by remember { mutableStateOf<Bookstore?>(null) }
    var showDetailDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }

    val filteredBookstores = remember(bookstores, searchQuery) {
        if (searchQuery.isBlank()) {
            bookstores
        } else {
            bookstores.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.url.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearching) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search bookstores...", fontSize = 16.sp) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            textStyle = LocalTextStyle.current.copy(fontSize = 16.sp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("bookstores_search_input"),
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear Search")
                                    }
                                }
                            }
                        )
                    } else {
                        Text("Bookstores", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                },
                navigationIcon = {
                    if (isSearching) {
                        IconButton(onClick = {
                            isSearching = false
                            searchQuery = ""
                        }) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (!isSearching) {
                        IconButton(onClick = { isSearching = true }) {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "Search Bookstores")
                        }
                        IconButton(
                            onClick = { showAddDialog = true },
                            modifier = Modifier.testTag("add_bookstore")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Bookstore")
                        }
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        if (bookstores.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No bookstores registered yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(onClick = { showAddDialog = true }) {
                        Text("Add Your First Bookstore")
                    }
                }
            }
        } else {
            val groupedBookstores = remember(filteredBookstores) {
                filteredBookstores
                    .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
                    .groupBy { it.name.firstOrNull()?.uppercaseChar() ?: '#' }
            }

            if (filteredBookstores.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No matching bookstores found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        TextButton(onClick = { searchQuery = "" }) {
                            Text("Clear Search")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
                ) {
                    groupedBookstores.forEach { (initial, stores) ->
                        item {
                            Text(
                                text = initial.toString(),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 4.dp)
                            )
                        }
                        items(stores) { bookstore ->
                            BookstoreListItem(
                                bookstore = bookstore,
                                onClick = {
                                    selectedBookstore = bookstore
                                    showDetailDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }

        // Add Bookstore Dialog
        if (showAddDialog) {
            var name by remember { mutableStateOf("") }
            var url by remember { mutableStateOf("") }
            var imageUrl by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = {
                    showAddDialog = false
                },
                title = { Text("Add Bookstore", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Round Bookstore Image Picker with Pencil Button
                        BookstoreImagePicker(
                            imageUrl = imageUrl,
                            onImageSelected = { imageUrl = it }
                        )

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Bookstore Name") },
                            modifier = Modifier.fillMaxWidth().testTag("add_bookstore_name")
                        )
                        OutlinedTextField(
                            value = url,
                            onValueChange = { url = it },
                            label = { Text("URL Link (e.g. https://...)") },
                            modifier = Modifier.fillMaxWidth().testTag("add_bookstore_url")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (name.isNotEmpty()) {
                                viewModel.addBookstore(name, url, imageUrl.ifEmpty { null })
                                showAddDialog = false
                            }
                        },
                        modifier = Modifier.testTag("confirm_add_bookstore")
                    ) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showAddDialog = false
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Details Popup Card Dialog
        if (showDetailDialog && selectedBookstore != null) {
            val store = selectedBookstore!!
            AlertDialog(
                onDismissRequest = { showDetailDialog = false },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!store.profilePic.isNullOrEmpty()) {
                                AsyncImage(
                                    model = store.profilePic,
                                    contentDescription = "${store.name} Logo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(imageVector = Icons.Default.Storefront, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Text(store.name, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Website URL:", fontWeight = FontWeight.Medium, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = store.url.ifEmpty { "No website link saved" },
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable {
                                    if (store.url.isNotEmpty()) {
                                        try {
                                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(store.url))
                                            context.startActivity(browserIntent)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                }
                                .padding(vertical = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "This bookstore manages your subscriptions and preorders locally on your device.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDetailDialog = false
                            showEditDialog = true
                        }
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Edit Details")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDetailDialog = false }) { Text("Close") }
                }
            )
        }

        // Edit Bookstore Dialog
        if (showEditDialog && selectedBookstore != null) {
            val store = selectedBookstore!!
            var editName by remember { mutableStateOf(store.name) }
            var editUrl by remember { mutableStateOf(store.url) }
            var editImageUrl by remember { mutableStateOf(store.profilePic ?: "") }

            AlertDialog(
                onDismissRequest = {
                    showEditDialog = false
                },
                title = { Text("Edit Bookstore", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Round Bookstore Image Picker with Pencil Button
                        BookstoreImagePicker(
                            imageUrl = editImageUrl,
                            onImageSelected = { editImageUrl = it }
                        )

                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text("Bookstore Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editUrl,
                            onValueChange = { editUrl = it },
                            label = { Text("URL Link") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                viewModel.deleteBookstore(store)
                                showEditDialog = false
                                selectedBookstore = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Delete")
                        }

                        Button(
                            onClick = {
                                if (editName.isNotEmpty()) {
                                    viewModel.updateBookstore(store.copy(
                                        name = editName,
                                        url = editUrl,
                                        profilePic = editImageUrl.ifEmpty { null }
                                    ))
                                    showEditDialog = false
                                    selectedBookstore = null
                                }
                            }
                        ) {
                            Text("Save")
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showEditDialog = false
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun BookstoreListItem(
    bookstore: Bookstore,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
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
                    .height(48.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )

            Spacer(modifier = Modifier.width(14.dp))

            // Icon Avatar Box
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                if (!bookstore.profilePic.isNullOrEmpty()) {
                    AsyncImage(
                        model = bookstore.profilePic,
                        contentDescription = "${bookstore.name} Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Bookstore Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bookstore.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = bookstore.url.ifEmpty { "No website link saved" },
                    fontSize = 12.sp,
                    color = if (bookstore.url.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Trailing Chevron / Action button
            IconButton(
                onClick = {
                    if (bookstore.url.isNotEmpty()) {
                        try {
                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(bookstore.url))
                            context.startActivity(browserIntent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } else {
                        onClick()
                    }
                }
            ) {
                Icon(
                    imageVector = if (bookstore.url.isNotEmpty()) Icons.Default.OpenInNew else Icons.Default.ChevronRight,
                    contentDescription = if (bookstore.url.isNotEmpty()) "Open Website" else "View Details",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun BookstoreImagePicker(
    imageUrl: String,
    onImageSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onImageSelected(uri.toString())
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    shape = CircleShape
                )
                .clickable { photoPickerLauncher.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            if (imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Selected Bookstore Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Storefront,
                    contentDescription = "Default Bookstore Icon",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        // Pencil Edit Button on the bottom-right corner of the circle
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = 35.dp, y = 35.dp)
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                .clickable { photoPickerLauncher.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit Image",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

