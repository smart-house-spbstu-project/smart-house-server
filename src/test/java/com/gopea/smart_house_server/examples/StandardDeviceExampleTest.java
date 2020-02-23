package com.gopea.smart_house_server.examples;

import com.gopea.smart_house_server.common.Helpers;
import com.gopea.smart_house_server.configs.StatusCode;
import com.gopea.smart_house_server.connectors.BaseTestDeviceConnector;
import com.gopea.smart_house_server.devices.DeviceAction;
import com.gopea.smart_house_server.devices.DeviceType;
import io.vertx.core.json.JsonObject;
import org.junit.Before;
import org.junit.Test;

import static com.gopea.smart_house_server.common.Helpers.EXTERNAL_STATUS_KEY;
import static com.gopea.smart_house_server.connectors.Connectors.COMMAND_ACTION_KEY;
import static com.gopea.smart_house_server.examples.StandardDeviceExample.*;
import static junit.framework.TestCase.*;

public class StandardDeviceExampleTest {
    private StandardDeviceExample target;

    @Before
    public void before() {
        target = new StandardDeviceExample(DeviceType.DOOR, StandardDeviceExample.State.OFF, "test", 80);
    }

    @Test
    public void testSetConnector() {
        StandardDeviceExample value = target.setConnector(new BaseTestDeviceConnector("test", 80, target));
        assertEquals(target, value);
    }

    @Test
    public void testGetResponseWithInvalidState() {
        JsonObject command = new JsonObject()
                .put(STATE_KEY, "eee");
        JsonObject response = target.getResponse(command);

        assertFalse(Helpers.isInternalStatusOk(response));
        assertEquals(StatusCode.UNPROCESSABLE_ENTITY.getStatusCode(), response.getInteger(EXTERNAL_STATUS_KEY).intValue());
    }

    @Test
    public void testGetResponseChangeState() {
        JsonObject command = new JsonObject()
                .put(STATE_KEY, "on");
        JsonObject response = target.getResponse(command);

        assertTrue(Helpers.isInternalStatusOk(response));
        assertEquals(StatusCode.SUCCESS.getStatusCode(), response.getInteger(EXTERNAL_STATUS_KEY).intValue());
    }

    @Test
    public void testGetResponseConnect() {
        JsonObject command = new JsonObject()
                .put(COMMAND_ACTION_KEY, DeviceAction.CONNECT);
        JsonObject response = target.getResponse(command);

        assertTrue(Helpers.isInternalStatusOk(response));
        assertEquals(StatusCode.SUCCESS.getStatusCode(), response.getInteger(EXTERNAL_STATUS_KEY).intValue());
        assertTrue(response.containsKey(COMMAND_ACTION_KEY));
        assertEquals("Connected".toLowerCase(), response.getString(COMMAND_ACTION_KEY).toLowerCase());
    }

    @Test
    public void testGetResponseDisconnect() {
        JsonObject command = new JsonObject()
                .put(COMMAND_ACTION_KEY, DeviceAction.DISCONNECT);
        JsonObject response = target.getResponse(command);

        assertTrue(Helpers.isInternalStatusOk(response));
        assertEquals(StatusCode.SUCCESS.getStatusCode(), response.getInteger(EXTERNAL_STATUS_KEY).intValue());
        assertTrue(response.containsKey(COMMAND_ACTION_KEY));
        assertEquals("DISCONNECTed".toLowerCase(), response.getString(COMMAND_ACTION_KEY).toLowerCase());
    }

    @Test
    public void testGetResponseGetData() {
        JsonObject command = new JsonObject()
                .put(COMMAND_ACTION_KEY, DeviceAction.GET_DATA);
        JsonObject response = target.getResponse(command);

        assertTrue(Helpers.isInternalStatusOk(response));
        assertEquals(StatusCode.SUCCESS.getStatusCode(), response.getInteger(EXTERNAL_STATUS_KEY).intValue());
        assertTrue(response.containsKey(COMMAND_ACTION_KEY));
        assertEquals(DeviceAction.GET_DATA.name().toLowerCase(), response.getString(COMMAND_ACTION_KEY).toLowerCase());
        assertTrue(response.containsKey(DATA_KEY));
    }

    @Test
    public void testGetResponseGetStatus() {
        JsonObject command = new JsonObject()
                .put(COMMAND_ACTION_KEY, DeviceAction.GET_STATUS);
        JsonObject response = target.getResponse(command);

        assertTrue(Helpers.isInternalStatusOk(response));
        assertEquals(StatusCode.SUCCESS.getStatusCode(), response.getInteger(EXTERNAL_STATUS_KEY).intValue());
        assertTrue(response.containsKey(COMMAND_ACTION_KEY));
        assertEquals(DeviceAction.GET_STATUS.name().toLowerCase(), response.getString(COMMAND_ACTION_KEY).toLowerCase());
        assertTrue(response.containsKey(STATUS_KEY));
    }

    @Test
    public void testGetResponseWithoutaction() {
        JsonObject command = new JsonObject();
        JsonObject response = target.getResponse(command);

        assertTrue(Helpers.isInternalStatusOk(response));
        assertEquals(StatusCode.SUCCESS.getStatusCode(), response.getInteger(EXTERNAL_STATUS_KEY).intValue());
    }
}
