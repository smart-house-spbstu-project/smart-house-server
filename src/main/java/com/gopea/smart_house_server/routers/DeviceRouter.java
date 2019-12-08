package com.gopea.smart_house_server.routers;

import com.gopea.smart_house_server.configs.RoutConfiguration;
import com.gopea.smart_house_server.configs.StatusCode;
import com.gopea.smart_house_server.devices.BaseDevice;
import com.gopea.smart_house_server.devices.Device;
import com.gopea.smart_house_server.devices.DeviceType;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;

import static com.gopea.smart_house_server.common.Helpers.BASE_BAD_REQUEST_MESSAGE;
import static com.gopea.smart_house_server.common.Helpers.handleEmptyCase;
import static com.gopea.smart_house_server.data_base.Storages.ID;
import static com.gopea.smart_house_server.common.Helpers.MESSAGE_KEY;
import static com.gopea.smart_house_server.common.Helpers.checkAdminRights;
import static com.gopea.smart_house_server.data_base.Storages.DEVICE_STORAGE;
import static com.gopea.smart_house_server.common.Helpers.getBody;
import static com.gopea.smart_house_server.common.Helpers.handleError;
import static com.gopea.smart_house_server.common.Helpers.isInternalStatusOk;
import static com.gopea.smart_house_server.common.Helpers.makeErrorResponse;
import static com.gopea.smart_house_server.common.Helpers.makeErrorRestResponse;
import static com.gopea.smart_house_server.common.Helpers.makeRestResponseFromResponse;
import static com.gopea.smart_house_server.devices.BaseDevice.UPDATE_TIME_KEY;

public class DeviceRouter implements Routable {
  private static final String PATH = RoutConfiguration.REST_PREFIX + "/device";
  private static final String DEVICE_TYPE_KEY = "device_type";
  private static final String DEVICE_PROPERTIES_KEY = "device_properties";

