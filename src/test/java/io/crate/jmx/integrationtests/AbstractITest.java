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
import com.sun.tools.attach.VirtualMachineDescriptor;
import io.crate.testing.CrateTestCluster;
import io.crate.testing.CrateTestServer;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

public abstract class AbstractITest {

    protected static final String LATEST_URL = "https://cdn.crate.io/downloads/releases/nightly/crate-latest.tar.gz";

    protected static String metricsResponse;

    private static final int JMX_HTTP_PORT = 17071;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private static CrateTestCluster testCluster;

    String getCrateDistributionURL() {
        return LATEST_URL;
    }

    @Before
    public void setup() throws Throwable {
        if (testCluster == null) {
            setUpClusterAndAgent(getCrateDistributionURL());
            metricsResponse = parseMartricsResponse();
        }
    }


    protected static void setUpClusterAndAgent(String url) throws Throwable {
        CrateTestCluster.Builder builder;
        builder = CrateTestCluster.fromURL(url);
        testCluster = builder.keepWorkingDir(false).build();
        testCluster.before();
        String pid = getCratePID();
        attachAgent(pid);
    }

    protected static String parseMartricsResponse() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) randomJmxUrlFromServers("/metrics").openConnection();
        assertThat(connection.getResponseCode(), is(200));
        return parseResponse(connection.getInputStream());
    }

    @AfterClass
    public static void tearDown() {
        testCluster.after();
        testCluster = null;
    }

    private static String getCratePID() {
        for (VirtualMachineDescriptor desc : VirtualMachine.list()) {
            if (desc.displayName().contains("CrateDB")) {
                return desc.id();
            }
        }
        throw new RuntimeException("Cannot find the CrateDB PID");
    }

    private static void attachAgent(String pid) throws Exception {
        File agentJar = findAgentJar();
        String options = String.format("%d", JMX_HTTP_PORT);
        VirtualMachine vm = VirtualMachine.attach(pid);
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
        int startIdx = metricsResponse.indexOf(metricString);
        assertThat(metricString + " not found in response", startIdx, greaterThanOrEqualTo(0));
        int endIdx = metricsResponse.indexOf("\n", startIdx);
        String metricValueStr = metricsResponse.substring(startIdx + metricString.length(), endIdx);
        assertThat(
                String.format("Metric '%s' value '%s' must be in the format ^-\\d+.\\d+(E\\d+)?", metricString, metricValueStr),
                metricValueStr.matches("^-?\\d+.\\d+(E\\d+)?"),
                is(true)
        );
    }
}
