package com.wa2c.android.medoly.plugin.action.tweet.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.text.TextUtils;


import com.twitter.Validator;
import com.wa2c.android.medoly.library.MediaPluginIntent;
import com.wa2c.android.medoly.library.PropertyData;
import com.wa2c.android.medoly.plugin.action.tweet.R;
import com.wa2c.android.medoly.plugin.action.tweet.util.Logger;
import com.wa2c.android.medoly.plugin.action.tweet.util.PropertyItem;

import java.util.HashSet;
import java.util.List;
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
     * @param propertyMap A property data.
     * @return The message text.
     */
    protected String getTweetMessage(final PropertyData propertyMap) {

        Validator validator = new Validator();

        final String TRIM_EXP = sharedPreferences.getBoolean(context.getString(R.string.prefkey_trim_before_empty_enabled), true) ? "\\w*" : "";
        final String TAG_EXP = "%([^%]+)%";
        final String TRIMMED_TAG_EXP = TRIM_EXP + TAG_EXP;
        final String format = sharedPreferences.getString(context.getString(R.string.prefkey_content_format), context.getString(R.string.format_content_default));
        final String constText = format.replaceAll(TAG_EXP, "");

        // フォーマットに含まれているタグ取得
        HashSet<String> propertyKeySet = new HashSet<>();
        Matcher tagMatcher = Pattern.compile(TRIMMED_TAG_EXP, Pattern.MULTILINE).matcher(format);
        while (tagMatcher.find()) {
            if (tagMatcher.groupCount() > 0) {
                propertyKeySet.add(tagMatcher.group(1));
            }
        }

        int charCount = constText.length();
        String currentText = format;
        List<PropertyItem> priorityList = PropertyItem.loadPropertyPriority(context);
        for (PropertyItem propertyItem : priorityList) {
            String propertyText = propertyData.getFirst(propertyItem.propertyKey);
            if (propertyText == null)
                propertyText = "";
            int propertyLength = validator.getTweetLength(propertyText);

            Matcher matcher = Pattern.compile("(\\w*)%" + propertyItem.propertyKey + "%").matcher(currentText); // タグ削除
            while (matcher.find()) {
                charCount += propertyLength;
                if (Validator.MAX_TWEET_LENGTH > charCount) {
                    currentText = matcher.replaceFirst(propertyText);
                } else {
                    currentText = matcher.replaceFirst("");
                    charCount = Validator.MAX_TWEET_LENGTH;
                }

            }
        }

        return currentText;
    }

//    /**
//     * Get tweet message text.
//     * @param propertyMap A property data.
//     * @return The message text.
//     */
//    protected String getTweetMessage(final PropertyData propertyMap) {
//        final String TAG_EXP = "%([^%]+)%"; // メタタグ
//        final String format = sharedPreferences.getString(context.getString(R.string.prefkey_content_format), context.getString(R.string.format_content_default));
//        if (TextUtils.isEmpty(format)) {
//            return null;
//        }
//        boolean isTrim = sharedPreferences.getBoolean(context.getString(R.string.prefkey_trim_before_empty_enabled), true);
//
//        // フォーマットに含まれているタグ取得
//        HashSet<String> propertyKeySet = new HashSet<>();
//        Matcher tagMatcher = Pattern.compile(TAG_EXP, Pattern.MULTILINE).matcher(format);
//        while (tagMatcher.find()) {
//            if (tagMatcher.groupCount() > 0) {
//                propertyKeySet.add(tagMatcher.group(1));
//            }
//        }
//
//        // フォーマットの内容を置換え
//        String tempText = format.replaceAll(TAG_EXP, "");
//        int textCount = tempText.length();
//        if (textCount > messageMax) {
//            // 置換え無しで文字数オーバー
//            return tempText.substring(0, messageMax - 4) + "... ";
//        }
//
//        // 優先度の高い順に置換え
//        String outputMessage = format;
//        boolean replaceComplete = false;
//        List<PropertyItem> priorityList = PropertyItem.loadPropertyPriority(context);
//        for (PropertyItem propertyItem : priorityList) {
//            if (!propertyKeySet.contains(propertyItem.propertyKey))
//                continue;
//
//            // 置換えテキスト取得
//            String replaceText = "";
//            if (propertyMap.containsKey(propertyItem.propertyKey))
//                replaceText = propertyMap.getFirst(propertyItem.propertyKey);
//
//            // 検索
//            Matcher matcher;
//            if (replaceComplete || TextUtils.isEmpty(replaceText) && isTrim) {
//                matcher = Pattern.compile("(\\w*)%" + propertyItem.propertyKey + "%").matcher(outputMessage); // タグ削除
//            } else {
//                matcher = Pattern.compile("%" + propertyItem.propertyKey + "%").matcher(outputMessage); // タグ置換
//            }
//
//            while (matcher.find()) {
//                if (replaceComplete || TextUtils.isEmpty(replaceText) && isTrim) {
//                    // タグを削除する
//                    outputMessage = matcher.replaceFirst("");
//                    textCount -= matcher.group(1).length();
//                } else {
//                    int remain = messageMax - textCount;
//
//                    if (remain >= replaceText.length()) {
//                        // 文字数内に収まる
//                        outputMessage = matcher.replaceFirst(replaceText);
//                        textCount += replaceText.length();
//                    } else {
//                        // 文字数内に収まらない
//                        if (propertyItem.shorten && remain > 4) {
//                            // 省略
//                            replaceText = replaceText.substring(0, remain - 4) + "... ";
//                            int newlineIndex = replaceText.lastIndexOf("\n");
//                            if (sharedPreferences.getBoolean(context.getString(R.string.prefkey_omit_newline), true) && newlineIndex > 0) {
//                                replaceText = replaceText.substring(0, newlineIndex) + "... ";
//                            }
//                            outputMessage = matcher.replaceFirst(replaceText);
//                            textCount += replaceText.length();
//                        } else {
//                            outputMessage = matcher.replaceAll("");
//                        }
//                        replaceComplete = true; // 完了
//                    }
//                }
//            }
//        }
//
//        return outputMessage;
//    }


}
