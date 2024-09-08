package com.dao;

import com.dao.admin.DaoAdmin;
import com.dao.client.DaoClientLogin;
import com.dao.client.DaoClientService;
import com.server.ConstanceWeb;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.SharedData;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.mongo.MongoClient;
import java.util.Arrays;
import java.util.List;

public class DaoServer extends AbstractVerticle {

    SharedData shareData;
    JWTAuth provider;

    MongoClient mongoClient;

    public DaoServer(SharedData shareData, JWTAuth provider) {
        this.shareData = shareData;
        this.provider = provider;
    }

    @Override
    public void start(Promise<Void> startPromise) throws Exception {

        createMongo()
                .compose(this::createAdmin)
                .compose(this::deployVerticeDao)
                .result();

    }

    Future<MongoClient> createMongo() {
        return Future.future((promise) -> {
            JsonObject query = new JsonObject().put("db_name", ConstanceWeb.Db_Name).put("connection_string", ConstanceWeb.Db_Uri);
            mongoClient = MongoClient.create(vertx, query);

            promise.complete(mongoClient);

        });
    }

    Future<MongoClient> createAdmin(MongoClient mongoClient) {
        return Future.future(promise -> {
            //info static : auto logAutoMb true ,
            var jsonAdmin = shareData.<String, JsonObject>getLocalMap(ConstanceWeb.mapConfigWeb).get("config").getJsonObject("admin");

            mongoClient.save(ConstanceWeb.Collections_Admin, jsonAdmin, (e) -> promise.complete(mongoClient));
        });
    }

    Future<Void> deployVerticeDao(MongoClient mongClient) {

        DeploymentOptions option = new DeploymentOptions();

        var daoClient = Future.future((promise) -> {
            promise.complete(vertx.deployVerticle(new DaoClientLogin(mongoClient, provider), option));
        });

        var daoClientService = Future.future((promise) -> {
            promise.complete(vertx.deployVerticle(()-> new DaoClientService(mongoClient, provider), option));
        });
        
        var daoAdmin = Future.future((promise) -> {
            promise.complete(vertx.deployVerticle(()-> new DaoAdmin(mongoClient, provider), option));
        });
        
        List<Future> listFut = Arrays.asList(
                daoClient , 
                daoClientService ,
                daoAdmin
        );

        return CompositeFuture.all(listFut).mapEmpty();
    }

}
