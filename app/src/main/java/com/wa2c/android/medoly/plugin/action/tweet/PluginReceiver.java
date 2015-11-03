package com.wa2c.android.medoly.plugin.action.tweet;

import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.text.TextUtils;

import com.wa2c.android.medoly.library.AlbumArtProperty;
import com.wa2c.android.medoly.library.MedolyParam;
import com.wa2c.android.medoly.library.PluginOperationCategory;
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


/**
 * メッセージプラグイン受信レシーバ。
 */
public class PluginReceiver extends BroadcastReceiver {

    /** ツイート文字数。 */
    private static final int MESSAGE_LENGTH = 140; // Twitter文字数
    /** 画像URLの文字数 */
    private static final int IMAGE_URL_LENGTH = 24; // 23 + 1 (space)

    /** 前回のファイルパス設定キー。 */
    private static final String PREFKEY_PREVIOUS_MEDIA_URI = "previous_media_uri";

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

        // URIを取得
        Uri mediaUri = null;
        if (intent.getExtras() != null) {
            Object extraStream = intent.getExtras().get(Intent.EXTRA_STREAM);
            if (extraStream != null && extraStream instanceof Uri)
                mediaUri = (Uri)extraStream;
        } else if (intent.getData() != null) {
            // Old version
            mediaUri = intent.getData();
        }

        // 値を取得
        HashMap<String, String> propertyMap = null;
        boolean isEvent = false;
        try {
            if (intent.hasExtra(MedolyParam.PLUGIN_VALUE_KEY)) {
                Serializable serializable = intent.getSerializableExtra(MedolyParam.PLUGIN_VALUE_KEY);
                if (serializable != null) {
                    propertyMap = (HashMap<String, String>) serializable;
                }
            }
            if (propertyMap == null || propertyMap.isEmpty()) { return; }

            if (intent.hasExtra(MedolyParam.PLUGIN_EVENT_KEY))
                isEvent = intent.getBooleanExtra(MedolyParam.PLUGIN_EVENT_KEY, false);
        } catch (ClassCastException | NullPointerException e) {
            Logger.e(e);
            return;
        }

        // カテゴリ取得
        Set<String> categories = intent.getCategories();
        if (categories == null || categories.size() == 0) {
            return;
        }

