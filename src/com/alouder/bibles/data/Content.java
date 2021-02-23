package com.alouder.bibles.data;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;

import android.os.Build;

public class Content extends RandomAccessFile {
	// Basic unit size when loading from file
	public final static int BLOCK_SIZE = 1024;

	// Size of the buffer in block
	public final static int TOTAL_BLOCKS = 100;

	// Intentionally reserved size when new loading happen
	public final static int OVERLAPPED_BLOCKS = 10;
	// Overlapped part will be kept in the page
	public final static int OVERLAPPED_SIZE = OVERLAPPED_BLOCKS * BLOCK_SIZE;
	// buffer of 100k, big enough to hold content of any single book.
	public final static int PAGE_LENGTH = TOTAL_BLOCKS * BLOCK_SIZE;

	public final static HashMap<Charset, byte[]> EXPECTED_CHARSET_MAP;
	static {
		EXPECTED_CHARSET_MAP = new HashMap<Charset, byte[]>();
		EXPECTED_CHARSET_MAP.put(Charset.forName("utf-8"), new byte[] {
				(byte) 0xEF, (byte) 0xBB, (byte) 0xBF });
		EXPECTED_CHARSET_MAP.put(Charset.forName("utf-16LE"), new byte[] {
				(byte) 0xFF, (byte) 0xFE });
		EXPECTED_CHARSET_MAP.put(Charset.forName("utf-16"), new byte[] {
				(byte) 0xFE, (byte) 0xFF });
		if (Charset.isSupported("utf-32LE")) {
			EXPECTED_CHARSET_MAP.put(Charset.forName("utf-32LE"), new byte[] {
					0x00, 0x00, (byte) 0xFF, (byte) 0xFE });
		}
		if (Charset.isSupported("utf-32BE")) {
			EXPECTED_CHARSET_MAP.put(Charset.forName("utf-32BE"), new byte[] {
					0x00, 0x00, (byte) 0xFE, (byte) 0xFF });
		}
	}

	// Charset decides how to decode the content in byte[] to String
	public final Charset charset;
	// The underlying file length in bytes
	public final long fileLength;
	// The needed block number to hold the whole underlying file content
	private final int blockSize;

	// If byte order mark (BOM)
	private final int BOMOffset;

	// The buffer to access the underlying file
	private byte[] page = new byte[PAGE_LENGTH];
	// The offset within the file of the buffered content
	private int pageOffset = -1;
	// The valid buffered content size
	private int validPageSize = -1;

	public Content(File file, Charset charset) throws FileNotFoundException,
			IOException, IllegalCharsetNameException {
		// Always open the content file as read-only
		super(file, "r");

		fileLength = file.length();
		blockSize = (int) Math.floor(((fileLength + BLOCK_SIZE) - 1)
				/ BLOCK_SIZE);
		loadFrom(0);

		Charset parsedCharset = parseCharset();

		if (parsedCharset == null) {
			if (charset == null) {
				throw new IllegalCharsetNameException(
						"Failed to detect the charset without BOM.");
			}
			this.charset = charset;
			BOMOffset = 0;
		} else if (parsedCharset == charset) {
			this.charset = charset;
			BOMOffset = EXPECTED_CHARSET_MAP.get(parsedCharset).length;
		} else {
			this.charset = parsedCharset;
			BOMOffset = EXPECTED_CHARSET_MAP.get(parsedCharset).length;
			charset = parsedCharset;
		}
	}

	private Charset parseCharset() throws IOException {
		Charset result = null;
		for (Entry<Charset, byte[]> entity : EXPECTED_CHARSET_MAP.entrySet()) {
			int len = entity.getValue().length;
			byte[] bom = bytesOf(0, len);
			if (Arrays.equals(bom, entity.getValue())) {
				return entity.getKey();
			}
		}

		return result;
	}

