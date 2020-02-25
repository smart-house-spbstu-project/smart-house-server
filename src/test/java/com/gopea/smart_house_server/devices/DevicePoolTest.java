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
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static com.gopea.smart_house_server.common.Helpers.EXTERNAL_STATUS_KEY;
import static com.gopea.smart_house_server.common.Helpers.isInternalStatusOk;
import static com.gopea.smart_house_server.connectors.Connectors.COMMAND_ACTION_KEY;
import static com.gopea.smart_house_server.devices.BaseDevice.*;
import static com.gopea.smart_house_server.devices.BaseDevice.UPDATE_TIME_KEY;
import static com.gopea.smart_house_server.examples.StandardDeviceExample.DATA_KEY;
import static com.gopea.smart_house_server.examples.StandardDeviceExample.STATE_KEY;
import static junit.framework.TestCase.assertEquals;

@RunWith(VertxUnitRunner.class)
public class DevicePoolTest {

    private static final String HOST = "host_val";
    private static final int PORT = 80;
    private static final JsonObject BASE_OBJECT = new JsonObject()
            .put("host", HOST)
            .put("port", PORT);

    private Vertx vertx;
    private DevicePool device;


    @Before
    public void before() {
        vertx = Vertx.vertx();
        device = new DevicePool(Arrays.asList(
                new ImmutablePair<>("first", new Door(BASE_OBJECT)),
                new ImmutablePair<>("second", new Door(BASE_OBJECT))),
                DeviceType.DOOR);
    }

    @After
    public void after() {
        vertx.close();
    }

    @Test
    public void testConstructor() {
        DevicePool target = new DevicePool(new ArrayList<>(), DeviceType.DOOR);
    }

    @Test(expected = RuntimeException.class)
    public void testConstructorFail() {
        new DevicePool(Collections.singletonList(new ImmutablePair<>("", new Lamp(BASE_OBJECT))), DeviceType.DOOR);
    }


    @Test
    public void testGetType() {
        assertEquals(DeviceType.DOOR, device.getType());
    }

    @Test(timeout = 60000)
    public void testConnect(TestContext context) {
        final Async async = context.async();
        device.connect()
                .flatMapCompletable(response -> Completable.fromAction(() -> {
                    context.assertTrue(isInternalStatusOk(response));
                    context.assertEquals(StatusCode.SUCCESS.getStatusCode(), response.getInteger(EXTERNAL_STATUS_KEY));
                    context.assertEquals(2, response.getJsonArray("responses").size());
                    async.complete();
                }))
                .doOnError(err -> {
                    context.fail(err);
                    async.complete();
                })
                .subscribe();
    }

    @Test(timeout = 0)
    public void testDisconnected(TestContext context) {
        final Async async = context.async();
        device.connect()
                .flatMap(ign -> device.disconnect())
                .flatMapCompletable(response -> Completable.fromAction(() -> {
                    context.assertTrue(Helpers.isInternalStatusOk(response));
                    context.assertEquals(StatusCode.SUCCESS.getStatusCode(), response.getInteger(EXTERNAL_STATUS_KEY));
                    context.assertEquals(2, response.getJsonArray("responses").size());
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
                    context.assertTrue(Helpers.isInternalStatusOk(response));
                    context.assertEquals(StatusCode.SUCCESS.getStatusCode(), response.getInteger(EXTERNAL_STATUS_KEY));
                    context.assertEquals(2, response.getJsonArray("responses").size());
                    async.complete();
                }))
                .subscribe();
    }

