package com.ft.jerseyhttpwrapper.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ft.jerseyhttpwrapper.ResilienceStrategy;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import io.dropwizard.client.JerseyClientConfiguration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EndpointConfiguration {
  private static final Pattern URL_REGEX =
      Pattern.compile("(https?:\\/\\/)?([^:]+)(:\\d+)?(:\\d+)?");

  private final Optional<String> shortName;
  private final JerseyClientConfiguration jerseyClientConfiguration;
  private final String path;

  private List<SimpleEndpointConfiguration> primaryNodes;
  private List<SimpleEndpointConfiguration> secondaryNodes;

  @JsonProperty private boolean retryNonIdempotentMethods;

  @JsonProperty
  private ResilienceStrategy resilienceStrategy = ResilienceStrategy.LOAD_BALANCED_STRATEGY;

  /**
   * Creates a simple endpoint configuration for a test host (e.g. WireMock) with GZip disabled.
   *
   * @param host the test server
   * @param port the test port
   * @return a simple configuration
   */
  public static EndpointConfiguration forTesting(String host, int port) {

    JerseyClientConfiguration clientConfig = new JerseyClientConfiguration();
    clientConfig.setGzipEnabled(false);
    clientConfig.setGzipEnabledForRequests(false);

    return new EndpointConfiguration(
        Optional.of(String.format("test-%s-%s", host, port)),
        Optional.of(clientConfig),
        Optional.<String>absent(),
        Arrays.asList(String.format("%s:%d:%d", host, port, port + 1)),
        Collections.<String>emptyList());
  }

  public EndpointConfiguration(
      @JsonProperty("shortName") Optional<String> shortName,
      @JsonProperty("jerseyClient") Optional<JerseyClientConfiguration> jerseyClientConfiguration,
      @JsonProperty("path") Optional<String> path,
      @JsonProperty("primaryNodes") List<String> primaryNodesRaw,
      @JsonProperty("secondaryNodes") List<String> secondaryNodesRaw) {
    this.shortName = shortName;
    this.jerseyClientConfiguration = jerseyClientConfiguration.or(new JerseyClientConfiguration());
    this.path = path.or("/");
    this.primaryNodes = extractNodes(primaryNodesRaw, null);
    String protocol = primaryNodes.isEmpty() ? null : primaryNodes.get(0).getProtocol();
    this.secondaryNodes = extractNodes(secondaryNodesRaw, protocol);
  }

  public void setRetryNonIdempotentMethods(final boolean retryNonIdempotentMethods) {
    this.retryNonIdempotentMethods = retryNonIdempotentMethods;
  }

  public void setResilienceStrategy(final ResilienceStrategy resilienceStrategy) {
    this.resilienceStrategy = resilienceStrategy;
  }

  public Optional<String> getShortName() {
    return shortName;
  }

  @JsonIgnore
  public String getProtocol() {
    return getPrimaryNodes().get(0).getProtocol();
  }

  @JsonIgnore
  public String getHost() {
    return getPrimaryNodes().get(0).getHost();
  }

  @JsonIgnore
  public int getAdminPort() {
    return getPrimaryNodes().get(0).getAdminPort();
  }

  @JsonIgnore
  public int getPort() {
    return getPrimaryNodes().get(0).getPort();
  }

  public String getPath() {
    return path;
  }

  public JerseyClientConfiguration getJerseyClientConfiguration() {
    return jerseyClientConfiguration;
  }

  @JsonProperty("primaryNodes")
  public List<String> getPrimaryNodesAsHostAndPorts() {
    return produceNodes(primaryNodes);
  }

  @JsonProperty("secondaryNodes")
  public List<String> getSecondaryNodesAsHostAndPorts() {
    return produceNodes(secondaryNodes);
  }

  public List<SimpleEndpointConfiguration> getPrimaryNodes() {
    return primaryNodes;
  }

  public List<SimpleEndpointConfiguration> getSecondaryNodes() {
    return secondaryNodes;
  }

  public boolean isRetryNonIdempotentMethods() {
    return retryNonIdempotentMethods;
  }

  public ResilienceStrategy getResilienceStrategy() {
    return resilienceStrategy;
  }

  protected MoreObjects.ToStringHelper toStringHelper() {
    return MoreObjects.toStringHelper(this)
        .add("shortName", shortName)
        .add("jerseyClientConfiguration", jerseyClientConfiguration)
        .add("primaryNodes", primaryNodes)
        .add("secondaryNodes", secondaryNodes)
        .add("retryNonIdempotentMethods", retryNonIdempotentMethods)
        .add("resilienceStrategy", resilienceStrategy);
  }

  @Override
  public String toString() {
    return toStringHelper().toString();
  }

  private List<SimpleEndpointConfiguration> extractNodes(List<String> rawNodes, String protocol) {
    if (rawNodes == null || rawNodes.isEmpty()) {
      return Collections.emptyList();
    } else {
      List<SimpleEndpointConfiguration> nodes = new ArrayList<>();
      for (String rawNode : rawNodes) {
        Matcher matcher = URL_REGEX.matcher(rawNode.trim());
        if (matcher.matches()) {
          String endpointProtocol = matcher.group(1);
          if (endpointProtocol != null) {
            endpointProtocol =
                endpointProtocol.substring(0, endpointProtocol.length() - 3); // must end with "://"

            if (protocol == null) {
              protocol = endpointProtocol;
            } else if (!endpointProtocol.equals(protocol)) {
              throw new IllegalArgumentException(
                  String.format(
                      "All nodes in a group must use the same protocol (%s conflicts with %s)",
                      protocol, rawNode));
            }
          } else if (protocol == null) {
            protocol = "http"; // default
          }

          String host = matcher.group(2);
          String endpointAppPort = matcher.group(3);
          int port =
              (endpointAppPort == null)
                  ? defaultPortFor(protocol)
                  : Integer.parseInt(endpointAppPort.substring(1));

          String endpointAdminPort = matcher.group(4);
          int adminPort =
              (endpointAdminPort == null) ? port : Integer.parseInt(endpointAdminPort.substring(1));

          nodes.add(new SimpleEndpointConfiguration(protocol, host, port, adminPort));
        } else {
          throw new IllegalArgumentException(
              String.format("`%s` is not a valid endpoint value.", rawNode));
        }
      }
      return nodes;
    }
  }

  private int defaultPortFor(String protocol) {
    return "https".equals(protocol) ? 443 : 80;
  }

  private List<String> produceNodes(
      List<SimpleEndpointConfiguration> simpleEndpointConfigurations) {
    List<String> hostPortAdminPortNodes = new ArrayList<>(primaryNodes.size());
    for (SimpleEndpointConfiguration config : simpleEndpointConfigurations) {
      hostPortAdminPortNodes.add(config.getHostAndPorts());
    }
    return hostPortAdminPortNodes;
  }
}
