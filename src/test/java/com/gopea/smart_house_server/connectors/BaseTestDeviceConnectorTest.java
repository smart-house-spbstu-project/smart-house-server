package com.gopea.smart_house_server.connectors;

import com.gopea.smart_house_server.common.InternalStatus;
import com.gopea.smart_house_server.configs.StatusCode;
import com.gopea.smart_house_server.examples.StandardDeviceExample;
import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.reactivex.core.Vertx;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.gopea.smart_house_server.common.Helpers.EXTERNAL_STATUS_KEY;
import static com.gopea.smart_house_server.common.Helpers.INTERNAL_STATUS_KEY;
import static com.gopea.smart_house_server.common.Helpers.isInternalStatusOk;
import static com.gopea.smart_house_server.connectors.Connectors.COMMAND_ACTION_KEY;
import static org.mockito.ArgumentMatchers.any;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(VertxUnitRunner.class)
public class BaseTestDeviceConnectorTest {

  private StandardDeviceExample deviceExample;
  private BaseTestDeviceConnector deviceConnector;

  @Before
  public void before() {
    deviceExample = mock(StandardDeviceExample.class);
    deviceConnector = new BaseTestDeviceConnector("", 0, deviceExample);
  }

  @Test(timeout = 60000)
  public void testConnectSuccess(TestContext context) {
    final Async async = context.async();

    when(deviceExample.getResponse(any())).thenReturn(new JsonObject()
        .put(INTERNAL_STATUS_KEY, InternalStatus.OK)
        .put(COMMAND_ACTION_KEY, BaseTestDeviceConnector.ConnectionState.CONNECTED)
    );

    deviceConnector.isConnected()
        .flatMapCompletable(isConnected -> Completable.fromAction(() -> context.assertFalse(isConnected)))
        .andThen(deviceConnector.connect())
        .map(response -> isInternalStatusOk(response))
        .flatMapCompletable(isOk -> Completable.fromAction(() -> context.assertTrue(isOk)))
        .andThen(deviceConnector.isConnected())
        .flatMapCompletable(isConnected -> Completable.fromAction(() -> context.assertTrue(isConnected)))
        .andThen(Completable.fromAction(() -> async.complete()))
        .subscribe();
  }

  @Test(timeout = 60000)
  public void testConnectFail(TestContext context) {
    final Async async = context.async();

    when(deviceExample.getResponse(any())).thenReturn(new JsonObject());

    deviceConnector.isConnected()
        .flatMapCompletable(isConnected -> Completable.fromAction(() -> context.assertFalse(isConnected)))
        .andThen(deviceConnector.connect())
        .map(response -> isInternalStatusOk(response))
        .flatMapCompletable(isOk -> Completable.fromAction(() -> context.assertFalse(isOk)))
        .andThen(deviceConnector.isConnected())
        .flatMapCompletable(isConnected -> Completable.fromAction(() -> context.assertFalse(isConnected)))
        .andThen(Completable.fromAction(() -> async.complete()))
        .subscribe();
  }

  @Test(timeout = 60000)
  public void testDisconnectSuccess(TestContext context) {
    final Async async = context.async();

    when(deviceExample.getResponse(any())).thenReturn(new JsonObject()
        .put(INTERNAL_STATUS_KEY, InternalStatus.OK)
        .put(COMMAND_ACTION_KEY, BaseTestDeviceConnector.ConnectionState.DISCONNECTED)
    );

    deviceConnector.disconnect()
        .map(response -> isInternalStatusOk(response))
        .flatMapCompletable(isOk -> Completable.fromAction(() -> context.assertTrue(isOk)))
        .andThen(deviceConnector.isConnected())
        .flatMapCompletable(isConnected -> Completable.fromAction(() -> context.assertFalse(isConnected)))
        .andThen(Completable.fromAction(() -> async.complete()))
        .subscribe();
  }

  @Test(timeout = 60000)
  public void testDisconnectFail(TestContext context) {
    final Async async = context.async();

    when(deviceExample.getResponse(any())).thenReturn(new JsonObject());

    deviceConnector.disconnect()
        .map(response -> isInternalStatusOk(response))
        .flatMapCompletable(isOk -> Completable.fromAction(() -> context.assertFalse(isOk)))
        .andThen(Completable.fromAction(() -> async.complete()))
        .subscribe();
  }

  @Test(timeout = 60000)
  public void testHasMessageFalse(TestContext context) {
    final Async async = context.async();
    deviceConnector.hasMessage()
        .flatMapCompletable(has -> Completable.fromAction(() -> context.assertFalse(has)))
        .andThen(Completable.fromAction(() -> async.complete()))
        .subscribe();
  }

  @Test(timeout = 60000)
  public void testHasMessageTrue(TestContext context) {
    final Async async = context.async();

    deviceConnector.messagesFromDevice.add(new JsonObject());

    deviceConnector.hasMessage()
        .flatMapCompletable(has -> Completable.fromAction(() -> context.assertTrue(has)))
        .andThen(Completable.fromAction(() -> async.complete()))
        .subscribe();
  }

