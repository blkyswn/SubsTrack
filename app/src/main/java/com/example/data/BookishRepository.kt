package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull

data class SubscriptionWithBookstore(
    val subscription: SubscriptionType,
    val bookstore: Bookstore?,
    val skipInfo: SubscriptionSkip? = null
)

data class ScheduledWithDetails(
    val scheduled: ScheduledSubscription,
    val subscriptionType: SubscriptionType?,
    val bookstore: Bookstore?
)

data class PreorderWithBookstore(
    val preorder: Preorder,
    val bookstore: Bookstore?
)

class BookishRepository(private val database: BookishDatabase) {
    private val userDao = database.userDao()
    private val bookstoreDao = database.bookstoreDao()
    private val bookstoreContactDao = database.bookstoreContactDao()
    private val forwardingServiceDao = database.forwardingServiceDao()
    private val forwardingServiceContactDao = database.forwardingServiceContactDao()
    private val userAddressDao = database.userAddressDao()
    private val subscriptionTypeDao = database.subscriptionTypeDao()
    private val subscriptionSkipMethodDao = database.subscriptionSkipMethodDao()
    private val scheduledSubscriptionDao = database.scheduledSubscriptionDao()
    private val preorderDao = database.preorderDao()
    private val subscriptionSkipDao = database.subscriptionSkipDao()

    // Expose User Flow
    val userFlow: Flow<User?> = userDao.getUser()

    suspend fun saveUser(user: User) {
        userDao.insertOrUpdate(user)
    }

    // Expose raw list of bookstores
    val allBookstoresFlow: Flow<List<Bookstore>> = bookstoreDao.getAllBookstores()
    val allBookstoreContactsFlow: Flow<List<BookstoreContact>> = bookstoreContactDao.getAllContacts()

    // Expose raw list of forwarding services
    val allForwardingServicesFlow: Flow<List<ForwardingService>> = forwardingServiceDao.getAllForwardingServices()
    val allForwardingServiceContactsFlow: Flow<List<ForwardingServiceContact>> = forwardingServiceContactDao.getAllContacts()

    // Expose raw list of user addresses
    val allUserAddressesFlow: Flow<List<UserAddress>> = userAddressDao.getAllUserAddresses()

    suspend fun insertUserAddress(address: UserAddress): Long {
        val id = userAddressDao.insert(address)
        if (address.isDefault) {
            userAddressDao.clearOtherDefaults(id.toInt())
        }
        return id
    }

    suspend fun updateUserAddress(address: UserAddress) {
        userAddressDao.update(address)
        if (address.isDefault) {
            userAddressDao.clearOtherDefaults(address.id)
        }
    }

    suspend fun deleteUserAddress(address: UserAddress) {
        userAddressDao.delete(address)
    }

    val allSubscriptionSkipsFlow: Flow<List<SubscriptionSkip>> = subscriptionSkipDao.getAllSubscriptionSkips()
    val allSubscriptionSkipMethodsFlow: Flow<List<SubscriptionSkipMethod>> = subscriptionSkipMethodDao.getAllSubscriptionSkipMethods()

    suspend fun insertBookstore(bookstore: Bookstore): Long {
        return bookstoreDao.insert(bookstore)
    }

    suspend fun updateBookstore(bookstore: Bookstore) {
        bookstoreDao.update(bookstore)
    }

    suspend fun deleteBookstore(bookstore: Bookstore) {
        bookstoreDao.delete(bookstore)
    }

    fun getContactsForBookstore(bookstoreId: Int): Flow<List<BookstoreContact>> {
        return bookstoreContactDao.getContactsForBookstore(bookstoreId)
    }

    suspend fun getContactsForBookstoreDirect(bookstoreId: Int): List<BookstoreContact> {
        return bookstoreContactDao.getContactsForBookstoreDirect(bookstoreId)
    }

    suspend fun saveBookstoreContacts(bookstoreId: Int, contacts: List<BookstoreContact>) {
        bookstoreContactDao.deleteContactsForBookstore(bookstoreId)
        val validContacts = contacts.filter { it.contactValue.isNotBlank() }.map {
            it.copy(id = 0, bookstoreId = bookstoreId)
        }
        if (validContacts.isNotEmpty()) {
            bookstoreContactDao.insertAll(validContacts)
        }
    }

    suspend fun insertForwardingService(service: ForwardingService): Long {
        return forwardingServiceDao.insert(service)
    }

