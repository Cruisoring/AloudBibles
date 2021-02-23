package com.alouder.bibles.activities;

import java.util.ArrayList;
import java.util.List;

import com.alouder.bibles.R;
import com.alouder.bibles.data.BiblesContentProvider;
import com.alouder.bibles.data.Work;
import com.alouder.bibles.text2speech.TtsService;
import com.alouder.bibles.text2speech.TtsService.TtsStopPattern;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class PreferenceActivity extends Activity implements
		OnCheckedChangeListener, OnItemSelectedListener,
		OnSeekBarChangeListener, OnClickListener {
	public static final String TAG = PreferenceActivity.class.getSimpleName();
	public static final String EMPTY = "";

	public static Float Default_Rate = 1.0f;
	public static Float Default_Pitch = 1.0f;

	private CheckBox checkSecondaryWork, checkPrimaryTts, checkSecondaryTts;
	private Spinner spinnerSpeakMode, spinnerPrimaryWork, spinnerSecondaryWork,
			spinnerPrimaryTts, spinnerSecondaryTts;
	private LinearLayout primaryTtsSettingsLayout, secondarySettingsLayout,
			secondaryTtsSettingsLayout;
	private SeekBar seekPrimaryRate, seekPrimaryPitch, seekSecondaryRate,
			seekSecondaryPitch;
	private TextView textPrimaryRate, textPrimaryPitch, textSecondaryRate,
			textSecondaryPitch;
	private Button buttonConfirm, buttonCancel;

	private ArrayAdapter<String> primaryWorksAdapter, secondaryWorksAdapter,
			primaryTtsAdapter, secondaryTtsAdapter, speakModesAdapter;

	private String[] speakModeOptions;
	private List<TtsStopPattern> speakModeValues;
	private String[] speechRateOptions = null;
	private String[] pitchOptions = null;
	private List<Float> speechRateValues = null;
	private List<Float> pitchValues = null;

	private final List<String> workNames = new ArrayList<String>();
	private final List<String> ttsLables = new ArrayList<String>(TtsService.ttsEngineLables);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Resources resources = this.getResources();
		speakModeOptions = resources.getStringArray(R.array.speak_mode_options);
		String[] temp = resources.getStringArray(R.array.speak_mode_values);
		speakModeValues = new ArrayList<TtsStopPattern>();
		try {
			for (String s : temp) {
				speakModeValues.add(TtsStopPattern.valueOf(s));
			}			
		} catch (Exception e) {
			speakModeValues.clear();
			TtsStopPattern[] patterns = TtsStopPattern.values();
			int len = Math.min(speakModeOptions.length, patterns.length);
			for (int i = 0; i < len; i ++) {
				speakModeValues.add(patterns[i]);
			}
		}
		speechRateOptions = resources.getStringArray(R.array.speech_rate_options);
		pitchOptions = resources.getStringArray(R.array.pitch_options);
		speechRateValues = fromResources(R.array.speech_rate_values);
		pitchValues = fromResources(R.array.pitch_values);
		
		for (Work w : BiblesContentProvider.workInstances) {
			workNames.add(w.toString());
		}

		setContentView(R.layout.preference);

		populateWidgets();

		updateUIFromPreferences();
	}

	private List<Float> fromResources(int id) {
		String[] temp = this.getResources().getStringArray(id);
		ArrayList<Float> list = new ArrayList<Float>();
		for (String s : temp) {
			list.add(Float.valueOf(s));
		}
		return list;
	}

	private void populateWidgets() {
		checkSecondaryWork = (CheckBox) findViewById(R.id.checkBoxSecondWork);
		checkPrimaryTts = (CheckBox) findViewById(R.id.checkboxPrimaryTts);
		checkSecondaryTts = (CheckBox) findViewById(R.id.checkboxSecondTts);

		checkPrimaryTts.setOnCheckedChangeListener(this);
		checkSecondaryWork.setOnCheckedChangeListener(this);
		checkSecondaryTts.setOnCheckedChangeListener(this);

		primaryTtsSettingsLayout = (LinearLayout) findViewById(R.id.primaryTtsSettingsLayout);
		secondarySettingsLayout = (LinearLayout) findViewById(R.id.secondarySettings);
		secondaryTtsSettingsLayout = (LinearLayout) findViewById(R.id.secondaryTtsSettingsLayout);

		spinnerSpeakMode = (Spinner) findViewById(R.id.speakModeSpinner);
		spinnerPrimaryWork = (Spinner) findViewById(R.id.primaryWorkSpinner);
		spinnerPrimaryTts = (Spinner) findViewById(R.id.primaryTtsSpinner);
		spinnerSecondaryWork = (Spinner) findViewById(R.id.secondaryWorkSpinner);
		spinnerSecondaryTts = (Spinner) findViewById(R.id.secondaryTtsSpinner);

		spinnerSpeakMode.setOnItemSelectedListener(this);
		spinnerPrimaryWork.setOnItemSelectedListener(this);
		spinnerPrimaryTts.setOnItemSelectedListener(this);
		spinnerSecondaryWork.setOnItemSelectedListener(this);
		spinnerSecondaryTts.setOnItemSelectedListener(this);

		seekPrimaryRate = (SeekBar) findViewById(R.id.seekBarPrimaryTtsRate);
		seekPrimaryPitch = (SeekBar) findViewById(R.id.seekBarPrimaryTtsPitch);
		seekSecondaryRate = (SeekBar) findViewById(R.id.seekBarSecondaryTtsRate);
		seekSecondaryPitch = (SeekBar) findViewById(R.id.seekBarSecondaryPitch);

		seekPrimaryRate.setOnSeekBarChangeListener(this);
		seekPrimaryPitch.setOnSeekBarChangeListener(this);
		seekSecondaryRate.setOnSeekBarChangeListener(this);
		seekSecondaryPitch.setOnSeekBarChangeListener(this);

		textPrimaryRate = (TextView) findViewById(R.id.textPrimaryRate);
		textPrimaryPitch = (TextView) findViewById(R.id.textPrimaryPitch);
		textSecondaryRate = (TextView) findViewById(R.id.textSecondaryRate);
		textSecondaryPitch = (TextView) findViewById(R.id.textSecondaryPitch);

		buttonConfirm = (Button) findViewById(R.id.buttonConfirm);
		buttonCancel = (Button) findViewById(R.id.buttonCancel);

		buttonConfirm.setOnClickListener(this);
		buttonCancel.setOnClickListener(this);

		int spinner_dd_item = android.R.layout.simple_spinner_dropdown_item;
		int spinner_item = android.R.layout.simple_spinner_item;
		speakModesAdapter = new ArrayAdapter<String>(this, spinner_item,
				speakModeOptions);
		speakModesAdapter.setDropDownViewResource(spinner_dd_item);
		spinnerSpeakMode.setAdapter(speakModesAdapter);
		
		primaryWorksAdapter = new ArrayAdapter<String>(this, spinner_item,
				workNames);
		primaryWorksAdapter.setDropDownViewResource(spinner_dd_item);
		spinnerPrimaryWork.setAdapter(primaryWorksAdapter);

		secondaryWorksAdapter = new ArrayAdapter<String>(this, spinner_item,
				workNames);
		secondaryWorksAdapter.setDropDownViewResource(spinner_dd_item);
		spinnerSecondaryWork.setAdapter(secondaryWorksAdapter);

		primaryTtsAdapter = new ArrayAdapter<String>(this, spinner_item,
				ttsLables);
		primaryTtsAdapter.setDropDownViewResource(spinner_dd_item);
		spinnerPrimaryTts.setAdapter(primaryTtsAdapter);

		secondaryTtsAdapter = new ArrayAdapter<String>(this, spinner_item,
				ttsLables);
		secondaryTtsAdapter.setDropDownViewResource(spinner_dd_item);
		spinnerSecondaryTts.setAdapter(secondaryTtsAdapter);
	}

	private void updateUIFromPreferences() {
		TtsStopPattern pattern = TtsService.stopPattern;
		int index = speakModeValues.indexOf(pattern);
		spinnerSpeakMode.setSelection(index);
		
		String[] names = AloudBibleApplication.selectedWorkCodes;
		if (names == null || names.length == 0) // Even the primary work is not
												// selected
		{
			if (primaryWorksAdapter.getCount() != 0)
				spinnerPrimaryWork.setSelection(0);
		} else {
			index = BiblesContentProvider.workCodes.indexOf(names[0]);
			if (index < 0) {
				Toast.makeText(
						this,
						"Primary work of "
								+ names[0]
								+ "is missing, set to the default first one of "
								+ BiblesContentProvider.workCodes.get(0), Toast.LENGTH_LONG).show();
				index = 0;
			}
			spinnerPrimaryWork.setSelection(index);

			if (names.length > 1 && names[1] != null) {
				index = BiblesContentProvider.workCodes.indexOf(names[1]);

				if (index != -1 && !checkSecondaryWork.isChecked()) {
					checkSecondaryWork.setChecked(true);
					spinnerSecondaryWork.setSelection(index);
				} else if (index == -1 && checkSecondaryWork.isChecked()) {
					checkSecondaryWork.setChecked(false);
				}
			}
		}

		names = AloudBibleApplication.selectedTtsNames;
		Float[] rates = AloudBibleApplication.selectedTtsRates;
		Float[] pitches = AloudBibleApplication.selectedTtsPitchs;
		if (names == null || names.length == 0) {
			if (checkPrimaryTts.isChecked())
				checkPrimaryTts.setChecked(false);

			if (checkSecondaryTts.isChecked())
				checkSecondaryTts.setChecked(false);
		} else {
			index = TtsService.ttsEngineNames.indexOf(names[0]);
			if (index >= 0 && !checkPrimaryTts.isChecked()) {
				checkPrimaryTts.setChecked(true);
				spinnerPrimaryTts.setSelection(index);
				setSeekBarValue(seekPrimaryRate, textPrimaryRate, rates[0]);
				setSeekBarValue(seekPrimaryPitch, textPrimaryPitch, pitches[0]);
			} else if (index < 0 && checkPrimaryTts.isChecked())
				checkPrimaryTts.setChecked(false);

			if (names.length > 1) {
				index = TtsService.ttsEngineNames.indexOf(names[1]);
				if (index >= 0 && !checkSecondaryTts.isChecked()) {
					checkSecondaryTts.setChecked(true);
					spinnerSecondaryTts.setSelection(index);
					setSeekBarValue(seekSecondaryRate, textSecondaryRate,
							rates[1]);
					setSeekBarValue(seekSecondaryPitch, textSecondaryPitch,
							pitches[1]);
				} else if (index < 0 && checkSecondaryTts.isChecked())
					checkSecondaryTts.setChecked(false);
			}
		}
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		int newVisibility = isChecked ? View.VISIBLE : View.GONE;
		switch (buttonView.getId()) {
		case R.id.checkboxPrimaryTts:
			if (!isChecked) {
				AloudBibleApplication.selectedTtsNames[0] = EMPTY;
			}

			primaryTtsSettingsLayout.setVisibility(newVisibility);
			break;

		case R.id.checkBoxSecondWork:
			secondarySettingsLayout.setVisibility(newVisibility);
			break;

		case R.id.checkboxSecondTts:
			if (!isChecked) {
				AloudBibleApplication.selectedTtsNames[1] = EMPTY;
			}
			secondaryTtsSettingsLayout.setVisibility(newVisibility);
			break;

		default:
			Log.e(TAG, "Unexpected calling onCheckedChanged() from "
					+ buttonView);
			break;
		}
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position,
			long id) {
		switch (parent.getId()) {
		case R.id.speakModeSpinner:
			TtsService.stopPattern = speakModeValues.get(position);			
			break;
			
		case R.id.primaryWorkSpinner:
			AloudBibleApplication.selectedWorkCodes[0] = BiblesContentProvider.workCodes
					.get(position);
			break;

		case R.id.secondaryWorkSpinner:
			AloudBibleApplication.selectedWorkCodes[1] = BiblesContentProvider.workCodes
					.get(position);
			break;

		case R.id.primaryTtsSpinner:
			AloudBibleApplication.selectedTtsNames[0] = TtsService.ttsEngineNames.get(position);
			break;

		case R.id.secondaryTtsSpinner:
			AloudBibleApplication.selectedTtsNames[1] = TtsService.ttsEngineNames.get(position);
			break;

		default:
			break;
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {

	}

	@Override
	public void onClick(View v) {
		if (v == buttonCancel) {
			AloudBibleApplication.application.getPreferences();
			PreferenceActivity.this.setResult(RESULT_CANCELED);
			this.finish();
		} else if (v == buttonConfirm) {
			savePreference();
			PreferenceActivity.this.setResult(RESULT_OK);
			this.finish();
		}
	}

	private void savePreference() {
		int position = spinnerPrimaryWork.getSelectedItemPosition();
		AloudBibleApplication.selectedWorkCodes[0] = position < 0 ? EMPTY
				: BiblesContentProvider.workCodes.get(position);

		if (!checkSecondaryWork.isChecked())
		{
			AloudBibleApplication.selectedWorkCodes[1] = EMPTY;
		}
		else
		{
			position = spinnerSecondaryWork.getSelectedItemPosition();
			
			AloudBibleApplication.selectedWorkCodes[1] = position >= 0 ? 
					BiblesContentProvider.workCodes.get(position)
					: EMPTY;
		}

		if (!checkPrimaryTts .isChecked())
		{
			AloudBibleApplication.selectedTtsNames[0] = EMPTY;
			AloudBibleApplication.selectedTtsRates[0] = TtsService.DEFAULT_SPEECH_RATE;
			AloudBibleApplication.selectedTtsPitchs[0] = TtsService.DEFAULT_SPEECH_PITCH;
		}
		else
		{
			position = spinnerPrimaryTts.getSelectedItemPosition();
			
			AloudBibleApplication.selectedTtsNames[0] = position >= 0
					? TtsService.ttsEngineNames.get(position)
					: EMPTY;
			position = seekPrimaryRate.getProgress();
			AloudBibleApplication.selectedTtsRates[0] = speechRateValues.get(position);
			position = seekPrimaryPitch.getProgress();
			AloudBibleApplication.selectedTtsPitchs[0] = pitchValues.get(position);
		}

		if (!checkSecondaryWork.isChecked() || !checkSecondaryTts.isChecked())
		{
			AloudBibleApplication.selectedTtsNames[1] = EMPTY;
			AloudBibleApplication.selectedTtsRates[1] = TtsService.DEFAULT_SPEECH_RATE;
			AloudBibleApplication.selectedTtsPitchs[1] = TtsService.DEFAULT_SPEECH_PITCH;
		}
		else
		{
			position = spinnerSecondaryTts.getSelectedItemPosition();
			
			AloudBibleApplication.selectedTtsNames[1] = position >= 0 ?
					TtsService.ttsEngineNames.get(position) : EMPTY;
			position = seekSecondaryRate.getProgress();
			AloudBibleApplication.selectedTtsRates[1] = speechRateValues.get(position);
			position = seekSecondaryPitch.getProgress();
			AloudBibleApplication.selectedTtsPitchs[1] = pitchValues.get(position);
		}

		AloudBibleApplication.application.savePreference();
		
		BiblesContentProvider.loadWorks(AloudBibleApplication.selectedWorkCodes);
		
		AloudBibleApplication.getTtsWrapperService().resetEngines();
	}

	private void setSeekBarValue(SeekBar seekBar, TextView textView,
			float theValue) {
		boolean isOfRate = seekBar == seekPrimaryRate
				|| seekBar == seekSecondaryRate;
		List<Float> values = isOfRate ? speechRateValues : pitchValues;
		String[] options = isOfRate ? speechRateOptions : pitchOptions;

		int index = values.indexOf(theValue);
		if (index == -1) {
			Log.w(TAG, "Unexpected value of " + theValue + " for "
					+ (isOfRate ? "rate" : "pitch"));
			index = values.indexOf(isOfRate ? Default_Rate : Default_Pitch);
		}
		seekBar.setProgress(index);
		textView.setText(options[index]);
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		boolean isOfRate = seekBar == seekPrimaryRate
				|| seekBar == seekSecondaryRate;
		String[] options = isOfRate ? speechRateOptions : pitchOptions;

		String valueString = options[progress];
		if (seekBar == seekPrimaryRate) {
			textPrimaryRate.setText(valueString);
		} else if (seekBar == seekPrimaryPitch) {
			textPrimaryPitch.setText(valueString);
		} else if (seekBar == seekSecondaryRate) {
			textSecondaryRate.setText(valueString);
		} else if (seekBar == seekSecondaryPitch) {
			textSecondaryPitch.setText(valueString);
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
	}

}
