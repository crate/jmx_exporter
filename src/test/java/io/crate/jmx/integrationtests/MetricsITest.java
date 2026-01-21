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

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.hamcrest.MatcherAssert.assertThat;

public class MetricsITest extends AbstractITest {

    @Test
    public void test_isMaster_and_roles() throws Exception {
        assertBusy(() -> {
            metricsResponse = parseMetricsResponse();
            assertMetricValue("crate_is_master ");
            assertMetricValue("crate_roles{is_data=\"true\",is_master_eligible=\"true\",} ");
        }, 30, TimeUnit.SECONDS);
    }

    @Test
    public void testQueryStatsMetrics() {
        assertMetricValue("crate_query_total_count{query=\"Select\",} ");
        assertMetricValue("crate_query_total_count{query=\"Update\",} ");
        assertMetricValue("crate_query_total_count{query=\"Delete\",} ");
        assertMetricValue("crate_query_total_count{query=\"Insert\",} ");
        assertMetricValue("crate_query_total_count{query=\"Management\",} ");
        assertMetricValue("crate_query_total_count{query=\"DDL\",} ");
        assertMetricValue("crate_query_total_count{query=\"Copy\",} ");
        assertMetricValue("crate_query_total_count{query=\"Undefined\",} ");

        assertMetricValue("crate_query_affected_row_count{query=\"Select\",} ");
        assertMetricValue("crate_query_affected_row_count{query=\"Update\",} ");
        assertMetricValue("crate_query_affected_row_count{query=\"Delete\",} ");
        assertMetricValue("crate_query_affected_row_count{query=\"Insert\",} ");
        assertMetricValue("crate_query_affected_row_count{query=\"Management\",} ");
        assertMetricValue("crate_query_affected_row_count{query=\"DDL\",} ");
        assertMetricValue("crate_query_affected_row_count{query=\"Copy\",} ");
        assertMetricValue("crate_query_affected_row_count{query=\"Undefined\",} ");

        assertMetricValue("crate_query_failed_count{query=\"Select\",} ");
        assertMetricValue("crate_query_failed_count{query=\"Update\",} ");
        assertMetricValue("crate_query_failed_count{query=\"Delete\",} ");
        assertMetricValue("crate_query_failed_count{query=\"Insert\",} ");
        assertMetricValue("crate_query_failed_count{query=\"Management\",} ");
        assertMetricValue("crate_query_failed_count{query=\"DDL\",} ");
        assertMetricValue("crate_query_failed_count{query=\"Copy\",} ");
        assertMetricValue("crate_query_failed_count{query=\"Undefined\",} ");

        assertMetricValue("crate_query_sum_of_durations_millis{query=\"Select\",} ");
        assertMetricValue("crate_query_sum_of_durations_millis{query=\"Update\",} ");
        assertMetricValue("crate_query_sum_of_durations_millis{query=\"Delete\",} ");
        assertMetricValue("crate_query_sum_of_durations_millis{query=\"Insert\",} ");
        assertMetricValue("crate_query_sum_of_durations_millis{query=\"Management\",} ");
        assertMetricValue("crate_query_sum_of_durations_millis{query=\"DDL\",} ");
        assertMetricValue("crate_query_sum_of_durations_millis{query=\"Copy\",} ");
        assertMetricValue("crate_query_sum_of_durations_millis{query=\"Undefined\",} ");
    }

