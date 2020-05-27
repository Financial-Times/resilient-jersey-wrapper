package com.ft.jerseyhttpwrapper.config;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.ExecutorService;
import javax.validation.Validator;

/**
 * ClientEnvironment
 *
 * @author Simon
 */
public interface ClientEnvironment {

  ObjectMapper createObjectMapper();

  ExecutorService createExecutorService(String shortName, int minThreads, int maxThreads);

  Validator getValidator();

  MetricRegistry getMetricsRegistry();
}
