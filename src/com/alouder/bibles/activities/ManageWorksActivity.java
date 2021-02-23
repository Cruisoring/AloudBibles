package com.alouder.bibles.activities;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.alouder.bibles.R;
import com.alouder.bibles.data.BiblesContentProvider;
import com.alouder.bibles.data.FileDialog;
import com.alouder.bibles.data.Work;

public class ManageWorksActivity extends ListActivity {
	private final static String TAG = ManageWorksActivity.class.getSimpleName();

	public static String SummaryFormat = null;
	public static String ConfirmRemoval = "";

	private LocalBroadcastManager lbm;
	private LayoutInflater layoutInflater;

	private Button importFileButton;
	private ListView listView;
	private WorksAdapter adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//		requestWindowFeature(Window.FEATURE_ACTION_BAR);

		if (SummaryFormat == null) {
			Resources resources = this.getResources();
			SummaryFormat = resources.getString(R.string.summary_format);
			ConfirmRemoval = resources.getString(R.string.confirm_removal);
		}

		lbm = LocalBroadcastManager.getInstance(this);
		IntentFilter filter = new IntentFilter();
		filter.addAction(BiblesContentProvider.ACTION_WORKS_CHANGED);
		lbm.registerReceiver(broadcastReceiver, filter);
		
		layoutInflater = LayoutInflater.from(this);

//		setContentView(R.layout.management);
		listView = getListView();
		listView.setSelector(R.drawable.text_selector);
		importFileButton = new Button(this);
		importFileButton.setText(R.string.import_file);
		importFileButton.setOnClickListener(importListener);
		listView.addFooterView(importFileButton, null, false);
		