    suspend fun updateForwardingService(service: ForwardingService) {
        forwardingServiceDao.update(service)
    }

    suspend fun deleteForwardingService(service: ForwardingService) {
        forwardingServiceDao.delete(service)
    }

    fun getContactsForForwardingService(forwardingServiceId: Int): Flow<List<ForwardingServiceContact>> {
        return forwardingServiceContactDao.getContactsForForwardingService(forwardingServiceId)
    }

    suspend fun getContactsForForwardingServiceDirect(forwardingServiceId: Int): List<ForwardingServiceContact> {
        return forwardingServiceContactDao.getContactsForForwardingServiceDirect(forwardingServiceId)
    }

    suspend fun saveForwardingServiceContacts(forwardingServiceId: Int, contacts: List<ForwardingServiceContact>) {
        forwardingServiceContactDao.deleteContactsForForwardingService(forwardingServiceId)
        val validContacts = contacts.filter { it.contactValue.isNotBlank() }.map {
            it.copy(id = 0, forwardingServiceId = forwardingServiceId)
        }
        if (validContacts.isNotEmpty()) {
            forwardingServiceContactDao.insertAll(validContacts)
        }
    }

    fun getSkipMethodsForSubscriptionType(subscriptionTypeId: Int): Flow<List<SubscriptionSkipMethod>> {
        return subscriptionSkipMethodDao.getSkipMethodsForSubscriptionType(subscriptionTypeId)
    }

    suspend fun getSkipMethodsForSubscriptionTypeDirect(subscriptionTypeId: Int): List<SubscriptionSkipMethod> {
        return subscriptionSkipMethodDao.getSkipMethodsForSubscriptionTypeDirect(subscriptionTypeId)
    }

    suspend fun saveSubscriptionSkipMethods(subscriptionTypeId: Int, methods: List<SubscriptionSkipMethod>) {
        subscriptionSkipMethodDao.deleteBySubscriptionTypeId(subscriptionTypeId)
        val validMethods = methods.filter { it.skipMethodValue.isNotBlank() }.mapIndexed { index, m ->
            m.copy(
                id = 0,
                subscriptionTypeId = subscriptionTypeId,
                skipMethodOrder = index + 1
            )
        }
        if (validMethods.isNotEmpty()) {
            subscriptionSkipMethodDao.insertAll(validMethods)
        }
    }

    private fun getStartAndEndOfCurrentYear(): Pair<Long, Long> {
        val cal = java.util.Calendar.getInstance()
        val year = cal.get(java.util.Calendar.YEAR)
        cal.clear()
        cal.set(java.util.Calendar.YEAR, year)
        cal.set(java.util.Calendar.MONTH, java.util.Calendar.JANUARY)
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        val startOfYear = cal.timeInMillis

        cal.set(java.util.Calendar.MONTH, java.util.Calendar.DECEMBER)
        cal.set(java.util.Calendar.DAY_OF_MONTH, 31)
        cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
        cal.set(java.util.Calendar.MINUTE, 59)
        cal.set(java.util.Calendar.SECOND, 59)
        cal.set(java.util.Calendar.MILLISECOND, 999)
        val endOfYear = cal.timeInMillis

        return Pair(startOfYear, endOfYear)
    }

    suspend fun syncSubscriptionSkip(subscriptionType: SubscriptionType) {
        val skipType = subscriptionType.skipType ?: "None"
        val existing = subscriptionSkipDao.getSkipForSubscriptionType(subscriptionType.id)

        if (skipType == "None") {
            return
        }

        val skipRecord = when (skipType) {
            "Each calendar year" -> {
                val (startYear, endYear) = getStartAndEndOfCurrentYear()
                SubscriptionSkip(
                    id = existing?.id ?: 0,
                    subscriptionTypeId = subscriptionType.id,
                    subscriptionSkipType = "Each calendar year",
                    numberOfSkips = subscriptionType.numberOfSkips,
                    skipsLeft = subscriptionType.numberOfSkips,
                    skipStartDate = startYear,
                    skipEndDate = endYear
                )
            }
            "Every certain months" -> {
                SubscriptionSkip(
                    id = existing?.id ?: 0,
                    subscriptionTypeId = subscriptionType.id,
                    subscriptionSkipType = "Every certain months",
                    numberOfSkips = subscriptionType.numberOfSkips,
                    skipsLeft = subscriptionType.numberOfSkips,
                    skipStartDate = null,
                    skipEndDate = null
                )
            }
            else -> { // "Unlimited"
                SubscriptionSkip(
                    id = existing?.id ?: 0,
                    subscriptionTypeId = subscriptionType.id,
                    subscriptionSkipType = "Unlimited",
                    numberOfSkips = null,
                    skipsLeft = null,
                    skipStartDate = null,
                    skipEndDate = null
                )
            }
        }
        subscriptionSkipDao.insert(skipRecord)
    }

