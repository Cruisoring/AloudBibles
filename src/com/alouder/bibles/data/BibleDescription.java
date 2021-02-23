package com.alouder.bibles.data;

import java.sql.Date;
import java.util.Locale;

import org.json.JSONObject;

public class BibleDescription
{
	public static final String UriFormat = DownloadTask.DefaultProjectUri + "%s" + DownloadTask.DefaultUriSuffix;
	public static BibleDescription newInstance(){
		return new BibleDescription();
	}
	
	private String Language;
    private String Code;
    private String Name;
    private String Build;
    private int BookCount;
    private int ChapterCount;
    private int VerseCount;
    private int ZipSize;
    
    public Locale getLocale() {
    	String[] subs = Language.split("_");
    	int len = subs.length;
    	
    	if (len == 1)
    		return new Locale(subs[0]);
    	else if (len == 2)
    		return new Locale(subs[0], subs[1]);
    	else if (len == 3)
    		return new Locale(subs[0], subs[1], subs[2]);
    	else
    		return Locale.getDefault();
    }
    
    public String getUri() {
    	return String.format(UriFormat, Code);
    }
    
    public Date getBuildDate() {
    	return Date.valueOf(Build);
    }
    
	public String getLanguage() {
		return Language;
	}
	public void setLanguage(String language) {
		Language = language;
	}
	public String getCode() {
		return Code;
	}
	public void setCode(String code) {
		Code = code;
	}
	public String getName() {
		return Name;
	}
	public void setName(String name) {
		Name = name;
	}
	public String getBuild() {
		return Build;
	}
	public void setBuild(String build) {
		Build = build;
	}
	public int getBookCount() {
		return BookCount;
	}
	public void setBookCount(int bookCount) {
		BookCount = bookCount;
	}
	public int getChapterCount() {
		return ChapterCount;
	}
	public void setChapterCount(int chapterCount) {
		ChapterCount = chapterCount;
	}
	public int getVerseCount() {
		return VerseCount;
	}
	public void setVerseCount(int verseCount) {
		VerseCount = verseCount;
	}
	public int getZipSize() {
		return ZipSize;
	}
	public void setZipSize(int zipSize) {
		ZipSize = zipSize;
	}
	
	public BibleDescription() {}
	
	public BibleDescription(JSONObject obj) {
		Language = obj.optString("Language", "en");
		Code = obj.optString("Code", "Unknown");
		Name = obj.optString("Name", "Unknown");
		Build = obj.optString("Build", "2000-01-01");
		BookCount = obj.optInt("BookCount", 0);
		ChapterCount = obj.optInt("ChapterCount", 0);
		VerseCount = obj.optInt("VerseCount", 0);
		ZipSize = obj.optInt("ZipSize", 0);
	}

	@Override
	public String toString() {
//		return String.format("%s: %dK, %d-%d-%d", 
//				Build, ZipSize / 1024, BookCount, ChapterCount, VerseCount);
		return String.format("%s: %dK", Build, ZipSize/1024);
	}
	
}

