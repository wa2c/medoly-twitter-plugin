package com.wa2c.android.medoly.plugin.action.tweet.dialog

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView

import com.mobeta.android.dslv.DragSortController
import com.mobeta.android.dslv.DragSortListView
import com.wa2c.android.medoly.plugin.action.tweet.R
import com.wa2c.android.medoly.plugin.action.tweet.util.AppUtils
import com.wa2c.android.medoly.plugin.action.tweet.util.PropertyItem

import java.util.ArrayList


class PropertyPriorityDialogFragment : AbstractDialogFragment() {


    /**
     * 項目リスト。
     */
    private val itemList = ArrayList<PropertyItem>()

    private var sortListView: DragSortListView? = null


    /**
     * onCreateDialogイベント処理。
     */
    override fun onCreateDialog(savedInstanceState: Bundle): Dialog {
        val context = activity
        val content = View.inflate(activity, R.layout.dialog_property_priority, null)

        // 読込み
        itemList.addAll(PropertyItem.loadPropertyPriority(context))

        sortListView = content.findViewById(R.id.propertyPriorityListView) as DragSortListView
        sortListView!!.choiceMode = DragSortListView.CHOICE_MODE_MULTIPLE
        sortListView!!.adapter = object : ArrayAdapter<PropertyItem>(activity, R.layout.layout_property_priority_item, itemList) {

            override fun isEnabled(position: Int): Boolean {
                return true
            }

            // 表示設定
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                var convertView = convertView
                val holder: ListItemViewHolder

                if (convertView == null) {
                    convertView = View.inflate(context, R.layout.layout_property_priority_item, null)
                    holder = ListItemViewHolder()
                    holder.TitleTextView = convertView!!.findViewById(R.id.propertyItemTitle) as TextView
                    holder.DragImageView = convertView.findViewById(R.id.propertyItemImageView) as ImageView
                    holder.ShortenCheckBox = convertView.findViewById(R.id.propertyItemCheckBox) as CheckBox
                    convertView.tag = holder
                } else {
                    holder = convertView.tag as ListItemViewHolder
                }

                val item = getItem(position)
                holder.ShortenCheckBox!!.setOnCheckedChangeListener { buttonView, isChecked -> item!!.shorten = isChecked }
                holder.ShortenCheckBox!!.isChecked = item!!.shorten
                holder.TitleTextView!!.text = item.propertyName

                return convertView
            }

            /** リスト項目のビュー情報を保持するHolder。  */
            internal inner class ListItemViewHolder {
                var TitleTextView: TextView? = null
                var DragImageView: ImageView? = null
                var ShortenCheckBox: CheckBox? = null
            }
        }


        // リスト設定
        val controller = DragSortController(sortListView!!)
        controller.isSortEnabled = true
        controller.isRemoveEnabled = false
        controller.dragInitMode = DragSortController.ON_DRAG
        controller.setDragHandleId(R.id.propertyItemImageView)
        sortListView!!.isDragEnabled = true
        sortListView!!.setFloatViewManager(controller)
        sortListView!!.setOnTouchListener(controller)
        sortListView!!.setDropListener(DragSortListView.DropListener { from, to ->
            // ドラッグ&ドロップ
            if (from == to) return@DropListener
            val item = itemList.removeAt(from)
            itemList.add(to, item)
            sortListView!!.invalidateViews()
        })

        // ダイアログ作成
        val builder = AlertDialog.Builder(activity)
        builder.setTitle(R.string.label_dialog_property_priority_title)
        builder.setView(content)

        val listener = DialogInterface.OnClickListener { dialog, which -> onClickButton(dialog, which) }
        builder.setPositiveButton(android.R.string.ok, listener)
        builder.setNegativeButton(R.string.label_default, listener)
        builder.setNeutralButton(android.R.string.cancel, listener)

        return builder.create()
    }

    override fun onClickButton(dialog: DialogInterface?, which: Int, close: Boolean) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            // 決定
            PropertyItem.savePropertyPriority(activity, itemList)
        } else if (which == DialogInterface.BUTTON_NEGATIVE) {
            // 初期化
            PropertyItem.savePropertyPriority(activity, PropertyItem.getDefaultPropertyPriority(activity))
            AppUtils.showToast(activity, R.string.message_initialize_priority)
        }

        super.onClickButton(dialog, which, close)
    }

    companion object {

        /**
         * ダイアログのインスタンスを作成する。
         * @return ダイアログのインスタンス。
         */
        fun newInstance(): PropertyPriorityDialogFragment {
            val fragment = PropertyPriorityDialogFragment()

            val args = Bundle()
            fragment.arguments = args
            return fragment
        }
    }
}
