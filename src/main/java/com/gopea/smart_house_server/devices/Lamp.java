package com.gopea.smart_house_server.devices;

import com.gopea.smart_house_server.common.InternalStatus;
import com.gopea.smart_house_server.connectors.BaseTestDeviceConnector;
import com.gopea.smart_house_server.connectors.Connector;
import com.gopea.smart_house_server.examples.StandardDeviceExample;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;

import static com.gopea.smart_house_server.common.Helpers.INTERNAL_STATUS_KEY;
import static com.gopea.smart_house_server.common.Helpers.isInternalStatusOk;
import static com.gopea.smart_house_server.connectors.Connectors.COMMAND_ACTION_KEY;

public class Lamp extends BaseDevice {

  public Lamp(JsonObject object) {
    super(object);
  }

  @Override
  protected Connector getConnector(String host, int port) {
    return new BaseTestDeviceConnector(host, port, new StandardDeviceExample(DeviceType.LAMP, StandardDeviceExample.State.OFF, host, port));
  }

  @Override
  protected JsonObject doToJson() {
    return new JsonObject();
  }

  @Override
  public Single<JsonObject> powerOff() {
    return connector.sendMessage(new JsonObject().put(COMMAND_ACTION_KEY, DeviceAction.POWER_OFF))
        .flatMap(response -> {
          if (isInternalStatusOk(response)) {
            return disconnect();
          }
          return Single.just(response);
        });
  }

  @Override
  public Single<JsonObject> reboot() {
    return connector.sendMessage(new JsonObject().put(COMMAND_ACTION_KEY, DeviceAction.REBOOT))
        .flatMap(response -> {
          if (isInternalStatusOk(response)) {
            return disconnect()
                .flatMap(response1->{
                  if (isInternalStatusOk(response1)){
                    return connect();
                  }
                  return Single.just(response1);
                });
          }
          return Single.just(response);
        });
  }

  @Override
  public Single<JsonObject> getData() {
    return null;
  }

  @Override
  public Single<JsonObject> getStatus() {
    return null;
  }

  @Override
  protected JsonObject validateCommand(JsonObject command) {
    return new JsonObject().put(INTERNAL_STATUS_KEY, InternalStatus.OK);
  }

  @Override
  protected Single<JsonObject> executeCommand(JsonObject command) {
    return connector.sendMessage(command);
  }
}
