package com.gopea.smart_house_server.data_base;

import com.gopea.smart_house_server.common.Helpers;
import io.vertx.reactivex.core.Vertx;

public final class Storages {
  public static final String ID = "id";
  public static final UserStorage USER_STORAGE = new FileUserStorage(Vertx.vertx(), Helpers.PASSWORDS_FILE);
  public static final DeviceStorage DEVICE_STORAGE = new RuntimeDeviceStorage();

  private Storages() {
  }
}
