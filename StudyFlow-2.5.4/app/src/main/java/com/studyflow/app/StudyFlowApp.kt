package com.studyflow.app

import android.app.Application
import com.studyflow.app.notify.Reminders

class StudyFlowApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Reminders.createChannels(this)
    }
}
