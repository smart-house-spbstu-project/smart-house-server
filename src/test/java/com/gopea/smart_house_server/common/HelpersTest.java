package com.gopea.smart_house_server.common;

import com.gopea.smart_house_server.configs.StatusCode;
import com.gopea.smart_house_server.routers.users.UserType;
import io.reactivex.Maybe;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.reactivex.core.http.HttpServerResponse;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.gopea.smart_house_server.common.Helpers.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;


@RunWith(VertxUnitRunner.class)
public class HelpersTest {

    @Test
    public void testPasswordEncryptionSame() {
        String password = "test password 123 !!";
        byte[] encrypt = Helpers.encryptPassword(password);

        byte[] encrypt1 = Helpers.encryptPassword(password);

        assertArrayEquals(encrypt, encrypt1);
    }

    @Test
    public void testPasswordEncryptionDifferent() {
        String password = "test password 123 !!";
        byte[] encrypt = Helpers.encryptPassword(password);

        byte[] encrypt1 = Helpers.encryptPassword(password + " ");

        assertFalse(Arrays.equals(encrypt, encrypt1));
    }

    @Test
    public void testHandleError() {
        RoutingContext context = mock(RoutingContext.class);
        Throwable throwable = new Exception("");
        Helpers.handleError(context, throwable);

        verify(context).fail(anyInt(), any());
    }

    @Test
    public void testMakeErrorResponseInternalStatusOk() {
        RoutingContext context = mock(RoutingContext.class);
        JsonObject response = new JsonObject().put(INTERNAL_STATUS_KEY, InternalStatus.OK);

        Helpers.makeErrorResponse(context, response);

        assertTrue(isInternalStatusOk(response));
    }

    @Test
    public void testMakeErrorResponseInternalStatusNotOkCodeNotSet() {
        AtomicBoolean isInternalError = new AtomicBoolean(false);

        RoutingContext context = mock(RoutingContext.class);
        HttpServerResponse httpResponse = mock(HttpServerResponse.class);
        when(context.response()).thenReturn(httpResponse);
        when(httpResponse.setStatusCode(anyInt())).thenAnswer(code -> {
            if ((int) code.getArgument(0) == StatusCode.ERROR.getStatusCode()) {
                isInternalError.set(true);
            }
            return httpResponse;
        });
        JsonObject response = new JsonObject().put(INTERNAL_STATUS_KEY, InternalStatus.FAILED);

        Helpers.makeErrorResponse(context, response);

        assertFalse(isInternalStatusOk(response));
        verify(httpResponse).setStatusCode(anyInt());
        verify(httpResponse).end(any(io.vertx.reactivex.core.buffer.Buffer.class));
        assertTrue(isInternalError.get());
    }

    @Test
    public void testMakeErrorResponseInternalStatusNotOkCodeSet() {
        AtomicBoolean isBadRequest = new AtomicBoolean(false);

        RoutingContext context = mock(RoutingContext.class);
        HttpServerResponse httpResponse = mock(HttpServerResponse.class);
        when(context.response()).thenReturn(httpResponse);
        when(httpResponse.setStatusCode(anyInt())).thenAnswer(code -> {
            if ((int) code.getArgument(0) == StatusCode.BAD_REQUEST.getStatusCode()) {
                isBadRequest.set(true);
            }
            return httpResponse;
        });

        JsonObject response = new JsonObject().put(INTERNAL_STATUS_KEY, InternalStatus.FAILED);
        response.put(EXTERNAL_STATUS_KEY, StatusCode.BAD_REQUEST.getStatusCode());


        Helpers.makeErrorResponse(context, response);

        assertFalse(isInternalStatusOk(response));
        verify(httpResponse).setStatusCode(anyInt());
        verify(httpResponse).end(any(io.vertx.reactivex.core.buffer.Buffer.class));
        assertTrue(isBadRequest.get());
    }

    @Test
    public void testMakeRestResponseFromResponse() {
        AtomicBoolean isSameCode = new AtomicBoolean(false);
        RoutingContext context = mock(RoutingContext.class);
        HttpServerResponse httpResponse = mock(HttpServerResponse.class);

        when(context.response()).thenReturn(httpResponse);
        when(httpResponse.setStatusCode(anyInt())).thenAnswer(code -> {
            if ((int) code.getArgument(0) == StatusCode.SUCCESS.getStatusCode()) {
                isSameCode.set(true);
            }
            return httpResponse;
        });

        JsonObject response = new JsonObject().put(INTERNAL_STATUS_KEY, InternalStatus.OK)
                .put(EXTERNAL_STATUS_KEY, StatusCode.SUCCESS.getStatusCode());

        Helpers.makeRestResponseFromResponse(context, response, null);

        verify(httpResponse).end();
        verify(httpResponse).setStatusCode(anyInt());
        assertTrue(isSameCode.get());
    }

    @Test
    public void testMakeRestResponseFromResponse2Params() {
        AtomicBoolean isSameCode = new AtomicBoolean(false);
        RoutingContext context = mock(RoutingContext.class);
        HttpServerResponse httpResponse = mock(HttpServerResponse.class);

        when(context.response()).thenReturn(httpResponse);
        when(httpResponse.setStatusCode(anyInt())).thenAnswer(code -> {
            if ((int) code.getArgument(0) == StatusCode.SUCCESS.getStatusCode()) {
                isSameCode.set(true);
            }
            return httpResponse;
        });

        JsonObject response = new JsonObject().put(INTERNAL_STATUS_KEY, InternalStatus.OK)
                .put(EXTERNAL_STATUS_KEY, StatusCode.SUCCESS.getStatusCode());

        Helpers.makeRestResponseFromResponse(context, response);

        verify(httpResponse).end();
        verify(httpResponse).setStatusCode(anyInt());
        assertTrue(isSameCode.get());
    }

