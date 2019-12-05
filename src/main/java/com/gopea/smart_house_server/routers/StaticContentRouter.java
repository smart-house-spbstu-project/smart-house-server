package com.gopea.smart_house_server.routers;

import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.handler.StaticHandler;

public class StaticContentRouter implements Routable {

  @Override
  public Router loadRouter(Vertx vertx) {
    Router router = Router.router(vertx);
    router.route("/info/*").handler(StaticHandler.create("info"));
    return router;
  }
}
