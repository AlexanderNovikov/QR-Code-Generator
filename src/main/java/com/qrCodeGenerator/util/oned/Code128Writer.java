

package com.qrCodeGenerator.util.oned;

import com.qrCodeGenerator.util.BarcodeFormat;
import com.qrCodeGenerator.util.EncodeHintType;
import com.qrCodeGenerator.util.WriterException;
import com.qrCodeGenerator.util.common.BitMatrix;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;


public final class Code128Writer extends UPCEANWriter {

  private static final int CODE_START_B = 104;
  private static final int CODE_START_C = 105;
  private static final int CODE_CODE_B = 100;
  private static final int CODE_CODE_C = 99;
  private static final int CODE_STOP = 106;


  private static final char ESCAPE_FNC_1 = '\u00f1';
  private static final char ESCAPE_FNC_2 = '\u00f2';
  private static final char ESCAPE_FNC_3 = '\u00f3';
  private static final char ESCAPE_FNC_4 = '\u00f4';

  private static final int CODE_FNC_1 = 102;
  private static final int CODE_FNC_2 = 97;
  private static final int CODE_FNC_3 = 96;
  private static final int CODE_FNC_4_B = 100;

  @Override
  public BitMatrix encode(String contents,
                          BarcodeFormat format,
                          int width,
                          int height,
                          Map<EncodeHintType,?> hints) throws WriterException {
    if (format != BarcodeFormat.CODE_128) {
      throw new IllegalArgumentException("Can only encode CODE_128, but got " + format);
    }
    return super.encode(contents, format, width, height, hints);
  }

  @Override
  public byte[] encode(String contents) {
    int length = contents.length();

    if (length < 1 || length > 80) {
      throw new IllegalArgumentException(
          "Contents length should be between 1 and 80 characters, but got " + length);
    }

    for (int i = 0; i < length; i++) {
      char c = contents.charAt(i);
      if (c < ' ' || c > '~') {
        switch (c) {
          case ESCAPE_FNC_1:
          case ESCAPE_FNC_2:
          case ESCAPE_FNC_3:
          case ESCAPE_FNC_4:
            break;
          default:
            throw new IllegalArgumentException("Bad character in input: " + c);
        }
      }
    }
    
    Collection<int[]> patterns = new ArrayList<int[]>();
    int checkSum = 0;
    int checkWeight = 1;
    int codeSet = 0;
    int position = 0;
    
    while (position < length) {

      int requiredDigitCount = codeSet == CODE_CODE_C ? 2 : 4;
      int newCodeSet;
      if (isDigits(contents, position, requiredDigitCount)) {
        newCodeSet = CODE_CODE_C;
      } else {
        newCodeSet = CODE_CODE_B;
      }
      

      int patternIndex;
      if (newCodeSet == codeSet) {

        if (codeSet == CODE_CODE_B) {
          patternIndex = contents.charAt(position) - ' ';
          position += 1;
        } else {
          switch (contents.charAt(position)) {
            case ESCAPE_FNC_1:
              patternIndex = CODE_FNC_1;
              position++;
              break;
            case ESCAPE_FNC_2:
              patternIndex = CODE_FNC_2;
              position++;
              break;
            case ESCAPE_FNC_3:
              patternIndex = CODE_FNC_3;
              position++;
              break;
            case ESCAPE_FNC_4:
              patternIndex = CODE_FNC_4_B;
              position++;
              break;
            default:
              patternIndex = Integer.parseInt(contents.substring(position, position + 2));
              position += 2;
              break;
          }
        }
      } else {


        if (codeSet == 0) {

          if (newCodeSet == CODE_CODE_B) {
            patternIndex = CODE_START_B;
          } else {

            patternIndex = CODE_START_C;
          }
        } else {

          patternIndex = newCodeSet;
        }
        codeSet = newCodeSet;
      }
      

      patterns.add(Code128Reader.CODE_PATTERNS[patternIndex]);
      

      checkSum += patternIndex * checkWeight;
      if (position != 0) {
        checkWeight++;
      }
    }
    

    checkSum %= 103;
    patterns.add(Code128Reader.CODE_PATTERNS[checkSum]);
    

    patterns.add(Code128Reader.CODE_PATTERNS[CODE_STOP]);
    

    int codeWidth = 0;
    for (int[] pattern : patterns) {
      for (int width : pattern) {
        codeWidth += width;
      }
    }
    

    byte[] result = new byte[codeWidth];
    int pos = 0;
    for (int[] pattern : patterns) {
      pos += appendPattern(result, pos, pattern, 1);
    }
    
    return result;
  }

  private static boolean isDigits(CharSequence value, int start, int length) {
    int end = start + length;
    int last = value.length();
    for (int i = start; i < end && i < last; i++) {
      char c = value.charAt(i);
      if (c < '0' || c > '9') {
        if (c != ESCAPE_FNC_1) {
          return false;
        }
        end++;
      }
    }
    return end <= last;
  }

}
