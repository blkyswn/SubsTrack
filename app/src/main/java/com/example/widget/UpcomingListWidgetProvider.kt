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

class UpcomingListWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        updateWidgetsInternal(context, appWidgetManager, appWidgetIds, pendingResult)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_BOOT_COMPLETED -> {
                triggerUpdate(context)
            }
        }
    }

    private fun updateWidgetsInternal(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
        pendingResult: PendingResult?
    ) {
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

                    val bookAndAuthor = buildString {
                        if (sched.bookTitle.isNotBlank() && sched.bookTitle != subTypeName) {
                            append(sched.bookTitle)
                        }
                        if (sched.bookAuthor.isNotBlank()) {
                            if (isNotEmpty()) append(" by ") else append("By ")
                            append(sched.bookAuthor)
                        }
                    }
                    val subtitle = if (bookAndAuthor.isNotBlank()) {
                        bookAndAuthor
                    } else if (bookstoreName.isNotBlank()) {
                        bookstoreName
                    } else {
                        subTypeName
                    }

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
                        subTitle = subtitle,
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

                    val subtitle = if (pr.bookAuthor.isNotBlank()) {
                        "By ${pr.bookAuthor}"
                    } else if (bookstoreName.isNotBlank()) {
                        bookstoreName
                    } else {
                        ""
                    }

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
                        subTitle = subtitle,
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

                val next5 = allEvents.take(5)

                val themeColors = WidgetThemeUtils.getThemeColors(context, user)
                val isDark = WidgetThemeUtils.isDarkTheme(context, user)

                val views = RemoteViews(context.packageName, R.layout.upcoming_list_widget)

                // Background tint matching user theme surface
                views.setInt(R.id.widget_list_bg, "setColorFilter", themeColors.surface)

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
                views.setOnClickPendingIntent(R.id.widget_list_root, pendingIntent)

                // Header styling matching app
                views.setTextColor(R.id.widget_list_header_title, themeColors.onSurface)
                views.setTextColor(R.id.widget_list_header_subtitle, themeColors.onSurfaceVariant)
                views.setInt(R.id.widget_list_header_icon, "setColorFilter", themeColors.primary)

                // Empty state styling
                views.setTextColor(R.id.widget_list_empty_title, themeColors.onSurface)
                views.setTextColor(R.id.widget_list_empty_subtitle, themeColors.onSurfaceVariant)

                if (next5.isEmpty()) {
                    views.setViewVisibility(R.id.widget_list_empty, View.VISIBLE)
                    views.setViewVisibility(R.id.widget_list_items, View.GONE)
                } else {
                    views.setViewVisibility(R.id.widget_list_empty, View.GONE)
                    views.setViewVisibility(R.id.widget_list_items, View.VISIBLE)

                    val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
                    val dayFormat = SimpleDateFormat("dd", Locale.getDefault())

                    val itemIds = intArrayOf(
                        R.id.widget_item_1,
                        R.id.widget_item_2,
                        R.id.widget_item_3,
                        R.id.widget_item_4,
                        R.id.widget_item_5
                    )
                    val barIds = intArrayOf(
                        R.id.widget_bar_1,
                        R.id.widget_bar_2,
                        R.id.widget_bar_3,
                        R.id.widget_bar_4,
                        R.id.widget_bar_5
                    )
                    val catIds = intArrayOf(
                        R.id.widget_category_1,
                        R.id.widget_category_2,
                        R.id.widget_category_3,
                        R.id.widget_category_4,
                        R.id.widget_category_5
                    )
                    val statusIds = intArrayOf(
                        R.id.widget_status_1,
                        R.id.widget_status_2,
                        R.id.widget_status_3,
                        R.id.widget_status_4,
                        R.id.widget_status_5
                    )
                    val titleIds = intArrayOf(
                        R.id.widget_title_1,
                        R.id.widget_title_2,
                        R.id.widget_title_3,
                        R.id.widget_title_4,
                        R.id.widget_title_5
                    )
                    val subtitleIds = intArrayOf(
                        R.id.widget_subtitle_1,
                        R.id.widget_subtitle_2,
                        R.id.widget_subtitle_3,
                        R.id.widget_subtitle_4,
                        R.id.widget_subtitle_5
                    )
                    val dateMonthIds = intArrayOf(
                        R.id.widget_date_month_1,
                        R.id.widget_date_month_2,
                        R.id.widget_date_month_3,
                        R.id.widget_date_month_4,
                        R.id.widget_date_month_5
                    )
                    val dateDayIds = intArrayOf(
                        R.id.widget_date_day_1,
                        R.id.widget_date_day_2,
                        R.id.widget_date_day_3,
                        R.id.widget_date_day_4,
                        R.id.widget_date_day_5
                    )
                    val dateTimeIds = intArrayOf(
                        R.id.widget_date_time_1,
                        R.id.widget_date_time_2,
                        R.id.widget_date_time_3,
                        R.id.widget_date_time_4,
                        R.id.widget_date_time_5
                    )

                    for (i in 0 until 5) {
                        if (i < next5.size) {
                            val event = next5[i]
                            views.setViewVisibility(itemIds[i], View.VISIBLE)

                            val accentColor = if (event.type == "scheduled") {
                                themeColors.primary
                            } else {
                                themeColors.tertiary
                            }

                            // Left vertical indicator bar
                            views.setInt(barIds[i], "setColorFilter", accentColor)

                            // Category mini chip
                            val catText = if (event.type == "scheduled") "Subscription" else "Preorder"
                            views.setTextViewText(catIds[i], catText)
                            views.setTextColor(catIds[i], accentColor)

                            // Status mini badge
                            val statusLower = event.status.lowercase(Locale.getDefault())
                            val statusTextColor = when (statusLower) {
                                "upcoming" -> if (isDark) 0xFFF1F5F9.toInt() else 0xFF334155.toInt()
                                "released" -> if (isDark) 0xFFFFEDD5.toInt() else 0xFF9A3412.toInt()
                                "wishlist" -> if (isDark) 0xFFFCE7F3.toInt() else 0xFF9D174D.toInt()
                                "renewed", "paid", "preordered" -> if (isDark) 0xFFDBEAFE.toInt() else 0xFF1E40AF.toInt()
                                "forwarded" -> if (isDark) 0xFFE0F2FE.toInt() else 0xFF0369A1.toInt()
                                "in suite" -> if (isDark) 0xFFFEF3C7.toInt() else 0xFF92400E.toInt()
                                "skipped" -> if (isDark) 0xFFFEE2E2.toInt() else 0xFF991B1B.toInt()
                                "shipped" -> if (isDark) 0xFFF3E8FF.toInt() else 0xFF5B21B6.toInt()
                                "received" -> if (isDark) 0xFFD1FAE5.toInt() else 0xFF065F46.toInt()
                                else -> themeColors.onSurfaceVariant
                            }
                            views.setTextViewText(statusIds[i], event.status)
                            views.setTextColor(statusIds[i], statusTextColor)

                            // Title & Price
                            val displayTitle = event.title.ifBlank { event.subTitle }
                            val priceStr = if (displayAmounts && event.price != null && event.price > 0) {
                                " (${currency}${String.format(Locale.US, "%.2f", event.price)})"
                            } else ""
                            views.setTextViewText(titleIds[i], "$displayTitle$priceStr")
                            views.setTextColor(titleIds[i], themeColors.onSurface)

                            // Subtitle
                            views.setTextViewText(subtitleIds[i], event.subTitle)
                            views.setTextColor(subtitleIds[i], themeColors.onSurfaceVariant)

                            // Date Badge
                            val dateObj = Date(event.date)
                            val monthStr = monthFormat.format(dateObj).uppercase(Locale.getDefault())
                            val dayStr = dayFormat.format(dateObj)
                            views.setTextViewText(dateMonthIds[i], monthStr)
                            views.setTextColor(dateMonthIds[i], accentColor)
                            views.setTextViewText(dateDayIds[i], dayStr)
                            views.setTextColor(dateDayIds[i], themeColors.onSurface)

                            // Time if available
                            val hasTime = event.hasTime || event.reminderHour != null
                            if (hasTime) {
                                val h = event.reminderHour ?: 8
                                val m = event.reminderMinute ?: 0
                                views.setTextViewText(dateTimeIds[i], String.format(Locale.US, "%02d:%02d", h, m))
                                views.setTextColor(dateTimeIds[i], themeColors.onSurfaceVariant)
                                views.setViewVisibility(dateTimeIds[i], View.VISIBLE)
                            } else {
                                views.setViewVisibility(dateTimeIds[i], View.GONE)
                            }
                        } else {
                            views.setViewVisibility(itemIds[i], View.GONE)
                        }
                    }
                }

                for (widgetId in appWidgetIds) {
                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            } catch (e: Exception) {
                android.util.Log.e("UpcomingListWidget", "Error updating widget", e)
            } finally {
                pendingResult?.finish()
            }
        }
    }

    companion object {
        fun triggerUpdate(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, UpcomingListWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                    val provider = UpcomingListWidgetProvider()
                    provider.updateWidgetsInternal(context, appWidgetManager, appWidgetIds, null)
                }
            } catch (e: Exception) {
                android.util.Log.e("UpcomingListWidget", "Error triggering update", e)
            }
        }
    }
}
