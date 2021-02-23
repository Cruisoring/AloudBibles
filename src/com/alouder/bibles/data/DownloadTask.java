package com.alouder.bibles.data;

import java.sql.Timestamp;
import java.util.HashMap;

import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.alouder.bibles.activities.AloudBibleApplication;

public class DownloadTask {
	public static String DefaultProjectUri = "https://sourceforge.net/projects/aloudbible/files/data/";
	public static String DefaultUriSuffix = ".zip/download";
	
	public static HashMap<String, DownloadTask> AllTasks = new HashMap<String, DownloadTask>();
	
	public static int[] STATUS_CODES = new int[] {
			DownloadManager.STATUS_FAILED, DownloadManager.STATUS_PAUSED,
			DownloadManager.STATUS_PENDING, DownloadManager.STATUS_RUNNING,
			DownloadManager.STATUS_SUCCESSFUL };

	public static final String STATUS_UNKNOWN = "STATUS_UNKNOWN";
	public static String[] STATUS_STRINGS = new String[] { "STATUS_FAILED",
			"STATUS_PAUSED", "STATUS_PENDING", "STATUS_RUNNING",
			"STATUS_SUCCESSFUL", STATUS_UNKNOWN };

	public static int[] STATUS_REASON_CODES = new int[] {
			DownloadManager.PAUSED_QUEUED_FOR_WIFI,
			DownloadManager.PAUSED_WAITING_FOR_NETWORK,
			DownloadManager.PAUSED_WAITING_TO_RETRY,
			DownloadManager.PAUSED_UNKNOWN,
			DownloadManager.ERROR_CANNOT_RESUME,
			DownloadManager.ERROR_DEVICE_NOT_FOUND,
			DownloadManager.ERROR_FILE_ALREADY_EXISTS,
			DownloadManager.ERROR_FILE_ERROR,
			DownloadManager.ERROR_HTTP_DATA_ERROR,
			DownloadManager.ERROR_INSUFFICIENT_SPACE,
			DownloadManager.ERROR_TOO_MANY_REDIRECTS,
			DownloadManager.ERROR_UNHANDLED_HTTP_CODE,
			DownloadManager.ERROR_UNKNOWN };

	public static String[] STATUS_REASON_STRINGS = new String[] {
			"PAUSED_QUEUED_FOR_WIFI", "PAUSED_WAITING_FOR_NETWORK",
			"PAUSED_WAITING_TO_RETRY", "PAUSED_UNKNOWN", "ERROR_CANNOT_RESUME",
			"ERROR_DEVICE_NOT_FOUND", "ERROR_FILE_ALREADY_EXISTS",
			"ERROR_FILE_ERROR", "ERROR_HTTP_DATA_ERROR",
			"ERROR_INSUFFICIENT_SPACE", "ERROR_TOO_MANY_REDIRECTS",
			"ERROR_UNHANDLED_HTTP_CODE", "ERROR_UNKNOWN" };

	private static int index_id = -1, index_title = -1, index_description = -1, 
			index_uri = -1, index_mediaType = -1, index_totalSizeBytes = -1,
			index_localUri = -1, index_local_filename = -1, index_status = -1,
			index_reason = -1, index_bytesSoFar = -1, index_lastModified = -1;
	public static class TaskStatus {
		public final long id;
		public final String title;
		public final String description;
		public final String uri;
		public final String mediaType;
		public final long totalSizeBytes;
		public final long bytesSoFar;
		public final String localUri;
		public final String localFilename;
		public final Timestamp lastModified;
		public final int statusCode;
		public final int reasonCode;
		
		public TaskStatus(Cursor c) {
			if (index_id == -1) {
				index_id = c.getColumnIndex(DownloadManager.COLUMN_ID);
				index_title = c.getColumnIndex(DownloadManager.COLUMN_TITLE);
				index_description = c.getColumnIndex(DownloadManager.COLUMN_DESCRIPTION);
				index_uri = c.getColumnIndex(DownloadManager.COLUMN_URI);
				index_mediaType = c.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE);
				index_totalSizeBytes = c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
				index_localUri = c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
				index_local_filename = c.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME);
				index_status = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
				index_reason = c.getColumnIndex(DownloadManager.COLUMN_REASON);
				index_bytesSoFar = c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
				index_lastModified = c.getColumnIndex(DownloadManager.COLUMN_LAST_MODIFIED_TIMESTAMP);
			}
			
