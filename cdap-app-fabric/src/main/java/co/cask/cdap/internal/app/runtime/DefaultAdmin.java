/*
 * Copyright © 2016 Cask Data, Inc.
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

package co.cask.cdap.internal.app.runtime;

import co.cask.cdap.api.Admin;
import co.cask.cdap.api.dataset.DatasetManagementException;
import co.cask.cdap.api.dataset.DatasetProperties;
import co.cask.cdap.api.dataset.DatasetSpecification;
import co.cask.cdap.api.dataset.InstanceNotFoundException;
import co.cask.cdap.api.messaging.MessagingAdmin;
import co.cask.cdap.api.messaging.TopicAlreadyExistsException;
import co.cask.cdap.api.messaging.TopicNotFoundException;
import co.cask.cdap.api.security.store.SecureStoreManager;
import co.cask.cdap.common.service.Retries;
import co.cask.cdap.common.service.RetryStrategies;
import co.cask.cdap.common.service.RetryStrategy;
import co.cask.cdap.data2.dataset2.DatasetFramework;
import co.cask.cdap.proto.id.DatasetId;
import co.cask.cdap.proto.id.NamespaceId;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nullable;

/**
 * Implementation of Admin that delegates dataset operations to a dataset framework.
 */
public class DefaultAdmin implements Admin {

  private final DatasetFramework dsFramework;
  private final NamespaceId namespace;
  private final SecureStoreManager secureStoreManager;
  private final MessagingAdmin messagingAdmin;
  private final RetryStrategy retryStrategy;

  /**
   * Creates an instance without messaging admin support.
   */
  public DefaultAdmin(DatasetFramework dsFramework, NamespaceId namespace, SecureStoreManager secureStoreManager) {
    this(dsFramework, namespace, secureStoreManager, null, RetryStrategies.noRetry());
  }

  /**
   * Creates an instance with all Admin functions supported.
   */
  public DefaultAdmin(DatasetFramework dsFramework, NamespaceId namespace,
                      SecureStoreManager secureStoreManager, @Nullable MessagingAdmin messagingAdmin,
                      RetryStrategy retryStrategy) {
    this.dsFramework = dsFramework;
    this.namespace = namespace;
    this.secureStoreManager = secureStoreManager;
    this.messagingAdmin = messagingAdmin;
    this.retryStrategy = retryStrategy;
  }

  private DatasetId createInstanceId(String name) {
    return namespace.dataset(name);
  }

  @Override
  public boolean datasetExists(final String name) throws DatasetManagementException {
    return callDatasetFramework(new Callable<Boolean>() {
      @Override
      public Boolean call() throws DatasetManagementException {
        return dsFramework.getDatasetSpec(createInstanceId(name)) != null;
      }
    }, "checking existence of dataset " + name);
  }

  @Override
  public String getDatasetType(final String name) throws DatasetManagementException {
    return callDatasetFramework(new Callable<String>() {
      @Override
      public String call() throws Exception {
        DatasetSpecification spec = dsFramework.getDatasetSpec(createInstanceId(name));
        if (spec == null) {
          throw new InstanceNotFoundException(name);
        }
        return spec.getType();
      }
    }, "getting type of dataset " + name);
  }

  @Override
  public DatasetProperties getDatasetProperties(final String name) throws DatasetManagementException {
    return callDatasetFramework(new Callable<DatasetProperties>() {
      @Override
      public DatasetProperties call() throws DatasetManagementException {
        DatasetSpecification spec = dsFramework.getDatasetSpec(createInstanceId(name));
        if (spec == null) {
          throw new InstanceNotFoundException(name);
        }
        return DatasetProperties.of(spec.getOriginalProperties());
      }
    }, "getting properties of dataset " + name);
  }

  @Override
  public void createDataset(final String name, final String type,
                            final DatasetProperties properties) throws DatasetManagementException {
    callDatasetFramework(new Callable<Void>() {
      @Override
      public Void call() throws DatasetManagementException {
        try {
          dsFramework.addInstance(type, createInstanceId(name), properties);
        } catch (IOException ioe) {
          // not the prettiest message, but this replicates exactly what RemoteDatasetFramework throws
          throw new DatasetManagementException(String.format("Failed to add instance %s, details: %s",
                                                             name, ioe.getMessage()), ioe);
        }
        return null;
      }
    }, "creating dataset " + name);
  }

  @Override
  public void updateDataset(final String name, final DatasetProperties properties) throws DatasetManagementException {
    callDatasetFramework(new Callable<Void>() {
      @Override
      public Void call() throws DatasetManagementException {
        try {
          dsFramework.updateInstance(createInstanceId(name), properties);
        } catch (IOException ioe) {
          // not the prettiest message, but this replicates exactly what RemoteDatasetFramework throws
          throw new DatasetManagementException(String.format("Failed to update instance %s, details: %s",
                                                             name, ioe.getMessage()), ioe);
        }
        return null;
      }
    }, "updating dataset " + name);
  }

