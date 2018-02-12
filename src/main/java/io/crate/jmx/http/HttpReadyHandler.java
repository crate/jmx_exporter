/*
 * Licensed to Crate under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.  Crate licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * However, if you have executed another commercial license agreement
 * with Crate these terms will supersede the license and you may use the
 * software solely pursuant to the terms of the relevant commercial
 * agreement.
 */

package io.crate.jmx.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.crate.jmx.CrateCollector;
import io.crate.jmx.MBeanAttributeValueStorage;

import java.io.IOException;
import java.net.HttpURLConnection;

public class HttpReadyHandler implements HttpHandler {

    private static final String READY_ATTR_NAME = "NodeStatus_Ready";

    private final CrateCollector crateCollector;
    private final MBeanAttributeValueStorage attributeValueStorage;

    public HttpReadyHandler(CrateCollector crateCollector, MBeanAttributeValueStorage attributeValueStorage) {
        this.crateCollector = crateCollector;
        this.attributeValueStorage = attributeValueStorage;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // collect crate jmx values
        attributeValueStorage.reset();
        crateCollector.collect();

        exchange.getResponseHeaders().set("Content-Length", "0");

        Object readyValue = attributeValueStorage.get(READY_ATTR_NAME);
        if (readyValue instanceof Boolean) {
            Boolean boolVal = (Boolean) readyValue;
            if (boolVal) {
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
            } else {
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_UNAVAILABLE, 0);
            }
        } else {
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_NOT_IMPLEMENTED, 0);
        }
        exchange.close();
    }
}
