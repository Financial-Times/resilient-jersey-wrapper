package com.ft.jerseyhttpwrapper;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ft.jerseyhttpwrapper.providers.HostAndPortIpResolver;
import com.google.common.net.HostAndPort;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

/**
 * Design Note:
 *
 * <p>The ResilientClient itself always makes actual HTTP requests, that's OK if you want to use
 * WireMock, but genuinely using DNS is not feasible with WireMock. That makes things rather
 * awkward.
 *
 * <p>Solution: FAIL! It's sometimes OK to fail deliberately as long as you know you tried.
 *
 * @author Simon.Gibbs
 */
public class ResilientClientUsingDNSTest {

  @Test
  public void shouldBeAbleToAttemptToAccessURLWithImplicitPortNumber() {

    HostAndPortIpResolver mockDNS = mock(HostAndPortIpResolver.class);

    ResilientClient client =
        ResilientClientBuilder.inTesting()
            .usingDNS()
            .named(this.getClass().getSimpleName())
            .withHostAndPortResolver(mockDNS)
            .build();

    RuntimeException expectedException = new RuntimeException("We can assert on this");

    ArgumentCaptor<HostAndPort> capturedEndpoint = ArgumentCaptor.forClass(HostAndPort.class);

    when(mockDNS.resolve(any(HostAndPort.class))).thenThrow(expectedException);

    try {
      client
          .resource("http://fail.example.com/hello-world.html")
          .get(String.class); // DOES NOT EXIST (we assume)
      fail("Expected the configured exception");
    } catch (RuntimeException e) {
      assertThat(e, is(expectedException));
    }

    verify(mockDNS).resolve(capturedEndpoint.capture());

    assertThat(capturedEndpoint.getValue().getPort(), is(80));
  }
}
