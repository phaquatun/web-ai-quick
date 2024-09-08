package com.dao;

import io.vertx.core.json.JsonObject;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(chain = true)
@Setter
@Getter
public class FormResponse {

    boolean success;

    String value;

    @Override
    public String toString() {
        return new JsonObject().put("success", success)
                .put("value", value)
                .toString();
    }

}
