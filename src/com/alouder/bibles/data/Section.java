package com.alouder.bibles.data;

import android.database.Cursor;

public class Section implements Comparable<Section>
{
	public static final String TABLENAME = BiblesContentProvider.TABLE_SECTIONS;
	public static String[] COLUMN_NAMES = null;
	
	// public static final String[] SECTION_COLUMN =
	// { KEY_ID, KEY_CHAPTER_ID, KEY_SECTION, KEY_TITLE, KEY_VERSE_FIRST,
	// KEY_VERSE_COUNT };
	
	protected static int ID_Index = -1;
	protected static int ChapterId_Index = -1;
	protected static int Section_Index = -1;
	protected static int Title_Index = -1;
	protected static int VersesOffset_Index = -1;
	protected static int VerseCount_Index = -1;
	
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
			Section_Index = cursor
					.getColumnIndexOrThrow(BiblesContentProvider.KEY_SECTION);
			Title_Index = cursor
					.getColumnIndexOrThrow(BiblesContentProvider.KEY_TITLE);
			VersesOffset_Index = cursor
					.getColumnIndexOrThrow(BiblesContentProvider.KEY_VERSES_OFFSET);
			VerseCount_Index = cursor
					.getColumnIndexOrThrow(BiblesContentProvider.KEY_VERSE_COUNT);
		}
	
	}
	
	public final int id;
	public final int chapterId;
	public final int sectionNum;
	public final String title;
	public final int versesOffset;
	public final int verseCount;
	
	private Chapter chapter;
	
	public Chapter getChapter()
	{
		return chapter;
	}
	
	public String getText()
	{
		return this.title;
	}
	
	public int firstVerseNum()
	{
		return this.versesOffset != -1 ? (this.versesOffset - chapter.versesOffset) + 1
				: 1;
	}
	
	public int lastVerseNum()
	{
		return (this.verseCount + this.versesOffset) - chapter.versesOffset;
	}
	
	public int getOrdinal()
	{
		return firstVerseNum();
	}
	
	protected Section(Cursor cursor, Chapter chapter)
	{
		id = cursor.getInt(ID_Index);
		chapterId = cursor.getInt(ChapterId_Index);
		sectionNum = cursor.getInt(Section_Index);
		title = cursor.getString(Title_Index);
		versesOffset = cursor.getInt(VersesOffset_Index);
		verseCount = cursor.getInt(VerseCount_Index);
		
		this.chapter = chapter;
	}
	
	// Special section for those without title and default definitions in Table
	// sections
	protected Section(Chapter chapter, int count)
	{
		id = -1;
		chapterId = chapter.id;
		sectionNum = 0;
		title = "";
		versesOffset = -1;
		verseCount = count;
		this.chapter = chapter;
	}
	
	@Override
	public int compareTo(Section another)
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
	public String toString()
	{
		return String.format("%s(%d-%d): %s", chapter, firstVerseNum(),
				lastVerseNum(), title);
	}

}
