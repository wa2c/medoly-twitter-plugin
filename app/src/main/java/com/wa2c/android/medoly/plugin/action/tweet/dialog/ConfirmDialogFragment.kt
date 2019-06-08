package com.wa2c.android.medoly.plugin.action.tweet.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog

/**
 * Confirmation dialog
 */
class ConfirmDialogFragment : AbstractDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)
        val args = arguments!!

        // dialog build
        return AlertDialog.Builder(context).apply {
            setTitle(args.getCharSequence(ARG_TITLE))
            setMessage(args.getCharSequence(ARG_MESSAGE))

            // button
            if (args.getBoolean(ARG_IS_BUTTON_DEFAULT)) {
                setPositiveButton(android.R.string.ok, null)
                setNeutralButton(android.R.string.cancel, null)
            } else {
                // Positive button
                args.getString(ARG_POSITIVE_BUTTON)?.let {
                    if (it.isNotEmpty()) setPositiveButton(it, null)
                }
                // Neutral button
                args.getString(ARG_NEUTRAL_BUTTON)?.let {
                    if (it.isNotEmpty()) setNeutralButton(it, null)
                }
                // Negative button
                args.getString(ARG_NEGATIVE_BUTTON)?.let {
                    if (it.isNotEmpty()) setNegativeButton(it, null)
                }
            }
        }.create()
    }

    companion object {

        /** Title key.  */
        private const val ARG_TITLE = "TITLE"
        /** Message key. */
        private const val ARG_MESSAGE = "MESSAGE"


        private const val ARG_IS_BUTTON_DEFAULT = "IS_BUTTON_DEFAULT"
        /** Positive button key.  */
        private const val ARG_POSITIVE_BUTTON = "POSITIVE_BUTTON"
        /** Neutral button key.  */
        private const val ARG_NEUTRAL_BUTTON = "NEUTRAL_BUTTON"
        /** Negative button key.  */
        private const val ARG_NEGATIVE_BUTTON = "NEGATIVE_BUTTON"

        /**
         * Create dialog instance. (OK/Cancel)
         * @param message A message.
         * @param title A title.
         * @return New dialog instance.
         */
        fun newInstance(message: CharSequence, title: CharSequence?): ConfirmDialogFragment {
            return ConfirmDialogFragment().apply {
                arguments = Bundle().apply {
                    putCharSequence(ARG_MESSAGE, message)
                    putCharSequence(ARG_TITLE, title)
                    putBoolean(ARG_IS_BUTTON_DEFAULT, true)
                }
            }
        }

    }

}
