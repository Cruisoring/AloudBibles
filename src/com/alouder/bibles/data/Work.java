package com.alouder.bibles.data;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;

import org.json.JSONException;
import org.json.JSONObject;

import android.database.Cursor;
import android.util.Log;

public class Work
{
	// public static final int TOTAL_BOOK_COUNT = 66;
	public static final String TAG = Work.class.getSimpleName();
	public static String TABLENAME = BiblesContentProvider.TABLE_WORKS;
	public static String[] COLUMN_NAMES = null;
	public static final String DEFAULT_CHPATER_TITLE = "Chapter %s";
	public static final String DEFAULT_VERSE_TITLE = "Verse %s";
	public static final String DEFAULT_SECTION_TITLE = "Section %s";
	
	public static final String ChapterFormat = "ChapterFormat";
	public static final String VerseFormat = "VerseFormat";
	public static final String SectionFormat = "SectionFormat";
	
	protected static int CodeIndex = -1;
	protected static int EditionIndex = -1;
	protected static int VersionIndex = -1;
	protected static int CharsetIndex = -1;
	protected static int LocaleIndex = -1;
	protected static int ContentIndex = -1;
	protected static int DescriptionIndex = -1;
	protected static int MetaDataIndex = -1;
	protected static int BooksOffsetIndex = -1;
	protected static int ChaptersOffsetIndex = -1;
	protected static int SectionsOffsetIndex = -1;
	protected static int VersesOffsetIndex = -1;
	protected static int BookCountIndex = -1;
	protected static int ChapterCountIndex = -1;
	protected static int SectionCountIndex = -1;
	protected static int VerseCountIndex = -1;
	protected static int ByteLengthIndex = -1;
	protected static int CharLengthIndex = -1;
	
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
			List<String> namesList = Arrays.asList(COLUMN_NAMES);
			CodeIndex = namesList.indexOf(
					BiblesContentProvider.KEY_CODE);
			EditionIndex = namesList.indexOf(
					BiblesContentProvider.KEY_WORK);
			VersionIndex = namesList.indexOf(
					BiblesContentProvider.KEY_VERSION);
			CharsetIndex = namesList.indexOf(
					BiblesContentProvider.KEY_CHARSET);
			LocaleIndex = namesList.indexOf(
					BiblesContentProvider.KEY_LOCALE);
			ContentIndex = namesList.indexOf(
					BiblesContentProvider.KEY_CONTENT);
			DescriptionIndex = namesList.indexOf(
					BiblesContentProvider.KEY_DESCRIPTION);
			MetaDataIndex = namesList.indexOf(
					BiblesContentProvider.KEY_METADATA);
			BooksOffsetIndex = namesList.indexOf(
					BiblesContentProvider.KEY_BOOKS_OFFSET);
			ChaptersOffsetIndex = namesList.indexOf(
					BiblesContentProvider.KEY_CHAPTERS_OFFSET);
			SectionsOffsetIndex = namesList.indexOf(
					BiblesContentProvider.KEY_SECTIONS_OFFSET);
			VersesOffsetIndex = namesList.indexOf(
					BiblesContentProvider.KEY_VERSES_OFFSET);
			BookCountIndex = namesList.indexOf(
					BiblesContentProvider.KEY_BOOK_COUNT);
			ChapterCountIndex = namesList.indexOf(
					BiblesContentProvider.KEY_CHAPTER_COUNT);
			SectionCountIndex = namesList.indexOf(
					BiblesContentProvider.KEY_SECTION_COUNT);
			VerseCountIndex = namesList.indexOf(
					BiblesContentProvider.KEY_VERSE_COUNT);
			ByteLengthIndex = namesList.indexOf(
					BiblesContentProvider.KEY_BYTE_LENGTH);
			CharLengthIndex = namesList.indexOf(
					BiblesContentProvider.KEY_CHAR_LENGTH);			
		}

	}
	
	public final EnumMap<BookEnum, String> bookNameMap = new EnumMap<BookEnum, String>(
			BookEnum.class);
	public final EnumMap<BookEnum, String> bookAbbreviationMap = new EnumMap<BookEnum, String>(
			BookEnum.class);
	
	public final String code;
	public final String work;
	public final String version;
	protected Charset charset;
	public final Locale locale;
	public final File contentFile;
	public final String description;
	public final String metaData;
	public final int booksOffset;
	public final int chaptersOffset;
	public final int sectionsOffset;
	public final int versesOffset;
	public final int bookCount;
	public final int chapterCount;
	public final int sectionCount;
	public final int verseCount;
	public final int byteLength;
	public final int charLength;
	private String chapterTitleFormat;
	private String verseTitleFormat;
	private String sectionTitleFormat;
	
	private boolean loaded = false;
	
	public boolean isLoaded()
	{
		return loaded;
	}
	
	private Content content = null;
	
	private EnumMap<BookEnum, Book> books = null;
	private BookEnum firstBookEnum = BookEnum.Revelation;
	private BookEnum lastBookEnum = BookEnum.Genesis;
	
	public BookEnum getFirstBookEnum()
	{
		return firstBookEnum;
	}
	
	public BookEnum getLastBookEnum()
	{
		return lastBookEnum;
	}
	
	protected void setBooks(EnumMap<BookEnum, Book> books)
	{
		this.books = books;
		
		bookNameMap.clear();
		bookAbbreviationMap.clear();
		
		if (books != null)
		{
			for (Entry<BookEnum, Book> entry : books.entrySet())
			{
				Book book = entry.getValue();
				BookEnum key = entry.getKey();
				if (key.compareTo(firstBookEnum) < 0)
				{
					firstBookEnum = key;
				}
				else if (key.compareTo(lastBookEnum) > 0)
				{
					lastBookEnum = key;
				}
				
				bookNameMap.put(key, book.title);
				bookAbbreviationMap.put(key, book.abbreviation);
			}
			
			BookEnum.LoadBookNames(bookNameMap);
			BookEnum.LoadBookNames(bookAbbreviationMap);
			loaded = true;
		}
		else
		{
			loaded = false;
		}
		
	}
	
	protected Work(Cursor cursor, File contentFile)
	{
		code = cursor.getString(CodeIndex);
		work = cursor.getString(EditionIndex);
		version = cursor.getString(VersionIndex);
		String tempString = cursor.getString(CharsetIndex);
		
		if (Charset.availableCharsets().containsKey(tempString))
		{
			charset = Charset.forName(tempString);
		}
		else
		{
			charset = null;
		}
		
		tempString = cursor.getString(LocaleIndex);
		String[] items = tempString.split("_");
		if (items.length == 3)
		{
			locale = new Locale(items[0], items[1], items[2]);
		}
		else if (items.length == 2)
		{
			locale = new Locale(items[0], items[1]);
		}
		else if (items.length == 1)
		{
			locale = new Locale(items[0]);
		}
		else
		{
			locale = Locale.getDefault();
		}
		
		this.contentFile = contentFile;
		description = cursor.getString(DescriptionIndex);
		// TODO parsing metaData as JSON Object to provide more features
		metaData = cursor.getString(MetaDataIndex);
		booksOffset = cursor.getInt(BooksOffsetIndex);
		chaptersOffset = cursor.getInt(ChaptersOffsetIndex);
		sectionsOffset = cursor.getInt(SectionsOffsetIndex);
		versesOffset = cursor.getInt(VersesOffsetIndex);
		bookCount = cursor.getInt(BookCountIndex);
		chapterCount = cursor.getInt(ChapterCountIndex);
		sectionCount = cursor.getInt(SectionCountIndex);
		verseCount = cursor.getInt(VerseCountIndex);
		byteLength = cursor.getInt(ByteLengthIndex);
		charLength = cursor.getInt(CharLengthIndex);
		
		try {
			JSONObject jObj = new JSONObject(metaData);
			chapterTitleFormat = jObj.getString(ChapterFormat);
			verseTitleFormat = jObj.getString(VerseFormat);
			sectionTitleFormat = jObj.getString(SectionFormat);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			chapterTitleFormat = DEFAULT_CHPATER_TITLE;
			verseTitleFormat = DEFAULT_VERSE_TITLE;
			sectionTitleFormat = DEFAULT_SECTION_TITLE;
		}
	}
	
	protected boolean loadContent()
	{
		if (content != null)
		{
			return true;
		}
		
		try
		{
			content = new Content(contentFile, this.charset);
			return true;
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
			return false;
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return false;
		}
	}
	
	public Collection<String> getBookAbbreviations()
	{
		return bookAbbreviationMap.values();
	}
	
	public Collection<String> getBookNames()
	{
		return bookNameMap.values();
	}
	
	public Collection<Book> getBooks()
	{
		return books.values();
	}
	
	public boolean containtsBook(BookEnum bookEnum)
	{
		return books.containsKey(bookEnum);
	}
	
	public String bookNameOf(BookEnum bookEnum)
	{
		return bookNameMap.containsKey(bookEnum) ? bookNameMap.get(bookEnum)
				: "";
	}
	
	public String bookAbbreviationOf(BookEnum bookEnum)
	{
		return bookAbbreviationMap.containsKey(bookEnum) ? bookAbbreviationMap
				.get(bookEnum) : "";
	}
	
	public Book bookOf(int bookNum)
	{
		BookEnum bookEnum = BookEnum.fromOrdinal(bookNum);
		return bookOf(bookEnum);
	}
	
	public Book bookOf(BookEnum bookEnum)
	{
		if (!BookEnum.allBooks.contains(bookEnum) || (books == null)
				|| !books.containsKey(bookEnum))
		{
			return null;
		}
		
		Book theBook = books.get(bookEnum);
		
		if (!theBook.isLoaded())
		{
			BiblesContentProvider.loadChaptersOf(theBook);
		}
		
		return theBook;
	}
	
	public Book nextBookOf(BookEnum bookEnum)
	{
		int ordinal = bookEnum.ordinal() + 1;
		return bookOf(ordinal);
	}
	
	public Book previousBookOf(BookEnum bookEnum)
	{
		int ordinal = bookEnum.ordinal() - 1;
		return bookOf(ordinal);
	}
	
	public int idOfBook(int bookNum)
	{
		return booksOffset + bookNum;
	}
	
	public String textOf(int byteOffset, int byteCount)
	{
		try
		{
			return this.content.getString(byteOffset, byteCount);
		}
		catch (IOException e)
		{
			Log.e("Content",
					String.format(
							"Failed to retrieve text (startPosition0=%d, count=%) from binary file",
							byteOffset, byteCount));
			e.printStackTrace();
			return "";
		}
	}
	
	public boolean hasChapterOf(int chapterId)
	{
		// Check to see if the chapterId is valid
		if ((chapterId > chaptersOffset)
				&& (chapterId <= (chapterCount + chaptersOffset)))
		{
			return true;
		}
		
		Log.w(TAG, String.format("%s doesn't contain chapter with id=%d",
				this.work, chapterId));
		return false;
	}
	
	public Chapter chapterOf(int chapterId)
	{
		if (!hasChapterOf(chapterId))
		{
			return null;
		}
		
		for (Book book : books.values())
		{
			int actualOffset = book.chaptersOffset + chaptersOffset;
			if ((actualOffset < chapterId)
					&& ((actualOffset + book.chapterCount) >= chapterId))
			{
				if (!book.isLoaded())
				{
					BiblesContentProvider.loadChaptersOf(book);
				}
				
				return book.chapterOf(chapterId - actualOffset);
			}
		}
		
		Log.e(TAG, "Failed to get the chapter whose id=" + chapterId);
		return null;
	}
	
	public Chapter chapterOf(int bookNum, int chapterNum)
	{
		Book book = bookOf(bookNum);
		if (book == null)
		{
			return null;
		}
		
		return book.chapterOf(chapterNum);
	}
	
	public Chapter chapterOf(BookEnum bookEnum, int chapterNum)
	{
		return chapterOf(bookEnum.ordinal(), chapterNum);
	}
	
	public boolean hasVerseOf(int verseId)
	{
		// Check to see if the verseId is valid
		if ((verseId > versesOffset)
				&& (verseId <= (verseCount + versesOffset)))
		{
			return true;
		}
		
//		Log.w(TAG, String.format("%s doesn't contain verse whose id=%d",
//				this.work, verseId));
		return false;
	}
	
	public Verse verseOf(int verseId)
	{
		if (!hasVerseOf(verseId))
		{
			return null;
		}
		
		int chapterId = BiblesContentProvider.chapterIdOfVerse(verseId);
		Chapter theChapter = chapterOf(chapterId);
		if (theChapter == null)
		{
			return null;
		}
		
		return theChapter.verseOf(verseId - theChapter.versesOffset);
	}
	
	public Verse verseOf(int bookNum, int chapterNum, int verseNum)
	{
		Book book = bookOf(bookNum);
		if (book == null)
		{
			return null;
		}
		
		Chapter chapter = book.chapterOf(chapterNum);
		if (chapter == null)
		{
			return null;
		}
		
		Verse result = chapter.verseOf(verseNum);
		return result;
	}
	
	public String titleOf(int bookNum)
	{
		return titleOf(BookEnum.fromOrdinal(bookNum));
	}
	
	public String titleOf(BookEnum bookEnum)
	{
		return bookNameMap.containsKey(bookEnum) ? bookNameMap.get(bookEnum)
				: "";
	}
	
	public String titleOf(Chapter chapter)
	{
		return String.format(chapterTitleFormat, chapter.chapterNum);
	}
	
	public String titleOf(Verse verse)
	{
		return String.format(verseTitleFormat, verse.verseNum);
	}

	@Override
	public String toString() {
		return String.format("%s(%s, %s)", work, code, locale.getDisplayLanguage(locale));
	}
}