package com.wa2c.android.medoly.plugin.action.tweet.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import com.mobeta.android.dslv.DragSortController
import com.mobeta.android.dslv.DragSortListView
import com.wa2c.android.medoly.plugin.action.tweet.R
import com.wa2c.android.medoly.plugin.action.tweet.activity.PropertyItem
import com.wa2c.android.medoly.plugin.action.tweet.util.AppUtils
import kotlinx.android.synthetic.main.dialog_property_priority.view.*
import java.util.*


/**
 * Property priority dialog.
 */
class PropertyPriorityDialogFragment : AbstractDialogFragment() {

    /** Item list. */
    private val itemList = ArrayList<PropertyItem>()

    /**
     * onCreateDialog
     */
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val content = View.inflate(activity, R.layout.dialog_property_priority, null)

        // Loading
        itemList.addAll(PropertyItem.loadPropertyPriority(activity))

        // Set data
        content.propertyPriorityListView.choiceMode = DragSortListView.CHOICE_MODE_MULTIPLE
        content.propertyPriorityListView.adapter = object : ArrayAdapter<PropertyItem>(activity, R.layout.layout_property_priority_item, itemList) {

            override fun isEnabled(position: Int): Boolean {
                return true
            }

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                var view = convertView
                val holder: ListItemViewHolder

                if (view == null) {
                    view = View.inflate(context, R.layout.layout_property_priority_item, null)
                    holder = ListItemViewHolder()
                    holder.titleTextView = view.findViewById(R.id.propertyItemTitle) as TextView
                    holder.dragImageView = view.findViewById(R.id.propertyItemImageView) as ImageView
                    holder.shortenCheckBox = view.findViewById(R.id.propertyItemCheckBox) as CheckBox
                    view.tag = holder
                } else {
                    holder = view.tag as ListItemViewHolder
                }

                val item = getItem(position)
                holder.shortenCheckBox!!.setOnCheckedChangeListener { _, isChecked -> item.shorten = isChecked }
                holder.shortenCheckBox!!.isChecked = item.shorten
                holder.titleTextView!!.text = item.propertyName

                return view!!
            }

            /** リスト項目のビュー情報を保持するHolder。  */
            internal inner class ListItemViewHolder {
                var titleTextView: TextView? = null
                var dragImageView: ImageView? = null
                var shortenCheckBox: CheckBox? = null
            }
        }

        // Set view
        val controller = DragSortController(content.propertyPriorityListView)
        controller.isSortEnabled = true
        controller.isRemoveEnabled = false
        controller.dragInitMode = DragSortController.ON_DRAG
        controller.setDragHandleId(R.id.propertyItemImageView)
        content.propertyPriorityListView.isDragEnabled = true
        content.propertyPriorityListView.setFloatViewManager(controller)
        content.propertyPriorityListView.setOnTouchListener(controller)
        content.propertyPriorityListView.setDropListener(DragSortListView.DropListener { from, to ->
            // Drag & Drop
            if (from == to) return@DropListener
            val item = itemList.removeAt(from)
            itemList.add(to, item)
            content.propertyPriorityListView.invalidateViews()
        })

        // Dialog build`
        val listener = DialogInterface.OnClickListener { dialog, which -> onClickButton(dialog, which) }
        val builder = AlertDialog.Builder(activity)
        builder.setTitle(R.string.label_dialog_property_priority_title)
        builder.setView(content)
        builder.setPositiveButton(android.R.string.ok, listener)
        builder.setNegativeButton(R.string.label_default, listener)
        builder.setNeutralButton(android.R.string.cancel, listener)

        return builder.create()
    }

    /**
     * On click action.
     * @param dialog A dialog.
     * @param which A clicked button.
     * @param close True if dialog closing.
     */
    override fun onClickButton(dialog: DialogInterface?, which: Int, close: Boolean) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            // OK
            PropertyItem.savePropertyPriority(activity, itemList)
        } else if (which == DialogInterface.BUTTON_NEGATIVE) {
            // Initialize
            PropertyItem.savePropertyPriority(activity, PropertyItem.getDefaultPropertyPriority(activity))
            AppUtils.showToast(activity, R.string.message_initialize_priority)
        }

        super.onClickButton(dialog, which, close)
    }

    companion object {
        /**
         * Create dialog instance
         */
        fun newInstance(): PropertyPriorityDialogFragment {
            val fragment = PropertyPriorityDialogFragment()

            val args = Bundle()
            fragment.arguments = args
            return fragment
        }
    }

}
