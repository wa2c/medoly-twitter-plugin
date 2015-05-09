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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            if (!TextUtils.isEmpty(filePath) && !TextUtils.isEmpty(previousMediaPath) && filePath.equals(previousMediaPath) && !previousMediaEnabled) {
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

        //private final String TAG_EXP = "(^|[^%])(%%)*%([^%]+)%(%%)*([^%]|$)";
        private final String TAG_EXP = "%([^%]+)%"; // メタタグ
        @Override
        protected Boolean doInBackground(String... params) {
            try {
                // アルバムアート有無
                File albumArtFile = null;
                if (sharedPreferences.getBoolean(context.getString(R.string.prefkey_content_album_art), true)) {
                    String albumArtPath = propertyMap.get(ActionPluginParam.AlbumArtProperty.FOLDER_PATH.getKeyName()) + propertyMap.get(ActionPluginParam.AlbumArtProperty.FILE_NAME.getKeyName());
                    if (!TextUtils.isEmpty(albumArtPath)) {
                        albumArtFile = new File(albumArtPath);
                        if (!albumArtFile.exists()) {
                            albumArtFile = null;
                        }
                    }
                }

                // メッセージ取得
                int messageMax = (albumArtFile == null) ? MESSAGE_LENGTH : MESSAGE_LENGTH - IMAGE_URL_LENGTH;
                String message = getMessage(messageMax);
                if (TextUtils.isEmpty(message)) {
                    return false;
                }

                twitter.updateStatus(new StatusUpdate(message).media(albumArtFile));

//                if (albumArtFile != null) {
////                    int length = MESSAGE_LENGTH - IMAGE_URL_LENGTH - 4;
////                    if (message.length() > length) {
////                        message = message.substring(0, length) + "... ";
////                    }
//                    twitter.updateStatus(new StatusUpdate(message).media(albumArtFile));
//                } else {
////                    int length = MESSAGE_LENGTH - 4;
////                    if (message.length() > length) {
////                        message = message.substring(0, length) + "... ";
////                    }
//                    twitter.updateStatus(message);
//                }

                return true;
            } catch (TwitterException e) {
                Logger.e(e);
                return false;
            }
        }

        /**
         * メッセージを取得する。
         * @param messageMax 最大文字数。
         * @return メッセージ。
         */
        private String getMessage(final int messageMax) {
            String format = sharedPreferences.getString(context.getString(R.string.prefkey_content_format), context.getString(R.string.format_content_default));
            if (TextUtils.isEmpty(format)) {
                return null;
            }
            boolean isTrim = sharedPreferences.getBoolean(context.getString(R.string.prefkey_trim_before_empty_enabled), true);

            // フォーマットに含まれているタグ取得
            HashSet<String> propertyKeySet = new HashSet<>();
            Matcher tagMatcher = Pattern.compile(TAG_EXP, Pattern.MULTILINE).matcher(format);
            while (tagMatcher.find()) {
                if (tagMatcher.groupCount() > 0) {
                    propertyKeySet.add(tagMatcher.group(1));
                }
            }

            // フォーマットの内容を置換え
            String tempText = format.replaceAll(TAG_EXP, "");
            int textCount = tempText.length();
            if (textCount > messageMax) {
                // 置換え無しで文字数オーバー
                return tempText.substring(0, messageMax - 4) + "... ";
            }

            // 優先度の高い順に置換え
            String outputMessage = format;
            boolean replaceComplete = false;
            List<PropertyItem> priorityList = PropertyItem.loadPropertyPriority(context);
            for (PropertyItem propertyItem : priorityList) {
                if (!propertyKeySet.contains(propertyItem.propertyKey))
                    continue;

                // 置換えテキスト取得
                String replaceText = "";
                if (propertyMap.containsKey(propertyItem.propertyKey))
                    replaceText = propertyMap.get(propertyItem.propertyKey);

                // 検索
                Matcher matcher;
                if (replaceComplete || TextUtils.isEmpty(replaceText) && isTrim) {
                    matcher = Pattern.compile("(\\w*)%" + propertyItem.propertyKey + "%").matcher(outputMessage); // タグ削除
                } else {
                    matcher = Pattern.compile("%" + propertyItem.propertyKey + "%").matcher(outputMessage); // タグ置換
                }

                while (matcher.find()) {
                    if (replaceComplete || TextUtils.isEmpty(replaceText) && isTrim) {
                        // タグを削除する
                        outputMessage = matcher.replaceFirst("");
                        textCount -= matcher.group(1).length();
                    } else {
                        int remain = messageMax - textCount;

                        if (remain >= replaceText.length()) {
                            // 文字数内に収まる
                            outputMessage = matcher.replaceFirst(replaceText);
                            textCount += replaceText.length();
                        } else {
                            // 文字数内に収まらない
                            if (propertyItem.omissible && remain > 4) {
                                // 省略
                                replaceText = replaceText.substring(0, remain - 4) + "... ";
                                int newlineIndex = replaceText.lastIndexOf("\n");
                                if (sharedPreferences.getBoolean(context.getString(R.string.prefkey_omit_newline), true) && newlineIndex > 0) {
                                    replaceText = replaceText.substring(0, newlineIndex) + "... ";
                                }
                                outputMessage = matcher.replaceFirst(replaceText);
                                textCount += replaceText.length();
                            } else {
                                outputMessage = matcher.replaceAll("");
                            }
                            replaceComplete = true; // 完了
                        }
                    }
                }
            }

            return outputMessage;
        }


        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                if (sharedPreferences.getBoolean(context.getString(R.string.prefkey_tweet_success_message_show), false)) {
                    AppUtils.showToast(context, R.string.message_post_success);
                }
            } else {
                if (sharedPreferences.getBoolean(context.getString(R.string.prefkey_tweet_failure_message_show), true)) {
                    AppUtils.showToast(context, R.string.message_post_failure);
                }
            }
        }
    }


}
