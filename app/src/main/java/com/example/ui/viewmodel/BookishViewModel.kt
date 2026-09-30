package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.utils.ExcelExporter
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class BookishViewModel(application: Application) : AndroidViewModel(application) {
    private val database = BookishDatabase.getDatabase(application)
    val repository = BookishRepository(database)

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // UI Search and Filter States as MutableStateFlows (Multi-select sets)
    val subOverviewSearch = MutableStateFlow("")
    val subOverviewFilterTypes = MutableStateFlow<Set<String>>(emptySet()) // Scheduled Subscription Statuses (Upcoming, Skipped, Renewed, Shipped, Received)
    val subOverviewFilterBookstores = MutableStateFlow<Set<Int>>(emptySet())
    val subOverviewFilterAuthor = MutableStateFlow<String?>(null)
    val subOverviewFilterBook = MutableStateFlow<String?>(null)
    val subOverviewFilterSubTypeIds = MutableStateFlow<Set<Int>>(emptySet())
    val subOverviewIsCalendarView = MutableStateFlow(false)

    val subSearch = MutableStateFlow("")
    val subFilterStatuses = MutableStateFlow<Set<String>>(emptySet()) // Active, Waitlist, Paused, Canceled, Wishlist
    val subFilterBookstores = MutableStateFlow<Set<Int>>(emptySet())
    val subFilterFrequencies = MutableStateFlow<Set<String>>(emptySet())
    val subFilterSubTypeIds = MutableStateFlow<Set<Int>>(emptySet())
    val subIsCalendarView = MutableStateFlow(false)
    val subSelectedTab = MutableStateFlow(0) // 0 = Overview (Renewals), 1 = Subscriptions

    val preorderSearch = MutableStateFlow("")
    val preorderFilterBookstores = MutableStateFlow<Set<Int>>(emptySet())
    val preorderFilterAuthor = MutableStateFlow<String?>(null)
    val preorderFilterBook = MutableStateFlow<String?>(null)
    val preorderFilterStatuses = MutableStateFlow<Set<String>>(emptySet()) // Upcoming, Released, Preordered, Shipped, Received
    val preorderIsCalendarView = MutableStateFlow(false)

    val homeScrollToTopTrigger = MutableStateFlow(0L)

    fun resetHomeScreenToTop() {
        homeScrollToTopTrigger.value = System.currentTimeMillis()
    }

    val alertMessage = MutableStateFlow<String?>(null)

    fun clearAlertMessage() {
        alertMessage.value = null
    }

    // Base Database flows
    val userState: StateFlow<User?> = repository.userFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val bookstoresState: StateFlow<List<Bookstore>> = repository.allBookstoresFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val bookstoreContactsState: StateFlow<List<BookstoreContact>> = repository.allBookstoreContactsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val forwardingServicesState: StateFlow<List<ForwardingService>> = repository.allForwardingServicesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val forwardingServiceContactsState: StateFlow<List<ForwardingServiceContact>> = repository.allForwardingServiceContactsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val shippingCompaniesState: StateFlow<List<ShippingCompany>> = repository.allShippingCompaniesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val shippingCompanyContactsState: StateFlow<List<ShippingCompanyContact>> = repository.allShippingCompanyContactsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val userAddressesState: StateFlow<List<UserAddress>> = repository.allUserAddressesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allPackagesState: StateFlow<List<PackageItem>> = repository.allPackagesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun insertUserAddress(address: UserAddress) {
        viewModelScope.launch {
            repository.insertUserAddress(address)
        }
    }

    fun updateUserAddress(address: UserAddress) {
        viewModelScope.launch {
            repository.updateUserAddress(address)
        }
    }

    fun deleteUserAddress(address: UserAddress) {
        viewModelScope.launch {
            repository.deleteUserAddress(address)
        }
    }

    val rawSubscriptionsState: StateFlow<List<SubscriptionWithBookstore>> = repository.subscriptionsWithBookstoreFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allSubscriptionSkipsState: StateFlow<List<SubscriptionSkip>> = repository.allSubscriptionSkipsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allSubscriptionSkipMethodsState: StateFlow<List<SubscriptionSkipMethod>> = repository.allSubscriptionSkipMethodsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun saveSubscriptionSkipMethods(subscriptionTypeId: Int, methods: List<SubscriptionSkipMethod>) {
        viewModelScope.launch {
            repository.saveSubscriptionSkipMethods(subscriptionTypeId, methods)
        }
    }

    private fun isSameDay(time1: Long, time2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = time1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = time2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    val rawScheduledState: StateFlow<List<ScheduledWithDetails>> = repository.scheduledWithDetailsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val rawPreordersState: StateFlow<List<PreorderWithBookstore>> = repository.preordersWithBookstoreFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        // Run prepopulate check and background reconciliation safely on IO dispatcher
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                repository.checkAndPrepopulate()
                checkAndGenerateActiveSubscriptions()
                reconcileStatuses()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun reconcileStatuses() {
        try {
            val now = System.currentTimeMillis()
            val scheduledList = repository.scheduledWithDetailsFlow.first()
            for (item in scheduledList) {
                if (item.scheduled.status.equals("Upcoming", ignoreCase = true) && isSameDay(now, item.scheduled.dueDate)) {
                    repository.updateScheduledSubscription(
                        item.scheduled.copy(status = "Renewed")
                    )
                }
            }
            val preordersList = repository.preordersWithBookstoreFlow.first()
            for (item in preordersList) {
                if (item.preorder.status.equals("Upcoming", ignoreCase = true)) {
                    if (now >= item.preorder.rangedSaleDateStart && now <= item.preorder.rangedSaleDateEnd) {
                        repository.updatePreorder(
                            item.preorder.copy(status = "Released")
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Helper classes for robust type-safe flow combination exceeding 5 elements
    data class ScheduledFilters(
        val query: String,
        val statuses: Set<String>,
        val storeIds: Set<Int>,
        val author: String?,
        val book: String?,
        val subTypeIds: Set<Int>
    )

    data class PreorderFilters(
        val query: String,
        val storeIds: Set<Int>,
        val author: String?,
        val book: String?,
        val statuses: Set<String>
    )

    @Suppress("UNCHECKED_CAST")
    private val scheduledFiltersFlow: Flow<ScheduledFilters> = combine(
        subOverviewSearch,
        subOverviewFilterTypes,
        subOverviewFilterBookstores,
        subOverviewFilterAuthor,
        subOverviewFilterBook,
        subOverviewFilterSubTypeIds
    ) { args: Array<Any?> ->
        ScheduledFilters(
            query = args[0] as String,
            statuses = args[1] as Set<String>,
            storeIds = args[2] as Set<Int>,
            author = args[3] as String?,
            book = args[4] as String?,
            subTypeIds = args[5] as Set<Int>
        )
    }

    @Suppress("UNCHECKED_CAST")
    private val preorderFiltersFlow: Flow<PreorderFilters> = combine(
        preorderSearch,
        preorderFilterBookstores,
        preorderFilterAuthor,
        preorderFilterBook,
        preorderFilterStatuses
    ) { args: Array<Any?> ->
        PreorderFilters(
            query = args[0] as String,
            storeIds = args[1] as Set<Int>,
            author = args[2] as String?,
            book = args[3] as String?,
            statuses = args[4] as Set<String>
        )
    }

    // 1. FILTERED Scheduled Subscriptions (Subscriptions Overview / Renewals)
    val filteredScheduledState: StateFlow<List<ScheduledWithDetails>> = combine(
        repository.scheduledWithDetailsFlow,
        scheduledFiltersFlow
    ) { list, filters ->
        list.filter { item ->
            val matchesQuery = filters.query.isBlank() ||
                    item.scheduled.bookTitle.contains(filters.query, ignoreCase = true) ||
                    item.scheduled.bookAuthor.contains(filters.query, ignoreCase = true) ||
                    (item.subscriptionType?.title?.contains(filters.query, ignoreCase = true) ?: false)
            val matchesStatus = filters.statuses.isEmpty() || item.scheduled.status in filters.statuses
            val matchesBookstore = filters.storeIds.isEmpty() || (item.bookstore?.id != null && item.bookstore.id in filters.storeIds)
            val matchesAuthor = filters.author == null || item.scheduled.bookAuthor.contains(filters.author, ignoreCase = true)
            val matchesBook = filters.book == null || item.scheduled.bookTitle.contains(filters.book, ignoreCase = true)
            val matchesSubType = filters.subTypeIds.isEmpty() || item.scheduled.subscriptionTypeId in filters.subTypeIds

            matchesQuery && matchesStatus && matchesBookstore && matchesAuthor && matchesBook && matchesSubType
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 2. FILTERED Subscription Types (Subscriptions main tab)
    @Suppress("UNCHECKED_CAST")
    val filteredSubscriptionsState: StateFlow<List<SubscriptionWithBookstore>> = combine(
        repository.subscriptionsWithBookstoreFlow,
        subSearch,
        subFilterStatuses,
        subFilterBookstores,
        subFilterFrequencies,
        subFilterSubTypeIds
    ) { args: Array<Any?> ->
        val list = args[0] as List<SubscriptionWithBookstore>
        val query = args[1] as String
        val statuses = args[2] as Set<String>
        val storeIds = args[3] as Set<Int>
        val frequencies = args[4] as Set<String>
        val subTypeIds = args[5] as Set<Int>

        list.filter { item ->
            val matchesQuery = query.isBlank() ||
                    item.subscription.title.contains(query, ignoreCase = true) ||
                    (item.bookstore?.name?.contains(query, ignoreCase = true) ?: false)
            val matchesStatus = statuses.isEmpty() || item.subscription.status in statuses
            val matchesBookstore = storeIds.isEmpty() || (item.bookstore?.id != null && item.bookstore.id in storeIds)
            val matchesFrequency = frequencies.isEmpty() || item.subscription.frequency in frequencies
            val matchesSubType = subTypeIds.isEmpty() || item.subscription.id in subTypeIds

            matchesQuery && matchesStatus && matchesBookstore && matchesFrequency && matchesSubType
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
            val matchesBookstore = filters.storeIds.isEmpty() || (item.bookstore?.id != null && item.bookstore.id in filters.storeIds)
            val matchesAuthor = filters.author == null || item.preorder.bookAuthor.contains(filters.author, ignoreCase = true)
            val matchesBook = filters.book == null || item.preorder.bookTitle.contains(filters.book, ignoreCase = true)
            val matchesStatus = filters.statuses.isEmpty() || item.preorder.status in filters.statuses

            matchesQuery && matchesBookstore && matchesAuthor && matchesBook && matchesStatus
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Database CRUD Actions ---
    fun updateProfile(username: String, currency: String, language: String, profilePic: String?, defaultScheduledSubCount: Int = 6, themeMode: String = "system", themeCombo: String = "default", dateFormat: String = "yyyy-MM-dd", displayAmounts: Boolean = false, country: String = "") {
        viewModelScope.launch {
            repository.saveUser(User(id = 1, username = username, currency = currency, language = language, profilePic = profilePic, defaultScheduledSubCount = defaultScheduledSubCount, themeMode = themeMode, themeCombo = themeCombo, dateFormat = dateFormat, displayAmounts = displayAmounts, country = country))
        }
    }

    fun updateDisplayAmounts(displayAmounts: Boolean) {
        viewModelScope.launch {
            val currentUser = userState.value ?: User(id = 1, username = "Eleanor Vance", profilePic = "avatar_classic", currency = "$", language = "English")
            repository.saveUser(currentUser.copy(displayAmounts = displayAmounts))
        }
    }

    fun updateThemeMode(themeMode: String) {
        viewModelScope.launch {
            val currentUser = userState.value ?: User(id = 1, username = "Eleanor Vance", profilePic = "avatar_classic", currency = "$", language = "English")
            repository.saveUser(currentUser.copy(themeMode = themeMode))
        }
    }

    fun updateThemeCombo(themeCombo: String) {
        viewModelScope.launch {
            val currentUser = userState.value ?: User(id = 1, username = "Eleanor Vance", profilePic = "avatar_classic", currency = "$", language = "English")
            repository.saveUser(currentUser.copy(themeCombo = themeCombo))
        }
    }

    fun addBookstore(
        name: String,
        website: String,
        profilePic: String?,
        contacts: List<BookstoreContact> = emptyList()
    ) {
        viewModelScope.launch {
            val storeId = repository.insertBookstore(Bookstore(name = name, website = website, profilePic = profilePic))
            if (contacts.isNotEmpty()) {
                repository.saveBookstoreContacts(storeId.toInt(), contacts)
            }
        }
    }

    fun updateBookstore(bookstore: Bookstore, contacts: List<BookstoreContact>? = null) {
        viewModelScope.launch {
            repository.updateBookstore(bookstore)
            if (contacts != null) {
                repository.saveBookstoreContacts(bookstore.id, contacts)
            }
        }
    }

    fun deleteBookstore(bookstore: Bookstore) {
        viewModelScope.launch {
            repository.deleteBookstore(bookstore)
        }
    }

    fun restoreBookstore(bookstore: Bookstore) {
        viewModelScope.launch {
            repository.insertBookstore(bookstore)
        }
    }

    fun addForwardingService(
        name: String,
        website: String,
        profilePic: String?,
        storageDays: Int? = null,
        contacts: List<ForwardingServiceContact> = emptyList(),
        reminderEnabled: Boolean = false,
        reminderDDayOffset: Int = 0,
        reminderHour: Int = 8,
        reminderMinute: Int = 0
    ) {
        viewModelScope.launch {
            val serviceId = repository.insertForwardingService(
                ForwardingService(
                    name = name,
                    website = website,
                    profilePic = profilePic,
                    storageDays = storageDays,
                    reminderEnabled = reminderEnabled,
                    reminderDDayOffset = reminderDDayOffset,
                    reminderHour = reminderHour,
                    reminderMinute = reminderMinute
                )
            )
            if (contacts.isNotEmpty()) {
                repository.saveForwardingServiceContacts(serviceId.toInt(), contacts)
            }
        }
    }

    fun updateForwardingService(service: ForwardingService, contacts: List<ForwardingServiceContact>? = null) {
        viewModelScope.launch {
            repository.updateForwardingService(service)
            if (contacts != null) {
                repository.saveForwardingServiceContacts(service.id, contacts)
            }
            syncStorageRemindersForService(service)
        }
    }

    fun deleteForwardingService(service: ForwardingService) {
        viewModelScope.launch {
            cancelStorageRemindersForService(service.id)
            repository.deleteForwardingService(service)
        }
    }

    fun restoreForwardingService(service: ForwardingService) {
        viewModelScope.launch {
            repository.insertForwardingService(service)
        }
    }

    fun addShippingCompany(
        name: String,
        website: String,
        profilePic: String?,
        contacts: List<ShippingCompanyContact> = emptyList()
    ) {
        viewModelScope.launch {
            val companyId = repository.insertShippingCompany(
                ShippingCompany(name = name, website = website, profilePic = profilePic)
            )
            if (contacts.isNotEmpty()) {
                repository.saveShippingCompanyContacts(companyId.toInt(), contacts)
            }
        }
    }

    fun updateShippingCompany(company: ShippingCompany, contacts: List<ShippingCompanyContact>? = null) {
        viewModelScope.launch {
            repository.updateShippingCompany(company)
            if (contacts != null) {
                repository.saveShippingCompanyContacts(company.id, contacts)
            }
        }
    }

    fun deleteShippingCompany(company: ShippingCompany) {
        viewModelScope.launch {
            repository.deleteShippingCompany(company)
        }
    }

    fun restoreShippingCompany(company: ShippingCompany) {
        viewModelScope.launch {
            repository.insertShippingCompany(company)
        }
    }

    private fun getTodayStartMs(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
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
        frequency: String,
        reminderEnabled: Boolean = false,
        reminderDDayOffset: Int = 0,
        reminderHour: Int = 8,
        reminderMinute: Int = 0,
        skipType: String = "None",
        numberOfSkips: Int? = null,
        numberOfMonths: Int? = null,
        skipMethod: String? = null,
        skipLink: String? = null,
        skipText: String? = null,
        picturePath: String? = null,
        skipMethods: List<SubscriptionSkipMethod> = emptyList(),
        shippingAddressId: Int? = null,
        currency: String? = null,
        basePrice: Double? = null,
        discountedAmount: Double? = null,
        shippingPrice: Double? = null,
        taxPrice: Double? = null,
        forwardShippingPrice: Double? = null,
        forwardTaxPrice: Double? = null
    ) {
        viewModelScope.launch {
            val isWishlist = status.equals("Wishlist", ignoreCase = true)
            val cleanDueDate = if (isWishlist) 0L else dueDate
            val cleanStartDate = if (isWishlist) 0L else startDate
            val cleanFinishDate = if (isWishlist) 0L else finishDate

            val subId = repository.insertSubscriptionType(
                SubscriptionType(
                    bookstoreId = bookstoreId,
                    title = title,
                    status = status,
                    price = price,
                    dueDate = cleanDueDate,
                    startDate = cleanStartDate,
                    finishDate = cleanFinishDate,
                    notificationAlertDays = notificationAlertDays,
                    frequency = frequency,
                    reminderEnabled = reminderEnabled,
                    reminderDDayOffset = reminderDDayOffset,
                    reminderHour = reminderHour,
                    reminderMinute = reminderMinute,
                    skipType = skipType,
                    numberOfSkips = numberOfSkips,
                    numberOfMonths = numberOfMonths,
                    skipMethod = skipMethod,
                    skipLink = skipLink,
                    skipText = skipText,
                    picturePath = picturePath,
                    shippingAddressId = shippingAddressId,
                    currency = currency,
                    basePrice = basePrice,
                    discountedAmount = discountedAmount,
                    shippingPrice = shippingPrice,
                    taxPrice = taxPrice,
                    forwardShippingPrice = forwardShippingPrice,
                    forwardTaxPrice = forwardTaxPrice
                )
            )

            if (skipMethods.isNotEmpty()) {
                repository.saveSubscriptionSkipMethods(subId.toInt(), skipMethods)
            }

            if (!isWishlist) {
                val limit = if (status.equals("Active", ignoreCase = true) && cleanFinishDate <= 0L) {
                    userState.value?.defaultScheduledSubCount ?: 6
                } else {
                    -1
                }
                val dates = generateScheduledSubDates(cleanDueDate, frequency, cleanStartDate, cleanFinishDate, limit)
                val todayStart = getTodayStartMs()

                dates.forEachIndexed { index, occurrenceDate ->
                    val scheduledStatus = if (isSameDay(System.currentTimeMillis(), occurrenceDate)) {
                        "Renewed"
                    } else if (occurrenceDate < todayStart) {
                        "Received"
                    } else {
                        "Upcoming"
                    }
                    val schedId = repository.insertScheduledSubscription(
                        ScheduledSubscription(
                            subscriptionTypeId = subId.toInt(),
                            bookTitle = "",
                            bookAuthor = "",
                            description = "Automatically scheduled delivery for subscription '$title'.",
                            dueDate = occurrenceDate,
                            status = scheduledStatus,
                            isSkipped = false,
                            picturePath = null
                        )
                    )

                    if (reminderEnabled && occurrenceDate >= todayStart) {
                        com.example.receiver.ReminderScheduler.scheduleScheduledSubReminder(
                            getApplication(),
                            schedId.toInt(),
                            title,
                            "",
                            occurrenceDate,
                            reminderDDayOffset,
                            reminderHour,
                            reminderMinute
                        )
                    }
                }
            }
        }
    }

    private fun generateScheduledSubDates(anchorDate: Long, frequency: String, startDate: Long, finishDate: Long, limit: Int = -1): List<Long> {
        val dates = mutableListOf<Long>()
        val cal = Calendar.getInstance().apply { timeInMillis = anchorDate }
        val todayStart = getTodayStartMs()

        if (limit > 0 && cal.timeInMillis < todayStart) {
            var rollIterations = 0
            while (cal.timeInMillis < todayStart && rollIterations < 100) {
                when (frequency) {
                    "Weekly" -> cal.add(Calendar.WEEK_OF_YEAR, 1)
                    "Monthly" -> cal.add(Calendar.MONTH, 1)
                    "Bi-Monthly" -> cal.add(Calendar.MONTH, 2)
                    "Quarterly" -> cal.add(Calendar.MONTH, 3)
                    "Yearly" -> cal.add(Calendar.YEAR, 1)
                    else -> cal.add(Calendar.MONTH, 1)
                }
                rollIterations++
            }
        }

        var current = cal.timeInMillis
        var iterations = 0
        if (limit > 0) {
            while (dates.size < limit && iterations < 100) {
                if (current >= startDate) {
                    dates.add(current)
                }
                when (frequency) {
                    "Weekly" -> cal.add(Calendar.WEEK_OF_YEAR, 1)
                    "Monthly" -> cal.add(Calendar.MONTH, 1)
                    "Bi-Monthly" -> cal.add(Calendar.MONTH, 2)
                    "Quarterly" -> cal.add(Calendar.MONTH, 3)
                    "Yearly" -> cal.add(Calendar.YEAR, 1)
                    else -> cal.add(Calendar.MONTH, 1)
                }
                current = cal.timeInMillis
                iterations++
            }
        } else {
            while (current <= finishDate && iterations < 100) {
                if (current >= startDate) {
                    dates.add(current)
                }
                when (frequency) {
                    "Weekly" -> cal.add(Calendar.WEEK_OF_YEAR, 1)
                    "Monthly" -> cal.add(Calendar.MONTH, 1)
                    "Bi-Monthly" -> cal.add(Calendar.MONTH, 2)
                    "Quarterly" -> cal.add(Calendar.MONTH, 3)
                    "Yearly" -> cal.add(Calendar.YEAR, 1)
                    else -> cal.add(Calendar.MONTH, 1)
                }
                current = cal.timeInMillis
                iterations++
            }
        }
        return dates
    }

    fun updateSubscriptionType(subscription: SubscriptionType, skipMethods: List<SubscriptionSkipMethod>? = null) {
        viewModelScope.launch {
            repository.updateSubscriptionType(subscription)
            if (skipMethods != null) {
                repository.saveSubscriptionSkipMethods(subscription.id, skipMethods)
            }
            
            val existingScheduled = repository.getScheduledSubscriptionsForType(subscription.id).sortedBy { it.dueDate }
            existingScheduled.forEach { sched ->
                com.example.receiver.ReminderScheduler.cancelScheduledSubReminder(getApplication(), sched.id)
            }

            val isWishlist = subscription.status.equals("Wishlist", ignoreCase = true)
            if (isWishlist) {
                existingScheduled.forEach { sched ->
                    if (sched.status == "Upcoming") {
                        repository.deleteScheduledSubscription(sched)
                    }
                }
            } else {
                val cleanDueDate = subscription.dueDate
                val cleanStartDate = subscription.startDate
                val cleanFinishDate = subscription.finishDate
                val isUnlimitedActive = subscription.status.equals("Active", ignoreCase = true) && cleanFinishDate <= 0L
                val limit = if (isUnlimitedActive) (userState.value?.defaultScheduledSubCount ?: 6) else -1
                val targetDates = generateScheduledSubDates(cleanDueDate, subscription.frequency, cleanStartDate, cleanFinishDate, limit)
                val todayStart = getTodayStartMs()

                val matchedExistingIds = mutableSetOf<Int>()
                val coveredDates = mutableSetOf<Long>()

                // 1. Identify existing items that already match target dates
                for (date in targetDates) {
                    val match = existingScheduled.firstOrNull { it.id !in matchedExistingIds && it.dueDate == date }
                    if (match != null) {
                        matchedExistingIds.add(match.id)
                        coveredDates.add(date)
                    }
                }

                // 2. For target dates not covered, reuse unmatched scheduled items equal or bigger than today (or upcoming) according to new frequency
                val reusableUnmatched = existingScheduled.filter {
                    it.id !in matchedExistingIds && (it.dueDate >= todayStart || it.status == "Upcoming")
                }.toMutableList()

                for (missingDate in targetDates) {
                    if (missingDate in coveredDates) continue

                    if (reusableUnmatched.isNotEmpty()) {
                        val itemToReuse = reusableUnmatched.removeAt(0)
                        matchedExistingIds.add(itemToReuse.id)

                        val newStatus = if (isSameDay(System.currentTimeMillis(), missingDate)) {
                            "Renewed"
                        } else if (missingDate < todayStart) {
                            "Received"
                        } else {
                            "Upcoming"
                        }

                        repository.updateScheduledSubscription(
                            itemToReuse.copy(
                                dueDate = missingDate,
                                status = if (itemToReuse.status == "Upcoming") newStatus else itemToReuse.status
                            )
                        )
                    } else {
                        val scheduledStatus = if (isSameDay(System.currentTimeMillis(), missingDate)) {
                            "Renewed"
                        } else if (missingDate < todayStart) {
                            "Received"
                        } else {
                            "Upcoming"
                        }
                        repository.insertScheduledSubscription(
                            ScheduledSubscription(
                                subscriptionTypeId = subscription.id,
                                bookTitle = "",
                                bookAuthor = "",
                                description = "Automatically scheduled delivery for subscription '${subscription.title}'.",
                                dueDate = missingDate,
                                status = scheduledStatus,
                                isSkipped = false,
                                picturePath = null
                            )
                        )
                    }
                }

                // 3. Delete remaining unmatched items equal or bigger than today (or upcoming) that fall outside the new schedule
                val remainingUnmatched = existingScheduled.filter { it.id !in matchedExistingIds }
                for (item in remainingUnmatched) {
                    if (item.dueDate >= todayStart || item.status == "Upcoming") {
                        repository.deleteScheduledSubscription(item)
                    }
                }
            }

            // Re-schedule remaining future ones if reminder is enabled
            if (subscription.reminderEnabled) {
                val updatedScheduled = repository.getScheduledSubscriptionsForType(subscription.id)
                val todayStart = getTodayStartMs()
                updatedScheduled.forEach { sched ->
                    if (sched.dueDate >= todayStart && !sched.status.equals("Skipped", ignoreCase = true) && !sched.isSkipped) {
                        com.example.receiver.ReminderScheduler.scheduleScheduledSubReminder(
                            getApplication(),
                            sched.id,
                            subscription.title,
                            sched.bookTitle,
                            sched.dueDate,
                            subscription.reminderDDayOffset,
                            subscription.reminderHour,
                            subscription.reminderMinute
                        )
                    }
                }
            }

            // Also check and fill up active open-ended subscriptions
            checkAndGenerateActiveSubscriptions()
        }
    }

    suspend fun checkAndGenerateActiveSubscriptions() {
        val user = repository.userFlow.first()
        val limit = user?.defaultScheduledSubCount ?: 6

        val allSubs = repository.subscriptionsWithBookstoreFlow.first()
        val activeSubs = allSubs.map { it.subscription }
            .filter { it.status.equals("Active", ignoreCase = true) && it.finishDate <= 0L }

        for (sub in activeSubs) {
            val existing = repository.getScheduledSubscriptionsForType(sub.id)
                .sortedBy { it.dueDate }
            if (existing.size < limit) {
                val needed = limit - existing.size
                val startAnchor = if (existing.isNotEmpty()) {
                    existing.last().dueDate
                } else {
                    sub.dueDate
                }

                val nextDates = mutableListOf<Long>()
                val cal = Calendar.getInstance().apply { timeInMillis = startAnchor }

                // If starting from an existing one, we need to advance first to get the next date.
                if (existing.isNotEmpty()) {
                    when (sub.frequency) {
                        "Weekly" -> cal.add(Calendar.WEEK_OF_YEAR, 1)
                        "Monthly" -> cal.add(Calendar.MONTH, 1)
                        "Bi-Monthly" -> cal.add(Calendar.MONTH, 2)
                        "Quarterly" -> cal.add(Calendar.MONTH, 3)
                        "Yearly" -> cal.add(Calendar.YEAR, 1)
                        else -> cal.add(Calendar.MONTH, 1)
                    }
                }

                var current = cal.timeInMillis
                var iterations = 0
                while (nextDates.size < needed && iterations < 100) {
                    if (current >= sub.startDate) {
                        nextDates.add(current)
                    }
                    when (sub.frequency) {
                        "Weekly" -> cal.add(Calendar.WEEK_OF_YEAR, 1)
                        "Monthly" -> cal.add(Calendar.MONTH, 1)
                        "Bi-Monthly" -> cal.add(Calendar.MONTH, 2)
                        "Quarterly" -> cal.add(Calendar.MONTH, 3)
                        "Yearly" -> cal.add(Calendar.YEAR, 1)
                        else -> cal.add(Calendar.MONTH, 1)
                    }
                    current = cal.timeInMillis
                    iterations++
                }

                val todayStart = getTodayStartMs()
                nextDates.forEach { occurrenceDate ->
                    val scheduledStatus = if (isSameDay(System.currentTimeMillis(), occurrenceDate)) {
                        "Renewed"
                    } else if (occurrenceDate < todayStart) {
                        "Received"
                    } else {
                        "Upcoming"
                    }
                    val schedId = repository.insertScheduledSubscription(
                        ScheduledSubscription(
                            subscriptionTypeId = sub.id,
                            bookTitle = "",
                            bookAuthor = "",
                            description = "Automatically scheduled delivery for subscription '${sub.title}'.",
                            dueDate = occurrenceDate,
                            status = scheduledStatus,
                            isSkipped = false,
                            picturePath = null
                        )
                    )

                    if (sub.reminderEnabled && occurrenceDate >= todayStart) {
                        com.example.receiver.ReminderScheduler.scheduleScheduledSubReminder(
                            getApplication(),
                            schedId.toInt(),
                            sub.title,
                            "",
                            occurrenceDate,
                            sub.reminderDDayOffset,
                            sub.reminderHour,
                            sub.reminderMinute
                        )
                    }
                }
            }
        }
    }

    fun deleteSubscriptionType(subscription: SubscriptionType) {
        viewModelScope.launch {
            val relatedScheduled = repository.getScheduledSubscriptionsForType(subscription.id)
            relatedScheduled.forEach { sched ->
                com.example.receiver.ReminderScheduler.cancelScheduledSubReminder(getApplication(), sched.id)
            }
            repository.deleteSubscriptionType(subscription)
        }
    }

    fun generatePastScheduledSubscriptions(
        bookstoreId: Int?,
        subscriptionTypeId: Int?,
        startDate: Long,
        endDate: Long
    ) {
        viewModelScope.launch {
            val allSubs = rawSubscriptionsState.value
            val targetSubs = allSubs.filter { subWithDetails ->
                val sub = subWithDetails.subscription
                val matchesBookstore = (bookstoreId == null || bookstoreId <= 0 || sub.bookstoreId == bookstoreId)
                val matchesSubType = (subscriptionTypeId == null || subscriptionTypeId <= 0 || sub.id == subscriptionTypeId)
                val isActiveOrValid = !sub.status.equals("Wishlist", ignoreCase = true) && !sub.status.equals("Canceled", ignoreCase = true)
                matchesBookstore && matchesSubType && isActiveOrValid
            }

            val todayStart = getTodayStartMs()

            targetSubs.forEach { subWithDetails ->
                val sub = subWithDetails.subscription
                val existingScheduled = repository.getScheduledSubscriptionsForType(sub.id)

                val anchorDate = if (sub.dueDate > 0L) sub.dueDate else if (sub.startDate > 0L) sub.startDate else startDate
                val datesToInsert = mutableListOf<Long>()
                val cal = Calendar.getInstance().apply { timeInMillis = anchorDate }

                if (cal.timeInMillis > startDate) {
                    while (cal.timeInMillis > startDate) {
                        when (sub.frequency) {
                            "Weekly" -> cal.add(Calendar.WEEK_OF_YEAR, -1)
                            "Monthly" -> cal.add(Calendar.MONTH, -1)
                            "Bi-Monthly" -> cal.add(Calendar.MONTH, -2)
                            "Quarterly" -> cal.add(Calendar.MONTH, -3)
                            "Yearly" -> cal.add(Calendar.YEAR, -1)
                            else -> cal.add(Calendar.MONTH, -1)
                        }
                    }
                    if (cal.timeInMillis < startDate) {
                        when (sub.frequency) {
                            "Weekly" -> cal.add(Calendar.WEEK_OF_YEAR, 1)
                            "Monthly" -> cal.add(Calendar.MONTH, 1)
                            "Bi-Monthly" -> cal.add(Calendar.MONTH, 2)
                            "Quarterly" -> cal.add(Calendar.MONTH, 3)
                            "Yearly" -> cal.add(Calendar.YEAR, 1)
                            else -> cal.add(Calendar.MONTH, 1)
                        }
                    }
                } else {
                    while (cal.timeInMillis < startDate) {
                        when (sub.frequency) {
                            "Weekly" -> cal.add(Calendar.WEEK_OF_YEAR, 1)
                            "Monthly" -> cal.add(Calendar.MONTH, 1)
                            "Bi-Monthly" -> cal.add(Calendar.MONTH, 2)
                            "Quarterly" -> cal.add(Calendar.MONTH, 3)
                            "Yearly" -> cal.add(Calendar.YEAR, 1)
                            else -> cal.add(Calendar.MONTH, 1)
                        }
                    }
                }

                var current = cal.timeInMillis
                var iterations = 0
                while (current <= endDate && iterations < 500) {
                    if (current >= startDate) {
                        val alreadyExists = existingScheduled.any { isSameDay(it.dueDate, current) }
                        if (!alreadyExists) {
                            datesToInsert.add(current)
                        }
                    }
                    when (sub.frequency) {
                        "Weekly" -> cal.add(Calendar.WEEK_OF_YEAR, 1)
                        "Monthly" -> cal.add(Calendar.MONTH, 1)
                        "Bi-Monthly" -> cal.add(Calendar.MONTH, 2)
                        "Quarterly" -> cal.add(Calendar.MONTH, 3)
                        "Yearly" -> cal.add(Calendar.YEAR, 1)
                        else -> cal.add(Calendar.MONTH, 1)
                    }
                    current = cal.timeInMillis
                    iterations++
                }

                datesToInsert.forEach { occurrenceDate ->
                    val scheduledStatus = if (isSameDay(System.currentTimeMillis(), occurrenceDate)) {
                        "Renewed"
                    } else if (occurrenceDate < todayStart) {
                        "Received"
                    } else {
                        "Upcoming"
                    }

                    val schedId = repository.insertScheduledSubscription(
                        ScheduledSubscription(
                            subscriptionTypeId = sub.id,
                            bookTitle = "",
                            bookAuthor = "",
                            description = "Automatically scheduled delivery for subscription '${sub.title}'.",
                            dueDate = occurrenceDate,
                            status = scheduledStatus,
                            isSkipped = false,
                            picturePath = null
                        )
                    )

                    if (sub.reminderEnabled && occurrenceDate >= todayStart) {
                        com.example.receiver.ReminderScheduler.scheduleScheduledSubReminder(
                            getApplication(),
                            schedId.toInt(),
                            sub.title,
                            "",
                            occurrenceDate,
                            sub.reminderDDayOffset,
                            sub.reminderHour,
                            sub.reminderMinute
                        )
                    }
                    syncStorageReminderForOrigin("scheduled_subs", schedId.toInt())
                }
            }
        }
    }

    fun addScheduledSubscription(
        subscriptionTypeId: Int,
        bookTitle: String,
        bookAuthor: String,
        description: String,
        dueDate: Long,
        status: String,
        picturePath: String? = null,
        rating: Double = 0.0
    ) {
        viewModelScope.launch {
            val newId = repository.insertScheduledSubscription(
                ScheduledSubscription(
                    subscriptionTypeId = subscriptionTypeId,
                    bookTitle = bookTitle,
                    bookAuthor = bookAuthor,
                    description = description,
                    dueDate = dueDate,
                    status = status,
                    isSkipped = false,
                    picturePath = picturePath,
                    rating = rating
                )
            )
            syncStorageReminderForOrigin("scheduled_subs", newId.toInt())
        }
    }

    fun updateScheduledSubscription(scheduled: ScheduledSubscription) {
        viewModelScope.launch {
            val oldScheduled = repository.getScheduledSubscriptionById(scheduled.id)
            val wasSkipped = oldScheduled?.status.equals("Skipped", ignoreCase = true) || oldScheduled?.isSkipped == true
            val isNowSkipped = scheduled.status.equals("Skipped", ignoreCase = true) || scheduled.isSkipped

            val pattern = userState.value?.dateFormat ?: "yyyy-MM-dd"

            if (!wasSkipped && isNowSkipped) {
                val error = repository.processSkipForScheduledSub(scheduled, pattern)
                if (error != null) {
                    alertMessage.value = error
                    return@launch
                }
            } else if (wasSkipped && !isNowSkipped) {
                repository.processUnskipForScheduledSub(scheduled)
            }

            repository.updateScheduledSubscription(scheduled)
            // Cancel old reminder
            com.example.receiver.ReminderScheduler.cancelScheduledSubReminder(getApplication(), scheduled.id)
            
            // Re-schedule reminder if parent subscription type has reminders enabled
            val subType = repository.getSubscriptionTypeById(scheduled.subscriptionTypeId)
            if (subType != null && subType.reminderEnabled && !isNowSkipped) {
                com.example.receiver.ReminderScheduler.scheduleScheduledSubReminder(
                    getApplication(),
                    scheduled.id,
                    subType.title,
                    scheduled.bookTitle,
                    scheduled.dueDate,
                    subType.reminderDDayOffset,
                    subType.reminderHour,
                    subType.reminderMinute
                )
            }
            syncStorageReminderForOrigin("scheduled_subs", scheduled.id)
        }
    }

    fun deleteScheduledSubscription(scheduled: ScheduledSubscription) {
        viewModelScope.launch {
            com.example.receiver.ReminderScheduler.cancelScheduledSubReminder(getApplication(), scheduled.id)
            val pkg = repository.getPackageDirect("scheduled_subs", scheduled.id)
            if (pkg != null) {
                com.example.receiver.ReminderScheduler.cancelForwardingStorageReminder(getApplication(), pkg.id)
            }
            repository.deleteScheduledSubscription(scheduled)
        }
    }

    fun toggleSkipScheduledSubscription(
        scheduled: ScheduledSubscription,
        onSkipSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            val isCurrentlySkipped = scheduled.status.equals("Skipped", ignoreCase = true) || scheduled.isSkipped
            val newStatus = if (isCurrentlySkipped) "Upcoming" else "Skipped"
            val updated = scheduled.copy(status = newStatus, isSkipped = !isCurrentlySkipped)

            val pattern = userState.value?.dateFormat ?: "yyyy-MM-dd"

            if (!isCurrentlySkipped) {
                val error = repository.processSkipForScheduledSub(updated, pattern)
                if (error != null) {
                    alertMessage.value = error
                    return@launch
                }
            } else {
                repository.processUnskipForScheduledSub(scheduled)
            }

            repository.updateScheduledSubscription(updated)

            if (!isCurrentlySkipped) {
                onSkipSuccess?.invoke()
            }

            com.example.receiver.ReminderScheduler.cancelScheduledSubReminder(getApplication(), scheduled.id)
            if (!updated.isSkipped) {
                val subType = repository.getSubscriptionTypeById(scheduled.subscriptionTypeId)
                if (subType != null && subType.reminderEnabled) {
                    com.example.receiver.ReminderScheduler.scheduleScheduledSubReminder(
                        getApplication(),
                        scheduled.id,
                        subType.title,
                        scheduled.bookTitle,
                        scheduled.dueDate,
                        subType.reminderDDayOffset,
                        subType.reminderHour,
                        subType.reminderMinute
                    )
                }
            }
        }
    }

    fun recalculateSubscriptionSkips() {
        viewModelScope.launch {
            val pattern = userState.value?.dateFormat ?: "yyyy-MM-dd"
            repository.recalculateSubscriptionSkips(pattern)
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
        picturePath: String? = null,
        reminderEnabled: Boolean = false,
        reminderDDayOffset: Int = 0,
        reminderHour: Int = 8,
        reminderMinute: Int = 0,
        rating: Double = 0.0,
        shippingAddressId: Int? = null,
        currency: String? = null,
        basePrice: Double? = null,
        discountedAmount: Double? = null,
        shippingPrice: Double? = null,
        taxPrice: Double? = null,
        forwardShippingPrice: Double? = null,
        forwardTaxPrice: Double? = null
    ) {
        viewModelScope.launch {
            val today = System.currentTimeMillis()
            val finalStatus = if (today >= saleDateStart && today <= saleDateEnd) "Released" else status
            val preorderId = repository.insertPreorder(
                Preorder(
                    bookstoreId = bookstoreId,
                    picturePath = picturePath,
                    bookTitle = bookTitle,
                    bookAuthor = bookAuthor,
                    description = description,
                    price = price,
                    rangedSaleDateStart = saleDateStart,
                    rangedSaleDateEnd = saleDateEnd,
                    status = finalStatus,
                    reminderEnabled = reminderEnabled,
                    reminderDDayOffset = reminderDDayOffset,
                    reminderHour = reminderHour,
                    reminderMinute = reminderMinute,
                    rating = rating,
                    shippingAddressId = shippingAddressId,
                    currency = currency,
                    basePrice = basePrice,
                    discountedAmount = discountedAmount,
                    shippingPrice = shippingPrice,
                    taxPrice = taxPrice,
                    forwardShippingPrice = forwardShippingPrice,
                    forwardTaxPrice = forwardTaxPrice
                )
            )

            if (reminderEnabled) {
                com.example.receiver.ReminderScheduler.schedulePreorderReminder(
                    getApplication(),
                    preorderId.toInt(),
                    bookTitle,
                    saleDateStart,
                    reminderDDayOffset,
                    reminderHour,
                    reminderMinute
                )
            }
            syncStorageReminderForOrigin("preorders", preorderId.toInt())
        }
    }

    fun updatePreorder(preorder: Preorder) {
        viewModelScope.launch {
            repository.updatePreorder(preorder)
            com.example.receiver.ReminderScheduler.cancelPreorderReminder(getApplication(), preorder.id)
            if (preorder.reminderEnabled) {
                com.example.receiver.ReminderScheduler.schedulePreorderReminder(
                    getApplication(),
                    preorder.id,
                    preorder.bookTitle,
                    preorder.rangedSaleDateStart,
                    preorder.reminderDDayOffset,
                    preorder.reminderHour,
                    preorder.reminderMinute
                )
            }
            syncStorageReminderForOrigin("preorders", preorder.id)
        }
    }

    fun deletePreorder(preorder: Preorder) {
        viewModelScope.launch {
            com.example.receiver.ReminderScheduler.cancelPreorderReminder(getApplication(), preorder.id)
            val pkg = repository.getPackageDirect("preorders", preorder.id)
            if (pkg != null) {
                com.example.receiver.ReminderScheduler.cancelForwardingStorageReminder(getApplication(), pkg.id)
            }
            repository.deletePreorder(preorder)
        }
    }

    fun restorePreorder(preorder: Preorder) {
        viewModelScope.launch {
            repository.insertPreorder(preorder)
        }
    }

    // --- CSV EXCEL EXPORTING ---
    fun exportToExcelSpreadsheet(context: Context) {
        viewModelScope.launch {
            val user = userState.value ?: User(id = 1, username = "Guest", profilePic = null, currency = "$", language = "English", themeMode = "system")
            val xmlString = ExcelExporter.generateWorkbookXml(
                user = user,
                bookstores = bookstoresState.value,
                bookstoreContacts = bookstoreContactsState.value,
                forwardingServices = forwardingServicesState.value,
                forwardingServiceContacts = forwardingServiceContactsState.value,
                shippingCompanies = shippingCompaniesState.value,
                shippingCompanyContacts = shippingCompanyContactsState.value,
                userAddresses = userAddressesState.value,
                subscriptions = rawSubscriptionsState.value,
                subscriptionSkipMethods = allSubscriptionSkipMethodsState.value,
                subscriptionSkips = allSubscriptionSkipsState.value,
                scheduled = rawScheduledState.value,
                preorders = rawPreordersState.value,
                packages = allPackagesState.value
            )
            shareExportFile(context, "bookish_library_export.xls", xmlString, "application/vnd.ms-excel")
        }
    }

    private fun shareExportFile(context: Context, fileName: String, content: String, mimeType: String) {
        try {
            val file = File(context.cacheDir, fileName)
            file.writeText(content)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
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

    // Packages
    fun getPackageFlow(originTable: String, originId: Int): Flow<PackageItem?> {
        return repository.getPackageFlow(originTable, originId)
    }

    suspend fun getOrCreatePackage(originTable: String, originId: Int): PackageItem {
        return repository.getOrCreatePackage(originTable, originId)
    }

    fun updatePackage(packageItem: PackageItem) {
        viewModelScope.launch {
            repository.updatePackage(packageItem)
            syncStorageReminderForOrigin(packageItem.originTable, packageItem.originId)
        }
    }

    // Forwarding Service Storage Reminders
    private suspend fun syncStorageReminderForOrigin(originTable: String, originId: Int) {
        val pkg = repository.getPackageDirect(originTable, originId) ?: return
        var bookTitle = ""
        var status = ""
        var forwardingService: ForwardingService? = null

        if (originTable == "preorders") {
            val preorder = repository.getPreorderById(originId) ?: return
            bookTitle = preorder.bookTitle
            status = preorder.status
            val address = preorder.shippingAddressId?.let { repository.getUserAddressById(it) }
            forwardingService = address?.forwardingServiceId?.let { repository.getForwardingServiceById(it) }
        } else if (originTable == "scheduled_subs") {
            val scheduled = repository.getScheduledSubscriptionById(originId) ?: return
            bookTitle = scheduled.bookTitle
            status = scheduled.status
            val subType = repository.getSubscriptionTypeById(scheduled.subscriptionTypeId)
            val address = subType?.shippingAddressId?.let { repository.getUserAddressById(it) }
            forwardingService = address?.forwardingServiceId?.let { repository.getForwardingServiceById(it) }
        }

        val curStatus = status.lowercase().trim()
        val isStoredAtForwarder = forwardingService != null &&
            curStatus !in listOf("shipped", "received") &&
            pkg.forwarderShippedDate == null &&
            (curStatus == "in suite" || pkg.forwarderReceivedDate != null)

        val storageDays = forwardingService?.storageDays
        if (isStoredAtForwarder && forwardingService != null && forwardingService.reminderEnabled && storageDays != null && storageDays > 0) {
            val effectiveReceivedDate = pkg.forwarderReceivedDate ?: if (curStatus == "in suite") System.currentTimeMillis() else null
            if (effectiveReceivedDate != null) {
                val utcCal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
                    timeInMillis = effectiveReceivedDate
                    add(java.util.Calendar.DAY_OF_YEAR, storageDays)
                }
                val limitDate = utcCal.timeInMillis
                com.example.receiver.ReminderScheduler.scheduleForwardingStorageReminder(
                    getApplication(),
                    pkg.id,
                    forwardingService.name,
                    bookTitle,
                    limitDate,
                    forwardingService.reminderDDayOffset,
                    forwardingService.reminderHour,
                    forwardingService.reminderMinute
                )
                return
            }
        }
        com.example.receiver.ReminderScheduler.cancelForwardingStorageReminder(getApplication(), pkg.id)
    }

    private suspend fun syncStorageRemindersForService(service: ForwardingService) {
        val userAddresses = userAddressesState.value
        val serviceAddressIds = userAddresses.filter { it.forwardingServiceId == service.id }.map { it.id }.toSet()
        if (serviceAddressIds.isEmpty()) return

        val preorders = rawPreordersState.value
        for (item in preorders) {
            if (item.preorder.shippingAddressId in serviceAddressIds) {
                syncStorageReminderForOrigin("preorders", item.preorder.id)
            }
        }

        val scheduledList = rawScheduledState.value
        for (item in scheduledList) {
            if (item.subscriptionType?.shippingAddressId in serviceAddressIds) {
                syncStorageReminderForOrigin("scheduled_subs", item.scheduled.id)
            }
        }
    }

    private suspend fun cancelStorageRemindersForService(serviceId: Int) {
        val userAddresses = userAddressesState.value
        val serviceAddressIds = userAddresses.filter { it.forwardingServiceId == serviceId }.map { it.id }.toSet()
        if (serviceAddressIds.isEmpty()) return

        val preorders = rawPreordersState.value
        for (item in preorders) {
            if (item.preorder.shippingAddressId in serviceAddressIds) {
                val pkg = repository.getPackageDirect("preorders", item.preorder.id)
                if (pkg != null) {
                    com.example.receiver.ReminderScheduler.cancelForwardingStorageReminder(getApplication(), pkg.id)
                }
            }
        }

        val scheduledList = rawScheduledState.value
        for (item in scheduledList) {
            if (item.subscriptionType?.shippingAddressId in serviceAddressIds) {
                val pkg = repository.getPackageDirect("scheduled_subs", item.scheduled.id)
                if (pkg != null) {
                    com.example.receiver.ReminderScheduler.cancelForwardingStorageReminder(getApplication(), pkg.id)
                }
            }
        }
    }
}
