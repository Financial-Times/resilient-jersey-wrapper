package com.ft.jerseyhttpwrapper;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.ft.jerseyhttpwrapper.continuation.ContinuationPolicy;
import com.ft.jerseyhttpwrapper.continuation.ContinuationSession;
import com.ft.jerseyhttpwrapper.providers.HostAndPortProvider;
import com.ft.membership.logging.Operation;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.net.HostAndPort;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Future;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.UriBuilder;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientRequest;
import org.glassfish.jersey.client.ClientResponse;
import org.glassfish.jersey.client.JerseyClient;
import org.glassfish.jersey.client.spi.AsyncConnectorCallback;
import org.glassfish.jersey.client.spi.Connector;
import org.slf4j.MDC;

public class ResilientClientNew extends JerseyClient implements Connector {

  /**
   * Maven dumps it's properties into a file when it build the JAR. Turns out you can read it at
   * runtime. Used for version stamping.
   */
  public static final String MAVEN_PROPERTIES =
      "/META-INF/maven/com.ft.resilient-jersey-wrapper/resilient-jersey-wrapper/pom.properties";

  private static final List<Integer> RECOVERABLE_STATUSES = Arrays.asList(500, 503, 504);
  private static final List<String> NON_IDEMPOTENT_METHODS = Arrays.asList("POST", "PATCH");

  private static final String TX_ID = "transaction_id";

  private String shortName;

  private ContinuationPolicy continuationPolicy;

  private String protocol;

  private Supplier<String> userAgentSupplier =
      new Supplier<String>() {
        @Override
        public String get() {
          return String.format(
              "Resilient Client (v=%s, sn=%s, %s)", _version, shortName, MDC.get(TX_ID));
        }
      };

