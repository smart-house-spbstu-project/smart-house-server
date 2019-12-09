package com.gopea.smart_house_server.routers;

import com.gopea.smart_house_server.configs.RouteConfiguration;
import com.gopea.smart_house_server.configs.StatusCode;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.Router;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;
import static io.vertx.core.http.HttpHeaders.TEXT_HTML;

public class HelloRouter implements Routable {

  @Override
  public Router loadRouter(Vertx vertx) {
    Router router = Router.router(vertx);
    router.route(RouteConfiguration.REST_PREFIX).handler(context -> {
      context.response().putHeader(CONTENT_TYPE, TEXT_HTML);
      context.response().setStatusCode(StatusCode.SUCCESS.getStatusCode());
      context.response().end("<h1>In the future we will add special page</h1>");
    });
    return router;
  }
}

