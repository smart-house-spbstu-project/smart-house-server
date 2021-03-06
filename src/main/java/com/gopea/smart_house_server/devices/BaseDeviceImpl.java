package com.gopea.smart_house_server.devices;

import com.gopea.smart_house_server.common.InternalStatus;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;

import static com.gopea.smart_house_server.common.Helpers.INTERNAL_STATUS_KEY;
import static com.gopea.smart_house_server.common.Helpers.isInternalStatusOk;
import static com.gopea.smart_house_server.connectors.Connectors.COMMAND_ACTION_KEY;

public abstract class BaseDeviceImpl extends BaseDevice {

  public BaseDeviceImpl(JsonObject object) {
    super(object);
  }


  @Override
  protected JsonObject doToJson() {
    return new JsonObject();
  }

  @Override
  protected Single<JsonObject> getDeviceData() {
    return connector.sendMessage(new JsonObject().put(COMMAND_ACTION_KEY, DeviceAction.GET_DATA))
        .map(data -> {
          data.remove(COMMAND_ACTION_KEY);
          return data;
        });
  }

  @Override
  protected Single<JsonObject> powerOffDevice() {
    return connector.sendMessage(new JsonObject().put(COMMAND_ACTION_KEY, DeviceAction.POWER_OFF))
        .flatMap(response -> {
          if (isInternalStatusOk(response)) {
            return disconnect();
          }
          return Single.just(response);
        });
  }

  @Override
  protected void handleEvent(JsonObject message) { }

  @Override
  public Single<JsonObject> reboot() {
    return connector.sendMessage(new JsonObject().put(COMMAND_ACTION_KEY, DeviceAction.REBOOT))
        .flatMap(response -> {
          if (isInternalStatusOk(response)) {
            return disconnect()
                .flatMap(response1 -> {
                  if (isInternalStatusOk(response1)) {
                    return connect();
                  }
                  return Single.just(response1);
                });
          }
          return Single.just(response);
        });
  }

  @Override
  public Single<JsonObject> getStatus() {
    return connector.sendMessage(new JsonObject().put(COMMAND_ACTION_KEY, DeviceAction.GET_STATUS))
        .map(data -> {
          data.remove(COMMAND_ACTION_KEY);
          return data;
        });
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
