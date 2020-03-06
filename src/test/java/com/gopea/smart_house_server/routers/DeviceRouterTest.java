package com.gopea.smart_house_server.routers;

import com.gopea.smart_house_server.configs.StatusCode;
import com.gopea.smart_house_server.connectors.Connectible;
import com.gopea.smart_house_server.data_base.Storages;
import com.gopea.smart_house_server.devices.Device;
import com.gopea.smart_house_server.devices.DeviceType;
import com.gopea.smart_house_server.devices.Door;
import com.gopea.smart_house_server.devices.Lamp;
import com.gopea.smart_house_server.routers.users.UserType;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.web.Route;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static com.gopea.smart_house_server.common.Helpers.USER_TYPE_HEADER;
import static com.gopea.smart_house_server.connectors.Connectors.COMMAND_ACTION_KEY;
import static com.gopea.smart_house_server.data_base.Storages.ID;
import static com.gopea.smart_house_server.devices.BaseDevice.HOST_KEY;
import static com.gopea.smart_house_server.devices.BaseDevice.UPDATE_TIME_KEY;
import static com.gopea.smart_house_server.devices.Devices.DEVICE_PROPERTIES_KEY;
import static com.gopea.smart_house_server.devices.Devices.DEVICE_TYPE_KEY;
import static com.gopea.smart_house_server.examples.StandardDeviceExample.STATE_KEY;
import static com.gopea.smart_house_server.routers.Common.createContext;
import static junit.framework.TestCase.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(VertxUnitRunner.class)
public class DeviceRouterTest {
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
  public void after(TestContext context) {
    final Async async = context.async();
    Storages.DEVICE_STORAGE.getDevices()
        .flatMapCompletable(list ->
            Flowable.fromIterable(list)
                .flatMapSingle(pair -> Storages.DEVICE_STORAGE.deleteDevice(pair.getKey()))
                .ignoreElements())
        .andThen(Completable.fromAction(async::complete))
        .subscribe();
    vertx.close();
  }


  @Test(timeout = 60_000L)
  public void testLoadRouter() {
    Router target = new DeviceRouter().loadRouter(Vertx.vertx());

    List<Route> list = target.getRoutes();
    assertEquals(11, list.size());
  }

  @Test(timeout = 60_000L)
  public void testPostRequestFailAccessRights(TestContext context) {
    RoutingContext routingContext = createContext();

    final Async async = context.async();

    when(routingContext.request().getHeader(USER_TYPE_HEADER)).thenReturn(UserType.USER.toString());

    DeviceRouter target = new DeviceRouter();
    target.handlePostRequest(routingContext)
        .andThen(Completable.fromAction(() -> {
          verify(routingContext).fail(StatusCode.FORBIDDEN.getStatusCode());
          async.complete();
        }))
        .subscribe();

  }

  @Test(timeout = 60_000L)
  public void testPostRequestWithNoBody(TestContext context) {
    RoutingContext routingContext = createContext();

    final Async async = context.async();

    when(routingContext.request().getHeader(USER_TYPE_HEADER)).thenReturn(UserType.ADMIN.toString());
    when(routingContext.getBodyAsJson()).thenThrow(new RuntimeException());

    DeviceRouter target = new DeviceRouter();
    target.handlePostRequest(routingContext)
        .andThen(Completable.fromAction(() -> {
          verify(routingContext).fail(anyInt(), any());
          async.complete();
        }))
        .subscribe();

  }

  @Test(timeout = 60_000L)
  public void testPostRequestWithNullBody(TestContext context) {
    RoutingContext routingContext = createContext();

    final Async async = context.async();

    when(routingContext.request().getHeader(USER_TYPE_HEADER)).thenReturn(UserType.ADMIN.toString());
    when(routingContext.getBodyAsJson()).thenReturn(null);

    DeviceRouter target = new DeviceRouter();
    target.handlePostRequest(routingContext)
        .andThen(Completable.fromAction(() -> {
          verify(routingContext.response()).setStatusCode(StatusCode.BAD_REQUEST.getStatusCode());
          verify(routingContext.response()).end(any(Buffer.class));
          async.complete();
        }))
        .subscribe();

  }

  @Test(timeout = 60_000L)
  public void testPostRequestWithMissedDeviceType(TestContext context) {
    RoutingContext routingContext = createContext();

    final Async async = context.async();

    when(routingContext.request().getHeader(USER_TYPE_HEADER)).thenReturn(UserType.ADMIN.toString());
    when(routingContext.getBodyAsJson()).thenReturn(new JsonObject());

    DeviceRouter target = new DeviceRouter();
    target.handlePostRequest(routingContext)
        .andThen(Completable.fromAction(() -> {
          verify(routingContext.response()).setStatusCode(StatusCode.BAD_REQUEST.getStatusCode());
          verify(routingContext.response()).end(any(Buffer.class));
          async.complete();
        }))
        .subscribe();

  }

  @Test(timeout = 60_000L)
  public void testPostRequestWithUnsupportedDeviceType(TestContext context) {
    RoutingContext routingContext = createContext();

    final Async async = context.async();

    when(routingContext.request().getHeader(USER_TYPE_HEADER)).thenReturn(UserType.ADMIN.toString());
    when(routingContext.getBodyAsJson()).thenReturn(new JsonObject()
        .put(DEVICE_TYPE_KEY, "jshja"));

    DeviceRouter target = new DeviceRouter();
    target.handlePostRequest(routingContext)
        .andThen(Completable.fromAction(() -> {
          verify(routingContext.response()).setStatusCode(StatusCode.UNPROCESSABLE_ENTITY.getStatusCode());
          verify(routingContext.response()).end(any(Buffer.class));
          async.complete();
        }))
        .subscribe();

  }

  @Test(timeout = 60_000L)
  public void testPostRequestWithoutDeviceProperties(TestContext context) {
    RoutingContext routingContext = createContext();

    final Async async = context.async();

    when(routingContext.request().getHeader(USER_TYPE_HEADER)).thenReturn(UserType.ADMIN.toString());
    when(routingContext.getBodyAsJson()).thenReturn(new JsonObject()
        .put(DEVICE_TYPE_KEY, DeviceType.DOOR.toString()));

    DeviceRouter target = new DeviceRouter();
    target.handlePostRequest(routingContext)
        .andThen(Completable.fromAction(() -> {
          verify(routingContext.response()).setStatusCode(StatusCode.BAD_REQUEST.getStatusCode());
          verify(routingContext.response()).end(any(Buffer.class));
          async.complete();
        }))
        .subscribe();

  }

