package com.client;

import com.dao.FormResponse;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.RoutingContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(chain = true)
public class HandlerCookieV2 {

    @Setter
    JWTAuth provider;

    String resultMongo = "resultMongo", resultFind = "resultFind";

    @Getter
    String nameCookie = "cfx";

    Vertx vertx;
    
    @Getter
    Map<String, String> mapPackage = new HashMap<>();

    public HandlerCookieV2(Vertx vertx) {
        this.vertx = vertx;
        
        mapPackage.put("package-basic-ai", "Ai Phân Tích Cơ Bản");
        mapPackage.put("package-advanded-ai", "Ai Phân Tích Nâng Cao");
        mapPackage.put("package-special-ai", "Ai Phân Tích Chuyên Sâu");
        
    }

    public interface CheckCookie {

        void handle(JsonObject jsonMsg);

    }

    public HandlerCookieV2 checkCookieClient(RoutingContext ctx, CheckCookie checkCookie) {

        futCheckCookie(ctx)
                .onSuccess((jsonMsg) -> checkCookie.handle(jsonMsg))
                .onFailure((e) -> ctx.response().putHeader("location", "/login").setStatusCode(302).end())
                .result();

        return this;
    }

    public Future<JsonObject> futCheckCookieClient(RoutingContext ctx, CheckCookie checkCookie) {
        return futCheckCookie(ctx)
                .compose((jsonMsg) -> futHandlerCookie(ctx, checkCookie, jsonMsg ));
    }

    Future<JsonObject> futCheckCookie(RoutingContext ctx) {
        return Future.future((promise) -> {

            Set<Cookie> setCookie = ctx.request().cookies();

            if (!setCookie.stream().anyMatch((t) -> t.getName().contains(nameCookie))) {// hasn't cookie cfx ==> redirect
                var jsonMsg = new JsonObject().put(resultFind, false);
                promise.complete(jsonMsg);

                return;
            }
            String valueCookie = setCookie.stream().filter((t) -> t.getName().contains(nameCookie))
                    .map((t) -> t.getValue()).collect(Collectors.joining());

            provider.authenticate(new JsonObject().put("token", valueCookie), (asynUser) -> {// check value exis in dao . time expire

                var user = asynUser.result();
                if (asynUser.succeeded()) {

                    if (user.expired()) {// expire time ==> redirect
                        System.out.println(">> check value exis in dao : expired");
                        promise.complete(new JsonObject().put(resultFind, false));
                        return;
                    }

                    // user.attributes  {"accessToken":{"email":"th","time":1724988838869,"iat":1724988838,"exp":1725334438},"exp":1725334438,"iat":1724988838,"rootClaim":"accessToken"}
                    var jsonToken = user.attributes().getJsonObject("accessToken");
                    var jsonClient = new JsonObject().put("email", jsonToken.getString("email"))
                            .put("session", jsonToken.getString("session"));

                    vertx.eventBus().request("cookie.exist.client", jsonClient)
                            .onSuccess((asyncMsg) -> {
                                var jsonMsg = (JsonObject) asyncMsg.body();
                                promise.complete(jsonMsg.put(resultFind, true).put("infoClient", jsonClient));
                            })
                            .onFailure((e) -> {
                                var jsonMsg = new JsonObject().put(resultFind, false);
                                promise.complete(jsonMsg.put(resultFind, true));
                            });

                } else {    //  ==> redirect
                    var jsonMsg = new JsonObject().put(resultFind, false);
                    promise.complete(jsonMsg);
                }

            });

        });
    }

    Future<JsonObject> futHandlerCookie(RoutingContext ctx, CheckCookie checkCookie , JsonObject jsonMsg) {
        return Future.future((promise) -> {
            checkCookie.handle(jsonMsg);
            promise.complete(jsonMsg);
        });
    }
}
