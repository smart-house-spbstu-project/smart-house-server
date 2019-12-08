package com.gopea.smart_house_server.devices;

import com.gopea.smart_house_server.connectors.BaseTestDeviceConnector;
import com.gopea.smart_house_server.connectors.Connector;
import com.gopea.smart_house_server.examples.StandardDeviceExample;
import io.vertx.core.json.JsonObject;

public class RGBLamp extends BaseDeviceImpl {
  public RGBLamp(JsonObject object) {
    super(object);
  }

  @Override
  protected Connector getConnector(String host, int port) {
    return new BaseTestDeviceConnector(host, port, new StandardDeviceExample(DeviceType.RGB_LAMP, StandardDeviceExample.State.OFF, host, port));
  }

  @Override
  public DeviceType getType() {
    return DeviceType.RGB_LAMP;
  }
}
