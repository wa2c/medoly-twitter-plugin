package com.wa2c.android.medoly.plugin.action.tweet.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.wa2c.android.medoly.library.AlbumArtProperty;
import com.wa2c.android.medoly.library.IProperty;
import com.wa2c.android.medoly.library.LyricsProperty;
import com.wa2c.android.medoly.library.MediaProperty;
import com.wa2c.android.medoly.library.QueueProperty;
import com.wa2c.android.medoly.plugin.action.tweet.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;



public class PropertyItem {

    /** プロパティキー。 */
    public String propertyKey;
    /** プロパティ名。 */
    public String propertyName;
    /** 短縮可。 */
    public boolean shorten;

    /** Get property tag. */
    public String getPropertyTag() {
        return "%" + propertyKey + "%";
    }




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
        for (MediaProperty p : MediaProperty.values()) {
            PropertyItem item = new PropertyItem();
            item.propertyKey = p.getKeyName();
            item.propertyName = context.getString(R.string.media) + " - " + p.getName(context);
            item.shorten = shorteningSet.contains(p);
            itemList.add(item);
        }

        // アルバムアート
        for (AlbumArtProperty p : AlbumArtProperty.values()) {
            PropertyItem item = new PropertyItem();
            item.propertyKey = p.getKeyName();
            item.propertyName = context.getString(R.string.album_art) + " - " + p.getName(context);
            item.shorten = shorteningSet.contains(p);
            itemList.add(item);
        }

        // 歌詞
        for (LyricsProperty p : LyricsProperty.values()) {
            PropertyItem item = new PropertyItem();
            item.propertyKey = p.getKeyName();
            item.propertyName = context.getString(R.string.lyrics) + " - " + p.getName(context);
            item.shorten = shorteningSet.contains(p);
            itemList.add(item);
        }

        // 再生キュー
        for (QueueProperty p : QueueProperty.values()) {
            PropertyItem item = new PropertyItem();
            item.propertyKey = p.getKeyName();
            item.propertyName = context.getString(R.string.queue) + " - " + p.getName(context);
            item.shorten = shorteningSet.contains(p);
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
                item.shorten = Boolean.parseBoolean(items[1]);
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
            builder.append(item.propertyKey).append(",").append(item.shorten).append("\n");
        }
        p.edit().putString(PREFKEY_PROPERTY_PRIORITY, builder.toString()).apply();
    }

    /** 省略可のプロパティセット。 */
    private static HashSet<IProperty> shorteningSet = new HashSet<IProperty>() {{
        // Media
        add( MediaProperty.TITLE             );
        add( MediaProperty.ARTIST            );
        add( MediaProperty.ORIGINAL_ARTIST   );
        add( MediaProperty.ALBUM_ARTIST      );
        add( MediaProperty.ALBUM             );
        add( MediaProperty.ORIGINAL_ALBUM    );
        add( MediaProperty.GENRE             );
        add( MediaProperty.MOOD              );
        add( MediaProperty.OCCASION          );
        add( MediaProperty.COMPOSER          );
        add( MediaProperty.ARRANGER          );
        add( MediaProperty.LYRICIST          );
        add( MediaProperty.ORIGINAL_LYRICIST );
        add( MediaProperty.CONDUCTOR         );
        add( MediaProperty.PRODUCER          );
        add( MediaProperty.ENGINEER          );
        add( MediaProperty.ENCODER           );
        add( MediaProperty.MIXER             );
        add( MediaProperty.DJMIXER           );
        add( MediaProperty.REMIXER           );
        add( MediaProperty.COPYRIGHT         );
        add( MediaProperty.RECORD_LABEL      );
        add( MediaProperty.COMMENT           );
        add( MediaProperty.FOLDER_PATH       );
        add( MediaProperty.FILE_NAME         );
        add( MediaProperty.LAST_MODIFIED     );
        // Album Art
        add( AlbumArtProperty.RESOURCE_TYPE  );
        add( AlbumArtProperty.FOLDER_PATH    );
        add( AlbumArtProperty.FILE_NAME      );
        add( AlbumArtProperty.LAST_MODIFIED  );
        // Lyrics
        add( LyricsProperty.LYRICS           );
        add( LyricsProperty.RESOURCE_TYPE    );
        add( LyricsProperty.FORMAT_TYPE      );
        add( LyricsProperty.SYNC_TYPE        );
        add( LyricsProperty.FOLDER_PATH      );
        add( LyricsProperty.FILE_NAME        );
        add( LyricsProperty.LAST_MODIFIED    );
    }};
}
