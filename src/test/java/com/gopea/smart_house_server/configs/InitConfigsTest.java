package com.gopea.smart_house_server.configs;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.file.FileSystem;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.gopea.smart_house_server.common.Helpers.PASSWORDS_FILE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(VertxUnitRunner.class)
public class InitConfigsTest {

  private FileSystem fileSystem;
  private Vertx vertx;

  @Before
  public void before() {
    vertx = mock(Vertx.class);
    fileSystem = mock(FileSystem.class);
    when(vertx.fileSystem()).thenReturn(fileSystem);
  }

  @Test(timeout = 60000)
  public void testSetupInitConfigFileExists(TestContext context) {
    final Async async = context.async();

    when(fileSystem.rxExists(anyString())).thenReturn(Single.just(true));

    InitConfigs.setupInitConfig(vertx)
        .andThen(Completable.fromAction(() -> async.complete()))
        .subscribe();

    verify(fileSystem, never()).rxWriteFile(anyString(), any());
  }

  @Test(timeout = 60000)
  public void testSetupInitConfigFileNotExists(TestContext context) {
    final Async async = context.async();
    final Vertx vertx = Vertx.vertx();

    vertx.fileSystem().rxExists(PASSWORDS_FILE)
        .flatMapCompletable(exists -> {
          if (exists){
            return vertx.fileSystem().rxDelete(PASSWORDS_FILE);
          }
          return Completable.complete();
        }).andThen(InitConfigs.setupInitConfig(vertx))
        .andThen(vertx.fileSystem().rxExists(PASSWORDS_FILE))
        .map(exists-> context.assertTrue(exists))
        .flatMapCompletable(ign->vertx.fileSystem().rxDelete(PASSWORDS_FILE))
        .andThen(Completable.fromAction(() -> async.complete()))
        .subscribe();

  }
}
