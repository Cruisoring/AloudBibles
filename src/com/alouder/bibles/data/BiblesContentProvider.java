package com.alouder.bibles.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Date;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.alouder.bibles.data.DownloadTask.TaskStatus;
import android.app.DownloadManager;
import android.app.SearchManager;
import android.app.DownloadManager.Request;
import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

public class BiblesContentProvider extends ContentProvider {
	// {{ #region Constants Definition

	public final static int DATABASE_VERSION = 1;
	public final static String DATABASE_NAME = "bibles.db";
	public final static String ZIP_SUFFIX = ".zip";
	public final static String DATABASE_SUFFIX = ".db";
	public final static String CONTENT_SUFFIX = ".txt";

	private final static String AUTHORITY = "com.alouder.bibles.data.provider";
	public final static String BASE_PATH = "bibles";
	public final static Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/*");

	public final static String TAG = BiblesContentProvider.class
			.getSimpleName();

	public static boolean OPEN_WORKS_AUTOMATICALLY = true;
	public static boolean OPEN_BOOKS_AUTOMATICALLY = true;
	public static boolean OPEN_CHAPTERS_AUTOMATICALLY = false;

	public static final String TABLE_WORKS = "works";
	public static final String TABLE_BOOKS = "books";
	public static final String TABLE_CHAPTERS = "chapters";
	public static final String TABLE_SECTIONS = "sections";
	public static final String TABLE_VERSES = "verses";

	// */ Views to facilitate accessing items
	public static final String VIEW_BOOKS = "viewBooks";
	public static final String VIEW_CHAPTERS = "viewChapters";
	public static final String VIEW_SECTIONS = "viewSections";
	public static final String VIEW_VERSES = "viewVerses";
	// */

	// Columns within tables
	public static final String KEY_ID = "_id";
	// public static final String KEY_ORDINAL = "ordinal";
	// public static final String KEY_CONTAINER = "container";
	public static final String KEY_TITLE = "title";
	public static final String KEY_ABBREVIATION = "abbreviation";
	public static final String KEY_CODE = "code";
	public static final String KEY_DESCRIPTION = "description";
	public static final String KEY_LOCALE = "locale";
	public static final String KEY_CHARSET = "charset";
	public static final String KEY_CONTENT = "content";
	public static final String KEY_METADATA = "metaData";
	public static final String KEY_VERSION = "version";
	public static final String KEY_BOOKS_OFFSET = "booksOffset";
	public static final String KEY_CHAPTERS_OFFSET = "chaptersOffset";
	public static final String KEY_SECTIONS_OFFSET = "sectionsOffset";
	public static final String KEY_VERSES_OFFSET = "versesOffset";

	public static final String KEY_WORK = "work";
	public static final String KEY_BOOK = "book";
	public static final String KEY_CHAPTER = "chapter";
	public static final String KEY_SECTION = "section";
	public static final String KEY_VERSE = "verse";

	public static final String KEY_BYTE_START = "bStart";
	public static final String KEY_BYTE_LENGTH = "bLength";
	public static final String KEY_CHAR_START = "cStart";
	public static final String KEY_CHAR_LENGTH = "length";

	// column names refer to the parent containers
	public static final String KEY_WORK_CODE = "workCode";
	public static final String KEY_BOOK_ID = "bookId";
	public static final String KEY_CHAPTER_ID = "chapterId";
	public static final String KEY_SECTION_ID = "sectionId";

	// column names refer to the first sibling item for performance optimization
	// ?
	public static final String KEY_BOOK_COUNT = "bookCount";
	public static final String KEY_CHAPTER_COUNT = "chapterCount";
	public static final String KEY_SECTION_COUNT = "sectionCount";
	public static final String KEY_VERSE_COUNT = "verseCount";
	
	public static final String KEY_TIMESTAMP = "KEY_TIMESTAMP";
	public static final String KEY_ADDRESS = "KEY_ADDRESS";

	public final static Uri CONTENT_URI_WORKS = Uri.parse("content://"
			+ AUTHORITY + "/" + TABLE_WORKS);
	public final static Uri CONTENT_URI_BOOKS = Uri.parse("content://"
			+ AUTHORITY + "/" + TABLE_BOOKS);
	public final static Uri CONTENT_URI_CHAPTERS = Uri.parse("content://"
			+ AUTHORITY + "/" + TABLE_CHAPTERS);
	public final static Uri CONTENT_URI_SECTIONS = Uri.parse("content://"
			+ AUTHORITY + "/" + TABLE_SECTIONS);
	public final static Uri CONTENT_URI_VERSES = Uri.parse("content://"
			+ AUTHORITY + "/" + TABLE_VERSES);

	public final static Uri CONTENT_URI_BOOK = Uri.parse("content://"
			+ AUTHORITY + "/" + KEY_BOOK);
	public final static Uri CONTENT_URI_CHAPTER = Uri.parse("content://"
			+ AUTHORITY + "/" + KEY_CHAPTER);
	public final static Uri CONTENT_URI_SECTION = Uri.parse("content://"
			+ AUTHORITY + "/" + KEY_SECTION);
	public final static Uri CONTENT_URI_VERSE = Uri.parse("content://"
			+ AUTHORITY + "/" + KEY_VERSE);

	private static final String[] ALL_TABLES = { TABLE_WORKS, TABLE_BOOKS,
			TABLE_CHAPTERS, TABLE_SECTIONS, TABLE_VERSES };
	private static final HashMap<String, String[]> tableColumns = new HashMap<String, String[]>();

	public static final ArrayList<String> workCodes = new ArrayList<String>();
	public static final ArrayList<Work> workInstances = new ArrayList<Work>();
	// public static HashMap<String, Work> worksHashMap = null;
	public static Work primaryWork = null;
	public static Work secondaryWork = null;

	public static Context context = null;
	private static BibleSQLiteOpenHelper databaseHelper;
	private static SQLiteDatabase database;

	public static Work getWorkInstance(String code) {
		int index = workCodes.indexOf(code);
		if (index == -1)
			return null;

		Work work = workInstances.get(index);

		if (work != null && !work.isLoaded())
			loadBooksOf(work);

		return work;
	}

	// }} #endregion **/

	public static final String ACTION_START_DOWNLOAD = "ACTION_START_DOWNLOAD";
	public static final String ACTION_CANCEL_DONWLOAD = "ACTION_CANCEL_DONWLOAD";
	public static final String ACTION_CHANGE_DONWLOAD = "ACTION_CHANGE_DONWLOAD";
	
//	public static final String ACTION_IMPORT_DOWNLOADED_FILE = "ACTION_IMPORT_DOWNLOADED_FILE";
	public static final String ACTION_IMPORT_LOCAL_FILE = "ACTION_IMPORT_LOCAL_FILE";
	public static final String ACTION_REMOVE_WORK = "ACTION_REMOVE_WORK";
	public static final String ACTION_WORKS_CHANGED = "ACTION_WORKS_CHANGED";

	public static final String COLUMN_URI = "COLUMN_URI";
	public static final String COLUMN_TITLE = "COLUMN_TITLE";
	public static final String COLUMN_CODE = "COLUMN_CODE";
	public static final String COLUMN_LANGUAGE = "COLUMN_LANGUAGE";
	public static final String COLUMN_SIZE = "COLUMN_SIZE";
	public static final String COLUMN_VERSION = "COLUMN_VERSION";
	public static final String COLUMN_DELETE_ZIP = "COLUMN_DELETE_ZIP";
	
	public static final String COLUMN_CHANGE = "COLUMN_CHANGE";
	public static final String ADD_WORK = "ADD_WORK";
	public static final String REMOVE_WORK = "REMOVE_WORK";	
	
	DownloadManager downloadManager;
	SharedPreferences downloadPrefs;
	LocalBroadcastManager lbm;
	private HashMap<Long, DownloadTask> downloadTasks = null;

	BroadcastReceiver bibleChangedReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			Bundle bundle = intent.getExtras();
			if (bundle == null) {
				Log.e(TAG, "No Bundle attached!");
				return;
			}

