package com.wa2c.android.medoly.plugin.action.tweet.activity

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.wa2c.android.medoly.plugin.action.tweet.R
import com.wa2c.android.medoly.plugin.action.tweet.activity.component.viewBinding
import com.wa2c.android.medoly.plugin.action.tweet.databinding.FragmentEditBinding
import com.wa2c.android.medoly.plugin.action.tweet.dialog.ConfirmDialogFragment
import com.wa2c.android.medoly.plugin.action.tweet.dialog.InsertPropertyDialogFragment
import com.wa2c.android.medoly.plugin.action.tweet.dialog.PropertyPriorityDialogFragment
import com.wa2c.android.prefs.Prefs
import kotlin.math.min

/**
 * Edit fragment
 */
class EditFragment : Fragment(R.layout.fragment_edit) {

    /** Binding */
    private val binding: FragmentEditBinding? by viewBinding()
    /** Prefs */
    private val prefs: Prefs by lazy { Prefs(requireContext()) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.setTitle(R.string.title_screen_edit)

        binding?.let { binding ->
            // Insert album art button
            binding.insertAlbumArtCheckBox.setOnCheckedChangeListener { _, isChecked ->
                prefs.putBoolean(R.string.prefkey_content_album_art, isChecked)
            }

            // Insert button
            binding.insertButton.setOnClickListener {
                val dialogFragment = InsertPropertyDialogFragment.newInstance()
                dialogFragment.itemSelectListener = object: InsertPropertyDialogFragment.ItemSelectListener {
                    override fun onItemSelect(insertString: String) {
                        var text = insertString
                        val index1 = binding.contentEditText.selectionStart
                        val index2 = binding.contentEditText.selectionEnd
                        val start = min(index1, index2)
                        val end = min(index1, index2)
                        val editable = binding.contentEditText.text
                        if (start > 0 && editable[start - 1] != ' ') {
                            text = " $text" // スペース挿入
                        }
                        editable.replace(start, end, text)
                    }
                }
                dialogFragment.show(requireActivity())
            }

            // Priority
            binding.priorityButton.setOnClickListener {
                val dialogFragment = PropertyPriorityDialogFragment.newInstance()
                dialogFragment.clickListener = listener@{ _, _, _ -> }
                dialogFragment.show(requireActivity())
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
                dialogFragment.show(requireActivity())
            }

            binding.contentEditText.setText(prefs.getString(R.string.prefkey_content_format, defRes = R.string.format_content_default))
            binding.insertAlbumArtCheckBox.isChecked = prefs.getBoolean(R.string.prefkey_content_album_art, true)
        }
    }

    override fun onStop() {
        super.onStop()
        prefs.putString(R.string.prefkey_content_format, binding?.contentEditText?.text)
    }

}
