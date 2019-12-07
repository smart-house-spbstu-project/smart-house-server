package com.gopea.smart_house_server.connectors;

import com.gopea.smart_house_server.common.InternalStatus;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;

import static com.gopea.smart_house_server.common.Helpers.INTERNAL_STATUS_KEY;

public class BaseDeviceConnector extends Connector {



  public BaseDeviceConnector(String host, int port) {

  }

  @Override
  public Single<JsonObject> sendMessage(JsonObject message) {
    return null;
  }

  @Override
  public Single<JsonObject> getMessage() {
    return null;
  }

  @Override
  public Single<JsonObject> connect() {
    return Single.just(
        new JsonObject()
        .put(INTERNAL_STATUS_KEY, InternalStatus.OK)
    );
  }

  @Override
  public Single<JsonObject> disconnect() {
    return null;
  }
}
