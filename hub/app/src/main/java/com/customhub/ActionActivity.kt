package com.customhub

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast

/** Invisible screen that widget buttons open. It hands the action to the gesture service and closes. */
class ActionActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val action = intent.getIntExtra(EXTRA_ACTION, Actions.NONE)
        val service = GestureService.instance
        if (service == null) {
            Toast.makeText(
                this, "Turn on Custom Hub in Accessibility settings first", Toast.LENGTH_LONG
            ).show()
            startActivity(
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } else {
            // A short delay lets this window disappear before a screenshot is taken.
            service.perform(action, 250)
        }
        finish()
    }

    companion object {
        const val EXTRA_ACTION = "action"
    }
}
