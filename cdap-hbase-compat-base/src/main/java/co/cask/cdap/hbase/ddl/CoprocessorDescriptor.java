/*
 * Copyright © 2017 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package co.cask.cdap.hbase.ddl;

import co.cask.cdap.api.common.Bytes;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.Coprocessor;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.regex.Matcher;

/**
 * Describes an HBase table descriptor.
 */
public final class CoprocessorDescriptor {
  private static final Logger LOG = LoggerFactory.getLogger(CoprocessorDescriptor.class);

  private final String className;
  private final Path path;
  private final int priority;
  private final Map<String, String> properties;

  public CoprocessorDescriptor(String className, Path path, int priority, Map<String, String> properties) {
    this.className = className;
    this.path = path;
    this.priority = priority;
    this.properties = ImmutableMap.copyOf(properties);
  }

  public String getClassName() {
    return className;
  }

  public Path getPath() {
    return path;
  }

  public int getPriority() {
    return priority;
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  /**
   * Returns information for all coprocessor configured for the table.
   *
   * @return a Map from coprocessor class name to {@link CoprocessorDescriptor}
   */
  public static Map<String, CoprocessorDescriptor> getCoprocessors(HTableDescriptor tableDescriptor) {
    Map<String, CoprocessorDescriptor> info = Maps.newHashMap();

    // Extract information about existing data janitor coprocessor
    // The following logic is copied from RegionCoprocessorHost in HBase
    for (Map.Entry<ImmutableBytesWritable, ImmutableBytesWritable> entry: tableDescriptor.getValues().entrySet()) {
      String key = Bytes.toString(entry.getKey().get()).trim();
      String spec = Bytes.toString(entry.getValue().get()).trim();

      if (!HConstants.CP_HTD_ATTR_KEY_PATTERN.matcher(key).matches()) {
        continue;
      }

      try {
        Matcher matcher = HConstants.CP_HTD_ATTR_VALUE_PATTERN.matcher(spec);
        if (!matcher.matches()) {
          continue;
        }

        String className = matcher.group(2).trim();
        Path path = matcher.group(1).trim().isEmpty() ? null : new Path(matcher.group(1).trim());
        int priority = matcher.group(3).trim().isEmpty() ? Coprocessor.PRIORITY_USER
          : Integer.valueOf(matcher.group(3));
        String cfgSpec = null;
        try {
          cfgSpec = matcher.group(4);
        } catch (IndexOutOfBoundsException ex) {
          // ignore
        }

        Map<String, String> properties = Maps.newHashMap();
        if (cfgSpec != null) {
          cfgSpec = cfgSpec.substring(cfgSpec.indexOf('|') + 1);
          // do an explicit deep copy of the passed configuration
          Matcher m = HConstants.CP_HTD_ATTR_VALUE_PARAM_PATTERN.matcher(cfgSpec);
          while (m.find()) {
            properties.put(m.group(1), m.group(2));
          }
        }
        info.put(className, new CoprocessorDescriptor(className, path, priority, properties));
      } catch (Exception ex) {
        LOG.warn("Coprocessor attribute '{}' has invalid coprocessor specification '{}'", key, spec, ex);
      }
    }

    return info;
  }
}