    // Expose combined Subscription with Bookstore and Skip Info
    val subscriptionsWithBookstoreFlow: Flow<List<SubscriptionWithBookstore>> = combine(
        subscriptionTypeDao.getAllSubscriptionTypes(),
        bookstoreDao.getAllBookstores(),
        subscriptionSkipDao.getAllSubscriptionSkips()
    ) { subs, stores, skips ->
        subs.map { sub ->
            val store = stores.find { it.id == sub.bookstoreId }
            val skip = skips.find { it.subscriptionTypeId == sub.id }
            SubscriptionWithBookstore(sub, store, skip)
        }
    }

    suspend fun insertSubscriptionType(subscriptionType: SubscriptionType): Long {
        val id = subscriptionTypeDao.insert(subscriptionType)
        val updatedSub = subscriptionType.copy(id = id.toInt())
        syncSubscriptionSkip(updatedSub)
        return id
    }

    suspend fun updateSubscriptionType(subscriptionType: SubscriptionType) {
        subscriptionTypeDao.update(subscriptionType)
        syncSubscriptionSkip(subscriptionType)
    }

    suspend fun deleteSubscriptionType(subscriptionType: SubscriptionType) {
        scheduledSubscriptionDao.deleteScheduledSubsForType(subscriptionType.id)
        subscriptionTypeDao.delete(subscriptionType)
    }

    suspend fun getSubscriptionTypeById(id: Int): SubscriptionType? {
        return subscriptionTypeDao.getSubscriptionTypeById(id)
    }

    // Expose combined Scheduled Subscription with Details
    val scheduledWithDetailsFlow: Flow<List<ScheduledWithDetails>> = combine(
        scheduledSubscriptionDao.getAllScheduledSubscriptions(),
        subscriptionTypeDao.getAllSubscriptionTypes(),
        bookstoreDao.getAllBookstores()
    ) { scheduledList, subs, stores ->
        scheduledList.map { sched ->
            val sub = subs.find { it.id == sched.subscriptionTypeId }
            val store = sub?.let { s -> stores.find { it.id == s.bookstoreId } }
            ScheduledWithDetails(sched, sub, store)
        }
    }

    suspend fun getScheduledSubscriptionById(id: Int): ScheduledSubscription? {
        return scheduledSubscriptionDao.getScheduledSubscriptionById(id)
    }

