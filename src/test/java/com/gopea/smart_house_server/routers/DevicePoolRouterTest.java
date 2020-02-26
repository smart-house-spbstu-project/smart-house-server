package com.gopea.smart_house_server.routers;

import com.gopea.smart_house_server.configs.StatusCode;
import com.gopea.smart_house_server.data_base.Storages;
import com.gopea.smart_house_server.devices.DeviceType;
import com.gopea.smart_house_server.devices.Lamp;
import com.gopea.smart_house_server.routers.users.UserType;
import io.reactivex.Completable;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.reactivex.core.http.HttpServerResponse;
import io.vertx.reactivex.ext.web.Route;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static com.gopea.smart_house_server.common.Helpers.USER_TYPE_HEADER;
import static com.gopea.smart_house_server.devices.Devices.DEVICE_TYPE_KEY;
import static com.gopea.smart_house_server.routers.DevicePoolRouter.DEVICES_KEY;
import static junit.framework.TestCase.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;


@RunWith(VertxUnitRunner.class)
public class DevicePoolRouterTest {
    private Vertx vertx;
    private static final String HOST = "host_val";
    private static final int PORT = 80;
    private static final JsonObject BASE_OBJECT = new JsonObject()
            .put("host", HOST)
            .put("port", PORT);

    @Before
    public void before() {
        vertx = Vertx.vertx();
    }

    @After
    public void after() {

        vertx.close();
    }

    @Test(timeout = 60_000L)
    public void testLoadRouter() {
        Router target = new DevicePoolRouter().loadRouter(Vertx.vertx());

        List<Route> list = target.getRoutes();
        assertEquals(2, list.size());
    }

    @Test(timeout = 60_000L)
    public void testPostWithEmptyBody(TestContext context) {
        RoutingContext routingContext = createContext();

        final Async async = context.async();
        when(routingContext.getBodyAsJson()).thenReturn(new JsonObject());

        DevicePoolRouter target = new DevicePoolRouter();

        target.handlePostRequest(routingContext)
                .andThen(Completable.fromAction(() -> {
                    verify(routingContext.response()).setStatusCode(StatusCode.BAD_REQUEST.getStatusCode());
                    verify(routingContext.response()).end(any(Buffer.class));
                    async.complete();
                }))
                .subscribe();
    }

    @Test(timeout = 60_000L)
    public void testPostWithoutBody(TestContext context) {
        RoutingContext routingContext = createContext();

        final Async async = context.async();
        when(routingContext.getBodyAsJson()).thenThrow(new RuntimeException());

        DevicePoolRouter target = new DevicePoolRouter();

        target.handlePostRequest(routingContext)
                .andThen(Completable.fromAction(() -> {
                    verify(routingContext).fail(anyInt(), any());
                    async.complete();
                }))
                .subscribe();
    }

    @Test(timeout = 60_000L)
    public void testPostWithoutDeviceType(TestContext context) {
        RoutingContext routingContext = createContext();

        final Async async = context.async();
        when(routingContext.getBodyAsJson()).thenReturn(new JsonObject()
                .put("efefe", "fefef"));

        DevicePoolRouter target = new DevicePoolRouter();

        target.handlePostRequest(routingContext)
                .andThen(Completable.fromAction(() -> {
                    verify(routingContext.response()).setStatusCode(StatusCode.BAD_REQUEST.getStatusCode());
                    verify(routingContext.response()).end(any(Buffer.class));
                    async.complete();
                }))
                .subscribe();
    }

    @Test(timeout = 60_000L)
    public void testPostWithInvalidDeviceType(TestContext context) {
        RoutingContext routingContext = createContext();

        final Async async = context.async();
        when(routingContext.getBodyAsJson()).thenReturn(new JsonObject()
                .put(DEVICE_TYPE_KEY, "fefef"));

        DevicePoolRouter target = new DevicePoolRouter();

        target.handlePostRequest(routingContext)
                .andThen(Completable.fromAction(() -> {
                    verify(routingContext.response()).setStatusCode(StatusCode.BAD_REQUEST.getStatusCode());
                    verify(routingContext.response()).end(any(Buffer.class));
                    async.complete();
                }))
                .subscribe();
    }

    @Test(timeout = 60_000L)
    public void testPostWithIoutDevicesKey(TestContext context) {
        RoutingContext routingContext = createContext();

        final Async async = context.async();
        when(routingContext.getBodyAsJson()).thenReturn(new JsonObject()
                .put(DEVICE_TYPE_KEY, DeviceType.DOOR));

        DevicePoolRouter target = new DevicePoolRouter();

        target.handlePostRequest(routingContext)
                .andThen(Completable.fromAction(() -> {
                    verify(routingContext.response()).setStatusCode(StatusCode.CREATED.getStatusCode());
                    verify(routingContext.response()).end(any(Buffer.class));
                    async.complete();
                }))
                .subscribe();
    }

