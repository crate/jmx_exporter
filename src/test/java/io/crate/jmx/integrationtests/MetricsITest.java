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
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

public class MetricsITest extends AbstractITest {

    private static String METRICS_RESPONSE;

    @BeforeClass
    public static void fetchMetrics() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) randomJmxUrlFromServers("/metrics").openConnection();
        assertThat(connection.getResponseCode(), is(200));
        METRICS_RESPONSE = parseResponse(connection.getInputStream());
    }

    @Test
    public void testQueryStatsMetrics() {
        assertMetricValue("crate_query_duration_seconds{query=\"Select\",} ");
        assertMetricValue("crate_query_duration_seconds{query=\"Update\",} ");
        assertMetricValue("crate_query_duration_seconds{query=\"Delete\",} ");
        assertMetricValue("crate_query_duration_seconds{query=\"Insert\",} ");
        assertMetricValue("crate_query_duration_seconds{query=\"Overall\",} ");

        assertMetricValue("crate_queries{query=\"Select\",} ");
        assertMetricValue("crate_queries{query=\"Update\",} ");
        assertMetricValue("crate_queries{query=\"Delete\",} ");
        assertMetricValue("crate_queries{query=\"Insert\",} ");
        assertMetricValue("crate_queries{query=\"Overall\",} ");
    }

    @Test
    public void testConnectionsMetrics() {
        assertMetricValue("crate_connections{protocol=\"psql\",property=\"open\",} ");
        assertMetricValue("crate_connections{protocol=\"psql\",property=\"total\",} ");
        assertMetricValue("crate_connections{protocol=\"http\",property=\"open\",} ");
        assertMetricValue("crate_connections{protocol=\"http\",property=\"total\",} ");
        assertMetricValue("crate_connections{protocol=\"transport\",property=\"open\",} ");
    }

    private void assertMetricValue(String metricString) {
        int startIdx = METRICS_RESPONSE.indexOf(metricString);
        assertThat(metricString + " not found in response", startIdx, greaterThanOrEqualTo(0));
        int endIdx = METRICS_RESPONSE.indexOf("\n", startIdx);
        String metricValueStr = METRICS_RESPONSE.substring(startIdx + metricString.length(), endIdx);
        assertThat(metricValueStr.matches("\\d+.\\d+"), is(true));
    }

    @Test
    public void testNodeStatusMetrics() {
        assertThat(METRICS_RESPONSE, containsString("crate_ready 1.0\n"));
    }

    @Test
    public void testJvmVersionMetrics() {
        assertThat(METRICS_RESPONSE, containsString("jvm_info{version="));
    }

    @Test
    public void testJvmMemoryMetrics() {
        assertThat(METRICS_RESPONSE, containsString("jvm_memory_bytes_used{area=\"heap\",}"));
        assertThat(METRICS_RESPONSE, containsString("jvm_memory_bytes_used{area=\"nonheap\",}"));
        assertThat(METRICS_RESPONSE, containsString("jvm_memory_bytes_committed{area=\"heap\",}"));
        assertThat(METRICS_RESPONSE, containsString("jvm_memory_bytes_committed{area=\"nonheap\",}"));
        assertThat(METRICS_RESPONSE, containsString("jvm_memory_bytes_max{area=\"heap\",}"));
        assertThat(METRICS_RESPONSE, containsString("jvm_memory_bytes_max{area=\"nonheap\",}"));
        assertThat(METRICS_RESPONSE, containsString("jvm_memory_bytes_init{area=\"heap\",}"));
        assertThat(METRICS_RESPONSE, containsString("jvm_memory_bytes_init{area=\"nonheap\",}"));

        assertThat(METRICS_RESPONSE, containsString("jvm_memory_pool_bytes_used{pool=\"Code Cache\",}"));
        assertThat(METRICS_RESPONSE, containsString("jvm_memory_pool_bytes_used{pool=\"Metaspace\",}"));
        assertThat(METRICS_RESPONSE, containsString("jvm_memory_pool_bytes_used{pool=\"Compressed Class Space\",}"));
        assertThat(METRICS_RESPONSE, containsString("jvm_memory_pool_bytes_used{pool=\"Par Eden Space\",}"));
        assertThat(METRICS_RESPONSE, containsString("jvm_memory_pool_bytes_used{pool=\"Par Survivor Space\",}"));
        assertThat(METRICS_RESPONSE, containsString("jvm_memory_pool_bytes_used{pool=\"CMS Old Gen\",}"));
        assertThat(METRICS_RESPONSE, containsString("jvm_memory_pool_bytes_committed{pool=\"Code Cache\",}"));
        assertThat(METRICS_RESPONSE, containsString("jvm_memory_pool_bytes_committed{pool=\"Metaspace\",}"));
        assertThat(METRICS_RESPONSE, containsString("jvm_memory_pool_bytes_committed{pool=\"Compressed Class Space\",}"));
        assertThat(METRICS_RESPONSE, containsString("jvm_memory_pool_bytes_committed{pool=\"Par Eden Space\",}"));
        assertThat(METRICS_RESPONSE, containsString("jvm_memory_pool_bytes_committed{pool=\"Par Survivor Space\",}"));
        assertThat(METRICS_RESPONSE, containsString("jvm_memory_pool_bytes_committed{pool=\"CMS Old Gen\",}"));
        assertThat(METRICS_RESPONSE, containsString("jvm_memory_pool_bytes_max{pool=\"Code Cache\",}"));
        assertThat(METRICS_RESPONSE, containsString("jvm_memory_pool_bytes_max{pool=\"Metaspace\",}"));
        assertThat(METRICS_RESPONSE, containsString("jvm_memory_pool_bytes_max{pool=\"Compressed Class Space\",}"));
        assertThat(METRICS_RESPONSE, containsString("jvm_memory_pool_bytes_max{pool=\"Par Eden Space\",}"));
        assertThat(METRICS_RESPONSE, containsString("jvm_memory_pool_bytes_max{pool=\"Par Survivor Space\",}"));
        assertThat(METRICS_RESPONSE, containsString("jvm_memory_pool_bytes_max{pool=\"CMS Old Gen\",}"));
        assertThat(METRICS_RESPONSE, containsString("jvm_memory_pool_bytes_init{pool=\"Code Cache\",}"));
        assertThat(METRICS_RESPONSE, containsString("jvm_memory_pool_bytes_init{pool=\"Metaspace\",}"));
        assertThat(METRICS_RESPONSE, containsString("jvm_memory_pool_bytes_init{pool=\"Compressed Class Space\",}"));
        assertThat(METRICS_RESPONSE, containsString("jvm_memory_pool_bytes_init{pool=\"Par Eden Space\",}"));
        assertThat(METRICS_RESPONSE, containsString("jvm_memory_pool_bytes_init{pool=\"Par Survivor Space\",}"));
        assertThat(METRICS_RESPONSE, containsString("jvm_memory_pool_bytes_init{pool=\"CMS Old Gen\",}"));
    }

    @Test
    public void testJvmThreadsMetrics() {
        assertThat(METRICS_RESPONSE, containsString("jvm_threads_current"));
        assertThat(METRICS_RESPONSE, containsString("jvm_threads_daemon"));
        assertThat(METRICS_RESPONSE, containsString("jvm_threads_peak"));
        assertThat(METRICS_RESPONSE, containsString("jvm_threads_started_total"));
        assertThat(METRICS_RESPONSE, containsString("jvm_threads_deadlocked"));
        assertThat(METRICS_RESPONSE, containsString("jvm_threads_deadlocked_monitor"));
    }

    @Test
    public void testJvmGcMetrics() {
        assertThat(METRICS_RESPONSE, containsString("jvm_gc_collection_seconds_count{gc=\"ParNew\",}"));
        assertThat(METRICS_RESPONSE, containsString("jvm_gc_collection_seconds_sum{gc=\"ParNew\",}"));
        assertThat(METRICS_RESPONSE, containsString("jvm_gc_collection_seconds_count{gc=\"ConcurrentMarkSweep\",}"));
        assertThat(METRICS_RESPONSE, containsString("jvm_gc_collection_seconds_sum{gc=\"ConcurrentMarkSweep\",}"));
    }

    @Test
    public void testJvmClassLoadingMetrics() {
        assertThat(METRICS_RESPONSE, containsString("jvm_classes_loaded"));
        assertThat(METRICS_RESPONSE, containsString("jvm_classes_loaded_total"));
        assertThat(METRICS_RESPONSE, containsString("jvm_classes_unloaded_total"));
    }

    @Test
    public void testJvmProcessMetrics() {
        assertThat(METRICS_RESPONSE, containsString("process_cpu_seconds_total"));
        assertThat(METRICS_RESPONSE, containsString("process_start_time_seconds"));
        assertThat(METRICS_RESPONSE, containsString("process_open_fds"));
        assertThat(METRICS_RESPONSE, containsString("process_max_fds"));
    }

    @Test
    public void testRequestingMultipleTimesDoesNotResultInDuplicateMetrics() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) randomJmxUrlFromServers("/metrics").openConnection();
        assertThat(connection.getResponseCode(), is(200));
        String response = parseResponse(connection.getInputStream());

        assertThat(response, containsString("crate_ready 1.0\n"));
        assertThat(response, not(containsString("crate_ready 1.0\ncrate_ready 1.0\n")));
    }
}
