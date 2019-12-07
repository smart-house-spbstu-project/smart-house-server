package com.gopea.smart_house_server.connectors;

import io.reactivex.Single;
import io.vertx.core.json.JsonObject;

public interface Connectible {
  Single<JsonObject> connect();
  Single<JsonObject> disconnect();
}
