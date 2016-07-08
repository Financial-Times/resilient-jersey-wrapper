package com.ft.jerseyhttpwrapper.providers;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

import com.google.common.net.HostAndPort;

public class RandomHostAndPortProvider extends StaticHostAndPortProvider {
	
	private final Random randomIndexGenerator;
	
	public RandomHostAndPortProvider(List<HostAndPort> hostNames) {
		super(hostNames);
        this.randomIndexGenerator = new Random();
	}

    public RandomHostAndPortProvider(List<HostAndPort> hostNames, Random randomIndexGenerator) {
        super(hostNames);
        this.randomIndexGenerator = randomIndexGenerator;
    }

    @Override
    public Iterator<HostAndPort> iterator(HostAndPort suppliedAddress) {
        return new RandomisedHostsIterator(getHostNames(), randomIndexGenerator);
    }
}
