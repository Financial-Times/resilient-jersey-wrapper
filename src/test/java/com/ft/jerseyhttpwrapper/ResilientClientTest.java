package com.ft.jerseyhttpwrapper;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.http.Fault.MALFORMED_RESPONSE_CHUNK;
import static com.github.tomakehurst.wiremock.http.RequestMethod.ANY;
import static com.github.tomakehurst.wiremock.http.RequestMethod.DELETE;
import static com.github.tomakehurst.wiremock.http.RequestMethod.GET;
import static com.github.tomakehurst.wiremock.http.RequestMethod.HEAD;
import static com.github.tomakehurst.wiremock.http.RequestMethod.OPTIONS;
import static com.github.tomakehurst.wiremock.http.RequestMethod.POST;
import static com.github.tomakehurst.wiremock.http.RequestMethod.PUT;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import com.ft.jerseyhttpwrapper.config.DummyClientEnvironment;
import com.ft.jerseyhttpwrapper.config.EndpointConfiguration;
import com.ft.jerseyhttpwrapper.providers.NullHostAndPortProvider;
import com.ft.jerseyhttpwrapper.providers.RandomHostAndPortProvider;
import com.ft.jerseyhttpwrapper.providers.SimpleHostAndPortProvider;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.net.HostAndPort;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.util.Duration;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import javax.ws.rs.core.UriBuilder;
import org.junit.*;
import org.slf4j.MDC;

public class ResilientClientTest {
  private static final String TX_HEADER = "X-Request-Id";
  private static final String TX_ID = "transaction_id";

  @Rule public WireMockRule wm = new WireMockRule(wireMockConfig().port(0));

  @Rule public WireMockRule wm2 = new WireMockRule(wireMockConfig().port(0));

  @Rule public WireMockRule delayedWm = new WireMockRule(wireMockConfig().port(0));

  private ResilientClientBuilder builder;

  @Before
  public void init() throws Exception {

    HostAndPort wmNode = HostAndPort.fromParts("localhost", wm.port());

    builder =
        ResilientClientBuilder.inTesting(wmNode)
            .withPrimary(new RandomHostAndPortProvider(Collections.singletonList(wmNode)))
            .withSecondary(new NullHostAndPortProvider());

    delayedWm.addRequestProcessingDelay(500);
  }

  @Test
  public void supportsBasicGet() {
    stubGetWillReturn(
        aResponse().withStatus(200).withHeader(CONTENT_TYPE, "text/plain").withBody("Hello world"));

    ClientResponse response = resource(builder.build()).get(ClientResponse.class);

    assertThat(response.getStatus(), is(200));
    assertThat(response.getEntity(String.class), is("Hello world"));

    wm.verify(getRequestedFor(urlEqualTo("/something")).withoutHeader(TX_HEADER));
  }

  @Test
  public void thatTransactionIdIsPropagated() {
    Client client = builder.withTransactionPropagation().build();
    String txId = "junit-tx-1";
    try {
      MDC.put(TX_ID, String.format("%s=%s", TX_ID, txId));

      stubGetWillReturn(
          aResponse()
              .withStatus(200)
              .withHeader(CONTENT_TYPE, "text/plain")
              .withBody("Hello world"));

      ClientResponse response = resource(client).get(ClientResponse.class);

      assertThat(response.getStatus(), is(200));
      assertThat(response.getEntity(String.class), is("Hello world"));

      wm.verify(getRequestedFor(urlEqualTo("/something")).withHeader(TX_HEADER, equalTo(txId)));
    } finally {
      MDC.clear();
    }
  }

  @Test
  public void thatAbsentTransactionIdIsNotPropagated() {
    Client client = builder.withTransactionPropagation().build();
    stubGetWillReturn(
        aResponse().withStatus(200).withHeader(CONTENT_TYPE, "text/plain").withBody("Hello world"));

    ClientResponse response = resource(client).get(ClientResponse.class);

    assertThat(response.getStatus(), is(200));
    assertThat(response.getEntity(String.class), is("Hello world"));

    wm.verify(getRequestedFor(urlEqualTo("/something")).withoutHeader(TX_HEADER));
  }

