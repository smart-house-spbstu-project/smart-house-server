package com.gopea.smart_house_server.devices;

import com.gopea.smart_house_server.common.Helpers;
import com.gopea.smart_house_server.common.InternalStatus;
import com.gopea.smart_house_server.configs.StatusCode;
import com.gopea.smart_house_server.connectors.Connector;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.lang.reflect.Constructor;
import java.time.Instant;

import static com.gopea.smart_house_server.common.Helpers.EXTERNAL_STATUS_KEY;
import static com.gopea.smart_house_server.common.Helpers.INTERNAL_STATUS_KEY;
import static com.gopea.smart_house_server.common.Helpers.getEnum;
import static com.gopea.smart_house_server.common.Helpers.isEqualsWithAny;
import static com.gopea.smart_house_server.common.Helpers.isInternalStatusOk;
import static com.gopea.smart_house_server.connectors.Connectors.COMMAND_ACTION_KEY;

public abstract class BaseDevice implements Device {

  public static final String HOST_KEY = "host";
  public static final String PORT_KEY = "port";
  public static final String UPDATE_TIME_KEY = "update_time";
  public static final String STATUS_KEY = "status";
  private long prevTime;

  public static <T extends BaseDevice>T getInstance(Class<T> clazz, JsonObject object) throws Exception {
    Constructor<T> constructor = clazz.getConstructor(JsonObject.class);
    return constructor.newInstance(object);
  }

  protected final String host;
  protected final int port;
  protected final Connector connector;

  private final int maxMetrics = 100;

  private DeviceState state;
  private int updateTime;
  private JsonArray metrics;
  private Thread askThread;

  protected BaseDevice(JsonObject json) {
    host = json.getString(HOST_KEY);
    port = json.getInteger(PORT_KEY);
    updateTime = json.getInteger(UPDATE_TIME_KEY, 0);
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
            if (askThread != null) {
              if (askThread.isAlive()) {
                askThread.interrupt();
              }
            }
            askThread = createAskThread();
            askThread.start();
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
  public Single<DeviceState> getState() {
    return Single.just(state);
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
    object.put(UPDATE_TIME_KEY, updateTime);
    object.put(STATUS_KEY, state.toString().toLowerCase());
    return object.mergeIn(doToJson());
  }

  @Override
  public Single<JsonObject> update(JsonObject object) {
    Integer updateTimeParam = object.getInteger(UPDATE_TIME_KEY);
    setUpdateTime(updateTimeParam);
    return Single.just(
        new JsonObject()
            .put(INTERNAL_STATUS_KEY, InternalStatus.OK)
            .put(EXTERNAL_STATUS_KEY, StatusCode.SUCCESS.getStatusCode())
            .put(UPDATE_TIME_KEY, updateTime)
    );
  }

  @Override
  public Single<JsonObject> execute(JsonObject command) {
    JsonObject validation = validateCommand(command);
    if (!isInternalStatusOk(validation)) {
      return Single.just(validation);
    }
    return executeCommand(command);
  }

  @Override
  public Single<JsonArray> getMetrics() {
    return Single.just(metrics);
  }

  @Override
  public Single<JsonObject> getData() {
    return getDeviceData()
        .map(data -> {
          if (!isInternalStatusOk(data)) {
            state = DeviceState.DISCONNECTED;
            if (DeviceState.ERROR.equals(getEnum(data.getString(STATUS_KEY), DeviceState.class))) {
              state = DeviceState.ERROR;
            }
          }
          addMetric(data);
          return data;
        });
  }

  @Override
  public Single<JsonObject> powerOff() {
    return powerOffDevice()
        .doOnSuccess(response -> {
          if (isInternalStatusOk(response)) {
            state = DeviceState.SWITCHED_OFF;
          }
        });
  }

  protected abstract Single<JsonObject> executeCommand(JsonObject command);

  protected abstract JsonObject validateCommand(JsonObject command);

  protected abstract JsonObject doToJson();

  protected abstract Single<JsonObject> getDeviceData();

  protected abstract Single<JsonObject> powerOffDevice();

  protected abstract void handleEvent(JsonObject message);

  private Thread createAskThread() {
    return new Thread(() ->
        connector
            .isConnected()
            .flatMapMaybe(isConnected -> {
              if (!isConnected) {
                state = DeviceState.DISCONNECTED;
                return Maybe.empty();
              }
              return connector.getMessage();
            })
            .map(this::handleMessage)
            .switchIfEmpty(Single.just(new JsonObject()))
            .flatMapCompletable(ign -> {
              if (updateTime > 0 && (System.currentTimeMillis() - prevTime >= updateTime * 1000)) {
                return getData()
                    .flatMapMaybe(Maybe::just)
                    .flatMapCompletable(ign1 -> {
                      prevTime = System.currentTimeMillis();
                      return Completable.complete();
                    });
              }
              return Completable.complete();
            })
            .repeatUntil(() -> isEqualsWithAny(state, DeviceState.DISCONNECTED, DeviceState.ERROR, DeviceState.SWITCHED_OFF))
            .subscribe()
    );
  }

  private JsonObject handleMessage(JsonObject message) {
    if (isInternalStatusOk(message)) {
      state = DeviceState.CONNECTED;
      DeviceAction action = getEnum(message.getString(COMMAND_ACTION_KEY), DeviceAction.class);
      if (action == null) {
        return message;
      }
      switch (action) {
        case GET_DATA:
          addMetric(message);
          break;
      }
    } else {
      state = DeviceState.ERROR;
    }
    return message;
  }

  private void addMetric(JsonObject message) {
    JsonObject data = message.copy();
    data.remove(INTERNAL_STATUS_KEY);
    data.remove(EXTERNAL_STATUS_KEY);
    data.put("time", Instant.now().toString());
    metrics.add(data);
    stripMetrics();
  }

  private void stripMetrics() {
    if (metrics.size() > maxMetrics) {
      while (metrics.size() > maxMetrics) {
        metrics.remove(0);
      }
    }
  }

}
