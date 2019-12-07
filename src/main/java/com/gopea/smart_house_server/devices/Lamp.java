package com.gopea.smart_house_server.devices;

import com.gopea.smart_house_server.common.InternalStatus;
import com.gopea.smart_house_server.connectors.BaseDeviceConnector;
import com.gopea.smart_house_server.connectors.Connector;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;

import static com.gopea.smart_house_server.common.Helpers.INTERNAL_STATUS_KEY;

public class Lamp extends BaseDevice {

  public Lamp(JsonObject object) {
    super(object);
  }

  @Override
  protected Connector getConnector(String host, int port) {
    return new BaseDeviceConnector(host, port);
  }

  @Override
  protected JsonObject doToJson() {
    return new JsonObject();
  }

  @Override
  public Single<JsonObject> powerOff() {
    return null;
  }

  @Override
  public Single<JsonObject> reboot() {
    return null;
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
  public Single<JsonObject> execute(JsonObject command) {

    return Single.just(new JsonObject().put(INTERNAL_STATUS_KEY, InternalStatus.OK));
  }


}
