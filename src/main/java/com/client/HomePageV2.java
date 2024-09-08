package com.client;

import com.dao.FormResponse;
import com.server.ConstanceWeb;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;

public class HomePageV2 extends AbstractVerticle {

    Router router;
    JWTAuth provider;

    HandlerCookieV2 handlerCookie;
    String resultMongo = "resultMongo";

    public HomePageV2(Router router, JWTAuth provider) {
        this.router = router;
        this.provider = provider;

    }

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        handlerCookie = new HandlerCookieV2(getVertx()).setProvider(provider);

        router.get("/").handler(this::handleHomePage);
        router.get("/homepage/:name").handler(this::handleFileNameHome);

        router.get("/login").handler(this::handleStaticLogin);
        router.get("/yk/:name").handler(this::handleFileNameLogin);

        router.post("/check/login/client/*").handler(this::handleLogIn);
        router.post("/regist/account/client/*").handler(this::handleCreate);

        
        router.post("/detail/unlock/ai/*").handler((e) -> {
        });
    }

    /*
        value jsonMsg {"valueJWT":"ejdfkd...","resultFind":true/false}
    
        note 3 way use redirect : sendFile(Path) or setStatusCode(302).putHeader("location", "/login")... 
        ctx.response().sendFile(ConstanceWeb.pathProject.concat("fontend\\login\\index.html"));
        ctx.response().putHeader("location", "/login").setStatusCode(302).end();
        ctx.response().putHeader("location", "/login").setStatusCode(302).sendFile(ConstanceWeb.pathProject.concat("fontend\\login\\index.html"));
    
     */
    void handleHomePage(RoutingContext ctx) {

        handlerCookie.checkCookieClient(ctx, (jsonMsg) -> {
            // err 
            if (jsonMsg.isEmpty() | jsonMsg.getString(ConstanceWeb.valueJWT)==null) {
                ctx.response()
                        .setChunked(true)
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

            // have account => refresh cookie expired 
            Cookie cookie = Cookie.cookie(handlerCookie.getNameCookie(), jsonMsg.getString(ConstanceWeb.valueJWT))
                    .setPath("/")
                    .setMaxAge(60 * 1000 * 60 * 24 * 3);

            ctx.response().setChunked(true)
                    .addCookie(cookie)
                    .putHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET")
                    .putHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "POST")
                    .sendFile(ConstanceWeb.pathProject.concat("fontend\\homepage\\index.html"));
        });

    }

    void handleFileNameHome(RoutingContext ctx) {
        String nameFile = ctx.pathParam("name");
        ctx.response().sendFile(ConstanceWeb.pathProject.concat("fontend\\homepage\\") + nameFile);
    }

    void handleStaticLogin(RoutingContext ctx) {
        handlerCookie.checkCookieClient(ctx, (jsonMsg) -> {
            // err 
            if (jsonMsg.isEmpty() | jsonMsg.getString(ConstanceWeb.valueJWT)==null) {
                 ctx.response().sendFile(ConstanceWeb.pathProject.concat("fontend\\login\\index.html"));
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

            Cookie cookie = Cookie.cookie(handlerCookie.getNameCookie(), jsonMsg.getString(ConstanceWeb.valueJWT))
                    .setPath("/")
                    .setMaxAge(60 * 1000 * 60 * 24 * 3);

            ctx.response().setChunked(true)
//                    .addCookie(cookie)
                    .putHeader("location", "/")
                    .setStatusCode(302)
                    .putHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET")
                    .end();
        });
    }

    void handleFileNameLogin(RoutingContext ctx) {
        String nameFile = ctx.pathParam("name");
        ctx.response().sendFile(ConstanceWeb.pathProject.concat("fontend\\login\\") + nameFile);
    }

    /*
    *** login {"email":"th","pass":"1fdc0f893412ce55f0d2811821b84d3b","loginC":0}
     */
    void handleLogIn(RoutingContext ctx) {
        var jsonClient = ctx.getBodyAsJson();

        vertx.eventBus().request("client.login", jsonClient, (msg) -> {
            JsonObject jsonMsg = (JsonObject) msg.result().body();

            boolean login = jsonMsg.getBoolean("login");
            boolean passMongo = jsonMsg.getBoolean(this.resultMongo);

            if (passMongo & login) {

                var formResponse = new FormResponse().setSuccess(true).setValue("pass").toString();

                Cookie cookie = Cookie.cookie(handlerCookie.getNameCookie(), jsonMsg.getString("valueJWT"))
                        .setPath("/")
                        .setMaxAge(60 * 1000 * 60 * 24 * 7);

                ctx.response().setChunked(true)
                        .addCookie(cookie)
                        .putHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET")
                        .putHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "POST")
                        .end(formResponse);

                return;
            }

            if (!passMongo | !login) {
                var formResponse = new FormResponse().setSuccess(false).setValue("some thing wrong , contact admin").toString();
                ctx.response().end(formResponse);
            }

        });
    }

    /*
    *** create account
    {"userName": "rgr","email": "rgr","pass": "34d04401346e8925e65994dec27914c7"}
     */
    void handleCreate(RoutingContext ctx) {
        var jsonClient = ctx.getBodyAsJson();

        vertx.eventBus().request("client.create", jsonClient, (msg) -> {

            JsonObject jsonMsg = (JsonObject) msg.result().body();

            boolean success = jsonMsg.getBoolean(resultMongo);
            String valueResponse = success ? "pass" : "err";
            jsonMsg.remove(resultMongo);

            var formResponse = new FormResponse().setSuccess(success).setValue(valueResponse).toString();
            ctx.response().end(formResponse);

        });
    }

    
}
