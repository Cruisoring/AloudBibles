package com.alouder.bibles.data;

import android.database.Cursor;
import android.util.Log;

public class Book implements Comparable<Book>, INavigatable<Book>
{
	public static final String TAG = Book.class.getSimpleName();
	public static final String TABLENAME = BiblesContentProvider.TABLE_BOOKS;
	
	public static String[] COLUMN_NAMES = null;
	protected static int ID_Index = -1;
	protected static int EditionCode_Index = -1;
	protected static int Book_Index = -1;
	protected static int Title_Index = -1;
	protected static int Abbreviation_Index = -1;
	protected static int MetaData_Index = -1;
	protected static int ChapterOffset_Index = -1;
	protected static int ChapterCount_Index = -1;
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
		public void getTableIndexes(Cursor booksCursor)
		{
			if (COLUMN_NAMES != null)
			{
				return;
			}
			
			COLUMN_NAMES = booksCursor.getColumnNames();
			ID_Index = booksCursor
					.getColumnIndexOrThrow(BiblesContentProvider.KEY_ID);
			EditionCode_Index = booksCursor
					.getColumnIndexOrThrow(BiblesContentProvider.KEY_WORK_CODE);
			Book_Index = booksCursor
					.getColumnIndexOrThrow(BiblesContentProvider.KEY_BOOK);
			Title_Index = booksCursor
					.getColumnIndexOrThrow(BiblesContentProvider.KEY_TITLE);
			Abbreviation_Index = booksCursor
					.getColumnIndexOrThrow(BiblesContentProvider.KEY_ABBREVIATION);
			MetaData_Index = booksCursor
					.getColumnIndexOrThrow(BiblesContentProvider.KEY_METADATA);
			ChapterOffset_Index = booksCursor
					.getColumnIndexOrThrow(BiblesContentProvider.KEY_CHAPTERS_OFFSET);
			ChapterCount_Index = booksCursor
					.getColumnIndexOrThrow(BiblesContentProvider.KEY_CHAPTER_COUNT);
			SectionCount_Index = booksCursor
					.getColumnIndexOrThrow(BiblesContentProvider.KEY_SECTION_COUNT);
			VerseCount_Index = booksCursor
					.getColumnIndexOrThrow(BiblesContentProvider.KEY_VERSE_COUNT);
			ByteStart_Index = booksCursor
					.getColumnIndexOrThrow(BiblesContentProvider.KEY_BYTE_START);
			ByteLength_Index = booksCursor
					.getColumnIndexOrThrow(BiblesContentProvider.KEY_BYTE_LENGTH);
			CharStart_Index = booksCursor
					.getColumnIndexOrThrow(BiblesContentProvider.KEY_CHAR_START);
			CharLength_Index = booksCursor
					.getColumnIndexOrThrow(BiblesContentProvider.KEY_CHAR_LENGTH);
		}
	
	}
	
	public final int id;
	public final String editionCode;
	public final BookEnum bookEnum;
	public final String title;
	public final String abbreviation;
	public final String metaData;
	public final int chaptersOffset;
	public final int chapterCount;
	public final int sectionCount;
	public final int verseCount;
	public final int byteStart;
	public final int byteLength;
	public final int charStart;
	public final int charLength;
	
	final Work work;
	
	private boolean loaded = false;
	
	public boolean isLoaded()
	{
		return loaded;
	}
	
	private Chapter[] chapters = null;
	
	protected void setChapters(Chapter[] chapters)
	{
//		Log.d(TAG, String.format("setChapter() when loaded=%s, chapters=%s",
//				loaded, this.chapters));
		this.chapters = chapters;
		loaded = (chapters == null ? false : true);
//		Log.d(TAG, String.format("then loaded=%s, chapters=%s", loaded,
//				this.chapters));
	}
	
	protected Book(Cursor cursor, Work work)
	{
		id = cursor.getInt(ID_Index);
		editionCode = cursor.getString(EditionCode_Index);
		int num = cursor.getInt(Book_Index);
		bookEnum = BookEnum.fromOrdinal(num);
		title = cursor.getString(Title_Index);
		abbreviation = cursor.getString(Abbreviation_Index);
		// TODO treat the meatData as JSON object to provide more features
		metaData = cursor.getString(MetaData_Index);
		
		chaptersOffset = cursor.getInt(ChapterOffset_Index);
		chapterCount = cursor.getInt(ChapterCount_Index);
		sectionCount = cursor.getInt(SectionCount_Index);
		verseCount = cursor.getInt(VerseCount_Index);
		byteStart = cursor.getInt(ByteStart_Index);
		byteLength = cursor.getInt(ByteLength_Index);
		charStart = cursor.getInt(CharStart_Index);
		charLength = cursor.getInt(CharLength_Index);
		
		if (work == null)
		{
			throw new NullPointerException(
					"the book must be associated with a work " + title);
		}
		
		this.work = work;
	}
	
	/*
	 * / protected Book(Cursor cursor) { id = cursor.getInt(ID_Index);
	 * editionCode = cursor.getString(EditionCode_Index); int num =
	 * cursor.getInt(Book_Index); bookEnum = BookEnum.fromOrdinal(num); title =
	 * cursor.getString(Title_Index); abbreviation =
	 * cursor.getString(Abbreviation_Index); // TODO treat the meatData as JSON
	 * object to provide more features metaData =
	 * cursor.getString(MetaData_Index);
	 * 
	 * chapterFirstId = cursor.getInt(ChapterOffset_Index); chapterCount =
	 * cursor.getInt(ChapterCount_Index); sectionCount =
	 * cursor.getInt(SectionCount_Index); verseCount =
	 * cursor.getInt(VerseCount_Index); byteStart =
	 * cursor.getInt(ByteStart_Index); byteLength =
	 * cursor.getInt(ByteLength_Index); charStart =
	 * cursor.getInt(CharStart_Index); charLength =
	 * cursor.getInt(CharLength_Index);
	 * 
	 * work = BiblesContentProvider.getWorkInstance(editionCode);
	 * 
	 * if (work == null) throw new
	 * NullPointerException("Failed to get the work of this book " + title); }
	 * //
	 */
	
	public Chapter chapterOf(int chapterNum)
	{
		if ((chapterNum < 1) || (chapterNum > chapterCount))
		{
			return null;
		}
		
		if (!loaded || (chapters == null))
		{
			Log.e(TAG, "Unexpected event happen: the book" + title
					+ " has not been loaded?!");
			return null;
		}
		
		Chapter chapter = chapters[chapterNum - 1];
		
		if (!chapter.isLoaded())
		{
			BiblesContentProvider.loadContentOf(chapter);
		}
		
		return chapter;
	}
	
	public int chapterIdOf(int chapterNum)
	{
		return chaptersOffset + chapterNum;
	}
	
	@Override
	public int compareTo(Book another)
	{
		return this.bookEnum.compareTo(another.bookEnum);
	}
	
	@Override
	public boolean hasPrevious()
	{
		BookEnum previousBookEnum = BookEnum.fromOrdinal(this.bookEnum
				.ordinal() - 1);
		return previousBookEnum == BookEnum.NONE ? false : work
				.containtsBook(previousBookEnum);
	}
	
	@Override
	public boolean hasNext()
	{
		BookEnum nextBookEnum = BookEnum
				.fromOrdinal(this.bookEnum.ordinal() + 1);
		return nextBookEnum == BookEnum.NONE ? false : work
				.containtsBook(nextBookEnum);
	}
	
	@Override
	public Book getPrevious()
	{
		return work.previousBookOf(this.bookEnum);
	}
	
	@Override
	public Book getNext()
	{
		return work.nextBookOf(this.bookEnum);
	}
	
	@Override
	public String getTitle()
	{
		return this.title;
	}
	
	@Override
	public String getAbbreviation()
	{
		return this.abbreviation;
	}
	
}