    suspend fun processSkipForScheduledSub(
        scheduled: ScheduledSubscription,
        userDateFormatPattern: String
    ): String? {
        val subType = subscriptionTypeDao.getSubscriptionTypeById(scheduled.subscriptionTypeId) ?: return null
        val skipType = subType.skipType ?: "None"
        if (skipType == "None") return null

        val renewalDate = scheduled.dueDate
        val dateFormat = java.text.SimpleDateFormat(userDateFormatPattern, java.util.Locale.getDefault())

        fun formatRenewDate(endDate: Long): String {
            val cal = java.util.Calendar.getInstance()
            cal.timeInMillis = endDate
            cal.add(java.util.Calendar.DAY_OF_MONTH, 1)
            return dateFormat.format(cal.time)
        }

        val skips = subscriptionSkipDao.getSkipsForSubscriptionType(subType.id)

        when (skipType) {
            "Unlimited" -> {
                val existing = skips.firstOrNull { it.subscriptionSkipType == "Unlimited" } ?: skips.firstOrNull()
                if (existing != null) {
                    val newCount = (existing.numberOfSkips ?: 0) + 1
                    subscriptionSkipDao.update(
                        existing.copy(
                            subscriptionSkipType = "Unlimited",
                            numberOfSkips = newCount,
                            skipsLeft = null,
                            skipStartDate = null,
                            skipEndDate = null
                        )
                    )
                } else {
                    subscriptionSkipDao.insert(
                        SubscriptionSkip(
                            subscriptionTypeId = subType.id,
                            subscriptionSkipType = "Unlimited",
                            numberOfSkips = 1,
                            skipsLeft = null,
                            skipStartDate = null,
                            skipEndDate = null
                        )
                    )
                }
                return null
            }
            "Each calendar year" -> {
                val matching = skips.firstOrNull { skip ->
                    skip.skipStartDate != null && skip.skipEndDate != null &&
                            renewalDate >= skip.skipStartDate && renewalDate <= skip.skipEndDate
                }

                if (matching != null) {
                    val currentLeft = matching.skipsLeft
                    if (currentLeft != null && currentLeft <= 0) {
                        val renewDateStr = formatRenewDate(matching.skipEndDate!!)
                        return "No skips left. Skips renew in $renewDateStr."
                    } else {
                        val initialSkips = matching.skipsLeft ?: (matching.numberOfSkips ?: subType.numberOfSkips ?: 0)
                        if (initialSkips <= 0) {
                            val renewDateStr = formatRenewDate(matching.skipEndDate!!)
                            return "No skips left. Skips renew in $renewDateStr."
                        }
                        val newSkipsLeft = initialSkips - 1
                        subscriptionSkipDao.update(matching.copy(skipsLeft = newSkipsLeft))
                        return null
                    }
                } else {
                    val cal = java.util.Calendar.getInstance()
                    cal.timeInMillis = renewalDate
                    val year = cal.get(java.util.Calendar.YEAR)

                    cal.set(year, java.util.Calendar.JANUARY, 1, 0, 0, 0)
                    cal.set(java.util.Calendar.MILLISECOND, 0)
                    val startOfYear = cal.timeInMillis

                    cal.set(year, java.util.Calendar.DECEMBER, 31, 23, 59, 59)
                    cal.set(java.util.Calendar.MILLISECOND, 999)
                    val endOfYear = cal.timeInMillis

                    val uninitialized = skips.firstOrNull { it.skipStartDate == null || it.skipEndDate == null }
                    val currentRegister = skips.firstOrNull {
                        it.skipStartDate != null && it.skipEndDate != null &&
                                endOfYear >= it.skipStartDate && endOfYear <= it.skipEndDate
                    }
                    val maxSkips = subType.numberOfSkips ?: 0
                    if (maxSkips <= 0) {
                        val renewDateStr = formatRenewDate(endOfYear)
                        return "No skips left. Skips renew in $renewDateStr."
                    } else if (currentRegister != null) {
                        val currentNum = currentRegister.numberOfSkips ?: maxSkips
                        val currentLeft = currentRegister.skipsLeft ?: currentNum
                        val newNum = currentNum + maxSkips
                        val newLeft = (currentLeft + maxSkips - 1).coerceAtLeast(0)
                        val newStart = minOf(startOfYear, currentRegister.skipStartDate!!)
                        val newEnd = maxOf(endOfYear, currentRegister.skipEndDate!!)
                        subscriptionSkipDao.update(
                            currentRegister.copy(
                                numberOfSkips = newNum,
                                skipsLeft = newLeft,
                                skipStartDate = newStart,
                                skipEndDate = newEnd
                            )
                        )
                        return null
                    } else {
                        if (uninitialized != null) {
                            subscriptionSkipDao.update(
                                uninitialized.copy(
                                    subscriptionSkipType = "Each calendar year",
                                    numberOfSkips = maxSkips,
                                    skipsLeft = maxSkips - 1,
                                    skipStartDate = startOfYear,
                                    skipEndDate = endOfYear
                                )
                            )
                        } else {
                            subscriptionSkipDao.insert(
                                SubscriptionSkip(
                                    subscriptionTypeId = subType.id,
                                    subscriptionSkipType = "Each calendar year",
                                    numberOfSkips = maxSkips,
                                    skipsLeft = maxSkips - 1,
                                    skipStartDate = startOfYear,
                                    skipEndDate = endOfYear
                                )
                            )
                        }
                        return null
                    }
                }
            }
            "Every certain months" -> {
                val startDate = renewalDate
                val cal = java.util.Calendar.getInstance()
                cal.timeInMillis = renewalDate
                cal.add(java.util.Calendar.MONTH, subType.numberOfMonths ?: 1)
                cal.add(java.util.Calendar.DAY_OF_MONTH, -1)
                val endDate = cal.timeInMillis

                val matchingDueDate = skips.firstOrNull { skip ->
                    skip.skipStartDate != null && skip.skipEndDate != null &&
                            renewalDate >= skip.skipStartDate && renewalDate <= skip.skipEndDate
                }

                if (matchingDueDate != null) {
                    val currentLeft = matchingDueDate.skipsLeft ?: (matchingDueDate.numberOfSkips ?: subType.numberOfSkips ?: 0)
                    if (currentLeft <= 0) {
                        val renewDateStr = formatRenewDate(matchingDueDate.skipEndDate!!)
                        return "No skips left. Skips renew in $renewDateStr."
                    }
                    val newSkipsLeft = currentLeft - 1
                    subscriptionSkipDao.update(matchingDueDate.copy(skipsLeft = newSkipsLeft))
                    return null
                } else {
                    val matchingEndDate = skips.firstOrNull { skip ->
                        skip.skipStartDate != null && skip.skipEndDate != null &&
                                endDate >= skip.skipStartDate && endDate <= skip.skipEndDate
                    }

                    if (matchingEndDate != null) {
                        val currentLeft = matchingEndDate.skipsLeft ?: (matchingEndDate.numberOfSkips ?: subType.numberOfSkips ?: 0)
                        if (currentLeft <= 0) {
                            val renewDateStr = formatRenewDate(matchingEndDate.skipEndDate!!)
                            return "No skips left. Skips renew in $renewDateStr."
                        }
                        val newSkipsLeft = currentLeft - 1
                        subscriptionSkipDao.update(
                            matchingEndDate.copy(
                                skipStartDate = startDate,
                                skipEndDate = endDate,
                                skipsLeft = newSkipsLeft
                            )
                        )
                        return null
                    } else {
                        val maxSkips = subType.numberOfSkips ?: 0
                        if (maxSkips <= 0) {
                            val renewDateStr = formatRenewDate(endDate)
                            return "No skips left. Skips renew in $renewDateStr."
                        }
                        val uninitialized = skips.firstOrNull { it.skipStartDate == null || it.skipEndDate == null }
                        if (uninitialized != null) {
                            subscriptionSkipDao.update(
                                uninitialized.copy(
                                    subscriptionSkipType = "Every certain months",
                                    numberOfSkips = maxSkips,
                                    skipsLeft = maxSkips - 1,
                                    skipStartDate = startDate,
                                    skipEndDate = endDate
                                )
                            )
                        } else {
                            subscriptionSkipDao.insert(
                                SubscriptionSkip(
                                    subscriptionTypeId = subType.id,
                                    subscriptionSkipType = "Every certain months",
                                    numberOfSkips = maxSkips,
                                    skipsLeft = maxSkips - 1,
                                    skipStartDate = startDate,
                                    skipEndDate = endDate
                                )
                            )
                        }
                        return null
                    }
                }
            }
        }
        return null
    }

