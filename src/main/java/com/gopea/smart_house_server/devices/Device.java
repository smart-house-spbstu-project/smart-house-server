package com.gopea.smart_house_server.devices;

import com.gopea.smart_house_server.connectors.Connectible;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;

public interface Device extends Connectible {

  Single<JsonObject> powerOff();

  Single<JsonObject> reboot();

  Single<JsonObject> getData();

  Single<JsonObject> getStatus();

  <T extends Command> Single<JsonObject> execute(T command);

  JsonObject toJson();
}
