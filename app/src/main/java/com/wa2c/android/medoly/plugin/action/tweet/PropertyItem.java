package com.wa2c.android.medoly.plugin.action.tweet;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.wa2c.android.medoly.plugin.action.ActionPluginParam;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Created by wa2c on 2015/05/09.
 */
public class PropertyItem {

    /** プロパティキー。 */
    public String propertyKey;
    /** プロパティ名。 */
    public String propertyName;
    /** 省略可。 */
    public boolean omissible;



    /** 設定保存キー。 */
    private static final String PREFKEY_PROPERTY_PRIORITY = "property_priority";

    /**
     * 標準プロパティ優先度を取得する。
     * @param context コンテキスト。
     * @return プロパティ優先度。
     */
    public static ArrayList<PropertyItem> getDefaultPropertyPriority(Context context) {
        ArrayList<PropertyItem> itemList = new ArrayList<>();

        // メディア
        for (ActionPluginParam.MediaProperty p : ActionPluginParam.MediaProperty.values()) {
            PropertyItem item = new PropertyItem();
            item.propertyKey = p.getKeyName();
            item.propertyName = context.getString(R.string.media) + " - " + p.getName(context);
            item.omissible = p.enableShortening();
            itemList.add(item);
        }

        // アルバムアート
        for (ActionPluginParam.AlbumArtProperty p : ActionPluginParam.AlbumArtProperty.values()) {
            PropertyItem item = new PropertyItem();
            item.propertyKey = p.getKeyName();
            item.propertyName = context.getString(R.string.album_art) + " - " + p.getName(context);
            item.omissible = p.enableShortening();
            itemList.add(item);
        }

        // 歌詞
        for (ActionPluginParam.LyricsProperty p : ActionPluginParam.LyricsProperty.values()) {
            PropertyItem item = new PropertyItem();
            item.propertyKey = p.getKeyName();
            item.propertyName = context.getString(R.string.lyrics) + " - " + p.getName(context);
            item.omissible = p.enableShortening();
            itemList.add(item);
        }

        // 再生キュー
        for (ActionPluginParam.QueueProperty p : ActionPluginParam.QueueProperty.values()) {
            PropertyItem item = new PropertyItem();
            item.propertyKey = p.getKeyName();
            item.propertyName = context.getString(R.string.queue) + " - " + p.getName(context);
            item.omissible = p.enableShortening();
            itemList.add(item);
        }

        return itemList;
    }

    /**
     * プロパティ優先度を読込む。
     * @param context コンテキスト。
     * @return プロパティ優先度。
     */
    public static ArrayList<PropertyItem> loadPropertyPriority(Context context) {
        ArrayList<PropertyItem> itemList = getDefaultPropertyPriority(context);
        LinkedHashMap<String, PropertyItem> itemMap = new LinkedHashMap<>();
        for (PropertyItem item : itemList) {
            itemMap.put(item.propertyKey, item);
        }

        ArrayList<PropertyItem> outputItemList = new ArrayList<>();
        try {
            SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
            String text = p.getString(PREFKEY_PROPERTY_PRIORITY, null);
            String[] lines = text.split("\n");
            for (String line : lines) {
                String[] items = line.split(",");
                if (items.length < 2) continue;

                String key = items[0];
                if (!itemMap.containsKey(key)) continue;

                PropertyItem item = itemMap.get(key);
                item.omissible = Boolean.parseBoolean(items[1]);
                outputItemList.add(itemMap.remove(key)); // プロパティをリストに追加
            }
            for (PropertyItem item : itemMap.values()) {
                outputItemList.add(item); // 設定に無い項目を追加
            }
        } catch (Exception e) {
            // エラー時はデフォルト状態
            outputItemList = itemList;
        }

        return outputItemList;
    }

    /**
     * プロパティ優先度を保存する。
     * @param context コンテキスト。
     * @param itemList プロパティ優先度。
     */
    public static void savePropertyPriority(Context context,  ArrayList<PropertyItem> itemList) {
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        StringBuilder builder = new StringBuilder();
        for (PropertyItem item : itemList) {
            builder.append(item.propertyKey).append(",").append(item.omissible).append("\n");
        }
        p.edit().putString(PREFKEY_PROPERTY_PRIORITY, builder.toString()).apply();
    }

}