  @Test(timeout = 60_000L)
  public void testPostRequestWithIncorrectDevicePropertiesNonInt(TestContext context) {
    RoutingContext routingContext = createContext();

    final Async async = context.async();

    when(routingContext.request().getHeader(USER_TYPE_HEADER)).thenReturn(UserType.ADMIN.toString());
    when(routingContext.getBodyAsJson()).thenReturn(new JsonObject()
        .put(DEVICE_TYPE_KEY, DeviceType.DOOR.toString())
        .put(DEVICE_PROPERTIES_KEY, new JsonObject().put(UPDATE_TIME_KEY, "")));

    DeviceRouter target = new DeviceRouter();
    target.handlePostRequest(routingContext)
        .andThen(Completable.fromAction(() -> {
          verify(routingContext.response()).setStatusCode(StatusCode.BAD_REQUEST.getStatusCode());
          verify(routingContext.response()).end(any(Buffer.class));
          async.complete();
        }))
        .subscribe();

  }

  @Test(timeout = 60_000L)
  public void testPostRequestWithIncorrectDevicePropertiesNegative(TestContext context) {
    RoutingContext routingContext = createContext();

    final Async async = context.async();

    when(routingContext.request().getHeader(USER_TYPE_HEADER)).thenReturn(UserType.ADMIN.toString());
    when(routingContext.getBodyAsJson()).thenReturn(new JsonObject()
        .put(DEVICE_TYPE_KEY, DeviceType.DOOR.toString())
        .put(DEVICE_PROPERTIES_KEY, new JsonObject().put(UPDATE_TIME_KEY, -1)));

    DeviceRouter target = new DeviceRouter();
    target.handlePostRequest(routingContext)
        .andThen(Completable.fromAction(() -> {
          verify(routingContext.response()).setStatusCode(StatusCode.BAD_REQUEST.getStatusCode());
          verify(routingContext.response()).end(any(Buffer.class));
          async.complete();
        }))
        .subscribe();

  }

  @Test(timeout = 60_000L)
  public void testPostRequestWithoutBadDeviceProperties(TestContext context) {
    RoutingContext routingContext = createContext();

    final Async async = context.async();

    when(routingContext.request().getHeader(USER_TYPE_HEADER)).thenReturn(UserType.ADMIN.toString());
    when(routingContext.getBodyAsJson()).thenReturn(new JsonObject()
        .put(DEVICE_TYPE_KEY, DeviceType.DOOR.toString())
        .put(DEVICE_PROPERTIES_KEY, new JsonObject().put(HOST_KEY, 112)));

    DeviceRouter target = new DeviceRouter();
    target.handlePostRequest(routingContext)
        .andThen(Completable.fromAction(() -> {
          verify(routingContext).fail(anyInt(), any());
          async.complete();
        }))
        .subscribe();

  }

  @Test(timeout = 60_000L)
  public void testPostRequestWithPositiveUpdateTime(TestContext context) {
    RoutingContext routingContext = createContext();

    final Async async = context.async();

    when(routingContext.request().getHeader(USER_TYPE_HEADER)).thenReturn(UserType.ADMIN.toString());
    when(routingContext.getBodyAsJson()).thenReturn(new JsonObject()
        .put(DEVICE_TYPE_KEY, DeviceType.DOOR.toString())
        .put(DEVICE_PROPERTIES_KEY, new JsonObject().put(UPDATE_TIME_KEY, 1).mergeIn(BASE_OBJECT)));

    DeviceRouter target = new DeviceRouter();
    target.handlePostRequest(routingContext)
        .andThen(Completable.fromAction(() -> {
          verify(routingContext.response()).setStatusCode(StatusCode.CREATED.getStatusCode());
          verify(routingContext.response()).end(any(Buffer.class));
          async.complete();
        }))
        .subscribe();

  }

  @Test(timeout = 60_000L)
  public void testPostRequestWithoutUpdateTime(TestContext context) {
    RoutingContext routingContext = createContext();

    final Async async = context.async();

    when(routingContext.request().getHeader(USER_TYPE_HEADER)).thenReturn(UserType.ADMIN.toString());
    when(routingContext.getBodyAsJson()).thenReturn(new JsonObject()
        .put(DEVICE_TYPE_KEY, DeviceType.DOOR.toString())
        .put(DEVICE_PROPERTIES_KEY, new JsonObject().mergeIn(BASE_OBJECT)));

    DeviceRouter target = new DeviceRouter();
    target.handlePostRequest(routingContext)
        .andThen(Completable.fromAction(() -> {
          verify(routingContext.response()).setStatusCode(StatusCode.CREATED.getStatusCode());
          verify(routingContext.response()).end(any(Buffer.class));
          async.complete();
        }))
        .subscribe();

  }

