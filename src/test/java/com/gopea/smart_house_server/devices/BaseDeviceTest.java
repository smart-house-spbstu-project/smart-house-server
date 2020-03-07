package com.gopea.smart_house_server.devices;

import com.gopea.smart_house_server.configs.StatusCode;
import io.reactivex.Completable;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.reactivex.core.Vertx;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.gopea.smart_house_server.TestHelpers.deleteDeviceFiles;
import static com.gopea.smart_house_server.common.Helpers.EXTERNAL_STATUS_KEY;
import static com.gopea.smart_house_server.common.Helpers.isInternalStatusOk;
import static com.gopea.smart_house_server.devices.BaseDevice.UPDATE_TIME_KEY;
import static junit.framework.TestCase.assertEquals;

@RunWith(VertxUnitRunner.class)
public class BaseDeviceTest {

    private static final String HOST = "host_val";
    private static final int PORT = 80;
    private static final JsonObject BASE_OBJECT = new JsonObject()
            .put("host", HOST)
            .put("port", PORT);

    @After
    public void after(TestContext context){
      Vertx vertx = Vertx.vertx();
      final Async async = context.async();
      deleteDeviceFiles(vertx)
          .andThen(vertx.rxClose())
          .andThen(Completable.fromAction(async::complete))
          .subscribe();
    }

    @Test
    public void testGetInstance() throws Exception {
        Lamp lamp = BaseDevice.getInstance(Lamp.class, BASE_OBJECT);
        assertEquals(PORT, lamp.port);
        assertEquals(HOST, lamp.host);
    }

    @Test(timeout = 60000)
    public void testGetUpdateTime(TestContext context) {
        final Async async = context.async();
        final JsonObject command = new JsonObject()
                .put(UPDATE_TIME_KEY, 1);

        BaseDevice door = new Door(BASE_OBJECT);

        door.connect()
                .flatMap(ign -> door.update(command))
                .map(response -> {
                    context.assertTrue(isInternalStatusOk(response));
                    context.assertEquals(StatusCode.SUCCESS.getStatusCode(), response.getInteger(EXTERNAL_STATUS_KEY));
                    context.assertEquals(1, response.getInteger(UPDATE_TIME_KEY));
                    return door.getUpdateTime();
                })
                .flatMapCompletable(updateTime -> Completable.fromAction(() -> {
                    context.assertEquals(1, updateTime);
                    async.complete();
                }))
                .subscribe();

    }

    @Test(timeout = 60000)
    public void testSetUpdateTime() {

        final JsonObject command = new JsonObject()
                .put(UPDATE_TIME_KEY, 1);

        BaseDevice door = new Door(BASE_OBJECT);
        door.setUpdateTime(2);
        assertEquals(2, door.getUpdateTime());

    }

    @Test(timeout = 180_000)
    public void testMetricsMaxSize(TestContext context) {
        final Async async = context.async();

        final JsonObject command = new JsonObject()
                .put(UPDATE_TIME_KEY, 1);

        Device device = new Door(BASE_OBJECT);

        device.connect()
                .flatMap(ign -> device.update(command))
                .flatMapCompletable(response -> Completable.fromAction(() -> {
                    context.assertTrue(isInternalStatusOk(response));
                    context.assertEquals(StatusCode.SUCCESS.getStatusCode(), response.getInteger(EXTERNAL_STATUS_KEY));
                    context.assertEquals(1, response.getInteger(UPDATE_TIME_KEY));
                    Thread.sleep(1 * 1000 * 110);
                }))
                .andThen(device.getMetrics())
                .flatMapCompletable(array -> Completable.fromAction(() -> {
                    context.assertEquals(100, array.size());
                    async.complete();
                }))
                .subscribe();
    }
}
