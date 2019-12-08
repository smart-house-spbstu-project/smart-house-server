package com.gopea.smart_house_server.data_base;

import com.gopea.smart_house_server.common.InternalStatus;
import com.gopea.smart_house_server.configs.StatusCode;
import com.gopea.smart_house_server.devices.Device;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.gopea.smart_house_server.common.Helpers.EXTERNAL_STATUS_KEY;
import static com.gopea.smart_house_server.common.Helpers.INTERNAL_STATUS_KEY;
import static com.gopea.smart_house_server.common.Helpers.MESSAGE_KEY;

public class RuntimeDeviceStorage implements DeviceStorage {

  private final Map<String, Device> devices;
  private static int id = 0;

  public RuntimeDeviceStorage() {
    devices = new HashMap<>();
  }

  @Override
  public Single<JsonObject> addDevice(Device device) {
    String currentId = String.format("device-%d", id++);
    devices.put(currentId, device);
    return Single.just(new JsonObject()
        .put(INTERNAL_STATUS_KEY, InternalStatus.OK)
        .put(EXTERNAL_STATUS_KEY, StatusCode.CREATED.getStatusCode())
        .put(Storages.ID, currentId));
  }

  @Override
  public Maybe<Device> getDevice(String id) {
    Device device = devices.get(id);
    if (device == null) {
      return Maybe.empty();
    }
    return Maybe.just(device);
  }

  @Override
  public Single<JsonObject> deleteDevice(String id) {
    Device device = devices.get(id);
    if (device == null) {
      return Single.just(
          new JsonObject()
              .put(INTERNAL_STATUS_KEY, InternalStatus.FAILED)
              .put(EXTERNAL_STATUS_KEY, StatusCode.NOT_FOUND.getStatusCode())
              .put(MESSAGE_KEY, String.format("Device with id: %s doesn't exists", id))
      );
    }
    devices.remove(id);
    return Single.just(
        new JsonObject()
            .put(INTERNAL_STATUS_KEY, InternalStatus.OK)
            .put(EXTERNAL_STATUS_KEY, StatusCode.NO_CONTENT.getStatusCode())
    );
  }

  @Override
  public Single<List<Pair<String, Device>>> getDevices() {
    List<Pair<String, Device>> list = new ArrayList<>();
    for (String key : devices.keySet()) {
      list.add(new ImmutablePair<>(key, devices.get(key)));
    }
    return Single.just(list);
  }


}