  private Supplier<String> txIdSupplier =
      new Supplier<String>() {
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

  public ResilientClientNew(
      String shortName,
      ClientConfig config,
      HostAndPortProvider provider,
      ContinuationPolicy continuationPolicy,
      boolean retryNonIdempotentMethods,
      MetricRegistry appMetrics) {
    super(config, (SSLContext) null, (HostnameVerifier) null);
    Preconditions.checkNotNull(shortName, "Resilient clients must be named");
    Preconditions.checkNotNull(provider, "host and port provider is mandatory");

    this.shortName = shortName;
    this.provider = provider;
    this.continuationPolicy = continuationPolicy;
    this.retryNonIdempotentMethods = retryNonIdempotentMethods;

    this.requests =
        appMetrics.timer(MetricRegistry.name(ResilientClientNew.class, "requests", shortName));
    this.attemptCounts =
        appMetrics.histogram(
            MetricRegistry.name(ResilientClientNew.class, "attemptCount", shortName));

    attemptLoggerFactory =
        new AttemptLoggerFactoryNew(
            appMetrics.timer(MetricRegistry.name(ResilientClientNew.class, "attempts", shortName)));
  }

  private final HostAndPortProvider provider;
  private final boolean retryNonIdempotentMethods;

  private AttemptLoggerFactoryNew attemptLoggerFactory;
  private final Timer requests;
  private final Histogram attemptCounts;

  @Override
  public ClientResponse apply(final ClientRequest originalRequest) {
    URI requestedUri = originalRequest.getUri();

    Timer.Context requestsTimer = requests.time();
    int attemptCount = 0;
    int failedAttemptCount = 0;

    ProcessingException lastProcessingException = null;
    ClientResponse lastResponse = null;

    HostAndPort suppliedAddress = HostAndPort.fromString(requestedUri.getAuthority());

    // pass implicit ports as implicit for validation purpsoses
    if (!provider.supports(suppliedAddress)) {
      throw new IllegalArgumentException("Unknown host and port " + suppliedAddress.toString());
    }

    // fill out the port as port 80 for use in practice
    suppliedAddress = suppliedAddress.withDefaultPort(80);

    final Operation operationJson = Operation.operation("handle").jsonLayout().initiate(this);

    try {
      ContinuationSession session = continuationPolicy.startSession(suppliedAddress, provider);

      while (session.shouldContinue()) {

        HostAndPort hostAndPort = session.nextHost();

        if (Strings.isNullOrEmpty(hostAndPort.getHost())) {
          // never been thrown, but helpful in proving/falsifying some theories in the debugger. SJG
          // Jan 2015
          throw new IllegalStateException(
              "ContinuationSession produced null or empty endpoint host");
        }

        URI attemptUri =
            UriBuilder.fromUri(requestedUri)
                .host(hostAndPort.getHost())
                .port(hostAndPort.getPortOrDefault(8080))
                .build();

        ClientRequest clonedRequest = new ClientRequest(originalRequest);
        clonedRequest.setUri(attemptUri);
        clonedRequest.getHeaders().putSingle("User-Agent", userAgentSupplier.get());

        maybePropagateTransactionId(clonedRequest);

        AttemptLoggerNew attempt = attemptLoggerFactory.startTimers(attemptUri, clonedRequest);

        if (lastResponse != null) {
          try {
            lastResponse.getEntityStream().close();
          } catch (IOException e) {
            operationJson
                .wasFailure()
                .withMessage(e)
                .withDetail(
                    "msg",
                    "Error occurred while trying to prevent connections from staying open. Could not close response stream.")
                .logWarn(e);
          }
        }

        ClientResponse currentResponse = null;

        try {
          attemptCount++;
          currentResponse = this.apply(clonedRequest);
          lastResponse = currentResponse;

          if (RECOVERABLE_STATUSES.contains(lastResponse.getStatus())) {
            // WARNING: recoverable events and failures are not quite the same thing
            session.handleFailedHost(hostAndPort);
            failedAttemptCount++;

            continue;
          }
          return lastResponse;

        } catch (ProcessingException e) {
          Throwable cause = e;
          // loop in case one ClientHandlerException has wrapped another
          do {
            cause = cause.getCause();
          } while (cause instanceof ProcessingException);

          failedAttemptCount++;
          lastProcessingException = e;

          if (cause instanceof IOException) {
            operationJson
                .wasFailure()
                .withMessage(e)
                .withDetail("msg", "Error communicating with server.")
                .logWarn(e);

            session.handleFailedHost(hostAndPort);

            if (isRemoteStateUncertain(cause)
                && !isIdempotentMethod(originalRequest.getMethod())
                && !retryNonIdempotentMethods) {
              operationJson
                  .logIntermediate()
                  .yielding(
                      "msg",
                      "Not retrying interrupted " + originalRequest.getMethod() + " not idempotent")
                  .logWarn(e);

              throw e;
            }

          } else {
            operationJson
                .logIntermediate()
                .yielding("msg", "Unexpected error communicating with server.")
                .logWarn(e);

            throw e;
          }

        } finally {

          if (currentResponse != null) {
            attempt.stop(this, currentResponse);
          } else if (lastProcessingException != null) {
            attempt.stop(this, currentResponse);
          }
        }
      }

    } finally {
      requestsTimer.stop();
      attemptCounts.update(attemptCount);

      String outcome = "unknown";
      int status = 0;
      if (lastResponse != null) {
        status = lastResponse.getStatus();
        outcome = Integer.toString(status);
      } else if (lastProcessingException != null) {
        outcome = "Exception";
      }

      if (attemptCount == 0 || (status > 499 && status <= 599)) {
        operationJson
            .logIntermediate()
            .yielding("msg", "[REQUEST FINISHED]")
            .yielding("short_name", shortName)
            .yielding("outcome", outcome)
            .yielding("total_attempts", attemptCount)
            .yielding("failed_attempts", failedAttemptCount)
            .logError();
      }
    }

    if (lastResponse != null) {
      return lastResponse;
    }

    throw lastProcessingException;
  }

  @Override
  public Future<?> apply(ClientRequest request, AsyncConnectorCallback callback) {
    // Async execution not called at all
    return null;
  }

  @Override
  public String getName() {
    return this.shortName;
  }

  @Override
  public void close() {
    super.close();
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

  public Supplier<String> getTxIdSupplier() {
    return txIdSupplier;
  }

  public void setTransactionIdSupplier(Supplier<String> transactionIdSupplier) {
    this.txIdSupplier = transactionIdSupplier;
  }

  public String getProtocol() {
    return protocol;
  }

  public void setProtocol(String protocol) {
    this.protocol = protocol;
  }

  private static String _version;

  static {
    InputStream mavenProperties = ResilientClientNew.class.getResourceAsStream(MAVEN_PROPERTIES);
    if (mavenProperties == null) {
      _version = "LOCAL";
    } else {
      Properties properties = new Properties();
      try {
        properties.load(mavenProperties);
      } catch (Exception e) {
        throw new IllegalStateException(
            "Unable to access maven properties from " + MAVEN_PROPERTIES, e);
      }

      _version = properties.getProperty("version");
    }
  }
}
