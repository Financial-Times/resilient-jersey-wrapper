package com.ft.jerseyhttpwrapper.continuation;

import com.ft.jerseyhttpwrapper.providers.HostAndPortProvider;
import com.google.common.net.HostAndPort;
import java.util.Iterator;

/**
 * Iterates over all hosts once
 *
 * @author Simon.Gibbs
 */
public class DefaultContinuationSession implements ContinuationSession {

  private final HostAndPortProvider hostAndPortProvider;
  private final Iterator<HostAndPort> iterator;

  public DefaultContinuationSession(
      HostAndPort suppliedAddress, HostAndPortProvider hostAndPortProvider) {
    this.hostAndPortProvider = hostAndPortProvider;
    this.iterator = hostAndPortProvider.iterator(suppliedAddress);
  }

  @Override
  public boolean shouldContinue() {
    return iterator.hasNext();
  }

  @Override
  public HostAndPort nextHost() {
    return iterator.next();
  }

  @Override
  public void handleFailedHost(HostAndPort hostAndPort) {
    synchronized (hostAndPortProvider) {
      hostAndPortProvider.handleFailedHost(hostAndPort);
    }
  }
}
