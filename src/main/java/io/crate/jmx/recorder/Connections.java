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

import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Connections implements Recorder {

    public static final String MBEAN_NAME = "Connections";

    private static final Pattern CONNECTIONS_PATTERN = Pattern.compile(
            "(?<protocol>(Psql|Http|Transport))" +
            "(?<property>(Open|Total|MessagesReceived|BytesReceived|MessagesSent|BytesSent))");


    @Override
    public boolean recordBean(String domain,
                              String attrName,
                              Number beanValue,
                              MetricSampleConsumer metricSampleConsumer) {

        Matcher matcher = CONNECTIONS_PATTERN.matcher(attrName);
        if (!matcher.matches()) {
            return false;
        }
        String property = matcher.group("property").toLowerCase(Locale.ENGLISH);
        String protocol = matcher.group("protocol").toLowerCase(Locale.ENGLISH);
        metricSampleConsumer.accept(
                new Collector.MetricFamilySamples.Sample(
                        domain + '_' + "connections",
                        Arrays.asList("protocol", "property"),
                        Arrays.asList(protocol, property),
                        beanValue.longValue()
                ),
                Collector.Type.GAUGE,
                "Number of " + property + " connections established via " + protocol
        );
        return true;
    }
}
