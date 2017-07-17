package com.wa2c.android.medoly.plugin.action.tweet.service;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.wa2c.android.medoly.library.AlbumArtProperty;
import com.wa2c.android.medoly.library.MediaPluginIntent;
import com.wa2c.android.medoly.library.PluginOperationCategory;
import com.wa2c.android.medoly.library.PropertyData;
import com.wa2c.android.medoly.plugin.action.tweet.db.PropertyItem;
import com.wa2c.android.medoly.plugin.action.tweet.R;
import com.wa2c.android.medoly.plugin.action.tweet.util.AppUtils;
import com.wa2c.android.medoly.plugin.action.tweet.util.Logger;
import com.wa2c.android.medoly.plugin.action.tweet.util.TwitterUtils;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
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




    /** Context. */
    private Context context = null;
    /** Shared preferences. */
    private SharedPreferences sharedPreferences = null;
    /** Package manager. */
    private PackageManager packageManager;

    /** Plugin intent.。 */
    private MediaPluginIntent pluginIntent;
    /** Property data. */
    private PropertyData propertyData;
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
            context = getApplicationContext();
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            packageManager = context.getPackageManager();
            pluginIntent = new MediaPluginIntent(intent);
            propertyData = pluginIntent.getPropertyData();
            twitter = TwitterUtils.getTwitterInstance(context);

            // 各アクション実行
            if (pluginIntent.hasCategory(PluginOperationCategory.OPERATION_PLAY_START)) {
                // Play Start
                if (!pluginIntent.isAutomatically() || this.sharedPreferences.getBoolean(context.getString(R.string.prefkey_operation_play_start_enabled), false)) {
                    post(PostType.TWEET);
                }
            } else if (pluginIntent.hasCategory(PluginOperationCategory.OPERATION_PLAY_NOW)) {
                // Play Now
                if (!pluginIntent.isAutomatically() || this.sharedPreferences.getBoolean(context.getString(R.string.prefkey_operation_play_now_enabled), true)) {
                    post(PostType.TWEET);
                }
            } else if (pluginIntent.hasCategory(PluginOperationCategory.OPERATION_EXECUTE)) {
                // Execute
                if (pluginIntent.hasExecuteId("execute_id_tweet")) {
                    // Send
                    post(PostType.SEND_APP);
                } else if (pluginIntent.hasExecuteId("execute_id_site")) {
                    // Twitter.com
                    Intent launchIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.twitter_uri)));
                    try {
                        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(launchIntent);
                    } catch (android.content.ActivityNotFoundException e) {
                        Logger.d(e);
                    }
                }
            }
        } catch (Exception e) {
            AppUtils.showToast(this, R.string.error_app);
        } finally {
            context = null;
            sharedPreferences = null;
            pluginIntent = null;
            twitter = null;
        }
    }



     /**
     * 投稿。
     */
    private void post(PostType postType) {
        // 音楽データ無し
        if (propertyData.getMediaUri() == null) {
            AppUtils.showToast(context, R.string.message_no_media);
            showResult(PostResult.IGNORE, postType);
            return;
        }

        // 認証確認
        if (postType == PostType.TWEET) {
            if (!TwitterUtils.hasAccessToken(context)) {
                showResult(PostResult.AUTH_FAILED, postType);
                return;
            }
        }

        // 前回メディア確認
        if (postType == PostType.TWEET) {
            String mediaUriText = propertyData.getMediaUri().toString();
            String previousMediaUri = sharedPreferences.getString(PREFKEY_PREVIOUS_MEDIA_URI, "");
            boolean previousMediaEnabled = sharedPreferences.getBoolean(context.getString(R.string.prefkey_previous_media_enabled), false);
            if (!previousMediaEnabled && !TextUtils.isEmpty(mediaUriText) && !TextUtils.isEmpty(previousMediaUri) && mediaUriText.equals(previousMediaUri)) {
                // 前回と同じメディアは無視
                showResult(PostResult.IGNORE, postType);
                return;
            }
            sharedPreferences.edit().putString(PREFKEY_PREVIOUS_MEDIA_URI, mediaUriText).apply();
        }

        // 投稿
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
        Uri albumArtUri = null;
        InputStream inputStream = null;
        try {
            // Get album art uri
            if (sharedPreferences.getBoolean(getString(R.string.prefkey_send_album_art), true)) {
                albumArtUri = propertyData.getAlbumArtUri();
                if (albumArtUri != null) {
                    try {
                        if (ContentResolver.SCHEME_CONTENT.equals(albumArtUri.getScheme())) {
                            inputStream = context.getContentResolver().openInputStream(albumArtUri);
                        } else if (ContentResolver.SCHEME_FILE.equals(albumArtUri.getScheme())) {
                            inputStream = new FileInputStream(albumArtUri.getPath());
                        }
                    } catch (Exception ignored) {
                        albumArtUri = null;
                        inputStream = null;
                    }
                }
            }

            // Get message
            int messageMax = (inputStream == null) ? MESSAGE_LENGTH : MESSAGE_LENGTH - IMAGE_URL_LENGTH;
            String message = getMessage(messageMax, propertyData);
            if (TextUtils.isEmpty(message)) {
                return PostResult.IGNORE;
            }

            if (inputStream == null) {
                twitter.updateStatus(new StatusUpdate(message));
            } else {
                twitter.updateStatus(new StatusUpdate(message).media(albumArtUri.getLastPathSegment(), inputStream));
            }
        } catch (Exception e) {
            Logger.e(e);
            return PostResult.FAILED;
        } finally {
            if (inputStream != null)
                try { inputStream.close(); } catch (Exception ignored) {}
        }

        return PostResult.SUCCEEDED;
    }

    /**
     * 外部アプリで投稿。
     * @return 投稿結果。
     */
    private PostResult sendApp() {
        try {
            // アルバムアート取得
            Uri albumArtUri= propertyData.getAlbumArtUri();

            // メッセージ取得
            int messageMax = (albumArtUri == null) ? MESSAGE_LENGTH : MESSAGE_LENGTH - IMAGE_URL_LENGTH;
            String message = getMessage(messageMax, propertyData);
            if (TextUtils.isEmpty(message)) {
                return PostResult.IGNORE;
            }

            Intent twitterIntent = new Intent();
            twitterIntent.setAction(Intent.ACTION_SEND);
            twitterIntent.setType(propertyData.getFirst(AlbumArtProperty.MIME_TYPE));
            twitterIntent.putExtra(Intent.EXTRA_TEXT, message);
            twitterIntent.putExtra(Intent.EXTRA_STREAM, albumArtUri);
            twitterIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // add URI permission
            if (albumArtUri != null && ContentResolver.SCHEME_CONTENT.equals(albumArtUri.getScheme())) {
                twitterIntent.setData(albumArtUri);
                twitterIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                List<ResolveInfo> resolveInfoList = packageManager.queryIntentActivities(twitterIntent, PackageManager.MATCH_DEFAULT_ONLY | PackageManager.GET_RESOLVED_FILTER);
                for (ResolveInfo resolveInfo : resolveInfoList) {
                    String packageName = resolveInfo.activityInfo.packageName;
                    context.getApplicationContext().grantUriPermission(packageName, albumArtUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
            }

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
    private String getMessage(final int messageMax, final PropertyData propertyMap) {
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
                replaceText = propertyMap.getFirst(propertyItem.propertyKey);

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