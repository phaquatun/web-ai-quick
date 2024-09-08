

package com.dao.admin;

import com.dao.FormResponse;
import com.server.ConstanceWeb;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.SharedData;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.RoutingContext;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringEscapeUtils;



public class DaoAdmin extends AbstractVerticle {
    
    MongoClient mongoClient;
    SharedData shareData ;
    JWTAuth provider ;
    
    String successFindAcc = "successFindAcc";

    public DaoAdmin(MongoClient mongoClient, JWTAuth provider) {
        this.mongoClient = mongoClient;
        this.shareData = shareData;
        this.provider = provider;
    }

      @Override
    public void start(Promise<Void> startPromise) throws Exception {

        vertx.eventBus().consumer("info.account", this::handleInfoAccount);

        vertx.eventBus().consumer("active.account", this::handleActiveAcount);

        vertx.eventBus().consumer("search.account", this::handleSearchAccount);

        vertx.eventBus().consumer("delete.account", this::handleDeleteAccount);

    }

    void handleInfoAccount(Message<Object> msg) {
        JsonObject jsonMsg = (JsonObject) msg.body();

        countAcc().compose((t) -> findAccount(t, jsonMsg))
                .compose((t) -> msgReply(t, msg))
                .result();
    }

    /*
    *** 
     */
    Future<Long> countAcc() {
        return mongoClient.count(ConstanceWeb.Collections_User, new JsonObject());
    }

    Future<JsonObject> findAccount(Long countDoc, JsonObject jsonMsg) {
        return Future.future((promise) -> {

            int limtCountClient = countDoc.intValue() % 20 == 0 ? countDoc.intValue() / 20 : (countDoc.intValue() / 20) + 1;

            int countPage = jsonMsg.getInteger("count");
            int skip = countDoc.intValue() - ((countPage + 1) * 20);

            int lim = skip < 0 ? countDoc.intValue() % 20 : 20;
            skip = skip < 0 ? 0 : skip;

            FindOptions option = new FindOptions().setLimit(lim).setSkip(skip);

            mongoClient.findWithOptions(ConstanceWeb.Collections_User, new JsonObject(), option, (asynResult) -> {
                if (asynResult.failed()) {
                    promise.complete(new JsonObject().put(successFindAcc, false));
                    return;
                }

                List<JsonObject> listJson = asynResult.result();

                var jsonRes = new JsonObject().put("valueAccount", new JsonArray(listJson))
                        .put("limtCountClient", limtCountClient);

                promise.complete(jsonRes);

            });

        });
    }

    Future<Void> msgReply(JsonObject jsonMsg, Message<Object> msg) {
        return Future.future((promise) -> {

            boolean success = jsonMsg.containsKey(successFindAcc) ? false : true;

            msg.reply(new FormResponse().setSuccess(success).setValue(jsonMsg.toString()).toString());
            promise.complete();
        });
    }

    /*
    *** active account
     */
    void handleActiveAcount(Message<Object> msg) {
        findAndUpdateAccount(msg).result();
    }

    Future<JsonObject> findAndUpdateAccount(Message<Object> msg) {
        return Future.future((promise) -> {
            var jsonMsg = (JsonObject) msg.body();

            var query = new JsonObject().put("id", StringEscapeUtils.unescapeHtml3(jsonMsg.getString("id")));
            var update = new JsonObject().put("$set",
                    new JsonObject().put("active", "Actived✓").put("timeActive", System.currentTimeMillis())
            );

            mongoClient.findOneAndUpdate(ConstanceWeb.Collections_User, query, update, (asynResult) -> {

                if (asynResult.failed()) {
                    
                    asynResult.cause().printStackTrace();
                    msg.reply(new FormResponse().setSuccess(false).setValue("some thing wring id to update").toString());
                    promise.complete(asynResult.result());
                    return;
                    
                }

                msg.reply(new FormResponse().setSuccess(true).setValue(asynResult.result().toString()).toString());
                promise.complete(asynResult.result());
            });

        });
    }

    /*
    *** search account 
     */
    void handleSearchAccount(Message<Object> msg) {
        var jsonMsg = (JsonObject) msg.body();

        findAccountsSearch(jsonMsg)
                .compose(this::findUserNamesSearch)
                .onComplete((asynResult) -> {
                    var jsonRes = asynResult.result();
                    msg.reply(new FormResponse().setSuccess(true).setValue(jsonRes.toString()).toString());
                })
                .result();
    }

    Future<JsonObject> findAccountsSearch(JsonObject jsonMsg) {
        return Future.future((promise) -> {

            String valueSearch = jsonMsg.getString("valueSearch");
            JsonObject query = new JsonObject().put("id", new JsonObject().put("$regex", valueSearch));

            mongoClient.find(ConstanceWeb.Collections_User, query, (asynResult) -> {
                if (asynResult.failed()) {
                    asynResult.cause().printStackTrace();
                    promise.complete(jsonMsg.put(successFindAcc, false));
                    return;
                }

                List<JsonObject> results = asynResult.result();
                if (results.size() == 0) {
                    promise.complete(jsonMsg.put(successFindAcc, true));
                    return;
                }

                jsonMsg.put(successFindAcc, true).put("valueIds", new JsonArray(results));
                promise.complete(jsonMsg);
            });

        });
    }

    Future<JsonObject> findUserNamesSearch(JsonObject jsonMsg) {
        return Future.future((promise) -> {

            String valueSearch = jsonMsg.getString("valueSearch");
            JsonObject query = new JsonObject().put("username", new JsonObject().put("$regex", valueSearch));

            mongoClient.find(ConstanceWeb.Collections_User, query, (asynResult) -> {
                if (asynResult.failed()) {
                    asynResult.cause().printStackTrace();
                    promise.complete(jsonMsg);
                    return;
                }
                List<JsonObject> results = asynResult.result();
                if (results.size() == 0) {
                    promise.complete(jsonMsg);
                    return;
                }

                jsonMsg.put(successFindAcc, true).put("valueNames", new JsonArray(results));
                promise.complete(jsonMsg);

            });

        });
    }

    /*
    *** delete account : msg reply string
     */
    void handleDeleteAccount(Message<Object> msg) {
        deleteAccount(msg).result();
    }

    Future<Void> deleteAccount(Message<Object> msg) {
        return Future.future((promise) -> {

            var jsonMsg = (JsonObject) msg.body();
            JsonObject query = new JsonObject().put("id", StringEscapeUtils.unescapeHtml3(jsonMsg.getString("id")));

            mongoClient.removeDocument(ConstanceWeb.Collections_User, query, (asynResult) -> {
                if (asynResult.failed()) {

                    asynResult.cause().printStackTrace();

                    msg.reply(new FormResponse().setSuccess(false).setValue(new JsonObject().toString()).toString());

                    promise.complete();
                    return;
                }

                msg.reply(new FormResponse().setSuccess(true).setValue(new JsonObject().toString()).toString());
                promise.complete();
            });

        });
    }
    
    
}
