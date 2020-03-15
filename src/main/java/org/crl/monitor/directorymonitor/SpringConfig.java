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

import java.util.List;
import org.crl.monitor.directorymonitor.processor.DefaultProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration("directorymonitor")
public class SpringConfig {

  @Bean(destroyMethod = "destroy", initMethod = "init")
  public DirectoryMonitor directoryMonitor(
      FileItemRepository fileItemRepository,
      ConfigProperties configProperties,
      List<Processor> processorList) {
    Processor processor =
        processorList.stream()
            .filter(p -> p.getId().equals(configProperties.getProcessorId()))
            .findFirst()
            .orElse(new DefaultProcessor());
    return new DirectoryMonitor(
        fileItemRepository,
        configProperties.getDir(),
        configProperties.getFileRegEx(),
        configProperties.getCheckPeriod(),
        configProperties.getStabilityPeriod(),
        processor);
  }

  @Bean
  public Processor defaultProcessor() {
    return new DefaultProcessor();
  }
}
