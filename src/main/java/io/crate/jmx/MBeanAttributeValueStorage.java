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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple wrapper around a map to store and retrieve attribute values of MBean's.
 * Useful if storage and retrieval happens at different classes.
 *
 * This implementation is thread-safe by using a {@link ConcurrentHashMap} internally.
 */
public class MBeanAttributeValueStorage {

    private final Map<String, Object> properties = new ConcurrentHashMap<>();

    public void put(String key, Object value) {
        properties.put(key, value);
    }

    public Object get(String key) {
        return properties.get(key);
    }

    public void reset() {
        properties.clear();
    }
}
