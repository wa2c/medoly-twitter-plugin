package com.wa2c.android.medoly.plugin.action.tweet.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.wa2c.android.medoly.plugin.action.tweet.util.AppUtils;

/**
 * Execute receiver.
 */
public class PluginReceivers {

    public static abstract class AbstractPluginReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Intent serviceIntent = new Intent(intent);
            serviceIntent.putExtra(ProcessService.RECEIVED_CLASS_NAME, intent.getComponent().getClassName());
            serviceIntent.setClass(context, ProcessService.class);
            context.startService(serviceIntent);
        }
    }

    // Event

    public static class EventPostTweetReceiver extends AbstractPluginReceiver { }

    // Execution

    public static class ExecutePostTweetReceiver extends AbstractPluginReceiver { }

    public static class ExecuteOpenTwitterReceiver extends AbstractPluginReceiver { }

}