  @Test(timeout = 60000)
  public void testSendMessage(TestContext context) {
    final Async async = context.async();

    when(deviceExample.getResponse(any())).thenReturn(new JsonObject()
        .put(INTERNAL_STATUS_KEY, InternalStatus.OK)
        .put(COMMAND_ACTION_KEY, BaseTestDeviceConnector.ConnectionState.CONNECTED)
    );

    deviceConnector.isConnected()
        .flatMapCompletable(isConnected -> Completable.fromAction(() -> context.assertFalse(isConnected)))
        .andThen(deviceConnector.connect())
        .map(response -> isInternalStatusOk(response))
        .flatMapCompletable(isOk -> Completable.fromAction(() -> context.assertTrue(isOk)))
        .andThen(deviceConnector.isConnected())
        .flatMapCompletable(isConnected -> Completable.fromAction(() -> context.assertTrue(isConnected)))
        .andThen(deviceConnector.sendMessage(new JsonObject()))
        .flatMapCompletable(response -> Completable.fromAction(() -> context.assertTrue(isInternalStatusOk(response))))
        .andThen(Completable.fromAction(() -> async.complete()))
        .subscribe();
  }

  @Test(timeout = 60000)
  public void testSendMessageFail(TestContext context) {
    final Async async = context.async();

    when(deviceExample.getResponse(any())).thenReturn(new JsonObject()
        .put(COMMAND_ACTION_KEY, BaseTestDeviceConnector.ConnectionState.DISCONNECTED)
    );

    deviceConnector.isConnected()
        .flatMapCompletable(isConnected -> Completable.fromAction(() -> context.assertFalse(isConnected)))
        .andThen(deviceConnector.connect())
        .map(response -> isInternalStatusOk(response))
        .flatMapCompletable(isOk -> Completable.fromAction(() -> context.assertFalse(isOk)))
        .andThen(deviceConnector.isConnected())
        .flatMapCompletable(isConnected -> Completable.fromAction(() -> context.assertFalse(isConnected)))
        .andThen(deviceConnector.sendMessage(new JsonObject()))
        .flatMapCompletable(response -> Completable.fromAction(() -> {
          context.assertFalse(isInternalStatusOk(response));
          context.assertEquals(StatusCode.UNAVAILABLE.getStatusCode(), response.getInteger(EXTERNAL_STATUS_KEY));
        }))
        .andThen(Completable.fromAction(() -> async.complete()))
        .subscribe();
  }

  @Test(timeout = 60000)
  public void testGetMessage(TestContext context) {
    final Async async = context.async();

    when(deviceExample.getResponse(any())).thenReturn(new JsonObject()
        .put(COMMAND_ACTION_KEY, BaseTestDeviceConnector.ConnectionState.CONNECTED)
    );

    JsonObject message = new JsonObject().put("key", "test message");

    deviceConnector.messagesFromDevice.add(message);

    deviceConnector.isConnected()
        .flatMapCompletable(isConnected -> Completable.fromAction(() -> context.assertFalse(isConnected)))
        .andThen(deviceConnector.connect())
        .map(response -> isInternalStatusOk(response))
        .flatMapCompletable(isOk -> Completable.fromAction(() -> context.assertTrue(isOk)))
        .andThen(deviceConnector.isConnected())
        .flatMapCompletable(isConnected -> Completable.fromAction(() -> context.assertTrue(isConnected)))
        .andThen(deviceConnector.getMessage())
        .flatMapCompletable(response -> Completable.fromAction(() -> context.assertEquals(message, response)))
        .andThen(Completable.fromAction(() -> async.complete()))
        .subscribe();
  }

  @Test(timeout = 60000)
  public void testGetMessageFail(TestContext context) {
    final Async async = context.async();

    when(deviceExample.getResponse(any())).thenReturn(new JsonObject()
        .put(COMMAND_ACTION_KEY, BaseTestDeviceConnector.ConnectionState.DISCONNECTED)
    );

    JsonObject message = new JsonObject().put("key", "test message");

    deviceConnector.messagesFromDevice.add(message);

    deviceConnector.getMessage()
        .flatMapCompletable(response -> Completable.fromAction(() -> {
          context.assertFalse(isInternalStatusOk(response));
          context.assertEquals(StatusCode.UNAVAILABLE.getStatusCode(), response.getInteger(EXTERNAL_STATUS_KEY));
        }))
        .andThen(Completable.fromAction(() -> async.complete()))
        .subscribe();
  }

  @Test(timeout = 60000)
  public void testGetMessageEmpty(TestContext context) {
    final Async async = context.async();

    when(deviceExample.getResponse(any())).thenReturn(new JsonObject()
        .put(COMMAND_ACTION_KEY, BaseTestDeviceConnector.ConnectionState.CONNECTED)
    );

    JsonObject message = new JsonObject().put("key", "test message");

    deviceConnector.isConnected()
        .flatMapCompletable(isConnected -> Completable.fromAction(() -> context.assertFalse(isConnected)))
        .andThen(deviceConnector.connect())
        .map(response -> isInternalStatusOk(response))
        .flatMapCompletable(isOk -> Completable.fromAction(() -> context.assertTrue(isOk)))
        .andThen(deviceConnector.isConnected())
        .flatMapCompletable(isConnected -> Completable.fromAction(() -> context.assertTrue(isConnected)))
        .andThen(deviceConnector.getMessage())
        .isEmpty()
        .flatMapCompletable(isEmpty -> Completable.fromAction(() -> context.assertTrue(isEmpty)))
        .andThen(Completable.fromAction(() -> async.complete()))
        .subscribe();
  }

}


