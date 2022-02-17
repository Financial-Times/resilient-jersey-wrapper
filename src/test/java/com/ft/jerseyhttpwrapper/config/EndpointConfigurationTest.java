package com.ft.jerseyhttpwrapper.config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.ft.jerseyhttpwrapper.ResilienceStrategy;
import com.google.common.base.Optional;
import io.dropwizard.client.JerseyClientConfiguration;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;

public class EndpointConfigurationTest {

  ObjectMapper objectMapper = new ObjectMapper();

  @Before
  public void registerGuavaModules() {
    objectMapper.registerModule(new GuavaModule());
  }

  @Test
  public void shouldNotFailOnNullNodes() {
    new EndpointConfiguration(Optional.absent(), Optional.absent(), Optional.absent(), null, null);
  }

  @Test
  public void shouldNotFailOnEmptyNodes() {
    new EndpointConfiguration(
        Optional.absent(),
        Optional.absent(),
        Optional.absent(),
        Collections.emptyList(),
        Collections.emptyList());
  }

  @Test
  public void shouldHaveNoNodesWhenNullNodes() {
    EndpointConfiguration configuration =
        new EndpointConfiguration(
            Optional.absent(), Optional.absent(), Optional.absent(), null, null);
    assertThat(configuration.getPrimaryNodes().size(), equalTo(0));
    assertThat(configuration.getSecondaryNodes().size(), equalTo(0));
  }

  @Test
  public void shouldHaveNoNodesWhenEmptyNodes() {
    EndpointConfiguration configuration =
        new EndpointConfiguration(
            Optional.absent(),
            Optional.absent(),
            Optional.absent(),
            Collections.emptyList(),
            Collections.emptyList());
    assertThat(configuration.getPrimaryNodes().size(), equalTo(0));
    assertThat(configuration.getSecondaryNodes().size(), equalTo(0));
  }

  @Test
  public void shouldHaveOnePrimaryNodeWhenOneNodeGiven() {
    EndpointConfiguration configuration =
        new EndpointConfiguration(
            Optional.absent(),
            Optional.absent(),
            Optional.absent(),
            Collections.singletonList("host:80:81"),
            Collections.emptyList());
    assertThat(configuration.getPrimaryNodes().size(), equalTo(1));
    assertThat(configuration.getSecondaryNodes().size(), equalTo(0));

    assertThat(configuration.getPrimaryNodes().get(0).getHost(), equalTo("host"));
    assertThat(configuration.getPrimaryNodes().get(0).getPort(), equalTo(80));
    assertThat(configuration.getPrimaryNodes().get(0).getAdminPort(), equalTo(81));
  }

  @Test
  public void shouldHaveOneSecondaryNodeWhenOneNodeGiven() {
    EndpointConfiguration configuration =
        new EndpointConfiguration(
            Optional.absent(),
            Optional.absent(),
            Optional.absent(),
            Collections.emptyList(),
            Collections.singletonList("host:80:81"));
    assertThat(configuration.getPrimaryNodes().size(), equalTo(0));
    assertThat(configuration.getSecondaryNodes().size(), equalTo(1));

    assertThat(configuration.getSecondaryNodes().get(0).getHost(), equalTo("host"));
    assertThat(configuration.getSecondaryNodes().get(0).getPort(), equalTo(80));
    assertThat(configuration.getSecondaryNodes().get(0).getAdminPort(), equalTo(81));
  }

  @Test
  public void shouldHaveOneSecondaryNodeAndCorrectAdminPortWhenOneNodeGivenAndNoAdminPortGiven() {
    EndpointConfiguration configuration =
        new EndpointConfiguration(
            Optional.absent(),
            Optional.absent(),
            Optional.absent(),
            Collections.emptyList(),
            Collections.singletonList("host:80"));
    assertThat(configuration.getPrimaryNodes().size(), equalTo(0));
    assertThat(configuration.getSecondaryNodes().size(), equalTo(1));

    assertThat(configuration.getSecondaryNodes().get(0).getHost(), equalTo("host"));
    assertThat(configuration.getSecondaryNodes().get(0).getPort(), equalTo(80));
    assertThat(configuration.getSecondaryNodes().get(0).getAdminPort(), equalTo(80));
  }

