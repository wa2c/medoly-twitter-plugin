package com.wa2c.android.medoly.plugin.action.twitter.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;
import com.wa2c.android.medoly.plugin.action.ActionPluginParam;
import com.wa2c.android.medoly.plugin.action.twitter.R;

import java.util.ArrayList;
import java.util.EventListener;


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


		for (ActionPluginParam.MediaProperty p : ActionPluginParam.MediaProperty.values()) {
			PropertyItem item = new PropertyItem();
			item.propertyKey = p.getKeyName();
			item.propertyName = getString(R.string.media) + " - " +  p.getName(context);
			itemList.add(item);
		}

		for (ActionPluginParam.AlbumArtProperty p : ActionPluginParam.AlbumArtProperty.values()) {
			PropertyItem item = new PropertyItem();
			item.propertyKey = p.getKeyName();
			item.propertyName = getString(R.string.album_art) + " - " +  p.getName(context);
			itemList.add(item);
		}

		for (ActionPluginParam.LyricsProperty p : ActionPluginParam.LyricsProperty.values()) {
			PropertyItem item = new PropertyItem();
			item.propertyKey = p.getKeyName();
			item.propertyName = getString(R.string.lyrics) + " - " +  p.getName(context);
			itemList.add(item);
		}

		sortListView = (DragSortListView)content.findViewById(R.id.propertyPriorityListView);
		sortListView.setChoiceMode(DragSortListView.CHOICE_MODE_MULTIPLE);
		sortListView.setAdapter(new ArrayAdapter<PropertyItem>(getActivity(), R.layout.layout_property_priority_item, itemList) {
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
					holder.AbbbrCheckBox = (CheckBox)convertView.findViewById(R.id.propertyItemCheckBox);
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
				public ImageView DragImageView;
				public CheckBox AbbbrCheckBox;
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
//		sortListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//			@Override
//			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//				SparseBooleanArray checked = sortListView.getCheckedItemPositions();
//				sortOrderList.get(position).setDesc(checked.valueAt(position));
//			}
//		});
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
		builder.setTitle("test");
		builder.setView(content);
		builder.setPositiveButton(android.R.string.ok, clickListener);
		builder.setNegativeButton(android.R.string.cancel, clickListener);

		// キャンセルボタン
		builder.setNegativeButton(android.R.string.cancel, null);

		return  builder.create();
	}


	/**
	 * プレイリスト項目。
	 */
	public class PropertyItem {
		public String propertyKey;
		public String propertyName;
	}

}
