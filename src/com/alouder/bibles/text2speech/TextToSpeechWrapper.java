package com.alouder.bibles.text2speech;

import java.util.HashMap;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.media.AudioManager;
import android.os.Build;
import android.speech.tts.TextToSpeech;

public abstract class TextToSpeechWrapper extends TextToSpeech {
	
	private static final String TAG = "TextToSpeechWrapper";
	public static final boolean isOldBuild = Build.VERSION.SDK_INT < 15;
	
	@SuppressLint("NewApi")
	public static TextToSpeech getInstance(Context context, OnInitListener listener, String engine)
	{
		return isOldBuild ? new TextToSpeech(context, listener) 
			: new TextToSpeech(context, listener, engine);
	}

    /**
     * Denotes a successful operation.
     */
    public static final int SUCCESS = 0;
    /**
     * Denotes a generic operation failure.
     */
    public static final int ERROR = -1;

    /**
     * Queue mode where all entries in the playback queue (media to be played
     * and text to be synthesized) are dropped and replaced by the new entry.
     * Queues are flushed with respect to a given calling app. Entries in the queue
     * from other callees are not discarded.
     */
    public static final int QUEUE_FLUSH = 0;
    /**
     * Queue mode where the new entry is added at the end of the playback queue.
     */
    public static final int QUEUE_ADD = 1;
    /**
     * Queue mode where the entire playback queue is purged. This is different
     * from {@link #QUEUE_FLUSH} in that all entries are purged, not just entries
     * from a given caller.
     *
     * @hide
     */
    static final int QUEUE_DESTROY = 2;

    /**
     * Denotes the language is available exactly as specified by the locale.
     */
    public static final int LANG_COUNTRY_VAR_AVAILABLE = 2;
    /**
     * Denotes the language is available for the language and country specified
     * by the locale, but not the variant.
     */
    public static final int LANG_COUNTRY_AVAILABLE = 1;
    /**
     * Denotes the language is available for the language by the locale,
     * but not the country and variant.
     */
    public static final int LANG_AVAILABLE = 0;

    /**
     * Denotes the language data is missing.
     */
    public static final int LANG_MISSING_DATA = -1;

    /**
     * Denotes the language is not supported.
     */
    public static final int LANG_NOT_SUPPORTED = -2;

    /**
     * Broadcast Action: The TextToSpeech synthesizer has completed processing
     * of all the text in the speech queue.
     *
     * Note that this notifies callers when the <b>engine</b> has finished has
     * processing text data. Audio playback might not have completed (or even started)
     * at this point. If you wish to be notified when this happens, see
     * {@link OnUtteranceCompletedListener}.
     */
    public static final String ACTION_TTS_QUEUE_PROCESSING_COMPLETED =
            "android.speech.tts.TTS_QUEUE_PROCESSING_COMPLETED";

    /**
     * Default audio stream used when playing synthesized speech.
     */
    public static final int DEFAULT_STREAM = AudioManager.STREAM_MUSIC;

    /**
     * Indicates success when checking the installation status of the resources used by the
     * TextToSpeech engine with the {@link #ACTION_CHECK_TTS_DATA} intent.
     */
    public static final int CHECK_VOICE_DATA_PASS = 1;

    /**
     * Indicates failure when checking the installation status of the resources used by the
     * TextToSpeech engine with the {@link #ACTION_CHECK_TTS_DATA} intent.
     */
    public static final int CHECK_VOICE_DATA_FAIL = 0;

    /**
     * Indicates erroneous data when checking the installation status of the resources used by
     * the TextToSpeech engine with the {@link #ACTION_CHECK_TTS_DATA} intent.
     */
    public static final int CHECK_VOICE_DATA_BAD_DATA = -1;

    /**
     * Indicates missing resources when checking the installation status of the resources used
     * by the TextToSpeech engine with the {@link #ACTION_CHECK_TTS_DATA} intent.
     */
    public static final int CHECK_VOICE_DATA_MISSING_DATA = -2;

    /**
     * Indicates missing storage volume when checking the installation status of the resources
     * used by the TextToSpeech engine with the {@link #ACTION_CHECK_TTS_DATA} intent.
     */
    public static final int CHECK_VOICE_DATA_MISSING_VOLUME = -3;

    /**
     * Intent for starting a TTS service. Services that handle this intent must
     * extend {@link TextToSpeechService}. Normal applications should not use this intent
     * directly, instead they should talk to the TTS service using the the methods in this
     * class.
     */
    public static final String INTENT_ACTION_TTS_SERVICE =
            "android.intent.action.TTS_SERVICE";

