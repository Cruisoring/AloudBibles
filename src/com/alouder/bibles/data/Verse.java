package com.alouder.bibles.data;

import android.database.Cursor;

public class Verse implements INavigatable<Verse>, Comparable<Verse>
{
	public static final String TAG = Verse.class.getSimpleName();
	public static final String TABLENAME = BiblesContentProvider.TABLE_VERSES;
	public static String[] COLUMN_NAMES = null;
	
	protected static int ID_Index = -1;
	protected static int ChapterId_Index = -1;
	protected static int Verse_Index = -1;
	protected static int ByteStart_Index = -1;
	protected static int ByteLength_Index = -1;
	protected static int CharStart_Index = -1;
	protected static int CharLength_Index = -1;
	
	public static TableMapping getTableMapping()
	{
		return TheTableMapping.getInstance();
	}
	
	public static class TheTableMapping extends TableMapping
	{
		private static TheTableMapping tableMapping = null;
		public static TableMapping getInstance()
		{
			if (tableMapping == null)
				tableMapping = new TheTableMapping();
			return tableMapping;
		}
		
		private TheTableMapping()
		{
			super(TABLENAME);
		}
	
		@Override
		public void getTableIndexes(Cursor cursor)
		{
			if (COLUMN_NAMES != null)
			{
				return;
			}
			
			COLUMN_NAMES = cursor.getColumnNames();
			
			ID_Index = cursor.getColumnIndexOrThrow(BiblesContentProvider.KEY_ID);
			ChapterId_Index = cursor
					.getColumnIndexOrThrow(BiblesContentProvider.KEY_CHAPTER_ID);
			Verse_Index = cursor
					.getColumnIndexOrThrow(BiblesContentProvider.KEY_VERSE);
			ByteStart_Index = cursor
					.getColumnIndexOrThrow(BiblesContentProvider.KEY_BYTE_START);
			ByteLength_Index = cursor
					.getColumnIndexOrThrow(BiblesContentProvider.KEY_BYTE_LENGTH);
			CharStart_Index = cursor
					.getColumnIndexOrThrow(BiblesContentProvider.KEY_CHAR_START);
			CharLength_Index = cursor
					.getColumnIndexOrThrow(BiblesContentProvider.KEY_CHAR_LENGTH);
		}
		
	}
	
	public final int id;
	public final int chapterId;
	public final int verseNum;
	public final int byteStart;
	public final int byteLength;
	public final int charStart;
	public final int charLength;
	
	private Chapter chapter;
	
	public Chapter getChapter()
	{
		return chapter;
	}
	
	public Work getWork()
	{
		return chapter.work;
	}
	
	public int getChapterNum()
	{
		return chapter.chapterNum;
	}
	
	public int getBookNum()
	{
		return chapter.bookEnum.ordinal();
	}
	
	public String getText()
	{
		int offset = this.charStart - chapter.charStart;
		return chapter.text.substring(offset, offset + charLength);
	}
	
	public int getOrdinal()
	{
		return verseNum;
	}
	
	protected Verse(Cursor cursor, Chapter chapter)
	{
		id = cursor.getInt(ID_Index);
		chapterId = cursor.getInt(ChapterId_Index);
		verseNum = cursor.getInt(Verse_Index);
		byteStart = cursor.getInt(ByteStart_Index);
		byteLength = cursor.getInt(ByteLength_Index);
		charStart = cursor.getInt(CharStart_Index);
		charLength = cursor.getInt(CharLength_Index);
		
		this.chapter = chapter;
	}
	
	public boolean isEmpty()
	{
		return charLength == 0;
	}
	
	@Override
	public String toString()
	{
		return String.format("%s-%d", chapter.toString(), verseNum);
	}
	
	@Override
	public int compareTo(Verse another)
	{
		int chapterDif = this.chapter.compareTo(another.getChapter());
		
		if (chapterDif != 0)
		{
			return chapterDif;
		}
		
		int ordinalDif = this.getOrdinal() - another.getOrdinal();
		
		if (ordinalDif != 0)
		{
			return ordinalDif;
		}
		
		return 0;
	}
	
	@Override
	public boolean hasPrevious()
	{
		return chapter.work.hasVerseOf(id - 1);
	}
	
	@Override
	public boolean hasNext()
	{
		return chapter.work.hasVerseOf(id + 1);
	}
	
	@Override
	public Verse getPrevious()
	{
		if (verseNum != 1)
		{
			Verse result = null;
			int previousNum = verseNum - 1;
			do
			{
				result = chapter.verseOf(previousNum--);
			}
			while (result.isEmpty() && (previousNum > 0));
			
			return result;
		}
		else
		{
			Chapter prevChapter = chapter.getPrevious();
			if (prevChapter == null)
			{
				return null;
			}
			
			return prevChapter.verseOf(prevChapter.verseCount);
		}
		
		// return chapter.work.verseOf(id-1);
	}
	
	@Override
	public Verse getNext()
	{
		int nextNum = verseNum + 1;
		Verse result = null;
		
		while (nextNum <= chapter.verseCount)
		{
			result = chapter.verseOf(nextNum++);
			if (!result.isEmpty())
			{
				return result;
			}
		}
		;
		
		if (nextNum > chapter.verseCount)
		{
			Chapter nextChapter = chapter.getNext();
			return nextChapter == null ? null : nextChapter.verseOf(1);
		}
		
		return chapter.work.verseOf(id + 1);
		
		// Log.e(TAG, "failed to getNext() of " + this);
		// return null;
	}
	
	@Override
	public String getTitle()
	{
		return chapter.work.titleOf(this);
	}
	
	@Override
	public String getAbbreviation()
	{
		return null;
	}
	
}
