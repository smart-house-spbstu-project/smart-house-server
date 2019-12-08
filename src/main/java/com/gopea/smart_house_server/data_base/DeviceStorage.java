package com.gopea.smart_house_server.data_base;

import com.gopea.smart_house_server.devices.Device;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public interface DeviceStorage {
  Single<JsonObject> addDevice(Device device);

  Maybe<Device> getDevice(String id);

  Single<JsonObject> deleteDevice(String id);

  Single<List<Pair<String, Device>>> getDevices();
}
