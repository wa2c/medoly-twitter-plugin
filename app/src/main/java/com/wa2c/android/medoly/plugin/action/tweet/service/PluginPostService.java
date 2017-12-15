package com.wa2c.android.medoly.plugin.action.tweet.service;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import com.wa2c.android.medoly.library.PluginOperationCategory;
import com.wa2c.android.medoly.library.PluginTypeCategory;
import com.wa2c.android.medoly.plugin.action.tweet.R;
import com.wa2c.android.medoly.plugin.action.tweet.util.AppUtils;
import com.wa2c.android.medoly.plugin.action.tweet.util.Logger;
import com.wa2c.android.medoly.plugin.action.tweet.util.TwitterUtils;

import java.io.FileInputStream;
import java.io.InputStream;

import twitter4j.StatusUpdate;
import twitter4j.Twitter;


/**
 * Intent service
 */
public class PluginPostService extends AbstractPluginService {

    /** Previous data key. */
    private static final String PREFKEY_PREVIOUS_MEDIA_URI = "previous_media_uri";
    /** Twitter。 */
    private Twitter twitter;


    /**
     * Constructor.
     */
    public PluginPostService() {
        super(PluginPostService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        super.onHandleIntent(intent);
        if (!pluginIntent.hasCategory(PluginTypeCategory.TYPE_POST_MESSAGE)) {
            return;
        }

        try {
            twitter = TwitterUtils.getTwitterInstance(context);

            if (receivedClassName.equals(PluginReceivers.EventPostTweetReceiver.class.getName())) {
                tweet();
            }
        } catch (Exception e) {
            Logger.e(e);
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


            // Get message
            String message = getTweetMessage(propertyData);
            if (TextUtils.isEmpty(message)) {
                result = CommandResult.IGNORE;
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
                if (pluginIntent.hasCategory(PluginOperationCategory.OPERATION_EXECUTE) || sharedPreferences.getBoolean(getString(R.string.prefkey_tweet_success_message_show), false))
                    AppUtils.showToast(context, R.string.message_post_success);
            } else if (result == CommandResult.FAILED) {
                if (pluginIntent.hasCategory(PluginOperationCategory.OPERATION_EXECUTE) || sharedPreferences.getBoolean(getString(R.string.prefkey_tweet_failure_message_show), true))
                    AppUtils.showToast(context, R.string.message_post_failure);
            }
        }
    }

}
