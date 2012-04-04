

package com.qrCodeGenerator.util.qrcode.decoder;


public enum ErrorCorrectionLevel {


  L(0x01),

  M(0x00),

  Q(0x03),

  H(0x02);

  private static final ErrorCorrectionLevel[] FOR_BITS = {M, L, H, Q};

  private final int bits;

  ErrorCorrectionLevel(int bits) {
    this.bits = bits;
  }

  public int getBits() {
    return bits;
  }


  public static ErrorCorrectionLevel forBits(int bits) {
    if (bits < 0 || bits >= FOR_BITS.length) {
      throw new IllegalArgumentException();
    }
    return FOR_BITS[bits];
  }


}
