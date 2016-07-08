package com.ft.jerseyhttpwrapper.providers;

import com.google.common.net.HostAndPort;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

/**
 * RandomisedHostsIterator
 *
 * @author Simon.Gibbs
 */
class RandomisedHostsIterator implements Iterator<HostAndPort> {

    ArrayList<HostAndPort> clonedHosts;

    private final Random randomIndexGenerator;

    public RandomisedHostsIterator(List<HostAndPort> hosts, Random randomSource) {
        clonedHosts = new ArrayList<HostAndPort>(hosts);
        randomIndexGenerator = randomSource;
    }

    @Override
    public boolean hasNext() {
        return !clonedHosts.isEmpty();
    }

    @Override
    public HostAndPort next() {
        if(!hasNext()) {
            throw new NoSuchElementException("No more hosts available");
        }
        int randomIndex = randomIndexGenerator.nextInt(clonedHosts.size());
        return clonedHosts.remove(randomIndex);
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Remove method not supported");
    }
}
