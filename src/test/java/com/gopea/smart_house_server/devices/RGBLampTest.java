package com.gopea.smart_house_server.devices;

import com.gopea.smart_house_server.configs.StatusCode;
import com.gopea.smart_house_server.connectors.Connector;
import com.gopea.smart_house_server.examples.StandardDeviceExample;
import io.reactivex.Completable;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.gopea.smart_house_server.common.Helpers.EXTERNAL_STATUS_KEY;
import static com.gopea.smart_house_server.common.Helpers.isInternalStatusOk;
import static com.gopea.smart_house_server.examples.StandardDeviceExample.STATE_KEY;
import static junit.framework.TestCase.assertEquals;

@RunWith(VertxUnitRunner.class)
public class RGBLampTest {
    private static final String HOST = "host_val";
    private static final int PORT = 80;
    private static final JsonObject BASE_OBJECT = new JsonObject()
            .put("host", HOST)
            .put("port", PORT);

    @Test
    public void testGetType() {
        RGBLamp target = new RGBLamp(BASE_OBJECT);
        assertEquals(DeviceType.RGB_LAMP, target.getType());
    }

    @Test
    public void testGetConnector() {
        TestClass target = new TestClass();

        Connector result = target.get();

        assertEquals(PORT, result.getPort());
        assertEquals(HOST, result.getHost());
    }

    @Test(timeout = 60000)
    public void testExecute(TestContext context) {
        final Async async = context.async();
        RGBLamp device = new RGBLamp(BASE_OBJECT);

        final JsonObject command = new JsonObject()
                .put(STATE_KEY, StandardDeviceExample.State.ON)
                .put("color", "red");

        device.connect()
                .flatMap(ign -> device.execute(command))
                .flatMapCompletable(response -> Completable.fromAction(() -> {
                    System.out.println(response);
                    context.assertTrue(isInternalStatusOk(response));
                    context.assertEquals(StatusCode.SUCCESS.getStatusCode(), response.getInteger(EXTERNAL_STATUS_KEY));
                    context.assertEquals(StandardDeviceExample.State.ON.name().toLowerCase(), response.getString(STATE_KEY));
                    context.assertEquals("red", response.getString("color"));
                    async.complete();
                }))
                .subscribe();
    }

    private static class TestClass extends RGBLamp {

        public TestClass() {
            super(BASE_OBJECT);
        }

        public Connector get() {
            return super.getConnector(HOST, PORT);
        }
    }
}
