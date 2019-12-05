package com.gopea.smart_house_server.routers.users;


import com.gopea.smart_house_server.data_base.FileUserStorage;
import com.gopea.smart_house_server.data_base.UserStorage;
import com.gopea.smart_house_server.helpers.Helpers;
import io.vertx.reactivex.core.Vertx;

public final class Users {
  public static final UserStorage USER_STORAGE = new FileUserStorage(Vertx.vertx(), Helpers.PASSWORDS_FILE);

  private Users() {

  }
}
