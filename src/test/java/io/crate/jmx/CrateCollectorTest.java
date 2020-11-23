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
import java.util.List;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
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
        deregister(CircuitBreakers.NAME);
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
        assertThat(samples.name, is("crate_query_failed_count"));

        assertThat(samples.samples.size(), is(8));
        var sample = samples.samples.get(0);
        assertThat(sample.value, is(1.0));
        assertThat(sample.labelNames, hasItem(is("query")));
        assertThat(sample.labelValues, hasItem(is("Select")));

        // make test deterministic
        samples.samples.sort(Comparator.comparing(c -> c.value));

        sample = samples.samples.get(1);
        assertThat(sample.value, is(1.0));
        assertThat(sample.labelNames, hasItem(is("query")));
        assertThat(sample.labelValues, hasItem(is("Insert")));

        sample = samples.samples.get(2);
        assertThat(sample.value, is(1.0));
        assertThat(sample.labelNames, hasItem(is("query")));
        assertThat(sample.labelValues, hasItem(is("Update")));

        sample = samples.samples.get(3);
        assertThat(sample.value, is(1.0));
        assertThat(sample.labelNames, hasItem(is("query")));
        assertThat(sample.labelValues, hasItem(is("Delete")));

        sample = samples.samples.get(4);
        assertThat(sample.value, is(1.0));
        assertThat(sample.labelNames, hasItem(is("query")));
        assertThat(sample.labelValues, hasItem(is("Management")));

        sample = samples.samples.get(5);
        assertThat(sample.value, is(1.0));
        assertThat(sample.labelNames, hasItem(is("query")));
        assertThat(sample.labelValues, hasItem(is("DDL")));

        sample = samples.samples.get(6);
        assertThat(sample.value, is(1.0));
        assertThat(sample.labelNames, hasItem(is("query")));
        assertThat(sample.labelValues, hasItem(is("Copy")));

        sample = samples.samples.get(7);
        assertThat(sample.value, is(1.0));
        assertThat(sample.labelNames, hasItem(is("query")));
        assertThat(sample.labelValues, hasItem(is("Undefined")));

        samples = metrics.nextElement();
        assertThat(samples.name, is("crate_query_total_count"));
        assertThat(samples.samples.size(), is(8));

        // make test deterministic
        samples.samples.sort(Comparator.comparing(c -> String.join("", c.labelNames)));

        sample = samples.samples.get(0);
        assertThat(sample.value, is(1.0));
        assertThat(sample.labelNames, hasItem(is("query")));
        assertThat(sample.labelValues, hasItem(is("Select")));

        sample = samples.samples.get(1);
        assertThat(sample.value, is(1.0));
        assertThat(sample.labelNames, hasItem(is("query")));
        assertThat(sample.labelValues, hasItem(is("Insert")));

        sample = samples.samples.get(2);
        assertThat(sample.value, is(1.0));
        assertThat(sample.labelNames, hasItem(is("query")));
        assertThat(sample.labelValues, hasItem(is("Update")));

        sample = samples.samples.get(3);
        assertThat(sample.value, is(1.0));
        assertThat(sample.labelNames, hasItem(is("query")));
        assertThat(sample.labelValues, hasItem(is("Delete")));

        sample = samples.samples.get(4);
        assertThat(sample.value, is(1.0));
        assertThat(sample.labelNames, hasItem(is("query")));
        assertThat(sample.labelValues, hasItem(is("Management")));

        sample = samples.samples.get(5);
        assertThat(sample.value, is(1.0));
        assertThat(sample.labelNames, hasItem(is("query")));
        assertThat(sample.labelValues, hasItem(is("DDL")));

        sample = samples.samples.get(7);
        assertThat(sample.value, is(1.0));
        assertThat(sample.labelNames, hasItem(is("query")));
        assertThat(sample.labelValues, hasItem(is("Undefined")));

        samples = metrics.nextElement();
        // metrics crate_queries is removed from 4.3.2, it only exists for bwc reasons with older versions
        assertThat(samples.name, is("crate_queries"));
        assertThat(samples.samples.size(), is(1));
        assertThat(sample.labelNames, hasItem(is("query")));
        assertThat(sample.labelValues, hasItem(is("Undefined")));

        samples = metrics.nextElement();
        // metrics crate_query_sum_of_durations_millis is removed from 4.3.2, it only exists for bwc reasons with older versions
        assertThat(samples.name, is("crate_query_sum_of_durations_millis"));
        assertThat(samples.samples.size(), is(8));
        assertThat(sample.labelNames, hasItem(is("query")));
        assertThat(sample.labelValues, hasItem(is("Undefined")));

        samples = metrics.nextElement();
        // metrics crate_query_sum_of_durations_millis is removed from 4.3.2, it only exists for bwc reasons with older versions
        assertThat(samples.name, is("crate_query_duration_seconds"));
        assertThat(samples.samples.size(), is(1));
        assertThat(sample.labelNames, hasItem(is("query")));
        assertThat(sample.labelValues, hasItem(is("Undefined")));

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

        Collector.MetricFamilySamples shardStats = metrics.nextElement();
        assertThat(shardStats.name, is("crate_node"));

        // make test deterministic
        shardStats.samples.sort(Comparator.comparing(c -> String.join("", c.labelValues)));

        var shardStatsInfo = shardStats.samples.get(0);
        assertThat(shardStatsInfo.labelNames, is(Arrays.asList("name", "property", "id", "table", "partition_ident")));
        assertThat(shardStatsInfo.labelValues, is(Arrays.asList("shard_info", "size", "1", "test", "p1")));
        assertThat(shardStatsInfo.value, is(100.0));

        shardStatsInfo = shardStats.samples.get(1);
        assertThat(shardStatsInfo.labelNames, is(Arrays.asList("name", "property", "id", "table", "partition_ident")));
        assertThat(shardStatsInfo.labelValues, is(Arrays.asList("shard_info", "size", "2", "test", "p1")));
        assertThat(shardStatsInfo.value, is(500.0));

        shardStatsInfo = shardStats.samples.get(2);
        assertThat(shardStatsInfo.labelNames, is(Arrays.asList("name", "property", "id", "table", "partition_ident")));
        assertThat(shardStatsInfo.labelValues, is(Arrays.asList("shard_info", "size", "3", "test", "")));
        assertThat(shardStatsInfo.value, is(1000.0));

        Collector.MetricFamilySamples.Sample shardStatsSample = shardStats.samples.get(3);
        assertThat(shardStatsInfo.labelNames, is(Arrays.asList("name", "property", "id", "table", "partition_ident")));
        assertThat(shardStatsSample.labelValues, is(Arrays.asList("shard_stats", "primaries")));
        assertThat(shardStatsSample.value, is(1.0));

        shardStatsSample = shardStats.samples.get(4);
        assertThat(shardStatsSample.labelNames, is(Arrays.asList("name", "property")));
        assertThat(shardStatsSample.labelValues, is(Arrays.asList("shard_stats", "replicas")));
        assertThat(shardStatsSample.value, is(2.0));

        shardStatsSample = shardStats.samples.get(5);
        assertThat(shardStatsSample.labelNames, is(Arrays.asList("name", "property")));
        assertThat(shardStatsSample.labelValues, is(Arrays.asList("shard_stats", "total")));
        assertThat(shardStatsSample.value, is(3.0));

        shardStatsSample = shardStats.samples.get(6);
        assertThat(shardStatsSample.labelNames, is(Arrays.asList("name", "property")));
        assertThat(shardStatsSample.labelValues, is(Arrays.asList("shard_stats", "unassigned")));
        assertThat(shardStatsSample.value, is(0.0));

        Collector.MetricFamilySamples clusterStateVersionSample = metrics.nextElement();
        assertThat(clusterStateVersionSample.name, is("crate_cluster_state_version"));
        Collector.MetricFamilySamples.Sample clusterStateSample = clusterStateVersionSample.samples.get(0);
        assertThat(clusterStateSample.value, is(3.0));

        // all string attributes of the NodeInfo MBean are ignored by intend, so no more elements must exists.
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
        assertThat(metrics.nextElement().samples.size(), is(7));
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

    @Test
    public void testCircuitBreakersMXBean() throws Exception {
        mbeanServer.registerMBean(new CircuitBreakers(), new ObjectName(CircuitBreakers.NAME));
        CollectorRegistry registry = CollectorRegistry.defaultRegistry;

        Enumeration<Collector.MetricFamilySamples> metrics = registry.metricFamilySamples();

        Collector.MetricFamilySamples samples = metrics.nextElement();
        assertThat(metrics.hasMoreElements(), is(false));
        assertThat(samples.name, is("crate_circuitbreakers"));

        // make test deterministic
        samples.samples.sort(Comparator.comparing(c -> c.labelValues.get(0) + c.labelValues.get(1)));

        Collector.MetricFamilySamples.Sample sample = samples.samples.get(0);
        assertThat(sample.labelNames, is(Arrays.asList("name", "property")));
        assertThat(sample.labelValues, is(Arrays.asList("fielddata", "limit")));
        assertThat(sample.value, is(123456d));

        sample = samples.samples.get(1);
        assertThat(sample.labelNames, is(Arrays.asList("name", "property")));
        assertThat(sample.labelValues, is(Arrays.asList("fielddata", "used")));
        assertThat(sample.value, is(42d));

        sample = samples.samples.get(2);
        assertThat(sample.labelNames, is(Arrays.asList("name", "property")));
        assertThat(sample.labelValues, is(Arrays.asList("flightrequests", "limit")));
        assertThat(sample.value, is(123456d));

        sample = samples.samples.get(3);
        assertThat(sample.labelNames, is(Arrays.asList("name", "property")));
        assertThat(sample.labelValues, is(Arrays.asList("flightrequests", "used")));
        assertThat(sample.value, is(42.0d));

        sample = samples.samples.get(4);
        assertThat(sample.labelNames, is(Arrays.asList("name", "property")));
        assertThat(sample.labelValues, is(Arrays.asList("jobslog", "limit")));
        assertThat(sample.value, is(123456d));

        sample = samples.samples.get(5);
        assertThat(sample.labelNames, is(Arrays.asList("name", "property")));
        assertThat(sample.labelValues, is(Arrays.asList("jobslog", "used")));
        assertThat(sample.value, is(42.0d));

        sample = samples.samples.get(6);
        assertThat(sample.labelNames, is(Arrays.asList("name", "property")));
        assertThat(sample.labelValues, is(Arrays.asList("operationlog", "limit")));
        assertThat(sample.value, is(123456d));

        sample = samples.samples.get(7);
        assertThat(sample.labelNames, is(Arrays.asList("name", "property")));
        assertThat(sample.labelValues, is(Arrays.asList("operationlog", "used")));
        assertThat(sample.value, is(42.0d));

        sample = samples.samples.get(8);
        assertThat(sample.labelNames, is(Arrays.asList("name", "property")));
        assertThat(sample.labelValues, is(Arrays.asList("parent", "limit")));
        assertThat(sample.value, is(123456d));

        sample = samples.samples.get(9);
        assertThat(sample.labelNames, is(Arrays.asList("name", "property")));
        assertThat(sample.labelValues, is(Arrays.asList("parent", "used")));
        assertThat(sample.value, is(42.0d));

        sample = samples.samples.get(10);
        assertThat(sample.labelNames, is(Arrays.asList("name", "property")));
        assertThat(sample.labelValues, is(Arrays.asList("query", "limit")));
        assertThat(sample.value, is(123456d));

        sample = samples.samples.get(11);
        assertThat(sample.labelNames, is(Arrays.asList("name", "property")));
        assertThat(sample.labelValues, is(Arrays.asList("query", "used")));
        assertThat(sample.value, is(22.0d));

        sample = samples.samples.get(12);
        assertThat(sample.labelNames, is(Arrays.asList("name", "property")));
        assertThat(sample.labelValues, is(Arrays.asList("request", "limit")));
        assertThat(sample.value, is(123456d));

        sample = samples.samples.get(13);
        assertThat(sample.labelNames, is(Arrays.asList("name", "property")));
        assertThat(sample.labelValues, is(Arrays.asList("request", "used")));
        assertThat(sample.value, is(42.0d));
    }

    public interface QueryStatsMBean {

        // Removed from 4.3.2, only exists for bwc
        double getSelectQueryFrequency();

        // Removed from 4.3.2, only exists for bwc
        double getSelectQueryAverageDuration();

        long getSelectQueryTotalCount();

        long getInsertQueryTotalCount();

        long getUpdateQueryTotalCount();

        long getDeleteQueryTotalCount();

        long getManagementQueryTotalCount();

        long getDDLQueryTotalCount();

        long getCopyQueryTotalCount();

        long getUndefinedQueryTotalCount();

        long getSelectQuerySumOfDurations();

        long getInsertQuerySumOfDurations();

        long getUpdateQuerySumOfDurations();

        long getDeleteQuerySumOfDurations();

        long getManagementQuerySumOfDurations();

        long getDDLQuerySumOfDurations();

        long getCopyQuerySumOfDurations();

        long getUndefinedQuerySumOfDurations();

        long getSelectQueryFailedCount();

        long getInsertQueryFailedCount();

        long getUpdateQueryFailedCount();

        long getDeleteQueryFailedCount();

        long getManagementQueryFailedCount();

        long getDDLQueryFailedCount();

        long getCopyQueryFailedCount();

        long getUndefinedQueryFailedCount();
    }

    private static class QueryStats implements QueryStatsMBean {

        static final String NAME = "io.crate.monitoring:type=QueryStats";

        // Removed from 4.3.2, only exists for bwc
        @Override
        public double getSelectQueryFrequency() {
            return 2;
        }

        // Removed from 4.3.2, only exists for bwc
        @Override
        public double getSelectQueryAverageDuration() {
            return 1.2;
        }

        @Override
        public long getSelectQueryTotalCount() {
            return 1;
        }

        @Override
        public long getInsertQueryTotalCount() {
            return 1;
        }

        @Override
        public long getUpdateQueryTotalCount() {
            return 1;
        }

        @Override
        public long getDeleteQueryTotalCount() {
            return 1;
        }

        @Override
        public long getManagementQueryTotalCount() {
            return 1;
        }

        @Override
        public long getDDLQueryTotalCount() {
            return 1;
        }

        @Override
        public long getCopyQueryTotalCount() {
            return 1;
        }

        @Override
        public long getUndefinedQueryTotalCount() {
            return 1;
        }

        @Override
        public long getSelectQuerySumOfDurations() {
            return 1;
        }

        @Override
        public long getInsertQuerySumOfDurations() {
            return 1;
        }

        @Override
        public long getUpdateQuerySumOfDurations() {
            return 1;
        }

        @Override
        public long getDeleteQuerySumOfDurations() {
            return 1;
        }

        @Override
        public long getManagementQuerySumOfDurations() {
            return 1;
        }

        @Override
        public long getDDLQuerySumOfDurations() {
            return 1;
        }

        @Override
        public long getCopyQuerySumOfDurations() {
            return 1;
        }

        @Override
        public long getUndefinedQuerySumOfDurations() {
            return 1;
        }

        @Override
        public long getSelectQueryFailedCount() {
            return 1;
        }

        @Override
        public long getInsertQueryFailedCount() {
            return 1;
        }

        @Override
        public long getUpdateQueryFailedCount() {
            return 1;
        }

        @Override
        public long getDeleteQueryFailedCount() {
            return 1;
        }

        @Override
        public long getManagementQueryFailedCount() {
            return 1;
        }

        @Override
        public long getDDLQueryFailedCount() {
            return 1;
        }

        @Override
        public long getCopyQueryFailedCount() {
            return 1;
        }

        @Override
        public long getUndefinedQueryFailedCount() {
            return 1;
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

    public interface CrateDummyNodeInfoMXBean {

        long getClusterStateVersion();

        String getNodeId();

        ShardStats getShardStats();

        List<ShardInfo> getShardInfo();

    }

    private static class CrateDummyNodeInfo implements CrateDummyNodeInfoMXBean {

        static final String NAME = "io.crate.monitoring:type=NodeInfo";

        @Override
        public long getClusterStateVersion() {
            return 3L;
        }

        @Override
        public String getNodeId() {
            return "node1";
        }

        public ShardStats getShardStats() {
            return new ShardStats(3, 1, 2, 0, 0);
        }

        public List<ShardInfo> getShardInfo() {
            return List.of(
                    new ShardInfo(1, "test", "p1", "STARTED", "STARTED", 100),
                    new ShardInfo(2, "test", "p1", "STARTED", "STARTED", 500),
                    new ShardInfo(3, "test", "", "STARTED", "STARTED", 1000)
            );
        }
    }

    public static class ShardStats {

        final int total;

        final int primaries;

        final int replicas;

        final int unassigned;

        @ConstructorProperties({"total", "primaries", "replicas", "unassigned"})
        public ShardStats(int total, int primaries, int replicas, int unassigned, int recovering) {
            this.total = total;
            this.primaries = primaries;
            this.replicas = replicas;
            this.unassigned = unassigned;
        }

        public int getTotal() {
            return total;
        }

        public int getPrimaries() {
            return primaries;
        }

        public int getReplicas() {
            return replicas;
        }

        public int getUnassigned() {
            return unassigned;
        }

    }

    public static class ShardInfo {
        final int shardId;
        final String routingState;
        final String state;
        final String table;
        final String partitionIdent;
        final long size;

        @ConstructorProperties({"shardId", "table", "partitionIdent", "routingState", "state", "size"})
        public ShardInfo(int shardId, String table, String partitionIdent, String routingState, String state, long size) {
            this.shardId = shardId;
            this.routingState = routingState;
            this.state = state;
            this.table = table;
            this.partitionIdent = partitionIdent;
            this.size = size;
        }

        public int getShardId() {
            return shardId;
        }

        public String getTable() {
            return table;
        }

        public String getRoutingState() {
            return routingState;
        }

        public String getState() {
            return state;
        }

        public long getSize() {
            return size;
        }

        public String getPartitionIdent() {
            return partitionIdent;
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

    @SuppressWarnings("unused")
    public interface CircuitBreakersMXBean {

        CircuitBreakers.Stats getParent();

        CircuitBreakers.Stats getFieldData();

        CircuitBreakers.Stats getInFlightRequests();

        CircuitBreakers.Stats getRequest();

        CircuitBreakers.Stats getQuery();

        CircuitBreakers.Stats getJobsLog();

        CircuitBreakers.Stats getOperationsLog();

    }

    private static class CircuitBreakers implements CircuitBreakersMXBean {

        static final String NAME = "io.crate.monitoring:type=CircuitBreakers";

        public static class Stats {

            private final String name;
            private final long limit;
            private final long used;

            @SuppressWarnings("WeakerAccess")
            @ConstructorProperties({"name", "limit", "used"})
            public Stats(String name, long limit, long used) {
                this.name = name;
                this.limit = limit;
                this.used = used;
            }

            @SuppressWarnings("unused")
            public String getName() {
                return name;
            }

            @SuppressWarnings("unused")
            public long getLimit() {
                return limit;
            }

            @SuppressWarnings("unused")
            public long getUsed() {
                return used;
            }
        }

        @Override
        public Stats getParent() {
            return new Stats("parent", 123456, 42);
        }

        @Override
        public Stats getFieldData() {
            return new Stats("fielddata", 123456, 42);
        }

        @Override
        public Stats getInFlightRequests() {
            return new Stats("flightrequests", 123456, 42);
        }

        @Override
        public Stats getRequest() {
            return new Stats("request", 123456, 42);
        }

        @Override
        public Stats getQuery() {
            return new Stats("query", 123456, 22);
        }

        @Override
        public Stats getJobsLog() {
            return new Stats("jobslog", 123456, 42);
        }

        @Override
        public Stats getOperationsLog() {
            return new Stats("operationlog", 123456, 42);

        }
    }
}
