package com.ft.jerseyhttpwrapper;

import com.codahale.metrics.Timer;
import java.net.URI;
import org.glassfish.jersey.client.ClientRequest;

/**
 * AttemptLogger
 *
 * @author Simon.Gibbs
 */
public class AttemptLoggerFactoryNew {

  private final Timer attemptsTimer;

  public AttemptLoggerFactoryNew(Timer attemptsTimer) {
    this.attemptsTimer = attemptsTimer;
  }

  public AttemptLoggerNew startTimers(URI uri, ClientRequest request) {
    return startTimers(uri.toString(), request);
  }

  public AttemptLoggerNew startTimers(String uri, ClientRequest request) {
    return new AttemptLoggerNew(attemptsTimer.time(), uri, request);
  }
}
