package com.alouder.bibles.activities;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.app.DownloadManager.Request;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
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
import android.widget.TextView;
import android.widget.Toast;

import com.alouder.bibles.R;
import com.alouder.bibles.data.BibleDescription;
import com.alouder.bibles.data.BiblesContentProvider;
import com.alouder.bibles.data.DownloadTask;
import com.alouder.bibles.data.DownloadTask.TaskStatus;
import com.alouder.bibles.data.Work;

public class DownloadActivity extends ListActivity {
	public static final String TAG = DownloadActivity.class.getSimpleName();
	private static String LIST_JSONFILE = "bibles.json";
	private static String TEMP_JSONFILE = "temp_bibles.json";
	private static BibleDescription[] availables = null;

	private static String confirmDownload, download_cancelled_format, build,
			data_size, books, chapters, verses;

	private LayoutInflater layoutInflater = null;
	private LocalBroadcastManager lbm;
	private DownloadManager dm;
	
	private long taskId = -1;

	private ListView listView;
	DownloadableAdapter adapter;
//	TextView textViewStatus = null;
	Button refreshButton = null;
	private BroadcastReceiver onDownloadCompletedReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {
				long downId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
				if (downId != taskId)
					return;
				
				Query query = new Query();
				query.setFilterById(taskId);
				DownloadActivity.this.unregisterReceiver(onDownloadCompletedReceiver);
				taskId = -1;
				
				try {
					Cursor c = dm.query(query);
					String filename = null;
					if (c.moveToFirst()) {
						int columnIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
						if (DownloadManager.STATUS_SUCCESSFUL == c.getInt(columnIndex)) {
//							ParcelFileDescriptor file = dm.openDownloadedFile(taskId);							
							columnIndex = c.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME);
							filename = c.getString(columnIndex);
							c.close();
						} else {
							TaskStatus status = new TaskStatus(c);
							Toast.makeText(DownloadActivity.this, status.toString(), Toast.LENGTH_SHORT).show();
							dm.remove(taskId);
							c.close();
							return;
						}
					}
					
					File tempFile = new File(filename);				
					availables = parseJsonFile(tempFile);
					
					if (filename == null)
						return;
					else if (availables.length != 0 && availables[0] != null) {
						adapter.clear();
						adapter.addAll(availables);
						adapter.notifyDataSetChanged();
						
						File oldJson = DownloadActivity.this.getFileStreamPath(LIST_JSONFILE);
						oldJson.delete();
						FileOutputStream outputStream = new FileOutputStream(oldJson);
						BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(tempFile));

						// create a buffer...
						byte[] buffer = new byte[1024];
						int bufferLength = 0; // used to store a temporary size of the
												// buffer

						// now, read through the input buffer and write the contents to the
						// file
						while ((bufferLength = inputStream.read(buffer)) > 0) {
							// add the data in the buffer to the file in the file output
							// stream (the file on the sd card
							outputStream.write(buffer, 0, bufferLength);
						}
						// close the output stream when done
						outputStream.close();						
						tempFile.delete();                		
					} else { // The newly json file is corrupted?
						tempFile.delete();
					}                						
				}catch(Exception ex) {
					Log.e(TAG, ex.getMessage());
					return;
				}
				
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		listView = this.getListView();
		layoutInflater = this.getLayoutInflater();

		if (confirmDownload == null) {
			Resources resources = this.getResources();
			confirmDownload = resources.getString(R.string.confirm_download);
			build = resources.getString(R.string.build);
			data_size = resources.getString(R.string.data_size);
			books = resources.getString(R.string.books);
			chapters = resources.getString(R.string.chapters);
			verses = resources.getString(R.string.verses);
			download_cancelled_format = resources
					.getString(R.string.download_cancelled_format);
		}

		dm = (DownloadManager)getSystemService(DOWNLOAD_SERVICE);
		lbm = LocalBroadcastManager.getInstance(this);
//		textViewStatus = new TextView(this);
//		listView.addHeaderView(textViewStatus);
		refreshButton = new Button(this);
		refreshButton.setText(R.string.refresh);
