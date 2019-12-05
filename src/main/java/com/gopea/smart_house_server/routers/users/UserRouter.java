package com.gopea.smart_house_server.routers.users;

import com.gopea.smart_house_server.configs.RoutConfiguration;
import com.gopea.smart_house_server.configs.StatusCode;
import com.gopea.smart_house_server.data_base.InternalStatus;
import com.gopea.smart_house_server.routers.Routable;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

import static com.gopea.smart_house_server.configs.StandardCredentials.ADMIN;
import static com.gopea.smart_house_server.helpers.Helpers.EXTERNAL_STATUS_KEY;
import static com.gopea.smart_house_server.helpers.Helpers.INTERNAL_STATUS_KEY;
import static com.gopea.smart_house_server.helpers.Helpers.MESSAGE_KEY;
import static com.gopea.smart_house_server.helpers.Helpers.USERNAME_HEADER;
import static com.gopea.smart_house_server.helpers.Helpers.USER_TYPE_HEADER;

public class UserRouter implements Routable {

  private static final String PATH = RoutConfiguration.REST_PREFIX + "/user";
  private static final String USER_TYPE_PARAM = "user_type";
  private static final String USERNAME_KEY = "username";
  private static final String USER_TYPE_KEY = "user_type";
  private static final String PASSWORD_KEY = "password";