  @Override
  public Router loadRouter(Vertx vertx) {
    Router router = Router.router(vertx);

    router.route(HttpMethod.POST, PATH).handler(ctx -> {
      if (!checkAdminRights(ctx)) {
        return;
      }
      JsonObject body = getBody(ctx);
      if (!checkBody(body, ctx)) {
        return;
      }

      DeviceType deviceType = DeviceType.getEnum(body.getString(DEVICE_TYPE_KEY));
      BaseDevice device = null;
      try {
        device = BaseDevice.getInstance(deviceType.getClazz(), body.getJsonObject(DEVICE_PROPERTIES_KEY));
      } catch (Exception e) {
        handleError(ctx, e);
        return;
      }
      final BaseDevice finalDevice = device;
      device.connect()
          .flatMapCompletable(connectorResponse -> {
            if (isInternalStatusOk(connectorResponse)) {
              return DEVICE_STORAGE.addDevice(finalDevice)
                  .flatMapCompletable(response -> {
                    if (isInternalStatusOk(response)) {
                      makeRestResponseFromResponse(ctx, response, new JsonObject().put(ID, response.getString(ID)));
                    } else {
                      makeErrorResponse(ctx, response);
                    }
                    return Completable.complete();
                  });
            }
            makeErrorRestResponse(ctx, StatusCode.UNPROCESSABLE_ENTITY, connectorResponse.getString(MESSAGE_KEY));
            return Completable.complete();
          })
          .doOnError(err -> handleError(ctx, err))
          .subscribe();
    });

    router.route(HttpMethod.POST, PATH + "/:id/execute").handler(ctx -> {
      String id = ctx.request().getParam(ID);
      if (StringUtils.isBlank(id)) {
        makeErrorRestResponse(ctx, StatusCode.BAD_REQUEST, BASE_BAD_REQUEST_MESSAGE);
        return;
      }
      JsonObject body = getBody(ctx);
      if (body == null || body.isEmpty()) {
        makeErrorRestResponse(ctx, StatusCode.BAD_REQUEST, "Body are required for this request");
        return;
      }
      handleDeviceActionWithId(ctx, id, device -> device.execute(body));
    });

    router.route(HttpMethod.POST, PATH + "/:id/power_off").handler(ctx -> {
      if (!checkAdminRights(ctx)) {
        return;
      }
      String id = ctx.request().getParam(ID);
      if (StringUtils.isBlank(id)) {
        makeErrorRestResponse(ctx, StatusCode.BAD_REQUEST, BASE_BAD_REQUEST_MESSAGE);
        return;
      }
      handleDeviceActionWithId(ctx, id, Device::powerOff);
    });

    router.route(HttpMethod.POST, PATH + "/:id/reboot").handler(ctx -> {
      if (!checkAdminRights(ctx)) {
        return;
      }
      String id = ctx.request().getParam(ID);
      if (StringUtils.isBlank(id)) {
        makeErrorRestResponse(ctx, StatusCode.BAD_REQUEST, BASE_BAD_REQUEST_MESSAGE);
        return;
      }
      handleDeviceActionWithId(ctx, id, Device::reboot);
    });

    router.route(HttpMethod.POST, PATH + "/:id/disconnect").handler(ctx -> {
      if (!checkAdminRights(ctx)) {
        return;
      }
      String id = ctx.request().getParam(ID);
      if (StringUtils.isBlank(id)) {
        makeErrorRestResponse(ctx, StatusCode.BAD_REQUEST, BASE_BAD_REQUEST_MESSAGE);
        return;
      }
      handleDeviceActionWithId(ctx, id, Device::disconnect);
    });

    router.route(HttpMethod.POST, PATH + "/:id/connect").handler(ctx -> {
      if (!checkAdminRights(ctx)) {
        return;
      }
      String id = ctx.request().getParam(ID);
      if (StringUtils.isBlank(id)) {
        makeErrorRestResponse(ctx, StatusCode.BAD_REQUEST, BASE_BAD_REQUEST_MESSAGE);
        return;
      }
      handleDeviceActionWithId(ctx, id, Device::connect);
    });

    router.route(HttpMethod.GET, PATH + "/:id").handler(ctx -> {
      String id = ctx.request().getParam(ID);
      if (StringUtils.isBlank(id)) {
        makeErrorRestResponse(ctx, StatusCode.BAD_REQUEST, BASE_BAD_REQUEST_MESSAGE);
        return;
      }
      handleDeviceActionWithId(ctx, id, device -> device
          .getData()
          .flatMap(response -> {
            if (isInternalStatusOk(response)) {
              return device.getStatus()
                  .map(response1 -> {
                    if (isInternalStatusOk(response1)) {
                      JsonObject object = response.copy();
                      return object.mergeIn(response1);
                    }
                    return response1;
                  });
            }
            return Single.just(response);
          })
      );

    });

    router.route(HttpMethod.GET, PATH + "/:id/metrics").handler(ctx -> {
      String id = ctx.request().getParam(ID);
      if (StringUtils.isBlank(id)) {
        makeErrorRestResponse(ctx, StatusCode.BAD_REQUEST, BASE_BAD_REQUEST_MESSAGE);
        return;
      }
      DEVICE_STORAGE.getDevice(id)
          .switchIfEmpty(handleEmptyCase(ctx, DEVICE_STORAGE.getDevice(id)))
          .flatMapCompletable(device ->
              device.getMetrics()
                  .flatMapCompletable(response -> {
                    ctx.response().setStatusCode(StatusCode.SUCCESS.getStatusCode());
                    ctx.response().end(Buffer.newInstance(response.toBuffer()));
                    return Completable.complete();
                  }))
          .doOnError(err -> handleError(ctx, err))
          .subscribe();
    });

    router.route(HttpMethod.GET, PATH).handler(ctx -> {
      String deviceTypeParam = ctx.request().getParam(DEVICE_TYPE_KEY);
      String id = ctx.request().getParam(ID);
      DeviceType deviceType = DeviceType.getEnum(deviceTypeParam);
      if (deviceType == null && StringUtils.isNotEmpty(deviceTypeParam)) {
        ctx.response().setStatusCode(StatusCode.SUCCESS.getStatusCode());
        ctx.response().end(Buffer.newInstance(new JsonArray().toBuffer()));
        return;
      }
      DEVICE_STORAGE.getDevices()
          .flatMap(pairs -> Flowable.fromIterable(pairs)
              .filter(pair -> StringUtils.isEmpty(deviceTypeParam) || deviceType.getClazz().isAssignableFrom(pair.getRight().getClass()))
              .filter(pair -> StringUtils.isEmpty(id) || pair.getLeft().equals(id))
              .collectInto(new ArrayList<Pair<String, ? extends Device>>(), ((arrayList, stringPair) -> arrayList.add(stringPair))))
          .map(list -> {
            JsonArray array = new JsonArray();
            for (Pair<String, ? extends Device> pair : list) {
              JsonObject object = new JsonObject()
                  .put(ID, pair.getLeft())
                  .put(DEVICE_TYPE_KEY, DeviceType.getEnum(pair.getRight().getClass()).toString().toLowerCase())
                  .mergeIn(pair.getRight().toJson());
              array.add(object);
            }
            return array;
          })
          .flatMapCompletable(array -> {
            ctx.response().setStatusCode(StatusCode.SUCCESS.getStatusCode());
            ctx.response().end(Buffer.newInstance(array.toBuffer()));
            return Completable.complete();
          })
          .doOnError(err -> handleError(ctx, err))
          .subscribe();
    });

    router.route(HttpMethod.PATCH, PATH + "/:id").handler(ctx -> {

      if (!checkAdminRights(ctx)) {
        return;
      }
      String id = ctx.request().getParam(ID);
      if (StringUtils.isBlank(id)) {
        makeErrorRestResponse(ctx, StatusCode.BAD_REQUEST, BASE_BAD_REQUEST_MESSAGE);
        return;
      }
      JsonObject body = getBody(ctx);
      if (body == null || body.isEmpty()) {
        makeErrorRestResponse(ctx, StatusCode.BAD_REQUEST, "Body are required for this request");
        return;
      }
      JsonObject deviceProperty = body.getJsonObject(DEVICE_PROPERTIES_KEY);
      if (deviceProperty == null) {
        makeErrorRestResponse(ctx, StatusCode.BAD_REQUEST, String.format("%s is required field", DEVICE_PROPERTIES_KEY));
        return;
      }
      if (deviceProperty.getValue(UPDATE_TIME_KEY) != null) {
        Object updateTimeObj = deviceProperty.getValue(UPDATE_TIME_KEY);
        if (!(updateTimeObj instanceof Integer) || ((int) updateTimeObj) < 0) {
          makeErrorRestResponse(ctx, StatusCode.BAD_REQUEST, String.format("%s should be a non negative int", UPDATE_TIME_KEY));
          return;
        }
      }
      DEVICE_STORAGE.getDevice(id)
          .switchIfEmpty(handleEmptyCase(ctx, DEVICE_STORAGE.getDevice(id)))
          .flatMapCompletable(device ->
              device.update(deviceProperty)
                  .flatMapCompletable(response -> {
                    if (!isInternalStatusOk(response)) {
                      makeErrorResponse(ctx, response);
                    }
                    ctx.response().setStatusCode(StatusCode.SUCCESS.getStatusCode());
                    ctx.response().end();
                    return Completable.complete();
                  }))
          .doOnError(err -> handleError(ctx, err))
          .subscribe();
    });

    router.route(HttpMethod.DELETE, PATH + "/:id").handler(ctx -> {
      String id = ctx.request().getParam("id");
      if (StringUtils.isBlank(id)) {
        makeErrorRestResponse(ctx, StatusCode.BAD_REQUEST, "id param couldn't be blank");
        return;
      }

      DEVICE_STORAGE.deleteDevice(id)
          .flatMapCompletable(response -> {
            if (isInternalStatusOk(response)) {
              makeRestResponseFromResponse(ctx, response);
            } else {
              makeErrorResponse(ctx, response);
            }
            return Completable.complete();
          })
          .doOnError(error -> handleError(ctx, error))
          .subscribe();
    });

    return router;
  }

