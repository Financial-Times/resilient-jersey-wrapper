package com.ft.jerseyhttpwrapper.providers;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import com.ft.jerseyhttpwrapper.ResilientClient;
import com.ft.jerseyhttpwrapper.ResilientClientBuilder;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.net.HostAndPort;
import java.net.URI;
import java.util.Arrays;
import javax.ws.rs.core.UriBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * DynamicDNSProviderTest
 *
 * @author Simon.Gibbs
 */
@RunWith(MockitoJUnitRunner.class)
public class DynamicDNSProviderTest {

  public static final String OK_BODY = "OK";
  public static final String HOST_A = "a.example.com:80";
  public static final String HOST_B = "b.example.com:80";
  public static final String PATH = "/path";
  @Mock HostAndPortIpResolver ipResolver;

  @Rule public WireMockRule wm1 = new WireMockRule(wireMockConfig().port(0));

  @Rule public WireMockRule wm2 = new WireMockRule(wireMockConfig().port(0));

  @Rule public WireMockRule wm3 = new WireMockRule(wireMockConfig().port(0));

  @Rule public WireMockRule wm4 = new WireMockRule(wireMockConfig().port(0));

  private HostAndPort hostA = HostAndPort.fromString("a.example.com:80");
  private HostAndPort hostB = HostAndPort.fromString("b.example.com:80");

  private ResilientClient client;

  @Before
  public void setIpResolver() {

    when(ipResolver.resolve(hostA))
        .thenReturn(Arrays.<HostAndPort>asList(wiremockPort(wm1), wiremockPort(wm2)));
    when(ipResolver.resolve(hostB))
        .thenReturn(Arrays.<HostAndPort>asList(wiremockPort(wm3), wiremockPort(wm4)));

    client =
        ResilientClientBuilder.inTesting()
            .named("test")
            .usingDNS()
            .withHostAndPortResolver(ipResolver)
            .build();
  }

  /*
   * IPs are, of course, faked so that the test can run on a single machine. The component responsible for returning multiple
   * IPs is mocked and will return multiple ports instead.
   */
  @Test
  public void shouldBalanceOverMultipleHostnamesAndIPs() {

    // hostname a fails once and moves from host 1 to 2
    wm1.stubFor(outage());
    wm2.stubFor(ok());

    // hostname b fails once and moves from host 3 to 4
    wm3.stubFor(outage());
    wm4.stubFor(ok());

    // so hitting hostname a actually does work (on the second host)
    assertThat(hit(HOST_A), is(OK_BODY));

    wm1.verify(1, ourHit());
    wm2.verify(1, ourHit());

    // but hosts 3 and 4 were not used
    wm3.verify(0, ourHit());
    wm4.verify(0, ourHit());

    // now hit hostname b, which should ultimately also work fine
    assertThat(hit(HOST_B), is(OK_BODY));

    // check there were no additional hits on host 1 and 2.
    wm1.verify(1, ourHit());
    wm2.verify(1, ourHit());

    // and hosts 3 and 4 had the same behaviour as 1 and 2 did earlier: failing once then trying
    // again and winning
    wm3.verify(1, ourHit());
    wm4.verify(1, ourHit());
  }

  private String hit(String hostAndPort) {

    HostAndPort endpoint = HostAndPort.fromString(hostAndPort);

    URI hitUri =
        UriBuilder.fromPath(PATH)
            .host(endpoint.getHostText())
            .port(endpoint.getPort())
            .scheme("http")
            .build();

    return client.resource(hitUri).get(String.class);
  }

  private RequestPatternBuilder ourHit() {
    return getRequestedFor(urlEqualTo(PATH));
  }

  private MappingBuilder ok() {
    return get(urlEqualTo(PATH)).willReturn(aResponse().withStatus(200).withBody("OK"));
  }

  private MappingBuilder outage() {
    return get(urlEqualTo(PATH)).willReturn(aResponse().withStatus(503));
  }

  private static HostAndPort wiremockPort(WireMockRule rule) {
    return HostAndPort.fromParts("localhost", rule.port());
  }
}