//		refreshButton.setOnClickListener(refreshListener);
		refreshButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				downloadList(LIST_JSONFILE, TEMP_JSONFILE);
			}
		});
		listView.addFooterView(refreshButton, null, false);

		File jsonFile = this.getFileStreamPath(LIST_JSONFILE);

		if (!jsonFile.exists()) {
			availables = new BibleDescription[0];
			
//			downloadList(LIST_JSONFILE, LIST_JSONFILE);
//			Toast.makeText(this, "Fail to get bible list", Toast.LENGTH_SHORT).show();
//			Intent i = new Intent(this, ManageWorksActivity.class);
//			this.startActivity(i);
//			this.finish();
		} else {
			availables = parseJsonFile(jsonFile);
		}

		adapter = new DownloadableAdapter(this, new ArrayList<BibleDescription>(Arrays.asList(availables)));
		listView.setAdapter(adapter);
		
		String lang = Locale.getDefault().getLanguage();
		int index = -1;
		for (int i = 0; i < availables.length; i++) {
			BibleDescription b = availables[i];
			if (b.getLanguage().startsWith(lang)) {
				index = i;
				break;
			}
		}
		listView.setSelection(index);
		
		IntentFilter filter = new IntentFilter();
		filter.addAction(BiblesContentProvider.ACTION_CHANGE_DONWLOAD);
		lbm.registerReceiver(downloadChangeReceiver, filter);
	}
	
