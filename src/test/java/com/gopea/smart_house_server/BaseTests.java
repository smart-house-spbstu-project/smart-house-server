package com.gopea.smart_house_server;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class BaseTests {
  private static final int TIMEOUT = 60;

  @Test
  public void testSingleNull(TestContext context) {
    //final Async async = context.async();
    Maybe.empty()
        .flatMapCompletable(element->{
            context.assertEquals(null, element);
            //async.complete();
          return Completable.complete();
        })
        .subscribe(()->{},context::fail);
  }
}
