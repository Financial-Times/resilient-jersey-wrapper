package com.ft.jerseyhttpwrapper.continuation;

import com.ft.jerseyhttpwrapper.providers.HostAndPortProvider;
import com.google.common.net.HostAndPort;

/**
 * ExponentialBackoffContinuation
 *
 * @author Simon.Gibbs
 */
public class ExponentialBackoffContinuationPolicy implements ContinuationPolicy {
  private int maxAttempts;
  private int backoffMultiplier;

  public ExponentialBackoffContinuationPolicy(int maxAttempts, int backoffMultiplier) {
    this.maxAttempts = maxAttempts;
    this.backoffMultiplier = backoffMultiplier;
  }

  @Override
  public ContinuationSession startSession(
      HostAndPort suppliedAddress, HostAndPortProvider hostAndPortProvider) {
    return new ExponentialBackoffContinuationSession(suppliedAddress, hostAndPortProvider, this);
  }

  public int getMaxAttempts() {
    return maxAttempts;
  }

  public int getBackoffMultiplier() {
    return backoffMultiplier;
  }
}
