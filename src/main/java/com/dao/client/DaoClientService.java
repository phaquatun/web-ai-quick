package com.dao.client;

import com.server.ConstanceWeb;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.mongo.MongoClient;
import java.util.UUID;

public class DaoClientService extends AbstractVerticle {

    MongoClient mongoClient;
    JWTAuth provider;

    String resultFind = "resultFind",
            resultMongo = "resultMongo";

    public DaoClientService(MongoClient mongoClient, JWTAuth provider) {
        this.mongoClient = mongoClient;
        this.provider = provider;
    }

    @Override
    public void start(Promise<Void> startPromise) throws Exception {

        vertx.eventBus().consumer("detail.account.chunk", this::handleDetailAccont);
        vertx.eventBus().consumer("change.pass.client", this::handleChangePass);
        vertx.eventBus().consumer("logout.client", this::handleLogOut);

    }

    private Future<JsonObject> findAccount(JsonObject jsonClient) {
        return Future.future((promise) -> {

            var emailClient = jsonClient.getString("email");
            JsonObject query = new JsonObject().put("email", emailClient);

            mongoClient.findOne(ConstanceWeb.Collections_User, query, null, (asynResult) -> {
                if (asynResult.failed()) { // err find mongodb
                    jsonClient.put(resultFind, false);
                    promise.complete(jsonClient);
                    return;
                }

                if (asynResult.result() == null) { //can not find account 
                    jsonClient.put(resultFind, false);
                    promise.complete(jsonClient);
                    return;
                }

                if (asynResult.result().getString("email").equals(emailClient)) { // find account 
                    var jsonFind = asynResult.result().put(resultFind, true);
                    promise.complete(jsonFind);
                }
            });
        });
    }

    /*
    *** handle Detail account
     */
    private void handleDetailAccont(Message<Object> msg) {

        var jsonClient = (JsonObject) msg.body();

        findAccount(jsonClient)
                .onSuccess((jsonFind) -> {
                    jsonFind.remove("pass");
                    jsonFind.remove("_id");
                    jsonFind.remove("timeCreate");

                    // json find {"userName":"ty","email":"ty","active":false,"historyPay":[],"resultFind":true}
                    msg.reply(jsonFind);
                })
                .onFailure((e) -> {
                    e.printStackTrace();
                    msg.reply(jsonClient.put(resultFind, false));
                })
                .result();

    }

    /*
    *** handle Detail account
     */
    void handleChangePass(Message<Object> msg) {

        var jsonClient = (JsonObject) msg.body();
        jsonClient.remove("type");

        var queryFind = new JsonObject().put("email", jsonClient.getString("email"))
                .put("pass", jsonClient.getString("passOld"));

        findAccount(queryFind)
                .compose(jsonFind -> updatePass(jsonFind, jsonClient))
                .onSuccess((jsonUpdate) -> msg.reply(jsonUpdate))
                .onFailure((e) -> {
                    e.printStackTrace();
                    msg.reply(jsonClient.put(resultFind, false));
                })
                .result();

    }

    Future<JsonObject> updatePass(JsonObject jsonFind, JsonObject jsonClient) {
        return Future.future((prommise) -> {

            if (!jsonFind.getBoolean(resultFind)) {
                prommise.complete(jsonClient.put(resultFind, false));
                return;
            }

            if (!jsonFind.getString("pass").equals(jsonClient.getString("passOld"))) {
                prommise.complete(jsonClient.put(resultFind, false));
                return;
            }

            var query = new JsonObject().put("email", jsonFind.getString("email"));
            var update = new JsonObject().put("$set",
                    new JsonObject().put("pass", jsonClient.getString("passNew"))
                            .put("session", UUID.randomUUID().toString())
            );

            mongoClient.updateCollection(ConstanceWeb.Collections_User, query, update, (asynResult) -> {
                if (asynResult.failed()) {
                    prommise.complete(jsonClient.put(resultFind, false));
                    return;
                }

                prommise.complete(jsonClient.put(resultFind, true));

            });

        });
    }

    /*
    *** handle log out
     */
    void handleLogOut(Message<Object> msg) {
        var jsonClient = (JsonObject) msg.body();
        var queryFind = new JsonObject().put("email", jsonClient.getString("email"));
        System.out.println(">> queryFind " +queryFind);

        findAccount(queryFind)
                .compose((jsonFind) -> logout(jsonFind, jsonClient, msg))
                .onSuccess((jsonUpdate) -> msg.reply(jsonUpdate))
                .onFailure((e) -> {
                    e.printStackTrace();
                    msg.reply(jsonClient.put(resultFind, false));
                })
                .result();
    }

    Future<JsonObject> logout(JsonObject jsonFind, JsonObject jsonClient, Message<Object> msg) {
        return Future.future((promise) -> {

            if (!jsonFind.getBoolean(resultFind)) {
                promise.complete(jsonFind);
                return;
            }
            System.out.println("> check jsonFind " + jsonFind);
            var query = new JsonObject().put("email", jsonFind.getString("email"));
            var update = new JsonObject().put("$set",
                    new JsonObject().put("session", UUID.randomUUID().toString())
            );

             mongoClient.updateCollection(ConstanceWeb.Collections_User, query, update, (asynResult) -> {
                if (asynResult.failed()) {
                    promise.complete(jsonClient.put(resultFind, false));
                    return;
                }

                promise.complete(jsonClient.put(resultFind, true));

            });
        });
    }
}
