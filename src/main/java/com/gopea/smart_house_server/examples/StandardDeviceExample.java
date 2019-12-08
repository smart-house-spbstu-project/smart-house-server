package com.gopea.smart_house_server.examples;

import com.gopea.smart_house_server.common.InternalStatus;
import com.gopea.smart_house_server.configs.StatusCode;
import com.gopea.smart_house_server.devices.DeviceAction;
import com.gopea.smart_house_server.connectors.BaseTestDeviceConnector;
import com.gopea.smart_house_server.devices.DeviceType;
import io.vertx.core.json.JsonObject;

import java.io.IOException;
import java.io.PrintWriter;

import static com.gopea.smart_house_server.common.Helpers.EXTERNAL_STATUS_KEY;
import static com.gopea.smart_house_server.common.Helpers.INTERNAL_STATUS_KEY;
import static com.gopea.smart_house_server.common.Helpers.MESSAGE_KEY;
import static com.gopea.smart_house_server.common.Helpers.getEnum;
import static com.gopea.smart_house_server.connectors.Connectors.COMMAND_ACTION_KEY;


public class StandardDeviceExample {

  public static final String STATE_KEY = "state";

  private static int id = 0;
  private final DeviceType type;
  private BaseTestDeviceConnector connector;
  private State state;
  private final int port;
  private final String host;
  private PrintWriter outputStream;

  public StandardDeviceExample(DeviceType type, State state, String host, int port) {
    this.type = type;
    this.state = state;
    this.port = port;
    this.host = host;

    try {
      outputStream = new PrintWriter(String.format("%s_%s_port_%d_id%d.txt", type.toString(), host, port, id++));
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  /**
   * Setter for the connector.
   *
   * @param connector The connector.
   * @return This, so the API can be used fluently.
   */
  public StandardDeviceExample setConnector(BaseTestDeviceConnector connector) {
    this.connector = connector;
    return this;
  }

  public JsonObject getResponse(JsonObject command) {

    DeviceAction action = getEnum(command.getString(COMMAND_ACTION_KEY), DeviceAction.class);
    State state = getEnum(command.getString(STATE_KEY), State.class);
    JsonObject message = new JsonObject().put(INTERNAL_STATUS_KEY, InternalStatus.FAILED)
        .put(EXTERNAL_STATUS_KEY, StatusCode.UNPROCESSABLE_ENTITY.getStatusCode())
        .put(MESSAGE_KEY, "Invalid request");

    if (state == null && action == null) {
      message = new JsonObject().put(INTERNAL_STATUS_KEY, InternalStatus.FAILED)
          .put(EXTERNAL_STATUS_KEY, StatusCode.UNPROCESSABLE_ENTITY.getStatusCode())
          .put(MESSAGE_KEY, "Invalid new state");
    } else if (state != null) {

      this.state = state;

      message = new JsonObject().put(INTERNAL_STATUS_KEY, InternalStatus.OK)
          .put(STATE_KEY, state.toString().toLowerCase())
          .put(EXTERNAL_STATUS_KEY, StatusCode.SUCCESS.getStatusCode());

    } else {
      message = new JsonObject()
          .put(INTERNAL_STATUS_KEY, InternalStatus.OK)
          .put(EXTERNAL_STATUS_KEY, StatusCode.SUCCESS.getStatusCode())
          .put(COMMAND_ACTION_KEY, action.toString().toLowerCase());
    }

    outputStream.println(message);
    outputStream.flush();

    return message;
  }

  public enum State {
    ON,
    OFF;
  }
}