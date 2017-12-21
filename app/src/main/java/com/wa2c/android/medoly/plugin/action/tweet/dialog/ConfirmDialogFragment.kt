package com.wa2c.android.medoly.plugin.action.tweet.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.text.TextUtils


/**
 * 確認ダイアログを表示する。
 */
class ConfirmDialogFragment : AbstractDialogFragment() {


    /**
     * onCreateDialogイベント処理。
     */
    override fun onCreateDialog(savedInstanceState: Bundle): Dialog {
        val args = arguments

        // ダイアログビルダ
        val builder = AlertDialog.Builder(activity)
        builder.setTitle(args.getCharSequence(ARG_TITLE))
        builder.setMessage(args.getCharSequence(ARG_MESSAGE))

        // ボタン
        if (args.getBoolean(ARG_IS_BUTTON_DEFAULT)) {
            builder.setPositiveButton(android.R.string.ok, clickListener)
            builder.setNeutralButton(android.R.string.cancel, clickListener)
        } else {
            // Positiveボタン
            val positive = args.getString(ARG_POSITIVE_BUTTON)
            if (!TextUtils.isEmpty(positive)) {
                builder.setPositiveButton(positive, clickListener)
            }
            // Neutralボタン
            val neutral = args.getString(ARG_NEUTRAL_BUTTON)
            if (!TextUtils.isEmpty(neutral)) {
                builder.setNeutralButton(neutral, clickListener)

            }
            // Negativeボタン
            val negative = args.getString(ARG_NEGATIVE_BUTTON)
            if (!TextUtils.isEmpty(negative)) {
                builder.setNegativeButton(negative, clickListener)
            }
        }

        return builder.create()
    }

    companion object {

        /** タイトルキー。  */
        private val ARG_TITLE = "TITLE"
        /** メッセージキー。  */
        private val ARG_MESSAGE = "MESSAGE"

        /** 標準ボタン判定。  */
        private val ARG_IS_BUTTON_DEFAULT = "IS_BUTTON_DEFAULT"
        /** Positiveボタンキー。  */
        private val ARG_POSITIVE_BUTTON = "POISITIVE_BUTTON"
        /** Neutralボタンキー。  */
        private val ARG_NEUTRAL_BUTTON = "NEUTRAL_BUTTON"
        /** Negativeボタンキー。  */
        private val ARG_NEGATIVE_BUTTON = "NEGATIVE_BUTTON"


        /**
         * ダイアログのインスタンスを作成する。(OK/Cancelボタン。)
         * @param message メッセージ。
         * @param title タイトル。
         * @return ダイアログのインスタンス。
         */
        fun newInstance(message: CharSequence, title: CharSequence): ConfirmDialogFragment {
            val fragment = ConfirmDialogFragment()
            val args = Bundle()
            args.putCharSequence(ARG_MESSAGE, message)
            args.putCharSequence(ARG_TITLE, title)
            args.putBoolean(ARG_IS_BUTTON_DEFAULT, true)
            fragment.arguments = args

            return fragment
        }

        /**
         * ダイアログのインスタンスを作成する。(ボタン文字指定。null指定の場合はボタン無し。)
         * @param message メッセージ。
         * @param title タイトル。
         * @param positiveButton Positiveボタン。
         * @param neutralButton  Neutralボタン。
         * @param negativeButton Negativeボタン。
         * @return ダイアログのインスタンス。
         */
        fun newInstance(message: String, title: String, positiveButton: String, neutralButton: String, negativeButton: String): ConfirmDialogFragment {
            val fragment = ConfirmDialogFragment()
            val args = Bundle()
            args.putCharSequence(ARG_MESSAGE, message)
            args.putCharSequence(ARG_TITLE, title)
            args.putString(ARG_POSITIVE_BUTTON, positiveButton)
            args.putString(ARG_NEUTRAL_BUTTON, neutralButton)
            args.putString(ARG_NEGATIVE_BUTTON, negativeButton)
            args.putBoolean(ARG_IS_BUTTON_DEFAULT, false)
            fragment.arguments = args

            return fragment
        }
    }

}
