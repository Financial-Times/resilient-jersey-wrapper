package com.ft.jerseyhttpwrapper.providers;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

import com.google.common.net.HostAndPort;
import java.util.Iterator;
import org.junit.Test;

/**
 * CompositeStaticHostAndPortProviderTest
 *
 * @author Simon.Gibbs
 */
public class CompositeStaticHostAndPortProviderTest {

  private HostAndPort hostA = HostAndPort.fromString("a.example.com:80");
  private HostAndPort hostB = HostAndPort.fromString("b.example.com:80");
  private HostAndPort hostC = HostAndPort.fromString("c.example.com:80");
  private HostAndPort hostD = HostAndPort.fromString("d.example.com:80");

  private SimpleHostAndPortProvider primary = new SimpleHostAndPortProvider(hostA, hostB);
  private SimpleHostAndPortProvider secondary = new SimpleHostAndPortProvider(hostC, hostD);

  @Test
  public void iteratorShouldReturnAllItems() throws Exception {
    final CompositeStaticHostAndPortProvider composite =
        new CompositeStaticHostAndPortProvider(primary, secondary);

    assertThat(iterableOf(composite.iterator(hostA)), hasItems(hostA, hostB, hostC, hostD));
    assertThat(iterableOf(composite.iterator(hostB)), hasItems(hostA, hostB, hostC, hostD));
    assertThat(iterableOf(composite.iterator(hostC)), hasItems(hostA, hostB, hostC, hostD));
    assertThat(iterableOf(composite.iterator(hostD)), hasItems(hostA, hostB, hostC, hostD));
  }

  @Test
  public void shouldNotSupportTheSuppliedItemIfItIsNotAValidAlternative() throws Exception {
    HostAndPort hostE = HostAndPort.fromString("e.example.com:80");

    final CompositeStaticHostAndPortProvider composite =
        new CompositeStaticHostAndPortProvider(primary, secondary);

    assertThat(composite.supports(hostE), is(false));
  }

  @Test
  public void shouldSupportTheKnownItemIfItFeaturesInAnyProvider() throws Exception {
    final CompositeStaticHostAndPortProvider composite =
        new CompositeStaticHostAndPortProvider(primary, secondary);

    assertThat(composite.supports(hostA), is(true));
    assertThat(composite.supports(hostB), is(true));
    assertThat(composite.supports(hostC), is(true));
    assertThat(composite.supports(hostD), is(true));
  }

  @Test
  public void shouldReportFailedHostToCorrectComposite() throws Exception {
    SimpleHostAndPortProvider surveilledPrimary = spy(primary);
    SimpleHostAndPortProvider surveilledSecondary = spy(secondary);

    final CompositeStaticHostAndPortProvider composite =
        new CompositeStaticHostAndPortProvider(surveilledPrimary, surveilledSecondary);

    composite.handleFailedHost(hostC);

    verify(surveilledPrimary, never()).handleFailedHost(hostC);
    verify(surveilledSecondary, times(1)).handleFailedHost(hostC);

    composite.handleFailedHost(hostA);

    verify(surveilledPrimary, times(1)).handleFailedHost(hostA);

    // and we didn't call this again
    verify(surveilledSecondary, times(1)).handleFailedHost(hostC);
  }

  private Iterable<HostAndPort> iterableOf(final Iterator<HostAndPort> iterator) {
    return new Iterable<HostAndPort>() {
      @Override
      public Iterator<HostAndPort> iterator() {
        return iterator;
      }
    };
  }
}
