package com.wa2c.android.medoly.plugin.action.tweet.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.*
import com.wa2c.android.medoly.plugin.action.tweet.R
import com.wa2c.android.medoly.plugin.action.tweet.activity.PropertyItem
import java.util.*

/**
 * Insert property dialog.
 */
class InsertPropertyDialogFragment : AbstractDialogFragment() {

    /**  Item list. */
    private val itemList = ArrayList<PropertyItem>()

    /** Item select listener.  */
    var itemSelectListener: ItemSelectListener? = null

    /**
     * onCreateDialog
     */
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // List
        itemList.addAll(PropertyItem.getDefaultPropertyPriority(activity))
        val adapter = PropertyListAdapter(activity, itemList)
        adapter.setNotifyOnChange(false)

        // Create list view
        val listView = ListView(activity)
        listView.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        listView.adapter = adapter
        listView.isFastScrollEnabled = true
        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val insert = "%" + itemList[position].propertyKey + "%"
            itemSelectListener?.onItemSelect(insert)
            dialog.dismiss()
        }

        val listLayout = LinearLayout(activity)
        listLayout.layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        listLayout.addView(listView)

        // Build dialog
        val builder = AlertDialog.Builder(activity)
        builder.setTitle(R.string.label_dialog_insert_property_title)
        builder.setView(listLayout)
        builder.setNeutralButton(android.R.string.cancel, null)

        return builder.create()
    }

    /**
     * List adapter
     */
    private inner class PropertyListAdapter(context: Context, itemList: ArrayList<PropertyItem>) : ArrayAdapter<PropertyItem>(context, android.R.layout.simple_list_item_1, itemList) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var view = convertView
            val holder: ListItemViewHolder

            if (view == null) {
                view = View.inflate(activity, android.R.layout.simple_list_item_1, null)
                holder = ListItemViewHolder()
                holder.titleTextView = view!!.findViewById(android.R.id.text1) as TextView
                view.tag = holder
            } else {
                holder = view.tag as ListItemViewHolder
            }

            val item = getItem(position)
            if (item != null) {
                holder.titleTextView!!.text = item.propertyName
            }

            return view
        }

        /** リスト項目のビュー情報を保持するHolder。  */
        internal inner class ListItemViewHolder {
            var titleTextView: TextView? = null
        }

    }



    /**
     * Item select listener
     */
    interface ItemSelectListener : EventListener {
        /**
         * Get insertion text.
         * @param insertString A insertion text.
         */
        fun onItemSelect(insertString: String)
    }

    companion object {
        /**
         * Create dialog instance.
         * @return New dialog instance.
         */
        fun newInstance(): InsertPropertyDialogFragment {
            val fragment = InsertPropertyDialogFragment()
            val args = Bundle()
            fragment.arguments = args
            return fragment
        }
    }

}
