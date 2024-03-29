

package com.qrCodeGenerator.util.qrcode.encoder;

import com.qrCodeGenerator.util.EncodeHintType;
import com.qrCodeGenerator.util.WriterException;
import com.qrCodeGenerator.util.common.BitArray;
import com.qrCodeGenerator.util.common.CharacterSetECI;
import com.qrCodeGenerator.util.common.reedsolomon.GenericGF;
import com.qrCodeGenerator.util.common.reedsolomon.ReedSolomonEncoder;
import com.qrCodeGenerator.util.qrcode.decoder.ErrorCorrectionLevel;
import com.qrCodeGenerator.util.qrcode.decoder.Mode;
import com.qrCodeGenerator.util.qrcode.decoder.Version;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;


public final class Encoder {


  private static final int[] ALPHANUMERIC_TABLE = {
      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
      36, -1, -1, -1, 37, 38, -1, -1, -1, -1, 39, 40, -1, 41, 42, 43,
      0,   1,  2,  3,  4,  5,  6,  7,  8,  9, 44, -1, -1, -1, -1, -1,
      -1, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24,
      25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, -1, -1, -1, -1, -1,
  };

  static final String DEFAULT_BYTE_MODE_ENCODING = "ISO-8859-1";

  private Encoder() {
  }



  private static int calculateMaskPenalty(ByteMatrix matrix) {
    int penalty = 0;
    penalty += MaskUtil.applyMaskPenaltyRule1(matrix);
    penalty += MaskUtil.applyMaskPenaltyRule2(matrix);
    penalty += MaskUtil.applyMaskPenaltyRule3(matrix);
    penalty += MaskUtil.applyMaskPenaltyRule4(matrix);
    return penalty;
  }


  public static void encode(String content, ErrorCorrectionLevel ecLevel, QRCode qrCode)
      throws WriterException {
    encode(content, ecLevel, null, qrCode);
  }

  public static void encode(String content,
                            ErrorCorrectionLevel ecLevel,
                            Map<EncodeHintType,?> hints,
                            QRCode qrCode) throws WriterException {

    String encoding = hints == null ? null : (String) hints.get(EncodeHintType.CHARACTER_SET);
    if (encoding == null) {
      encoding = DEFAULT_BYTE_MODE_ENCODING;
    }


    Mode mode = chooseMode(content, encoding);


    BitArray dataBits = new BitArray();
    appendBytes(content, mode, dataBits, encoding);

    int numInputBits = dataBits.getSize();
    initQRCode(numInputBits, ecLevel, mode, qrCode);


    BitArray headerAndDataBits = new BitArray();


    if (mode == Mode.BYTE && !DEFAULT_BYTE_MODE_ENCODING.equals(encoding)) {
      CharacterSetECI eci = CharacterSetECI.getCharacterSetECIByName(encoding);
      if (eci != null) {
        appendECI(eci, headerAndDataBits);
      }
    }

    appendModeInfo(mode, headerAndDataBits);

    int numLetters = mode == Mode.BYTE ? dataBits.getSizeInBytes() : content.length();
    appendLengthInfo(numLetters, qrCode.getVersion(), mode, headerAndDataBits);
    headerAndDataBits.appendBitArray(dataBits);


    terminateBits(qrCode.getNumDataBytes(), headerAndDataBits);


    BitArray finalBits = new BitArray();
    interleaveWithECBytes(headerAndDataBits, qrCode.getNumTotalBytes(), qrCode.getNumDataBytes(),
        qrCode.getNumRSBlocks(), finalBits);


    ByteMatrix matrix = new ByteMatrix(qrCode.getMatrixWidth(), qrCode.getMatrixWidth());
    qrCode.setMaskPattern(chooseMaskPattern(finalBits, ecLevel, qrCode.getVersion(), matrix));


    MatrixUtil.buildMatrix(finalBits, ecLevel, qrCode.getVersion(), qrCode.getMaskPattern(), matrix);
    qrCode.setMatrix(matrix);

    if (!qrCode.isValid()) {
      throw new WriterException("Invalid QR code: " + qrCode.toString());
    }
  }


  static int getAlphanumericCode(int code) {
    if (code < ALPHANUMERIC_TABLE.length) {
      return ALPHANUMERIC_TABLE[code];
    }
    return -1;
  }

  public static Mode chooseMode(String content) {
    return chooseMode(content, null);
  }


