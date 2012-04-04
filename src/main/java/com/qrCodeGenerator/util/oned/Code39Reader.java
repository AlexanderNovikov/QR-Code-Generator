

package com.qrCodeGenerator.util.oned;

import com.qrCodeGenerator.util.BarcodeFormat;
import com.qrCodeGenerator.util.ChecksumException;
import com.qrCodeGenerator.util.DecodeHintType;
import com.qrCodeGenerator.util.FormatException;
import com.qrCodeGenerator.util.NotFoundException;
import com.qrCodeGenerator.util.Result;
import com.qrCodeGenerator.util.ResultPoint;
import com.qrCodeGenerator.util.common.BitArray;

import java.util.Map;


public final class Code39Reader extends OneDReader {

  static final String ALPHABET_STRING = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-. *$/+%";
  private static final char[] ALPHABET = ALPHABET_STRING.toCharArray();


  static final int[] CHARACTER_ENCODINGS = {
      0x034, 0x121, 0x061, 0x160, 0x031, 0x130, 0x070, 0x025, 0x124, 0x064,
      0x109, 0x049, 0x148, 0x019, 0x118, 0x058, 0x00D, 0x10C, 0x04C, 0x01C,
      0x103, 0x043, 0x142, 0x013, 0x112, 0x052, 0x007, 0x106, 0x046, 0x016,
      0x181, 0x0C1, 0x1C0, 0x091, 0x190, 0x0D0, 0x085, 0x184, 0x0C4, 0x094,
      0x0A8, 0x0A2, 0x08A, 0x02A
  };

  private static final int ASTERISK_ENCODING = CHARACTER_ENCODINGS[39];

  private final boolean usingCheckDigit;
  private final boolean extendedMode;


  public Code39Reader() {
    usingCheckDigit = false;
    extendedMode = false;
  }


  public Code39Reader(boolean usingCheckDigit) {
    this.usingCheckDigit = usingCheckDigit;
    this.extendedMode = false;
  }


  public Code39Reader(boolean usingCheckDigit, boolean extendedMode) {
    this.usingCheckDigit = usingCheckDigit;
    this.extendedMode = extendedMode;
  }

  @Override
  public Result decodeRow(int rowNumber, BitArray row, Map<DecodeHintType,?> hints)
      throws NotFoundException, ChecksumException, FormatException {

    int[] counters = new int[9];
    int[] start = findAsteriskPattern(row, counters);

    int nextStart = row.getNextSet(start[1]);
    int end = row.getSize();

    StringBuilder result = new StringBuilder(20);
    char decodedChar;
    int lastStart;
    do {
      recordPattern(row, nextStart, counters);
      int pattern = toNarrowWidePattern(counters);
      if (pattern < 0) {
        throw NotFoundException.getNotFoundInstance();
      }
      decodedChar = patternToChar(pattern);
      result.append(decodedChar);
      lastStart = nextStart;
      for (int counter : counters) {
        nextStart += counter;
      }

      nextStart = row.getNextSet(nextStart);
    } while (decodedChar != '*');
    result.setLength(result.length() - 1);


    int lastPatternSize = 0;
    for (int counter : counters) {
      lastPatternSize += counter;
    }
    int whiteSpaceAfterEnd = nextStart - lastStart - lastPatternSize;


    if (nextStart != end && (whiteSpaceAfterEnd >> 1) < lastPatternSize) {
      throw NotFoundException.getNotFoundInstance();
    }

    if (usingCheckDigit) {
      int max = result.length() - 1;
      int total = 0;
      for (int i = 0; i < max; i++) {
        total += ALPHABET_STRING.indexOf(result.charAt(i));
      }
      if (result.charAt(max) != ALPHABET[total % 43]) {
        throw ChecksumException.getChecksumInstance();
      }
      result.setLength(max);
    }

    if (result.length() == 0) {

      throw NotFoundException.getNotFoundInstance();
    }

    String resultString;
    if (extendedMode) {
      resultString = decodeExtended(result);
    } else {
      resultString = result.toString();
    }

    float left = (float) (start[1] + start[0]) / 2.0f;
    float right = (float) (nextStart + lastStart) / 2.0f;
    return new Result(
        resultString,
        null,
        new ResultPoint[]{
            new ResultPoint(left, (float) rowNumber),
            new ResultPoint(right, (float) rowNumber)},
        BarcodeFormat.CODE_39);

  }

