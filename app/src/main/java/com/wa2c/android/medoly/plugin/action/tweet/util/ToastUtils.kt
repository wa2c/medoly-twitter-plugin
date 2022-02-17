package com.wa2c.android.medoly.plugin.action.tweet.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment

/**
 * Toast receiver.
 */
class ToastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Toast.makeText(context.applicationContext, intent.getStringExtra(MESSAGE_TOAST), Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val MESSAGE_TOAST = "message"

        fun showToast(context: Context, @StringRes stringId: Int) {
            showToast(context, context.getString(stringId))
        }

        fun showToast(context: Context, text: String) {
            val intent = Intent(context, ToastReceiver::class.java)
            intent.putExtra(MESSAGE_TOAST, text)
            context.sendBroadcast(intent)
            logD(text)
        }
    }

}

/** Show toast message */
fun Context.toast(@StringRes messageRes: Int) {
    ToastReceiver.showToast(this.applicationContext, messageRes)
}

/** Show toast message */
fun Context.toast(message: Any?) {
    ToastReceiver.showToast(this.applicationContext, message.toString())
}

/** Show toast message */
fun Fragment.toast(@StringRes messageRes: Int) {
    context?.toast(messageRes)
}

/** Show toast message */
fun Fragment.toast(message: Any?) {
    context?.toast(message)
}
