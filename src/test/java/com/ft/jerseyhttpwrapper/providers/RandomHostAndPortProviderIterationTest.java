package com.ft.jerseyhttpwrapper.providers;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.google.common.net.HostAndPort;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Random;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * RandomHostAndPortProviderIterationTest
 *
 * @author Simon.Gibbs
 */
@RunWith(MockitoJUnitRunner.class)
public class RandomHostAndPortProviderIterationTest {

  public static final ArrayList<HostAndPort> HOSTS_ABC =
      Lists.newArrayList(
          HostAndPort.fromParts("a", 1),
          HostAndPort.fromParts("b", 2),
          HostAndPort.fromParts("c", 3));

  public static final HostAndPort EXAMPLE_ENDPOINT = HOSTS_ABC.get(0);

  private RandomHostAndPortProvider provider;

  @Mock Random sequence;

  @Before
  public void setUp() {
    provider = new RandomHostAndPortProvider(HOSTS_ABC, sequence);
  }

  @Test
  public void shouldReturnSameHostsProvided() {
    when(sequence.nextInt(anyInt())).thenReturn(0);

    Iterator<HostAndPort> hosts = provider.iterator(EXAMPLE_ENDPOINT);

    assertThat(hosts.next().getHostText(), is("a"));
    assertThat(hosts.next().getHostText(), is("b"));
    assertThat(hosts.next().getHostText(), is("c"));
  }

  @Test
  public void shouldReportEndOfHostsList() {
    when(sequence.nextInt(anyInt())).thenReturn(0);

    Iterator<HostAndPort> hosts = provider.iterator(EXAMPLE_ENDPOINT);

    hosts.next();
    hosts.next();
    hosts.next();

    assertThat(hosts.hasNext(), is(false));
  }

  @Test(expected = NoSuchElementException.class)
  public void shouldBlowUpIfHasNextIsNotRespected() {
    when(sequence.nextInt(anyInt())).thenReturn(0);

    Iterator<HostAndPort> hosts = provider.iterator(EXAMPLE_ENDPOINT);

    hosts.next();
    hosts.next();
    hosts.next();
    hosts.next();
  }
}
