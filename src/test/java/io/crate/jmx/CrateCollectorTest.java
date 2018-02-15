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
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.Enumeration;

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
        try {
            mbeanServer.unregisterMBean(new ObjectName(CrateDummyStatus.NAME));
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
}
