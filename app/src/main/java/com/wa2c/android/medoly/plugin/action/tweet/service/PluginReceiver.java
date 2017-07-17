package com.wa2c.android.medoly.plugin.action.tweet.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.wa2c.android.medoly.plugin.action.tweet.service.PostIntentService;


/**
 * メッセージプラグイン受信レシーバ。
 */
public class PluginReceiver extends BroadcastReceiver {

    /**
     * Receive message.
     * @param context A context.
     * @param intent Received intent.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent serviceIntent = new Intent(intent);
        serviceIntent.setClass(context, PostIntentService.class);
        context.startService(serviceIntent);
    }
}