  @Test(timeout = 60_000L)
  public void testHandleExecuteWithoutId(TestContext context) {
    RoutingContext routingContext = createContext();

    final Async async = context.async();

    when(routingContext.request().getHeader(USER_TYPE_HEADER)).thenReturn(UserType.ADMIN.toString());
    when(routingContext.request().getParam(ID)).thenReturn(null);

    DeviceRouter target = new DeviceRouter();
    target.handleExecute(routingContext)
        .andThen(Completable.fromAction(() -> {
          verify(routingContext.response()).setStatusCode(StatusCode.BAD_REQUEST.getStatusCode());
          verify(routingContext.response()).end(any(Buffer.class));
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60_000L)
  public void testHandleExecuteWithoutBody(TestContext context) {
    RoutingContext routingContext = createContext();

    final Async async = context.async();

    when(routingContext.request().getHeader(USER_TYPE_HEADER)).thenReturn(UserType.ADMIN.toString());
    when(routingContext.request().getParam(ID)).thenReturn("id");
    when(routingContext.getBodyAsJson()).thenReturn(null);

    DeviceRouter target = new DeviceRouter();
    target.handleExecute(routingContext)
        .andThen(Completable.fromAction(() -> {
          verify(routingContext.response()).setStatusCode(StatusCode.BAD_REQUEST.getStatusCode());
          verify(routingContext.response()).end(any(Buffer.class));
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60_000L)
  public void testHandleExecuteWithoutBody1(TestContext context) {
    RoutingContext routingContext = createContext();

    final Async async = context.async();

    when(routingContext.request().getHeader(USER_TYPE_HEADER)).thenReturn(UserType.ADMIN.toString());
    when(routingContext.request().getParam(ID)).thenReturn("id");
    when(routingContext.getBodyAsJson()).thenThrow(new RuntimeException());

    DeviceRouter target = new DeviceRouter();
    target.handleExecute(routingContext)
        .andThen(Completable.fromAction(() -> {
          verify(routingContext).fail(anyInt(), any());
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60_000L)
  public void testHandleExecuteWithUnExistsID(TestContext context) {
    RoutingContext routingContext = createContext();

    final Async async = context.async();

    when(routingContext.request().getHeader(USER_TYPE_HEADER)).thenReturn(UserType.ADMIN.toString());
    when(routingContext.request().getParam(ID)).thenReturn("id");
    when(routingContext.getBodyAsJson()).thenReturn(new JsonObject().put(COMMAND_ACTION_KEY, ""));

    DeviceRouter target = new DeviceRouter();
    target.handleExecute(routingContext)
        .andThen(Completable.fromAction(() -> {
          verify(routingContext.response()).setStatusCode(StatusCode.NOT_FOUND.getStatusCode());
          verify(routingContext.response()).end(any(Buffer.class));
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60_000L)
  public void testHandleExecuteSuccess(TestContext context) {
    RoutingContext routingContext = createContext();

    final Async async = context.async();

    when(routingContext.request().getHeader(USER_TYPE_HEADER)).thenReturn(UserType.ADMIN.toString());
    when(routingContext.getBodyAsJson()).thenReturn(new JsonObject()
        .put(STATE_KEY, "on"));

    DeviceRouter target = new DeviceRouter();

    Storages.DEVICE_STORAGE.addDevice(new Lamp(BASE_OBJECT))
        .flatMapCompletable(response -> {
          when(routingContext.request().getParam(ID)).thenReturn(response.getString(ID));
          return Storages.DEVICE_STORAGE.getDevice(routingContext.request().getParam(ID))
              .flatMapSingle(Device::connect)
              .ignoreElement()
              .andThen(target.handleExecute(routingContext));
        })
        .andThen(Completable.fromAction(() -> {
          verify(routingContext.response()).setStatusCode(StatusCode.SUCCESS.getStatusCode());
          verify(routingContext.response()).end(any(Buffer.class));
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60_000L)
  public void testHandleExecuteUnavailable(TestContext context) {
    RoutingContext routingContext = createContext();

    final Async async = context.async();

    when(routingContext.request().getHeader(USER_TYPE_HEADER)).thenReturn(UserType.ADMIN.toString());
    when(routingContext.getBodyAsJson()).thenReturn(new JsonObject()
        .put(STATE_KEY, "on"));

    DeviceRouter target = new DeviceRouter();

    Storages.DEVICE_STORAGE.addDevice(new Lamp(BASE_OBJECT))
        .flatMapCompletable(response -> {
          when(routingContext.request().getParam(ID)).thenReturn(response.getString(ID));
          return target.handleExecute(routingContext);
        })
        .andThen(Completable.fromAction(() -> {
          verify(routingContext.response()).setStatusCode(StatusCode.UNAVAILABLE.getStatusCode());
          verify(routingContext.response()).end(any(Buffer.class));
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60_000L)
  public void testHandlePowerOffInvalidAccessRights(TestContext context) {
    RoutingContext routingContext = createContext();

    final Async async = context.async();

    when(routingContext.request().getHeader(USER_TYPE_HEADER)).thenReturn(UserType.USER.toString());

    DeviceRouter target = new DeviceRouter();
    target.handlePowerOff(routingContext)
        .andThen(Completable.fromAction(() -> {
          verify(routingContext).fail(StatusCode.FORBIDDEN.getStatusCode());
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60_000L)
  public void testHandlePowerOffBadId(TestContext context) {
    RoutingContext routingContext = createContext();

    final Async async = context.async();

    when(routingContext.request().getHeader(USER_TYPE_HEADER)).thenReturn(UserType.ADMIN.toString());
    when(routingContext.request().getParam(ID)).thenReturn(null);

    DeviceRouter target = new DeviceRouter();
    target.handlePowerOff(routingContext)
        .andThen(Completable.fromAction(() -> {
          verify(routingContext.response()).setStatusCode(StatusCode.BAD_REQUEST.getStatusCode());
          verify(routingContext.response()).end(any(Buffer.class));
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60_000L)
  public void testHandlePowerOffNotExists(TestContext context) {
    RoutingContext routingContext = createContext();

    final Async async = context.async();

    when(routingContext.request().getHeader(USER_TYPE_HEADER)).thenReturn(UserType.ADMIN.toString());
    when(routingContext.request().getParam(ID)).thenReturn("siqsij");

    DeviceRouter target = new DeviceRouter();
    target.handlePowerOff(routingContext)
        .andThen(Completable.fromAction(() -> {
          verify(routingContext.response()).setStatusCode(StatusCode.NOT_FOUND.getStatusCode());
          verify(routingContext.response()).end(any(Buffer.class));
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60_000L)
  public void testHandlePowerOffSuccess(TestContext context) {
    RoutingContext routingContext = createContext();

    final Async async = context.async();

    when(routingContext.request().getHeader(USER_TYPE_HEADER)).thenReturn(UserType.ADMIN.toString());

    DeviceRouter target = new DeviceRouter();

    Device device = new Lamp(BASE_OBJECT);

    Storages.DEVICE_STORAGE.addDevice(device)
        .flatMapCompletable(response -> {
          when(routingContext.request().getParam(ID)).thenReturn(response.getString(ID));
          return device.connect()
              .ignoreElement()
              .andThen(target.handlePowerOff(routingContext));
        })
        .andThen(Completable.fromAction(() -> {
          verify(routingContext.response()).setStatusCode(StatusCode.SUCCESS.getStatusCode());
          verify(routingContext.response()).end(any(Buffer.class));
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60_000L)
  public void testHandlePowerOffUnavailable(TestContext context) {
    RoutingContext routingContext = createContext();

    final Async async = context.async();

    when(routingContext.request().getHeader(USER_TYPE_HEADER)).thenReturn(UserType.ADMIN.toString());
    when(routingContext.getBodyAsJson()).thenReturn(new JsonObject()
        .put(STATE_KEY, "on"));

    DeviceRouter target = new DeviceRouter();

    Storages.DEVICE_STORAGE.addDevice(new Lamp(BASE_OBJECT))
        .flatMapCompletable(response -> {
          when(routingContext.request().getParam(ID)).thenReturn(response.getString(ID));
          return target.handlePowerOff(routingContext);
        })
        .andThen(Completable.fromAction(() -> {
          verify(routingContext.response()).setStatusCode(StatusCode.UNAVAILABLE.getStatusCode());
          verify(routingContext.response()).end(any(Buffer.class));
          async.complete();
        }))
        .subscribe();
  }


  @Test(timeout = 60_000L)
  public void testHandleRebootInvalidAccessRights(TestContext context) {
    RoutingContext routingContext = createContext();

    final Async async = context.async();

    when(routingContext.request().getHeader(USER_TYPE_HEADER)).thenReturn(UserType.USER.toString());

    DeviceRouter target = new DeviceRouter();
    target.handleReboot(routingContext)
        .andThen(Completable.fromAction(() -> {
          verify(routingContext).fail(StatusCode.FORBIDDEN.getStatusCode());
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60_000L)
  public void testHandleRebootBadId(TestContext context) {
    RoutingContext routingContext = createContext();

    final Async async = context.async();

    when(routingContext.request().getHeader(USER_TYPE_HEADER)).thenReturn(UserType.ADMIN.toString());
    when(routingContext.request().getParam(ID)).thenReturn(null);

    DeviceRouter target = new DeviceRouter();
    target.handleReboot(routingContext)
        .andThen(Completable.fromAction(() -> {
          verify(routingContext.response()).setStatusCode(StatusCode.BAD_REQUEST.getStatusCode());
          verify(routingContext.response()).end(any(Buffer.class));
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60_000L)
  public void testHandleRebootNotExists(TestContext context) {
    RoutingContext routingContext = createContext();

    final Async async = context.async();

    when(routingContext.request().getHeader(USER_TYPE_HEADER)).thenReturn(UserType.ADMIN.toString());
    when(routingContext.request().getParam(ID)).thenReturn("siqsij");

    DeviceRouter target = new DeviceRouter();
    target.handleReboot(routingContext)
        .andThen(Completable.fromAction(() -> {
          verify(routingContext.response()).setStatusCode(StatusCode.NOT_FOUND.getStatusCode());
          verify(routingContext.response()).end(any(Buffer.class));
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60_000L)
  public void testHandleRebootUnavailable(TestContext context) {
    RoutingContext routingContext = createContext();

    final Async async = context.async();

    when(routingContext.request().getHeader(USER_TYPE_HEADER)).thenReturn(UserType.ADMIN.toString());
    when(routingContext.getBodyAsJson()).thenReturn(new JsonObject()
        .put(STATE_KEY, "on"));

    DeviceRouter target = new DeviceRouter();

    Storages.DEVICE_STORAGE.addDevice(new Lamp(BASE_OBJECT))
        .flatMapCompletable(response -> {
          when(routingContext.request().getParam(ID)).thenReturn(response.getString(ID));
          return target.handleReboot(routingContext);
        })
        .andThen(Completable.fromAction(() -> {
          verify(routingContext.response()).setStatusCode(StatusCode.UNAVAILABLE.getStatusCode());
          verify(routingContext.response()).end(any(Buffer.class));
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60_000L)
  public void testHandleRebootSuccess(TestContext context) {
    RoutingContext routingContext = createContext();

    final Async async = context.async();

    when(routingContext.request().getHeader(USER_TYPE_HEADER)).thenReturn(UserType.ADMIN.toString());

    DeviceRouter target = new DeviceRouter();

    Device device = new Lamp(BASE_OBJECT);

    Storages.DEVICE_STORAGE.addDevice(device)
        .flatMapCompletable(response -> {
          when(routingContext.request().getParam(ID)).thenReturn(response.getString(ID));
          return device.connect()
              .ignoreElement()
              .andThen(target.handleReboot(routingContext));
        })
        .andThen(Completable.fromAction(() -> {
          verify(routingContext.response()).setStatusCode(StatusCode.SUCCESS.getStatusCode());
          verify(routingContext.response()).end(any(Buffer.class));
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60_000L)
  public void testHandleDisconnectInvalidAccessRights(TestContext context) {
    RoutingContext routingContext = createContext();

    final Async async = context.async();

    when(routingContext.request().getHeader(USER_TYPE_HEADER)).thenReturn(UserType.USER.toString());

    DeviceRouter target = new DeviceRouter();
    target.handleDisconnect(routingContext)
        .andThen(Completable.fromAction(() -> {
          verify(routingContext).fail(StatusCode.FORBIDDEN.getStatusCode());
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60_000L)
  public void testHandleDisconnectBadId(TestContext context) {
    RoutingContext routingContext = createContext();

    final Async async = context.async();

    when(routingContext.request().getHeader(USER_TYPE_HEADER)).thenReturn(UserType.ADMIN.toString());
    when(routingContext.request().getParam(ID)).thenReturn(null);

    DeviceRouter target = new DeviceRouter();
    target.handleDisconnect(routingContext)
        .andThen(Completable.fromAction(() -> {
          verify(routingContext.response()).setStatusCode(StatusCode.BAD_REQUEST.getStatusCode());
          verify(routingContext.response()).end(any(Buffer.class));
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60_000L)
  public void testHandleDisconnectNotExists(TestContext context) {
    RoutingContext routingContext = createContext();

    final Async async = context.async();

    when(routingContext.request().getHeader(USER_TYPE_HEADER)).thenReturn(UserType.ADMIN.toString());
    when(routingContext.request().getParam(ID)).thenReturn("siqsij");

    DeviceRouter target = new DeviceRouter();
    target.handleDisconnect(routingContext)
        .andThen(Completable.fromAction(() -> {
          verify(routingContext.response()).setStatusCode(StatusCode.NOT_FOUND.getStatusCode());
          verify(routingContext.response()).end(any(Buffer.class));
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60_000L)
  public void testHandleDisconnectSuccess(TestContext context) {
    RoutingContext routingContext = createContext();

    final Async async = context.async();

    when(routingContext.request().getHeader(USER_TYPE_HEADER)).thenReturn(UserType.ADMIN.toString());

    DeviceRouter target = new DeviceRouter();

    Device device = new Lamp(BASE_OBJECT);

    Storages.DEVICE_STORAGE.addDevice(device)
        .flatMapCompletable(response -> {
          when(routingContext.request().getParam(ID)).thenReturn(response.getString(ID));
          return device.connect()
              .ignoreElement()
              .andThen(target.handleDisconnect(routingContext));
        })
        .andThen(Completable.fromAction(() -> {
          verify(routingContext.response()).setStatusCode(StatusCode.SUCCESS.getStatusCode());
          verify(routingContext.response()).end(any(Buffer.class));
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60_000L)
  public void testHandleConnectInvalidAccessRights(TestContext context) {
    RoutingContext routingContext = createContext();

    final Async async = context.async();

    when(routingContext.request().getHeader(USER_TYPE_HEADER)).thenReturn(UserType.USER.toString());

    DeviceRouter target = new DeviceRouter();
    target.handleConnect(routingContext)
        .andThen(Completable.fromAction(() -> {
          verify(routingContext).fail(StatusCode.FORBIDDEN.getStatusCode());
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60_000L)
  public void testHandleConnectBadId(TestContext context) {
    RoutingContext routingContext = createContext();

    final Async async = context.async();

    when(routingContext.request().getHeader(USER_TYPE_HEADER)).thenReturn(UserType.ADMIN.toString());
    when(routingContext.request().getParam(ID)).thenReturn(null);

    DeviceRouter target = new DeviceRouter();
    target.handleConnect(routingContext)
        .andThen(Completable.fromAction(() -> {
          verify(routingContext.response()).setStatusCode(StatusCode.BAD_REQUEST.getStatusCode());
          verify(routingContext.response()).end(any(Buffer.class));
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60_000L)
  public void testHandleConnectNotExists(TestContext context) {
    RoutingContext routingContext = createContext();

    final Async async = context.async();

    when(routingContext.request().getHeader(USER_TYPE_HEADER)).thenReturn(UserType.ADMIN.toString());
    when(routingContext.request().getParam(ID)).thenReturn("siqsij");

    DeviceRouter target = new DeviceRouter();
    target.handleConnect(routingContext)
        .andThen(Completable.fromAction(() -> {
          verify(routingContext.response()).setStatusCode(StatusCode.NOT_FOUND.getStatusCode());
          verify(routingContext.response()).end(any(Buffer.class));
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60_000L)
  public void testHandleConnectSuccess(TestContext context) {
    RoutingContext routingContext = createContext();

    final Async async = context.async();

    when(routingContext.request().getHeader(USER_TYPE_HEADER)).thenReturn(UserType.ADMIN.toString());

    DeviceRouter target = new DeviceRouter();

    Device device = new Lamp(BASE_OBJECT);

    Storages.DEVICE_STORAGE.addDevice(device)
        .flatMapCompletable(response -> {
          when(routingContext.request().getParam(ID)).thenReturn(response.getString(ID));
          return device.connect()
              .ignoreElement()
              .andThen(target.handleConnect(routingContext));
        })
        .andThen(Completable.fromAction(() -> {
          verify(routingContext.response()).setStatusCode(StatusCode.SUCCESS.getStatusCode());
          verify(routingContext.response()).end(any(Buffer.class));
          async.complete();
        }))
        .subscribe();
  }


  @Test(timeout = 60_000L)
  public void testHandleGetWithBadId(TestContext context) {
    RoutingContext routingContext = createContext();

    final Async async = context.async();

    when(routingContext.request().getParam(ID)).thenReturn(null);

    DeviceRouter target = new DeviceRouter();
    target.handleGetWithId(routingContext)
        .andThen(Completable.fromAction(() -> {
          verify(routingContext.response()).setStatusCode(StatusCode.BAD_REQUEST.getStatusCode());
          verify(routingContext.response()).end(any(Buffer.class));
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60_000L)
  public void testHandleGetWithIdUnExists(TestContext context) {
    RoutingContext routingContext = createContext();

    final Async async = context.async();

    when(routingContext.request().getParam(ID)).thenReturn("hjsshdj");

    DeviceRouter target = new DeviceRouter();
    target.handleGetWithId(routingContext)
        .andThen(Completable.fromAction(() -> {
          verify(routingContext.response()).setStatusCode(StatusCode.NOT_FOUND.getStatusCode());
          verify(routingContext.response()).end(any(Buffer.class));
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60_000L)
  public void testHandleGetWithIdUnavailable(TestContext context) {
    RoutingContext routingContext = createContext();

    final Async async = context.async();

    when(routingContext.request().getParam(ID)).thenReturn("hjsshdj");

    DeviceRouter target = new DeviceRouter();

    Device lamp = new Lamp(BASE_OBJECT);

    Storages.DEVICE_STORAGE.addDevice(lamp)
        .flatMapCompletable(response -> {
          when(routingContext.request().getParam(ID)).thenReturn(response.getString(ID));
          return target.handleGetWithId(routingContext);
        })
        .andThen(Completable.fromAction(() -> {
          verify(routingContext.response()).setStatusCode(StatusCode.UNAVAILABLE.getStatusCode());
          verify(routingContext.response()).end(any(Buffer.class));
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60_000L)
  public void testHandleGetSuccess(TestContext context) {
    RoutingContext routingContext = createContext();

    final Async async = context.async();

    when(routingContext.request().getParam(ID)).thenReturn("hjsshdj");

    DeviceRouter target = new DeviceRouter();

    Device lamp = new Lamp(BASE_OBJECT);

    Storages.DEVICE_STORAGE.addDevice(lamp)
        .flatMapCompletable(response -> {
          when(routingContext.request().getParam(ID)).thenReturn(response.getString(ID));
          return lamp.connect()
              .ignoreElement()
              .andThen(target.handleGetWithId(routingContext));
        })
        .andThen(Completable.fromAction(() -> {
          verify(routingContext.response()).setStatusCode(StatusCode.SUCCESS.getStatusCode());
          verify(routingContext.response()).end(any(Buffer.class));
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60_000L)
  public void testHandleMetricsBadId(TestContext context) {
    RoutingContext routingContext = createContext();

    final Async async = context.async();

    when(routingContext.request().getParam(ID)).thenReturn(null);

    DeviceRouter target = new DeviceRouter();
    target.handleMetrics(routingContext)
        .andThen(Completable.fromAction(() -> {
          verify(routingContext.response()).setStatusCode(StatusCode.BAD_REQUEST.getStatusCode());
          verify(routingContext.response()).end(any(Buffer.class));
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60_000L)
  public void testHandleMetricsNotExists(TestContext context) {
    RoutingContext routingContext = createContext();

    final Async async = context.async();

    when(routingContext.request().getParam(ID)).thenReturn("device-pppp");

    DeviceRouter target = new DeviceRouter();
    target.handleMetrics(routingContext)
        .andThen(Completable.fromAction(() -> {
          verify(routingContext.response()).setStatusCode(StatusCode.NOT_FOUND.getStatusCode());
          verify(routingContext.response()).end(any(Buffer.class));
          async.complete();
        }))
        .subscribe();
  }


  @Test(timeout = 60_000L)
  public void testHandleMetricsSuccess(TestContext context) {
    RoutingContext routingContext = createContext();

    final Async async = context.async();

    DeviceRouter target = new DeviceRouter();

    Device device = new Lamp(BASE_OBJECT);

    Storages.DEVICE_STORAGE.addDevice(device)
        .flatMapCompletable(response -> {
          when(routingContext.request().getParam(ID)).thenReturn(response.getString(ID));
          return device.connect()
              .flatMap(ign -> device.getData())
              .ignoreElement()
              .andThen(target.handleMetrics(routingContext));
        })
        .andThen(Completable.fromAction(() -> {
          verify(routingContext.response()).setStatusCode(StatusCode.SUCCESS.getStatusCode());
          verify(routingContext.response()).end(any(Buffer.class));
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60_000L)
  public void testHandleGetInvalidDeviceType(TestContext context) {
    final Async async = context.async();

    RoutingContext routingContext = createContext();
    when(routingContext.request().getParam(ID)).thenReturn(null);
    when(routingContext.request().getParam(DEVICE_TYPE_KEY)).thenReturn("InvalidDevice");

    DeviceRouter target = new DeviceRouter();

    target.handleGet(routingContext)
        .andThen(Completable.fromAction(() -> {
          verify(routingContext.response()).setStatusCode(StatusCode.SUCCESS.getStatusCode());
          verify(routingContext.response()).end(any(Buffer.class));
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60_000L)
  public void testHandleGetWithDeviceType(TestContext context) {
    final Async async = context.async();

    RoutingContext routingContext = createContext();
    when(routingContext.request().getParam(ID)).thenReturn(null);
    when(routingContext.request().getParam(DEVICE_TYPE_KEY)).thenReturn(DeviceType.LAMP.toString());


    DeviceRouter target = new DeviceRouter();
    Storages.DEVICE_STORAGE.addDevice(new Lamp(BASE_OBJECT))
        .flatMap(ign -> Storages.DEVICE_STORAGE.addDevice(new Lamp(BASE_OBJECT)))
        .flatMap(ign -> Storages.DEVICE_STORAGE.addDevice(new Door(BASE_OBJECT)))
        .flatMapCompletable(ign -> target.handleGet(routingContext))
        .andThen(Completable.fromAction(() -> {
          ArgumentCaptor<Buffer> valueCapture = ArgumentCaptor.forClass(Buffer.class);
          verify(routingContext.response()).setStatusCode(StatusCode.SUCCESS.getStatusCode());
          verify(routingContext.response()).end(valueCapture.capture());

          context.assertEquals(2, valueCapture.getValue().toJsonArray().size());

          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60_000L)
  public void testHandleGetWithUnExistsID(TestContext context) {
    final Async async = context.async();

    RoutingContext routingContext = createContext();
    when(routingContext.request().getParam(ID)).thenReturn("blabla");
    when(routingContext.request().getParam(DEVICE_TYPE_KEY)).thenReturn(null);


    DeviceRouter target = new DeviceRouter();
    Storages.DEVICE_STORAGE.addDevice(new Lamp(BASE_OBJECT))
        .flatMap(ign -> Storages.DEVICE_STORAGE.addDevice(new Lamp(BASE_OBJECT)))
        .flatMap(ign -> Storages.DEVICE_STORAGE.addDevice(new Door(BASE_OBJECT)))
        .flatMapCompletable(ign -> target.handleGet(routingContext))
        .andThen(Completable.fromAction(() -> {
          ArgumentCaptor<Buffer> valueCapture = ArgumentCaptor.forClass(Buffer.class);
          verify(routingContext.response()).setStatusCode(StatusCode.SUCCESS.getStatusCode());
          verify(routingContext.response()).end(valueCapture.capture());

          context.assertTrue(valueCapture.getValue().toJsonArray().isEmpty());

          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60_000L)
  public void testHandleGetWithUnExistsType(TestContext context) {
    final Async async = context.async();

    RoutingContext routingContext = createContext();
    when(routingContext.request().getParam(ID)).thenReturn(null);
    when(routingContext.request().getParam(DEVICE_TYPE_KEY)).thenReturn("wshwjhw");


    DeviceRouter target = new DeviceRouter();
    Storages.DEVICE_STORAGE.addDevice(new Lamp(BASE_OBJECT))
        .flatMap(ign -> Storages.DEVICE_STORAGE.addDevice(new Lamp(BASE_OBJECT)))
        .flatMap(ign -> Storages.DEVICE_STORAGE.addDevice(new Door(BASE_OBJECT)))
        .flatMapCompletable(ign -> target.handleGet(routingContext))
        .andThen(Completable.fromAction(() -> {
          ArgumentCaptor<Buffer> valueCapture = ArgumentCaptor.forClass(Buffer.class);
          verify(routingContext.response()).setStatusCode(StatusCode.SUCCESS.getStatusCode());
          verify(routingContext.response()).end(valueCapture.capture());

          context.assertTrue(valueCapture.getValue().toJsonArray().isEmpty());

          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60_000L)
  public void testHandleGetWithId(TestContext context) {
    final Async async = context.async();

    RoutingContext routingContext = createContext();

    when(routingContext.request().getParam(DEVICE_TYPE_KEY)).thenReturn(null);


    DeviceRouter target = new DeviceRouter();
    Storages.DEVICE_STORAGE.addDevice(new Lamp(BASE_OBJECT))
        .flatMapCompletable(response -> {
          when(routingContext.request().getParam(ID)).thenReturn(response.getString(ID));
          return target.handleGet(routingContext);
        })
        .andThen(Completable.fromAction(() -> {
          ArgumentCaptor<Buffer> valueCapture = ArgumentCaptor.forClass(Buffer.class);
          verify(routingContext.response()).setStatusCode(StatusCode.SUCCESS.getStatusCode());
          verify(routingContext.response()).end(valueCapture.capture());

          JsonArray array = valueCapture.getValue().toJsonArray();

          context.assertEquals(1, array.size());

          JsonObject device = array.getJsonObject(0);

          context.assertEquals(routingContext.request().getParam(ID), device.getString(ID));

          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60_000L)
  public void testHandleDeleteInvalidAccessRights(TestContext context) {
    RoutingContext routingContext = createContext();

    final Async async = context.async();

    when(routingContext.request().getHeader(USER_TYPE_HEADER)).thenReturn(UserType.USER.toString());

    DeviceRouter target = new DeviceRouter();
    target.handleDelete(routingContext)
        .andThen(Completable.fromAction(() -> {
          verify(routingContext).fail(StatusCode.FORBIDDEN.getStatusCode());
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60_000L)
  public void testHandleDeleteBadId(TestContext context) {
    RoutingContext routingContext = createContext();

    final Async async = context.async();

    when(routingContext.request().getHeader(USER_TYPE_HEADER)).thenReturn(UserType.ADMIN.toString());
    when(routingContext.request().getParam(ID)).thenReturn(null);

    DeviceRouter target = new DeviceRouter();
    target.handleDelete(routingContext)
        .andThen(Completable.fromAction(() -> {
          verify(routingContext.response()).setStatusCode(StatusCode.BAD_REQUEST.getStatusCode());
          verify(routingContext.response()).end(any(Buffer.class));
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60_000L)
  public void testHandleDeleteNotExists(TestContext context) {
    RoutingContext routingContext = createContext();

    final Async async = context.async();

    when(routingContext.request().getHeader(USER_TYPE_HEADER)).thenReturn(UserType.ADMIN.toString());
    when(routingContext.request().getParam(ID)).thenReturn("siqsij");

    DeviceRouter target = new DeviceRouter();
    target.handleDelete(routingContext)
        .andThen(Completable.fromAction(() -> {
          verify(routingContext.response()).setStatusCode(StatusCode.NOT_FOUND.getStatusCode());
          verify(routingContext.response()).end(any(Buffer.class));
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60_000L)
  public void testHandleDeleteSuccess(TestContext context) {
    RoutingContext routingContext = createContext();

    final Async async = context.async();

    when(routingContext.request().getHeader(USER_TYPE_HEADER)).thenReturn(UserType.ADMIN.toString());

    DeviceRouter target = new DeviceRouter();

    Device device = new Lamp(BASE_OBJECT);

    Storages.DEVICE_STORAGE.addDevice(device)
        .flatMapCompletable(response -> {
          when(routingContext.request().getParam(ID)).thenReturn(response.getString(ID));
          return device.connect()
              .ignoreElement()
              .andThen(target.handleDelete(routingContext));
        })
        .andThen(Completable.fromAction(() -> {
          verify(routingContext.response()).setStatusCode(StatusCode.NO_CONTENT.getStatusCode());
          verify(routingContext.response()).end();
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60_000L)
  public void testHandlePatchInvalidAccessRights(TestContext context) {
    RoutingContext routingContext = createContext();

    final Async async = context.async();

    when(routingContext.request().getHeader(USER_TYPE_HEADER)).thenReturn(UserType.USER.toString());

    DeviceRouter target = new DeviceRouter();
    target.handlePatch(routingContext)
        .andThen(Completable.fromAction(() -> {
          verify(routingContext).fail(StatusCode.FORBIDDEN.getStatusCode());
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60_000L)
  public void testHandlePatchBadId(TestContext context) {
    RoutingContext routingContext = createContext();

    final Async async = context.async();

    when(routingContext.request().getHeader(USER_TYPE_HEADER)).thenReturn(UserType.ADMIN.toString());
    when(routingContext.request().getParam(ID)).thenReturn(null);

    DeviceRouter target = new DeviceRouter();
    target.handlePatch(routingContext)
        .andThen(Completable.fromAction(() -> {
          verify(routingContext.response()).setStatusCode(StatusCode.BAD_REQUEST.getStatusCode());
          verify(routingContext.response()).end(any(Buffer.class));
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60_000L)
  public void testHandlePatchWithoutBody(TestContext context) {
    RoutingContext routingContext = createContext();

    final Async async = context.async();

    when(routingContext.request().getHeader(USER_TYPE_HEADER)).thenReturn(UserType.ADMIN.toString());
    when(routingContext.request().getParam(ID)).thenReturn("some id");
    when(routingContext.getBodyAsJson()).thenThrow(new RuntimeException());

    DeviceRouter target = new DeviceRouter();
    target.handlePatch(routingContext)
        .andThen(Completable.fromAction(() -> {
          verify(routingContext.response()).setStatusCode(StatusCode.BAD_REQUEST.getStatusCode());
          verify(routingContext.response()).end(any(Buffer.class));
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60_000L)
  public void testHandlePatchWithEmptyBody(TestContext context) {
    RoutingContext routingContext = createContext();

    final Async async = context.async();

    when(routingContext.request().getHeader(USER_TYPE_HEADER)).thenReturn(UserType.ADMIN.toString());
    when(routingContext.request().getParam(ID)).thenReturn("some id");
    when(routingContext.getBodyAsJson()).thenReturn(new JsonObject());

    DeviceRouter target = new DeviceRouter();
    target.handlePatch(routingContext)
        .andThen(Completable.fromAction(() -> {
          verify(routingContext.response()).setStatusCode(StatusCode.BAD_REQUEST.getStatusCode());
          verify(routingContext.response()).end(any(Buffer.class));
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60_000L)
  public void testHandlePatchWithoutDeviceProperties(TestContext context) {
    RoutingContext routingContext = createContext();

    final Async async = context.async();

    when(routingContext.request().getHeader(USER_TYPE_HEADER)).thenReturn(UserType.ADMIN.toString());
    when(routingContext.request().getParam(ID)).thenReturn("some id");
    when(routingContext.getBodyAsJson()).thenReturn(new JsonObject().put("key", "value"));

    DeviceRouter target = new DeviceRouter();
    target.handlePatch(routingContext)
        .andThen(Completable.fromAction(() -> {
          verify(routingContext.response()).setStatusCode(StatusCode.BAD_REQUEST.getStatusCode());
          verify(routingContext.response()).end(any(Buffer.class));
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60_000L)
  public void testHandlePatchWithNonIntUpdateTime(TestContext context) {
    RoutingContext routingContext = createContext();

    final Async async = context.async();

    when(routingContext.request().getHeader(USER_TYPE_HEADER)).thenReturn(UserType.ADMIN.toString());
    when(routingContext.request().getParam(ID)).thenReturn("some id");
    when(routingContext.getBodyAsJson()).thenReturn(new JsonObject().put(DEVICE_PROPERTIES_KEY, new JsonObject()
        .put(UPDATE_TIME_KEY, "time")));

    DeviceRouter target = new DeviceRouter();
    target.handlePatch(routingContext)
        .andThen(Completable.fromAction(() -> {
          verify(routingContext.response()).setStatusCode(StatusCode.BAD_REQUEST.getStatusCode());
          verify(routingContext.response()).end(any(Buffer.class));
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60_000L)
  public void testHandlePatchWithNegativeUpdateTime(TestContext context) {
    RoutingContext routingContext = createContext();

    final Async async = context.async();

    when(routingContext.request().getHeader(USER_TYPE_HEADER)).thenReturn(UserType.ADMIN.toString());
    when(routingContext.request().getParam(ID)).thenReturn("some id");
    when(routingContext.getBodyAsJson()).thenReturn(new JsonObject().put(DEVICE_PROPERTIES_KEY, new JsonObject()
        .put(UPDATE_TIME_KEY, -5)));

    DeviceRouter target = new DeviceRouter();
    target.handlePatch(routingContext)
        .andThen(Completable.fromAction(() -> {
          verify(routingContext.response()).setStatusCode(StatusCode.BAD_REQUEST.getStatusCode());
          verify(routingContext.response()).end(any(Buffer.class));
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60_000L)
  public void testHandlePatchUnExists(TestContext context) {
    RoutingContext routingContext = createContext();

    final Async async = context.async();

    when(routingContext.request().getHeader(USER_TYPE_HEADER)).thenReturn(UserType.ADMIN.toString());
    when(routingContext.request().getParam(ID)).thenReturn("some id");
    when(routingContext.getBodyAsJson()).thenReturn(new JsonObject().put(DEVICE_PROPERTIES_KEY, new JsonObject()));

    DeviceRouter target = new DeviceRouter();
    target.handlePatch(routingContext)
        .andThen(Completable.fromAction(() -> {
          verify(routingContext.response()).setStatusCode(StatusCode.NOT_FOUND.getStatusCode());
          verify(routingContext.response()).end(any(Buffer.class));
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60_000L)
  public void testHandlePatchError(TestContext context) {
    RoutingContext routingContext = createContext();

    final Async async = context.async();

    when(routingContext.request().getHeader(USER_TYPE_HEADER)).thenReturn(UserType.ADMIN.toString());

    when(routingContext.getBodyAsJson()).thenReturn(new JsonObject().put(DEVICE_PROPERTIES_KEY, new JsonObject()));

    Device lamp = new Lamp(BASE_OBJECT);

    DeviceRouter target = new DeviceRouter();
    Storages.DEVICE_STORAGE.addDevice(lamp)
        .flatMapCompletable(response -> {
          when(routingContext.request().getParam(ID)).thenReturn(response.getString(ID));
          return target.handlePatch(routingContext);
        })
        .andThen(Completable.fromAction(() -> {
          verify(routingContext.response()).setStatusCode(StatusCode.UNPROCESSABLE_ENTITY.getStatusCode());
          verify(routingContext.response()).end(any(Buffer.class));
          async.complete();
        }))
        .subscribe();
  }

  @Test(timeout = 60_000L)
  public void testHandlePatchSuccess(TestContext context) {
    RoutingContext routingContext = createContext();

    final Async async = context.async();

    when(routingContext.request().getHeader(USER_TYPE_HEADER)).thenReturn(UserType.ADMIN.toString());

    when(routingContext.getBodyAsJson()).thenReturn(new JsonObject().put(DEVICE_PROPERTIES_KEY, new JsonObject()
        .put(UPDATE_TIME_KEY, 5)));

    Device lamp = new Lamp(BASE_OBJECT);

    DeviceRouter target = new DeviceRouter();
    Storages.DEVICE_STORAGE.addDevice(lamp)
        .flatMapCompletable(response -> {
          when(routingContext.request().getParam(ID)).thenReturn(response.getString(ID));
          return target.handlePatch(routingContext);
        })
        .andThen(Completable.fromAction(() -> {
          verify(routingContext.response()).setStatusCode(StatusCode.SUCCESS.getStatusCode());
          verify(routingContext.response()).end(any(Buffer.class));
          async.complete();
        }))
        .subscribe();
  }

}
