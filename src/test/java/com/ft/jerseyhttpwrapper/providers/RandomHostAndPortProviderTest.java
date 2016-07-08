package com.ft.jerseyhttpwrapper.providers;

import com.ft.jerseyhttpwrapper.ResilientClient;
import com.ft.jerseyhttpwrapper.ResilientClientBuilder;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.google.common.collect.Lists;
import com.google.common.net.HostAndPort;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.core.UriBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class RandomHostAndPortProviderTest {

    private static final long KNOWN_SEED = 1L;
    public static final int HOSTS = 5;
    
    @ClassRule
	public static WireMockClassRule wmClassRule1 = new WireMockClassRule(0);
    
    @Rule
    public WireMockClassRule instanceRule1 = wmClassRule1;
	
	@ClassRule
	public static WireMockClassRule wmClassRule2 = new WireMockClassRule(0);
	
	@Rule
    public WireMockClassRule instanceRule2 = wmClassRule2;
	
	@ClassRule
	public static WireMockClassRule wmClassRule3 = new WireMockClassRule(0);
	
	@Rule
    public WireMockClassRule instanceRule3 = wmClassRule3;

	@ClassRule
	public static WireMockClassRule wmClassRule4 = new WireMockClassRule(0);
	
	@Rule
    public WireMockClassRule instanceRule4 = wmClassRule4;

	@ClassRule
	public static WireMockClassRule wmClassRule5 = new WireMockClassRule(0);
	
	@Rule
    public WireMockClassRule instanceRule5 = wmClassRule5;

    /**
     * This test protects us against changes in (and differences between) random number generators
     */
    @Test
    public void givenAKnownRandomSequence() {
        Random knownSequence = knownSequence();

        int [] hostOrdinalSequence = {
                knownSequence.nextInt(HOSTS),
                knownSequence.nextInt(HOSTS),
                knownSequence.nextInt(HOSTS),
                knownSequence.nextInt(HOSTS),
                knownSequence.nextInt(HOSTS)
        };

        assertThat(hostOrdinalSequence[0],is(0));
        assertThat(hostOrdinalSequence[1],is(3));
        assertThat(hostOrdinalSequence[2],is(2));
        assertThat(hostOrdinalSequence[3],is(3));
        assertThat(hostOrdinalSequence[4],is(4));
    }


    @Test
    public void shouldTryAllNodesInKnownSequence() {

        ResilientClient resilientClient = ResilientClientBuilder.inTesting(mockHost(wmClassRule1))
                .withPrimary(new RandomHostAndPortProvider(fiveMockHosts(), knownSequence()))
                .withSecondary(new NullHostAndPortProvider())
                .build();


        invokeSomeUrl(resilientClient);
        verifyHostWasHit(wmClassRule1);
        WireMock.reset();

        invokeSomeUrl(resilientClient);
        verifyHostWasHit(wmClassRule4);
        WireMock.reset();

        invokeSomeUrl(resilientClient);
        verifyHostWasHit(wmClassRule3);
        WireMock.reset();

        invokeSomeUrl(resilientClient);
        verifyHostWasHit(wmClassRule4);
        WireMock.reset();

        invokeSomeUrl(resilientClient);
        verifyHostWasHit(wmClassRule5);
        WireMock.reset();

    }

    @Test(timeout = 120000)
    public void shouldStopFailingOverAfterTryingEachHost() {

        // given a client wired into five hosts
        ResilientClient resilientClient = ResilientClientBuilder.inTesting(mockHost(wmClassRule1))
                .withPrimary(new RandomHostAndPortProvider(fiveMockHosts(), knownSequence()))
                .withSecondary(new NullHostAndPortProvider())
                .build();

        // and all five hosts will fail
        for(WireMockClassRule mockHost : wireMockServices()) {
            stubForWireMockClassRule(mockHost, WireMock.get(WireMock.urlMatching("/testing")).willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE)));
        }

        try {
            invokeSomeUrl(resilientClient);
            fail("expected all hosts to fail");
        } catch (ClientHandlerException e) {
            e.printStackTrace();
        }

        ArrayList<LoggedRequest> requests = new ArrayList<LoggedRequest>();
        for(WireMockClassRule mockHost : wireMockServices()) {
            WireMock.configureFor("localhost", mockHost.port());
            requests.addAll(
                    WireMock.findAll(WireMock.getRequestedFor(urlMatching("/testing")))
            );
        }

        //assertThat(requests.size(),is(5));

    }

    private List<WireMockClassRule> wireMockServices() {
        return Arrays.asList(wmClassRule1, wmClassRule2, wmClassRule3, wmClassRule4, wmClassRule5);
    }

    private void stubForWireMockClassRule(WireMockClassRule classRule, MappingBuilder mappingBuilder) {
        WireMock.configureFor("localhost", classRule.port());
        WireMock.stubFor(mappingBuilder);
    }

    private void verifyHostWasHit(WireMockClassRule wireMockClassRule) {
        WireMock.configureFor("localhost", wireMockClassRule.port());
        WireMock.verify(1,WireMock.getRequestedFor(urlMatching("/testing")));
    }

    private ClientResponse invokeSomeUrl(ResilientClient resilientClient) {
        return resilientClient.resource(someUrl()).get(ClientResponse.class);
    }

    private URI someUrl() {
        return UriBuilder
                .fromPath("/testing")
                .scheme("http")
                .host("localhost")
                .build();
    }

    private ArrayList<HostAndPort> fiveMockHosts() {
        return Lists.newArrayList(
                mockHost(wmClassRule1),
                mockHost(wmClassRule2),
                mockHost(wmClassRule3),
                mockHost(wmClassRule4),
                mockHost(wmClassRule5)
        );
    }

    private HostAndPort mockHost(WireMockClassRule wireMock) {
        return HostAndPort.fromParts("localhost", wireMock.port());
    }


    private Random knownSequence() {
        return new Random(KNOWN_SEED);
    }

}
