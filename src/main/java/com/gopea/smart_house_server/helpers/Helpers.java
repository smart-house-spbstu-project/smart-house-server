package com.gopea.smart_house_server.helpers;


import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Helpers {

  public static final String USER_TYPE_HEADER = "User-Type";
  public static final String USERNAME_HEADER = "Username";
  public static final String PASSWORDS_FILE = "passwords.json";
  public static final String INTERNAL_STATUS_KEY = "status";
  public static final String EXTERNAL_STATUS_KEY = "rest_status";
  public static final String MESSAGE_KEY = "message";

  public static byte[] encryptPassword(String password) {
    MessageDigest messageDigest = null;
    try {
      messageDigest = MessageDigest.getInstance("SHA-256");
      messageDigest.update(password.getBytes());
      return messageDigest.digest();
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    }
    return null;
  }

  private Helpers() {

  }
}
