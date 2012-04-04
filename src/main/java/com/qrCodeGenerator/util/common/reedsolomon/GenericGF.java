

package com.qrCodeGenerator.util.common.reedsolomon;


public final class GenericGF {

  public static final GenericGF AZTEC_DATA_12 = new GenericGF(0x1069, 4096);
  public static final GenericGF AZTEC_DATA_10 = new GenericGF(0x409, 1024);
  public static final GenericGF AZTEC_DATA_6 = new GenericGF(0x43, 64);
  public static final GenericGF AZTEC_PARAM = new GenericGF(0x13, 16);
  public static final GenericGF QR_CODE_FIELD_256 = new GenericGF(0x011D, 256);
  public static final GenericGF DATA_MATRIX_FIELD_256 = new GenericGF(0x012D, 256);
  public static final GenericGF AZTEC_DATA_8 = DATA_MATRIX_FIELD_256;
  public static final GenericGF MAXICODE_FIELD_64 = AZTEC_DATA_6;

  private static final int INITIALIZATION_THRESHOLD = 0;

  private int[] expTable;
  private int[] logTable;
  private GenericGFPoly zero;
  private GenericGFPoly one;
  private final int size;
  private final int primitive;
  private boolean initialized = false;


  public GenericGF(int primitive, int size) {
  	this.primitive = primitive;
    this.size = size;
    
    if (size <= INITIALIZATION_THRESHOLD){
    	initialize();
    }
  }

  private void initialize(){
    expTable = new int[size];
    logTable = new int[size];
    int x = 1;
    for (int i = 0; i < size; i++) {
      expTable[i] = x;
      x <<= 1;
      if (x >= size) {
        x ^= primitive;
        x &= size-1;
      }
    }
    for (int i = 0; i < size-1; i++) {
      logTable[expTable[i]] = i;
    }

    zero = new GenericGFPoly(this, new int[]{0});
    one = new GenericGFPoly(this, new int[]{1});
    initialized = true;
  }
  
  private void checkInit(){
  	if (!initialized) {
      initialize();
    }
  }
  
  GenericGFPoly getZero() {
  	checkInit();
  	
    return zero;
  }

  GenericGFPoly getOne() {
  	checkInit();
  	
    return one;
  }


  GenericGFPoly buildMonomial(int degree, int coefficient) {
  	checkInit();
  	
    if (degree < 0) {
      throw new IllegalArgumentException();
    }
    if (coefficient == 0) {
      return zero;
    }
    int[] coefficients = new int[degree + 1];
    coefficients[0] = coefficient;
    return new GenericGFPoly(this, coefficients);
  }


  static int addOrSubtract(int a, int b) {
    return a ^ b;
  }


  int exp(int a) {
  	checkInit();
  	
    return expTable[a];
  }


  int log(int a) {
  	checkInit();
  	
    if (a == 0) {
      throw new IllegalArgumentException();
    }
    return logTable[a];
  }


  int inverse(int a) {
  	checkInit();
  	
    if (a == 0) {
      throw new ArithmeticException();
    }
    return expTable[size - logTable[a] - 1];
  }


  int multiply(int a, int b) {
  	checkInit();
  	
    if (a == 0 || b == 0) {
      return 0;
    }
    
    if (a<0 || b <0 || a>=size || b >=size){
    	a++;
    }
    
    int logSum = logTable[a] + logTable[b];
    return expTable[(logSum % size) + logSum / size];
  }

  public int getSize(){
  	return size;
  }
  
}
