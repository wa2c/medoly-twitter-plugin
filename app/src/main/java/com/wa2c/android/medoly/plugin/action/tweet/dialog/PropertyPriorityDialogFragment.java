package com.wa2c.android.medoly.plugin.action.tweet.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;
import com.wa2c.android.medoly.plugin.action.tweet.AppUtils;
import com.wa2c.android.medoly.plugin.action.tweet.PropertyItem;
import com.wa2c.android.medoly.plugin.action.tweet.R;

import java.util.ArrayList;


public class PropertyPriorityDialogFragment extends AbstractDialogFragment {

    /**
     * ダイアログのインスタンスを作成する。
     * @return ダイアログのインスタンス。
     */
    static public PropertyPriorityDialogFragment newInstance() {
        PropertyPriorityDialogFragment fragment = new PropertyPriorityDialogFragment();

        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }


    /**
     * 項目リスト。
     */
    private final ArrayList<PropertyItem> itemList = new ArrayList<>();

    private DragSortListView sortListView;


    /**
     * onCreateDialogイベント処理。
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity context = getActivity();
        final View content = View.inflate(getActivity(), R.layout.dialog_property_priority, null);

        // 読込み
        itemList.addAll(PropertyItem.loadPropertyPriority(context));

        sortListView = (DragSortListView)content.findViewById(R.id.propertyPriorityListView);
        sortListView.setChoiceMode(DragSortListView.CHOICE_MODE_MULTIPLE);
        sortListView.setAdapter(new ArrayAdapter<PropertyItem>(getActivity(), R.layout.layout_property_priority_item, itemList) {

            @Override
            public boolean isEnabled(int position) {
                return false;
            }

            // 表示設定
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                final PropertyItem item = getItem(position);
                final ListItemViewHolder holder;

                if (convertView == null) {
                    convertView = View.inflate(context, R.layout.layout_property_priority_item, null);
                    holder = new ListItemViewHolder();
                    holder.TitleTextView = (TextView)convertView.findViewById(R.id.propertyItemTitle);
                    holder.DragImageView = (ImageView)convertView.findViewById(R.id.propertyItemImageView);
                    holder.ShortenCheckBox = (CheckBox)convertView.findViewById(R.id.propertyItemCheckBox);
                    convertView.setTag(holder);
                } else {
                    holder = (ListItemViewHolder) convertView.getTag();
                }

                holder.ShortenCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        item.shorten = isChecked;
                    }
                });
                holder.ShortenCheckBox.setChecked(item.shorten);

                holder.TitleTextView.setText(item.propertyName);
                return convertView;
            }

            /** リスト項目のビュー情報を保持するHolder。 */
            class ListItemViewHolder {
                public TextView TitleTextView;
                public ImageView DragImageView;
                public CheckBox ShortenCheckBox;
            }
        });



        // リスト設定
        DragSortController controller = new DragSortController(sortListView);
        controller.setSortEnabled(true);
        controller.setRemoveEnabled(false);
        controller.setDragInitMode(DragSortController.ON_DRAG);
        controller.setDragHandleId(R.id.propertyItemImageView);
        sortListView.setDragEnabled(true);
        sortListView.setFloatViewManager(controller);
        sortListView.setOnTouchListener(controller);
        sortListView.setDropListener(new DragSortListView.DropListener() {
            // ドラッグ&ドロップ
            @Override
            public void drop(int from, int to) {
                if (from == to) return;
                PropertyItem item = itemList.remove(from);
                itemList.add(to, item);
                sortListView.invalidateViews();
            }
        });

        // ダイアログ作成
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.label_dialog_property_priority_title);
        builder.setView(content);


        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                onClickButton(dialog, which);
            }
        };
        builder.setPositiveButton(android.R.string.ok, listener);
        builder.setNeutralButton(R.string.label_default, listener);
        builder.setNegativeButton(android.R.string.cancel, listener);

        // キャンセルボタン
        builder.setNegativeButton(android.R.string.cancel, null);

        return  builder.create();
    }

    @Override
    protected void onClickButton(DialogInterface dialog, int which, boolean close) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            // 決定
            PropertyItem.savePropertyPriority(getActivity(), itemList);
        } else if (which == DialogInterface.BUTTON_NEUTRAL) {
            // 初期化
            PropertyItem.savePropertyPriority(getActivity(), PropertyItem.getDefaultPropertyPriority(getActivity()));
            AppUtils.showToast(getActivity(), R.string.message_initialize_priority);
        }

        super.onClickButton(dialog, which, close);
    }
}