  private static Mode chooseMode(String content, String encoding) {
    if ("Shift_JIS".equals(encoding)) {

      return isOnlyDoubleByteKanji(content) ? Mode.KANJI : Mode.BYTE;
    }
    boolean hasNumeric = false;
    boolean hasAlphanumeric = false;
    for (int i = 0; i < content.length(); ++i) {
      char c = content.charAt(i);
      if (c >= '0' && c <= '9') {
        hasNumeric = true;
      } else if (getAlphanumericCode(c) != -1) {
        hasAlphanumeric = true;
      } else {
        return Mode.BYTE;
      }
    }
    if (hasAlphanumeric) {
      return Mode.ALPHANUMERIC;
    }
    if (hasNumeric) {
      return Mode.NUMERIC;
    }
    return Mode.BYTE;
  }

  private static boolean isOnlyDoubleByteKanji(String content) {
    byte[] bytes;
    try {
      bytes = content.getBytes("Shift_JIS");
    } catch (UnsupportedEncodingException uee) {
      return false;
    }
    int length = bytes.length;
    if (length % 2 != 0) {
      return false;
    }
    for (int i = 0; i < length; i += 2) {
      int byte1 = bytes[i] & 0xFF;
      if ((byte1 < 0x81 || byte1 > 0x9F) && (byte1 < 0xE0 || byte1 > 0xEB)) {
        return false;
      }
    }
    return true;
  }

  private static int chooseMaskPattern(BitArray bits,
                                       ErrorCorrectionLevel ecLevel,
                                       int version,
                                       ByteMatrix matrix) throws WriterException {

    int minPenalty = Integer.MAX_VALUE;
    int bestMaskPattern = -1;

    for (int maskPattern = 0; maskPattern < QRCode.NUM_MASK_PATTERNS; maskPattern++) {
      MatrixUtil.buildMatrix(bits, ecLevel, version, maskPattern, matrix);
      int penalty = calculateMaskPenalty(matrix);
      if (penalty < minPenalty) {
        minPenalty = penalty;
        bestMaskPattern = maskPattern;
      }
    }
    return bestMaskPattern;
  }


  private static void initQRCode(int numInputBits,
                                 ErrorCorrectionLevel ecLevel,
                                 Mode mode,
                                 QRCode qrCode) throws WriterException {
    qrCode.setECLevel(ecLevel);
    qrCode.setMode(mode);


    for (int versionNum = 1; versionNum <= 40; versionNum++) {
      Version version = Version.getVersionForNumber(versionNum);

      int numBytes = version.getTotalCodewords();

      Version.ECBlocks ecBlocks = version.getECBlocksForLevel(ecLevel);
      int numEcBytes = ecBlocks.getTotalECCodewords();

      int numRSBlocks = ecBlocks.getNumBlocks();

      int numDataBytes = numBytes - numEcBytes;



      if (numDataBytes >= getTotalInputBytes(numInputBits, version, mode)) {

        qrCode.setVersion(versionNum);
        qrCode.setNumTotalBytes(numBytes);
        qrCode.setNumDataBytes(numDataBytes);
        qrCode.setNumRSBlocks(numRSBlocks);

        qrCode.setNumECBytes(numEcBytes);

        qrCode.setMatrixWidth(version.getDimensionForVersion());
        return;
      }
    }
    throw new WriterException("Cannot find proper rs block info (input data too big?)");
  }
  
  private static int getTotalInputBytes(int numInputBits, Version version, Mode mode) {
    int modeInfoBits = 4;
    int charCountBits = mode.getCharacterCountBits(version);
    int headerBits = modeInfoBits + charCountBits;
    int totalBits = numInputBits + headerBits;
      
    return (totalBits + 7) / 8;
  }


  static void terminateBits(int numDataBytes, BitArray bits) throws WriterException {
    int capacity = numDataBytes << 3;
    if (bits.getSize() > capacity) {
      throw new WriterException("data bits cannot fit in the QR Code" + bits.getSize() + " > " +
          capacity);
    }
    for (int i = 0; i < 4 && bits.getSize() < capacity; ++i) {
      bits.appendBit(false);
    }


    int numBitsInLastByte = bits.getSize() & 0x07;    
    if (numBitsInLastByte > 0) {
      for (int i = numBitsInLastByte; i < 8; i++) {
        bits.appendBit(false);
      }
    }

    int numPaddingBytes = numDataBytes - bits.getSizeInBytes();
    for (int i = 0; i < numPaddingBytes; ++i) {
      bits.appendBits((i & 0x01) == 0 ? 0xEC : 0x11, 8);
    }
    if (bits.getSize() != capacity) {
      throw new WriterException("Bits size does not equal capacity");
    }
  }


