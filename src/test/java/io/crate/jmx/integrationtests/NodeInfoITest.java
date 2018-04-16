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

package io.crate.jmx.integrationtests;

import org.junit.BeforeClass;
import org.junit.Test;

import java.net.HttpURLConnection;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;

public class NodeInfoITest extends AbstractITest {

    private static String METRICS_RESPONSE;

    @BeforeClass
    public static void fetchMetrics() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) randomJmxUrlFromServers("/metrics").openConnection();
        assertThat(connection.getResponseCode(), is(200));
        METRICS_RESPONSE = parseResponse(connection.getInputStream());
    }

    @Test
    public void testNodeInfoMetrics() {
        assertThat(METRICS_RESPONSE, containsString("crate_node_info{id="));

        int startIdx = METRICS_RESPONSE.indexOf("crate_node_info{id=");
        int endIdx = METRICS_RESPONSE.indexOf("\n", startIdx);
        String nodeInfoStr = METRICS_RESPONSE.substring(startIdx, endIdx);
        assertThat(nodeInfoStr.matches("crate_node_info\\{id=\".+\",name=\".+\",\\} 1.0"), is(true));

        // Check that there is no duplicate line and that NodeInfo records only when both attributes
        // (id & name) are available.
        assertThat(METRICS_RESPONSE.indexOf("crate_node_info{id=", endIdx), is(-1));
    }
}