  @Override
  public void dropDataset(final String name) throws DatasetManagementException {
    callDatasetFramework(new Callable<Void>() {
      @Override
      public Void call() throws DatasetManagementException {
        try {
          dsFramework.deleteInstance(createInstanceId(name));
        } catch (IOException ioe) {
          // not the prettiest message, but this replicates exactly what RemoteDatasetFramework throws
          throw new DatasetManagementException(String.format("Failed to delete instance %s, details: %s",
                                                             name, ioe.getMessage()), ioe);
        }
        return null;
      }
    }, "dropping dataset " + name);
  }

  @Override
  public void truncateDataset(final String name) throws DatasetManagementException {
    callDatasetFramework(new Callable<Void>() {
      @Override
      public Void call() throws Exception {
        try {
          dsFramework.truncateInstance(createInstanceId(name));
        } catch (IOException ioe) {
          // not the prettiest message, but this replicates exactly what RemoteDatasetFramework throws
          throw new DatasetManagementException(String.format("Failed to truncate instance %s, details: %s",
                                                             name, ioe.getMessage()), ioe);
        }
        return null;
      }
    }, "truncating dataset " + name);
  }

  @Override
  public void putSecureData(String namespace, String name, String data,
                            String description, Map<String, String> properties) throws Exception {
    secureStoreManager.putSecureData(namespace, name, data, description, properties);
  }

  @Override
  public void deleteSecureData(String namespace, String name) throws Exception {
    secureStoreManager.deleteSecureData(namespace, name);
  }

  @Override
  public void createTopic(final String topic) throws TopicAlreadyExistsException, IOException {
    if (messagingAdmin == null) {
      throw new UnsupportedOperationException("Messaging not supported");
    }

    callMessagingAdminCreate(new Callable<Void>() {
      @Override
      public Void call() throws Exception {
        messagingAdmin.createTopic(topic);
        return null;
      }
    }, "creating topic " + topic);
  }

  @Override
  public void createTopic(final String topic,
                          final Map<String, String> properties) throws TopicAlreadyExistsException, IOException {
    if (messagingAdmin == null) {
      throw new UnsupportedOperationException("Messaging not supported");
    }

    callMessagingAdminCreate(new Callable<Void>() {
      @Override
      public Void call() throws Exception {
        messagingAdmin.createTopic(topic, properties);
        return null;
      }
    }, "creating topic " + topic);
  }

  @Override
  public Map<String, String> getTopicProperties(final String topic) throws TopicNotFoundException, IOException {
    if (messagingAdmin == null) {
      throw new UnsupportedOperationException("Messaging not supported");
    }

    return callMessagingAdmin(new Callable<Map<String, String>>() {
      @Override
      public Map<String, String> call() throws Exception {
        return messagingAdmin.getTopicProperties(topic);
      }
    }, "getting properties for topic " + topic);
  }

  @Override
  public void updateTopic(final String topic,
                          final Map<String, String> properties) throws TopicNotFoundException, IOException {
    if (messagingAdmin == null) {
      throw new UnsupportedOperationException("Messaging not supported");
    }

    callMessagingAdmin(new Callable<Void>() {
      @Override
      public Void call() throws Exception {
        messagingAdmin.updateTopic(topic, properties);
        return null;
      }
    }, "updating topic " + topic);
  }

  @Override
  public void deleteTopic(final String topic) throws TopicNotFoundException, IOException {
    if (messagingAdmin == null) {
      throw new UnsupportedOperationException("Messaging not supported");
    }

    callMessagingAdmin(new Callable<Void>() {
      @Override
      public Void call() throws Exception {
        messagingAdmin.deleteTopic(topic);
        return null;
      }
    }, "deleting topic " + topic);
  }

  private <T> T callMessagingAdminCreate(Callable<T> callable,
                                         String opMsg) throws TopicAlreadyExistsException, IOException {
    try {
      return Retries.callWithRetries(callable, retryStrategy);
    } catch (InterruptedException e) {
      throw new IOException(String.format("Interrupted while %s.", opMsg));
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      if (cause instanceof TopicAlreadyExistsException) {
        throw (TopicAlreadyExistsException) cause;
      } else if (cause instanceof IOException) {
        throw (IOException) cause;
      } else {
        throw new IOException(String.format("Error while %s.", opMsg), cause);
      }
    }
  }

  private <T> T callMessagingAdmin(Callable<T> callable, String opMsg) throws TopicNotFoundException, IOException {
    try {
      return Retries.callWithRetries(callable, retryStrategy);
    } catch (InterruptedException e) {
      throw new IOException(String.format("Interrupted while %s.", opMsg));
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      if (cause instanceof TopicNotFoundException) {
        throw (TopicNotFoundException) cause;
      } else if (cause instanceof IOException) {
        throw (IOException) cause;
      } else {
        throw new IOException(String.format("Error while %s.", opMsg), cause);
      }
    }
  }

  // performs some dataset framework call with retries and exception handling
  private <T> T callDatasetFramework(Callable<T> callable, String opMsg) throws DatasetManagementException {
    try {
      return Retries.callWithRetries(callable, retryStrategy);
    } catch (ExecutionException e) {
      if (e.getCause() instanceof DatasetManagementException) {
        throw (DatasetManagementException) e.getCause();
      }
      throw new DatasetManagementException(String.format("Error while %s.", opMsg), e.getCause());
    } catch (InterruptedException e) {
      throw new DatasetManagementException(String.format("Interrupted while %s.", opMsg), e);
    }
  }

}
