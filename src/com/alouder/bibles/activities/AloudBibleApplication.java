package com.alouder.bibles.activities;



import java.util.ArrayList;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.UnderlineSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.TextView.BufferType;

import com.alouder.bibles.R;
import com.alouder.bibles.data.BiblesContentProvider;
import com.alouder.bibles.data.Verse;
import com.alouder.bibles.text2speech.TextToSpeechWrapper;
import com.alouder.bibles.text2speech.TtsService;
import com.alouder.bibles.text2speech.TtsService.TtsServiceBinder;
import com.alouder.bibles.widgets.MyRelativeSizeSpan;


public class AloudBibleApplication extends Application
{
	private static final String TAG = AloudBibleApplication.class.getSimpleName();
	
//	public static final String APPLICATION_PREFERENCE = "APPLICATION_PREFERENCE";
	public static final String PREF_SIZESPAN_VALUE = "PREF_SIZESPAN_VALUE";
	public static final String PREF_SPEAK_STOP_PATTERN = "PREF_SPEAK_STOP_PATTERN";
	public static final String PREF_SELECTED_WORK_CODES = "PREF_SELECTED_WORK_CODES";
	public static final String PREF_SELECTED_TTS_NAMES = "PREF_SELECTED_TTS_NAMES";
	public static final String PREF_SELECTED_TTS_RATES = "PREF_SELECTED_TTS_RATES";
	public static final String PREF_SELECTED_TTS_PITCHES = "PREF_SELECTED_TTS_PITCHES";
	public static final String STRING_SPLITTER = "###";
	public static final String DEFAULT_SPEAK_STOP_PATTERN = TtsService.TtsStopPattern.EndOfBook.toString();
	public static final String DEFAULT_STRING_PAIR = STRING_SPLITTER;
	public static final String DEFAULT_FLOAT_PAIR = "1.0" + STRING_SPLITTER + "1.0";
	
	public static SharedPreferences applicationPreferences;
	
	public static AloudBibleApplication application;
	
	public static String[] selectedWorkCodes;
	public static String[] selectedTtsNames;
	public static Float[] selectedTtsRates;
	public static Float[] selectedTtsPitchs;
	
	private static Context context;
	
	private static final float defaultSizeSpan = 1.2f;
	
	public static MyRelativeSizeSpan sizeSpan = null;
	private static SuperscriptSpan superscriptSpan = new SuperscriptSpan();
	
	private static UnderlineSpan underlineSpan = new UnderlineSpan();
	
	//	private static StyleSpan topicStyleSpan = new StyleSpan(Typeface.BOLD_ITALIC);
	private static RelativeSizeSpan indexRelativeSizeSpan = new RelativeSizeSpan(0.7f);
	
	private static DisplayMetrics displayMetrics = null;
	
	//	// private static TextToSpeech ttsEngine1;
//	private static TtsWrapperService mTts;
//	
//	public static TtsWrapperService getTtsWrapperService()
//	{
//		return mTts;
//	}
	// private static TextToSpeech ttsEngine1;
	private static TtsService mTts;
	
	public static float adjustSizeSpan(float delta)
	{
		return sizeSpan.adjust(delta);
	}
	public static Context getAppContext()
	{
		return context;
	}
	
	public static boolean isSelectedWork(String code) {
		return code.equals(selectedWorkCodes[0]) || code.equals(selectedWorkCodes[1]);
	}
	
	public static final int MENU_CHOOSE = Menu.FIRST + 1;
	public static final int MENU_MANAGE = Menu.FIRST + 2;
	public static final int MENU_DOWNLOAD = Menu.FIRST + 3;
	public static final int MENU_HELP = Menu.FIRST + 4;
	
	public static final int SHOW_PREFERENCE = 100;
//	public static final int PROMPT_IMPORT = 101;
//	public static final int PROMPT_DOWNLOAD = 102;
	

	public static boolean onCreateOptionsMenu(Menu menu, Activity callingActivity) {
		MenuInflater inflater = callingActivity.getMenuInflater();
		inflater.inflate(R.menu.option_menu, menu);
//		menu.add(0, MENU_CHOOSE, Menu.NONE, R.string.preference);
//		menu.add(0, MENU_MANAGE, Menu.NONE, R.string.manage);
//		menu.add(0, MENU_DOWNLOAD, Menu.NONE, R.string.download);
//		menu.add(0, MENU_HELP, Menu.NONE, R.string.help);
		return true;
 	}

