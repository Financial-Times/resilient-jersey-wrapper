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
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.jersey.jackson.JacksonMessageBodyProvider;
import io.dropwizard.setup.Environment;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.filter.EncodingFilter;
import org.glassfish.jersey.message.GZipEncoder;

/**
 * Convenience for the creation of resilient clients.
 *
 * @see io.dropwizard.client.JerseyClientBuilder
 * @author Simon.Gibbs
 */
public class ResilientClientBuilderNew {
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

  public static ResilientClientBuilderNew in(Environment environment) {
    return new ResilientClientBuilderNew(new DW20xClientEnvironment(environment));
  }

  public static ResilientClientBuilderNew in(ClientEnvironment environment) {
    return new ResilientClientBuilderNew(environment);
  }

  public static ResilientClientBuilderNew inTesting(HostAndPort endpoint) {

    EndpointConfiguration testHost =
        EndpointConfiguration.forTesting(endpoint.getHost(), endpoint.getPortOrDefault(8080));

    return inTesting().using(testHost);
  }

  public static ResilientClientBuilderNew inTesting() {
    return in(DummyClientEnvironment.inTesting());
  }

  public ResilientClientBuilderNew(ClientEnvironment environment) {
    this.appMetrics = environment.getMetricsRegistry();
    this.environment = environment;
  }

  public ResilientClientBuilderNew using(EndpointConfiguration configuration) {

    using(configuration.getPrimaryNodes(), configuration.getSecondaryNodes());

    this.retryNonIdempotentMethods = configuration.isRetryNonIdempotentMethods();
    this.resilienceStrategy = configuration.getResilienceStrategy();
    this.configuration = configuration;
    this.jerseyClientConfig = configuration.getJerseyClientConfiguration();

    return this;
  }

  public ResilientClientBuilderNew usingDNS() {
    return withResilienceStrategy(ResilienceStrategy.DYNAMIC_RANDOM_IP_STRATEGY);
  }

  public ResilientClientBuilderNew using(
      List<SimpleEndpointConfiguration> primaryNodes,
      List<SimpleEndpointConfiguration> secondaryNodes) {
    this.primaryNodes = primaryNodes;
    this.secondaryNodes = secondaryNodes;
    return this;
  }

  public ResilientClientBuilderNew using(JerseyClientConfiguration jerseyClientConfiguration) {
    this.jerseyClientConfig = jerseyClientConfiguration;
    return this;
  }

  public ResilientClientBuilderNew named(String name) {
    this.shortName = name;
    return this;
  }

  public ResilientClientBuilderNew withPrimary(HostAndPortProvider provider) {
    this.primaryProvider = provider;
    return this;
  }

  public ResilientClientBuilderNew withSecondary(HostAndPortProvider provider) {
    this.secondaryProvider = provider;
    return this;
  }

  public ResilientClientBuilderNew retryingNonIdempotentMethods(boolean retryNonIdempotentMethods) {
    this.retryNonIdempotentMethods = retryNonIdempotentMethods;
    return this;
  }

  public ResilientClientBuilderNew withResilienceStrategy(ResilienceStrategy resilienceStrategy) {
    this.resilienceStrategy = resilienceStrategy;
    return this;
  }

  /* mostly for testing */
  public ResilientClientBuilderNew withHostAndPortResolver(HostAndPortIpResolver resolver) {
    this.hostAndPortIpResolver = resolver;
    return this;
  }

  public ResilientClientBuilderNew withContinuationPolicy(ContinuationPolicy continuationPolicy) {
    this.continuationPolicy = continuationPolicy;
    return this;
  }

  public ResilientClientBuilderNew withTransactionPropagation() {
    this.txHeader = DEFAULT_TX_HEADER;
    return this;
  }

  public ResilientClientBuilderNew withProtocol(String protocol) {
    this.protocol = protocol;
    return this;
  }

  public ResilientClientBuilderNew usingAdminPorts() {
    useAdminPorts = true;
    return this;
  }

  public ResilientClientNew build() {

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

    final ClientConfig clientConfig = buildConfig();

    ExecutorService threadPool =
        environment.createExecutorService(
            shortName, jerseyClientConfig.getMinThreads(), jerseyClientConfig.getMaxThreads());
    // client.register(threadPool);
    clientConfig.executorService(threadPool);

    final ResilientClientNew client =
        new ResilientClientNew(
            shortName,
            clientConfig,
            finalProvider,
            continuationPolicy,
            retryNonIdempotentMethods,
            appMetrics);

    if (jerseyClientConfig.isGzipEnabled()) {
      client.register(GZipEncoder.class);
      client.register(EncodingFilter.class);
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

  private ClientConfig buildConfig() {
    final ObjectMapper objectMapper = environment.createObjectMapper();

    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    return new ClientConfig(new JacksonMessageBodyProvider(objectMapper));
  }
}
