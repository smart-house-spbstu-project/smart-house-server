package com.gopea.smart_house_server;

import com.gopea.smart_house_server.configs.RoutConfiguration;
import io.vertx.core.Future;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.ext.web.Router;

import static com.gopea.smart_house_server.configs.InitConfigs.HTTP_PORT;
import static com.gopea.smart_house_server.configs.InitConfigs.setupInitConfig;


public class MainVerticle extends AbstractVerticle {

  @Override
  public void start(Future<Void> startFuture) throws Exception {
    super.start(startFuture);
    Router router = Router.router(vertx);
    RoutConfiguration.configureRouter(router, vertx);
    setupInitConfig(vertx)
        .andThen(vertx
            .createHttpServer()
            .requestHandler(router)
            .rxListen(HTTP_PORT))
        .subscribe(httpServer -> startFuture.complete(), startFuture::fail);
  }
}