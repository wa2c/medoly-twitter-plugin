package com.wa2c.android.medoly.plugin.action.tweet.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.wa2c.android.medoly.library.MediaPluginIntent;
import com.wa2c.android.medoly.library.PluginOperationCategory;
import com.wa2c.android.medoly.plugin.action.tweet.R;

/**
 * Execute receiver.
 */
public class PluginReceivers {

    public static abstract class AbstractPluginReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            MediaPluginIntent serviceIntent = new MediaPluginIntent(intent);
            serviceIntent.putExtra(AbstractPluginService.RECEIVED_CLASS_NAME, this.getClass().getName());

            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            if (this instanceof EventPostTweetReceiver) {
                try {
                    PluginOperationCategory operation = PluginOperationCategory.valueOf(pref.getString(context.getString(R.string.prefkey_event_tweet_operation), ""));
                    if (serviceIntent.hasCategory(PluginOperationCategory.OPERATION_EXECUTE) || serviceIntent.hasCategory(operation)) {
                        serviceIntent.setClass(context, PluginPostService.class);
                    }
                } catch (Exception ignore) { }
            } else if (this instanceof ExecutePostTweetReceiver ||
                       this instanceof ExecuteOpenTwitterReceiver) {
                serviceIntent.setClass(context, PluginRunService.class);
            }

            context.stopService(serviceIntent);
            context.startService(serviceIntent);
        }
    }

    // Event

    public static class EventPostTweetReceiver extends AbstractPluginReceiver { }

    // Execution

    public static class ExecutePostTweetReceiver extends AbstractPluginReceiver { }

    public static class ExecuteOpenTwitterReceiver extends AbstractPluginReceiver { }

}
