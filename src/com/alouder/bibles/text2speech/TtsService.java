package com.alouder.bibles.text2speech;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.speech.tts.UtteranceProgressListener;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.alouder.bibles.R;
import com.alouder.bibles.activities.AloudBibleApplication;
import com.alouder.bibles.activities.VersesActivity;
import com.alouder.bibles.data.BiblesContentProvider;
import com.alouder.bibles.data.Chapter;
import com.alouder.bibles.data.Verse;
import com.alouder.bibles.data.Work;

public class TtsService extends Service {
	private final static String TAG = TtsService.class.getSimpleName();
	
	public static float DEFAULT_SPEECH_RATE = 1.0f;
	public static float DEFAULT_SPEECH_PITCH = 1.0f;
	
	public static final String WORK_IDENTIFIER = "WORK_IDENTIFIER";
	public static final String WORK_PRIMARY = "WORK_PRIMARY";
	public static final String WORK_SECONDARY = "WORK_SECONDARY";
	public static final String WITH_TITLE = "WITH_TITLE";
	
	public static final String BOOK_NUM = "BOOK_NUM";
	public static final String CHAPTER_NUM = "CHAPTER_NUM";
	public static final String VERSE_NUM = "VERSE_NUM";
	public static final String TEXT_STRING = "TEXT_STRING";
	
	public static final String RESULT_CODE = "RESULT_CODE";
	public static final String RESULT_SUCCESS = "RESULT_SUCCESS";
	public static final String RESULT_ERROR = "RESULT_ERROR";
	
	private static int bookSilence = 800;
	private static int chapterSilence = 500;
	// private static int sectionSilence = 500;
	private static int verseSilence = 200;
	
	private static float titlePitchDelta = -0.1f;
	private static float titleSpeechRateDelta = -0.1f;
	
	public static ArrayList<String> ttsEngineNames = new ArrayList<String>();
	public static ArrayList<String> ttsEngineLables = new ArrayList<String>();
	
	public static void populate()
	{
		List<EngineInfo> engines = getEngines();
		
		for (EngineInfo engineInfo : engines)
		{
			ttsEngineNames.add(engineInfo.name);
			ttsEngineLables.add(engineInfo.label);
		}
	}
	
    public static class EngineInfo {
        /**
         * Engine package name..
         */
        public String name;
        /**
         * Localized label for the engine.
         */
        public String label;
        /**
         * Icon for the engine.
         */
        public int icon;
        /**
         * Whether this engine is a part of the system
         * image.
         *
         * @hide
         */
        public boolean system;
        /**
         * The priority the engine declares for the the intent filter
         * {@code android.intent.action.TTS_SERVICE}
         *
         * @hide
         */
        public int priority;

        @Override
        public String toString() {
            return "EngineInfo{name=" + name + "}";
        }

    }

	/**
	 * Gets a list of all installed TTS engines.
	 * 
	 * @return A list of engine info objects. The list can be empty, but never
	 *         {@code null}.
	 */
	public static List<EngineInfo> getEngines()
	{
		PackageManager pm = AloudBibleApplication.getAppContext().getPackageManager();
		Intent intent = new Intent(TextToSpeechWrapper.INTENT_ACTION_TTS_SERVICE);
		List<ResolveInfo> resolveInfos = pm.queryIntentServices(intent,
				PackageManager.MATCH_DEFAULT_ONLY);
		if (resolveInfos == null)
		{
			return Collections.emptyList();
		}
		
		List<EngineInfo> engines = new ArrayList<EngineInfo>(
				resolveInfos.size());
		
		for (ResolveInfo resolveInfo : resolveInfos)
		{
			EngineInfo engine = getEngineInfo(resolveInfo, pm);
			if (engine != null)
			{
				engines.add(engine);
			}
		}
		// Collections.sort(engines, EngineInfoComparator.INSTANCE);
		
		return engines;
	}
	
