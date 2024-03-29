

package com.qrCodeGenerator.util.oned;

import com.qrCodeGenerator.util.BarcodeFormat;
import com.qrCodeGenerator.util.NotFoundException;
import com.qrCodeGenerator.util.Result;
import com.qrCodeGenerator.util.ResultMetadataType;
import com.qrCodeGenerator.util.ResultPoint;
import com.qrCodeGenerator.util.common.BitArray;

import java.util.EnumMap;
import java.util.Map;

final class UPCEANExtensionSupport {

  private static final int[] EXTENSION_START_PATTERN = {1,1,2};
  private static final int[] CHECK_DIGIT_ENCODINGS = {
      0x18, 0x14, 0x12, 0x11, 0x0C, 0x06, 0x03, 0x0A, 0x09, 0x05
  };

  private final int[] decodeMiddleCounters = new int[4];
  private final StringBuilder decodeRowStringBuffer = new StringBuilder();

  Result decodeRow(int rowNumber, BitArray row, int rowOffset) throws NotFoundException {

    int[] extensionStartRange = UPCEANReader.findGuardPattern(row, rowOffset, false, EXTENSION_START_PATTERN);

    StringBuilder result = decodeRowStringBuffer;
    result.setLength(0);
    int end = decodeMiddle(row, extensionStartRange, result);

    String resultString = result.toString();
    Map<ResultMetadataType,Object> extensionData = parseExtensionString(resultString);

    Result extensionResult =
        new Result(resultString,
                   null,
                   new ResultPoint[] {
                       new ResultPoint((extensionStartRange[0] + extensionStartRange[1]) / 2.0f, (float) rowNumber),
                       new ResultPoint((float) end, (float) rowNumber),
                   },
                   BarcodeFormat.UPC_EAN_EXTENSION);
    if (extensionData != null) {
      extensionResult.putAllMetadata(extensionData);
    }
    return extensionResult;
  }

  int decodeMiddle(BitArray row,
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

    for (int x = 0; x < 5 && rowOffset < end; x++) {
      int bestMatch = UPCEANReader.decodeDigit(row, counters, rowOffset, UPCEANReader.L_AND_G_PATTERNS);
      resultString.append((char) ('0' + bestMatch % 10));
      for (int counter : counters) {
        rowOffset += counter;
      }
      if (bestMatch >= 10) {
        lgPatternFound |= 1 << (4 - x);
      }
      if (x != 4) {

        rowOffset = row.getNextSet(rowOffset);
        rowOffset = row.getNextUnset(rowOffset);
      }
    }

    if (resultString.length() != 5) {
      throw NotFoundException.getNotFoundInstance();
    }

    int checkDigit = determineCheckDigit(lgPatternFound);
    if (extensionChecksum(resultString.toString()) != checkDigit) {
      throw NotFoundException.getNotFoundInstance();
    }
    
    return rowOffset;
  }

  private static int extensionChecksum(CharSequence s) {
    int length = s.length();
    int sum = 0;
    for (int i = length - 2; i >= 0; i -= 2) {
      sum += (int) s.charAt(i) - (int) '0';
    }
    sum *= 3;
    for (int i = length - 1; i >= 0; i -= 2) {
      sum += (int) s.charAt(i) - (int) '0';
    }
    sum *= 3;
    return sum % 10;
  }

  private static int determineCheckDigit(int lgPatternFound)
      throws NotFoundException {
    for (int d = 0; d < 10; d++) {
      if (lgPatternFound == CHECK_DIGIT_ENCODINGS[d]) {
        return d;
      }
    }
    throw NotFoundException.getNotFoundInstance();
  }


  private static Map<ResultMetadataType,Object> parseExtensionString(String raw) {
    ResultMetadataType type;
    Object value;
    switch (raw.length()) {
      case 2:
        type = ResultMetadataType.ISSUE_NUMBER;
        value = parseExtension2String(raw);
        break;
      case 5:
        type = ResultMetadataType.SUGGESTED_PRICE;
        value = parseExtension5String(raw);
        break;
      default:
        return null;
    }
    if (value == null) {
      return null;
    }
    Map<ResultMetadataType,Object> result = new EnumMap<ResultMetadataType,Object>(ResultMetadataType.class);
    result.put(type, value);
    return result;
  }

  private static Integer parseExtension2String(String raw) {
    return Integer.valueOf(raw);
  }

  private static String parseExtension5String(String raw) {
    String currency;
    switch (raw.charAt(0)) {
      case '0':
        currency = "£";
        break;
      case '5':
        currency = "$";
        break;
      case '9':

        if ("90000".equals(raw)) {

          return null;
        }
        if ("99991".equals(raw)) {

          return "0.00";
        }
        if ("99990".equals(raw)) {
          return "Used";
        }

        currency = "";
        break;
      default:
        currency = "";
        break;
    }
    int rawAmount = Integer.parseInt(raw.substring(1));
    String unitsString = String.valueOf(rawAmount / 100);
    int hundredths = rawAmount % 100;
    String hundredthsString = hundredths < 10 ? "0" + hundredths : String.valueOf(hundredths);
    return currency + unitsString + '.' + hundredthsString;
  }

}
