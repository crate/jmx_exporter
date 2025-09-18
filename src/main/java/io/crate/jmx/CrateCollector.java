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

import io.crate.jmx.recorder.Recorder;
import io.crate.jmx.recorder.RecorderRegistry;
import io.prometheus.client.Collector;

import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * A prometheus jmx {@link Collector} implementation for CrateDB specific JMX metrics.
 *
 * Derived from
 * https://github.com/prometheus/jmx_exporter/blob/master/collector/src/main/java/io/prometheus/jmx/JmxCollector.java.
 */
public class CrateCollector extends Collector {

    private static final Logger LOGGER = Logger.getLogger(CrateCollector.class.getName());

    private static final String CRATE_DOMAIN = "io.crate.monitoring";
    private static final String CRATE_DOMAIN_REPLACEMENT = "crate";

    private static final char SEP = '_';
    private static final Pattern SNAKE_CASE_PATTERN = Pattern.compile("([a-z0-9])([A-Z])");
    private static final Pattern UNSAFE_CHARS = Pattern.compile("[^a-zA-Z0-9:_]");
    private static final Pattern MULTIPLE_UNDERSCORES = Pattern.compile("__+");

    private final MBeanServer beanConn;
    private final BiConsumer<String, Object> beanValueConsumer;
    private final Map<String, MetricFamilySamples> metricFamilySamplesMap = new HashMap<>();
    private final MBeanPropertyCache MBeanPropertyCache = new MBeanPropertyCache();

    CrateCollector(BiConsumer<String, Object> beanValueConsumer) {
        beanConn = ManagementFactory.getPlatformMBeanServer();
        this.beanValueConsumer = beanValueConsumer;
    }

    @Override
    public List<MetricFamilySamples> collect() {
        return collect("*");
    }

    public List<MetricFamilySamples> collect(String mBeanNamePattern) {
        metricFamilySamplesMap.clear();
        RecorderRegistry.resetRecorders();
        for (ObjectName mBeanName : resolveMBean(CRATE_DOMAIN  + ":" + mBeanNamePattern)) {
            try {
                MBeanInfo mBeanInfo = beanConn.getMBeanInfo(mBeanName);
                scrapeMBean(mBeanInfo, mBeanName);
            } catch (InstanceNotFoundException | IntrospectionException | ReflectionException e) {
                LOGGER.log(Level.SEVERE, "Cannot get MBean info for " + mBeanName.getCanonicalName(), e);
            }
        }
        return new ArrayList<>(metricFamilySamplesMap.values());
    }

    private void scrapeMBean(MBeanInfo mBeanInfo, ObjectName mBeanName) {
        MBeanAttributeInfo[] attrInfos = mBeanInfo.getAttributes();

        for (int idx = 0; idx < attrInfos.length; ++idx) {
            MBeanAttributeInfo attr = attrInfos[idx];
            if (!attr.isReadable()) {
                logScrape(mBeanName, attr, "not readable");
                continue;
            }

            Object value;
            try {
                value = beanConn.getAttribute(mBeanName, attr.getName());
            } catch (Exception e) {
                logScrape(mBeanName, attr, "Fail: " + e);
                continue;
            }

            logScrape(mBeanName, attr, "process");

            processBeanValue(
                    mBeanName.getDomain(),
                    MBeanPropertyCache.getKeyPropertyList(mBeanName),
                    attr.getName(),
                    attr.getType(),
                    attr.getDescription(),
                    value
            );
        }
    }

    private void processBeanValue(String domain,
                                  LinkedHashMap<String, String> beanProperties,
                                  String attrName,
                                  String attrType,
                                  String attrDescription,
                                  Object value) {
        if (value == null) {
            logScrape(domain + beanProperties + attrName, "null");
        } else if (value instanceof Number || value instanceof String || value instanceof Boolean
                   || value instanceof CompositeDataSupport || value instanceof CompositeData[]) {
            logScrape(domain + beanProperties + attrName, value.toString());
            recordBean(beanProperties, attrName, attrDescription, value);
        } else {
            logScrape(domain + beanProperties, attrType + " is not exported");
        }
    }

    private void recordBean(LinkedHashMap<String, String> beanProperties,
                            String attrName,
                            String attrDescription,
                            Object beanValue) {
        String mBeanName = "";
        if (beanProperties.size() > 0) {
            mBeanName = beanProperties.values().iterator().next();
        }
        beanValueConsumer.accept(mBeanName + "_" + attrName, beanValue);

        if (beanValue instanceof Number) {
            recordNumericMBeanValue(
                    beanProperties,
                    attrName,
                    attrDescription,
                    beanValue,
                    mBeanName,
                    ((Number) beanValue).doubleValue());
        } else if (beanValue instanceof Boolean) {
            recordNumericMBeanValue(
                    beanProperties,
                    attrName,
                    attrDescription,
                    beanValue,
                    mBeanName,
                    (Boolean) beanValue ? 1 : 0);
        } else if (beanValue instanceof CompositeData) {
            recordCompositeDataMBeanValue(attrName, mBeanName, (CompositeData) beanValue);
        } else if (beanValue instanceof CompositeData[]) {
            recordCompositeDataMBeanValue(attrName, mBeanName, (CompositeData[]) beanValue);
        } else if ((beanValue instanceof String) == false) {
            // only log on non-string values, string values are ignored by intend
            LOGGER.log(Level.SEVERE, "Ignoring unsupported bean: " + mBeanName + "_" + attrName + ": " + beanValue);
        }
    }

