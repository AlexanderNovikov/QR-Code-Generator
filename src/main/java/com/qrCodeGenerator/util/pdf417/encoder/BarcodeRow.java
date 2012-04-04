

package com.qrCodeGenerator.util.pdf417.encoder;


final class BarcodeRow {

  private final byte[] row;

  private int currentLocation;


  BarcodeRow(int width) {
    this.row = new byte[width];
    currentLocation = 0;
  }


  void set(int x, byte value) {
    row[x] = value;
  }


  void set(int x, boolean black) {
    row[x] = (byte) (black ? 1 : 0);
  }


  void addBar(boolean black, int width) {
    for (int ii = 0; ii < width; ii++) {
      set(currentLocation++, black);
    }
  }

  byte[] getRow() {
    return row;
  }


  byte[] getScaledRow(int scale) {
    byte[] output = new byte[row.length * scale];
    for (int i = 0; i < output.length; i++) {
      output[i] = row[i / scale];
    }
    return output;
  }
}
