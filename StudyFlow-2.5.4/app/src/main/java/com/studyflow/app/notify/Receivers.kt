package com.studyflow.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.studyflow.app.data.Storage

/** Fires the alarms scheduled by [Reminders]. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Reminders.ACTION_CLASS -> {
                val name = intent.getStringExtra(Reminders.EXTRA_NAME).orEmpty()
                val time = intent.getStringExtra(Reminders.EXTRA_TIME).orEmpty()
                Reminders.notifyClassSoon(context, name, time)
                Reminders.rescheduleAll(context)
            }
            Reminders.ACTION_DEADLINE -> {
                Reminders.notifyDueToday(context, Storage.load(context))
                Reminders.rescheduleAll(context)
            }
            Reminders.ACTION_TIMER -> Reminders.notifyTimerDone(context)
        }
    }
}

/** Alarms are wiped by a reboot / app update / clock change, so put them back. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Reminders.rescheduleAll(context)
    }
}
