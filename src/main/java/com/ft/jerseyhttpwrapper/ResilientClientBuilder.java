package com.ft.jerseyhttpwrapper;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ft.jerseyhttpwrapper.config.*;
import com.ft.jerseyhttpwrapper.continuation.ContinuationPolicy;
import com.ft.jerseyhttpwrapper.continuation.DefaultContinuationPolicy;
import com.ft.jerseyhttpwrapper.providers.*;
import com.google.common.base.Preconditions;
import com.google.common.net.HostAndPort;
import com.sun.jersey.api.client.filter.GZIPContentEncodingFilter;
import com.sun.jersey.client.apache4.ApacheHttpClient4Handler;
import com.sun.jersey.client.apache4.config.ApacheHttpClient4Config;
import com.sun.jersey.client.apache4.config.DefaultApacheHttpClient4Config;
import io.dropwizard.client.HttpClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.jersey.jackson.JacksonMessageBodyProvider;
import io.dropwizard.setup.Environment;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Convenience for the creation of resilient clients.
 *
 * @see io.dropwizard.client.JerseyClientBuilder
 * @author Simon.Gibbs
 */
public class ResilientClientBuilder {
  private static final String DEFAULT_TX_HEADER = "X-Request-Id";
  private static final String EMPTY_STRING = "";

  private HostAndPortProvider primaryProvider;
  private HostAndPortProvider secondaryProvider;
  private List<SimpleEndpointConfiguration> primaryNodes;
  private List<SimpleEndpointConfiguration> secondaryNodes;
  private boolean retryNonIdempotentMethods;
  private ResilienceStrategy resilienceStrategy;
  private EndpointConfiguration configuration;
  private ClientEnvironment environment;
  private boolean useAdminPorts;
  private MetricRegistry appMetrics;
  private HostAndPortIpResolver hostAndPortIpResolver =
      new HostAndPortIpResolver(new HostToIpMapper());
  private JerseyClientConfiguration jerseyClientConfig = new JerseyClientConfiguration();
  private String shortName;
  private ContinuationPolicy continuationPolicy = new DefaultContinuationPolicy();
  private String txHeader;
  private String protocol;

  public static ResilientClientBuilder in(Environment environment) {
    return new ResilientClientBuilder(new DW07xClientEnvironment(environment));
  }

  public static ResilientClientBuilder in(ClientEnvironment environment) {
    return new ResilientClientBuilder(environment);
  }

  public static ResilientClientBuilder inTesting(HostAndPort endpoint) {

    EndpointConfiguration testHost =
        EndpointConfiguration.forTesting(endpoint.getHostText(), endpoint.getPortOrDefault(8080));

    return inTesting().using(testHost);
  }

  public static ResilientClientBuilder inTesting() {
    return in(DummyClientEnvironment.inTesting());
  }

  public ResilientClientBuilder(ClientEnvironment environment) {
    this.appMetrics = environment.getMetricsRegistry();
    this.environment = environment;
  }

  public ResilientClientBuilder using(EndpointConfiguration configuration) {

    using(configuration.getPrimaryNodes(), configuration.getSecondaryNodes());

    this.retryNonIdempotentMethods = configuration.isRetryNonIdempotentMethods();
    this.resilienceStrategy = configuration.getResilienceStrategy();
    this.configuration = configuration;
    this.jerseyClientConfig = configuration.getJerseyClientConfiguration();

    return this;
  }

  public ResilientClientBuilder usingDNS() {
    return withResilienceStrategy(ResilienceStrategy.DYNAMIC_RANDOM_IP_STRATEGY);
  }

  public ResilientClientBuilder using(
      List<SimpleEndpointConfiguration> primaryNodes,
      List<SimpleEndpointConfiguration> secondaryNodes) {
    this.primaryNodes = primaryNodes;
    this.secondaryNodes = secondaryNodes;
    return this;
  }

  public ResilientClientBuilder using(JerseyClientConfiguration jerseyClientConfiguration) {
    this.jerseyClientConfig = jerseyClientConfiguration;
    return this;
  }

  public ResilientClientBuilder named(String name) {
    this.shortName = name;
    return this;
  }

  public ResilientClientBuilder withPrimary(HostAndPortProvider provider) {
    this.primaryProvider = provider;
    return this;
  }

  public ResilientClientBuilder withSecondary(HostAndPortProvider provider) {
    this.secondaryProvider = provider;
    return this;
  }

  public ResilientClientBuilder retryingNonIdempotentMethods(boolean retryNonIdempotentMethods) {
    this.retryNonIdempotentMethods = retryNonIdempotentMethods;
    return this;
  }