  static void getNumDataBytesAndNumECBytesForBlockID(int numTotalBytes,
                                                     int numDataBytes,
                                                     int numRSBlocks,
                                                     int blockID,
                                                     int[] numDataBytesInBlock,
                                                     int[] numECBytesInBlock) throws WriterException {
    if (blockID >= numRSBlocks) {
      throw new WriterException("Block ID too large");
    }

    int numRsBlocksInGroup2 = numTotalBytes % numRSBlocks;

    int numRsBlocksInGroup1 = numRSBlocks - numRsBlocksInGroup2;

    int numTotalBytesInGroup1 = numTotalBytes / numRSBlocks;

    int numTotalBytesInGroup2 = numTotalBytesInGroup1 + 1;

    int numDataBytesInGroup1 = numDataBytes / numRSBlocks;

    int numDataBytesInGroup2 = numDataBytesInGroup1 + 1;

    int numEcBytesInGroup1 = numTotalBytesInGroup1 - numDataBytesInGroup1;

    int numEcBytesInGroup2 = numTotalBytesInGroup2 - numDataBytesInGroup2;


    if (numEcBytesInGroup1 != numEcBytesInGroup2) {
      throw new WriterException("EC bytes mismatch");
    }

    if (numRSBlocks != numRsBlocksInGroup1 + numRsBlocksInGroup2) {
      throw new WriterException("RS blocks mismatch");
    }

    if (numTotalBytes !=
        ((numDataBytesInGroup1 + numEcBytesInGroup1) *
            numRsBlocksInGroup1) +
            ((numDataBytesInGroup2 + numEcBytesInGroup2) *
                numRsBlocksInGroup2)) {
      throw new WriterException("Total bytes mismatch");
    }

    if (blockID < numRsBlocksInGroup1) {
      numDataBytesInBlock[0] = numDataBytesInGroup1;
      numECBytesInBlock[0] = numEcBytesInGroup1;
    } else {
      numDataBytesInBlock[0] = numDataBytesInGroup2;
      numECBytesInBlock[0] = numEcBytesInGroup2;
    }
  }


  static void interleaveWithECBytes(BitArray bits,
                                    int numTotalBytes,
                                    int numDataBytes,
                                    int numRSBlocks,
                                    BitArray result) throws WriterException {


    if (bits.getSizeInBytes() != numDataBytes) {
      throw new WriterException("Number of bits and data bytes does not match");
    }



    int dataBytesOffset = 0;
    int maxNumDataBytes = 0;
    int maxNumEcBytes = 0;


    Collection<BlockPair> blocks = new ArrayList<BlockPair>(numRSBlocks);

    for (int i = 0; i < numRSBlocks; ++i) {
      int[] numDataBytesInBlock = new int[1];
      int[] numEcBytesInBlock = new int[1];
      getNumDataBytesAndNumECBytesForBlockID(
          numTotalBytes, numDataBytes, numRSBlocks, i,
          numDataBytesInBlock, numEcBytesInBlock);

      int size = numDataBytesInBlock[0];
      byte[] dataBytes = new byte[size];
      bits.toBytes(8*dataBytesOffset, dataBytes, 0, size);
      byte[] ecBytes = generateECBytes(dataBytes, numEcBytesInBlock[0]);
      blocks.add(new BlockPair(dataBytes, ecBytes));

      maxNumDataBytes = Math.max(maxNumDataBytes, size);
      maxNumEcBytes = Math.max(maxNumEcBytes, ecBytes.length);
      dataBytesOffset += numDataBytesInBlock[0];
    }
    if (numDataBytes != dataBytesOffset) {
      throw new WriterException("Data bytes does not match offset");
    }


    for (int i = 0; i < maxNumDataBytes; ++i) {
      for (BlockPair block : blocks) {
        byte[] dataBytes = block.getDataBytes();
        if (i < dataBytes.length) {
          result.appendBits(dataBytes[i], 8);
        }
      }
    }

    for (int i = 0; i < maxNumEcBytes; ++i) {
      for (BlockPair block : blocks) {
        byte[] ecBytes = block.getErrorCorrectionBytes();
        if (i < ecBytes.length) {
          result.appendBits(ecBytes[i], 8);
        }
      }
    }
    if (numTotalBytes != result.getSizeInBytes()) {
      throw new WriterException("Interleaving error: " + numTotalBytes + " and " +
          result.getSizeInBytes() + " differ.");
    }
  }

