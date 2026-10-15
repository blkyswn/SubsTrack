package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.BookishDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class UpcomingCalendarWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        updateWidgetsInternal(context, appWidgetManager, appWidgetIds, pendingResult)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        val minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
        val isMonthlyBySize = minHeight >= 180

        val prefs = getPrefs(context)
        prefs.edit()
            .putBoolean(KEY_IS_MONTHLY + "_" + appWidgetId, isMonthlyBySize)
            .apply()

        updateWidgetsInternal(context, appWidgetManager, intArrayOf(appWidgetId), null)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val prefs = getPrefs(context)

        when (action) {
            ACTION_CALENDAR_WIDGET_PREV -> {
                val isMonthly = isMonthlyMode(context, appWidgetId)
                if (isMonthly) {
                    val currentMonthOffset = prefs.getInt(KEY_MONTH_OFFSET + "_" + appWidgetId, 0)
                    prefs.edit().putInt(KEY_MONTH_OFFSET + "_" + appWidgetId, currentMonthOffset - 1).apply()
                } else {
                    val currentWeekOffset = prefs.getInt(KEY_WEEK_OFFSET + "_" + appWidgetId, 0)
                    prefs.edit().putInt(KEY_WEEK_OFFSET + "_" + appWidgetId, currentWeekOffset - 1).apply()
                }
                triggerUpdate(context)
            }
            ACTION_CALENDAR_WIDGET_NEXT -> {
                val isMonthly = isMonthlyMode(context, appWidgetId)
                if (isMonthly) {
                    val currentMonthOffset = prefs.getInt(KEY_MONTH_OFFSET + "_" + appWidgetId, 0)
                    prefs.edit().putInt(KEY_MONTH_OFFSET + "_" + appWidgetId, currentMonthOffset + 1).apply()
                } else {
                    val currentWeekOffset = prefs.getInt(KEY_WEEK_OFFSET + "_" + appWidgetId, 0)
                    prefs.edit().putInt(KEY_WEEK_OFFSET + "_" + appWidgetId, currentWeekOffset + 1).apply()
                }
                triggerUpdate(context)
            }
            ACTION_CALENDAR_WIDGET_TOGGLE -> {
                val currentMode = isMonthlyMode(context, appWidgetId)
                prefs.edit().putBoolean(KEY_IS_MONTHLY + "_" + appWidgetId, !currentMode).apply()
                triggerUpdate(context)
            }
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
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
                val user = db.userDao().getUserDirect()
                val scheduledSubs = db.scheduledSubscriptionDao().getAllScheduledSubscriptionsList()
                val preorders = db.preorderDao().getAllPreordersList()

                val dateFormatKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val todayStr = dateFormatKey.format(Date())

                // Aggregate event markers per date: dateStr -> list of colors
                val eventMarkerMap = mutableMapOf<String, MutableList<Int>>()

                scheduledSubs.forEach { item ->
                    if (!item.isSkipped && !item.status.equals("Skipped", ignoreCase = true)) {
                        val dStr = dateFormatKey.format(Date(item.dueDate))
                        val color = when (item.status.lowercase(Locale.getDefault())) {
                            "upcoming" -> 0xFF64748B.toInt()
                            "renewed", "paid" -> 0xFF2563EB.toInt()
                            "forwarded" -> 0xFF0EA5E9.toInt()
                            "in suite" -> 0xFFD97706.toInt()
                            "shipped" -> 0xFF8B5CF6.toInt()
                            "received" -> 0xFF10B981.toInt()
                            else -> 0xFF2563EB.toInt()
                        }
                        eventMarkerMap.getOrPut(dStr) { mutableListOf() }.add(color)
                    }
                }

                preorders.forEach { item ->
                    if (!item.status.equals("Cancelled", ignoreCase = true) && !item.status.equals("Canceled", ignoreCase = true)) {
                        val dStr = dateFormatKey.format(Date(item.rangedSaleDateStart))
                        val color = when (item.status.lowercase(Locale.getDefault())) {
                            "upcoming" -> 0xFF64748B.toInt()
                            "preordered" -> 0xFF2563EB.toInt()
                            "released" -> 0xFFF97316.toInt()
                            "forwarded" -> 0xFF0EA5E9.toInt()
                            "in suite" -> 0xFFD97706.toInt()
                            "shipped" -> 0xFF8B5CF6.toInt()
                            "received" -> 0xFF10B981.toInt()
                            else -> 0xFF2563EB.toInt()
                        }
                        eventMarkerMap.getOrPut(dStr) { mutableListOf() }.add(color)
                    }
                }

                val themePrimaryColor = getThemePrimaryColor(user?.themeCombo, false)

                for (widgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.upcoming_calendar_widget)
                    views.setInt(R.id.widget_cal_bg_card, "setColorFilter", 0xFFFBF8F5.toInt())

                    val isMonthly = isMonthlyMode(context, widgetId)
                    val prefs = getPrefs(context)
                    val monthOffset = prefs.getInt(KEY_MONTH_OFFSET + "_" + widgetId, 0)
                    val weekOffset = prefs.getInt(KEY_WEEK_OFFSET + "_" + widgetId, 0)

                    // Navigation pending intents
                    views.setOnClickPendingIntent(
                        R.id.widget_cal_prev,
                        createNavPendingIntent(context, widgetId, ACTION_CALENDAR_WIDGET_PREV, 1000 + widgetId)
                    )
                    views.setOnClickPendingIntent(
                        R.id.widget_cal_next,
                        createNavPendingIntent(context, widgetId, ACTION_CALENDAR_WIDGET_NEXT, 2000 + widgetId)
                    )
                    views.setOnClickPendingIntent(
                        R.id.widget_cal_toggle,
                        createNavPendingIntent(context, widgetId, ACTION_CALENDAR_WIDGET_TOGGLE, 3000 + widgetId)
                    )

                    // Header title click opens UpcomingCalendarScreen for today
                    views.setOnClickPendingIntent(
                        R.id.widget_cal_title,
                        createDayClickPendingIntent(context, todayStr, 4000 + widgetId)
                    )

                    if (isMonthly) {
                        // FULL CALENDAR (MONTHLY) VIEW
                        views.setViewVisibility(R.id.widget_weekly_container, View.GONE)
                        views.setViewVisibility(R.id.widget_monthly_container, View.VISIBLE)
                        views.setImageViewResource(R.id.widget_cal_toggle, R.drawable.ic_expand_less)

                        val calMonth = Calendar.getInstance().apply {
                            set(Calendar.DAY_OF_MONTH, 1)
                            add(Calendar.MONTH, monthOffset)
                        }

                        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                        views.setTextViewText(R.id.widget_cal_title, monthFormat.format(calMonth.time))

                        val daysInMonth = calMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
                        val firstDayOfWeek = calMonth.get(Calendar.DAY_OF_WEEK) // 1 = Sun, 2 = Mon
                        val leadingEmptyDays = firstDayOfWeek - 1

                        val totalCellsNeeded = leadingEmptyDays + daysInMonth
                        val rowsNeeded = (totalCellsNeeded + 6) / 7

                        views.setViewVisibility(R.id.widget_mrow_6, if (rowsNeeded > 5) View.VISIBLE else View.GONE)

                        for (cellIndex in 0 until 42) {
                            val cellNum = cellIndex + 1
                            val mcellId = context.resources.getIdentifier("widget_mcell_$cellNum", "id", context.packageName)
                            val mdayId = context.resources.getIdentifier("widget_mday_$cellNum", "id", context.packageName)
                            val mtodayId = context.resources.getIdentifier("widget_mtoday_$cellNum", "id", context.packageName)
                            val mdotId = context.resources.getIdentifier("widget_mdot_$cellNum", "id", context.packageName)
                            val mbgId = context.resources.getIdentifier("widget_mbg_$cellNum", "id", context.packageName)

                            val dayNumber = cellIndex - leadingEmptyDays + 1
                            if (cellIndex < leadingEmptyDays || dayNumber > daysInMonth) {
                                views.setViewVisibility(mcellId, View.INVISIBLE)
                            } else {
                                views.setViewVisibility(mcellId, View.VISIBLE)
                                val cellCal = calMonth.clone() as Calendar
                                cellCal.set(Calendar.DAY_OF_MONTH, dayNumber)
                                val dateStr = dateFormatKey.format(cellCal.time)
                                val isToday = dateStr == todayStr
                                val markers = eventMarkerMap[dateStr] ?: emptyList()

                                views.setTextViewText(mdayId, dayNumber.toString())

                                if (isToday) {
                                    views.setViewVisibility(mtodayId, View.VISIBLE)
                                    views.setInt(mtodayId, "setColorFilter", themePrimaryColor)
                                    views.setTextColor(mdayId, 0xFFFFFFFF.toInt())
                                } else {
                                    views.setViewVisibility(mtodayId, View.GONE)
                                    views.setTextColor(mdayId, 0xFF1F1F1F.toInt())
                                }

                                if (markers.isNotEmpty()) {
                                    views.setViewVisibility(mdotId, View.VISIBLE)
                                    views.setInt(mdotId, "setColorFilter", markers.first())
                                    views.setViewVisibility(mbgId, View.VISIBLE)
                                    views.setInt(mbgId, "setColorFilter", 0x1A2563EB)
                                } else {
                                    views.setViewVisibility(mdotId, View.GONE)
                                    views.setViewVisibility(mbgId, View.GONE)
                                }

                                // Day click intent opening upcoming calendar screen
                                views.setOnClickPendingIntent(
                                    mcellId,
                                    createDayClickPendingIntent(context, dateStr, (widgetId * 100) + cellNum)
                                )
                            }
                        }
                    } else {
                        // WEEKLY VIEW
                        views.setViewVisibility(R.id.widget_weekly_container, View.VISIBLE)
                        views.setViewVisibility(R.id.widget_monthly_container, View.GONE)
                        views.setImageViewResource(R.id.widget_cal_toggle, R.drawable.ic_expand_more)

                        val calWeek = Calendar.getInstance().apply {
                            val dayOfWeek = get(Calendar.DAY_OF_WEEK)
                            add(Calendar.DAY_OF_MONTH, -(dayOfWeek - 1) + (weekOffset * 7))
                        }

                        val weekStartCal = calWeek.clone() as Calendar
                        val weekEndCal = calWeek.clone() as Calendar
                        weekEndCal.add(Calendar.DAY_OF_MONTH, 6)

                        val weekTitleFormat = SimpleDateFormat("MMM d", Locale.getDefault())
                        val weekTitle = "${weekTitleFormat.format(weekStartCal.time)} – ${weekTitleFormat.format(weekEndCal.time)}, ${weekEndCal.get(Calendar.YEAR)}"
                        views.setTextViewText(R.id.widget_cal_title, weekTitle)

                        for (c in 0 until 7) {
                            val cellNum = c + 1
                            val wcellId = context.resources.getIdentifier("widget_wcell_$cellNum", "id", context.packageName)
                            val wdayId = context.resources.getIdentifier("widget_wday_$cellNum", "id", context.packageName)
                            val wtodayId = context.resources.getIdentifier("widget_wtoday_$cellNum", "id", context.packageName)
                            val wbgId = context.resources.getIdentifier("widget_wbg_$cellNum", "id", context.packageName)
                            val wdot1Id = context.resources.getIdentifier("widget_wdot1_$cellNum", "id", context.packageName)
                            val wdot2Id = context.resources.getIdentifier("widget_wdot2_$cellNum", "id", context.packageName)
                            val wdot3Id = context.resources.getIdentifier("widget_wdot3_$cellNum", "id", context.packageName)

                            val dayCal = calWeek.clone() as Calendar
                            dayCal.add(Calendar.DAY_OF_MONTH, c)
                            val dateStr = dateFormatKey.format(dayCal.time)
                            val dayNumber = dayCal.get(Calendar.DAY_OF_MONTH)
                            val isToday = dateStr == todayStr
                            val markers = eventMarkerMap[dateStr] ?: emptyList()

                            views.setTextViewText(wdayId, dayNumber.toString())

                            if (isToday) {
                                views.setViewVisibility(wtodayId, View.VISIBLE)
                                views.setInt(wtodayId, "setColorFilter", themePrimaryColor)
                                views.setTextColor(wdayId, 0xFFFFFFFF.toInt())
                            } else {
                                views.setViewVisibility(wtodayId, View.GONE)
                                views.setTextColor(wdayId, 0xFF1F1F1F.toInt())
                            }

                            if (markers.isNotEmpty()) {
                                views.setViewVisibility(wbgId, View.VISIBLE)
                                views.setInt(wbgId, "setColorFilter", 0x1A2563EB)
                            } else {
                                views.setViewVisibility(wbgId, View.GONE)
                            }

                            // Populate up to 3 event indicator dots
                            if (markers.isNotEmpty()) {
                                views.setViewVisibility(wdot1Id, View.VISIBLE)
                                views.setInt(wdot1Id, "setColorFilter", markers[0])
                            } else {
                                views.setViewVisibility(wdot1Id, View.GONE)
                            }

                            if (markers.size > 1) {
                                views.setViewVisibility(wdot2Id, View.VISIBLE)
                                views.setInt(wdot2Id, "setColorFilter", markers[1])
                            } else {
                                views.setViewVisibility(wdot2Id, View.GONE)
                            }

                            if (markers.size > 2) {
                                views.setViewVisibility(wdot3Id, View.VISIBLE)
                                views.setInt(wdot3Id, "setColorFilter", markers[2])
                            } else {
                                views.setViewVisibility(wdot3Id, View.GONE)
                            }

                            // Day click opens UpcomingCalendarScreen showcasing the clicked day
                            views.setOnClickPendingIntent(
                                wcellId,
                                createDayClickPendingIntent(context, dateStr, (widgetId * 100) + 50 + cellNum)
                            )
                        }
                    }

                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            } catch (e: Exception) {
                android.util.Log.e("UpcomingCalendarWidget", "Error updating calendar widget", e)
            } finally {
                pendingResult?.finish()
            }
        }
    }

    private fun isMonthlyMode(context: Context, appWidgetId: Int): Boolean {
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return false
        val prefs = getPrefs(context)
        return prefs.getBoolean(KEY_IS_MONTHLY + "_" + appWidgetId, false)
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun createNavPendingIntent(
        context: Context,
        appWidgetId: Int,
        action: String,
        requestCode: Int
    ): PendingIntent {
        val intent = Intent(context, UpcomingCalendarWidgetProvider::class.java).apply {
            this.action = action
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createDayClickPendingIntent(
        context: Context,
        dateStr: String,
        requestCode: Int
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = "com.example.ACTION_OPEN_CALENDAR"
            putExtra("NAV_TARGET", "upcoming_calendar")
            putExtra("SELECTED_DATE", dateStr)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
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
            else -> 0xFFD4A373.toInt()
        }
    }

    companion object {
        const val PREFS_NAME = "UpcomingCalendarWidgetPrefs"
        const val KEY_IS_MONTHLY = "is_monthly"
        const val KEY_MONTH_OFFSET = "month_offset"
        const val KEY_WEEK_OFFSET = "week_offset"

        const val ACTION_CALENDAR_WIDGET_PREV = "com.example.ACTION_CALENDAR_WIDGET_PREV"
        const val ACTION_CALENDAR_WIDGET_NEXT = "com.example.ACTION_CALENDAR_WIDGET_NEXT"
        const val ACTION_CALENDAR_WIDGET_TOGGLE = "com.example.ACTION_CALENDAR_WIDGET_TOGGLE"

        fun triggerUpdate(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, UpcomingCalendarWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                    val provider = UpcomingCalendarWidgetProvider()
                    provider.updateWidgetsInternal(context, appWidgetManager, appWidgetIds, null)
                }
            } catch (e: Exception) {
                android.util.Log.e("UpcomingCalendarWidget", "Error triggering calendar widget update", e)
            }
        }
    }
}
