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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class PreferenceOldActivity extends Activity implements
		OnCheckedChangeListener, OnItemSelectedListener,
		OnClickListener {
	public static final String TAG = PreferenceOldActivity.class.getSimpleName();
	public static final String EMPTY = "";

	private CheckBox checkSecondaryWork, checkPrimaryTts, checkSecondaryTts;
	private Spinner spinnerSpeakMode, spinnerPrimaryWork, spinnerSecondaryWork;
	private LinearLayout  secondarySettingsLayout;
	private Button buttonConfirm, buttonCancel;

	private ArrayAdapter<String> primaryWorksAdapter, secondaryWorksAdapter, speakModesAdapter;

	private String[] speakModeOptions;
	private List<TtsStopPattern> speakModeValues;
	private final List<String> workNames = new ArrayList<String>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Resources resources = this.getResources();
		speakModeOptions = resources.getStringArray(R.array.speak_mode_options);
		String[] temp = resources.getStringArray(R.array.speak_mode_values);
		speakModeValues = new ArrayList<TtsStopPattern>();
		for (String s : temp) {
			speakModeValues.add(TtsStopPattern.valueOf(s));
		}

		for (Work w : BiblesContentProvider.workInstances) {
			workNames.add(w.toString());
		}

		setContentView(R.layout.preference_old);

		populateWidgets();

		updateUIFromPreferences();
	}

	private void populateWidgets() {
		secondarySettingsLayout = (LinearLayout) findViewById(R.id.secondarySettings);
		
		checkSecondaryWork = (CheckBox) findViewById(R.id.checkBoxSecondWork);
		checkPrimaryTts = (CheckBox) findViewById(R.id.checkboxPrimaryTts);
		checkSecondaryTts = (CheckBox) findViewById(R.id.checkboxSecondTts);

		checkPrimaryTts.setOnCheckedChangeListener(this);
		checkSecondaryWork.setOnCheckedChangeListener(this);
		checkSecondaryTts.setOnCheckedChangeListener(this);

		spinnerSpeakMode = (Spinner) findViewById(R.id.speakModeSpinner);
		spinnerPrimaryWork = (Spinner) findViewById(R.id.primaryWorkSpinner);
		spinnerSecondaryWork = (Spinner) findViewById(R.id.secondaryWorkSpinner);

		spinnerSpeakMode.setOnItemSelectedListener(this);
		spinnerPrimaryWork.setOnItemSelectedListener(this);
		spinnerSecondaryWork.setOnItemSelectedListener(this);

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
								+ BiblesContentProvider.workCodes.get(0),
						Toast.LENGTH_LONG).show();
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
		if (names == null || names.length == 0) {
			if (checkPrimaryTts.isChecked())
				checkPrimaryTts.setChecked(false);

			if (checkSecondaryTts.isChecked())
				checkSecondaryTts.setChecked(false);
		} else {
			checkPrimaryTts.setChecked(names.length > 0 && names[0].equals("Default"));
			checkSecondaryTts.setChecked(names.length > 1 && names[1].equals("Default"));
		}
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		int newVisibility = isChecked ? View.VISIBLE : View.GONE;
		switch (buttonView.getId()) {
		case R.id.checkboxPrimaryTts:
			AloudBibleApplication.selectedTtsNames[0] = isChecked? "Default" : EMPTY;
			break;

		case R.id.checkBoxSecondWork:
			secondarySettingsLayout.setVisibility(newVisibility);
			break;

		case R.id.checkboxSecondTts:
			AloudBibleApplication.selectedTtsNames[1] = isChecked? "Default" : EMPTY;
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
			PreferenceOldActivity.this.setResult(RESULT_CANCELED);
			this.finish();
		} else if (v == buttonConfirm) {
			savePreference();
			PreferenceOldActivity.this.setResult(RESULT_OK);
			this.finish();
		}
	}

	private void savePreference() {
		int position = spinnerPrimaryWork.getSelectedItemPosition();
		AloudBibleApplication.selectedWorkCodes[0] = position < 0 ? EMPTY
				: BiblesContentProvider.workCodes.get(position);

		if (!checkSecondaryWork.isChecked()) {
			AloudBibleApplication.selectedWorkCodes[1] = EMPTY;
		} else {
			position = spinnerSecondaryWork.getSelectedItemPosition();

			AloudBibleApplication.selectedWorkCodes[1] = position >= 0 ? BiblesContentProvider.workCodes
					.get(position) : EMPTY;
		}

		if (!checkPrimaryTts.isChecked()) {
			AloudBibleApplication.selectedTtsNames[0] = EMPTY;
			AloudBibleApplication.selectedTtsRates[0] = TtsService.DEFAULT_SPEECH_RATE;
			AloudBibleApplication.selectedTtsPitchs[0] = TtsService.DEFAULT_SPEECH_PITCH;
		}
		else
		{
			AloudBibleApplication.selectedTtsNames[0] = "Default";
		}

		if (!checkSecondaryWork.isChecked() || !checkSecondaryTts.isChecked()) {
			AloudBibleApplication.selectedTtsNames[1] = EMPTY;
			AloudBibleApplication.selectedTtsRates[1] = TtsService.DEFAULT_SPEECH_RATE;
			AloudBibleApplication.selectedTtsPitchs[1] = TtsService.DEFAULT_SPEECH_PITCH;
		}

		AloudBibleApplication.application.savePreference();

		BiblesContentProvider
				.loadWorks(AloudBibleApplication.selectedWorkCodes);

		AloudBibleApplication.getTtsWrapperService().resetEngines();
	}

}