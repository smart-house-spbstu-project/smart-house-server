package com.gopea.smart_house_server.devices;

import com.gopea.smart_house_server.common.Helpers;
import com.gopea.smart_house_server.configs.StatusCode;
import com.gopea.smart_house_server.connectors.BaseTestDeviceConnector;
import com.gopea.smart_house_server.examples.StandardDeviceExample;
import io.reactivex.Completable;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.reactivex.core.Vertx;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.gopea.smart_house_server.TestHelpers.deleteDeviceFiles;
import static com.gopea.smart_house_server.common.Helpers.EXTERNAL_STATUS_KEY;
import static com.gopea.smart_house_server.common.Helpers.isInternalStatusOk;
import static com.gopea.smart_house_server.connectors.Connectors.COMMAND_ACTION_KEY;
import static com.gopea.smart_house_server.devices.BaseDevice.*;
import static com.gopea.smart_house_server.devices.BaseDevice.UPDATE_TIME_KEY;
import static com.gopea.smart_house_server.examples.StandardDeviceExample.DATA_KEY;
import static com.gopea.smart_house_server.examples.StandardDeviceExample.STATE_KEY;
import static junit.framework.TestCase.assertEquals;

@RunWith(VertxUnitRunner.class)
public class DeviceTest {
  private static final String HOST = "host_val";
  private static final int PORT = 80;
  private static final JsonObject BASE_OBJECT = new JsonObject()
      .put("host", HOST)
      .put("port", PORT);


  private Device device;

  @Before
  public void before() {
    device = new Door(BASE_OBJECT);
  }

  @After
  public void after(TestContext context) {
    Vertx vertx = Vertx.vertx();
    final Async async = context.async();
    deleteDeviceFiles(vertx)
        .andThen(vertx.rxClose())
        .andThen(Completable.fromAction(async::complete))
        .subscribe();
  }

