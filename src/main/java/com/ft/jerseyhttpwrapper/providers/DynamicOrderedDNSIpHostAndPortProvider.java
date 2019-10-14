package com.ft.jerseyhttpwrapper.providers;

import com.ft.membership.logging.Operation;
import com.google.common.net.HostAndPort;

import java.util.Iterator;

/**
 * Fails over across the set of IPs returned by DNS, without modifying the order
 * of IPs.
 *
 * @author Simon.Gibbs
 */
public class DynamicOrderedDNSIpHostAndPortProvider implements HostAndPortProvider {

    private HostAndPortIpResolver hostAndPortIpResolver;

    public DynamicOrderedDNSIpHostAndPortProvider(HostAndPortIpResolver hostAndPortIpResolver) {
        this.hostAndPortIpResolver = hostAndPortIpResolver;
    }

    @Override
    public Iterator<HostAndPort> iterator(HostAndPort suppliedAddress) {
        return hostAndPortIpResolver.resolve(suppliedAddress).iterator();
    }

    @Override
    public void handleFailedHost(HostAndPort hostAndPort) {
		final Operation operationJson = Operation.operation("handleFailedHost")
				.jsonLayout().initiate(this);
        operationJson.logIntermediate()
        	.yielding("msg", "failed to respond correctly " + hostAndPort.getHost())
        	.logInfo();
    }

    /**
     * Will return true, as there is no fixed set of routes for a dynamic provider.
     * @param hostAndPort
     * @return true
     */
    @Override
    public boolean supports(HostAndPort hostAndPort) {
        return true;
    }

}
