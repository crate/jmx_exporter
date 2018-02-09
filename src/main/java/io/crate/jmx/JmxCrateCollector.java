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

import io.prometheus.client.Collector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JmxCrateCollector extends Collector {

    private static final Logger LOGGER = LogManager.getLogger(JmxCrateCollector.class);

    private static final String CRATE_NS = "io.crate.monitoring";
    private static final String CRATE_MBEAN_PATTERN = CRATE_NS + ":*";
    private static final String NAME_PREFIX = "crate_";

    private static final char SEP = '_';
    private static final Pattern UNSAFE_CHARS = Pattern.compile("[^a-zA-Z0-9:_]");
    private static final Pattern MULTIPLE_UNDERSCORES = Pattern.compile("__+");
    private static final Pattern SNAKE_CASE_PATTERN = Pattern.compile("([a-z0-9])([A-Z])");
    private static final Pattern LABEL_VALUE_PATTERN = Pattern.compile("([A-Z][a-z0-9]+)(.+)*");

    private final MBeanServer beanConn;
    private Map<String, MetricFamilySamples> metricFamilySamplesMap = new HashMap<>();
    private final JmxMBeanPropertyCache jmxMBeanPropertyCache = new JmxMBeanPropertyCache();

    JmxCrateCollector() {
        beanConn = ManagementFactory.getPlatformMBeanServer();
    }

    @Override
    public List<MetricFamilySamples> collect() {
        for (ObjectName mBeanName : resolveMBeans()) {
            try {
                MBeanInfo mBeanInfo = beanConn.getMBeanInfo(mBeanName);
                scrapeMBean(mBeanInfo, mBeanName);
            } catch (InstanceNotFoundException | IntrospectionException | ReflectionException e) {
                LOGGER.error("Cannot get MBean info for {}", mBeanName.getCanonicalName(), e);
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
                    jmxMBeanPropertyCache.getKeyPropertyList(mBeanName),
                    new LinkedList<>(),
                    attr.getName(),
                    attr.getType(),
                    attr.getDescription(),
                    value
            );
        }
    }

    private void processBeanValue(
            String domain,
            LinkedHashMap<String, String> beanProperties,
            LinkedList<String> attrKeys,
            String attrName,
            String attrType,
            String attrDescription,
            Object value) {
        if (value == null) {
            logScrape(domain + beanProperties + attrName, "null");
        } else if (value instanceof Number || value instanceof String || value instanceof Boolean) {
            logScrape(domain + beanProperties + attrName, value.toString());
            recordBean(
                    domain,
                    beanProperties,
                    attrKeys,
                    attrName,
                    attrType,
                    attrDescription,
                    value);
        } else {
            logScrape(domain + beanProperties, attrType + " is not exported");
        }
    }

    private void recordBean(
            String domain,
            LinkedHashMap<String, String> beanProperties,
            LinkedList<String> attrKeys,
            String attrName,
            String attrType,
            String attrDescription,
            Object beanValue) {

        String beanName = domain + angleBrackets(beanProperties.toString()) + angleBrackets(attrKeys.toString());
        LOGGER.debug("beanName: {}", beanName);
        // attrDescription tends not to be useful, so give the fully qualified name too.
        String help = attrDescription + " (" + beanName + attrName + ")";

        domain =  domain.replace(CRATE_NS, NAME_PREFIX);

        defaultExport(domain, beanProperties, attrKeys, attrName, attrType, help, beanValue, Type.UNTYPED);
    }

    private void defaultExport(
            String domain,
            LinkedHashMap<String, String> beanProperties,
            LinkedList<String> attrKeys,
            String attrName,
            String attrType,
            String help,
            Object beanValue,
            Type type) {
        StringBuilder name = new StringBuilder();
        name.append(domain);
        if (beanProperties.size() > 0) {
            name.append(SEP);
            name.append(beanProperties.values().iterator().next());
        }
        for (String k : attrKeys) {
            name.append(SEP);
            name.append(k);
        }

        Matcher matcher = LABEL_VALUE_PATTERN.matcher(attrName);
        String labelName = "label";
        String labelValue = "";

        if (matcher.matches()) {
            labelValue = matcher.group(1);
            attrName = matcher.group(2);
            if (attrName != null) {
                name.append(SEP);
                name.append(attrName);
            }
        }

        Number value;
        if (beanValue instanceof Number) {
            value = ((Number) beanValue).doubleValue();
        } else if (beanValue instanceof Boolean) {
            labelValue = labelValue + ": " + beanValue.toString();
            value = (Boolean) beanValue ? 1 : 0;
        } else {
            LOGGER.error("Ignoring unsupported bean: " + name + ": " + beanValue);
            return;
        }

        List<String> labelNames = Collections.singletonList(labelName);
        List<String> labelValues = Collections.singletonList(labelValue);
        String fullname = SNAKE_CASE_PATTERN.matcher(safeName(name.toString())).replaceAll("$1_$2").toLowerCase();

        LOGGER.debug("add metric sample: " + fullname + " " + labelNames + " " + labelValues + " " + value.doubleValue());
        addSample(new MetricFamilySamples.Sample(fullname, labelNames, labelValues, value.doubleValue()),
                type, help);
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

    private static Set<ObjectName> resolveMBeans() {
        LOGGER.debug("resolving mbeans...");
        MBeanServer beanConn = ManagementFactory.getPlatformMBeanServer();
        Set<ObjectName> mBeanNames = new HashSet<>();
        try {
            for (ObjectInstance instance : beanConn.queryMBeans(new ObjectName(CRATE_MBEAN_PATTERN), null)) {
                LOGGER.debug("Found Crate MBean {}", instance.getClassName());
                mBeanNames.add(instance.getObjectName());
            }
        } catch (MalformedObjectNameException e) {
            LOGGER.error("Cannot resolve Crate MBean, malformed pattern {}", CRATE_MBEAN_PATTERN, e);
        }
        return mBeanNames;
    }

    // [] and () are special in regexes, so switch to <>.
    private static String angleBrackets(String s) {
        return "<" + s.substring(1, s.length() - 1) + ">";
    }

    private static String safeName(String s) {
        // Change invalid chars to underscore, and merge underscores.
        return MULTIPLE_UNDERSCORES.matcher(UNSAFE_CHARS.matcher(s).replaceAll("_")).replaceAll("_");
    }


    /**
     * For debugging.
     */
    private static void logScrape(ObjectName mbeanName, MBeanAttributeInfo attr, String msg) {
        logScrape(mbeanName + "'_'" + attr.getName(), msg);
    }

    private static void logScrape(String name, String msg) {
        LOGGER.debug("scrape: '" + name + "': " + msg);
    }

}