	public static boolean onOptionItemsSelected(MenuItem item, Activity callingActivity)  {
		Intent i;
		switch(item.getItemId())
		{
			case R.id.preference: // MENU_CHOOSE:
				if (TextToSpeechWrapper.isOldBuild)
					i = new Intent(callingActivity, PreferenceOldActivity.class);
				else
					i = new Intent(callingActivity, PreferenceActivity.class);
				
				callingActivity.startActivityForResult(i, SHOW_PREFERENCE);
				return true;
			case R.id.manage: // MENU_MANAGE:
				i = new Intent(callingActivity, ManageWorksActivity.class);
				callingActivity.startActivity(i);
				return true;
			case R.id.download: // MENU_DOWNLOAD:
				i = new Intent(callingActivity, DownloadActivity.class);
				callingActivity.startActivity(i);
				return true;
			case R.id.help: // MENU_HELP:
				i = new Intent(callingActivity, HelpActivity.class);
				callingActivity.startActivity(i);
				return true;
		}
		return false;		 
	}
	
//	public static void onActivityResult(int requestCode, int resultCode, Intent data, Activity callingActivity) {
//		if (requestCode == PROMPT_IMPORT)
//		{
//			if (resultCode == Activity.RESULT_OK)
//			{
//				BiblesContentProvider.preloadWorks();
//			}
//		}
//	}
	
	public static float getDisplayDensity()
	{
		return displayMetrics.density;
	}
	
	public static int getDisplayHeight()
	{
		return displayMetrics.heightPixels;
	}
	
	public static int getDisplayWidth()
	{
		return displayMetrics.widthPixels;
	}
	
	public static float getSizeProportion()
	{
		return sizeSpan.getSizeChange();
	}
	
