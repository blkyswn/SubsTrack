package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class BookishViewModel(application: Application) : AndroidViewModel(application) {
    private val database = BookishDatabase.getDatabase(application)
    val repository = BookishRepository(database)

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // UI Search and Filter States as MutableStateFlows
    val subOverviewSearch = MutableStateFlow("")
    val subOverviewFilterType = MutableStateFlow<String?>(null) // Scheduled Subscription Status (Upcoming, Paid, Shipped, Received)
    val subOverviewFilterBookstore = MutableStateFlow<Int?>(null)
    val subOverviewFilterAuthor = MutableStateFlow<String?>(null)
    val subOverviewFilterBook = MutableStateFlow<String?>(null)
    val subOverviewIsCalendarView = MutableStateFlow(false)

    val subSearch = MutableStateFlow("")
    val subFilterStatus = MutableStateFlow<String?>(null) // Active, Waitlist, Paused, Canceled
    val subFilterBookstore = MutableStateFlow<Int?>(null)
    val subFilterFrequency = MutableStateFlow<String?>(null)
    val subIsCalendarView = MutableStateFlow(false)

    val preorderSearch = MutableStateFlow("")
    val preorderFilterBookstore = MutableStateFlow<Int?>(null)
    val preorderFilterAuthor = MutableStateFlow<String?>(null)
    val preorderFilterBook = MutableStateFlow<String?>(null)
    val preorderFilterStatus = MutableStateFlow<String?>(null) // Upcoming, Released, Preordered, Shipped, Received
    val preorderIsCalendarView = MutableStateFlow(false)

    // Base Database flows
    val userState: StateFlow<User?> = repository.userFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val bookstoresState: StateFlow<List<Bookstore>> = repository.allBookstoresFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rawSubscriptionsState: StateFlow<List<SubscriptionWithBookstore>> = repository.subscriptionsWithBookstoreFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rawScheduledState: StateFlow<List<ScheduledWithDetails>> = repository.scheduledWithDetailsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rawPreordersState: StateFlow<List<PreorderWithBookstore>> = repository.preordersWithBookstoreFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Run prepopulate check
        viewModelScope.launch {
            repository.checkAndPrepopulate()
        }
    }

    // Helper classes for robust type-safe flow combination exceeding 5 elements
    data class ScheduledFilters(
        val query: String,
        val status: String?,
        val storeId: Int?,
        val author: String?,
        val book: String?
    )

    data class PreorderFilters(
        val query: String,
        val storeId: Int?,
        val author: String?,
        val book: String?,
        val status: String?
    )

    private val scheduledFiltersFlow: Flow<ScheduledFilters> = combine(
        subOverviewSearch,
        subOverviewFilterType,
        subOverviewFilterBookstore,
        subOverviewFilterAuthor,
        subOverviewFilterBook
    ) { query, status, storeId, author, book ->
        ScheduledFilters(query, status, storeId, author, book)
    }

    private val preorderFiltersFlow: Flow<PreorderFilters> = combine(
        preorderSearch,
        preorderFilterBookstore,
        preorderFilterAuthor,
        preorderFilterBook,
        preorderFilterStatus
    ) { query, storeId, author, book, status ->
        PreorderFilters(query, storeId, author, book, status)
    }

    // 1. FILTERED Scheduled Subscriptions (Subscriptions Overview)
    val filteredScheduledState: StateFlow<List<ScheduledWithDetails>> = combine(
        repository.scheduledWithDetailsFlow,
        scheduledFiltersFlow
    ) { list, filters ->
        list.filter { item ->
            val matchesQuery = filters.query.isBlank() ||
                    item.scheduled.bookTitle.contains(filters.query, ignoreCase = true) ||
                    item.scheduled.bookAuthor.contains(filters.query, ignoreCase = true) ||
                    (item.subscriptionType?.title?.contains(filters.query, ignoreCase = true) ?: false)
            val matchesStatus = filters.status == null || item.scheduled.status == filters.status
            val matchesBookstore = filters.storeId == null || item.bookstore?.id == filters.storeId
            val matchesAuthor = filters.author == null || item.scheduled.bookAuthor.contains(filters.author, ignoreCase = true)
            val matchesBook = filters.book == null || item.scheduled.bookTitle.contains(filters.book, ignoreCase = true)

            matchesQuery && matchesStatus && matchesBookstore && matchesAuthor && matchesBook
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 2. FILTERED Subscription Types (Subscriptions main tab)
    val filteredSubscriptionsState: StateFlow<List<SubscriptionWithBookstore>> = combine(
        repository.subscriptionsWithBookstoreFlow,
        subSearch,
        subFilterStatus,
        subFilterBookstore,
        subFilterFrequency
    ) { list, query, status, storeId, frequency ->
        list.filter { item ->
            val matchesQuery = query.isBlank() ||
                    item.subscription.title.contains(query, ignoreCase = true) ||
                    (item.bookstore?.name?.contains(query, ignoreCase = true) ?: false)
            val matchesStatus = status == null || item.subscription.status == status
            val matchesBookstore = storeId == null || item.bookstore?.id == storeId
            val matchesFrequency = frequency == null || item.subscription.frequency == frequency

            matchesQuery && matchesStatus && matchesBookstore && matchesFrequency
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 3. FILTERED Preorders
    val filteredPreordersState: StateFlow<List<PreorderWithBookstore>> = combine(
        repository.preordersWithBookstoreFlow,
        preorderFiltersFlow
    ) { list, filters ->
        list.filter { item ->
            val matchesQuery = filters.query.isBlank() ||
                    item.preorder.bookTitle.contains(filters.query, ignoreCase = true) ||
                    item.preorder.bookAuthor.contains(filters.query, ignoreCase = true) ||
                    (item.bookstore?.name?.contains(filters.query, ignoreCase = true) ?: false)
            val matchesBookstore = filters.storeId == null || item.bookstore?.id == filters.storeId
            val matchesAuthor = filters.author == null || item.preorder.bookAuthor.contains(filters.author, ignoreCase = true)
            val matchesBook = filters.book == null || item.preorder.bookTitle.contains(filters.book, ignoreCase = true)
            val matchesStatus = filters.status == null || item.preorder.status == filters.status

            matchesQuery && matchesBookstore && matchesAuthor && matchesBook && matchesStatus
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Database CRUD Actions ---
    fun updateProfile(username: String, currency: String, language: String, profilePic: String?) {
        viewModelScope.launch {
            repository.saveUser(User(id = 1, username = username, currency = currency, language = language, profilePic = profilePic))
        }
    }

    fun addBookstore(name: String, url: String, profilePic: String?) {
        viewModelScope.launch {
            repository.insertBookstore(Bookstore(name = name, url = url, profilePic = profilePic))
        }
    }

    fun updateBookstore(bookstore: Bookstore) {
        viewModelScope.launch {
            repository.updateBookstore(bookstore)
        }
    }

    fun deleteBookstore(bookstore: Bookstore) {
        viewModelScope.launch {
            repository.deleteBookstore(bookstore)
        }
    }

    fun addSubscriptionType(
        bookstoreId: Int,
        title: String,
        status: String,
        price: Double,
        dueDate: Long,
        startDate: Long,
        finishDate: Long,
        notificationAlertDays: Int?,
        frequency: String
    ) {
        viewModelScope.launch {
            repository.insertSubscriptionType(
                SubscriptionType(
                    bookstoreId = bookstoreId,
                    title = title,
                    status = status,
                    price = price,
                    dueDate = dueDate,
                    startDate = startDate,
                    finishDate = finishDate,
                    notificationAlertDays = notificationAlertDays,
                    frequency = frequency
                )
            )
        }
    }

    fun updateSubscriptionType(subscription: SubscriptionType) {
        viewModelScope.launch {
            repository.updateSubscriptionType(subscription)
        }
    }

    fun deleteSubscriptionType(subscription: SubscriptionType) {
        viewModelScope.launch {
            repository.deleteSubscriptionType(subscription)
        }
    }

    fun addScheduledSubscription(
        subscriptionTypeId: Int,
        bookTitle: String,
        bookAuthor: String,
        description: String,
        dueDate: Long,
        status: String
    ) {
        viewModelScope.launch {
            repository.insertScheduledSubscription(
                ScheduledSubscription(
                    subscriptionTypeId = subscriptionTypeId,
                    bookTitle = bookTitle,
                    bookAuthor = bookAuthor,
                    description = description,
                    dueDate = dueDate,
                    status = status,
                    isSkipped = false
                )
            )
        }
    }

    fun updateScheduledSubscription(scheduled: ScheduledSubscription) {
        viewModelScope.launch {
            repository.updateScheduledSubscription(scheduled)
        }
    }

    fun deleteScheduledSubscription(scheduled: ScheduledSubscription) {
        viewModelScope.launch {
            repository.deleteScheduledSubscription(scheduled)
        }
    }

    fun toggleSkipScheduledSubscription(scheduled: ScheduledSubscription) {
        viewModelScope.launch {
            repository.updateScheduledSubscription(
                scheduled.copy(isSkipped = !scheduled.isSkipped)
            )
        }
    }

    fun addPreorder(
        bookstoreId: Int,
        bookTitle: String,
        bookAuthor: String,
        description: String,
        price: Double,
        saleDateStart: Long,
        saleDateEnd: Long,
        status: String,
        picturePath: String? = null
    ) {
        viewModelScope.launch {
            repository.insertPreorder(
                Preorder(
                    bookstoreId = bookstoreId,
                    picturePath = picturePath,
                    bookTitle = bookTitle,
                    bookAuthor = bookAuthor,
                    description = description,
                    price = price,
                    rangedSaleDateStart = saleDateStart,
                    rangedSaleDateEnd = saleDateEnd,
                    status = status
                )
            )
        }
    }

    fun updatePreorder(preorder: Preorder) {
        viewModelScope.launch {
            repository.updatePreorder(preorder)
        }
    }

    fun deletePreorder(preorder: Preorder) {
        viewModelScope.launch {
            repository.deletePreorder(preorder)
        }
    }

    // --- CSV EXCEL EXPORTING ---
    fun exportToExcelSpreadsheet(context: Context) {
        viewModelScope.launch {
            val bookstores = bookstoresState.value
            val subscriptions = rawSubscriptionsState.value
            val scheduled = rawScheduledState.value
            val preorders = rawPreordersState.value
            val user = userState.value ?: User(id = 1, username = "Guest", profilePic = null, currency = "$", language = "English")

            // Build cohesive unified CSV report contents with separate sections or individual spreadsheets
            val csvBuilder = java.lang.StringBuilder()

            csvBuilder.append("=== BOOKISH REPORT DATA EXPORT ===\n")
            csvBuilder.append("Export Date: ${dateFormat.format(Date())}\n")
            csvBuilder.append("User Name: ${user.username}\n")
            csvBuilder.append("Preferred Currency: ${user.currency}\n")
            csvBuilder.append("Preferred Language: ${user.language}\n\n")

            // Bookstores Section
            csvBuilder.append("--- BOOKSTORES ---\n")
            csvBuilder.append("Bookstore ID,Name,URL\n")
            for (b in bookstores) {
                csvBuilder.append("${b.id},\"${escapeCsv(b.name)}\",\"${escapeCsv(b.url)}\"\n")
            }
            csvBuilder.append("\n")

            // Subscriptions Section
            csvBuilder.append("--- SUBSCRIPTIONS ---\n")
            csvBuilder.append("Subscription ID,Bookstore,Title,Status,Price,Due Date,Start Date,Finish Date,Alert Days,Frequency\n")
            for (s in subscriptions) {
                val sub = s.subscription
                val bookstoreName = s.bookstore?.name ?: "Unknown"
                csvBuilder.append(
                    "${sub.id},\"${escapeCsv(bookstoreName)}\",\"${escapeCsv(sub.title)}\",\"${sub.status}\",${sub.price},${dateFormat.format(Date(sub.dueDate))},${dateFormat.format(Date(sub.startDate))},${dateFormat.format(Date(sub.finishDate))},${sub.notificationAlertDays ?: 0},${sub.frequency}\n"
                )
            }
            csvBuilder.append("\n")

            // Scheduled Subscriptions Section
            csvBuilder.append("--- SCHEDULED SUBSCRIPTIONS ---\n")
            csvBuilder.append("Scheduled ID,Subscription,Book Title,Author,Description,Due Date,Status,Is Skipped\n")
            for (sch in scheduled) {
                val sc = sch.scheduled
                val subTitle = sch.subscriptionType?.title ?: "Unknown"
                csvBuilder.append(
                    "${sc.id},\"${escapeCsv(subTitle)}\",\"${escapeCsv(sc.bookTitle)}\",\"${escapeCsv(sc.bookAuthor)}\",\"${escapeCsv(sc.description)}\",${dateFormat.format(Date(sc.dueDate))},\"${sc.status}\",${sc.isSkipped}\n"
                )
            }
            csvBuilder.append("\n")

            // Preorders Section
            csvBuilder.append("--- PREORDERS ---\n")
            csvBuilder.append("Preorder ID,Bookstore,Book Title,Author,Description,Price,Sale Start Date,Sale End Date,Status\n")
            for (p in preorders) {
                val pr = p.preorder
                val storeName = p.bookstore?.name ?: "Unknown"
                csvBuilder.append(
                    "${pr.id},\"${escapeCsv(storeName)}\",\"${escapeCsv(pr.bookTitle)}\",\"${escapeCsv(pr.bookAuthor)}\",\"${escapeCsv(pr.description)}\",${pr.price},${dateFormat.format(Date(pr.rangedSaleDateStart))},${dateFormat.format(Date(pr.rangedSaleDateEnd))},\"${pr.status}\"\n"
                )
            }

            // Share Excel-compatible CSV via Sharesheet
            shareCsvFile(context, "bookish_library_export.csv", csvBuilder.toString())
        }
    }

    private fun escapeCsv(value: String): String {
        return value.replace("\"", "\"\"")
    }

    private fun shareCsvFile(context: Context, fileName: String, content: String) {
        try {
            val file = File(context.cacheDir, fileName)
            file.writeText(content)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Bookish Spreadsheets Backup")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Share spreadsheet report via")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