    suspend fun processUnskipForScheduledSub(scheduled: ScheduledSubscription) {
        val subType = subscriptionTypeDao.getSubscriptionTypeById(scheduled.subscriptionTypeId) ?: return
        val skipType = subType.skipType ?: "None"
        if (skipType == "None") return

        val renewalDate = scheduled.dueDate
        val skips = subscriptionSkipDao.getSkipsForSubscriptionType(subType.id)

        when (skipType) {
            "Unlimited" -> {
                val existing = skips.firstOrNull { it.subscriptionSkipType == "Unlimited" } ?: skips.firstOrNull()
                if (existing != null) {
                    val currentCount = existing.numberOfSkips ?: 0
                    if (currentCount > 0) {
                        subscriptionSkipDao.update(existing.copy(numberOfSkips = currentCount - 1))
                    }
                }
            }
            "Each calendar year", "Every certain months" -> {
                val matching = skips.firstOrNull { skip ->
                    skip.skipStartDate != null && skip.skipEndDate != null &&
                            renewalDate >= skip.skipStartDate && renewalDate <= skip.skipEndDate
                }
                if (matching != null) {
                    val maxSkips = matching.numberOfSkips ?: subType.numberOfSkips ?: 0
                    val currentLeft = matching.skipsLeft ?: 0
                    if (currentLeft < maxSkips) {
                        subscriptionSkipDao.update(matching.copy(skipsLeft = currentLeft + 1))
                    }
                }
            }
        }
    }

    suspend fun getScheduledSubscriptionsForType(subTypeId: Int): List<ScheduledSubscription> {
        return scheduledSubscriptionDao.getScheduledSubscriptionsForType(subTypeId)
    }

    suspend fun insertScheduledSubscription(scheduled: ScheduledSubscription): Long {
        return scheduledSubscriptionDao.insert(scheduled)
    }

