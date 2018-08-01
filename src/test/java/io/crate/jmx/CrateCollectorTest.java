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

package io.crate.jmx;

import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.beans.ConstructorProperties;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;

public class CrateCollectorTest {

    private CrateCollector crateCollector;
    private MBeanServer mbeanServer;
    private MBeanAttributeValueStorage beanAttributeValueStorage = new MBeanAttributeValueStorage();

    @Before
    public void setUpCollectorAndMbeanServer() {
        crateCollector = new CrateCollector(beanAttributeValueStorage::put).register();
        mbeanServer = ManagementFactory.getPlatformMBeanServer();
    }

    @After
    public void unregisterMBean() throws Exception {
        CollectorRegistry.defaultRegistry.clear();
        deregister(QueryStats.NAME);
        deregister(CrateDummyStatus.NAME);
        deregister(CrateDummyNodeInfo.NAME);
        deregister(Connections.NAME);
        deregister(ThreadPools.NAME);
    }

    private void deregister(String name) throws MBeanRegistrationException, MalformedObjectNameException {
        try {
            mbeanServer.unregisterMBean(new ObjectName(name));
        } catch (InstanceNotFoundException ignored) {
        }
    }

    @Test
    public void testNoCrateMBeanFound() {
        CollectorRegistry registry = CollectorRegistry.defaultRegistry;
        Enumeration<Collector.MetricFamilySamples> metrics = registry.metricFamilySamples();
        assertThat(metrics.hasMoreElements(), is(false));
    }

    @Test
    public void testQueryStatsMBean() throws Exception {
        mbeanServer.registerMBean(new QueryStats(), new ObjectName(QueryStats.NAME));

        CollectorRegistry registry = CollectorRegistry.defaultRegistry;
        // metrics are collected while iterating on registered ones
        Enumeration<Collector.MetricFamilySamples> metrics = registry.metricFamilySamples();
        assertThat(metrics.hasMoreElements(), is(true));

        Collector.MetricFamilySamples samples = metrics.nextElement();
        assertThat(samples.name, is("crate_queries"));

        assertThat(samples.samples.size(), is(1));
        Collector.MetricFamilySamples.Sample sample = samples.samples.get(0);
        assertThat(sample.value, is(2.0));
        assertThat(sample.labelNames, hasItem(is("query")));
        assertThat(sample.labelValues, hasItem(is("Select")));

        samples = metrics.nextElement();
        assertThat(samples.name, is("crate_query_duration_seconds"));
        assertThat(samples.samples.size(), is(1));
        sample = samples.samples.get(0);
        assertThat(sample.value, is(1.2));
        assertThat(sample.labelNames, hasItem(is("query")));
        assertThat(sample.labelValues, hasItem(is("Select")));
    }

    @Test
    public void testConnectionsMBean() throws Exception {
        mbeanServer.registerMBean(new Connections(), new ObjectName(Connections.NAME));
        CollectorRegistry registry = CollectorRegistry.defaultRegistry;

        Enumeration<Collector.MetricFamilySamples> metrics = registry.metricFamilySamples();

        Collector.MetricFamilySamples samples = metrics.nextElement();
        assertThat(metrics.hasMoreElements(), is(false));
        assertThat(samples.name, is("crate_connections"));

        // make test deterministic
        samples.samples.sort(Comparator.comparing(c -> c.value));

        Collector.MetricFamilySamples.Sample fst = samples.samples.get(0);
        assertThat(fst.value, is(1.0d));
        assertThat(fst.labelNames, is(Arrays.asList("protocol", "property")));
        assertThat(fst.labelValues, is(Arrays.asList("http", "open")));

        Collector.MetricFamilySamples.Sample snd = samples.samples.get(1);
        assertThat(snd.value, is(2.0d));
        assertThat(snd.labelValues, is(Arrays.asList("http", "total")));

        Collector.MetricFamilySamples.Sample third = samples.samples.get(2);
        assertThat(third.value, is(3.0d));
        assertThat(third.labelValues, is(Arrays.asList("psql", "open")));

        Collector.MetricFamilySamples.Sample fourth = samples.samples.get(3);
        assertThat(fourth.value, is(4.0d));
        assertThat(fourth.labelValues, is(Arrays.asList("psql", "total")));

        Collector.MetricFamilySamples.Sample fifth = samples.samples.get(4);
        assertThat(fifth.value, is(5.0d));
        assertThat(fifth.labelValues, is(Arrays.asList("transport", "open")));
    }

