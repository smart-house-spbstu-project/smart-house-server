package com.gopea.smart_house_server.devices;

import com.gopea.smart_house_server.connectors.Connectible;
import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface Device extends Connectible {

  Single<JsonObject> powerOff();

  Single<JsonObject> reboot();

  Single<JsonObject> getData();

  Single<JsonObject> getStatus();

  Single<JsonObject> execute(JsonObject command);

  JsonObject toJson();

  Single<JsonObject> update(JsonObject object);

  Single<JsonArray> getMetrics();

  Single<DeviceState> getState();

  DeviceType getType();
}
