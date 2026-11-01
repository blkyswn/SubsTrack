package com.example.receiver

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.BookishDatabase
import com.example.data.BookishRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class ReminderReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_REMINDER = "com.example.ACTION_REMINDER"
        const val ACTION_MARK_PREORDERED = "com.example.ACTION_MARK_PREORDERED"
        private val deliveredReminders = ConcurrentHashMap.newKeySet<String>()

        fun clearDeliveredReminder(type: String, id: Int) {
            deliveredReminders.removeIf { it.startsWith("$type:$id:") }
        }

        fun sendTestNotification(context: Context, type: String = "preorder", title: String = "Test Reminder") {
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                action = ACTION_REMINDER
                putExtra("type", type)
                putExtra("id", (System.currentTimeMillis() % 100000).toInt())
                putExtra("title", title)
                putExtra("date", System.currentTimeMillis())
                putExtra("targetTime", System.currentTimeMillis())
            }
            ReminderReceiver().onReceive(context, intent)
        }

        fun markPreorderAsPreordered(context: Context, preorderId: Int, notificationId: Int = preorderId) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (notificationId != 0) {
                notificationManager?.cancel(notificationId)
            }
            ReminderEventBus.dismiss()

            if (preorderId != 0) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = BookishDatabase.getDatabase(context)
                        val repository = BookishRepository(db)
                        val preorder = repository.getPreorderById(preorderId)
                        if (preorder != null) {
                            val updated = preorder.copy(status = "Preordered")
                            repository.updatePreorder(updated)
                            ReminderScheduler.cancelPreorderReminder(context, preorderId)
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    "${preorder.bookTitle}: Status changed to Preordered",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            com.example.widget.CalendarBentoWidgetProvider.triggerUpdate(context)
                            com.example.widget.UpcomingCalendarWidgetProvider.triggerUpdate(context)
                            com.example.widget.UpcomingListWidgetProvider.triggerUpdate(context)
                            com.example.widget.TodayPreordersWidgetProvider.triggerUpdate(context)
                        }
                    } catch (e: Exception) {
                        Log.e("ReminderReceiver", "Error changing preorder status to Preordered", e)
                    }
                }
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_MARK_PREORDERED) {
            val preorderId = intent.getIntExtra("preorderId", 0)
            val notificationId = intent.getIntExtra("notificationId", preorderId)
            markPreorderAsPreordered(context, preorderId, notificationId)
            return
        }

        if (intent.action != ACTION_REMINDER && intent.action != "com.example.ACTION_REMINDER") return

        val type = intent.getStringExtra("type") ?: return
        val id = intent.getIntExtra("id", 0)
        val title = intent.getStringExtra("title") ?: "Reminder"
        val book = intent.getStringExtra("book") ?: ""
        val dateMs = intent.getLongExtra("date", 0L)
        val targetTimeMs = intent.getLongExtra("targetTime", 0L)

        // Drop stale reminders where reminder datetime has passed
        if (targetTimeMs > 0L) {
            val now = System.currentTimeMillis()
            // Allow 30-minute OS delivery tolerance for background/doze alarms, drop stale past reminders
            if (now > targetTimeMs + 30 * 60 * 1000L) {
                Log.w("ReminderReceiver", "Dropping stale reminder: target was $targetTimeMs, now is $now")
                return
            }
            val dedupKey = "$type:$id:$targetTimeMs"
            if (!deliveredReminders.add(dedupKey)) {
                Log.d("ReminderReceiver", "Duplicate reminder delivery ignored for $dedupKey")
                return
            }
        }

        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val isToday = if (dateMs != 0L) {
            val todayCal = Calendar.getInstance()
            val dateCal = Calendar.getInstance().apply { timeInMillis = dateMs }
            todayCal.get(Calendar.YEAR) == dateCal.get(Calendar.YEAR) &&
            todayCal.get(Calendar.DAY_OF_YEAR) == dateCal.get(Calendar.DAY_OF_YEAR)
        } else false

        val formattedDate = if (isToday) "today" else if (dateMs != 0L) dateFormat.format(Date(dateMs)) else ""

        val notificationTitle: String
        val notificationText: String
        val smallIconRes: Int

        if (type == "preorder") {
            smallIconRes = com.example.R.drawable.ic_book_5
            notificationTitle = "Preorder Release: $title"
            notificationText = if (isToday) {
                "Your preorder is releasing today!"
            } else if (formattedDate.isNotEmpty()) {
                "Your preorder is releasing on $formattedDate!"
            } else {
                "Your preorder is releasing soon!"
            }
        } else if (type == "forwarding_storage") {
            smallIconRes = com.example.R.drawable.ic_package_2
            notificationTitle = "Storage Alert: $title"
            val daysLeft = intent.getIntExtra("daysLeft", 0)
            notificationText = if (daysLeft == 0) {
                if (book.isNotEmpty()) "Storage expires today for '$book' at $title!" else "Storage expires today at $title!"
            } else if (daysLeft == 1) {
                if (book.isNotEmpty()) "1 day of storage left for '$book' at $title!" else "1 day of storage left at $title!"
            } else {
                if (book.isNotEmpty()) "$daysLeft days of storage left for '$book' at $title!" else "$daysLeft days of storage left at $title!"
            }
        } else {
            smallIconRes = com.example.R.drawable.ic_subscriptions_auto_stories
            notificationTitle = "Subscription Renewal: $title"
            notificationText = if (isToday) {
                if (book.isNotEmpty()) "Renewal delivery '$book' is due today!" else "Your subscription is renewing today!"
            } else if (formattedDate.isNotEmpty()) {
                if (book.isNotEmpty()) "Renewal delivery '$book' is due on $formattedDate!" else "Your subscription is renewing on $formattedDate!"
            } else {
                "Your subscription is renewing soon!"
            }
        }

        // Open main app when clicked
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Make sure channel is created
        ReminderScheduler.createNotificationChannel(context)

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(smallIconRes)
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 500, 250, 500))
            .setFullScreenIntent(pendingIntent, false)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        // Add action button for preorders to mark as Preordered
        if (type == "preorder") {
            val preorderedIntent = Intent(context, ReminderReceiver::class.java).apply {
                action = ACTION_MARK_PREORDERED
                putExtra("preorderId", id)
                putExtra("notificationId", id)
            }
            val preorderedPendingIntent = PendingIntent.getBroadcast(
                context,
                2000000 + id,
                preorderedIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            notificationBuilder.addAction(
                com.example.R.drawable.ic_shopping_bag_speed,
                "Preordered",
                preorderedPendingIntent
            )
        }

        val notification = notificationBuilder.build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        try {
            notificationManager?.notify(id, notification)
        } catch (e: Exception) {
            Log.e("ReminderReceiver", "Error posting notification", e)
        }

        // Post to ReminderEventBus if active
        try {
            ReminderEventBus.post(
                ReminderEventBus.InAppReminder(
                    id = id,
                    type = type,
                    title = notificationTitle,
                    message = notificationText
                )
            )
        } catch (_: Exception) {}
    }
}

