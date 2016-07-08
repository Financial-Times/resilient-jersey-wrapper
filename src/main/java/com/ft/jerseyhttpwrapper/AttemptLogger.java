package com.ft.jerseyhttpwrapper;

import com.codahale.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AttemptLogger
 *
 * @author Simon.Gibbs
 */
public class AttemptLogger {

    public static final Logger LOGGER = LoggerFactory.getLogger(AttemptLogger.class);

    private final String attemptUri;
    private final Timer.Context attemptTimer;
    private Object entity;
    private final long startMillis;


    public AttemptLogger(Timer.Context attemptTimer, String uri, Object entity) {
        this.attemptTimer = attemptTimer;
        this.entity = entity;
        startMillis = System.currentTimeMillis();
        this.attemptUri = uri;
    }


    public void stop(String outcome) {
        long endTime = System.currentTimeMillis();
        attemptTimer.stop();
        long timeTakenMillis = (endTime - startMillis);

        LOGGER.debug("[ATTEMPT FINISHED] request_uri={}, request_entity={}", attemptUri, entity);
        LOGGER.info("[ATTEMPT FINISHED] request_uri={}, outcome={}, time_ms={}", attemptUri, outcome, timeTakenMillis);

    }

}
