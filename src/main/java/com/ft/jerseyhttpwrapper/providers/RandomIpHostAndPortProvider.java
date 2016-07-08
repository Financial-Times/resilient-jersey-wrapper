package com.ft.jerseyhttpwrapper.providers;

import com.google.common.net.HostAndPort;

import java.util.List;

public class RandomIpHostAndPortProvider extends RandomHostAndPortProvider {
	private final HostAndPortIpResolver resolver;

	public RandomIpHostAndPortProvider(final List<HostAndPort> hostNames, final HostAndPortIpResolver resolver) {
		super(hostNames);
		this.resolver = resolver;
	}

	@Override
	protected List<HostAndPort> getHostNames() {
		return resolver.resolveAll(super.getHostNames());
	}
}
