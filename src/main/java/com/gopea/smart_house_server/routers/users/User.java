package com.gopea.smart_house_server.routers.users;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

import static com.gopea.smart_house_server.helpers.Helpers.encryptPassword;

public class User {
  private final UserType userType;
  private final String username;
  private byte[] password;

  public User(String username, UserType userType, byte[] password) {
    this.userType = userType;
    this.username = username;
    this.password = password;
  }

  public User(String username, UserType userType, String password) {
    this(username, userType, encryptPassword(password));
  }

  /**
   * Getter for the userType.
   *
   * @return The userType.
   */
  public UserType getUserType() {
    return userType;
  }

  /**
   * Getter for the userName.
   *
   * @return The userName.
   */
  public String getUsername() {
    return username;
  }

  /**
   * Getter for the password.
   *
   * @return The password.
   */
  public byte[] getPassword() {
    return password;
  }

  /**
   * Setter for the password.
   *
   * @param password The password.
   * @return This, so the API can be used fluently.
   */
  public User setPassword(byte[] password) {
    this.password = password;
    return this;
  }


  public User setPassword(String password) {
    return setPassword(encryptPassword(password));
  }

  public boolean checkPassword(String password) {
    byte[] encodePassword = encryptPassword(password);
    return Arrays.equals(encodePassword, this.password);
  }

}