  @Test(timeout = 60000)
  public void testConnect(TestContext context) {
    final Async async = context.async();
    device.connect()
        .flatMapCompletable(response -> Completable.fromAction(() -> {
          context.assertTrue(Helpers.isInternalStatusOk(response));
          context.assertEquals(StatusCode.SUCCESS.getStatusCode(), response.getInteger(EXTERNAL_STATUS_KEY));
          context.assertEquals(BaseTestDeviceConnector.ConnectionState.CONNECTED.name().toLowerCase(), response.getString(COMMAND_ACTION_KEY));
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60000)
  public void testDisconnected(TestContext context) {
    final Async async = context.async();
    device.connect()
        .flatMap(ign -> device.disconnect())
        .flatMapCompletable(response -> Completable.fromAction(() -> {
          context.assertTrue(Helpers.isInternalStatusOk(response));
          context.assertEquals(StatusCode.SUCCESS.getStatusCode(), response.getInteger(EXTERNAL_STATUS_KEY));
          context.assertEquals(BaseTestDeviceConnector.ConnectionState.DISCONNECTED.name().toLowerCase(), response.getString(COMMAND_ACTION_KEY));
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60000)
  public void testGetStatus(TestContext context) {
    final Async async = context.async();
    device.connect()
        .flatMap(ign -> device.getStatus())
        .flatMapCompletable(response -> Completable.fromAction(() -> {
          System.out.println(response);
          context.assertTrue(Helpers.isInternalStatusOk(response));
          context.assertEquals(StatusCode.SUCCESS.getStatusCode(), response.getInteger(EXTERNAL_STATUS_KEY));
          context.assertTrue(response.containsKey(STATUS_KEY));
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60000)
  public void testGetState(TestContext context) {
    final Async async = context.async();
    device.getState()
        .flatMapCompletable(state -> Completable.fromAction(() -> {
          context.assertEquals(DeviceState.DISCONNECTED, state);
        }))
        .andThen(device.connect())
        .flatMap(ign -> device.getState())
        .flatMapCompletable(state -> Completable.fromAction(() -> {
          context.assertEquals(DeviceState.CONNECTED, state);
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60000)
  public void testPowerOff(TestContext context) {
    final Async async = context.async();
    device.connect()
        .flatMap(ign -> device.powerOff())
        .flatMapCompletable(response -> Completable.fromAction(() -> {
          context.assertTrue(Helpers.isInternalStatusOk(response));
          context.assertEquals(StatusCode.SUCCESS.getStatusCode(), response.getInteger(EXTERNAL_STATUS_KEY));
          context.assertEquals(BaseTestDeviceConnector.ConnectionState.DISCONNECTED.name().toLowerCase(), response.getString(COMMAND_ACTION_KEY));
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60000)
  public void testPowerOffFail(TestContext context) {
    final Async async = context.async();
    device.powerOff()
        .flatMapCompletable(response -> Completable.fromAction(() -> {
          context.assertFalse(isInternalStatusOk(response));
          context.assertEquals(StatusCode.UNAVAILABLE.getStatusCode(), response.getInteger(EXTERNAL_STATUS_KEY));
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60000)
  public void testRebootDisconnected(TestContext context) {
    final Async async = context.async();
    device.reboot()
        .flatMapCompletable(response -> Completable.fromAction(() -> {
          context.assertFalse(isInternalStatusOk(response));
          context.assertEquals(StatusCode.UNAVAILABLE.getStatusCode(), response.getInteger(EXTERNAL_STATUS_KEY));
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60000)
  @Ignore
  public void testRebootFail(TestContext context) {
    final Async async = context.async();

    device.reboot()
        .flatMapCompletable(response -> Completable.fromAction(() -> {
          context.assertFalse(isInternalStatusOk(response));
          context.assertEquals(StatusCode.UNAVAILABLE.getStatusCode(), response.getInteger(EXTERNAL_STATUS_KEY));
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60000)
  public void testReboot(TestContext context) {
    final Async async = context.async();
    device.connect()
        .flatMap(ign -> device.reboot())
        .flatMapCompletable(response -> Completable.fromAction(() -> {
          context.assertTrue(Helpers.isInternalStatusOk(response));
          context.assertEquals(StatusCode.SUCCESS.getStatusCode(), response.getInteger(EXTERNAL_STATUS_KEY));
          context.assertEquals(BaseTestDeviceConnector.ConnectionState.CONNECTED.name().toLowerCase(), response.getString(COMMAND_ACTION_KEY));
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60000)
  public void testGetData(TestContext context) {
    final Async async = context.async();
    device.connect()
        .flatMap(ign -> device.getData())
        .flatMapCompletable(response -> Completable.fromAction(() -> {
          context.assertTrue(isInternalStatusOk(response));
          context.assertEquals(StatusCode.SUCCESS.getStatusCode(), response.getInteger(EXTERNAL_STATUS_KEY));
          context.assertTrue(response.containsKey(DATA_KEY));
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60000)
  public void testGetDataFail(TestContext context) {
    final Async async = context.async();
    device.getData()
        .flatMapCompletable(response -> Completable.fromAction(() -> {
          context.assertFalse(isInternalStatusOk(response));
          context.assertEquals(StatusCode.UNAVAILABLE.getStatusCode(), response.getInteger(EXTERNAL_STATUS_KEY));
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60000)
  public void testGetMetrics(TestContext context) {
    final Async async = context.async();
    device.connect()
        .flatMap(ign -> device.getData())
        .flatMap(ign -> device.getData())
        .flatMap(ign -> device.getData())
        .flatMap(ign -> device.getData())
        .flatMap(ign -> device.getMetrics())
        .flatMapCompletable(response -> Completable.fromAction(() -> {
          context.assertEquals(4, response.size());
          async.complete();
        }))
        .subscribe();
  }

  @Test
  public void testToJson() {
    JsonObject json = device.toJson();
    assertEquals(HOST, json.getString(HOST_KEY));
    assertEquals(PORT, json.getInteger(PORT_KEY).intValue());
  }

  @Test(timeout = 60000)
  public void testExecute(TestContext context) {
    final Async async = context.async();

    final JsonObject command = new JsonObject()
        .put(STATE_KEY, StandardDeviceExample.State.ON);

    device.connect()
        .flatMap(ign -> device.execute(command))
        .flatMapCompletable(response -> Completable.fromAction(() -> {
          context.assertTrue(isInternalStatusOk(response));
          context.assertEquals(StatusCode.SUCCESS.getStatusCode(), response.getInteger(EXTERNAL_STATUS_KEY));
          context.assertEquals(StandardDeviceExample.State.ON.name().toLowerCase(), response.getString(STATE_KEY));
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60000)
  public void testUpdate(TestContext context) {
    final Async async = context.async();

    final JsonObject command = new JsonObject()
        .put(UPDATE_TIME_KEY, 1);

    device.connect()
        .flatMap(ign -> device.update(command))
        .flatMapCompletable(response -> Completable.fromAction(() -> {
          context.assertTrue(isInternalStatusOk(response));
          context.assertEquals(StatusCode.SUCCESS.getStatusCode(), response.getInteger(EXTERNAL_STATUS_KEY));
          context.assertEquals(1, response.getInteger(UPDATE_TIME_KEY));
          Thread.sleep(1 * 1000 * 3);
        }))
        .andThen(device.getMetrics())
        .flatMapCompletable(array -> Completable.fromAction(() -> {
          context.assertTrue(array.size() >= 3);
          async.complete();
        }))
        .subscribe();
  }
}
