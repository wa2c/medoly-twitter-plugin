package com.wa2c.android.medoly.plugin.action.twitter;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

import com.wa2c.android.medoly.plugin.action.twitter.dialog.ConfirmDialogFragment;
import com.wa2c.android.medoly.plugin.action.twitter.dialog.InsertPropertyDialogFragment;
import com.wa2c.android.medoly.plugin.action.twitter.dialog.PropertyPriorityDialogFragment;


public class EditActivity extends Activity {

    /** 設定。 */
    private SharedPreferences sharedPreferences;
    /** 編集テキスト。 */
    private EditText contentEditText;
    /** アルバムアート挿入 */
    private CheckBox insertAlbumArtCheckBox;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        contentEditText = (EditText)findViewById(R.id.contentEditText);
        insertAlbumArtCheckBox = (CheckBox)findViewById(R.id.insertAlbumArtCheckBox);

        // アクションバー
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(true);
        }

        // アルバムアート追加
        insertAlbumArtCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                sharedPreferences.edit().putBoolean(getString(R.string.prefkey_content_album_art), isChecked).apply();
            }
        });

        // 挿入
        findViewById(R.id.insertButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                InsertPropertyDialogFragment dialogFragment = InsertPropertyDialogFragment.newInstance();
                dialogFragment.setOnItemSelectListener(new InsertPropertyDialogFragment.ItemSelectListener() {
                    @Override
                    public void onItemSelect(String insertString) {
                        int index1 = contentEditText.getSelectionStart();
                        int index2 = contentEditText.getSelectionEnd();
                        int start = Math.min(index1, index2);
                        int end = Math.min(index1, index2);
                        Editable editable = contentEditText.getText();
                        if (start > 0 && editable.charAt(start - 1) != ' ') {
                            insertString = " " + insertString; // スペース挿入
                        }
                        editable.replace(start, end, insertString);
                    }
                });
                dialogFragment.show(EditActivity.this);
            }
        });

        // 優先度
        findViewById(R.id.priorityButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                PropertyPriorityDialogFragment dialogFragment = PropertyPriorityDialogFragment.newInstance();
//                dialogFragment.setOnItemSelectListener(new InsertPropertyDialogFragment.ItemSelectListener() {
//                    @Override
//                    public void onItemSelect(String insertString) {
//                        int index1 = contentEditText.getSelectionStart();
//                        int index2 = contentEditText.getSelectionEnd();
//                        int start = Math.min(index1, index2);
//                        int end = Math.min(index1, index2);
//                        Editable editable = contentEditText.getText();
//                        if (start > 0 && editable.charAt(Math.min(start, end) - 1) == ' ') {
//                            insertString = " " + insertString; // スペース挿入
//                        }
//                        editable.replace(start, end, insertString);
//                    }
//                });
                dialogFragment.show(EditActivity.this);
            }
        });


        // 初期化
        findViewById(R.id.initializeButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                ConfirmDialogFragment dialogFragment = ConfirmDialogFragment.newInstance("確認", "確認");
                dialogFragment.setClickListener(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            contentEditText.setText(getString(R.string.format_content_default));
                            insertAlbumArtCheckBox.setChecked(true);
                        }
                    }
                });
                dialogFragment.show(EditActivity.this);
            }
        });

        contentEditText.setText(sharedPreferences.getString(getString(R.string.prefkey_content_format), getString(R.string.format_content_default)));
        insertAlbumArtCheckBox.setChecked(sharedPreferences.getBoolean(getString(R.string.prefkey_content_album_art), true));
    }

    /**
     * onStopイベント処理。 c
     */
    @Override
    public void onStop() {
        super.onStop();

        // テキスト保存
        sharedPreferences.edit().putString(getString(R.string.prefkey_content_format), contentEditText.getText().toString()).apply();

     }

    /**
     * onOptionsItemSelected event.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
