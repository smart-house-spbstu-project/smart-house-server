package com.gopea.smart_house_server.routers;

import com.gopea.smart_house_server.configs.StatusCode;
import com.gopea.smart_house_server.data_base.Storages;
import com.gopea.smart_house_server.routers.users.User;
import com.gopea.smart_house_server.routers.users.UserType;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import static com.gopea.smart_house_server.common.Helpers.*;


public class AuthRouter implements Routable {

    public static final String AUTH_HEADER = "Authorization";

    @Override
    public Router loadRouter(Vertx vertx) {

        Router router = Router.router(vertx);
        router.route("/*").handler(context -> handleRequest(context)
                .subscribe(context::next));

        return router;
    }

    Completable handleRequest(RoutingContext context) {
        String authHeader = context.request().getHeader(AUTH_HEADER);
        Pair<String, String> credentials = getCredentials(authHeader);
        if (credentials == null) {
            context.fail(StatusCode.UNAUTHORISED.getStatusCode());
            return Completable.complete();
        }

        return Storages.USER_STORAGE.getUser(credentials.getLeft())
                .doOnSuccess(user1 -> {
                    if (!user1.checkPassword(credentials.getRight())) {
                        context.fail(StatusCode.UNAUTHORISED.getStatusCode());
                        return;
                    }
                    context.request().headers().add(USERNAME_HEADER, user1.getUsername());
                    context.request().headers().add(USER_TYPE_HEADER, user1.getUserType().toString().toLowerCase());
                })
                .switchIfEmpty(Single.fromCallable(() -> {
                    context.fail(StatusCode.UNAUTHORISED.getStatusCode());
                    return new User("", UserType.USER, "");
                }))
                .ignoreElement()
                .doOnError(error -> handleError(context, error));
    }

    private Pair<String, String> getCredentials(String authHeader) {
        if (StringUtils.isEmpty(authHeader) || !authHeader.startsWith("Basic ")) {
            return null;
        }

        String encodeCredentials = authHeader.replaceFirst("Basic ", "");
        String usernamePassword = new String(Base64.decodeBase64(encodeCredentials.getBytes()));
        String[] credentials = usernamePassword.split(":");
        if (credentials.length != 2) {
            return null;
        }
        String username = credentials[0];
        String password = credentials[1];
        return new ImmutablePair<>(username, password);
    }


}
