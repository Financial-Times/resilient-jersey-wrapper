package com.ft.jerseyhttpwrapper.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import java.util.Objects;

public class SimpleEndpointConfiguration {
  private final String protocol;
  private final String host;
  private final int port;
  private final int adminPort;

  public SimpleEndpointConfiguration(
      @JsonProperty("protocol") String protocol,
      @JsonProperty("host") String host,
      @JsonProperty("port") int port,
      @JsonProperty("adminPort") int adminPort) {
    this.protocol = protocol;
    this.host = host;
    this.port = port;
    this.adminPort = adminPort;
  }

  public String getProtocol() {
    return protocol;
  }

  public String getHost() {
    return host;
  }

  public int getAdminPort() {
    return adminPort;
  }

  public int getPort() {
    return port;
  }

  public String getHostAndPorts() {
    return host + ":" + port + ":" + adminPort;
  }

  protected MoreObjects.ToStringHelper toStringHelper() {
    return MoreObjects.toStringHelper(this)
        .add("protocol", protocol)
        .add("host", host)
        .add("port", port)
        .add("adminPort", adminPort);
  }

  @Override
  public String toString() {
    return toStringHelper().toString();
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this) return true;
    if (obj instanceof SimpleEndpointConfiguration) {
      SimpleEndpointConfiguration that = (SimpleEndpointConfiguration) obj;
      return Objects.equals(this.protocol, that.protocol)
          && Objects.equals(this.host, that.host)
          && Objects.equals(this.port, that.port)
          && Objects.equals(this.adminPort, that.adminPort);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(protocol, host, port, adminPort);
  }
}
