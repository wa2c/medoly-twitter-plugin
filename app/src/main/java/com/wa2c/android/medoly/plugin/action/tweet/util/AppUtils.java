package com.wa2c.android.medoly.plugin.action.tweet.util;

import android.content.Context;
import android.content.Intent;

import com.wa2c.android.medoly.plugin.action.tweet.service.ProcessService;

public class AppUtils {

    /**
     * Show message.
     * @param context context.
     * @param text message.
     */
    public static void showToast(Context context, String text) {
        ToastReceiver.showToast(context, text);
    }

    /**
     * Show message.
     * @param context context
     * @param stringId resource id.
     */
    public static void showToast(Context context, int stringId) {
        ToastReceiver.showToast(context, stringId);
    }

    /**
     * Start service.
     * @param context A context.
     * @param intent A received intent.
     */
    public static void startService(Context context, Intent intent) {
        // Stop exists service
        Intent stopIntent = new Intent(context, ProcessService.class);
        context.stopService(stopIntent);

        // Launch service
        Intent serviceIntent = new Intent(intent);
        serviceIntent.putExtra(ProcessService.RECEIVED_CLASS_NAME, intent.getComponent().getClassName());
        serviceIntent.setClass(context, ProcessService.class);
        context.startService(serviceIntent);
    }

}
