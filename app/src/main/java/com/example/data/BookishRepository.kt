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
    private val shippingCompanyDao = database.shippingCompanyDao()
    private val shippingCompanyContactDao = database.shippingCompanyContactDao()
    private val userAddressDao = database.userAddressDao()
    private val subscriptionTypeDao = database.subscriptionTypeDao()
    private val subscriptionSkipMethodDao = database.subscriptionSkipMethodDao()
    private val scheduledSubscriptionDao = database.scheduledSubscriptionDao()
    private val preorderDao = database.preorderDao()
    private val subscriptionSkipDao = database.subscriptionSkipDao()
    private val packageDao = database.packageDao()

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

    // Expose raw list of shipping companies
    val allShippingCompaniesFlow: Flow<List<ShippingCompany>> = shippingCompanyDao.getAllShippingCompanies()
    val allShippingCompanyContactsFlow: Flow<List<ShippingCompanyContact>> = shippingCompanyContactDao.getAllContacts()

    // Expose raw list of user addresses
    val allUserAddressesFlow: Flow<List<UserAddress>> = userAddressDao.getAllUserAddresses()

    // Expose raw list of packages
    val allPackagesFlow: Flow<List<PackageItem>> = packageDao.getAllPackages()

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

    suspend fun insertShippingCompany(shippingCompany: ShippingCompany): Long {
        return shippingCompanyDao.insert(shippingCompany)
    }

    suspend fun updateShippingCompany(shippingCompany: ShippingCompany) {
        shippingCompanyDao.update(shippingCompany)
    }

    suspend fun deleteShippingCompany(shippingCompany: ShippingCompany) {
        shippingCompanyDao.delete(shippingCompany)
    }

    fun getContactsForShippingCompany(shippingCompanyId: Int): Flow<List<ShippingCompanyContact>> {
        return shippingCompanyContactDao.getContactsForShippingCompany(shippingCompanyId)
    }

    suspend fun getContactsForShippingCompanyDirect(shippingCompanyId: Int): List<ShippingCompanyContact> {
        return shippingCompanyContactDao.getContactsForShippingCompanyDirect(shippingCompanyId)
    }

    suspend fun saveShippingCompanyContacts(shippingCompanyId: Int, contacts: List<ShippingCompanyContact>) {
        shippingCompanyContactDao.deleteContactsForShippingCompany(shippingCompanyId)
        val validContacts = contacts.filter { it.contactValue.isNotBlank() }.map {
            it.copy(id = 0, shippingCompanyId = shippingCompanyId)
        }
        if (validContacts.isNotEmpty()) {
            shippingCompanyContactDao.insertAll(validContacts)
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

    suspend fun getPreorderById(id: Int): Preorder? {
        return preorderDao.getPreorderById(id)
    }

    suspend fun getUserAddressById(id: Int): UserAddress? {
        return userAddressDao.getAddressById(id)
    }

    suspend fun getForwardingServiceById(id: Int): ForwardingService? {
        return forwardingServiceDao.getForwardingServiceById(id)
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
        val id = scheduledSubscriptionDao.insert(scheduled)
        val existing = packageDao.getPackageDirect("scheduled_subs", id.toInt())
        val statusLower = scheduled.status.lowercase().trim()
        val today = System.currentTimeMillis()
        if (existing == null) {
            packageDao.insert(
                PackageItem(
                    originTable = "scheduled_subs",
                    originId = id.toInt(),
                    purchaseDate = if (statusLower in listOf("renewed", "paid", "forwarded", "in suite", "shipped", "received")) today else null,
                    storeShippingDate = if (statusLower == "shipped" || statusLower == "forwarded") today else null,
                    forwarderReceivedDate = if (statusLower == "in suite") today else null,
                    forwarderShippedDate = if (statusLower == "shipped") today else null,
                    receivedDate = if (statusLower == "received") today else null
                )
            )
        }
        return id
    }

    suspend fun updateScheduledSubscription(scheduled: ScheduledSubscription) {
        val oldScheduled = scheduledSubscriptionDao.getScheduledSubscriptionById(scheduled.id)
        scheduledSubscriptionDao.update(scheduled)
        if (oldScheduled != null && !oldScheduled.status.equals(scheduled.status, ignoreCase = true)) {
            syncPackageStatusChange("scheduled_subs", scheduled.id, oldScheduled.status, scheduled.status)
        }
    }

    suspend fun deleteScheduledSubscription(scheduled: ScheduledSubscription) {
        packageDao.deleteByOrigin("scheduled_subs", scheduled.id)
        scheduledSubscriptionDao.delete(scheduled)
    }

    suspend fun deleteScheduledSubsAfterDate(subTypeId: Int, date: Long) {
        val subsToDelete = scheduledSubscriptionDao.getScheduledSubscriptionsForType(subTypeId).filter { it.dueDate > date }
        subsToDelete.forEach {
            packageDao.deleteByOrigin("scheduled_subs", it.id)
        }
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
        val id = preorderDao.insert(preorder)
        val existing = packageDao.getPackageDirect("preorders", id.toInt())
        val statusLower = preorder.status.lowercase().trim()
        val today = System.currentTimeMillis()
        if (existing == null) {
            packageDao.insert(
                PackageItem(
                    originTable = "preorders",
                    originId = id.toInt(),
                    purchaseDate = if (statusLower in listOf("preordered", "forwarded", "in suite", "shipped", "received")) today else null,
                    storeShippingDate = if (statusLower == "shipped" || statusLower == "forwarded") today else null,
                    forwarderReceivedDate = if (statusLower == "in suite") today else null,
                    forwarderShippedDate = if (statusLower == "shipped") today else null,
                    receivedDate = if (statusLower == "received") today else null
                )
            )
        }
        return id
    }

    suspend fun updatePreorder(preorder: Preorder) {
        val oldPreorder = preorderDao.getPreorderById(preorder.id)
        val resolvedPreorder = if (preorder.shippingAddressId == null && oldPreorder?.shippingAddressId != null) {
            preorder.copy(shippingAddressId = oldPreorder.shippingAddressId)
        } else {
            preorder
        }
        preorderDao.update(resolvedPreorder)
        if (oldPreorder != null && !oldPreorder.status.equals(resolvedPreorder.status, ignoreCase = true)) {
            syncPackageStatusChange("preorders", resolvedPreorder.id, oldPreorder.status, resolvedPreorder.status)
        }
    }

    suspend fun syncPackageStatusChange(
        originTable: String,
        originId: Int,
        oldStatusStr: String,
        newStatusStr: String
    ) {
        val old = oldStatusStr.lowercase().trim()
        val next = newStatusStr.lowercase().trim()
        if (old == next) return

        val pkg = getOrCreatePackage(originTable, originId)
        val today = System.currentTimeMillis()

        var purchaseDate = pkg.purchaseDate
        var storeShippingDate = pkg.storeShippingDate
        var forwarderReceivedDate = pkg.forwarderReceivedDate
        var forwarderShippedDate = pkg.forwarderShippedDate
        var receivedDate = pkg.receivedDate

        // Purchase date:
        // When preorders change status from upcoming/released to preordered (or higher)
        if (originTable == "preorders") {
            if ((old == "upcoming" || old == "released") && (next == "preordered" || next == "forwarded" || next == "in suite" || next == "shipped" || next == "received")) {
                purchaseDate = today
            }
        }

        // When scheduled subs change status from upcoming (or skipped) to renewed (or higher)
        if (originTable == "scheduled_subs") {
            if ((old == "upcoming" || old == "skipped") && (next == "renewed" || next == "paid" || next == "forwarded" || next == "in suite" || next == "shipped" || next == "received")) {
                purchaseDate = today
            }
        }

        val isOldPreorderedOrRenewed = old == "preordered" || old == "renewed" || old == "paid"
        val isNextPreorderedOrRenewed = next == "preordered" || next == "renewed" || next == "paid" || next == "upcoming" || next == "released" || next == "skipped"

        // 1. when status changes from preordered (in preorders)/renewed (scheduled subs) to shipped/forwarded, update store shipped date in packages table to today
        if (isOldPreorderedOrRenewed && (next == "shipped" || next == "forwarded")) {
            storeShippingDate = today
        }

        // 2. when status changes forwarded to in suite, update forwarder received date in packages table to today
        if (old == "forwarded" && next == "in suite") {
            forwarderReceivedDate = today
        }

        // 3. when status changes in suite to shipped, update forwarder sent date in packages table to today
        if (old == "in suite" && next == "shipped") {
            forwarderShippedDate = today
        }

        // 4. when status changes shipped to received, update received date in packages table to today
        if (old == "shipped" && next == "received") {
            receivedDate = today
        }

        // 5. when status is reverted, corresponding date is cleared:
        // 5a. Reverting from received to shipped (or earlier): clear receivedDate
        if (old == "received" && next != "received") {
            receivedDate = null
        }

        // 5b. Reverting from shipped:
        if (old == "shipped") {
            if (next == "in suite") {
                forwarderShippedDate = null
                receivedDate = null
            } else if (next == "forwarded") {
                forwarderReceivedDate = null
                forwarderShippedDate = null
                receivedDate = null
            } else if (isNextPreorderedOrRenewed) {
                storeShippingDate = null
                forwarderReceivedDate = null
                forwarderShippedDate = null
                receivedDate = null
            }
        }

        // 5c. Reverting from in suite:
        if (old == "in suite") {
            if (next == "forwarded") {
                forwarderReceivedDate = null
                forwarderShippedDate = null
                receivedDate = null
            } else if (isNextPreorderedOrRenewed) {
                storeShippingDate = null
                forwarderReceivedDate = null
                forwarderShippedDate = null
                receivedDate = null
            }
        }

        // 5d. Reverting from forwarded:
        if (old == "forwarded" && isNextPreorderedOrRenewed) {
            storeShippingDate = null
            forwarderReceivedDate = null
            forwarderShippedDate = null
            receivedDate = null
        }

        // Catch-all reversion to preordered/renewed/upcoming from any higher state:
        if (isNextPreorderedOrRenewed && (old == "forwarded" || old == "in suite" || old == "shipped" || old == "received")) {
            storeShippingDate = null
            forwarderReceivedDate = null
            forwarderShippedDate = null
            receivedDate = null
        }

        // 5e. Reverting back to upcoming/released/skipped clears purchase date:
        if (originTable == "preorders" && (next == "upcoming" || next == "released")) {
            purchaseDate = null
            storeShippingDate = null
            forwarderReceivedDate = null
            forwarderShippedDate = null
            receivedDate = null
        }

        if (originTable == "scheduled_subs" && (next == "upcoming" || next == "skipped")) {
            purchaseDate = null
            storeShippingDate = null
            forwarderReceivedDate = null
            forwarderShippedDate = null
            receivedDate = null
        }

        val updated = pkg.copy(
            purchaseDate = purchaseDate,
            storeShippingDate = storeShippingDate,
            forwarderReceivedDate = forwarderReceivedDate,
            forwarderShippedDate = forwarderShippedDate,
            receivedDate = receivedDate
        )
        if (updated != pkg) {
            packageDao.update(updated)
        }
    }

    suspend fun deletePreorder(preorder: Preorder) {
        packageDao.deleteByOrigin("preorders", preorder.id)
        preorderDao.delete(preorder)
    }

    // Package Table APIs
    fun getPackageFlow(originTable: String, originId: Int): Flow<PackageItem?> {
        return packageDao.getPackage(originTable, originId)
    }

    suspend fun getPackageDirect(originTable: String, originId: Int): PackageItem? {
        return packageDao.getPackageDirect(originTable, originId)
    }

    suspend fun getOrCreatePackage(originTable: String, originId: Int): PackageItem {
        val existing = packageDao.getPackageDirect(originTable, originId)
        if (existing != null) {
            return existing
        }
        val newPkg = PackageItem(
            originTable = originTable,
            originId = originId
        )
        val id = packageDao.insert(newPkg)
        return newPkg.copy(id = id.toInt())
    }

    suspend fun updatePackage(packageItem: PackageItem) {
        packageDao.update(packageItem)
    }

    suspend fun insertPackage(packageItem: PackageItem): Long {
        return packageDao.insert(packageItem)
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
            insertScheduledSubscription(
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

            insertScheduledSubscription(
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

            insertScheduledSubscription(
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

            insertScheduledSubscription(
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
            insertPreorder(
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

            insertPreorder(
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

            insertPreorder(
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

            insertPreorder(
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
            val fs1Id = forwardingServiceDao.insert(ForwardingService(name = "Stackry", website = "https://www.stackry.com", profilePic = "ic_launcher_foreground", storageDays = 45)).toInt()
            val fs2Id = forwardingServiceDao.insert(ForwardingService(name = "Buyee", website = "https://buyee.jp", profilePic = "ic_launcher_foreground", storageDays = 30)).toInt()
            val fs3Id = forwardingServiceDao.insert(ForwardingService(name = "Forward2me", website = "https://www.forward2me.com", profilePic = "ic_launcher_foreground", storageDays = 30)).toInt()

            forwardingServiceContactDao.insertAll(listOf(
                ForwardingServiceContact(forwardingServiceId = fs1Id, contactType = "Website", contactValue = "https://www.stackry.com"),
                ForwardingServiceContact(forwardingServiceId = fs1Id, contactType = "Email", contactValue = "support@stackry.com"),
                ForwardingServiceContact(forwardingServiceId = fs2Id, contactType = "Website", contactValue = "https://buyee.jp"),
                ForwardingServiceContact(forwardingServiceId = fs2Id, contactType = "Email", contactValue = "support@buyee.jp"),
                ForwardingServiceContact(forwardingServiceId = fs3Id, contactType = "Website", contactValue = "https://www.forward2me.com"),
                ForwardingServiceContact(forwardingServiceId = fs3Id, contactType = "Email", contactValue = "info@forward2me.com")
            ))
        }

        val shippingCompanies = shippingCompanyDao.getAllShippingCompanies().firstOrNull() ?: emptyList()
        if (shippingCompanies.isEmpty()) {
            val sc1Id = shippingCompanyDao.insert(ShippingCompany(name = "DHL Express", website = "https://www.dhl.com", profilePic = "ic_launcher_foreground")).toInt()
            val sc2Id = shippingCompanyDao.insert(ShippingCompany(name = "FedEx", website = "https://www.fedex.com", profilePic = "ic_launcher_foreground")).toInt()
            val sc3Id = shippingCompanyDao.insert(ShippingCompany(name = "UPS", website = "https://www.ups.com", profilePic = "ic_launcher_foreground")).toInt()

            shippingCompanyContactDao.insertAll(listOf(
                ShippingCompanyContact(shippingCompanyId = sc1Id, contactType = "Website", contactValue = "https://www.dhl.com"),
                ShippingCompanyContact(shippingCompanyId = sc1Id, contactType = "Phone", contactValue = "+1-800-225-5345"),
                ShippingCompanyContact(shippingCompanyId = sc2Id, contactType = "Website", contactValue = "https://www.fedex.com"),
                ShippingCompanyContact(shippingCompanyId = sc2Id, contactType = "Phone", contactValue = "+1-800-463-3339"),
                ShippingCompanyContact(shippingCompanyId = sc3Id, contactType = "Website", contactValue = "https://www.ups.com"),
                ShippingCompanyContact(shippingCompanyId = sc3Id, contactType = "Phone", contactValue = "+1-800-742-5877")
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
