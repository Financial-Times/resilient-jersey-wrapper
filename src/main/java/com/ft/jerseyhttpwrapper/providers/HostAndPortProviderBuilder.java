package com.ft.jerseyhttpwrapper.providers;

import static com.ft.jerseyhttpwrapper.ResilienceStrategy.*;

import com.ft.jerseyhttpwrapper.ResilienceStrategy;
import com.ft.jerseyhttpwrapper.config.SimpleEndpointConfiguration;
import com.google.common.net.HostAndPort;
import java.util.List;

public class HostAndPortProviderBuilder {

  public static final ResilienceStrategy DEFAULT_RESILIENCE_STRATEGY = LOAD_BALANCED_STRATEGY;

  private static final NullHostAndPortProvider NULL_PROVIDER = new NullHostAndPortProvider();

  private final HostAndPortIpResolver hostAndPortIpResolver;

  private ResilienceStrategy strategy = DEFAULT_RESILIENCE_STRATEGY;
  private boolean useAdminPorts;
  private List<SimpleEndpointConfiguration> endpoints;

  public HostAndPortProviderBuilder(final HostAndPortIpResolver hostAndPortIpResolver) {
    this.hostAndPortIpResolver = hostAndPortIpResolver;
  }

  public HostAndPortProviderBuilder withStrategy(ResilienceStrategy strategy) {
    this.strategy = strategy;
    return this;
  }

  public HostAndPortProviderBuilder usingAdminPorts(boolean useAdminPorts) {
    this.useAdminPorts = useAdminPorts;
    return this;
  }

  public HostAndPortProviderBuilder withSimpleEndpointConfiguration(
      List<SimpleEndpointConfiguration> endpoints) {
    this.endpoints = endpoints;
    return this;
  }

  public HostAndPortProvider build() {
    List<HostAndPort> nodes =
        new SimpleEndpointConfigurationToHostPortConverter(useAdminPorts).convertAll(endpoints);

    switch (strategy) {
      case DYNAMIC_RANDOM_IP_STRATEGY:
        break;
      default:
        if (nodes == null) {
          return NULL_PROVIDER;
        }
        break;
    }

    switch (strategy) {
      case EMPTY_STRATEGY:
        return NULL_PROVIDER;
      case SIMPLE_FAILOVER_STRATEGY:
        return new SimpleHostAndPortProvider(nodes);
      case LOAD_BALANCED_STRATEGY:
        return new RandomHostAndPortProvider(nodes);
      case LOAD_BALANCED_IP_STRATEGY:
        return new RandomIpHostAndPortProvider(nodes, hostAndPortIpResolver);
      case DYNAMIC_RANDOM_IP_STRATEGY:
        if (nodes != null) {
          throw new IllegalArgumentException(
              "You should not provide default routes for use with a dynamic strategy");
        }
        return new DynamicOrderedDNSIpHostAndPortProvider(hostAndPortIpResolver);
      default:
        throw new IllegalArgumentException("Unknown strategy " + strategy);
    }
  }
}
