package com.ft.jerseyhttpwrapper;

import static com.google.common.base.MoreObjects.toStringHelper;

public enum ResilienceStrategy {
  EMPTY_STRATEGY("No hosts - for testing."),
  SIMPLE_FAILOVER_STRATEGY("Tries in fixed sequence."),
  LOAD_BALANCED_STRATEGY("Tries using random host."),
  LOAD_BALANCED_IP_STRATEGY("Resolves host names to IPs, then tries using a random IP."),
  DYNAMIC_RANDOM_IP_STRATEGY("Resolves host names to IPs on any feasible route.");

  private final String description;

  ResilienceStrategy(String description) {
    this.description = description;
  }

  @Override
  public String toString() {
    return toStringHelper(this).add("name", name()).add("description", description).toString();
  }
}
