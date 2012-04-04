

package com.qrCodeGenerator.util;

import com.qrCodeGenerator.util.common.BitArray;
import com.qrCodeGenerator.util.common.BitMatrix;


public abstract class Binarizer {

  private final LuminanceSource source;

  protected Binarizer(LuminanceSource source) {
    this.source = source;
  }

  public LuminanceSource getLuminanceSource() {
    return source;
  }


  public abstract BitArray getBlackRow(int y, BitArray row) throws NotFoundException;


  public abstract BitMatrix getBlackMatrix() throws NotFoundException;


  public abstract Binarizer createBinarizer(LuminanceSource source);

  public int getWidth() {
    return source.getWidth();
  }

  public int getHeight() {
    return source.getHeight();
  }

}
