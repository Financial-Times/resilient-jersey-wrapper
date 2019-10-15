package com.ft.jerseyhttpwrapper.providers;

import static com.ft.membership.logging.Operation.operation;

import java.net.URI;
import java.util.List;

import com.ft.membership.logging.Operation;
import com.google.common.net.HostAndPort;

/**
 * StaticHostAndPortProvider
 *
 * @author Simon.Gibbs
 */
public abstract class StaticHostAndPortProvider implements HostAndPortProvider {

    public StaticHostAndPortProvider(List<HostAndPort> hostsAndPorts) {
        this.hostsAndPorts = hostsAndPorts;
    }
    private final List<HostAndPort> hostsAndPorts;

	@Override
	public void handleFailedHost(HostAndPort hostAndPort) {
		final Operation operationJson = operation("handleFailedHost")
				.jsonLayout().initiate(this);
		operationJson.logIntermediate()
				.yielding("msg",
						hostAndPort.getHost() + " failed to respond correctly")
				.logInfo();
	}

    protected List<HostAndPort> getHostNames() {
        return hostsAndPorts;
    }

    public boolean supports(HostAndPort knownAddress) {
        return hasHost(knownAddress);
    }

    public boolean hasHost(URI originalUri) {
        return hasHost(HostAndPort.fromString(originalUri.getAuthority()));
    }

    public boolean hasHost(HostAndPort someEndpoint) {
        for(HostAndPort hostAndPort : hostsAndPorts) {
            if(someEndpoint.getHost().equals(hostAndPort.getHost())) {
                int candidatesPort = hostAndPort.getPortOrDefault(-1);
                int someEndpointsPort = someEndpoint.getPortOrDefault(-1);

                // ports are only supported for testing purposes anyway, so be permissive
                if(candidatesPort<0 || someEndpointsPort <0) {
            		final Operation operationJson = operation("hasHost")
            				.jsonLayout().initiate(this);
            		operationJson.logIntermediate()
            				.yielding("msg", "Using wildcard port").logDebug();

                    return true;
                }

                if(candidatesPort == someEndpointsPort) {
                    return true;
                }
            }
        }
        return false;
    }


}
