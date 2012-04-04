

package com.qrCodeGenerator.util.qrcode;

import com.qrCodeGenerator.util.BarcodeFormat;
import com.qrCodeGenerator.util.EncodeHintType;
import com.qrCodeGenerator.util.Writer;
import com.qrCodeGenerator.util.WriterException;
import com.qrCodeGenerator.util.common.BitMatrix;
import com.qrCodeGenerator.util.qrcode.encoder.ByteMatrix;
import com.qrCodeGenerator.util.qrcode.decoder.ErrorCorrectionLevel;
import com.qrCodeGenerator.util.qrcode.encoder.Encoder;
import com.qrCodeGenerator.util.qrcode.encoder.QRCode;

import java.util.Map;


public final class QRCodeWriter implements Writer {

  private static final int QUIET_ZONE_SIZE = 4;

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

    if (format != BarcodeFormat.QR_CODE) {
      throw new IllegalArgumentException("Can only encode QR_CODE, but got " + format);
    }

    if (width < 0 || height < 0) {
      throw new IllegalArgumentException("Requested dimensions are too small: " + width + 'x' +
          height);
    }

    ErrorCorrectionLevel errorCorrectionLevel = ErrorCorrectionLevel.L;
    if (hints != null) {
      ErrorCorrectionLevel requestedECLevel = (ErrorCorrectionLevel) hints.get(EncodeHintType.ERROR_CORRECTION);
      if (requestedECLevel != null) {
        errorCorrectionLevel = requestedECLevel;
      }
    }

    QRCode code = new QRCode();
    Encoder.encode(contents, errorCorrectionLevel, hints, code);
    return renderResult(code, width, height);
  }



  private static BitMatrix renderResult(QRCode code, int width, int height) {
    ByteMatrix input = code.getMatrix();
    if (input == null) {
      throw new IllegalStateException();
    }
    int inputWidth = input.getWidth();
    int inputHeight = input.getHeight();
    int qrWidth = inputWidth + (QUIET_ZONE_SIZE << 1);
    int qrHeight = inputHeight + (QUIET_ZONE_SIZE << 1);
    int outputWidth = Math.max(width, qrWidth);
    int outputHeight = Math.max(height, qrHeight);

    int multiple = Math.min(outputWidth / qrWidth, outputHeight / qrHeight);




    int leftPadding = (outputWidth - (inputWidth * multiple)) / 2;
    int topPadding = (outputHeight - (inputHeight * multiple)) / 2;

    BitMatrix output = new BitMatrix(outputWidth, outputHeight);

    for (int inputY = 0, outputY = topPadding; inputY < inputHeight; inputY++, outputY += multiple) {

      for (int inputX = 0, outputX = leftPadding; inputX < inputWidth; inputX++, outputX += multiple) {
        if (input.get(inputX, inputY) == 1) {
          output.setRegion(outputX, outputY, multiple, multiple);
        }
      }
    }

    return output;
  }

}
