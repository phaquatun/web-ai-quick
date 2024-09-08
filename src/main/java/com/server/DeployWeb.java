

package com.server;

import io.vertx.core.Vertx;



public class DeployWeb {
    
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle("com.server.SetupServer");
    }
}