	public byte[] bytesOf(int byteOffsetInFile, int byteCount)
			throws IOException {
		int lastOffset = (byteOffsetInFile + byteCount) - 1;

		if ((byteOffsetInFile < 0) || (byteOffsetInFile >= fileLength)) {
			throw new IOException("The byteOffsetInFile = " + lastOffset
					+ " is invalid when fileLength=" + fileLength);
		} else if (lastOffset > fileLength) {
			throw new IOException("The byteOffsetInFile + byteCount = "
					+ lastOffset + " is out of file range in bytes.");
		}

		byte[] result = new byte[byteCount];
		seek(byteOffsetInFile);

		int readed = read(result);

		if (readed > 0) {
			return result;
		} else {
			throw new IOException(String.format(
					"Failed to read when offset={0}, byteCount={1}",
					byteOffsetInFile, byteCount));
		}
	}

	private boolean loadFrom(int firstByteOffset) throws IOException {
		// Expected page has already been loaded
		if ((firstByteOffset < 0) || (firstByteOffset >= fileLength)) {
			throw new IOException(String.format(
					"firstByteOffset({0}) is out of file range (0-{1}).",
					firstByteOffset, fileLength));
		}
		// else if ((pageOffset <= firstByteOffset)
		// && ((pageOffset + validPageSize) > firstByteOffset))
		// {
		// return true;
		// }

		int startBlockNum = (int) Math.floor(firstByteOffset / BLOCK_SIZE);
		if (startBlockNum > (blockSize - TOTAL_BLOCKS)) {
			startBlockNum = blockSize - TOTAL_BLOCKS;
		}

		// TODO to enhance the file accessing efficiency later
		pageOffset = startBlockNum * BLOCK_SIZE;
		validPageSize = (int) Math.min(PAGE_LENGTH, fileLength - pageOffset);
		seek(pageOffset);

		int readed = read(page, 0, validPageSize);

		return validPageSize == readed;
	}

	public String getString(int offset, int byteCount) throws IOException {
		offset += BOMOffset;
		int last = offset + byteCount;

		if ((validPageSize < 0) || (offset < pageOffset)
				|| (last > (pageOffset + PAGE_LENGTH))) {
			loadFrom(offset);
		}

		int first = offset - pageOffset;
		last = (first + byteCount) - 1;

		if ((first >= 0) && (last < PAGE_LENGTH)) {
			// get the String directly from the buffer when it holds the expected content
			return stringOf(page, first, byteCount, charset);
		} else if (byteCount > PAGE_LENGTH) {
			byte[] content = bytesOf(offset, byteCount);
			return stringOf(content, 0, byteCount, charset);
		}

		if (loadFrom(offset)) {
			first = offset - pageOffset;
			return stringOf(page, first, byteCount, charset);
		} else {
			throw new IOException(String.format(
					"Failed to load bytes when offset={0}, byteCount={1}",
					offset, byteCount));
		}
	}

