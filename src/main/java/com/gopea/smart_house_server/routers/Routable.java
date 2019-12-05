package com.gopea.smart_house_server.routers;

import io.reactivex.Single;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.Router;

public interface Routable {
  Router loadRouter(Vertx vertx);
}
