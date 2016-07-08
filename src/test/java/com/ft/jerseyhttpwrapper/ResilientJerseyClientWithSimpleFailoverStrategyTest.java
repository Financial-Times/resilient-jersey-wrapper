package com.ft.jerseyhttpwrapper;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.ws.rs.core.UriBuilder;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.UniformInterfaceException;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import com.ft.jerseyhttpwrapper.providers.SimpleHostAndPortProvider;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.google.common.net.HostAndPort;
import com.sun.jersey.api.client.ClientResponse;

public class ResilientJerseyClientWithSimpleFailoverStrategyTest {

    private static final String PAYLOAD = "\u266b \u266b The hills are alive with the sound of music \u266b \u266b";

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
	
	ResilientClient resilientClient;
	
	@Before
	public void setup() {	
		resilientClient = ResilientClientBuilder.inTesting(HostAndPort.fromParts("localhost", instanceRule1.port()))
                .withPrimary(new SimpleHostAndPortProvider(
                        HostAndPort.fromParts("localhost", instanceRule1.port()),
                        HostAndPort.fromParts("localhost", instanceRule2.port())
                ))
                .withSecondary(new SimpleHostAndPortProvider(
                        HostAndPort.fromParts("localhost", instanceRule3.port()),
                        HostAndPort.fromParts("localhost", instanceRule4.port())))
                .build();		
	}
	
	@Test
	public void givenHappyPathShouldTryFirstHost(){
		
		WireMock.configureFor("localhost", wmClassRule1.port());
		WireMock.stubFor(get(urlMatching(".*")).willReturn(aResponse().withStatus(200)));
		
		URI uri = UriBuilder
                .fromPath("testing")
                .scheme("http")
                .host("localhost")
                .port(wmClassRule1.port())
                .build();
		
		ClientResponse clientResponse = resilientClient.resource(uri).get(ClientResponse.class);
		assertThat(clientResponse.getStatus(), is(200));
	}
	
