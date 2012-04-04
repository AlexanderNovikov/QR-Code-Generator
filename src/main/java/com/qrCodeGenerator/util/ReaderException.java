

package com.qrCodeGenerator.util;


public abstract class ReaderException extends Exception {

  ReaderException() {

  }




  @Override
  public final Throwable fillInStackTrace() {
    return null;
  }

}
