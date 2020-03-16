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

import javax.persistence.*;

@Entity
public class FileItem {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private long id;

  private String fileId;

  private String altFileId;

  private long modified;

  private Status status;

  public FileItem() {}

  public FileItem(String fileId, String altFileId, long modified, Status status) {
    this.fileId = fileId;
    this.altFileId = altFileId;
    this.modified = modified;
    this.status = status;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public void setFileId(String fileId) {
    this.fileId = fileId;
  }

  public String getFileId() {
    return fileId;
  }

  public String getAltFileId() {
    return altFileId;
  }

  public void setAltFileId(String altId) {
    this.altFileId = altId;
  }

  public long getModified() {
    return modified;
  }

  public void setModified(long modified) {
    this.modified = modified;
  }

  public Status getStatus() {
    return this.status;
  }

  public void setStatus(Status newStatus) {
    this.status = newStatus;
  }

  @Override
  public String toString() {
    return "FileItem{"
        + "id='"
        + id
        + '\''
        + ", fileId='"
        + fileId
        + '\''
        + ", altFileId='"
        + altFileId
        + '\''
        + ", modified="
        + modified
        + ", status="
        + status
        + '}';
  }
}
