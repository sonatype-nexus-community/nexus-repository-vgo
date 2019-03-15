package org.sonatype.repository.vgo.internal.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VgoInfo
{
  @JsonProperty(value = "Version")
  private String version;

  @JsonProperty(value = "Time")
  private String time;

  public VgoInfo(final String version, final String time) {
    this.version = version;
    this.time = time;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(final String version) {
    this.version = version;
  }

  public String getTime() {
    return time;
  }

  public void setTime(final String time) {
    this.time = time;
  }
}
