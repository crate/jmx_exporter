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

import com.sun.tools.attach.VirtualMachine;
import io.crate.testing.CrateTestCluster;
import io.crate.testing.CrateTestServer;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

public abstract class AbstractITest {

    protected static final String LATEST_URL = "https://cdn.crate.io/downloads/releases/nightly/crate-latest.tar.gz";

    protected static String metricsResponse;

    private static Integer JMX_HTTP_PORT;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private static CrateTestCluster testCluster;

    String getCrateDistributionURL() {
        return LATEST_URL;
    }

    @Before
    public void setup() throws Throwable {
        if (JMX_HTTP_PORT == null) {
            try (ServerSocket socket = new ServerSocket(0)) {
                JMX_HTTP_PORT = socket.getLocalPort();
            }
        }
        if (testCluster == null) {
            setUpClusterAndAgent(getCrateDistributionURL());
            setupTables();
            metricsResponse = parseMetricsResponse();
        }
    }

    @AfterClass
    public static void shutdown() {
        if (testCluster != null) {
            testCluster.after();
        }
    }

    protected static void setUpClusterAndAgent(String url) throws Throwable {
        CrateTestCluster.Builder builder;
        builder = CrateTestCluster.fromURL(url);
        testCluster = builder.keepWorkingDir(false).build();
        testCluster.before();
        long pid = testCluster.randomServer().pid().toCompletableFuture().get(5, TimeUnit.SECONDS);
        attachAgent(pid);
    }

    private void setupTables() throws SQLException {
        int jdbcPort = testCluster.servers().iterator().next().psqlPort();
        String jdbcConnectionString = "jdbc:postgresql://localhost:" + jdbcPort + "/crate";
        String user = "crate";
        String password = "";
        try (Connection connection = DriverManager.getConnection(jdbcConnectionString, user, password)) {
            connection.createStatement().execute("CREATE TABLE test_shards(a int) CLUSTERED INTO 2 shards");
            connection.createStatement().execute(
                "INSERT INTO test_shards " +
                "SELECT * FROM generate_series(1, 10, 1)");
            connection.createStatement().execute("REFRESH TABLE test_shards");

            connection.createStatement().execute("CREATE TABLE test_shards_parted(a int, " +
                "p int generated always as a % 2) PARTITIONED BY (p) CLUSTERED INTO 2 shards");
            connection.createStatement().execute(
                "INSERT INTO test_shards_parted(a) " +
                    "SELECT * FROM generate_series(1, 10, 1)");
            connection.createStatement().execute("REFRESH TABLE test_shards_parted");
        } catch (SQLException e) {
            System.err.println("Failed to establish database connection");
            throw e;
        }
    }

    protected static String parseMetricsResponse() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) randomJmxUrlFromServers("/metrics").openConnection();
        assertThat(connection.getResponseCode(), is(200));
        return parseResponse(connection.getInputStream());
    }

    @AfterClass
    public static void tearDown() {
        testCluster.after();
        testCluster = null;
    }

    private static void attachAgent(long pid) throws Exception {
        File agentJar = findAgentJar();
        String options = String.format("%d", JMX_HTTP_PORT);
        VirtualMachine vm = VirtualMachine.attach(Long.toString(pid));
        vm.loadAgent(agentJar.getAbsolutePath(), options);
    }

    private static File findAgentJar() {
        File buildDir = new File(new File(System.getProperty("user.dir"), "build"), "libs");
        if (buildDir.exists() == false) {
            throw new RuntimeException("No build folder found, please run './gradlew buildJar' first");
        }
        File[] files = buildDir.listFiles();
        if (files == null) {
            throw new RuntimeException("Cannot list files while searching the agent jar");
        }
        for (File file : files) {
            if (file.getName().contains("crate-jmx-exporter")) {
                return file;
            }
        }
        throw new RuntimeException("Cannot find agent jar file, please run './gradlew buildJar' first");
    }


    static URL randomJmxUrlFromServers(String uri) throws MalformedURLException {
        CrateTestServer server = testCluster.randomServer();
        return new URL(String.format("http://%s:%d%s", server.crateHost(), JMX_HTTP_PORT, uri));
    }

    static String parseResponse(InputStream inputStream) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder res = new StringBuilder();

        String line;
        while((line = br.readLine()) != null) {
            res.append(line);
            res.append("\n");
        }

        br.close();
        return res.toString();
    }

    void assertMetricValue(String metricString) {
        int startIdx = metricsResponse.lastIndexOf(metricString);
        assertThat(metricString + " not found in response", startIdx, greaterThanOrEqualTo(-1));
        int endIdx = metricsResponse.indexOf("\n", startIdx);
        String metricValueStr = metricsResponse.substring(startIdx + metricString.length(), endIdx);
        assertThat(
                String.format("Metric '%s' value '%s' must be in the format ^-\\d+.\\d+(E\\d+)?", metricString, metricValueStr),
                metricValueStr.matches("^-?\\d+.\\d+(E\\d+)?"),
                is(true)
        );
    }

    static void assertBusy(CheckedRunnable<Exception> codeBlock, long maxWaitTime, TimeUnit unit) throws Exception {
        long maxTimeInMillis = TimeUnit.MILLISECONDS.convert(maxWaitTime, unit);
        long iterations = Math.max(Math.round(Math.log10(maxTimeInMillis) / Math.log10(2)), 1);
        long timeInMillis = 1;
        long sum = 0;
        List<AssertionError> failures = new ArrayList<>();
        for (int i = 0; i < iterations; i++) {
            try {
                codeBlock.run();
                return;
            } catch (AssertionError e) {
                failures.add(e);
            }
            sum += timeInMillis;
            Thread.sleep(timeInMillis);
            timeInMillis *= 2;
        }
        timeInMillis = maxTimeInMillis - sum;
        Thread.sleep(Math.max(timeInMillis, 0));
        try {
            codeBlock.run();
        } catch (AssertionError e) {
            String msg = e.getMessage();
            for (AssertionError failure : failures) {
                if (!failure.getMessage().equals(msg)) {
                    e.addSuppressed(failure);
                }
            }
            throw e;
        }
    }

    @FunctionalInterface
    interface CheckedRunnable<E extends Exception> {
        void run() throws E;
    }
}
