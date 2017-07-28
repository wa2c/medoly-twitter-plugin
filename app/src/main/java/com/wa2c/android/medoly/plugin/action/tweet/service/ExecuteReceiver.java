package com.wa2c.android.medoly.plugin.action.tweet.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.wa2c.android.medoly.plugin.action.tweet.util.AppUtils;

/**
 * Execute receiver.
 */
public class ExecuteReceiver {

    public static class ExecutePostTweetReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            AppUtils.startService(context, intent);
        }
    }

    public static class ExecuteOpenTwitterReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            AppUtils.startService(context, intent);
        }
    }

}
