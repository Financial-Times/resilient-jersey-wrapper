package com.ft.jerseyhttpwrapper.providers;

import com.ft.jerseyhttpwrapper.config.SimpleEndpointConfiguration;
import com.google.common.collect.Lists;
import com.google.common.net.HostAndPort;

import java.util.LinkedHashSet;
import java.util.List;

public class SimpleEndpointConfigurationToHostPortConverter {

	private final boolean useAdminPorts;

	public SimpleEndpointConfigurationToHostPortConverter(boolean useAdminPorts) {
		this.useAdminPorts = useAdminPorts;
	}

	public List<HostAndPort> convertAll(List<SimpleEndpointConfiguration> configurations) {
		if(configurations==null || configurations.size()==0) {
			return null;
		}

		// exploit the de-duping in LinkedHashSet
		LinkedHashSet<HostAndPort> nodes = new LinkedHashSet<>(configurations.size());
		for(SimpleEndpointConfiguration endpointConfiguration : configurations) {
			nodes.add(convert(endpointConfiguration));
		}

		return Lists.newArrayList(nodes);
	}

	private HostAndPort convert(final SimpleEndpointConfiguration endpointConfiguration) {
		return HostAndPort.fromParts(endpointConfiguration.getHost(), useAdminPorts ? endpointConfiguration.getAdminPort() : endpointConfiguration.getPort());
	}
}
