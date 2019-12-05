package com.gopea.smart_house_server.configs;

import com.gopea.smart_house_server.data_base.InternalStatus;
import com.gopea.smart_house_server.helpers.Helpers;
import io.reactivex.Completable;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;

import static com.gopea.smart_house_server.configs.StandardCredentials.ADMIN;
import static com.gopea.smart_house_server.data_base.FileUserStorage.USERS_KEY;
import static com.gopea.smart_house_server.helpers.Helpers.INTERNAL_STATUS_KEY;
import static com.gopea.smart_house_server.routers.users.Users.USER_STORAGE;

public class InitConfigs {

  public static final int HTTP_PORT = 8080;

  public static Completable setupInitConfig(Vertx vertx) {
    return vertx.fileSystem().rxExists(Helpers.PASSWORDS_FILE)
        .flatMapCompletable(exists -> {
          if (exists) {
            return Completable.complete();
          }
          return vertx.fileSystem().rxWriteFile(Helpers.PASSWORDS_FILE,
              Buffer.newInstance(new JsonObject().put(USERS_KEY, new JsonObject()).toBuffer()))
              .andThen(USER_STORAGE.addUser(ADMIN))
              .flatMapCompletable(json -> {
                if (!InternalStatus.valueOf(json.getString(INTERNAL_STATUS_KEY)).isOk) {
                  throw new RuntimeException("Can't add default user");
                }
                return Completable.complete();
              });
        });
  }

  private InitConfigs() {

  }
}
