package com.ft.jerseyhttpwrapper.continuation;

import com.ft.jerseyhttpwrapper.providers.HostAndPortProvider;
import com.google.common.net.HostAndPort;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Iterator;
import java.util.NoSuchElementException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ExponentialBackoffContinuationSessionTest
 *
 * @author Simon.Gibbs
 */
@RunWith(MockitoJUnitRunner.class)
public class ExponentialBackoffContinuationSessionTest {

    HostAndPort someHost = HostAndPort.fromString("localhost:8080");


    @Mock
    HostAndPortProvider mockProvider;

    @Mock
    private Iterator<HostAndPort> firstMockProviderIterator;

    @Mock
    private Iterator<HostAndPort> secondMockProviderIterator;

    @Mock
    private Iterator<HostAndPort> thirdMockProviderIterator;

    int hostsReturned;

    private Runnable sessionLoadGenerator(final ContinuationSession session) {
        return new Runnable() {
            @Override
            public void run() {
                // this will run until interrupted
                while(session.shouldContinue()) {
                    session.nextHost();
                    hostsReturned++;
                }
            }
        };
    }

    @Before
    public void resetPerTestCount() {
        hostsReturned=0;
    }

    @Test
    public void shouldStopIteratingIfInterupted() {

        // Given an infinite supply of hosts
        when(mockProvider.iterator(someHost)).thenReturn(firstMockProviderIterator);
        when(firstMockProviderIterator.next()).thenReturn(someHost);
        when(firstMockProviderIterator.hasNext()).thenReturn(true);

        final ExponentialBackoffContinuationSession session = new ExponentialBackoffContinuationSession(someHost,mockProvider,new ExponentialBackoffContinuationPolicy(100,100));

        Thread threadUnderTest = new Thread(sessionLoadGenerator(session));

        // interrupt the the thread almost immediately
        threadUnderTest.start();
        sleepFor(5);
        threadUnderTest.interrupt();

        // wait long enough to detect an error (enough for a few loops)
        sleepFor(1000);

        // the minimum delay was 100ms, which implies the second attempt was observable but we should not have survived to see a third item returned.
        assertThat(hostsReturned, is(2));

    }


    @Test
    public void shouldBackOffExponentially() {
        // Given many hosts
        when(mockProvider.iterator(someHost)).thenReturn(firstMockProviderIterator);
        when(firstMockProviderIterator.next()).thenReturn(someHost);
        when(firstMockProviderIterator.hasNext()).thenReturn(true, true, true, true, false);

        final ExponentialBackoffContinuationSession session = new ExponentialBackoffContinuationSession(someHost,mockProvider,new ExponentialBackoffContinuationPolicy(4,100));

        Thread threadUnderTest = new Thread(sessionLoadGenerator(session));

        // interrupt the the thread almost immediately
        threadUnderTest.start();

        sleepFor(5);
        assertThat(hostsReturned, is(1));
        sleepFor(100);
        assertThat(hostsReturned, is(2));
        sleepFor(200);
        assertThat(hostsReturned, is(3));
        sleepFor(400);
        assertThat(hostsReturned, is(4));

        // should stop naturally after the 4 configured "true" return values for hasNext()
        sleepFor(800);
        assertThat(hostsReturned, is(4));

        threadUnderTest.interrupt();

    }


    @Test
    public void shouldLoopThroughHostsListMultipleTimesUpToConfiguredLimit() {

        when(mockProvider.iterator(someHost)).thenReturn(firstMockProviderIterator, secondMockProviderIterator, thirdMockProviderIterator);

        when(firstMockProviderIterator.next()).thenReturn(someHost); // always this one, we aren't checking
        when(secondMockProviderIterator.next()).thenReturn(someHost);
        when(thirdMockProviderIterator.next()).thenReturn(someHost);

        // iterator behaviour consistent with 2 hosts over two loops.
        when(firstMockProviderIterator.hasNext()).thenReturn(true, true, false);
        when(secondMockProviderIterator.hasNext()).thenReturn(true, true, false);

        // add provision for a third loop to demonstrate the session stopping on it's own
        when(thirdMockProviderIterator.hasNext()).thenReturn(true, true, false);

        final ExponentialBackoffContinuationSession session = new ExponentialBackoffContinuationSession(someHost,mockProvider,new ExponentialBackoffContinuationPolicy(4,1));

        int hostsReturned = 0; // local variable, not a field this time
        while(session.shouldContinue()) {
            assertThat(session.nextHost(), notNullValue());
            hostsReturned++;
        }

        // must have done two loops and stopped
        assertThat(hostsReturned,is(4));

        verify(thirdMockProviderIterator,never()).next();

    }

    @Test
    public void shouldTryOnceIfSoConfigured() {
        // Given an infinite supply of hosts
        when(mockProvider.iterator(someHost)).thenReturn(firstMockProviderIterator);
        when(firstMockProviderIterator.next()).thenReturn(someHost);
        when(firstMockProviderIterator.hasNext()).thenReturn(true);

        final ExponentialBackoffContinuationSession session = new ExponentialBackoffContinuationSession(someHost,mockProvider,new ExponentialBackoffContinuationPolicy(1,100));

        assertThat(session.shouldContinue(), is(true));

        session.nextHost();

        assertThat(session.shouldContinue(),is(false));

    }


    @Test(expected = NoSuchElementException.class)
    public void shouldThrowNoSuchElementIfTooManyHostsRequested() {
        // Given an infinite supply of hosts
        when(mockProvider.iterator(someHost)).thenReturn(firstMockProviderIterator);
        when(firstMockProviderIterator.next()).thenReturn(someHost);
        when(firstMockProviderIterator.hasNext()).thenReturn(true);

        final ExponentialBackoffContinuationSession session = new ExponentialBackoffContinuationSession(someHost,mockProvider,new ExponentialBackoffContinuationPolicy(1,100));

        session.nextHost();
        session.nextHost();

    }

    private void sleepFor(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }


}
