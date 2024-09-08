package com.client;

import com.dao.FormResponse;
import com.server.ConstanceWeb;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.SharedData;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import java.util.Map;
import java.util.stream.Collectors;

public class PageServiceV2 extends AbstractVerticle {

    Router router;
    SharedData shareData;
    JWTAuth provider;

    HandlerCookieV2 handlerCookie;

    public PageServiceV2(Router router, SharedData shareData, JWTAuth provider) {
        this.router = router;
        this.shareData = shareData;
        this.provider = provider;
    }

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        handlerCookie = new HandlerCookieV2(vertx).setProvider(provider);

        router.get("/detail/client/info/lo/*").handler(this::handleDetailAccount);
        router.post("/detail/unlock/ai/client/*").handler(this::handleDetailInlockAi);
        router.post("/change/pass/*").handler(this::handleChangePass);

        router.post("/client/logout/*").handler(this::handleLogout);

    }

    /*
    *** json client {"package":"package-basic-ai" , "logType":"C"}
     */
    void handleDetailInlockAi(RoutingContext ctx) {

        handlerCookie.checkCookieClient(ctx, (jsonMsg) -> {
            // err 
            if (jsonMsg.isEmpty() | jsonMsg.getString(ConstanceWeb.valueJWT) == null) {
                ctx.response().setChunked(true)
                        .setStatusCode(302)
                        .putHeader("location", "/login")
                        .sendFile(ConstanceWeb.pathProject.concat("fontend\\login\\index.html"));
                return;
            }

            //  acc invalid
            if (!jsonMsg.getBoolean(ConstanceWeb.resultFind)) {
                ctx.response()
                        .setChunked(true)
                        .setStatusCode(302)
                        .putHeader("location", "/login")
                        .sendFile(ConstanceWeb.pathProject.concat("fontend\\login\\index.html"));
                return;
            }

            System.out.println(">> check jsonMSg " + jsonMsg);
            var jsonClient = ctx.getBodyAsJson();
            System.out.println("check jsClient " + jsonClient);
            var namePackage = jsonClient.getString("package");

            var jsonShareData = shareData.<String, JsonObject>getLocalMap(ConstanceWeb.mapConfigWeb).get("config");
            var jsonPackages = jsonShareData.getJsonArray(namePackage);
            var jsonResponse = new JsonObject()
                    .put("namePackage",
                            handlerCookie.getMapPackage().entrySet().stream()
                                    .filter((t) -> t.getKey().equals(namePackage))
                                    .map((t) -> t.getValue()).collect(Collectors.joining())
                    )
                    .put("walletAddress", jsonShareData.getString("walletAddress"))
                    .put("packages", jsonPackages)
                    .put("contentPay", jsonMsg.getJsonObject("infoClient").getString("email"));

            System.out.println(">> jsonResponse handleDetailInlockAi " + jsonResponse);
            ctx.response().end(
                    new FormResponse().setSuccess(true)
                            .setValue(jsonResponse.toString()).toString()
            );
        });

    }

    void handleDetailAccount(RoutingContext ctx) {
        handlerCookie.checkCookieClient(ctx, (jsonMsg) -> {
            // err 
            if (jsonMsg.isEmpty() | jsonMsg.getString(ConstanceWeb.valueJWT) == null) {
                ctx.response().setChunked(true).setStatusCode(302)
                        .putHeader("location", "/login")
                        .sendFile(ConstanceWeb.pathProject.concat("fontend\\login\\index.html"));
                return;
            }

            //  account invalid
            if (!jsonMsg.getBoolean(ConstanceWeb.resultFind)) {
                ctx.response().setChunked(true).setStatusCode(302)
                        .putHeader("location", "/login")
                        .sendFile(ConstanceWeb.pathProject.concat("fontend\\login\\index.html"));
                return;
            }

            // account valid
            var jsonClient = jsonMsg.getJsonObject("infoClient");// {"email":"123"}
            vertx.eventBus().request("detail.account.chunk", jsonClient, msg -> {

                JsonObject jsonResponse = (JsonObject) msg.result().body();
                var jsonShareData = shareData.<String, JsonObject>getLocalMap(ConstanceWeb.mapConfigWeb).get("config");
                jsonResponse.put("walletAddress", jsonShareData.getString("walletAddress"));

                System.out.println(">> json Response " + jsonResponse);

                ctx.response().send(
                        new FormResponse().setSuccess(true)
                                .setValue(jsonResponse.toString())
                                .toString()
                );
            });
        });
    }

    void handleChangePass(RoutingContext ctx) {
        handlerCookie.checkCookieClient(ctx, (jsonMsg) -> {
            // err 
            if (jsonMsg.isEmpty() | jsonMsg.getString(ConstanceWeb.valueJWT) == null) {
                ctx.response()
                        .setChunked(true)
                        .setStatusCode(302)
                        .putHeader("location", "/login")
                        .sendFile(ConstanceWeb.pathProject.concat("fontend\\login\\index.html"));
                return;
            }

            //  account invalid
            if (!jsonMsg.getBoolean(ConstanceWeb.resultFind)) {
                ctx.response()
                        .setChunked(true)
                        .setStatusCode(302)
                        .putHeader("location", "/login")
                        .sendFile(ConstanceWeb.pathProject.concat("fontend\\login\\index.html"));
                return;
            }

            var jsonClient = ctx.getBodyAsJson();// {"email":"123" ,"passOld":"ksdls","passNew":"12rtret39243i","type":"changeC"}

            vertx.eventBus().request("change.pass.client", jsonClient, (msg) -> {

                var jsonResponse = (JsonObject) msg.result().body();

                var formResponse = new FormResponse();

                formResponse = jsonResponse.getBoolean(ConstanceWeb.resultFind) ? formResponse.setSuccess(true).setValue("pass")
                        : formResponse.setSuccess(false).setValue("something wromg");

                Cookie cookie = Cookie.cookie("cfx", jsonMsg.getString("valueJWT"))
                        .setPath("/")
                        .setMaxAge(60 * 1000 * 60 * 24 * 7);
                ctx.response()
                        .addCookie(cookie)
                        .putHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET")
                        .putHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "POST")
                        .end(formResponse.toString());

            });

        });
    }

    void handleLogout(RoutingContext ctx) {

        handlerCookie.checkCookieClient(ctx, (jsonMsg) -> {
            // err 
            if (jsonMsg.isEmpty() | jsonMsg.getString(ConstanceWeb.valueJWT) == null) {
                ctx.response()
                        .setChunked(true)
                        .setStatusCode(302)
                        .putHeader("location", "/login")
                        .sendFile(ConstanceWeb.pathProject.concat("fontend\\login\\index.html"));
                return;
            }

            //  account invalid
            if (!jsonMsg.getBoolean(ConstanceWeb.resultFind)) {
                ctx.response()
                        .setChunked(true)
                        .setStatusCode(302)
                        .putHeader("location", "/login")
                        .sendFile(ConstanceWeb.pathProject.concat("fontend\\login\\index.html"));
                return;
            }

            var jsonInfoClient = jsonMsg.getJsonObject("infoClient");
            var jsonClient = new JsonObject().put("email", jsonInfoClient.getString("email"))
                    .put("session", jsonInfoClient.getString("session"));

            vertx.eventBus().request("logout.client", jsonClient, msg -> {
                var jsonResponse = (JsonObject) msg.result().body();

                var formResponse = new FormResponse();

                formResponse = jsonResponse.getBoolean(ConstanceWeb.resultFind) ? 
                        formResponse.setSuccess(true).setValue("pass")
                        : formResponse.setSuccess(false).setValue("something wromg");

                Cookie cookie = Cookie.cookie("cfx", jsonMsg.getString("valueJWT"))
                        .setPath("/")
                        .setMaxAge(60 * 1000 * 60 * 24 * 7);
                ctx.response()
                        .addCookie(cookie)
                        .putHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET")
                        .putHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "POST")
                        .end(formResponse.toString());
                        ;
            });
        });

    }

}
