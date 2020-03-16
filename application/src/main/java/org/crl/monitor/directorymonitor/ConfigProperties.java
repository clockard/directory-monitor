/**
 * Copyright (c) 2020 Chris Lockard
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.crl.monitor.directorymonitor;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "monitor")
public class ConfigProperties {

  private String dir;

  private String fileRegEx;

  private long checkPeriod;

  private long stabilityPeriod;

  private String processorId;

  public String getDir() {
    return dir;
  }

  public void setDir(String dir) {
    this.dir = dir;
  }

  public long getCheckPeriod() {
    if (checkPeriod <= 0) {
      return 10000L;
    }
    return checkPeriod;
  }

  public void setCheckPeriod(long checkPeriod) {
    this.checkPeriod = checkPeriod;
  }

  public String getFileRegEx() {
    if (fileRegEx == null) {
      return ".*";
    }
    return fileRegEx;
  }

  public void setFileRegEx(String fileRegEx) {
    this.fileRegEx = fileRegEx;
  }

  public String getProcessorId() {
    if (processorId == null) {
      return "Default";
    }
    return processorId;
  }

  public void setProcessorId(String processorId) {
    this.processorId = processorId;
  }

  public long getStabilityPeriod() {
    if (stabilityPeriod < 1000) {
      return 2000L;
    }
    return stabilityPeriod;
  }

  public void setStabilityPeriod(long stabilityPeriod) {
    this.stabilityPeriod = stabilityPeriod;
  }
}