    @Test
    public void testConnectionsMetrics() {
        assertMetricValue("crate_connections{protocol=\"psql\",property=\"open\",} ");
        assertMetricValue("crate_connections{protocol=\"psql\",property=\"total\",} ");
        assertMetricValue("crate_connections{protocol=\"psql\",property=\"messagesreceived\",} ");
        assertMetricValue("crate_connections{protocol=\"psql\",property=\"bytesreceived\",} ");
        assertMetricValue("crate_connections{protocol=\"psql\",property=\"messagessent\",} ");
        assertMetricValue("crate_connections{protocol=\"psql\",property=\"bytessent\",} ");
        assertMetricValue("crate_connections{protocol=\"http\",property=\"open\",} ");
        assertMetricValue("crate_connections{protocol=\"http\",property=\"total\",} ");
        assertMetricValue("crate_connections{protocol=\"http\",property=\"messagesreceived\",} ");
        assertMetricValue("crate_connections{protocol=\"http\",property=\"bytesreceived\",} ");
        assertMetricValue("crate_connections{protocol=\"http\",property=\"messagessent\",} ");
        assertMetricValue("crate_connections{protocol=\"http\",property=\"bytessent\",} ");
        assertMetricValue("crate_connections{protocol=\"transport\",property=\"open\",} ");
        assertMetricValue("crate_connections{protocol=\"transport\",property=\"total\",} ");
        assertMetricValue("crate_connections{protocol=\"transport\",property=\"messagesreceived\",} ");
        assertMetricValue("crate_connections{protocol=\"transport\",property=\"bytesreceived\",} ");
        assertMetricValue("crate_connections{protocol=\"transport\",property=\"messagessent\",} ");
        assertMetricValue("crate_connections{protocol=\"transport\",property=\"bytessent\",} ");
    }

    @Test
    public void testThreadPoolsMetrics() {
        List<String> poolNames = Arrays.asList(
                "generic", "search", "snapshot", "management", "write",
                "listener", "flush", "refresh", "force_merge", "fetch_shard_started", "fetch_shard_store");
        List<String> properties = Arrays.asList(
                "poolSize", "largestPoolSize", "queueSize", "active", "completed", "rejected");
        for (String poolName : poolNames) {
            for (String property : properties) {
                assertMetricValue("crate_threadpools{name=\""+poolName+"\",property=\""+property+"\",} ");
            }
        }
    }

    @Test
    public void testCircuitBreakersMetrics() {
        List<String> names = Arrays.asList(
                "parent", "query", "jobs_log", "operations_log", "fielddata", "request", "in_flight_requests");
        List<String> properties = Arrays.asList(
                "limit", "used", "trippedCount", "overhead");
        for (String name : names) {
            for (String property : properties) {
                assertMetricValue("crate_circuitbreakers{name=\""+name+"\",property=\""+property+"\",} ");
            }
        }
    }

    @Test
    public void testNodeStatusMetrics() {
        assertThat(metricsResponse, containsString("crate_ready 1.0\n"));
    }

    @Test
    public void testJvmVersionMetrics() {
        assertThat(metricsResponse, stringContainsInOrder("jvm_info{runtime=", ",vendor=", ",version=", "}"));
    }