    suspend fun updateScheduledSubscription(scheduled: ScheduledSubscription) {
        scheduledSubscriptionDao.update(scheduled)
    }

    suspend fun deleteScheduledSubscription(scheduled: ScheduledSubscription) {
        scheduledSubscriptionDao.delete(scheduled)
    }

    suspend fun deleteScheduledSubsAfterDate(subTypeId: Int, date: Long) {
        scheduledSubscriptionDao.deleteScheduledSubsAfterDate(subTypeId, date)
    }

    // Expose combined Preorders with Bookstore
    val preordersWithBookstoreFlow: Flow<List<PreorderWithBookstore>> = combine(
        preorderDao.getAllPreorders(),
        bookstoreDao.getAllBookstores()
    ) { preorders, stores ->
        preorders.map { preorder ->
            PreorderWithBookstore(preorder, stores.find { it.id == preorder.bookstoreId })
        }
    }

    suspend fun insertPreorder(preorder: Preorder): Long {
        return preorderDao.insert(preorder)
    }

    suspend fun updatePreorder(preorder: Preorder) {
        preorderDao.update(preorder)
    }

    suspend fun deletePreorder(preorder: Preorder) {
        preorderDao.delete(preorder)
    }

    // Prepopulate database with rich realistic sample data if empty
    suspend fun checkAndPrepopulate() {
        val bookstores = bookstoreDao.getAllBookstores().first()
        if (bookstores.isEmpty()) {
            // 1. Create Default User
            val defaultUser = User(
                id = 1,
                username = "Eleanor Vance",
                profilePic = "avatar_classic",
                currency = "$",
                language = "English",
                defaultScheduledSubCount = 6,
                themeMode = "system",
                country = "United States"
            )
            userDao.insertOrUpdate(defaultUser)

            // 2. Insert Bookstore Samples
            val store1Id = bookstoreDao.insert(Bookstore(name = "FairyLoot", website = "https://fairyloot.com", profilePic = "ic_launcher_foreground")).toInt()
            val store2Id = bookstoreDao.insert(Bookstore(name = "Illumicrate", website = "https://www.illumicrate.com", profilePic = "ic_launcher_foreground")).toInt()
            val store3Id = bookstoreDao.insert(Bookstore(name = "Barnes & Noble", website = "https://www.barnesandnoble.com", profilePic = "ic_launcher_foreground")).toInt()
            val store4Id = bookstoreDao.insert(Bookstore(name = "Waterstones", website = "https://www.waterstones.com", profilePic = "ic_launcher_foreground")).toInt()

            bookstoreContactDao.insertAll(listOf(
                BookstoreContact(bookstoreId = store1Id, contactType = "Website", contactValue = "https://fairyloot.com"),
                BookstoreContact(bookstoreId = store1Id, contactType = "Email", contactValue = "support@fairyloot.com"),
                BookstoreContact(bookstoreId = store1Id, contactType = "Phone", contactValue = "+44 20 7946 0912"),
                BookstoreContact(bookstoreId = store2Id, contactType = "Website", contactValue = "https://www.illumicrate.com"),
                BookstoreContact(bookstoreId = store2Id, contactType = "Email", contactValue = "support@illumicrate.com"),
                BookstoreContact(bookstoreId = store3Id, contactType = "Website", contactValue = "https://www.barnesandnoble.com"),
                BookstoreContact(bookstoreId = store3Id, contactType = "Phone", contactValue = "1-800-843-2665"),
                BookstoreContact(bookstoreId = store4Id, contactType = "Website", contactValue = "https://www.waterstones.com"),
                BookstoreContact(bookstoreId = store4Id, contactType = "Email", contactValue = "enquiries@waterstones.com")
            ))

            // Time constant: Let's use current time as reference
            val now = System.currentTimeMillis()
            val dayMs = 24 * 60 * 60 * 1000L

            // 3. Insert Subscription Types
            val sub1Obj = SubscriptionType(
                bookstoreId = store1Id,
                title = "FairyLoot YA Monthly",
                status = "Active",
                price = 32.90,
                dueDate = now + 4 * dayMs,
                startDate = now - 60 * dayMs,
                finishDate = now + 300 * dayMs,
                notificationAlertDays = 3,
                frequency = "Monthly",
                skipType = "Each calendar year",
                numberOfSkips = 3,
                numberOfMonths = null,
                picturePath = "https://images.unsplash.com/photo-1544947950-fa07a98d237f?auto=format&fit=crop&w=300&q=80"
            )
            val sub1Id = insertSubscriptionType(sub1Obj).toInt()

            val sub2Obj = SubscriptionType(
                bookstoreId = store2Id,
                title = "Illumicrate Quarterly Special",
                status = "Active",
                price = 45.00,
                dueDate = now + 25 * dayMs,
                startDate = now - 180 * dayMs,
                finishDate = now + 365 * dayMs,
                notificationAlertDays = 5,
                frequency = "Quarterly",
                skipType = "Every certain months",
                numberOfSkips = 1,
                numberOfMonths = 6,
                picturePath = "https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&w=300&q=80"
            )
            val sub2Id = insertSubscriptionType(sub2Obj).toInt()

            val sub3Obj = SubscriptionType(
                bookstoreId = store3Id,
                title = "B&N Premium Membership",
                status = "Active",
                price = 39.99,
                dueDate = now + 120 * dayMs,
                startDate = now - 12 * dayMs,
                finishDate = now + 353 * dayMs,
                notificationAlertDays = 7,
                frequency = "Yearly",
                skipType = "Unlimited",
                numberOfSkips = null,
                numberOfMonths = null,
                picturePath = "https://images.unsplash.com/photo-1543002588-bfa74002ed7e?auto=format&fit=crop&w=300&q=80"
            )
            val sub3Id = insertSubscriptionType(sub3Obj).toInt()

            // 3b. Insert Sample Skip Methods for Subscriptions
            subscriptionSkipMethodDao.insertAll(listOf(
                SubscriptionSkipMethod(
                    subscriptionTypeId = sub1Id,
                    skipMethodOrder = 1,
                    skipMethodType = "Website",
                    skipMethodValue = "https://fairyloot.com/account/skip",
                    skipMethodText = "",
                    consecutiveSkips = 0
                ),
                SubscriptionSkipMethod(
                    subscriptionTypeId = sub1Id,
                    skipMethodOrder = 2,
                    skipMethodType = "Email",
                    skipMethodValue = "support@fairyloot.com",
                    skipMethodText = "Hi there,\nI would like to skip my upcoming FairyLoot YA Monthly subscription delivery.\nThank you!\nReader",
                    consecutiveSkips = 2
                ),
                SubscriptionSkipMethod(
                    subscriptionTypeId = sub2Id,
                    skipMethodOrder = 1,
                    skipMethodType = "Email",
                    skipMethodValue = "support@illumicrate.com",
                    skipMethodText = "Hello,\nPlease skip my upcoming quarterly box delivery.\nThanks,\nReader",
                    consecutiveSkips = 0
                )
            ))

            // 4. Insert Scheduled Subscriptions (issues / monthly picks)
            scheduledSubscriptionDao.insert(
                ScheduledSubscription(
                    subscriptionTypeId = sub1Id,
                    bookTitle = "The Shadow of the Gods",
                    bookAuthor = "John Gwynne",
                    description = "Special premium edition with custom spray edges and unique illustrations.",
                    dueDate = now + 4 * dayMs,
                    status = "Upcoming",
                    isSkipped = false
                )
            )

            scheduledSubscriptionDao.insert(
                ScheduledSubscription(
                    subscriptionTypeId = sub1Id,
                    bookTitle = "The Hunger Games (Deluxe)",
                    bookAuthor = "Suzanne Collins",
                    description = "May Monthly book pick. Beautiful foil hardback detailing.",
                    dueDate = now - 26 * dayMs,
                    status = "Received",
                    isSkipped = false
                )
            )

            scheduledSubscriptionDao.insert(
                ScheduledSubscription(
                    subscriptionTypeId = sub2Id,
                    bookTitle = "Piranesi (Illustrated)",
                    bookAuthor = "Susanna Clarke",
                    description = "Quarterly custom edition of the contemporary classic.",
                    dueDate = now + 25 * dayMs,
                    status = "Upcoming",
                    isSkipped = false
                )
            )

            scheduledSubscriptionDao.insert(
                ScheduledSubscription(
                    subscriptionTypeId = sub1Id,
                    bookTitle = "A Court of Thorns and Roses",
                    bookAuthor = "Sarah J. Maas",
                    description = "Skipped June pick due to duplicate version ownership.",
                    dueDate = now + 34 * dayMs,
                    status = "Upcoming",
                    isSkipped = true
                )
            )

            // 5. Insert Preorders
            val targetReleaseDay = now + 5 * dayMs
            preorderDao.insert(
                Preorder(
                    bookstoreId = store3Id,
                    picturePath = "https://images.unsplash.com/photo-1544947950-fa07a98d237f?auto=format&fit=crop&w=300&q=80",
                    bookTitle = "Wind and Truth",
                    bookAuthor = "Brandon Sanderson",
                    description = "Stormlight Archive Book 5. Signed first edition hardback.",
                    price = 34.99,
                    rangedSaleDateStart = targetReleaseDay,
                    rangedSaleDateEnd = targetReleaseDay + 2 * dayMs,
                    status = "Preordered"
                )
            )

            preorderDao.insert(
                Preorder(
                    bookstoreId = store4Id,
                    picturePath = "https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&w=300&q=80",
                    bookTitle = "Onyx Storm",
                    bookAuthor = "Rebecca Yarros",
                    description = "Deluxe dragon rider fantasy novel preorder.",
                    price = 32.00,
                    rangedSaleDateStart = targetReleaseDay,
                    rangedSaleDateEnd = targetReleaseDay + 2 * dayMs,
                    status = "Preordered"
                )
            )

            preorderDao.insert(
                Preorder(
                    bookstoreId = store3Id,
                    picturePath = "https://images.unsplash.com/photo-1543002588-bfa74002ed7e?auto=format&fit=crop&w=300&q=80",
                    bookTitle = "Iron Flame (Deluxe Edition)",
                    bookAuthor = "Rebecca Yarros",
                    description = "Special stenciled edges preorder.",
                    price = 30.00,
                    rangedSaleDateStart = now - 5 * dayMs,
                    rangedSaleDateEnd = now - 3 * dayMs,
                    status = "Shipped"
                )
            )

            preorderDao.insert(
                Preorder(
                    bookstoreId = store4Id,
                    picturePath = "https://images.unsplash.com/photo-1532012197267-da84d127e765?auto=format&fit=crop&w=300&q=80",
                    bookTitle = "The Winds of Winter",
                    bookAuthor = "George R.R. Martin",
                    description = "Expected release from local bookstore pre-orders.",
                    price = 28.50,
                    rangedSaleDateStart = now + 120 * dayMs,
                    rangedSaleDateEnd = now + 125 * dayMs,
                    status = "Upcoming"
                )
            )
        }

        val forwardingServices = forwardingServiceDao.getAllForwardingServices().firstOrNull() ?: emptyList()
        if (forwardingServices.isEmpty()) {
            val fs1Id = forwardingServiceDao.insert(ForwardingService(name = "Stackry", website = "https://www.stackry.com", profilePic = "ic_launcher_foreground")).toInt()
            val fs2Id = forwardingServiceDao.insert(ForwardingService(name = "Buyee", website = "https://buyee.jp", profilePic = "ic_launcher_foreground")).toInt()
            val fs3Id = forwardingServiceDao.insert(ForwardingService(name = "Forward2me", website = "https://www.forward2me.com", profilePic = "ic_launcher_foreground")).toInt()

            forwardingServiceContactDao.insertAll(listOf(
                ForwardingServiceContact(forwardingServiceId = fs1Id, contactType = "Website", contactValue = "https://www.stackry.com"),
                ForwardingServiceContact(forwardingServiceId = fs1Id, contactType = "Email", contactValue = "support@stackry.com"),
                ForwardingServiceContact(forwardingServiceId = fs2Id, contactType = "Website", contactValue = "https://buyee.jp"),
                ForwardingServiceContact(forwardingServiceId = fs2Id, contactType = "Email", contactValue = "support@buyee.jp"),
                ForwardingServiceContact(forwardingServiceId = fs3Id, contactType = "Website", contactValue = "https://www.forward2me.com"),
                ForwardingServiceContact(forwardingServiceId = fs3Id, contactType = "Email", contactValue = "info@forward2me.com")
            ))
        }
    }

    suspend fun recalculateSubscriptionSkips(userDateFormatPattern: String) {
        subscriptionSkipDao.deleteAllSubscriptionSkips()
        val allSubTypes = subscriptionTypeDao.getAllSubscriptionTypesList()
        for (subType in allSubTypes) {
            syncSubscriptionSkip(subType)
        }
        val skippedSubs = scheduledSubscriptionDao.getSkippedScheduledSubscriptions()
        for (scheduled in skippedSubs) {
            processSkipForScheduledSub(scheduled, userDateFormatPattern)
        }
    }
}
