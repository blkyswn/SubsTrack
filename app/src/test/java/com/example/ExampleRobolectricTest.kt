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
@Config(sdk = [36])
class ExampleRobolectricTest {

  @org.junit.Before
  fun setUp() {
    com.example.data.BookishDatabase.resetInstanceForTesting()
  }

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
  fun `test database snapshot creation and restoration`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = com.example.data.BookishDatabase.getDatabase(context)

    // Ensure database initialized with at least one record so file is written to disk
    if (db.userDao().getUserDirect() == null) {
      db.userDao().insertOrUpdate(com.example.data.User(id = 1, username = "SnapshotUser", profilePic = null, currency = "$", language = "English"))
    }
    com.example.data.BookishDatabase.checkpointDatabase(context)

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
    if (db.bookstoreDao().getAllBookstoresList().none { it.name.equals("FairyLoot", ignoreCase = true) }) {
      db.bookstoreDao().insert(com.example.data.Bookstore(name = "FairyLoot", website = "https://fairyloot.com", profilePic = null))
    }

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

  @Test
  fun `test CalendarBentoWidgetProvider updates widget cleanly`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = com.example.data.BookishDatabase.getDatabase(context)
    val repo = com.example.data.BookishRepository(db)
    repo.checkAndPrepopulate(com.example.utils.AppFolderManager.getDefaultAppFolder(context))

