package com.ft.jerseyhttpwrapper.config;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.validation.Validation;
import javax.validation.Validator;
import org.hibernate.validator.HibernateValidator;

/**
 * DummyClientEnvironment
 *
 * @author Simon
 */
public class DummyClientEnvironment implements ClientEnvironment {

  private ObjectMapper objectMapper;
  private ExecutorService executorService;

  public DummyClientEnvironment(ObjectMapper objectMapper, ExecutorService executorService) {
    this.objectMapper = objectMapper;
    this.executorService = executorService;
  }

  public static ClientEnvironment inTesting() {
    return new DummyClientEnvironment(new ObjectMapper(), Executors.newFixedThreadPool(8));
  }

  @Override
  public ObjectMapper createObjectMapper() {
    return objectMapper;
  }

  @Override
  public ExecutorService createExecutorService(String shortName, int minThreads, int maxThreads) {
    return executorService;
  }

  @Override
  public Validator getValidator() {
    return Validation.byProvider(HibernateValidator.class)
        .configure()
        .buildValidatorFactory()
        .getValidator();
  }

  @Override
  public MetricRegistry getMetricsRegistry() {
    return new MetricRegistry();
  }
}
