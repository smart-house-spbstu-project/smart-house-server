package com.gopea.smart_house_server.common;


import com.gopea.smart_house_server.configs.StatusCode;
import com.gopea.smart_house_server.routers.users.UserType;
import io.reactivex.Maybe;

import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.jetbrains.annotations.NotNull;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Helpers {


  public static final String USER_TYPE_HEADER = "User-Type";
  public static final String USERNAME_HEADER = "Username";
  public static final String PASSWORDS_FILE = "passwords.json";
  public static final String INTERNAL_STATUS_KEY = "internal_status";
  public static final String EXTERNAL_STATUS_KEY = "rest_status";
  public static final String MESSAGE_KEY = "message";
  public static final String BASE_BAD_REQUEST_MESSAGE = "Bad request";

  public static byte[] encryptPassword(String password) {
    MessageDigest messageDigest = null;
    try {
      messageDigest = MessageDigest.getInstance("SHA-256");
      messageDigest.update(password.getBytes());
      return messageDigest.digest();
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    }
    return null;
  }

  public static void handleError(RoutingContext context, Throwable error) {
    System.out.println(error.getMessage());
    error.printStackTrace();
    context.fail(StatusCode.ERROR.getStatusCode(), error);
  }

  public static void makeErrorResponse(RoutingContext context, JsonObject internalResponse) {
    if (!isInternalStatusOk(internalResponse)) {
      Integer statusCode = internalResponse.getInteger(EXTERNAL_STATUS_KEY);
      if (statusCode == null) {
        statusCode = StatusCode.ERROR.getStatusCode();
      }
      JsonObject message = new JsonObject()
          .put(MESSAGE_KEY, internalResponse.getString(MESSAGE_KEY));
      makeRestResponse(context, statusCode, message);
    }
  }

  public static void makeRestResponseFromResponse(RoutingContext context, JsonObject response, JsonObject message) {
    makeRestResponse(context, response.getInteger(EXTERNAL_STATUS_KEY), message);
  }

  public static void makeRestResponseFromResponse(RoutingContext context, JsonObject response) {
    response.remove(INTERNAL_STATUS_KEY);
    int statusKey = response.getInteger(EXTERNAL_STATUS_KEY);
    response.remove(EXTERNAL_STATUS_KEY);
    makeRestResponse(context, statusKey, response);
  }

  public static void makeErrorRestResponse(RoutingContext context, StatusCode code, Object message) {
    JsonObject messageObj = new JsonObject()
        .put(MESSAGE_KEY, message);
    makeRestResponse(context, code.getStatusCode(), messageObj);
  }

  public static boolean checkAdminRights(RoutingContext routingContext) {
    if (!UserType.getEnum(routingContext.request().getHeader(USER_TYPE_HEADER)).equals(UserType.ADMIN)) {
      routingContext.fail(StatusCode.FORBIDDEN.getStatusCode());
      return false;
    }
    return true;
  }

  public static JsonObject getBody(RoutingContext routingContext) {
    try {
      return routingContext.getBodyAsJson();
    } catch (Exception e) {
      System.out.println(e.getMessage());
      routingContext.fail(StatusCode.BAD_REQUEST.getStatusCode(), e);
    }
    return null;
  }

  public static <T> Maybe<T> handleEmptyCase(RoutingContext context, Maybe<T> maybe) {
    return maybe
        .isEmpty()
        .flatMapMaybe(isEmpty -> {
          if (isEmpty) {
            makeErrorRestResponse(context, StatusCode.NOT_FOUND, "Element not found");
            return Maybe.empty();
          }
          return maybe;
        });

  }

  public static <T extends Enum> T getEnum(String name, Class<T> clazz) {
    if (name == null) {
      return null;
    }
    name = name.toUpperCase();
    T en = null;
    try {
      en = (T) T.valueOf(clazz, name);
    } catch (IllegalArgumentException e) {
    }
    return en;
  }

  public static boolean isInternalStatusOk(JsonObject response) {
    return InternalStatus.valueOf(response.getString(INTERNAL_STATUS_KEY)).isOk;
  }

  public static boolean isEqualsWithAny(@NotNull Object base, Object... others) {
    for (Object object : others) {
      if (base.equals(object)) {
        return true;
      }
    }
    return false;
  }

  public static JsonObject createResponseJson(InternalStatus status, StatusCode code, @NotNull JsonObject response) {
    return new JsonObject()
        .put(INTERNAL_STATUS_KEY, status)
        .put(EXTERNAL_STATUS_KEY, code.getStatusCode())
        .mergeIn(response);
  }

  private static void makeRestResponse(RoutingContext context, int code, JsonObject message) {
    context.response().setStatusCode(code);
    if (message != null) {
      context.response().end(
          Buffer.newInstance(message.toBuffer()));
    } else {
      context.response().end();
    }
  }


  private Helpers() {

  }
}
