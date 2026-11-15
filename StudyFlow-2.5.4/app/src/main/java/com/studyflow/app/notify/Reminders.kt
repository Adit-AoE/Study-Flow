package com.studyflow.app.notify

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.studyflow.app.MainActivity
import com.studyflow.app.R
import com.studyflow.app.data.AppData
import com.studyflow.app.data.RoutineItem
import com.studyflow.app.data.Storage
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Native replacement for the web version's notification engine:
 *  - a reminder 15 minutes before every routine session (repeats weekly)
 *  - a daily 8:00 AM reminder when tasks are due today
 *  - a "time is up" alert for the focus timer
 */
object Reminders {
    const val CH_CLASS = "class_reminders"
    const val CH_DEADLINE = "deadlines"
    const val CH_TIMER = "focus_timer"

    const val ACTION_CLASS = "com.studyflow.app.action.CLASS_REMINDER"
    const val ACTION_DEADLINE = "com.studyflow.app.action.DEADLINE_REMINDER"
    const val ACTION_TIMER = "com.studyflow.app.action.TIMER_DONE"

    const val EXTRA_NAME = "name"
    const val EXTRA_TIME = "time"

    private const val PREFS = "reminders"
    private const val KEY_CODES = "codes"

    private const val CODE_DEADLINE = 1001
    private const val CODE_TIMER = 1002
    private const val NOTIF_TIMER = 2001
    private const val NOTIF_DEADLINE = 2002
    private const val NOTIF_TEST = 2003

    private const val LEAD_MINUTES = 15L
    private const val DEADLINE_HOUR = 8

    // ---- channels / permissions --------------------------------------------------------------

    fun createChannels(ctx: Context) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CH_CLASS, "Class & study reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "15-minute reminders before sessions in your routine"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_DEADLINE, "Deadlines", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Reminders for tasks that are due today"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_TIMER, "Focus timer", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Alert when a focus session is finished"
            }
        )
    }

    fun notificationsEnabled(ctx: Context): Boolean = NotificationManagerCompat.from(ctx).areNotificationsEnabled()

    fun canScheduleExact(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return am.canScheduleExactAlarms()
    }

    // ---- scheduling -------------------------------------------------------------------------

    private fun setAlarm(ctx: Context, type: Int, triggerAt: Long, pi: PendingIntent) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try {
            if (canScheduleExact(ctx)) {
                am.setExactAndAllowWhileIdle(type, triggerAt, pi)
            } else {
                am.setAndAllowWhileIdle(type, triggerAt, pi)
            }
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(type, triggerAt, pi)
        }
    }

    private fun receiverIntent(ctx: Context, action: String): Intent =
        Intent(ctx, ReminderReceiver::class.java).setAction(action)

    private fun cancelCode(ctx: Context, code: Int, action: String) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(
            ctx, code, receiverIntent(ctx, action),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) {
            am.cancel(pi)
            pi.cancel()
        }
    }

    /** Next moment (15 min before the session) that is still in the future. */
    fun nextClassReminder(item: RoutineItem, now: LocalDateTime): LocalDateTime? {
        return try {
            val dow = DayOfWeek.of(if (item.day == 0) 7 else item.day)
            val time = LocalTime.parse(item.time)
            val today = now.toLocalDate()
            val offset = ((dow.value - today.dayOfWeek.value + 7) % 7).toLong()
            var at = today.plusDays(offset).atTime(time).minusMinutes(LEAD_MINUTES)
            if (!at.isAfter(now)) at = at.plusDays(7)
            at
        } catch (e: Exception) {
            null
        }
    }

    /** Re-creates every alarm from the saved data. Safe to call as often as needed. */
    fun rescheduleAll(ctx: Context, data: AppData = Storage.load(ctx)) {
        val app = ctx.applicationContext
        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        prefs.getStringSet(KEY_CODES, emptySet())?.forEach { code ->
            code.toIntOrNull()?.let { cancelCode(app, it, ACTION_CLASS) }
        }

        val now = LocalDateTime.now()
        val zone = ZoneId.systemDefault()
        val codes = mutableSetOf<String>()

        data.active.routine.forEach { r ->
            val at = nextClassReminder(r, now) ?: return@forEach
            val code = ("r_" + r.id).hashCode()
            val intent = receiverIntent(app, ACTION_CLASS)
                .putExtra(EXTRA_NAME, r.name)
                .putExtra(EXTRA_TIME, r.time)
            val pi = PendingIntent.getBroadcast(
                app, code, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            setAlarm(app, AlarmManager.RTC_WAKEUP, at.atZone(zone).toInstant().toEpochMilli(), pi)
            codes += code.toString()
        }
        prefs.edit().putStringSet(KEY_CODES, codes).apply()

        // Daily "tasks due today" reminder.
        var next = now.toLocalDate().atTime(DEADLINE_HOUR, 0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val deadlinePi = PendingIntent.getBroadcast(
            app, CODE_DEADLINE, receiverIntent(app, ACTION_DEADLINE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setAlarm(app, AlarmManager.RTC_WAKEUP, next.atZone(zone).toInstant().toEpochMilli(), deadlinePi)
    }

    // ---- focus timer ------------------------------------------------------------------------

    fun scheduleTimer(ctx: Context, triggerElapsedRealtime: Long) {
        val app = ctx.applicationContext
        val pi = PendingIntent.getBroadcast(
            app, CODE_TIMER, receiverIntent(app, ACTION_TIMER),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setAlarm(app, AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerElapsedRealtime, pi)
    }

    fun cancelTimer(ctx: Context) {
        cancelCode(ctx.applicationContext, CODE_TIMER, ACTION_TIMER)
    }

    // ---- notifications ------------------------------------------------------------------------

    private fun openAppIntent(ctx: Context): PendingIntent = PendingIntent.getActivity(
        ctx, 0,
        Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    @SuppressLint("MissingPermission")
    private fun post(ctx: Context, id: Int, channel: String, title: String, body: String) {
        if (!notificationsEnabled(ctx)) return
        val notification = NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setColor(0xFFDC2626.toInt())
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(ctx))
            .build()
        try {
            NotificationManagerCompat.from(ctx).notify(id, notification)
        } catch (e: SecurityException) {
            // Permission was revoked between the check and the call – nothing to do.
        }
    }

    fun format12h(time24: String): String = try {
        LocalTime.parse(time24).format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
    } catch (e: Exception) {
        time24
    }

    fun notifyClassSoon(ctx: Context, name: String, time24: String) {
        post(
            ctx,
            ("class_" + name + time24).hashCode(),
            CH_CLASS,
            "Upcoming Class/Study Session",
            "Your session \"$name\" is starting in 15 minutes at ${format12h(time24)}."
        )
    }

    fun notifyDueToday(ctx: Context, data: AppData) {
        val today = LocalDate.now().toString()
        val due = data.active.tasks.count { it.date == today && !it.completed }
        if (due > 0) {
            post(ctx, NOTIF_DEADLINE, CH_DEADLINE, "Study Flow Deadlines", "Reminder: You have $due task(s) due today!")
        }
    }

    fun notifyTimerDone(ctx: Context) {
        post(ctx, NOTIF_TIMER, CH_TIMER, "Study Flow Timer", "Time is up! Great focus.")
    }

    fun notifyEnabled(ctx: Context) {
        post(ctx, NOTIF_TEST, CH_DEADLINE, "Study Flow", "Notifications are successfully enabled!")
    }
}
