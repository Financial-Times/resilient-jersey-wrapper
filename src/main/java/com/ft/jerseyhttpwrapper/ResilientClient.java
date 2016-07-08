package com.ft.jerseyhttpwrapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.ws.rs.core.UriBuilder;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.ft.jerseyhttpwrapper.continuation.ContinuationPolicy;
import com.ft.jerseyhttpwrapper.continuation.ContinuationSession;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.sun.jersey.api.client.ClientHandler;
import com.sun.jersey.api.client.config.ClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ft.jerseyhttpwrapper.providers.HostAndPortProvider;
import com.google.common.base.Preconditions;
import com.google.common.net.HostAndPort;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import org.slf4j.MDC;

public class ResilientClient extends Client {

    /**
     * Maven dumps it's properties into a file when it build the JAR. Turns out you can read it at runtime.
     * Used for version stamping.
     */
    public static final String MAVEN_PROPERTIES = "/META-INF/maven/com.ft.resilient-jersey-wrapper/resilient-jersey-wrapper/pom.properties";

    public static final Logger LOGGER = LoggerFactory.getLogger(ResilientClient.class);
    private static final List<Integer> RECOVERABLE_STATUSES = Arrays.asList(500, 503, 504);
    private static final List<String> NON_IDEMPOTENT_METHODS = Arrays.asList("POST", "PATCH");
    
    private static final String TX_ID = "transaction_id";

    private String shortName;

    private ContinuationPolicy continuationPolicy;

    private Supplier<String> userAgentSupplier = new Supplier<String>() {
        @Override
        public String get() {
            return String.format("Resilient Client (v=%s, sn=%s, %s)", _version, shortName, MDC.get(TX_ID));
        }
    };

    private Supplier<String> txIdSupplier = new Supplier<String>() {
        @Override
        public String get() {
            String txId = MDC.get(TX_ID);
            if (!Strings.isNullOrEmpty(txId)) {
                txId = txId.substring(txId.indexOf('=') + 1);
            }
            return txId;
        }
    };
    
    private String txPropagationHeader;
    
    public ResilientClient(String shortName, ClientHandler root, ClientConfig config, HostAndPortProvider provider, ContinuationPolicy continuationPolicy, boolean retryNonIdempotentMethods, MetricRegistry appMetrics) {
		super(root, config);

        Preconditions.checkNotNull(shortName,"Resilient clients must be named");
        Preconditions.checkNotNull(provider,"host and port provider is mandatory");

		this.shortName = shortName;
		this.provider = provider;
        this.continuationPolicy = continuationPolicy;
		this.retryNonIdempotentMethods = retryNonIdempotentMethods;

	    this.requests = appMetrics.timer(MetricRegistry.name(ResilientClient.class, "requests", shortName));
	    this.attemptCounts = appMetrics.histogram(MetricRegistry.name(ResilientClient.class, "attemptCount", shortName));

        attemptLoggerFactory = new AttemptLoggerFactory(appMetrics.timer(MetricRegistry.name(ResilientClient.class, "attempts", shortName)));

	}

	private final HostAndPortProvider provider;
	private final boolean retryNonIdempotentMethods;

    private AttemptLoggerFactory attemptLoggerFactory;
    private final Timer requests;
    private final Histogram attemptCounts;

