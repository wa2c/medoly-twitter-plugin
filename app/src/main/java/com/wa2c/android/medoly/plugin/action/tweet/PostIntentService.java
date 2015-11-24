package com.wa2c.android.medoly.plugin.action.tweet;

import android.app.IntentService;
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
import com.wa2c.android.medoly.library.MediaProperty;
import com.wa2c.android.medoly.library.MedolyParam;
import com.wa2c.android.medoly.library.PluginOperationCategory;
import com.wa2c.android.medoly.utils.Logger;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
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
 * 投稿サービス。
 */
public class PostIntentService extends IntentService {

    /** ツイート文字数。 */
    private static final int MESSAGE_LENGTH = 140; // Twitter文字数
    /** 画像URLの文字数 */
    private static final int IMAGE_URL_LENGTH = 24; // 23 + 1 (space)
    /** 前回のファイルパス設定キー。 */
    private static final String PREFKEY_PREVIOUS_MEDIA_URI = "previous_media_uri";

    /**
     * 投稿種別。
     */
    private enum PostType {
        /** Tweet */
        TWEET,
        /** Send App */
        SEND_APP,
    }


    /**
     * 投稿結果。
     */
    private enum PostResult {
        /** 成功。 */
        SUCCEEDED,
        /** 失敗。 */
        FAILED,
        /** 認証失敗。 */
        AUTH_FAILED,
        /** 投稿一時保存。 */
        SAVED,
        /** 無視。 */
        IGNORE
    }




    /** コンテキスト。 */
    private Context context = null;
    /** 設定。 */
    private SharedPreferences sharedPreferences = null;
    /** 受信データ。 */
    private HashMap<String, String> propertyMap = null;
    /** メディアURI。 */
    private Uri mediaUri = null;
    /** Twitter。 */
    private Twitter twitter;