    @Test
    public void testJvmMemoryMetrics() {
        assertThat(metricsResponse, containsString("jvm_memory_bytes_used{area=\"heap\",}"));
        assertThat(metricsResponse, containsString("jvm_memory_bytes_used{area=\"nonheap\",}"));
        assertThat(metricsResponse, containsString("jvm_memory_bytes_committed{area=\"heap\",}"));
        assertThat(metricsResponse, containsString("jvm_memory_bytes_committed{area=\"nonheap\",}"));
        assertThat(metricsResponse, containsString("jvm_memory_bytes_max{area=\"heap\",}"));
        assertThat(metricsResponse, containsString("jvm_memory_bytes_max{area=\"nonheap\",}"));
        assertThat(metricsResponse, containsString("jvm_memory_bytes_init{area=\"heap\",}"));
        assertThat(metricsResponse, containsString("jvm_memory_bytes_init{area=\"nonheap\",}"));

        assertThat(metricsResponse, containsString("jvm_memory_pool_bytes_used{pool=\"CodeHeap 'non-nmethods'\",}"));
        assertThat(metricsResponse, containsString("jvm_memory_pool_bytes_used{pool=\"CodeHeap 'profiled nmethods'\",}"));
        assertThat(metricsResponse, containsString("jvm_memory_pool_bytes_used{pool=\"CodeHeap 'non-profiled nmethods'\",}"));
        assertThat(metricsResponse, containsString("jvm_memory_pool_bytes_used{pool=\"Metaspace\",}"));
        assertThat(metricsResponse, containsString("jvm_memory_pool_bytes_used{pool=\"Compressed Class Space\",}"));
        assertThat(metricsResponse, containsString("jvm_memory_pool_bytes_used{pool=\"G1 Eden Space\",}"));
        assertThat(metricsResponse, containsString("jvm_memory_pool_bytes_used{pool=\"G1 Survivor Space\",}"));
        assertThat(metricsResponse, containsString("jvm_memory_pool_bytes_used{pool=\"G1 Old Gen\",}"));
        assertThat(metricsResponse, containsString("jvm_memory_pool_bytes_committed{pool=\"CodeHeap 'non-nmethods'\",}"));
        assertThat(metricsResponse, containsString("jvm_memory_pool_bytes_committed{pool=\"CodeHeap 'profiled nmethods'\",}"));
        assertThat(metricsResponse, containsString("jvm_memory_pool_bytes_committed{pool=\"CodeHeap 'non-profiled nmethods'\",}"));
        assertThat(metricsResponse, containsString("jvm_memory_pool_bytes_committed{pool=\"Metaspace\",}"));
        assertThat(metricsResponse, containsString("jvm_memory_pool_bytes_committed{pool=\"Compressed Class Space\",}"));
        assertThat(metricsResponse, containsString("jvm_memory_pool_bytes_committed{pool=\"G1 Eden Space\",}"));
        assertThat(metricsResponse, containsString("jvm_memory_pool_bytes_committed{pool=\"G1 Survivor Space\",}"));
        assertThat(metricsResponse, containsString("jvm_memory_pool_bytes_committed{pool=\"G1 Old Gen\",}"));
        assertThat(metricsResponse, containsString("jvm_memory_pool_bytes_max{pool=\"CodeHeap 'non-nmethods'\",}"));
        assertThat(metricsResponse, containsString("jvm_memory_pool_bytes_max{pool=\"CodeHeap 'profiled nmethods'\",}"));
        assertThat(metricsResponse, containsString("jvm_memory_pool_bytes_max{pool=\"CodeHeap 'non-profiled nmethods'\",}"));
        assertThat(metricsResponse, containsString("jvm_memory_pool_bytes_max{pool=\"Metaspace\",}"));
        assertThat(metricsResponse, containsString("jvm_memory_pool_bytes_max{pool=\"Compressed Class Space\",}"));
        assertThat(metricsResponse, containsString("jvm_memory_pool_bytes_max{pool=\"G1 Eden Space\",}"));
        assertThat(metricsResponse, containsString("jvm_memory_pool_bytes_max{pool=\"G1 Survivor Space\",}"));
        assertThat(metricsResponse, containsString("jvm_memory_pool_bytes_max{pool=\"G1 Old Gen\",}"));
        assertThat(metricsResponse, containsString("jvm_memory_pool_bytes_init{pool=\"CodeHeap 'non-nmethods'\",}"));
        assertThat(metricsResponse, containsString("jvm_memory_pool_bytes_init{pool=\"CodeHeap 'profiled nmethods'\",}"));
        assertThat(metricsResponse, containsString("jvm_memory_pool_bytes_init{pool=\"CodeHeap 'non-profiled nmethods'\",}"));
        assertThat(metricsResponse, containsString("jvm_memory_pool_bytes_init{pool=\"Metaspace\",}"));
        assertThat(metricsResponse, containsString("jvm_memory_pool_bytes_init{pool=\"Compressed Class Space\",}"));
        assertThat(metricsResponse, containsString("jvm_memory_pool_bytes_init{pool=\"G1 Eden Space\",}"));
        assertThat(metricsResponse, containsString("jvm_memory_pool_bytes_init{pool=\"G1 Survivor Space\",}"));
        assertThat(metricsResponse, containsString("jvm_memory_pool_bytes_init{pool=\"G1 Old Gen\",}"));
    }

