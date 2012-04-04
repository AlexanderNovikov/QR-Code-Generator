



package com.qrCodeGenerator.util.pdf417.encoder;

import com.qrCodeGenerator.util.WriterException;

import java.math.BigInteger;
import java.util.Arrays;


final class PDF417HighLevelEncoder {


  private static final int TEXT_COMPACTION = 0;


  private static final int BYTE_COMPACTION = 1;


  private static final int NUMERIC_COMPACTION = 2;


  private static final int SUBMODE_ALPHA = 0;


  private static final int SUBMODE_LOWER = 1;


  private static final int SUBMODE_MIXED = 2;


  private static final int SUBMODE_PUNCTUATION = 3;


  private static final int LATCH_TO_TEXT = 900;


  private static final int LATCH_TO_BYTE_PADDED = 901;


  private static final int LATCH_TO_NUMERIC = 902;


  private static final int SHIFT_TO_BYTE = 913;


  private static final int LATCH_TO_BYTE = 924;


  private static final byte[] TEXT_MIXED_RAW = {
      48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 38, 13, 9, 44, 58,
      35, 45, 46, 36, 47, 43, 37, 42, 61, 94, 0, 32, 0, 0, 0};


  private static final byte[] TEXT_PUNCTUATION_RAW = {
      59, 60, 62, 64, 91, 92, 93, 95, 96, 126, 33, 13, 9, 44, 58,
      10, 45, 46, 36, 47, 34, 124, 42, 40, 41, 63, 123, 125, 39, 0};

  private static final byte[] MIXED = new byte[128];
  private static final byte[] PUNCTUATION = new byte[128];

  private PDF417HighLevelEncoder() {
  }

  static {

    Arrays.fill(MIXED, (byte) -1);
    for (byte i = 0; i < TEXT_MIXED_RAW.length; i++) {
      byte b = TEXT_MIXED_RAW[i];
      if (b > 0) {
        MIXED[b] = i;
      }
    }
    Arrays.fill(PUNCTUATION, (byte) -1);
    for (byte i = 0; i < TEXT_PUNCTUATION_RAW.length; i++) {
      byte b = TEXT_PUNCTUATION_RAW[i];
      if (b > 0) {
        PUNCTUATION[b] = i;
      }
    }
  }


  private static byte[] getBytesForMessage(String msg) {
    return msg.getBytes();
  }


  static String encodeHighLevel(String msg, Compaction compaction) throws WriterException {
    byte[] bytes = null;


    StringBuilder sb = new StringBuilder(msg.length());

    int len = msg.length();
    int p = 0;
    int encodingMode = TEXT_COMPACTION;
    int textSubMode = SUBMODE_ALPHA;


    if (compaction == Compaction.TEXT) {
      encodeText(msg, p, len, sb, textSubMode);

    } else if (compaction == Compaction.BYTE) {
      encodingMode = BYTE_COMPACTION;
      bytes = getBytesForMessage(msg);
      encodeBinary(bytes, p, bytes.length, encodingMode, sb);

    } else if (compaction == Compaction.NUMERIC) {
      encodingMode = NUMERIC_COMPACTION;
      sb.append((char) LATCH_TO_NUMERIC);
      encodeNumeric(msg, p, len, sb);

    } else {
      while (p < len) {
        int n = determineConsecutiveDigitCount(msg, p);
        if (n >= 13) {
          sb.append((char) LATCH_TO_NUMERIC);
          encodingMode = NUMERIC_COMPACTION;
          textSubMode = SUBMODE_ALPHA;
          encodeNumeric(msg, p, n, sb);
          p += n;
        } else {
          int t = determineConsecutiveTextCount(msg, p);
          if (t >= 5 || n == len) {
            if (encodingMode != TEXT_COMPACTION) {
              sb.append((char) LATCH_TO_TEXT);
              encodingMode = TEXT_COMPACTION;
              textSubMode = SUBMODE_ALPHA;
            }
            textSubMode = encodeText(msg, p, t, sb, textSubMode);
            p += t;
          } else {
            if (bytes == null) {
              bytes = getBytesForMessage(msg);
            }
            int b = determineConsecutiveBinaryCount(msg, bytes, p);
            if (b == 0) {
              b = 1;
            }
            if (b == 1 && encodingMode == TEXT_COMPACTION) {

              encodeBinary(bytes, p, 1, TEXT_COMPACTION, sb);
            } else {

              encodeBinary(bytes, p, b, encodingMode, sb);
              encodingMode = BYTE_COMPACTION;
              textSubMode = SUBMODE_ALPHA;
            }
            p += b;
          }
        }
      }
    }

    return sb.toString();
  }