    @Test(timeout = 60000)
    public void testGetState(TestContext context) {
        final Async async = context.async();
        device.getState()
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
                    context.assertTrue(isInternalStatusOk(response));
                    context.assertEquals(StatusCode.SUCCESS.getStatusCode(), response.getInteger(EXTERNAL_STATUS_KEY));
                    context.assertEquals(2, response.getJsonArray("responses").size());
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
                    context.assertEquals(StatusCode.ERROR.getStatusCode(), response.getInteger(EXTERNAL_STATUS_KEY));
                    context.assertEquals(2, response.getJsonArray("responses").size());
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
                    context.assertEquals(StatusCode.ERROR.getStatusCode(), response.getInteger(EXTERNAL_STATUS_KEY));
                    context.assertEquals(2, response.getJsonArray("responses").size());
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
                    context.assertTrue(isInternalStatusOk(response));
                    context.assertEquals(StatusCode.SUCCESS.getStatusCode(), response.getInteger(EXTERNAL_STATUS_KEY));
                    context.assertEquals(2, response.getJsonArray("responses").size());
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
                    context.assertEquals(2, response.getJsonArray("responses").size());
                    context.assertTrue(response.getJsonArray("responses").getJsonObject(0).containsKey(DATA_KEY));
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
                    context.assertEquals(StatusCode.ERROR.getStatusCode(), response.getInteger(EXTERNAL_STATUS_KEY));
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
                    context.assertEquals(2, response.size());
                    for (int i = 0; i < response.size(); i++) {
                        context.assertEquals(4, response.getJsonObject(i).getJsonArray("metrics").size());
                    }
                    async.complete();
                }))
                .subscribe();
    }

    @Test
    public void testToJson() {
        JsonObject json = device.toJson();
        assertEquals(2, json.getJsonArray("devices").size());
        assertEquals(DeviceType.DOOR.name().toLowerCase(), json.getString("device_type"));
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
                    context.assertEquals(2, response.getJsonArray("responses").size());
                    async.complete();
                }))
                .subscribe();
    }

    @Test(timeout = 60000)
    public void testUpdateTime(TestContext context) {
        final Async async = context.async();

        final JsonObject command = new JsonObject()
                .put(UPDATE_TIME_KEY, 1);

        device.connect()
                .flatMap(ign -> device.update(command))
                .flatMapCompletable(response -> Completable.fromAction(() -> {
                    context.assertTrue(isInternalStatusOk(response));
                    context.assertEquals(StatusCode.SUCCESS.getStatusCode(), response.getInteger(EXTERNAL_STATUS_KEY));
                    Thread.sleep(1 * 1000 * 3);
                }))
                .andThen(device.getMetrics())
                .flatMapCompletable(array -> Completable.fromAction(() -> {
                    context.assertEquals(2, array.size());
                    context.assertTrue(array.getJsonObject(0).getJsonArray("metrics").size() >= 3);
                    async.complete();
                }))
                .subscribe();
    }

    @Test(timeout = 60000)
    public void testUpdateAddDevice(TestContext context) {
        final Async async = context.async();

        final JsonObject command = new JsonObject()
                .put(UPDATE_TIME_KEY, 1);

        device.connect()
                .flatMap(ign -> device.update(command))
                .flatMapCompletable(response -> Completable.fromAction(() -> {
                    context.assertTrue(isInternalStatusOk(response));
                    context.assertEquals(StatusCode.SUCCESS.getStatusCode(), response.getInteger(EXTERNAL_STATUS_KEY));
                    Thread.sleep(1 * 1000 * 3);
                }))
                .andThen(device.getMetrics())
                .flatMapCompletable(array -> Completable.fromAction(() -> {
                    context.assertEquals(2, array.size());
                    context.assertTrue(array.getJsonObject(0).getJsonArray("metrics").size() >= 3);
                    async.complete();
                }))
                .subscribe();
    }

    @Test(timeout = 60000)
    public void testUpdateRemoveDevice(TestContext context) {
        final Async async = context.async();

        final JsonObject command = new JsonObject()
                .put(UPDATE_TIME_KEY, 1);

        device.connect()
                .flatMap(ign -> device.update(command))
                .flatMapCompletable(response -> Completable.fromAction(() -> {
                    context.assertTrue(isInternalStatusOk(response));
                    context.assertEquals(StatusCode.SUCCESS.getStatusCode(), response.getInteger(EXTERNAL_STATUS_KEY));
                    Thread.sleep(1 * 1000 * 3);
                }))
                .andThen(device.getMetrics())
                .flatMapCompletable(array -> Completable.fromAction(() -> {
                    context.assertEquals(2, array.size());
                    context.assertTrue(array.getJsonObject(0).getJsonArray("metrics").size() >= 3);
                    async.complete();
                }))
                .subscribe();
    }

    @Test(timeout = 60000)
    public void testUpdateRemoveEmptyBody(TestContext context) {
        final Async async = context.async();

        final JsonObject command = new JsonObject();

        device.connect()
                .flatMap(ign -> device.update(command))
                .flatMapCompletable(response -> Completable.fromAction(() -> {
                    context.assertFalse(isInternalStatusOk(response));
                    context.assertEquals(StatusCode.BAD_REQUEST.getStatusCode(), response.getInteger(EXTERNAL_STATUS_KEY));
                    async.complete();
                }))
                .subscribe();
    }

}
