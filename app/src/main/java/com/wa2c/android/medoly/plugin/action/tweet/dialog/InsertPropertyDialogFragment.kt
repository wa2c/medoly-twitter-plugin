package com.wa2c.android.medoly.plugin.action.tweet.dialog

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView

import com.wa2c.android.medoly.plugin.action.tweet.R
import com.wa2c.android.medoly.plugin.action.tweet.util.PropertyItem

import java.util.ArrayList
import java.util.EventListener


class InsertPropertyDialogFragment : AbstractDialogFragment() {

    /**
     * 項目リスト。
     */
    private val itemList = ArrayList<PropertyItem>()


    /** 選択リスナ。  */
    private var itemSelectListener: ItemSelectListener? = null


    /**
     * onCreateDialogイベント処理。
     */
    override fun onCreateDialog(savedInstanceState: Bundle): Dialog {
        val context = activity

        // 再生キュー
        itemList.addAll(PropertyItem.getDefaultPropertyPriority(context))

        // リストアダプタ
        val adapter = PropertyListAdapter(context, itemList)
        adapter.setNotifyOnChange(false)

        // リスト作成
        val listView = ListView(context)
        listView.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        listView.adapter = adapter
        listView.isFastScrollEnabled = true
        listView.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            if (itemSelectListener != null) {
                val insert = "%" + itemList[position].propertyKey + "%"
                itemSelectListener!!.onItemSelect(insert)
            }
            dialog.dismiss()
        }

        val listLayout = LinearLayout(context)
        listLayout.layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        listLayout.addView(listView)

        // ダイアログ
        val builder = AlertDialog.Builder(activity)
        builder.setTitle(R.string.label_dialog_insert_property_title)
        builder.setView(listLayout)

        // キャンセルボタン
        builder.setNeutralButton(android.R.string.cancel, null)

        return builder.create()
    }

    /**
     * リストアダプタ。
     */
    private inner class PropertyListAdapter
    /** コンストラクタ。  */
    internal constructor(context: Context, itemList: ArrayList<PropertyItem>) : ArrayAdapter<PropertyItem>(context, android.R.layout.simple_list_item_1, itemList) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var convertView = convertView
            val holder: ListItemViewHolder

            if (convertView == null) {
                convertView = View.inflate(activity, android.R.layout.simple_list_item_1, null)
                holder = ListItemViewHolder()
                holder.TitleTextView = convertView!!.findViewById(android.R.id.text1) as TextView
                convertView.tag = holder
            } else {
                holder = convertView.tag as ListItemViewHolder
            }

            val item = getItem(position)
            if (item != null) {
                holder.TitleTextView!!.text = item.propertyName
            }

            return convertView
        }

        /** リスト項目のビュー情報を保持するHolder。  */
        internal inner class ListItemViewHolder {
            var TitleTextView: TextView? = null
        }

    }

    /**
     * 選択リスナを設定する。
     * @param listener リスナ。
     */
    fun setOnItemSelectListener(listener: ItemSelectListener) {
        this.itemSelectListener = listener
    }

    /**
     * 選択インターフェース。。
     */
    interface ItemSelectListener : EventListener {
        /**
         * 挿入文字を取得する。
         * @param insertString 挿入文字。
         */
        fun onItemSelect(insertString: String)
    }

    companion object {

        /**
         * ダイアログのインスタンスを作成する。
         * @return ダイアログのインスタンス。
         */
        fun newInstance(): InsertPropertyDialogFragment {
            val fragment = InsertPropertyDialogFragment()

            val args = Bundle()
            fragment.arguments = args
            return fragment
        }
    }

}