	/**
	 * Returns the engine info for a given engine name. Note that engines are
	 * identified by their package name.
	 */
	public static EngineInfo getEngineInfo(String packageName)
	{
		PackageManager pm = AloudBibleApplication.getAppContext().getPackageManager();
		Intent intent = new Intent(TextToSpeechWrapper.INTENT_ACTION_TTS_SERVICE);
		intent.setPackage(packageName);
		List<ResolveInfo> resolveInfos = pm.queryIntentServices(intent,
				PackageManager.MATCH_DEFAULT_ONLY);
		// Note that the current API allows only one engine per
		// package name. Since the "engine name" is the same as
		// the package name.
		if ((resolveInfos != null) && (resolveInfos.size() == 1))
		{
			return getEngineInfo(resolveInfos.get(0), pm);
		}
		
		return null;
	}
	
	private static EngineInfo getEngineInfo(ResolveInfo resolve, PackageManager pm)
	{
		ServiceInfo service = resolve.serviceInfo;
		if (service != null)
		{
			EngineInfo engine = new EngineInfo();
			// Using just the package name isn't great, since it disallows
			// having
			// multiple engines in the same package, but that's what the
			// existing API does.
			engine.name = service.packageName;
			CharSequence label = service.loadLabel(pm);
			engine.label = TextUtils.isEmpty(label) ? engine.name : label
					.toString();
			engine.icon = service.getIconResource();
			// engine.priority = resolve.priority;
			// engine.system = isSystemEngine(service);
			return engine;
		}
		
		return null;
	}
	
	public static TtsStopPattern stopPattern = TtsStopPattern.EndOfBook;

	public enum TtsStopPattern
	{
		Never, // Always keep speaking if no explicit stop signal is received
		EndOfBook, // Keep speaking till the end of the current book
		EndOfChapter, // Keep speaking till the end of the current chapter
//		EndOfSection, // Keep speaking till the end of the current section
		Immediately // Stop once the current item is spoken
	}
	
	public static String noTtsWarning = null;
	public static String SUCCESS = "SUCCESS";
	
	// Binder given to clients
	private final IBinder mBinder = new TtsServiceBinder();
	private LocalBroadcastManager lbm = null;
	private TelephonyManager telephonyManager = null;
	
	private TextToSpeech primaryTts, secondaryTts;
	private Work primaryWork, secondaryWork;
	private ArrayList<Verse> verses = new ArrayList<Verse>();
//	private boolean[] primaryIndicators;
//	private int index = -1;
	private Verse speakingVerse = null;
	private boolean forceStop = false;
	
	public Verse getSpeakingVerse()
	{
		return speakingVerse;
	}
	
	public boolean isEnabled()
	{
		return primaryTts != null || secondaryTts != null;
	}
	
	public boolean isSpeaking()
	{
		return (primaryTts != null && primaryTts.isSpeaking()) 
				|| (secondaryTts != null && secondaryTts.isSpeaking());
	}
	
	public String start(Verse startVerse)
	{
		if (primaryTts == null && secondaryTts == null) {
			indicateDone(startVerse);
			return noTtsWarning;
		}
		else if (speakingVerse != null && speakingVerse.id == startVerse.id) {
			stop();
//			indicateDone(startVerse);
			speakingVerse = null;
			return SUCCESS;
		}
		
//		speakingVerse = startVerse;
		stop();
		
		//Determine if title shall be read
		Work theWork = startVerse.getWork();
		HashMap<String, String> positions = new HashMap<String, String>();
		int bookNum = speakingVerse == null ? -1 : speakingVerse.getBookNum();
		int chapterNum = speakingVerse == null ? -1 : speakingVerse.getChapterNum();
		int verseNum = speakingVerse == null ? -1 : speakingVerse.verseNum;
		// Start speaking or turn to another book
		if (bookNum != startVerse.getBookNum())
		{
			bookNum = startVerse.getBookNum();
			chapterNum = startVerse.getChapterNum();
			
			positions.put(BOOK_NUM, theWork.titleOf(bookNum));
			positions.put(CHAPTER_NUM, theWork.titleOf(startVerse.getChapter()));
			positions.put(VERSE_NUM, theWork.titleOf(startVerse));
		}
		// Start speaking another chapter
		else if (chapterNum != startVerse.getChapterNum())
		{
			chapterNum = startVerse.getChapterNum();
			
			positions.put(CHAPTER_NUM, theWork.titleOf(startVerse.getChapter()));
			positions.put(VERSE_NUM, theWork.titleOf(startVerse));
		}
		// Case when initiating speaking or the verse is the first of a chapter
		else if (startVerse.verseNum == 1)
		{
			positions.put(CHAPTER_NUM, theWork.titleOf(startVerse.getChapter()));
//			positions.put(VERSE_NUM, theWork.titleOf(startVerse));
		}
		else if (Math.abs(startVerse.verseNum-verseNum) > 1) {
			positions.put(VERSE_NUM, theWork.titleOf(startVerse));
		}
		
		forceStop = false;
		
		//Load verses of the same chapter if needed, both from primary and secondary work
		Verse next = loadVerses(startVerse);		
		
		if (positions.size() == 0)
			speak(next);
		else
			speak(next, positions);
		
		return SUCCESS;
	}