    @Test
    public void testCrateMBeanFoundAndAttributesReadAsDefault() throws Exception {
        mbeanServer.registerMBean(new CrateDummyStatus(), new ObjectName(CrateDummyStatus.NAME));

        CollectorRegistry registry = CollectorRegistry.defaultRegistry;
        // metrics are collected while iterating on registered ones
        Enumeration<Collector.MetricFamilySamples> metrics = registry.metricFamilySamples();
        assertThat(metrics.hasMoreElements(), is(true));

        Collector.MetricFamilySamples samples = metrics.nextElement();
        assertThat(samples.name, is("crate_dummy_status_something_enabled"));

        Collector.MetricFamilySamples.Sample sample = samples.samples.get(0);
        assertThat(sample.value, is(1.0));
        assertThat(sample.labelNames.size(), is(0));
        assertThat(sample.labelValues.size(), is(0));

        samples = metrics.nextElement();
        assertThat(samples.name, is("crate_dummy_status_select_stats"));

        sample = samples.samples.get(0);
        assertThat(sample.value, is(123.0));
        assertThat(sample.labelNames.size(), is(0));
        assertThat(sample.labelValues.size(), is(0));
    }

    @Test
    public void testCrateMBeanAttributeValuesAreStored() throws Exception {
        CrateDummyStatus dummyBean = new CrateDummyStatus();
        mbeanServer.registerMBean(dummyBean, new ObjectName(CrateDummyStatus.NAME));

        // manually start metric collection
        crateCollector.collect();

        assertThat(beanAttributeValueStorage.get("DummyStatus_SomethingEnabled"), is(true));
        assertThat(beanAttributeValueStorage.get("DummyStatus_SelectStats"), is(123L));

        dummyBean.boolValue = false;
        crateCollector.collect();
        assertThat(beanAttributeValueStorage.get("DummyStatus_SomethingEnabled"), is(false));
    }

    @Test
    public void testNodeInfoMBean() throws Exception {
        mbeanServer.registerMBean(new CrateDummyNodeInfo(), new ObjectName(CrateDummyNodeInfo.NAME));

        // metrics are collected while iterating on registered ones
        Enumeration<Collector.MetricFamilySamples> metrics = CollectorRegistry.defaultRegistry.metricFamilySamples();
        assertThat(metrics.hasMoreElements(), is(true));

        Collector.MetricFamilySamples nodeInfoSample = metrics.nextElement();
        assertThat(nodeInfoSample.name, is("crate_node_info"));

        assertThat(nodeInfoSample.samples.size(), is(1));
        Collector.MetricFamilySamples.Sample nodeSample = nodeInfoSample.samples.get(0);
        assertThat(nodeSample.value, is(1.0));
        assertThat(nodeSample.labelNames, contains("id", "name"));
        assertThat(nodeSample.labelValues, contains("testNodeId", "testNodeName"));

        assertThat(metrics.hasMoreElements(), is(true));

        Collector.MetricFamilySamples clusterStateVersionSample = metrics.nextElement();
        assertThat(clusterStateVersionSample.name, is("crate_cluster_state_version"));
        Collector.MetricFamilySamples.Sample clusterStateSample = clusterStateVersionSample.samples.get(0);
        assertThat(clusterStateSample.value, is(3.0));

        assertThat(metrics.hasMoreElements(), is(false));
    }

    @Test
    public void testNodeInfoResetsBeforeNextCollect() throws Exception {
        mbeanServer.registerMBean(new CrateDummyNodeInfo(), new ObjectName(CrateDummyNodeInfo.NAME));

        // Call collect multiple times
        crateCollector.collect();
        crateCollector.collect();

        Enumeration<Collector.MetricFamilySamples> metrics = CollectorRegistry.defaultRegistry.metricFamilySamples();
        assertThat(metrics.hasMoreElements(), is(true));
        assertThat(metrics.nextElement().samples.size(), is(1)); // Only one sample
    }

