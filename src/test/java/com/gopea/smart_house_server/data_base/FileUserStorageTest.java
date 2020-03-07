package com.gopea.smart_house_server.data_base;

import com.gopea.smart_house_server.common.Helpers;
import com.gopea.smart_house_server.configs.StatusCode;
import com.gopea.smart_house_server.routers.users.User;
import com.gopea.smart_house_server.routers.users.UserType;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

import static com.gopea.smart_house_server.TestHelpers.deleteDeviceFiles;
import static com.gopea.smart_house_server.common.Helpers.EXTERNAL_STATUS_KEY;
import static com.gopea.smart_house_server.common.Helpers.isInternalStatusOk;
import static com.gopea.smart_house_server.data_base.FileUserStorage.USERS_KEY;

@RunWith(VertxUnitRunner.class)
public class FileUserStorageTest {
  private static final String path = "test.json";

  private Vertx vertx;
  private UserStorage target;

  @Before
  public void before(TestContext context) {
    vertx = Vertx.vertx();
    JsonObject objet = new JsonObject().put(USERS_KEY, new JsonObject());

    final Async async = context.async();
    vertx.fileSystem()
        .rxWriteFile(path, Buffer.newInstance(objet.toBuffer()))
        .andThen(Completable.fromAction(async::complete))
        .subscribe();
    target = new FileUserStorage(vertx, path);
  }

  @After
  public void after(TestContext context) {
    final Async async = context.async();

    vertx.fileSystem().rxDelete(path)
        .andThen(deleteDeviceFiles(vertx))
        .andThen(vertx.rxClose())
        .andThen(Completable.fromAction(async::complete))
        .subscribe();
  }

