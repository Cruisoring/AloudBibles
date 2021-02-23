package com.alouder.bibles.activities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.alouder.bibles.R;
import com.alouder.bibles.data.BiblesContentProvider;
import com.alouder.bibles.data.Book;
import com.alouder.bibles.data.BookEnum;
import com.alouder.bibles.data.Chapter;
import com.alouder.bibles.data.Verse;
import com.alouder.bibles.data.Work;
import com.alouder.bibles.text2speech.TtsService;
import com.alouder.bibles.widgets.EasyGestureListener;
import com.alouder.bibles.widgets.EasyGestureListener.DetectedActionCode;
import com.alouder.bibles.widgets.EasyTableLayout;
import com.alouder.bibles.widgets.MyRelativeSizeSpan;
import com.alouder.bibles.widgets.NavigatorBar;

import android.app.Activity;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Spannable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class VersesActivity extends ListActivity implements
		EasyTableLayout.onCellSelectedListener,
		NavigatorBar.onNavigationSelectedListener,
		EasyGestureListener.OnDetectedActionListener
{
	public static final String TAG = VersesActivity.class.getSimpleName();
	private static final String ADAPTER = VersesAdapter.class.getSimpleName();
	public static final String ACTION_PLAY_START = "ACTION_PLAY_START";
	public static final String ACTION_PLAY_DONE = "ACTION_PLAY_DONE";
	
	public static final String BUNDLE = "BUNDLE";
	public static final String CHAPTER_NUMBER = "ChapterNum";
	public static final String FIRST_VISIBLE = "firstVisible";
	public static final String SELECTED_POSITION = "selectedPosition";
	
	TextView titleView;
	EasyTableLayout tableLayout;
	ListView listView;
	NavigatorBar footerBar;
	
	Chapter primaryChapter, secondaryChapter;
	VersesAdapter adapter;
	int position = 0;
	int headerViewsCount = 0;
	
	EasyGestureListener myGestureListener = null;
	
//	TtsWrapperService mTts;
	TtsService mTts;
	boolean mBound = false;
	
	LocalBroadcastManager lbm;
	IntentFilter filter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		listView = getListView();
		listView.setSelector(R.drawable.text_selector);
		
		Bundle bundle = (savedInstanceState == null) ? getIntent().getExtras() : savedInstanceState;
		if ((primaryChapter == null) && (bundle == null))
		{
			Log.e(TAG,
					"getExtras() returns null, no way to get the chapter identity.");
		}
		else
		{
			String bookEnumString = bundle
					.getString(ChapterActivity.BOOK_ENUM);
			BookEnum bookEnum = Enum.valueOf(BookEnum.class, bookEnumString);
			
			if (BiblesContentProvider.primaryWork == null) {
				ContentResolver cr = this.getContentResolver();
				Cursor c = cr.query(BiblesContentProvider.CONTENT_URI, null, null, null, null);
				if (c!= null)
					c.close();
			}

			Book book = BiblesContentProvider.primaryWork.bookOf(bookEnum);
			int chapterNum = bundle.getInt(CHAPTER_NUMBER, 1);
			primaryChapter = book.chapterOf(chapterNum);
			if(BiblesContentProvider.secondaryWork != null)
				secondaryChapter = BiblesContentProvider.secondaryWork.chapterOf(bookEnum, chapterNum);
		}
		
		View headerView = this.getLayoutInflater().inflate(
				R.layout.book_header, null);
		titleView = (TextView) headerView.findViewById(R.id.bookTitle);
		tableLayout = (EasyTableLayout) headerView
				.findViewById(R.id.indexTable);
		
		listView.addHeaderView(headerView, null, false);
		// headerBar = new NavigatorBar(this);
		// listView.addHeaderView(headerBar, null, false);
		headerViewsCount = listView.getHeaderViewsCount();
		
		// TODO: decide if footerBar should be totally removed later
		footerBar = new NavigatorBar(this);
		listView.addFooterView(footerBar, null, false);
		
		myGestureListener = new EasyGestureListener(false);
		myGestureListener.setOnDetectedAction(this);
		listView.setOnTouchListener(myGestureListener);
		tableLayout.setOnTouchListener(myGestureListener);
		
		adapter = new VersesAdapter(this);
		listView.setAdapter(adapter);
		
		int firstVisible = 1;
		int selectedPosition = -1;
		if (savedInstanceState != null)
		{
			firstVisible = savedInstanceState.getInt(FIRST_VISIBLE, 1);
			selectedPosition = savedInstanceState.getInt(SELECTED_POSITION, -1);
			Log.d(TAG, "SelectedPosition=" + selectedPosition
					+ " thus VerseNumber=" + (selectedPosition + 1));
			adapter.setSelectedPosition(selectedPosition);
			Verse v = adapter.getItem(selectedPosition);
			if (v != null)
				Toast.makeText(this, v.toString(), Toast.LENGTH_LONG).show();
		}
		
		position = (firstVisible == 1) ? 0 : (firstVisible - 1)
				+ headerViewsCount;
		listView.setSelection(position);
		showChapter(primaryChapter.bookEnum, primaryChapter.chapterNum);
		
		// We use this to send broadcasts within our local process.
		lbm = LocalBroadcastManager.getInstance(this);
		
		// We are going to watch for interesting local broadcasts.
		filter = new IntentFilter();
		filter.addAction(ACTION_PLAY_START);
		filter.addAction(ACTION_PLAY_DONE);
		
		mTts = AloudBibleApplication.getTtsWrapperService();
		
		AloudBibleApplication.application.registerTtsActivity();
		this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
	}
	
	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		AloudBibleApplication.application.unregisterTtsActivity();
