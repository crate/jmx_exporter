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

package io.crate.jmx.recorder;

import io.prometheus.client.Collector;

import javax.management.openmbean.CompositeData;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

public class ThreadPools implements Recorder {

    static final String MBEAN_NAME = "ThreadPools";

    @Override
    public boolean recordBean(String domain,
                              String attrName,
                              Number beanValue,
                              MetricSampleConsumer metricSampleConsumer) {
        throw new UnsupportedOperationException(ThreadPools.class.getSimpleName() + " cannot be called with Numeric " +
                                                "bean value");
    }

    @Override
    public boolean recordBean(String domain,
                              String attrName,
                              String beanValue,
                              MetricSampleConsumer metricSampleConsumer) {
        throw new UnsupportedOperationException(ThreadPools.class.getSimpleName() + " cannot be called with String " +
                                                "bean value");
    }

    @Override
    public boolean recordBean(String domain,
                              String attrName,
                              CompositeData beanValue,
                              MetricSampleConsumer metricSampleConsumer) {
        Set<String> names = beanValue.getCompositeType().keySet();
        String poolName = ((String) beanValue.get("name")).toLowerCase(Locale.ENGLISH);

        for (String propertyName : names) {
            Object value = beanValue.get(propertyName);
            if ((value instanceof Number) == false) {
                // we're not interested in non-numeric values e.g. pool name
                continue;
            }

            metricSampleConsumer.accept(
                    new Collector.MetricFamilySamples.Sample(
                            domain + '_' + "threadpools",
                            Arrays.asList("name", "property"),
                            Arrays.asList(poolName, propertyName),
                            ((Number) value).longValue()
                    ),
                    Collector.Type.GAUGE,
                    "Statistics of thread pools"
            );
        }
        return true;
    }
}
