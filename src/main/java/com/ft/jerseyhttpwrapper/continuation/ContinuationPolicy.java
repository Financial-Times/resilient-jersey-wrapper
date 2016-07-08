package com.ft.jerseyhttpwrapper.continuation;

import com.ft.jerseyhttpwrapper.providers.HostAndPortProvider;
import com.google.common.net.HostAndPort;

/**
 * <p>Defines a factory interface for {@link ContinuationSession} objects. Implementors should encapsulate
 * any stateless features of the policy and create the remaining state as appropriate within the
 * {@link ContinuationSession}.</p>
 *
 * <p>The content of the policy is whether or not to continue retrying, and if so with which endpoints.</p>
 *
 * @author Simon.Gibbs
 */
public interface ContinuationPolicy {

    /**
     * Creates a new session for a request providing a modified view/version of the provider's endpoints
     * @param suppliedAddress the address provided by the application
     * @param hostAndPortProvider the prevailing endpoint provider
     * @return a stateful iterator-style object
     */
    ContinuationSession startSession(HostAndPort suppliedAddress, HostAndPortProvider hostAndPortProvider);

}
