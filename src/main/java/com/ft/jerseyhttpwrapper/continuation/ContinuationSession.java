package com.ft.jerseyhttpwrapper.continuation;

import com.google.common.net.HostAndPort;

/**
 * <p>Iterates over available endpoints an implementation specific number of times. Encapsulates the per-request state
 * of a {@link ContinuationPolicy}.</p>
 *
 * <p>Implementors must ensure there is a halting condition</p>
 *
 * @author Simon.Gibbs
 */
public interface ContinuationSession {

    /**
     * Returns {@code true} if if is appropriate to try additional nodes.
     * (In other words, returns {@code true} if {@link #nextHost} would
     * return an endpoint rather than throwing an exception.)
     *
     * @return {@code true} if the iteration has more elements
     */
    boolean shouldContinue();

    /**
     * Returns the next endpoints in the session.
     *
     * @return the next endpoints in the session
     * @throws java.util.NoSuchElementException if the session has no more endpoints
     */
    HostAndPort nextHost();

    /**
     * <p>Informs the session and the provider that a request to the host failed.</p>
     *
     * <p>Implementors are obligated to maintain a reference to the provider and delegate the call during execution of the method.</p>
     *
     * TODO if we prefer to extract from ResilientClient the aspects of the retry policy that interpret status codes and exceptions then this method signature MAY need to change
     *
     * @param hostAndPort the affected host and the port used in the failed request
     * */
    void handleFailedHost(HostAndPort hostAndPort);

}
