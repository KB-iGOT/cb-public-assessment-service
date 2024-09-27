/** */
package com.assessment.datasecurity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * This class will do one way data hashing.
 *
 * @author Manzarul
 */
public class OneWayHashing {

  private static Logger logger = LoggerFactory.getLogger(OneWayHashing.class.getName());

  private OneWayHashing() {}

  /**
   * This method will encrypt value using SHA-256 . it is one way encryption.
   *
   * @param val String
   * @return String encrypted value or empty in case of exception
   */
  public static String encryptVal(String val) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      md.update(val.getBytes(StandardCharsets.UTF_8));
      byte byteData[] = md.digest();
      // convert the byte to hex format method 1
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < byteData.length; i++) {
        sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
      }
      return sb.toString();
    } catch (Exception e) {
      logger.error("Error while encrypting", e);
    }
    return "";
  }
}