  @Test
  public void shouldHaveTwoPrimaryNodesWhenTwoNodesGiven() {
    EndpointConfiguration configuration =
        new EndpointConfiguration(
            Optional.absent(),
            Optional.absent(),
            Optional.absent(),
            Arrays.asList("host:80:81", "host2:90:91"),
            Collections.emptyList());
    assertThat(configuration.getPrimaryNodes().size(), equalTo(2));
    assertThat(configuration.getSecondaryNodes().size(), equalTo(0));

    assertThat(configuration.getPrimaryNodes().get(0).getProtocol(), equalTo("http"));
    assertThat(configuration.getPrimaryNodes().get(0).getHost(), equalTo("host"));
    assertThat(configuration.getPrimaryNodes().get(0).getPort(), equalTo(80));
    assertThat(configuration.getPrimaryNodes().get(0).getAdminPort(), equalTo(81));
    assertThat(configuration.getPrimaryNodes().get(1).getProtocol(), equalTo("http"));
    assertThat(configuration.getPrimaryNodes().get(1).getHost(), equalTo("host2"));
    assertThat(configuration.getPrimaryNodes().get(1).getPort(), equalTo(90));
    assertThat(configuration.getPrimaryNodes().get(1).getAdminPort(), equalTo(91));
  }

  @Test
  public void shouldHaveTwoPrimaryNodesWhenTwoNodesGivenWeirdWhiteSpace() {
    EndpointConfiguration configuration =
        new EndpointConfiguration(
            Optional.absent(),
            Optional.absent(),
            Optional.absent(),
            Arrays.asList(" host:80:81 ", " host2:90:91"),
            Collections.emptyList());
    assertThat(configuration.getPrimaryNodes().size(), equalTo(2));
    assertThat(configuration.getSecondaryNodes().size(), equalTo(0));

    assertThat(configuration.getPrimaryNodes().get(0).getHost(), equalTo("host"));
    assertThat(configuration.getPrimaryNodes().get(0).getPort(), equalTo(80));
    assertThat(configuration.getPrimaryNodes().get(0).getAdminPort(), equalTo(81));
    assertThat(configuration.getPrimaryNodes().get(1).getHost(), equalTo("host2"));
    assertThat(configuration.getPrimaryNodes().get(1).getPort(), equalTo(90));
    assertThat(configuration.getPrimaryNodes().get(1).getAdminPort(), equalTo(91));
  }

  @Test
  public void shouldHaveThreePrimaryNodesWhenTwoNodesGivenWeirdWhiteSpace() {
    EndpointConfiguration configuration =
        new EndpointConfiguration(
            Optional.absent(),
            Optional.absent(),
            Optional.absent(),
            Arrays.asList(" host:80:81 ", "host2:90:91 ", "host3:100:101"),
            Collections.emptyList());
    assertThat(configuration.getPrimaryNodes().size(), equalTo(3));
    assertThat(configuration.getSecondaryNodes().size(), equalTo(0));

    assertThat(configuration.getPrimaryNodes().get(0).getHost(), equalTo("host"));
    assertThat(configuration.getPrimaryNodes().get(0).getPort(), equalTo(80));
    assertThat(configuration.getPrimaryNodes().get(0).getAdminPort(), equalTo(81));
    assertThat(configuration.getPrimaryNodes().get(1).getHost(), equalTo("host2"));
    assertThat(configuration.getPrimaryNodes().get(1).getPort(), equalTo(90));
    assertThat(configuration.getPrimaryNodes().get(1).getAdminPort(), equalTo(91));
    assertThat(configuration.getPrimaryNodes().get(2).getHost(), equalTo("host3"));
    assertThat(configuration.getPrimaryNodes().get(2).getPort(), equalTo(100));
    assertThat(configuration.getPrimaryNodes().get(2).getAdminPort(), equalTo(101));
  }

  @Test
  public void shouldRoundTripConfiguration() throws Exception {
    final JerseyClientConfiguration jerseyClientConfig = new JerseyClientConfiguration();
    final EndpointConfiguration endpointConfig =
        new EndpointConfiguration(
            Optional.of("name"),
            Optional.of(jerseyClientConfig),
            Optional.of("path"),
            Arrays.asList(" host:80:81", "host2:90:91"),
            Collections.singletonList("host3:100:101"));

    final String json = objectMapper.writeValueAsString(endpointConfig);
    final EndpointConfiguration readEndpointConfig =
        objectMapper.readValue(json, EndpointConfiguration.class);

    assertThat(readEndpointConfig.getShortName(), equalTo(endpointConfig.getShortName()));
    assertThat(readEndpointConfig.getPath(), equalTo(endpointConfig.getPath()));

    assertThat(readEndpointConfig.getPrimaryNodes(), equalTo(endpointConfig.getPrimaryNodes()));
    assertThat(readEndpointConfig.getSecondaryNodes(), equalTo(endpointConfig.getSecondaryNodes()));

    assertThat(readEndpointConfig.isRetryNonIdempotentMethods(), is(false));
  }