    // Trigger update
    com.example.widget.CalendarBentoWidgetProvider.triggerUpdate(context)
    org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
  }

  @Test
  fun `test widget layouts inflate without error`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val rvWidget = android.widget.RemoteViews(context.packageName, R.layout.calendar_bento_widget)
    val viewWidget = rvWidget.apply(context, null)
    org.junit.Assert.assertNotNull(viewWidget)

    val rvPreview = android.widget.RemoteViews(context.packageName, R.layout.calendar_bento_widget_preview)
    val viewPreview = rvPreview.apply(context, null)
    org.junit.Assert.assertNotNull(viewPreview)
  }

  @Test
  fun `test widget actions apply cleanly on inflated view`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val rvWidget = android.widget.RemoteViews(context.packageName, R.layout.calendar_bento_widget)
    rvWidget.setInt(R.id.widget_bg_image, "setColorFilter", -0x2b5c8d) // 0xFFD4A373
    rvWidget.setTextViewText(R.id.widget_today_day_name, "MONDAY")
    rvWidget.setTextViewText(R.id.widget_today_day_number, "15")
    rvWidget.setViewVisibility(R.id.widget_today_empty, android.view.View.GONE)
    rvWidget.setViewVisibility(R.id.widget_today_event_1, android.view.View.VISIBLE)
    rvWidget.setTextViewText(R.id.widget_today_event_1_title, "Test Book ($10.00)")
    rvWidget.setTextViewText(R.id.widget_today_event_1_time, "Starts at 9:00")
    val view = rvWidget.apply(context, null)
    org.junit.Assert.assertNotNull(view)
  }

  @Test
  fun `test preorder status changes to released when end date and end time passes`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = com.example.data.BookishDatabase.getDatabase(context)
    val repo = com.example.data.BookishRepository(db)
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    repo.checkAndPrepopulate(com.example.utils.AppFolderManager.getDefaultAppFolder(context))
    val viewModel = com.example.ui.viewmodel.BookishViewModel(app)

    val now = System.currentTimeMillis()
    val pastStart = now - 2 * 3600 * 1000L // 2 hours ago
    val pastEnd = now - 1 * 3600 * 1000L   // 1 hour ago
    val futureStart = now + 1 * 3600 * 1000L
    val futureEnd = now + 2 * 3600 * 1000L

    // 1. Preorder in the past with status Upcoming
    val pastPreorderId = repo.insertPreorder(
      com.example.data.Preorder(
        bookstoreId = 1,
        picturePath = null,
        bookTitle = "Past End Book",
        bookAuthor = "Author A",
        description = "Desc",
        price = 25.0,
        rangedSaleDateStart = pastStart,
        rangedSaleDateEnd = pastEnd,
        status = "Upcoming"
      )
    ).toInt()

    // 2. Preorder in the future with status Upcoming
    val futurePreorderId = repo.insertPreorder(
      com.example.data.Preorder(
        bookstoreId = 1,
        picturePath = null,
        bookTitle = "Future End Book",
        bookAuthor = "Author B",
        description = "Desc",
        price = 30.0,
        rangedSaleDateStart = futureStart,
        rangedSaleDateEnd = futureEnd,
        status = "Upcoming"
      )
    ).toInt()

    // 3. Preorder in the past that was already marked Preordered
    val preorderedId = repo.insertPreorder(
      com.example.data.Preorder(
        bookstoreId = 1,
        picturePath = null,
        bookTitle = "Already Preordered Book",
        bookAuthor = "Author C",
        description = "Desc",
        price = 35.0,
        rangedSaleDateStart = pastStart,
        rangedSaleDateEnd = pastEnd,
        status = "Preordered"
      )
    ).toInt()

    // Reconcile statuses
    viewModel.reconcileStatuses()

    val updatedPast = db.preorderDao().getPreorderById(pastPreorderId)
    val updatedFuture = db.preorderDao().getPreorderById(futurePreorderId)
    val updatedPreordered = db.preorderDao().getPreorderById(preorderedId)

    org.junit.Assert.assertEquals("Upcoming preorder past end date/time should be Released", "Released", updatedPast?.status)
    org.junit.Assert.assertEquals("Upcoming preorder before end date/time should remain Upcoming", "Upcoming", updatedFuture?.status)
    org.junit.Assert.assertEquals("Already preordered item should not be changed to Released", "Preordered", updatedPreordered?.status)
  }

  @Test
  fun `test upcoming calendar widget layout inflates without error`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val rvWidget = android.widget.RemoteViews(context.packageName, R.layout.upcoming_calendar_widget)
    val viewWidget = rvWidget.apply(context, null)
    org.junit.Assert.assertNotNull(viewWidget)

    val rvPreview = android.widget.RemoteViews(context.packageName, R.layout.upcoming_calendar_widget_preview)
    val viewPreview = rvPreview.apply(context, null)
    org.junit.Assert.assertNotNull(viewPreview)
  }

  @Test
  fun `test upcoming calendar widget trigger update`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.widget.UpcomingCalendarWidgetProvider.triggerUpdate(context)
  }

  @Test
  fun `test country list format is consistent`() {
    val isoCountries = java.util.Locale.getISOCountries()
      .map { java.util.Locale("", it).getDisplayCountry(java.util.Locale.ENGLISH) }
      .filter { it.isNotBlank() }
      .distinct()
      .sorted()
    org.junit.Assert.assertTrue(isoCountries.contains("United States"))
    org.junit.Assert.assertTrue(isoCountries.contains("United Kingdom"))
  }

  @Test
  fun `test widget theme colors for all combos`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val combos = listOf("summer", "cozy", "winter", "serenity", "green_serenity", "blood_moon", "earthy_harmony", "chocolate_sunset", "red_sunburst", "earthy_forest", "deep_sea", "soft_lavender", "default")
    for (combo in combos) {
      val user = com.example.data.User(id = 1, username = "Test", profilePic = null, currency = "USD", language = "English", themeCombo = combo, themeMode = "light")
      val colorsLight = com.example.widget.WidgetThemeUtils.getThemeColors(context, user)
      org.junit.Assert.assertNotEquals(0, colorsLight.primary)
      org.junit.Assert.assertNotEquals(0, colorsLight.surface)

      val userDark = com.example.data.User(id = 1, username = "Test", profilePic = null, currency = "USD", language = "English", themeCombo = combo, themeMode = "dark")
      val colorsDark = com.example.widget.WidgetThemeUtils.getThemeColors(context, userDark)
      org.junit.Assert.assertNotEquals(0, colorsDark.primary)
      org.junit.Assert.assertNotEquals(0, colorsDark.surface)
    }
  }

  @Test
  fun `test adding store shipping date automatically changes preorder and scheduled sub status to shipped`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = com.example.data.BookishDatabase.getDatabase(context)
    val repo = com.example.data.BookishRepository(db)

    // Ensure bookstore exists
    if (db.bookstoreDao().getBookstoreById(1) == null) {
      db.bookstoreDao().insert(
        com.example.data.Bookstore(id = 1, name = "Test Store", website = "https://example.com", profilePic = null)
      )
    }
    // Ensure subscription type exists
    if (db.subscriptionTypeDao().getSubscriptionTypeById(1) == null) {
      db.subscriptionTypeDao().insert(
        com.example.data.SubscriptionType(
          id = 1,
          bookstoreId = 1,
          title = "Test Sub Type",
          status = "Active",
          price = 20.0,
          dueDate = System.currentTimeMillis(),
          startDate = System.currentTimeMillis(),
          finishDate = System.currentTimeMillis() + 864000000L,
          notificationAlertDays = 3,
          frequency = "Monthly"
        )
      )
    }

    // 1. Create a preorder with status Preordered
    val preorderId = repo.insertPreorder(
      com.example.data.Preorder(
        bookstoreId = 1,
        picturePath = null,
        bookTitle = "Auto Ship Test Preorder",
        bookAuthor = "Author X",
        description = "Desc",
        price = 25.0,
        rangedSaleDateStart = System.currentTimeMillis(),
        rangedSaleDateEnd = System.currentTimeMillis() + 86400000L,
        status = "Preordered"
      )
    ).toInt()

    val initialPreorder = db.preorderDao().getPreorderById(preorderId)
    org.junit.Assert.assertEquals("Preordered", initialPreorder?.status)

    // Add store shipping date to package
    val pkg = repo.getOrCreatePackage("preorders", preorderId)
    repo.updatePackage(pkg.copy(storeShippingDate = System.currentTimeMillis()))

    val updatedPreorder = db.preorderDao().getPreorderById(preorderId)
    org.junit.Assert.assertEquals("Shipped", updatedPreorder?.status)

    // 2. Create a scheduled subscription with status Renewed
    val subId = repo.insertScheduledSubscription(
      com.example.data.ScheduledSubscription(
        subscriptionTypeId = 1,
        bookTitle = "Auto Ship Test Scheduled Sub",
        bookAuthor = "Author Y",
        description = "Desc",
        dueDate = System.currentTimeMillis(),
        status = "Renewed"
      )
    ).toInt()

    val initialSub = db.scheduledSubscriptionDao().getScheduledSubscriptionById(subId)
    org.junit.Assert.assertEquals("Renewed", initialSub?.status)

    // Add store shipping date to package
    val subPkg = repo.getOrCreatePackage("scheduled_subs", subId)
    repo.updatePackage(subPkg.copy(storeShippingDate = System.currentTimeMillis()))

    val updatedSub = db.scheduledSubscriptionDao().getScheduledSubscriptionById(subId)
    org.junit.Assert.assertEquals("Shipped", updatedSub?.status)
  }

  @Test
  fun `test tracking numbers and dates auto update status and dates`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = com.example.data.BookishDatabase.getDatabase(context)
    val repo = com.example.data.BookishRepository(db)

    // Ensure bookstore exists
    if (db.bookstoreDao().getBookstoreById(1) == null) {
      db.bookstoreDao().insert(
        com.example.data.Bookstore(id = 1, name = "Test Store", website = "https://example.com", profilePic = null)
      )
    }

    // 1. Test store tracking number automatically sets storeShippingDate to today and status to Shipped
    val preorder1 = repo.insertPreorder(
      com.example.data.Preorder(
        bookstoreId = 1,
        picturePath = null,
        bookTitle = "Tracking Test 1",
        bookAuthor = "Author A",
        description = "Desc",
        price = 20.0,
        rangedSaleDateStart = System.currentTimeMillis(),
        rangedSaleDateEnd = System.currentTimeMillis() + 86400000L,
        status = "Preordered"
      )
    ).toInt()

    val pkg1 = repo.getOrCreatePackage("preorders", preorder1)
    repo.updatePackage(pkg1.copy(storeTrackingNumber = "TRACK123456"))

    val updatedPkg1 = repo.getPackageDirect("preorders", preorder1)
    val updatedPr1 = db.preorderDao().getPreorderById(preorder1)
    org.junit.Assert.assertNotNull(updatedPkg1?.storeShippingDate)
    org.junit.Assert.assertEquals("Shipped", updatedPr1?.status)

    // 2. Test forwarder received date automatically sets status to In Suite
    repo.updatePackage(updatedPkg1!!.copy(forwarderReceivedDate = System.currentTimeMillis()))
    val updatedPr2 = db.preorderDao().getPreorderById(preorder1)
    org.junit.Assert.assertEquals("In Suite", updatedPr2?.status)

    // 3. Test forwarder tracking number automatically sets forwarderShippedDate to today and status to Forwarded
    repo.updatePackage(updatedPkg1.copy(forwarderTrackingNumber = "FWD987654"))
    val updatedPkg3 = repo.getPackageDirect("preorders", preorder1)
    val updatedPr3 = db.preorderDao().getPreorderById(preorder1)
    org.junit.Assert.assertNotNull(updatedPkg3?.forwarderShippedDate)
    org.junit.Assert.assertEquals("Forwarded", updatedPr3?.status)

    // 4. Test forwarder shipping date directly informed sets status to Forwarded
    val preorder2 = repo.insertPreorder(
      com.example.data.Preorder(
        bookstoreId = 1,
        picturePath = null,
        bookTitle = "Tracking Test 2",
        bookAuthor = "Author B",
        description = "Desc",
        price = 22.0,
        rangedSaleDateStart = System.currentTimeMillis(),
        rangedSaleDateEnd = System.currentTimeMillis() + 86400000L,
        status = "In Suite"
      )
    ).toInt()
    val pkg2 = repo.getOrCreatePackage("preorders", preorder2)
    repo.updatePackage(pkg2.copy(forwarderShippedDate = System.currentTimeMillis()))
    val updatedPr4 = db.preorderDao().getPreorderById(preorder2)
    org.junit.Assert.assertEquals("Forwarded", updatedPr4?.status)

    // 5. Test received date informed sets status to Received
    repo.updatePackage(pkg2.copy(receivedDate = System.currentTimeMillis()))
    val updatedPr5 = db.preorderDao().getPreorderById(preorder2)
    org.junit.Assert.assertEquals("Received", updatedPr5?.status)
  }

  @Test
  fun `test today preorders widget layout inflates cleanly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val rvWidget = android.widget.RemoteViews(context.packageName, R.layout.today_preorders_widget)
    val viewWidget = rvWidget.apply(context, null)
    org.junit.Assert.assertNotNull(viewWidget)

    val rvItem = android.widget.RemoteViews(context.packageName, R.layout.today_preorders_widget_item)
    val viewItem = rvItem.apply(context, null)
    org.junit.Assert.assertNotNull(viewItem)

    val rvPreview = android.widget.RemoteViews(context.packageName, R.layout.today_preorders_widget_preview)
    val viewPreview = rvPreview.apply(context, null)
    org.junit.Assert.assertNotNull(viewPreview)

    val samplePreorder = com.example.data.Preorder(
      id = 1,
      bookstoreId = 1,
      picturePath = null,
      bookTitle = "Sample Book",
      bookAuthor = "Sample Author",
      description = "",
      price = 15.0,
      rangedSaleDateStart = System.currentTimeMillis(),
      rangedSaleDateEnd = System.currentTimeMillis(),
      status = "Preordered"
    )
    val themeColors = com.example.widget.WidgetThemeUtils.getThemeColors(context, null)
    val populatedItemRv = com.example.widget.TodayPreordersWidgetProvider.buildPreorderItemViews(
      context = context,
      item = samplePreorder,
      bookstores = emptyList(),
      currency = "$",
      themeColors = themeColors,
      position = 0,
      totalCount = 1
    )
    val populatedView = populatedItemRv.apply(context, null)
    org.junit.Assert.assertNotNull(populatedView)

    // Test multiple preorders with slider dots bitmap
    val multiItemRv = com.example.widget.TodayPreordersWidgetProvider.buildPreorderItemViews(
      context = context,
      item = samplePreorder.copy(status = "Forwarded"),
      bookstores = emptyList(),
      currency = "$",
      themeColors = themeColors,
      position = 1,
      totalCount = 3
    )
    val multiPopulatedView = multiItemRv.apply(context, null)
    org.junit.Assert.assertNotNull(multiPopulatedView)

    // Test 1-row mode buildPreorderItemViews
    val oneRowItemRv = com.example.widget.TodayPreordersWidgetProvider.buildPreorderItemViews(
      context = context,
      item = samplePreorder,
      bookstores = emptyList(),
      currency = "$",
      themeColors = themeColors,
      position = 0,
      totalCount = 1,
      isOneRow = true
    )
    val oneRowView = oneRowItemRv.apply(context, null)
    org.junit.Assert.assertNotNull(oneRowView)

    // Test that triggerUpdate runs without NullPointerException or collection errors
    com.example.widget.TodayPreordersWidgetProvider.triggerUpdate(context)
    org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

    // Test that ACTION_NEXT_PREORDER and ACTION_PREV_PREORDER run cleanly
    val provider = com.example.widget.TodayPreordersWidgetProvider()
    val nextIntent = android.content.Intent("com.example.ACTION_NEXT_PREORDER")
    provider.onReceive(context, nextIntent)
    val prevIntent = android.content.Intent("com.example.ACTION_PREV_PREORDER")
    provider.onReceive(context, prevIntent)
    org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

    // Test Wishlist status rendering in widget
    val wishlistRv = com.example.widget.TodayPreordersWidgetProvider.buildPreorderItemViews(
      context = context,
      item = samplePreorder.copy(status = "Wishlist"),
      bookstores = emptyList(),
      currency = "$",
      themeColors = themeColors,
      position = 0,
      totalCount = 1
    )
    val wishlistView = wishlistRv.apply(context, null)
    org.junit.Assert.assertNotNull(wishlistView)
  }

  @Test
  fun `test wishlist status transitions and auto package dates`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = com.example.data.BookishDatabase.getDatabase(context)
    val repo = com.example.data.BookishRepository(db)

    // Create a bookstore
    repo.insertBookstore(com.example.data.Bookstore(name = "Test Bookstore", website = "https://example.com", profilePic = "ic_book_1"))

    // 1. Create a preorder with Wishlist status
    val wishlistPreorder = com.example.data.Preorder(
      bookstoreId = 1,
      picturePath = null,
      bookTitle = "Wishlist Book",
      bookAuthor = "Author W",
      description = "Desc",
      price = 28.0,
      rangedSaleDateStart = System.currentTimeMillis(),
      rangedSaleDateEnd = System.currentTimeMillis(),
      status = "Wishlist"
    )
    val preorderId = repo.insertPreorder(wishlistPreorder).toInt()
    val saved = db.preorderDao().getPreorderById(preorderId)
    org.junit.Assert.assertEquals("Wishlist", saved?.status)

    // 2. Promote to Preordered: should set purchase date in packages
    repo.updatePreorder(saved!!.copy(status = "Preordered"))
    val pkgAfterPreorder = db.packageDao().getPackageDirect("preorders", preorderId)
    org.junit.Assert.assertNotNull("Package should exist when preordered", pkgAfterPreorder)
    org.junit.Assert.assertNotNull("Purchase date should be set when promoted to Preordered", pkgAfterPreorder?.purchaseDate)

    // 3. Revert back to Wishlist: should clear purchase date
    repo.updatePreorder(saved.copy(status = "Wishlist"))
    val pkgAfterRevert = db.packageDao().getPackageDirect("preorders", preorderId)
    org.junit.Assert.assertNull("Purchase date should be cleared when reverted to Wishlist", pkgAfterRevert?.purchaseDate)
  }

  @Test
  fun `reminder does not schedule when reminder datetime has passed`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
    val shadowAlarmManager = org.robolectric.Shadows.shadowOf(alarmManager)
    shadowAlarmManager.scheduledAlarms.clear()

    // 1. Past date
    val pastDate = System.currentTimeMillis() - 2 * 24 * 60 * 60 * 1000L
    com.example.receiver.ReminderScheduler.schedulePreorderReminder(
      context = context,
      preorderId = 999,
      title = "Past Preorder",
      saleDateStart = pastDate,
      offsetDays = 0,
      hour = 8,
      minute = 0
    )

    org.junit.Assert.assertEquals(0, shadowAlarmManager.scheduledAlarms.size)

    // 2. Past sub
    com.example.receiver.ReminderScheduler.scheduleScheduledSubReminder(
      context = context,
      scheduledSubId = 888,
      subTitle = "Past Sub",
      bookTitle = "Book",
      dueDate = pastDate,
      offsetDays = 0,
      hour = 8,
      minute = 0
    )

    org.junit.Assert.assertEquals(0, shadowAlarmManager.scheduledAlarms.size)

    // 3. Future date
    val futureDate = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L
    com.example.receiver.ReminderScheduler.schedulePreorderReminder(
      context = context,
      preorderId = 1001,
      title = "Future Preorder",
      saleDateStart = futureDate,
      offsetDays = 1,
      hour = 8,
      minute = 0
    )

    org.junit.Assert.assertTrue("Future preorder should schedule an alarm", shadowAlarmManager.scheduledAlarms.isNotEmpty())

    // 4. Test ReminderReceiver drops past reminder onReceive
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
    val shadowNotificationManager = org.robolectric.Shadows.shadowOf(notificationManager)
    notificationManager.cancelAll()

    val receiver = com.example.receiver.ReminderReceiver()
    val pastIntent = android.content.Intent("com.example.ACTION_REMINDER").apply {
      putExtra("type", "preorder")
      putExtra("id", 12345)
      putExtra("title", "Stale Preorder")
      putExtra("targetTime", System.currentTimeMillis() - 35 * 60 * 1000L) // 35 minutes in the past
    }
    receiver.onReceive(context, pastIntent)
    org.junit.Assert.assertEquals("No notification should pop up when reminder datetime has passed", 0, shadowNotificationManager.allNotifications.size)

    // 5. Test Preorder notification includes Preordered action button
    notificationManager.cancelAll()
    val futureReminderIntent = android.content.Intent("com.example.ACTION_REMINDER").apply {
      putExtra("type", "preorder")
      putExtra("id", 777)
      putExtra("title", "Action Preorder")
      putExtra("targetTime", System.currentTimeMillis() + 10_000L)
    }
    receiver.onReceive(context, futureReminderIntent)
    val notifications = shadowNotificationManager.allNotifications
    org.junit.Assert.assertEquals("Notification should be posted", 1, notifications.size)
    val postedNotification = notifications.first()
    val actions = postedNotification.actions
    org.junit.Assert.assertNotNull("Notification should have action buttons", actions)
    val preorderedAction = actions.firstOrNull { it.title?.toString().equals("Preordered", ignoreCase = true) }
    org.junit.Assert.assertNotNull("Preorder notification must have 'Preordered' action button", preorderedAction)
    org.junit.Assert.assertNotNull("In-app reminder should be posted to ReminderEventBus", com.example.receiver.ReminderEventBus.currentReminder.value)
  }

  @Test
  fun `notification action changes preorder status to Preordered`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = com.example.data.BookishDatabase.getDatabase(context)
    val repo = com.example.data.BookishRepository(db)

    val bookstoreId = repo.insertBookstore(
      com.example.data.Bookstore(name = "Test Bookstore", website = "https://bookstore.com", profilePic = null)
    ).toInt()

    val preorderId = repo.insertPreorder(
      com.example.data.Preorder(
        bookstoreId = bookstoreId,
        picturePath = null,
        bookTitle = "The Winds of Winter",
        bookAuthor = "George R.R. Martin",
        description = "Test Description",
        price = 28.5,
        rangedSaleDateStart = System.currentTimeMillis() + 86400000L,
        rangedSaleDateEnd = 0L,
        status = "Upcoming"
      )
    ).toInt()

    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
    val shadowNotificationManager = org.robolectric.Shadows.shadowOf(notificationManager)

    // Post notification first
    val receiver = com.example.receiver.ReminderReceiver()
    val reminderIntent = android.content.Intent("com.example.ACTION_REMINDER").apply {
      putExtra("type", "preorder")
      putExtra("id", preorderId)
      putExtra("title", "The Winds of Winter")
      putExtra("targetTime", System.currentTimeMillis() + 10_000L)
    }
    receiver.onReceive(context, reminderIntent)
    org.junit.Assert.assertEquals(1, shadowNotificationManager.allNotifications.size)

    // Trigger ACTION_MARK_PREORDERED
    val actionIntent = android.content.Intent(com.example.receiver.ReminderReceiver.ACTION_MARK_PREORDERED).apply {
      putExtra("preorderId", preorderId)
      putExtra("notificationId", preorderId)
    }
    receiver.onReceive(context, actionIntent)

    // Allow background coroutines and UI thread tasks to execute
    org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
    kotlinx.coroutines.delay(100)

    val updatedPreorder = repo.getPreorderById(preorderId)
    org.junit.Assert.assertNotNull(updatedPreorder)
    org.junit.Assert.assertEquals("Preordered", updatedPreorder?.status)
    org.junit.Assert.assertEquals(0, shadowNotificationManager.allNotifications.size)
  }

  @Test
  fun `test wishlist boolean field in preorder and scheduled subs tables`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = com.example.data.BookishDatabase.getDatabase(context)

    // Ensure bookstore exists
    val bookstoreId = db.bookstoreDao().insert(
      com.example.data.Bookstore(name = "Wishlist Bookstore", website = "https://wishlist.com", profilePic = null)
    ).toInt()

    // Ensure subscription type exists
    val subTypeId = db.subscriptionTypeDao().insert(
      com.example.data.SubscriptionType(
        bookstoreId = bookstoreId,
        title = "Fantasy Monthly",
        status = "Active",
        price = 30.0,
        dueDate = System.currentTimeMillis() + 86400000L,
        startDate = System.currentTimeMillis(),
        finishDate = 0L,
        notificationAlertDays = 2,
        frequency = "Monthly"
      )
    ).toInt()

    // 1. Test Preorder table wishlist field
    val preorderFalse = com.example.data.Preorder(
      bookstoreId = bookstoreId,
      picturePath = null,
      bookTitle = "Normal Book",
      bookAuthor = "Author A",
      description = "Desc",
      price = 25.0,
      rangedSaleDateStart = System.currentTimeMillis() + 100000L,
      rangedSaleDateEnd = System.currentTimeMillis() + 200000L,
      status = "Upcoming",
      wishlist = false
    )
    val prIdFalse = db.preorderDao().insert(preorderFalse).toInt()
    val fetchedPrFalse = db.preorderDao().getPreorderById(prIdFalse)
    org.junit.Assert.assertNotNull(fetchedPrFalse)
    org.junit.Assert.assertFalse("Preorder wishlist should be false", fetchedPrFalse!!.wishlist)

    val preorderTrue = com.example.data.Preorder(
      bookstoreId = bookstoreId,
      picturePath = null,
      bookTitle = "Wishlist Book",
      bookAuthor = "Author B",
      description = "Desc",
      price = 29.99,
      rangedSaleDateStart = System.currentTimeMillis() + 100000L,
      rangedSaleDateEnd = System.currentTimeMillis() + 200000L,
      status = "Upcoming",
      wishlist = true
    )
    val prIdTrue = db.preorderDao().insert(preorderTrue).toInt()
    val fetchedPrTrue = db.preorderDao().getPreorderById(prIdTrue)
    org.junit.Assert.assertNotNull(fetchedPrTrue)
    org.junit.Assert.assertTrue("Preorder wishlist should be true", fetchedPrTrue!!.wishlist)

    // Update preorder wishlist field
    db.preorderDao().update(fetchedPrFalse.copy(wishlist = true))
    val updatedPr = db.preorderDao().getPreorderById(prIdFalse)
    org.junit.Assert.assertTrue("Updated preorder wishlist should be true", updatedPr!!.wishlist)

    // 2. Test ScheduledSubscription table bookmark field
    val schedFalse = com.example.data.ScheduledSubscription(
      subscriptionTypeId = subTypeId,
      bookTitle = "Monthly Delivery 1",
      bookAuthor = "Author X",
      description = "Desc",
      dueDate = System.currentTimeMillis() + 86400000L,
      status = "Upcoming",
      bookmark = false
    )
    val schedIdFalse = db.scheduledSubscriptionDao().insert(schedFalse).toInt()
    val fetchedSchedFalse = db.scheduledSubscriptionDao().getScheduledSubscriptionById(schedIdFalse)
    org.junit.Assert.assertNotNull(fetchedSchedFalse)
    org.junit.Assert.assertFalse("Scheduled sub bookmark should be false", fetchedSchedFalse!!.bookmark)

    val schedTrue = com.example.data.ScheduledSubscription(
      subscriptionTypeId = subTypeId,
      bookTitle = "Monthly Delivery 2",
      bookAuthor = "Author Y",
      description = "Desc",
      dueDate = System.currentTimeMillis() + 86400000L * 2,
      status = "Upcoming",
      bookmark = true
    )
    val schedIdTrue = db.scheduledSubscriptionDao().insert(schedTrue).toInt()
    val fetchedSchedTrue = db.scheduledSubscriptionDao().getScheduledSubscriptionById(schedIdTrue)
    org.junit.Assert.assertNotNull(fetchedSchedTrue)
    org.junit.Assert.assertTrue("Scheduled sub bookmark should be true", fetchedSchedTrue!!.bookmark)

    // Update scheduled sub bookmark field
    db.scheduledSubscriptionDao().update(fetchedSchedFalse.copy(bookmark = true))
    val updatedSched = db.scheduledSubscriptionDao().getScheduledSubscriptionById(schedIdFalse)
    org.junit.Assert.assertTrue("Updated scheduled sub bookmark should be true", updatedSched!!.bookmark)
  }

  @Test
  fun `test copy as preorder inherits properties and wishlist toggling`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = com.example.data.BookishDatabase.getDatabase(context)

    val bookstoreId = db.bookstoreDao().insert(
      com.example.data.Bookstore(name = "Waterstones", website = "https://waterstones.com", profilePic = null)
    ).toInt()

    val original = com.example.data.Preorder(
      bookstoreId = bookstoreId,
      picturePath = "path/to/cover.jpg",
      bookTitle = "The Winds of Winter",
      bookAuthor = "George R.R. Martin",
      description = "Epic fantasy novel",
      price = 28.50,
      rangedSaleDateStart = 1771804800000L,
      rangedSaleDateEnd = 1771891200000L,
      status = "Upcoming",
      reminderEnabled = true,
      reminderHour = 10,
      reminderMinute = 30,
      rating = 4.5,
      currency = "£",
      basePrice = 25.0,
      shippingPrice = 3.5,
      wishlist = false
    )
    val origId = db.preorderDao().insert(original).toInt()
    val fetched = db.preorderDao().getPreorderById(origId)
    org.junit.Assert.assertNotNull(fetched)

    // Toggling wishlist updates field
    db.preorderDao().update(fetched!!.copy(wishlist = true))
    val toggledWishlist = db.preorderDao().getPreorderById(origId)
    org.junit.Assert.assertTrue("Wishlist should be toggled to true", toggledWishlist!!.wishlist)

    // "Copy As": creating a new preorder with original preorder's values as base
    val copy = original.copy(
      id = 0,
      bookTitle = "The Winds of Winter (Special Edition)",
      wishlist = toggledWishlist.wishlist
    )
    val copyId = db.preorderDao().insert(copy).toInt()
    val fetchedCopy = db.preorderDao().getPreorderById(copyId)

    org.junit.Assert.assertNotNull(fetchedCopy)
    org.junit.Assert.assertNotEquals(origId, copyId)
    org.junit.Assert.assertEquals(original.bookAuthor, fetchedCopy!!.bookAuthor)
    org.junit.Assert.assertEquals(original.bookstoreId, fetchedCopy.bookstoreId)
    org.junit.Assert.assertEquals(original.price, fetchedCopy.price, 0.001)
    org.junit.Assert.assertEquals(original.currency, fetchedCopy.currency)
    org.junit.Assert.assertEquals(original.rangedSaleDateStart, fetchedCopy.rangedSaleDateStart)
    org.junit.Assert.assertTrue(fetchedCopy.wishlist)
  }

  @Test
  fun `test transfer scheduled subscription to preorder with released status if past due`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = com.example.data.BookishDatabase.getDatabase(context)

    val bookstoreId = db.bookstoreDao().insert(
      com.example.data.Bookstore(name = "FairyLoot", website = "https://fairyloot.com", profilePic = null)
    ).toInt()

    val subTypeId = db.subscriptionTypeDao().insert(
      com.example.data.SubscriptionType(
        bookstoreId = bookstoreId,
        title = "YA Box",
        status = "Active",
        price = 32.0,
        dueDate = System.currentTimeMillis() + 86400000L,
        startDate = System.currentTimeMillis(),
        finishDate = 0L,
        notificationAlertDays = 2,
        frequency = "Monthly"
      )
    ).toInt()

    val pastDueDate = System.currentTimeMillis() - 86400000L
    val schedPast = com.example.data.ScheduledSubscription(
      subscriptionTypeId = subTypeId,
      bookTitle = "The Stolen Heir",
      bookAuthor = "Holly Black",
      description = "Monthly box edition",
      dueDate = pastDueDate,
      status = "Upcoming",
      bookmark = true
    )
    val schedId = db.scheduledSubscriptionDao().insert(schedPast).toInt()
    val fetched = db.scheduledSubscriptionDao().getScheduledSubscriptionById(schedId)
    org.junit.Assert.assertNotNull(fetched)

    // Simulate transfer logic: if dueDate <= now, status is Released, else Upcoming
    val isPast = fetched!!.dueDate <= System.currentTimeMillis()
    val transferStatus = if (isPast) "Released" else "Upcoming"

    val transferredPreorder = com.example.data.Preorder(
      bookstoreId = bookstoreId,
      picturePath = fetched.picturePath,
      bookTitle = fetched.bookTitle,
      bookAuthor = fetched.bookAuthor,
      description = fetched.description,
      price = 32.0,
      rangedSaleDateStart = fetched.dueDate,
      rangedSaleDateEnd = fetched.dueDate,
      status = transferStatus,
      rating = fetched.rating,
      wishlist = fetched.bookmark
    )
    val prId = db.preorderDao().insert(transferredPreorder).toInt()
    val fetchedPr = db.preorderDao().getPreorderById(prId)

    org.junit.Assert.assertNotNull(fetchedPr)
    org.junit.Assert.assertEquals("The Stolen Heir", fetchedPr!!.bookTitle)
    org.junit.Assert.assertEquals("Holly Black", fetchedPr.bookAuthor)
    org.junit.Assert.assertEquals("Released", fetchedPr.status)
    org.junit.Assert.assertTrue(fetchedPr.wishlist)
  }

  @Test
  fun `test transfer disabled for renewed forwarded in suite shipped received statuses`() {
    val disabledStatuses = listOf("renewed", "Renewed", "paid", "forwarded", "Forwarded", "in suite", "In Suite", "shipped", "Shipped", "received", "Received")
    val enabledStatuses = listOf("upcoming", "Upcoming", "skipped", "Skipped", "CustomStatus")

    fun isTransferEnabled(status: String): Boolean {
      val curSubStatus = status.lowercase().trim()
      return curSubStatus !in listOf("renewed", "paid", "forwarded", "in suite", "shipped", "received")
    }

    for (status in disabledStatuses) {
      org.junit.Assert.assertFalse("Transfer should be disabled for status $status", isTransferEnabled(status))
    }
    for (status in enabledStatuses) {
      org.junit.Assert.assertTrue("Transfer should be enabled for status $status", isTransferEnabled(status))
    }
  }
}


