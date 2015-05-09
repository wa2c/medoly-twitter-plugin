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
                String albumArtPath = propertyMap.get(ActionPluginParam.AlbumArtProperty.FOLDER_PATH.getKeyName()) + propertyMap.get(ActionPluginParam.AlbumArtProperty.FILE_NAME.getKeyName());

//                String format = sharedPreferences.getString(context.getString(R.string.prefkey_content_format), context.getString(R.string.format_content_default));
//                boolean isTrim = sharedPreferences.getBoolean(context.getString(R.string.prefkey_trim_before_empty_enabled), true);

                File f = null;
                if (sharedPreferences.getBoolean(context.getString(R.string.prefkey_content_album_art), true) && !TextUtils.isEmpty(albumArtPath)) {
                    f = new File(albumArtPath);
                    if (!f.exists()) {
                        f = null;
                    }
                }

                int messageMax = (f == null) ? MESSAGE_LENGTH : MESSAGE_LENGTH - IMAGE_URL_LENGTH;


//                // フォーマットに含まれているタグ取得
//                HashSet<String> propertyKeySet = new HashSet<>();
//                Matcher matcher = Pattern.compile(TAG_EXP, Pattern.MULTILINE).matcher(format);
//                while (matcher.find()) {
//                    if (matcher.groupCount() > 0) {
//                        propertyKeySet.add(matcher.group(1));
//                    }
//                }
//
//                // フォーマットの内容を置換え
//                int textCount = format.replaceAll(TAG_EXP, "").length();
//
//                String message = format;
//                List<PropertyItem> priorityList = PropertyItem.loadPropertyPriority(context);
//                for (PropertyItem item : priorityList) {
//                    if (!propertyKeySet.contains(item.propertyKey))
//                        continue;
//
//                    String replaceText = "";
//                    if (propertyMap.containsKey(item.propertyKey))
//                        replaceText = propertyMap.get(item.propertyKey);
//
//                    String tempText;
//                    if (!TextUtils.isEmpty(replaceText)) {
//                        tempText = message.replaceAll("%" + item.propertyKey + "%", replaceText);
//                    } else {
//                        if (isTrim)
//                            tempText = message.replaceAll("\\w*%" + item.propertyKey + "%", "");
//                    }
//
//                    if ()
//
//                    message = tempText;
//                   // String tempText =  message.replaceFirst("%" + item.propertyKey + "%", replaceText);
//
//
//
//
//
////                    String keyName = item.propertyKey;
////                    String val = propertyMap.get(keyName);
////                    if (!TextUtils.isEmpty(val)) {
////                        message = message.replaceAll("%" + keyName + "%", val);
////                    } else {
////                        if (isTrim)
////                            message = message.replaceAll("\\w*%" + keyName + "%", "");
////                    }
//
//                }




