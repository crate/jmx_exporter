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

    private static final Pattern QUERIES = Pattern.compile("(.+)Query(Frequency|AverageDuration|TotalCount|AffectedRowCount|FailedCount|SumOfDurations)");
    private static final String FREQUENCY = "Frequency";
    private static final String AVG_DURATION = "AverageDuration";
    private static final String TOTAL_COUNT = "TotalCount";
    private static final String AFFECTED_ROW_COUNT = "AffectedRowCount";
    private static final String FAILED_COUNT = "FailedCount";
    private static final String SUM_OF_DURATIONS = "SumOfDurations";

    @Override
    public boolean recordBean(String domain,
                              String attrName,
                              Number beanValue,
                              MetricSampleConsumer metricSampleConsumer) {
        Matcher matcher = QUERIES.matcher(attrName);
        if (matcher.matches()) {
            String metric = matcher.group(2);
            String label = matcher.group(1);
            switch (metric) {
                case FREQUENCY:
                    recordBean(
                        domain,
                        "queries",
                        label,
                        beanValue,
                        "Queries per second for a given query type.",
                        metricSampleConsumer);
                    return true;
                case AVG_DURATION:
                    recordBean(
                        domain,
                        "query_duration_seconds",
                        label,
                        beanValue,
                        "The average query duration for a given query type.",
                        metricSampleConsumer);
                    return true;
                case TOTAL_COUNT:
                    recordBean(
                        domain,
                        "query_total_count",
                        label,
                        beanValue,
                        "The total number of queries that were executed for a given query type.",
                        metricSampleConsumer);
                    return true;
                case AFFECTED_ROW_COUNT:
                    recordBean(
                        domain,
                        "query_affected_row_count",
                        label,
                        beanValue,
                        "The total number of affected rows of all statement executions for a given query type.",
                        metricSampleConsumer);
                    return true;
                case FAILED_COUNT:
                    recordBean(
                        domain,
                        "query_failed_count",
                        label,
                        beanValue,
                        "The total number of queries that failed to complete successfully for a given query type.",
                        metricSampleConsumer);
                    return true;
                case SUM_OF_DURATIONS:
                    recordBean(
                        domain,
                        "query_sum_of_durations_millis",
                        label,
                        beanValue,
                        "The sum of durations of all executed queries of a given type, expressed in milliseconds.",
                        metricSampleConsumer);
                    return true;
                default:
                    return false;
            }
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