    /**
     * コンストラクタ。
     */
    public PostIntentService() {
        super(PostIntentService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null)
            return;

        try {
            this.context = getApplicationContext();
            this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            this.twitter = TwitterUtils.getTwitterInstance(context);

            Bundle extras = intent.getExtras();

            // URIを取得
            Object extraStream;
            if (extras != null && (extraStream = intent.getExtras().get(Intent.EXTRA_STREAM)) != null && extraStream instanceof Uri) {
                mediaUri = (Uri) extraStream;
            } else if (intent.getData() != null) {
                // Old version
                mediaUri = intent.getData();
            }


            // 値を取得
            boolean isEvent = false;
            try {
                if (intent.hasExtra(MedolyParam.PLUGIN_VALUE_KEY)) {
                    Serializable serializable = intent.getSerializableExtra(MedolyParam.PLUGIN_VALUE_KEY);
                    if (serializable != null) {
                        propertyMap = (HashMap<String, String>) serializable;
                    }
                }
                if (propertyMap == null || propertyMap.isEmpty()) {
                    return;
                }

                if (intent.hasExtra(MedolyParam.PLUGIN_EVENT_KEY))
                    isEvent = intent.getBooleanExtra(MedolyParam.PLUGIN_EVENT_KEY, false);
            } catch (ClassCastException | NullPointerException e) {
                Logger.e(e);
                return;
            }

            // カテゴリを取得
            Set<String> categories = intent.getCategories();
            if (categories == null || categories.size() == 0) {
                return;
            }

            // 各アクション実行
            if (categories.contains(PluginOperationCategory.OPERATION_PLAY_START.getCategoryValue())) {
                // Play Start
                if (!isEvent || this.sharedPreferences.getBoolean(context.getString(R.string.prefkey_operation_play_start_enabled), false)) {
                    post(PostType.TWEET);
                }
            } else if (categories.contains(PluginOperationCategory.OPERATION_PLAY_NOW.getCategoryValue())) {
                // Play Now
                if (!isEvent || this.sharedPreferences.getBoolean(context.getString(R.string.prefkey_operation_play_now_enabled), true)) {
                    post(PostType.TWEET);
                }
            } else if (categories.contains(PluginOperationCategory.OPERATION_EXECUTE.getCategoryValue())) {
                // Execute
                final String EXECUTE_TWEET_ID = "execute_id_tweet";
                final String EXECUTE_SITE_ID = "execute_id_site";

                if (extras != null) {
                    if (extras.keySet().contains(EXECUTE_TWEET_ID)) {
                        post(PostType.SEND_APP);
                    } else if (extras.keySet().contains(EXECUTE_SITE_ID)) {
                        Intent launchIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.twitter_uri)));
                        try {
                            launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(launchIntent);
                        } catch (android.content.ActivityNotFoundException e) {
                            Logger.d(e);
                        }
                    }
                }
            }
        } finally {
            context = null;
            sharedPreferences = null;
            propertyMap = null;
            mediaUri = null;
            twitter = null;
        }
    }



    /**
     * 投稿。
     */
    private void post(PostType postType) {
        // 音楽データ無し
        if (mediaUri == null) {
            AppUtils.showToast(context, R.string.message_no_media);
            showResult(PostResult.IGNORE, postType);
            return;
        }

        if (postType == PostType.TWEET) {
            String mediaUriText = mediaUri.toString();
            String previousMediaUri = sharedPreferences.getString(PREFKEY_PREVIOUS_MEDIA_URI, "");
            boolean previousMediaEnabled = sharedPreferences.getBoolean(context.getString(R.string.prefkey_previous_media_enabled), false);
            if (!previousMediaEnabled && !TextUtils.isEmpty(mediaUriText) && !TextUtils.isEmpty(previousMediaUri) && mediaUriText.equals(previousMediaUri)) {
                // 前回と同じメディアは無視
                showResult(PostResult.IGNORE, postType);
                return;
            }
            sharedPreferences.edit().putString(PREFKEY_PREVIOUS_MEDIA_URI, mediaUriText).apply();
        }

        PostResult result = PostResult.IGNORE;
        try {
            switch (postType) {
                case TWEET:
                    result = tweet();
                    break;
                case SEND_APP:
                    result = sendApp();
                    break;
            }
        } catch (Exception e) {
            Logger.e(e);
            result = PostResult.FAILED;
        }
        showResult(result, postType);
    }

    /**
     * ツイート投稿。
     * @return 投稿結果。
     */
    private PostResult tweet() {
        try {
            // アルバムアート取得
            File albumArtFile = null;
            Uri albumArtUri =getAlbumArtFile(propertyMap);
            if (albumArtUri != null) {
                String path = albumArtUri.getPath();
                albumArtFile = new File(path);
            }

            // メッセージ取得
            int messageMax = (albumArtFile == null) ? MESSAGE_LENGTH : MESSAGE_LENGTH - IMAGE_URL_LENGTH;
            String message = getMessage(messageMax, propertyMap);
            if (TextUtils.isEmpty(message)) {
                return PostResult.IGNORE;
            }

            // ツイート
            twitter.updateStatus(new StatusUpdate(message).media(albumArtFile));
            return PostResult.SUCCEEDED;
        } catch (Exception e) {
            Logger.e(e);
            return PostResult.FAILED;
        }
    }

    /**
     * 外部アプリで投稿。
     * @return 投稿結果。
     */
    private PostResult sendApp() {
        try {
            // アルバムアート取得
            Uri albumArtUri= getAlbumArtFile(propertyMap);

            // メッセージ取得
            int messageMax = (albumArtUri == null) ? MESSAGE_LENGTH : MESSAGE_LENGTH - IMAGE_URL_LENGTH;
            String message = getMessage(messageMax, propertyMap);
            if (TextUtils.isEmpty(message)) {
                return PostResult.IGNORE;
            }

            Intent twitterIntent = new Intent();
            twitterIntent.setAction(Intent.ACTION_SEND);
            twitterIntent.setType("text/plain");
            twitterIntent.putExtra(Intent.EXTRA_TEXT, message);

//            Uri imageUri = null;
//            if (albumArtFile != null) {
//                Cursor cursor = null;
//                try {
//                    // 画像のURI取得 (取得パスがエイリアスでMediaStorageのパスと異なる場合は取得できない)
//                    cursor = context.getContentResolver().query(
//                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
//                            null,
//                            MediaStore.Images.Media.DATA + " = ?",
//                            new String[]{ albumArtFile.getAbsolutePath() },
//                            null);
//                    if (cursor != null &&  cursor.moveToFirst()) {
//                        long id = cursor.getLong(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
//                        imageUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
//                    }
//                } catch (Exception e) {
//                    Logger.e(e);
//                } finally {
//                    if (cursor != null && !cursor.isClosed()) cursor.close();
//                }
//                if (imageUri == null) {
//                    // DBから取得できない場合はfile://スキーマを使用
//                    imageUri = Uri.parse("file://" + albumArtFile.getPath());
//                }
//                twitterIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
//            }
            twitterIntent.putExtra(Intent.EXTRA_STREAM, albumArtUri);
            twitterIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            twitterIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            context.startActivity(twitterIntent);
            return PostResult.SUCCEEDED;
        } catch (Exception e) {
            Logger.e(e);
            return PostResult.FAILED;
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
                        if (propertyItem.shorten && remain > 4) {
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
    private Uri getAlbumArtFile( final Map<String, String> propertyMap) {
        Uri albumArtUri = null;
        if (sharedPreferences.getBoolean(context.getString(R.string.prefkey_content_album_art), true)) {
//            String albumArtPath = propertyMap.get(AlbumArtProperty.FOLDER_PATH.getKeyName()) + propertyMap.get(AlbumArtProperty.FILE_NAME.getKeyName());
//            if (!TextUtils.isEmpty(albumArtPath)) {
//                albumArtFile = new File(albumArtPath);
//                if (!albumArtFile.exists()) {
//                    albumArtFile = null;
//                }
//            }
            String uri = propertyMap.get(AlbumArtProperty.DATA_URI.getKeyName());
            if (!TextUtils.isEmpty(uri)) {
                albumArtUri = Uri.parse(uri);
            }
        }
        return albumArtUri;
    }



    /**
     * 投稿結果を出力。
     * @param result 投稿結果。
     * @param postType 投稿種別。
     */
    private void showResult(PostResult result, PostType postType) {
        if (result == PostResult.IGNORE || result == PostResult.SAVED) {
            return;
        } else if (result == PostResult.AUTH_FAILED) {
            AppUtils.showToast(context, R.string.message_account_not_auth);
            return;
        }

        switch (postType) {
            case TWEET:
                if (result == PostResult.SUCCEEDED) {
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
