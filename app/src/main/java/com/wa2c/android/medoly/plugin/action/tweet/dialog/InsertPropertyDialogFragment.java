package com.wa2c.android.medoly.plugin.action.tweet.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.wa2c.android.medoly.plugin.action.tweet.PropertyItem;
import com.wa2c.android.medoly.plugin.action.tweet.R;

import java.util.ArrayList;
import java.util.EventListener;


public class InsertPropertyDialogFragment extends AbstractDialogFragment {

    /**
     * ダイアログのインスタンスを作成する。
     * @return ダイアログのインスタンス。
     */
    static public InsertPropertyDialogFragment newInstance() {
        InsertPropertyDialogFragment fragment = new InsertPropertyDialogFragment();

        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }



    /**
     * onCreateDialogイベント処理。
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity context = getActivity();

        // 再生キュー
        itemList.addAll(PropertyItem.getDefaultPropertyPriority(context));

        // リストアダプタ
        final PropertytListAdapter adapter = new PropertytListAdapter(context, itemList);
        adapter.setNotifyOnChange(false);

        // リスト作成
        final ListView listView = new ListView(context);
        listView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        listView.setAdapter(adapter);
        listView.setFastScrollEnabled(true);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (itemSelectListener != null) {
                    String insert = "%" +itemList.get(position).propertyKey + "%";
                    itemSelectListener.onItemSelect(insert);
                }
                getDialog().dismiss();
            }
        });

        final LinearLayout listLayout = new LinearLayout(context);
        listLayout.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        listLayout.addView(listView);

        // ダイアログ
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.label_dialog_insert_property_title);
        builder.setView(listLayout);

        // キャンセルボタン
        builder.setNegativeButton(android.R.string.cancel, null);

        return  builder.create();
    }

    /**
     * 項目リスト。
     */
    private final ArrayList<PropertyItem> itemList = new ArrayList<>();

    /**
     * リストアダプタ。
     */
    private class PropertytListAdapter extends ArrayAdapter<PropertyItem> {
        /** コンストラクタ。 */
        public PropertytListAdapter(Context context, ArrayList<PropertyItem> itemList) {
            super(context, android.R.layout.simple_list_item_1, itemList);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            final PropertyItem item = getItem(position);
            final ListItemViewHolder holder;

            if (convertView == null) {
                convertView = View.inflate(getActivity(), android.R.layout.simple_list_item_1, null);
                holder = new ListItemViewHolder();
                holder.TitleTextView = (TextView)convertView.findViewById(android.R.id.text1);
                convertView.setTag(holder);
            } else {
                holder = (ListItemViewHolder) convertView.getTag();
            }

            holder.TitleTextView.setText(item.propertyName);

            return convertView;
        }

        /** リスト項目のビュー情報を保持するHolder。 */
        class ListItemViewHolder {
            public TextView TitleTextView;
        }

    }




    /** 選択リスナ。 */
    private ItemSelectListener itemSelectListener;

    /**
     * 選択リスナを設定する。
     * @param listener リスナ。
     */
    public void setOnItemSelectListener(ItemSelectListener listener) {
        this.itemSelectListener = listener;
    }

    /**
     * 選択インターフェース。。
     */
    public interface ItemSelectListener extends EventListener {
        /**
         * 挿入文字を取得する。
         * @param insertString 挿入文字。
         */
        void onItemSelect(String insertString);
    }

}
