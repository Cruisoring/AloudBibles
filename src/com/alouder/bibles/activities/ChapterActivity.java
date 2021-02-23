package com.alouder.bibles.activities;

import com.alouder.bibles.R;
import com.alouder.bibles.data.BiblesContentProvider;
import com.alouder.bibles.data.Book;
import com.alouder.bibles.data.BookEnum;
import com.alouder.bibles.data.Chapter;
import com.alouder.bibles.text2speech.TtsService;
import com.alouder.bibles.widgets.EasyGestureListener;
import com.alouder.bibles.widgets.EasyGestureListener.DetectedActionCode;
import com.alouder.bibles.widgets.EasyTableLayout;
import com.alouder.bibles.widgets.MyRelativeSizeSpan;
import com.alouder.bibles.widgets.NavigatorBar;

import android.R.anim;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

public class ChapterActivity extends Activity implements
		EasyTableLayout.onCellSelectedListener,
		NavigatorBar.onNavigationSelectedListener,
		EasyGestureListener.OnDetectedActionListener
{
	public static final String TAG = ChapterActivity.class.getSimpleName();
	public static final String BOOK_ENUM = BookEnum.class.getSimpleName();
	public static final String SELECTED = "SELECTED";
	public static final String TOP_INDEX = "TOP_INDEX";
	public static final String TOP_OFFSET = "TOP_OFFSET";
	
	public static final int SHOW_CHAPTER = 80001;
	
	BookEnum bookEnum = BookEnum.Genesis;
	int chapterNum = -1;
	
	TextView titleView;
	EasyTableLayout tableLayout;
	NavigatorBar navigatorBar;
	
	float adjusted = AloudBibleApplication.getSizeProportion();
	int topIndex = -1;
	int topOffset = 0;
	float remained = 1f;
	
	EasyGestureListener myGestureListener = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.index);
		titleView = (TextView) findViewById(R.id.bookTitle);
		tableLayout = (EasyTableLayout) findViewById(R.id.indexTable);
		navigatorBar = (NavigatorBar) findViewById(R.id.navigator);
		
		myGestureListener = new EasyGestureListener(false);
		myGestureListener.setOnDetectedAction(this);
		// View root = findViewById(android.R.id.content);
		View root = findViewById(R.id.root);
		root.setOnTouchListener(myGestureListener);
		
		Bundle bundle = (savedInstanceState == null) ? getIntent().getExtras() : savedInstanceState;

		if (bundle == null) {
			Log.e(TAG,
					"getExtras() returns null, no way to get the chapter identity.");
		} else {
			String bookEnumString = bundle.getString(BOOK_ENUM);
			bookEnum = Enum.valueOf(BookEnum.class, bookEnumString);
			chapterNum = bundle.getInt(SELECTED, -1);	
		}
			
		showBook();
		
		AloudBibleApplication.application.registerTtsActivity();
		this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
	}
	
	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		AloudBibleApplication.application.unregisterTtsActivity();
	}
	
	private void showBook()
	{
		if (BiblesContentProvider.primaryWork == null) {
			ContentResolver cr = this.getContentResolver();
			Cursor c = cr.query(BiblesContentProvider.CONTENT_URI, null, null, null, null);
			if (c!= null)
				c.close();
		}

		Book book = BiblesContentProvider.primaryWork.bookOf(bookEnum);
		String title = book.title;
		if (BiblesContentProvider.secondaryWork != null)
		{
			title += "|" + BiblesContentProvider.secondaryWork.bookNameOf(bookEnum);
		}
		AloudBibleApplication.setText(titleView, title);
		int count = book.chapterCount;
		tableLayout.load(count, true);
		navigatorBar.setCurrent(book);
		
		if (chapterNum >= 0)
		{
			tableLayout.setSelectedPosition(chapterNum - 1);
		}
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		outState.putString(BOOK_ENUM, bookEnum.toString());
		outState.putInt(SELECTED, chapterNum);
		super.onSaveInstanceState(outState);
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
		float currentProportion = AloudBibleApplication.getSizeProportion();
		if (adjusted != currentProportion)
		{
			refresh(0);
		}
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
			showBook();
		} else if (requestCode == SHOW_CHAPTER) {
			Bundle bundle = data.getExtras();
			if (bundle != null) {
				String bookString = bundle.getString(BiblesContentProvider.KEY_BOOK_ID);
				if (bookString != null) {
					bookEnum = BookEnum.valueOf(bookString);
					chapterNum = bundle.getInt(BiblesContentProvider.KEY_CHAPTER_ID);		
				} else {
					bookEnum = BookEnum.Genesis;
					chapterNum = 1;
				}
			}
			showBook();
		}
	}
	
	@Override
	public void finish() {
		
		
		Intent resultIntent = new Intent();
		Bundle bundle = new Bundle();
		
		TtsService tts = AloudBibleApplication.getTtsWrapperService();
		if (tts != null && tts.isSpeaking()) {
			
			Chapter currentChapter = tts.getSpeakingVerse().getChapter();
			bundle.putString(BiblesContentProvider.KEY_BOOK_ID, currentChapter.bookEnum.toString());
		} else {
			bundle.putString(BiblesContentProvider.KEY_BOOK_ID, bookEnum.toString());
		}
		resultIntent.putExtras(bundle);
		this.setResult(RESULT_OK, resultIntent);

		super.finish();
	}

	@Override
	public void onCellSelected(int position)
	{
		chapterNum = position + EasyTableLayout.DEFAULT_POSITION_OFFSET;
		Intent intent = new Intent(ChapterActivity.this, VersesActivity.class);
		
		Bundle bundle = new Bundle();
		bundle.putString(ChapterActivity.BOOK_ENUM, bookEnum.toString());
		bundle.putInt(VersesActivity.CHAPTER_NUMBER, chapterNum);
		intent.putExtras(bundle);
		
//		ChapterActivity.this.startActivity(intent);
		this.startActivityForResult(intent, SHOW_CHAPTER);
		this.overridePendingTransition(anim.slide_in_left, anim.slide_out_right);
	}
	
	@Override
	public void onPrevious()
	{
		Book book = BiblesContentProvider.primaryWork.bookOf(bookEnum);
		if (!book.hasPrevious())
			return;

		chapterNum = -1;
		bookEnum = book.getPrevious().bookEnum;
		showBook();
	}
	
	@Override
	public void onNext()
	{
		Book book = BiblesContentProvider.primaryWork.bookOf(bookEnum);
		if (!book.hasNext())
			return;

		chapterNum = -1;
		bookEnum = book.getNext().bookEnum;
		showBook();
	}
	
	@Override
	public boolean onDetectedAction(DetectedActionCode code)
	{
		switch (code)
		{
			case DUAL_RIGHT:
			case SWIPE_RIGHT:
				onPrevious();
				break;
			case DUAL_LEFT:
			case SWIPE_LEFT:
				onNext();
				break;
			case ZOOM_IN:
				refresh(MyRelativeSizeSpan.AdjustStep);
				Log.i(TAG, "Zoom_In handled, adjustedProportion=" + adjusted);
				return true;
			case ZOOM_OUT:
				refresh(-MyRelativeSizeSpan.AdjustStep);
				Log.i(TAG, "Zoom_Out handled, adjustedProportion=" + adjusted);
				return true;
			default:
				break;
		}
		
		return false;
	}
	
	private void refresh(float delta)
	{
		adjusted = AloudBibleApplication.adjustSizeSpan(delta);
		titleView.requestLayout();
		navigatorBar.invalidate();
	}
	
	/*
	 * / public boolean onDetectedAction(DetectedActionCode code) { float
	 * adjusted = 0; switch (code) { case ZOOM_IN: adjusted =
	 * AloudBibleApplication.SizeSpan().adjust(MyRelativeSizeSpan.AdjustStep);
	 * Log.i(TAG, "Zoom_In handled, adjustedProportion=" + adjusted);
	 * titleView.requestLayout(); navigatorBar.invalidate(); break; case
	 * ZOOM_OUT: adjusted =
	 * AloudBibleApplication.SizeSpan().adjust(-MyRelativeSizeSpan.AdjustStep);
	 * Log.i(TAG, "Zoom_Out handled, adjustedProportion=" + adjusted);
	 * titleView.requestLayout(); navigatorBar.invalidate(); break; case
	 * SWIPE_RIGHT: onPrevious(); break; case SWIPE_LEFT: onNext(); break;
	 * default: break; }
	 * 
	 * return false; } //
	 */
	
}