/*
 * / public boolean load(Context context) { if (chapters != null &&
 * chapters.length == chapterCount && chapters[chapterCount-1] != null) return
 * true;
 * 
 * try { // boolean result = false;
 * 
 * chapters = new Chapter[chapterCount];
 * 
 * ContentResolver cResolver = context.getContentResolver();
 * 
 * String selection = String.format("%s > ? AND %s = ?",
 * BiblesContentProvider.KEY_ID, BiblesContentProvider.KEY_WORK_CODE); String[]
 * selectionArgs = new String[] { String.valueOf(chaptersOffset) }; String
 * sortOrder = String.format("%s ASC LIMIT %s",
 * BiblesContentProvider.KEY_CHAPTER, chapterCount);
 * 
 * Cursor chapterCursor =
 * cResolver.query(BiblesContentProvider.CONTENT_URI_CHAPTERS, null, selection,
 * selectionArgs, sortOrder);
 * 
 * int cursorCount = chapterCursor.getCount(); if (cursorCount != chapterCount)
 * throw new
 * SQLException(String.format("%d chapters are expected instead of %d",
 * chapterCount, cursorCount));
 * 
 * Chapter theChapter; int expectedBookId = this.bookEnum.ordinal(); while
 * (chapterCursor.moveToNext()) { int bookId =
 * chapterCursor.getInt(Chapter.BookId_Index); int chapterNum =
 * chapterCursor.getInt(Chapter.Chapter_Index); if (bookId != expectedBookId)
 * throw new IndexOutOfBoundsException(String.format(
 * "Chapter %d of book %s doesn't belong to %s", chapterNum,
 * BookEnum.fromOrdinal(bookId), this.bookEnum));
 * 
 * theChapter = new Chapter(chapterCursor, work);
 * 
 * chapters[bookId-1] = theChapter; }
 * 
 * return true; } catch (SQLException e) { e.printStackTrace(); Log.e(title,
 * e.getMessage()); return false; } } //
 */