//		Intent resultIntent = new Intent();
//		Bundle bundle = new Bundle();
//		Chapter currentChapter = mTts.isSpeaking() ? mTts.getSpeakingVerse().getChapter()
//				: primaryChapter;
//		bundle.putString(BiblesContentProvider.KEY_BOOK_ID, currentChapter.bookEnum.toString());
//		bundle.putInt(BiblesContentProvider.KEY_CHAPTER_ID, currentChapter.chapterNum);
//		resultIntent.putExtras(bundle);
//		this.setResult(RESULT_OK, resultIntent);
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
		
		lbm.registerReceiver(mReceiver, filter);
		
		if (mTts != null && mTts.isSpeaking()) {
			Verse speakingVerse = mTts.getSpeakingVerse();
			
			BookEnum bookEnum = BookEnum.fromOrdinal(speakingVerse.getBookNum());
			int chapterNum = speakingVerse.getChapterNum();
			if (primaryChapter.bookEnum != bookEnum || 
					chapterNum != primaryChapter.chapterNum) {
				showChapter(bookEnum, chapterNum);
			}
			
			highlight(speakingVerse);
		}
	}
	
	@Override
	protected void onPause()
	{
		super.onPause();
		
		lbm.unregisterReceiver(mReceiver);
	}
	
	public void showChapter(BookEnum theBookEnum, int chapterNum)
	{
		primaryChapter = BiblesContentProvider.primaryWork.chapterOf(theBookEnum, chapterNum);
		secondaryChapter = BiblesContentProvider.secondaryWork == null ? null : BiblesContentProvider.secondaryWork.chapterOf(theBookEnum, chapterNum);
		String title = secondaryChapter == null ? primaryChapter.getTitle()
				: primaryChapter.toString() + " | " + secondaryChapter.toString();
		
		String hint = BiblesContentProvider.primaryWork.titleOf(theBookEnum) + " " + 
				primaryChapter.getTitle();
		Toast.makeText(this, hint, Toast.LENGTH_SHORT).show();
		ArrayList<Verse> verses = primaryChapter.getVerses();

		adapter.clear();
		if (BiblesContentProvider.secondaryWork != null)
		{
			if (secondaryChapter != null)
			{
				verses.addAll(secondaryChapter.getVerses());
				Collections.sort(verses);
			}
		}
		
		AloudBibleApplication.setText(titleView, title);
		adapter.addAll(verses);
		int count = primaryChapter.verseCount;
		tableLayout.load(count, true);
		
		footerBar.setCurrent(primaryChapter);
		adapter.notifyDataSetChanged();	
	}
	
	
	
	@Override
	public void finish() {
		
		Intent resultIntent = new Intent();
		Bundle bundle = new Bundle();
		Chapter currentChapter = mTts.isSpeaking() ? mTts.getSpeakingVerse().getChapter()
				: primaryChapter;
		bundle.putString(BiblesContentProvider.KEY_BOOK_ID, currentChapter.bookEnum.toString());
		bundle.putInt(BiblesContentProvider.KEY_CHAPTER_ID, currentChapter.chapterNum);
		resultIntent.putExtras(bundle);
		this.setResult(RESULT_OK, resultIntent);
		super.finish();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		int firstVisible = 1;
		View v = listView.getChildAt(0);
		if (v != null)
		{
			Verse verse = (Verse) v.getTag();
			if (verse != null)
			{
				firstVisible = verse.verseNum;
			}
		}
		
		outState.putString(ChapterActivity.BOOK_ENUM,
				primaryChapter.bookEnum.toString());
		outState.putInt(CHAPTER_NUMBER, primaryChapter.chapterNum);
		outState.putInt(FIRST_VISIBLE, firstVisible);
		outState.putInt(SELECTED_POSITION, adapter.getSelectedPosition());
		
		super.onSaveInstanceState(outState);
	}
	
	@Override
	public void onCellSelected(int position)
	{
		Verse verse = primaryChapter.verseOf(position + EasyTableLayout.DEFAULT_POSITION_OFFSET);
		int itemPos = adapter.getPosition(verse) + headerViewsCount;
		listView.setSelection(itemPos);
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
			if (!mTts.isSpeaking())
				showChapter(primaryChapter.bookEnum, primaryChapter.chapterNum);
			else {
				Verse speakingVerse = mTts.getSpeakingVerse();
				
				BookEnum bookEnum = BookEnum.fromOrdinal(speakingVerse.getBookNum());
				int chapterNum = speakingVerse.getChapterNum();
				showChapter(bookEnum, chapterNum);
			}
		}
	}

	public class VersesAdapter extends ArrayAdapter<Verse>
	{
		/**
		 * Lock used to modify the content of {@link #mObjects}. Any write
		 * operation performed on the array should be synchronized on this lock.
		 * This lock is also used by the filter (see {@link #getFilter()} to
		 * make a synchronized copy of the original array of data.
		 */
		private final Object mLock = new Object();
		
		private final ArrayList<Verse> verseList;
		private final Context mContext;
		
		private VersesAdapter(Context context, int resource,
				int textViewResourceId, List<Verse> objects)
		{
			super(context, resource, textViewResourceId, objects);
			mContext = AloudBibleApplication.getAppContext();
			verseList = (ArrayList<Verse>) objects;
		}
		
		public VersesAdapter(Context context)
		{
			this(context, android.R.layout.simple_list_item_1,
					android.R.id.text1, new ArrayList<Verse>());
		}
		
		@Override
		public int getCount()
		{
			return verseList.size();
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
		
		public int setSelectedPosition(Verse verse)
		{
			int index = verseList.indexOf(verse);
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
		public Verse getItem(int position)
		{
			if ((position < 0) || (position >= verseList.size()))
			{
				return null;
			}
			
			return verseList.get(position);
		}
		
		public Verse getSelectedVerse()
		{
			return selectedPosition == AdapterView.INVALID_POSITION ? null
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
			// Log.d(ADAPTER, "selectedListPosition: " + position +
			// ", convert view: " + (convertView != null));
			
			Verse verse = verseList.get(position);
			TextView textView = null;
			if (convertView == null)
			{
				textView = new TextView(mContext);
				textView.setBackgroundResource(R.drawable.text_selector);
			}
			else
			{
				textView = (TextView) convertView;
			}
			
			textView.setTextAppearance(mContext, 
					verse.getChapter()==primaryChapter ? R.style.primaryTextView : R.style.secondaryTextView);
			Spannable spannable = AloudBibleApplication.getSpannableVerse(verse);
			textView.setText(spannable);
			textView.setTag(verse);
			
			if (selectedPosition == position)
			{
				textView.setSelected(true);
				textView.setBackgroundResource(R.color.baby_blue_eyes);
				Log.d(ADAPTER, textView.toString() + "@" + position
						+ " selected"
						+ (textView.isSelected() ? " really" : " not really"));
				// if (verse != mTts.getSpeakingVerse())
				// Log.e(ADAPTER, "index error when verse=" + verse +
				// ", speakingVerse=" + mTts.getSpeakingVerse());
			}
			else
			{
				textView.setSelected(false);
				textView.setBackgroundResource(android.R.color.transparent);
			}
			
			return textView;
		}
		
		/**
		 * Remove all elements from the list.
		 */
		@Override
		public void clear()
		{
			synchronized (mLock)
			{
				if (verseList != null)
				{
					verseList.clear();
					selectedPosition = -1;
				}
			}
			notifyDataSetChanged();
		}
		
		@Override
		public void addAll(Collection<? extends Verse> collection)
		{
			synchronized (mLock)
			{
				if (verseList != null)
				{
					verseList.addAll(collection);
					notifyDataSetChanged();
				}
			}
		}
	}
	
	@Override
	public void onPrevious()
	{
		adapter.clear();
		primaryChapter = primaryChapter.getPrevious();
		showChapter(primaryChapter.bookEnum, primaryChapter.chapterNum);
		listView.setSelectionFromTop(0, 0);
	}
	
	@Override
	public void onNext()
	{
		adapter.clear();
		primaryChapter = primaryChapter.getNext();
		showChapter(primaryChapter.bookEnum, primaryChapter.chapterNum);
		listView.setSelectionFromTop(0, 0);
	}
	
	@Override
	public boolean onDetectedAction(DetectedActionCode code)
	{
		float adjusted = 0;
		switch (code)
		{
			case ZOOM_IN:
				adjusted = AloudBibleApplication.SizeSpan().adjust(
						MyRelativeSizeSpan.AdjustStep);
				Log.i(TAG, "Zoom_In handled, adjustedProportion=" + adjusted);
				titleView.requestLayout();
				tableLayout.requestLayout();
				adapter.notifyDataSetChanged();
				footerBar.invalidate();
				break;
			case ZOOM_OUT:
				adjusted = AloudBibleApplication.SizeSpan().adjust(
						-MyRelativeSizeSpan.AdjustStep);
				Log.i(TAG, "Zoom_Out handled, adjustedProportion=" + adjusted);
				titleView.requestLayout();
				tableLayout.requestLayout();
				adapter.notifyDataSetChanged();
				footerBar.invalidate();
				break;
			case DUAL_RIGHT:
			case SWIPE_RIGHT:
				onPrevious();
				break;
			case DUAL_LEFT:
			case SWIPE_LEFT:
				onNext();
				break;
			case DUAL_UP:
				listView.setSelection(adapter.getCount()-1);
				break;
			case DUAL_DOWN:
				listView.setSelection(0);
				break;
			default:
				break;
		}
		
		return false;
	}
	
	// View selectedView = null;
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id)
	{
		super.onListItemClick(l, v, position, id);
		
		int positionInAdapter = position - headerViewsCount;
		
		int selectedPos = adapter.getSelectedPosition();
		if (selectedPos == positionInAdapter) {
			Log.w(TAG, "Re-Click of " + v.getTag() + "@" + position + " shall stop the TTS");
			adapter.setSelectedPosition(-1);
			
			if (mTts != null)
				mTts.stop();	
			
//			Drawable background = v.getBackground();
//			Log.i(TAG, "current background is " + background);
			
			return;
		}
		else {
			adapter.setSelectedPosition(positionInAdapter);
		}
		
		// Log.d(TAG, "position = " + position);
		Verse theVerse = (Verse) (v.getTag());
		
		// Neglect the click if no verse is selected
		if (theVerse != null && mTts != null)
		{
			Log.i(TAG, "onListItemClick() trigger start of " + theVerse);
			String result = mTts.start(theVerse);
			if (result != TtsService.SUCCESS)
				Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
		}

	}
	
	private void highlight(Verse theVerse) {
		int position = adapter.setSelectedPosition(theVerse);
		
		if (position != 0)
			position += headerViewsCount;
		
		int first = listView.getFirstVisiblePosition();
		int last = listView.getLastVisiblePosition();
		
		//The verse being spoken is out of view, scroll the listview to show it
		if ((position < first) || (position >= last))
		{
			Log.d(TAG, "index=" + position + " is out of (" + first + "-"
					+ last + "), setSelection to " + position);
			
			if (position == headerViewsCount)
				position = 0;
			
			listView.setSelection(position);
		}
	}
	
	BroadcastReceiver mReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			String action = intent.getAction();
			
			Bundle bundle = intent.getExtras();
			String resultString = bundle.getString(TtsService.RESULT_CODE);
			
			String workIdentity = bundle.getString(TtsService.WORK_IDENTIFIER);
			Work work = workIdentity == TtsService.WORK_PRIMARY ? BiblesContentProvider.primaryWork :
				BiblesContentProvider.secondaryWork;
			int bookNum = bundle.getInt(TtsService.BOOK_NUM);
			int chapterNum = bundle.getInt(TtsService.CHAPTER_NUM);
			int verseNum = bundle.getInt(TtsService.VERSE_NUM);
			Verse theVerse = work.verseOf(bookNum, chapterNum, verseNum);
			Log.i(TAG, String.format("%s: %s, %s", action, resultString, theVerse));
			
			// Reload content when current verse is not displayed
			if ((primaryChapter.bookEnum.ordinal() != bookNum)
					|| (primaryChapter.chapterNum != chapterNum))
			{
//				Book book = BiblesContentProvider.primaryWork.bookOf(bookNum);
//				primaryChapter = book.chapterOf(chapterNum);
//				if (BiblesContentProvider.secondaryWork != null)
//					secondaryChapter = BiblesContentProvider.secondaryWork.chapterOf(bookNum, chapterNum);
//				
//				adapter.clear();
				showChapter(BookEnum.fromOrdinal(bookNum), chapterNum);
			}
			
			if (action.equals(ACTION_PLAY_START) && theVerse.id == mTts.getSpeakingVerse().id)
			{
				highlight(theVerse);
			} else if (action.equals(ACTION_PLAY_DONE) && mTts.getSpeakingVerse()==null) {
				adapter.setSelectedPosition(-1);
			}
			
			/*/
			// TTS is speaking
			if (action.equals(ACTION_PLAY_START))
			{
				Log.d(TAG, "ACTION_PLAY_START results in setSelectedPosition of " + theVerse);
				int position = adapter.setSelectedPosition(theVerse) + headerViewsCount;
				
				int first = listView.getFirstVisiblePosition();
				int last = listView.getLastVisiblePosition();
				
				//The verse being spoken is out of view, scroll the listview to show it
				if ((position < first) || (position >= last))
				{
					Log.d(TAG, "index=" + position + " is out of (" + first + "-"
							+ last + "), setSelection to " + position);
					listView.setSelection(position);
				}
				// highlightSpeakingVerse();
			}
			else if (action.equals(ACTION_PLAY_DONE))
			{
				Log.d(TAG, "ACTION_PLAY_DONE of " + theVerse + " results in unselect of listview");
				adapter.setSelectedPosition(-1);
				
//				Verse speakingVerse = adapter.getSelectedVerse();
//				if ((speakingVerse != null) && (speakingVerse.uniqueId == id))
//				{
//					adapter.setSelectedPosition(-1);
//				}
//				else
//				{
//					Log.w(TAG, "speakingVerse=" + speakingVerse + ", id=" + id);
//				}
			}
			//*/
		}

	};
	
