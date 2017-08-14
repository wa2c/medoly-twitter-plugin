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

}
