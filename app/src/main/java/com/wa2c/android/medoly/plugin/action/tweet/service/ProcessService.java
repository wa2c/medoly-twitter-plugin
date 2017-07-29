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
import android.webkit.MimeTypeMap;

import com.wa2c.android.medoly.library.AlbumArtProperty;
import com.wa2c.android.medoly.library.MediaPluginIntent;
import com.wa2c.android.medoly.library.PluginOperationCategory;
import com.wa2c.android.medoly.library.PluginTypeCategory;
import com.wa2c.android.medoly.library.PropertyData;
import com.wa2c.android.medoly.plugin.action.tweet.R;
import com.wa2c.android.medoly.plugin.action.tweet.util.AppUtils;
import com.wa2c.android.medoly.plugin.action.tweet.util.Logger;
import com.wa2c.android.medoly.plugin.action.tweet.util.PropertyItem;
import com.wa2c.android.medoly.plugin.action.tweet.util.TwitterUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import twitter4j.StatusUpdate;
import twitter4j.Twitter;



/**
 * Intent service
 */
public class ProcessService extends IntentService {

    /** Received receiver class name. */
    public static String RECEIVED_CLASS_NAME = "RECEIVED_CLASS_NAME";

    /** Max tweet length.。 */
    private static final int MESSAGE_LENGTH = 140; // Twitter文字数
    /** Image url length.*/
    private static final int IMAGE_URL_LENGTH = 24; // 23 + 1 (space)
    /** Previous data key. */
    private static final String PREFKEY_PREVIOUS_MEDIA_URI = "previous_media_uri";

