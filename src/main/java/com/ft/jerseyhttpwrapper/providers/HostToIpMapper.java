package com.ft.jerseyhttpwrapper.providers;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * maps hostname to array of IPs; used to allow mocking of IP Address lookup.
 */
public class HostToIpMapper {

	public InetAddress[] mapToIps(String hostname) throws UnknownHostException {
		return InetAddress.getAllByName(hostname);
	}

}
