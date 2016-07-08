package com.ft.jerseyhttpwrapper.providers;

import com.google.common.net.HostAndPort;

import java.util.Collections;
import java.util.Iterator;

/**
 * NullHostAndPortProvider
 *
 * @author Simon.Gibbs
 */
public class NullHostAndPortProvider implements HostAndPortProvider {

    public Iterator<HostAndPort> iterator(HostAndPort suppliedAddress) {
        return Collections.emptyIterator();
    }

    @Override
    public void handleFailedHost(HostAndPort hostAndPort) {
        throw new IllegalArgumentException("Not a managed node: " + hostAndPort.toString());
    }

    @Override
    public boolean supports(HostAndPort hostAndPort) {
        return false;
    }

}
