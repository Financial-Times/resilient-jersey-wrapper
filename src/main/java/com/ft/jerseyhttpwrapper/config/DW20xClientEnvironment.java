package com.ft.jerseyhttpwrapper.config;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.setup.Environment;
import io.dropwizard.util.Duration;
import java.util.concurrent.ExecutorService;
import javax.validation.Validator;

/**
 * DW20xClientEnvironment
 *
 * @author Georgi Kazakov
 */
public class DW20xClientEnvironment implements ClientEnvironment {

  private final Environment environment;

  public DW20xClientEnvironment(Environment environment) {
    this.environment = environment;
  }

  @Override
  public ObjectMapper createObjectMapper() {
    return environment.getObjectMapper();
  }

  @Override
  public Validator getValidator() {
    return environment.getValidator();
  }

  @Override
  public ExecutorService createExecutorService(String shortName, int minThreads, int maxThreads) {

    String generatedTemplate = String.format("resilient-client-%s-%s", shortName, "%d");

    return environment
        .lifecycle()
        .executorService(generatedTemplate)
        .minThreads(minThreads)
        .maxThreads(maxThreads)
        .keepAliveTime(Duration.seconds(60))
        .allowCoreThreadTimeOut(true)
        .build();
  }

  @Override
  public MetricRegistry getMetricsRegistry() {
    return environment.metrics();
  }
}
