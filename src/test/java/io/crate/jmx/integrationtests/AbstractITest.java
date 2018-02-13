package io.crate.jmx.integrationtests;

import com.carrotsearch.randomizedtesting.RandomizedTest;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import io.crate.testing.CrateTestCluster;
import io.crate.testing.CrateTestServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Random;

public abstract class AbstractITest extends RandomizedTest {

    // TODO: remove this and use concrete version once v3.0.0 is released
    private static final String LATEST_URL = "https://cdn.crate.io/downloads/releases/nightly/crate-3.0.0-201802120203-a2f4bb9.tar.gz";
    private static String[] CRATE_VERSIONS = new String[]{"latest"};

    private static final int JMX_HTTP_PORT = 17071;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private static CrateTestCluster testCluster;

    private static String getRandomServerVersion() {
        String version = System.getenv().get("CRATE_VERSION");
        if (version != null) {
            return version;
        }
        Random random = getRandom();
        return CRATE_VERSIONS[random.nextInt(CRATE_VERSIONS.length)];
    }

    @BeforeClass
    public static void setUpClusterAndAgent() throws Throwable {
        String version = getRandomServerVersion();
        CrateTestCluster.Builder builder;

        // TODO: remove the latest branch and use concrete version once v3.0.0 is released
        if (version.equals("latest") && System.getenv().get("CRATE_VERSION") == null) {
            builder = CrateTestCluster.fromURL(LATEST_URL);
        } else {
            builder = CrateTestCluster.fromVersion(getRandomServerVersion());
        }
        testCluster = builder
                .keepWorkingDir(false)
                .build();

        testCluster.before();

        String pid = getCratePID();
        attachAgent(pid);
    }

    @AfterClass
    public static void tearDown() {
        testCluster.after();
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
        String options = String.format(":%d", JMX_HTTP_PORT);
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
            if (file.getName().contains("crate_jmx_agent")) {
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
}
