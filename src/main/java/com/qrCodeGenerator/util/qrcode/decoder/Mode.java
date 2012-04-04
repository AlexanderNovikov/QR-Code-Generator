

package com.qrCodeGenerator.util.qrcode.decoder;


public enum Mode {

  TERMINATOR(new int[]{0, 0, 0}, 0x00),
  NUMERIC(new int[]{10, 12, 14}, 0x01),
  ALPHANUMERIC(new int[]{9, 11, 13}, 0x02),
  STRUCTURED_APPEND(new int[]{0, 0, 0}, 0x03),
  BYTE(new int[]{8, 16, 16}, 0x04),
  ECI(new int[]{0, 0, 0}, 0x07),
  KANJI(new int[]{8, 10, 12}, 0x08),
  FNC1_FIRST_POSITION(new int[]{0, 0, 0}, 0x05),
  FNC1_SECOND_POSITION(new int[]{0, 0, 0}, 0x09),

  HANZI(new int[]{8, 10, 12}, 0x0D);

  private final int[] characterCountBitsForVersions;
  private final int bits;

  Mode(int[] characterCountBitsForVersions, int bits) {
    this.characterCountBitsForVersions = characterCountBitsForVersions;
    this.bits = bits;
  }


  public static Mode forBits(int bits) {
    switch (bits) {
      case 0x0:
        return TERMINATOR;
      case 0x1:
        return NUMERIC;
      case 0x2:
        return ALPHANUMERIC;
      case 0x3:
        return STRUCTURED_APPEND;
      case 0x4:
        return BYTE;
      case 0x5:
        return FNC1_FIRST_POSITION;
      case 0x7:
        return ECI;
      case 0x8:
        return KANJI;
      case 0x9:
        return FNC1_SECOND_POSITION;
      case 0xD:

        return HANZI;
      default:
        throw new IllegalArgumentException();
    }
  }


  public int getCharacterCountBits(Version version) {
    int number = version.getVersionNumber();
    int offset;
    if (number <= 9) {
      offset = 0;
    } else if (number <= 26) {
      offset = 1;
    } else {
      offset = 2;
    }
    return characterCountBitsForVersions[offset];
  }

  public int getBits() {
    return bits;
  }

}
