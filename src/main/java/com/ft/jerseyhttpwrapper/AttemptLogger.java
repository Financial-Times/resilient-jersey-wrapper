package com.ft.jerseyhttpwrapper;

import com.codahale.metrics.Timer;
import com.ft.membership.logging.Operation;

/**
 * AttemptLogger
 *
 * @author Simon.Gibbs
 */
public class AttemptLogger {

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

        String outcome = null;
        if (status > 499 && status <= 599) {
            outcome = Integer.toString(status);
        } else if (status == 0) {
            outcome = "Exception";
        }

        if (outcome != null) {
            operationJson.logIntermediate()
                    .yielding("msg", "ATTEMPT FINISHED")
                    .yielding("uri", attemptUri)
                    .yielding("responsetime", timeTakenMillis)
                    .yielding("request_entity", entity)
                    .yielding("outcome", outcome)
                    .logError();
        } else {
        	 operationJson.logIntermediate()
             	.yielding("msg", "ATTEMPT FINISHED")
             	.yielding("uri", attemptUri)
             	.yielding("responsetime", timeTakenMillis)
             	.yielding("request_entity", entity)
             	.logDebug();
        }
    }

}
