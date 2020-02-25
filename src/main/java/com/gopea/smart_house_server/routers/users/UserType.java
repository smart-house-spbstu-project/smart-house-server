package com.gopea.smart_house_server.routers.users;

public enum UserType {
  ADMIN,
  USER;

  public static UserType getEnum(String name) {
    if (name == null) {
      return null;
    }
    name = name.toUpperCase();
    UserType userType = null;
    try {
      userType = UserType.valueOf(name);
    } catch (IllegalArgumentException e) {
    }
    return userType;
  }
}
