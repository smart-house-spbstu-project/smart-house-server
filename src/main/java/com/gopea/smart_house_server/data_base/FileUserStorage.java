package com.gopea.smart_house_server.data_base;

import com.gopea.smart_house_server.common.InternalStatus;
import com.gopea.smart_house_server.configs.StatusCode;
import com.gopea.smart_house_server.routers.users.User;
import com.gopea.smart_house_server.routers.users.UserType;

import io.reactivex.Flowable;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

import static com.gopea.smart_house_server.common.Helpers.EXTERNAL_STATUS_KEY;
import static com.gopea.smart_house_server.common.Helpers.INTERNAL_STATUS_KEY;
import static com.gopea.smart_house_server.common.Helpers.MESSAGE_KEY;


public class FileUserStorage implements UserStorage {
  private Pair<JsonObject, Long> file;
  public static final String USERS_KEY = "users";
  private static final String PASSWORD_KEY = "password";
  private static final String USER_TYPE_KEY = "user_type";

  private final String filePath;
  private final Vertx vertx;


  public FileUserStorage(Vertx vertx, String filePath) {
    this.vertx = vertx;
    this.filePath = filePath;
  }

  @Override
  public Single<JsonObject> updatePassword(User user, byte[] newPassword) {
    return updatePassword(user.setPassword(newPassword));
  }

  @Override
  public Single<JsonObject> updatePassword(User user) {
    return readFile()
        .flatMap(json -> {
          JsonObject userData = json.getJsonObject(USERS_KEY).getJsonObject(user.getUsername());
          if (userData == null) {
            return Single.just(
                new JsonObject()
                    .put(INTERNAL_STATUS_KEY, InternalStatus.FAILED)
                    .put(MESSAGE_KEY, "User doesn't exists")
                    .put(EXTERNAL_STATUS_KEY, StatusCode.NOT_FOUND.getStatusCode()));
          }
          userData.put(PASSWORD_KEY, user.getPassword());
          return vertx.fileSystem().rxWriteFile(filePath,
              Buffer.newInstance(json.toBuffer()))
              .andThen(Single.just(new JsonObject()
                  .put(INTERNAL_STATUS_KEY, InternalStatus.OK)
                  .put(EXTERNAL_STATUS_KEY, StatusCode.SUCCESS.getStatusCode()))
              );
        });
  }


  @Override
  public Single<JsonObject> addUser(User user) {
    return readFile()
        .flatMap(json -> {
          JsonObject users = json.getJsonObject(USERS_KEY);
          if (users.getJsonObject(user.getUsername()) != null) {
            return Single.just(
                new JsonObject()
                    .put(INTERNAL_STATUS_KEY, InternalStatus.FAILED)
                    .put(EXTERNAL_STATUS_KEY, StatusCode.BAD_REQUEST.getStatusCode())
                    .put(MESSAGE_KEY, String.format("User %s already exists", user.getUsername()))
            );
          }
          JsonObject userData = new JsonObject()
              .put(PASSWORD_KEY, user.getPassword())
              .put(USER_TYPE_KEY, user.getUserType().toString().toLowerCase());
          users.put(user.getUsername(), userData);
          return vertx.fileSystem().rxWriteFile(filePath, Buffer.newInstance(json.toBuffer()))
              .andThen(Single.just(new JsonObject()
                  .put(INTERNAL_STATUS_KEY, InternalStatus.OK)
                  .put(EXTERNAL_STATUS_KEY, StatusCode.CREATED.getStatusCode())
                  .put(Storages.ID, user.getUsername())));
        });
  }

  @Override
  public Single<JsonObject> deleteUser(String username) {
    return readFile()
        .flatMap(json -> {
          JsonObject users = json.getJsonObject(USERS_KEY);

          if (users.getJsonObject(username) == null) {
            return Single.just(
                new JsonObject()
                    .put(INTERNAL_STATUS_KEY, InternalStatus.FAILED)
                    .put(EXTERNAL_STATUS_KEY, StatusCode.NOT_FOUND.getStatusCode())
                    .put(MESSAGE_KEY, String.format("Username %s doesn't exists", username))
            );
          }
          users.remove(username);

          return vertx.fileSystem().rxWriteFile(filePath, Buffer.newInstance(users.toBuffer()))
              .andThen(Single.just(
                  new JsonObject()
                      .put(INTERNAL_STATUS_KEY, InternalStatus.OK)
                      .put(EXTERNAL_STATUS_KEY, StatusCode.NO_CONTENT.getStatusCode())
              ));
        });
  }


  @Override
  public Single<JsonObject> deleteUser(User user) {
    return deleteUser(user.getUsername());
  }

  @Override
  public Single<List<User>> getUsers(UserType userType) {
    return getUsers()
        .flatMap(list -> Flowable.fromIterable(list)
            .filter(user -> user.getUserType().equals(userType))
            .collectInto(new ArrayList<User>(), (arrayList, user) -> arrayList.add(user)));
  }

  @Override
  public Single<List<User>> getUsers() {
    return readFile()
        .map(json -> json.getJsonObject(USERS_KEY))
        .map(users -> {
          List<User> listUsers = new ArrayList<>();
          for (String username : users.fieldNames()) {
            JsonObject user = users.getJsonObject(username);
            listUsers.add(new User(username,
                UserType.valueOf(user.getString(USER_TYPE_KEY).toUpperCase()),
                user.getBinary(PASSWORD_KEY)));
          }
          return listUsers;
        });
  }

  @Override
  public Single<User> getUser(String username) {
    return readFile()
        .map(json -> {
          JsonObject user = json.getJsonObject(USERS_KEY).getJsonObject(username);
          if (user == null) {
            return null;
          }
          return new User(username, UserType.valueOf(user.getString(USER_TYPE_KEY).toUpperCase()), user.getBinary(PASSWORD_KEY));
        });
  }

  private Single<JsonObject> readFile() {
    return vertx.fileSystem().rxProps(filePath)
        .flatMap(fileProps -> {
          if (file == null || file.getRight() < fileProps.lastModifiedTime()) {
            System.out.println("Read users file");
            return vertx.fileSystem().rxReadFile(filePath)
                .map(Buffer::toJsonObject)
                .map(json -> {
                  file = new ImmutablePair<>(json, System.currentTimeMillis());
                  return json;
                });
          }
          return Single.just(file.getLeft());
        });
  }

}
