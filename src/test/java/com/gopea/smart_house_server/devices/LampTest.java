package com.gopea.smart_house_server.devices;

import com.gopea.smart_house_server.connectors.Connector;
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
import static junit.framework.TestCase.assertEquals;

@RunWith(VertxUnitRunner.class)
public class LampTest {
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
    public void testGetType() {
        Lamp target = new Lamp(BASE_OBJECT);
        assertEquals(DeviceType.LAMP, target.getType());
    }

    @Test
    public void testGetConnector() {
        TestClass target = new TestClass();

        Connector result = target.get();

        assertEquals(PORT, result.getPort());
        assertEquals(HOST, result.getHost());
    }

    private static class TestClass extends Lamp {

        public TestClass() {
            super(BASE_OBJECT);
        }

        public Connector get() {
            return super.getConnector(HOST, PORT);
        }
    }
}