	@Test
	public void shouldFailoverAcrossPairsOfHosts() {

		stubForWireMockClassRule(wmClassRule1, get(urlMatching(".*")).willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE)));
		stubForWireMockClassRule(wmClassRule2, get(urlMatching(".*")).willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE)));
		stubForWireMockClassRule(wmClassRule3, get(urlMatching(".*")).willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE)));
		stubForWireMockClassRule(wmClassRule4, get(urlMatching(".*")).willReturn(aResponse().withStatus(200)));

		URI uri = UriBuilder
                .fromPath("testing")
                .scheme("http")
                .host("localhost")
                .port(wmClassRule4.port())
                .build();

		ClientResponse clientResponse = resilientClient.resource(uri).get(ClientResponse.class);
		assertThat(clientResponse.getStatus(), is(200));

		verifyBehaviourForClassRule(wmClassRule1, WireMock.getRequestedFor(urlMatching("/testing")));
		verifyBehaviourForClassRule(wmClassRule2, WireMock.getRequestedFor(urlMatching("/testing")));
		verifyBehaviourForClassRule(wmClassRule3, WireMock.getRequestedFor(urlMatching("/testing")));
		verifyBehaviourForClassRule(wmClassRule4, WireMock.getRequestedFor(urlMatching("/testing")));

	}

    @Test
    public void shouldFailoverAcrossPairsOfHostsWithPUTAndPayload() {
    	
        stubForWireMockClassRule(wmClassRule1, put(urlMatching(".*")).willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE)));
        stubForWireMockClassRule(wmClassRule2, put(urlMatching(".*")).willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE)));
        stubForWireMockClassRule(wmClassRule3, put(urlMatching(".*")).willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE)));
        stubForWireMockClassRule(wmClassRule4, put(urlMatching(".*")).willReturn(aResponse().withStatus(200)));

        URI uri = UriBuilder
                .fromPath("testing")
                .scheme("http")
                .host("localhost")
                .port(wmClassRule4.port())
                .build();

        resilientClient
                .resource(uri)
                .header("Content-Type","text/plain; charset=UTF-8")
                .put(PAYLOAD);

        // assertThat(clientResponse.getStatus(), is(200));

        verifyBehaviourForClassRule(wmClassRule1, putRequestedFor(urlMatching("/testing")).withRequestBody(equalTo(PAYLOAD)));
        verifyBehaviourForClassRule(wmClassRule2, putRequestedFor(urlMatching("/testing")).withRequestBody(equalTo(PAYLOAD)));
        verifyBehaviourForClassRule(wmClassRule3, putRequestedFor(urlMatching("/testing")).withRequestBody(equalTo(PAYLOAD)));
        verifyBehaviourForClassRule(wmClassRule4, putRequestedFor(urlMatching("/testing")).withRequestBody(equalTo(PAYLOAD)));

    }
	
	@Test
	public void shouldFailoverFor503ResponseStatusAndReport503WhenAllNodesAffected() {
        assertFailoverAndReportingOnStatusCode(503);
	}

    @Test
    public void shouldFailoverFor500ResponseStatusAndReport500WhenAllNodesAffected() {
        assertFailoverAndReportingOnStatusCode(500);
    }

    @Test
    public void shouldFailoverFor504ResponseStatusAndReport504WhenAllNodesAffected() {
        assertFailoverAndReportingOnStatusCode(504);
    }

    private void assertFailoverAndReportingOnStatusCode(int failoverStatus) {

        stubForWireMockClassRule(wmClassRule1, get(urlMatching(".*")).willReturn(aResponse().withStatus(failoverStatus)));
        stubForWireMockClassRule(wmClassRule2, get(urlMatching(".*")).willReturn(aResponse().withStatus(failoverStatus)));
        stubForWireMockClassRule(wmClassRule3, get(urlMatching(".*")).willReturn(aResponse().withStatus(failoverStatus)));
        stubForWireMockClassRule(wmClassRule4, get(urlMatching(".*")).willReturn(aResponse().withStatus(failoverStatus)));

        URI uri = UriBuilder
            .fromPath("testing")
            .scheme("http")
            .host("localhost")
            .port(wmClassRule4.port())
            .build();

        ClientResponse clientResponse = resilientClient.resource(uri).get(ClientResponse.class);
        assertThat(clientResponse.getStatus(), is(failoverStatus));

        verifyBehaviourForClassRule(wmClassRule1, WireMock.getRequestedFor(urlMatching("/testing")));
        verifyBehaviourForClassRule(wmClassRule2, WireMock.getRequestedFor(urlMatching("/testing")));
        verifyBehaviourForClassRule(wmClassRule3, WireMock.getRequestedFor(urlMatching("/testing")));
        verifyBehaviourForClassRule(wmClassRule4, WireMock.getRequestedFor(urlMatching("/testing")));
    }


    @Test
    public void shouldReportServerErrorRatherThanNetworkErrorIfOutcomeIsMixed() {

        stubForWireMockClassRule(wmClassRule1, get(urlMatching(".*")).willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE)));
        stubForWireMockClassRule(wmClassRule2, get(urlMatching(".*")).willReturn(aResponse().withStatus(500)));
        stubForWireMockClassRule(wmClassRule3, get(urlMatching(".*")).willReturn(aResponse().withStatus(500)));
        stubForWireMockClassRule(wmClassRule4, get(urlMatching(".*")).willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE)));

        URI uri = UriBuilder
                .fromPath("testing")
                .scheme("http")
                .host("localhost")
                .port(wmClassRule4.port())
                .build();

        ClientResponse clientResponse = resilientClient.resource(uri).get(ClientResponse.class);
        assertThat(clientResponse.getStatus(), is(500));

        verifyBehaviourForClassRule(wmClassRule1, WireMock.getRequestedFor(urlMatching("/testing")));
        verifyBehaviourForClassRule(wmClassRule2, WireMock.getRequestedFor(urlMatching("/testing")));
        verifyBehaviourForClassRule(wmClassRule3, WireMock.getRequestedFor(urlMatching("/testing")));
        verifyBehaviourForClassRule(wmClassRule4, WireMock.getRequestedFor(urlMatching("/testing")));
    }

    @Test
    public void shouldNotFailoverToSecondListIfFirstListIsUp() {

        stubForWireMockClassRule(wmClassRule1, post(urlMatching(".*")).willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE)));
        stubForWireMockClassRule(wmClassRule2, post(urlMatching(".*")).willReturn(aResponse().withStatus(201)));
        stubForWireMockClassRule(wmClassRule3, post(urlMatching(".*")).willReturn(aResponse().withStatus(500)));
        stubForWireMockClassRule(wmClassRule4, post(urlMatching(".*")).willReturn(aResponse().withStatus(500)));

        URI uri = UriBuilder
                .fromPath("testing")
                .scheme("http")
                .host("localhost")
                .build();

        ClientResponse clientResponse = resilientClient.resource(uri).post(ClientResponse.class);
        assertThat(clientResponse.getStatus(), is(201));

        verifyBehaviourForClassRule(wmClassRule1, WireMock.postRequestedFor(urlMatching("/testing")));
        verifyBehaviourForClassRule(wmClassRule2, WireMock.postRequestedFor(urlMatching("/testing")));
        verifyNeverCalledForClassRule(wmClassRule3, WireMock.postRequestedFor(urlMatching("/testing")));
        verifyNeverCalledForClassRule(wmClassRule4, WireMock.postRequestedFor(urlMatching("/testing")));

    }

    @Test
    public void shouldNotExhaustConnectionPoolForSlow503s() throws ExecutionException {

        int responseSpeed = 350; // milliseconds

        String testUrl = "/slow-503";

        stubForWireMockClassRule(wmClassRule1, get(urlMatching(testUrl)).willReturn(aSlow503WithBody(responseSpeed)));
        stubForWireMockClassRule(wmClassRule2, get(urlMatching(testUrl)).willReturn(aSlow503WithBody(responseSpeed)));
        stubForWireMockClassRule(wmClassRule3, get(urlMatching(testUrl)).willReturn(aSlow503WithBody(responseSpeed)));
        stubForWireMockClassRule(wmClassRule4, get(urlMatching(testUrl)).willReturn(aSlow503WithBody(responseSpeed)));

        final URI uri = UriBuilder
                .fromPath(testUrl)
                .scheme("http")
                .host("localhost")
                .port(-1)
                .build();

        ExecutorService executorService = Executors.newFixedThreadPool(25);

        List<Future<String>> loadUnits = new ArrayList<>(100);

        for(int i=0;i<100;i++) {
           Future<String> loadUnit =  executorService.submit(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    String result = resilientClient.resource(uri).get(String.class);
                    return result;
                }
            });
            loadUnits.add(loadUnit);
        }

        for(Future<String> loadUnit : loadUnits) {
            try {
                assertThat(loadUnit.get(), notNullValue());
            } catch (ExecutionException ee) {
                UniformInterfaceException uie = (UniformInterfaceException) ee.getCause();
                ClientResponse response = uie.getResponse();
                try {
                    assertThat(response.getStatus(),is(503));
                } finally {
                    response.close();
                }
            }
            catch (InterruptedException e) {
                //Thread.currentThread().interrupt();
            }
        }

    }

    @Test
    public void shouldNotExhaustConnectionPoolForNonResponsiveServer() throws ExecutionException {

        int responseSpeed = 250; // milliseconds

        String testUrl = "/slow-non-response";

        stubForWireMockClassRule(wmClassRule1, get(urlMatching(testUrl)).willReturn(aResponse().withFixedDelay(responseSpeed).withFault(Fault.EMPTY_RESPONSE)));
        stubForWireMockClassRule(wmClassRule2, get(urlMatching(testUrl)).willReturn(aResponse().withFixedDelay(responseSpeed).withFault(Fault.EMPTY_RESPONSE)));
        stubForWireMockClassRule(wmClassRule3, get(urlMatching(testUrl)).willReturn(aResponse().withFixedDelay(responseSpeed).withFault(Fault.EMPTY_RESPONSE)));
        stubForWireMockClassRule(wmClassRule4, get(urlMatching(testUrl)).willReturn(aResponse().withFixedDelay(responseSpeed).withFault(Fault.EMPTY_RESPONSE)));

        final URI uri = UriBuilder
                .fromPath(testUrl)
                .scheme("http")
                .host("localhost")
                .port(-1)
                .build();

        ExecutorService executorService = Executors.newFixedThreadPool(25);

        List<Future<String>> loadUnits = new ArrayList<>(100);

        for(int i=0;i<100;i++) {
            Future<String> loadUnit =  executorService.submit(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    String result = resilientClient.resource(uri).get(String.class);
                    return result;
                }
            });
            loadUnits.add(loadUnit);
        }

        for(Future<String> loadUnit : loadUnits) {
            try {
                assertThat(loadUnit.get(), notNullValue());
            } catch (ExecutionException ee) {
                assertTrue(ee.getCause() instanceof ClientHandlerException);
                assertFalse(ee.getCause().getCause() instanceof ConnectionPoolTimeoutException);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

    }

    private ResponseDefinitionBuilder aSlow503WithBody(int responseSpeed) {
        return aResponse().withBody(PAYLOAD).withStatus(503).withFixedDelay(responseSpeed);
    }


    private void verifyBehaviourForClassRule(WireMockClassRule wireMockClassRule, RequestPatternBuilder requestPatternBuilder) {
		WireMock.configureFor("localhost", wireMockClassRule.port());
		WireMock.verify(requestPatternBuilder);
	}
	
	private void verifyNeverCalledForClassRule(WireMockClassRule wireMockClassRule, RequestPatternBuilder requestPatternBuilder) {
		WireMock.configureFor("localhost", wireMockClassRule.port());
		WireMock.verify(0, requestPatternBuilder);
	}

	private void stubForWireMockClassRule(WireMockClassRule classRule, MappingBuilder mappingBuilder) {
		WireMock.configureFor("localhost", classRule.port());
		WireMock.stubFor(mappingBuilder);
	}
}
