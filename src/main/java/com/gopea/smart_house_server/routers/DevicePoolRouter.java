package com.gopea.smart_house_server.routers;

import com.gopea.smart_house_server.configs.RouteConfiguration;
import com.gopea.smart_house_server.configs.StatusCode;
import com.gopea.smart_house_server.devices.DevicePool;
import com.gopea.smart_house_server.devices.DeviceType;
import io.reactivex.Completable;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;

import static com.gopea.smart_house_server.common.Helpers.checkAdminRights;
import static com.gopea.smart_house_server.common.Helpers.getBody;
import static com.gopea.smart_house_server.common.Helpers.handleError;
import static com.gopea.smart_house_server.common.Helpers.isInternalStatusOk;
import static com.gopea.smart_house_server.common.Helpers.makeErrorResponse;
import static com.gopea.smart_house_server.common.Helpers.makeErrorRestResponse;
import static com.gopea.smart_house_server.common.Helpers.makeRestResponseFromResponse;
import static com.gopea.smart_house_server.data_base.Storages.DEVICE_STORAGE;
import static com.gopea.smart_house_server.data_base.Storages.ID;
import static com.gopea.smart_house_server.devices.Devices.DEVICE_TYPE_KEY;

public class DevicePoolRouter implements Routable {
    private static final String PATH = RouteConfiguration.REST_PREFIX + "/device_pool";
    static final String DEVICES_KEY = "devices";

    @Override
    public Router loadRouter(Vertx vertx) {
        Router router = Router.router(vertx);
        router.route(HttpMethod.POST, PATH).handler(ctx -> handlePostRequest(ctx).subscribe());

        router.route(HttpMethod.DELETE, PATH + "/:id").handler(ctx -> handleDeleteRequest(ctx).subscribe());

        return router;
    }

    Completable handlePostRequest(RoutingContext ctx) {
        JsonObject body = getBody(ctx);
        if (body == null || body.isEmpty()) {
            makeErrorRestResponse(ctx, StatusCode.BAD_REQUEST, "Body are required for this request");
            return Completable.complete();
        }
        String deviceTypeParam = body.getString(DEVICE_TYPE_KEY, "");

        if (StringUtils.isBlank(deviceTypeParam)) {
            makeErrorRestResponse(ctx, StatusCode.BAD_REQUEST, String.format("%s field is missed", DEVICE_TYPE_KEY));
            return Completable.complete();
        }
        DeviceType deviceType = DeviceType.getEnum(deviceTypeParam);
        if (deviceType == null) {
            makeErrorRestResponse(ctx, StatusCode.BAD_REQUEST, String.format("%s is unsupported %s", deviceTypeParam, DEVICE_TYPE_KEY));
            return Completable.complete();
        }

        JsonArray devices = body.getJsonArray(DEVICES_KEY, new JsonArray());

        return DEVICE_STORAGE.addDevice(new DevicePool(new ArrayList<>(), deviceType))
                .flatMapCompletable(response -> {
                    if (isInternalStatusOk(response)) {
                        makeRestResponseFromResponse(ctx, response, new JsonObject().put(ID, response.getString(ID)));
                    } else {
                        makeErrorResponse(ctx, response);
                    }
                    return Completable.complete();
                })
                .doOnError(err -> handleError(ctx, err));
    }

    Completable handleDeleteRequest(RoutingContext ctx) {
        if (!checkAdminRights(ctx)) {
            return Completable.complete();
        }

        String id = ctx.request().getParam("id");
        if (StringUtils.isBlank(id)) {
            makeErrorRestResponse(ctx, StatusCode.BAD_REQUEST, "id param couldn't be blank");
            return Completable.complete();
        }

        return DEVICE_STORAGE.deleteDevice(id)
                .flatMapCompletable(response -> {
                    if (isInternalStatusOk(response)) {
                        makeRestResponseFromResponse(ctx, response);
                    } else {
                        makeErrorResponse(ctx, response);
                    }
                    return Completable.complete();
                })
                .doOnError(error -> handleError(ctx, error));
    }
}
