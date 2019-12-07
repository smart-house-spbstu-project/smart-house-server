package com.gopea.smart_house_server.devices;

import com.gopea.smart_house_server.common.Helpers;
import com.gopea.smart_house_server.common.InternalStatus;
import com.gopea.smart_house_server.connectors.Connector;
import io.reactivex.Single;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.lang.reflect.Constructor;

import static com.gopea.smart_house_server.common.Helpers.INTERNAL_STATUS_KEY;

public abstract class BaseDevice implements Device {

  public static final String HOST_KEY = "host";
  public static final String PORT_KEY = "port";
  public static final String UPDATE_STATE_TIME_KEY = "update_state_time";
  public static final String STATE_KEY = "state";

  public static BaseDevice getInstance(Class<? extends BaseDevice> clazz, JsonObject object) throws Exception {
    Constructor<? extends BaseDevice> constructor = clazz.getConstructor(JsonObject.class);
    return constructor.newInstance(object);
  }

  protected final String host;
  protected final int port;
  protected final Connector connector;

  private final int maxMetrics = 100;

  private DeviceState state;
  private int updateTime;
  private JsonArray metrics;

  protected BaseDevice(JsonObject json) {
    host = json.getString(HOST_KEY);
    port = json.getInteger(PORT_KEY);
    updateTime = json.getInteger(UPDATE_STATE_TIME_KEY, -1);
    connector = getConnector(host, port);
    state = DeviceState.DISCONNECTED;
    metrics = new JsonArray();
  }

  @Override
  public Single<JsonObject> connect() {
    return connector.connect()
        .map(response -> {
          if (Helpers.isInternalStatusOk(response)) {
            state = DeviceState.CONNECTED;
          }
          return response;
        });
  }

  @Override
  public Single<JsonObject> disconnect() {
    return connector.disconnect()
        .map(response -> {
          state = Helpers.isInternalStatusOk(response) ? DeviceState.DISCONNECTED : DeviceState.ERROR;
          return response;
        });
  }

  protected abstract Connector getConnector(String host, int port);

  /**
   * Getter for the state.
   *
   * @return The state.
   */
  public DeviceState getState() {
    return state;
  }

  /**
   * Getter for the updateTime.
   *
   * @return The updateTime.
   */
  public int getUpdateTime() {
    return updateTime;
  }

  /**
   * Setter for the updateTime.
   *
   * @param updateTime The updateTime.
   * @return This, so the API can be used fluently.
   */
  public BaseDevice setUpdateTime(int updateTime) {
    this.updateTime = updateTime;
    return this;
  }

  @Override
  public JsonObject toJson() {
    JsonObject object = new JsonObject();
    object.put(HOST_KEY, host);
    object.put(PORT_KEY, port);
    object.put(UPDATE_STATE_TIME_KEY, updateTime);
    object.put(STATE_KEY, state.toString().toLowerCase());
    return object.mergeIn(doToJson());
  }

  @Override
  public Single<JsonObject> update(JsonObject object) {
    Integer updateTime = object.getInteger(UPDATE_STATE_TIME_KEY);
    setUpdateTime(updateTime);
    return Single.just(
        new JsonObject().put(INTERNAL_STATUS_KEY, InternalStatus.OK)
    );
  }

  protected abstract JsonObject doToJson();
}
