package com.dao.client;

import com.server.ConstanceWeb;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.mongo.MongoClient;
import java.util.UUID;

public class DaoClientLogin extends AbstractVerticle {

    MongoClient mongoClient;
    String resultFind = "resultFind",
            resultMongo = "resultMongo";

    JWTAuth provider;

    public DaoClientLogin(MongoClient mongoClient, JWTAuth provider) {
        this.mongoClient = mongoClient;
        this.provider = provider;
    }

    @Override
    public void start(Promise<Void> startPromise) throws Exception {//Message reply all as json

        vertx.eventBus().consumer("cookie.exist.client", this::cookieClientExis);

        vertx.eventBus().consumer("client.create", this::creatClient);

        vertx.eventBus().consumer("client.login", this::loginClient);
    }

    Future<JsonObject> findAccount(JsonObject jsonClient) {
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
    *** check cookie exist
     */
    void cookieClientExis(Message<Object> msg) {
        var jsonClient = (JsonObject) msg.body();

        findAccount(jsonClient)
                .onSuccess((jsonFind) -> {
                    if (jsonFind.getBoolean(resultFind)) {
                        handleJWT(jsonFind, jsonClient, msg);
                        return;
                    }

                    msg.reply(new JsonObject().put(resultFind, false));
                })
                .onFailure((e) -> {
                    msg.reply(new JsonObject().put(resultFind, false));
                })
                .result();
    }

    void handleJWT(JsonObject jsonFind,JsonObject jsonClient ,Message<Object> msg) {

        String sessionFind = jsonFind.getString("session");
        String sessionClient = jsonClient.getString("session");
        System.out.println(">> sessionFind " +sessionFind  + " sessionClient " +sessionClient);
        
        if(sessionClient==null){
            msg.reply(jsonFind.put(resultFind, false));
        }
        boolean checkSession = sessionClient.equals(sessionFind);
        
        if (jsonFind.getBoolean(resultFind) & checkSession) {
            var jsonAuth = new JsonObject().put("email", jsonFind.getString("email"))
                    .put("session", jsonFind.getString("session"))
                    .put("time", System.currentTimeMillis());

            String valueJWT = provider.generateToken(jsonAuth);

            msg.reply(new JsonObject().put("valueJWT", valueJWT));

            return;
        }

        msg.reply(jsonFind.put(resultFind, false));

    }

    /*
    *** create client
    {"userName": "rgr","email": "rgr","pass": "34d04401346e8925e65994dec27914c7","timeCreate": 1724843089710,"active":false,
    "historyPay":[{"packageName":"...basic-ai","datePay":174545565565,"statusPackage":"outDate"},
                  {"packageName":"...basic-ai","datePay":174545565565,"statusPackage":"inDate"}
                 ]
    }
     */
    void creatClient(Message<Object> msg) {
        JsonObject jsonClient = (JsonObject) msg.body();
        jsonClient.put("timeCreate", System.currentTimeMillis())
                .put("active", false)
                .put("historyPay", new JsonArray())
                .put("session", UUID.randomUUID().toString())
                ;

        findAccount(jsonClient)
                .compose((resultFind) -> saveUser(resultFind, jsonClient))
                .onComplete((asynJson) -> {

                    if (asynJson.succeeded()) {
                        var jsonResponse = jsonMsg(asynJson.result(), asynJson.result().getBoolean(resultMongo));
                        msg.reply(jsonResponse);
                        return;
                    }

                    var jsonResponse = jsonMsg(asynJson.result(), false);
                    msg.reply(jsonResponse);

                })
                .result();

    }

    Future<JsonObject> saveUser(JsonObject jsonResultFind, JsonObject jsonClient) {

        return Future.future(promise -> {

            if (jsonResultFind.getBoolean(resultFind)) {
                promise.complete(new JsonObject().put(resultMongo, false));
                return;
            } else {
                
                jsonClient.remove(resultFind);
                
                mongoClient.save(ConstanceWeb.Collections_User, jsonClient, (asynResult) -> {

                    if (asynResult.failed()) {
                        jsonClient.put(resultMongo, false);
                        return;
                    } else {
                        jsonClient.put(resultMongo, true);
                    }

                    promise.complete(jsonClient);
                });
            }

        });
    }

    /*
    *** login client
    {"loginC": 0,"email": "rgr","pass": "34d04401346e8925e65994dec27914c7"}
     */
    void loginClient(Message<Object> msg) {
        JsonObject jsonClient = (JsonObject) msg.body();

        findAccount(jsonClient)
                .onComplete((asynResult) -> handleMsgLogin(asynResult, msg, jsonClient))
                .result();
    }

    void handleMsgLogin(AsyncResult<JsonObject> asynResult, Message<Object> msg, JsonObject jsonClient) {
        if (asynResult.failed()) { // err mongo

            var jsonResponse = jsonMsg(new JsonObject().put("login", false), false);
            msg.reply(jsonResponse);

            return;
        }

        var jsonFind = asynResult.result();
        var accountExist = jsonFind.getBoolean(resultFind);

        if (!accountExist) {    // accouunt not exist

            var jsonResponse = jsonMsg(jsonFind.put("login", false), false);
            msg.reply(jsonResponse);

            return;
        }

        var mailClient = jsonClient.getString("email");
        var passClient = jsonClient.getString("pass");

        if (jsonFind.getString("email").equals(mailClient) & jsonFind.getString("pass").equals(passClient)) { // corect mail & pass
            var jsonAuth = new JsonObject().put("email", jsonFind.getString("email"))
                    .put("session", jsonFind.getString("session"))
                    .put("time", System.currentTimeMillis());

            String valueJWT = provider.generateToken(jsonAuth);

            var jsonResponse = jsonMsg(jsonAuth.put("login", true).put("valueJWT", valueJWT), true);
            msg.reply(jsonResponse);
            return;
        }

        var jsonResponse = jsonMsg(jsonFind.put("login", false), false);
        msg.reply(jsonResponse);

    }

    JsonObject jsonMsg(JsonObject jsonClient, boolean success) {
        var json = jsonClient.put(resultMongo, success);
        return json;
    }
}
