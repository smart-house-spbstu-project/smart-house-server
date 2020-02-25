package com.gopea.smart_house_server.devices;

import com.gopea.smart_house_server.common.Helpers;
import com.gopea.smart_house_server.common.InternalStatus;
import com.gopea.smart_house_server.configs.StatusCode;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;


import java.util.List;

import static com.gopea.smart_house_server.common.Helpers.EXTERNAL_STATUS_KEY;
import static com.gopea.smart_house_server.common.Helpers.INTERNAL_STATUS_KEY;
import static com.gopea.smart_house_server.common.Helpers.MESSAGE_KEY;
import static com.gopea.smart_house_server.common.Helpers.createResponseJson;
import static com.gopea.smart_house_server.common.Helpers.isInternalStatusOk;
import static com.gopea.smart_house_server.data_base.Storages.DEVICE_STORAGE;
import static com.gopea.smart_house_server.data_base.Storages.ID;
import static com.gopea.smart_house_server.devices.BaseDevice.UPDATE_TIME_KEY;
import static com.gopea.smart_house_server.devices.Devices.DEVICE_TYPE_KEY;

public class DevicePool implements Device {
  private final List<Pair<String, Device>> devices;
  private final DeviceType type;
  public static final String RESPONSES_KEY = "responses";
  public static final String ADD_KEY = "add";
  public static final String REMOVE_KEY = "remove";

  public DevicePool(List<Pair<String, Device>> devices, DeviceType type) {
    this.devices = devices;
    this.type = type;
    for (Pair<String, Device> pair : devices) {
      if (!pair.getRight().getType().equals(type)) {
        throw new RuntimeException(String.format("Types %s and %s are different, You should add only %s", type.toString().toLowerCase(),
            pair.getRight().getType().toString().toLowerCase(), type.toString().toLowerCase()));
      }
    }
  }

  @Override
  public Single<JsonObject> powerOff() {
    return cloneCommand(Device::powerOff);
  }

  @Override
  public Single<JsonObject> reboot() {
    return cloneCommand(Device::reboot);
  }

  @Override
  public Single<JsonObject> getData() {
    return cloneCommand(Device::getData);
  }

  @Override
  public Single<JsonObject> getStatus() {
    return cloneCommand(Device::getStatus);
  }

  @Override
  public Single<JsonObject> execute(JsonObject command) {
    return cloneCommand(device -> device.execute(command));
  }

  @Override
  public JsonObject toJson() {
    JsonArray array = new JsonArray();
    for (Pair<String, Device> pair : devices) {
      array.add(pair.getLeft());
    }

    return new JsonObject()
        .put("devices", array)
        .put(DEVICE_TYPE_KEY, type.toString().toLowerCase());
  }

  @Override
  public Single<JsonObject> update(JsonObject object) {
    Single<JsonObject> objectSingle = null;
    if (object.containsKey(UPDATE_TIME_KEY)) {
      objectSingle = cloneCommand(device -> device.update(object));
    }

    if (object.isEmpty()) {
      return Single.just(createResponseJson(InternalStatus.FAILED,
          StatusCode.BAD_REQUEST,
          new JsonObject().put(MESSAGE_KEY, "Empty body is invalid")));
    }
    JsonArray removeArray = object.getJsonArray(REMOVE_KEY, new JsonArray());
    JsonArray addArray = object.getJsonArray(ADD_KEY, new JsonArray());

    JsonObject resp1 = checkTypeAndExists(addArray);
    if (!isInternalStatusOk(resp1)) {
      return Single.just(resp1);
    }

    resp1 = checkTypeAndExists(removeArray);
    if (!isInternalStatusOk(resp1)) {
      return Single.just(resp1);
    }

    resp1 = isRemoveDeviceInPool(removeArray);
    if (!isInternalStatusOk(resp1)) {
      return Single.just(resp1);
    }

    removeDevices(removeArray);

    if (objectSingle == null) {
      objectSingle = Single.just(new JsonObject().put(INTERNAL_STATUS_KEY, InternalStatus.OK));
    }
    return
        objectSingle
            .flatMap(resp -> {
              if (!isInternalStatusOk(resp)) {
                return Single.just(resp);
              }
              return addDevices(addArray)
                  .map(response -> {
                    if (!isInternalStatusOk(response)) {
                      return response;
                    }
                    JsonObject object1 = toJson();
                    object1.remove(DEVICE_TYPE_KEY);
                    object1.put(INTERNAL_STATUS_KEY, InternalStatus.OK);
                    object1.put(EXTERNAL_STATUS_KEY, StatusCode.SUCCESS.getStatusCode());
                    return object1;
                  });
            });

  }

  @Override
  public Single<JsonArray> getMetrics() {
    return Flowable.fromIterable(devices)
        .flatMapSingle(pair -> pair.getRight().getMetrics()
            .map(response -> new JsonObject()
                .put(ID, pair.getLeft())
                .put("metrics", response)))
        .collectInto(new JsonArray(), JsonArray::add);

  }

