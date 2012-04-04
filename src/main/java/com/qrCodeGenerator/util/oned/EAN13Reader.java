

package com.qrCodeGenerator.util.oned;

import com.qrCodeGenerator.util.BarcodeFormat;
import com.qrCodeGenerator.util.NotFoundException;
import com.qrCodeGenerator.util.common.BitArray;


public final class EAN13Reader extends UPCEANReader {






























  static final int[] FIRST_DIGIT_ENCODINGS = {
      0x00, 0x0B, 0x0D, 0xE, 0x13, 0x19, 0x1C, 0x15, 0x16, 0x1A
  };

  private final int[] decodeMiddleCounters;

  public EAN13Reader() {
    decodeMiddleCounters = new int[4];
  }

  @Override
  protected int decodeMiddle(BitArray row,
                             int[] startRange,
                             StringBuilder resultString) throws NotFoundException {
    int[] counters = decodeMiddleCounters;
    counters[0] = 0;
    counters[1] = 0;
    counters[2] = 0;
    counters[3] = 0;
    int end = row.getSize();
    int rowOffset = startRange[1];

    int lgPatternFound = 0;

    for (int x = 0; x < 6 && rowOffset < end; x++) {
      int bestMatch = decodeDigit(row, counters, rowOffset, L_AND_G_PATTERNS);
      resultString.append((char) ('0' + bestMatch % 10));
      for (int counter : counters) {
        rowOffset += counter;
      }
      if (bestMatch >= 10) {
        lgPatternFound |= 1 << (5 - x);
      }
    }

    determineFirstDigit(resultString, lgPatternFound);

    int[] middleRange = findGuardPattern(row, rowOffset, true, MIDDLE_PATTERN);
    rowOffset = middleRange[1];

    for (int x = 0; x < 6 && rowOffset < end; x++) {
      int bestMatch = decodeDigit(row, counters, rowOffset, L_PATTERNS);
      resultString.append((char) ('0' + bestMatch));
      for (int counter : counters) {
        rowOffset += counter;
      }
    }

    return rowOffset;
  }

  @Override
  BarcodeFormat getBarcodeFormat() {
    return BarcodeFormat.EAN_13;
  }


  private static void determineFirstDigit(StringBuilder resultString, int lgPatternFound)
      throws NotFoundException {
    for (int d = 0; d < 10; d++) {
      if (lgPatternFound == FIRST_DIGIT_ENCODINGS[d]) {
        resultString.insert(0, (char) ('0' + d));
        return;
      }
    }
    throw NotFoundException.getNotFoundInstance();
  }

}
