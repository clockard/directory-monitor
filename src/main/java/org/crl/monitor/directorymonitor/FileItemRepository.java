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
import javax.transaction.Transactional;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface FileItemRepository extends PagingAndSortingRepository<FileItem, String> {
  FileItem findByFileId(String id);

  List<FileItem> findByStatus(Status status);

  @Transactional
  void deleteByFileId(String id);
}