    /**
     * Name under which a text to speech engine publishes information about itself.
     * This meta-data should reference an XML resource containing a
     * <code>&lt;{@link android.R.styleable#TextToSpeechEngine tts-engine}&gt;</code>
     * tag.
     */
    public static final String SERVICE_META_DATA = "android.speech.tts";

    // intents to ask engine to install data or check its data
    /**
     * Activity Action: Triggers the platform TextToSpeech engine to
     * start the activity that installs the resource files on the device
     * that are required for TTS to be operational. Since the installation
     * of the data can be interrupted or declined by the user, the application
     * shouldn't expect successful installation upon return from that intent,
     * and if need be, should check installation status with
     * {@link #ACTION_CHECK_TTS_DATA}.
     */
    public static final String ACTION_INSTALL_TTS_DATA =
            "android.speech.tts.engine.INSTALL_TTS_DATA";

    /**
     * Broadcast Action: broadcast to signal the completion of the installation of
     * the data files used by the synthesis engine. Success or failure is indicated in the
     * {@link #EXTRA_TTS_DATA_INSTALLED} extra.
     */
    public static final String ACTION_TTS_DATA_INSTALLED =
            "android.speech.tts.engine.TTS_DATA_INSTALLED";

    /**
     * Activity Action: Starts the activity from the platform TextToSpeech
     * engine to verify the proper installation and availability of the
     * resource files on the system. Upon completion, the activity will
     * return one of the following codes:
     * {@link #CHECK_VOICE_DATA_PASS},
     * {@link #CHECK_VOICE_DATA_FAIL},
     * {@link #CHECK_VOICE_DATA_BAD_DATA},
     * {@link #CHECK_VOICE_DATA_MISSING_DATA}, or
     * {@link #CHECK_VOICE_DATA_MISSING_VOLUME}.
     * <p> Moreover, the data received in the activity result will contain the following
     * fields:
     * <ul>
     *   <li>{@link #EXTRA_VOICE_DATA_ROOT_DIRECTORY} which
     *       indicates the path to the location of the resource files,</li>
     *   <li>{@link #EXTRA_VOICE_DATA_FILES} which contains
     *       the list of all the resource files,</li>
     *   <li>and {@link #EXTRA_VOICE_DATA_FILES_INFO} which
     *       contains, for each resource file, the description of the language covered by
     *       the file in the xxx-YYY format, where xxx is the 3-letter ISO language code,
     *       and YYY is the 3-letter ISO country code.</li>
     * </ul>
     */
    public static final String ACTION_CHECK_TTS_DATA =
            "android.speech.tts.engine.CHECK_TTS_DATA";

    /**
     * Activity intent for getting some sample text to use for demonstrating TTS.
     *
     * @hide This intent was used by engines written against the old API.
     * Not sure if it should be exposed.
     */
    public static final String ACTION_GET_SAMPLE_TEXT =
            "android.speech.tts.engine.GET_SAMPLE_TEXT";

    // extras for a TTS engine's check data activity
    /**
     * Extra information received with the {@link #ACTION_CHECK_TTS_DATA} intent where
     * the TextToSpeech engine specifies the path to its resources.
     */
    public static final String EXTRA_VOICE_DATA_ROOT_DIRECTORY = "dataRoot";

    /**
     * Extra information received with the {@link #ACTION_CHECK_TTS_DATA} intent where
     * the TextToSpeech engine specifies the file names of its resources under the
     * resource path.
     */
    public static final String EXTRA_VOICE_DATA_FILES = "dataFiles";

    /**
     * Extra information received with the {@link #ACTION_CHECK_TTS_DATA} intent where
     * the TextToSpeech engine specifies the locale associated with each resource file.
     */
    public static final String EXTRA_VOICE_DATA_FILES_INFO = "dataFilesInfo";

    /**
     * Extra information received with the {@link #ACTION_CHECK_TTS_DATA} intent where
     * the TextToSpeech engine returns an ArrayList<String> of all the available voices.
     * The format of each voice is: lang-COUNTRY-variant where COUNTRY and variant are
     * optional (ie, "eng" or "eng-USA" or "eng-USA-FEMALE").
     */
    public static final String EXTRA_AVAILABLE_VOICES = "availableVoices";

    /**
     * Extra information received with the {@link #ACTION_CHECK_TTS_DATA} intent where
     * the TextToSpeech engine returns an ArrayList<String> of all the unavailable voices.
     * The format of each voice is: lang-COUNTRY-variant where COUNTRY and variant are
     * optional (ie, "eng" or "eng-USA" or "eng-USA-FEMALE").
     */
    public static final String EXTRA_UNAVAILABLE_VOICES = "unavailableVoices";

