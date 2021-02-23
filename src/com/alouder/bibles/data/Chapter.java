package com.alouder.bibles.data;

import java.util.ArrayList;
import java.util.Collections;

import android.database.Cursor;

public class Chapter implements Comparable<Chapter>, INavigatable<Chapter>
{
	public static final String TAG = Chapter.class.getSimpleName();
	public static final String TABLENAME = BiblesContentProvider.TABLE_CHAPTERS;

	public static String[] COLUMN_NAMES = null;
	protected static int ID_Index = -1;
	protected static int BookId_Index = -1;
	protected static int Chapter_Index = -1;
	protected static int SectionsOffset_Index = -1;
	protected static int VersesOffset_Index = -1;
	protected static int SectionCount_Index = -1;
	protected static int VerseCount_Index = -1;
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
			BookId_Index = cursor
					.getColumnIndexOrThrow(BiblesContentProvider.KEY_BOOK_ID);
			Chapter_Index = cursor
					.getColumnIndexOrThrow(BiblesContentProvider.KEY_CHAPTER);
			SectionsOffset_Index = cursor
					.getColumnIndexOrThrow(BiblesContentProvider.KEY_SECTIONS_OFFSET);
			VersesOffset_Index = cursor
					.getColumnIndexOrThrow(BiblesContentProvider.KEY_VERSES_OFFSET);
			SectionCount_Index = cursor
					.getColumnIndexOrThrow(BiblesContentProvider.KEY_SECTION_COUNT);
			VerseCount_Index = cursor
					.getColumnIndexOrThrow(BiblesContentProvider.KEY_VERSE_COUNT);
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
	public final BookEnum bookEnum;
	public final int chapterNum;
	public final int sectionsOffset;
	public final int versesOffset;
	public final int sectionCount;
	public final int verseCount;
	public final int byteStart;
	public final int byteLength;
	public final int charStart;
	public final int charLength;
	
	public final Work work;
	public final boolean isLastOfBook;
	
	private boolean loaded = false;
	
	public boolean isLoaded()
	{
		return loaded;
	}
	
	protected String text;
	
	protected void setText(String text)
	{
		this.text = text;
	}
	
	private Section[] sections;
	private Verse[] verses;
	
	protected void setSiblings(Section[] sections, Verse[] verses)
	{
		if ((sections == null) || (verses == null))
		{
			this.loaded = false;
			this.text = null;
			return;
		}
		
		this.sections = sections;
		this.verses = verses;
		this.loaded = true;
	}
	
	protected Chapter(Cursor cursor, Book book)
	{
		id = cursor.getInt(ID_Index);
		int bookId = cursor.getInt(BookId_Index);
		bookEnum = BookEnum.fromOrdinal(bookId);
		
		if (bookEnum != book.bookEnum)
		{
			throw new NullPointerException(
					"Mismatch of the book and stored data!");
		}
		
		chapterNum = cursor.getInt(Chapter_Index);
		versesOffset = cursor.getInt(VersesOffset_Index);
		verseCount = cursor.getInt(VerseCount_Index);
		sectionsOffset = cursor.getInt(SectionsOffset_Index);
		sectionCount = cursor.getInt(SectionCount_Index);
		byteStart = cursor.getInt(ByteStart_Index);
		byteLength = cursor.getInt(ByteLength_Index);
		charStart = cursor.getInt(CharStart_Index);
		charLength = cursor.getInt(CharLength_Index);
		
		this.work = book.work;
		this.isLastOfBook = (chapterNum == book.chapterCount);
	}
	
	/*
	 * / protected Chapter(Cursor cursor, Work work) { id =
	 * cursor.getInt(ID_Index); int bookId = cursor.getInt(BookId_Index);
	 * bookEnum = BookEnum.fromOrdinal(bookId); chapterNum =
	 * cursor.getInt(Chapter_Index); versesOffset =
	 * cursor.getInt(VersesOffset_Index); verseCount =
	 * cursor.getInt(VerseCount_Index); sectionsOffset =
	 * cursor.getInt(SectionsOffset_Index); sectionCount =
	 * cursor.getInt(SectionCount_Index); byteStart =
	 * cursor.getInt(ByteStart_Index); byteLength =
	 * cursor.getInt(ByteLength_Index); charStart =
	 * cursor.getInt(CharStart_Index); charLength =
	 * cursor.getInt(CharLength_Index);
	 * 
	 * if (work == null) { throw new NullPointerException(String.format(
	 * "the %s-%s must be associated with a work.", bookEnum, chapterNum)); }
	 * 
	 * this.work = work; uniqueId = (bookId << 24) + (chapterNum << 16); } //
	 */
	
	public Section sectionOf(int ordinal)
	{
		if ((ordinal < 1) || (ordinal > sectionCount))
		{
			return null;
		}
		
		return sections[ordinal - 1];
	}
	
	public Verse verseOf(int ordinal)
	{
		if ((ordinal < 1) || (ordinal > verseCount))
		{
			return null;
		}
		
		return verses[ordinal - 1];
	}
	
	public int sectionIdOf(int sectionOrdinal)
	{
		return work.sectionsOffset + sectionsOffset + sectionOrdinal;
	}
	
	public int verseIdOf(int verseOrdinal)
	{
		return work.versesOffset + versesOffset + verseOrdinal;
	}
	
	@Override
	public String toString()
	{
		Book book = work.bookOf(bookEnum);
		return String.format("%s.%d", book.abbreviation, chapterNum);
	}
	
	protected void unload()
	{
		this.text = null;
		this.sections = null;
		this.verses = null;
		this.loaded = false;
	}
	
	@Override
	public int compareTo(Chapter another)
	{
		int bookDif = this.bookEnum.compareTo(another.bookEnum);
		
		return bookDif == 0 ? this.chapterNum - another.chapterNum : bookDif;
	}
	
	public ArrayList<Verse> getVerses()
	{
		ArrayList<Verse> result = new ArrayList<Verse>();
		Collections.addAll(result, verses);
		return result;
	}
	
	public ArrayList<String> getSectionNames()
	{
		ArrayList<String> resultArrayList = new ArrayList<String>();
		if (sectionCount == 0)
		{
			return resultArrayList;
		}
		
		if (sections[0].firstVerseNum() != 1)
		{
			resultArrayList.add(String.format("%s(1-%d)", this,
					sections[0].firstVerseNum() - 1));
		}
		
		for (Section section : sections)
		{
			String title = section.toString();
			int index = title.indexOf("ги");
			String capitalString = (index == -1) ? title : title.substring(0,
					index);
			
			resultArrayList.add(capitalString);
		}
		
		return resultArrayList;
	}
	
	@Override
	public boolean hasPrevious()
	{
		int previousId = this.id - 1;
		return work.hasChapterOf(previousId);
	}
	
	@Override
	public boolean hasNext()
	{
		int nextId = this.id + 1;
		return work.hasChapterOf(nextId);
	}
	
	@Override
	public Chapter getPrevious()
	{
		int previousId = this.id - 1;
		return work.chapterOf(previousId);
	}
	
	@Override
	public Chapter getNext()
	{
		int nextId = this.id + 1;
		return work.chapterOf(nextId);
	}
	
	@Override
	public String getTitle()
	{
		// return work.bookNameOf(bookEnum) + " " + chapterNum;
		return work.titleOf(bookEnum) + " " + chapterNum;
	}
	
	@Override
	public String getAbbreviation()
	{
		return work.bookAbbreviationOf(bookEnum) + " " + chapterNum;
	}
}
