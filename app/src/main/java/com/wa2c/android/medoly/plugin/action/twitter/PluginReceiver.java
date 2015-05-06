package com.wa2c.android.medoly.plugin.action.twitter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.wa2c.android.medoly.plugin.action.ActionPluginParam;
import com.wa2c.android.medoly.plugin.action.Logger;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;



/**
 * メッセージプラグイン受信レシーバ。
 */
public class PluginReceiver extends BroadcastReceiver {

    /** ツイート文字数。 */
    private static final int MESSAGE_LENGTH = 140; // Twitter文字数
    /** 画像URLの文字数 */
    private static final int IMAGE_URL_LENGTH = 24; // 23 + 1 (space)

    /** 値マップのキー。 */
    private static final String PLUGIN_VALUE_KEY  = "value_map";
    /** 前回のファイルパス設定キー。 */
    private static final String PREFKEY_PREVIOUS_MEDIA_PATH = "previous_media_path";

    /** コンテキスト。 */
    private Context context;
    /** 設定。 */
    private SharedPreferences sharedPreferences;
    /** Twitter。 */
    private Twitter twitter;


    /**
     * メッセージ受信。
     * @param context コンテキスト。
     * @param intent インテント。
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.twitter = TwitterUtils.getTwitterInstance(context);

        Set<String> categories = intent.getCategories();
       if (categories.contains(ActionPluginParam.PluginOperationCategory.OPERATION_PLAY_START.getCategoryValue())) {
           // 再生開始
           if (this.sharedPreferences.getBoolean(context.getString(R.string.prefkey_operation_play_start_enabled), false)) {
               post(intent);
           }
        } else if (categories.contains(ActionPluginParam.PluginOperationCategory.OPERATION_PLAY_NOW.getCategoryValue())) {
           // 再生中
           if (this.sharedPreferences.getBoolean(context.getString(R.string.prefkey_operation_play_now_enabled), true)) {
               post(intent);
           }
       }
    }

    /**
     * 投稿前準備。
     * @param intent インテント。
     */
    @SuppressWarnings("unchecked")
    private void post(Intent intent) {
        Serializable serializable = intent.getSerializableExtra(PLUGIN_VALUE_KEY);
        if (serializable != null) {
            HashMap<String, String> propertyMap = (HashMap<String, String>) serializable;

            String filePath = propertyMap.get(ActionPluginParam.MediaProperty.FOLDER_PATH.getKeyName()) + propertyMap.get(ActionPluginParam.MediaProperty.FILE_NAME.getKeyName());
            String previousMediaPath = sharedPreferences.getString(PREFKEY_PREVIOUS_MEDIA_PATH, "");
            boolean previousMediaEnabled = sharedPreferences.getBoolean(context.getString(R.string.prefkey_previous_media_enabled), false);
            if (!TextUtils.isEmpty(filePath) && !TextUtils.isEmpty(previousMediaPath) && filePath.equals(previousMediaPath) && previousMediaEnabled) {
                // 前回と同じメディアは無視
                return;
            }

           // post((HashMap<String, String>) serializable);
           (new AsyncPostTask(propertyMap)).execute();
           sharedPreferences.edit().putString(PREFKEY_PREVIOUS_MEDIA_PATH, filePath).apply();
        }
    }

    /**
     * 投稿タスク。
     */
    private class AsyncPostTask extends AsyncTask<String, Void, Boolean> {
        /** プロパティマップ。 */
        private Map<String, String> propertyMap;

        public AsyncPostTask(Map<String, String> propertyMap) {
            this.propertyMap = propertyMap;
        }

        @Override
        protected Boolean doInBackground(String... params) {
            try {
                String albumArtPath = propertyMap.get(ActionPluginParam.AlbumArtProperty.FOLDER_PATH.getKeyName()) + propertyMap.get(ActionPluginParam.AlbumArtProperty.FILE_NAME.getKeyName());
                String format = sharedPreferences.getString(context.getString(R.string.prefkey_content_format), context.getString(R.string.format_content_default));
                boolean isTrim = sharedPreferences.getBoolean(context.getString(R.string.prefkey_trim_before_empty_enabled), true);

                String message = format;
                for (ActionPluginParam.MediaProperty property : ActionPluginParam.MediaProperty.values()) {
                    String keyName = property.getKeyName();
                    if (!propertyMap.containsKey(keyName)) continue;
                    String val = propertyMap.get(keyName);

                    if (!TextUtils.isEmpty(val)) {
                        message = message.replaceAll("%" + keyName + "%", val);
                    } else {
                        if (isTrim)
                            message = message.replaceAll("\\w*%" + keyName + "%", "");
                    }
                }

                for (ActionPluginParam.AlbumArtProperty property : ActionPluginParam.AlbumArtProperty.values()) {
                    String keyName = property.getKeyName();
                    if (!propertyMap.containsKey(keyName)) continue;
                    String val = propertyMap.get(keyName);

                    if (!TextUtils.isEmpty(val)) {
                        message = message.replaceAll("%" + keyName + "%", val);
                    } else {
                        if (isTrim)
                            message = message.replaceAll("\\w*%" + keyName + "%", "");
                    }
                }

                for (ActionPluginParam.LyricsProperty property : ActionPluginParam.LyricsProperty.values()) {
                    String keyName = property.getKeyName();
                    if (!propertyMap.containsKey(keyName)) continue;
                    String val = propertyMap.get(keyName);

                    if (!TextUtils.isEmpty(val)) {
                        message = message.replaceAll("%" + keyName + "%", val);
                    } else {
                        if (isTrim)
                            message = message.replaceAll("\\w*%" + keyName + "%", "");
                    }
                }

                File f = null;
                if (sharedPreferences.getBoolean(context.getString(R.string.prefkey_content_album_art), true) && !TextUtils.isEmpty(albumArtPath)) {
                    f = new File(albumArtPath);
                    if (!f.exists()) {
                        f = null;
                   }
                }

                if (f != null) {
                    int length = MESSAGE_LENGTH - IMAGE_URL_LENGTH - 4;
                    if (message.length() > length) {
                        message = message.substring(0, length) + "... ";
                    }
                    twitter.updateStatus(new StatusUpdate(message).media(f));
                } else {
                    int length = MESSAGE_LENGTH - 4;
                    if (message.length() > length) {
                        message = message.substring(0, length) + "... ";
                    }
                    twitter.updateStatus(message);
                }

                return true;
            } catch (TwitterException e) {
                Logger.e(e);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                AppUtils.showToast(context, R.string.message_post_completed);
            } else {
                AppUtils.showToast(context, R.string.message_post_failed);
            }
        }
    }


}
