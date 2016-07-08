package com.ft.jerseyhttpwrapper.providers;

import com.google.common.net.HostAndPort;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class SimpleHostAndPortProvider extends StaticHostAndPortProvider {

    public SimpleHostAndPortProvider(HostAndPort... hostsAndPorts) {
        this(Arrays.asList(hostsAndPorts));
    }

    public SimpleHostAndPortProvider(List<HostAndPort> hostsAndPorts) {
        super(hostsAndPorts);
    }

    public Iterator<HostAndPort> iterator(HostAndPort suppliedAddress) {
        return getHostNames().iterator();
    }
}
