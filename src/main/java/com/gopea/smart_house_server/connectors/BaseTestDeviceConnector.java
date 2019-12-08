package com.gopea.smart_house_server.connectors;

import com.gopea.smart_house_server.common.InternalStatus;
import com.gopea.smart_house_server.configs.StatusCode;
import com.gopea.smart_house_server.devices.DeviceAction;
import com.gopea.smart_house_server.examples.StandardDeviceExample;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;

import java.util.ArrayDeque;
import java.util.Deque;

import static com.gopea.smart_house_server.common.Helpers.EXTERNAL_STATUS_KEY;
import static com.gopea.smart_house_server.common.Helpers.INTERNAL_STATUS_KEY;
import static com.gopea.smart_house_server.common.Helpers.MESSAGE_KEY;
import static com.gopea.smart_house_server.common.Helpers.getEnum;
import static com.gopea.smart_house_server.connectors.Connectors.COMMAND_ACTION_KEY;

public class BaseTestDeviceConnector extends Connector {

  public Deque<JsonObject> messagesFromDevice = new ArrayDeque<>();
  public static final int MAX_REPEAT = 3;

  private boolean isConnected = false;

  private final StandardDeviceExample example;

  public BaseTestDeviceConnector(String host, int port, StandardDeviceExample example) {
    super(host, port);
    this.example = example;
  }

  @Override
  public Single<JsonObject> sendMessage(JsonObject message) {
    JsonObject errorResponse = new JsonObject()
        .put(INTERNAL_STATUS_KEY, InternalStatus.FAILED)
        .put(EXTERNAL_STATUS_KEY, StatusCode.UNAVAILABLE.getStatusCode())
        .put(MESSAGE_KEY, "Device is unavailable");
    return isConnected()
        .flatMap(isCon -> {
          if (isCon) {
            return Single.just(example.getResponse(message))
                .retry(MAX_REPEAT)
                .onErrorResumeNext(ign -> Single.just(errorResponse));
          }
          return Single.just(errorResponse);
        });
  }

  @Override
  public Maybe<JsonObject> getMessage() {
    return isConnected()
        .flatMapMaybe(isCon -> {
          if (isCon) {
            return hasMessage()
                .flatMapMaybe(has -> has ? Maybe.just(messagesFromDevice.poll()) : Maybe.empty());
          }
          return Maybe.just(new JsonObject()
              .put(INTERNAL_STATUS_KEY, InternalStatus.FAILED)
              .put(EXTERNAL_STATUS_KEY, StatusCode.UNAVAILABLE.getStatusCode())
              .put(MESSAGE_KEY, "Device is disconnected"));
        });

  }

  @Override
  public Single<Boolean> hasMessage() {
    return Single.just(!messagesFromDevice.isEmpty());
  }

  @Override
  public Single<Boolean> isConnected() {
    return Single.just(isConnected);
  }

  @Override
  public Single<JsonObject> connect() {
    JsonObject object = example.getResponse(new JsonObject().put(COMMAND_ACTION_KEY, DeviceAction.CONNECT.toString().toLowerCase()));
    ConnectionState connectionState = getEnum(object.getString(COMMAND_ACTION_KEY), ConnectionState.class);
    if (connectionState == null || connectionState.equals(ConnectionState.DISCONNECTED)) {

      return Single.just(
          new JsonObject()
              .put(INTERNAL_STATUS_KEY, InternalStatus.FAILED)
              .mergeIn(object)
      );
    }

    isConnected = true;
    return Single.just(
        new JsonObject()
            .put(INTERNAL_STATUS_KEY, InternalStatus.OK)
            .mergeIn(object)
    );
  }

  @Override
  public Single<JsonObject> disconnect() {
    JsonObject response = example.getResponse(new JsonObject().put(COMMAND_ACTION_KEY, DeviceAction.DISCONNECT.toString().toLowerCase()));
    ConnectionState connectionState = getEnum(response.getString(COMMAND_ACTION_KEY), ConnectionState.class);
    if (connectionState == null || connectionState.equals(ConnectionState.CONNECTED)) {
      return Single.just(
          new JsonObject()
              .put(INTERNAL_STATUS_KEY, InternalStatus.FAILED)
              .mergeIn(response)
      );
    }
    isConnected = false;
    return Single.just(
        new JsonObject()
            .put(INTERNAL_STATUS_KEY, InternalStatus.OK)
            .mergeIn(response)
    );
  }

  public enum ConnectionState {
    CONNECTED,
    DISCONNECTED;
  }
}
