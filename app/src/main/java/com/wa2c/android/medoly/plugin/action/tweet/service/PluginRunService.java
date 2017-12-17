package com.wa2c.android.medoly.plugin.action.tweet.service;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.text.TextUtils;

import com.wa2c.android.medoly.library.AlbumArtProperty;
import com.wa2c.android.medoly.plugin.action.tweet.R;
import com.wa2c.android.medoly.plugin.action.tweet.util.AppUtils;
import com.wa2c.android.medoly.plugin.action.tweet.util.Logger;

import java.util.List;


/**
 *  Download intent service.
 */
public class PluginRunService extends AbstractPluginService {

    /**
     * Constructor.
     */
    public PluginRunService() {
        super(PluginRunService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        super.onHandleIntent(intent);

        try {
            if (receivedClassName.equals(PluginReceivers.ExecutePostTweetReceiver.class.getName())) {
                shareTweet();
            } else if (receivedClassName.equals(PluginReceivers.ExecuteOpenTwitterReceiver.class.getName())) {
                openTwitter();
            }
        } catch (Exception e) {
            Logger.e(e);
        }
    }


    /**
     * Share tweet message.
     */
    private void shareTweet() {
        CommandResult result = CommandResult.IGNORE;
        try {
            if (propertyData == null || propertyData.isMediaEmpty()) {
                result = CommandResult.NO_MEDIA;
                return;
            }

            // Get message
            String message = getTweetMessage();
            if (TextUtils.isEmpty(message)) {
                result = CommandResult.IGNORE;
                return;
            }

            // Get album art
            Uri albumArtUri= propertyData.getAlbumArtUri();

            Intent twitterIntent = new Intent();
            twitterIntent.setAction(Intent.ACTION_SEND);
            twitterIntent.setType("text/plain");
            twitterIntent.putExtra(Intent.EXTRA_TEXT, message);
            twitterIntent.putExtra(Intent.EXTRA_STREAM, albumArtUri);
            twitterIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // add URI permission
            if (albumArtUri != null) {
                // content:// scheme needs permission.
                if (ContentResolver.SCHEME_CONTENT.equals(albumArtUri.getScheme())) {
                    twitterIntent.setData(albumArtUri);
                    twitterIntent.setType(propertyData.getFirst(AlbumArtProperty.MIME_TYPE));
                    twitterIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                    List<ResolveInfo> resolveInfoList = getPackageManager().queryIntentActivities(twitterIntent, PackageManager.MATCH_DEFAULT_ONLY | PackageManager.GET_RESOLVED_FILTER);
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

}
