

package com.qrCodeGenerator.util;

import com.qrCodeGenerator.util.common.BitMatrix;
import com.qrCodeGenerator.util.oned.CodaBarWriter;
import com.qrCodeGenerator.util.oned.Code128Writer;
import com.qrCodeGenerator.util.oned.Code39Writer;
import com.qrCodeGenerator.util.oned.EAN13Writer;
import com.qrCodeGenerator.util.oned.EAN8Writer;
import com.qrCodeGenerator.util.oned.ITFWriter;
import com.qrCodeGenerator.util.oned.UPCAWriter;
import com.qrCodeGenerator.util.pdf417.encoder.PDF417Writer;
import com.qrCodeGenerator.util.qrcode.QRCodeWriter;

import java.util.Map;


public final class MultiFormatWriter implements Writer {

  public BitMatrix encode(String contents,
                          BarcodeFormat format,
                          int width,
                          int height) throws WriterException {
    return encode(contents, format, width, height, null);
  }

  public BitMatrix encode(String contents,
                          BarcodeFormat format,
                          int width, int height,
                          Map<EncodeHintType,?> hints) throws WriterException {

    Writer writer;
    switch (format) {
      case EAN_8:
        writer = new EAN8Writer();
        break;
      case EAN_13:
        writer = new EAN13Writer();
        break;
      case UPC_A:
        writer = new UPCAWriter();
        break;
      case QR_CODE:
        writer = new QRCodeWriter();
        break;
      case CODE_39:
        writer = new Code39Writer();
        break;
      case CODE_128:
        writer = new Code128Writer();
        break;
      case ITF:
        writer = new ITFWriter();
        break;
      case PDF_417:
        writer = new PDF417Writer();
        break;
      case CODABAR:
	      writer = new CodaBarWriter();
        break;
      default:
        throw new IllegalArgumentException("No encoder available for format " + format);
    }
    return writer.encode(contents, format, width, height, hints);
  }

}
