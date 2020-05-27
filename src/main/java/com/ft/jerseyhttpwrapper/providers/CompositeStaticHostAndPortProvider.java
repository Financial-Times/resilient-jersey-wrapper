package com.ft.jerseyhttpwrapper.providers;

import static com.google.common.collect.Iterators.concat;

import com.google.common.net.HostAndPort;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Provides access to a sequenced list of providers with validation.
 *
 * <p>The component providers are expected to have explicit support for the contacted host.
 *
 * @author Simon.Gibbs
 */
public class CompositeStaticHostAndPortProvider implements HostAndPortProvider {

  private List<HostAndPortProvider> providers;

  public CompositeStaticHostAndPortProvider(HostAndPortProvider... providers) {
    this.providers = Arrays.asList(providers);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Iterator<HostAndPort> iterator(HostAndPort suppliedAddress) {

    Iterator[] iteratorsArray = new Iterator[providers.size()];
    for (int i = 0; i < providers.size(); i++) {

      HostAndPortProvider provider = providers.get(i);

      iteratorsArray[i] = provider.iterator(suppliedAddress);
    }

    return (Iterator<HostAndPort>) concat(iteratorsArray);
  }

  @Override
  public void handleFailedHost(HostAndPort hostAndPort) {
    for (HostAndPortProvider provider : providers) {
      if (provider.supports(hostAndPort)) {
        provider.handleFailedHost(hostAndPort);
      }
    }
  }

  @Override
  public boolean supports(HostAndPort hostAndPort) {
    for (HostAndPortProvider provider : providers) {
      if (provider.supports(hostAndPort)) {
        return true;
      }
    }
    return false;
  }

  private IllegalArgumentException unknownHost(HostAndPort hostAndPort) {
    return new IllegalArgumentException("Unknown host and port " + hostAndPort.toString());
  }
}