  @Test
  public void shouldAcceptRetryNonIdempotentMethodsOption() throws Exception {
    final JerseyClientConfiguration jerseyClientConfig = new JerseyClientConfiguration();
    final EndpointConfiguration endpointConfig =
        new EndpointConfiguration(
            Optional.absent(),
            Optional.of(jerseyClientConfig),
            Optional.absent(),
            Collections.singletonList(" host:80:81"),
            Collections.emptyList());
    endpointConfig.setRetryNonIdempotentMethods(true);

    final String json = objectMapper.writeValueAsString(endpointConfig);
    final EndpointConfiguration readEndpointConfig =
        objectMapper.readValue(json, EndpointConfiguration.class);

    assertThat(readEndpointConfig.isRetryNonIdempotentMethods(), is(true));
  }

  @Test
  public void shouldAcceptResilienceStrategyOption() throws Exception {
    final JerseyClientConfiguration jerseyClientConfig = new JerseyClientConfiguration();
    final EndpointConfiguration endpointConfig =
        new EndpointConfiguration(
            Optional.absent(),
            Optional.of(jerseyClientConfig),
            Optional.absent(),
            Collections.singletonList("host:80:81"),
            Collections.emptyList());
    endpointConfig.setResilienceStrategy(ResilienceStrategy.LOAD_BALANCED_IP_STRATEGY);

    final String json = objectMapper.writeValueAsString(endpointConfig);
    final EndpointConfiguration readEndpointConfig =
        objectMapper.readValue(json, EndpointConfiguration.class);

    assertThat(
        readEndpointConfig.getResilienceStrategy(),
        is(ResilienceStrategy.LOAD_BALANCED_IP_STRATEGY));
  }

  @Test
  public void shouldDefaultToNotRetryNonIdempotentMethods() {
    final JerseyClientConfiguration jerseyClientConfig = new JerseyClientConfiguration();
    final EndpointConfiguration endpointConfig =
        new EndpointConfiguration(
            Optional.absent(),
            Optional.of(jerseyClientConfig),
            Optional.absent(),
            Collections.singletonList(" host:80:81"),
            Collections.emptyList());

    assertThat(endpointConfig.isRetryNonIdempotentMethods(), is(false));
  }

  @Test
  public void thatDefaultsToHttpOnPort80() {
    EndpointConfiguration configuration =
        new EndpointConfiguration(
            Optional.absent(),
            Optional.absent(),
            Optional.absent(),
            Arrays.asList("host1", "host2:90:91"),
            Collections.emptyList());
    assertThat(configuration.getPrimaryNodes().size(), equalTo(2));
    assertThat(configuration.getSecondaryNodes().size(), equalTo(0));

    assertThat(configuration.getPrimaryNodes().get(0).getProtocol(), equalTo("http"));
    assertThat(configuration.getPrimaryNodes().get(0).getHost(), equalTo("host1"));
    assertThat(configuration.getPrimaryNodes().get(0).getPort(), equalTo(80));
    assertThat(configuration.getPrimaryNodes().get(0).getAdminPort(), equalTo(80));
    assertThat(configuration.getPrimaryNodes().get(1).getProtocol(), equalTo("http"));
    assertThat(configuration.getPrimaryNodes().get(1).getHost(), equalTo("host2"));
    assertThat(configuration.getPrimaryNodes().get(1).getPort(), equalTo(90));
    assertThat(configuration.getPrimaryNodes().get(1).getAdminPort(), equalTo(91));
  }

  @Test
  public void thatDefaultsToPort80ForHttp() {
    EndpointConfiguration configuration =
        new EndpointConfiguration(
            Optional.absent(),
            Optional.absent(),
            Optional.absent(),
            Arrays.asList("http://host1", "host2:90:91"),
            Collections.emptyList());
    assertThat(configuration.getPrimaryNodes().size(), equalTo(2));
    assertThat(configuration.getSecondaryNodes().size(), equalTo(0));

    assertThat(configuration.getPrimaryNodes().get(0).getProtocol(), equalTo("http"));
    assertThat(configuration.getPrimaryNodes().get(0).getHost(), equalTo("host1"));
    assertThat(configuration.getPrimaryNodes().get(0).getPort(), equalTo(80));
    assertThat(configuration.getPrimaryNodes().get(0).getAdminPort(), equalTo(80));
    assertThat(configuration.getPrimaryNodes().get(1).getProtocol(), equalTo("http"));
    assertThat(configuration.getPrimaryNodes().get(1).getHost(), equalTo("host2"));
    assertThat(configuration.getPrimaryNodes().get(1).getPort(), equalTo(90));
    assertThat(configuration.getPrimaryNodes().get(1).getAdminPort(), equalTo(91));
  }

