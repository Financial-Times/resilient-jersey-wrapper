package com.ft.jerseyhttpwrapper;

import java.net.URI;

import com.codahale.metrics.Timer;
import com.sun.jersey.api.client.ClientRequest;

/**
 * AttemptLogger
 *
 * @author Simon.Gibbs
 */
public class AttemptLoggerFactory {

    private final Timer attemptsTimer;

    public AttemptLoggerFactory(Timer attemptsTimer) {
        this.attemptsTimer = attemptsTimer;
    }

    public AttemptLogger startTimers(URI uri, ClientRequest request) {
        return startTimers(uri.toString(), request);
    }

    public AttemptLogger startTimers(String uri, ClientRequest request) {
        return new AttemptLogger(attemptsTimer.time(), uri, request);
    }

}
