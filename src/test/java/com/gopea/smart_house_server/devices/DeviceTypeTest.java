package com.gopea.smart_house_server.devices;

import com.gopea.smart_house_server.connectors.Connector;
import io.vertx.core.json.JsonObject;
import org.junit.Test;

import java.util.Arrays;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;

public class DeviceTypeTest {
    @Test
    public void testGetEnumViaClass() {
        Arrays.stream(DeviceType.values())
                .forEach(deviceType -> {
                    DeviceType getType = DeviceType.getEnum(deviceType.getClazz());
                    assertEquals(deviceType, getType);
                });
    }

    @Test
    public void testGetEnumViaClassNull() {
        BaseDeviceImpl test = new BaseDeviceImpl(new JsonObject().put("host", "host").put("port", 0)) {

            @Override
            public DeviceType getType() {
                return null;
            }

            @Override
            protected Connector getConnector(String host, int port) {
                return null;
            }
        };
        assertNull(DeviceType.getEnum(test.getClass()));
    }

    @Test
    public void testGetEnumViaName() {
        Arrays.stream(DeviceType.values())
                .forEach(deviceType -> {
                    DeviceType getType = DeviceType.getEnum(deviceType.name().toLowerCase());
                    assertEquals(deviceType, getType);
                });
        Arrays.stream(DeviceType.values())
                .forEach(deviceType -> {
                    DeviceType getType = DeviceType.getEnum(deviceType.name().toUpperCase());
                    assertEquals(deviceType, getType);
                });
    }

    @Test
    public void testGetEnumViaNameNull() {
        assertNull(DeviceType.getEnum((String) null));
        assertNull(DeviceType.getEnum(""));
    }
}
