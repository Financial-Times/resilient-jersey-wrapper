package com.ft.jerseyhttpwrapper.providers;

import com.ft.jerseyhttpwrapper.config.SimpleEndpointConfiguration;
import com.google.common.net.HostAndPort;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;

public class SimpleEndpointConfigurationToHostPortConverterTest {

	@Test
	public void should_convert_simple_config_using_main_port() throws Exception {
		final SimpleEndpointConfiguration configuration = new SimpleEndpointConfiguration("http", "host", 80, 8080);

		final List<HostAndPort> hostAndPorts = new SimpleEndpointConfigurationToHostPortConverter(false).convertAll(Arrays.asList(configuration));

		assertThat(hostAndPorts, equalTo(Arrays.asList(HostAndPort.fromString("host:80"))));
	}

	@Test
	public void should_convert_simple_config_using_admin_port() throws Exception {
		final SimpleEndpointConfiguration configuration = new SimpleEndpointConfiguration("http", "host", 80, 8080);

		final List<HostAndPort> hostAndPorts = new SimpleEndpointConfigurationToHostPortConverter(true).convertAll(Arrays.asList(configuration));

		assertThat(hostAndPorts, equalTo(Arrays.asList(HostAndPort.fromString("host:8080"))));
	}

	@Test
	public void should_convert_and_preserve_order() throws Exception {
		final List<SimpleEndpointConfiguration> configuration =
				Arrays.asList(
						new SimpleEndpointConfiguration("http", "a", 80, 8080),
						new SimpleEndpointConfiguration("http", "c", 82, 8082),
						new SimpleEndpointConfiguration("http", "b", 81, 8081)
				);

		final List<HostAndPort> hostAndPorts =
				new SimpleEndpointConfigurationToHostPortConverter(true).convertAll(configuration);

		assertThat(hostAndPorts, equalTo(Arrays.asList(
								HostAndPort.fromString("a:8080"),
								HostAndPort.fromString("c:8082"),
								HostAndPort.fromString("b:8081")
						)
				)
		);
	}

	@Test
	public void should_deduplicate_hosts() throws Exception {
		final List<SimpleEndpointConfiguration> configuration =
				Arrays.asList(
						new SimpleEndpointConfiguration("http", "a", 80, 8080),
						new SimpleEndpointConfiguration("http", "c", 82, 8082),
						new SimpleEndpointConfiguration("http", "a", 81, 8080)
				);

		final List<HostAndPort> hostAndPorts =
				new SimpleEndpointConfigurationToHostPortConverter(true).convertAll(configuration);

		assertThat(hostAndPorts, equalTo(Arrays.asList(
								HostAndPort.fromString("a:8080"),
								HostAndPort.fromString("c:8082")
						)
				)
		);

	}

}