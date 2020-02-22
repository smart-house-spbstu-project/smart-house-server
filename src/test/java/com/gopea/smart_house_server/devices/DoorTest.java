package com.gopea.smart_house_server.devices;

import com.gopea.smart_house_server.common.Helpers;
import com.gopea.smart_house_server.configs.StatusCode;
import com.gopea.smart_house_server.connectors.BaseTestDeviceConnector;
import com.gopea.smart_house_server.connectors.Connector;
import com.gopea.smart_house_server.examples.StandardDeviceExample;
import io.reactivex.Completable;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.gopea.smart_house_server.common.Helpers.EXTERNAL_STATUS_KEY;
import static com.gopea.smart_house_server.common.Helpers.isInternalStatusOk;
import static com.gopea.smart_house_server.connectors.Connectors.COMMAND_ACTION_KEY;
import static com.gopea.smart_house_server.devices.BaseDevice.*;
import static com.gopea.smart_house_server.examples.StandardDeviceExample.DATA_KEY;
import static com.gopea.smart_house_server.examples.StandardDeviceExample.STATE_KEY;
import static junit.framework.TestCase.assertEquals;

@RunWith(VertxUnitRunner.class)
public class DoorTest {
    private static final String HOST = "host_val";
    private static final int PORT = 80;
    private static final JsonObject BASE_OBJECT = new JsonObject()
            .put("host", HOST)
            .put("port", PORT);


    @Test
    public void testGetType() {
        Door target = new Door(BASE_OBJECT);
        assertEquals(DeviceType.DOOR, target.getType());
    }

    @Test
    public void testGetConnector() {
        TestClass target = new TestClass();

        Connector result = target.get();

        assertEquals(PORT, result.getPort());
        assertEquals(HOST, result.getHost());
    }



    private static class TestClass extends Door {

        public TestClass() {
            super(BASE_OBJECT);
        }

        public Connector get() {
            return super.getConnector(HOST, PORT);
        }
    }
}
