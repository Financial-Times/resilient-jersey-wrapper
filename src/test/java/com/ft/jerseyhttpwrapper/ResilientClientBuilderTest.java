package com.ft.jerseyhttpwrapper;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import com.ft.jerseyhttpwrapper.config.EndpointConfiguration;
import com.google.common.net.HostAndPort;
import io.dropwizard.client.JerseyClientConfiguration;
import java.util.Collections;
import java.util.Optional;
import org.junit.Test;

/**
 * ResilientClientBuilderTest
 *
 * @author Simon.Gibbs
 */
public class ResilientClientBuilderTest {

  @Test
  public void shouldSupportSingletonTestConfiguration() {

    ResilientClient client =
        ResilientClientBuilder.inTesting(locally()) // just one node
            .build();

    assertThat(client, notNullValue());
  }

  @Test
  public void testShouldAppendAdminToShortNameWhenUsingAdminPorts() {
    ResilientClient client = ResilientClientBuilder.inTesting(locally()).usingAdminPorts().build();

    assertThat(client.getShortName(), is(equalTo("test-localhost-8080-admin")));
  }

  @Test
  public void shouldSupportDNSOnlyDynamicConfiguration() {
    ResilientClient client = ResilientClientBuilder.inTesting().named("test").usingDNS().build();
    assertThat(client, notNullValue());
  }

  @Test(expected = IllegalStateException.class)
  public void shouldFailIfDNSOnlyDynamicConfigurationIsNotNamed() {
    ResilientClientBuilder.inTesting().usingDNS().build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldRejectNodesIfDynamicStrategySelected() {
    final JerseyClientConfiguration jerseyClientConfig = new JerseyClientConfiguration();
    final EndpointConfiguration endpointConfig =
        new EndpointConfiguration(
            Optional.empty(),
            Optional.of(jerseyClientConfig),
            Optional.empty(),
            Collections.singletonList("host:80:81"),
            Collections.emptyList());
    endpointConfig.setResilienceStrategy(ResilienceStrategy.DYNAMIC_RANDOM_IP_STRATEGY);

    ResilientClientBuilder.inTesting().using(endpointConfig).build();
  }

  private HostAndPort locally() {
    return HostAndPort.fromString("localhost:8080");
  }
}