	public static Spannable getSpannableVerse(Verse verse)
	{
		SpannableStringBuilder sb = new SpannableStringBuilder();
		String index = String.format("[%d]", verse.getOrdinal());
		sb.append(index);
		int length = index.length();
		sb.setSpan(superscriptSpan, 0, length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
		sb.setSpan(underlineSpan, 0, length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
		sb.setSpan(indexRelativeSizeSpan, 0, length,
				Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
		sb.append(" " + verse.getText());
		sb.setSpan(sizeSpan, 0, sb.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
		return sb;
	}
	
	public static TtsService getTtsWrapperService()
	{
		return mTts;
	}
	
	public static void setText(TextView view, CharSequence text)
	{
		Spannable spannable = (text instanceof Spannable) ? (Spannable) text
				: new SpannableString(text);
		
		if (spannable.getSpanStart(sizeSpan) < 0)
		{
			spannable.setSpan(sizeSpan, 0, spannable.length(),
					Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
		}
		
		view.setText(spannable, BufferType.SPANNABLE);
	}
	
	public static MyRelativeSizeSpan SizeSpan()
	{
		return sizeSpan;
	}
	
	/** Defines callback for service binding, passed to bindService() */
	private ServiceConnection mConnection = new ServiceConnection()
	{
		
		@Override
		public void onServiceConnected(ComponentName className, IBinder service)
		{
			TtsServiceBinder binder = (TtsServiceBinder) service;
			mTts = binder.getService();
		}
		
		@Override
		public void onServiceDisconnected(ComponentName arg0)
		{
			mTts = null;
		}
	};
	
	private int activityCount = 0;
	
	private Float[] floatArrayOf(String stringSet, Float defaultValue)
	{
		if (stringSet == null || stringSet.length() == 0)
			return new Float[]{defaultValue, defaultValue};
		
		ArrayList<Float> floats = new ArrayList<Float>();
		int start=0, end, len = STRING_SPLITTER.length();
		
		do
		{
			end = stringSet.indexOf(STRING_SPLITTER, start);
			if (end != -1)
			{
				String subStr = stringSet.substring(start, end);
				floats.add(Float.valueOf(subStr));
				start = end + len;
			}
			else
			{
				floats.add(Float.valueOf(stringSet.substring(start, stringSet.length())));
			}
		}while(end != -1);
		
		return floats.toArray(new Float[floats.size()]);
	}
	
	public void getPreferences()
	{
		Float spanSizeValue = applicationPreferences.getFloat(PREF_SIZESPAN_VALUE, defaultSizeSpan);
		sizeSpan = new MyRelativeSizeSpan(spanSizeValue);
		
		String stringSet = applicationPreferences.getString(PREF_SELECTED_WORK_CODES, 
				DEFAULT_STRING_PAIR);		
		selectedWorkCodes = stringArrayOf(stringSet);
		
		stringSet = applicationPreferences.getString(PREF_SELECTED_TTS_NAMES, DEFAULT_STRING_PAIR);
		selectedTtsNames = stringArrayOf(stringSet);
		
		stringSet = applicationPreferences.getString(PREF_SELECTED_TTS_RATES, DEFAULT_FLOAT_PAIR);
		selectedTtsRates = floatArrayOf(stringSet, TtsService.DEFAULT_SPEECH_RATE);
		
		stringSet = applicationPreferences.getString(PREF_SELECTED_TTS_PITCHES, DEFAULT_FLOAT_PAIR);
		selectedTtsPitchs = floatArrayOf(stringSet, TtsService.DEFAULT_SPEECH_PITCH);
		
		stringSet = applicationPreferences.getString(PREF_SPEAK_STOP_PATTERN, DEFAULT_SPEAK_STOP_PATTERN);
		TtsService.stopPattern = TtsService.TtsStopPattern.valueOf(stringSet);
	}
	
	@Override
	public void onCreate()
	{
		if (application == null)
		{
			application = this;
			context = application.getApplicationContext();
			displayMetrics = context.getResources().getDisplayMetrics();
			Log.i(TAG, displayMetrics.toString());			
		}
		super.onCreate();
		CrashHandler.getInstance().init(this); 
		
		TtsService.populate();
		applicationPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		
		getPreferences();
		
		BiblesContentProvider.loadWorks(selectedWorkCodes);
	}

	public void registerTtsActivity()
	{
		activityCount++;

		if (mTts == null)
		{
			Intent intent = new Intent(context, TtsService.class);
			bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
		}
	}

	public void savePreference()
	{
		SharedPreferences.Editor editor = applicationPreferences.edit();
		editor.putString(AloudBibleApplication.PREF_SELECTED_WORK_CODES,
				stringOfArray(AloudBibleApplication.selectedWorkCodes));
		editor.putString(AloudBibleApplication.PREF_SELECTED_TTS_NAMES,
				stringOfArray(AloudBibleApplication.selectedTtsNames));
		editor.putString(AloudBibleApplication.PREF_SELECTED_TTS_RATES,
				stringOfArray(AloudBibleApplication.selectedTtsRates));
		editor.putString(AloudBibleApplication.PREF_SELECTED_TTS_PITCHES,
				stringOfArray(AloudBibleApplication.selectedTtsPitchs));
		editor.putString(PREF_SPEAK_STOP_PATTERN, TtsService.stopPattern.toString());
		editor.putFloat(PREF_SIZESPAN_VALUE, getSizeProportion());

		editor.commit();
	}
	
	private String[] stringArrayOf(String stringSet)
	{
		if (stringSet == null || stringSet.length() == 0)
			return null;
		
		ArrayList<String> strings = new ArrayList<String>();
		int start=0, end, len = STRING_SPLITTER.length();
		
		do
		{
			end = stringSet.indexOf(STRING_SPLITTER, start);
			if (end != -1)
			{
				strings.add(stringSet.substring(start, end));
				start = end + len;
			}
			else
			{
				strings.add(stringSet.substring(start, stringSet.length()));
			}
		}while(end != -1);
		
		return strings.toArray(new String[strings.size()]);
	}
	
	private String stringOfArray(Float[] floatArray) {
		StringBuilder sb = new StringBuilder();
		for (Float f : floatArray) {
			sb.append(f.toString());
			sb.append(AloudBibleApplication.STRING_SPLITTER);
		}
		return sb.substring(0, sb.length()
				- AloudBibleApplication.STRING_SPLITTER.length());
	}
	
	private String stringOfArray(String[] stringArray) {
		StringBuilder sb = new StringBuilder();
		for (String string : stringArray) {
			sb.append(string);
			sb.append(AloudBibleApplication.STRING_SPLITTER);
		}
		return sb.substring(0, sb.length()
				- AloudBibleApplication.STRING_SPLITTER.length());
	}
	
	public void unregisterTtsActivity()
	{
		activityCount--;

		if ((activityCount <= 0) && (mTts != null))
		{
			context.unbindService(mConnection);
			Log.w(TAG, "no visible activities any longer, unbindService()");
			mTts = null;
		}
	}
}