  @Test(expected = ClientHandlerException.class)
  public void shouldHonourDefaultTimeout() {
    stubGetWillReturn(
        aResponse()
            .withStatus(200)
            .withHeader(CONTENT_TYPE, "text/plain")
            .withFixedDelay(505)
            .withBody("Hello world"));

    resource(builder.build()).get(ClientResponse.class);
  }

  @Test
  public void supportsSingleValuedResponseHeaders() {
    stubGetWillReturn(aResponse().withStatus(200).withHeader("X-Some-Header", "hello-header"));

    ClientResponse response = resource(builder.build()).get(ClientResponse.class);

    assertThat(response.getHeaders().getFirst("X-Some-Header"), is("hello-header"));
  }

  @Test
  public void supportsSingleValuedRequestHeaders() {
    stubGetWillReturn(aResponse().withStatus(200));

    resource(builder.build()).header("X-Request-Header", "request-value").get(ClientResponse.class);

    wm.verify(
        getRequestedFor(urlEqualTo("/something"))
            .withHeader("X-Request-Header", equalTo("request-value")));
  }

  @Test
  public void supportsMultiValuedRequestHeaders() {
    stubGetWillReturn(aResponse().withStatus(200));

    resource(builder.build())
        .header("X-Multi-Request-Header", "request-value-1")
        .header("X-Multi-Request-Header", "request-value-2")
        .header("X-Multi-Request-Header", "request-value-3")
        .get(ClientResponse.class);

    List<LoggedRequest> requests = wm.findAll(getRequestedFor(urlEqualTo("/something")));
    HttpHeader header = requests.get(0).getHeaders().getHeader("X-Multi-Request-Header");

    // TODO this sucks, worth looking into if you have time - should be a list, no?
    assertThat(header.firstValue(), is("request-value-1,request-value-2,request-value-3"));
  }

  @Test
  public void supportsHead() {
    testSupportFor(HEAD);
  }

  @Test
  public void supportsDelete() {
    testSupportFor(DELETE);
  }

  @Test
  public void supportsOptions() {
    testSupportFor(OPTIONS);
  }

  @Test
  public void supportsPost() {
    testSupportFor(POST, "Post body");
  }

  @Test
  public void supportsPut() {
    testSupportFor(PUT, "Put body");
  }

  @Test
  @Ignore("pending team decision about whether to address this concern")
  public void sendsContentLengthHeaderOnPost() {
    stubWillReturn(POST, aResponse().withStatus(200));

    handleWithBody(POST, "TEST");

    wm.verify(
        postRequestedFor(urlEqualTo("/something")).withHeader("Content-Length", equalTo("4")));
  }

