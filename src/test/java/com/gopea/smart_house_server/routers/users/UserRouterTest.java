package com.gopea.smart_house_server.routers.users;

import com.gopea.smart_house_server.configs.StatusCode;
import com.gopea.smart_house_server.data_base.Storages;
import io.reactivex.Completable;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.reactivex.core.http.HttpServerResponse;
import io.vertx.reactivex.ext.web.Route;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.List;

import static com.gopea.smart_house_server.common.Helpers.PASSWORDS_FILE;
import static com.gopea.smart_house_server.common.Helpers.USER_TYPE_HEADER;
import static com.gopea.smart_house_server.configs.StandardCredentials.ADMIN;
import static com.gopea.smart_house_server.data_base.FileUserStorage.USERS_KEY;
import static com.gopea.smart_house_server.routers.users.UserRouter.*;
import static junit.framework.TestCase.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(VertxUnitRunner.class)
public class UserRouterTest {

  private Vertx vertx;

  @Before
  public void before(TestContext context) {
    final Async async = context.async();
    vertx = Vertx.vertx();
    vertx.fileSystem()
        .rxWriteFile(PASSWORDS_FILE, Buffer.newInstance(new JsonObject().put(USERS_KEY, new JsonObject()).toBuffer()))
        .andThen(Completable.fromAction(async::complete))
        .subscribe();
  }

  @After
  public void after(TestContext context) {
    final Async async = context.async();

    vertx.fileSystem()
        .rxDelete(PASSWORDS_FILE)
        .andThen(vertx.rxClose())
        .andThen(Completable.fromAction(async::complete))
        .subscribe();
  }

  @Test(timeout = 60_000L)
  public void testLoadRouter(TestContext context) {
    Router target = new UserRouter().loadRouter(vertx);

    List<Route> list = target.getRoutes();
    assertEquals(4, list.size());
  }

  @Test
  public void testGetRequestIncorrectAccessRights(TestContext context) {
    UserRouter target = new UserRouter();
    RoutingContext routingContext = createContext();

    final Async async = context.async();

    when(routingContext.request().getHeader(any())).then((invocationOnMock -> {
      if (invocationOnMock.getArgument(0).equals(USER_TYPE_HEADER)) {
        return UserType.USER.toString();
      }
      return "";
    }));

    target.handleGetRequest(routingContext).
        andThen(Completable.fromAction(async::complete))
        .subscribe();

    verify(routingContext.request()).getHeader(anyString());
    verify(routingContext).fail(anyInt());
  }

  @Test
  public void testPostRequestIncorrectAccessRights(TestContext context) {
    UserRouter target = new UserRouter();
    RoutingContext routingContext = createContext();
    final Async async = context.async();

    when(routingContext.request().getHeader(any())).then((invocationOnMock -> {
      if (invocationOnMock.getArgument(0).equals(USER_TYPE_HEADER)) {
        return UserType.USER.toString();
      }
      return "";
    }));

    target.handlePostRequest(routingContext)
        .andThen(Completable.fromAction(async::complete))
        .subscribe();

    verify(routingContext.request()).getHeader(anyString());
    verify(routingContext).fail(anyInt());
  }

  @Test
  public void testDeleteRequestIncorrectAccessRights(TestContext context) {
    UserRouter target = new UserRouter();
    RoutingContext routingContext = createContext();
    final Async async = context.async();

    when(routingContext.request().getHeader(any())).then((invocationOnMock -> {
      if (invocationOnMock.getArgument(0).equals(USER_TYPE_HEADER)) {
        return UserType.USER.toString();
      }
      return "";
    }));

    target.handleDeleteRequest(routingContext)
        .andThen(Completable.fromAction(async::complete))
        .subscribe();

    verify(routingContext.request()).getHeader(anyString());
    verify(routingContext).fail(anyInt());
  }

  @Test(timeout = 60000L)
  public void testGetRequestWithoutUserTypePararmEmptyStorage(TestContext context) {
    final Async async = context.async();

    UserRouter target = new UserRouter();
    RoutingContext routingContext = createContext();

    when(routingContext.request().getHeader(any())).then((invocationOnMock -> {
      if (invocationOnMock.getArgument(0).equals(USER_TYPE_HEADER)) {
        return UserType.ADMIN.toString();
      }
      return "";
    }));

    target.handleGetRequest(routingContext)
        .andThen(Completable.fromAction(() -> {
          ArgumentCaptor<String> valueCapture = ArgumentCaptor.forClass(String.class);
          verify(routingContext.request()).getHeader(anyString());
          verify(routingContext.response()).setStatusCode(StatusCode.SUCCESS.getStatusCode());
          verify(routingContext.response()).end(valueCapture.capture());

          context.assertEquals(new JsonArray().toString(), valueCapture.getValue());
          async.complete();
        }))
        .subscribe();

  }