  private static int encodeText(CharSequence msg,
                                int startpos,
                                int count,
                                StringBuilder sb,
                                int initialSubmode) {
    StringBuilder tmp = new StringBuilder(count);
    int submode = initialSubmode;
    int idx = 0;
    while (true) {
      char ch = msg.charAt(startpos + idx);
      switch (submode) {
        case SUBMODE_ALPHA:
          if (isAlphaUpper(ch)) {
            if (ch == ' ') {
              tmp.append((char) 26);
            } else {
              tmp.append((char) (ch - 65));
            }
          } else {
            if (isAlphaLower(ch)) {
              submode = SUBMODE_LOWER;
              tmp.append((char) 27);
              continue;
            } else if (isMixed(ch)) {
              submode = SUBMODE_MIXED;
              tmp.append((char) 28);
              continue;
            } else {
              tmp.append((char) 29);
              tmp.append((char) PUNCTUATION[ch]);
              break;
            }
          }
          break;
        case SUBMODE_LOWER:
          if (isAlphaLower(ch)) {
            if (ch == ' ') {
              tmp.append((char) 26);
            } else {
              tmp.append((char) (ch - 97));
            }
          } else {
            if (isAlphaUpper(ch)) {
              tmp.append((char) 27);
              tmp.append((char) (ch - 65));

              break;
            } else if (isMixed(ch)) {
              submode = SUBMODE_MIXED;
              tmp.append((char) 28);
              continue;
            } else {
              tmp.append((char) 29);
              tmp.append((char) PUNCTUATION[ch]);
              break;
            }
          }
          break;
        case SUBMODE_MIXED:
          if (isMixed(ch)) {
            tmp.append((char) MIXED[ch]);
          } else {
            if (isAlphaUpper(ch)) {
              submode = SUBMODE_ALPHA;
              tmp.append((char) 28);
              continue;
            } else if (isAlphaLower(ch)) {
              submode = SUBMODE_LOWER;
              tmp.append((char) 27);
              continue;
            } else {
              if (startpos + idx + 1 < count) {
                char next = msg.charAt(startpos + idx + 1);
                if (isPunctuation(next)) {
                  submode = SUBMODE_PUNCTUATION;
                  tmp.append((char) 25);
                  continue;
                }
              }
              tmp.append((char) 29);
              tmp.append((char) PUNCTUATION[ch]);
            }
          }
          break;
        default:
          if (isPunctuation(ch)) {
            tmp.append((char) PUNCTUATION[ch]);
          } else {
            submode = SUBMODE_ALPHA;
            tmp.append((char) 29);
            continue;
          }
      }
      idx++;
      if (idx >= count) {
        break;
      }
    }
    char h = 0;
    int len = tmp.length();
    for (int i = 0; i < len; i++) {
      boolean odd = (i % 2) != 0;
      if (odd) {
        h = (char) ((h * 30) + tmp.charAt(i));
        sb.append(h);
      } else {
        h = tmp.charAt(i);
      }
    }
    if ((len % 2) != 0) {
      sb.append((char) ((h * 30) + 29));
    }
    return submode;
  }


  private static void encodeBinary(byte[] bytes,
                                   int startpos,
                                   int count,
                                   int startmode,
                                   StringBuilder sb) {
    if (count == 1 && startmode == TEXT_COMPACTION) {
      sb.append((char) SHIFT_TO_BYTE);
    }

    int idx = startpos;

    if (count >= 6) {
      sb.append((char) LATCH_TO_BYTE);
      char[] chars = new char[5];
      while ((startpos + count - idx) >= 6) {
        long t = 0;
        for (int i = 0; i < 6; i++) {
          t <<= 8;
          t += bytes[idx + i] & 0xff;
        }
        for (int i = 0; i < 5; i++) {
          chars[i] = (char) (t % 900);
          t /= 900;
        }
        for (int i = chars.length - 1; i >= 0; i--) {
          sb.append(chars[i]);
        }
        idx += 6;
      }
    }

    if (idx < startpos + count) {
      sb.append((char) LATCH_TO_BYTE_PADDED);
    }
    for (int i = idx; i < startpos + count; i++) {
      int ch = bytes[i] & 0xff;
      sb.append((char) ch);
    }
  }

