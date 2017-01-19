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

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;

/**
 * Implementation of {@link HBaseDDLExecutor} for HBase version 1.0 CDH
 */
public class DefaultHBase10CDHDDLExecutor extends DefaultHBaseDDLExecutor {

  public DefaultHBase10CDHDDLExecutor(Configuration hConf) {
    super(hConf);
  }

  public HTableDescriptor getHTableDescriptorInstance(TableName tableName) {
    return new HTableDescriptor(tableName);
  }

  public HColumnDescriptor getHColumnDescriptorInstance(String name) {
    return new HColumnDescriptor(name);
  }
}
