package com.ft.jerseyhttpwrapper.providers;

import static com.ft.membership.logging.Operation.operation;

import com.ft.membership.logging.Operation;
import com.google.common.net.HostAndPort;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * resolves the IP Addresses of a list of HostAndPort, returning a unique list of IP-Addressed HostAndPort.
 */
public class HostAndPortIpResolver {

	private static final int NODES_PER_HOSTNAME_GUESS = 6;

	private final HostToIpMapper hostMapper;
	
	public HostAndPortIpResolver(HostToIpMapper hostMapper) {
		this.hostMapper = checkNotNull(hostMapper);
	}

	public List<HostAndPort> resolveAll(List<HostAndPort> hostAndPorts) {
		Set<HostAndPort> resolvedHostAndPorts = new LinkedHashSet<>(hostAndPorts.size() * NODES_PER_HOSTNAME_GUESS);
		for (HostAndPort hostAndPort : hostAndPorts) {
			resolvedHostAndPorts.addAll(resolve(hostAndPort));
		}
		return new ArrayList<>(resolvedHostAndPorts);
	}

	public List<HostAndPort> resolve(HostAndPort hostAndPort) {
		final Operation resultOperation = operation("resolve").with("argument", hostAndPort.getHostText()).jsonLayout().initiate(this);
		try {
			final InetAddress[] inetAddresses = hostMapper.mapToIps(hostAndPort.getHostText());
			return mapAddressesAcrossPorts(hostAndPort, inetAddresses);

		} catch(UnknownHostException e) {
			resultOperation.logIntermediate().yielding("msg", "Unable to resolve host "  + hostAndPort.getHostText()).logWarn();
			return Collections.singletonList(hostAndPort);
		}

	}

	private List<HostAndPort> mapAddressesAcrossPorts(final HostAndPort hostAndPort, final InetAddress[] inetAddresses) {
		List<HostAndPort> hostAndPorts = new ArrayList<>(inetAddresses.length);
		for (InetAddress inetAddress : inetAddresses) {
			hostAndPorts.add(HostAndPort.fromParts(inetAddress.getHostAddress(), hostAndPort.getPort()));
		}
		return hostAndPorts;
	}

}