//	private void highlightSpeakingVerse()
//	{
//		if (mTts == null)
//		{
//			mTts = AloudBibleApplication.getTtsWrapperService();
//		}
//		
//		Verse speakingVerse = mTts.getSpeakingVerse();
//		if (speakingVerse == null)
//		{
//			return;
//		}
//		
//		int first = listView.getFirstVisiblePosition();
//		int count = listView.getChildCount();
//		Log.i(TAG, "first=" + first + ", count=" + count
//				+ ", speakingVerse.uniqueId=" + speakingVerse.uniqueId);
//		
//		// View highlightedView = null;
//		for (int i = 0; i < count; i++)
//		{
//			View view = listView.getChildAt(i);
//			if (view == null)
//			{
//				Log.w(TAG, "failed to get child view at position of " + i);
//			}
//			else
//			{
//				Verse theVerse = (Verse) view.getTag();
//				if (theVerse == null)
//				{
//					continue;
//				}
//				
//				Log.i(TAG, "theVerse = " + theVerse + ", uniqueId="
//						+ theVerse.uniqueId);
//				if (theVerse.uniqueId == speakingVerse.uniqueId)
//				{
//					view.setSelected(true);
//					view.setBackgroundResource(R.color.baby_blue);
//					Log.d(TAG, view.toString()
//							+ " after highlightSpeakingVerse(), isSelected()="
//							+ view.isSelected());
//					
//					// highlightedView = view;
//				}
//				else
//				// if (theVerse.verseNum != verseNum && view.isSelected())
//				{
//					// view.setSelected(false);
//					view.setBackgroundResource(android.R.color.transparent);
//				}
//			}
//		}
//		
//		// Log.i(TAG, "highlightedView=" + highlightedView + ", isSelected()=" +
//		// highlightedView.isSelected());
//	}
//	
}