//                // メディア
//                for (ActionPluginParam.MediaProperty property : ActionPluginParam.MediaProperty.values()) {
//                    String keyName = property.getKeyName();
//                    if (!propertyKeySet.contains(keyName))
//                        continue;
//                    String val = propertyMap.get(keyName);
//                    if (!TextUtils.isEmpty(val)) {
//                        message = message.replaceAll("%" + keyName + "%", val);
//                    } else {
//                        if (isTrim)
//                            message = message.replaceAll("\\w*%" + keyName + "%", "");
//                    }
//                }
//
//                // アルバムアート
//                for (ActionPluginParam.AlbumArtProperty property : ActionPluginParam.AlbumArtProperty.values()) {
//                    String keyName = property.getKeyName();
//                    if (!propertyKeySet.contains(keyName))
//                        continue;
//                    String val = propertyMap.get(keyName);
//                    if (!TextUtils.isEmpty(val)) {
//                        message = message.replaceAll("%" + keyName + "%", val);
//                    } else {
//                        if (isTrim)
//                            message = message.replaceAll("\\w*%" + keyName + "%", "");
//                    }
//                }
//
//                // 歌詞
//                for (ActionPluginParam.LyricsProperty property : ActionPluginParam.LyricsProperty.values()) {
//                    String keyName = property.getKeyName();
//                    if (!propertyKeySet.contains(keyName))
//                        continue;
//                    String val = propertyMap.get(keyName);
//                    if (!TextUtils.isEmpty(val)) {
//                        message = message.replaceAll("%" + keyName + "%", val);
//                    } else {
//                        if (isTrim)
//                            message = message.replaceAll("\\w*%" + keyName + "%", "");
//                    }
//                }

                String message = getMessage(messageMax);
                if (TextUtils.isEmpty(message)) {
                    return false;
                }



                if (f != null) {
//                    int length = MESSAGE_LENGTH - IMAGE_URL_LENGTH - 4;
//                    if (message.length() > length) {
//                        message = message.substring(0, length) + "... ";
//                    }
                    twitter.updateStatus(new StatusUpdate(message).media(f));
                } else {
//                    int length = MESSAGE_LENGTH - 4;
//                    if (message.length() > length) {
//                        message = message.substring(0, length) + "... ";
//                    }
                    twitter.updateStatus(message);
                }

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
            String tempText = format.replaceAll((isTrim ? "\\w*" : "")  + TAG_EXP, "");
            int textCount = tempText.length();
            if (textCount > messageMax) {
                // 置換え無しで文字数オーバー
                return tempText.substring(0, messageMax - 4) + "... ";
            }

            // 優先度の高い順に置換え
            String message = format;
            boolean replaceComplete = false;
            List<PropertyItem> priorityList = PropertyItem.loadPropertyPriority(context);
            for (PropertyItem propertyItem : priorityList) {
                if (!propertyKeySet.contains(propertyItem.propertyKey))
                    continue;

                String replaceText = "";
                if (propertyMap.containsKey(propertyItem.propertyKey))
                    replaceText = propertyMap.get(propertyItem.propertyKey);

                Matcher matcher;
                if (replaceComplete || TextUtils.isEmpty(replaceText) && isTrim) {
                    matcher = Pattern.compile("(\\w*)%" + propertyItem.propertyKey + "%").matcher(message); // タグ削除
                } else {
                    matcher = Pattern.compile("%" + propertyItem.propertyKey + "%").matcher(message); // タグ残す
                }

                while (matcher.find()) {
                    if (matcher.groupCount() > 0) {
                        // タグを削除する
                        textCount -= matcher.group(1).length();
                    } else {
                        int remain = messageMax - textCount - replaceText.length();

                        if (remain > 0) {
                            // 文字数内に収まる
                            message = matcher.replaceFirst(replaceText);
                            textCount += replaceText.length();
                        } else {
                            remain *= -1;
                            // 文字数内に収まらない
                            if (propertyItem.omissible && (remain >= 4)) {
                                // 省略
                                replaceText = replaceText.substring(0, messageMax - textCount - 4) + "... ";
                                message = matcher.replaceFirst(replaceText);
                            } else {
                                // 削除
                                message = matcher.replaceFirst("");
                            }
                            replaceComplete = true; // 完了
                        }
                    }



//                    afterText = matcher.replaceFirst(replaceText);
//
//                    int matchCount = matcher.end() -  matcher.start();
//                    int
//
//                    tempText = message.replaceAll((isTrim ? "\\w*" : "")  + TAG_EXP, "");
//                    int textCount = tempText.length();
//                    if (textCount > messageMax) {
//                        // 文字数オーバー
//                        return tempText.substring(0, messageMax - 4) + "... ";
//                    }
//
//                    if (matcher.groupCount() > 0) {
//
//                    }
                }



//                String afterText = "";
//                if (TextUtils.isEmpty(replaceText) && isTrim) {
//                    afterText = message.replaceAll("\\w*%" + item.propertyKey + "%", "");
//                } else {
//                    afterText = message.replaceAll("%" + item.propertyKey + "%", replaceText);
//                }
//
//                tempText = afterText.replaceAll((isTrim ? "\\w*" : "")  + TAG_EXP, "");
//                if (tempText.length() > messageMax) {
//                    return message;
//                }


               // message = afterText;
                // String tempText =  message.replaceFirst("%" + item.propertyKey + "%", replaceText);





//                    String keyName = item.propertyKey;
//                    String val = propertyMap.get(keyName);
//                    if (!TextUtils.isEmpty(val)) {
//                        message = message.replaceAll("%" + keyName + "%", val);
//                    } else {
//                        if (isTrim)
//                            message = message.replaceAll("\\w*%" + keyName + "%", "");
//                    }



            }

            return message;
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