    /**
     * Command result.
     */
    private enum CommandResult {
        /** Succeeded. */
        SUCCEEDED,
        /** Failed. */
        FAILED,
        /** Authorization failed. */
        AUTH_FAILED,
        /** No media. */
        NO_MEDIA,
        /** Post saved. */
        SAVED,
        /** Ignore. */
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
     * Constructor.
     */
    public ProcessService() {
        super(ProcessService.class.getSimpleName());
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

            // Execute

            if (pluginIntent.hasCategory(PluginOperationCategory.OPERATION_EXECUTE)) {
                String receivedClassName = pluginIntent.getStringExtra(RECEIVED_CLASS_NAME);
                if (receivedClassName.equals(PluginReceiver.ExecutePostTweetReceiver.class.getName())) {
                    postTweet();
                } else if (receivedClassName.equals(PluginReceiver.ExecuteOpenTwitterReceiver.class.getName())) {
                    openTwitter();
                }
                return;
            }

            // Event

            if (pluginIntent.hasCategory(PluginTypeCategory.TYPE_POST_MESSAGE)) {
                if (pluginIntent.hasCategory(PluginOperationCategory.OPERATION_PLAY_START) && this.sharedPreferences.getBoolean(context.getString(R.string.prefkey_operation_play_start_enabled), false)) {
                    tweet();
                } else if (pluginIntent.hasCategory(PluginOperationCategory.OPERATION_PLAY_NOW) && this.sharedPreferences.getBoolean(context.getString(R.string.prefkey_operation_play_now_enabled), true)) {
                    tweet();
                }
                return;
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
     * Tweet.
     */
    private void tweet() {
        CommandResult result = CommandResult.IGNORE;
        InputStream inputStream = null;
        try {
            if (propertyData == null || propertyData.isMediaEmpty()) {
                result = CommandResult.NO_MEDIA;
                return;
            }

            // Check previous media
            String mediaUriText = propertyData.getMediaUri().toString();
            String previousMediaUri = sharedPreferences.getString(PREFKEY_PREVIOUS_MEDIA_URI, "");
            boolean previousMediaEnabled = sharedPreferences.getBoolean(context.getString(R.string.prefkey_previous_media_enabled), false);
            if (!previousMediaEnabled && !TextUtils.isEmpty(mediaUriText) && !TextUtils.isEmpty(previousMediaUri) && mediaUriText.equals(previousMediaUri)) {
                result = CommandResult.IGNORE;
                return;
            }
            sharedPreferences.edit().putString(PREFKEY_PREVIOUS_MEDIA_URI, mediaUriText).apply();

            if (!TwitterUtils.hasAccessToken(context)) {
                result = CommandResult.AUTH_FAILED;
                return;
            }

            // Get album art uri
            Uri albumArtUri = null;
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
                result = CommandResult.IGNORE;
                return;
            }

            if (inputStream == null) {
                twitter.updateStatus(new StatusUpdate(message));
            } else {
                twitter.updateStatus(new StatusUpdate(message).media(albumArtUri.getLastPathSegment(), inputStream));
            }
            result = CommandResult.SUCCEEDED;
        } catch (Exception e) {
            Logger.e(e);
            result = CommandResult.FAILED;
        } finally {
            if (inputStream != null)
                try { inputStream.close(); } catch (Exception ignored) {}
            if (result == CommandResult.AUTH_FAILED) {
                AppUtils.showToast(context, R.string.message_account_not_auth);
            } else if (result == CommandResult.NO_MEDIA) {
                AppUtils.showToast(context, R.string.message_no_media);
            } else if (result == CommandResult.SUCCEEDED) {
                if (sharedPreferences.getBoolean(getString(R.string.prefkey_tweet_success_message_show), false))
                    AppUtils.showToast(context, R.string.message_post_success);
            } else if (result == CommandResult.FAILED) {
                if (sharedPreferences.getBoolean(getString(R.string.prefkey_tweet_failure_message_show), true))
                    AppUtils.showToast(context, R.string.message_post_failure);
            }
        }
    }

    /**
     * Post tweet message.
     */
    private void postTweet() {
        CommandResult result = CommandResult.IGNORE;
        try {
            if (propertyData == null || propertyData.isMediaEmpty()) {
                result = CommandResult.NO_MEDIA;
                return;
            }

            // Get album art
            Uri albumArtUri= propertyData.getAlbumArtUri();

            // Get message
            int messageMax = (albumArtUri == null) ? MESSAGE_LENGTH : MESSAGE_LENGTH - IMAGE_URL_LENGTH;
            String message = getMessage(messageMax, propertyData);
            if (TextUtils.isEmpty(message)) {
                result = CommandResult.IGNORE;
                return;
            }

            Intent twitterIntent = new Intent();
            twitterIntent.setAction(Intent.ACTION_SEND);
            twitterIntent.setType(propertyData.getFirst(AlbumArtProperty.MIME_TYPE));
            twitterIntent.putExtra(Intent.EXTRA_TEXT, message);
            twitterIntent.putExtra(Intent.EXTRA_STREAM, albumArtUri);
            twitterIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // add URI permission
            if (albumArtUri != null) {
                if (ContentResolver.SCHEME_CONTENT.equals(albumArtUri.getScheme())) {
                    twitterIntent.setData(albumArtUri);
                    twitterIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                    List<ResolveInfo> resolveInfoList = packageManager.queryIntentActivities(twitterIntent, PackageManager.MATCH_DEFAULT_ONLY | PackageManager.GET_RESOLVED_FILTER);
                    for (ResolveInfo resolveInfo : resolveInfoList) {
                        String packageName = resolveInfo.activityInfo.packageName;
                        context.getApplicationContext().grantUriPermission(packageName, albumArtUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    }
                }
            }

            context.startActivity(twitterIntent);
            result = CommandResult.SUCCEEDED;
        } catch (Exception e) {
            Logger.e(e);
            result = CommandResult.FAILED;
        } finally {
           if (result == CommandResult.NO_MEDIA) {
                AppUtils.showToast(context, R.string.message_no_media);
            } else if (result == CommandResult.FAILED) {
                AppUtils.showToast(context, R.string.message_post_failure);
            }
       }
    }

    /**
     * Open twitter.
     */
    private void openTwitter() {
        // Twitter.com
        Intent launchIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.twitter_uri)));
        try {
            launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(launchIntent);
        } catch (android.content.ActivityNotFoundException e) {
            Logger.d(e);
        }
    }

    /**
     * Get message text.
     * @param messageMax Max text length.
     * @param propertyMap A property data.
     * @return The message text.
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

}
