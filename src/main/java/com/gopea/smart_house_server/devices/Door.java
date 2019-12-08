package com.gopea.smart_house_server.devices;

import com.gopea.smart_house_server.connectors.BaseTestDeviceConnector;
import com.gopea.smart_house_server.connectors.Connector;
import com.gopea.smart_house_server.examples.StandardDeviceExample;
import io.vertx.core.json.JsonObject;

public class Door extends BaseDeviceImpl {
  public Door(JsonObject object) {
    super(object);
  }

  @Override
  protected Connector getConnector(String host, int port) {
    return new BaseTestDeviceConnector(host, port, new StandardDeviceExample(DeviceType.DOOR, StandardDeviceExample.State.OFF, host, port));
  }

}
