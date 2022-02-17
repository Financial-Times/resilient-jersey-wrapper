package com.ft.jerseyhttpwrapper.providers;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.google.common.net.HostAndPort;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RandomIpHostAndPortProviderTest {

  private final InetAddress ip1;
  private final InetAddress ip2;
  private final InetAddress ip3;

  @Mock HostToIpMapper hostToIpMapper;

  public RandomIpHostAndPortProviderTest() throws UnknownHostException {
    this.ip1 = InetAddress.getByAddress(new byte[] {10, 0, 0, 1});
    this.ip2 = InetAddress.getByAddress(new byte[] {10, 0, 0, 2});
    this.ip3 = InetAddress.getByAddress(new byte[] {10, 0, 0, 3});
  }

  @Test
  public void should_resolve_single_host_with_two_ips_to_two_host_and_port() throws Exception {

    HostAndPortIpResolver hostAndPortIpResolver = new HostAndPortIpResolver(hostToIpMapper);

    when(hostToIpMapper.mapToIps("host")).thenReturn(new InetAddress[] {ip1, ip2});

    List<HostAndPort> hostAndPortNames = Arrays.asList(HostAndPort.fromString("host:80"));
    RandomIpHostAndPortProvider hostAndPortProvider =
        new RandomIpHostAndPortProvider(hostAndPortNames, hostAndPortIpResolver);

    List<HostAndPort> hostAndPortIps =
        Arrays.asList(HostAndPort.fromString("10.0.0.1:80"), HostAndPort.fromString("10.0.0.2:80"));

    assertThat(hostAndPortProvider.getHostNames(), equalTo(hostAndPortIps));
    assertThat(
        hostAndPortProvider.getHostNames(), hasItems(hostAndPortIps.get(0), hostAndPortIps.get(1)));
  }

  @Test
  public void should_resolve_list_of_hosts() throws Exception {

    HostAndPortIpResolver hostAndPortIpResolver = new HostAndPortIpResolver(hostToIpMapper);

    when(hostToIpMapper.mapToIps("host1")).thenReturn(new InetAddress[] {ip1});
    when(hostToIpMapper.mapToIps("host2")).thenReturn(new InetAddress[] {ip2, ip3});

    List<HostAndPort> hostAndPortNames =
        Arrays.asList(HostAndPort.fromString("host1:80"), HostAndPort.fromString("host2:81"));
    RandomIpHostAndPortProvider hostAndPortProvider =
        new RandomIpHostAndPortProvider(hostAndPortNames, hostAndPortIpResolver);

    List<HostAndPort> hostAndPortIps =
        Arrays.asList(
            HostAndPort.fromString("10.0.0.1:80"),
            HostAndPort.fromString("10.0.0.2:81"),
            HostAndPort.fromString("10.0.0.3:81"));

    assertThat(hostAndPortProvider.getHostNames(), equalTo(hostAndPortIps));
    assertThat(
        hostAndPortProvider.getHostNames(),
        hasItems(hostAndPortIps.get(0), hostAndPortIps.get(1), hostAndPortIps.get(2)));
  }
}
