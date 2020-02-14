package com.gopea.smart_house_server.configs;

import com.gopea.smart_house_server.routers.HelloRouter;
import com.gopea.smart_house_server.routers.Routable;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.Route;
import io.vertx.reactivex.ext.web.Router;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(VertxUnitRunner.class)
public class RouteConfigurationTest {

  @Test
  public void testConfigureRouter() {
    Router router = mock(Router.class);
    Route route = mock(Route.class);

    when(router.route()).thenReturn(route);
    when(route.handler(any())).thenReturn(route);
    when(router.mountSubRouter(anyString(), any())).thenReturn(router);

    List<Routable> routables = new ArrayList<>(RouteConfiguration.ROUTABLES);
    RouteConfiguration.ROUTABLES.clear();
    RouteConfiguration.ROUTABLES.add(new HelloRouter());

    RouteConfiguration.configureRouter(router, Vertx.vertx());

    RouteConfiguration.ROUTABLES.clear();
    RouteConfiguration.ROUTABLES.addAll(routables);

    assertTrue(RouteConfiguration.ROUTABLES.size() > 0);

    verify(route, times(2)).handler(any());
    verify(router, times(1)).mountSubRouter(anyString(), any());
  }
}
