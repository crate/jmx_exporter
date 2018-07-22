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

import javax.management.openmbean.CompositeData;

public interface Recorder {

    /**
     * Adds a MBean attribute as a metric sample to the given consumer.
     */
    boolean recordBean(String domain, String attrName, Number beanValue, MetricSampleConsumer metricSampleConsumer);

    /**
     * Adds a MBean attribute as a String label to the given consumer.
     */
    boolean recordBean(String domain, String attrName, String beanValue, MetricSampleConsumer metricSampleConsumer);

    default boolean recordBean(String domain,
                               String attrName,
                               CompositeData beanValue,
                               MetricSampleConsumer metricSampleConsumer) {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() +
                                                " cannot be called with CompositeData bean value");
    }

    /**
     * Clears any internal structures before new collect()
     */
    default void reset() {
    }
}
