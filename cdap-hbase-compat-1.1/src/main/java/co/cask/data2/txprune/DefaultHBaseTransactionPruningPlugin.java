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

package co.cask.data2.txprune;

import co.cask.cdap.data2.transaction.coprocessor.hbase11.DefaultTransactionProcessor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.tephra.hbase.coprocessor.janitor.HBaseTransactionPruningPlugin;
import org.apache.tephra.janitor.TransactionPruningPlugin;

/**
 * {@link TransactionPruningPlugin} for CDAP Datasets.
 */
public class DefaultHBaseTransactionPruningPlugin extends HBaseTransactionPruningPlugin {

  @Override
  protected boolean isTransactionalTable(HTableDescriptor tableDescriptor) {
    return tableDescriptor.hasCoprocessor(DefaultTransactionProcessor.class.getName());
  }
}
