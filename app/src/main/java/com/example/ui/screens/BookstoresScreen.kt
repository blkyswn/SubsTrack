package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.example.utils.saveImageToInternalStorage
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
import com.example.ui.components.InteractiveImagePicker
import com.example.ui.components.ImageOptionsDialog
import com.example.ui.components.ImageViewerDialog
import com.example.data.Bookstore
import com.example.data.BookstoreContact
import com.example.data.ForwardingService
import com.example.data.ForwardingServiceContact
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.ui.viewmodel.BookishViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookstoresScreen(
    viewModel: BookishViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) } // 0: Bookstores, 1: Forwarding Services

    val bookstores by viewModel.bookstoresState.collectAsState()
    val allBookstoreContacts by viewModel.bookstoreContactsState.collectAsState()

    val forwardingServices by viewModel.forwardingServicesState.collectAsState()
    val allForwardingServiceContacts by viewModel.forwardingServiceContactsState.collectAsState()

    val context = LocalContext.current

    // Bookstores Dialog States
    var showAddBookstoreDialog by remember { mutableStateOf(false) }
    var selectedBookstore by remember { mutableStateOf<Bookstore?>(null) }
    var showDetailDialog by remember { mutableStateOf(false) }
    var showEditBookstoreDialog by remember { mutableStateOf(false) }

    // Forwarding Services Dialog States
    var showAddForwardingServiceDialog by remember { mutableStateOf(false) }
    var selectedForwardingService by remember { mutableStateOf<ForwardingService?>(null) }
    var showEditForwardingServiceDialog by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val onDeleteBookstore: (Bookstore) -> Unit = { bookstore ->
        viewModel.deleteBookstore(bookstore)
        coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = "Deleted bookstore: ${bookstore.name}",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.restoreBookstore(bookstore)
            }
        }
    }

    val onDeleteForwardingService: (ForwardingService) -> Unit = { service ->
        viewModel.deleteForwardingService(service)
        coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = "Deleted forwarding service: ${service.name}",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.restoreForwardingService(service)
            }
        }
    }

    val filteredBookstores = remember(bookstores, allBookstoreContacts, searchQuery) {
        if (searchQuery.isBlank()) {
            bookstores
        } else {
            bookstores.filter { store ->
                store.name.contains(searchQuery, ignoreCase = true) ||
                        store.website.contains(searchQuery, ignoreCase = true) ||
                        allBookstoreContacts.any { it.bookstoreId == store.id && it.contactValue.contains(searchQuery, ignoreCase = true) }
            }
        }
    }

    val filteredForwardingServices = remember(forwardingServices, allForwardingServiceContacts, searchQuery) {
        if (searchQuery.isBlank()) {
            forwardingServices
        } else {
            forwardingServices.filter { service ->
                service.name.contains(searchQuery, ignoreCase = true) ||
                        service.website.contains(searchQuery, ignoreCase = true) ||
                        allForwardingServiceContacts.any { it.forwardingServiceId == service.id && it.contactValue.contains(searchQuery, ignoreCase = true) }
            }
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        if (isSearching) {
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = {
                                    Text(
                                        if (selectedTab == 0) "Search bookstores..." else "Search forwarding services...",
                                        fontSize = 16.sp
                                    )
                                },
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
                                    .testTag(if (selectedTab == 0) "bookstores_search_input" else "forwarding_services_search_input"),
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear Search")
                                        }
                                    }
                                }
                            )
                        } else {
                            Text("Stores", fontWeight = FontWeight.Bold, fontSize = 20.sp)
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
                                Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                            }
                            IconButton(
                                onClick = {
                                    if (selectedTab == 0) {
                                        showAddBookstoreDialog = true
                                    } else {
                                        showAddForwardingServiceDialog = true
                                    }
                                },
                                modifier = Modifier.testTag(if (selectedTab == 0) "add_bookstore" else "add_forwarding_service")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = if (selectedTab == 0) "Add Bookstore" else "Add Forwarding Service"
                                )
                            }
                        }
                    }
                )

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = {
                            selectedTab = 0
                            searchQuery = ""
                        },
                        text = { Text("Bookstores", fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.Storefront, contentDescription = "Bookstores Tab") },
                        modifier = Modifier.testTag("tab_bookstores")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                            searchQuery = ""
                        },
                        text = { Text("Forwarding Services", fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.LocalShipping, contentDescription = "Forwarding Services Tab") },
                        modifier = Modifier.testTag("tab_forwarding_services")
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        if (selectedTab == 0) {
            // BOOKSTORES TAB
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
                        TextButton(onClick = { showAddBookstoreDialog = true }) {
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
                            items(stores, key = { it.id }) { bookstore ->
                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = { dismissValue ->
                                        when (dismissValue) {
                                            SwipeToDismissBoxValue.StartToEnd -> {
                                                onDeleteBookstore(bookstore)
                                                true
                                            }
                                            SwipeToDismissBoxValue.EndToStart -> {
                                                selectedBookstore = bookstore
                                                showEditBookstoreDialog = true
                                                false
                                            }
                                            else -> false
                                        }
                                    }
                                )

                                SwipeToDismissBox(
                                    state = dismissState,
                                    backgroundContent = {
                                        val color = when (dismissState.dismissDirection) {
                                            SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.errorContainer
                                            SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.secondaryContainer
                                            else -> Color.Transparent
                                        }
                                        val alignment = when (dismissState.dismissDirection) {
                                            SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                                            SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                                            else -> Alignment.Center
                                        }
                                        val icon = when (dismissState.dismissDirection) {
                                            SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Delete
                                            SwipeToDismissBoxValue.EndToStart -> Icons.Default.Edit
                                            else -> null
                                        }
                                        val iconTint = when (dismissState.dismissDirection) {
                                            SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.onErrorContainer
                                            SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.onSecondaryContainer
                                            else -> Color.Transparent
                                        }

                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(vertical = 4.dp)
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(color)
                                                .padding(horizontal = 20.dp),
                                            contentAlignment = alignment
                                        ) {
                                            icon?.let {
                                                Icon(
                                                    imageVector = it,
                                                    contentDescription = null,
                                                    tint = iconTint
                                                )
                                            }
                                        }
                                    },
                                    enableDismissFromStartToEnd = true,
                                    enableDismissFromEndToStart = true
                                ) {
                                    BookstoreListItem(
                                        bookstore = bookstore,
                                        onClick = {
                                            selectedBookstore = bookstore
                                            showDetailDialog = true
                                        },
                                        onImageUpdated = { newPath ->
                                            viewModel.updateBookstore(bookstore.copy(profilePic = newPath))
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // FORWARDING SERVICES TAB
            if (forwardingServices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.LocalShipping,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No forwarding services registered yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        TextButton(onClick = { showAddForwardingServiceDialog = true }) {
                            Text("Add Your First Forwarding Service")
                        }
                    }
                }
            } else {
                val groupedForwardingServices = remember(filteredForwardingServices) {
                    filteredForwardingServices
                        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
                        .groupBy { it.name.firstOrNull()?.uppercaseChar() ?: '#' }
                }

                if (filteredForwardingServices.isEmpty()) {
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
                            Text("No matching forwarding services found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        groupedForwardingServices.forEach { (initial, services) ->
                            item {
                                Text(
                                    text = initial.toString(),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 4.dp)
                                )
                            }
                            items(services, key = { it.id }) { service ->
                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = { dismissValue ->
                                        when (dismissValue) {
                                            SwipeToDismissBoxValue.StartToEnd -> {
                                                onDeleteForwardingService(service)
                                                true
                                            }
                                            SwipeToDismissBoxValue.EndToStart -> {
                                                selectedForwardingService = service
                                                showEditForwardingServiceDialog = true
                                                false
                                            }
                                            else -> false
                                        }
                                    }
                                )

                                SwipeToDismissBox(
                                    state = dismissState,
                                    backgroundContent = {
                                        val color = when (dismissState.dismissDirection) {
                                            SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.errorContainer
                                            SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.secondaryContainer
                                            else -> Color.Transparent
                                        }
                                        val alignment = when (dismissState.dismissDirection) {
                                            SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                                            SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                                            else -> Alignment.Center
                                        }
                                        val icon = when (dismissState.dismissDirection) {
                                            SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Delete
                                            SwipeToDismissBoxValue.EndToStart -> Icons.Default.Edit
                                            else -> null
                                        }
                                        val iconTint = when (dismissState.dismissDirection) {
                                            SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.onErrorContainer
                                            SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.onSecondaryContainer
                                            else -> Color.Transparent
                                        }

                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(vertical = 4.dp)
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(color)
                                                .padding(horizontal = 20.dp),
                                            contentAlignment = alignment
                                        ) {
                                            icon?.let {
                                                Icon(
                                                    imageVector = it,
                                                    contentDescription = null,
                                                    tint = iconTint
                                                )
                                            }
                                        }
                                    },
                                    enableDismissFromStartToEnd = true,
                                    enableDismissFromEndToStart = true
                                ) {
                                    ForwardingServiceListItem(
                                        service = service,
                                        onClick = {
                                            selectedForwardingService = service
                                            showEditForwardingServiceDialog = true
                                        },
                                        onImageUpdated = { newPath ->
                                            viewModel.updateForwardingService(service.copy(profilePic = newPath))
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add Bookstore Dialog
        if (showAddBookstoreDialog) {
            var name by remember { mutableStateOf("") }
            var imageUrl by remember { mutableStateOf("") }
            var contacts by remember {
                mutableStateOf(
                    listOf(
                        ContactDraft(type = "Website", value = ""),
                        ContactDraft(type = "Email", value = ""),
                        ContactDraft(type = "Phone", value = "")
                    )
                )
            }

            AlertDialog(
                onDismissRequest = {
                    showAddBookstoreDialog = false
                },
                title = { Text("Add Bookstore", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Round Bookstore Image Picker with Pencil Button
                        BookstoreImagePicker(
                            imageUrl = imageUrl,
                            onImageSelected = { imageUrl = it }
                        )

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Bookstore Name", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            modifier = Modifier.fillMaxWidth().testTag("add_bookstore_name")
                        )

                        // Google Contacts style contact fields
                        BookstoreContactsSection(
                            contacts = contacts,
                            onUpdateValue = { id, newVal ->
                                contacts = contacts.map { if (it.id == id) it.copy(value = newVal) else it }
                            },
                            onUpdateType = { id, newType ->
                                contacts = contacts.map { if (it.id == id) it.copy(type = newType) else it }
                            },
                            onRemoveContact = { id ->
                                contacts = contacts.filter { it.id != id }
                            },
                            onAddContact = { type ->
                                contacts = contacts + ContactDraft(type = type, value = "")
                            }
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                val validContacts = contacts
                                    .filter { it.value.isNotBlank() }
                                    .map { BookstoreContact(bookstoreId = 0, contactType = it.type.ifBlank { "Other" }, contactValue = it.value.trim()) }
                                val primaryWebsite = validContacts
                                    .firstOrNull { it.contactType.equals("Website", ignoreCase = true) }
                                    ?.contactValue ?: ""
                                viewModel.addBookstore(
                                    name = name.trim(),
                                    website = primaryWebsite,
                                    profilePic = imageUrl.ifEmpty { null },
                                    contacts = validContacts
                                )
                                showAddBookstoreDialog = false
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
                            showAddBookstoreDialog = false
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
            val storeContacts = allBookstoreContacts.filter { it.bookstoreId == store.id }

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
                            if (!store.profilePic.isNullOrEmpty() && store.profilePic != "ic_launcher_foreground") {
                                AsyncImage(
                                    model = store.profilePic,
                                    contentDescription = "${store.name} Logo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                val initial = (store.name.firstOrNull { it.isLetterOrDigit() } ?: 'B').uppercaseChar().toString()
                                Text(
                                    text = initial,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Text(store.name, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (storeContacts.isNotEmpty()) {
                            storeContacts.forEach { contact ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            when (contact.contactType.lowercase()) {
                                                "website" -> {
                                                    try {
                                                        val url = if (contact.contactValue.startsWith("http://") || contact.contactValue.startsWith("https://")) {
                                                            contact.contactValue
                                                        } else {
                                                            "https://${contact.contactValue}"
                                                        }
                                                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                        context.startActivity(browserIntent)
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }
                                                }
                                                "email" -> {
                                                    try {
                                                        val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${contact.contactValue}"))
                                                        context.startActivity(emailIntent)
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }
                                                }
                                                "phone" -> {
                                                    try {
                                                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.contactValue}"))
                                                        context.startActivity(dialIntent)
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }
                                                }
                                            }
                                        }
                                        .padding(vertical = 4.dp, horizontal = 2.dp)
                                ) {
                                    val icon = when (contact.contactType.lowercase()) {
                                        "website" -> Icons.Default.Language
                                        "email" -> Icons.Default.Email
                                        "phone" -> Icons.Default.Phone
                                        else -> Icons.Default.ContactMail
                                    }
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = contact.contactType,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = contact.contactType,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = contact.contactValue,
                                            fontSize = 14.sp,
                                            color = if (contact.contactType.equals("website", ignoreCase = true)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    if (contact.contactType.equals("website", ignoreCase = true)) {
                                        Icon(
                                            imageVector = Icons.Default.OpenInNew,
                                            contentDescription = "Open",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        } else if (store.website.isNotEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        try {
                                            val url = if (store.website.startsWith("http://") || store.website.startsWith("https://")) {
                                                store.website
                                            } else {
                                                "https://${store.website}"
                                            }
                                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                            context.startActivity(browserIntent)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = "Website",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Website",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = store.website,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = "Open",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else {
                            Text(
                                text = "No contact details saved",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDetailDialog = false
                            showEditBookstoreDialog = true
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
        if (showEditBookstoreDialog && selectedBookstore != null) {
            val store = selectedBookstore!!
            val storeContacts = allBookstoreContacts.filter { it.bookstoreId == store.id }
            var editName by remember(store.id) { mutableStateOf(store.name) }
            var editImageUrl by remember(store.id) { mutableStateOf(store.profilePic ?: "") }
            var editContacts by remember(store.id, storeContacts) {
                val initialList = mutableListOf<ContactDraft>()
                if (storeContacts.isNotEmpty()) {
                    storeContacts.forEach { initialList.add(ContactDraft(type = it.contactType, value = it.contactValue)) }
                } else if (store.website.isNotEmpty()) {
                    initialList.add(ContactDraft(type = "Website", value = store.website))
                }
                // Ensure default empty Website, Email, Phone rows if not already present
                if (!initialList.any { it.type.equals("Website", ignoreCase = true) }) {
                    initialList.add(0, ContactDraft(type = "Website", value = ""))
                }
                if (!initialList.any { it.type.equals("Email", ignoreCase = true) }) {
                    initialList.add(ContactDraft(type = "Email", value = ""))
                }
                if (!initialList.any { it.type.equals("Phone", ignoreCase = true) }) {
                    initialList.add(ContactDraft(type = "Phone", value = ""))
                }
                mutableStateOf(initialList.toList())
            }

            AlertDialog(
                onDismissRequest = {
                    showEditBookstoreDialog = false
                },
                title = { Text("Edit Bookstore", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Round Bookstore Image Picker with Pencil Button
                        BookstoreImagePicker(
                            imageUrl = editImageUrl,
                            onImageSelected = { editImageUrl = it }
                        )

                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text("Bookstore Name", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            modifier = Modifier.fillMaxWidth().testTag("edit_bookstore_name")
                        )

                        // Google Contacts style contact fields
                        BookstoreContactsSection(
                            contacts = editContacts,
                            onUpdateValue = { id, newVal ->
                                editContacts = editContacts.map { if (it.id == id) it.copy(value = newVal) else it }
                            },
                            onUpdateType = { id, newType ->
                                editContacts = editContacts.map { if (it.id == id) it.copy(type = newType) else it }
                            },
                            onRemoveContact = { id ->
                                editContacts = editContacts.filter { it.id != id }
                            },
                            onAddContact = { type ->
                                editContacts = editContacts + ContactDraft(type = type, value = "")
                            }
                        )
                    }
                },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                onDeleteBookstore(store)
                                showEditBookstoreDialog = false
                                selectedBookstore = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.testTag("delete_bookstore_btn")
                        ) {
                            Text("Delete")
                        }

                        Button(
                            onClick = {
                                if (editName.isNotBlank()) {
                                    val validContacts = editContacts
                                        .filter { it.value.isNotBlank() }
                                        .map { BookstoreContact(bookstoreId = store.id, contactType = it.type.ifBlank { "Other" }, contactValue = it.value.trim()) }
                                    val primaryWebsite = validContacts
                                        .firstOrNull { it.contactType.equals("Website", ignoreCase = true) }
                                        ?.contactValue ?: store.website
                                    viewModel.updateBookstore(
                                        store.copy(
                                            name = editName.trim(),
                                            website = primaryWebsite,
                                            profilePic = editImageUrl.ifEmpty { null }
                                        ),
                                        contacts = validContacts
                                    )
                                    showEditBookstoreDialog = false
                                    selectedBookstore = null
                                }
                            },
                            modifier = Modifier.testTag("save_bookstore_btn")
                        ) {
                            Text("Save")
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showEditBookstoreDialog = false
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Add Forwarding Service Dialog
        if (showAddForwardingServiceDialog) {
            var name by remember { mutableStateOf("") }
            var imageUrl by remember { mutableStateOf("") }
            var contacts by remember {
                mutableStateOf(
                    listOf(
                        ContactDraft(type = "Website", value = ""),
                        ContactDraft(type = "Email", value = ""),
                        ContactDraft(type = "Phone", value = "")
                    )
                )
            }

            AlertDialog(
                onDismissRequest = {
                    showAddForwardingServiceDialog = false
                },
                title = { Text("Add Forwarding Service", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ForwardingServiceImagePicker(
                            imageUrl = imageUrl,
                            onImageSelected = { imageUrl = it }
                        )

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Forwarding Service Name", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            modifier = Modifier.fillMaxWidth().testTag("add_forwarding_service_name")
                        )

                        BookstoreContactsSection(
                            contacts = contacts,
                            onUpdateValue = { id, newVal ->
                                contacts = contacts.map { if (it.id == id) it.copy(value = newVal) else it }
                            },
                            onUpdateType = { id, newType ->
                                contacts = contacts.map { if (it.id == id) it.copy(type = newType) else it }
                            },
                            onRemoveContact = { id ->
                                contacts = contacts.filter { it.id != id }
                            },
                            onAddContact = { type ->
                                contacts = contacts + ContactDraft(type = type, value = "")
                            }
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                val validContacts = contacts
                                    .filter { it.value.isNotBlank() }
                                    .map { ForwardingServiceContact(forwardingServiceId = 0, contactType = it.type.ifBlank { "Other" }, contactValue = it.value.trim()) }
                                val primaryWebsite = validContacts
                                    .firstOrNull { it.contactType.equals("Website", ignoreCase = true) }
                                    ?.contactValue ?: ""
                                viewModel.addForwardingService(
                                    name = name.trim(),
                                    website = primaryWebsite,
                                    profilePic = imageUrl.ifEmpty { null },
                                    contacts = validContacts
                                )
                                showAddForwardingServiceDialog = false
                            }
                        },
                        modifier = Modifier.testTag("confirm_add_forwarding_service")
                    ) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showAddForwardingServiceDialog = false
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Edit Forwarding Service Dialog
        if (showEditForwardingServiceDialog && selectedForwardingService != null) {
            val service = selectedForwardingService!!
            val serviceContacts = allForwardingServiceContacts.filter { it.forwardingServiceId == service.id }
            var editName by remember(service.id) { mutableStateOf(service.name) }
            var editImageUrl by remember(service.id) { mutableStateOf(service.profilePic ?: "") }
            var editContacts by remember(service.id, serviceContacts) {
                val initialList = mutableListOf<ContactDraft>()
                if (serviceContacts.isNotEmpty()) {
                    serviceContacts.forEach { initialList.add(ContactDraft(type = it.contactType, value = it.contactValue)) }
                } else if (service.website.isNotEmpty()) {
                    initialList.add(ContactDraft(type = "Website", value = service.website))
                }
                if (!initialList.any { it.type.equals("Website", ignoreCase = true) }) {
                    initialList.add(0, ContactDraft(type = "Website", value = ""))
                }
                if (!initialList.any { it.type.equals("Email", ignoreCase = true) }) {
                    initialList.add(ContactDraft(type = "Email", value = ""))
                }
                if (!initialList.any { it.type.equals("Phone", ignoreCase = true) }) {
                    initialList.add(ContactDraft(type = "Phone", value = ""))
                }
                mutableStateOf(initialList.toList())
            }

            AlertDialog(
                onDismissRequest = {
                    showEditForwardingServiceDialog = false
                },
                title = { Text("Edit Forwarding Service", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ForwardingServiceImagePicker(
                            imageUrl = editImageUrl,
                            onImageSelected = { editImageUrl = it }
                        )

                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text("Forwarding Service Name", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            modifier = Modifier.fillMaxWidth().testTag("edit_forwarding_service_name")
                        )

                        BookstoreContactsSection(
                            contacts = editContacts,
                            onUpdateValue = { id, newVal ->
                                editContacts = editContacts.map { if (it.id == id) it.copy(value = newVal) else it }
                            },
                            onUpdateType = { id, newType ->
                                editContacts = editContacts.map { if (it.id == id) it.copy(type = newType) else it }
                            },
                            onRemoveContact = { id ->
                                editContacts = editContacts.filter { it.id != id }
                            },
                            onAddContact = { type ->
                                editContacts = editContacts + ContactDraft(type = type, value = "")
                            }
                        )
                    }
                },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                onDeleteForwardingService(service)
                                showEditForwardingServiceDialog = false
                                selectedForwardingService = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.testTag("delete_forwarding_service_btn")
                        ) {
                            Text("Delete")
                        }

                        Button(
                            onClick = {
                                if (editName.isNotBlank()) {
                                    val validContacts = editContacts
                                        .filter { it.value.isNotBlank() }
                                        .map { ForwardingServiceContact(forwardingServiceId = service.id, contactType = it.type.ifBlank { "Other" }, contactValue = it.value.trim()) }
                                    val primaryWebsite = validContacts
                                        .firstOrNull { it.contactType.equals("Website", ignoreCase = true) }
                                        ?.contactValue ?: service.website
                                    viewModel.updateForwardingService(
                                        service.copy(
                                            name = editName.trim(),
                                            website = primaryWebsite,
                                            profilePic = editImageUrl.ifEmpty { null }
                                        ),
                                        contacts = validContacts
                                    )
                                    showEditForwardingServiceDialog = false
                                    selectedForwardingService = null
                                }
                            },
                            modifier = Modifier.testTag("save_forwarding_service_btn")
                        ) {
                            Text("Save")
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showEditForwardingServiceDialog = false
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

data class ContactDraft(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: String,
    val value: String = ""
)

@Composable
fun BookstoreContactsSection(
    contacts: List<ContactDraft>,
    onUpdateValue: (id: String, value: String) -> Unit,
    onUpdateType: (id: String, type: String) -> Unit,
    onRemoveContact: (id: String) -> Unit,
    onAddContact: (type: String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Website Fields
        val websiteContacts = contacts.filter { it.type.equals("Website", ignoreCase = true) }
        websiteContacts.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = item.value,
                    onValueChange = { onUpdateValue(item.id, it) },
                    label = { Text("Website", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    placeholder = { Text("https://...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Website",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("contact_field_website_${item.id}")
                )
                IconButton(
                    onClick = { onRemoveContact(item.id) },
                    modifier = Modifier.testTag("remove_website_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove website",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        TextButton(
            onClick = { onAddContact("Website") },
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
            modifier = Modifier.testTag("add_website_btn")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add website")
        }

        // 2. Email Fields
        val emailContacts = contacts.filter { it.type.equals("Email", ignoreCase = true) }
        emailContacts.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = item.value,
                    onValueChange = { onUpdateValue(item.id, it) },
                    label = { Text("Email", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    placeholder = { Text("contact@example.com") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("contact_field_email_${item.id}")
                )
                IconButton(
                    onClick = { onRemoveContact(item.id) },
                    modifier = Modifier.testTag("remove_email_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove email",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        TextButton(
            onClick = { onAddContact("Email") },
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
            modifier = Modifier.testTag("add_email_btn")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add email")
        }

        // 3. Phone Fields
        val phoneContacts = contacts.filter { it.type.equals("Phone", ignoreCase = true) }
        phoneContacts.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = item.value,
                    onValueChange = { onUpdateValue(item.id, it) },
                    label = { Text("Phone", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    placeholder = { Text("+1 (555) 000-0000") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Phone",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("contact_field_phone_${item.id}")
                )
                IconButton(
                    onClick = { onRemoveContact(item.id) },
                    modifier = Modifier.testTag("remove_phone_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove phone",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        TextButton(
            onClick = { onAddContact("Phone") },
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
            modifier = Modifier.testTag("add_phone_btn")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add phone")
        }

        // 4. Other / Custom Fields
        val otherContacts = contacts.filter {
            !it.type.equals("Website", ignoreCase = true) &&
            !it.type.equals("Email", ignoreCase = true) &&
            !it.type.equals("Phone", ignoreCase = true)
        }
        otherContacts.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = item.type,
                        onValueChange = { onUpdateType(item.id, it) },
                        label = { Text("Custom label", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        placeholder = { Text("e.g. Instagram, Discord, Notes...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Label,
                                contentDescription = "Label",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("custom_label_${item.id}")
                    )
                    OutlinedTextField(
                        value = item.value,
                        onValueChange = { onUpdateValue(item.id, it) },
                        label = { Text("Value", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        placeholder = { Text("Contact info") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.ContactMail,
                                contentDescription = "Value",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("custom_value_${item.id}")
                    )
                }
                IconButton(
                    onClick = { onRemoveContact(item.id) },
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .testTag("remove_other_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove custom contact",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 5. Add custom contact Button underneath everything
        TextButton(
            onClick = { onAddContact("") },
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
            modifier = Modifier.testTag("add_custom_contact_btn")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add custom contact")
        }
    }
}

@Composable
fun BookstoreListItem(
    bookstore: Bookstore,
    onClick: () -> Unit,
    onImageUpdated: ((String?) -> Unit)? = null
) {
    val context = LocalContext.current
    var showViewerDialog by remember { mutableStateOf(false) }

    if (showViewerDialog && !bookstore.profilePic.isNullOrEmpty() && bookstore.profilePic != "ic_launcher_foreground") {
        ImageViewerDialog(
            imageUrl = bookstore.profilePic!!,
            onDismiss = { showViewerDialog = false }
        )
    }

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
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .clickable {
                        if (!bookstore.profilePic.isNullOrEmpty() && bookstore.profilePic != "ic_launcher_foreground") {
                            showViewerDialog = true
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (!bookstore.profilePic.isNullOrEmpty() && bookstore.profilePic != "ic_launcher_foreground") {
                    AsyncImage(
                        model = bookstore.profilePic,
                        contentDescription = "${bookstore.name} Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    val initial = (bookstore.name.firstOrNull { it.isLetterOrDigit() } ?: 'B').uppercaseChar().toString()
                    Text(
                        text = initial,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
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
                    text = bookstore.website.ifEmpty { "No website link saved" },
                    fontSize = 12.sp,
                    color = if (bookstore.website.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Trailing Chevron / Action button
            IconButton(
                onClick = {
                    if (bookstore.website.isNotEmpty()) {
                        try {
                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(bookstore.website))
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
                    imageVector = if (bookstore.website.isNotEmpty()) Icons.Default.OpenInNew else Icons.Default.ChevronRight,
                    contentDescription = if (bookstore.website.isNotEmpty()) "Open Website" else "View Details",
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
    InteractiveImagePicker(
        imageUrl = imageUrl,
        onImageSelected = onImageSelected,
        modifier = modifier,
        defaultIcon = Icons.Default.Storefront,
        contentDescription = "Bookstore Image"
    )
}

@Composable
fun ForwardingServiceImagePicker(
    imageUrl: String,
    onImageSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    InteractiveImagePicker(
        imageUrl = imageUrl,
        onImageSelected = onImageSelected,
        modifier = modifier,
        defaultIcon = Icons.Default.LocalShipping,
        contentDescription = "Forwarding Service Image"
    )
}

@Composable
fun ForwardingServiceListItem(
    service: ForwardingService,
    onClick: () -> Unit,
    onImageUpdated: ((String?) -> Unit)? = null
) {
    val context = LocalContext.current
    var showViewerDialog by remember { mutableStateOf(false) }

    if (showViewerDialog && !service.profilePic.isNullOrEmpty() && service.profilePic != "ic_launcher_foreground") {
        ImageViewerDialog(
            imageUrl = service.profilePic!!,
            onDismiss = { showViewerDialog = false }
        )
    }

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
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .clickable {
                        if (!service.profilePic.isNullOrEmpty() && service.profilePic != "ic_launcher_foreground") {
                            showViewerDialog = true
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (!service.profilePic.isNullOrEmpty() && service.profilePic != "ic_launcher_foreground") {
                    AsyncImage(
                        model = service.profilePic,
                        contentDescription = "${service.name} Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    val initial = (service.name.firstOrNull { it.isLetterOrDigit() } ?: 'F').uppercaseChar().toString()
                    Text(
                        text = initial,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Service Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = service.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = service.website.ifEmpty { "No website link saved" },
                    fontSize = 12.sp,
                    color = if (service.website.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Trailing Chevron / Action button
            IconButton(
                onClick = {
                    if (service.website.isNotEmpty()) {
                        try {
                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(service.website))
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
                    imageVector = if (service.website.isNotEmpty()) Icons.Default.OpenInNew else Icons.Default.ChevronRight,
                    contentDescription = if (service.website.isNotEmpty()) "Open Website" else "View Details",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

