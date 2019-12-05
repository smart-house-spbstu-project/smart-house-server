package com.gopea.smart_house_server.data_base;

import com.gopea.smart_house_server.routers.users.User;
import com.gopea.smart_house_server.routers.users.UserType;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;

import java.util.List;

public interface UserStorage {

  Single<JsonObject> updatePassword(User user, byte[] newPassword);

  Single<JsonObject> updatePassword(User user);

  Single<JsonObject> addUser(User user);

  Single<JsonObject> deleteUser(String username);

  Single<JsonObject> deleteUser(User user);

  Single<List<User>> getUsers(UserType userType);

  Single<List<User>> getUsers();

  Single<User> getUser(String username);

}
