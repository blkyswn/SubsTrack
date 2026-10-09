package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlinx.coroutines.flow.first

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("SubsTrack", appName)
  }

  @Test
  fun `launch MainActivity cleanly`() {
    val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
    org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
    val activity = controller.get()
    org.junit.Assert.assertNotNull(activity)
  }

  @Test
  fun `test excel export and import roundtrip`() {
    val sampleUser = com.example.data.User(
      id = 1,
      username = "TestReader",
      profilePic = "avatar_classic",
      currency = "£",
      language = "English",
      country = "United Kingdom",
      defaultTrackingUrl = "https://tracking.example.com",
      defaultScheduledSubCount = 4
    )
    val sampleBookstore = com.example.data.Bookstore(
      id = 1,
      name = "Illumicrate",
      website = "https://illumicrate.com",
      profilePic = "ic_book_1"
    )
    val sampleSub = com.example.data.SubscriptionWithBookstore(
      subscription = com.example.data.SubscriptionType(
        id = 1,
        bookstoreId = 1,
        title = "Illumicrate Monthly",
        status = "Active",
        price = 28.0,
        dueDate = 1727788800000L,
        startDate = 1725196800000L,
        finishDate = 0L,
        notificationAlertDays = 3,
        frequency = "Monthly",
        skipType = "Unlimited",
        numberOfSkips = 10,
        numberOfMonths = 12
      ),
      bookstore = sampleBookstore,
      skipInfo = null
    )

    val xml = com.example.utils.ExcelExporter.generateWorkbookXml(
      user = sampleUser,
      bookstores = listOf(sampleBookstore),
      bookstoreContacts = emptyList(),
      forwardingServices = emptyList(),
      forwardingServiceContacts = emptyList(),
      shippingCompanies = emptyList(),
      shippingCompanyContacts = emptyList(),
      userAddresses = emptyList(),
      subscriptions = listOf(sampleSub),
      subscriptionSkipMethods = emptyList(),
      subscriptionSkips = emptyList(),
      scheduled = emptyList(),
      preorders = emptyList(),
      packages = emptyList()
    )

    org.junit.Assert.assertTrue("Export should produce XML workbook", xml.contains("<Workbook"))
    org.junit.Assert.assertTrue("Export should include bookstore name", xml.contains("Illumicrate"))
    org.junit.Assert.assertTrue("Export should include subscription title", xml.contains("Illumicrate Monthly"))

    val parsed = com.example.utils.ExcelImporter.parseWorkbook(
      java.io.ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)),
      "yyyy-MM-dd"
    )

    org.junit.Assert.assertEquals("TestReader", parsed.user?.username)
    org.junit.Assert.assertEquals("£", parsed.user?.currency)
    org.junit.Assert.assertEquals(1, parsed.bookstores.size)
    org.junit.Assert.assertEquals("Illumicrate", parsed.bookstores[0].name)
    org.junit.Assert.assertEquals(1, parsed.subscriptions.size)
    org.junit.Assert.assertEquals("Illumicrate Monthly", parsed.subscriptions[0].title)
    org.junit.Assert.assertEquals(28.0, parsed.subscriptions[0].price, 0.001)
  }

  @Test
  fun `test database snapshot creation and restoration`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = com.example.data.BookishDatabase.getDatabase(context)

    // Ensure database initialized
    val snapshotBytes = com.example.data.BookishDatabase.createSnapshotBytes(context)
    org.junit.Assert.assertNotNull(snapshotBytes)
    org.junit.Assert.assertTrue((snapshotBytes?.size ?: 0) > 0)

    // Restore snapshot from the created bytes
    val restored = com.example.data.BookishDatabase.restoreDatabaseSnapshot(
      context,
      java.io.ByteArrayInputStream(snapshotBytes!!)
    )
    org.junit.Assert.assertTrue("Snapshot restoration should succeed", restored)
    org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
  }

  @Test
  fun `test AppFolderManager initialization and setting`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.utils.AppFolderManager.init(context)

    // Setting null resets to default internal storage app folder
    com.example.utils.AppFolderManager.setAppFolderUri(context, null)
    val defaultPath = com.example.utils.AppFolderManager.getDefaultAppFolder(context)
    org.junit.Assert.assertEquals(defaultPath, com.example.utils.AppFolderManager.appFolderUri.value)
    org.junit.Assert.assertTrue(com.example.utils.AppFolderManager.appFolderDisplayPath.value?.contains("SubsTrack") == true)
  }

  @Test
  fun `test backup filename has datetime suffix format`() {
    val timestamp = java.text.SimpleDateFormat("yyyyMMddHHmmss", java.util.Locale.US).format(java.util.Date())
    val dbFileName = "bookish_database_backup_${timestamp}.db"
    val excelFileName = "bookish_library_export_${timestamp}.xls"

    // Verify trailing 14-digit timestamp suffix before extension
    val regex = Regex(".+_[0-9]{14}\\.[a-zA-Z0-9]+")
    org.junit.Assert.assertTrue("Database backup file name should match _yyyyMMddHHmmss suffix pattern", regex.matches(dbFileName))
    org.junit.Assert.assertTrue("Excel export file name should match _yyyyMMddHHmmss suffix pattern", regex.matches(excelFileName))
  }

  @Test
  fun `test user table default app folder path route to internal storage`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val defaultPath = com.example.utils.AppFolderManager.getDefaultAppFolder(context)
    org.junit.Assert.assertTrue("Default path must point to internal storage", defaultPath.startsWith(context.filesDir.absolutePath))
    org.junit.Assert.assertTrue("Default path must include app name SubsTrack", defaultPath.endsWith("SubsTrack"))
  }

  @Test
  fun `test excel import deduplication without comparing ids`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = com.example.data.BookishDatabase.getDatabase(context)
    val repo = com.example.data.BookishRepository(db)

    // Prepopulate
    repo.checkAndPrepopulate(com.example.utils.AppFolderManager.getDefaultAppFolder(context))

    val existingBookstoresBefore = repo.allBookstoresFlow.first()
    val initialCount = existingBookstoresBefore.size
    org.junit.Assert.assertTrue(initialCount > 0)

    // Try importing an existing store with ID = 999 (should not create duplicate, ignoring ID)
    val duplicateStore = com.example.data.Bookstore(id = 999, name = "FairyLoot", website = "https://fairyloot.com", profilePic = null)
    val importData = com.example.utils.ExcelImportData(
      bookstores = listOf(duplicateStore)
    )

    val result = repo.importSpreadsheetData(importData, replaceExisting = false)
    org.junit.Assert.assertTrue(result.success)
    org.junit.Assert.assertEquals("Duplicate bookstore should not be added", 0, result.bookstoresCount)

    val bookstoresAfter = repo.allBookstoresFlow.first()
    org.junit.Assert.assertEquals("Bookstore count should remain the same", initialCount, bookstoresAfter.size)
  }

  @Test
  fun `test scheduled subscription skip logic with edited skip registers and skips left`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = com.example.data.BookishDatabase.getDatabase(context)
    val repo = com.example.data.BookishRepository(db)

    // Prepopulate default data (bookstore 1 FairyLoot)
    repo.checkAndPrepopulate(com.example.utils.AppFolderManager.getDefaultAppFolder(context))

    // Insert a subscription type with 2 skips
    val subId = db.subscriptionTypeDao().insert(
      com.example.data.SubscriptionType(
        id = 101,
        bookstoreId = 1,
        title = "Test Sub With Skips",
        status = "Active",
        price = 30.0,
        dueDate = 1727788800000L,
        startDate = 1725196800000L,
        finishDate = 0L,
        notificationAlertDays = 3,
        frequency = "Monthly",
        skipType = "Each calendar year",
        numberOfSkips = 2
      )
    ).toInt()

    // Sync skip registers
    repo.syncSubscriptionSkip(db.subscriptionTypeDao().getSubscriptionTypeById(subId)!!)
    var skips = db.subscriptionSkipDao().getSkipsForSubscriptionType(subId)
    org.junit.Assert.assertEquals(1, skips.size)
    org.junit.Assert.assertEquals(2, skips[0].skipsLeft)

    val testDueDate = skips[0].skipStartDate!! + 86400000L

    val sched1 = com.example.data.ScheduledSubscription(
      id = 201,
      subscriptionTypeId = subId,
      bookTitle = "Book 1",
      bookAuthor = "Author 1",
      description = "Desc 1",
      dueDate = testDueDate,
      status = "Upcoming",
      isSkipped = false
    )
    val sched1Id = repo.insertScheduledSubscription(sched1).toInt()
    val insertedSched1 = sched1.copy(id = sched1Id)

    // Check canSkip
    var canSkip = repo.canSkipScheduledSubscription(insertedSched1, db.subscriptionTypeDao().getSubscriptionTypeById(subId), skips)
    org.junit.Assert.assertTrue("Should allow skip when skipsLeft > 0", canSkip)

    // Process skip
    var error = repo.processSkipForScheduledSub(insertedSched1, "yyyy-MM-dd")
    org.junit.Assert.assertNull("Skip should succeed without error", error)

    skips = db.subscriptionSkipDao().getSkipsForSubscriptionType(subId)
    org.junit.Assert.assertEquals(1, skips.size)
    org.junit.Assert.assertEquals(1, skips[0].skipsLeft)

    // User edits the skip register to set skipsLeft = 0
    val editedRegister = skips[0].copy(skipsLeft = 0)
    repo.updateSubscriptionSkip(editedRegister)

    skips = db.subscriptionSkipDao().getSkipsForSubscriptionType(subId)
    org.junit.Assert.assertEquals(0, skips[0].skipsLeft)

    val sched2 = com.example.data.ScheduledSubscription(
      id = 202,
      subscriptionTypeId = subId,
      bookTitle = "Book 2",
      bookAuthor = "Author 2",
      description = "Desc 2",
      dueDate = testDueDate,
      status = "Upcoming",
      isSkipped = false
    )
    canSkip = repo.canSkipScheduledSubscription(sched2, db.subscriptionTypeDao().getSubscriptionTypeById(subId), skips)
    org.junit.Assert.assertFalse("Should not allow skip when edited skipsLeft is 0", canSkip)

    error = repo.processSkipForScheduledSub(sched2, "yyyy-MM-dd")
    org.junit.Assert.assertNotNull("Skip should fail when skipsLeft is 0", error)
    org.junit.Assert.assertTrue("Error message should mention no skips left", error!!.contains("No skips left"))

    // User now edits the register to set numberOfSkips = 3, skipsLeft = 3
    repo.updateSubscriptionSkip(skips[0].copy(numberOfSkips = 3, skipsLeft = 3))
    skips = db.subscriptionSkipDao().getSkipsForSubscriptionType(subId)
    canSkip = repo.canSkipScheduledSubscription(sched2, db.subscriptionTypeDao().getSubscriptionTypeById(subId), skips)
    org.junit.Assert.assertTrue("Should allow skip after user edited skipsLeft to 3", canSkip)

    error = repo.processSkipForScheduledSub(sched2, "yyyy-MM-dd")
    org.junit.Assert.assertNull("Skip should succeed now", error)

    skips = db.subscriptionSkipDao().getSkipsForSubscriptionType(subId)
    org.junit.Assert.assertEquals(2, skips[0].skipsLeft)

    // Unskip sched2
    repo.processUnskipForScheduledSub(sched2)
    skips = db.subscriptionSkipDao().getSkipsForSubscriptionType(subId)
    org.junit.Assert.assertEquals(3, skips[0].skipsLeft)

    // Edit register to None
    repo.updateSubscriptionSkip(skips[0].copy(subscriptionSkipType = "None"))
    skips = db.subscriptionSkipDao().getSkipsForSubscriptionType(subId)
    canSkip = repo.canSkipScheduledSubscription(sched2, db.subscriptionTypeDao().getSubscriptionTypeById(subId), skips)
    org.junit.Assert.assertFalse("Should not allow skip when skip register type is None", canSkip)

    // Edit register to Unlimited
    repo.updateSubscriptionSkip(skips[0].copy(subscriptionSkipType = "Unlimited", numberOfSkips = 5, skipsLeft = null))
    skips = db.subscriptionSkipDao().getSkipsForSubscriptionType(subId)
    canSkip = repo.canSkipScheduledSubscription(sched2, db.subscriptionTypeDao().getSubscriptionTypeById(subId), skips)
    org.junit.Assert.assertTrue("Should allow skip when skip register type is Unlimited", canSkip)

    error = repo.processSkipForScheduledSub(sched2, "yyyy-MM-dd")
    org.junit.Assert.assertNull("Skip should succeed for Unlimited register", error)
    skips = db.subscriptionSkipDao().getSkipsForSubscriptionType(subId)
    org.junit.Assert.assertEquals(6, skips[0].numberOfSkips)
  }

  @Test
  fun `test resync skips left when changing end date of sub type and regenerating scheduled subs`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = com.example.data.BookishDatabase.getDatabase(context)
    val repo = com.example.data.BookishRepository(db)

    repo.checkAndPrepopulate(com.example.utils.AppFolderManager.getDefaultAppFolder(context))

    val cal = java.util.Calendar.getInstance()
    cal.set(2026, java.util.Calendar.JANUARY, 1, 0, 0, 0)
    cal.set(java.util.Calendar.MILLISECOND, 0)
    val jan1 = cal.timeInMillis

    cal.set(2026, java.util.Calendar.DECEMBER, 31, 23, 59, 59)
    val dec31 = cal.timeInMillis

    val subType = com.example.data.SubscriptionType(
      id = 301,
      bookstoreId = 1,
      title = "End Date Test Sub",
      status = "Active",
      price = 25.0,
      dueDate = jan1 + 10 * 86400000L,
      startDate = jan1,
      finishDate = dec31,
      notificationAlertDays = null,
      frequency = "Monthly",
      skipType = "Each calendar year",
      numberOfSkips = 3
    )
    val subId = db.subscriptionTypeDao().insert(subType).toInt()
    val savedSub = subType.copy(id = subId)

    repo.syncSubscriptionSkip(savedSub)
    var skips = db.subscriptionSkipDao().getSkipsForSubscriptionType(subId)
    org.junit.Assert.assertEquals(1, skips.size)
    org.junit.Assert.assertEquals(3, skips[0].numberOfSkips)
    org.junit.Assert.assertEquals(3, skips[0].skipsLeft)

    // User manually edited skipsLeft in register to 1
    repo.updateSubscriptionSkip(skips[0].copy(skipsLeft = 1))
    skips = db.subscriptionSkipDao().getSkipsForSubscriptionType(subId)
    org.junit.Assert.assertEquals(1, skips[0].skipsLeft)

    // Verify: skipsLeft cannot be bigger than numberOfSkips
    repo.updateSubscriptionSkip(skips[0].copy(skipsLeft = 10))
    skips = db.subscriptionSkipDao().getSkipsForSubscriptionType(subId)
    org.junit.Assert.assertEquals("Skips left cannot exceed numberOfSkips", 3, skips[0].skipsLeft)

    // Reset back to 1
    repo.updateSubscriptionSkip(skips[0].copy(skipsLeft = 1))

    // Insert scheduled subs for Jan, Feb, Mar, Apr
    val feb11 = jan1 + 41 * 86400000L
    val mar11 = jan1 + 70 * 86400000L
    val apr11 = jan1 + 100 * 86400000L

    val sched1 = com.example.data.ScheduledSubscription(
      id = 401,
      subscriptionTypeId = subId,
      bookTitle = "Book 1",
      bookAuthor = "Author",
      description = "Desc",
      dueDate = feb11,
      status = "Skipped",
      isSkipped = true
    )
    val sched2 = com.example.data.ScheduledSubscription(
      id = 402,
      subscriptionTypeId = subId,
      bookTitle = "Book 2",
      bookAuthor = "Author",
      description = "Desc",
      dueDate = mar11,
      status = "Skipped",
      isSkipped = true
    )
    val sched3 = com.example.data.ScheduledSubscription(
      id = 403,
      subscriptionTypeId = subId,
      bookTitle = "Book 3",
      bookAuthor = "Author",
      description = "Desc",
      dueDate = apr11,
      status = "Upcoming",
      isSkipped = false
    )
    db.scheduledSubscriptionDao().insert(sched1)
    db.scheduledSubscriptionDao().insert(sched2)
    db.scheduledSubscriptionDao().insert(sched3)

    // Currently 2 are skipped (sched1, sched2).
    // recalculatedSkipsLeft = maxSkips (3) - skippedCount (2) = 1.
    // actual in register = 1.
    repo.resyncSkipsLeftForSubscriptionType(subId)
    skips = db.subscriptionSkipDao().getSkipsForSubscriptionType(subId)
    org.junit.Assert.assertEquals(1, skips[0].skipsLeft)

    // Now, change end date to before March (so March sched2 is removed)
    db.scheduledSubscriptionDao().delete(sched2)
    // Now skippedCount = 1.
    // recalculatedSkipsLeft = 3 - 1 = 2.
    // BUT actual one in register is 1.
    // recalculated (2) is bigger than actual (1) -> MUST NOT UPDATE!
    repo.resyncSkipsLeftForSubscriptionType(subId)
    skips = db.subscriptionSkipDao().getSkipsForSubscriptionType(subId)
    org.junit.Assert.assertEquals("Recalculated (2) > actual (1) must not update skips left", 1, skips[0].skipsLeft)

    // Now mark sched3 as skipped and add another skipped sub (sched4):
    db.scheduledSubscriptionDao().update(sched3.copy(status = "Skipped", isSkipped = true))
    val sched4 = com.example.data.ScheduledSubscription(
      id = 404,
      subscriptionTypeId = subId,
      bookTitle = "Book 4",
      bookAuthor = "Author",
      description = "Desc",
      dueDate = feb11 + 5 * 86400000L,
      status = "Skipped",
      isSkipped = true
    )
    db.scheduledSubscriptionDao().insert(sched4)
    // Now skippedCount = 3 (sched1, sched3, sched4).
    // recalculatedSkipsLeft = 3 - 3 = 0.
    // recalculated (0) is smaller than actual (1) -> SHOULD update to 0!
    repo.resyncSkipsLeftForSubscriptionType(subId)
    skips = db.subscriptionSkipDao().getSkipsForSubscriptionType(subId)
    org.junit.Assert.assertEquals("Recalculated (0) <= actual (1) should update skips left to 0", 0, skips[0].skipsLeft)
  }
}
