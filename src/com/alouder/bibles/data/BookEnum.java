package com.alouder.bibles.data;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public enum BookEnum
{
	NONE(""),
	
	// Written by Moses
	Genesis("Gen"), // 1. Genesis
	Exodus("Exod"), // 2. Exodus
	Leviticus("Lev"), // 3. Leviticus
	Numbers("Num"), // 4. Numbers
	Deuteronomy("Deut"), // 5. Deuteronomy
	
	// OT Narratives
	Joshua("Josh"), // 6. Joshua
	Judges("Judg"), // 7. Judges
	Ruth("Ruth"), // 8. Ruth
	Samuel1("1Sam"), // 9. 1 Samuel
	Samuel2("2Sam"), // 10. 2 Samuel
	Kings1("1Kgs"), // 11. 1 Kings
	Kings2("2Kgs"), // 12. 2 Kings
	Chronicles1("1Chr"), // 13. 1 Chronicles
	Chronicles2("2Chr"), // 14. 2 Chronicles
	Ezra("Ezra"), // 15. Ezra
	Nehemiah("Neh"), // 16. Nehemiah
	Esther("Esth"), // 17. Esther
	
	// Wisdom Literature
	Job("Job"), // 18. Job
	Psalms("Ps"), // 19. Psalms
	Proverbs("Prov"), // 20. Proverbs
	Ecclesiastes("Eccl"), // 21. Ecclesiastes
	Song("Song"), // 22. Song of Songs
	
	// Major Prophets
	Isaiah("Isa"), // 23. Isaiah
	Jeremiah("Jer"), // 24. Jeremiah
	Lamentations("Lam"), // 25. Lamentations
	Ezekiel("Ezek"), // 26. Ezekiel
	Daniel("Dan"), // 27. Daniel
	
	// Minor Prophets
	Hosea("Hos"), // 28. Hosea
	Joel("Joel"), // 29. Joel
	Amos("Amos"), // 30. Amos
	Obadiah("Obad"), // 31. Obadiah
	Jonah("Jonah"), // 32. Jonah
	Micah("Mic"), // 33. Micah
	Nahum("Nah"), // 34. Nahum
	Habakkuk("Hab"), // 35. Habakkuk
	Zephaniah("Zeph"), // 36. Zephaniah
	Haggai("Hag"), // 37. Haggai
	Zechariah("Zech"), // 38. Zechariah
	Malachi("Mal"), // 39. Malachi
	
	// NT Narratives
	Matthew("Matt"), // 40. Matthew
	Mark("Mark"), // 41. Mark
	Luke("Luke"), // 42. Luke
	John("John"), // 43. John
	Acts("Acts"), // 44. Acts
	
	// Epistles by Paul
	Romans("Rom"), // 45. Romans
	Corinthians1("1Cor"), // 46. 1 Corinthians
	Corinthians2("2Cor"), // 47. 2 Corinthians
	Galatians("Gal"), // 48. Galatians
	Ephesians("Eph"), // 49. Ephesians
	Philippians("Phil"), // 50. Philippians
	Colossians("Col"), // 51. Colossians
	Thessalonians1("1Thess"), // 52. 1 Thessalonians
	Thessalonians2("2Thess"), // 53. 2 Thessalonians
	Timothy1("1Tim"), // 54. 1 Timothy
	Timothy2("2Tim"), // 55. 2 Timothy
	Titus("Titus"), // 56. Titus
	Philemon("Phlm"), // 57. Philemon
	
	// General Epistles
	Hebrews("Heb"), // 58. Hebrews
	James("Jas"), // 59. James
	Peter1("1Pet"), // 60. 1 Peter
	Peter2("2Pet"), // 61. 2 Peter
	John1("1John"), // 62. 1 John
	John2("2John"), // 63. 2 John
	John3("3John"), // 64. 3 John
	Jude("Jude"), // 65. Jude
	
	// Apocalyptic Epistle by John
	Revelation("Rev") // 66. Revelation
	;
	
	public static final int BOOK_NUM_OFFSET = 1;
	
	public static final EnumSet<BookEnum> allBooks = EnumSet.range(Genesis,
			Revelation);
	public static final EnumSet<BookEnum> oldTestament = EnumSet.range(Genesis,
			Malachi);
	public static final EnumSet<BookEnum> newTestament = EnumSet.range(Matthew,
			Revelation);
	
	public static String[] osisIds()
	{
		String[] result = new String[allBooks.size()];
		for (BookEnum book : allBooks)
		{
			result[book.ordinal() - BOOK_NUM_OFFSET] = book.getOsisId();
		}
		return result;
	}
	
	private static final Map<Integer, BookEnum> BOOKNUMBER_MAP = new HashMap<Integer, BookEnum>();
	static
	{
		for (BookEnum book : allBooks)
		{
			BOOKNUMBER_MAP.put(book.ordinal(), book);
		}
	}
	
	public static BookEnum fromOrdinal(int ordinal)
	{
		return BOOKNUMBER_MAP.containsKey(ordinal) ? BOOKNUMBER_MAP
				.get(ordinal) : NONE;
	}
	
	private static final Map<BookEnum, String> BOOKNAMES_MAP = new HashMap<BookEnum, String>();
	static
	{
		for (BookEnum book : EnumSet.range(Genesis, Revelation))
		{
			BOOKNAMES_MAP.put(book, book.name().toLowerCase());
		}
	}
	
	public static final Map<BookEnum, String> OSISNAMES_MAP = new HashMap<BookEnum, String>();
	static
	{
		for (BookEnum book : allBooks)
		{
			OSISNAMES_MAP.put(book, book.osisId.toLowerCase());
		}
	}
	
	private static final ArrayList<EnumMap<BookEnum, String>> ActiveNamesMAP = new ArrayList<EnumMap<BookEnum, String>>();
	
	public static boolean LoadBookNames(EnumMap<BookEnum, String> extraMap)
	{
		if (ActiveNamesMAP.contains(extraMap) || (extraMap == null)
				|| (extraMap.size() == 0))
		{
			return false;
		}
		
		ActiveNamesMAP.add(extraMap);
		return true;
		
		// boolean result = false;
		//
		// for(java.util.Map.Entry<BookEnum, String> entry :
		// extraMap.entrySet())
		// {
		// String lowerString = entry.getValue().toLowerCase();
		// if (!ActiveNamesMAP.containsKey(lowerString))
		// {
		// ActiveNamesMAP.put(lowerString, entry.getKey());
		// result = true;
		// }
		// }
		// return result;
	}
	
	public static BookEnum fromString(String name)
	{
		// String key = name.toLowerCase();
		//
		// for (EnumMap<BookEnum, String> map : ActiveNamesMAP)
		// {
		// if (map.values().contains(key))
		// {
		//
		// }
		// }
		//
		// if (ActiveNamesMAP.containsKey(key))
		// return ActiveNamesMAP.get(key);
		// else if (BOOKNAMES_MAP.containsKey(key))
		// return BOOKNAMES_MAP.get(key);
		// else if (OSISNAMES_MAP.containsKey(key))
		// return OSISNAMES_MAP.get(key);
		
		return NONE;
	}
	
	/*
	 * / private static final Map<String, BookEnum> BOOKNAMES_MAP = new
	 * HashMap<String, BookEnum>(); static { for (BookEnum book :
	 * EnumSet.range(Genesis, Revelation)) {
	 * BOOKNAMES_MAP.put(book.name().toLowerCase(), book); } }
	 * 
	 * public static final Map<String, BookEnum> OSISNAMES_MAP = new
	 * HashMap<String, BookEnum>(); static { for (BookEnum book :
	 * EnumSet.range(Genesis, Revelation)) { if
	 * (!BOOKNAMES_MAP.containsKey(book.osisId.toLowerCase()))
	 * OSISNAMES_MAP.put(book.osisId.toLowerCase(), book); } }
	 * 
	 * private static final Map<String, BookEnum> ActiveNamesMAP = new
	 * HashMap<String, BookEnum>();
	 * 
	 * public static boolean LoadBookNames(EnumMap<BookEnum, String> extraMap) {
	 * boolean result = false;
	 * 
	 * for(java.util.Map.Entry<BookEnum, String> entry : extraMap.entrySet()) {
	 * String lowerString = entry.getValue().toLowerCase(); if
	 * (!ActiveNamesMAP.containsKey(lowerString)) {
	 * ActiveNamesMAP.put(lowerString, entry.getKey()); result = true; } }
	 * return result; }
	 * 
	 * public static BookEnum fromString(String name) { String key =
	 * name.toLowerCase(); if (ActiveNamesMAP.containsKey(key)) return
	 * ActiveNamesMAP.get(key); else if (BOOKNAMES_MAP.containsKey(key)) return
	 * BOOKNAMES_MAP.get(key); else if (OSISNAMES_MAP.containsKey(key)) return
	 * OSISNAMES_MAP.get(key);
	 * 
	 * return NONE; } //
	 */
	
	private String osisId;
	
	public String getOsisId()
	{
		return osisId;
	}
	
	private BookEnum(String osis)
	{
		this.osisId = osis;
	}
	
}
