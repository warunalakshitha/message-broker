/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package io.ballerina.messaging.broker.client.resources;

import com.google.gson.JsonObject;

/**
 * Representation of exchange in the broker.
 */
public class Exchange {

    public static final String NAME = "name";
    public static final String TYPE = "type";
    public static final String DURABLE = "durable";

    private String name;

    private String type;

    private boolean durable;

    public Exchange(String name, String type, boolean durable) {
        this.name = name;
        this.type = type;
        this.durable = durable;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public boolean isDurable() {
        return durable;
    }

    public String getAsJsonString() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(NAME, name);
        jsonObject.addProperty(TYPE, type);
        jsonObject.addProperty(DURABLE, durable);
        return jsonObject.toString();
    }
}