  @Test
  public void thatHttpsIsSupported() {
    EndpointConfiguration configuration =
        new EndpointConfiguration(
            Optional.absent(),
            Optional.absent(),
            Optional.absent(),
            Arrays.asList("https://host:8443:8444", "host2:9090:9091"),
            Collections.emptyList());
    assertThat(configuration.getPrimaryNodes().size(), equalTo(2));
    assertThat(configuration.getSecondaryNodes().size(), equalTo(0));

    assertThat(configuration.getPrimaryNodes().get(0).getProtocol(), equalTo("https"));
    assertThat(configuration.getPrimaryNodes().get(0).getHost(), equalTo("host"));
    assertThat(configuration.getPrimaryNodes().get(0).getPort(), equalTo(8443));
    assertThat(configuration.getPrimaryNodes().get(0).getAdminPort(), equalTo(8444));
    assertThat(configuration.getPrimaryNodes().get(1).getProtocol(), equalTo("https"));
    assertThat(configuration.getPrimaryNodes().get(1).getHost(), equalTo("host2"));
    assertThat(configuration.getPrimaryNodes().get(1).getPort(), equalTo(9090));
    assertThat(configuration.getPrimaryNodes().get(1).getAdminPort(), equalTo(9091));
  }

  @Test
  public void thatDefaultsToPort443ForHttps() {
    EndpointConfiguration configuration =
        new EndpointConfiguration(
            Optional.absent(),
            Optional.absent(),
            Optional.absent(),
            Arrays.asList("https://host1", "host2:90:91"),
            Collections.emptyList());
    assertThat(configuration.getPrimaryNodes().size(), equalTo(2));
    assertThat(configuration.getSecondaryNodes().size(), equalTo(0));

    assertThat(configuration.getPrimaryNodes().get(0).getProtocol(), equalTo("https"));
    assertThat(configuration.getPrimaryNodes().get(0).getHost(), equalTo("host1"));
    assertThat(configuration.getPrimaryNodes().get(0).getPort(), equalTo(443));
    assertThat(configuration.getPrimaryNodes().get(0).getAdminPort(), equalTo(443));
    assertThat(configuration.getPrimaryNodes().get(1).getProtocol(), equalTo("https"));
    assertThat(configuration.getPrimaryNodes().get(1).getHost(), equalTo("host2"));
    assertThat(configuration.getPrimaryNodes().get(1).getPort(), equalTo(90));
    assertThat(configuration.getPrimaryNodes().get(1).getAdminPort(), equalTo(91));
  }

  @Test(expected = IllegalArgumentException.class)
  public void thatMixedProtocolsAreNotAllowed() {
    new EndpointConfiguration(
        Optional.absent(),
        Optional.absent(),
        Optional.absent(),
        Arrays.asList("https://host1", "http://host2:90:91"),
        Collections.emptyList());
  }

  @Test
  public void thatSecondaryNodeDefaultsToPrimaryNodesProtocol() {
    EndpointConfiguration configuration =
        new EndpointConfiguration(
            Optional.absent(),
            Optional.absent(),
            Optional.absent(),
            Collections.singletonList("https://host1"),
            Collections.singletonList("host2"));
    assertThat(configuration.getPrimaryNodes().size(), equalTo(1));
    assertThat(configuration.getSecondaryNodes().size(), equalTo(1));

    assertThat(configuration.getPrimaryNodes().get(0).getProtocol(), equalTo("https"));
    assertThat(configuration.getPrimaryNodes().get(0).getHost(), equalTo("host1"));
    assertThat(configuration.getPrimaryNodes().get(0).getPort(), equalTo(443));
    assertThat(configuration.getPrimaryNodes().get(0).getAdminPort(), equalTo(443));
    assertThat(configuration.getSecondaryNodes().get(0).getProtocol(), equalTo("https"));
    assertThat(configuration.getSecondaryNodes().get(0).getHost(), equalTo("host2"));
    assertThat(configuration.getSecondaryNodes().get(0).getPort(), equalTo(443));
    assertThat(configuration.getSecondaryNodes().get(0).getAdminPort(), equalTo(443));
  }

  @Test(expected = IllegalArgumentException.class)
  public void thatMixedPrimaryAndSecondaryProtocolsAreNotAllowed() {
    new EndpointConfiguration(
        Optional.absent(),
        Optional.absent(),
        Optional.absent(),
        Collections.singletonList(/*http://*/ "host1"),
        Collections.singletonList("https://host2"));
  }
}