    @Test
    public void testMakeErrorRestResponse() {
        RoutingContext context = mock(RoutingContext.class);
        HttpServerResponse httpResponse = mock(HttpServerResponse.class);

        when(context.response()).thenReturn(httpResponse);

        Helpers.makeErrorRestResponse(context, StatusCode.NO_CONTENT, null);

        verify(httpResponse).end(any(Buffer.class));
        verify(httpResponse).setStatusCode(anyInt());
    }

    @Test
    public void testCheckAdminRightsSuccess() {
        RoutingContext context = mock(RoutingContext.class);
        HttpServerRequest request = mock(HttpServerRequest.class);


        when(context.request()).thenReturn(request);
        when(request.getHeader(anyString())).thenReturn(UserType.ADMIN.toString());

        boolean result = Helpers.checkAdminRights(context);

        assertTrue(result);
        verify(request).getHeader(anyString());
    }

    @Test
    public void testCheckAdminRightsFail() {

        RoutingContext context = mock(RoutingContext.class);
        HttpServerRequest request = mock(HttpServerRequest.class);
        HttpServerResponse httpResponse = mock(HttpServerResponse.class);

        when(context.response()).thenReturn(httpResponse);
        when(context.request()).thenReturn(request);
        when(request.getHeader(anyString())).thenReturn(UserType.USER.toString());


        boolean result = Helpers.checkAdminRights(context);

        assertFalse(result);
        verify(request).getHeader(anyString());
        verify(context).fail(anyInt());
    }

    @Test
    public void testGetBodySuccess() {
        RoutingContext context = mock(RoutingContext.class);
        JsonObject body = new JsonObject().put("test", "value");

        when(context.getBodyAsJson()).thenReturn(body);

        JsonObject result = Helpers.getBody(context);

        assertEquals(body, result);
    }

    @Test
    public void testGetBodyFail() {
        RoutingContext context = mock(RoutingContext.class);
        when(context.getBodyAsJson()).thenThrow(new RuntimeException());

        JsonObject result = getBody(context);

        assertNull(result);
        verify(context).fail(anyInt(), any());
    }

    @Test
    public void testHandleEmptyCaseSuccess() {
        RoutingContext context = mock(RoutingContext.class);
        HttpServerResponse httpResponse = mock(HttpServerResponse.class);

        when(context.response()).thenReturn(httpResponse);

        Maybe<String> var = Maybe.just("test");

        Maybe<String> result = handleEmptyCase(context, var);

        assertEquals(var.blockingGet(), result.blockingGet());
    }

    @Test(timeout = 60000)
    public void testHandleEmptyCaseFail(TestContext testContext) {
        AtomicBoolean isSameCode = new AtomicBoolean(false);

        RoutingContext context = mock(RoutingContext.class);
        HttpServerResponse httpResponse = mock(HttpServerResponse.class);

        when(context.response()).thenReturn(httpResponse);
        when(httpResponse.setStatusCode(anyInt())).thenAnswer(code -> {
            if ((int) code.getArgument(0) == StatusCode.NOT_FOUND.getStatusCode()) {
                isSameCode.set(true);
            }
            return httpResponse;
        });

        Maybe<String> var = Maybe.empty();
        final Async async = testContext.async();
        handleEmptyCase(context, var)
                .isEmpty()
                .map(isEmpty -> {
                    async.complete();
                    return isEmpty;
                })
                .subscribe();

        verify(httpResponse).end(any(Buffer.class));
        verify(httpResponse).setStatusCode(anyInt());
    }

    @Test
    public void testGetEnumNull() {
        assertNull(getEnum(null, StatusCode.class));
    }

    @Test
    public void testGetEnumNotPresentedValue() {
        assertNull(getEnum("OK", StatusCode.class));
    }

    @Test
    public void testGetEnumSuccess() {
        StatusCode code = StatusCode.NOT_FOUND;

        StatusCode result = getEnum(code.name().toLowerCase(), StatusCode.class);
        assertEquals(code, result);
    }

    @Test
    public void testIsInternalStatusOkNull() {
        assertFalse(Helpers.isInternalStatusOk(new JsonObject()));
    }

    @Test
    public void testIsInternalStatusOkFalse() {
        assertFalse(Helpers.isInternalStatusOk(new JsonObject().put(INTERNAL_STATUS_KEY, InternalStatus.FAILED)));
    }

    @Test
    public void testIsInternalStatusOkSuccess() {
        assertTrue(Helpers.isInternalStatusOk(new JsonObject().put(INTERNAL_STATUS_KEY, InternalStatus.OK)));
    }

    @Test
    public void isEqualsWithAnyEmpty() {
        assertFalse(Helpers.isEqualsWithAny(new Object()));
    }

    @Test
    public void isEqualsWithAnyFalse() {
        assertFalse(Helpers.isEqualsWithAny(new Object(), StatusCode.BAD_REQUEST));
    }

    @Test
    public void isEqualsWithAnyTrue() {
        assertTrue(Helpers.isEqualsWithAny(StatusCode.SUCCESS, StatusCode.BAD_REQUEST, StatusCode.SUCCESS,
                                            new Object(), InternalStatus.OK));
    }

  @Test
  public void testCreateResponseJson() {
    JsonObject response = createResponseJson(InternalStatus.OK, StatusCode.NO_CONTENT, new JsonObject());

    assertTrue(isInternalStatusOk(response));
    assertEquals(StatusCode.NO_CONTENT.getStatusCode(), response.getInteger(EXTERNAL_STATUS_KEY).intValue());
  }
}