  @Test(timeout = 60000L)
  public void testGetRequestWithoutUserTypePararmNotEmptyStorage(TestContext context) {
    final Async async = context.async();

    UserRouter target = new UserRouter();
    RoutingContext routingContext = createContext();

    when(routingContext.request().getHeader(any())).then((invocationOnMock -> {
      if (invocationOnMock.getArgument(0).equals(USER_TYPE_HEADER)) {
        return UserType.ADMIN.toString();
      }
      return "";
    }));
    Storages.USER_STORAGE.addUser(new User("test_user", UserType.ADMIN, "password"))
        .flatMapCompletable(ign -> target.handleGetRequest(routingContext))
        .andThen(Completable.fromAction(() -> {
          ArgumentCaptor<String> valueCapture = ArgumentCaptor.forClass(String.class);
          verify(routingContext.request()).getHeader(anyString());
          verify(routingContext.response()).setStatusCode(StatusCode.SUCCESS.getStatusCode());
          verify(routingContext.response()).end(valueCapture.capture());

          context.assertEquals(1, new JsonArray(valueCapture.getValue()).size());
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60000L)
  public void testGetRequestWithUnExistsUserTypePararm(TestContext context) {
    final Async async = context.async();

    UserRouter target = new UserRouter();
    RoutingContext routingContext = createContext();
    Storages.USER_STORAGE.addUser(new User("test_user", UserType.USER, "password"));
    Storages.USER_STORAGE.addUser(new User("test_user_admin", UserType.ADMIN, "password"));

    when(routingContext.request().getHeader(any())).then((invocationOnMock -> {
      if (invocationOnMock.getArgument(0).equals(USER_TYPE_HEADER)) {
        return UserType.ADMIN.toString();
      }
      return "";
    }));

    when(routingContext.request().getParam(anyString())).thenReturn("test");

    target.handleGetRequest(routingContext)
        .andThen(Completable.fromAction(() -> {
          ArgumentCaptor<String> valueCapture = ArgumentCaptor.forClass(String.class);
          verify(routingContext.request()).getHeader(anyString());
          verify(routingContext.response()).setStatusCode(StatusCode.SUCCESS.getStatusCode());
          verify(routingContext.response()).end(valueCapture.capture());

          context.assertEquals(new JsonArray().toString(), valueCapture.getValue());
          async.complete();
        }))
        .subscribe();

  }

  @Test(timeout = 60000L)
  public void testGetRequestWithExistsUserTypePararm(TestContext context) {
    final Async async = context.async();

    UserRouter target = new UserRouter();
    RoutingContext routingContext = createContext();


    when(routingContext.request().getHeader(any())).then((invocationOnMock -> {
      if (invocationOnMock.getArgument(0).equals(USER_TYPE_HEADER)) {
        return UserType.ADMIN.toString();
      }
      return "";
    }));

    when(routingContext.request().getParam(anyString())).thenReturn(UserType.ADMIN.toString());
    Storages.USER_STORAGE.addUser(new User("test_user", UserType.USER, "password"))
        .flatMap(ign -> Storages.USER_STORAGE
            .addUser(new User("test_user_admin", UserType.ADMIN, "password")))
        .flatMapCompletable(ign -> target.handleGetRequest(routingContext))
        .andThen(Completable.fromAction(() -> {
          ArgumentCaptor<String> valueCapture = ArgumentCaptor.forClass(String.class);
          verify(routingContext.request()).getHeader(anyString());
          verify(routingContext.response()).setStatusCode(StatusCode.SUCCESS.getStatusCode());
          verify(routingContext.response()).end(valueCapture.capture());

          context.assertEquals(1, new JsonArray(valueCapture.getValue()).size());
          async.complete();
        }))
        .subscribe();

  }

  @Test(timeout = 60_000L)
  public void testPostRequestWithoutBody(TestContext context) {
    final Async async = context.async();

    UserRouter target = new UserRouter();
    RoutingContext routingContext = createContext();


    when(routingContext.request().getHeader(any())).then((invocationOnMock -> {
      if (invocationOnMock.getArgument(0).equals(USER_TYPE_HEADER)) {
        return UserType.ADMIN.toString();
      }
      return "";
    }));

    when(routingContext.getBodyAsJson()).thenThrow(new RuntimeException(""));

    target.handlePostRequest(routingContext)
        .andThen(Completable.fromAction(() -> {
          verify(routingContext.request()).getHeader(anyString());
          verify(routingContext).fail(anyInt(), any(Throwable.class));

          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60_000L)
  public void testPostRequestWithEmptyBody(TestContext context) {
    final Async async = context.async();

    UserRouter target = new UserRouter();
    RoutingContext routingContext = createContext();


    when(routingContext.request().getHeader(any())).then((invocationOnMock -> {
      if (invocationOnMock.getArgument(0).equals(USER_TYPE_HEADER)) {
        return UserType.ADMIN.toString();
      }
      return "";
    }));

    when(routingContext.getBodyAsJson()).thenReturn(new JsonObject());

    target.handlePostRequest(routingContext)
        .andThen(Completable.fromAction(() -> {
          verify(routingContext.request()).getHeader(anyString());
          verify(routingContext.response()).setStatusCode(StatusCode.BAD_REQUEST.getStatusCode());
          verify(routingContext.response()).end(any(Buffer.class));
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60_000L)
  public void testPostRequestWithoutNameInBody(TestContext context) {
    final Async async = context.async();

    UserRouter target = new UserRouter();
    RoutingContext routingContext = createContext();


    when(routingContext.request().getHeader(any())).then((invocationOnMock -> {
      if (invocationOnMock.getArgument(0).equals(USER_TYPE_HEADER)) {
        return UserType.ADMIN.toString();
      }
      return "";
    }));

    when(routingContext.getBodyAsJson()).thenReturn(new JsonObject()
        .put(PASSWORD_KEY, "test_pass")
        .put(USER_TYPE_KEY, UserType.USER.name()));

    target.handlePostRequest(routingContext)
        .andThen(Completable.fromAction(() -> {
          verify(routingContext.request()).getHeader(anyString());
          verify(routingContext.response()).setStatusCode(StatusCode.BAD_REQUEST.getStatusCode());
          verify(routingContext.response()).end(any(Buffer.class));
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60_000L)
  public void testPostRequestWithoutPasswordInBody(TestContext context) {
    final Async async = context.async();

    UserRouter target = new UserRouter();
    RoutingContext routingContext = createContext();


    when(routingContext.request().getHeader(any())).then((invocationOnMock -> {
      if (invocationOnMock.getArgument(0).equals(USER_TYPE_HEADER)) {
        return UserType.ADMIN.toString();
      }
      return "";
    }));

    when(routingContext.getBodyAsJson()).thenReturn(new JsonObject()
        .put(USERNAME_KEY, "test")
        .put(USER_TYPE_KEY, UserType.USER.name()));

    target.handlePostRequest(routingContext)
        .andThen(Completable.fromAction(() -> {
          verify(routingContext.request()).getHeader(anyString());
          verify(routingContext.response()).setStatusCode(StatusCode.BAD_REQUEST.getStatusCode());
          verify(routingContext.response()).end(any(Buffer.class));
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60_000L)
  public void testPostRequestWithoutUserTypeInBody(TestContext context) {
    final Async async = context.async();

    UserRouter target = new UserRouter();
    RoutingContext routingContext = createContext();


    when(routingContext.request().getHeader(any())).then((invocationOnMock -> {
      if (invocationOnMock.getArgument(0).equals(USER_TYPE_HEADER)) {
        return UserType.ADMIN.toString();
      }
      return "";
    }));

    when(routingContext.getBodyAsJson()).thenReturn(new JsonObject()
        .put(USERNAME_KEY, "test")
        .put(PASSWORD_KEY, "test_pass"));

    target.handlePostRequest(routingContext)
        .andThen(Completable.fromAction(() -> {
          verify(routingContext.request()).getHeader(anyString());
          verify(routingContext.response()).setStatusCode(StatusCode.BAD_REQUEST.getStatusCode());
          verify(routingContext.response()).end(any(Buffer.class));
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60_000L)
  public void testPostRequestWithInvalidUserType(TestContext context) {
    final Async async = context.async();

    UserRouter target = new UserRouter();
    RoutingContext routingContext = createContext();


    when(routingContext.request().getHeader(any())).then((invocationOnMock -> {
      if (invocationOnMock.getArgument(0).equals(USER_TYPE_HEADER)) {
        return UserType.ADMIN.toString();
      }
      return "";
    }));

    when(routingContext.getBodyAsJson()).thenReturn(new JsonObject()
        .put(USERNAME_KEY, "test")
        .put(PASSWORD_KEY, "test_pass")
        .put(USER_TYPE_KEY, "hey"));

    target.handlePostRequest(routingContext)
        .andThen(Completable.fromAction(() -> {
          verify(routingContext.request()).getHeader(anyString());
          verify(routingContext.response()).setStatusCode(StatusCode.BAD_REQUEST.getStatusCode());
          verify(routingContext.response()).end(any(Buffer.class));
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60_000L)
  public void testPostRequestSuccess(TestContext context) {
    final Async async = context.async();

    UserRouter target = new UserRouter();
    RoutingContext routingContext = createContext();


    when(routingContext.request().getHeader(any())).then((invocationOnMock -> {
      if (invocationOnMock.getArgument(0).equals(USER_TYPE_HEADER)) {
        return UserType.ADMIN.toString();
      }
      return "";
    }));

    when(routingContext.getBodyAsJson()).thenReturn(new JsonObject()
        .put(USERNAME_KEY, "test")
        .put(PASSWORD_KEY, "test_pass")
        .put(USER_TYPE_KEY, UserType.USER.name()));

    target.handlePostRequest(routingContext)
        .andThen(Completable.fromAction(() -> {
          verify(routingContext.request()).getHeader(anyString());
          verify(routingContext.response()).setStatusCode(StatusCode.CREATED.getStatusCode());
          verify(routingContext.response()).end(any(Buffer.class));
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60_000L)
  public void testPostRequestFail(TestContext context) {
    final Async async = context.async();

    UserRouter target = new UserRouter();
    RoutingContext routingContext = createContext();


    when(routingContext.request().getHeader(any())).then((invocationOnMock -> {
      if (invocationOnMock.getArgument(0).equals(USER_TYPE_HEADER)) {
        return UserType.ADMIN.toString();
      }
      return "";
    }));

    when(routingContext.getBodyAsJson()).thenReturn(new JsonObject()
        .put(USERNAME_KEY, "test")
        .put(PASSWORD_KEY, "test_pass")
        .put(USER_TYPE_KEY, UserType.USER.name()));
    Storages.USER_STORAGE.addUser(new User("test", UserType.ADMIN, "iuisu"))
        .flatMapCompletable(ign -> target.handlePostRequest(routingContext))
        .andThen(Completable.fromAction(() -> {
          verify(routingContext.request()).getHeader(anyString());
          verify(routingContext.response()).setStatusCode(StatusCode.UNPROCESSABLE_ENTITY.getStatusCode());
          verify(routingContext.response()).end(any(Buffer.class));
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60_000L)
  public void testPatchRequestSuccess(TestContext context) {
    final Async async = context.async();

    UserRouter target = new UserRouter();
    RoutingContext routingContext = createContext();

    final User user = new User("test_user", UserType.USER, "password");

    when(routingContext.request().getHeader(anyString())).thenReturn(user.getUsername());
    when(routingContext.request().getParam(anyString())).thenReturn(user.getUsername());

    when(routingContext.getBodyAsJson()).thenReturn(new JsonObject()
        .put(PASSWORD_KEY, "test_pass"));

    Storages.USER_STORAGE.addUser(user)
        .flatMapCompletable(ign -> target.handlePatchRequest(routingContext))
        .andThen(Completable.fromAction(() -> {
          verify(routingContext.response()).setStatusCode(StatusCode.SUCCESS.getStatusCode());
          verify(routingContext.response()).end();
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60_000L)
  public void testPatchRequestWithoutBody(TestContext context) {
    final Async async = context.async();

    UserRouter target = new UserRouter();
    RoutingContext routingContext = createContext();

    final User user = new User("test_user", UserType.USER, "password");

    when(routingContext.request().getHeader(anyString())).thenReturn(user.getUsername());
    when(routingContext.request().getParam(anyString())).thenReturn(user.getUsername());

    when(routingContext.getBodyAsJson()).thenThrow(new RuntimeException());

    target.handlePatchRequest(routingContext)
        .andThen(Completable.fromAction(() -> {
          verify(routingContext).fail(anyInt(), any(Throwable.class));
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60_000L)
  public void testPatchRequestInvalidUser(TestContext context) {
    final Async async = context.async();

    UserRouter target = new UserRouter();
    RoutingContext routingContext = createContext();

    final User user = new User("test_user", UserType.USER, "password");

    when(routingContext.request().getHeader(anyString())).thenReturn(user.getUsername());
    when(routingContext.request().getParam(anyString())).thenReturn(user.getUsername() + "kdjk");

    when(routingContext.getBodyAsJson()).thenReturn(new JsonObject()
        .put(PASSWORD_KEY, "test_pass"));

    target.handlePatchRequest(routingContext)
        .andThen(Completable.fromAction(() -> {
          verify(routingContext.response()).setStatusCode(StatusCode.FORBIDDEN.getStatusCode());
          verify(routingContext.response()).end(any(Buffer.class));
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60_000L)
  public void testPatchRequestWithEmptyBody(TestContext context) {
    final Async async = context.async();

    UserRouter target = new UserRouter();
    RoutingContext routingContext = createContext();

    final User user = new User("test_user", UserType.USER, "password");

    when(routingContext.request().getHeader(anyString())).thenReturn(user.getUsername());
    when(routingContext.request().getParam(anyString())).thenReturn(user.getUsername());

    when(routingContext.getBodyAsJson()).thenReturn(new JsonObject());

    target.handlePatchRequest(routingContext)
        .andThen(Completable.fromAction(() -> {
          verify(routingContext.response()).setStatusCode(StatusCode.SUCCESS.getStatusCode());
          verify(routingContext.response()).end();
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60_000L)
  @Ignore
  public void testPatchRequestWithUnExistsUser(TestContext context) {
    final Async async = context.async();

    UserRouter target = new UserRouter();
    RoutingContext routingContext = createContext();

    final User user = new User("test_user", UserType.USER, "password");

    when(routingContext.request().getHeader(anyString())).thenReturn(user.getUsername());
    when(routingContext.request().getParam(anyString())).thenReturn(user.getUsername());

    when(routingContext.getBodyAsJson()).thenReturn(new JsonObject()
        .put(PASSWORD_KEY, "test_pass"));

    target.handlePatchRequest(routingContext)
        .andThen(Completable.fromAction(() -> {
          verify(routingContext.response()).setStatusCode(StatusCode.NOT_FOUND.getStatusCode());
          verify(routingContext.response()).end(any(Buffer.class));
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60_000L)
  public void testDeleteAdminUserForbidden(TestContext context) {
    final Async async = context.async();

    UserRouter target = new UserRouter();
    RoutingContext routingContext = createContext();

    when(routingContext.request().getHeader(any())).then((invocationOnMock -> {
      if (invocationOnMock.getArgument(0).equals(USER_TYPE_HEADER)) {
        return UserType.ADMIN.toString();
      }
      return ADMIN.getUsername();
    }));
    when(routingContext.request().getParam(anyString())).thenReturn(ADMIN.getUsername());

    target.handleDeleteRequest(routingContext)
        .andThen(Completable.fromAction(() -> {
          verify(routingContext.response()).setStatusCode(StatusCode.FORBIDDEN.getStatusCode());
          verify(routingContext.response()).end(any(Buffer.class));
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60_000L)
  public void testDeleteUserSuccess(TestContext context) {
    final Async async = context.async();

    UserRouter target = new UserRouter();
    RoutingContext routingContext = createContext();
    final User user = new User("test_user", UserType.USER, "password");


    when(routingContext.request().getHeader(any())).then((invocationOnMock -> {
      if (invocationOnMock.getArgument(0).equals(USER_TYPE_HEADER)) {
        return UserType.ADMIN.toString();
      }
      return user.getUsername();
    }));
    when(routingContext.request().getParam(anyString())).thenReturn(user.getUsername());

    Storages.USER_STORAGE.addUser(user)
        .flatMapCompletable(ign -> target.handleDeleteRequest(routingContext))
        .andThen(Completable.fromAction(() -> {
          verify(routingContext.response()).setStatusCode(StatusCode.NO_CONTENT.getStatusCode());
          verify(routingContext.response()).end();
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60_000L)
  public void testDeleteUserFail(TestContext context) {
    final Async async = context.async();

    UserRouter target = new UserRouter();
    RoutingContext routingContext = createContext();
    final User user = new User("test_user", UserType.USER, "password");


    when(routingContext.request().getHeader(any())).then((invocationOnMock -> {
      if (invocationOnMock.getArgument(0).equals(USER_TYPE_HEADER)) {
        return UserType.ADMIN.toString();
      }
      return user.getUsername();
    }));
    when(routingContext.request().getParam(anyString())).thenReturn(user.getUsername());

    target.handleDeleteRequest(routingContext)
        .andThen(Completable.fromAction(() -> {
          verify(routingContext.response()).setStatusCode(StatusCode.NOT_FOUND.getStatusCode());
          verify(routingContext.response()).end(any(Buffer.class));
          async.complete();
        }))
        .subscribe();
  }

  public static RoutingContext createContext() {
    RoutingContext context = mock(RoutingContext.class);
    HttpServerResponse response = mock(HttpServerResponse.class);
    HttpServerRequest request = mock(HttpServerRequest.class);

    when(context.response()).thenReturn(response);
    when(context.request()).thenReturn(request);

    return context;
  }
}
