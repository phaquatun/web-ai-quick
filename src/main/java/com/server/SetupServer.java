package com.server;

import com.admin.AdminPage;
import com.client.HomePageV2;
import com.client.PageServiceV2;
import com.dao.DaoServer;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.shareddata.SharedData;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.sstore.SessionStore;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class SetupServer extends AbstractVerticle {

    Router router;
    SharedData shareData;
    JWTAuth provider;

    String host;
    int port;

    @Override
    public void start(Promise<Void> startPromise) throws Exception {

        createRouter()
                .compose(this::createHttpServer)
                .compose(this::deployVerticles)
                .onComplete((asynResult) -> {

                    if (asynResult.failed()) {
                        System.out.println(">> err deploy web ");
                        asynResult.cause().printStackTrace();
                        return;
                    }

                    if (asynResult.succeeded()) {
                        System.out.println(">> success deploy web " + host + " : " + port);
                    }

                });

    }

    Future<Router> createRouter() {

        return Future.future((promise) -> {

            router = Router.router(vertx);

            router.route().handler(BodyHandler.create());

            SessionStore sessionCookie = LocalSessionStore.create(vertx, "cobn");
            router.route("/*").handler(SessionHandler.create(sessionCookie)
                    .setCookieHttpOnlyFlag(true)
                    .setSessionCookieName("vcard")
                    .setCookieMaxAge(60 * 24 * 4 * 1000).
                    setMinLength(30)
            );

            router.route().handler(CorsHandler.create()
                    .allowedMethod(io.vertx.core.http.HttpMethod.GET)
                    .allowedMethod(io.vertx.core.http.HttpMethod.POST)
                    .allowedMethod(io.vertx.core.http.HttpMethod.OPTIONS)
                    .allowedHeader("Access-Control-Allow-Credentials")
                    .allowedHeader("Access-Control-Allow-Headers")
                    .allowedHeader("Content-Type"));

            shareData = vertx.sharedData();

            promise.complete(router);
        });
    }

    /*
    *** create server with
     */
    Future<Router> createHttpServer(Router router) {
        return handleConfig(router)
                .compose(this::handleCreateServer);
    }

    Future<JsonObject> handleConfig(Router router) {
        return Future.future((promise) -> {

            vertx.fileSystem().readFile(ConstanceWeb.pathConfig, (asynBuff) -> {

                JsonObject json = new JsonObject(asynBuff.result().toString(StandardCharsets.UTF_8));

                shareData.<String, JsonObject>getLocalMap(ConstanceWeb.mapConfigWeb).put("config", json);

                promise.complete(json);

            });

        });
    }

    Future<Router> handleCreateServer(JsonObject json) {
        return Future.future((promise) -> {
            provider = JWTAuth.create(vertx, new JWTAuthOptions().setJWTOptions(new JWTOptions().setExpiresInMinutes(60 * 24 * 4))
                    .addPubSecKey(new PubSecKeyOptions().setAlgorithm("HS256").setBuffer("phisFbadmin456198"))
            );

            host = json.getString("host");
            port = json.getInteger("port");

            HttpServerOptions option = new HttpServerOptions();
            boolean useSslTrial = json.getString("sslFileTrial").length() != 0;
            if (useSslTrial) {
                String nameFile = json.getString("sslFileTrial");
                String pathGetTrial = "/.well-known/pki-validation/" + nameFile;

                router.get(pathGetTrial).handler(
                        StaticHandler.create("ssltrial\\")
                                .setIndexPage(nameFile)
                                .setDefaultContentEncoding(StandardCharsets.UTF_8.toString())
                );
            }

            if (port == 443) {
                String pathCert = ConstanceWeb.pathFolderCertBS;

                option.setUseAlpn(true).setSsl(true)
                        .setKeyCertOptions(new PemKeyCertOptions()
                                .setCertPath(pathCert + "certificate.crt")
                                .setKeyPath(pathCert + "private.key")
                        );
            }

            vertx.createHttpServer(option).requestHandler(router).listen(port);

            promise.complete(router);

        });
    }

    /*
    *** deploy verticles  
     */
    Future<Router> deployVerticles(Router router) {

        int core = Runtime.getRuntime().availableProcessors() * 2;
        DeploymentOptions option = new DeploymentOptions().setHa(true)
                .setInstances(core)
                .setWorker(true).setWorkerPoolSize(100);

        DeploymentOptions optionMongo = new DeploymentOptions().setHa(true)
                .setInstances(1)
                .setWorker(true).setWorkerPoolSize(100);

        var pageService = Future.future(promise -> {
            var deploy = vertx.deployVerticle(() -> new PageServiceV2(router, shareData, provider), option);
            promise.complete(deploy);
        });

        var futDaoServer = Future.future(promise -> {
            var deploy = vertx.deployVerticle(() -> new DaoServer(shareData, provider), optionMongo).result();
            promise.complete(deploy);
        });

        var futHomePage = Future.future(promise -> {
            var deploy = vertx.deployVerticle(() -> new HomePageV2(router, provider), option);
            promise.complete(deploy);
        });

        var futAdminPage  = Future.future(promise -> {
            var deploy = vertx.deployVerticle(() -> new AdminPage(router, shareData), option);
            promise.complete(deploy);
        });
        
        List<Future> listFut = Arrays.asList(
                pageService,
                futDaoServer,
                futHomePage,
                futAdminPage
        );

        return CompositeFuture.all(listFut).mapEmpty();
    }
}