  private static int[] findAsteriskPattern(BitArray row, int[] counters) throws NotFoundException {
    int width = row.getSize();
    int rowOffset = row.getNextSet(0);

    int counterPosition = 0;
    int patternStart = rowOffset;
    boolean isWhite = false;
    int patternLength = counters.length;

    for (int i = rowOffset; i < width; i++) {
      if (row.get(i) ^ isWhite) {
        counters[counterPosition]++;
      } else {
        if (counterPosition == patternLength - 1) {
          if (toNarrowWidePattern(counters) == ASTERISK_ENCODING) {

            if (row.isRange(Math.max(0, patternStart - ((i - patternStart) >> 1)), patternStart, false)) {
              return new int[]{patternStart, i};
            }
          }
          patternStart += counters[0] + counters[1];
          System.arraycopy(counters, 2, counters, 0, patternLength - 2);
          counters[patternLength - 2] = 0;
          counters[patternLength - 1] = 0;
          counterPosition--;
        } else {
          counterPosition++;
        }
        counters[counterPosition] = 1;
        isWhite = !isWhite;
      }
    }
    throw NotFoundException.getNotFoundInstance();
  }



  private static int toNarrowWidePattern(int[] counters) {
    int numCounters = counters.length;
    int maxNarrowCounter = 0;
    int wideCounters;
    do {
      int minCounter = Integer.MAX_VALUE;
      for (int i = 0; i < numCounters; i++) {
        int counter = counters[i];
        if (counter < minCounter && counter > maxNarrowCounter) {
          minCounter = counter;
        }
      }
      maxNarrowCounter = minCounter;
      wideCounters = 0;
      int totalWideCountersWidth = 0;
      int pattern = 0;
      for (int i = 0; i < numCounters; i++) {
        int counter = counters[i];
        if (counters[i] > maxNarrowCounter) {
          pattern |= 1 << (numCounters - 1 - i);
          wideCounters++;
          totalWideCountersWidth += counter;
        }
      }
      if (wideCounters == 3) {



        for (int i = 0; i < numCounters && wideCounters > 0; i++) {
          int counter = counters[i];
          if (counters[i] > maxNarrowCounter) {
            wideCounters--;

            if ((counter << 1) >= totalWideCountersWidth) {
              return -1;
            }
          }
        }
        return pattern;
      }
    } while (wideCounters > 3);
    return -1;
  }

  private static char patternToChar(int pattern) throws NotFoundException {
    for (int i = 0; i < CHARACTER_ENCODINGS.length; i++) {
      if (CHARACTER_ENCODINGS[i] == pattern) {
        return ALPHABET[i];
      }
    }
    throw NotFoundException.getNotFoundInstance();
  }

  private static String decodeExtended(CharSequence encoded) throws FormatException {
    int length = encoded.length();
    StringBuilder decoded = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      char c = encoded.charAt(i);
      if (c == '+' || c == '$' || c == '%' || c == '/') {
        char next = encoded.charAt(i + 1);
        char decodedChar = '\0';
        switch (c) {
          case '+':

            if (next >= 'A' && next <= 'Z') {
              decodedChar = (char) (next + 32);
            } else {
              throw FormatException.getFormatInstance();
            }
            break;
          case '$':

            if (next >= 'A' && next <= 'Z') {
              decodedChar = (char) (next - 64);
            } else {
              throw FormatException.getFormatInstance();
            }
            break;
          case '%':

            if (next >= 'A' && next <= 'E') {
              decodedChar = (char) (next - 38);
            } else if (next >= 'F' && next <= 'W') {
              decodedChar = (char) (next - 11);
            } else {
              throw FormatException.getFormatInstance();
            }
            break;
          case '/':

            if (next >= 'A' && next <= 'O') {
              decodedChar = (char) (next - 32);
            } else if (next == 'Z') {
              decodedChar = ':';
            } else {
              throw FormatException.getFormatInstance();
            }
            break;
        }
        decoded.append(decodedChar);

        i++;
      } else {
        decoded.append(c);
      }
    }
    return decoded.toString();
  }

}