    private void recordNumericMBeanValue(LinkedHashMap<String, String> beanProperties,
                                         String attrName,
                                         String attrDescription,
                                         Object beanValue,
                                         String mBeanName,
                                         Number value) {
        Recorder recorder = RecorderRegistry.get(mBeanName);
        if (recorder != null) {
            boolean supportedAttribute = recorder.recordBean(CRATE_DOMAIN_REPLACEMENT, attrName, value, this::addSample);
            if (supportedAttribute == false) {
                LOGGER.log(Level.SEVERE,
                        "Ignoring unsupported bean attribute: " + mBeanName + "_" + attrName + ": " + beanValue);
            }
        } else {
            String beanName = CRATE_DOMAIN_REPLACEMENT + angleBrackets(beanProperties.toString());
            // attrDescription tends not to be useful, so give the fully qualified name too.
            String help = attrDescription + " (" + beanName + attrName + ")";
            defaultExport(CRATE_DOMAIN_REPLACEMENT, mBeanName, attrName, help, value, Type.UNKNOWN);
        }
    }

    private void recordCompositeDataMBeanValue(String attrName,
                                               String mBeanName,
                                               CompositeData beanValue) {
        Recorder recorder = RecorderRegistry.get(mBeanName);
        if (recorder != null) {
            boolean supportedAttribute = recorder.recordBean(CRATE_DOMAIN_REPLACEMENT, attrName, beanValue, this::addSample);
            if (supportedAttribute == false) {
                LOGGER.log(Level.SEVERE,
                        "Ignoring unsupported bean attribute: " + mBeanName + "_" + attrName + ": " + beanValue);
            }
        } else {
            LOGGER.log(Level.SEVERE,
                    "Ignoring unsupported bean attribute: " + mBeanName + "_" + attrName + ": " + beanValue);
        }
    }

    private void recordCompositeDataMBeanValue(String attrName,
                                               String mBeanName,
                                               CompositeData[] beanValue) {
        Recorder recorder = RecorderRegistry.get(mBeanName);
        if (recorder != null) {
            boolean supportedAttribute = recorder.recordBean(CRATE_DOMAIN_REPLACEMENT, attrName, beanValue, this::addSample);
            if (supportedAttribute == false) {
                LOGGER.log(Level.SEVERE,
                        "Ignoring unsupported bean attribute: " + mBeanName + "_" + attrName + ": " + beanValue);
            }
        } else {
            LOGGER.log(Level.SEVERE,
                    "Ignoring unsupported bean attribute: " + mBeanName + "_" + attrName + ": " + beanValue);
        }
    }

    private void defaultExport(String domain,
                               String mBeanName,
                               String attrName,
                               String help,
                               Number value,
                               Type type) {
        StringBuilder name = new StringBuilder();
        name.append(domain);
        if (!mBeanName.isEmpty()) {
            name.append(SEP);
            name.append(mBeanName);
        }
        name.append(SEP);
        name.append(attrName);
        String fullname = camelCaseToLower(name.toString());

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "add metric sample: " + fullname + " " + value.doubleValue());
        }
        addSample(new MetricFamilySamples.Sample(fullname, Collections.emptyList(), Collections.emptyList(), value.doubleValue()), type, help);
    }

    private void addSample(MetricFamilySamples.Sample sample, Type type, String help) {
        MetricFamilySamples mfs = metricFamilySamplesMap.get(sample.name);
        if (mfs == null) {
            // JmxScraper.MBeanReceiver is only called from one thread,
            // so there's no race here.
            mfs = new MetricFamilySamples(sample.name, type, help, new ArrayList<>());
            metricFamilySamplesMap.put(sample.name, mfs);
        }
        mfs.samples.add(sample);
    }

    private static Set<ObjectName> resolveMBean(String mBeanPattern) {
        MBeanServer beanConn = ManagementFactory.getPlatformMBeanServer();
        Set<ObjectName> mBeanNames = new HashSet<>();
        try {
            for (ObjectInstance instance : beanConn.queryMBeans(new ObjectName(mBeanPattern), null)) {
                mBeanNames.add(instance.getObjectName());
            }
        } catch (MalformedObjectNameException e) {
            LOGGER.log(Level.SEVERE, "Cannot resolve Crate MBean, malformed pattern " + mBeanPattern, e);
        }
        return mBeanNames;
    }

    // [] and () are special in regexes, so switch to <>.
    private static String angleBrackets(String str) {
        return "<" + str.substring(1, str.length() - 1) + ">";
    }

    private static String camelCaseToLower(String str) {
        return SNAKE_CASE_PATTERN.matcher(safeName(str)).replaceAll("$1_$2").toLowerCase(Locale.ENGLISH);
    }

    private static String safeName(String str) {
        // Change invalid chars to underscore, and merge underscores.
        return MULTIPLE_UNDERSCORES.matcher(UNSAFE_CHARS.matcher(str).replaceAll("_")).replaceAll("_");
    }

    /**
     * For debugging.
     */
    private static void logScrape(ObjectName mbeanName, MBeanAttributeInfo attr, String msg) {
        logScrape(mbeanName + "'_'" + attr.getName(), msg);
    }

    private static void logScrape(String name, String msg) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "scrape: '" + name + "': " + msg);
        }
    }
}
