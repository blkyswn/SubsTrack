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
    val subOverviewFilterSubTypeId = MutableStateFlow<Int?>(null)
    val subOverviewIsCalendarView = MutableStateFlow(false)

    val subSearch = MutableStateFlow("")
    val subFilterStatus = MutableStateFlow<String?>(null) // Active, Waitlist, Paused, Canceled
    val subFilterBookstore = MutableStateFlow<Int?>(null)
    val subFilterFrequency = MutableStateFlow<String?>(null)
    val subFilterSubTypeId = MutableStateFlow<Int?>(null)
    val subIsCalendarView = MutableStateFlow(false)
    val subSelectedTab = MutableStateFlow(0) // 0 = Overview (Renewals), 1 = Subscriptions

    val preorderSearch = MutableStateFlow("")
    val preorderFilterBookstore = MutableStateFlow<Int?>(null)
    val preorderFilterAuthor = MutableStateFlow<String?>(null)
    val preorderFilterBook = MutableStateFlow<String?>(null)
    val preorderFilterStatus = MutableStateFlow<String?>(null) // Upcoming, Released, Preordered, Shipped, Received
    val preorderIsCalendarView = MutableStateFlow(false)

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

    val userAddressesState: StateFlow<List<UserAddress>> = repository.allUserAddressesFlow
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
        val status: String?,
        val storeId: Int?,
        val author: String?,
        val book: String?,
        val subTypeId: Int? = null
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
        subOverviewFilterBook,
        subOverviewFilterSubTypeId
    ) { args: Array<Any?> ->
        ScheduledFilters(
            query = args[0] as String,
            status = args[1] as String?,
            storeId = args[2] as Int?,
            author = args[3] as String?,
            book = args[4] as String?,
            subTypeId = args[5] as Int?
        )
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
            val matchesSubType = filters.subTypeId == null || item.scheduled.subscriptionTypeId == filters.subTypeId

            matchesQuery && matchesStatus && matchesBookstore && matchesAuthor && matchesBook && matchesSubType
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 2. FILTERED Subscription Types (Subscriptions main tab)
    val filteredSubscriptionsState: StateFlow<List<SubscriptionWithBookstore>> = combine(
        repository.subscriptionsWithBookstoreFlow,
        subSearch,
        subFilterStatus,
        subFilterBookstore,
        subFilterFrequency,
        subFilterSubTypeId
    ) { args: Array<Any?> ->
        val list = args[0] as List<SubscriptionWithBookstore>
        val query = args[1] as String
        val status = args[2] as String?
        val storeId = args[3] as Int?
        val frequency = args[4] as String?
        val subTypeId = args[5] as Int?

        list.filter { item ->
            val matchesQuery = query.isBlank() ||
                    item.subscription.title.contains(query, ignoreCase = true) ||
                    (item.bookstore?.name?.contains(query, ignoreCase = true) ?: false)
            val matchesStatus = status == null || item.subscription.status == status
            val matchesBookstore = storeId == null || item.bookstore?.id == storeId
            val matchesFrequency = frequency == null || item.subscription.frequency == frequency
            val matchesSubType = subTypeId == null || item.subscription.id == subTypeId

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
            val matchesBookstore = filters.storeId == null || item.bookstore?.id == filters.storeId
            val matchesAuthor = filters.author == null || item.preorder.bookAuthor.contains(filters.author, ignoreCase = true)
            val matchesBook = filters.book == null || item.preorder.bookTitle.contains(filters.book, ignoreCase = true)
            val matchesStatus = filters.status == null || item.preorder.status == filters.status

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
        contacts: List<ForwardingServiceContact> = emptyList()
    ) {
        viewModelScope.launch {
            val serviceId = repository.insertForwardingService(ForwardingService(name = name, website = website, profilePic = profilePic))
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
        }
    }

    fun deleteForwardingService(service: ForwardingService) {
        viewModelScope.launch {
            repository.deleteForwardingService(service)
        }
    }

    fun restoreForwardingService(service: ForwardingService) {
        viewModelScope.launch {
            repository.insertForwardingService(service)
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
            repository.insertScheduledSubscription(
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
        }
    }

    fun deleteScheduledSubscription(scheduled: ScheduledSubscription) {
        viewModelScope.launch {
            com.example.receiver.ReminderScheduler.cancelScheduledSubReminder(getApplication(), scheduled.id)
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
        }
    }

    fun deletePreorder(preorder: Preorder) {
        viewModelScope.launch {
            com.example.receiver.ReminderScheduler.cancelPreorderReminder(getApplication(), preorder.id)
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
            val bookstores = bookstoresState.value
            val subscriptions = rawSubscriptionsState.value
            val scheduled = rawScheduledState.value
            val preorders = rawPreordersState.value
            val user = userState.value ?: User(id = 1, username = "Guest", profilePic = null, currency = "$", language = "English", themeMode = "system")

            val userDateFormatPattern = user.dateFormat ?: "yyyy-MM-dd"
            val dateFormat = SimpleDateFormat(userDateFormatPattern, Locale.getDefault())

            val xmlBuilder = StringBuilder()

            xmlBuilder.append("""<?xml version="1.0" encoding="UTF-8"?>
<?mso-application progid="Excel.Sheet"?>
<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet"
 xmlns:o="urn:schemas-microsoft-com:office:office"
 xmlns:x="urn:schemas-microsoft-com:office:excel"
 xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet"
 xmlns:html="http://www.w3.org/TR/REC-html40">
 <DocumentProperties xmlns="urn:schemas-microsoft-com:office:office">
  <Author>Bookish</Author>
  <Title>Bookish Export</Title>
 </DocumentProperties>
 <Styles>
  <Style ss:ID="Default" ss:Name="Normal">
   <Alignment ss:Vertical="Bottom"/>
   <Font ss:FontName="Calibri" x:Family="Swiss" ss:Size="11" ss:Color="#000000"/>
  </Style>
  <Style ss:ID="Header">
   <Font ss:FontName="Calibri" ss:Size="11" ss:Color="#1E293B" ss:Bold="1"/>
   <Interior ss:Color="#E2E8F0" ss:Pattern="Solid"/>
  </Style>
  <Style ss:ID="Title">
   <Font ss:FontName="Calibri" ss:Size="14" ss:Color="#0F172A" ss:Bold="1"/>
  </Style>
 </Styles>
""")

            // 1. Overview Sheet
            xmlBuilder.append(""" <Worksheet ss:Name="Overview">
  <Table>
   <Column ss:Width="180"/>
   <Column ss:Width="220"/>
   <Row>
    <Cell ss:StyleID="Title"><Data ss:Type="String">BOOKISH DATA EXPORT</Data></Cell>
   </Row>
   <Row>
    <Cell><Data ss:Type="String">Export Date</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(dateFormat.format(Date()))}</Data></Cell>
   </Row>
   <Row>
    <Cell><Data ss:Type="String">User Name</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(user.username)}</Data></Cell>
   </Row>
   <Row>
    <Cell><Data ss:Type="String">Preferred Currency</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(user.currency)}</Data></Cell>
   </Row>
   <Row>
    <Cell><Data ss:Type="String">Preferred Language</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(user.language)}</Data></Cell>
   </Row>
   <Row>
    <Cell><Data ss:Type="String">Total Bookstores</Data></Cell>
    <Cell><Data ss:Type="Number">${bookstores.size}</Data></Cell>
   </Row>
   <Row>
    <Cell><Data ss:Type="String">Total Subscriptions</Data></Cell>
    <Cell><Data ss:Type="Number">${subscriptions.size}</Data></Cell>
   </Row>
   <Row>
    <Cell><Data ss:Type="String">Total Scheduled Deliveries</Data></Cell>
    <Cell><Data ss:Type="Number">${scheduled.size}</Data></Cell>
   </Row>
   <Row>
    <Cell><Data ss:Type="String">Total Preorders</Data></Cell>
    <Cell><Data ss:Type="Number">${preorders.size}</Data></Cell>
   </Row>
  </Table>
 </Worksheet>
""")

            // 2. Bookstores Sheet
            xmlBuilder.append(""" <Worksheet ss:Name="Bookstores">
  <Table>
   <Column ss:Width="100"/>
   <Column ss:Width="200"/>
   <Column ss:Width="300"/>
   <Row>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Bookstore ID</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Name</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Website</Data></Cell>
   </Row>
""")
            for (b in bookstores) {
                xmlBuilder.append("""   <Row>
    <Cell><Data ss:Type="Number">${b.id}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(b.name)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(b.website)}</Data></Cell>
   </Row>
""")
            }
            xmlBuilder.append("  </Table>\n </Worksheet>\n")

            // 3. Subscriptions Sheet
            xmlBuilder.append(""" <Worksheet ss:Name="Subscriptions">
  <Table>
   <Column ss:Width="100"/>
   <Column ss:Width="180"/>
   <Column ss:Width="200"/>
   <Column ss:Width="100"/>
   <Column ss:Width="100"/>
   <Column ss:Width="120"/>
   <Column ss:Width="120"/>
   <Column ss:Width="120"/>
   <Column ss:Width="100"/>
   <Column ss:Width="120"/>
   <Row>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Subscription ID</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Bookstore</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Title</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Status</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Price</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Due Date</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Start Date</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Finish Date</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Alert Days</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Frequency</Data></Cell>
   </Row>
""")
            for (s in subscriptions) {
                val sub = s.subscription
                val bookstoreName = s.bookstore?.name ?: "Unknown"
                val dueDateStr = if (sub.dueDate > 0) dateFormat.format(Date(sub.dueDate)) else ""
                val startDateStr = if (sub.startDate > 0) dateFormat.format(Date(sub.startDate)) else ""
                val finishDateStr = if (sub.finishDate > 0) dateFormat.format(Date(sub.finishDate)) else ""
                xmlBuilder.append("""   <Row>
    <Cell><Data ss:Type="Number">${sub.id}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(bookstoreName)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(sub.title)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(sub.status)}</Data></Cell>
    <Cell><Data ss:Type="Number">${sub.price}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(dueDateStr)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(startDateStr)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(finishDateStr)}</Data></Cell>
    <Cell><Data ss:Type="Number">${sub.notificationAlertDays ?: 0}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(sub.frequency)}</Data></Cell>
   </Row>
""")
            }
            xmlBuilder.append("  </Table>\n </Worksheet>\n")

            // 4. Scheduled Subscriptions Sheet
            xmlBuilder.append(""" <Worksheet ss:Name="Scheduled Subscriptions">
  <Table>
   <Column ss:Width="100"/>
   <Column ss:Width="200"/>
   <Column ss:Width="200"/>
   <Column ss:Width="180"/>
   <Column ss:Width="250"/>
   <Column ss:Width="120"/>
   <Column ss:Width="100"/>
   <Column ss:Width="90"/>
   <Row>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Scheduled ID</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Subscription</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Book Title</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Author</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Description</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Due Date</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Status</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Is Skipped</Data></Cell>
   </Row>
""")
            for (sch in scheduled) {
                val sc = sch.scheduled
                val subTitle = sch.subscriptionType?.title ?: "Unknown"
                val dueDateStr = if (sc.dueDate > 0) dateFormat.format(Date(sc.dueDate)) else ""
                xmlBuilder.append("""   <Row>
    <Cell><Data ss:Type="Number">${sc.id}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(subTitle)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(sc.bookTitle)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(sc.bookAuthor)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(sc.description)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(dueDateStr)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(sc.status)}</Data></Cell>
    <Cell><Data ss:Type="String">${if (sc.isSkipped) "Yes" else "No"}</Data></Cell>
   </Row>
""")
            }
            xmlBuilder.append("  </Table>\n </Worksheet>\n")

            // 5. Preorders Sheet
            xmlBuilder.append(""" <Worksheet ss:Name="Preorders">
  <Table>
   <Column ss:Width="100"/>
   <Column ss:Width="180"/>
   <Column ss:Width="200"/>
   <Column ss:Width="180"/>
   <Column ss:Width="250"/>
   <Column ss:Width="100"/>
   <Column ss:Width="120"/>
   <Column ss:Width="120"/>
   <Column ss:Width="100"/>
   <Row>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Preorder ID</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Bookstore</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Book Title</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Author</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Description</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Price</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Sale Start Date</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Sale End Date</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Status</Data></Cell>
   </Row>
""")
            for (p in preorders) {
                val pr = p.preorder
                val storeName = p.bookstore?.name ?: "Unknown"
                val startDateStr = if (pr.rangedSaleDateStart > 0) dateFormat.format(Date(pr.rangedSaleDateStart)) else ""
                val endDateStr = if (pr.rangedSaleDateEnd > 0) dateFormat.format(Date(pr.rangedSaleDateEnd)) else ""
                xmlBuilder.append("""   <Row>
    <Cell><Data ss:Type="Number">${pr.id}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(storeName)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(pr.bookTitle)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(pr.bookAuthor)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(pr.description)}</Data></Cell>
    <Cell><Data ss:Type="Number">${pr.price}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(startDateStr)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(endDateStr)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(pr.status)}</Data></Cell>
   </Row>
""")
            }
            xmlBuilder.append("  </Table>\n </Worksheet>\n")
            xmlBuilder.append("</Workbook>")

            shareExportFile(context, "bookish_library_export.xls", xmlBuilder.toString(), "application/vnd.ms-excel")
        }
    }

    private fun escapeXml(value: String): String {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
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
}