object ReminderScheduler {
    const val CHANNEL_ID = "sub_track_reminders_v4"
    const val CHANNEL_NAME = "Preorder & Subscription Reminders"
    private val timerScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val inProcessTimers = ConcurrentHashMap<Int, Job>()

    private fun scheduleExactOrFallbackAlarm(alarmManager: AlarmManager, targetTimeMs: Long, pendingIntent: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetTimeMs, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetTimeMs, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetTimeMs, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, targetTimeMs, pendingIntent)
            }
        } catch (se: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetTimeMs, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, targetTimeMs, pendingIntent)
            }
        }
    }

    private fun scheduleInProcessTimer(context: Context, requestCode: Int, targetTimeMs: Long, intent: Intent) {
        inProcessTimers.remove(requestCode)?.cancel()
        val delayMs = targetTimeMs - System.currentTimeMillis()
        if (delayMs <= 0L) return
        val appContext = context.applicationContext ?: context
        val intentCopy = Intent(intent)
        inProcessTimers[requestCode] = timerScope.launch {
            delay(delayMs)
            try {
                ReminderReceiver().onReceive(appContext, intentCopy)
            } catch (e: Exception) {
                Log.e("ReminderScheduler", "Error in in-process reminder timer", e)
            } finally {
                inProcessTimers.remove(requestCode)
            }
        }
    }

    private fun cancelInProcessTimer(requestCode: Int) {
        inProcessTimers.remove(requestCode)?.cancel()
    }

    fun createNotificationChannel(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
                
                // Clean up previous silent channel registrations so new heads-up channel takes effect
                try {
                    notificationManager.deleteNotificationChannel("sub_track_reminders_channel")
                    notificationManager.deleteNotificationChannel("sub_track_reminders_channel_v2")
                    notificationManager.deleteNotificationChannel("sub_track_reminders_channel_v3")
                } catch (_: Exception) {}

                val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    .build()

                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Heads-up alert notifications for preorders and subscription renewals"
                    enableLights(true)
                    lightColor = android.graphics.Color.BLUE
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 500, 250, 500)
                    setSound(defaultSoundUri, audioAttributes)
                    lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                    setShowBadge(true)
                    setBypassDnd(false)
                }
                notificationManager.createNotificationChannel(channel)
            }
        } catch (e: Exception) {
            Log.e("ReminderScheduler", "Failed to create notification channel", e)
        }
    }

    fun schedulePreorderReminder(context: Context, preorderId: Int, title: String, saleDateStart: Long, offsetDays: Int, hour: Int, minute: Int) {
        try {
            val targetTimeMs = calculateReminderTime(saleDateStart, offsetDays, hour, minute)
            if (targetTimeMs <= System.currentTimeMillis()) {
                // If the reminder date and time has already passed, cancel any old alarm and do NOT schedule a new one
                cancelPreorderReminder(context, preorderId)
                Log.d("ReminderScheduler", "Preorder reminder time ($targetTimeMs) has already passed (now=${System.currentTimeMillis()}). Skipping.")
                return
            }

            ReminderReceiver.clearDeliveredReminder("preorder", preorderId)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                action = "com.example.ACTION_REMINDER"
                putExtra("type", "preorder")
                putExtra("id", preorderId)
                putExtra("title", title)
                putExtra("date", saleDateStart)
                putExtra("targetTime", targetTimeMs)
            }

            // Generate a unique request code for preorder alarms (e.g. 100000 + preorderId)
            val requestCode = 100000 + preorderId
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            scheduleExactOrFallbackAlarm(alarmManager, targetTimeMs, pendingIntent)
            scheduleInProcessTimer(context, requestCode, targetTimeMs, intent)
            Log.d("ReminderScheduler", "Scheduled preorder reminder for $title at $targetTimeMs")
        } catch (e: Exception) {
            Log.e("ReminderScheduler", "Failed to schedule preorder alarm", e)
        }
    }

    fun cancelPreorderReminder(context: Context, preorderId: Int) {
        try {
            val requestCode = 100000 + preorderId
            cancelInProcessTimer(requestCode)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                action = "com.example.ACTION_REMINDER"
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        } catch (e: Exception) {
            Log.e("ReminderScheduler", "Failed to cancel preorder alarm", e)
        }
    }

    fun scheduleScheduledSubReminder(context: Context, scheduledSubId: Int, subTitle: String, bookTitle: String, dueDate: Long, offsetDays: Int, hour: Int, minute: Int) {
        try {
            val targetTimeMs = calculateReminderTime(dueDate, offsetDays, hour, minute)
            if (targetTimeMs <= System.currentTimeMillis()) {
                // If the reminder date and time has already passed, cancel any old alarm and do NOT schedule a new one
                cancelScheduledSubReminder(context, scheduledSubId)
                Log.d("ReminderScheduler", "Subscription reminder time ($targetTimeMs) has already passed (now=${System.currentTimeMillis()}). Skipping.")
                return
            }

            ReminderReceiver.clearDeliveredReminder("subscription", scheduledSubId)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                action = "com.example.ACTION_REMINDER"
                putExtra("type", "subscription")
                putExtra("id", scheduledSubId)
                putExtra("title", subTitle)
                putExtra("book", bookTitle)
                putExtra("date", dueDate)
                putExtra("targetTime", targetTimeMs)
            }

            // Generate a unique request code for scheduled sub alarms (e.g. 200000 + scheduledSubId)
            val requestCode = 200000 + scheduledSubId
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            scheduleExactOrFallbackAlarm(alarmManager, targetTimeMs, pendingIntent)
            scheduleInProcessTimer(context, requestCode, targetTimeMs, intent)
            Log.d("ReminderScheduler", "Scheduled subscription reminder for $subTitle ($bookTitle) at $targetTimeMs")
        } catch (e: Exception) {
            Log.e("ReminderScheduler", "Failed to schedule sub alarm", e)
        }
    }

    fun cancelScheduledSubReminder(context: Context, scheduledSubId: Int) {
        try {
            val requestCode = 200000 + scheduledSubId
            cancelInProcessTimer(requestCode)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                action = "com.example.ACTION_REMINDER"
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        } catch (e: Exception) {
            Log.e("ReminderScheduler", "Failed to cancel sub alarm", e)
        }
    }

    fun scheduleForwardingStorageReminder(
        context: Context,
        packageId: Int,
        serviceName: String,
        bookTitle: String,
        limitDate: Long,
        daysLeftOffset: Int,
        hour: Int,
        minute: Int
    ) {
        try {
            val targetTimeMs = calculateReminderTime(limitDate, daysLeftOffset, hour, minute)
            if (targetTimeMs <= System.currentTimeMillis()) {
                cancelForwardingStorageReminder(context, packageId)
                Log.d("ReminderScheduler", "Storage reminder time ($targetTimeMs) has already passed (now=${System.currentTimeMillis()}). Skipping.")
                return
            }

            ReminderReceiver.clearDeliveredReminder("forwarding_storage", packageId)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                action = "com.example.ACTION_REMINDER"
                putExtra("type", "forwarding_storage")
                putExtra("id", packageId)
                putExtra("title", serviceName)
                putExtra("book", bookTitle)
                putExtra("daysLeft", daysLeftOffset)
                putExtra("date", limitDate)
                putExtra("targetTime", targetTimeMs)
            }

            val requestCode = 300000 + packageId
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            scheduleExactOrFallbackAlarm(alarmManager, targetTimeMs, pendingIntent)
            scheduleInProcessTimer(context, requestCode, targetTimeMs, intent)
            Log.d("ReminderScheduler", "Scheduled storage reminder for $serviceName ($bookTitle) at $targetTimeMs")
        } catch (e: Exception) {
            Log.e("ReminderScheduler", "Failed to schedule storage alarm", e)
        }
    }

    fun cancelForwardingStorageReminder(context: Context, packageId: Int) {
        try {
            val requestCode = 300000 + packageId
            cancelInProcessTimer(requestCode)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                action = "com.example.ACTION_REMINDER"
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        } catch (e: Exception) {
            Log.e("ReminderScheduler", "Failed to cancel storage alarm", e)
        }
    }

    fun calculateReminderTime(eventDateMs: Long, dDayOffset: Int, hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = eventDateMs
            add(Calendar.DAY_OF_YEAR, -dDayOffset)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}
