package com.ft.jerseyhttpwrapper.config;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.setup.Environment;
import io.dropwizard.util.Duration;

import javax.validation.Validator;
import java.util.concurrent.ExecutorService;

/**
 * DW07xClientEnvironment
 *
 * @author Simon
 */
public class DW07xClientEnvironment implements ClientEnvironment {

	private final Environment environment;

	public DW07xClientEnvironment(Environment environment) {
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

		String generatedTemplate = String.format("resilient-client-%s-%s",shortName,"%d");

		return environment.lifecycle().executorService(generatedTemplate)
				.minThreads(minThreads)
				.maxThreads(maxThreads)
				.keepAliveTime(Duration.seconds(60))
				.build();
	}

	@Override
	public MetricRegistry getMetricsRegistry() {
		return environment.metrics();
	}
}