        if (categories.contains(PluginOperationCategory.OPERATION_PLAY_START.getCategoryValue())) {
            // Play Start
            if (!isEvent || this.sharedPreferences.getBoolean(context.getString(R.string.prefkey_operation_play_start_enabled), false)) {
                post(mediaUri, propertyMap);
            }
        } else if (categories.contains(PluginOperationCategory.OPERATION_PLAY_NOW.getCategoryValue())) {
            // Play Now
            if (!isEvent || this.sharedPreferences.getBoolean(context.getString(R.string.prefkey_operation_play_now_enabled), true)) {
                post(mediaUri, propertyMap);
            }
        } else if (categories.contains(PluginOperationCategory.OPERATION_EXECUTE.getCategoryValue())) {
            // Execute
            final String EXECUTE_TWEET_ID = "execute_id_tweet";
            final String EXECUTE_SITE_ID = "execute_id_site";

            Bundle extras = intent.getExtras();
            if (extras != null) {
                if (extras.keySet().contains(EXECUTE_TWEET_ID)) {
                    sendApp(mediaUri, propertyMap);
                } else if (extras.keySet().contains(EXECUTE_SITE_ID)) {
                    Intent launchIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://twitter.com/"));
                    try {
                        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(launchIntent);
                    } catch (android.content.ActivityNotFoundException e) {
                        Logger.d(e);
                    }
                }
            }
        }
    }



    /**
     * 外部アプリで投稿。
     * @param propertyMap プロパティ情報。
     */
    private void sendApp(Uri uri, HashMap<String, String> propertyMap) {
        // 音楽データ無し
        if (uri == null) {
            AppUtils.showToast(context, R.string.message_no_media);
            return;
        }

        // アルバムアート取得
        File albumArtFile = getAlbumArtFile(propertyMap);

        // メッセージ取得
        int messageMax = (albumArtFile == null) ? MESSAGE_LENGTH : MESSAGE_LENGTH - IMAGE_URL_LENGTH;
        String message = getMessage(messageMax, propertyMap);
        if (TextUtils.isEmpty(message)) {
            return;
        }

        Intent twitterIntent = new Intent();
        twitterIntent.setAction(Intent.ACTION_SEND);
        twitterIntent.setType("text/plain");
        twitterIntent.putExtra(Intent.EXTRA_TEXT, message);

        Uri imageUri = null;
        if (albumArtFile != null) {
            Cursor cursor = null;
            try {
                // 画像のURI取得 (取得パスがエイリアスでMediaStorageのパスと異なる場合は取得できない)
                cursor = context.getContentResolver().query(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        null,
                        MediaStore.Images.Media.DATA + " = ?",
                        new String[]{ albumArtFile.getAbsolutePath() },
                        null);
                if (cursor.moveToFirst()) {
                    long id = cursor.getLong(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
                    imageUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                }
            } catch (Exception e) {
                Logger.e(e);
            } finally {
                if (cursor != null && !cursor.isClosed()) cursor.close();
            }
            if (imageUri == null) {
                // DBから取得できない場合はfile://スキーマを使用
                imageUri = Uri.parse("file://" + albumArtFile.getPath());
            }
            twitterIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
        }

        twitterIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        twitterIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        context.startActivity(twitterIntent);
    }

    /**
     * 投稿。
     * @param uri URI。
     * @param propertyMap プロパティ情報。
     */
    @SuppressWarnings("unchecked")
    private void post(Uri uri, HashMap<String, String> propertyMap) {
        // 音楽データ無し
        if (uri == null) {
            AppUtils.showToast(context, R.string.message_no_media);
            return;
        }

        String mediaUri = uri.toString();
        String previousMediaUri = sharedPreferences.getString(PREFKEY_PREVIOUS_MEDIA_URI, "");
        boolean previousMediaEnabled = sharedPreferences.getBoolean(context.getString(R.string.prefkey_previous_media_enabled), false);
        if (!previousMediaEnabled && !TextUtils.isEmpty(mediaUri) && !TextUtils.isEmpty(previousMediaUri) && mediaUri.equals(previousMediaUri)) {
            // 前回と同じメディアは無視
            return;
        }
        sharedPreferences.edit().putString(PREFKEY_PREVIOUS_MEDIA_URI, mediaUri).apply();

        (new AsyncPostTask(propertyMap)).execute();
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
                // アルバムアート取得
                File albumArtFile =getAlbumArtFile(propertyMap);

                // メッセージ取得
                int messageMax = (albumArtFile == null) ? MESSAGE_LENGTH : MESSAGE_LENGTH - IMAGE_URL_LENGTH;
                String message = getMessage(messageMax, propertyMap);
                if (TextUtils.isEmpty(message)) {
                    return false;
                }

                // ツイート
                twitter.updateStatus(new StatusUpdate(message).media(albumArtFile));

                return true;
            } catch (Exception e) {
                Logger.e(e);
                return false;
            }
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

    /**
     * メッセージを取得する。
     * @param messageMax 最大文字数。
     * @param propertyMap プロパティ情報マップ。
     * @return メッセージ。
     */
    private String getMessage(final int messageMax, final Map<String, String> propertyMap) {
        final String TAG_EXP = "%([^%]+)%"; // メタタグ
        final String format = sharedPreferences.getString(context.getString(R.string.prefkey_content_format), context.getString(R.string.format_content_default));
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

    /**
     * アルバムアートファイルを取得する。
     * @param propertyMap プロパティ情報マップ。
     * @return アルバムアートファイルファイル。
     */
    private File getAlbumArtFile( final Map<String, String> propertyMap) {
        File albumArtFile = null;
        if (sharedPreferences.getBoolean(context.getString(R.string.prefkey_content_album_art), true)) {
            String albumArtPath = propertyMap.get(AlbumArtProperty.FOLDER_PATH.getKeyName()) + propertyMap.get(AlbumArtProperty.FILE_NAME.getKeyName());
            if (!TextUtils.isEmpty(albumArtPath)) {
                albumArtFile = new File(albumArtPath);
                if (!albumArtFile.exists()) {
                    albumArtFile = null;
                }
            }
        }
        return albumArtFile;
    }


}
