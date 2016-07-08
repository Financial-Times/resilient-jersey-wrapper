package com.ft.jerseyhttpwrapper.config;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;

import javax.validation.Validator;
import java.util.concurrent.ExecutorService;

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