    @Test
    public void testThreadPoolsMXBean() throws Exception {
        mbeanServer.registerMBean(new ThreadPools(), new ObjectName(ThreadPools.NAME));
        CollectorRegistry registry = CollectorRegistry.defaultRegistry;

        Enumeration<Collector.MetricFamilySamples> metrics = registry.metricFamilySamples();

        Collector.MetricFamilySamples samples = metrics.nextElement();
        assertThat(metrics.hasMoreElements(), is(false));
        assertThat(samples.name, is("crate_threadpools"));

        // make test deterministic
        samples.samples.sort(Comparator.comparing(c -> c.labelValues.get(0) + c.labelValues.get(1)));

        Collector.MetricFamilySamples.Sample sample = samples.samples.get(0);
        assertThat(sample.labelNames, is(Arrays.asList("name", "property")));
        assertThat(sample.labelValues, is(Arrays.asList("generic", "active")));
        assertThat(sample.value, is(4.0d));

        sample = samples.samples.get(1);
        assertThat(sample.labelNames, is(Arrays.asList("name", "property")));
        assertThat(sample.labelValues, is(Arrays.asList("generic", "completed")));
        assertThat(sample.value, is(20.0d));

        sample = samples.samples.get(2);
        assertThat(sample.labelNames, is(Arrays.asList("name", "property")));
        assertThat(sample.labelValues, is(Arrays.asList("search", "active")));
        assertThat(sample.value, is(2.0d));

        sample = samples.samples.get(3);
        assertThat(sample.labelNames, is(Arrays.asList("name", "property")));
        assertThat(sample.labelValues, is(Arrays.asList("search", "completed")));
        assertThat(sample.value, is(42.0d));
    }

    public interface QueryStatsMBean {

        double getSelectQueryFrequency();

        double getSelectQueryAverageDuration();
    }

    private static class QueryStats implements QueryStatsMBean {

        static final String NAME = "io.crate.monitoring:type=QueryStats";
        @Override
        public double getSelectQueryFrequency() {
            return 2;
        }

        @Override
        public double getSelectQueryAverageDuration() {
            return 1.2;
        }
    }

    public interface CrateDummyStatusMBean {

        long getSelectStats();

        boolean isSomethingEnabled();
    }

    private static class CrateDummyStatus implements CrateDummyStatusMBean {

        static final String NAME = "io.crate.monitoring:type=DummyStatus";

        private boolean boolValue = true;

        @Override
        public long getSelectStats() {
            return 123L;
        }

        @Override
        public boolean isSomethingEnabled() {
            return boolValue;
        }
    }

    public interface CrateDummyNodeInfoMBean {

        String getNodeId();

        String getNodeName();

        long getClusterStateVersion();
    }

    private static class CrateDummyNodeInfo implements CrateDummyNodeInfoMBean{

        static final String NAME = "io.crate.monitoring:type=NodeInfo";

        @Override
        public String getNodeId() {
            return "testNodeId";
        }

        @Override
        public String getNodeName() {
            return "testNodeName";
        }

        @Override
        public long getClusterStateVersion() {
            return 3L;
        }
    }

    public interface ConnectionsMBean {

        long getHttpOpen();

        long getHttpTotal();

        long getPsqlOpen();

        long getPsqlTotal();

        long getTransportOpen();
    }

    private static class Connections implements ConnectionsMBean {

        static final String NAME = "io.crate.monitoring:type=Connections";

        @Override
        public long getHttpOpen() {
            return 1;
        }

        @Override
        public long getHttpTotal() {
            return 2;
        }

        @Override
        public long getPsqlOpen() {
            return 3;
        }

        @Override
        public long getPsqlTotal() {
            return 4;
        }

        @Override
        public long getTransportOpen() {
            return 5;
        }
    }


    public static class ThreadPoolInfo {

        private final String name;
        private final int active;
        private final long completed;

        @ConstructorProperties({"name", "active", "completed"})
        public ThreadPoolInfo(String name, int active, long completed) {
            this.name = name;
            this.active = active;
            this.completed = completed;
        }

        public String getName() {
            return name;
        }

        public int getActive() {
            return active;
        }

        public long getCompleted() {
            return completed;
        }
    }

    public interface ThreadPoolsMXBean {

        ThreadPoolInfo getGeneric();

        ThreadPoolInfo getSearch();
    }

    private static class ThreadPools implements ThreadPoolsMXBean {

        static final String NAME = "io.crate.monitoring:type=ThreadPools";

        @Override
        public ThreadPoolInfo getGeneric() {
            return new ThreadPoolInfo("generic", 4, 20);
        }

        @Override
        public ThreadPoolInfo getSearch() {
            return new ThreadPoolInfo("search", 2, 42);
        }
    }
}