    @Test(timeout = 60_000L)
    public void testPostSuccess(TestContext context) {
        RoutingContext routingContext = createContext();

        final Async async = context.async();
        when(routingContext.getBodyAsJson()).thenReturn(new JsonObject()
                .put(DEVICE_TYPE_KEY, DeviceType.DOOR)
                .put(DEVICES_KEY, new JsonArray()));

        DevicePoolRouter target = new DevicePoolRouter();

        target.handlePostRequest(routingContext)
                .andThen(Completable.fromAction(() -> {
                    verify(routingContext.response()).setStatusCode(StatusCode.CREATED.getStatusCode());
                    verify(routingContext.response()).end(any(Buffer.class));
                    async.complete();
                }))
                .subscribe();
    }

    @Test(timeout = 60_000L)
    public void testDeleteSuccess(TestContext context) {
        RoutingContext routingContext = createContext();

        final Async async = context.async();


        when(routingContext.request().getHeader(any())).then((invocationOnMock -> {
            if (invocationOnMock.getArgument(0).equals(USER_TYPE_HEADER)) {
                return UserType.ADMIN.toString();
            }
            return "";
        }));

        when(routingContext.request().getParam("id")).thenReturn("device-0");

        DevicePoolRouter target = new DevicePoolRouter();

        Storages.DEVICE_STORAGE.addDevice(new Lamp(BASE_OBJECT))
                .flatMapCompletable(ign -> target.handleDeleteRequest(routingContext))
                .andThen(Completable.fromAction(() -> {
                    verify(routingContext.response()).setStatusCode(StatusCode.NO_CONTENT.getStatusCode());
                    verify(routingContext.response()).end();
                    async.complete();
                }))
                .subscribe();
    }

    @Test(timeout = 60_000L)
    public void testDeleteFail(TestContext context) {
        RoutingContext routingContext = createContext();

        final Async async = context.async();

        when(routingContext.request().getHeader(any())).then((invocationOnMock -> {
            if (invocationOnMock.getArgument(0).equals(USER_TYPE_HEADER)) {
                return UserType.ADMIN.toString();
            }
            return "";
        }));

        when(routingContext.request().getParam("id")).thenReturn("device-0");

        DevicePoolRouter target = new DevicePoolRouter();

        target.handleDeleteRequest(routingContext)
                .andThen(Completable.fromAction(() -> {
                    verify(routingContext.response()).setStatusCode(StatusCode.NOT_FOUND.getStatusCode());
                    verify(routingContext.response()).end(any(Buffer.class));
                    async.complete();
                }))
                .subscribe();
    }

    @Test(timeout = 60_000L)
    public void testDeleteWithBlankId(TestContext context) {
        RoutingContext routingContext = createContext();

        final Async async = context.async();

        when(routingContext.request().getHeader(any())).then((invocationOnMock -> {
            if (invocationOnMock.getArgument(0).equals(USER_TYPE_HEADER)) {
                return UserType.ADMIN.toString();
            }
            return "";
        }));

        when(routingContext.request().getParam("id")).thenReturn("     ");

        DevicePoolRouter target = new DevicePoolRouter();

        target.handleDeleteRequest(routingContext)
                .andThen(Completable.fromAction(() -> {
                    verify(routingContext.response()).setStatusCode(StatusCode.BAD_REQUEST.getStatusCode());
                    verify(routingContext.response()).end(any(Buffer.class));
                    async.complete();
                }))
                .subscribe();
    }

    @Test(timeout = 60_000L)
    public void testDeleteWithInvalidAccessRights(TestContext context) {
        RoutingContext routingContext = createContext();

        final Async async = context.async();

        when(routingContext.request().getHeader(any())).then((invocationOnMock -> {
            if (invocationOnMock.getArgument(0).equals(USER_TYPE_HEADER)) {
                return UserType.USER.toString();
            }
            return "";
        }));

        DevicePoolRouter target = new DevicePoolRouter();

        target.handleDeleteRequest(routingContext)
                .andThen(Completable.fromAction(() -> {
                    verify(routingContext).fail(StatusCode.FORBIDDEN.getStatusCode());
                    async.complete();
                }))
                .subscribe();
    }


    public static RoutingContext createContext() {
        RoutingContext context = mock(RoutingContext.class);
        HttpServerResponse response = mock(HttpServerResponse.class);
        HttpServerRequest request = mock(HttpServerRequest.class);

        when(context.response()).thenReturn(response);
        when(context.request()).thenReturn(request);

        return context;
    }


}
