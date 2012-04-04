

package com.qrCodeGenerator.util.oned;

import com.qrCodeGenerator.util.BarcodeFormat;
import com.qrCodeGenerator.util.EncodeHintType;
import com.qrCodeGenerator.util.WriterException;
import com.qrCodeGenerator.util.common.BitMatrix;

import java.util.Map;


public final class EAN13Writer extends UPCEANWriter {

  private static final int CODE_WIDTH = 3 +
      (7 * 6) +
      5 +
      (7 * 6) +
      3;

  @Override
  public BitMatrix encode(String contents,
                          BarcodeFormat format,
                          int width,
                          int height,
                          Map<EncodeHintType,?> hints) throws WriterException {
    if (format != BarcodeFormat.EAN_13) {
      throw new IllegalArgumentException("Can only encode EAN_13, but got " + format);
    }

    return super.encode(contents, format, width, height, hints);
  }

  @Override
  public byte[] encode(String contents) {
    if (contents.length() != 13) {
      throw new IllegalArgumentException(
          "Requested contents should be 13 digits long, but got " + contents.length());
    }

    int firstDigit = Integer.parseInt(contents.substring(0, 1));
    int parities = EAN13Reader.FIRST_DIGIT_ENCODINGS[firstDigit];
    byte[] result = new byte[CODE_WIDTH];
    int pos = 0;

    pos += appendPattern(result, pos, UPCEANReader.START_END_PATTERN, 1);


    for (int i = 1; i <= 6; i++) {
      int digit = Integer.parseInt(contents.substring(i, i + 1));
      if ((parities >> (6 - i) & 1) == 1) {
        digit += 10;
      }
      pos += appendPattern(result, pos, UPCEANReader.L_AND_G_PATTERNS[digit], 0);
    }

    pos += appendPattern(result, pos, UPCEANReader.MIDDLE_PATTERN, 0);

    for (int i = 7; i <= 12; i++) {
      int digit = Integer.parseInt(contents.substring(i, i + 1));
      pos += appendPattern(result, pos, UPCEANReader.L_PATTERNS[digit], 1);
    }
    pos += appendPattern(result, pos, UPCEANReader.START_END_PATTERN, 1);

    return result;
  }

}