//	private OnClickListener refreshListener = new OnClickListener() {
//		
//		@Override
//		public void onClick(View v) {
////			downloadList(LIST_JSONFILE, TEMP_JSONFILE);
//			if (getBibleList(LIST_JSONFILE)){
//				File file = DownloadActivity.this.getFileStreamPath(TEMP_JSONFILE);
//				availables = parseJsonFile(file);
//				
//				if (availables.length==0 || availables[0] == null)
//					return;
//				
//				File oldJson = DownloadActivity.this.getFileStreamPath(LIST_JSONFILE);
//				oldJson.delete();
//				file.renameTo(oldJson);                		
//				
//        		adapter.clear();
//           		adapter.addAll(availables);
//        		adapter.notifyDataSetChanged();        		
//			}
//		}
//	};
	
	private void downloadList(String sourceFile, String destFile) {
		Uri uri = Uri.parse(DownloadTask.DefaultProjectUri + sourceFile + "/download");
		Request request = new DownloadManager.Request(uri);
		request.setAllowedNetworkTypes(Request.NETWORK_WIFI);
		request.setAllowedOverRoaming(false);

		request.setTitle(sourceFile);
		request.setDescription(sourceFile);
		
		File outputFile = new File(this.getExternalCacheDir(), destFile);
		request.setDestinationUri(Uri.fromFile(outputFile));

		taskId = dm.enqueue(request);
		
		IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
		this.registerReceiver(onDownloadCompletedReceiver, filter);
	}

	/*/
	private boolean getBibleList(String filename) {

		try {
			String address = DownloadTask.DefaultProjectUri + filename + "/download";
			// set the download URL, a url that points to a file on the internet
			// this is the file to be downloaded
			URL url = new URL(address);

			// create the new connection
			HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();

			// set up some things on the connection
			urlConnection.setRequestMethod("GET");
			urlConnection.setDoOutput(true);

			// and connect!
			urlConnection.connect();

			// create a new file, specifying the path, and the filename
			// which we want to save the file as.
			File file = this.getFileStreamPath(TEMP_JSONFILE);

			// this will be used to write the downloaded data into the file we
			// created
			FileOutputStream fileOutput = new FileOutputStream(file);

			// this will be used in reading the data from the internet
//			InputStream inputStream = urlConnection.getInputStream();
			InputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());

			// variable to store total downloaded bytes
			int downloadedSize = 0;

			// create a buffer...
			byte[] buffer = new byte[1024];
			int bufferLength = 0; // used to store a temporary size of the
									// buffer

			// now, read through the input buffer and write the contents to the
			// file
			while ((bufferLength = inputStream.read(buffer)) > 0) {
				// add the data in the buffer to the file in the file output
				// stream (the file on the sd card
				fileOutput.write(buffer, 0, bufferLength);
				// add up the size so we know how much is downloaded
				downloadedSize += bufferLength;
			}
			// close the output stream when done
			fileOutput.close();
			
			return downloadedSize != 0;

			// catch some possible errors...
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	//*/
	
	private BibleDescription[] parseJsonFile(File jsonFile) {
		String jsonText = null;
		StringBuilder text = new StringBuilder();
		try {
			BufferedReader br = new BufferedReader(new FileReader(jsonFile));
			while ((jsonText = br.readLine()) != null) {
				text.append(jsonText);
				text.append('\n');
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		jsonText = text.toString();	
		
		JSONArray array;
		try {
			array = new JSONArray(jsonText);
			int len = array.length();
			BibleDescription[] result = new BibleDescription[len];
			
			for(int i=0; i < len; i ++) {
				JSONObject obj = array.getJSONObject(i);
				BibleDescription desc = new BibleDescription(obj);
				
				result[i] = desc;
			}
			
			return result;
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return new BibleDescription[0];
		}
	}
	
//	private BibleDescription[] getBibleDescriptions(File jsonFile) {
//		String jsonText = null;
//		StringBuilder text = new StringBuilder();
//		try {
//			BufferedReader br = new BufferedReader(new FileReader(jsonFile));
//			while ((jsonText = br.readLine()) != null) {
//				text.append(jsonText);
//				text.append('\n');
//			}
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//			return null;
//		} catch (IOException e) {
//			e.printStackTrace();
//			return null;
//		}
//		jsonText = text.toString();
//		Log.d(TAG, "jsonText:\n" + jsonText);
//		return jsonText=="" ? new BibleDescription[0] : JSONHelper.parseArray(jsonText, BibleDescription.class);
//	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		lbm.unregisterReceiver(downloadChangeReceiver);
		if (taskId != -1) {
			DownloadManager downloadManager = (DownloadManager)getSystemService(DOWNLOAD_SERVICE);
			downloadManager.remove(taskId);
			this.unregisterReceiver(onDownloadCompletedReceiver);
		}
	}

	BroadcastReceiver downloadChangeReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(
					BiblesContentProvider.ACTION_CHANGE_DONWLOAD)) {
				if (adapter != null) {
					adapter.notifyDataSetChanged();
				}
			}
		}
	};

	public class DownloadableAdapter extends ArrayAdapter<BibleDescription> {

		public final String ADAPTER = DownloadableAdapter.class.getSimpleName();

		private final ArrayList<BibleDescription> bibles;
		private final Context mContext;
//		private final AlertDialog.Builder confirmDownloadBuilder;

		private OnClickListener cancelListener = new OnClickListener() {

			@Override
			public void onClick(View v) {
				final BibleDescription description = (BibleDescription) v
						.getTag();
				if (description != null) {
					Intent cancelIntent = new Intent(
							BiblesContentProvider.ACTION_CANCEL_DONWLOAD);
					Bundle bundle = new Bundle();
					String code = description.getCode();
					bundle.putString(BiblesContentProvider.COLUMN_CODE, code);
					bundle.putString(BiblesContentProvider.COLUMN_URI,
							description.getUri());
					bundle.putString(BiblesContentProvider.COLUMN_TITLE,
							description.getName());
					bundle.putString(BiblesContentProvider.COLUMN_LANGUAGE,
							description.getLanguage());
					bundle.putInt(BiblesContentProvider.COLUMN_SIZE,
							description.getZipSize());
					cancelIntent.putExtras(bundle);
					lbm.sendBroadcast(cancelIntent);

					Toast.makeText(mContext,
							String.format(download_cancelled_format, code),
							Toast.LENGTH_LONG).show();
				}
			}
		};

		private OnClickListener downloadListener = new OnClickListener() {

			@Override
			public void onClick(View v) {
				final BibleDescription description = (BibleDescription) v
						.getTag();
				if (description != null) {
					StringBuilder sb = new StringBuilder();
					sb.append(String.format("%s %s(%s)?\n\n", confirmDownload,
							description.getCode(), description.getName()));
					Locale locale = description.getLocale();
					if (locale.equals(Locale.getDefault()))
						sb.append(String.format("%s\n", locale.getDisplayName()));
					else
						sb.append(String.format("%s (%s)\n",
								locale.getDisplayName(),
								locale.getDisplayName(locale)));
					sb.append(String.format("\t%s: %s\n", build,
							description.getBuild()));
					sb.append(String.format("\t%s: %dK\n", data_size,
							description.getZipSize() / 1024));
					sb.append(String.format("\t%s: %d\n\t%s %d\n\t%s %d\n",
							books, description.getBookCount(), chapters,
							description.getChapterCount(), verses,
							description.getVerseCount()));
					
					AlertDialog.Builder confirmDownloadBuilder = new AlertDialog.Builder(mContext);
					confirmDownloadBuilder.setTitle("Confirm downloading");

					confirmDownloadBuilder.setMessage(sb.toString());
					confirmDownloadBuilder.setPositiveButton(R.string.confirm,
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									Intent downloadIntent = new Intent(
											BiblesContentProvider.ACTION_START_DOWNLOAD);
									Bundle bundle = new Bundle();
									bundle.putString(
											BiblesContentProvider.COLUMN_CODE,
											description.getCode());
									bundle.putString(
											BiblesContentProvider.COLUMN_URI,
											description.getUri());
									bundle.putString(
											BiblesContentProvider.COLUMN_TITLE,
											description.getName());
									bundle.putString(
											BiblesContentProvider.COLUMN_LANGUAGE,
											description.getLanguage());
									bundle.putString(
											BiblesContentProvider.COLUMN_VERSION,
											description.getBuild());
									bundle.putInt(
											BiblesContentProvider.KEY_BOOK_COUNT,
											description.getBookCount());
									bundle.putInt(
											BiblesContentProvider.KEY_CHAPTER_COUNT,
											description.getChapterCount());
									bundle.putInt(
											BiblesContentProvider.KEY_VERSE_COUNT,
											description.getVerseCount());
									bundle.putInt(
											BiblesContentProvider.COLUMN_SIZE,
											description.getZipSize());
									downloadIntent.putExtras(bundle);
									lbm.sendBroadcast(downloadIntent);
								}
							});

					confirmDownloadBuilder.setNegativeButton(R.string.cancel,
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.dismiss();
								}
							});

					confirmDownloadBuilder.show();
				}
			}
		};

