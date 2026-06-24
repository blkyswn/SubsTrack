package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

data class SubscriptionWithBookstore(
    val subscription: SubscriptionType,
    val bookstore: Bookstore?
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

    // Expose User Flow
    val userFlow: Flow<User?> = userDao.getUser()

    suspend fun saveUser(user: User) {
        userDao.insertOrUpdate(user)
    }

    // Expose raw list of bookstores
    val allBookstoresFlow: Flow<List<Bookstore>> = bookstoreDao.getAllBookstores()

    suspend fun insertBookstore(bookstore: Bookstore): Long {
        return bookstoreDao.insert(bookstore)
    }

    suspend fun updateBookstore(bookstore: Bookstore) {
        bookstoreDao.update(bookstore)
    }

    suspend fun deleteBookstore(bookstore: Bookstore) {
        bookstoreDao.delete(bookstore)
    }

    // Expose combined Subscription with Bookstore
    val subscriptionsWithBookstoreFlow: Flow<List<SubscriptionWithBookstore>> = combine(
        subscriptionTypeDao.getAllSubscriptionTypes(),
        bookstoreDao.getAllBookstores()
    ) { subs, stores ->
        subs.map { sub ->
            SubscriptionWithBookstore(sub, stores.find { it.id == sub.bookstoreId })
        }
    }

    suspend fun insertSubscriptionType(subscriptionType: SubscriptionType): Long {
        return subscriptionTypeDao.insert(subscriptionType)
    }

    suspend fun updateSubscriptionType(subscriptionType: SubscriptionType) {
        subscriptionTypeDao.update(subscriptionType)
    }

    suspend fun deleteSubscriptionType(subscriptionType: SubscriptionType) {
        subscriptionTypeDao.delete(subscriptionType)
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

    suspend fun insertScheduledSubscription(scheduled: ScheduledSubscription): Long {
        return scheduledSubscriptionDao.insert(scheduled)
    }

    suspend fun updateScheduledSubscription(scheduled: ScheduledSubscription) {
        scheduledSubscriptionDao.update(scheduled)
    }

    suspend fun deleteScheduledSubscription(scheduled: ScheduledSubscription) {
        scheduledSubscriptionDao.delete(scheduled)
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
                language = "English"
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
            val sub1Id = subscriptionTypeDao.insert(
                SubscriptionType(
                    bookstoreId = store1Id,
                    title = "FairyLoot YA Monthly",
                    status = "Active",
                    price = 32.90,
                    dueDate = now + 4 * dayMs,
                    startDate = now - 60 * dayMs,
                    finishDate = now + 300 * dayMs,
                    notificationAlertDays = 3,
                    frequency = "Monthly"
                )
            ).toInt()

            val sub2Id = subscriptionTypeDao.insert(
                SubscriptionType(
                    bookstoreId = store2Id,
                    title = "Illumicrate Quarterly Special",
                    status = "Active",
                    price = 45.00,
                    dueDate = now + 25 * dayMs,
                    startDate = now - 180 * dayMs,
                    finishDate = now + 365 * dayMs,
                    notificationAlertDays = 5,
                    frequency = "Quarterly"
                )
            ).toInt()

            val sub3Id = subscriptionTypeDao.insert(
                SubscriptionType(
                    bookstoreId = store3Id,
                    title = "B&N Premium Membership",
                    status = "Active",
                    price = 39.99,
                    dueDate = now + 120 * dayMs,
                    startDate = now - 12 * dayMs,
                    finishDate = now + 353 * dayMs,
                    notificationAlertDays = 7,
                    frequency = "Yearly"
                )
            ).toInt()

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