			c.moveToFirst();
			title = c.getString(index_title);
			id = c.getLong(index_id);
			description = c.getString(index_description);
			uri = c.getString(index_uri);
			mediaType = c.getString(index_mediaType);
			totalSizeBytes = c.getLong(index_totalSizeBytes);
			localUri = c.getString(index_localUri);
			localFilename = c.getString(index_local_filename);
			bytesSoFar = c.getLong(index_bytesSoFar);
			long time = c.getLong(index_lastModified);
			lastModified = new Timestamp(time);
			statusCode = c.getInt(index_status);
			reasonCode = c.getInt(index_reason);
		}
		
		public String getStatus(){
			for (int i = 0; i < STATUS_CODES.length; i ++)
			{
				if (STATUS_CODES[i] == statusCode)
					return STATUS_STRINGS[i];
			}		
			
			return STATUS_STRINGS[STATUS_STRINGS.length-1];
		}
		
		public String getReason() {
			for(int i = 0; i < STATUS_REASON_CODES.length; i ++)
			{
				if (STATUS_REASON_CODES[i] == reasonCode)
					return STATUS_REASON_STRINGS[i];
			}
			
			return STATUS_REASON_STRINGS[STATUS_REASON_CODES.length-1];
		}

		public boolean isOngoing() {
			return this.statusCode != DownloadManager.STATUS_SUCCESSFUL;
		}
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(String.format("ID\t:\t%s\r\n", id));
			sb.append(String.format("Title\t:%s\r\n", title));
			sb.append(String.format("Description\t:\t%s\r\n", description));
			sb.append(String.format("Uri\t:%s\r\n", uri));
			sb.append(String.format("MediaType\t:\t%s\r\n", mediaType));
			sb.append(String.format("Total\t:%sK\r\n", totalSizeBytes/1024));
			sb.append(String.format("Downloaded\t:\t%sK\r\n", bytesSoFar / 1024));
			sb.append(String.format("LocalUri\t:%s\r\n", localUri));
			sb.append(String.format("LocalFile\t:\t%s\r\n", localFilename));
			sb.append(String.format("LastModified\t:%s\r\n", lastModified));
			sb.append(String.format("Status\t:\t%s\r\n", getStatus()));
			
			if (statusCode == DownloadManager.STATUS_FAILED || statusCode == DownloadManager.STATUS_PAUSED)
				sb.append(String.format("Reason\t:%s\r\n", getReason()));
			
			return sb.toString();
		}
	}
	
	public static String DefaultAddressOf(String code)
	{
		return DefaultProjectUri + code + DefaultUriSuffix;
	}
	
	public static TaskStatus queryDownloadStatus(long id) {
		Query query = new Query();
		query.setFilterById(id);
		Cursor c = downloadManager.query(query);
		if (c.getCount() != 1) {
			Log.w("Query", String.format("Task of %s might have been removed", id));
			return null;
		}

		TaskStatus result = new TaskStatus(c);
		c.close();
		return result;
	}

	private static DownloadManager downloadManager = null;
	static {
		downloadManager = (DownloadManager) AloudBibleApplication.getAppContext().getSystemService(Context.DOWNLOAD_SERVICE);
	}
	
	public final long id;
	public final String code;
	public final String uri;
	public final Timestamp time;
	
	public DownloadTask(long id, String code) {
		this(id, code, DefaultAddressOf(code));
	}
	
	public DownloadTask(long id, String code, String uri) {
		Long theTime = System.currentTimeMillis();
		this.time = new Timestamp(theTime);
		this.code = code;
		this.id = id;
		this.uri = uri;
		AllTasks.put(code, this);
	}
	
	public DownloadTask(long id, String code, String uri, Timestamp time) {
		this.time = time;
		this.code = code;
		this.id = id;
		this.uri = uri;
		AllTasks.put(code, this);
	}
	
	@Override
	public String toString() {
		return String.format("%s %s(%d)- %s", time, code, id, uri);
	}

	public TaskStatus getStatus() {
		Query query = new Query();
		query.setFilterById(id);
		Cursor c = downloadManager.query(query);
		if (c.getCount() != 1) {
//			Log.e(TAG, "The cursor shall only return one item instead of " + c.getCount());
			return null;
		}

		TaskStatus result = new TaskStatus(c);
		c.close();
		return result;
	}
}
