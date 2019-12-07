package com.gopea.smart_house_server.data_base;

import com.gopea.smart_house_server.devices.Device;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public interface DeviceStorage {
  Single<JsonObject> addDevice(Device device);

  Single<? extends Device> getDevice(String id);

  Single<JsonObject> deleteDevice(String id);

  Single<List<Pair<String, ? extends Device>>> getDevices();
}
