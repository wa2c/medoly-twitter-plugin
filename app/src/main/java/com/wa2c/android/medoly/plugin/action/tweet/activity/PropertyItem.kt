package com.wa2c.android.medoly.plugin.action.tweet.activity

import android.content.Context
import com.wa2c.android.medoly.library.*
import com.wa2c.android.medoly.plugin.action.tweet.R
import com.wa2c.android.prefs.Prefs

/**
 * Property Item.
 */
class PropertyItem {

    /** Property key.  */
    var propertyKey: String? = null
    /** Property name.  */
    var propertyName: String? = null
    /** True if possible to shorten.  */
    var shorten: Boolean = false

    /** Get property tag.  */
    val propertyTag: String
        get() = "%$propertyKey%"

    companion object {

        /** Preference key.  */
        private const val PREFKEY_PROPERTY_PRIORITY = "property_priority"

        /**
         * Get default property priority.
         * @param context A context.
         * @return A property list.
         */
        fun getDefaultPropertyPriority(context: Context): ArrayList<PropertyItem> {
            val itemList = ArrayList<PropertyItem>()

            // Media
            for (p in MediaProperty.values()) {
                val item = PropertyItem().apply {
                    propertyKey = p.keyName
                    propertyName = context.getString(R.string.media) + " - " + p.getName(context)
                    shorten = shorteningSet.contains(p)
                }
                itemList.add(item)
            }

            // Album Art
            for (p in AlbumArtProperty.values()) {
                val item = PropertyItem().apply {
                    propertyKey = p.keyName
                    propertyName = context.getString(R.string.album_art) + " - " + p.getName(context)
                    shorten = shorteningSet.contains(p)
                }
                itemList.add(item)
            }

            // Lyrics
            for (p in LyricsProperty.values()) {
                val item = PropertyItem().apply {
                    propertyKey = p.keyName
                    propertyName = context.getString(R.string.lyrics) + " - " + p.getName(context)
                    shorten = shorteningSet.contains(p)
                }
                itemList.add(item)
            }

            // Queue
            for (p in QueueProperty.values()) {
                val item = PropertyItem().apply {
                    propertyKey = p.keyName
                    propertyName = context.getString(R.string.queue) + " - " + p.getName(context)
                    shorten = shorteningSet.contains(p)
                }
                itemList.add(item)
            }

            return itemList
        }

        /**
         * Load property priority.
         * @param context A context
         * @return A property list ordered by priority.
         */
        fun loadPropertyPriority(context: Context): ArrayList<PropertyItem> {
            val itemList = getDefaultPropertyPriority(context)
            val itemMap = LinkedHashMap<String?, PropertyItem>()
            for (item in itemList) {
                itemMap[item.propertyKey] = item
            }

            var outputItemList = ArrayList<PropertyItem>()
            try {
                val prefs = Prefs(context)
                val text = prefs.getString(PREFKEY_PROPERTY_PRIORITY)
                val lines = text.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                for (line in lines) {
                    val items = line.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    if (items.size < 2)
                        continue

                    val item = itemMap.remove(items[0]) ?: continue
                    item.shorten = items[1].toBoolean()
                    outputItemList.add(item)
                }
                for (item in itemMap.values) {
                    outputItemList.add(item) // Add an item not exists settings
                }
            } catch (e: Exception) {
                outputItemList = itemList
            }

            return outputItemList
        }

        /**
         * Save property priority.
         * @param context A context
         * @param itemList A property list ordered by priority.
         */
        fun savePropertyPriority(context: Context, itemList: ArrayList<PropertyItem>) {
            val prefs = Prefs(context)
            val builder = StringBuilder()
            for (item in itemList) {
                builder.append(item.propertyKey).append(",").append(item.shorten).append("\n")
            }
            prefs.putString(PREFKEY_PROPERTY_PRIORITY, builder.toString())
        }

        /** Property set of possibility to shorten.  */
        private val shorteningSet = hashSetOf<IProperty>(
                // Media
                MediaProperty.TITLE,
                MediaProperty.ARTIST,
                MediaProperty.ORIGINAL_ARTIST,
                MediaProperty.ALBUM_ARTIST,
                MediaProperty.ALBUM,
                MediaProperty.ORIGINAL_ALBUM,
                MediaProperty.GENRE,
                MediaProperty.MOOD,
                MediaProperty.OCCASION,
                MediaProperty.COMPOSER,
                MediaProperty.ARRANGER,
                MediaProperty.LYRICIST,
                MediaProperty.ORIGINAL_LYRICIST,
                MediaProperty.CONDUCTOR,
                MediaProperty.PRODUCER,
                MediaProperty.ENGINEER,
                MediaProperty.ENCODER,
                MediaProperty.MIXER,
                MediaProperty.DJMIXER,
                MediaProperty.REMIXER,
                MediaProperty.COPYRIGHT,
                MediaProperty.RECORD_LABEL,
                MediaProperty.COMMENT,
                MediaProperty.FOLDER_PATH,
                MediaProperty.FILE_NAME,
                MediaProperty.LAST_MODIFIED,
                // Album Art
                AlbumArtProperty.RESOURCE_TYPE,
                AlbumArtProperty.FOLDER_PATH,
                AlbumArtProperty.FILE_NAME,
                AlbumArtProperty.LAST_MODIFIED,
                // Lyrics
                LyricsProperty.LYRICS,
                LyricsProperty.RESOURCE_TYPE,
                LyricsProperty.FORMAT_TYPE,
                LyricsProperty.SYNC_TYPE,
                LyricsProperty.FOLDER_PATH,
                LyricsProperty.FILE_NAME,
                LyricsProperty.LAST_MODIFIED
        )

    }
}
