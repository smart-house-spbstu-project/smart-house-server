package com.gopea.smart_house_server.configs;

import com.gopea.smart_house_server.routers.users.User;
import com.gopea.smart_house_server.routers.users.UserType;

import static com.gopea.smart_house_server.common.Helpers.encryptPassword;


public final class StandardCredentials {
  public static final String ADMIN_PASSWORD = "admin";
  public static final User ADMIN = new User("admin", UserType.ADMIN, encryptPassword(ADMIN_PASSWORD));

  private StandardCredentials() {

  }
}
