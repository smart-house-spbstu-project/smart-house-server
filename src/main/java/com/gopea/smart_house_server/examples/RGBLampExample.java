package com.gopea.smart_house_server.examples;

import com.gopea.smart_house_server.common.Helpers;
import com.gopea.smart_house_server.common.InternalStatus;
import com.gopea.smart_house_server.configs.StatusCode;
import com.gopea.smart_house_server.devices.DeviceAction;
import com.gopea.smart_house_server.devices.DeviceType;
import io.vertx.core.json.JsonObject;

import static com.gopea.smart_house_server.common.Helpers.MESSAGE_KEY;
import static com.gopea.smart_house_server.common.Helpers.getEnum;
import static com.gopea.smart_house_server.connectors.Connectors.COMMAND_ACTION_KEY;

public class RGBLampExample extends StandardDeviceExample {
  private String color = "FFFFFF";
  private static final String COLOR_KEY = "color";

  public RGBLampExample(DeviceType type,
      State state, String host, int port) {
    super(type, state, host, port);
  }

  @Override
  public JsonObject getResponse(JsonObject command) {
    if (command.getValue(COLOR_KEY) != null && !(command.getValue(COLOR_KEY) instanceof String)) {
      return Helpers.createResponseJson(InternalStatus.FAILED,
          StatusCode.BAD_REQUEST, new JsonObject().put(MESSAGE_KEY, "invalid color type"));
    }

    JsonObject message = super.getResponse(command);

    if (command.getString(COLOR_KEY) != null) {
      color = command.getString(COLOR_KEY);
      writeMessage(new JsonObject().put(COLOR_KEY, color));
      message.put(COLOR_KEY, color);
    }

    if (DeviceAction.GET_DATA.equals(getEnum(message.getString(COMMAND_ACTION_KEY), DeviceAction.class))) {
      message.getJsonObject(DATA_KEY).put(COLOR_KEY, color);
    }
    return message;
  }
}