  private boolean checkBody(JsonObject body, RoutingContext context) {
    if (body == null || body.isEmpty()) {
      makeErrorRestResponse(context, StatusCode.BAD_REQUEST, "Body are required for this request");
      return false;
    }
    String deviceType = body.getString(DEVICE_TYPE_KEY);

    if (StringUtils.isBlank(deviceType)) {
      context.response().setStatusCode(StatusCode.BAD_REQUEST.getStatusCode());
      context.response().end(Buffer.newInstance(
          new JsonObject()
              .put(MESSAGE_KEY, String.format("%s field is missed", DEVICE_TYPE_KEY))
              .toBuffer()
      ));
      return false;
    }

    if (DeviceType.getEnum(deviceType) == null) {
      makeErrorRestResponse(context, StatusCode.BAD_REQUEST, String.format("%s is unsupported %s", deviceType, DEVICE_TYPE_KEY));
      return false;
    }

    JsonObject deviceProp = body.getJsonObject(DEVICE_PROPERTIES_KEY);

    if (deviceProp == null) {
      makeErrorRestResponse(context, StatusCode.BAD_REQUEST, String.format("%s field is required", DEVICE_PROPERTIES_KEY));
      return false;
    }

    if (deviceProp.getValue(UPDATE_TIME_KEY) != null) {
      Object updateTimeObj = deviceProp.getValue(UPDATE_TIME_KEY);
      if (!(updateTimeObj instanceof Integer) || ((int) updateTimeObj) < 0) {
        makeErrorRestResponse(context, StatusCode.BAD_REQUEST, String.format("%s should be a non negative int", UPDATE_TIME_KEY));
        return false;
      }
    }
    return true;
  }

  private void handleDeviceActionWithId(RoutingContext context, String id, DeviceRollback rollback) {
    DEVICE_STORAGE.getDevice(id)
        .switchIfEmpty(handleEmptyCase(context, DEVICE_STORAGE.getDevice(id)))
        .flatMapCompletable(device ->
            rollback.execute(device)
                .flatMapCompletable(response -> {
                  if (!isInternalStatusOk(response)) {
                    makeErrorResponse(context, response);
                    return Completable.complete();
                  }
                  makeRestResponseFromResponse(context, response);
                  return Completable.complete();
                }))
        .doOnError(err -> handleError(context, err))
        .subscribe();
  }

  @FunctionalInterface
  private interface DeviceRollback {
    Single<JsonObject> execute(Device device);
  }
}

