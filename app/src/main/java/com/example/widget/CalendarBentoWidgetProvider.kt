package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
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

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = BookishDatabase.getDatabase(context)
                val user = db.userDao().getUser().firstOrNull()
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
                    val title = subType?.title?.ifBlank { null } ?: sched.bookTitle
                    val subTitle = store?.name?.trim() ?: title
                    val startDt = sched.dueDate
                    val isSubReminderDDay = subType?.reminderEnabled == true && subType.reminderDDayOffset == 0
                    val reminderH = if (isSubReminderDDay) subType?.reminderHour else null
                    val reminderM = if (isSubReminderDDay) subType?.reminderMinute else null
                    UpcomingEvent(
                        id = "sched_${sched.id}",
                        title = title,
                        author = sched.bookAuthor,
                        date = startDt,
                        startDate = startDt,
                        endDate = startDt,
                        type = "scheduled",
                        status = sched.status,
                        isSkipped = false,
                        subTitle = subTitle,
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
                        subTitle = store?.name?.trim() ?: "",
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
                val todayDayName = SimpleDateFormat("EEEE", Locale.getDefault()).format(todayCal.time).uppercase(Locale.getDefault())
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

                val todayEvents = allEvents.filter { event ->
                    val startDay = getDayStart(event.startDate)
                    val endDay = getDayStart(event.endDate)
                    (todayStart in startDay..endDay) || (event.startDate in todayStart until tomorrowStart)
                }

                val futureEvents = allEvents.filter { event ->
                    val startDay = getDayStart(event.startDate)
                    val endDay = getDayStart(event.endDate)
                    startDay >= tomorrowStart || endDay >= tomorrowStart
                }

                val views = RemoteViews(context.packageName, R.layout.calendar_bento_widget)

                // Background tint
                val primaryColor = getThemePrimaryColor(user?.themeCombo)
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

                // Left Column - Today Header
                views.setTextViewText(R.id.widget_today_day_name, todayDayName)
                views.setTextViewText(R.id.widget_today_day_number, todayDayNumber)

                if (todayEvents.isEmpty()) {
                    views.setViewVisibility(R.id.widget_today_empty, View.VISIBLE)
                    views.setViewVisibility(R.id.widget_today_event_1, View.GONE)
                    views.setViewVisibility(R.id.widget_today_event_2, View.GONE)
                    views.setViewVisibility(R.id.widget_today_more, View.GONE)
                } else {
                    views.setViewVisibility(R.id.widget_today_empty, View.GONE)

                    // Event 1
                    val e1 = todayEvents[0]
                    views.setViewVisibility(R.id.widget_today_event_1, View.VISIBLE)
                    views.setTextViewText(R.id.widget_today_event_1_title, formatEventTitle(e1, displayAmounts, currency))
                    views.setTextViewText(R.id.widget_today_event_1_time, formatEventSubtitle(e1, todayStart))

                    // Event 2
                    if (todayEvents.size > 1) {
                        val e2 = todayEvents[1]
                        views.setViewVisibility(R.id.widget_today_event_2, View.VISIBLE)
                        views.setTextViewText(R.id.widget_today_event_2_title, formatEventTitle(e2, displayAmounts, currency))
                        views.setTextViewText(R.id.widget_today_event_2_time, formatEventSubtitle(e2, todayStart))
                    } else {
                        views.setViewVisibility(R.id.widget_today_event_2, View.GONE)
                    }

                    // More today count
                    if (todayEvents.size > 2) {
                        views.setViewVisibility(R.id.widget_today_more, View.VISIBLE)
                        views.setTextViewText(R.id.widget_today_more, "+${todayEvents.size - 2} more")
                    } else {
                        views.setViewVisibility(R.id.widget_today_more, View.GONE)
                    }
                }

                // Right Column - Future Header
                if (futureEvents.isEmpty()) {
                    views.setTextViewText(R.id.widget_future_header, "UPCOMING")
                    views.setViewVisibility(R.id.widget_future_empty, View.VISIBLE)
                    views.setViewVisibility(R.id.widget_future_event_1, View.GONE)
                    views.setViewVisibility(R.id.widget_future_event_2, View.GONE)
                    views.setViewVisibility(R.id.widget_future_event_3, View.GONE)
                    views.setViewVisibility(R.id.widget_future_more, View.GONE)
                } else {
                    views.setViewVisibility(R.id.widget_future_empty, View.GONE)

                    val firstFutureDay = getDayStart(futureEvents[0].startDate)
                    val headerText = when {
                        firstFutureDay == tomorrowStart -> "TOMORROW"
                        firstFutureDay < sevenDaysOutStart -> SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(firstFutureDay)).uppercase(Locale.getDefault())
                        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(firstFutureDay)).uppercase(Locale.getDefault())
                    }
                    views.setTextViewText(R.id.widget_future_header, headerText)

                    // Future Event 1
                    val fe1 = futureEvents[0]
                    views.setViewVisibility(R.id.widget_future_event_1, View.VISIBLE)
                    views.setTextViewText(R.id.widget_future_event_1_title, formatEventTitle(fe1, displayAmounts, currency))
                    views.setTextViewText(R.id.widget_future_event_1_time, formatEventSubtitle(fe1, firstFutureDay))

                    // Future Event 2
                    if (futureEvents.size > 1) {
                        val fe2 = futureEvents[1]
                        views.setViewVisibility(R.id.widget_future_event_2, View.VISIBLE)
                        views.setTextViewText(R.id.widget_future_event_2_title, formatEventTitle(fe2, displayAmounts, currency))
                        views.setTextViewText(R.id.widget_future_event_2_time, formatEventSubtitle(fe2, firstFutureDay))
                    } else {
                        views.setViewVisibility(R.id.widget_future_event_2, View.GONE)
                    }

                    // Future Event 3
                    if (futureEvents.size > 2) {
                        val fe3 = futureEvents[2]
                        views.setViewVisibility(R.id.widget_future_event_3, View.VISIBLE)
                        views.setTextViewText(R.id.widget_future_event_3_title, formatEventTitle(fe3, displayAmounts, currency))
                        views.setTextViewText(R.id.widget_future_event_3_time, formatEventSubtitle(fe3, firstFutureDay))
                    } else {
                        views.setViewVisibility(R.id.widget_future_event_3, View.GONE)
                    }

                    // More future count
                    if (futureEvents.size > 3) {
                        views.setViewVisibility(R.id.widget_future_more, View.VISIBLE)
                        views.setTextViewText(R.id.widget_future_more, "+${futureEvents.size - 3} more")
                    } else {
                        views.setViewVisibility(R.id.widget_future_more, View.GONE)
                    }
                }

                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                android.util.Log.e("CalendarBentoWidget", "Error updating widget", e)
            }
        }
    }

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

    private fun getThemePrimaryColor(themeCombo: String?): Int {
        return when (themeCombo?.lowercase(Locale.getDefault())) {
            "forest" -> 0xFF588157.toInt()
            "crimson", "sunset" -> 0xFFCE4257.toInt()
            "night", "ocean", "blue", "midnight" -> 0xFF415A77.toInt()
            "rose", "blush" -> 0xFFD19E82.toInt()
            "lavender", "plum" -> 0xFF6A3937.toInt()
            "coffee", "earth", "chocolate" -> 0xFF8E443D.toInt()
            "sage" -> 0xFF344E41.toInt()
            "sand", "linen" -> 0xFFC4B69E.toInt()
            "classic", "purple" -> 0xFF6200EE.toInt()
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
                    val intent = Intent(context, CalendarBentoWidgetProvider::class.java).apply {
                        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                    }
                    context.sendBroadcast(intent)
                }
            } catch (e: Exception) {
                android.util.Log.e("CalendarBentoWidget", "Error triggering widget update", e)
            }
        }
    }
}
