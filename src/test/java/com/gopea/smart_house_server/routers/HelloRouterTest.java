package com.gopea.smart_house_server.routers;

import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.Route;
import io.vertx.reactivex.ext.web.Router;
import org.junit.Test;

import java.util.List;

import static junit.framework.TestCase.assertEquals;

public class HelloRouterTest {

    @Test(timeout = 60_000L)
    public void testLoadRouter() {
        Router target = new HelloRouter().loadRouter(Vertx.vertx());

        List<Route> list = target.getRoutes();
        assertEquals(1, list.size());
        assertEquals("/rest", list.get(0).getPath());
    }
}
