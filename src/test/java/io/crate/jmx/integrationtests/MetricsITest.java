package io.crate.jmx.integrationtests;

import org.junit.Test;

import java.net.HttpURLConnection;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;

public class MetricsITest extends AbstractITest {

    @Test
    public void testMetricEndpoint() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) randomJmxUrlFromServers("/metrics").openConnection();
        assertThat(connection.getResponseCode(), is(200));
        String res = parseResponse(connection.getInputStream());
        assertThat(res, containsString("crate_query_stats_query_average_duration{label=\"Select\",} 0.0\n"));
    }
}
