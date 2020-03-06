package com.gopea.smart_house_server.routers;

import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.reactivex.core.http.HttpServerResponse;
import io.vertx.reactivex.ext.web.RoutingContext;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

public final class Common {

    public static RoutingContext createContext() {
        RoutingContext context = mock(RoutingContext.class);
        HttpServerResponse response = mock(HttpServerResponse.class);
        HttpServerRequest request = mock(HttpServerRequest.class);

        when(context.response()).thenReturn(response);
        when(context.request()).thenReturn(request);

        return context;
    }
}
