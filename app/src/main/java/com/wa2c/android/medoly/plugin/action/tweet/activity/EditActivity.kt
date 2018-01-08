package com.wa2c.android.medoly.plugin.action.tweet.activity

import android.app.Activity
import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.MenuItem

import com.wa2c.android.medoly.plugin.action.tweet.R
import com.wa2c.android.medoly.plugin.action.tweet.dialog.ConfirmDialogFragment
import com.wa2c.android.medoly.plugin.action.tweet.dialog.InsertPropertyDialogFragment
import com.wa2c.android.medoly.plugin.action.tweet.dialog.PropertyPriorityDialogFragment
import kotlinx.android.synthetic.main.activity_edit.*


class EditActivity : Activity() {

    /** Shared preferences  */
    private lateinit var  preferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)
        preferences = PreferenceManager.getDefaultSharedPreferences(this)

        // Action bar
        actionBar.setDisplayShowHomeEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setDisplayShowTitleEnabled(true)

        // Insert album art button
        insertAlbumArtCheckBox.setOnCheckedChangeListener { _, isChecked ->
            preferences.edit().putBoolean(getString(R.string.prefkey_content_album_art), isChecked).apply()
        }

        // Insert button
        insertButton.setOnClickListener {
            val dialogFragment = InsertPropertyDialogFragment.newInstance()
            dialogFragment.itemSelectListener = object:InsertPropertyDialogFragment.ItemSelectListener {
                override fun onItemSelect(insertString: String) {
                    var text = insertString
                    val index1 = contentEditText.selectionStart
                    val index2 = contentEditText.selectionEnd
                    val start = Math.min(index1, index2)
                    val end = Math.min(index1, index2)
                    val editable = contentEditText!!.text
                    if (start > 0 && editable[start - 1] != ' ') {
                        text = " " + text // スペース挿入
                    }
                    editable.replace(start, end, text)
                }
            }
            dialogFragment.show(this@EditActivity)
        }

        // Priority
        priorityButton.setOnClickListener {
            val dialogFragment = PropertyPriorityDialogFragment.newInstance()
            dialogFragment.clickListener = DialogInterface.OnClickListener { _, _ -> }
            dialogFragment.show(this@EditActivity)
        }

        // Initialize
        initializeButton.setOnClickListener {
            val dialogFragment = ConfirmDialogFragment.newInstance(getString(R.string.message_confirm_initialize_format), getString(R.string.label_confirm))
            dialogFragment.clickListener = DialogInterface.OnClickListener { _, which ->
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    contentEditText!!.setText(getString(R.string.format_content_default))
                    insertAlbumArtCheckBox!!.isChecked = true
                }
            }
            dialogFragment.show(this@EditActivity)
        }

        contentEditText!!.setText(preferences.getString(getString(R.string.prefkey_content_format), getString(R.string.format_content_default)))
        insertAlbumArtCheckBox!!.isChecked = preferences.getBoolean(getString(R.string.prefkey_content_album_art), true)
    }

    /**
     * onStop
     */
    public override fun onStop() {
        super.onStop()

        preferences.edit().putString(getString(R.string.prefkey_content_format), contentEditText.text.toString()).apply()
    }

    /**
     * onOptionsItemSelected
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

}
