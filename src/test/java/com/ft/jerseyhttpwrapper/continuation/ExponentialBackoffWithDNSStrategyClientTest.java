package com.ft.jerseyhttpwrapper.continuation;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.ft.jerseyhttpwrapper.ResilientClientBuilder;
import com.ft.jerseyhttpwrapper.providers.HostAndPortIpResolver;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.google.common.net.HostAndPort;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.UniformInterfaceException;
import java.net.URI;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import javax.ws.rs.core.UriBuilder;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * ExponentialBackoffWithDNSStrategyClientTest
 *
 * @author Simon.Gibbs
 */
@RunWith(MockitoJUnitRunner.class)
public class ExponentialBackoffWithDNSStrategyClientTest {

  Client client;

  @Mock private HostAndPortIpResolver mockResolver;

  @ClassRule public static WireMockClassRule wmClassRule1 = new WireMockClassRule(0);

  @Rule public WireMockClassRule instanceRule1 = wmClassRule1;

  @ClassRule public static WireMockClassRule wmClassRule2 = new WireMockClassRule(0);

  @Rule public WireMockClassRule instanceRule2 = wmClassRule2;

  @ClassRule public static WireMockClassRule wmClassRule3 = new WireMockClassRule(0);

  @Rule public WireMockClassRule instanceRule3 = wmClassRule3;

  ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

  @Before
  public void setup() {
    client =
        ResilientClientBuilder.inTesting()
            .usingDNS()
            .withHostAndPortResolver(mockResolver)
            .withContinuationPolicy(new ExponentialBackoffContinuationPolicy(4, 1000))
            .named("test")
            .build();

    for (WireMockClassRule wm : Arrays.asList(instanceRule1, instanceRule2, instanceRule3)) {
      // fast network type failure
      wm.stubFor(get(urlEqualTo("/path")).willReturn(aResponse().withStatus(500)));
    }
  }

  @Test
  public void givenAClient() {
    assertThat(client, notNullValue());
  }

  @Test
  public void shouldHitEachEndpointInTurnAndWrapAround()
      throws InterruptedException, ExecutionException {

    givenThreeNodesAreLookedUp();

    Future<Exception> expectedException =
        asynchronously(
            new Callable<Exception>() {
              @Override
              public Exception call() {
                try {
                  client.resource(urlOf(instanceRule1)).get(String.class);
                } catch (Exception e) {
                  return e;
                }
                return null;
              }
            });

    Thread.sleep(1000);

    instanceRule1.verify(1, getRequestedFor(urlEqualTo("/path")));

    Thread.sleep(1000);

    instanceRule2.verify(1, getRequestedFor(urlEqualTo("/path")));

    Thread.sleep(2000);

    instanceRule3.verify(1, getRequestedFor(urlEqualTo("/path")));

    Thread.sleep(4000);

    instanceRule1.verify(2, getRequestedFor(urlEqualTo("/path")));

    assertThat(expectedException.get(), instanceOf(UniformInterfaceException.class));
  }

  private URI urlOf(WireMockClassRule instanceRule1) {
    return UriBuilder.fromPath("/path")
        .host("localhost")
        .port(instanceRule1.port())
        .scheme("http")
        .build();
  }

  private <T> Future<T> asynchronously(Callable<T> runnable) {
    return executor.submit(runnable);
  }

  private void givenThreeNodesAreLookedUp() {
    when(mockResolver.resolve(any(HostAndPort.class)))
        .thenReturn(
            Arrays.asList(
                HostAndPort.fromParts("localhost", instanceRule1.port()),
                HostAndPort.fromParts("localhost", instanceRule2.port()),
                HostAndPort.fromParts("localhost", instanceRule3.port())));
  }
}
