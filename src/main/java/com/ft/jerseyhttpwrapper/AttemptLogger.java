package com.ft.jerseyhttpwrapper;

import com.codahale.metrics.Timer;
import com.ft.membership.logging.Operation;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

/**
 * AttemptLogger
 *
 * @author Simon.Gibbs
 */
public class AttemptLogger {

//    public static final Logger LOGGER = LoggerFactory.getLogger(AttemptLogger.class);

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


    public void stop(int status) {
        long endTime = System.currentTimeMillis();
        attemptTimer.stop();
        long timeTakenMillis = (endTime - startMillis);

        final Operation operationJson = Operation.operation("stop")
                .jsonLayout().initiate(this);

        operationJson.logIntermediate()
                .yielding("attempt_state", "ATTEMPT FINISHED")
                .yielding("request_uri", attemptUri)
                .yielding("request_entity", entity)
                .log();

        // LOGGER.debug("[ATTEMPT FINISHED] request_uri={}, request_entity={}", attemptUri, entity);

        String outcome = null;
        if (status > 499 && status <= 599) {
            outcome = Integer.toString(status);
        } else if (status == 0) {
            outcome = "Exception";
        }

        if (outcome != null) {
            // LOGGER.error("[ATTEMPT FINISHED] request_uri={}, outcome={}, time_ms={}", attemptUri, outcome, timeTakenMillis);

            operationJson.logIntermediate()
                    .yielding("attempt_state", "ATTEMPT FINISHED")
                    .yielding("request_uri", attemptUri)
                    .yielding("outcome", outcome)
                    .yielding("time_ms", timeTakenMillis)
                    .logError();
        }
    }

}
