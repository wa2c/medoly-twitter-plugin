package com.wa2c.android.medoly.plugin.action.tweet.util

import android.os.Debug
import android.util.Log

import com.wa2c.android.medoly.plugin.action.tweet.BuildConfig

import java.io.PrintWriter
import java.io.StringWriter


/**
 * ログ処理用クラス。
 */
object Logger {
    /**
     * デバッグメッセージのタグ名。
     */
    val TAG = "Medoly"


    /**
     * デバッグメッセージを出力する。
     *
     * @param msg メッセージ。
     */
    fun d(msg: Any) {
        if (!BuildConfig.DEBUG) return

        Log.d(TAG, msg.toString())
    }

    /**
     * エラーメッセージを出力する。
     *
     * @param msg メッセージ。
     */
    fun e(msg: Any) {
        var msg = msg
        if (!BuildConfig.DEBUG) return

        if (msg is Exception) {
            val w = StringWriter()
            val pw = PrintWriter(w)
            msg.printStackTrace(pw)
            pw.flush()
            msg = w.toString()
        }

        Log.e(TAG, msg.toString())
    }

    /**
     * 情報メッセージを出力する。
     *
     * @param msg メッセージ。
     */
    fun i(msg: Any) {
        if (!BuildConfig.DEBUG) return

        Log.i(TAG, msg.toString())
    }

    /**
     * 詳細メッセージを出力する。
     *
     * @param msg メッセージ。
     */
    fun v(msg: Any) {
        if (!BuildConfig.DEBUG) return

        Log.v(TAG, msg.toString())
    }

    /**
     * 警告メッセージを出力する。
     *
     * @param msg メッセージ。
     */
    fun w(msg: Any) {
        if (!BuildConfig.DEBUG) return

        Log.w(TAG, msg.toString())
    }

    /**
     * デバッグメッセージを出力する。
     */
    fun heap() {
        if (!BuildConfig.DEBUG) return

        val msg = ("heap : Free=" + java.lang.Long.toString(Debug.getNativeHeapFreeSize() / 1024) + "kb"
                + ", Allocated=" + java.lang.Long.toString(Debug.getNativeHeapAllocatedSize() / 1024) + "kb" + ", Size="
                + java.lang.Long.toString(Debug.getNativeHeapSize() / 1024) + "kb")
        Log.v(TAG, msg)
    }

}