  @Test
  @Ignore("pending team decision about whether to address this concern")
  public void sendsChunkedBodyOnPostWhenTransferEncodingHeaderPresentAndChunkSizeSet() {
    stubWillReturn(POST, aResponse().withStatus(200));

    resource(builder.build())
        .header("Transfer-Encoding", "chunked")
        .post(ClientResponse.class, "TEST");

    wm.verify(
        postRequestedFor(urlEqualTo("/something"))
            .withHeader("Transfer-Encoding", equalTo("chunked")));
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldFailToConnectToUnregisteredHosts() {
    builder.build().resource("http://example.com:666/something").get(ClientResponse.class);
  }

  @Test
  public void shouldRetryForRecoverableStatus() {
    final JerseyClientConfiguration jerseyClientConfiguration = new JerseyClientConfiguration();
    jerseyClientConfiguration.setTimeout(Duration.milliseconds(200));

    HostAndPort wmNode1 = HostAndPort.fromParts("localhost", wm.port());
    HostAndPort wmNode2 = HostAndPort.fromParts("localhost", wm2.port());

    final EndpointConfiguration endpointConfiguration =
        new EndpointConfiguration(
            Optional.<String>absent(),
            Optional.of(jerseyClientConfiguration),
            Optional.<String>absent(),
            Lists.newArrayList(wmNode1.toString(), wmNode2.toString()),
            Lists.<String>newArrayList());

    Client client =
        ResilientClientBuilder.in(DummyClientEnvironment.inTesting())
            .using(endpointConfiguration)
            .withPrimary(new SimpleHostAndPortProvider(wmNode1, wmNode2))
            .withSecondary(new NullHostAndPortProvider())
            .build();

    wm.stubFor(post(urlEqualTo("/firstRequestFails")).willReturn(aResponse().withStatus(500)));

    wm2.stubFor(post(urlEqualTo("/firstRequestFails")).willReturn(aResponse().withStatus(200)));

    ClientResponse response =
        client
            .resource("http://localhost:" + wm.port() + "/firstRequestFails")
            .post(ClientResponse.class);

    assertThat(response.getStatus(), is(200));
  }

  @Test
  public void shouldRetryForHttpFaultResponses() {
    final JerseyClientConfiguration jerseyClientConfiguration = new JerseyClientConfiguration();
    jerseyClientConfiguration.setTimeout(Duration.milliseconds(200));

    HostAndPort wmNode1 = HostAndPort.fromParts("localhost", wm.port());
    HostAndPort wmNode2 = HostAndPort.fromParts("localhost", wm2.port());

    final EndpointConfiguration endpointConfiguration =
        new EndpointConfiguration(
            Optional.<String>absent(),
            Optional.of(jerseyClientConfiguration),
            Optional.<String>absent(),
            Lists.newArrayList(wmNode1.toString(), wmNode2.toString()),
            Lists.<String>newArrayList());

    Client client =
        ResilientClientBuilder.in(DummyClientEnvironment.inTesting())
            .using(endpointConfiguration)
            .withPrimary(new SimpleHostAndPortProvider(wmNode1, wmNode2))
            .withSecondary(new NullHostAndPortProvider())
            .build();

    wm.stubFor(
        post(urlEqualTo("/firstRequestFails"))
            .willReturn(aResponse().withFault(MALFORMED_RESPONSE_CHUNK)));

    wm2.stubFor(post(urlEqualTo("/firstRequestFails")).willReturn(aResponse().withStatus(200)));

    ClientResponse response =
        client
            .resource("http://localhost:" + wm.port() + "/firstRequestFails")
            .post(ClientResponse.class);

    assertThat(response.getStatus(), is(200));
  }

  @Test
  public void shouldNotRetryNonIdempotentMethodByDefaultWhenRemoteStateUncertain() {

    final JerseyClientConfiguration jerseyClientConfiguration = new JerseyClientConfiguration();
    jerseyClientConfiguration.setTimeout(Duration.milliseconds(200));

    HostAndPort wmNode1 = HostAndPort.fromParts("localhost", delayedWm.port());
    HostAndPort wmNode2 = HostAndPort.fromParts("localhost", wm.port());

    final EndpointConfiguration endpointConfiguration =
        new EndpointConfiguration(
            Optional.<String>absent(),
            Optional.of(jerseyClientConfiguration),
            Optional.<String>absent(),
            Lists.newArrayList(wmNode1.toString()),
            Lists.<String>newArrayList());

    Client client =
        ResilientClientBuilder.in(DummyClientEnvironment.inTesting())
            .using(endpointConfiguration)
            .withPrimary(new SimpleHostAndPortProvider(wmNode1, wmNode2))
            .withSecondary(new NullHostAndPortProvider())
            .build();

    delayedWm.stubFor(post(urlEqualTo("/timeout/1")).willReturn(aResponse().withStatus(200)));
    wm.stubFor(post(urlEqualTo("/timeout/1")).willReturn(aResponse().withStatus(500)));

    Exception caught = null;
    try {
      client.resource("http://localhost:" + delayedWm.port() + "/timeout/1").post();

    } catch (ClientHandlerException e) {
      caught = e;
    }

    assertThat(
        "Expecting ClientHandlerException", caught, instanceOf(ClientHandlerException.class));
    assertThat(
        "Expecting SocketTimeoutException",
        caught.getCause(),
        instanceOf(SocketTimeoutException.class));

    // NB not possible to verify this because of the way WireMock implements timeout
    // delayedWm.verify(postRequestedFor(urlEqualTo("/timeout")));
    wm.verify(0, postRequestedFor(urlEqualTo("/timeout/1")));
  }

  @Test
  public void shouldRetryNonIdempotentMethodWhenRemoteStateUncertainIfConfigured() {
    final JerseyClientConfiguration jerseyClientConfiguration = new JerseyClientConfiguration();
    jerseyClientConfiguration.setTimeout(Duration.milliseconds(200));

    HostAndPort wmNode1 = HostAndPort.fromParts("localhost", delayedWm.port());
    HostAndPort wmNode2 = HostAndPort.fromParts("localhost", wm.port());

    final EndpointConfiguration endpointConfiguration =
        new EndpointConfiguration(
            Optional.<String>absent(),
            Optional.of(jerseyClientConfiguration),
            Optional.<String>absent(),
            Lists.newArrayList(wmNode1.toString()),
            Lists.<String>newArrayList());

    Client client =
        ResilientClientBuilder.in(DummyClientEnvironment.inTesting())
            .using(endpointConfiguration)
            .withPrimary(new SimpleHostAndPortProvider(wmNode1, wmNode2))
            .withSecondary(new NullHostAndPortProvider())
            .retryingNonIdempotentMethods(true)
            .build();

    delayedWm.stubFor(post(urlEqualTo("/timeout/2")).willReturn(aResponse().withStatus(200)));
    wm.stubFor(post(urlEqualTo("/timeout/2")).willReturn(aResponse().withStatus(201)));

    final ClientResponse response =
        client
            .resource("http://localhost:" + delayedWm.port() + "/timeout/2")
            .post(ClientResponse.class);

    assertThat(response.getStatus(), is(201));

    wm.verify(postRequestedFor(urlEqualTo("/timeout/2")));
  }

  @Test
  public void shouldRetryNonIdempotentMethodWhenRemoteStateCertain() throws Exception {

    final JerseyClientConfiguration jerseyClientConfiguration = new JerseyClientConfiguration();

    HostAndPort noNode = HostAndPort.fromParts("localhost", 7); // should be no service on this port
    HostAndPort wmNode1 = HostAndPort.fromParts("localhost", wm.port());

    final EndpointConfiguration endpointConfiguration =
        new EndpointConfiguration(
            Optional.<String>absent(),
            Optional.of(jerseyClientConfiguration),
            Optional.<String>absent(),
            Lists.newArrayList(noNode.toString()),
            Lists.<String>newArrayList());

    Client client =
        ResilientClientBuilder.in(DummyClientEnvironment.inTesting())
            .using(endpointConfiguration)
            .withPrimary(new SimpleHostAndPortProvider(noNode, wmNode1))
            .withSecondary(new NullHostAndPortProvider())
            .retryingNonIdempotentMethods(false)
            .build();

    wm.stubFor(post(urlEqualTo("/timeout/3")).willReturn(aResponse().withStatus(201)));

    final ClientResponse response =
        client.resource("http://localhost:7/timeout/3").post(ClientResponse.class);

    assertThat(response.getStatus(), is(201));

    wm.verify(postRequestedFor(urlEqualTo("/timeout/3")));
  }

  @Test
  public void shouldHonourLargeTimeOuts() {
    final JerseyClientConfiguration jerseyClientConfiguration = new JerseyClientConfiguration();
    jerseyClientConfiguration.setTimeout(Duration.seconds(16));

    HostAndPort wmNode1_delayed = HostAndPort.fromParts("localhost", delayedWm.port());
    HostAndPort wmNode2 = HostAndPort.fromParts("localhost", wm.port());

    final EndpointConfiguration endpointConfiguration =
        new EndpointConfiguration(
            Optional.<String>absent(),
            Optional.of(jerseyClientConfiguration),
            Optional.<String>absent(),
            Lists.newArrayList(wmNode1_delayed.toString()),
            Lists.<String>newArrayList());

    Client client =
        ResilientClientBuilder.in(DummyClientEnvironment.inTesting())
            .using(endpointConfiguration)
            .withPrimary(new SimpleHostAndPortProvider(wmNode1_delayed, wmNode2))
            .withSecondary(new NullHostAndPortProvider())
            .retryingNonIdempotentMethods(true)
            .build();

    delayedWm.stubFor(
        post(urlEqualTo("/timeout/4"))
            .willReturn(aResponse().withStatus(200).withFixedDelay(14500)));
    wm.stubFor(post(urlEqualTo("/timeout/4")).willReturn(aResponse().withStatus(200)));

    final ClientResponse response =
        client
            .resource("http://localhost:" + delayedWm.port() + "/timeout/4")
            .post(ClientResponse.class);

    assertThat(response.getStatus(), is(200));

    delayedWm.verify(1, postRequestedFor(urlEqualTo("/timeout/4")));
    wm.verify(0, postRequestedFor(urlEqualTo("/timeout/4")));
  }

  private void testSupportFor(RequestMethod method) {
    stubWillReturn(
        method, aResponse().withStatus(200).withHeader("X-Request-Header", "req-header-value"));

    ClientResponse response = handle(method);

    assertThat(response.getStatus(), is(200));
    assertThat(response.getHeaders().getFirst("X-Request-Header"), is("req-header-value"));
  }

  private void testSupportFor(RequestMethod method, String body) {
    stubWillReturn(
        method, aResponse().withStatus(200).withHeader("X-Request-Header", "req-header-value"));

    ClientResponse response = handleWithBody(method, body);

    assertThat(response.getStatus(), is(200));
    assertThat(response.getHeaders().getFirst("X-Request-Header"), is("req-header-value"));

    List<LoggedRequest> requests =
        wm.findAll(new RequestPatternBuilder(ANY, urlEqualTo("/something")));
    assertThat(requests.get(0).getBodyAsString(), is(body));
  }

  private ClientResponse handle(RequestMethod method) {
    Client client = builder.build();
    switch (method) {
      case GET:
        return resource(client).get(ClientResponse.class);
      case POST:
        return resource(client).post(ClientResponse.class);
      case PUT:
        return resource(client).put(ClientResponse.class);
      case DELETE:
        return resource(client).delete(ClientResponse.class);
      case HEAD:
        return resource(client).head();
      case OPTIONS:
        return resource(client).options(ClientResponse.class);
      default:
        throw new IllegalArgumentException("Unable to execute " + method);
    }
  }

  private ClientResponse handleWithBody(RequestMethod method, String body) {
    Client client = builder.build();
    switch (method) {
      case POST:
        return resource(client).post(ClientResponse.class, body);
      case PUT:
        return resource(client).put(ClientResponse.class, body);
      default:
        throw new IllegalArgumentException("Unable to execute " + method);
    }
  }

  private void stubGetWillReturn(ResponseDefinitionBuilder response) {
    stubWillReturn(GET, response);
  }

  private WebResource resource(Client client) {

    URI someUri =
        UriBuilder.fromPath("/something").scheme("http").port(wm.port()).host("localhost").build();

    return client.resource(someUri);
  }

  private void stubWillReturn(RequestMethod method, ResponseDefinitionBuilder response) {
    wm.stubFor(new MappingBuilder(method, urlEqualTo("/something")).willReturn(response));
  }
}
