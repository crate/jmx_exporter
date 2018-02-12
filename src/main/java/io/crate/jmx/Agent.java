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

package io.crate.jmx;

import com.sun.net.httpserver.HttpHandler;
import io.crate.jmx.http.HttpMetricHandler;
import io.crate.jmx.http.HttpReadyHandler;
import io.crate.jmx.http.HttpServer;
import io.prometheus.client.hotspot.DefaultExports;

import java.lang.instrument.Instrumentation;
import java.net.InetSocketAddress;

public class Agent {

    static HttpServer SERVER;

    public static void premain(String agentArgument, Instrumentation instrumentation) throws Exception {
        // Bind to all interfaces by default (this includes IPv6).
        String host = "0.0.0.0";

        // If we have IPv6 address in square brackets, extract it first and then
        // remove it from arguments to prevent confusion from too many colons.
        Integer indexOfClosingSquareBracket = agentArgument.indexOf("]:");
        if (indexOfClosingSquareBracket >= 0) {
            host = agentArgument.substring(0, indexOfClosingSquareBracket + 1);
            agentArgument = agentArgument.substring(indexOfClosingSquareBracket + 2);
        }

        String[] args = agentArgument.split(":");
        if (args.length < 1 || args.length > 2) {
            System.err.println("Usage: -javaagent:/path/to/JavaAgent.jar=[host:]<port>");
            System.exit(1);
        }

        int port;
        InetSocketAddress socket;

        if (args.length == 2) {
            port = Integer.parseInt(args[1]);
            socket = new InetSocketAddress(args[0], port);
        } else {
            port = Integer.parseInt(args[0]);
            socket = new InetSocketAddress(host, port);
        }

        DefaultExports.initialize();

        SERVER = new HttpServer(socket, true);

        HttpHandler mHandler = new HttpMetricHandler();
        SERVER.registerHandler("/", mHandler);
        SERVER.registerHandler("/metrics", mHandler);

        MBeanAttributeValueStorage beanAttributeValueStorage = new MBeanAttributeValueStorage();
        CrateCollector crateCollector = new CrateCollector(beanAttributeValueStorage::put).register();

        HttpHandler readyHandler = new HttpReadyHandler(crateCollector, beanAttributeValueStorage);
        SERVER.registerHandler("/ready", readyHandler);

        SERVER.start(true);
    }
}
