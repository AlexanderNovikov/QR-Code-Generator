

package com.qrCodeGenerator.util.oned;

import com.qrCodeGenerator.util.BarcodeFormat;
import com.qrCodeGenerator.util.EncodeHintType;
import com.qrCodeGenerator.util.Writer;
import com.qrCodeGenerator.util.WriterException;
import com.qrCodeGenerator.util.common.BitMatrix;

import java.util.Map;


public abstract class OneDimensionalCodeWriter implements Writer {

  private final int sidesMargin;

  protected OneDimensionalCodeWriter(int sidesMargin) {
    this.sidesMargin = sidesMargin;
  }

  public BitMatrix encode(String contents, BarcodeFormat format, int width, int height)
      throws WriterException {
    return encode(contents, format, width, height, null);
  }



  public BitMatrix encode(String contents,
                          BarcodeFormat format,
                          int width,
                          int height,
                          Map<EncodeHintType,?> hints) throws WriterException {
    if (contents.length() == 0) {
      throw new IllegalArgumentException("Found empty contents");
    }

    if (width < 0 || height < 0) {
      throw new IllegalArgumentException("Negative size is not allowed. Input: "
                                             + width + 'x' + height);
    }

    byte[] code = encode(contents);
    return renderResult(code, width, height);
  }


  private BitMatrix renderResult(byte[] code, int width, int height) {
    int inputWidth = code.length;

    int fullWidth = inputWidth + sidesMargin;
    int outputWidth = Math.max(width, fullWidth);
    int outputHeight = Math.max(1, height);

    int multiple = outputWidth / fullWidth;
    int leftPadding = (outputWidth - (inputWidth * multiple)) / 2;

    BitMatrix output = new BitMatrix(outputWidth, outputHeight);
    for (int inputX = 0, outputX = leftPadding; inputX < inputWidth; inputX++, outputX += multiple) {
      if (code[inputX] == 1) {
        output.setRegion(outputX, 0, multiple, outputHeight);
      }
    }
    return output;
  }



  protected static int appendPattern(byte[] target, int pos, int[] pattern, int startColor) {
    if (startColor != 0 && startColor != 1) {
      throw new IllegalArgumentException(
          "startColor must be either 0 or 1, but got: " + startColor);
    }

    byte color = (byte) startColor;
    int numAdded = 0;
    for (int len : pattern) {
      for (int j = 0; j < len; j++) {
        target[pos] = color;
        pos += 1;
        numAdded += 1;
      }
      color ^= 1;
    }
    return numAdded;
  }


  public abstract byte[] encode(String contents);
}

