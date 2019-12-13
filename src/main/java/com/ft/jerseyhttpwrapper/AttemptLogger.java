package com.ft.jerseyhttpwrapper;

import static java.lang.String.valueOf;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang.StringUtils.isBlank;

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

        IntermediateYield yield = operationJson.logIntermediate();

        withField(yield, "msg", "ATTEMPT FINISHED");
        withField(yield, "transaction_id", client.getTxIdSupplier().get());
        withField(yield, "responsetime", timeTakenMillis);
        withField(yield, "protocol", client.getProtocol());
        withField(yield, "uri", attemptUri);
        if (nonNull(request.getURI())) {
            withField(yield, "path", request.getURI().getPath());
        }
        withField(yield, "method", request.getMethod());
        withField(yield, "status", status);
        if (nonNull(request.getHeaders())) {
            withField(yield, "content_type", request.getHeaders().getFirst("Content-Type"));
            withField(yield, "userAgent", request.getHeaders().get("User-Agent"));
        }    
        withField(yield, "size", responseLength);
        withField(yield, "exception_was_thrown", (outcome != null));
        
        if (outcome != null) {
            yield.logError();
        } else {
            yield.logDebug();
        }
    }
    
    private void withField(IntermediateYield yield, String key, Object value) {
        if (isBlank(key) || isNull(value) || isBlank(valueOf(value))) {
            return;
        }
        
        yield.yielding(key, value);
    }

}
