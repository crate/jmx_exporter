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

import java.util.HashMap;
import java.util.Map;

/**
 * CrateDB MBean to {@link Recorder} registry for custom JMX metric recording.
 */
public final class RecorderRegistry {

    private static final Map<String, Recorder> REGISTRY = new HashMap<>();

    static {
        REGISTRY.put(QueryStats.MBEAN_NAME, new QueryStats());
        REGISTRY.put(NodeStatus.MBEAN_NAME, new NodeStatus());
        REGISTRY.put(NodeInfo.MBEAN_NAME, new NodeInfo());
        REGISTRY.put(Connections.MBEAN_NAME, new Connections());
        REGISTRY.put(ThreadPools.MBEAN_NAME, new ThreadPools());
        REGISTRY.put(CircuitBreakers.MBEAN_NAME, new CircuitBreakers());
    }

    public static Recorder get(String name) {
        return REGISTRY.get(name);
    }

    private RecorderRegistry() {
    }

    public static void resetRecorders() {
        REGISTRY.values().forEach(Recorder::reset);
    }
}
