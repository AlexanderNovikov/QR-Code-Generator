

package com.qrCodeGenerator.util.pdf417.encoder;


final class BarcodeMatrix {

  private final BarcodeRow[] matrix;
  private int currentRow;
  private final int height;
  private final int width;


  BarcodeMatrix(int height, int width) {
    matrix = new BarcodeRow[height + 2];

    for (int i = 0, matrixLength = matrix.length; i < matrixLength; i++) {
      matrix[i] = new BarcodeRow((width + 4) * 17 + 1);
    }
    this.width = width * 17;
    this.height = height + 2;
    this.currentRow = 0;
  }

  void set(int x, int y, byte value) {
    matrix[y].set(x, value);
  }

  void setMatrix(int x, int y, boolean black) {
    set(x, y, (byte) (black ? 1 : 0));
  }

  void startRow() {
    ++currentRow;
  }

  BarcodeRow getCurrentRow() {
    return matrix[currentRow];
  }

  byte[][] getMatrix() {
    return getScaledMatrix(1, 1);
  }

  byte[][] getScaledMatrix(int Scale) {
    return getScaledMatrix(Scale, Scale);
  }

  byte[][] getScaledMatrix(int xScale, int yScale) {
    byte[][] matrixOut = new byte[height * yScale][width * xScale];
    int yMax = height * yScale;
    for (int ii = 0; ii < yMax; ii++) {
      matrixOut[yMax - ii - 1] = matrix[ii / yScale].getScaledRow(xScale);
    }
    return matrixOut;
  }
}