    /**
     * Extra information sent with the {@link #ACTION_CHECK_TTS_DATA} intent where the
     * caller indicates to the TextToSpeech engine which specific sets of voice data to
     * check for by sending an ArrayList<String> of the voices that are of interest.
     * The format of each voice is: lang-COUNTRY-variant where COUNTRY and variant are
     * optional (ie, "eng" or "eng-USA" or "eng-USA-FEMALE").
     */
    public static final String EXTRA_CHECK_VOICE_DATA_FOR = "checkVoiceDataFor";

    // extras for a TTS engine's data installation
    /**
     * Extra information received with the {@link #ACTION_TTS_DATA_INSTALLED} intent.
     * It indicates whether the data files for the synthesis engine were successfully
     * installed. The installation was initiated with the  {@link #ACTION_INSTALL_TTS_DATA}
     * intent. The possible values for this extra are
     * {@link TextToSpeech#SUCCESS} and {@link TextToSpeech#ERROR}.
     */
    public static final String EXTRA_TTS_DATA_INSTALLED = "dataInstalled";

    // keys for the parameters passed with speak commands. Hidden keys are used internally
    // to maintain engine state for each TextToSpeech instance.
    public static final String KEY_PARAM_RATE = "rate";
    public static final String KEY_PARAM_LANGUAGE = "language";
    public static final String KEY_PARAM_COUNTRY = "country";
    public static final String KEY_PARAM_VARIANT = "variant";
    public static final String KEY_PARAM_ENGINE = "engine";
    public static final String KEY_PARAM_PITCH = "pitch";

    /**
     * Parameter key to specify the audio stream type to be used when speaking text
     * or playing back a file. The value should be one of the STREAM_ constants
     * defined in {@link AudioManager}.
     *
     * @see TextToSpeech#speak(String, int, HashMap)
     * @see TextToSpeech#playEarcon(String, int, HashMap)
     */
    public static final String KEY_PARAM_STREAM = "streamType";

    /**
     * Parameter key to identify an utterance in the
     * {@link TextToSpeech.OnUtteranceCompletedListener} after text has been
     * spoken, a file has been played back or a silence duration has elapsed.
     *
     * @see TextToSpeech#speak(String, int, HashMap)
     * @see TextToSpeech#playEarcon(String, int, HashMap)
     * @see TextToSpeech#synthesizeToFile(String, HashMap, String)
     */
    public static final String KEY_PARAM_UTTERANCE_ID = "utteranceId";

    /**
     * Parameter key to specify the speech volume relative to the current stream type
     * volume used when speaking text. Volume is specified as a float ranging from 0 to 1
     * where 0 is silence, and 1 is the maximum volume (the default behavior).
     *
     * @see TextToSpeech#speak(String, int, HashMap)
     * @see TextToSpeech#playEarcon(String, int, HashMap)
     */
    public static final String KEY_PARAM_VOLUME = "volume";

    /**
     * Parameter key to specify how the speech is panned from left to right when speaking text.
     * Pan is specified as a float ranging from -1 to +1 where -1 maps to a hard-left pan,
     * 0 to center (the default behavior), and +1 to hard-right.
     *
     * @see TextToSpeech#speak(String, int, HashMap)
     * @see TextToSpeech#playEarcon(String, int, HashMap)
     */
    public static final String KEY_PARAM_PAN = "pan";

    /**
     * Feature key for network synthesis. See {@link TextToSpeech#getFeatures(Locale)}
     * for a description of how feature keys work. If set (and supported by the engine
     * as per {@link TextToSpeech#getFeatures(Locale)}, the engine must
     * use network based synthesis.
     *
     * @see TextToSpeech#speak(String, int, java.util.HashMap)
     * @see TextToSpeech#synthesizeToFile(String, java.util.HashMap, String)
     * @see TextToSpeech#getFeatures(java.util.Locale)
     */
    public static final String KEY_FEATURE_NETWORK_SYNTHESIS = "networkTts";

    /**
     * Feature key for embedded synthesis. See {@link TextToSpeech#getFeatures(Locale)}
     * for a description of how feature keys work. If set and supported by the engine
     * as per {@link TextToSpeech#getFeatures(Locale)}, the engine must synthesize
     * text on-device (without making network requests).
     */
    public static final String KEY_FEATURE_EMBEDDED_SYNTHESIS = "embeddedTts";
    
    protected TextToSpeechWrapper(Context context, OnInitListener listener) {
		super(context, listener);
	}

	@TargetApi(14)
	public TextToSpeechWrapper(Context context, OnInitListener listener,
			String engine) {
		super(context, listener, engine);
	}

}
