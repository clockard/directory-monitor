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
package org.crl.monitor.directorymonitor.processor;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.crl.monitor.directorymonitor.Processor;

public class DefaultProcessor implements Processor {
  int createOpCount = 0;
  int updateOpCount = 0;
  int deleteOpCount = 0;

  @Override
  public String getId() {
    return "Default";
  }

  @Override
  public String processCreate(File file) {
    createOpCount++;
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-1");
      return convertByteArrayToHexString(
          digest.digest(file.getAbsolutePath().getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      // ignore
    }
    return null;
  }

  @Override
  public void processUpdate(File file, String altId) {
    updateOpCount++;
  }

  @Override
  public void processDelete(File file, String altId) {
    deleteOpCount++;
  }

  private static String convertByteArrayToHexString(byte[] arrayBytes) {
    StringBuilder stringBuffer = new StringBuilder();
    for (int i = 0; i < arrayBytes.length; i++) {
      stringBuffer.append(Integer.toString((arrayBytes[i] & 0xff) + 0x100, 16).substring(1));
    }
    return stringBuffer.toString();
  }
}
