package com.gopea.smart_house_server.routers;

import com.gopea.smart_house_server.configs.StatusCode;
import com.gopea.smart_house_server.data_base.Storages;
import com.gopea.smart_house_server.routers.users.User;
import com.gopea.smart_house_server.routers.users.UserType;
import io.reactivex.Completable;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.reactivex.core.MultiMap;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.web.Route;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;


import static com.gopea.smart_house_server.common.Helpers.*;
import static com.gopea.smart_house_server.common.Helpers.USER_TYPE_HEADER;
import static com.gopea.smart_house_server.data_base.FileUserStorage.USERS_KEY;
import static com.gopea.smart_house_server.routers.AuthRouter.AUTH_HEADER;
import static com.gopea.smart_house_server.routers.Common.createContext;
import static junit.framework.TestCase.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(VertxUnitRunner.class)
public class AuthRouterTest {

    private Vertx vertx;

    @Before
    public void before(TestContext context) {
        final Async async = context.async();
        vertx = Vertx.vertx();
        vertx.fileSystem()
                .rxWriteFile(PASSWORDS_FILE, Buffer.newInstance(new JsonObject().put(USERS_KEY, new JsonObject()).toBuffer()))
                .andThen(Completable.fromAction(async::complete))
                .subscribe();
    }

    @After
    public void after(TestContext context) {
        final Async async = context.async();

        vertx.fileSystem()
                .rxDelete(PASSWORDS_FILE)
                .andThen(Completable.fromAction(async::complete))
                .subscribe();

        vertx.close();
    }

    @Test(timeout = 60_000L)
    public void testLoadRouter() {
        Router target = new AuthRouter().loadRouter(Vertx.vertx());

        List<Route> list = target.getRoutes();
        assertEquals(1, list.size());
        assertEquals("/", list.get(0).getPath());
    }

    @Test(timeout = 60_000L)
    public void testEmptyAuthHeader(TestContext context) {
        final Async async = context.async();

        AuthRouter target = new AuthRouter();

        RoutingContext routingContext = createContext();

        when(routingContext.request().getHeader(AUTH_HEADER)).thenReturn("");

        target.handleRequest(routingContext)
                .andThen(Completable.fromAction(() -> {
                    verify(routingContext).fail(StatusCode.UNAUTHORISED.getStatusCode());
                    async.complete();
                }))
                .subscribe();
    }

    @Test(timeout = 60_000L)
    public void testInvalidAuthHeader(TestContext context) {
        final Async async = context.async();

        AuthRouter target = new AuthRouter();

        RoutingContext routingContext = createContext();

        when(routingContext.request().getHeader(AUTH_HEADER)).thenReturn("Basic :");

        target.handleRequest(routingContext)
                .andThen(Completable.fromAction(() -> {
                    verify(routingContext).fail(StatusCode.UNAUTHORISED.getStatusCode());
                    async.complete();
                }))
                .subscribe();
    }

    @Test(timeout = 60_000L)
    public void testNotExistsUser(TestContext context) {
        final Async async = context.async();

        AuthRouter target = new AuthRouter();

        RoutingContext routingContext = createContext();

        when(routingContext.request().getHeader(AUTH_HEADER)).thenReturn(String.format("Basic %s", Base64.getEncoder().encodeToString("test:password".getBytes())));

        target.handleRequest(routingContext)
                .andThen(Completable.fromAction(() -> {
                    verify(routingContext).fail(StatusCode.UNAUTHORISED.getStatusCode());
                    async.complete();
                }))
                .subscribe();
    }

    @Test(timeout = 60_000L)
    public void testAuthorisedFail(TestContext context) {
        final Async async = context.async();

        AuthRouter target = new AuthRouter();

        RoutingContext routingContext = createContext();

        User user = new User("test", UserType.ADMIN, "password");

        when(routingContext.request().getHeader(AUTH_HEADER)).thenReturn(String.format("Basic %s", Base64.getEncoder().encodeToString("test:password1".getBytes())));
        MultiMap map = mock(MultiMap.class);

        when(map.add(any(String.class), any(String.class))).thenReturn(map);
        when(routingContext.request().headers()).thenReturn(map);


        Storages.USER_STORAGE.addUser(user)
                .flatMapCompletable(ign -> target.handleRequest(routingContext))
                .andThen(Completable.fromAction(() -> {
                    verify(routingContext).fail(StatusCode.UNAUTHORISED.getStatusCode());
                    async.complete();
                }))
                .subscribe();
    }

    @Test(timeout = 60_000L)
    public void testAuthorisedSuccess(TestContext context) {
        final Async async = context.async();

        AuthRouter target = new AuthRouter();

        RoutingContext routingContext = createContext();

        User user = new User("test", UserType.ADMIN, "password");

        when(routingContext.request().getHeader(AUTH_HEADER)).thenReturn(String.format("Basic %s", Base64.getEncoder().encodeToString("test:password".getBytes())));
        MultiMap map = mock(MultiMap.class);

        when(map.add(any(String.class), any(String.class))).thenReturn(map);
        when(routingContext.request().headers()).thenReturn(map);


        Storages.USER_STORAGE.addUser(user)
                .flatMapCompletable(ign -> target.handleRequest(routingContext))
                .andThen(Completable.fromAction(() -> {
                    verify(routingContext.request().headers()).add(USERNAME_HEADER, user.getUsername());
                    verify(routingContext.request().headers()).add(USER_TYPE_HEADER, user.getUserType().toString().toLowerCase());
                    async.complete();
                }))
                .subscribe();
    }

}
