package com.wa2c.android.medoly.plugin.action.tweet.util;

import android.content.Context;

import com.wa2c.android.medoly.plugin.action.tweet.util.ToastReceiver;

public class AppUtils {

    /**
     * トーストを表示。
     * @param context コンテキスト。
     * @param text メッセージ。
     */
    public static void showToast(Context context, String text) {
        ToastReceiver.showToast(context, text);
    }

    /**
     * トーストを表示。
     * @param context コンテキスト。
     * @param stringId メッセージ。
     */
    public static void showToast(final Context context, final int stringId) {
        showToast(context, context.getString(stringId));
    }

}
