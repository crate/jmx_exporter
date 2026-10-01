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

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Concurrent requests to /metrics and /ready must not interfere with each other,
 * see https://github.com/crate/jmx_exporter/issues/108.
 */
public class ConcurrencyITest extends AbstractITest {

    private static final int THREADS = 4;
    private static final int ITERATIONS = 100;

    @Test
    public void testConcurrentMetricsAndReadyRequests() throws Exception {
        Map<String, Integer> expected = crateSampleCounts(metricsResponse);

        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        CyclicBarrier start = new CyclicBarrier(THREADS);
        List<Future<?>> futures = new ArrayList<>();
        for (int t = 0; t < THREADS; t++) {
            boolean ready = (t % 2 == 1);
            futures.add(executor.submit(() -> {
                start.await();
                for (int i = 0; i < ITERATIONS; i++) {
                    if (ready) {
                        HttpURLConnection connection = (HttpURLConnection) randomJmxUrlFromServers("/ready").openConnection();
                        assertThat(connection.getResponseCode(), is(200));
                        connection.disconnect();
                    } else {
                        assertThat(crateSampleCounts(parseMetricsResponse()), is(expected));
                    }
                }
                return null;
            }));
        }
        try {
            for (Future<?> future : futures) {
                future.get(5, TimeUnit.MINUTES);
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private static Map<String, Integer> crateSampleCounts(String response) {
        Map<String, Integer> counts = new TreeMap<>();
        for (String line : response.split("\n")) {
            if (line.startsWith("crate_")) {
                int end = line.indexOf('{');
                if (end == -1) {
                    end = line.indexOf(' ');
                }
                counts.merge(line.substring(0, end), 1, Integer::sum);
            }
        }
        return counts;
    }
}
