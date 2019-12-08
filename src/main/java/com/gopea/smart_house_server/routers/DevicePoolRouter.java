package com.gopea.smart_house_server.routers;

import com.gopea.smart_house_server.configs.RoutConfiguration;
import com.gopea.smart_house_server.configs.StatusCode;
import com.gopea.smart_house_server.devices.Device;
import com.gopea.smart_house_server.devices.DevicePool;
import com.gopea.smart_house_server.devices.DeviceType;
import io.reactivex.Completable;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.web.Router;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.gopea.smart_house_server.common.Helpers.MESSAGE_KEY;
import static com.gopea.smart_house_server.common.Helpers.checkAdminRights;
import static com.gopea.smart_house_server.common.Helpers.getBody;
import static com.gopea.smart_house_server.common.Helpers.handleError;
import static com.gopea.smart_house_server.common.Helpers.isInternalStatusOk;
import static com.gopea.smart_house_server.common.Helpers.makeErrorResponse;
import static com.gopea.smart_house_server.common.Helpers.makeErrorRestResponse;
import static com.gopea.smart_house_server.common.Helpers.makeRestResponseFromResponse;
import static com.gopea.smart_house_server.data_base.Storages.DEVICE_STORAGE;
import static com.gopea.smart_house_server.data_base.Storages.ID;
import static com.gopea.smart_house_server.devices.Devices.DEVICE_PROPERTIES_KEY;
import static com.gopea.smart_house_server.devices.Devices.DEVICE_TYPE_KEY;

public class DevicePoolRouter implements Routable {
  private static final String PATH = RoutConfiguration.REST_PREFIX + "/device_pool";
  private static final String DEVICES_KEY = "devices";

  @Override
  public Router loadRouter(Vertx vertx) {
    Router router = Router.router(vertx);
    router.route(HttpMethod.POST, PATH).handler(ctx -> {
      JsonObject body = getBody(ctx);
      if (body == null || body.isEmpty()) {
        makeErrorRestResponse(ctx, StatusCode.BAD_REQUEST, "Body are required for this request");
        return;
      }
      String deviceTypeParam = body.getString(DEVICE_TYPE_KEY);

      if (StringUtils.isBlank(deviceTypeParam)) {
        makeErrorRestResponse(ctx, StatusCode.BAD_REQUEST, String.format("%s field is missed", DEVICE_TYPE_KEY));
        return;
      }
      DeviceType deviceType = DeviceType.getEnum(deviceTypeParam);
      if (deviceType == null) {
        makeErrorRestResponse(ctx, StatusCode.BAD_REQUEST, String.format("%s is unsupported %s", deviceTypeParam, DEVICE_TYPE_KEY));
        return;
      }

      JsonArray devices = body.getJsonArray(DEVICES_KEY);

      if (devices == null) {
        makeErrorRestResponse(ctx, StatusCode.BAD_REQUEST, String.format("%s field is required", DEVICES_KEY));
        return;
      }

      DEVICE_STORAGE.addDevice(new DevicePool(new ArrayList<>(), deviceType))
          .flatMapCompletable(response -> {
            if (isInternalStatusOk(response)) {
              makeRestResponseFromResponse(ctx, response, new JsonObject().put(ID, response.getString(ID)));
            } else {
              makeErrorResponse(ctx, response);
            }
            return Completable.complete();
          })
          .doOnError(err -> handleError(ctx, err))
          .subscribe();

    });

    router.route(HttpMethod.DELETE, PATH + "/:id").handler(ctx -> {
      if (!checkAdminRights(ctx)) {
        return;
      }

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


    //router.route(HttpMethod.POST, PATH + "/:id/add").handler(ctx->{});
    //router.route(HttpMethod.POST, PATH + "/:id/remove").handler(ctx->{});
    return router;
  }
}