  private static void encodeNumeric(String msg, int startpos, int count, StringBuilder sb) {
    int idx = 0;
    StringBuilder tmp = new StringBuilder(count / 3 + 1);
    BigInteger num900 = BigInteger.valueOf(900);
    BigInteger num0 = BigInteger.valueOf(0);
    while (idx < count - 1) {
      tmp.setLength(0);
      int len = Math.min(44, count - idx);
      String part = '1' + msg.substring(startpos + idx, startpos + idx + len);
      BigInteger bigint = new BigInteger(part);
      do {
        BigInteger c = bigint.mod(num900);
        tmp.append((char) c.intValue());
        bigint = bigint.divide(num900);
      } while (!bigint.equals(num0));


      for (int i = tmp.length() - 1; i >= 0; i--) {
        sb.append(tmp.charAt(i));
      }
      idx += len;
    }
  }


  private static boolean isDigit(char ch) {
    return ch >= '0' && ch <= '9';
  }

  private static boolean isAlphaUpper(char ch) {
    return ch == ' ' || (ch >= 'A' && ch <= 'Z');
  }

  private static boolean isAlphaLower(char ch) {
    return ch == ' ' || (ch >= 'a' && ch <= 'z');
  }

  private static boolean isMixed(char ch) {
    return MIXED[ch] != -1;
  }

  private static boolean isPunctuation(char ch) {
    return PUNCTUATION[ch] != -1;
  }

  private static boolean isText(char ch) {
    return ch == '\t' || ch == '\n' || ch == '\r' || (ch >= 32 && ch <= 126);
  }


  private static int determineConsecutiveDigitCount(CharSequence msg, int startpos) {
    int count = 0;
    int len = msg.length();
    int idx = startpos;
    if (idx < len) {
      char ch = msg.charAt(idx);
      while (isDigit(ch) && idx < len) {
        count++;
        idx++;
        if (idx < len) {
          ch = msg.charAt(idx);
        }
      }
    }
    return count;
  }


  private static int determineConsecutiveTextCount(CharSequence msg, int startpos) {
    int len = msg.length();
    int idx = startpos;
    while (idx < len) {
      char ch = msg.charAt(idx);
      int numericCount = 0;
      while (numericCount < 13 && isDigit(ch) && idx < len) {
        numericCount++;
        idx++;
        if (idx < len) {
          ch = msg.charAt(idx);
        }
      }
      if (numericCount >= 13) {
        return idx - startpos - numericCount;
      }
      if (numericCount > 0) {

        continue;
      }
      ch = msg.charAt(idx);


      if (!isText(ch)) {
        break;
      }
      idx++;
    }
    return idx - startpos;
  }


  private static int determineConsecutiveBinaryCount(CharSequence msg, byte[] bytes, int startpos)
      throws WriterException {
    int len = msg.length();
    int idx = startpos;
    while (idx < len) {
      char ch = msg.charAt(idx);
      int numericCount = 0;

      while (numericCount < 13 && isDigit(ch)) {
        numericCount++;

        int i = idx + numericCount;
        if (i >= len) {
          break;
        }
        ch = msg.charAt(i);
      }
      if (numericCount >= 13) {
        return idx - startpos;
      }
      int textCount = 0;
      while (textCount < 5 && isText(ch)) {
        textCount++;
        int i = idx + textCount;
        if (i >= len) {
          break;
        }
        ch = msg.charAt(i);
      }
      if (textCount >= 5) {
        return idx - startpos;
      }
      ch = msg.charAt(idx);




      if (bytes[idx] == 63 && ch != '?') {
        throw new WriterException("Non-encodable character detected: " + ch + " (Unicode: " + (int) ch + ')');
      }
      idx++;
    }
    return idx - startpos;
  }


}
