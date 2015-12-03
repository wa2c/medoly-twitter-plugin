package com.wa2c.android.medoly.plugin.action.tweet;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


/**
 * メッセージプラグイン受信レシーバ。
 */
public class PluginReceiver extends BroadcastReceiver {

    /**
     * メッセージ受信。
     * @param context コンテキスト。
     * @param intent インテント。
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent serviceIntent = new Intent(intent);
        serviceIntent.setClass(context, PostIntentService.class);
        context.startService(serviceIntent);
    }
}
