package com.gopea.smart_house_server.routers;

import com.gopea.smart_house_server.configs.StatusCode;
import com.gopea.smart_house_server.routers.users.Users;
import io.reactivex.Completable;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.Router;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import static com.gopea.smart_house_server.helpers.Helpers.USERNAME_HEADER;
import static com.gopea.smart_house_server.helpers.Helpers.USER_TYPE_HEADER;


public class AuthRouter implements Routable {

  @Override
  public Router loadRouter(Vertx vertx) {

    Router router = Router.router(vertx);
    router.route("/*").handler(context -> {
      String authHeader = context.request().getHeader("Authorization");
      Pair<String, String> credentials = getCredentials(authHeader);
      if (credentials == null) {
        context.fail(StatusCode.UNAUTHORISED.getStatusCode());
      }

      Users.USER_STORAGE.getUser(credentials.getLeft())
          .flatMapCompletable(user1 -> {
            if (user1 == null || !user1.checkPassword(credentials.getRight())) {
              context.fail(StatusCode.UNAUTHORISED.getStatusCode());
            }
            context.request().headers().add(USERNAME_HEADER, user1.getUsername());
            context.request().headers().add(USER_TYPE_HEADER, user1.getUserType().toString().toLowerCase());
            return Completable.complete();
          })
          .subscribe(context::next, context::fail);

    });
    return router;
  }

  private Pair<String, String> getCredentials(String authHeader) {
    if (StringUtils.isEmpty(authHeader) || !authHeader.startsWith("Basic ")) {
      return null;
    }

    String encodeCredentials = authHeader.replaceFirst("Basic ", "");
    String usernamePassword = new String(Base64.decodeBase64(encodeCredentials.getBytes()));
    String[] credentials = usernamePassword.split(":");
    if (credentials.length != 2) {
      return null;
    }
    String username = credentials[0];
    String password = credentials[1];
    return new ImmutablePair<>(username, password);
  }
}
