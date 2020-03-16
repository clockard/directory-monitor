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

import java.io.File;

public interface Processor {

  /**
   * Get the id of the processor
   *
   * @return the processor id
   */
  String getId();

  /**
   * Called when a new file is discovered in the monitored directory
   *
   * @param file The file that was discovered
   * @return returns an alternate id to associate with the file. This id will be handed back when
   *     processUpdate or processDelete is called. CAn be null.
   */
  String processCreate(File file);

  /**
   * Called when a monitored file is updated
   *
   * @param file The file that was updated
   * @param altId The alternate id that was returned from the processCreate method was call. Can be
   *     null.
   */
  void processUpdate(File file, String altId);

  /**
   * Called when a monitored file is deleted
   *
   * @param file The file that was deleted
   * @param altId The alternate id that was returned from the processCreate method was call. Can be
   *     null.
   */
  void processDelete(File file, String altId);
}