	private Verse loadVerses(Verse verse) {
		TextToSpeech tts = ttsOf(verse);
		if (tts != null && verses.contains(verse))
			return verse;
		
		verses.clear();
		
		int bookNum = verse.getBookNum();
		int chapterNum = verse.getChapterNum();
		Chapter theChapter = null;
		if (primaryWork != null && primaryTts != null) {
			theChapter = primaryWork.chapterOf(bookNum, chapterNum);
			verses.addAll(theChapter.getVerses());
		}
		
		if (secondaryWork != null && secondaryTts != null)
		{
			theChapter = secondaryWork.chapterOf(bookNum, chapterNum);
			if (theChapter != null)
			{
				verses.addAll(theChapter.getVerses());
				Collections.sort(verses);
			}
		}
		
		if (verses.contains(verse))
			return verse;
		else if (verses.size() == 0)
			return null;
		else {
			for(Verse v : verses)
			{
				if (v.verseNum >= verse.verseNum)
					return v;
			}
			return null;
		}		
	}
	
	private TextToSpeech ttsOf(Verse verse) {
		if (verse == null)
			return null;
		
		boolean isPrimary = verse.getWork() == primaryWork;
		return isPrimary ? primaryTts : secondaryTts;		
	}
	
	private void speak(Verse verse) {
		if (verse == null) {
			Log.e(TAG, "Null verse shall not be spoken!");
			return;
		}
		
		TextToSpeech tts = ttsOf(verse);
		
		if (tts == null) {
			indicateDone(verse);
			Log.e(TAG, "Unexpected null verse when its TTS is null!");
		} else {		
			indicateStart(verse);

			// Speak when uniqueId refers to a valid verse
			HashMap<String, String> params = new HashMap<String, String>();
			params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteraneIdOf(verse));
			
			tts.speak(verse.getText(), TextToSpeech.QUEUE_ADD, params);
		}
	}

	private void speak(Verse verse, HashMap<String, String> titles) {
		if (verse == null) {
			Log.e(TAG, "Null verse shall not be spoken!");
			return;
		}
		
		boolean isPrimary = verse.getWork() == primaryWork;		
		
		TextToSpeech tts = isPrimary ? primaryTts : secondaryTts;
		float speechRate = AloudBibleApplication.selectedTtsRates[isPrimary ? 0 : 1];
		float pitch = AloudBibleApplication.selectedTtsPitchs[isPrimary ? 0 : 1];
		
		if (tts == null)
		{
			indicateDone(verse);
			Log.e(TAG, "Unexpected null verse when its TTS is null!");
		}
		
		indicateStart(verse);

		// To flush previously unspoken content
		tts.playSilence(10, TextToSpeech.QUEUE_FLUSH, null);		
		tts.setSpeechRate(speechRate + titleSpeechRateDelta);
		tts.setPitch(pitch + titlePitchDelta);
		Work theWork = verse.getWork();
		if (titles.containsKey(BOOK_NUM))
		{
			tts.speak(theWork.titleOf(verse.getBookNum()), TextToSpeech.QUEUE_ADD, null);
			tts.playSilence(bookSilence, TextToSpeech.QUEUE_ADD, null);
		}
		
		if (titles.containsKey(CHAPTER_NUM))
		{
			tts.speak(theWork.titleOf(verse.getChapter()), TextToSpeech.QUEUE_ADD, null);
			tts.playSilence(chapterSilence, TextToSpeech.QUEUE_ADD, null);
		}
		
		if (titles.containsKey(VERSE_NUM))
		{
			tts.speak(theWork.titleOf(verse), TextToSpeech.QUEUE_ADD, null);
			tts.playSilence(verseSilence, TextToSpeech.QUEUE_ADD, null);
		}
		
		tts.setSpeechRate(speechRate);
		tts.setPitch(pitch);
		
		// Speak when uniqueId refers to a valid verse
		HashMap<String, String> params = new HashMap<String, String>();
		params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteraneIdOf(verse));
		
		tts.speak(verse.getText(), TextToSpeech.QUEUE_ADD, params);
	}
	
	private String utteraneIdOf(Verse verse)
	{
		StringBuilder sb = new StringBuilder();
		
		boolean isPrimary = verse.getWork() == primaryWork;
		sb.append(isPrimary ? "0" : "1");
		sb.append("-" + verse.getBookNum());
		sb.append("-" + verse.getChapterNum());
		sb.append("-" + verse.verseNum);
		
		return sb.toString();
	}
	
	private Verse fromUtteranceId(String id)
	{
		String[] subStrings = id.split("-");
		if (subStrings.length != 4)
			return null;
		
		int num = Integer.valueOf(subStrings[0]);
		Work work = num == 0 ? primaryWork : secondaryWork;
		int bookNum = Integer.valueOf(subStrings[1]);
		int chapterNum = Integer.valueOf(subStrings[2]);
		int verseNum = Integer.valueOf(subStrings[3]);
		
		return work.verseOf(bookNum, chapterNum, verseNum);
	}

	private void indicateDone(Verse verse) {
		int i = (verses == null || verses.size() == 0) ? 0 : verses.indexOf(verse);
		String workIdentifier = (verse.getWork()==primaryWork) ? 
				WORK_PRIMARY : WORK_SECONDARY;
		
		// No engine configured to speak, thus broadcast the ACTION_PLAY_DONE events directly.
		Intent callbackIntent = new Intent(VersesActivity.ACTION_PLAY_DONE);
		Bundle bundle = new Bundle();
		bundle.putString(RESULT_CODE, RESULT_ERROR);
		bundle.putString(WORK_IDENTIFIER, workIdentifier);
		bundle.putInt(BOOK_NUM, verse.getBookNum());
		bundle.putInt(CHAPTER_NUM, verse.getChapterNum());
		bundle.putInt(VERSE_NUM, verse.verseNum);
		callbackIntent.putExtras(bundle);
		lbm.sendBroadcast(callbackIntent);
		
		Log.d(TAG, "indicateDone() of " + verse);
		speakingVerse = null;
	}

	private void indicateStart(Verse verse) {
		// No engine configured to speak, thus broadcast the ACTION_PLAY_DONE events directly.
		speakingVerse = verse;
		Intent callbackIntent = new Intent(VersesActivity.ACTION_PLAY_START);
		Bundle bundle = new Bundle();
		bundle.putString(RESULT_CODE, RESULT_SUCCESS);
		bundle.putString(WORK_IDENTIFIER, verse.getWork() == primaryWork ? WORK_PRIMARY : WORK_SECONDARY);
		bundle.putInt(BOOK_NUM, verse.getBookNum());
		bundle.putInt(CHAPTER_NUM, verse.getChapterNum());
		bundle.putInt(VERSE_NUM, verse.verseNum);
		callbackIntent.putExtras(bundle);
		lbm.sendBroadcast(callbackIntent);
		
		Log.d(TAG, "indicateStart() of " + verse);
		
		if (!listenPhoneState)
		{
			listenPhoneState = true;
			telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
		}
	}

	public void stop()
	{
		forceStop = true;
		if (primaryTts != null && primaryTts.isSpeaking())
		{
			primaryTts.stop();
		}

		if (secondaryTts != null && secondaryTts.isSpeaking())
		{
			secondaryTts.stop();
		}
		
		if (listenPhoneState) {
			listenPhoneState = false;
			telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
		}
	}
	
	/**
	 * Class used for the client Binder. Because we know this service always
	 * runs in the same process as its clients, we don't need to deal with IPC.
	 */
	public class TtsServiceBinder extends Binder
	{
		public TtsService getService()
		{
			// Return this instance of LocalService so clients can call public
			// methods
			return TtsService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		
		if (primaryWork == null)
		{
			if (TextToSpeechWrapper.isOldBuild)
				loadEnginesOld();
			else
				loadEngines();
		}
		
		return mBinder;
	}
	
	private void loadEnginesOld() {
		primaryWork = BiblesContentProvider.primaryWork;
		secondaryWork = BiblesContentProvider.secondaryWork;
		
		final Locale primaryLocale = primaryWork.locale;
		if (AloudBibleApplication.selectedTtsNames[0].compareTo("Default") == 0) {
			primaryTts = TextToSpeechWrapper.getInstance(this, new OnInitListener() {
				
				@Override
				public void onInit(int status) {
					if (status == TextToSpeech.SUCCESS)
					{
						int supported = primaryTts.isLanguageAvailable(primaryLocale);
						switch(supported)
						{
							case TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE:
								Locale alternative = new Locale(primaryLocale.getLanguage());
								primaryTts.setLanguage(alternative);
								Log.w(TAG, String.format("%s instead of %s is set to %s", 
										alternative.getDisplayName(), primaryLocale.getDisplayName(),
										primaryTts.toString()));
								break;
							case TextToSpeech.LANG_COUNTRY_AVAILABLE:
							case TextToSpeech.LANG_AVAILABLE:
								primaryTts.setLanguage(primaryLocale);
								String info1 = "Default TTS engine is set to ("
										+ supported + ") " + primaryLocale.toString();
								Log.i(TAG, info1);
								break;
							case TextToSpeech.LANG_MISSING_DATA:
								try {
									primaryTts = null;
									Log.w(TAG, String.format("No data installed of %s for %s",
											primaryTts, primaryLocale.getDisplayName()));
									Intent installIntent = new Intent();
									installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
									installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
									startActivity(installIntent);
								}
								catch (Exception e)
								{
									Log.e(TAG, e.getMessage());
								}
								break;
							case TextToSpeech.LANG_NOT_SUPPORTED:
							default:
//								Locale locale2 = primaryTts.getLanguage();
//								String info2 = primaryTts.toString() + " failed to be set to "
//											+ primaryLocale.toString();
								String localeDesc = primaryLocale.getDisplayName();
								Log.i(TAG, String.format("%s is failed to be set to ", 
										primaryTts, localeDesc));
								primaryTts = null;
								break;
						}
						
						if (primaryTts != null) {
							primaryTts.setSpeechRate(AloudBibleApplication.selectedTtsRates[0]);							
							primaryTts.setPitch(AloudBibleApplication.selectedTtsPitchs[0]);
							
							setListener(primaryTts);
						}
					}
					else
					{
						Log.e(TAG, "Failed initiate TTS engine of default");
					}
				}
			}, "");
		} else if (AloudBibleApplication.selectedTtsNames[0].length() == 0 && primaryTts != null) {
			primaryTts.stop();
			primaryTts.shutdown();
			primaryTts = null;
		}
	}
	
	@SuppressLint("NewApi")
	private void loadEngines() {
		primaryWork = BiblesContentProvider.primaryWork;
		secondaryWork = BiblesContentProvider.secondaryWork;
		
		final Locale primaryLocale = primaryWork.locale;
		final String primaryEngine = AloudBibleApplication.selectedTtsNames[0];
		
		if (ttsEngineNames.contains(primaryEngine))
		{
			primaryTts = TextToSpeechWrapper.getInstance(this, new OnInitListener() {
				
				@Override
				public void onInit(int status) {
					if (status == TextToSpeech.SUCCESS)
					{
						int supported = primaryTts.isLanguageAvailable(primaryLocale);
						switch(supported)
						{
							case TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE:
							case TextToSpeech.LANG_COUNTRY_AVAILABLE:
							case TextToSpeech.LANG_AVAILABLE:
								primaryTts.setLanguage(primaryLocale);
								String info1 = primaryEngine + " is set to ("
										+ supported + ") " + primaryLocale.toString();
								Log.i(TAG, info1);
								break;
							case TextToSpeech.LANG_MISSING_DATA:
								try {
									Intent installIntent = new Intent();
									installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
									installIntent.setPackage(primaryEngine);
									installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
									startActivity(installIntent);
								}
								catch (Exception e)
								{
									Log.e(TAG, e.getMessage());
								}
								break;
							case TextToSpeech.LANG_NOT_SUPPORTED:
							default:
								Locale locale2 = primaryTts.getLanguage();
								String info2;
								
								if (locale2 == null)
								{
									info2 = primaryTts.toString() + " failed to be set to "
											+ primaryLocale.toString();
								}
								else
								{
									info2 = primaryTts + " is still " + locale2.toString();
								}
								Log.i(TAG, info2);
								break;
						}
						primaryTts.setSpeechRate(AloudBibleApplication.selectedTtsRates[0]);							
						primaryTts.setPitch(AloudBibleApplication.selectedTtsPitchs[0]);
						
						setListener(primaryTts);
					}
					else
					{
						Log.e(TAG, "Failed initiate TTS engine of " + primaryEngine);
					}
				}
			}, primaryEngine);				
		} else if (primaryEngine.length() == 0 && primaryTts != null) {
			primaryTts.stop();
			primaryTts.shutdown();
			primaryTts = null;
		}
		
		if (secondaryWork != null)
		{
			final Locale secondaryLocale = secondaryWork.locale;
			final String secondaryEngine = AloudBibleApplication.selectedTtsNames[1];
			
			if (ttsEngineNames.contains(secondaryEngine))
			{
				secondaryTts = TextToSpeechWrapper.getInstance(this, new OnInitListener() {
					
					@Override
					public void onInit(int status) {
						if (status == TextToSpeech.SUCCESS)
						{
							int supported = secondaryTts.isLanguageAvailable(secondaryLocale);
							switch(supported)
							{
								case TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE:
								case TextToSpeech.LANG_COUNTRY_AVAILABLE:
								case TextToSpeech.LANG_AVAILABLE:
									secondaryTts.setLanguage(secondaryLocale);
									String info1 = secondaryEngine + " is set to ("
											+ supported + ") " + secondaryLocale.toString();
									Log.i(TAG, info1);
									break;
								case TextToSpeech.LANG_MISSING_DATA:
									Intent installIntent = new Intent();
									installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
									installIntent.setPackage(secondaryEngine);
									installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
									startActivity(installIntent);
									break;
								case TextToSpeech.LANG_NOT_SUPPORTED:
								default:
									Locale locale2 = secondaryTts.getLanguage();
									String info2;
									
									if (locale2 == null)
									{
										info2 = secondaryTts.toString() + " failed to be set to "
												+ secondaryLocale.toString();
									}
									else
									{
										info2 = secondaryTts + " is still " + locale2.toString();
									}
									Log.i(TAG, info2);
									break;
							}
							secondaryTts.setSpeechRate(AloudBibleApplication.selectedTtsRates[1]);							
							secondaryTts.setPitch(AloudBibleApplication.selectedTtsPitchs[1]);

							setListener(secondaryTts);
						}
						else
						{
							Log.e(TAG, "Failed initiate TTS engine of " + secondaryEngine);
						}
					}
				}, secondaryEngine);	
			} else if (AloudBibleApplication.selectedTtsNames[1].length() == 0 && secondaryTts != null) {
				secondaryTts.stop();
				secondaryTts.shutdown();
				secondaryTts = null;
			}
		}
	}
	
	private Verse nextOf(Verse verse) {
		if (verse == null)
			return null;
		
		if (verses.contains(verse)) {
			int index = verses.indexOf(verse);
			if (index != verses.size()-1) {
				return verses.get(index +1);
			}
		}
		
		Verse next = verse.getNext();
		
		if (next.verseNum == 1) {
			next = primaryWork.verseOf(next.getBookNum(), next.getChapterNum(), next.verseNum);
		}
		
		return next;		
	}
	
	private void onDoneSpeak(String utteranceId){
		Verse theVerse = fromUtteranceId(utteranceId);
		Log.i(TAG, "onDone() of " + theVerse);
		
		int bookNum = theVerse.getBookNum();
		int chapterNum = theVerse.getChapterNum();
		// Broadcast the ACTION_PLAY_DONE events.
		Intent callbackIntent = new Intent(VersesActivity.ACTION_PLAY_DONE);
		Bundle bundle = new Bundle();
		bundle.putString(RESULT_CODE, RESULT_SUCCESS);
		bundle.putString(WORK_IDENTIFIER, primaryWork == theVerse.getWork() ? WORK_PRIMARY : WORK_SECONDARY);
		bundle.putInt(BOOK_NUM, bookNum);
		bundle.putInt(CHAPTER_NUM, chapterNum);
		bundle.putInt(VERSE_NUM, theVerse.verseNum);
		callbackIntent.putExtras(bundle);
		lbm.sendBroadcast(callbackIntent);
		
		if( forceStop) {
			if (listenPhoneState) {
				listenPhoneState = false;
				telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
			}
			
			return;
		} else if (speakingVerse == null || speakingVerse.id == theVerse.id) {
			Verse next = nextOf(theVerse);
			switch (stopPattern)
			{
			case Immediately:
				next = null;
				break;
			case EndOfBook:
				if (next.getBookNum() != bookNum) {
					next = null;
				}
				break;
			case EndOfChapter:
				if (next.getChapterNum() != chapterNum)
					next = null;
				break;
			case Never:
			default:
				break;
			}
			
			if (next != null)
				start(next);
			else if (listenPhoneState) {
				listenPhoneState = false;
				telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
			}
		}
	}
	
	
	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	protected void setListener(TextToSpeech tts) {
		if (!TextToSpeechWrapper.isOldBuild){
			tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
				
				@Override
				public void onStart(String utteranceId) {
				}
				
				@Override
				public void onError(String utteranceId) {
					Log.w(TAG, utteranceId + " onError().");
				}
				
				@Override
				public void onDone(String utteranceId) {
					onDoneSpeak(utteranceId);
				}
			});
		}
		else {
			tts.setOnUtteranceCompletedListener( new OnUtteranceCompletedListener() {
				
				@Override
				public void onUtteranceCompleted(String utteranceId) {
					onDoneSpeak(utteranceId);
				}
			});
		}

	}

	public void resetEngines()
	{
		shutdown();
		if (TextToSpeechWrapper.isOldBuild)
			loadEnginesOld();
		else
			loadEngines();
		
		verses.clear();
	}
	
	@Override
	public void onCreate()
	{
		super.onCreate();
		lbm = LocalBroadcastManager.getInstance(this);
		// audioManager = (AudioManager)
		// this.getSystemService(Context.AUDIO_SERVICE);
		telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		
		if (noTtsWarning == null) {
			noTtsWarning = this.getResources().getString(R.string.no_tts_warning);
		}
	}	

	@Override
	public void onDestroy() {
		super.onDestroy();
		AloudBibleApplication.application.savePreference();
		shutdown();
	}

	private void shutdown() {
		if (primaryTts != null){
			primaryTts.stop();
			primaryTts.shutdown();
			Log.i(TAG, primaryTts.toString() + " has been shutdowned.");
		}
		
		if (secondaryTts != null){
			secondaryTts.stop();
			secondaryTts.shutdown();
			Log.i(TAG, secondaryTts.toString() + " has been shutdowned.");
		}
		
		speakingVerse = null;
		unfinishedVerse = null;
	}

	private boolean listenPhoneState = false;
	private Verse unfinishedVerse = null;
	PhoneStateListener phoneStateListener = new PhoneStateListener()
	{
		@Override
		public void onCallStateChanged(int state, String incomingNumber)
		{
			if ((state == TelephonyManager.CALL_STATE_RINGING) || (state == TelephonyManager.CALL_STATE_OFFHOOK))
			{
				unfinishedVerse = speakingVerse;
				stop();
			}
			else if (state == TelephonyManager.CALL_STATE_IDLE)
			{
				// Not in call: Re-start speaking
//				if (lastVerse != null)
//				{
//					Log.i(TAG, "lastVerse=" + lastVerse
//							+ ", re-start speak()");
//					speak(lastVerse, true);
//				}
				if (unfinishedVerse != null)
				{
					speak(unfinishedVerse);
					unfinishedVerse = null;
				}
			}
			
			super.onCallStateChanged(state, incomingNumber);
		}
	};
	
	/*/
	UtteranceProgressListener mListener = new UtteranceProgressListener()
	{
		@Override
		public void onStart(String utteranceId)
		{
//			Verse theVerse = fromUtteranceId(utteranceId);
//			Log.i(TAG, "onStart() of " + theVerse);
//			//Verify if everything is ok
//			if (index < 0 || index >= verses.size()){
//				Log.e(TAG, "onStart(): index shall not be " + index);
//				return;
//			}
//			else {
//				Verse verse = verses.get(index);
//				if (verse.id != theVerse.id) {
//					Log.w(TAG, "The index refers to " + verse);					
//				}				
//				
//				Intent intent = new Intent(VersesActivity.ACTION_PLAY_START);
//				Bundle bundle = new Bundle();
//				bundle.putString(RESULT_CODE, RESULT_SUCCESS);
//				bundle.putString(WORK_IDENTIFIER, primaryWork == theVerse.getWork() ? WORK_PRIMARY : WORK_SECONDARY);
//				bundle.putInt(BOOK_NUM, theVerse.getBookNum());
//				bundle.putInt(CHAPTER_NUM, theVerse.getChapterNum());
//				bundle.putInt(VERSE_NUM, theVerse.verseNum);
//				intent.putExtras(bundle);
//				lbm.sendBroadcast(intent);
//			}
//			
//			if (!listenPhoneState)
//			{
//				listenPhoneState = true;
//				telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
//			}
		}
		
		@Override
		public void onError(String utteranceId)
		{
			Log.w(TAG, utteranceId + " onError().");
		}
		
		@Override
		public void onDone(String utteranceId)
		{
			Verse theVerse = fromUtteranceId(utteranceId);
			Log.i(TAG, "onDone() of " + theVerse);
			
			// Broadcast the ACTION_PLAY_DONE events.
			Intent callbackIntent = new Intent(VersesActivity.ACTION_PLAY_DONE);
			Bundle bundle = new Bundle();
			bundle.putString(RESULT_CODE, RESULT_SUCCESS);
			bundle.putString(WORK_IDENTIFIER, primaryWork == theVerse.getWork() ? WORK_PRIMARY : WORK_SECONDARY);
			bundle.putInt(BOOK_NUM, theVerse.getBookNum());
			bundle.putInt(CHAPTER_NUM, theVerse.getChapterNum());
			bundle.putInt(VERSE_NUM, theVerse.verseNum);
			callbackIntent.putExtras(bundle);
			lbm.sendBroadcast(callbackIntent);
			
			//Verify if everything is ok
			if (index >= 0 && index < verses.size()){
				Verse verse = verses.get(index);
				if (verse == theVerse) {
					
					index ++;
					
					if (index < verses.size()) {
						speak();
					} else {
						Chapter chapter = primaryWork.chapterOf(bookNum, chapterNum);
						Verse next = chapter.verseOf(chapter.verseCount).getNext();
						if (next == null)
							return;
						
						switch (stopPattern)
						{
						case EndOfBook:
							if (next.getBookNum() != bookNum)							
								return;
						case EndOfChapter:
							if (next.getChapterNum() != chapterNum)
								return;
						case EndOfSection: // TODO:
						case Never:
						default:
							break;
						}
						start(next);
					}
					
				}
			}
		}
	};
	
	OnUtteranceCompletedListener completedListener = new OnUtteranceCompletedListener() {
		
		@Override
		public void onUtteranceCompleted(String utteranceId) {
			mListener.onDone(utteranceId);
		}
	};
	//*/
}
