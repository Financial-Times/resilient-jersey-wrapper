package com.ft.jerseyhttpwrapper;

import com.codahale.metrics.Timer;
import com.ft.membership.logging.IntermediateYield;
import com.ft.membership.logging.Operation;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;

/**
 * AttemptLogger
 *
 * @author Simon.Gibbs
 */
public class AttemptLogger {

    private final String attemptUri;
    private final Timer.Context attemptTimer;
    private ClientRequest request;
    private final long startMillis;


    public AttemptLogger(Timer.Context attemptTimer, String uri, ClientRequest request) {
        this.attemptTimer = attemptTimer;
        this.request = request;
        startMillis = System.currentTimeMillis();
        this.attemptUri = uri;
    }


    public void stop(ResilientClient client, ClientResponse response) {
        long endTime = System.currentTimeMillis();
        attemptTimer.stop();
        long timeTakenMillis = (endTime - startMillis);

        final Operation operationJson = Operation.operation("stop")
                .jsonLayout().initiate(this);
        final int status = response != null ? response.getStatus() : 0;
        final int responseLength = response != null ? response.getLength() : 0;

        String outcome = null;
        if (status > 499 && status <= 599) {
            outcome = Integer.toString(status);
        } else if (status == 0) {
            outcome = "Exception";
        }

        IntermediateYield yield = operationJson.logIntermediate()
        		.yielding("msg", "ATTEMPT FINISHED")
        		.yielding("transaction_id", client.getTxIdSupplier().get())
        		.yielding("responsetime", timeTakenMillis)
        		.yielding("protocol", client.getProtocol())
        		.yielding("uri", attemptUri)
        		.yielding("path", request.getURI().getPath())
        		.yielding("method", request.getMethod())
        		.yielding("status", status)
        		.yielding("content_type", request.getHeaders().getFirst("Content-Type"))
        		.yielding("size", responseLength)
        		.yielding("userAgent", request.getHeaders().get("User-Agent"))
        		.yielding("exception_was_thrown", (outcome == null)); 
        
        if (outcome != null) {
            yield.logError();
        } else {
        	yield.logDebug();
        }
    }

}
