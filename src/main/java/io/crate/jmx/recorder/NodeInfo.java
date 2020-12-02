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
import java.util.List;
import java.util.Set;
import java.util.Arrays;
import java.util.Locale;
import java.util.Collections;


public class NodeInfo implements Recorder {

    static final String MBEAN_NAME = "NodeInfo";

    @Override
    public boolean recordBean(String domain, String attrName, CompositeData beanValue, MetricSampleConsumer metricSampleConsumer) {
        if (!"ShardStats".equals(attrName)) {
            return false;
        }
        Set<String> names = beanValue.getCompositeType().keySet();
        for (String propertyName : names) {
            Object value = beanValue.get(propertyName);
            if (value instanceof String) {
                // we're not interested in non-numeric values e.g. name
                continue;
            }
            metricSampleConsumer.accept(
                    new Collector.MetricFamilySamples.Sample(
                            domain + '_' + "node",
                            Arrays.asList("name", "property"),
                            Arrays.asList("shard_stats", propertyName.toLowerCase(Locale.getDefault())),
                            ((Number) value).longValue()
                    ),
                    Collector.Type.GAUGE,
                    "Statistics for Shards located on the Node."
            );
        }
        return true;
    }

    @Override
    public boolean recordBean(String domain, String attrName, CompositeData[] beanValue, MetricSampleConsumer metricSampleConsumer) {
        if (!"ShardInfo".equals(attrName)) {
            return false;
        }
        for (CompositeData compositeData : beanValue) {
            Integer shardId = (Integer) compositeData.get("shardId");
            String table = (String) compositeData.get("table");
            Long size = (Long) compositeData.get("size");
            String partitionIdent = (String) compositeData.get("partitionIdent");

            metricSampleConsumer.accept(
                    new Collector.MetricFamilySamples.Sample(
                            domain + '_' + "node",
                            List.of("name", "property", "id", "table", "partition_ident"),
                            Arrays.asList("shard_info", "size", shardId.toString(), table, partitionIdent),
                            size
                    ),
                    Collector.Type.GAUGE,
                    "Information for Shards located on the Node."
            );
        }
        return true;
    }

    @Override
    public boolean recordBean(String domain,
                              String attrName,
                              Number beanValue,
                              MetricSampleConsumer metricSampleConsumer) {
        String fullname = domain + "_cluster_state_version";
        boolean validAttribute = false;

        if (attrName.equalsIgnoreCase("ClusterStateVersion")) {
            metricSampleConsumer.accept(
                    new Collector.MetricFamilySamples.Sample(
                            fullname,
                            Collections.emptyList(),
                            Collections.emptyList(),
                            beanValue.doubleValue()),
                    Collector.Type.UNTYPED,
                    "Cluster information.");
            validAttribute = true;
        }
        return validAttribute;
    }
}
