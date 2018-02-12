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
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URLDecoder;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

public class HttpMetricHandler implements HttpHandler {

    private static class LocalByteArray extends ThreadLocal<ByteArrayOutputStream> {
        protected ByteArrayOutputStream initialValue() {
            return new ByteArrayOutputStream(1 << 20);
        }
    }

    private static Set<String> parseQuery(String query) throws IOException {
        Set<String> names = new HashSet<>();
        if (query != null) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf("=");
                if (idx != -1 && URLDecoder.decode(pair.substring(0, idx), "UTF-8").equals("name[]")) {
                    names.add(URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
                }
            }
        }
        return names;
    }

    private final CollectorRegistry registry = CollectorRegistry.defaultRegistry;
    private final LocalByteArray response = new LocalByteArray();

    public HttpMetricHandler() {
    }


    public void handle(HttpExchange t) throws IOException {
        String query = t.getRequestURI().getRawQuery();

        ByteArrayOutputStream response = this.response.get();
        response.reset();
        OutputStreamWriter osw = new OutputStreamWriter(response);
        TextFormat.write004(osw,
                registry.filteredMetricFamilySamples(parseQuery(query)));
        osw.flush();
        osw.close();
        response.flush();
        response.close();

        t.getResponseHeaders().set("Content-Type",
                TextFormat.CONTENT_TYPE_004);
        t.getResponseHeaders().set("Content-Length",
                String.valueOf(response.size()));
        if (HttpServer.shouldUseCompression(t)) {
            t.getResponseHeaders().set("Content-Encoding", "gzip");
            t.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
            final GZIPOutputStream os = new GZIPOutputStream(t.getResponseBody());
            response.writeTo(os);
            os.finish();
        } else {
            t.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.size());
            response.writeTo(t.getResponseBody());
        }
        t.close();
    }

}