  @Test(timeout = 60000)
  public void testAddUser(TestContext context) {
    final Async async = context.async();
    User user = new User("test_user", UserType.USER, "password");
    target.addUser(user)
        .flatMapCompletable(response -> Completable.fromAction(() -> {
          context.assertTrue(Helpers.isInternalStatusOk(response));
          context.assertEquals(StatusCode.CREATED.getStatusCode(), response.getInteger(EXTERNAL_STATUS_KEY));
          context.assertEquals("test_user", response.getString(Storages.ID));
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60000)
  public void testAddUserFail(TestContext context) {
    final Async async = context.async();
    User user = new User("test_user", UserType.USER, "password");
    target.addUser(user)
        .flatMapCompletable(response -> Completable.fromAction(() -> {
          context.assertTrue(Helpers.isInternalStatusOk(response));
          context.assertEquals(StatusCode.CREATED.getStatusCode(), response.getInteger(EXTERNAL_STATUS_KEY));
          context.assertEquals("test_user", response.getString(Storages.ID));
        }))
        .andThen(target.addUser(user))
        .flatMapCompletable(response -> Completable.fromAction(() -> {
          context.assertFalse(isInternalStatusOk(response));
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60000)
  public void testDeleteUser(TestContext context) {
    final Async async = context.async();
    User user = new User("test_user", UserType.USER, "password");
    target.addUser(user)
        .flatMapCompletable(response -> Completable.fromAction(() -> {
          context.assertTrue(Helpers.isInternalStatusOk(response));
          context.assertEquals(StatusCode.CREATED.getStatusCode(), response.getInteger(EXTERNAL_STATUS_KEY));
          context.assertEquals("test_user", response.getString(Storages.ID));

        }))
        .andThen(target.deleteUser(user.getUsername()))
        .flatMapCompletable(response -> Completable.fromAction(() -> {
          context.assertTrue(Helpers.isInternalStatusOk(response));
          context.assertEquals(StatusCode.NO_CONTENT.getStatusCode(), response.getInteger(EXTERNAL_STATUS_KEY));
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60000)
  public void testDeleteFail(TestContext context) {
    final Async async = context.async();
    User user = new User("test_user", UserType.USER, "password");
    target.addUser(user)
        .flatMapCompletable(response -> Completable.fromAction(() -> {
          context.assertTrue(Helpers.isInternalStatusOk(response));
          context.assertEquals(StatusCode.CREATED.getStatusCode(), response.getInteger(EXTERNAL_STATUS_KEY));
          context.assertEquals("test_user", response.getString(Storages.ID));

        }))
        .andThen(target.deleteUser(user.getUsername()))
        .flatMapCompletable(response -> Completable.fromAction(() -> {
          context.assertTrue(Helpers.isInternalStatusOk(response));
          context.assertEquals(StatusCode.NO_CONTENT.getStatusCode(), response.getInteger(EXTERNAL_STATUS_KEY));
        }))
        .andThen(target.deleteUser(user))
        .flatMapCompletable(response -> Completable.fromAction(() -> {
          context.assertFalse(Helpers.isInternalStatusOk(response));
          context.assertEquals(StatusCode.NOT_FOUND.getStatusCode(), response.getInteger(EXTERNAL_STATUS_KEY));
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60000)
  public void testGetUsers(TestContext context) {
    final Async async = context.async();
    User user = new User("test_user", UserType.USER, "password");
    target.addUser(user)
        .flatMap(ign -> target.getUsers())
        .flatMapCompletable(list -> Completable.fromAction(() -> {
          context.assertEquals(1, list.size());
          User listUser = list.get(0);
          context.assertEquals(user.getUsername(), listUser.getUsername());
          context.assertTrue(Arrays.equals(user.getPassword(), listUser.getPassword()));
          context.assertEquals(user.getUserType(), listUser.getUserType());
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60000)
  public void testGetUsersFilter(TestContext context) {
    final Async async = context.async();
    User user = new User("test_user", UserType.USER, "password");
    User user1 = new User("test_user1", UserType.ADMIN, "password");
    target.addUser(user)
        .flatMap(ign -> target.addUser(user1))
        .flatMap(ign -> target.getUsers(UserType.USER))
        .flatMapCompletable(list -> Completable.fromAction(() -> {
          context.assertEquals(1, list.size());
          User listUser = list.get(0);
          context.assertEquals(user.getUsername(), listUser.getUsername());
          context.assertTrue(Arrays.equals(user.getPassword(), listUser.getPassword()));
          context.assertEquals(user.getUserType(), listUser.getUserType());
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60000)
  public void testGetUser(TestContext context) {
    final Async async = context.async();
    User user = new User("test_user", UserType.USER, "password");
    target.addUser(user)
        .flatMapMaybe(ign -> target.getUser(user.getUsername()))
        .flatMapCompletable(user1 -> Completable.fromAction(() -> {
          context.assertEquals(user.getUsername(), user1.getUsername());
          context.assertTrue(Arrays.equals(user.getPassword(), user1.getPassword()));
          context.assertEquals(user.getUserType(), user1.getUserType());
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60000)
  public void testGetUserEmpty(TestContext context) {
    final Async async = context.async();
    User user = new User("test_user", UserType.USER, "password");
    target.addUser(user)
        .flatMapMaybe(ign -> target.getUser(""))
        .isEmpty()
        .flatMapCompletable(isEmpty -> Completable.fromAction(() -> {
          context.assertTrue(isEmpty);
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60000)
  public void testGetOperation(TestContext context) {
    final Async async = context.async();
    User user = new User("test_user", UserType.USER, "password");
    target.addUser(user)
        .flatMapMaybe(ign -> target.getUser(user.getUsername()))
        .flatMapCompletable(user1 -> Completable.fromAction(() -> {
          context.assertEquals(user.getUsername(), user1.getUsername());
          context.assertTrue(Arrays.equals(user.getPassword(), user1.getPassword()));
          context.assertEquals(user.getUserType(), user1.getUserType());
        }))
        .andThen(target.getUser(user.getUsername()))
        .flatMapCompletable(user1 -> Completable.fromAction(() -> {
          context.assertEquals(user.getUsername(), user1.getUsername());
          context.assertTrue(Arrays.equals(user.getPassword(), user1.getPassword()));
          context.assertEquals(user.getUserType(), user1.getUserType());
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60000)
  public void testUpdatePassword(TestContext context) {
    final Async async = context.async();
    User user = new User("test_user", UserType.USER, "password");
    target.addUser(user)
        .flatMapCompletable(ign -> Completable.fromAction(() -> user.setPassword("test")))
        .andThen(target.updatePassword(user))
        .flatMapCompletable(response -> Completable.fromAction(() -> {
          context.assertTrue(isInternalStatusOk(response));
          context.assertEquals(StatusCode.SUCCESS.getStatusCode(), response.getInteger(EXTERNAL_STATUS_KEY));
        }))
        .andThen(target.getUser(user.getUsername()))
        .flatMapCompletable(user1 -> Completable.fromAction(() -> {
          context.assertEquals(user.getUsername(), user1.getUsername());
          context.assertTrue(Arrays.equals(user.getPassword(), user1.getPassword()));
          context.assertEquals(user.getUserType(), user1.getUserType());
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60000)
  public void testUpdatePasswordFail(TestContext context) {
    final Async async = context.async();
    User user = new User("test_user", UserType.USER, "password");
    target.updatePassword(user, new byte[]{0, 1, 3})
        .flatMapCompletable(response -> Completable.fromAction(() -> {
          context.assertFalse(isInternalStatusOk(response));
          context.assertEquals(StatusCode.NOT_FOUND.getStatusCode(), response.getInteger(EXTERNAL_STATUS_KEY));
          async.complete();
        }))
        .subscribe();
  }

}
