package com.ft.jerseyhttpwrapper.providers;

import com.google.common.net.HostAndPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

import java.util.List;

/**
 * StaticHostAndPortProvider
 *
 * @author Simon.Gibbs
 */
public abstract class StaticHostAndPortProvider implements HostAndPortProvider {


    private static final Logger LOGGER = LoggerFactory.getLogger(StaticHostAndPortProvider.class);

    public StaticHostAndPortProvider(List<HostAndPort> hostsAndPorts) {
        this.hostsAndPorts = hostsAndPorts;
    }
    private final List<HostAndPort> hostsAndPorts;

    @Override
    public void handleFailedHost(HostAndPort hostAndPort) {
        LOGGER.info("Host - {} failed to respond correctly", hostAndPort.getHostText());
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
            if(someEndpoint.getHostText().equals(hostAndPort.getHostText())) {
                int candidatesPort = hostAndPort.getPortOrDefault(-1);
                int someEndpointsPort = someEndpoint.getPortOrDefault(-1);

                // ports are only supported for testing purposes anyway, so be permissive
                if(candidatesPort<0 || someEndpointsPort <0) {
                    LOGGER.debug("Using wildcard port");
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
