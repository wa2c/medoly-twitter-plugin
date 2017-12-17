package com.wa2c.android.medoly.plugin.action.tweet.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.twitter.twittertext.TwitterTextParseResults;
import com.twitter.twittertext.TwitterTextParser;
import com.wa2c.android.medoly.library.MediaPluginIntent;
import com.wa2c.android.medoly.library.PropertyData;
import com.wa2c.android.medoly.plugin.action.tweet.R;
import com.wa2c.android.medoly.plugin.action.tweet.util.Logger;
import com.wa2c.android.medoly.plugin.action.tweet.util.PropertyItem;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



/**
 *  Plugin service base.
 */
public abstract class AbstractPluginService extends IntentService {

    /** Received receiver class name. */
    public static String RECEIVED_CLASS_NAME = "RECEIVED_CLASS_NAME";

    /**
     * Command result.
     */
    enum CommandResult {
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
    protected Context context = null;
    /** Preferences. */
    protected SharedPreferences sharedPreferences = null;
    /** Plugin intent. */
    protected MediaPluginIntent pluginIntent;
    /** Property data. */
    protected PropertyData propertyData;
    /** Received class name. */
    protected String receivedClassName;



    /**
     * Constructor.
     */
    public AbstractPluginService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Logger.d("onHandleIntent");
        if (intent == null)
            return;

        try {
            context = getApplicationContext();
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            pluginIntent = new MediaPluginIntent(intent);
            propertyData = pluginIntent.getPropertyData();
            receivedClassName = pluginIntent.getStringExtra(RECEIVED_CLASS_NAME);
        } catch (Exception e) {
            Logger.e(e);
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        Logger.d("onDestroy" + this.getClass().getSimpleName());
    }


    /**
     * Get tweet message text.
     * @return The message text.
     */
    protected String getTweetMessage() {
        // test data
        //propertyData.put(MediaProperty.TITLE, "０１２３４５６７８９０１２３４５６７８９０１２３４５６７８９０１２３４５６７８９０１２３４５６");
        //propertyData.put(MediaProperty.ARTIST, "０１２３４５６７８９０１２３４５６７８９０１２３４５６７８９０１２３４５６７８９");
        //propertyData.put(MediaProperty.ALBUM, "０１２３４５６７８９０１２３４５６７８９０１２３４５６７８９０１２３４５６７８９");

        final String format = sharedPreferences.getString(context.getString(R.string.prefkey_content_format), context.getString(R.string.format_content_default));
        final String TRIM_EXP = sharedPreferences.getBoolean(context.getString(R.string.prefkey_trim_before_empty_enabled), true) ? "\\w*" : "";
        List<PropertyItem> priorityList = PropertyItem.loadPropertyPriority(context);

        // get contains tag
        LinkedHashSet<PropertyItem> containsMap = new LinkedHashSet<>();
        for (PropertyItem item : priorityList) {
            Matcher matcher = Pattern.compile(item.getPropertyTag(), Pattern.MULTILINE).matcher(format);
            if (matcher.find())
                containsMap.add(item);
        }

        String outputText = format;
        for (PropertyItem propertyItem : containsMap) {
            String propertyText = propertyData.getFirst(propertyItem.propertyKey);
            String regexpText = propertyItem.getPropertyTag();
            if (TextUtils.isEmpty(propertyText)) {
                propertyText = "";
                regexpText = TRIM_EXP + regexpText;
            }

            String workText = outputText;
            Matcher matcher = Pattern.compile(regexpText).matcher(workText);
            while (matcher.find()) {
                workText = matcher.replaceFirst(propertyText);
                String removedText = getPropertyRemovedText(workText, containsMap);
                TwitterTextParseResults result = TwitterTextParser.parseTweet(removedText);
                int remainWeight = 999 - result.permillage; // 1000丁度だと入らない可能性
                if (remainWeight > 0) {
                    outputText = workText;
                } else {
                    if (propertyItem.shorten) {
                        workText = matcher.replaceFirst(trimWeightedText(propertyText, TwitterTextParser.parseTweet(propertyText).permillage + remainWeight));
                        outputText = getPropertyRemovedText(workText, containsMap);
                    }
                    break;
                }
            }
        }

        return outputText;
    }


    private String getPropertyRemovedText(String workText, Set<PropertyItem> containsMap) {
        String parseText = workText;
        for (PropertyItem pi : containsMap) {
            parseText = parseText.replaceAll("%" + pi.propertyKey + "%", "");
        }
        return parseText;
    }

    private String trimWeightedText(String propertyText, int remainWeight) {
        if (TextUtils.isEmpty(propertyText) || propertyText.length() < "...".length()) {
            return "";
        }

        Matcher newlineMatcher = Pattern.compile("\\r\\n|\\n|\\r").matcher(propertyText);
        if (newlineMatcher.find() && sharedPreferences.getBoolean(context.getString(R.string.prefkey_omit_newline), true)) {
            String returnText = "";
            while (newlineMatcher.find()) {
                TwitterTextParseResults result = TwitterTextParser.parseTweet(propertyText.substring(0, newlineMatcher.start()) + "...");
                if (result.permillage >= remainWeight) {
                    break;
                }
                returnText = propertyText.substring(0, newlineMatcher.start()) + "...";
            }
            return returnText;
        } else {
            for (int i = 1; i < propertyText.length(); i++) {
                TwitterTextParseResults result = TwitterTextParser.parseTweet(propertyText.substring(0, i) + "...");
                if (result.permillage >= remainWeight) {
                    return propertyText.substring(0, i - 1) + "...";
                }
            }
            return propertyText;
        }
    }

}
