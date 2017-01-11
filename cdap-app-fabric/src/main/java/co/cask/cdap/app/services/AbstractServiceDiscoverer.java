/*
 * Copyright © 2014-2015 Cask Data, Inc.
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

package co.cask.cdap.app.services;

import co.cask.cdap.api.ServiceDiscoverer;
import co.cask.cdap.api.retry.RetriesExhaustedException;
import co.cask.cdap.api.retry.RetryableException;
import co.cask.cdap.common.conf.Constants;
import co.cask.cdap.common.discovery.EndpointStrategy;
import co.cask.cdap.common.discovery.RandomEndpointStrategy;
import co.cask.cdap.common.service.Retries;
import co.cask.cdap.common.service.RetryStrategy;
import co.cask.cdap.proto.id.ProgramId;
import com.google.common.base.Supplier;
import org.apache.twill.discovery.Discoverable;
import org.apache.twill.discovery.DiscoveryServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import javax.annotation.Nullable;

/**
 * An abstract implementation of {@link ServiceDiscoverer}.
 * It provides definition for {@link ServiceDiscoverer#getServiceURL}  and expects the sub-classes to give definition
 * for {@link AbstractServiceDiscoverer#getDiscoveryServiceClient}.
 */
public abstract class AbstractServiceDiscoverer implements ServiceDiscoverer {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractServiceDiscoverer.class);

  private final String namespaceId;
  private final String applicationId;
  protected final RetryStrategy retryStrategy;

  public AbstractServiceDiscoverer(ProgramId programId, RetryStrategy retryStrategy) {
    this.namespaceId = programId.getNamespace();
    this.applicationId = programId.getApplication();
    this.retryStrategy = retryStrategy;
  }

  @Override
  public URL getServiceURL(final String applicationId, final String serviceId) {
    String discoveryName = String.format("service.%s.%s.%s", namespaceId, applicationId, serviceId);
    final EndpointStrategy endpointStrategy =
      new RandomEndpointStrategy(getDiscoveryServiceClient().discover(discoveryName));

    try {
      return Retries.supplyWithRetries(new Supplier<URL>() {
        @Override
        public URL get() {
          Discoverable discoverable = endpointStrategy.pick();
          if (discoverable == null) {
            throw new RetryableException();
          }
          return createURL(discoverable, applicationId, serviceId);
        }
      }, retryStrategy);
    } catch (RetriesExhaustedException | InterruptedException e) {
      return null;
    }
  }

  @Override
  public URL getServiceURL(String serviceId) {
    return getServiceURL(applicationId, serviceId);
  }

  /**
   * @return the {@link DiscoveryServiceClient} for Service Discovery
   */
  protected abstract DiscoveryServiceClient getDiscoveryServiceClient();

  @Nullable
  private URL createURL(Discoverable discoverable, String applicationId, String serviceId) {
    InetSocketAddress address = discoverable.getSocketAddress();
    String scheme = Arrays.equals(Constants.Security.SSL_URI_SCHEME.getBytes(), discoverable.getPayload()) ?
      Constants.Security.SSL_URI_SCHEME : Constants.Security.URI_SCHEME;

    String path = String.format("%s%s:%d%s/namespaces/%s/apps/%s/services/%s/methods/", scheme,
                                address.getHostName(), address.getPort(),
                                Constants.Gateway.API_VERSION_3, namespaceId, applicationId, serviceId);
    try {
      return new URL(path);
    } catch (MalformedURLException e) {
      LOG.error("Got malformed path '{}' while discovering service {}", path, discoverable.getName(), e);
      return null;
    }
  }
}
