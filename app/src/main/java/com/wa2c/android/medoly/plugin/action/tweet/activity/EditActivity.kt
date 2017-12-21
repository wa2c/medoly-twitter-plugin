package com.wa2c.android.medoly.plugin.action.tweet.activity

import android.app.ActionBar
import android.app.Activity
import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.Editable
import android.view.MenuItem
import android.view.View
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.EditText

import com.wa2c.android.medoly.plugin.action.tweet.R
import com.wa2c.android.medoly.plugin.action.tweet.dialog.ConfirmDialogFragment
import com.wa2c.android.medoly.plugin.action.tweet.dialog.InsertPropertyDialogFragment
import com.wa2c.android.medoly.plugin.action.tweet.dialog.PropertyPriorityDialogFragment


class EditActivity : Activity() {

    /** 設定。  */
    private var sharedPreferences: SharedPreferences? = null
    /** 編集テキスト。  */
    private var contentEditText: EditText? = null
    /** アルバムアート挿入  */
    private var insertAlbumArtCheckBox: CheckBox? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        contentEditText = findViewById(R.id.contentEditText) as EditText
        insertAlbumArtCheckBox = findViewById(R.id.insertAlbumArtCheckBox) as CheckBox

        // アクションバー
        val actionBar = actionBar
        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(true)
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setDisplayShowTitleEnabled(true)
        }

        // アルバムアート追加
        insertAlbumArtCheckBox!!.setOnCheckedChangeListener { buttonView, isChecked -> sharedPreferences!!.edit().putBoolean(getString(R.string.prefkey_content_album_art), isChecked).apply() }

        // 挿入
        findViewById(R.id.insertButton).setOnClickListener {
            val dialogFragment = InsertPropertyDialogFragment.newInstance()
            dialogFragment.setOnItemSelectListener { insertString ->
                var insertString = insertString
                val index1 = contentEditText!!.selectionStart
                val index2 = contentEditText!!.selectionEnd
                val start = Math.min(index1, index2)
                val end = Math.min(index1, index2)
                val editable = contentEditText!!.text
                if (start > 0 && editable[start - 1] != ' ') {
                    insertString = " " + insertString // スペース挿入
                }
                editable.replace(start, end, insertString)
            }
            dialogFragment.show(this@EditActivity)
        }

        // 優先度
        findViewById(R.id.priorityButton).setOnClickListener {
            val dialogFragment = PropertyPriorityDialogFragment.newInstance()
            dialogFragment.setClickListener { dialog, which -> }
            dialogFragment.show(this@EditActivity)
        }

        // 初期化
        findViewById(R.id.initializeButton).setOnClickListener {
            val dialogFragment = ConfirmDialogFragment.newInstance(getString(R.string.message_confirm_initialize_format), getString(R.string.label_confirm))
            dialogFragment.setClickListener { dialog, which ->
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    contentEditText!!.setText(getString(R.string.format_content_default))
                    insertAlbumArtCheckBox!!.isChecked = true
                }
            }
            dialogFragment.show(this@EditActivity)
        }

        contentEditText!!.setText(sharedPreferences!!.getString(getString(R.string.prefkey_content_format), getString(R.string.format_content_default)))
        insertAlbumArtCheckBox!!.isChecked = sharedPreferences!!.getBoolean(getString(R.string.prefkey_content_album_art), true)
    }

    /**
     * onStopイベント処理。 c
     */
    public override fun onStop() {
        super.onStop()

        // テキスト保存
        sharedPreferences!!.edit().putString(getString(R.string.prefkey_content_format), contentEditText!!.text.toString()).apply()
    }

    /**
     * onOptionsItemSelected event.
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
