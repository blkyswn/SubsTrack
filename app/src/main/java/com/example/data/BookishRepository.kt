package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

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
    private val subscriptionTypeDao = database.subscriptionTypeDao()
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
    val allSubscriptionSkipsFlow: Flow<List<SubscriptionSkip>> = subscriptionSkipDao.getAllSubscriptionSkips()

    suspend fun insertBookstore(bookstore: Bookstore): Long {
        return bookstoreDao.insert(bookstore)
    }

    suspend fun updateBookstore(bookstore: Bookstore) {
        bookstoreDao.update(bookstore)
    }

    suspend fun deleteBookstore(bookstore: Bookstore) {
        bookstoreDao.delete(bookstore)
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
                    val maxSkips = subType.numberOfSkips ?: 0
                    if (maxSkips <= 0) {
                        val renewDateStr = formatRenewDate(endOfYear)
                        return "No skips left. Skips renew in $renewDateStr."
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
                    val startDate = renewalDate
                    val cal = java.util.Calendar.getInstance()
                    cal.timeInMillis = renewalDate
                    cal.add(java.util.Calendar.MONTH, subType.numberOfMonths ?: 1)
                    cal.add(java.util.Calendar.DAY_OF_MONTH, -1)
                    val endDate = cal.timeInMillis

                    val uninitialized = skips.firstOrNull { it.skipStartDate == null || it.skipEndDate == null }
                    val maxSkips = subType.numberOfSkips ?: 0
                    if (maxSkips <= 0) {
                        val renewDateStr = formatRenewDate(endDate)
                        return "No skips left. Skips renew in $renewDateStr."
                    } else {
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
                themeMode = "system"
            )
            userDao.insertOrUpdate(defaultUser)

            // 2. Insert Bookstore Samples
            val store1Id = bookstoreDao.insert(Bookstore(name = "FairyLoot", url = "https://fairyloot.com", profilePic = "ic_launcher_foreground")).toInt()
            val store2Id = bookstoreDao.insert(Bookstore(name = "Illumicrate", url = "https://www.illumicrate.com", profilePic = "ic_launcher_foreground")).toInt()
            val store3Id = bookstoreDao.insert(Bookstore(name = "Barnes & Noble", url = "https://www.barnesandnoble.com", profilePic = "ic_launcher_foreground")).toInt()
            val store4Id = bookstoreDao.insert(Bookstore(name = "Waterstones", url = "https://www.waterstones.com", profilePic = "ic_launcher_foreground")).toInt()

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
                numberOfMonths = null
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
                numberOfMonths = 6
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
                numberOfMonths = null
            )
            val sub3Id = insertSubscriptionType(sub3Obj).toInt()

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
            preorderDao.insert(
                Preorder(
                    bookstoreId = store3Id,
                    picturePath = null,
                    bookTitle = "Wind and Truth",
                    bookAuthor = "Brandon Sanderson",
                    description = "Stormlight Archive Book 5. Signed first edition hardback.",
                    price = 34.99,
                    rangedSaleDateStart = now + 15 * dayMs,
                    rangedSaleDateEnd = now + 17 * dayMs,
                    status = "Preordered"
                )
            )

            preorderDao.insert(
                Preorder(
                    bookstoreId = store4Id,
                    picturePath = null,
                    bookTitle = "The Winds of Winter",
                    bookAuthor = "George R.R. Martin",
                    description = "Expected release from local bookstore pre-orders.",
                    price = 28.50,
                    rangedSaleDateStart = now + 120 * dayMs,
                    rangedSaleDateEnd = now + 125 * dayMs,
                    status = "Upcoming"
                )
            )

            preorderDao.insert(
                Preorder(
                    bookstoreId = store3Id,
                    picturePath = null,
                    bookTitle = "Iron Flame (Deluxe Edition)",
                    bookAuthor = "Rebecca Yarros",
                    description = "Special stenciled edges preorder.",
                    price = 30.00,
                    rangedSaleDateStart = now - 5 * dayMs,
                    rangedSaleDateEnd = now - 3 * dayMs,
                    status = "Shipped"
                )
            )
        }
    }
}
