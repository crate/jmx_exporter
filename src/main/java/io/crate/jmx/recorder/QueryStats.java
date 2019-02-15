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

import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QueryStats implements Recorder {

    static final String MBEAN_NAME = "QueryStats";

    private static final Pattern QUERIES_PER_SECONDS_PATTERN = Pattern.compile("(.+)QueryFrequency");
    private static final Pattern QUERIES_DURATION_PATTERN = Pattern.compile("(.+)QueryAverageDuration");
    private static final Pattern QUERIES_TOTAL_COUNT_PATTERN = Pattern.compile("(.+)QueryTotalCount");
    private static final Pattern QUERIES_FAILED_COUNT_PATTERN = Pattern.compile("(.+)QueryFailedCount");
    private static final Pattern QUERIES_SUM_OF_DURATIONS_PATTERN = Pattern.compile("(.+)QuerySumOfDurations");

    @Override
    public boolean recordBean(String domain,
                              String attrName,
                              Number beanValue,
                              MetricSampleConsumer metricSampleConsumer) {
        Matcher matcher = QUERIES_PER_SECONDS_PATTERN.matcher(attrName);
        if (matcher.matches()) {
            recordBean(
                    domain,
                    "queries",
                    matcher.group(1),
                    beanValue,
                    "Queries per second for a given query type.",
                    metricSampleConsumer);
            return true;
        }
        matcher = QUERIES_DURATION_PATTERN.matcher(attrName);
        if (matcher.matches()) {
            recordBean(
                    domain,
                    "query_duration_seconds",
                    matcher.group(1),
                    beanValue,
                    "The average query duration for a given query type.",
                    metricSampleConsumer);
            return true;
        }
        matcher = QUERIES_TOTAL_COUNT_PATTERN.matcher(attrName);
        if (matcher.matches()) {
            recordBean(
                    domain,
                    "query_total_count",
                    matcher.group(1),
                    beanValue,
                    "The total number of queries that were executed for a given query type.",
                    metricSampleConsumer);
            return true;
        }
        matcher = QUERIES_FAILED_COUNT_PATTERN.matcher(attrName);
        if (matcher.matches()) {
            recordBean(
                    domain,
                    "query_failed_count",
                    matcher.group(1),
                    beanValue,
                    "The total number of queries that failed to complete successfully for a given query type.",
                    metricSampleConsumer);
            return true;
        }
        matcher = QUERIES_SUM_OF_DURATIONS_PATTERN.matcher(attrName);
        if (matcher.matches()) {
            recordBean(
                    domain,
                    "query_sum_of_durations_millis",
                    matcher.group(1),
                    beanValue,
                    "The sum of durations of all executed queries of a given type, expressed in milliseconds.",
                    metricSampleConsumer);
            return true;
        }
        return false;
    }

    private static void recordBean(String domain,
                                   String attrName,
                                   String labelValue,
                                   Number beanValue,
                                   String help,
                                   MetricSampleConsumer metricSampleConsumer) {
        String fullname = domain + "_" + attrName;
        metricSampleConsumer.accept(
                new Collector.MetricFamilySamples.Sample(
                        fullname,
                        Collections.singletonList("query"),
                        Collections.singletonList(labelValue),
                        beanValue.doubleValue()),
                Collector.Type.GAUGE,
                help);
    }
}
