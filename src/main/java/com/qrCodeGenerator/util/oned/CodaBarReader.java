

package com.qrCodeGenerator.util.oned;

import com.qrCodeGenerator.util.BarcodeFormat;
import com.qrCodeGenerator.util.DecodeHintType;
import com.qrCodeGenerator.util.NotFoundException;
import com.qrCodeGenerator.util.Result;
import com.qrCodeGenerator.util.ResultPoint;
import com.qrCodeGenerator.util.common.BitArray;

import java.util.Map;


public final class CodaBarReader extends OneDReader {

  private static final String ALPHABET_STRING = "0123456789-$:/.+ABCDTN";
  static final char[] ALPHABET = ALPHABET_STRING.toCharArray();


  static final int[] CHARACTER_ENCODINGS = {
      0x003, 0x006, 0x009, 0x060, 0x012, 0x042, 0x021, 0x024, 0x030, 0x048,
      0x00c, 0x018, 0x045, 0x051, 0x054, 0x015, 0x01A, 0x029, 0x00B, 0x00E,
      0x01A, 0x029
  };





  private static final int minCharacterLength = 6; 
  


  private static final char[] STARTEND_ENCODING = {'E', '*', 'A', 'B', 'C', 'D', 'T', 'N'};


  



  @Override
  public Result decodeRow(int rowNumber, BitArray row, Map<DecodeHintType,?> hints)
      throws NotFoundException {
    int[] start = findAsteriskPattern(row);
    start[1] = 0;

    int nextStart = row.getNextSet(start[1]);
    int end = row.getSize();

    StringBuilder result = new StringBuilder();
    int[] counters = new int[7];
    int lastStart;

    do {
      for (int i = 0; i < counters.length; i++) {
        counters[i] = 0;
      }
      recordPattern(row, nextStart, counters);

      char decodedChar = toNarrowWidePattern(counters);
      if (decodedChar == '!') {
        throw NotFoundException.getNotFoundInstance();
      }
      result.append(decodedChar);
      lastStart = nextStart;
      for (int counter : counters) {
        nextStart += counter;
      }


      nextStart = row.getNextSet(nextStart);
    } while (nextStart < end);


    int lastPatternSize = 0;
    for (int counter : counters) {
      lastPatternSize += counter;
    }

    int whiteSpaceAfterEnd = nextStart - lastStart - lastPatternSize;


    if (nextStart != end && (whiteSpaceAfterEnd / 2 < lastPatternSize)) {
      throw NotFoundException.getNotFoundInstance();
    }


    if (result.length() < 2) {
      throw NotFoundException.getNotFoundInstance();
    }

    char startchar = result.charAt(0);
    if (!arrayContains(STARTEND_ENCODING, startchar)) {

      throw NotFoundException.getNotFoundInstance();
    }


    for (int k = 1; k < result.length(); k++) {
      if (result.charAt(k) == startchar) {

        if (k + 1 != result.length()) {
          result.delete(k + 1, result.length() - 1);
          break;
        }
      }
    }


    if (result.length() <= minCharacterLength) {

      throw NotFoundException.getNotFoundInstance();
    }

    result.deleteCharAt(result.length() - 1);
    result.deleteCharAt(0);

    float left = (float) (start[1] + start[0]) / 2.0f;
    float right = (float) (nextStart + lastStart) / 2.0f;
    return new Result(
        result.toString(),
        null,
        new ResultPoint[]{
            new ResultPoint(left, (float) rowNumber),
            new ResultPoint(right, (float) rowNumber)},
        BarcodeFormat.CODABAR);
  }

  private static int[] findAsteriskPattern(BitArray row) throws NotFoundException {
    int width = row.getSize();
    int rowOffset = row.getNextSet(0);

    int counterPosition = 0;
    int[] counters = new int[7];
    int patternStart = rowOffset;
    boolean isWhite = false;
    int patternLength = counters.length;

    for (int i = rowOffset; i < width; i++) {
      if (row.get(i) ^ isWhite) {
        counters[counterPosition]++;
      } else {
        if (counterPosition == patternLength - 1) {
          try {
            if (arrayContains(STARTEND_ENCODING, toNarrowWidePattern(counters))) {

              if (row.isRange(Math.max(0, patternStart - (i - patternStart) / 2), patternStart, false)) {
                return new int[]{patternStart, i};
              }
            }
          } catch (IllegalArgumentException re) {

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
        isWhite ^= true;
      }
    }
    throw NotFoundException.getNotFoundInstance();
  }

  static boolean arrayContains(char[] array, char key) {
    if (array != null) {
      for (char c : array) {
        if (c == key) {
          return true;
        }
      }
    }
    return false;
  }

  private static char toNarrowWidePattern(int[] counters) {



    int numCounters = counters.length;
    int maxNarrowCounter = 0;

    int minCounter = Integer.MAX_VALUE;
    for (int i = 0; i < numCounters; i++) {
      if (counters[i] < minCounter) {
        minCounter = counters[i];
      }
      if (counters[i] > maxNarrowCounter) {
        maxNarrowCounter = counters[i];
      }
    }



    do {
      int wideCounters = 0;
      int pattern = 0;
      for (int i = 0; i < numCounters; i++) {
        if (counters[i] > maxNarrowCounter) {
          pattern |= 1 << (numCounters - 1 - i);
          wideCounters++;
        }
      }

      if ((wideCounters == 2) || (wideCounters == 3)) {
        for (int i = 0; i < CHARACTER_ENCODINGS.length; i++) {
          if (CHARACTER_ENCODINGS[i] == pattern) {
            return ALPHABET[i];
          }
        }
      }
      maxNarrowCounter--;
    } while (maxNarrowCounter > minCounter);
    return '!';
  }

}
