package com.wa2c.android.medoly.plugin.action.tweet.activity

import android.content.DialogInterface
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.wa2c.android.medoly.plugin.action.tweet.R
import com.wa2c.android.medoly.plugin.action.tweet.databinding.ActivityEditBinding
import com.wa2c.android.medoly.plugin.action.tweet.dialog.ConfirmDialogFragment
import com.wa2c.android.medoly.plugin.action.tweet.dialog.InsertPropertyDialogFragment
import com.wa2c.android.medoly.plugin.action.tweet.dialog.PropertyPriorityDialogFragment
import com.wa2c.android.prefs.Prefs


/**
 * Edit activity
 */
class EditActivity : AppCompatActivity() {

    private lateinit var  prefs: Prefs
    private lateinit var binding: ActivityEditBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_edit)

        // Action bar
        supportActionBar?.let {
            it.setDisplayShowHomeEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
            it.setDisplayShowTitleEnabled(true)
        }

        // Insert album art button
        binding.insertAlbumArtCheckBox.setOnCheckedChangeListener { _, isChecked ->
            prefs.putBoolean(R.string.prefkey_content_album_art, isChecked)
        }

        // Insert button
        binding.insertButton.setOnClickListener {
            val dialogFragment = InsertPropertyDialogFragment.newInstance()
            dialogFragment.itemSelectListener = object:InsertPropertyDialogFragment.ItemSelectListener {
                override fun onItemSelect(insertString: String) {
                    var text = insertString
                    val index1 = binding.contentEditText.selectionStart
                    val index2 = binding.contentEditText.selectionEnd
                    val start = Math.min(index1, index2)
                    val end = Math.min(index1, index2)
                    val editable = binding.contentEditText.text
                    if (start > 0 && editable[start - 1] != ' ') {
                        text = " $text" // スペース挿入
                    }
                    editable.replace(start, end, text)
                }
            }
            dialogFragment.show(this@EditActivity)
        }

        // Priority
        binding.priorityButton.setOnClickListener {
            val dialogFragment = PropertyPriorityDialogFragment.newInstance()
            dialogFragment.clickListener = listener@{ _, _, _ -> }
            dialogFragment.show(this@EditActivity)
        }

        // Initialize
        binding.initializeButton.setOnClickListener {
            val dialogFragment = ConfirmDialogFragment.newInstance(getString(R.string.message_confirm_initialize_format), getString(R.string.label_confirm))
            dialogFragment.clickListener = listener@{ _, which, _ ->
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    binding.contentEditText.setText(getString(R.string.format_content_default))
                    binding.insertAlbumArtCheckBox.isChecked = true
                }
            }
            dialogFragment.show(this@EditActivity)
        }

        binding.contentEditText.setText(prefs.getString(R.string.prefkey_content_format, defRes = R.string.format_content_default))
        binding.insertAlbumArtCheckBox.isChecked = prefs.getBoolean(R.string.prefkey_content_album_art, true)
    }

    /**
     * onStop
     */
    public override fun onStop() {
        super.onStop()

        prefs.putString(R.string.prefkey_content_format, binding.contentEditText.text)
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
