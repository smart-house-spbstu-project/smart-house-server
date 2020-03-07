package com.gopea.smart_house_server;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.vertx.reactivex.core.Vertx;

public final class TestHelpers {

  public static Completable deleteDeviceFiles(Vertx vertx){
    return vertx.fileSystem().rxReadDir("./")
        .flatMapCompletable(list ->
            Flowable.fromIterable(list)
                .filter(string -> string.endsWith(".txt"))
                .flatMapCompletable(string -> vertx.fileSystem().rxDelete(string))
        );
  }
}
