package com.ft.jerseyhttpwrapper.providers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.net.HostAndPort;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

/**
 * Ensure configuration is appropriately validated.
 *
 * @author Simon.Gibbs
 */
public class HostAndPortProviderConfigurationTest {

  List<HostAndPort> hostAndPortList =
      Arrays.asList(
          HostAndPort.fromParts("host-a.example.com", 80),
          HostAndPort.fromParts("host-b.example.com", 80));

  @Test
  public void givenAnExactMatchSimpleProviderShouldReportThatItHasAHostAndPortPair() {

    SimpleHostAndPortProvider provider = new SimpleHostAndPortProvider(hostAndPortList);

    assertTrue(provider.hasHost(URI.create("http://host-a.example.com:80/resource?foo=bar")));
  }

  @Test
  public void givenAnImplicitMatchSimpleProviderShouldReportThatItHasAHostAndPortPair() {

    SimpleHostAndPortProvider provider = new SimpleHostAndPortProvider(hostAndPortList);

    assertTrue(provider.hasHost(URI.create("http://host-a.example.com/resource?foo=bar")));
  }

  @Test
  public void givenAnMismatchSimpleProviderShouldReportItDoesNotHaveAHostAndPortPair() {

    SimpleHostAndPortProvider provider = new SimpleHostAndPortProvider(hostAndPortList);

    assertFalse(provider.hasHost(URI.create("http://anotherservice.com/thing/3")));
  }

  @Test
  public void givenAnExactMatchRandomProviderShouldReportThatItSupportsAHostAndPortPair() {

    RandomHostAndPortProvider provider = new RandomHostAndPortProvider(hostAndPortList);

    assertTrue(provider.supports(HostAndPort.fromParts("host-a.example.com", 80)));
  }

  @Test
  public void givenAnImplicitMatchRandomProviderShouldReportThatItSupportsAHostAndPortPair() {

    RandomHostAndPortProvider provider = new RandomHostAndPortProvider(hostAndPortList);

    assertTrue(provider.supports(HostAndPort.fromString("host-a.example.com")));
  }

  @Test
  public void givenAnMismatchRandomProviderShouldReportItDoesNotSupportAHostAndPortPair() {

    RandomHostAndPortProvider provider = new RandomHostAndPortProvider(hostAndPortList);

    assertFalse(provider.supports(HostAndPort.fromString("anotherservice.com")));
  }
}
