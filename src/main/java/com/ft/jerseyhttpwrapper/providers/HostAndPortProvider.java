package com.ft.jerseyhttpwrapper.providers;

import java.util.Iterator;

import com.google.common.net.HostAndPort;

public interface HostAndPortProvider  {

    /**
     * Provides valid alternates for the given endpoint, optionally including the supplied endpoint.
     * @param suppliedAddress whatever endpoint is known the the application
     * @return valid alternates for the given endpoint
     */
    Iterator<HostAndPort> iterator(HostAndPort suppliedAddress);

    /**
     * Provides a feedback mechanism for the provider so that it can remove nodes from it's pool or change load
     * balancing priorities
     * @param hostAndPort an endpoint which has encountered an error condition
     */
	void handleFailedHost(HostAndPort hostAndPort);


    /**
     * Check if the endpoint is supportable
     * @param hostAndPort the endpoint known to the application
     * @return <code>true</code> if the endpoint is supportable; otherwise <code>false</code>
     */
    boolean supports(HostAndPort hostAndPort);

}
