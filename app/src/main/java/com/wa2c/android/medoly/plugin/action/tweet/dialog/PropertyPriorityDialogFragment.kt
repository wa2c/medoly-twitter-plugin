package com.wa2c.android.medoly.plugin.action.tweet.dialog

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import com.mobeta.android.dslv.DragSortController
import com.mobeta.android.dslv.DragSortListView
import com.wa2c.android.medoly.plugin.action.tweet.R
import com.wa2c.android.medoly.plugin.action.tweet.activity.PropertyItem
import com.wa2c.android.medoly.plugin.action.tweet.databinding.DialogPropertyPriorityBinding
import com.wa2c.android.medoly.plugin.action.tweet.util.AppUtils
import kotlinx.android.synthetic.main.layout_property_priority_item.view.*
import java.util.*


/**
 * Property priority dialog.
 */
class PropertyPriorityDialogFragment : AbstractDialogFragment() {

    private lateinit var binding: DialogPropertyPriorityBinding

    /** Item list. */
    private val itemList = ArrayList<PropertyItem>()

    /**
     * onCreateDialog
     */
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)
        binding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.dialog_property_priority, null, false)

        // Loading
        itemList.addAll(PropertyItem.loadPropertyPriority(context))

        // Set data
        binding.propertyPriorityListView.choiceMode = DragSortListView.CHOICE_MODE_MULTIPLE
        binding.propertyPriorityListView.adapter = object : ArrayAdapter<PropertyItem>(context, R.layout.layout_property_priority_item, itemList) {

            override fun isEnabled(position: Int): Boolean {
                return true
            }

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val holder = if (convertView == null) {
                    ListItemViewHolder(parent.context)
                } else {
                    convertView.tag as ListItemViewHolder
                }
                holder.bind(getItem(position)!!)
                return holder.itemView
            }

            /** List item view holder  */
            private inner class ListItemViewHolder(context: Context) {
                val itemView = View.inflate(context, R.layout.layout_property_priority_item, null)!!
                init {
                    itemView.tag = this
                }

                fun bind(item: PropertyItem) {
                    itemView.propertyItemTitle.text = item.propertyName
                    itemView.propertyItemCheckBox.setOnCheckedChangeListener { _, isChecked ->
                        item.shorten = isChecked
                    }
                    itemView.propertyItemCheckBox.isChecked = item.shorten
                }
            }
        }

        // Set view
        val controller = DragSortController(binding.propertyPriorityListView).apply {
            isSortEnabled = true
            isRemoveEnabled = false
            dragInitMode = DragSortController.ON_DRAG
            setDragHandleId(R.id.propertyItemImageView)
        }
        binding.propertyPriorityListView.isDragEnabled = true
        binding.propertyPriorityListView.setFloatViewManager(controller)
        binding.propertyPriorityListView.setOnTouchListener(controller)
        binding.propertyPriorityListView.setDropListener(DragSortListView.DropListener { from, to ->
            // Drag & Drop
            if (from == to) return@DropListener
            val item = itemList.removeAt(from)
            itemList.add(to, item)
            binding.propertyPriorityListView.invalidateViews()
        })

        // Dialog build`
        return AlertDialog.Builder(context).apply {
            setTitle(R.string.label_dialog_property_priority_title)
            setView(binding.root)
            setPositiveButton(android.R.string.ok, null)
            setNegativeButton(R.string.label_default, null)
            setNeutralButton(android.R.string.cancel, null)
        }.create()
    }

    override fun invokeListener(which: Int, bundle: Bundle?, close: Boolean) {
        val result = bundle ?: Bundle()
        if (which == DialogInterface.BUTTON_POSITIVE) {
            PropertyItem.savePropertyPriority(context, itemList)
        } else if (which == DialogInterface.BUTTON_NEGATIVE) {
            PropertyItem.savePropertyPriority(context, PropertyItem.getDefaultPropertyPriority(context))
            AppUtils.showToast(context, R.string.message_initialize_priority)
        }
        super.invokeListener(which, result, close)
    }


    companion object {
        /**
         * Create dialog instance
         */
        fun newInstance(): PropertyPriorityDialogFragment {
            return PropertyPriorityDialogFragment().apply {
                arguments = Bundle()
            }
        }
    }

}