	//TODO:
	// TimeOut the request - helps in case the host list is long and most of them are failing slowly
	@Override
	public ClientResponse handle(final ClientRequest originalRequest) throws ClientHandlerException {
	  URI requestedUri = originalRequest.getURI();
	  if ("https".equals(requestedUri.getScheme())) {
	    // minimal support for https (don't fiddle with hostname) - just pass-through to the default impl
        LOGGER.info("[REQUEST STARTED] short_name={} uri={}", shortName, requestedUri);
        try {
          return super.handle(originalRequest);
        }
        finally {
          LOGGER.info("[REQUEST FINISHED] short_name={} uri={}", shortName, requestedUri);
        }
	  }
	  
        Timer.Context requestsTimer = requests.time();
        int attemptCount = 0;
		int failedAttemptCount = 0;

        ClientHandlerException lastClientHandlerException = null;
        ClientResponse lastResponse = null;

        HostAndPort suppliedAddress = HostAndPort.fromString(requestedUri.getAuthority());

        // pass implicit ports as implicit for validation purpsoses
        if(!provider.supports(suppliedAddress)) {
            throw new IllegalArgumentException("Unknown host and port " + suppliedAddress.toString());
        }

        // fill out the port as port 80 for use in practice
        suppliedAddress = suppliedAddress.withDefaultPort(80);

        try {
			LOGGER.info("[REQUEST STARTED] short_name={}", shortName);

            ContinuationSession session = continuationPolicy.startSession(suppliedAddress, provider);

            while(session.shouldContinue()) {

                HostAndPort hostAndPort = session.nextHost();

                if(Strings.isNullOrEmpty(hostAndPort.getHostText())) {
                    // never been thrown, but helpful in proving/falsifying some theories in the debugger. SJG Jan 2015
                    throw new IllegalStateException("ContinuationSession produced null or empty endpoint host");
                }

                URI attemptUri = UriBuilder.fromUri(requestedUri)
                        .host(hostAndPort.getHostText())
                        .port(hostAndPort.getPortOrDefault(8080))
                        .build();

                ClientRequest clonedRequest = originalRequest.clone();
                clonedRequest.setURI(attemptUri);
                clonedRequest.getHeaders().putSingle("User-Agent", userAgentSupplier.get());
                
                maybePropagateTransactionId(clonedRequest);
                
                String requestOutcome = "unknown";

                AttemptLogger attempt = attemptLoggerFactory.startTimers(attemptUri, clonedRequest);

                if (lastResponse != null) {
                    try {
                        lastResponse.getEntityInputStream().close();
                    } catch (IOException e) {
                        LOGGER.warn("Error occurred while trying to prevent connections from staying open. Could not close response stream.", e);
                    }
                }

                ClientResponse currentResponse=null;

                try {
                    attemptCount++;
                    currentResponse = super.handle(clonedRequest);
                    lastResponse = currentResponse;

                    if(RECOVERABLE_STATUSES.contains(lastResponse.getStatus())) {

                        // WARNING: recoverable events and failures are not quite the same thing
                        session.handleFailedHost(hostAndPort);
                        failedAttemptCount++;

                        continue;
                    }
                    return lastResponse;

                } catch(ClientHandlerException e) {
                    Throwable cause = e;
                    // loop in case one ClientHandlerException has wrapped another
                    do {
                        cause = cause.getCause();
                    } while (cause instanceof ClientHandlerException);
                    
                    failedAttemptCount++;
                    lastClientHandlerException = e;

                    if (cause instanceof IOException) {
                        LOGGER.warn("Error communicating with server.",e);
                        session.handleFailedHost(hostAndPort);

                        if(isRemoteStateUncertain(cause) && !isIdempotentMethod(originalRequest.getMethod()) && !retryNonIdempotentMethods) {
                            LOGGER.warn("Not retrying interrupted {}: not idempotent", originalRequest.getMethod());
                            throw e;
                        }

                    } else {
                        LOGGER.warn("Unexpected error communicating with server.", e);
                        throw e;
                    }

                } finally {


                    if(currentResponse!=null) {
                        requestOutcome=Integer.toString(currentResponse.getStatus());
                    } else if(lastClientHandlerException!=null) {
                        requestOutcome="Exception";
                    }

                    attempt.stop(requestOutcome);
                }
            }

        } finally {
            requestsTimer.stop();
            attemptCounts.update(attemptCount);

            String outcome = "unknown";
            if(lastResponse!=null) {
                outcome = Integer.toString(lastResponse.getStatus());
            } else if(lastClientHandlerException!=null) {
                outcome="Exception";
            }

            String finishedMessage = String.format("[REQUEST FINISHED] short_name=%s, outcome=%s, total_attempts=%d, failed_attempts=%d", shortName, outcome, attemptCount, failedAttemptCount);

            if(attemptCount==0) {
                LOGGER.error(finishedMessage);
            } else {
                LOGGER.info(finishedMessage);
            }
        }

        if(lastResponse!=null) {
            return lastResponse;
        }

		throw lastClientHandlerException;

	}

	private boolean isRemoteStateUncertain(final Throwable cause) {
		return cause instanceof SocketTimeoutException && cause.getMessage().contains("Read");
	}

    private boolean isIdempotentMethod(final String method) {
        return !NON_IDEMPOTENT_METHODS.contains(method);
    }
    
    private void maybePropagateTransactionId(ClientRequest request) {
        if (!Strings.isNullOrEmpty(txPropagationHeader)) {
            String txId = txIdSupplier.get();
            if (!Strings.isNullOrEmpty(txId)) {
                request.getHeaders().putSingle(txPropagationHeader, txId);
            }
        }
    }
    
    public String getShortName() {
        return shortName;
    }

    public void setUserAgentSupplier(Supplier<String> userAgentSupplier) {
        this.userAgentSupplier = userAgentSupplier;
    }
    
    public void setTransactionHeader(String header) {
        txPropagationHeader = header;
    }
    
    public String getTransactionHeader() {
        return txPropagationHeader;
    }
    
    public void setTransactionIdSupplier(Supplier<String> transactionIdSupplier) {
        this.txIdSupplier = transactionIdSupplier;
    }
    
    private static String _version;

    static {

        InputStream mavenProperties = ResilientClient.class.getResourceAsStream(MAVEN_PROPERTIES);
        if(mavenProperties==null) {
            _version = "LOCAL";
        } else {
            Properties properties = new Properties();
            try {
                properties.load(mavenProperties);
            } catch (Exception e) {
                throw new IllegalStateException("Unable to access maven properties from " + MAVEN_PROPERTIES,e);
            }

            _version = properties.getProperty("version");
        }

    }

}