		adapter = new WorksAdapter(this, new ArrayList<Work>(BiblesContentProvider.workInstances));
		listView.setAdapter(adapter);
	}
	
	private BroadcastReceiver broadcastReceiver = new BroadcastReceiver(){

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			
			if (action != BiblesContentProvider.ACTION_WORKS_CHANGED)
				return;
			
			Bundle bundle = intent.getExtras();
			if (bundle == null)
				return;
			
			String reason = bundle.getString(BiblesContentProvider.COLUMN_CHANGE);
			String code = bundle.getString(BiblesContentProvider.COLUMN_CODE);
			Log.i(TAG, "Bibles changed: " + reason + " of " + code);

			adapter.reload();			
		}
		
	};
	
	private OnClickListener importListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			File mPath = new File(Environment.getExternalStorageDirectory() + "//DIR//");
            FileDialog fileDialog = new FileDialog(ManageWorksActivity.this, mPath);
            fileDialog.setFileEndsWith(".zip");
            fileDialog.addFileListener(new FileDialog.FileSelectedListener() {
                @Override
				public void fileSelected(File file) {
                    Log.d(TAG, " selected file " + file.toString());
                    
    				Intent openFileIntent = new Intent(BiblesContentProvider.ACTION_IMPORT_LOCAL_FILE);
					Bundle bundle = new Bundle();
					bundle.putString(BiblesContentProvider.COLUMN_URI, file.getPath());
					bundle.putString(BiblesContentProvider.COLUMN_TITLE, file.getName());
					openFileIntent.putExtras(bundle);
					lbm.sendBroadcast(openFileIntent);
                }
            });

            fileDialog.showDialog();
        }
	};

	public class WorksAdapter extends ArrayAdapter<Work> {
		/**
		 * Lock used to modify the content of {@link #mObjects}. Any write
		 * operation performed on the array should be synchronized on this lock.
		 * This lock is also used by the filter (see {@link #getFilter()} to
		 * make a synchronized copy of the original array of data.
		 */
		public final String ADAPTER = WorksAdapter.class.getSimpleName();

		private final ArrayList<Work> workList;
		private final Context mContext;
		private final AlertDialog.Builder builder, confirmRemovalBuilder;

		private OnClickListener removeListener = new OnClickListener() {

			@Override
			public void onClick(View v) {
				final Work work = (Work) v.getTag();
				if (work != null) {

					confirmRemovalBuilder.setMessage(ConfirmRemoval + work.code + "?");
					confirmRemovalBuilder.setPositiveButton(R.string.confirm,
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									Intent removeWorkIntent = new Intent(
											BiblesContentProvider.ACTION_REMOVE_WORK);
									Bundle bundle = new Bundle();
									bundle.putString(
											BiblesContentProvider.COLUMN_CODE,
											work.code);
									removeWorkIntent.putExtras(bundle);
									lbm.sendBroadcast(removeWorkIntent);
								}
							});

					confirmRemovalBuilder.setNegativeButton(R.string.cancel,
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.dismiss();
								}
							});

					confirmRemovalBuilder.show();
				}
			}
		};

		private OnClickListener showDetailListener = new OnClickListener() {

			@Override
			public void onClick(View v) {
				Work work = (Work) v.getTag();
				if (work != null) {
					ScrollView layout = (ScrollView) layoutInflater
							.inflate(R.layout.work_description, null);
					String text = String.format("%s %s", work.work, work.code);
					builder.setTitle(text);

					TextView textView = (TextView) layout
							.findViewById(R.id.textViewTitle);
					textView.setText(text);
					Locale locale = work.locale;
					text = String.format("%s, %s", locale.getDisplayName(),
							locale.getDisplayName(locale));
					textView = (TextView) layout
							.findViewById(R.id.textViewLanguage);
					textView.setText(text);

					textView = (TextView) layout
							.findViewById(R.id.textViewVersion);
					textView.setText(work.version);

					text = String.format(SummaryFormat, work.bookCount,
							work.chapterCount, work.verseCount);
					textView = (TextView) layout
							.findViewById(R.id.textViewSummary);
					textView.setText(text);

					textView = (TextView) layout
							.findViewById(R.id.textViewDescription);
					textView.setText(work.description);

					builder.setView(layout);
					builder.show();
				}
			}
		};

		public WorksAdapter(Context context, ArrayList<Work> list) {
			super(context, R.layout.text_2, list);
			mContext = context;
			workList = list;
			builder = new AlertDialog.Builder(mContext);
			builder.setTitle("Detail of work");
			builder.setPositiveButton(R.string.confirm, null);

			confirmRemovalBuilder = new AlertDialog.Builder(mContext);
			confirmRemovalBuilder.setTitle(R.string.confirm_work_removal);
		}

		public void reload() {

			workList.clear();
			
			workList.addAll(BiblesContentProvider.workInstances);

			this.notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			return workList.size();
		}

		@Override
		public Work getItem(int position) {
			if ((position < 0) || (position >= workList.size())) {
				return null;
			}

			return workList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Work work = workList.get(position);
			LinearLayout layout = null;
			
			if (convertView == null) {
				layout = (LinearLayout) layoutInflater.inflate(R.layout.work_item, null, false);
			}
			else {
				layout = (LinearLayout)convertView;
			}

			LinearLayout workItem = (LinearLayout) layout
					.findViewById(R.id.work_item);
//			workItem.setOnClickListener(showDetailListener);

			TextView text1 = (TextView) layout.findViewById(android.R.id.text1);
			String localeString = String.format("%s %s", 
					work.locale.getDisplayName(), work.locale.getDisplayName(work.locale));
			AloudBibleApplication.setText(text1, localeString);
			text1.setTag(work);
			text1.setOnClickListener(showDetailListener);

			TextView text2 = (TextView) layout.findViewById(android.R.id.text2);
			AloudBibleApplication.setText(text2, work.toString());
			text2.setTag(work);
			text2.setOnClickListener(showDetailListener);

			ImageButton removeButton = (ImageButton) layout
					.findViewById(R.id.removeButton);

			boolean isRemovable = work != BiblesContentProvider.primaryWork
					&& work != BiblesContentProvider.secondaryWork;
			removeButton.setEnabled(isRemovable);
			removeButton.setClickable(isRemovable);
			removeButton.setImageResource(isRemovable ? R.drawable.cross : R.drawable.cross_gray);
			removeButton.setTag(work);

			removeButton.setOnClickListener(removeListener);

			return layout;
		}

	}

}
