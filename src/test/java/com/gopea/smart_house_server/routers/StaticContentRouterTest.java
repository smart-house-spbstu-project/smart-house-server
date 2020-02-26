package com.gopea.smart_house_server.routers;

import com.gopea.smart_house_server.routers.users.UserRouter;
import io.vertx.ext.unit.TestContext;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.Route;
import io.vertx.reactivex.ext.web.Router;
import org.junit.Test;

import java.util.List;

import static junit.framework.TestCase.assertEquals;

public class StaticContentRouterTest {

    @Test(timeout = 60_000L)
    public void testLoadRouter() {
        Router target = new StaticContentRouter().loadRouter(Vertx.vertx());

        List<Route> list = target.getRoutes();
        assertEquals(1, list.size());
        assertEquals("/info/",list.get(0).getPath());
    }
}
