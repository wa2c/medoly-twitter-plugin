package com.wa2c.android.medoly.plugin.action.tweet.util;

import android.content.Context;
import android.content.Intent;

import com.wa2c.android.medoly.plugin.action.tweet.service.PostIntentService;

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


    /**
     * Start service.
     * @param context A context.
     * @param intent A received intent.
     */
    public static void startService(Context context, Intent intent) {
        // Stop exists service
        Intent stopIntent = new Intent(context, PostIntentService.class);
        context.stopService(stopIntent);

        // Launch service
        Intent serviceIntent = new Intent(intent);
        serviceIntent.putExtra(PostIntentService.RECEIVED_CLASS_NAME, intent.getComponent().getClassName());
        serviceIntent.setClass(context, PostIntentService.class);
        context.startService(serviceIntent);
    }
}