  @Override
  public Router loadRouter(Vertx vertx) {
    Router router = Router.router(vertx);

    router.route(HttpMethod.GET, PATH).handler(routingContext -> {
      checkAccessRights(routingContext);
      String userTypeParam = routingContext.request().getParam(USER_TYPE_PARAM);
      Single<List<User>> single = null;
      if (StringUtils.isNotBlank(userTypeParam)) {
        UserType userType = UserType.getUserType(userTypeParam);
        if (userType != null) {
          single = Users.USER_STORAGE.getUsers(userType);
        } else {
          routingContext.response().setStatusCode(StatusCode.SUCCESS.getStatusCode());
          routingContext.response().end(String.valueOf(new JsonArray().toBuffer()));
        }
      } else {
        single = Users.USER_STORAGE.getUsers();
      }
      if (single != null) {
        single
            .map(list -> {
              JsonArray jsonArray = new JsonArray();
              for (User user : list) {
                JsonObject object = new JsonObject()
                    .put(USERNAME_KEY, user.getUsername())
                    .put(USER_TYPE_KEY, user.getUserType().toString().toLowerCase());
                jsonArray.add(object);
              }
              return jsonArray;
            })
            .flatMapCompletable(array -> {
              routingContext.response().setStatusCode(StatusCode.SUCCESS.getStatusCode());
              routingContext.response().end(String.valueOf(array.toBuffer()));
              return Completable.complete();
            })
            .doOnError(error -> handleError(routingContext, error))
            .subscribe();
      }
    });

    router.route(HttpMethod.POST, PATH).handler(routingContext -> {
      checkAccessRights(routingContext);
      JsonObject body = getBody(routingContext);
      String username = body.getString(USERNAME_KEY);
      String password = body.getString(PASSWORD_KEY);
      String userType = body.getString(USER_TYPE_KEY);
      if (StringUtils.isAnyEmpty(username, password, username)) {
        routingContext.response().setStatusCode(StatusCode.BAD_REQUEST.getStatusCode());
        routingContext.response()
            .end(Buffer.newInstance(new JsonObject().put(MESSAGE_KEY,
                String.format("Fields %s, %s, %s are required and should be not empty",
                    USERNAME_KEY, PASSWORD_KEY, USER_TYPE_KEY)).toBuffer()));
      }
      UserType userType1 = UserType.getUserType(userType);
      if (userType1 == null) {
        routingContext.response().setStatusCode(StatusCode.BAD_REQUEST.getStatusCode());
        routingContext.response()
            .end(Buffer.newInstance(new JsonObject().put(MESSAGE_KEY,
                String.format("user_type %s is not supported", userType)).toBuffer()));
      }
      User user = new User(username, userType1, password);
      Users.USER_STORAGE.addUser(user)
          .flatMapCompletable(response -> {
            if (!InternalStatus.valueOf(response.getString(INTERNAL_STATUS_KEY)).isOk) {
              makeErrorResponse(routingContext, response);
            } else {
              routingContext.response().setStatusCode(response.getInteger(EXTERNAL_STATUS_KEY));
              routingContext.response().end(
                  Buffer.newInstance(new JsonObject().put(USERNAME_KEY, username).toBuffer()));
            }
            return Completable.complete();
          })
          .doOnError(error -> handleError(routingContext, error))
          .subscribe();
    });

    router.route(HttpMethod.PATCH, PATH + "/:id").handler(routingContext -> {
      String username = routingContext.request().getParam("id");
      if (!username.equals(routingContext.request().getHeader(USERNAME_HEADER))) {
        routingContext.response().setStatusCode(StatusCode.FORBIDDEN.getStatusCode());
        routingContext.response().end(Buffer.newInstance(new JsonObject()
            .put(MESSAGE_KEY, "You can change only your own data").toBuffer()));
      }
      JsonObject body = getBody(routingContext);
      String password = body.getString(PASSWORD_KEY);
      if (StringUtils.isNotEmpty(password)) {
        Users.USER_STORAGE.getUser(username)
            .map(user -> user.setPassword(password))
            .flatMap(user -> Users.USER_STORAGE.updatePassword(user))
            .flatMapCompletable(response -> {
              makeErrorResponse(routingContext, response);
              routingContext.response().setStatusCode(StatusCode.SUCCESS.getStatusCode());
              routingContext.response().end();
              return Completable.complete();
            })
            .doOnError(error -> handleError(routingContext, error))
            .subscribe();
      }
      routingContext.response().setStatusCode(StatusCode.SUCCESS.getStatusCode());
      routingContext.response().end();
    });

    router.route(HttpMethod.DELETE, PATH + "/:id").handler(routingContext -> {
      checkAccessRights(routingContext);
      String username = routingContext.request().getParam("id");
      if (username.equals(ADMIN.getUsername())) {
        routingContext.response().setStatusCode(StatusCode.FORBIDDEN.getStatusCode());
        routingContext.response().end(Buffer.newInstance(new JsonObject()
            .put(MESSAGE_KEY, String.format("You can't delete user %s", ADMIN.getUsername())).toBuffer()));
      }
      Users.USER_STORAGE.deleteUser(username)
          .flatMapCompletable(response -> {
            if (InternalStatus.valueOf(response.getString(INTERNAL_STATUS_KEY)).isOk) {
              routingContext.response().setStatusCode(StatusCode.NO_CONTENT.getStatusCode());
              routingContext.response().end();
            } else {
              makeErrorResponse(routingContext, response);
            }
            return Completable.complete();
          })
          .doOnError(error -> handleError(routingContext, error))
          .subscribe();

    });
    return router;
  }

  private void checkAccessRights(RoutingContext routingContext) {
    if (!UserType.getUserType(routingContext.request().getHeader(USER_TYPE_HEADER)).equals(UserType.ADMIN)) {
      routingContext.fail(StatusCode.FORBIDDEN.getStatusCode());
    }
  }

  private JsonObject getBody(RoutingContext routingContext) {
    try {
      return routingContext.getBodyAsJson();
    } catch (Exception e) {
      System.out.println(e.getMessage());
      routingContext.fail(StatusCode.BAD_REQUEST.getStatusCode(), e);
    }
    return null;
  }

  private void handleError(RoutingContext context, Throwable error) {
    System.out.println(error.getMessage());
    context.fail(StatusCode.ERROR.getStatusCode(), error);
  }

  private void makeErrorResponse(RoutingContext context, JsonObject internalResponse) {
    if (!InternalStatus.valueOf(internalResponse.getString(INTERNAL_STATUS_KEY)).isOk) {
      context.response().setStatusCode(internalResponse.getInteger(EXTERNAL_STATUS_KEY));
      context.response().end(Buffer.newInstance(new JsonObject()
          .put(MESSAGE_KEY, internalResponse.getString(MESSAGE_KEY)).toBuffer()));
    }
  }


}
