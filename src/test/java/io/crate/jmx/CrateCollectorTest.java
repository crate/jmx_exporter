package io.crate.jmx;

import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import org.junit.Before;
import org.junit.Test;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.Enumeration;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class CrateCollectorTest {

    private MBeanServer mbeanServer;

    @Before
    public void setUpCollectorAndMbeanServer() {
        new CrateCollector().register();
        mbeanServer = ManagementFactory.getPlatformMBeanServer();
    }

    @Test
    public void testNoCrateMBeanFound() {
        CollectorRegistry registry = CollectorRegistry.defaultRegistry;
        Enumeration<Collector.MetricFamilySamples> metrics = registry.metricFamilySamples();
        assertThat(metrics.hasMoreElements(), is(false));
    }

    @Test
    public void testCrateMBeanFoundAndAttributesRead() throws Exception {
        mbeanServer.registerMBean(new CrateDummyStatus(), new ObjectName(CrateDummyStatus.NAME));

        CollectorRegistry registry = CollectorRegistry.defaultRegistry;
        Enumeration<Collector.MetricFamilySamples> metrics = registry.metricFamilySamples();
        assertThat(metrics.hasMoreElements(), is(true));

        // MBean's attributes are added in alphabetic order
        // first property, a long.
        Collector.MetricFamilySamples samples = metrics.nextElement();
        assertThat(samples.name, is("crate_dummy_status_stats"));

        Collector.MetricFamilySamples.Sample sample = samples.samples.get(0);
        assertThat(sample.value, is(123.0));
        assertThat(sample.labelNames, hasItem(is("label")));
        assertThat(sample.labelValues, hasItem(is("Select")));

        // second property, a boolean
        samples = metrics.nextElement();
        assertThat(samples.name, is("crate_dummy_status_enabled"));

        sample = samples.samples.get(0);
        assertThat(sample.value, is(1.0));
        assertThat(sample.labelNames, hasItem(is("label")));
        assertThat(sample.labelValues, hasItem(is("Something: true")));
    }

    public interface CrateDummyStatusMBean {

        long getSelectStats();

        boolean isSomethingEnabled();
    }

    private static class CrateDummyStatus implements CrateDummyStatusMBean {

        static final String NAME = "io.crate.monitoring:type=DummyStatus";

        @Override
        public long getSelectStats() {
            return 123L;
        }

        @Override
        public boolean isSomethingEnabled() {
            return true;
        }
    }
}
