package com.ft.jerseyhttpwrapper;

import static java.lang.String.valueOf;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.codahale.metrics.Timer;
import com.ft.jerseyhttpwrapper.config.validation.DeniedRegexpsValidator;
import com.ft.membership.logging.IntermediateYield;
import com.ft.membership.logging.Operation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.ws.rs.core.MultivaluedMap;
import org.glassfish.jersey.client.ClientRequest;
import org.glassfish.jersey.client.ClientResponse;

/**
 * AttemptLogger
 *
 * @author Georgi Kazakov
 */
public class AttemptLoggerNew {

  private final String attemptUri;
  private final Timer.Context attemptTimer;
  private ClientRequest request;
  private final long startMillis;

  private DeniedRegexpsValidator deniedRegexpsValidator;

  public AttemptLoggerNew(Timer.Context attemptTimer, String uri, ClientRequest request) {
    this.attemptTimer = attemptTimer;
    this.request = request;
    startMillis = System.currentTimeMillis();
    this.attemptUri = uri;
  }

  public void stop(ResilientClientNew client, ClientResponse response) {
    long endTime = System.currentTimeMillis();
    attemptTimer.stop();
    long timeTakenMillis = (endTime - startMillis);

    final Operation operationJson = Operation.operation("stop").jsonLayout().initiate(this);
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
    if (nonNull(request.getUri())) {
      withField(yield, "path", request.getUri().getPath());
    }
    withField(yield, "method", request.getMethod());
    withField(yield, "status", status);
    extractAllAllowedHeaders(yield, response);
    withField(yield, "size", responseLength);
    withField(yield, "exception_was_thrown", (outcome != null));

    if (outcome != null) {
      yield.logError();
    } else {
      yield.logDebug();
    }
  }

  private void extractAllAllowedHeaders(IntermediateYield yield, ClientResponse response) {
    // construct validator for denied regexp patterns
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    Validator validator = factory.getValidator();
    // get all presented request/response headers and remove denied ones
    extractAllowedRequestHeaders(yield, validator);
    extractAllowedResponseHeaders(yield, validator, response);

    if (nonNull(request.getHeaders())) {
      withField(yield, "content_type", request.getHeaders().getFirst("Content-Type"));
      withField(yield, "userAgent", request.getHeaders().get("User-Agent"));
    }
  }

  private void extractAllowedRequestHeaders(IntermediateYield yield, Validator validator) {
    Map<String, String> requestHeaders = getRequestHeaders();
    deniedRegexpsValidator = new DeniedRegexpsValidator(requestHeaders.keySet());

    Set<ConstraintViolation<DeniedRegexpsValidator>> result =
        validator.validate(deniedRegexpsValidator);

    Iterator<ConstraintViolation<DeniedRegexpsValidator>> iter = result.iterator();
    Set<String> deniedRequestHeaders = new HashSet<String>();
    while (iter.hasNext()) {
      String currentDeniedHeader = iter.next().getMessage();
      if (requestHeaders.keySet().contains(currentDeniedHeader)) {
        deniedRequestHeaders.add(currentDeniedHeader);
      }
    }

    requestHeaders.keySet().removeAll(deniedRequestHeaders);

    if (!requestHeaders.isEmpty()) {
      yield.yielding("requestHeaders", requestHeaders);
    }
  }

  private void extractAllowedResponseHeaders(
      IntermediateYield yield, Validator validator, ClientResponse response) {
    Map<String, String> responseHeaders = getResponseHeaders(response);
    if (isNull(responseHeaders)) {
      return;
    }
    deniedRegexpsValidator = new DeniedRegexpsValidator(responseHeaders.keySet());

    Set<ConstraintViolation<DeniedRegexpsValidator>> result =
        validator.validate(deniedRegexpsValidator);

    Iterator<ConstraintViolation<DeniedRegexpsValidator>> iter = result.iterator();
    List<String> deniedResponseHeaders = new ArrayList<String>();
    while (iter.hasNext()) {
      String currentDeniedHeader = iter.next().getMessage();
      if (responseHeaders.keySet().contains(currentDeniedHeader)) {
        deniedResponseHeaders.add(currentDeniedHeader);
      }
    }

    responseHeaders.keySet().removeAll(deniedResponseHeaders);

    if (!responseHeaders.isEmpty()) {
      yield.yielding("responsetHeaders", responseHeaders);
    }
  }

  private Map<String, String> getRequestHeaders() {
    MultivaluedMap<String, Object> requestHeaders = request.getHeaders();
    Map<String, String> headers = new HashMap<String, String>();
    List<String> headerValues = new ArrayList<String>();
    if (nonNull(requestHeaders)) {
      for (String headerName : requestHeaders.keySet()) {
        for (Object valueObject : requestHeaders.get(headerName)) {
          headerValues.add(valueObject.toString());
        }
        headers.put(headerName, headerValues.toString());
      }
    }

    return headers;
  }

  private Map<String, String> getResponseHeaders(ClientResponse response) {
    if (isNull(response)) {
      return null;
    }
    MultivaluedMap<String, String> responseHeaders = response.getHeaders();
    Map<String, String> headers = new HashMap<String, String>();
    List<String> headerValues = new ArrayList<String>();
    if (nonNull(responseHeaders)) {
      for (String headerName : responseHeaders.keySet()) {
        for (String valueObject : responseHeaders.get(headerName)) {
          headerValues.add(valueObject.toString());
        }
        headers.put(headerName, headerValues.toString());
      }
    }

    return headers;
  }

  private void withField(IntermediateYield yield, String key, Object value) {
    if (isBlank(key) || isNull(value) || isBlank(valueOf(value))) {
      return;
    }

    yield.yielding(key, value);
  }
}
