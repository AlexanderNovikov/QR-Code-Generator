

package com.qrCodeGenerator.util.oned;

import com.qrCodeGenerator.util.BinaryBitmap;
import com.qrCodeGenerator.util.ChecksumException;
import com.qrCodeGenerator.util.DecodeHintType;
import com.qrCodeGenerator.util.FormatException;
import com.qrCodeGenerator.util.NotFoundException;
import com.qrCodeGenerator.util.Reader;
import com.qrCodeGenerator.util.ReaderException;
import com.qrCodeGenerator.util.Result;
import com.qrCodeGenerator.util.ResultMetadataType;
import com.qrCodeGenerator.util.ResultPoint;
import com.qrCodeGenerator.util.common.BitArray;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;


public abstract class OneDReader implements Reader {

  protected static final int INTEGER_MATH_SHIFT = 8;
  protected static final int PATTERN_MATCH_RESULT_SCALE_FACTOR = 1 << INTEGER_MATH_SHIFT;

  public Result decode(BinaryBitmap image) throws NotFoundException, FormatException {
    return decode(image, null);
  }

  public Result decode(BinaryBitmap image,
                       Map<DecodeHintType,?> hints) throws NotFoundException, FormatException {
    try {
      return doDecode(image, hints);
    } catch (NotFoundException nfe) {
      boolean tryHarder = hints != null && hints.containsKey(DecodeHintType.TRY_HARDER);
      if (tryHarder && image.isRotateSupported()) {
        BinaryBitmap rotatedImage = image.rotateCounterClockwise();
        Result result = doDecode(rotatedImage, hints);

        Map<ResultMetadataType,?> metadata = result.getResultMetadata();
        int orientation = 270;
        if (metadata != null && metadata.containsKey(ResultMetadataType.ORIENTATION)) {

          orientation = (orientation +
              (Integer) metadata.get(ResultMetadataType.ORIENTATION)) % 360;
        }
        result.putMetadata(ResultMetadataType.ORIENTATION, orientation);

        ResultPoint[] points = result.getResultPoints();
        if (points != null) {
          int height = rotatedImage.getHeight();
          for (int i = 0; i < points.length; i++) {
            points[i] = new ResultPoint(height - points[i].getY() - 1, points[i].getX());
          }
        }
        return result;
      } else {
        throw nfe;
      }
    }
  }

  public void reset() {

  }


  private Result doDecode(BinaryBitmap image,
                          Map<DecodeHintType,?> hints) throws NotFoundException {
    int width = image.getWidth();
    int height = image.getHeight();
    BitArray row = new BitArray(width);

    int middle = height >> 1;
    boolean tryHarder = hints != null && hints.containsKey(DecodeHintType.TRY_HARDER);
    int rowStep = Math.max(1, height >> (tryHarder ? 8 : 5));
    int maxLines;
    if (tryHarder) {
      maxLines = height;
    } else {
      maxLines = 15;
    }

    for (int x = 0; x < maxLines; x++) {


      int rowStepsAboveOrBelow = (x + 1) >> 1;
      boolean isAbove = (x & 0x01) == 0;
      int rowNumber = middle + rowStep * (isAbove ? rowStepsAboveOrBelow : -rowStepsAboveOrBelow);
      if (rowNumber < 0 || rowNumber >= height) {

        break;
      }


      try {
        row = image.getBlackRow(rowNumber, row);
      } catch (NotFoundException nfe) {
        continue;
      }



      for (int attempt = 0; attempt < 2; attempt++) {
        if (attempt == 1) {
          row.reverse();




          if (hints != null && hints.containsKey(DecodeHintType.NEED_RESULT_POINT_CALLBACK)) {
            Map<DecodeHintType,Object> newHints = new EnumMap<DecodeHintType,Object>(DecodeHintType.class);
            newHints.putAll(hints);
            newHints.remove(DecodeHintType.NEED_RESULT_POINT_CALLBACK);
            hints = newHints;
          }
        }
        try {

          Result result = decodeRow(rowNumber, row, hints);

          if (attempt == 1) {

            result.putMetadata(ResultMetadataType.ORIENTATION, 180);

            ResultPoint[] points = result.getResultPoints();
            if (points != null) {
              points[0] = new ResultPoint(width - points[0].getX() - 1, points[0].getY());
              points[1] = new ResultPoint(width - points[1].getX() - 1, points[1].getY());
            }
          }
          return result;
        } catch (ReaderException re) {

        }
      }
    }

    throw NotFoundException.getNotFoundInstance();
  }


  protected static void recordPattern(BitArray row,
                                      int start,
                                      int[] counters) throws NotFoundException {
    int numCounters = counters.length;
    Arrays.fill(counters, 0, numCounters, 0);
    int end = row.getSize();
    if (start >= end) {
      throw NotFoundException.getNotFoundInstance();
    }
    boolean isWhite = !row.get(start);
    int counterPosition = 0;
    int i = start;
    while (i < end) {
      if (row.get(i) ^ isWhite) {
        counters[counterPosition]++;
      } else {
        counterPosition++;
        if (counterPosition == numCounters) {
          break;
        } else {
          counters[counterPosition] = 1;
          isWhite = !isWhite;
        }
      }
      i++;
    }


    if (!(counterPosition == numCounters || (counterPosition == numCounters - 1 && i == end))) {
      throw NotFoundException.getNotFoundInstance();
    }
  }

  protected static void recordPatternInReverse(BitArray row, int start, int[] counters)
      throws NotFoundException {

    int numTransitionsLeft = counters.length;
    boolean last = row.get(start);
    while (start > 0 && numTransitionsLeft >= 0) {
      if (row.get(--start) != last) {
        numTransitionsLeft--;
        last = !last;
      }
    }
    if (numTransitionsLeft >= 0) {
      throw NotFoundException.getNotFoundInstance();
    }
    recordPattern(row, start + 1, counters);
  }


  protected static int patternMatchVariance(int[] counters,
                                            int[] pattern,
                                            int maxIndividualVariance) {
    int numCounters = counters.length;
    int total = 0;
    int patternLength = 0;
    for (int i = 0; i < numCounters; i++) {
      total += counters[i];
      patternLength += pattern[i];
    }
    if (total < patternLength) {


      return Integer.MAX_VALUE;
    }



    int unitBarWidth = (total << INTEGER_MATH_SHIFT) / patternLength;
    maxIndividualVariance = (maxIndividualVariance * unitBarWidth) >> INTEGER_MATH_SHIFT;

    int totalVariance = 0;
    for (int x = 0; x < numCounters; x++) {
      int counter = counters[x] << INTEGER_MATH_SHIFT;
      int scaledPattern = pattern[x] * unitBarWidth;
      int variance = counter > scaledPattern ? counter - scaledPattern : scaledPattern - counter;
      if (variance > maxIndividualVariance) {
        return Integer.MAX_VALUE;
      }
      totalVariance += variance;
    }
    return totalVariance / total;
  }


  public abstract Result decodeRow(int rowNumber, BitArray row, Map<DecodeHintType,?> hints)
      throws NotFoundException, ChecksumException, FormatException;

}
