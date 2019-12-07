package com.gopea.smart_house_server.connectors;

import io.reactivex.Single;
import io.vertx.core.json.JsonObject;

public abstract class Connector implements Connectible {

  public abstract Single<JsonObject> sendMessage(JsonObject message);

  public abstract Single<JsonObject> getMessage();
}
