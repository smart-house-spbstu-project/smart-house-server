package com.gopea.smart_house_server.data_base;

import com.gopea.smart_house_server.common.Helpers;
import com.gopea.smart_house_server.configs.StatusCode;
import com.gopea.smart_house_server.devices.BaseDevice;
import com.gopea.smart_house_server.devices.Device;
import com.gopea.smart_house_server.devices.Devices;
import com.gopea.smart_house_server.devices.Door;
import com.sun.xml.internal.ws.util.xml.CDATA;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.gopea.smart_house_server.common.Helpers.EXTERNAL_STATUS_KEY;
import static com.gopea.smart_house_server.common.Helpers.isInternalStatusOk;

@RunWith(VertxUnitRunner.class)
public class RuntimeDeviceStorageTest {

  @Test(timeout = 60000)
  public void testAddStorage(TestContext context) {
    final Async async = context.async();

    DeviceStorage target = new RuntimeDeviceStorage();

    Device device = new Door(new JsonObject()
        .put(BaseDevice.HOST_KEY, "host")
        .put(BaseDevice.PORT_KEY, 8080)
    );

    target.addDevice(device)
        .map(response -> {
          context.assertTrue(Helpers.isInternalStatusOk(response));
          context.assertEquals(StatusCode.CREATED.getStatusCode(), response.getInteger(EXTERNAL_STATUS_KEY));
          return response.getString(Storages.ID);
        })
        .flatMapMaybe(id -> target.getDevice(id))
        .switchIfEmpty(Maybe.defer(() -> {
          context.fail();
          return Maybe.empty();
        }))
        .flatMapCompletable(device1 -> Completable.fromAction(() -> context.assertEquals(device, device1)))
        .andThen(Completable.fromAction(() -> async.complete()))
        .subscribe();
  }

  @Test(timeout = 60000)
  public void testGetDeviceFail(TestContext context) {
    final Async async = context.async();


    DeviceStorage target = new RuntimeDeviceStorage();

    target.getDevice("")
        .isEmpty()
        .flatMapCompletable(isEmpty -> Completable.fromAction(() -> context.assertTrue(isEmpty)))
        .andThen(Completable.fromAction(() -> async.complete()))
        .subscribe();
  }

  @Test(timeout = 60000)
  public void testDeleteDeviceFail(TestContext context) {
    final Async async = context.async();

    DeviceStorage target = new RuntimeDeviceStorage();

    target.deleteDevice("")
        .flatMapCompletable(response -> Completable.fromAction(() -> {
              context.assertFalse(isInternalStatusOk(response));
              context.assertEquals(StatusCode.NOT_FOUND.getStatusCode(), response.getInteger(EXTERNAL_STATUS_KEY));
              async.complete();
            })
        )
        .subscribe();

  }

  @Test(timeout = 60000)
  public void testDeleteDevice(TestContext context) {
    final Async async = context.async();

    Device device = new Door(new JsonObject()
        .put(BaseDevice.HOST_KEY, "host")
        .put(BaseDevice.PORT_KEY, 8080)
    );

    DeviceStorage target = new RuntimeDeviceStorage();

    target.addDevice(device)
        .map(response -> {
          context.assertTrue(Helpers.isInternalStatusOk(response));
          context.assertEquals(StatusCode.CREATED.getStatusCode(), response.getInteger(EXTERNAL_STATUS_KEY));
          return response.getString(Storages.ID);
        })
        .flatMap(id -> target.deleteDevice(id))
        .flatMapCompletable(response -> Completable.fromAction(() -> {
          context.assertTrue(isInternalStatusOk(response));
          context.assertEquals(StatusCode.NO_CONTENT.getStatusCode(), response.getInteger(EXTERNAL_STATUS_KEY));
        }))
        .andThen(Completable.fromAction(() -> async.complete()))
        .subscribe();
  }

  @Test(timeout = 60000)
  public void testGetDevices(TestContext context) {
    final Async async = context.async();

    DeviceStorage target = new RuntimeDeviceStorage();

    Device device = new Door(new JsonObject()
        .put(BaseDevice.HOST_KEY, "host")
        .put(BaseDevice.PORT_KEY, 8080)
    );

    target.addDevice(device)
        .map(response -> {
          context.assertTrue(isInternalStatusOk(response));
          context.assertEquals(StatusCode.CREATED.getStatusCode(), response.getInteger(EXTERNAL_STATUS_KEY));
          return response.getString(Storages.ID);
        })
        .flatMapCompletable(id -> target.getDevices()
            .flatMapCompletable(list -> Completable.fromAction(() -> {
              context.assertEquals(1, list.size());
              Pair<String, Device> data = list.get(0);
              context.assertEquals(id, data.getKey());
              context.assertEquals(device, data.getValue());
            })))
        .andThen(Completable.fromAction(() -> async.complete()))
        .subscribe();

  }
}
