package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.BookishDatabase
import com.example.ui.screens.UpcomingEvent
import com.example.ui.screens.isUpcomingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CalendarBentoWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        for (widgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, widgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        triggerUpdate(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                triggerUpdate(context)
            }
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val pendingResult = try {
            goAsync()
        } catch (_: Exception) {
            null
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = BookishDatabase.getDatabase(context)
                val user = db.userDao().getUserDirect() ?: db.userDao().getUser().firstOrNull()
                val bookstores = db.bookstoreDao().getAllBookstoresList()
                val subTypes = db.subscriptionTypeDao().getAllSubscriptionTypesList()
                val scheduledSubs = db.scheduledSubscriptionDao().getAllScheduledSubscriptionsList()
                val preorders = db.preorderDao().getAllPreordersList()

                val now = System.currentTimeMillis()
                val displayAmounts = user?.displayAmounts == true
                val currency = user?.currency ?: "$"

                val activeScheduled = scheduledSubs.filter {
                    !it.isSkipped &&
                            !it.status.equals("Skipped", ignoreCase = true) &&
                            !it.status.equals("Received", ignoreCase = true) &&
                            !it.status.equals("Cancelled", ignoreCase = true)
                }.map { sched ->
                    val subType = subTypes.find { st -> st.id == sched.subscriptionTypeId }
                    val store = bookstores.find { b -> b.id == subType?.bookstoreId }
                    val subTypeName = subType?.title?.ifBlank { null } ?: sched.bookTitle
                    val bookstoreName = store?.name?.trim() ?: ""
                    val startDt = sched.dueDate
                    val endDt = sched.dueDate
                    val isSubReminderDDay = subType?.reminderEnabled == true && subType.reminderDDayOffset == 0
                    val reminderH = if (isSubReminderDDay) subType?.reminderHour else null
                    val reminderM = if (isSubReminderDDay) subType?.reminderMinute else null
                    UpcomingEvent(
                        id = "sched_${sched.id}",
                        title = subTypeName,
                        author = sched.bookAuthor,
                        date = startDt,
                        startDate = startDt,
                        endDate = endDt,
                        type = "scheduled",
                        status = sched.status,
                        isSkipped = false,
                        subTitle = if (bookstoreName.isNotBlank()) bookstoreName else subTypeName,
                        price = subType?.price,
                        rawScheduled = null,
                        rawPreorder = null,
                        reminderHour = reminderH,
                        reminderMinute = reminderM,
                        hasTime = reminderH != null
                    )
                }

                val activePreorders = preorders.filter {
                    !it.status.equals("Received", ignoreCase = true) &&
                            !it.status.equals("Cancelled", ignoreCase = true)
                }.map { pr ->
                    val store = bookstores.find { b -> b.id == pr.bookstoreId }
                    val bookstoreName = store?.name?.trim() ?: ""
                    val title = pr.bookTitle
                    val startDt = pr.rangedSaleDateStart
                    val endDt = if (pr.rangedSaleDateEnd > 0L) pr.rangedSaleDateEnd else startDt
                    val calStart = Calendar.getInstance().apply { timeInMillis = startDt }
                    val hasStartSpecificTime = calStart.get(Calendar.HOUR_OF_DAY) != 0 || calStart.get(Calendar.MINUTE) != 0
                    val startHour = if (hasStartSpecificTime) calStart.get(Calendar.HOUR_OF_DAY) else null
                    val startMinute = if (hasStartSpecificTime) calStart.get(Calendar.MINUTE) else null
                    val isPreorderReminderInDDay = pr.reminderEnabled && pr.reminderDDayOffset == 0
                    val finalH = if (startHour != null) startHour else if (isPreorderReminderInDDay) pr.reminderHour else null
                    val finalM = if (startMinute != null) startMinute else if (isPreorderReminderInDDay) pr.reminderMinute else null

                    UpcomingEvent(
                        id = "pre_${pr.id}",
                        title = title,
                        author = pr.bookAuthor,
                        date = startDt,
                        startDate = startDt,
                        endDate = endDt,
                        type = "preorder",
                        status = pr.status,
                        isSkipped = false,
                        subTitle = bookstoreName,
                        price = pr.price,
                        rawScheduled = null,
                        rawPreorder = null,
                        reminderHour = finalH,
                        reminderMinute = finalM,
                        hasTime = finalH != null
                    )
                }

                val allEvents = (activeScheduled + activePreorders)
                    .filter { isUpcomingEvent(it, now) }
                    .sortedBy { it.startDate }

                val todayCal = Calendar.getInstance()
                val dayNameFormat = SimpleDateFormat("EEEE", Locale.getDefault())
                val todayDayName = dayNameFormat.format(todayCal.time).uppercase(Locale.getDefault())
                val todayDayNumber = todayCal.get(Calendar.DAY_OF_MONTH).toString()

                val dayStartCal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val todayStart = dayStartCal.timeInMillis
                val tomorrowStart = todayStart + 24 * 60 * 60 * 1000L
                val sevenDaysOutStart = todayStart + 7 * 24 * 60 * 60 * 1000L

                fun eventCoversDay(event: UpcomingEvent, targetDayStart: Long): Boolean {
                    val s = getDayStart(event.startDate)
                    val e = getDayStart(event.endDate)
                    return targetDayStart in s..e
                }

                val todayEvents = allEvents.filter { eventCoversDay(it, todayStart) }
                val sortedTodayEvents = todayEvents.sortedWith(
                    compareByDescending<UpcomingEvent> { it.hasTime || it.reminderHour != null }
                        .thenBy { it.reminderHour ?: 24 }
                        .thenBy { it.reminderMinute ?: 60 }
                )

                val futureEventsList = allEvents.filter {
                    getDayStart(it.startDate) >= tomorrowStart || getDayStart(it.endDate) >= tomorrowStart
                }

                val views = RemoteViews(context.packageName, R.layout.calendar_bento_widget)

                // Background tint matching user theme
                val isDarkMode = user?.themeMode == "dark"
                val primaryColor = getThemePrimaryColor(user?.themeCombo, isDarkMode)
                views.setInt(R.id.widget_bg_image, "setColorFilter", primaryColor)

                // Open App on Click
                val clickIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    clickIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

                // --- LEFT COLUMN: Today Header & Events ---
                views.setTextViewText(R.id.widget_today_day_name, todayDayName)
                views.setTextViewText(R.id.widget_today_day_number, todayDayNumber)

                if (sortedTodayEvents.size > 1) {
                    views.setViewVisibility(R.id.widget_today_grouped_indicator, View.VISIBLE)
                    views.setTextViewText(R.id.widget_today_grouped_indicator, "${sortedTodayEvents.size} items")
                } else {
                    views.setViewVisibility(R.id.widget_today_grouped_indicator, View.GONE)
                }

                if (sortedTodayEvents.isEmpty()) {
                    views.setViewVisibility(R.id.widget_today_empty, View.VISIBLE)
                    views.setViewVisibility(R.id.widget_today_event_1, View.GONE)
                    views.setViewVisibility(R.id.widget_today_event_2, View.GONE)
                    views.setViewVisibility(R.id.widget_today_more, View.GONE)
                } else {
                    views.setViewVisibility(R.id.widget_today_empty, View.GONE)

                    // Event 1
                    val e1 = sortedTodayEvents[0]
                    views.setViewVisibility(R.id.widget_today_event_1, View.VISIBLE)
                    views.setTextViewText(R.id.widget_today_event_1_title, formatEventTitle(e1, displayAmounts, currency))
                    views.setTextViewText(R.id.widget_today_event_1_time, formatEventSubtitle(e1, todayStart))

                    // Event 2
                    if (sortedTodayEvents.size > 1) {
                        val e2 = sortedTodayEvents[1]
                        views.setViewVisibility(R.id.widget_today_event_2, View.VISIBLE)
                        views.setTextViewText(R.id.widget_today_event_2_title, formatEventTitle(e2, displayAmounts, currency))
                        views.setTextViewText(R.id.widget_today_event_2_time, formatEventSubtitle(e2, todayStart))
                    } else {
                        views.setViewVisibility(R.id.widget_today_event_2, View.GONE)
                    }

                    // More today count
                    if (sortedTodayEvents.size > 2) {
                        views.setViewVisibility(R.id.widget_today_more, View.VISIBLE)
                        views.setTextViewText(R.id.widget_today_more, "+${sortedTodayEvents.size - 2} more")
                    } else {
                        views.setViewVisibility(R.id.widget_today_more, View.GONE)
                    }
                }

                // --- RIGHT COLUMN: Grouped Future Events ---
                if (futureEventsList.isEmpty()) {
                    views.setViewVisibility(R.id.widget_future_empty_container, View.VISIBLE)
                    views.setViewVisibility(R.id.widget_g1_container, View.GONE)
                    views.setViewVisibility(R.id.widget_g2_container, View.GONE)
                    views.setViewVisibility(R.id.widget_g3_container, View.GONE)
                } else {
                    views.setViewVisibility(R.id.widget_future_empty_container, View.GONE)

                    val groupedFuture = futureEventsList.groupBy { event ->
                        val startDay = getDayStart(event.startDate)
                        if (startDay >= tomorrowStart) startDay else tomorrowStart
                    }.toSortedMap()

                    val displayedDayGroups = mutableListOf<WidgetDayGroupInfo>()
                    var remainingItemBudget = 3

                    for ((dayStartTs, eventsForDay) in groupedFuture) {
                        if (remainingItemBudget <= 0) break

                        val isFirstDay = displayedDayGroups.isEmpty()
                        if (eventsForDay.size <= remainingItemBudget) {
                            displayedDayGroups.add(
                                WidgetDayGroupInfo(
                                    dayStartTs = dayStartTs,
                                    shownEvents = eventsForDay,
                                    leftoverCount = 0,
                                    totalCount = eventsForDay.size
                                )
                            )
                            remainingItemBudget -= eventsForDay.size
                        } else {
                            val numToShow = if (isFirstDay) {
                                remainingItemBudget.coerceAtMost(3)
                            } else {
                                1
                            }
                            val shown = eventsForDay.take(numToShow)
                            val leftover = eventsForDay.size - numToShow
                            displayedDayGroups.add(
                                WidgetDayGroupInfo(
                                    dayStartTs = dayStartTs,
                                    shownEvents = shown,
                                    leftoverCount = leftover,
                                    totalCount = eventsForDay.size
                                )
                            )
                            remainingItemBudget = 0
                        }
                    }

                    val dayOfWeekFormat = SimpleDateFormat("EEEE", Locale.getDefault())
                    val dayMonthFormat = SimpleDateFormat("d MMM", Locale.getDefault())

                    fun getHeaderTitle(dayStartTs: Long): String {
                        return when {
                            dayStartTs == tomorrowStart -> "TOMORROW"
                            dayStartTs < sevenDaysOutStart -> dayOfWeekFormat.format(Date(dayStartTs)).uppercase(Locale.getDefault())
                            else -> dayMonthFormat.format(Date(dayStartTs)).uppercase(Locale.getDefault())
                        }
                    }

                    // Bind Group 1
                    if (displayedDayGroups.isNotEmpty()) {
                        val g1 = displayedDayGroups[0]
                        views.setViewVisibility(R.id.widget_g1_container, View.VISIBLE)
                        views.setTextViewText(R.id.widget_g1_header, getHeaderTitle(g1.dayStartTs))
                        if (g1.totalCount > 1) {
                            views.setViewVisibility(R.id.widget_g1_count, View.VISIBLE)
                            views.setTextViewText(R.id.widget_g1_count, "${g1.totalCount} items")
                        } else {
                            views.setViewVisibility(R.id.widget_g1_count, View.GONE)
                        }

                        // Event 1
                        val fe1 = g1.shownEvents[0]
                        views.setViewVisibility(R.id.widget_g1_e1, View.VISIBLE)
                        views.setTextViewText(R.id.widget_g1_e1_title, formatEventTitle(fe1, displayAmounts, currency))
                        views.setTextViewText(R.id.widget_g1_e1_time, formatEventSubtitle(fe1, g1.dayStartTs))

                        // Event 2
                        if (g1.shownEvents.size > 1) {
                            val fe2 = g1.shownEvents[1]
                            views.setViewVisibility(R.id.widget_g1_e2, View.VISIBLE)
                            views.setTextViewText(R.id.widget_g1_e2_title, formatEventTitle(fe2, displayAmounts, currency))
                            views.setTextViewText(R.id.widget_g1_e2_time, formatEventSubtitle(fe2, g1.dayStartTs))
                        } else {
                            views.setViewVisibility(R.id.widget_g1_e2, View.GONE)
                        }

                        // Event 3
                        if (g1.shownEvents.size > 2) {
                            val fe3 = g1.shownEvents[2]
                            views.setViewVisibility(R.id.widget_g1_e3, View.VISIBLE)
                            views.setTextViewText(R.id.widget_g1_e3_title, formatEventTitle(fe3, displayAmounts, currency))
                            views.setTextViewText(R.id.widget_g1_e3_time, formatEventSubtitle(fe3, g1.dayStartTs))
                        } else {
                            views.setViewVisibility(R.id.widget_g1_e3, View.GONE)
                        }

                        // More
                        if (g1.leftoverCount > 0) {
                            views.setViewVisibility(R.id.widget_g1_more, View.VISIBLE)
                            views.setTextViewText(R.id.widget_g1_more, "+${g1.leftoverCount} more")
                        } else {
                            views.setViewVisibility(R.id.widget_g1_more, View.GONE)
                        }
                    } else {
                        views.setViewVisibility(R.id.widget_g1_container, View.GONE)
                    }

                    // Bind Group 2
                    if (displayedDayGroups.size > 1) {
                        val g2 = displayedDayGroups[1]
                        views.setViewVisibility(R.id.widget_g2_container, View.VISIBLE)
                        views.setTextViewText(R.id.widget_g2_header, getHeaderTitle(g2.dayStartTs))
                        if (g2.totalCount > 1) {
                            views.setViewVisibility(R.id.widget_g2_count, View.VISIBLE)
                            views.setTextViewText(R.id.widget_g2_count, "${g2.totalCount} items")
                        } else {
                            views.setViewVisibility(R.id.widget_g2_count, View.GONE)
                        }

                        // Event 1
                        val fe1 = g2.shownEvents[0]
                        views.setViewVisibility(R.id.widget_g2_e1, View.VISIBLE)
                        views.setTextViewText(R.id.widget_g2_e1_title, formatEventTitle(fe1, displayAmounts, currency))
                        views.setTextViewText(R.id.widget_g2_e1_time, formatEventSubtitle(fe1, g2.dayStartTs))

                        // Event 2
                        if (g2.shownEvents.size > 1) {
                            val fe2 = g2.shownEvents[1]
                            views.setViewVisibility(R.id.widget_g2_e2, View.VISIBLE)
                            views.setTextViewText(R.id.widget_g2_e2_title, formatEventTitle(fe2, displayAmounts, currency))
                            views.setTextViewText(R.id.widget_g2_e2_time, formatEventSubtitle(fe2, g2.dayStartTs))
                        } else {
                            views.setViewVisibility(R.id.widget_g2_e2, View.GONE)
                        }

                        // More
                        if (g2.leftoverCount > 0) {
                            views.setViewVisibility(R.id.widget_g2_more, View.VISIBLE)
                            views.setTextViewText(R.id.widget_g2_more, "+${g2.leftoverCount} more")
                        } else {
                            views.setViewVisibility(R.id.widget_g2_more, View.GONE)
                        }
                    } else {
                        views.setViewVisibility(R.id.widget_g2_container, View.GONE)
                    }

                    // Bind Group 3
                    if (displayedDayGroups.size > 2) {
                        val g3 = displayedDayGroups[2]
                        views.setViewVisibility(R.id.widget_g3_container, View.VISIBLE)
                        views.setTextViewText(R.id.widget_g3_header, getHeaderTitle(g3.dayStartTs))
                        if (g3.totalCount > 1) {
                            views.setViewVisibility(R.id.widget_g3_count, View.VISIBLE)
                            views.setTextViewText(R.id.widget_g3_count, "${g3.totalCount} items")
                        } else {
                            views.setViewVisibility(R.id.widget_g3_count, View.GONE)
                        }

                        // Event 1
                        val fe1 = g3.shownEvents[0]
                        views.setViewVisibility(R.id.widget_g3_e1, View.VISIBLE)
                        views.setTextViewText(R.id.widget_g3_e1_title, formatEventTitle(fe1, displayAmounts, currency))
                        views.setTextViewText(R.id.widget_g3_e1_time, formatEventSubtitle(fe1, g3.dayStartTs))

                        // More
                        if (g3.leftoverCount > 0) {
                            views.setViewVisibility(R.id.widget_g3_more, View.VISIBLE)
                            views.setTextViewText(R.id.widget_g3_more, "+${g3.leftoverCount} more")
                        } else {
                            views.setViewVisibility(R.id.widget_g3_more, View.GONE)
                        }
                    } else {
                        views.setViewVisibility(R.id.widget_g3_container, View.GONE)
                    }
                }

                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                android.util.Log.e("CalendarBentoWidget", "Error updating widget", e)
            } finally {
                pendingResult?.finish()
            }
        }
    }

    private data class WidgetDayGroupInfo(
        val dayStartTs: Long,
        val shownEvents: List<UpcomingEvent>,
        val leftoverCount: Int,
        val totalCount: Int
    )

    private fun getDayStart(ts: Long): Long {
        val c = Calendar.getInstance().apply {
            timeInMillis = ts
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return c.timeInMillis
    }

    private fun formatEventTitle(event: UpcomingEvent, userDisplayAmounts: Boolean, currency: String): String {
        val displayTitle = event.title.ifBlank { event.subTitle }
        val priceStr = if (userDisplayAmounts && event.price != null && event.price > 0) " (${currency}${String.format(Locale.US, "%.2f", event.price)})" else ""
        return "$displayTitle$priceStr"
    }

    private fun formatEventSubtitle(event: UpcomingEvent, targetDayStart: Long): String {
        val eventStartDay = getDayStart(event.startDate)
        val eventEndDay = getDayStart(event.endDate)
        val hasTime = event.hasTime || event.reminderHour != null
        val reminderH = event.reminderHour ?: 8
        val reminderM = event.reminderMinute ?: 0
        val timeStr = String.format(Locale.US, "%d:%02d", reminderH, reminderM)

        return if (eventStartDay == eventEndDay) {
            if (hasTime) "Starts at $timeStr" else "All day"
        } else {
            when (targetDayStart) {
                eventStartDay -> if (hasTime) "Starts at $timeStr" else "Starts at 8:00"
                eventEndDay -> if (hasTime) "Until $timeStr" else "Until 20:00"
                else -> "All day"
            }
        }
    }

    private fun getThemePrimaryColor(themeCombo: String?, isDark: Boolean): Int {
        return when (themeCombo?.lowercase(Locale.getDefault())) {
            "summer" -> if (isDark) 0xFFE0B48A.toInt() else 0xFFD4A373.toInt()
            "cozy" -> if (isDark) 0xFFEBAF94.toInt() else 0xFFD19E82.toInt()
            "winter" -> if (isDark) 0xFFDDD3C1.toInt() else 0xFFC4B69E.toInt()
            "serenity" -> if (isDark) 0xFFE6D7AB.toInt() else 0xFFD4C294.toInt()
            "green_serenity" -> if (isDark) 0xFFA1CCA5.toInt() else 0xFF588157.toInt()
            "blood_moon" -> if (isDark) 0xFF9DC7C8.toInt() else 0xFF6A3937.toInt()
            "earthy_harmony" -> if (isDark) 0xFFDDC8C4.toInt() else 0xFF896A67.toInt()
            "chocolate_sunset" -> if (isDark) 0xFFCB9173.toInt() else 0xFF8E443D.toInt()
            "red_sunburst" -> if (isDark) 0xFFCE4257.toInt() else 0xFFCE4257.toInt()
            "earthy_forest" -> if (isDark) 0xFFA3B18A.toInt() else 0xFF344E41.toInt()
            "deep_sea" -> if (isDark) 0xFF778DA9.toInt() else 0xFF415A77.toInt()
            "soft_lavender" -> if (isDark) 0xFFC9ADA7.toInt() else 0xFF4A4E69.toInt()
            "default" -> if (isDark) 0xFFD0BCFF.toInt() else 0xFF6650A4.toInt()
            else -> 0xFFD4A373.toInt() // Golden Summer / default
        }
    }

    companion object {
        fun triggerUpdate(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, CalendarBentoWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                    val provider = CalendarBentoWidgetProvider()
                    for (widgetId in appWidgetIds) {
                        provider.updateAppWidget(context, appWidgetManager, widgetId)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("CalendarBentoWidget", "Error triggering widget update", e)
            }
        }
    }
}
