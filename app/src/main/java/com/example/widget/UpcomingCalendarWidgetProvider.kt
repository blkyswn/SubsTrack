package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.*
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
            .putInt(KEY_MIN_HEIGHT + "_" + appWidgetId, minHeight)
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
                val subTypes = db.subscriptionTypeDao().getAllSubscriptionTypesList()
                val preorders = db.preorderDao().getAllPreordersList()
                val bookstores = db.bookstoreDao().getAllBookstoresList()

                val dateFormatKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val todayStr = dateFormatKey.format(Date())

                // Aggregate event markers per date: dateStr -> list of WidgetEventInfo
                val dayEventsMap = mutableMapOf<String, MutableList<WidgetEventInfo>>()

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
                        val subType = subTypes.find { it.id == item.subscriptionTypeId }
                        val store = bookstores.find { it.id == subType?.bookstoreId }
                        val pic = item.picturePath.takeIf { !it.isNullOrEmpty() }
                            ?: subType?.picturePath.takeIf { !it.isNullOrEmpty() }
                            ?: store?.profilePic.takeIf { !it.isNullOrEmpty() && it != "ic_launcher_foreground" }
                        val title = item.bookTitle.ifBlank { subType?.title ?: "Subscription" }
                        dayEventsMap.getOrPut(dStr) { mutableListOf() }.add(
                            WidgetEventInfo(
                                id = "sched_${item.id}",
                                title = title,
                                color = color,
                                picturePath = pic
                            )
                        )
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
                        val store = bookstores.find { it.id == item.bookstoreId }
                        val pic = item.picturePath.takeIf { !it.isNullOrEmpty() }
                            ?: store?.profilePic.takeIf { !it.isNullOrEmpty() && it != "ic_launcher_foreground" }
                        val title = item.bookTitle.ifBlank { "Preorder" }
                        dayEventsMap.getOrPut(dStr) { mutableListOf() }.add(
                            WidgetEventInfo(
                                id = "pre_${item.id}",
                                title = title,
                                color = color,
                                picturePath = pic
                            )
                        )
                    }
                }

                val themeColors = WidgetThemeUtils.getThemeColors(context, user)
                val isDark = WidgetThemeUtils.isDarkTheme(context, user)

                for (widgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.upcoming_calendar_widget)

                    // Theme colors applied across the widget
                    views.setInt(R.id.widget_cal_bg_card, "setColorFilter", themeColors.surface)
                    views.setTextColor(R.id.widget_cal_title, themeColors.onSurface)
                    views.setInt(R.id.widget_cal_prev, "setColorFilter", themeColors.primary)
                    views.setInt(R.id.widget_cal_next, "setColorFilter", themeColors.primary)
                    views.setInt(R.id.widget_cal_toggle, "setColorFilter", themeColors.onSurfaceVariant)

                    // Weekday headers colored with theme
                    for (h in 1..7) {
                        val hdrId = context.resources.getIdentifier("widget_wday_hdr_$h", "id", context.packageName)
                        if (hdrId != 0) {
                            views.setTextColor(hdrId, themeColors.onSurfaceVariant)
                        }
                    }

                    val isMonthly = isMonthlyMode(context, widgetId)
                    val prefs = getPrefs(context)
                    val monthOffset = prefs.getInt(KEY_MONTH_OFFSET + "_" + widgetId, 0)
                    val weekOffset = prefs.getInt(KEY_WEEK_OFFSET + "_" + widgetId, 0)
                    val widgetHeight = prefs.getInt(KEY_MIN_HEIGHT + "_" + widgetId, 0)

                    // If widget height is tightly compact in 1 row (e.g. < 70dp), keep weekday header clean and compact
                    if (widgetHeight in 1..65 && !isMonthly) {
                        views.setViewVisibility(R.id.widget_cal_weekdays, View.GONE)
                    } else {
                        views.setViewVisibility(R.id.widget_cal_weekdays, View.VISIBLE)
                    }

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
                            val mpicId = context.resources.getIdentifier("widget_mpic_$cellNum", "id", context.packageName)
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
                                val eventsForDay = dayEventsMap[dateStr] ?: emptyList()

                                views.setTextViewText(mdayId, dayNumber.toString())

                                if (isToday) {
                                    views.setViewVisibility(mtodayId, View.VISIBLE)
                                    views.setInt(mtodayId, "setColorFilter", themeColors.primary)
                                    views.setTextColor(mdayId, themeColors.onPrimary)
                                } else {
                                    views.setViewVisibility(mtodayId, View.GONE)
                                    views.setTextColor(mdayId, themeColors.onSurface)
                                }

                                if (eventsForDay.isNotEmpty()) {
                                    views.setViewVisibility(mbgId, View.VISIBLE)
                                    views.setInt(mbgId, "setColorFilter", themeColors.surfaceVariant)

                                    // Display pictures of upcoming events
                                    val dayBitmap = buildDayThumbnailBitmap(context, eventsForDay, isWeekly = false, isDark = isDark)
                                    views.setViewVisibility(mpicId, View.VISIBLE)
                                    views.setImageViewBitmap(mpicId, dayBitmap)
                                } else {
                                    views.setViewVisibility(mbgId, View.GONE)
                                    views.setViewVisibility(mpicId, View.GONE)
                                }

                                // Day click intent opening upcoming calendar screen
                                views.setOnClickPendingIntent(
                                    mcellId,
                                    createDayClickPendingIntent(context, dateStr, (widgetId * 100) + cellNum)
                                )
                            }
                        }
                    } else {
                        // WEEKLY VIEW (Fits 1-row up to multi-row)
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
                            val wpicId = context.resources.getIdentifier("widget_wpic_$cellNum", "id", context.packageName)

                            val dayCal = calWeek.clone() as Calendar
                            dayCal.add(Calendar.DAY_OF_MONTH, c)
                            val dateStr = dateFormatKey.format(dayCal.time)
                            val dayNumber = dayCal.get(Calendar.DAY_OF_MONTH)
                            val isToday = dateStr == todayStr
                            val eventsForDay = dayEventsMap[dateStr] ?: emptyList()

                            views.setTextViewText(wdayId, dayNumber.toString())

                            if (isToday) {
                                views.setViewVisibility(wtodayId, View.VISIBLE)
                                views.setInt(wtodayId, "setColorFilter", themeColors.primary)
                                views.setTextColor(wdayId, themeColors.onPrimary)
                            } else {
                                views.setViewVisibility(wtodayId, View.GONE)
                                views.setTextColor(wdayId, themeColors.onSurface)
                            }

                            if (eventsForDay.isNotEmpty()) {
                                views.setViewVisibility(wbgId, View.VISIBLE)
                                views.setInt(wbgId, "setColorFilter", themeColors.surfaceVariant)

                                // Display pictures of upcoming events just like in the calendar views in the app
                                val dayBitmap = buildDayThumbnailBitmap(context, eventsForDay, isWeekly = true, isDark = isDark)
                                views.setViewVisibility(wpicId, View.VISIBLE)
                                views.setImageViewBitmap(wpicId, dayBitmap)
                            } else {
                                views.setViewVisibility(wbgId, View.GONE)
                                views.setViewVisibility(wpicId, View.GONE)
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

    private suspend fun buildDayThumbnailBitmap(
        context: Context,
        events: List<WidgetEventInfo>,
        isWeekly: Boolean,
        isDark: Boolean
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        val totalWidthDp = if (isWeekly) 36f else 22f
        val totalHeightDp = if (isWeekly) 28f else 16f
        val coverWidthDp = if (isWeekly) 18f else 12f
        val coverHeightDp = if (isWeekly) 24f else 14f
        val cornerRadiusDp = if (isWeekly) 3f else 2f

        val totalWidthPx = (totalWidthDp * density).toInt().coerceAtLeast(1)
        val totalHeightPx = (totalHeightDp * density).toInt().coerceAtLeast(1)
        val coverWidthPx = (coverWidthDp * density).toInt().coerceAtLeast(1)
        val coverHeightPx = (coverHeightDp * density).toInt().coerceAtLeast(1)
        val cornerRadiusPx = cornerRadiusDp * density

        val compositeBitmap = Bitmap.createBitmap(totalWidthPx, totalHeightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(compositeBitmap)

        val maxShow = 3
        val displayList = events.take(maxShow)
        val count = displayList.size

        displayList.forEachIndexed { index, event ->
            val offsetX = when (count) {
                1 -> 0f
                2 -> if (index == 0) -2.5f * density else 2.5f * density
                else -> when (index) {
                    0 -> -3.5f * density
                    1 -> 0f
                    else -> 3.5f * density
                }
            }
            val offsetY = when (count) {
                1 -> 0f
                2 -> if (index == 0) -1.5f * density else 1.5f * density
                else -> when (index) {
                    0 -> -2f * density
                    1 -> 0f
                    else -> 2f * density
                }
            }

            val coverBitmap = loadCoverBitmap(context, event, coverWidthPx, coverHeightPx, cornerRadiusPx, isDark)
            val left = (totalWidthPx - coverWidthPx) / 2f + offsetX
            val top = (totalHeightPx - coverHeightPx) / 2f + offsetY

            canvas.drawBitmap(coverBitmap, left, top, null)
        }

        // If more than maxShow events, draw small +N badge
        if (events.size > maxShow) {
            val badgeRadius = 4.5f * density
            val badgeX = totalWidthPx - badgeRadius - 1f
            val badgeY = totalHeightPx - badgeRadius - 1f
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isDark) 0xFFD4A373.toInt() else 0xFF2563EB.toInt()
                style = Paint.Style.FILL
            }
            canvas.drawCircle(badgeX, badgeY, badgeRadius, paint)
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 6.5f * density
                textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT_BOLD
            }
            val textY = badgeY - (textPaint.descent() + textPaint.ascent()) / 2
            canvas.drawText("+${events.size - maxShow}", badgeX, textY, textPaint)
        }

        return compositeBitmap
    }

    private suspend fun loadCoverBitmap(
        context: Context,
        event: WidgetEventInfo,
        widthPx: Int,
        heightPx: Int,
        cornerRadiusPx: Float,
        isDark: Boolean
    ): Bitmap {
        if (!event.picturePath.isNullOrBlank()) {
            try {
                val imageLoader = coil.Coil.imageLoader(context)
                val request = coil.request.ImageRequest.Builder(context)
                    .data(event.picturePath)
                    .size(widthPx, heightPx)
                    .allowHardware(false)
                    .transformations(coil.transform.RoundedCornersTransformation(cornerRadiusPx))
                    .build()
                val result = imageLoader.execute(request)
                if (result is coil.request.SuccessResult) {
                    val d = result.drawable
                    val b = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
                    val c = Canvas(b)
                    d.setBounds(0, 0, widthPx, heightPx)
                    d.draw(c)

                    // Add subtle border around cover like MonthCalendar
                    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = if (isDark) 0xFF333333.toInt() else Color.WHITE
                        style = Paint.Style.STROKE
                        strokeWidth = 0.8f * context.resources.displayMetrics.density
                    }
                    c.drawRoundRect(0f, 0f, widthPx.toFloat(), heightPx.toFloat(), cornerRadiusPx, cornerRadiusPx, strokePaint)
                    return b
                }
            } catch (_: Exception) {}
        }

        // Fallback: styled book cover placeholder exactly like BookCoverThumbnail
        val b = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        val rect = RectF(0f, 0f, widthPx.toFloat(), heightPx.toFloat())

        val shader = LinearGradient(
            0f, 0f, 0f, heightPx.toFloat(),
            intArrayOf(event.color, darkenColor(event.color, 0.75f)),
            null,
            Shader.TileMode.CLAMP
        )
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader }
        c.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, fillPaint)

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDark) 0xFF333333.toInt() else Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 0.8f * context.resources.displayMetrics.density
        }
        c.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, strokePaint)

        val letter = event.title.trim().firstOrNull()?.uppercase() ?: "B"
        val letterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = (heightPx * 0.45f)
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        val textY = (heightPx / 2f) - (letterPaint.descent() + letterPaint.ascent()) / 2f
        c.drawText(letter, widthPx / 2f, textY, letterPaint)

        return b
    }

    private fun darkenColor(color: Int, factor: Float): Int {
        val a = Color.alpha(color)
        val r = (Color.red(color) * factor).toInt().coerceIn(0, 255)
        val g = (Color.green(color) * factor).toInt().coerceIn(0, 255)
        val b = (Color.blue(color) * factor).toInt().coerceIn(0, 255)
        return Color.argb(a, r, g, b)
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

    companion object {
        const val PREFS_NAME = "UpcomingCalendarWidgetPrefs"
        const val KEY_IS_MONTHLY = "is_monthly"
        const val KEY_MIN_HEIGHT = "min_height"
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

data class WidgetEventInfo(
    val id: String,
    val title: String,
    val color: Int,
    val picturePath: String?
)
