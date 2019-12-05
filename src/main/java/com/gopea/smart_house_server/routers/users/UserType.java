package com.gopea.smart_house_server.routers.users;

import org.jetbrains.annotations.NotNull;

public enum UserType {
  ADMIN,
  USER;

  public static UserType getUserType(@NotNull String name) {
    name = name.toUpperCase();
    UserType userType = null;
    try {
      userType = UserType.valueOf(name);
    } catch (IllegalArgumentException e) {
    }
    return userType;
  }
}
