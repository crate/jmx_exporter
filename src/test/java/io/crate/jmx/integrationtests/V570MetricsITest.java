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

import org.junit.Test;

public class V570MetricsITest extends MetricsITest {

    private static final String URL = "https://cdn.crate.io/downloads/releases/crate-5.7.0.tar.gz";

    @Override
    String getCrateDistributionURL() {
        return URL;
    }

    @Override
    @Test
    public void test_isMaster_and_roles() throws Exception {
        // Versions < 6.2.x don't support these attributes
    }

    @Override
    @Test
    public void testConnectionsMetrics() {
        assertMetricValue("crate_connections{protocol=\"psql\",property=\"open\",} ");
        assertMetricValue("crate_connections{protocol=\"psql\",property=\"total\",} ");
        assertMetricValue("crate_connections{protocol=\"http\",property=\"open\",} ");
        assertMetricValue("crate_connections{protocol=\"http\",property=\"total\",} ");
        assertMetricValue("crate_connections{protocol=\"transport\",property=\"open\",} ");
    }

    @Test
    public void testShardMetrics() throws Exception {
        // primary flag is not available on < 6.2.x
        // schema property is always empty for < 5.8.2
        assertMetricValue("crate_node{name=\"shard_info\",property=\"size\",id=\"0\",schema=\"\",table=\"test_shards\",partition_ident=\"\",primary=\"\",} ");
        assertMetricValue("crate_node{name=\"shard_info\",property=\"size\",id=\"1\",schema=\"\",table=\"test_shards\",partition_ident=\"\",primary=\"\",} ");
        assertMetricValue("crate_node{name=\"shard_info\",property=\"size\",id=\"0\",schema=\"\",table=\"test_shards_parted\",partition_ident=\"04130\",primary=\"\",} ");
        assertMetricValue("crate_node{name=\"shard_info\",property=\"size\",id=\"1\",schema=\"\",table=\"test_shards_parted\",partition_ident=\"04130\",primary=\"\",} ");
        assertMetricValue("crate_node{name=\"shard_info\",property=\"size\",id=\"0\",schema=\"\",table=\"test_shards_parted\",partition_ident=\"04132\",primary=\"\",} ");
        assertMetricValue("crate_node{name=\"shard_info\",property=\"size\",id=\"1\",schema=\"\",table=\"test_shards_parted\",partition_ident=\"04132\",primary=\"\",} ");
        assertMetricValue("crate_node{name=\"shard_stats\",property=\"primaries\",} ");
        assertMetricValue("crate_node{name=\"shard_stats\",property=\"replicas\",} ");
        assertMetricValue("crate_node{name=\"shard_stats\",property=\"total\",} ");
        assertMetricValue("crate_node{name=\"shard_stats\",property=\"unassigned\",} ");
    }
}
