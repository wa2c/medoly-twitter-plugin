package com.wa2c.android.medoly.plugin.action.tweet.util

import android.content.Context
import android.preference.PreferenceManager
import com.wa2c.android.medoly.library.*
import com.wa2c.android.medoly.plugin.action.tweet.R
import java.util.*


class PropertyItem {

    /** プロパティキー。  */
    var propertyKey: String? = null
    /** プロパティ名。  */
    var propertyName: String? = null
    /** 短縮可。  */
    var shorten: Boolean = false

    /** Get property tag.  */
    val propertyTag: String
        get() = "%$propertyKey%"

    companion object {


        /** 設定保存キー。  */
        private val PREFKEY_PROPERTY_PRIORITY = "property_priority"

        /**
         * 標準プロパティ優先度を取得する。
         * @param context コンテキスト。
         * @return プロパティ優先度。
         */
        fun getDefaultPropertyPriority(context: Context): ArrayList<PropertyItem> {
            val itemList = ArrayList<PropertyItem>()

            // メディア
            for (p in MediaProperty.values()) {
                val item = PropertyItem()
                item.propertyKey = p.keyName
                item.propertyName = context.getString(R.string.media) + " - " + p.getName(context)
                item.shorten = shorteningSet.contains(p)
                itemList.add(item)
            }

            // アルバムアート
            for (p in AlbumArtProperty.values()) {
                val item = PropertyItem()
                item.propertyKey = p.keyName
                item.propertyName = context.getString(R.string.album_art) + " - " + p.getName(context)
                item.shorten = shorteningSet.contains(p)
                itemList.add(item)
            }

            // 歌詞
            for (p in LyricsProperty.values()) {
                val item = PropertyItem()
                item.propertyKey = p.keyName
                item.propertyName = context.getString(R.string.lyrics) + " - " + p.getName(context)
                item.shorten = shorteningSet.contains(p)
                itemList.add(item)
            }

            // 再生キュー
            for (p in QueueProperty.values()) {
                val item = PropertyItem()
                item.propertyKey = p.keyName
                item.propertyName = context.getString(R.string.queue) + " - " + p.getName(context)
                item.shorten = shorteningSet.contains(p)
                itemList.add(item)
            }

            return itemList
        }

        /**
         * プロパティ優先度を読込む。
         * @param context コンテキスト。
         * @return プロパティ優先度。
         */
        fun loadPropertyPriority(context: Context): ArrayList<PropertyItem> {
            val itemList = getDefaultPropertyPriority(context)
            val itemMap = LinkedHashMap<String?, PropertyItem>()
            for (item in itemList) {
                itemMap.put(item.propertyKey, item)
            }

            var outputItemList = ArrayList<PropertyItem>()
            try {
                val p = PreferenceManager.getDefaultSharedPreferences(context)
                val text = p.getString(PREFKEY_PROPERTY_PRIORITY, null)
                val lines = text!!.split("\n".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
                for (line in lines) {
                    val items = line.split(",".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
                    if (items.size < 2)
                        continue

                    val item = itemMap.remove(items[0]) ?: continue
                    item.shorten = items[1].toBoolean()
                    outputItemList.add(item)

                    //val key = items[0]
                    //if (!itemMap.containsKey(key))
                    //    continue
                    //val item = itemMap[key]
                    //item.shorten = java.lang.Boolean.parseBoolean(items[1])
                    //outputItemList.add(itemMap.remove(key)!!) // プロパティをリストに追加
                }
                for (item in itemMap.values) {
                    outputItemList.add(item) // 設定に無い項目を追加
                }
            } catch (e: Exception) {
                // エラー時はデフォルト状態
                outputItemList = itemList
            }

            return outputItemList
        }

        /**
         * プロパティ優先度を保存する。
         * @param context コンテキスト。
         * @param itemList プロパティ優先度。
         */
        fun savePropertyPriority(context: Context, itemList: ArrayList<PropertyItem>) {
            val p = PreferenceManager.getDefaultSharedPreferences(context)
            val builder = StringBuilder()
            for (item in itemList) {
                builder.append(item.propertyKey).append(",").append(item.shorten).append("\n")
            }
            p.edit().putString(PREFKEY_PROPERTY_PRIORITY, builder.toString()).apply()
        }

        /** 省略可のプロパティセット。  */
        private val shorteningSet = object : HashSet<IProperty>() {
            init {
                // Media
                add(MediaProperty.TITLE)
                add(MediaProperty.ARTIST)
                add(MediaProperty.ORIGINAL_ARTIST)
                add(MediaProperty.ALBUM_ARTIST)
                add(MediaProperty.ALBUM)
                add(MediaProperty.ORIGINAL_ALBUM)
                add(MediaProperty.GENRE)
                add(MediaProperty.MOOD)
                add(MediaProperty.OCCASION)
                add(MediaProperty.COMPOSER)
                add(MediaProperty.ARRANGER)
                add(MediaProperty.LYRICIST)
                add(MediaProperty.ORIGINAL_LYRICIST)
                add(MediaProperty.CONDUCTOR)
                add(MediaProperty.PRODUCER)
                add(MediaProperty.ENGINEER)
                add(MediaProperty.ENCODER)
                add(MediaProperty.MIXER)
                add(MediaProperty.DJMIXER)
                add(MediaProperty.REMIXER)
                add(MediaProperty.COPYRIGHT)
                add(MediaProperty.RECORD_LABEL)
                add(MediaProperty.COMMENT)
                add(MediaProperty.FOLDER_PATH)
                add(MediaProperty.FILE_NAME)
                add(MediaProperty.LAST_MODIFIED)
                // Album Art
                add(AlbumArtProperty.RESOURCE_TYPE)
                add(AlbumArtProperty.FOLDER_PATH)
                add(AlbumArtProperty.FILE_NAME)
                add(AlbumArtProperty.LAST_MODIFIED)
                // Lyrics
                add(LyricsProperty.LYRICS)
                add(LyricsProperty.RESOURCE_TYPE)
                add(LyricsProperty.FORMAT_TYPE)
                add(LyricsProperty.SYNC_TYPE)
                add(LyricsProperty.FOLDER_PATH)
                add(LyricsProperty.FILE_NAME)
                add(LyricsProperty.LAST_MODIFIED)
            }
        }
    }
}
