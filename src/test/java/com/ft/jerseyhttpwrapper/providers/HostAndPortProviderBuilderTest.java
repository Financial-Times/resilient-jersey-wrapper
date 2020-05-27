package com.ft.jerseyhttpwrapper.providers;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;

import com.ft.jerseyhttpwrapper.ResilienceStrategy;
import com.ft.jerseyhttpwrapper.config.SimpleEndpointConfiguration;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HostAndPortProviderBuilderTest {

  public static final SimpleEndpointConfiguration HOST_A =
      new SimpleEndpointConfiguration("http", "a", 80, 8080);

  @Mock private HostAndPortIpResolver resolver;

  @Test
  public void empty_host_config_should_result_in_null_provider() throws Exception {
    final HostAndPortProvider hostAndPortProvider =
        new HostAndPortProviderBuilder(resolver)
            .withStrategy(ResilienceStrategy.LOAD_BALANCED_STRATEGY)
            .withSimpleEndpointConfiguration(Collections.<SimpleEndpointConfiguration>emptyList())
            .build();

    assertThat(hostAndPortProvider, instanceOf(NullHostAndPortProvider.class));
  }

  @Test
  public void should_build_null_host_and_port_provider_for_empty_strategy() throws Exception {
    final HostAndPortProvider hostAndPortProvider =
        new HostAndPortProviderBuilder(resolver)
            .withStrategy(ResilienceStrategy.EMPTY_STRATEGY)
            .withSimpleEndpointConfiguration(Arrays.asList(HOST_A))
            .build();

    assertThat(hostAndPortProvider, instanceOf(NullHostAndPortProvider.class));
  }

  @Test
  public void should_build_simple_host_and_port_provider_for_simple_failover_strategy()
      throws Exception {
    final HostAndPortProvider hostAndPortProvider =
        new HostAndPortProviderBuilder(resolver)
            .withStrategy(ResilienceStrategy.SIMPLE_FAILOVER_STRATEGY)
            .withSimpleEndpointConfiguration(Arrays.asList(HOST_A))
            .build();

    assertThat(hostAndPortProvider, instanceOf(SimpleHostAndPortProvider.class));
  }

  @Test
  public void should_build_random_host_and_port_provider_for_load_balanced_strategy()
      throws Exception {
    final HostAndPortProvider hostAndPortProvider =
        new HostAndPortProviderBuilder(resolver)
            .withStrategy(ResilienceStrategy.LOAD_BALANCED_STRATEGY)
            .withSimpleEndpointConfiguration(Arrays.asList(HOST_A))
            .build();

    assertThat(hostAndPortProvider, instanceOf(RandomHostAndPortProvider.class));
  }

  @Test
  public void should_build_random_ip_host_and_port_provider_for_load_balanced_ip_strategy()
      throws Exception {
    final HostAndPortProvider hostAndPortProvider =
        new HostAndPortProviderBuilder(resolver)
            .withStrategy(ResilienceStrategy.LOAD_BALANCED_IP_STRATEGY)
            .withSimpleEndpointConfiguration(Arrays.asList(HOST_A))
            .build();

    assertThat(hostAndPortProvider, instanceOf(RandomIpHostAndPortProvider.class));
  }

  @Test
  public void default_strategy_should_be_load_balanced() throws Exception {
    final HostAndPortProvider hostAndPortProvider =
        new HostAndPortProviderBuilder(resolver)
            .withSimpleEndpointConfiguration(Arrays.asList(HOST_A))
            .build();

    assertThat(hostAndPortProvider, instanceOf(RandomHostAndPortProvider.class));
  }
}
