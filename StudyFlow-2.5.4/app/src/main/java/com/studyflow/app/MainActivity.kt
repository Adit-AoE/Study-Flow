package com.studyflow.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.studyflow.app.notify.Reminders
import com.studyflow.app.ui.StudyFlowRoot
import com.studyflow.app.ui.StudyFlowViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: StudyFlowViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Same as the web version: remind about tasks due today when the app is opened.
        if (savedInstanceState == null) {
            Reminders.notifyDueToday(this, viewModel.data.value)
        }

        setContent {
            StudyFlowRoot(viewModel)
        }
    }
}
