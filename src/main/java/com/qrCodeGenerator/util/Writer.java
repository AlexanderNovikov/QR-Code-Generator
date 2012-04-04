

package com.qrCodeGenerator.util;

import com.qrCodeGenerator.util.common.BitMatrix;

import java.util.Map;


public interface Writer {


  BitMatrix encode(String contents, BarcodeFormat format, int width, int height)
      throws WriterException;


  BitMatrix encode(String contents,
                   BarcodeFormat format,
                   int width,
                   int height,
                   Map<EncodeHintType, ?> hints)
      throws WriterException;

}