    private static final char REPLACEMENT_CHAR = (char) 0xfffd;
    /**
     * Converts the byte array to a string using the given charset.
     *
     * <p>The behavior when the bytes cannot be decoded by the given charset
     * is to replace malformed input and unmappable characters with the charset's default
     * replacement string. Use {@link java.nio.charset.CharsetDecoder} for more control.
     *
     * @throws IndexOutOfBoundsException
     *             if {@code byteCount < 0 || offset < 0 || offset + byteCount > data.length}
     * @throws NullPointerException
     *             if {@code data == null}
     *
     * @since 1.6
     */
    private String stringOf(byte[] data, int offset, int byteCount, Charset charset) {
    	if (Build.VERSION.SDK_INT >= 10)
    		return new String(data, offset, byteCount, charset);
    	
        if ((offset | byteCount) < 0 || byteCount > data.length - offset) {
            return null;
        }

        int theOffset, theCount;
        char[] theValue;
        
        // We inline UTF-8, ISO-8859-1, and US-ASCII decoders for speed and because 'count' and
        // 'value' are final.
        String canonicalCharsetName = charset.name();
        if (canonicalCharsetName.equals("UTF-8")) {
            byte[] d = data;
            char[] v = new char[byteCount];

            int idx = offset;
            int last = offset + byteCount;
            int s = 0;
outer:
            while (idx < last) {
                byte b0 = d[idx++];
                if ((b0 & 0x80) == 0) {
                    // 0xxxxxxx
                    // Range:  U-00000000 - U-0000007F
                    int val = b0 & 0xff;
                    v[s++] = (char) val;
                } else if (((b0 & 0xe0) == 0xc0) || ((b0 & 0xf0) == 0xe0) ||
                        ((b0 & 0xf8) == 0xf0) || ((b0 & 0xfc) == 0xf8) || ((b0 & 0xfe) == 0xfc)) {
                    int utfCount = 1;
                    if ((b0 & 0xf0) == 0xe0) utfCount = 2;
                    else if ((b0 & 0xf8) == 0xf0) utfCount = 3;
                    else if ((b0 & 0xfc) == 0xf8) utfCount = 4;
                    else if ((b0 & 0xfe) == 0xfc) utfCount = 5;

                    // 110xxxxx (10xxxxxx)+
                    // Range:  U-00000080 - U-000007FF (count == 1)
                    // Range:  U-00000800 - U-0000FFFF (count == 2)
                    // Range:  U-00010000 - U-001FFFFF (count == 3)
                    // Range:  U-00200000 - U-03FFFFFF (count == 4)
                    // Range:  U-04000000 - U-7FFFFFFF (count == 5)

                    if (idx + utfCount > last) {
                        v[s++] = REPLACEMENT_CHAR;
                        break;
                    }

                    // Extract usable bits from b0
                    int val = b0 & (0x1f >> (utfCount - 1));
                    for (int i = 0; i < utfCount; i++) {
                        byte b = d[idx++];
                        if ((b & 0xC0) != 0x80) {
                            v[s++] = REPLACEMENT_CHAR;
                            idx--; // Put the input char back
                            continue outer;
                        }
                        // Push new bits in from the right side
                        val <<= 6;
                        val |= b & 0x3f;
                    }

                    // Note: Java allows overlong char
                    // specifications To disallow, check that val
                    // is greater than or equal to the minimum
                    // value for each count:
                    //
                    // count    min value
                    // -----   ----------
                    //   1           0x80
                    //   2          0x800
                    //   3        0x10000
                    //   4       0x200000
                    //   5      0x4000000

                    // Allow surrogate values (0xD800 - 0xDFFF) to
                    // be specified using 3-byte UTF values only
                    if ((utfCount != 2) && (val >= 0xD800) && (val <= 0xDFFF)) {
                        v[s++] = REPLACEMENT_CHAR;
                        continue;
                    }

                    // Reject chars greater than the Unicode maximum of U+10FFFF.
                    if (val > 0x10FFFF) {
                        v[s++] = REPLACEMENT_CHAR;
                        continue;
                    }

                    // Encode chars from U+10000 up as surrogate pairs
                    if (val < 0x10000) {
                        v[s++] = (char) val;
                    } else {
                        int x = val & 0xffff;
                        int u = (val >> 16) & 0x1f;
                        int w = (u - 1) & 0xffff;
                        int hi = 0xd800 | (w << 6) | (x >> 10);
                        int lo = 0xdc00 | (x & 0x3ff);
                        v[s++] = (char) hi;
                        v[s++] = (char) lo;
                    }
                } else {
                    // Illegal values 0x8*, 0x9*, 0xa*, 0xb*, 0xfd-0xff
                    v[s++] = REPLACEMENT_CHAR;
                }
            }

            if (s == byteCount) {
                // We guessed right, so we can use our temporary array as-is.
                theOffset = 0;
                theValue = v;
                theCount = s;
            } else {
                // Our temporary array was too big, so reallocate and copy.
                theOffset = 0;
                theValue = new char[s];
                theCount = s;
                System.arraycopy(v, 0, theValue, 0, s);
            }
        } else {
            CharBuffer cb = charset.decode(ByteBuffer.wrap(data, offset, byteCount));
            theOffset = 0;
            theCount = cb.length();
            if (theCount > 0) {
                // We could use cb.array() directly, but that would mean we'd have to trust
                // the CharsetDecoder doesn't hang on to the CharBuffer and mutate it later,
                // which would break String's immutability guarantee. It would also tend to
                // mean that we'd be wasting memory because CharsetDecoder doesn't trim the
                // array. So we copy.
                theValue = new char[theCount];
                System.arraycopy(cb.array(), 0, theValue, 0, theCount);
            } else {
                theValue = null;
            }
        }
        
        return new String(theValue);
    }
}