  @Override
  public Single<DeviceState> getState() {
    return Single.just(DeviceState.CONNECTED);
  }

  @Override
  public DeviceType getType() {
    return type;
  }

  @Override
  public Single<JsonObject> connect() {
    return cloneCommand(Device::connect);
  }

  @Override
  public Single<JsonObject> disconnect() {
    return cloneCommand(Device::disconnect);
  }


  private Single<JsonObject> cloneCommand(Command command) {
    return Flowable.fromIterable(devices)
        .flatMapSingle(pair -> command.execute(pair.getRight()).map(response -> response.put(ID, pair.getLeft())))
        .collectInto(new JsonArray(), JsonArray::add)
        .map(array -> {

          InternalStatus status = InternalStatus.OK;
          for (int i = 0; i < array.size(); i++) {
            JsonObject object = array.getJsonObject(i);
            if (!isInternalStatusOk(object)) {
              status = InternalStatus.FAILED;
            }
            object.remove(INTERNAL_STATUS_KEY);
            object.remove(EXTERNAL_STATUS_KEY);
          }
          JsonObject response = new JsonObject()
              .put(INTERNAL_STATUS_KEY, status)
              .put(RESPONSES_KEY, array);

          if (status.isOk) {
            return response.put(EXTERNAL_STATUS_KEY, StatusCode.SUCCESS.getStatusCode());
          }
          return response.put(EXTERNAL_STATUS_KEY, StatusCode.ERROR.getStatusCode());
        });
  }

  private interface Command {
    Single<JsonObject> execute(Device device);
  }

  private JsonObject checkTypeAndExists(JsonArray array) {
    for (int i = 0; i < array.size(); i++) {
      if (!(array.getValue(i) instanceof String)) {
        return createResponseJson(InternalStatus.FAILED,
            StatusCode.BAD_REQUEST,
            new JsonObject().put(MESSAGE_KEY, "id should have String type"));
      }
      String id = array.getString(i);
      JsonObject response = new JsonObject();
      DEVICE_STORAGE.getDevice(id)
          .map(Device::getType)
          .map(deviceType -> {
            if (!deviceType.equals(type)) {
              return createResponseJson(InternalStatus.FAILED,
                  StatusCode.UNPROCESSABLE_ENTITY,
                  new JsonObject().put(MESSAGE_KEY,
                      String.format("You can add only devices with type %s to this pool", type.toString().toLowerCase())));
            }
            return new JsonObject().put(INTERNAL_STATUS_KEY, InternalStatus.OK);
          })
          .doOnSuccess(response::mergeIn)
          .switchIfEmpty(Single.fromCallable(() ->
              response.mergeIn(createResponseJson(InternalStatus.FAILED, StatusCode.UNPROCESSABLE_ENTITY,
                  new JsonObject().put(MESSAGE_KEY, String.format("Device with id: %s doesn't exists", id))))
          ))
          .subscribe();
      if (!isInternalStatusOk(response)) {
        return response;
      }
    }
    return new JsonObject().put(INTERNAL_STATUS_KEY, InternalStatus.OK);
  }

  private JsonObject isRemoveDeviceInPool(JsonArray removeIds) {
    for (int i = 0; i < removeIds.size(); i++) {
      String id = removeIds.getString(i);
      for (Pair<String, Device> pair : devices) {
        if (id.equals(pair.getLeft())) {
          break;
        }
      }
      return createResponseJson(InternalStatus.FAILED, StatusCode.UNPROCESSABLE_ENTITY,
          new JsonObject().put(MESSAGE_KEY, String.format("Pool doesn't have device with id: %s", id)));
    }
    return new JsonObject().put(INTERNAL_STATUS_KEY, InternalStatus.OK);
  }

  private void removeDevices(JsonArray array) {
    for (int i = 0; i < array.size(); i++) {
      String id = array.getString(i);
      for (Pair<String, Device> pair : devices) {
        if (pair.getLeft().equals(id)) {
          devices.remove(pair);
          break;
        }
      }
    }
  }

  //TODO: Improve it
  private Single<JsonObject> addDevices(JsonArray array) {
    for (int i = 0; i < array.size(); i++) {
      String id = array.getString(i);
      JsonObject object = new JsonObject();
      DEVICE_STORAGE.getDevice(id)
          .map(device -> {
            devices.add(new ImmutablePair<String, Device>(id, device));
            return new JsonObject().put(INTERNAL_STATUS_KEY, InternalStatus.OK);
          })
          .doOnSuccess(object::mergeIn)
          .switchIfEmpty(Maybe.defer(() -> {
            object.mergeIn(
                createResponseJson(InternalStatus.FAILED,
                    StatusCode.NOT_FOUND,
                    new JsonObject().put(MESSAGE_KEY, "Not found device with id " + id)));

            return Maybe.empty();
          }))
          .subscribe();
      if (!isInternalStatusOk(object)) {
        return Single.just(object);
      }
    }
    return Single.just(new JsonObject().put(INTERNAL_STATUS_KEY, InternalStatus.OK));
  }
}
