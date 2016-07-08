package com.ft.jerseyhttpwrapper.continuation;

import com.ft.jerseyhttpwrapper.providers.HostAndPortProvider;
import com.google.common.net.HostAndPort;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * ExponentialBackoffContinuationSession
 *
 * @author Simon.Gibbs
 */
public class ExponentialBackoffContinuationSession implements ContinuationSession {
    private final HostAndPort suppliedAddress;
    private final HostAndPortProvider hostAndPortProvider;
    private final ExponentialBackoffContinuationPolicy policy;

    private Iterator<HostAndPort> iterator;
    private int attemptsCount;

    public ExponentialBackoffContinuationSession(HostAndPort suppliedAddress, HostAndPortProvider hostAndPortProvider, ExponentialBackoffContinuationPolicy exponentialBackoffContinuationPolicy) {
        this.suppliedAddress = suppliedAddress;
        this.hostAndPortProvider = hostAndPortProvider;
        this.policy = exponentialBackoffContinuationPolicy;
        iterator = hostAndPortProvider.iterator(suppliedAddress);
    }

    @Override
    public boolean shouldContinue() {
        // we've been interrupted. We don't want to be handing nulls in nextHost() so just stop producing attempts now
        if(Thread.currentThread().isInterrupted()) {
            return false;
        }
        return attemptsCount < policy.getMaxAttempts();
    }

    @Override
    public HostAndPort nextHost() {

        // Block before the second and subsequent attempts.
        if(attemptsCount>0) {

            if(attemptsCount == policy.getMaxAttempts()) {
                throw new NoSuchElementException("Attempt " + attemptsCount);
            }

            try {
                Thread.sleep((long) Math.pow(2,attemptsCount-1)* policy.getBackoffMultiplier());
            } catch (InterruptedException e) {
                // Restore the interrupted status. It will be inspected no later than the the next call to shouldContinue()
                Thread.currentThread().interrupt();
            }
        }

        // don't be limited by a lack of hosts
        if(!iterator.hasNext()) {
            iterator = hostAndPortProvider.iterator(suppliedAddress);
        }

        attemptsCount++;

        return iterator.next();
    }

    @Override
    public void handleFailedHost(HostAndPort hostAndPort) {
        synchronized (hostAndPortProvider) {
            hostAndPortProvider.handleFailedHost(hostAndPort);
        }
    }
}
