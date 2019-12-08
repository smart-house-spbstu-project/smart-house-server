package com.gopea.smart_house_server.connectors;

import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;

public abstract class Connector implements Connectible {

  protected final String host;
  protected final int port;
  protected Connector(String host, int port){
    this.host = host;
    this.port = port;
  }

  /**
   * Getter for the port.
   *
   * @return The port.
   */
  public int getPort() {
    return port;
  }

  /**
   * Getter for the host.
   *
   * @return The host.
   */
  public String getHost() {
    return host;
  }

  public abstract Single<JsonObject> sendMessage(JsonObject message);

  public abstract Maybe<JsonObject> getMessage();

  public abstract Single<Boolean> hasMessage();

  public abstract Single<Boolean> isConnected();
}
