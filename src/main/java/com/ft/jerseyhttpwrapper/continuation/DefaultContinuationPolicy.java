package com.ft.jerseyhttpwrapper.continuation;

import com.ft.jerseyhttpwrapper.providers.HostAndPortProvider;
import com.google.common.net.HostAndPort;

import java.net.URI;

/**
 * DefaultContinuationPolicy
 *
 * @author Simon.Gibbs
 */
public class DefaultContinuationPolicy implements ContinuationPolicy {
    @Override
    public ContinuationSession startSession(HostAndPort suppliedAddress, HostAndPortProvider hostAndPortProvider) {

        return new DefaultContinuationSession(suppliedAddress, hostAndPortProvider);
    }
}
