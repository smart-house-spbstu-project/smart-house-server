package com.gopea.smart_house_server.devices;

import com.gopea.smart_house_server.connectors.Connector;
import io.vertx.core.json.JsonObject;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

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

    private static class TestClass extends RGBLamp {

        public TestClass() {
            super(BASE_OBJECT);
        }

        public Connector get() {
            return super.getConnector(HOST, PORT);
        }
    }
}