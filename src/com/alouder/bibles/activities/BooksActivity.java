package com.alouder.bibles.activities;

import java.util.ArrayList;

import android.app.Activity;
import android.app.ListActivity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.alouder.bibles.R;
import com.alouder.bibles.data.BiblesContentProvider;
import com.alouder.bibles.data.BookEnum;
import com.alouder.bibles.data.Work;
import com.alouder.bibles.text2speech.TextToSpeechWrapper;
import com.alouder.bibles.widgets.EasyGestureListener;
import com.alouder.bibles.widgets.EasyGestureListener.DetectedActionCode;
import com.alouder.bibles.widgets.EasyGestureListener.OnDetectedActionListener;
import com.alouder.bibles.widgets.MyRelativeSizeSpan;

public class BooksActivity extends ListActivity implements
		OnDetectedActionListener
// , EasyTableLayout.onCellSelectedListener
{
	public static final String TAG = BooksActivity.class.getSimpleName();
	public static final String WORK = "WORK";
	public static final String SELECTED = "SELECTED";
	public static final String TOP_INDEX = "TOP_INDEX";
	public static final String TOP_OFFSET = "TOP_OFFSET";
	
	public static final int SHOW_CHAPTERS = 900001;
	
	float adjusted = AloudBibleApplication.getSizeProportion();
	TextView titleTextView = null;
	
	ListView listView;
	LinearLayout header;
	ImageButton buttonPreference, buttonManage, buttonDownload, buttonInfo;
	BooksAdapter adapter;
	int headerViewsCount = 0;
	int selectedListPosition = -1;
	int topIndex = -1;
	int topOffset = 0;
	float remained = 1f;
	
	EasyGestureListener myGestureListener = null;
	
	OnClickListener preferenceListener = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			Intent i = new Intent(BooksActivity.this, 
					TextToSpeechWrapper.isOldBuild ? PreferenceOldActivity.class : PreferenceActivity.class);
			
			BooksActivity.this.startActivityForResult(i, AloudBibleApplication.SHOW_PREFERENCE);
		}
	};
	
	OnClickListener manageListener = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			Intent intent = new Intent(BooksActivity.this, ManageWorksActivity.class);
			BooksActivity.this.startActivity(intent);
		}
	};
	
	OnClickListener downloadListener = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			Intent intent = new Intent(BooksActivity.this, DownloadActivity.class);
			BooksActivity.this.startActivity(intent);
		}
	};
	
	OnClickListener infoListener = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			Intent intent = new Intent(BooksActivity.this, HelpActivity.class);
			BooksActivity.this.startActivity(intent);
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		listView = getListView();
		
		header = (LinearLayout) this.getLayoutInflater().inflate(R.layout.work_header, null);
		listView.addHeaderView(header);
		headerViewsCount = listView.getHeaderViewsCount();
		titleTextView = (TextView)header.findViewById(R.id.workTitle);
		
		myGestureListener = new EasyGestureListener(false);
		myGestureListener.setOnDetectedAction(this);
		listView.setOnTouchListener(myGestureListener);
		
		AloudBibleApplication.application.registerTtsActivity();
		this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
		
		if (BiblesContentProvider.primaryWork == null) {
			ContentResolver cr = this.getContentResolver();
			Cursor c = cr.query(BiblesContentProvider.CONTENT_URI, null, null, null, null);
			if (c!= null)
				c.close();
		}
		
		String title = BiblesContentProvider.secondaryWork == null ?
				String.format("%s(%s)", BiblesContentProvider.primaryWork.work,
						BiblesContentProvider.primaryWork.code)
				: String.format("%s | %s", BiblesContentProvider.primaryWork.code,
						BiblesContentProvider.secondaryWork.code );
		AloudBibleApplication.setText(titleTextView, title);
		
		adapter = new BooksAdapter(this, new ArrayList<BookEnum>());
		listView.setAdapter(adapter);
		
		buttonPreference = (ImageButton) header.findViewById(R.id.imageButtonPreference);
		buttonManage = (ImageButton)header.findViewById(R.id.imageButtonManage);
		buttonDownload = (ImageButton)header.findViewById(R.id.imageButtonDownload);
		buttonInfo = (ImageButton)header.findViewById(R.id.imageButtonInfo);
		
		buttonPreference.setOnClickListener(preferenceListener);
		buttonManage.setOnClickListener(manageListener);
		buttonDownload.setOnClickListener(downloadListener);
		buttonInfo.setOnClickListener(infoListener);
		
		show();
		
		if ((savedInstanceState != null)
				&& savedInstanceState.containsKey(SELECTED))
		{
			selectedListPosition = savedInstanceState.getInt(SELECTED);
			topIndex = savedInstanceState.getInt(TOP_INDEX, -1);
			topOffset = savedInstanceState.getInt(TOP_OFFSET, 0);
			if (topIndex >= 0)
			{
				listView.setSelectionFromTop(topIndex, topOffset);
			}
			else if (selectedListPosition >= 0)
			{
				listView.setSelection(selectedListPosition);
			}
		}	
	}
	
	private void show() {
		String title = BiblesContentProvider.secondaryWork == null ?
				String.format("%s(%s)", BiblesContentProvider.primaryWork.work,
						BiblesContentProvider.primaryWork.code)
				: String.format("%s | %s", BiblesContentProvider.primaryWork.code,
						BiblesContentProvider.secondaryWork.code );
		AloudBibleApplication.setText(titleTextView, title);

		adapter.reload();
	}
	
	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		AloudBibleApplication.application.unregisterTtsActivity();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		saveTopViewState();
		outState.putInt(SELECTED, selectedListPosition);
		outState.putInt(TOP_INDEX, topIndex);
		outState.putInt(TOP_OFFSET, topOffset);
		super.onSaveInstanceState(outState);
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
		float currentProportion = AloudBibleApplication.getSizeProportion();
		if (adjusted != currentProportion)
		{
			resize(0);
		}
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id)
	{
		super.onListItemClick(l, v, position, id);
		v.setSelected(true);
		
		this.selectedListPosition = position - headerViewsCount;
		BookEnum bookEnum = BookEnum.fromOrdinal(this.selectedListPosition
				+ BookEnum.BOOK_NUM_OFFSET);
		
		Intent intent = new Intent(this, ChapterActivity.class);
		intent.putExtra(ChapterActivity.BOOK_ENUM, bookEnum.name());
		startActivityForResult(intent, SHOW_CHAPTERS);
		overridePendingTransition(android.R.anim.fade_in,
				android.R.anim.fade_out);
	}
	
	@Override
	public boolean onDetectedAction(DetectedActionCode code)
	{
		switch (code)
		{
			case ZOOM_IN:
				resize(MyRelativeSizeSpan.AdjustStep);
				Log.i(TAG, "Zoom_In handled, adjustedProportion=" + adjusted);
				return true;
			case ZOOM_OUT:
				resize(-MyRelativeSizeSpan.AdjustStep);
				Log.i(TAG, "Zoom_Out handled, adjustedProportion=" + adjusted);
				return true;
			case DUAL_MODE_BEGIN:
				View selected = listView.getSelectedView();
				if (selected != null)
				{
					selected.setSelected(false);
				}
				
				saveTopViewState();
				break;
			case DUAL_MODE_END:
				break;
			default:
				break;
		}
		
		return false;
	}
	
	private void resize(float delta)
	{
		adjusted = AloudBibleApplication.adjustSizeSpan(delta);
		titleTextView.requestLayout();
		
		if (adapter != null)
			adapter.notifyDataSetChanged();
		listView.invalidate();
		
		// listView.setSelectionFromTop(topIndex, topOffset);
		// Log.d(TAG, "setSelectionFromTop(topIndex=" + topIndex +
		// ", topOffset=" + topOffset);
		
		View v = listView.getChildAt(0);
		int offset = (int) (remained * v.getHeight());
		listView.setSelectionFromTop(topIndex, offset);
		Log.d(TAG, "setSelectionFromTop(topIndex=" + topIndex + ", topOffset="
				+ topOffset + ", offset=" + offset);
	}
		
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		return AloudBibleApplication.onCreateOptionsMenu(menu, this);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {		
		return AloudBibleApplication.onOptionItemsSelected(item, this);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == AloudBibleApplication.SHOW_PREFERENCE && resultCode == Activity.RESULT_OK)
		{
			show();
		} else if (requestCode == SHOW_CHAPTERS) {
			Bundle bundle = data.getExtras();
			if (bundle != null) {
				String bookString = bundle.getString(BiblesContentProvider.KEY_BOOK_ID);
				BookEnum bookEnum = (bookString != null ) ? BookEnum.valueOf(bookString) : BookEnum.Genesis;
				adapter.setSelectedPosition(bookEnum);				
			}
		}
			
	}

	private void saveTopViewState()
	{
		topIndex = listView.getFirstVisiblePosition();
		View v = listView.getChildAt(0);
		if (v == null)
		{
			return;
		}
		
		topOffset = (v == null) ? 0 : v.getTop();
		remained = (float) topOffset / v.getHeight();
//		TextView textView = (TextView) listView.getChildAt(0);
//		Log.d(TAG, "topIndex=" + topIndex + ", topOffset=" + topOffset
//				+ ", remained=" + remained
//				+ (textView == null ? "" : " " + textView.getText()));
	}
	
	public class BooksAdapter extends ArrayAdapter<BookEnum>
	{
		/**
		 * Lock used to modify the content of {@link #mObjects}. Any write
		 * operation performed on the array should be synchronized on this lock.
		 * This lock is also used by the filter (see {@link #getFilter()} to
		 * make a synchronized copy of the original array of data.
		 */
		private final Object mLock = new Object();
		public final String ADAPTER = BooksAdapter.class.getSimpleName();
		
		private final ArrayList<BookEnum> bookEnumList;
		private final Context mContext;
		private final LayoutInflater layoutInflater;
		private Work primaryWork, secondaryWork;
		
		public BooksAdapter(Context context, ArrayList<BookEnum> list)
		{
			super(context, R.layout.text_2, list);
			mContext = context;
			bookEnumList = list;
			layoutInflater = LayoutInflater.from(mContext);
			reload();
		}
		
		public void reload() {
			
			if (primaryWork == BiblesContentProvider.primaryWork && secondaryWork == BiblesContentProvider.secondaryWork)
				return;
			
			bookEnumList.clear();
			
			primaryWork = BiblesContentProvider.primaryWork;
			secondaryWork = BiblesContentProvider.secondaryWork;
			bookEnumList.addAll(primaryWork.bookNameMap.keySet());
			
			this.notifyDataSetChanged();
		}		
		
		@Override
		public int getCount()
		{
			return bookEnumList.size();
		}
		
		private int selectedPosition = ListView.INVALID_POSITION;
		
		public void setSelectedPosition(int position)
		{
			if (selectedPosition != position)
			{
				Log.d(ADAPTER, "selectedPosition is changed to " + position);
				selectedPosition = position;
			
				adapter.notifyDataSetChanged();
			}
		}
		
		public int setSelectedPosition(BookEnum bookEnum)
		{
			int index = bookEnumList.indexOf(bookEnum);
			if (selectedPosition != index){
				selectedPosition = index;
				notifyDataSetChanged();
			}
			return index;
		}
		
		public int getSelectedPosition()
		{
			return selectedPosition;
		}
		
		@Override
		public BookEnum getItem(int position)
		{
			if ((position < 0) || (position >= bookEnumList.size()))
			{
				return null;
			}
			
			return bookEnumList.get(position);
		}
		
		public BookEnum getSelectedBookEnum()
		{
			return selectedPosition == AdapterView.INVALID_POSITION ? BookEnum.NONE
					: getItem(selectedPosition);
		}
		
		@Override
		public long getItemId(int position)
		{
			return position;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			BookEnum bookEnum = bookEnumList.get(position);
			LinearLayout layout = null;
			if (convertView == null)
			{
				layout = (LinearLayout) layoutInflater.inflate(R.layout.text_2, null, false);
			}
			else
			{
				layout = (LinearLayout) convertView;
			}
			layout.setTag(bookEnum);
			
			TextView text1 = (TextView) layout.findViewById(android.R.id.text1);
			
			if (primaryWork != null)
				AloudBibleApplication.setText(text1, primaryWork.bookNameOf(bookEnum));
			
			TextView text2 = (TextView) layout.findViewById(android.R.id.text2);
			if (secondaryWork != null) {
				AloudBibleApplication.setText(text2, secondaryWork.bookNameOf(bookEnum));
			} else {
				text2.setText("");
			}
			
			if (selectedPosition == position)
			{
				layout.setSelected(true);
				layout.setBackgroundResource(R.color.baby_blue_eyes);
//				Log.d(ADAPTER, layout.toString() + "@" + position
//						+ " selected"
//						+ (layout.isSelected() ? " really" : " not really"));
			}
			else
			{
				layout.setBackgroundResource(android.R.color.transparent);
			}
			
			return layout;
		}
		
		/**
		 * Remove all elements from the list.
		 */
		@Override
		public void clear()
		{
			synchronized (mLock)
			{
				if (bookEnumList != null)
				{
					bookEnumList.clear();
					selectedPosition = -1;
				}
			}
			notifyDataSetChanged();
		}

	}
}
