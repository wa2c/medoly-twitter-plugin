package com.wa2c.android.medoly.plugin.action.twitter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.widget.Toast;

import com.wa2c.android.medoly.plugin.action.ActionPluginParam;

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

    /** 値マップのキー。 */
    private final String PLUGIN_VALUE_KEY  = "value_map";
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
    @SuppressWarnings("unchecked")
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.twitter = TwitterUtils.getTwitterInstance(context);

        Set<String> categories = intent.getCategories();
       if (categories.contains(ActionPluginParam.PluginOperationCategory.OPERATION_PLAY_START.getCategoryValue())) {
           // 再生開始
           if (this.sharedPreferences.getBoolean(context.getString(R.string.prefkey_operation_play_start_enabled), false)) {
               preparePost(intent);
           }
        } else if (categories.contains(ActionPluginParam.PluginOperationCategory.OPERATION_PLAY_NOW.getCategoryValue())) {
           // 再生中
           if (this.sharedPreferences.getBoolean(context.getString(R.string.prefkey_operation_play_now_enabled), true)) {
               preparePost(intent);
           }
       }
    }

    /**
     * 投稿前準備。
     * @param intent インテント。
     */
    private void preparePost(Intent intent) {
        Serializable serializable = intent.getSerializableExtra(PLUGIN_VALUE_KEY);
        if (serializable != null) {
            post((HashMap<String, String>) serializable);
        }
    }

    /**
     * 投稿。
     * @param propertyMap プロパティ情報。
     */
    private void post(Map<String, String> propertyMap) {
        String filePath = propertyMap.get(ActionPluginParam.MediaProperty.FOLDER_PATH.getKeyName() + ActionPluginParam.MediaProperty.FILE_NAME.getKeyName());
        String previousMediaPath = sharedPreferences.getString(PREFKEY_PREVIOUS_MEDIA_PATH, "");

        if (!TextUtils.isEmpty(filePath) && !TextUtils.isEmpty(previousMediaPath) && filePath.equals(previousMediaPath)) {
            // 前回と同じメディアは無視
            return;
        }

        final String title = propertyMap.get(ActionPluginParam.MediaProperty.TITLE.getKeyName());
        final String album = propertyMap.get(ActionPluginParam.MediaProperty.ALBUM.getKeyName());
        final String artist = propertyMap.get(ActionPluginParam.MediaProperty.ARTIST.getKeyName());
        final String lyrics = propertyMap.get(ActionPluginParam.LyricsProperty.LYRICS.getKeyName());
        final String albumArt = propertyMap.get(ActionPluginParam.AlbumArtProperty.FOLDER_PATH.getKeyName()) + propertyMap.get(ActionPluginParam.AlbumArtProperty.FILE_NAME.getKeyName());

        String message = title + " - " + artist + " " + album + " " + lyrics;

        sharedPreferences.edit().putString(PREFKEY_PREVIOUS_MEDIA_PATH, filePath).apply();


        AsyncTask<String, Void, Boolean> task = new AsyncTask<String, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(String... params) {
                try {
                    if (!TextUtils.isEmpty(albumArt)) {
                        File f = new File(albumArt);

                        String message = params[0];
                        message = message.substring(0, 100) + "...";
                        if (f.exists()) {
                            twitter.updateStatus(new StatusUpdate(message).media(f));
                        } else {
                            twitter.updateStatus(message);
                            return true;
                        }
                    }

                    return true;
                } catch (TwitterException e) {
                    e.printStackTrace();
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

//                // 終了
//                stopService(new Intent(PostService.this, PostService.class));
            }
        };
        task.execute(message);
    }

}
