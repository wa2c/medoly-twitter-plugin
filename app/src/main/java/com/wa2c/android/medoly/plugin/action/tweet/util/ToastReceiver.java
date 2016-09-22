package com.wa2c.android.medoly.plugin.action.tweet.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

/**
 * トースト表示レシーバ。(IntentServiceからToastを表示するため。)
 */
public class ToastReceiver extends BroadcastReceiver {
    public static final String MESSAGE_TOAST = "message_toast";

    @Override
    public void onReceive(Context context, Intent intent) {
        Toast.makeText(context, intent.getStringExtra(MESSAGE_TOAST), Toast.LENGTH_SHORT).show();
    }

    public static void showToast(Context context, String text) {
        // IntentService起動
        Intent intent = new Intent(context, ToastReceiver.class);
        intent.putExtra(MESSAGE_TOAST, text);
        context.sendBroadcast(intent);
    }
}