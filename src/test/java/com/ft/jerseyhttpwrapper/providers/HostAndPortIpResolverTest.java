package com.ft.jerseyhttpwrapper.providers;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
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
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HostAndPortIpResolverTest {

  private final InetAddress host1Ip;
  private final InetAddress host2Ip;
  private final InetAddress host3Ip;
  private final InetAddress host4Ip;

  @Mock HostToIpMapper hostToIpMapper;

  public HostAndPortIpResolverTest() throws UnknownHostException {
    host1Ip = InetAddress.getByAddress(new byte[] {10, 0, 0, 1});
    host2Ip = InetAddress.getByAddress(new byte[] {10, 0, 0, 2});
    host3Ip = InetAddress.getByAddress(new byte[] {10, 0, 0, 3});
    host4Ip = InetAddress.getByAddress(new byte[] {10, 0, 0, 4});
  }

  @Test
  public void should_resolve_one_hostport_with_one_ip_to_one_hostport() throws Exception {
    when(hostToIpMapper.mapToIps("host1")).thenReturn(new InetAddress[] {host1Ip});

    final HostAndPortIpResolver hostAndPortIpResolver = new HostAndPortIpResolver(hostToIpMapper);

    final List<HostAndPort> hostAndPorts =
        hostAndPortIpResolver.resolveAll(Arrays.asList(HostAndPort.fromString("host1:80")));

    assertThat(hostAndPorts.size(), equalTo(1));
    assertThat(hostAndPorts, hasItem(HostAndPort.fromString("10.0.0.1:80")));
  }

  @Test
  public void
      should_resolve_one_hostport_with_two_ips_to_two_hostsports_with_different_ips_but_same_port()
          throws Exception {
    when(hostToIpMapper.mapToIps("host1")).thenReturn(new InetAddress[] {host1Ip, host2Ip});

    final HostAndPortIpResolver hostAndPortIpResolver = new HostAndPortIpResolver(hostToIpMapper);

    final List<HostAndPort> hostAndPorts =
        hostAndPortIpResolver.resolveAll(Arrays.asList(HostAndPort.fromString("host1:80")));

    assertThat(hostAndPorts.size(), equalTo(2));
    assertThat(hostAndPorts, hasItem(HostAndPort.fromString("10.0.0.1:80")));
    assertThat(hostAndPorts, hasItem(HostAndPort.fromString("10.0.0.2:80")));
  }

  @Test
  public void
      should_resolve_two_hostports_with_same_single_ip_but_different_ports_to_two_hostports_same_ip_but_different_ports()
          throws Exception {
    when(hostToIpMapper.mapToIps("host1")).thenReturn(new InetAddress[] {host1Ip});

    final HostAndPortIpResolver hostAndPortIpResolver = new HostAndPortIpResolver(hostToIpMapper);

    final List<HostAndPort> hostAndPorts =
        hostAndPortIpResolver.resolveAll(
            Arrays.asList(HostAndPort.fromString("host1:80"), HostAndPort.fromString("host1:81")));

    assertThat(hostAndPorts.size(), equalTo(2));
    assertThat(hostAndPorts, hasItem(HostAndPort.fromString("10.0.0.1:80")));
    assertThat(hostAndPorts, hasItem(HostAndPort.fromString("10.0.0.1:81")));
  }

  @Test
  public void should_resolve_two_hostports_with_different_hosts_with_multiple_ips_and_same_ports()
      throws Exception {
    when(hostToIpMapper.mapToIps("host1")).thenReturn(new InetAddress[] {host1Ip, host3Ip});
    when(hostToIpMapper.mapToIps("host2")).thenReturn(new InetAddress[] {host2Ip, host4Ip});

    final HostAndPortIpResolver hostAndPortIpResolver = new HostAndPortIpResolver(hostToIpMapper);

    final List<HostAndPort> hostAndPorts =
        hostAndPortIpResolver.resolveAll(
            Arrays.asList(HostAndPort.fromString("host1:80"), HostAndPort.fromString("host2:80")));

    assertThat(hostAndPorts.size(), equalTo(4));
    assertThat(hostAndPorts, hasItem(HostAndPort.fromString("10.0.0.1:80")));
    assertThat(hostAndPorts, hasItem(HostAndPort.fromString("10.0.0.2:80")));
    assertThat(hostAndPorts, hasItem(HostAndPort.fromString("10.0.0.3:80")));
    assertThat(hostAndPorts, hasItem(HostAndPort.fromString("10.0.0.4:80")));
  }

  @Test
  public void
      should_resolve_two_hostports_with_different_hosts_and_ports_with_multiple_ips_and_expected_ports()
          throws Exception {
    when(hostToIpMapper.mapToIps("host1")).thenReturn(new InetAddress[] {host1Ip, host3Ip});
    when(hostToIpMapper.mapToIps("host2")).thenReturn(new InetAddress[] {host2Ip, host4Ip});

    final HostAndPortIpResolver hostAndPortIpResolver = new HostAndPortIpResolver(hostToIpMapper);

    final List<HostAndPort> hostAndPorts =
        hostAndPortIpResolver.resolveAll(
            Arrays.asList(HostAndPort.fromString("host1:80"), HostAndPort.fromString("host2:81")));

    assertThat(hostAndPorts.size(), equalTo(4));
    assertThat(hostAndPorts, hasItem(HostAndPort.fromString("10.0.0.1:80")));
    assertThat(hostAndPorts, hasItem(HostAndPort.fromString("10.0.0.2:81")));
    assertThat(hostAndPorts, hasItem(HostAndPort.fromString("10.0.0.3:80")));
    assertThat(hostAndPorts, hasItem(HostAndPort.fromString("10.0.0.4:81")));
  }

  @Test
  public void should_resolve_to_hosts_listed_in_same_order_as_given() throws Exception {
    when(hostToIpMapper.mapToIps("host1")).thenReturn(new InetAddress[] {host1Ip, host3Ip});
    when(hostToIpMapper.mapToIps("host2")).thenReturn(new InetAddress[] {host2Ip, host4Ip});

    final HostAndPortIpResolver hostAndPortIpResolver = new HostAndPortIpResolver(hostToIpMapper);

    final List<HostAndPort> hostAndPorts =
        hostAndPortIpResolver.resolveAll(
            Arrays.asList(HostAndPort.fromString("host1:80"), HostAndPort.fromString("host2:81")));

    assertThat(
        hostAndPorts,
        equalTo(
            Arrays.asList(
                HostAndPort.fromString("10.0.0.1:80"),
                HostAndPort.fromString("10.0.0.3:80"),
                HostAndPort.fromString("10.0.0.2:81"),
                HostAndPort.fromString("10.0.0.4:81"))));
  }

  @Test
  public void should_deduplicate_hosts_that_resolve_to_same_ips() throws Exception {
    when(hostToIpMapper.mapToIps("host1")).thenReturn(new InetAddress[] {host1Ip, host2Ip});
    when(hostToIpMapper.mapToIps("host2")).thenReturn(new InetAddress[] {host1Ip, host2Ip});

    final HostAndPortIpResolver hostAndPortIpResolver = new HostAndPortIpResolver(hostToIpMapper);

    final List<HostAndPort> hostAndPorts =
        hostAndPortIpResolver.resolveAll(
            Arrays.asList(
                HostAndPort.fromString("host1:80"),
                HostAndPort.fromString("host2:81"),
                HostAndPort.fromString("host2:80")));

    assertThat(
        hostAndPorts,
        equalTo(
            Arrays.asList(
                HostAndPort.fromString("10.0.0.1:80"),
                HostAndPort.fromString("10.0.0.2:80"),
                HostAndPort.fromString("10.0.0.1:81"),
                HostAndPort.fromString("10.0.0.2:81"))));
  }
}
