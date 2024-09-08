 package com.admin;

import com.dao.FormResponse;
import com.server.ConstanceWeb;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.SharedData;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.authentication.Credentials;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class AdminPage extends AbstractVerticle {

    Router router;
    SharedData shareData;

    JWTAuth provider;
    String nameCookie = "adf";

    public AdminPage(Router router, SharedData shareData) {
        this.router = router;
        this.shareData = shareData;

        this.provider = JWTAuth.create(vertx,
                new JWTAuthOptions()
                        .setJWTOptions(new JWTOptions().setExpiresInMinutes(60 * 24 * 4))
                        .addPubSecKey(new PubSecKeyOptions().setAlgorithm("HS256").setBuffer("phisFbadmin456198"))
        );
    }

    @Override
    public void start(Promise<Void> startPromise) throws Exception {

        router.get("/admin/login/dung/*").handler(StaticHandler.create("fontend\\admin\\login\\")
                .setIndexPage("index.html").setDefaultContentEncoding(StandardCharsets.UTF_8.toString())
        );

        router.post("/log/au/*").handler(this::handleLogin);

        router.get("/admin/info/account/*").order(0).handler(this::checkCookie);
        router.get("/admin/info/account/*").order(1).handler(StaticHandler.create("fontend\\admin\\infoAccount\\")
                .setIndexPage("index.html")
                .setDefaultContentEncoding(StandardCharsets.UTF_8.toString())
        );

        router.post("/api/info/account/*").order(0).handler(this::checkCookie);
        router.post("/api/info/account/*").order(1).handler(this::handleInfoAccount);

        router.post("/api/active/account/*").order(0).handler(this::checkCookie);
        router.post("/api/active/account/*").order(1).handler(this::handleActiveAccount);

        router.post("/search/api/result/*").order(0).handler(this::checkCookie);
        router.post("/search/api/result/*").order(1).handler(this::handleSearchAccount);

        router.post("/delete/api/account/*").order(0).handler(this::checkCookie);
        router.post("/delete/api/account/*").order(1).handler(this::handleDeleteAccount);

    }

    void handleLogin(RoutingContext ctx) {
        JsonObject jsonClient = ctx.getBodyAsJson();
        System.out.println(">> jsonClient " + jsonClient);

        if (jsonClient.containsKey("id") & jsonClient.containsKey("pass")) {

            var jsonAdmin = shareData.<String, JsonObject>getLocalMap(ConstanceWeb.mapConfigWeb).get("config").getJsonObject("admin");
            String id = jsonAdmin.getString("id");
            String pass = jsonAdmin.getString("pass");
            
            System.out.println(
                    ">> check id " + id + " pass " + pass + " - " 
                    +(id.equals(jsonClient.getString("id")) & pass.equals(jsonClient.getString("pass")))
            );

            if (id.equals(jsonClient.getString("id")) & pass.equals(jsonClient.getString("pass"))) {// log success
                String valueCookie = provider.generateToken(jsonClient);
                Cookie cookie = Cookie.cookie(nameCookie, valueCookie)
                        .setPath("/")
                        .setMaxAge(60 * 1000 * 60 * 24 * 7);

                ctx.response().setChunked(true)
                        .addCookie(cookie)
                        .putHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET")
                        .putHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "POST")
                        .end(new FormResponse().setSuccess(true).toString());
                return;
            }

        }
        //log failed
        ctx.request().response().end(new FormResponse().setSuccess(false).setValue("Some thing wrong id or pass").toString());
    }

    void checkCookie(RoutingContext ctx) {
        var setCookie = ctx.request().cookies();

        if (!setCookie.stream().anyMatch((t) -> t.getName().contains(nameCookie))) {// hasn't cookie adf
            ctx.redirect("/admin/login/dung/");
            return;
        }

        String valueCookie = setCookie.stream().filter((t) -> t.getName().contains(nameCookie))
                .map((t) -> t.getValue()).collect(Collectors.joining());

        provider.authenticate(new JsonObject().put("token", valueCookie)).onComplete((asynUser) -> {

            var user = asynUser.result();

            if (asynUser.succeeded()) {
                var jsonAdmin = shareData.<String, JsonObject>getLocalMap(ConstanceWeb.mapConfigWeb).get("config").getJsonObject("admin");
                String id = jsonAdmin.getString("id");
                String pass = jsonAdmin.getString("pass");

                var jsonToken = user.attributes().getJsonObject("accessToken");
                boolean passCookieWeb = id.equals(jsonToken.getString("id")) & pass.equals(jsonToken.getString("pass"));

                if (passCookieWeb) {
                    ctx.next();
                    return;
                }
            }

            ctx.redirect("/admin/login/dung/");
        });

    }

    /*
    ***  display info account
    {countPage:0}
     */
    void handleInfoAccount(RoutingContext ctx) {
        var jsonClient = ctx.getBodyAsJson();

        vertx.eventBus().request("info.account", jsonClient, (msg) -> {
            String response = (String) msg.result().body();
            ctx.request().response().end(response);
        });
    }

    /*
    *** active account client {id:123,pass:adminActive}
     */
    void handleActiveAccount(RoutingContext ctx) {
        var jsonClient = ctx.getBodyAsJson();

        var jsonAdmin = shareData.<String, JsonObject>getLocalMap(ConstanceWeb.mapConfigWeb).get("config");
        String passActive = jsonAdmin.getString("adminActive");

        if (jsonClient.getString("passAdmin").equals(passActive)) {
            vertx.eventBus().request("active.account", jsonClient, (msg) -> {
                String response = (String) msg.result().body();
                ctx.request().response().end(response);
            });
            return;
        }
        ctx.response()
                .end(new FormResponse().setSuccess(false).setValue(new JsonObject().put("err", "wrong pass").toString()).toString());

    }

    /*
    ** search account {valueSearch:"sdska"}
     */
    void handleSearchAccount(RoutingContext ctx) {
        var jsonClient = ctx.getBodyAsJson();

        vertx.eventBus().request("search.account", jsonClient, (msg) -> {
            String response = (String) msg.result().body();
            ctx.request().response().end(response);
        });
    }

    /*
    *** delete account 
     */
    void handleDeleteAccount(RoutingContext ctx) {
        var jsonClient = ctx.getBodyAsJson();

        var jsonAdmin = shareData.<String, JsonObject>getLocalMap(ConstanceWeb.mapConfigWeb).get("config");
        String passActive = jsonAdmin.getString("adminActive");

        if (jsonClient.getString("passAdmin").equals(passActive)) {

            vertx.eventBus().request("delete.account", jsonClient, (msg) -> {
                String response = (String) msg.result().body();
                ctx.request().response().end(response);
            });
            return;
        }
        ctx.response()
                .end(new FormResponse().setSuccess(false).setValue(new JsonObject().put("err", "wrong pass").toString()).toString());
    }
}