			// */
			String uriString = bundle.getString(COLUMN_URI);
			String code, title, version;
			if (action.equals(ACTION_START_DOWNLOAD)){
				code = bundle.getString(COLUMN_CODE);
				title = bundle.getString(COLUMN_TITLE);
				version = bundle.getString(COLUMN_VERSION);
				
				if (workCodes.contains(code)) {
					Work existed = workInstances.get(workCodes.indexOf(code));
					Date existedVersion = Date.valueOf(existed.version);
					Date newVersion = Date.valueOf(version);
					if (existedVersion.after(newVersion)) {
						Log.w(TAG, String.format("Existed %s (%s) is already the latest build.",
								code, existedVersion));
						return;
					} else {
						Toast.makeText(context, "Please remove " + code + " first!", Toast.LENGTH_LONG).show();
						return;
					}
				} else if (DownloadTask.AllTasks.containsKey(code)){
					Log.w(TAG, "DownloadTask of " + code + " has already been added.");
					return;
				}
				
				Uri uri = Uri.parse(uriString);
				Request request = new DownloadManager.Request(uri);
				request.setAllowedNetworkTypes(Request.NETWORK_WIFI);
				request.setAllowedOverRoaming(false);

				request.setTitle(title);
				String filename = code + ".zip";
				request.setDescription(filename);
				request.setDestinationInExternalPublicDir(
						Environment.DIRECTORY_DOWNLOADS, filename);

				long id = downloadManager.enqueue(request);
				DownloadTask task = new DownloadTask(id, code, uriString);
				downloadTasks.put(id, task);
				
				if (downloadTasks.size() == 1) {
					IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
					context.registerReceiver(onDownloadCompletedReceiver, filter);
				}

				Intent downloadChangeIntent = new Intent(ACTION_CHANGE_DONWLOAD);
				lbm.sendBroadcast(downloadChangeIntent);
			} else if (action.equals(ACTION_CANCEL_DONWLOAD)) {
				code = bundle.getString(COLUMN_CODE);
				if (DownloadTask.AllTasks.containsKey(code)) {
					long id = -1;
					if (DownloadTask.AllTasks.containsKey(code)) {
						DownloadTask task = DownloadTask.AllTasks.get(code);
						id = task.id;
						DownloadTask.AllTasks.remove(code);
					} else {
						String v = bundle.getString(KEY_ID);
						id = Long.valueOf(v);
					}

					int result = downloadManager.remove(id);
					if (result != 1) {
						Log.e(TAG, "DownloadManager.remove " + id + " returns " + result);
						
						TaskStatus status = DownloadTask.queryDownloadStatus(id);
						
						if (status != null) {
							Log.w(TAG, status.toString());
						}
					} else {
						Log.d(TAG, "Task of " + code + " is removed.");
					}
				}
				lbm.sendBroadcast(new Intent(ACTION_CHANGE_DONWLOAD));
			}else if (action.equals(ACTION_REMOVE_WORK)) {
				try {
					code = bundle.getString(COLUMN_CODE);

					if (!removeBibleOf(code)) {
						Log.e(TAG, "Failed to remove work of " + code);
					}
					lbm.sendBroadcast(new Intent(ACTION_CHANGE_DONWLOAD));
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}				
			}
			else if (action.equals(ACTION_IMPORT_LOCAL_FILE)) {
				try {
					Uri fileUri = Uri.parse(uriString);

					if (importZippedBibles(fileUri)) {
						boolean deleteZipFile = bundle.getBoolean(COLUMN_DELETE_ZIP, false);
						if (deleteZipFile) {
							new File(uriString).delete();
						}
					}else {
						throw new Exception();
					}
				} catch (FileNotFoundException fe) {
					// TODO Auto-generated catch block
					fe.printStackTrace();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			// */
		}
	};

	// */
	BroadcastReceiver onDownloadCompletedReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {
				long downId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
				if (!downloadTasks.containsKey(downId))
					return;
				
				DownloadTask task = downloadTasks.get(downId);
                TaskStatus result = DownloadTask.queryDownloadStatus(downId);
                if (result != null) {
    				try {
    					Uri fileUri = Uri.parse(result.localUri);

    					if (importZippedBibles(fileUri)) {
    						new File(result.localUri).delete();
    					} else {
    						throw new Exception();
    					}
    				} catch (FileNotFoundException fe) {
    					fe.printStackTrace();
    				} catch (Exception e) {
    					e.printStackTrace();
    				}
                	int removedNum = downloadManager.remove(downId);
                	downloadTasks.remove(downId);
                	DownloadTask.AllTasks.remove(task.code);
                	Toast.makeText(context, String.format("Download of %s(%d) is imported.", task.code, downId), 
                			Toast.LENGTH_SHORT).show();  
                } else {
                	Log.w(TAG, "Failed to get the TaskResult of " + downId);
                }
			}
			