//		private OnClickListener queryListener = new OnClickListener() {
//
//			@Override
//			public void onClick(View v) {
//				final BibleDescription description = (BibleDescription) v
//						.getTag();
//				if (description != null
//						&& DownloadTask.AllTasks.containsKey(description
//								.getCode())) {
//					DownloadTask task = DownloadTask.AllTasks.get(description
//							.getCode());
//					TaskStatus status = task.getStatus();
//					if (status != null) {
//						textViewStatus.setText(status.toString());
//					}
//				}
//			}
//		};

		public DownloadableAdapter(Context context,
				ArrayList<BibleDescription> bibleList) {
			super(context, R.layout.text_2, bibleList);
			mContext = context;
			bibles = bibleList;
//			confirmDownloadBuilder = new AlertDialog.Builder(mContext);
//			confirmDownloadBuilder.setTitle("Confirm downloading");
//			confirmDownloadBuilder.setTitle(R.string.download_confirmation);
		}

		@Override
		public int getCount() {
			return bibles.size();
		}

		@Override
		public BibleDescription getItem(int position) {
			if ((position < 0) || (position >= bibles.size())) {
				return null;
			}

			return bibles.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			
//			Log.d(ADAPTER, "getView() for " + position);
			try {
				BibleDescription description = bibles.get(position);
				String code = description.getCode();
				LinearLayout layout = null;
				
				if (convertView == null) {
					layout = (LinearLayout) layoutInflater.inflate(
							R.layout.work_item, null, false);
				} else {
					layout = (LinearLayout) convertView;
				}
				
//				Log.d(ADAPTER, "layout" + (layout==null ? "==null" : "!=null"));
				TextView text1 = (TextView) layout.findViewById(android.R.id.text1);
//				Log.d(ADAPTER, "text1" + (text1==null ? "==null" : "!=null"));
				
//				Log.d(ADAPTER, "description=" + (description==null ? "null" : description.toString()));
				
				Locale locale = description.getLocale();
//				Log.d(ADAPTER, "locale" + (locale==null ? "==null" : "!=null"));
				String localeString = String.format("%s %s", locale.getDisplayName(), code);
//				Log.d(ADAPTER, "localeString: " + localeString);
				AloudBibleApplication.setText(text1, localeString);
				text1.setTag(description);
				
				TextView text2 = (TextView) layout.findViewById(android.R.id.text2);
//				Log.d(ADAPTER, "text2" + (text2==null ? "==null" : "!=null"));
				AloudBibleApplication.setText(text2, description.toString());
				text2.setTag(description);
				
				boolean isDisabled = BiblesContentProvider.workCodes.contains(code);
				
				if (isDisabled && !AloudBibleApplication.isSelectedWork(code)) {
					Work existed = BiblesContentProvider.getWorkInstance(code);
					try {
						Date existedBuild = Date.valueOf(existed.version);
						if (existedBuild.before(description.getBuildDate()))
							isDisabled = false;
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
						isDisabled = false;
					}
				}
				
				ImageButton theButton = (ImageButton) layout
						.findViewById(R.id.removeButton);
				theButton.setTag(description);
				
				if (isDisabled) {
					theButton.setImageResource(R.drawable.plus_gray);
					text1.setOnClickListener(null);
					text2.setOnClickListener(null);
					theButton.setOnClickListener(null);
				} else {
					int resId = R.drawable.plus;
					OnClickListener listener = downloadListener;
					// OnClickListener listener2 = downloadListener;
					if (DownloadTask.AllTasks.containsKey(code)) {
						DownloadTask task = DownloadTask.AllTasks.get(code);
						TaskStatus status = task.getStatus();
						if (status != null && status.isOngoing()) {
							resId = R.drawable.minus;
							listener = cancelListener;
							// listener2 = queryListener;
						}
					}
					theButton.setImageResource(resId);
					text1.setOnClickListener(listener);
					text2.setOnClickListener(listener);
					theButton.setOnClickListener(listener);
				}
				
				return layout;
				
			} catch (Exception e){
//				Log.e(TAG, e.getMessage());
				return null;
			}
		}

		
	}

}