    @Test
    public void testJvmThreadsMetrics() {
        assertThat(metricsResponse, containsString("jvm_threads_current"));
        assertThat(metricsResponse, containsString("jvm_threads_daemon"));
        assertThat(metricsResponse, containsString("jvm_threads_peak"));
        assertThat(metricsResponse, containsString("jvm_threads_started_total"));
        assertThat(metricsResponse, containsString("jvm_threads_deadlocked"));
        assertThat(metricsResponse, containsString("jvm_threads_deadlocked_monitor"));
    }

    @Test
    public void testJvmGcMetrics() {
        assertThat(metricsResponse, containsString("jvm_gc_collection_seconds_count{gc=\"G1 Young Generation\",}"));
        assertThat(metricsResponse, containsString("jvm_gc_collection_seconds_sum{gc=\"G1 Young Generation\",}"));
    }

    @Test
    public void testJvmClassLoadingMetrics() {
        assertThat(metricsResponse, containsString("jvm_classes_loaded"));
        assertThat(metricsResponse, containsString("jvm_classes_loaded_total"));
        assertThat(metricsResponse, containsString("jvm_classes_unloaded_total"));
    }

    @Test
    public void testJvmProcessMetrics() {
        assertThat(metricsResponse, containsString("process_cpu_seconds_total"));
        assertThat(metricsResponse, containsString("process_start_time_seconds"));
        assertThat(metricsResponse, containsString("process_open_fds"));
        assertThat(metricsResponse, containsString("process_max_fds"));
    }

    @Test
    public void testRequestingMultipleTimesDoesNotResultInDuplicateMetrics() throws Exception {
        String response = parseMetricsResponse();
        assertThat(response, containsString("crate_ready 1.0\n"));
        assertThat(response, not(containsString("crate_ready 1.0\ncrate_ready 1.0\n")));
    }

    @Test
    public void testShardMetrics() throws Exception {
        assertMetricValue("crate_node{name=\"shard_info\",property=\"size\",id=\"0\",schema=\"crate\",table=\"test_shards\",partition_ident=\"\",primary=\"true\",} ");
        assertMetricValue("crate_node{name=\"shard_info\",property=\"size\",id=\"1\",schema=\"crate\",table=\"test_shards\",partition_ident=\"\",primary=\"true\",} ");
        assertMetricValue("crate_node{name=\"shard_info\",property=\"size\",id=\"0\",schema=\"crate\",table=\"test_shards_parted\",partition_ident=\"04130\",primary=\"true\",} ");
        assertMetricValue("crate_node{name=\"shard_info\",property=\"size\",id=\"1\",schema=\"crate\",table=\"test_shards_parted\",partition_ident=\"04130\",primary=\"true\",} ");
        assertMetricValue("crate_node{name=\"shard_info\",property=\"size\",id=\"0\",schema=\"crate\",table=\"test_shards_parted\",partition_ident=\"04132\",primary=\"true\",} ");
        assertMetricValue("crate_node{name=\"shard_info\",property=\"size\",id=\"1\",schema=\"crate\",table=\"test_shards_parted\",partition_ident=\"04132\",primary=\"true\",} ");
        assertMetricValue("crate_node{name=\"shard_stats\",property=\"primaries\",} ");
        assertMetricValue("crate_node{name=\"shard_stats\",property=\"replicas\",} ");
        assertMetricValue("crate_node{name=\"shard_stats\",property=\"total\",} ");
        assertMetricValue("crate_node{name=\"shard_stats\",property=\"unassigned\",} ");
    }
}