			if (downloadTasks == null || downloadTasks.size() == 0)	{
				context.unregisterReceiver(onDownloadCompletedReceiver);
			}
		}
	};

	// */

	@Override
	public boolean onCreate() {
		// */
		if (context != null) {
			return true;
		}

		context = this.getContext();

		downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
		downloadPrefs = context.getSharedPreferences("downloadtasks", Context.MODE_PRIVATE);
		
		lbm = LocalBroadcastManager.getInstance(context);
		// We are going to watch for interesting local broadcasts.
		IntentFilter filter = new IntentFilter();
		filter.addAction(ACTION_START_DOWNLOAD);
		filter.addAction(ACTION_CANCEL_DONWLOAD);
		filter.addAction(ACTION_IMPORT_LOCAL_FILE);
		filter.addAction(ACTION_REMOVE_WORK);
		lbm.registerReceiver(bibleChangedReceiver, filter);

		downloadTasks = getOngoingTasks();

		File file = context.getDatabasePath(BiblesContentProvider.DATABASE_NAME);
		if (!file.exists())
			extractBibles();

		databaseHelper = new BibleSQLiteOpenHelper(context);
		database = databaseHelper.getWritableDatabase();
		getColumnNames();

		if (OPEN_WORKS_AUTOMATICALLY) {
			preloadWorks();
		}

		if (OPEN_BOOKS_AUTOMATICALLY) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					for (Work work : workInstances) {
						if (!work.isLoaded()) {
							loadBooksOf(work);
						}
					}
				}
			}).start();
		}
		// */

		Environment.getExternalStoragePublicDirectory(
				Environment.DIRECTORY_DOWNLOADS).mkdirs();

		return true;
	}

	protected boolean importZippedBibles(Uri fileUri) {
		if (fileUri == null
				|| !fileUri.getLastPathSegment().endsWith(ZIP_SUFFIX))
			return false;

		String extension = MimeTypeMap.getFileExtensionFromUrl(fileUri
				.getPath());
		if (extension == null)
			return false;

		String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
				extension);
		if (type != "application/zip")
			return false;

		String path = fileUri.getPath();
		File file = new File(path);
		String szName;
		File dbFile = null, txtFile = null;
		try {
			InputStream inputStream = new FileInputStream(file);
			ZipInputStream inZip = new ZipInputStream(inputStream);
			ZipEntry zipEntry;

			byte[] buffer = new byte[1024];
			int readed = 0;
			FileOutputStream outputStream = null;

			while ((zipEntry = inZip.getNextEntry()) != null) {
				szName = zipEntry.getName();

				if (szName.endsWith(CONTENT_SUFFIX) && txtFile == null) {
					txtFile = context.getFileStreamPath(szName);
					outputStream = new FileOutputStream(txtFile);
					Log.i(TAG,
							"Extracting content: " + szName + "(size="
									+ zipEntry.getSize() + ", compressedSize="
									+ zipEntry.getCompressedSize() + ") to "
									+ txtFile.getAbsolutePath());

					while ((readed = inZip.read(buffer, 0, 1024)) > 0) {
						outputStream.write(buffer, 0, readed);
					}

					inZip.closeEntry();
					Log.i(TAG, "Extraction of content file - " + szName
							+ " is done.");
					outputStream.close();
				} else if (szName.endsWith(DATABASE_SUFFIX) && dbFile == null) {
					dbFile = new File(context.getCacheDir(), szName);

					outputStream = new FileOutputStream(dbFile);
					Log.i(TAG,
							"Extracting db to cache: " + szName + "(size="
									+ zipEntry.getSize() + ", compressedSize="
									+ zipEntry.getCompressedSize() + ") to "
									+ dbFile.getAbsolutePath());

					while ((readed = inZip.read(buffer, 0, 1024)) > 0) {
						outputStream.write(buffer, 0, readed);
					}

					inZip.closeEntry();
					Log.i(TAG, "Extraction" + szName + " done.");
					outputStream.close();

					mergeDatabase(dbFile);

					dbFile.delete();
				} else {
					throw new Exception("unexpected zipEntry of " + szName);
				}
			}// end of while

			inZip.close();
			inputStream.close();

			return true;
		} catch (IOException e) {
			Log.e(TAG, "read assets ERROR: " + e.toString());
			e.printStackTrace();
		} catch (Exception e) {
			Log.e(TAG, "read assets ERROR: " + e.toString());
			e.printStackTrace();
		}

		return false;
	}

	private void mergeDatabase(File dbFile) {
		SQLiteDatabase fromDatabase = null;
		Cursor cursor;
		String path = dbFile.getAbsolutePath();
		ArrayList<String> existedCodes = new ArrayList<String>();
		try {
			fromDatabase = SQLiteDatabase.openDatabase(path, null,
					SQLiteDatabase.NO_LOCALIZED_COLLATORS);

			if (!isSchemaValid(fromDatabase)) {
				Log.w(TAG,
						"Schema of the imported database is not matching with the existing one!");
				return;
			}

			existedCodes = getCodes("");
		} catch (SQLiteException e) {
			e.printStackTrace();
		} finally {
			if (fromDatabase != null && fromDatabase.isOpen())
				fromDatabase.close();
		}

		String sql;
		try {
			String attachDbString = String.format("ATTACH \'%s\' AS toMerge;",
					path);
			database.execSQL(attachDbString);

			// Get the table sequences of existing database
			int worksShift = 0, booksShift = 0, chaptersShift = 0, sectionsShift = 0, versesShift = 0;
			sql = String.format("SELECT * from toMerge.sqlite_sequence;");
			cursor = database.rawQuery(sql, null);
			while (cursor.moveToNext()) {
				String tableName = cursor.getString(0);
				if (tableName.compareTo(TABLE_BOOKS) == 0)
					booksShift = cursor.getInt(1);
				else if (tableName.compareTo(TABLE_CHAPTERS) == 0)
					chaptersShift = cursor.getInt(1);
				else if (tableName.compareTo(TABLE_SECTIONS) == 0)
					sectionsShift = cursor.getInt(1);
				else if (tableName.compareTo(TABLE_VERSES) == 0)
					versesShift = cursor.getInt(1);
			}
			cursor.close();

			sql = "SELECT * from sqlite_sequence;";
			cursor = database.rawQuery(sql, null);
			database.beginTransaction();

			while (cursor.moveToNext()) {
				String tablename = cursor.getString(0);
				int count = cursor.getInt(1);
				if (tablename.equals(TABLE_WORKS)) {
					worksShift = count;
					continue;
				} else if (tablename.equals(TABLE_BOOKS)) {
					booksShift = Math.max(booksShift, count);
					count = booksShift;
				} else if (tablename.equals(TABLE_CHAPTERS)) {
					chaptersShift = Math.max(chaptersShift, count);
					count = chaptersShift;
				} else if (tablename.equals(TABLE_SECTIONS)) {
					sectionsShift = Math.max(sectionsShift, count);
					count = sectionsShift;
				} else if (tablename.equals(TABLE_VERSES)) {
					versesShift = Math.max(versesShift, count);
					count = versesShift;
				}
				sql = String.format("UPDATE toMerge.%s SET %s=%s+%d;",
						tablename, KEY_ID, KEY_ID, count);
				// Shift the items in corresponding table by increasing KEY_ID
				// by count
				Log.i(TAG, "Shift the items in " + tablename + " by " + count);
				database.execSQL(sql);
			}
			sql = String
					.format("UPDATE toMerge.%s SET %s=%s+%d, %s=%s+%d, %s=%s+%d, %s=%s+%d;",
							TABLE_WORKS, KEY_BOOKS_OFFSET, KEY_BOOKS_OFFSET,
							booksShift, KEY_CHAPTERS_OFFSET,
							KEY_CHAPTERS_OFFSET, chaptersShift,
							KEY_SECTIONS_OFFSET, KEY_SECTIONS_OFFSET,
							sectionsShift, KEY_VERSES_OFFSET,
							KEY_VERSES_OFFSET, versesShift);
			database.execSQL(sql);
			cursor.close();

			ArrayList<String> toBeMergedCode = getCodes("toMerge");
			for (String code : toBeMergedCode) {
				if (!existedCodes.contains(code))
					importBible(code);
				else {
					Log.w(TAG, code + " is already existed, importing escaped!");
				}
			}

			database.setTransactionSuccessful();
		} catch (SQLiteException e) {
			e.printStackTrace();
		} finally {
			database.endTransaction();
		}

		String detachDbString = "DETACH toMerge;";
		database.execSQL(detachDbString);
	}

	private void importBible(String code) {
		// Get the relative items of the bible specified by the code
		String queryWorks = String
				.format("SELECT %s,%s,%s,%s,%s,%s,%s,%s from toMerge.%s where %s=\'%s\';",
						KEY_BOOKS_OFFSET, KEY_BOOK_COUNT, KEY_CHAPTERS_OFFSET,
						KEY_CHAPTER_COUNT, KEY_SECTIONS_OFFSET,
						KEY_SECTION_COUNT, KEY_VERSES_OFFSET, KEY_VERSE_COUNT,
						TABLE_WORKS, KEY_CODE, code);
		Cursor cursor = database.rawQuery(queryWorks, null);
		ArrayList<Integer> indexes = new ArrayList<Integer>();
		cursor.moveToFirst();
		for (int i = 0; i < 8; i++) {
			indexes.add(cursor.getInt(i));
		}
		cursor.close();

		// Batch copy of items related with work identified by code in table
		// books
		String sql = String
				.format("INSERT INTO %s SELECT * FROM toMerge.%s WHERE %s>%d LIMIT %d;",
						TABLE_BOOKS, TABLE_BOOKS, KEY_ID, indexes.get(0),
						indexes.get(1));
		Log.i(TAG, sql + " is executed.");
		database.execSQL(sql);

		// Batch copy of items related with work identified by code in table
		// chapters
		sql = String
				.format("INSERT INTO %s SELECT * FROM toMerge.%s WHERE %s>%d LIMIT %d;",
						TABLE_CHAPTERS, TABLE_CHAPTERS, KEY_ID, indexes.get(2),
						indexes.get(3));
		Log.i(TAG, sql + " is executed.");
		database.execSQL(sql);

		// Batch copy of items related with work identified by code in table
		// sections
		sql = String
				.format("INSERT INTO %s SELECT * FROM toMerge.%s WHERE %s>%d LIMIT %d;",
						TABLE_SECTIONS, TABLE_SECTIONS, KEY_ID, indexes.get(4),
						indexes.get(5));
		Log.i(TAG, sql + " is executed.");
		database.execSQL(sql);

		// Batch copy of items related with work identified by code in table
		// verses
		sql = String
				.format("INSERT INTO %s SELECT * FROM toMerge.%s WHERE %s>%d LIMIT %d;",
						TABLE_VERSES, TABLE_VERSES, KEY_ID, indexes.get(6),
						indexes.get(7));
		Log.i(TAG, sql + " is executed.");
		database.execSQL(sql);

		// Finally copy the item in table works
		sql = String.format(
				"INSERT INTO %s SELECT * FROM toMerge.%s WHERE %s=\'%s\';",
				TABLE_WORKS, TABLE_WORKS, KEY_CODE, code);
		Log.i(TAG, sql + " is executed.");
		database.execSQL(sql);
		
		Cursor resultCursor = database.query(TABLE_WORKS, null,
				KEY_CODE + "=?", new String[] {code}, 
				null, null, null, null);

		// Iterate over the cursors rows.
		while (resultCursor.moveToNext()) {
			String contentFileName = resultCursor.getString(Work.ContentIndex);
			File contentFile = context.getFileStreamPath(contentFileName);

			workCodes.add(code);
			workInstances.add(new Work(resultCursor, contentFile));
		}
		// Close the Cursor after using it
		resultCursor.close();
		
		Log.i(TAG, "Import of bible " + code + " is done.");
		
		Intent worksChangeIntent = new Intent(BiblesContentProvider.ACTION_WORKS_CHANGED);
		Bundle bundle = new Bundle();
		bundle.putString(COLUMN_CHANGE, ADD_WORK);
		bundle.putString(COLUMN_CODE, code);
		worksChangeIntent.putExtras(bundle);
		lbm.sendBroadcast(worksChangeIntent);
	}

	private ArrayList<String> getCodes(String dbName) {
		ArrayList<String> codes = new ArrayList<String>();
		Cursor cursor;
		String databasePrefix = (dbName == null || dbName == "") ? "" : (dbName
				.endsWith(".") ? dbName : dbName + ".");
		String sqlCode = String.format("SELECT %s FROM %s%s;", KEY_CODE,
				databasePrefix, TABLE_WORKS);
		cursor = database.rawQuery(sqlCode, null);
		while (cursor.moveToNext()) {
			String code = cursor.getString(0);
			codes.add(code);
		}
		cursor.close();
		return codes;
	}

	private static final String QUERY_TABLE_SCHEMA = "SELECT name,sql FROM sqlite_master where type=\'table\';";
	private static final String QUERY_TABLE_SCHEMA_BY_NAME = "SELECT sql FROM sqlite_master where name=\'?\';";

	private boolean isSchemaValid(SQLiteDatabase db) {
		HashMap<String, String> schemas = new HashMap<String, String>();
		Cursor cursor = db.rawQuery(QUERY_TABLE_SCHEMA, null);
		while (cursor.moveToNext()) {
			String name = cursor.getString(0);
			String schema = cursor.getString(1);
			schemas.put(name, schema);
		}
		cursor.close();

		// Confirm that all tables are included
		for (String tableName : ALL_TABLES) {
			if (!schemas.containsKey(tableName)) {
				Log.w(TAG, db.toString() + " has no table of " + tableName);
				return false;
			}

			String sqlString = String.format(
					"SELECT sql FROM sqlite_master WHERE name=\'%s\';",
					tableName);
			cursor = database.rawQuery(sqlString, null);
			cursor.moveToFirst();
			String schema = cursor.getString(0);
			cursor.close();
			if (schema.compareToIgnoreCase(schemas.get(tableName)) == 0)
				continue;

			sqlString = String.format("SELECT * FROM %s WHERE 0;", tableName);
			cursor = database.rawQuery(sqlString, null);
			List<String> columns = Arrays.asList(cursor.getColumnNames());
			cursor.close();

			String[] expected = tableColumns.get(tableName);
			if (columns.size() < expected.length) {
				Log.w(TAG, "Missing of columns of table " + tableName);
				return false;
			}

			for (int i = 0; i < expected.length; i++) {
				String clmn = expected[i];
				// if (!columns.contains(clmn))
				if (columns.get(i).compareTo(clmn) != 0) {
					Log.w(TAG, String.format("Column %s of %s is missing!",
							clmn, tableName));
					return false;
				}
			}
		}

		return true;
	}

	private boolean removeBibleOf(String code) {
		if (primaryWork != null && primaryWork.code.equals(code)) {
			Log.e(TAG, "Primary work cannot be removed: " + code);
			return false;
		} else if (secondaryWork != null && secondaryWork.code.equals(code)) {
			Log.e(TAG, "Secondary work cannot be removed: " + code);
			return false;
		}
		
		int workIndex = workCodes.indexOf(code);
		if (workIndex == -1) {
			Log.e(TAG, "Work doesnot exist for " + code);
			return false;
		}
		Work theWork = workInstances.get(workIndex);
		
		String sql;
		int first = -1, last = -1;
		database.beginTransaction();
		try {
			//Remove verse records
			first = theWork.versesOffset + 1;
			last = theWork.verseCount + theWork.versesOffset;
			String where = String.format("%s BETWEEN ? AND ?", KEY_ID);
			
			database.delete(TABLE_VERSES,  where, 
					new String[] { Integer.toString(first), Integer.toString(last)});
			
			//Remove section records if needed
			if (theWork.sectionCount != 0){
				first = theWork.sectionsOffset + 1;
				last = theWork.sectionCount + theWork.sectionsOffset;
				
				database.delete(TABLE_SECTIONS,  where, 
						new String[] { Integer.toString(first), Integer.toString(last)});

			}
				
			//Removal of chapter records
			first = theWork.chaptersOffset + 1;
			last = theWork.chaptersOffset + theWork.chapterCount;
			
			database.delete(TABLE_CHAPTERS,  where, 
					new String[] { Integer.toString(first), Integer.toString(last)});
			
			//Removal of book records
			first = theWork.booksOffset + 1;
			last = theWork.booksOffset + theWork.bookCount;
			
			database.delete(TABLE_BOOKS,  where, 
					new String[] { Integer.toString(first), Integer.toString(last)});
			
			//Removal of work record
			database.delete(TABLE_WORKS, KEY_CODE + "=?", new String[] {code});
			
			database.setTransactionSuccessful();
			
			theWork.contentFile.delete();			
			workCodes.remove(workIndex);
			workInstances.remove(workIndex);
			theWork = null;
			
			Intent worksChangeIntent = new Intent(BiblesContentProvider.ACTION_WORKS_CHANGED);
			Bundle bundle = new Bundle();
			bundle.putString(COLUMN_CHANGE, REMOVE_WORK);
			bundle.putString(COLUMN_CODE, code);
			worksChangeIntent.putExtras(bundle);
			lbm.sendBroadcast(worksChangeIntent);
		} catch (SQLiteException e) {
			e.printStackTrace();
		} finally {
			database.endTransaction();
		}
			
		return false;
	}
	
	private HashMap<Long, DownloadTask> getOngoingTasks() {
		HashMap<Long, DownloadTask> result = new HashMap<Long, DownloadTask>();
		if (downloadPrefs == null)
			return result;
		
		String stringSet = downloadPrefs.getString(KEY_ID, "");
		if (stringSet.length() == 0)
			return result;
		
		String[] ids = stringSet.split(",");
		
		int idSize = ids.length;
		
		for (int i = 0; i < idSize; i ++) 
		{
			Long id = Long.valueOf(ids[i]);
			TaskStatus status = DownloadTask.queryDownloadStatus(id);
			
			DownloadTask task = new DownloadTask(id, status.title, status.uri, status.lastModified);
			result.put(id, task);
			
			if (status.statusCode == DownloadManager.STATUS_SUCCESSFUL){
				Intent downloadCompleted = new Intent(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
				downloadCompleted.putExtra(DownloadManager.EXTRA_DOWNLOAD_ID, id);
				context.sendBroadcast(downloadCompleted);				
			}
		}
		
		if (result.size() != 0) {
			IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
			context.registerReceiver(onDownloadCompletedReceiver, filter);
		}

		return result;
	}
	
	public void saveOngoingTasks() {
		SharedPreferences.Editor editor = downloadPrefs.edit();
		
		String s = "";
		if (downloadTasks.size() > 0) {
			StringBuilder sb = new StringBuilder();
			for (Long id : downloadTasks.keySet()) {
				sb.append(id);
				sb.append(",");
			}
			s = sb.substring(0, sb.length()-1);
		}
		editor.putString(KEY_ID, s);

		editor.commit();
	}

	// @Override
	@Override
	public void shutdown() {
		// super.shutdown();
		saveOngoingTasks();

		if (lbm != null)
			lbm.unregisterReceiver(bibleChangedReceiver);
	}

	private static void getColumnNames() {
		Work.getTableMapping().Mapping(database);
		Book.getTableMapping().Mapping(database);
		Chapter.getTableMapping().Mapping(database);
		Section.getTableMapping().Mapping(database);
		Verse.getTableMapping().Mapping(database);

		tableColumns.put(Work.TABLENAME, Work.COLUMN_NAMES);
		tableColumns.put(Book.TABLENAME, Book.COLUMN_NAMES);
		tableColumns.put(Chapter.TABLENAME, Chapter.COLUMN_NAMES);
		tableColumns.put(Section.TABLENAME, Section.COLUMN_NAMES);
		tableColumns.put(Verse.TABLENAME, Verse.COLUMN_NAMES);

		// String sql = "SELECT * from ? WHERE 0";
		// Cursor c = null;
		//
		// c = database.rawQuery("SELECT * from works WHERE 0", null);
		// Work.getTableIndexes(c);
		// c.close();
		// c = database.rawQuery("SELECT * from books WHERE 0", null);
		// Book.getTableIndexes(c);
		// c.close();
		// c = database.rawQuery("SELECT * from chapters WHERE 0", null);
		// Chapter.getTableIndexes(c);
		// c.close();
		// c = database.rawQuery("SELECT * from sections WHERE 0", null);
		// Section.getTableIndexes(c);
		// c.close();
		// c = database.rawQuery("SELECT * from verses WHERE 0", null);
		// Verse.getTableIndexes(c);
		// c.close();
	}

	public static void preloadWorks() {
		workCodes.clear();
		workInstances.clear();

		String sql = String.format("SELECT * from %s;", TABLE_WORKS);
		Cursor resultCursor = database.rawQuery(sql, null);

		// Iterate over the cursors rows.
		while (resultCursor.moveToNext()) {
			String code = resultCursor.getString(Work.CodeIndex);
			String contentFileName = resultCursor.getString(Work.ContentIndex);
			File contentFile = context.getFileStreamPath(contentFileName);

			workCodes.add(code);
			workInstances.add(new Work(resultCursor, contentFile));
		}
		// Close the Cursor when you鈥檝e finished with it.
		resultCursor.close();

		if (primaryWork == null && workCodes.size() > 0) {
			if (primaryWork == null)
				primaryWork = workInstances.get(0);
		}
	}

	public static void loadWorks(String[] codes) {
		if (codes != null && codes.length > 0) {
			Work theWork = getWorkInstance(codes[0]);
			if (theWork != null && primaryWork != theWork)
				primaryWork = theWork;

			if (codes.length > 1) {
				theWork = getWorkInstance(codes[1]);
				if (secondaryWork != theWork)
					secondaryWork = theWork;
			}
		}
	}

	protected static void loadBooksOf(Work work) {
		if (work.isLoaded()) {
			return;
		}

		Cursor bookCursor = null;
		try {
			// Content content = new Content(work.contentFile, work.charset);
			work.loadContent();

			EnumMap<BookEnum, Book> books = new EnumMap<BookEnum, Book>(
					BookEnum.class);

			int bookCount = work.bookCount;
			String selection = String.format("%s > ? AND %s <= ?",
					BiblesContentProvider.KEY_ID, BiblesContentProvider.KEY_ID);
			String[] selectionArgs = new String[] {
					String.valueOf(work.booksOffset),
					String.valueOf(work.booksOffset + bookCount) };
			String sortOrder = String.format("%s ASC",
					BiblesContentProvider.KEY_BOOK);
			// String sortOrder = String.format("%s ASC LIMIT %s OFFSET %s",
			// BiblesContentProvider.KEY_BOOK,
			// bookCount, booksOffset);

			bookCursor = getBooks(BiblesContentProvider.CONTENT_URI_BOOKS,
					null, selection, selectionArgs, sortOrder);
			int cursorCount = bookCursor.getCount();
			if (cursorCount != bookCount) {
				Log.e(TAG,
						"Failed to open books according to the natual order expected in table books.");
				selection = String.format("%s = ?",
						BiblesContentProvider.KEY_WORK_CODE);
				selectionArgs = new String[] { work.code };
				bookCursor = getBooks(BiblesContentProvider.CONTENT_URI_BOOKS,
						null, selection, selectionArgs, sortOrder);

				cursorCount = bookCursor.getCount();
				if (cursorCount != bookCount) {
					throw new SQLException(String.format(
							"%d books are expected instead of %d", bookCount,
							cursorCount));
				}

				Log.w(TAG,
						"Succeeded in open books. However, the books are not stored as expected for work "
								+ work.code);
			}

			Book theBook;
			while (bookCursor.moveToNext()) {
				int bookNum = bookCursor.getInt(Book.Book_Index);
				theBook = new Book(bookCursor, work);
				BookEnum bookEnum = BookEnum.fromOrdinal(bookNum);
				books.put(bookEnum, theBook);
			}
			work.setBooks(books);

		}
		// catch (FileNotFoundException e)
		// {
		// e.printStackTrace();
		// Log.e(TAG, "Failed to load content file: " + work.contentFile);
		// }
		catch (SQLException e) {
			e.printStackTrace();
			Log.e(TAG, e.getMessage());
		} finally {
			if ((bookCursor != null) && !bookCursor.isClosed()) {
				bookCursor.close();
				bookCursor = null;
			}
		}
	}

	protected static void loadChaptersOf(Book book) {
		if (book.isLoaded()) {
			return;
		}

		Cursor chapterCursor = null;
		int chapterCount = book.chapterCount;
		Work work = book.work;
		try {
			Chapter[] chapters = new Chapter[chapterCount];

			String selection = String.format("%s > ? AND %s <= ?",
					BiblesContentProvider.KEY_ID, BiblesContentProvider.KEY_ID);
			String[] selectionArgs = new String[] {
					String.valueOf(book.chaptersOffset + work.chaptersOffset),
					String.valueOf(book.chaptersOffset + work.chaptersOffset
							+ chapterCount) };
			String sortOrder = String.format("%s ASC",
					BiblesContentProvider.KEY_CHAPTER);

			chapterCursor = getChapters(
					BiblesContentProvider.CONTENT_URI_CHAPTERS, null,
					selection, selectionArgs, sortOrder);

			int cursorCount = chapterCursor.getCount();
			if (cursorCount != chapterCount) {
				Log.e(TAG,
						"Failed to open "
								+ chapterCount
								+ " chapters according to the natual order expected in table chapters.");
				selection = String.format("%s = ?",
						BiblesContentProvider.KEY_BOOK_ID);
				selectionArgs = new String[] { String.valueOf(book.id) };
				chapterCursor = getChapters(
						BiblesContentProvider.CONTENT_URI_CHAPTERS, null,
						selection, selectionArgs, sortOrder);

				cursorCount = chapterCursor.getCount();

				/*
				 * / // TODO: remove the validation in future if (chapterCount
				 * != cursorCount) { throw new SQLException(String.format(
				 * "%d chapters are expected instead of %d", chapterCount,
				 * cursorCount)); }
				 * 
				 * Log.w(TAG,
				 * "Succeeded in open chapters. However, the chapters are not stored as expected for book "
				 * + book.title);
				 * 
				 * //
				 */
			}

			Chapter theChapter;
			BookEnum expectedBook = book.bookEnum;
			while (chapterCursor.moveToNext()) {
				// int bookId = chapterCursor.getInt(Chapter.BookId_Index);
				// int chapterNum = chapterCursor.getInt(Chapter.Chapter_Index);

				theChapter = new Chapter(chapterCursor, book);
				int chapterNum = theChapter.chapterNum;

				if (!theChapter.bookEnum.equals(expectedBook)) {
					throw new IndexOutOfBoundsException(String.format(
							"Chapter %d of book %s doesn't belong to %s",
							chapterNum, theChapter.bookEnum, expectedBook));
				} else if (chapters[chapterNum - 1] != null) {
					throw new Exception(
							"Chapter "
									+ chapterNum
									+ " has been loaded! Checking for duplicated data in table chapters!");
				}

				chapters[chapterNum - 1] = theChapter;
			}

			book.setChapters(chapters);
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(TAG, e.getMessage());
		} finally {
			if (chapterCursor != null && !chapterCursor.isClosed()) {
				chapterCursor.close();
			}
		}
	}

	protected static void loadContentOf(Chapter chapter) {
		// To avoid loading content when it is already loaded
		if (chapter.isLoaded()) {
			return;
		}

		Cursor cursor = null;
		int sectionCount = chapter.sectionCount;
		int verseCount = chapter.verseCount;
		Work work = chapter.work;
		try {
			String text = work.textOf(chapter.byteStart, chapter.byteLength);
			chapter.setText(text);

			// Begin to load verses
			Verse[] verses = new Verse[verseCount];

			String selection = String.format("%s > ? AND %s <= ?",
					BiblesContentProvider.KEY_ID, BiblesContentProvider.KEY_ID);
			String[] selectionArgs = new String[] {
					String.valueOf(chapter.versesOffset + work.versesOffset),
					String.valueOf(chapter.verseCount + chapter.versesOffset
							+ work.versesOffset) };
			String sortOrder = String.format("%s ASC",
					BiblesContentProvider.KEY_VERSE);

			cursor = getVerses(BiblesContentProvider.CONTENT_URI_VERSES, null,
					selection, selectionArgs, sortOrder);

			int cursorCount = cursor.getCount();
			if (cursorCount != verseCount) {
				cursor.close();
				Log.e(TAG,
						String.format(
								"Failed to query %d verses when %d are stored as expected in Table verses.",
								verseCount, cursorCount));
				selection = String.format("%s = ?",
						BiblesContentProvider.KEY_CHAPTER_ID);
				selectionArgs = new String[] { String.valueOf(chapter.id) };
				cursor = getVerses(BiblesContentProvider.CONTENT_URI_VERSES,
						null, selection, selectionArgs, sortOrder);

				cursorCount = cursor.getCount();
				if (cursorCount != verseCount) {
					throw new SQLException(String.format(
							"%d verses are expected instead of %d", verseCount,
							cursorCount));
				}
				Log.w(TAG,
						"Succeeded to get verses when they are not stored as expected for chapter "
								+ chapter.chapterNum);
			}

			Verse theVerse;
			while (cursor.moveToNext()) {
				theVerse = new Verse(cursor, chapter);

				if (theVerse.chapterId != chapter.id - work.chaptersOffset) {
					throw new IndexOutOfBoundsException(
							String.format(
									"Verse %d whose chapterId=%s doesn't belong to Chapter of id = %s",
									theVerse.verseNum, theVerse.chapterId,
									chapter.id));
				} else if (verses[theVerse.verseNum - 1] != null) {
					throw new Exception(
							"Verse "
									+ theVerse.verseNum
									+ " has been loaded! Checking for duplicated data in table verses!");
				}

				verses[theVerse.verseNum - 1] = theVerse;
			}
			cursor.close();

			Section[] sections = new Section[sectionCount];

			if (sectionCount != 0) {
				selection = String.format("%s > ? AND %s <= ?",
						BiblesContentProvider.KEY_ID,
						BiblesContentProvider.KEY_ID);
				selectionArgs = new String[] {
						String.valueOf(chapter.sectionsOffset
								+ work.sectionsOffset),
						String.valueOf(chapter.sectionCount
								+ chapter.sectionsOffset + work.sectionsOffset) };
				sortOrder = String.format("%s ASC",
						BiblesContentProvider.KEY_SECTION);

				cursor = getSections(
						BiblesContentProvider.CONTENT_URI_SECTIONS, null,
						selection, selectionArgs, sortOrder);

				cursorCount = cursor.getCount();
				if (cursorCount != sectionCount) {
					cursor.close();
					Log.e(TAG,
							String.format(
									"Failed to query %d sections are expected instead of %d in Table sections.",
									sectionCount, cursorCount));
					selection = String.format("%s = ?",
							BiblesContentProvider.KEY_CHAPTER_ID);
					selectionArgs = new String[] { String.valueOf(chapter.id) };
					cursor = getSections(
							BiblesContentProvider.CONTENT_URI_SECTIONS, null,
							selection, selectionArgs, sortOrder);

					cursorCount = cursor.getCount();
					if (cursorCount != sectionCount) {
						throw new SQLException(String.format(
								"%d sections are expected instead of %d",
								sectionCount, cursorCount));
					}

					Log.w(TAG,
							"Succeeded to get sections when they are not stored as expected for chapter "
									+ chapter.chapterNum);
				}

				Section theSection;
				while (cursor.moveToNext()) {
					theSection = new Section(cursor, chapter);

					if (theSection.chapterId != chapter.id) {
						throw new IndexOutOfBoundsException(
								String.format(
										"Section %d of chapterId=%s doesn't belong to Chapter %s",
										theSection.sectionNum,
										theSection.chapterId, chapter.id));
					} else if (sections[theSection.sectionNum - 1] != null) {
						throw new Exception(
								"Section "
										+ theSection.sectionNum
										+ " has been loaded! Checking for duplicated data in table sections!");
					}

					sections[theSection.sectionNum - 1] = theSection;
				}
				cursor.close();
			}

			chapter.setSiblings(sections, verses);
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(TAG, e.getMessage());
			chapter.setSiblings(null, null);
		}
	}

	protected static int chapterIdOfVerse(int verseId) {
		Cursor cursor = null;
		try {
			String selection = String.format("%s == ?",
					BiblesContentProvider.KEY_ID);
			String[] selectionArgs = new String[] { String.valueOf(verseId) };

			cursor = getVerseById(BiblesContentProvider.CONTENT_URI_VERSES,
					null, selection, selectionArgs, null);

			if ((cursor != null) && cursor.moveToNext()) {
				int chapterId = cursor.getInt(Verse.ChapterId_Index);
				return chapterId;
			}

			return -1;
		} catch (Exception e) {
			Log.e(TAG, "Failed to get the chapterId of verse whose id="
					+ verseId);
			e.printStackTrace();
			Log.e(TAG, e.getMessage());
			return -1;
		}
	}

	/**
	 * Extracts and copies your database from the embedded assets-folder to the
	 * default database folder, from where it can be accessed and handled. This
	 * is done by transferring bytestream.
	 * */
	private static void extractBibles() {
		// To get the DatabasePath, first get the path of a file within it, then
		// get this file's parent path.
		File file = context.getDatabasePath(".");
		file = new File(file.getParent());
		if (file.exists()) {
			File[] filelist = file.listFiles();

			if (filelist.length > 0) {
				// Log.i(TAG, "Extraction cancelled: " + file.getAbsolutePath()
				// + " exists already.");
			}
			return;
		}

		file.mkdir();

		AssetManager assetManager = context.getResources().getAssets();
		String zipFileName = null;
		String szName = "";
		try {
			String[] files = assetManager.list("");
			FileOutputStream outputStream = null;
			InputStream inputStream = null;

			byte[] buffer = new byte[1024];
			int readed = 0;
			
			for (String filename : files) {
				if (filename.contains(ZIP_SUFFIX) && zipFileName == null) {
					zipFileName = filename;
					break;
				} 
			}

			if (zipFileName == null) {
				Log.w(TAG, "Failed to find the manifest file within the assets");
				return;
			}

			inputStream = assetManager.open(zipFileName);
			ZipInputStream inZip = new ZipInputStream(inputStream);
			ZipEntry zipEntry;
			
			String extracting = context.getResources().getString(com.alouder.bibles.R.string.extracting);

			while ((zipEntry = inZip.getNextEntry()) != null) {
				szName = zipEntry.getName();

				if (szName.contains(BiblesContentProvider.DATABASE_NAME)
						|| szName.endsWith(DATABASE_SUFFIX)) {
					file = context
							.getDatabasePath(BiblesContentProvider.DATABASE_NAME);
				}else {
					file = context.getFileStreamPath(szName);
				}

				Toast.makeText(context, extracting + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
				outputStream = new FileOutputStream(file);
				Log.i(TAG,
						"Extracting: " + szName + "(size=" + zipEntry.getSize()
								+ ", compressedSize="
								+ zipEntry.getCompressedSize() + ") to "
								+ file.getAbsolutePath());

				while ((readed = inZip.read(buffer, 0, 1024)) > 0) {
					outputStream.write(buffer, 0, readed);
				}

				inZip.closeEntry();
				Log.i(TAG, "Extraction" + szName + " done.");
				outputStream.close();
			}// end of while

			inZip.close();
			inputStream.close();
		} catch (IOException e) {
			Log.e(TAG, "read assets ERROR: " + e.toString());
			e.printStackTrace();
		}
	}

	// #region UriMatcher and getType() handlers and definition

	// */ Codes to return when URI is matched
	private static final int BIBLE_ALL = 10;
	private static final int BIBLE_ITEM = 11;

	private static final int BIBLE_SEARCH = 100;

	private static final int SEVERAL_WORKS = 201;
	private static final int SEVERAL_BOOKS = 301;
	private static final int SEVERAL_CHAPTERS = 401;
	private static final int SEVERAL_SECTIONS = 501;
	private static final int SEVERAL_VERSES = 601;

	private static final int WORK_BY_CODE = 1001;
	private static final int BOOK_BY_ID = 1002;
	private static final int CHAPTER_BY_ID = 1003;
	private static final int SECTION_BY_ID = 1004;
	private static final int VERSE_BY_ID = 1005;
	// */

	private static final UriMatcher uriMatcher;
	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(AUTHORITY, "/", BIBLE_ALL);
		uriMatcher.addURI(AUTHORITY, "/*", BIBLE_ALL);

		uriMatcher.addURI(AUTHORITY, TABLE_WORKS, SEVERAL_WORKS);
		uriMatcher.addURI(AUTHORITY, TABLE_BOOKS, SEVERAL_BOOKS);
		uriMatcher.addURI(AUTHORITY, TABLE_CHAPTERS, SEVERAL_CHAPTERS);
		uriMatcher.addURI(AUTHORITY, TABLE_SECTIONS, SEVERAL_SECTIONS);
		uriMatcher.addURI(AUTHORITY, TABLE_VERSES, SEVERAL_VERSES);

		uriMatcher.addURI(AUTHORITY, KEY_WORK + "/*", WORK_BY_CODE);
		uriMatcher.addURI(AUTHORITY, KEY_BOOK + "/#", BOOK_BY_ID);
		uriMatcher.addURI(AUTHORITY, KEY_CHAPTER + "/#", CHAPTER_BY_ID);
		uriMatcher.addURI(AUTHORITY, KEY_SECTION + "/#", SECTION_BY_ID);
		uriMatcher.addURI(AUTHORITY, KEY_VERSE + "/#", VERSE_BY_ID);

		uriMatcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY,
				BIBLE_SEARCH);
		uriMatcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY
				+ "/*", BIBLE_SEARCH);
		uriMatcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_SHORTCUT,
				BIBLE_SEARCH);
		uriMatcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_SHORTCUT
				+ "/*", BIBLE_SEARCH);
	}

	// #endregion
	@Override
	public String getType(Uri uri) {
		switch (uriMatcher.match(uri)) {
		case BIBLE_SEARCH:
			return SearchManager.SUGGEST_MIME_TYPE;

		case BIBLE_ITEM:
			return ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.aloudbible."
					+ TABLE_VERSES;

		case BIBLE_ALL:
		case SEVERAL_WORKS:
			return ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.aloudbible."
					+ TABLE_WORKS;
		case SEVERAL_BOOKS:
			return ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.aloudbible."
					+ TABLE_BOOKS;
		case SEVERAL_CHAPTERS:
			return ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.aloudbible."
					+ TABLE_CHAPTERS;
		case SEVERAL_SECTIONS:
			return ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.aloudbible."
					+ TABLE_SECTIONS;
		case SEVERAL_VERSES:
			return ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.aloudbible."
					+ TABLE_VERSES;

		case WORK_BY_CODE:
			return ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.aloudbible."
					+ KEY_WORK;
		case BOOK_BY_ID:
			return ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.aloudbible."
					+ KEY_BOOK;
		case CHAPTER_BY_ID:
			return ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.aloudbible."
					+ KEY_CHAPTER;
		case SECTION_BY_ID:
			return ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.aloudbible."
					+ KEY_SECTION;
		case VERSE_BY_ID:
			return ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.aloudbible."
					+ KEY_VERSE;
		default:
			throw new IllegalArgumentException("Unsupported URI: " + uri);
		}
	}

	// #region query() handlers
	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		switch (uriMatcher.match(uri)) {
		case BIBLE_SEARCH:
		case BIBLE_ITEM:
			return null;

		case BIBLE_ALL:
		case SEVERAL_WORKS:
			return getWorks(uri, projection, selection, selectionArgs,
					sortOrder);
		case SEVERAL_BOOKS:
			return getBooks(uri, projection, selection, selectionArgs,
					sortOrder);
		case SEVERAL_CHAPTERS:
			return getChapters(uri, projection, selection, selectionArgs,
					sortOrder);
		case SEVERAL_SECTIONS:
			return getSections(uri, projection, selection, selectionArgs,
					sortOrder);
		case SEVERAL_VERSES:
			return getVerses(uri, projection, selection, selectionArgs,
					sortOrder);

		case WORK_BY_CODE:
			return getEditionByCode(uri, projection, selection, selectionArgs,
					sortOrder);
		case BOOK_BY_ID:
			return getBookById(uri, projection, selection, selectionArgs,
					sortOrder);
		case CHAPTER_BY_ID:
			return getChapterById(uri, projection, selection, selectionArgs,
					sortOrder);
		case SECTION_BY_ID:
			return getSectionById(uri, projection, selection, selectionArgs,
					sortOrder);
		case VERSE_BY_ID:
			return getVerseById(uri, projection, selection, selectionArgs,
					sortOrder);
		default:
//			throw new IllegalArgumentException("Unsupported URI: " + uri);
			return null;
		}
	}

	private static Cursor getWorks(Uri uri, String[] projection,
			String selection, String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
		queryBuilder.setTables(TABLE_WORKS);

		Cursor cursor = queryBuilder.query(database, projection, selection,
				selectionArgs, null, null, sortOrder);
		return cursor;
	}

	private static Cursor getBooks(Uri uri, String[] projection,
			String selection, String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
		queryBuilder.setTables(TABLE_BOOKS);

		Cursor cursor = queryBuilder.query(database, projection, selection,
				selectionArgs, null, null, sortOrder);
		return cursor;
	}

	private static Cursor getChapters(Uri uri, String[] projection,
			String selection, String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
		queryBuilder.setTables(TABLE_CHAPTERS);

		Cursor cursor = queryBuilder.query(database, projection, selection,
				selectionArgs, null, null, sortOrder);
		return cursor;
	}

	private static Cursor getSections(Uri uri, String[] projection,
			String selection, String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
		queryBuilder.setTables(TABLE_SECTIONS);

		Cursor cursor = queryBuilder.query(database, projection, selection,
				selectionArgs, null, null, sortOrder);
		return cursor;

	}

	private static Cursor getVerses(Uri uri, String[] projection,
			String selection, String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
		queryBuilder.setTables(TABLE_VERSES);

		Cursor cursor = queryBuilder.query(database, projection, selection,
				selectionArgs, null, null, sortOrder);
		return cursor;
	}

	// */ Helper functions to get single item directly from a specific table
	private static Cursor getEditionByCode(Uri uri, String[] projection,
			String selection, String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
		queryBuilder.setTables(TABLE_WORKS);

		String code = uri.getPathSegments().get(1);
		queryBuilder.appendWhere(KEY_CODE + "=" + code);

		Cursor cursor = queryBuilder.query(database, projection, selection,
				selectionArgs, null, null, sortOrder);
		return cursor;
	}

	private static Cursor getItemById(String tableName, Uri uri,
			String[] projection, String selection, String[] selectionArgs,
			String sortOrder) {
		// Replace these with valid SQL statements if necessary.
		// String groupBy = null;
		// String having = null;
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
		queryBuilder.setTables(tableName);

		String rowID = uri.getPathSegments().get(1);
		queryBuilder.appendWhere(KEY_ID + "=" + rowID);

		Cursor cursor = queryBuilder.query(database, projection, selection,
				selectionArgs, null, null, sortOrder);
		return cursor;
	}

	private static Cursor getBookById(Uri uri, String[] projection,
			String selection, String[] selectionArgs, String sortOrder) {
		return getItemById(TABLE_BOOKS, uri, projection, selection,
				selectionArgs, sortOrder);
	}

	private static Cursor getChapterById(Uri uri, String[] projection,
			String selection, String[] selectionArgs, String sortOrder) {
		return getItemById(TABLE_CHAPTERS, uri, projection, selection,
				selectionArgs, sortOrder);
	}

	private static Cursor getSectionById(Uri uri, String[] projection,
			String selection, String[] selectionArgs, String sortOrder) {
		return getItemById(TABLE_CHAPTERS, uri, projection, selection,
				selectionArgs, sortOrder);
	}

	private static Cursor getVerseById(Uri uri, String[] projection,
			String selection, String[] selectionArgs, String sortOrder) {
		return getItemById(TABLE_VERSES, uri, projection, selection,
				selectionArgs, sortOrder);
	}

	// */ End of Helper functions to get single item directly from a specific
	// table

	// #endregion

	// #region Other override methods

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

	// #endregion

	private static class BibleSQLiteOpenHelper extends SQLiteOpenHelper {
		// #region SQL commands to create tables within the Database
		// SQL commands to create table works
		private static final String CREATE_TABLE_WORKS = "CREATE TABLE IF NOT EXISTS "
				+ TABLE_WORKS
				+ " ("
				// + KEY_ID
				// + " INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ KEY_CODE
				+ " TEXT PRIMARY KEY, "
				+ KEY_WORK
				+ " TEXT, "
				+ KEY_VERSION
				+ " TEXT, "
				+ KEY_CHARSET
				+ " TEXT, "
				+ KEY_LOCALE
				+ " TEXT, "
				+ KEY_CONTENT
				+ " TEXT NOT NULL, "
				+ KEY_DESCRIPTION
				+ " TEXT, "
				+ KEY_METADATA
				+ " TEXT, "
				+ KEY_BOOKS_OFFSET
				+ " INTEGER NOT NULL, "
				+ KEY_CHAPTERS_OFFSET
				+ " INTEGER NOT NULL, "
				+ KEY_SECTIONS_OFFSET
				+ " INTEGER NOT NULL, "
				+ KEY_VERSES_OFFSET
				+ " INTEGER NOT NULL, "
				+ KEY_BOOK_COUNT
				+ " INTEGER NOT NULL, "
				+ KEY_CHAPTER_COUNT
				+ " INTEGER NOT NULL, "
				+ KEY_SECTION_COUNT
				+ " INTEGER NOT NULL, "
				+ KEY_VERSE_COUNT
				+ " INTEGER NOT NULL, "
				+ KEY_BYTE_LENGTH
				+ " INTEGER NOT NULL, "
				+ KEY_CHAR_LENGTH
				+ " INTEGER NOT NULL"
				+ ");";

		// SQL commands to create table books
		private static final String CREATE_TABLE_BOOKS = "CREATE TABLE IF NOT EXISTS "
				+ TABLE_BOOKS
				+ " ("
				+ KEY_ID
				+ " INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ KEY_WORK_CODE
				+ " TEXT NOT NULL, "
				+ KEY_BOOK
				+ " INTEGER NOT NULL, "
				+ KEY_TITLE
				+ " TEXT NOT NULL, "
				+ KEY_ABBREVIATION
				+ " TEXT NOT NULL, "
				+ KEY_METADATA
				+ " TEXT, "
				+ KEY_CHAPTERS_OFFSET
				+ " INTEGER NOT NULL, "
				+ KEY_CHAPTER_COUNT
				+ " INTEGER NOT NULL, "
				+ KEY_SECTION_COUNT
				+ " INTEGER, "
				+ KEY_VERSE_COUNT
				+ " INTEGER NOT NULL, "
				+ KEY_BYTE_START
				+ " INTEGER NOT NULL, "
				+ KEY_BYTE_LENGTH
				+ " INTEGER NOT NULL, "
				+ KEY_CHAR_START
				+ " INTEGER, "
				+ KEY_CHAR_LENGTH
				+ " INTEGER NOT NULL, "
				+ " FOREIGN KEY("
				+ KEY_WORK_CODE
				+ ") REFERENCES "
				+ TABLE_WORKS
				+ "("
				+ KEY_CODE + ")" + ");";

		// SQL commands to create table chapters
		private static final String CREATE_TABLE_CHAPTERS = "CREATE TABLE IF NOT EXISTS "
				+ TABLE_CHAPTERS
				+ " ("
				+ KEY_ID
				+ " INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ KEY_BOOK_ID
				+ " INTEGER NOT NULL, "
				+ KEY_CHAPTER
				+ " INTEGER NOT NULL, "
				+ KEY_SECTIONS_OFFSET
				+ " INTEGER, "
				+ KEY_VERSES_OFFSET
				+ " INTEGER, "
				+ KEY_SECTION_COUNT
				+ " INTEGER, "
				+ KEY_VERSE_COUNT
				+ " INTEGER, "
				+ KEY_BYTE_START
				+ " INTEGER NOT NULL, "
				+ KEY_BYTE_LENGTH
				+ " INTEGER NOT NULL, "
				+ KEY_CHAR_START
				+ " INTEGER, "
				+ KEY_CHAR_LENGTH
				+ " INTEGER NOT NULL, "
				+ " FOREIGN KEY("
				+ KEY_BOOK_ID
				+ ") REFERENCES "
				+ TABLE_BOOKS
				+ "("
				+ KEY_ID
				+ ")" + ");";

		// SQL commands to create table verses
		private static final String CREATE_TABLE_VERSES = "CREATE TABLE IF NOT EXISTS "
				+ TABLE_VERSES
				+ " ("
				+ KEY_ID
				+ " INTEGER PRIMARY KEY AUTOINCREMENT, "
				// + KEY_BOOK
				// + " INTEGER NOT NULL, "
				+ KEY_CHAPTER_ID
				+ " INTEGER NOT NULL, "
				+ KEY_VERSE
				+ " INTEGER NOT NULL, "
				+ KEY_BYTE_START
				+ " INTEGER NOT NULL, "
				+ KEY_BYTE_LENGTH
				+ " INTEGER NOT NULL, "
				+ KEY_CHAR_START
				+ " INTEGER, "
				+ KEY_CHAR_LENGTH
				+ " INTEGER, "
				// + " FOREIGN KEY("
				// + KEY_BOOK
				// + ") REFERENCES " + TABLE_BOOKS
				// + "(" + KEY_ID + "),"
				+ " FOREIGN KEY("
				+ KEY_CHAPTER_ID
				+ ") REFERENCES "
				+ TABLE_CHAPTERS + "(" + KEY_ID + ")" + ");";

		// SQL commands to create table sections
		private static final String CREATE_TABLE_SECTIONS = "CREATE TABLE IF NOT EXISTS "
				+ TABLE_SECTIONS
				+ " ("
				+ KEY_ID
				+ " INTEGER PRIMARY KEY AUTOINCREMENT, "
				// + KEY_BOOK
				// + " INTEGER NOT NULL, "
				+ KEY_CHAPTER_ID
				+ " INTEGER NOT NULL, "
				+ KEY_SECTION
				+ " INTEGER NOT NULL, "
				+ KEY_TITLE
				+ " TEXT NOT NULL, "
				+ KEY_VERSES_OFFSET
				+ " INTEGER NOT NULL, "
				+ KEY_VERSE_COUNT
				+ " INTEGER NOT NULL, "
				// + KEY_BYTE_START
				// + " INTEGER NOT NULL, "
				// + KEY_BYTE_LENGTH
				// + " INTEGER NOT NULL, "
				// + KEY_CHAR_START
				// + " INTEGER, "
				// + KEY_CHAR_LENGTH
				// + " INTEGER, "
				+ " FOREIGN KEY("
				+ KEY_VERSES_OFFSET
				+ ") REFERENCES "
				+ TABLE_VERSES
				+ "("
				+ KEY_ID
				+ "),"
				// + "FOREIGN KEY("
				// + KEY_VERSE_LAST
				// + ") REFERENCES " + TABLE_VERSES
				// + "(" + KEY_ID + "), "
				+ "FOREIGN KEY("
				+ KEY_CHAPTER_ID
				+ ") REFERENCES "
				+ TABLE_CHAPTERS + "(" + KEY_ID + ")" + ");";

		// */
		// #endregion

		/*
		 * / SQL commands to create tables within the Database
		 * 
		 * // SQL commands to create table books private static final String
		 * CREATE_VIEW_BOOKS = "CREATE TEMP VIEW IF NOT EXISTS " + VIEW_BOOKS +
		 * " (" + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
		 * KEY_WOKR_ABBRV + " TEXT NOT NULL, " + KEY_BOOK +
		 * " INTEGER NOT NULL, " + KEY_TITLE + " TEXT NOT NULL, " +
		 * KEY_CHAPTER_FIRST + " INTEGER NOT NULL, " + KEY_CHAPTER_COUNT +
		 * " INTEGER NOT NULL, " // + KEY_SECTION_FIRST // + " INTEGER, " +
		 * KEY_SECTION_COUNT + " INTEGER, " // + KEY_VERSE_FIRST // +
		 * " INTEGER NOT NULL, " + KEY_VERSE_COUNT + " INTEGER NOT NULL, " +
		 * KEY_BYTE_START + " INTEGER NOT NULL, " + KEY_BYTE_LENGTH +
		 * " INTEGER NOT NULL, " + KEY_CHAR_START + " INTEGER, " +
		 * KEY_CHAR_LENGTH + " INTEGER NOT NULL, " + " FOREIGN KEY(" +
		 * KEY_WOKR_ABBRV + ") REFERENCES " + TABLE_WOKRS + "(" +
		 * KEY_ABBREVIATION + ")" + ");";
		 * 
		 * // SQL commands to create table chapters private static final String
		 * CREATE_VIEW_CHAPTERS = "CREATE TEMP VIEW IF NOT EXISTS " +
		 * VIEW_CHAPTERS + " (" + KEY_ID +
		 * " INTEGER PRIMARY KEY AUTOINCREMENT, " + KEY_BOOK_ID +
		 * " INTEGER NOT NULL, " + KEY_CHAPTER + " INTEGER NOT NULL, " +
		 * KEY_VERSE_FIRST + " INTEGER, " + KEY_VERSE_COUNT + " INTEGER, " +
		 * KEY_SECTION_FIRST + " INTEGER, " + KEY_SECTION_COUNT + " INTEGER, " +
		 * KEY_BYTE_START + " INTEGER NOT NULL, " + KEY_BYTE_LENGTH +
		 * " INTEGER NOT NULL, " + KEY_CHAR_START + " INTEGER, " +
		 * KEY_CHAR_LENGTH + " INTEGER NOT NULL, " + " FOREIGN KEY(" +
		 * KEY_BOOK_ID + ") REFERENCES " + TABLE_BOOKS + "(" + KEY_ID + ")" +
		 * ");";
		 * 
		 * // SQL commands to create table verses private static final String
		 * CREATE_VIEW_VERSES = "CREATE TEMP VIEW IF NOT EXISTS " + TABLE_VERSES
		 * + " (" + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " // +
		 * KEY_BOOK // + " INTEGER NOT NULL, " + KEY_CHAPTER_ID +
		 * " INTEGER NOT NULL, " + KEY_VERSE + " INTEGER NOT NULL, " +
		 * KEY_BYTE_START + " INTEGER NOT NULL, " + KEY_BYTE_LENGTH +
		 * " INTEGER NOT NULL, " + KEY_CHAR_START + " INTEGER, " +
		 * KEY_CHAR_LENGTH + " INTEGER, " // + " FOREIGN KEY(" // + KEY_BOOK //
		 * + ") REFERENCES " + TABLE_BOOKS // + "(" + KEY_ID + ")," +
		 * " FOREIGN KEY(" + KEY_CHAPTER_ID + ") REFERENCES " + TABLE_CHAPTERS +
		 * "(" + KEY_ID + ")" + ");";
		 * 
		 * // SQL commands to create table sections private static final String
		 * CREATE_VIEW_SECTIONS = "CREATE TEMP VIEW IF NOT EXISTS " +
		 * TABLE_SECTIONS + " (" + KEY_ID +
		 * " INTEGER PRIMARY KEY AUTOINCREMENT, " // + KEY_BOOK // +
		 * " INTEGER NOT NULL, " + KEY_CHAPTER_ID + " INTEGER NOT NULL, " +
		 * KEY_SECTION + " INTEGER NOT NULL, " + KEY_TITLE + " TEXT NOT NULL, "
		 * + KEY_VERSE_FIRST + " INTEGER NOT NULL, " + KEY_VERSE_COUNT +
		 * " INTEGER NOT NULL, " // + KEY_BYTE_START // + " INTEGER NOT NULL, "
		 * // + KEY_BYTE_LENGTH // + " INTEGER NOT NULL, " // + KEY_CHAR_START
		 * // + " INTEGER, " // + KEY_CHAR_LENGTH // + " INTEGER, " +
		 * " FOREIGN KEY(" + KEY_VERSE_FIRST + ") REFERENCES " + TABLE_VERSES +
		 * "(" + KEY_ID + ")," // + "FOREIGN KEY(" // + KEY_VERSE_LAST // +
		 * ") REFERENCES " + TABLE_VERSES // + "(" + KEY_ID + "), " +
		 * "FOREIGN KEY(" + KEY_CHAPTER_ID + ") REFERENCES " + TABLE_CHAPTERS +
		 * "(" + KEY_ID + ")" + ");"; //
		 */

		public BibleSQLiteOpenHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(CREATE_TABLE_WORKS);
			db.execSQL(CREATE_TABLE_BOOKS);
			db.execSQL(CREATE_TABLE_CHAPTERS);
			db.execSQL(CREATE_TABLE_SECTIONS);
			db.execSQL(CREATE_TABLE_VERSES);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.i(TAG, "Done nothing onUpgrade()");
			/*/ Do nothing when upgrade the database.
			// Log the version upgrade.
			Log.w("TaskDBAdapter", "Upgrading from version " + oldVersion
					+ " to " + newVersion + ", which will destroy all old data");
			// Upgrade the existing database to conform to the new version.
			// Multiple
			// previousView versions can be handled by comparing oldVersion and
			// newVersion values.

			// The simplest case is to drop the old table and create a new one.

			db.execSQL("DROP TABLE IF IT EXISTS " + TABLE_VERSES);
			db.execSQL("DROP TABLE IF IT EXISTS " + TABLE_SECTIONS);
			db.execSQL("DROP TABLE IF IT EXISTS " + TABLE_CHAPTERS);
			db.execSQL("DROP TABLE IF IT EXISTS " + TABLE_BOOKS);
			db.execSQL("DROP TABLE IF IT EXISTS " + TABLE_WORKS);
			db.execSQL("VACUUM");

			// Create a new one.
			onCreate(db);
			//*/
		}
	}

}
