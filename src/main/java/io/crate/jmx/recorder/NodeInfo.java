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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NodeInfo implements Recorder {

    static final String MBEAN_NAME = "NodeInfo";

    private static final String NODE_ID_LABEL = "id";
    private static final String NODE_NAME_LABEL = "name";
    private static final List<String> LABEL_NAMES = Arrays.asList(NODE_ID_LABEL, NODE_NAME_LABEL);
    private static final Map<String, String> LABEL_VALUES = new HashMap<>(2);

    @Override
    public boolean recordBean(String domain,
                              String attrName,
                              String beanValue,
                              MetricSampleConsumer metricSampleConsumer) {
        String fullname = domain + "_node_info";
        boolean validAttribute = false;

        if (attrName.equalsIgnoreCase("NodeId")) {
            LABEL_VALUES.put(NODE_ID_LABEL, beanValue);
            validAttribute = true;
        } else if (attrName.equalsIgnoreCase("NodeName")) {
            LABEL_VALUES.put(NODE_NAME_LABEL, beanValue);
            validAttribute = true;
        }
        if (LABEL_VALUES.size() == 2) {
            List<String> labelValues = new ArrayList<>(2);
            labelValues.add(LABEL_VALUES.get(NODE_ID_LABEL));
            labelValues.add(LABEL_VALUES.get(NODE_NAME_LABEL));
            metricSampleConsumer.accept(
                    new Collector.MetricFamilySamples.Sample(
                            fullname,
                            LABEL_NAMES,
                            labelValues,
                            1.0),
                    Collector.Type.UNTYPED,
                    "Node information.");
        }
        return validAttribute;
    }

    @Override
    public boolean recordBean(String domain,
                              String attrName,
                              Number beanValue,
                              MetricSampleConsumer metricSampleConsumer) {
        throw new UnsupportedOperationException(QueryStats.class.getSimpleName() + " cannot be called with Number " +
                                                "bean value");
    }

    @Override
    public void reset() {
        LABEL_VALUES.clear();
    }
}
