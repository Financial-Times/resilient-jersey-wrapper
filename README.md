# Resilient Client

[![Circle CI](https://circleci.com/gh/Financial-Times/resilient-jersey-wrapper/tree/master.png?style=shield)](https://circleci.com/gh/Financial-Times/resilient-jersey-wrapper/tree/master)

An implementation of the Jersey HTTP client API using the supplied Apache HttpClient implementation libraries.

This client adds the following resilience features to the standard feature set:

* failover of requests to nodes within a cluster
* random assignment of load to configured nodes (load balancing)
* configuration of available nodes (XOR import from DNS)
* control over the degree and timing of continued retry attempts
* extension points for these features

By default the client balances requests across nodes in a primary, followed by a secondary group. Requests are
retried when it is safe to do so and the default policy is to retry immediately on the next available host until all
hosts have been tried once.

An exponential back-off policy is available which permits for retry counts different from the number of available nodes.

# Static Endpoint Configuration

        Client client = ResilientClientBuilder.in(environment)
                        .using(configuration.getVarnish())
                        .build();:
        shortName: "methode"
        jerseyClient:
            timeout: 5000ms
        primaryNodes: ["localhost:9080:9081", "localhost:9080:9081"]
        secondaryNodes: null

The YAML above can be read into an EndpointConfiguration object by the Jackson YAML parser (or automatically by DropWizard)

    Client client = ResilientClientBuilder.in(environment)
                    .using(configuration.getEndpointConfiguration())
                    .build();

If you attempt to access a host that is not listed in the primary or secondary nodes collection then yu will get an
error as the configuration cannot blend load balancing approaches.

## Using one DNS entry per datacentre

If the hostnames listed in your EndpointConfiguration correspond to clusters of hosts (e.g. in two or more data centres)
then you can round off the configuration by having the client resolve all the additional host IP addresses from DNS:

     endpointConfiguration:
         shortName: "methode"
         jerseyClient:
             timeout: 5000ms
         primaryNodes: ["dc1.example.com:9080:9081", "dc2.example.com:9080:9081"]
         secondaryNodes: ["dc2.example.com:9080:9081"]
         resilienceStrategy: LOAD_BALANCED_IP_STRATEGY

This will add the IP addresses to the pool of nodes before load balancing randomly.

# Dynamic DNS Driven configuration

    Client client = ResilientClientBuilder.in(environment)
                    .usingDNS()
                    .named("name")
                    .build()

This sets up the client without any fixed connection. The nodes are produced by resolving the DNS entry for whatever host
is requested via the Jersey API.

# Exponential Backoff and Retry

ContinuationPolicy objects encapsulate workflow logic that controls whether and when the transaction continues to be
attempted. In this example, the client continues to retry 5 times with a short initial delay.

    ContinuationPolicy fiveQuickAttempts =  new ExponentialBackoffContinuationPolicy(5,500);

    Client client = ResilientClientBuilder.in(environment).usingDNS()
                .named("name")
                .withContinuationPolicy(fiveQuickAttempts)
                .build();

The 500 millisecond delay results in the following schedule of attempts.

1. Immediate
1. after 500 milliseconds
1. 1500 milliseconds
1. 3500 milliseconds
1. 7500 milliseconds

The first attempt is immediate in every case, the formula used for the remaining intervals is (in [Tex](http://bit.ly/1v4Biff)):

    interval = initialDelay*2^{attempt-1}

In the table above, the interval is added to the total elapsed time. In real life, the schedule will be delayed by the
cumulative time taken for attempts to fail.

# MDC -> User-Agent transaction ID forwarding

Access logs often make no accommodation for transaction_id, so Resilient Client, by default, encodes a `transaction_id` into the
`User-Agent` header.

You can pull this back out again using the Splunk `rex` command:

    source=/var/log*transformer*dw-access.log useragent="Resilient Client*"
       | rex field=useragent "transaction_id=(?<transaction_id>[^\)]+)\)"
       | table useragent transaction_id

Where does the transaction ID come from? The ResilientClient object has a default policy of reading this field from the
[Mapped Diagnostic Context](http://logback.qos.ch/manual/mdc.html) feature of SL4J. The key consulted is "transaction_id"
and this is expected to contain e.g. "transaction_id=xxxx", that is, the value contains the key. This is consistent with
[internal FT tools](http://git.svc.ft.com:8080/projects/APILIBS/repos/jax-rs-transaction-id-handling/browse/src/main/java/com/ft/api/util/transactionid/TransactionIdFilter.java).

This behaviour is implemented as an embedded Guava [Supplier](http://docs.guava-libraries.googlecode.com/git/javadoc/com/google/common/base/Supplier.html)
which returns the entire User-Agent string.

You can override this behaviour as follows:

    client.setUserAgentSupplier(Suppliers.ofInstance("My User-Agent"));


# Known short comings

* The name is incorrect. Technically, this is not an API wrapper but a sub-type of the Jersey implementation.
* Port centric. To permit testing within the build the API makes host:port pairs first class citizens. This is
  occasionally inconvenient.
* DropWizard centric. The requirement to provide an "environment" is trivial on DropWizard but may be onerous on other platforms
* Default MDC to user agent mapping is FT centric.

## IP v4/6 inconsistency

In test runs on Windows machines the following stack trace can sometimes be observed:

    INFO  [2015-01-30 15:50:31,980] com.ft.jerseyhttpwrapper.ResilientClient: transaction_id=tid_kshspxsqdn [REQUEST STARTED] short_name=SemanticWriter [dw-153 - POST /ingest]
    WARN  [2015-01-30 15:50:31,981] com.ft.jerseyhttpwrapper.ResilientClient: transaction_id=tid_kshspxsqdn Unexpected error communicating with server. [dw-153 - POST /ingest]
    ! com.sun.jersey.api.client.ClientHandlerException: java.lang.IllegalArgumentException: Host name may not be null
    ! at com.sun.jersey.client.apache4.ApacheHttpClient4Handler.handle(ApacheHttpClient4Handler.java:187) ~[jersey-apache-client4-1.18.1.jar:1.18.1]
    ! at com.sun.jersey.api.client.filter.GZIPContentEncodingFilter.handle(GZIPContentEncodingFilter.java:120) ~[jersey-client-1.18.1.jar:1.18.1]
    ! at com.sun.jersey.api.client.Client.handle(Client.java:652) ~[jersey-client-1.18.1.jar:1.18.1]
    ! at com.ft.jerseyhttpwrapper.ResilientClient.handle(ResilientClient.java:134) ~[resilient-jersey-wrapper-0.3-SNAPSHOT.jar:na]
    ...
    ...
    ! at java.lang.Thread.run(Thread.java:724) ~[na:1.7.0_25]
    ! Caused by: java.lang.IllegalArgumentException: Host name may not be null
    ! at org.apache.http.HttpHost.<init>(HttpHost.java:79) ~[httpcore-4.2.2.jar:4.2.2]
    ! at com.sun.jersey.client.apache4.ApacheHttpClient4Handler.getHost(ApacheHttpClient4Handler.java:198) ~[jersey-apache-client4-1.18.1.jar:1.18.1]
    ! at com.sun.jersey.client.apache4.ApacheHttpClient4Handler.handle(ApacheHttpClient4Handler.java:167) ~[jersey-apache-client4-1.18.1.jar:1.18.1]
    ! ... 62 common frames omitted

To suppress this exception add the following system property:

    -Djava.net.preferIPv4Stack=true


