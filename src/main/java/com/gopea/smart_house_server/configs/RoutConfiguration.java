package com.gopea.smart_house_server.configs;


import com.gopea.smart_house_server.routers.AuthRouter;
import com.gopea.smart_house_server.routers.HelloRouter;
import com.gopea.smart_house_server.routers.Routable;
import com.gopea.smart_house_server.routers.StaticContentRouter;
import com.gopea.smart_house_server.routers.users.UserRouter;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.handler.BodyHandler;

import java.util.Arrays;
import java.util.List;

public final class RoutConfiguration {
  public static final String REST_PREFIX = "/rest";
  public static final String STATIC_CONTENT_PATH = "/info";
  public static final String SELECT_KEY = "select";

  public static final List<Routable> ROUTABLES = Arrays.asList(
      new AuthRouter(),
      new HelloRouter(),
      new StaticContentRouter(),
      new UserRouter()
  );

  public static void configureRouter(Router router, Vertx vertx) {
    router.route().handler(BodyHandler.create());
    for (Routable routable : ROUTABLES) {
      router.mountSubRouter("/", routable.loadRouter(vertx));
    }
  }

  private RoutConfiguration() {
  }
}