  public ResilientClientBuilder withResilienceStrategy(ResilienceStrategy resilienceStrategy) {
    this.resilienceStrategy = resilienceStrategy;
    return this;
  }

  /* mostly for testing */
  public ResilientClientBuilder withHostAndPortResolver(HostAndPortIpResolver resolver) {
    this.hostAndPortIpResolver = resolver;
    return this;
  }

  public ResilientClientBuilder withContinuationPolicy(ContinuationPolicy continuationPolicy) {
    this.continuationPolicy = continuationPolicy;
    return this;
  }

  public ResilientClientBuilder withTransactionPropagation() {
    this.txHeader = DEFAULT_TX_HEADER;
    return this;
  }

  public ResilientClientBuilder withProtocol(String protocol) {
    this.protocol = protocol;
    return this;
  }

  public ResilientClientBuilder usingAdminPorts() {
    useAdminPorts = true;
    return this;
  }

  public ResilientClient build() {

    if (primaryProvider != null) {
      Preconditions.checkState(configuration != null, "Missing endpoint configuration");
    }
    Preconditions.checkState(appMetrics != null, "Missing metrics registry");

    if (primaryProvider == null) {
      // add the main host / port to the additional nodes.
      List<SimpleEndpointConfiguration> allPrimaryNodes = new ArrayList<>(3);

      if (configuration != null) {
        allPrimaryNodes.add(
            new SimpleEndpointConfiguration(
                configuration.getProtocol(),
                configuration.getHost(),
                configuration.getPort(),
                configuration.getAdminPort()));
      }

      if (primaryNodes != null) {
        allPrimaryNodes.addAll(primaryNodes);
      }

      primaryProvider =
          new HostAndPortProviderBuilder(hostAndPortIpResolver)
              .withStrategy(resilienceStrategy)
              .withSimpleEndpointConfiguration(allPrimaryNodes)
              .usingAdminPorts(useAdminPorts)
              .build();
    }

    HostAndPortProvider finalProvider = primaryProvider;

    if (secondaryProvider == null && secondaryNodes != null && !secondaryNodes.isEmpty()) {
      secondaryProvider =
          new HostAndPortProviderBuilder(hostAndPortIpResolver)
              .withStrategy(resilienceStrategy)
              .withSimpleEndpointConfiguration(secondaryNodes)
              .usingAdminPorts(useAdminPorts)
              .build();
    }

    if (secondaryProvider != null) {
      finalProvider = new CompositeStaticHostAndPortProvider(primaryProvider, secondaryProvider);
    }

    String shortName = getShortName(useAdminPorts);

    final ResilientClient client =
        new ResilientClient(
            shortName,
            buildHandler(shortName),
            buildConfig(),
            finalProvider,
            continuationPolicy,
            retryNonIdempotentMethods,
            appMetrics);

    ExecutorService threadPool =
        environment.createExecutorService(
            shortName, jerseyClientConfig.getMinThreads(), jerseyClientConfig.getMaxThreads());
    client.setExecutorService(threadPool);

    if (jerseyClientConfig.isGzipEnabled()) {
      client.addFilter(
          new GZIPContentEncodingFilter(jerseyClientConfig.isGzipEnabledForRequests()));
    }
    client.setTransactionHeader(txHeader);
    client.setProtocol(configuration != null ? configuration.getProtocol() : EMPTY_STRING);

    return client;
  }

  private int getPort() {
    int port = configuration.getPort();
    if (useAdminPorts) {
      port = configuration.getAdminPort();
    }
    return port;
  }

  private String getShortName(boolean useAdminPorts) {

    if (this.shortName != null) {
      return shortName;
    } else if (configuration == null) {
      throw new IllegalStateException("An explicit short name is required");
    }

    String shortName =
        configuration.getShortName().or(String.format("%s-%d", configuration.getHost(), getPort()));
    if (useAdminPorts) {
      return shortName + "-admin";
    }
    return shortName;
  }

  private ApacheHttpClient4Handler buildHandler(String shortName) {

    HttpClientBuilder builder = new HttpClientBuilder(appMetrics);

    builder.using(jerseyClientConfig);

    return new ApacheHttpClient4Handler(builder.build(shortName), null, true);
  }

  private ApacheHttpClient4Config buildConfig() {
    final ApacheHttpClient4Config config = new DefaultApacheHttpClient4Config();
    final ObjectMapper objectMapper = environment.createObjectMapper();

    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    config
        .getSingletons()
        .add(new JacksonMessageBodyProvider(objectMapper, environment.getValidator()));
    return config;
  }
}