  static byte[] generateECBytes(byte[] dataBytes, int numEcBytesInBlock) {
    int numDataBytes = dataBytes.length;
    int[] toEncode = new int[numDataBytes + numEcBytesInBlock];
    for (int i = 0; i < numDataBytes; i++) {
      toEncode[i] = dataBytes[i] & 0xFF;
    }
    new ReedSolomonEncoder(GenericGF.QR_CODE_FIELD_256).encode(toEncode, numEcBytesInBlock);

    byte[] ecBytes = new byte[numEcBytesInBlock];
    for (int i = 0; i < numEcBytesInBlock; i++) {
      ecBytes[i] = (byte) toEncode[numDataBytes + i];
    }
    return ecBytes;
  }


  static void appendModeInfo(Mode mode, BitArray bits) {
    bits.appendBits(mode.getBits(), 4);
  }



  static void appendLengthInfo(int numLetters, int version, Mode mode, BitArray bits)
      throws WriterException {
    int numBits = mode.getCharacterCountBits(Version.getVersionForNumber(version));
    if (numLetters > ((1 << numBits) - 1)) {
      throw new WriterException(numLetters + "is bigger than" + ((1 << numBits) - 1));
    }
    bits.appendBits(numLetters, numBits);
  }


  static void appendBytes(String content,
                          Mode mode,
                          BitArray bits,
                          String encoding) throws WriterException {
    switch (mode) {
      case NUMERIC:
        appendNumericBytes(content, bits);
        break;
      case ALPHANUMERIC:
        appendAlphanumericBytes(content, bits);
        break;
      case BYTE:
        append8BitBytes(content, bits, encoding);
        break;
      case KANJI:
        appendKanjiBytes(content, bits);
        break;
      default:
        throw new WriterException("Invalid mode: " + mode);
    }
  }

  static void appendNumericBytes(CharSequence content, BitArray bits) {
    int length = content.length();
    int i = 0;
    while (i < length) {
      int num1 = content.charAt(i) - '0';
      if (i + 2 < length) {

        int num2 = content.charAt(i + 1) - '0';
        int num3 = content.charAt(i + 2) - '0';
        bits.appendBits(num1 * 100 + num2 * 10 + num3, 10);
        i += 3;
      } else if (i + 1 < length) {

        int num2 = content.charAt(i + 1) - '0';
        bits.appendBits(num1 * 10 + num2, 7);
        i += 2;
      } else {

        bits.appendBits(num1, 4);
        i++;
      }
    }
  }

  static void appendAlphanumericBytes(CharSequence content, BitArray bits) throws WriterException {
    int length = content.length();
    int i = 0;
    while (i < length) {
      int code1 = getAlphanumericCode(content.charAt(i));
      if (code1 == -1) {
        throw new WriterException();
      }
      if (i + 1 < length) {
        int code2 = getAlphanumericCode(content.charAt(i + 1));
        if (code2 == -1) {
          throw new WriterException();
        }

        bits.appendBits(code1 * 45 + code2, 11);
        i += 2;
      } else {

        bits.appendBits(code1, 6);
        i++;
      }
    }
  }

  static void append8BitBytes(String content, BitArray bits, String encoding)
      throws WriterException {
    byte[] bytes;
    try {
      bytes = content.getBytes(encoding);
    } catch (UnsupportedEncodingException uee) {
      throw new WriterException(uee.toString());
    }
    for (byte b : bytes) {
      bits.appendBits(b, 8);
    }
  }

  static void appendKanjiBytes(String content, BitArray bits) throws WriterException {
    byte[] bytes;
    try {
      bytes = content.getBytes("Shift_JIS");
    } catch (UnsupportedEncodingException uee) {
      throw new WriterException(uee.toString());
    }
    int length = bytes.length;
    for (int i = 0; i < length; i += 2) {
      int byte1 = bytes[i] & 0xFF;
      int byte2 = bytes[i + 1] & 0xFF;
      int code = (byte1 << 8) | byte2;
      int subtracted = -1;
      if (code >= 0x8140 && code <= 0x9ffc) {
        subtracted = code - 0x8140;
      } else if (code >= 0xe040 && code <= 0xebbf) {
        subtracted = code - 0xc140;
      }
      if (subtracted == -1) {
        throw new WriterException("Invalid byte sequence");
      }
      int encoded = ((subtracted >> 8) * 0xc0) + (subtracted & 0xff);
      bits.appendBits(encoded, 13);
    }
  }

  private static void appendECI(CharacterSetECI eci, BitArray bits) {
    bits.appendBits(Mode.ECI.getBits(), 4);

    bits.appendBits(eci.getValue(), 8);
  }

}
