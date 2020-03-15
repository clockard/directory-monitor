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
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DirectoryMonitor {

  private static final Logger LOGGER = LoggerFactory.getLogger(DirectoryMonitor.class);

  private String monitoredDirectory;

  private String fileRegEx;

  private long checkPeriod;

  private long stabilityPeriod;

  private FileAlterationMonitor monitor;

  private FileItemRepository fileItemRepository;

  private Processor processor;

  private ScheduledExecutorService executorService;

  public DirectoryMonitor(
      FileItemRepository fileItemRepository,
      String monitoredDirectory,
      String fileRegEx,
      long checkPeriod,
      long stabilityPeriod,
      Processor processor) {
    this.fileItemRepository = fileItemRepository;
    this.monitoredDirectory = monitoredDirectory;
    this.fileRegEx = fileRegEx;
    this.checkPeriod = checkPeriod;
    this.stabilityPeriod = stabilityPeriod;
    this.processor = processor;
    this.executorService = Executors.newSingleThreadScheduledExecutor();
  }

  public void init() throws Exception {
    final File directory = new File(this.monitoredDirectory);
    if (!directory.exists()) {
      throw new IllegalStateException(
          "Monitored directory does not exist: " + directory.getAbsolutePath());
    }
    LOGGER.info("Monitoring directory: {}", directory.getCanonicalPath());
    FileAlterationObserver fao = new SafeFileAlterationObserver(directory, this::fileMatchesFilter);
    fao.addListener(
        new FileAlterationListenerAdaptor() {
          long start = 0;

          @Override
          public void onStart(FileAlterationObserver observer) {
            start = System.currentTimeMillis();
          }

          @Override
          public void onFileCreate(File file) {
            notifyCreate(file);
          }

          @Override
          public void onFileChange(File file) {
            notifyUpdate(file);
          }

          @Override
          public void onFileDelete(File file) {
            notifyDelete(file);
          }

          @Override
          public void onStop(FileAlterationObserver observer) {
            LOGGER.info(
                "Directory scan took {} seconds", (System.currentTimeMillis() - start) / 1000f);
          }
        });
    monitor = new FileAlterationMonitor(checkPeriod);
    monitor.addObserver(fao);
    LOGGER.info("Starting monitor. Checking every {} MS", checkPeriod);
    monitor.start();

    executorService.schedule(this::checkForChangesSinceLastRun, 10, TimeUnit.SECONDS);
    executorService.scheduleAtFixedRate(this::retryUnprocessed, 120, 120, TimeUnit.SECONDS);
  }

  private boolean fileMatchesFilter(File file) {
    return fileMatchesFilter(file, true);
  }

  private boolean fileMatchesFilter(File file, boolean includeDirs) {
    String name = file.getName();
    return (includeDirs || !file.isDirectory()) && !name.startsWith(".") && name.matches(fileRegEx);
  }

  private synchronized void notifyCreate(File file) {
    waitForFileToStabilize(file);
    FileItem item = fileItemRepository.findByFileId(file.getAbsolutePath());
    if (item != null && item.getStatus() != Status.UNPROCESSED) {
      return;
    }
    LOGGER.info("Notify file created: {}", file.getAbsolutePath());
    String altId = null;
    Status status = Status.UNPROCESSED;
    try {
      altId = processor.processCreate(file);
      status = Status.PROCESSED;
    } catch (Exception e) {
      LOGGER.warn("Error processing create event", e);
    }
    if (item == null) {
      item = new FileItem(file.getAbsolutePath(), altId, file.lastModified(), status);
    } else {
      item.setAltFileId(altId);
      item.setModified(file.lastModified());
      item.setStatus(status);
    }
    fileItemRepository.save(item);
  }

  private synchronized void notifyUpdate(File file) {
    waitForFileToStabilize(file);
    FileItem item = fileItemRepository.findByFileId(file.getAbsolutePath());
    if (item != null) {
      if (item.getModified() >= file.lastModified()
          && item.getStatus() != Status.UNPROCESSED_UPDATE) {
        return;
      }
      LOGGER.info("Notify file updated: {}", file.getAbsolutePath());
      try {
        processor.processUpdate(file, item.getAltFileId());
        item.setModified(file.lastModified());
        item.setStatus(Status.PROCESSED);
      } catch (Exception e) {
        LOGGER.warn("Error processing update event", e);
        item.setStatus(Status.UNPROCESSED_UPDATE);
      }

      fileItemRepository.save(item);
    } else {
      notifyCreate(file);
    }
  }

  private synchronized void notifyDelete(File file) {

    FileItem item = fileItemRepository.findByFileId(file.getAbsolutePath());
    if (item == null) {
      // never processed. noop
      return;
    }
    if (item.getStatus() == Status.UNPROCESSED) {
      // item never successfully processed so just remove it from the db
      fileItemRepository.deleteByFileId(file.getAbsolutePath());
    }
    LOGGER.info("Notify file deleted: {}", file.getAbsolutePath());
    try {
      processor.processDelete(file, item.getAltFileId());
      fileItemRepository.deleteByFileId(file.getAbsolutePath());
    } catch (Exception e) {
      LOGGER.warn("Error processing delete event", e);
      item.setStatus(Status.UNPROCESSED_DELETE);
      fileItemRepository.save(item);
    }
  }

  private void waitForFileToStabilize(File file) {
    while (System.currentTimeMillis() - file.lastModified() < stabilityPeriod) {
      try {
        Thread.sleep(stabilityPeriod);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private void checkForChangesSinceLastRun() {
    checkForNewFiles();
    checkForDeletedFiles();
  }

  private void retryUnprocessed() {
    long start = System.currentTimeMillis();
    fileItemRepository.findByStatus(Status.UNPROCESSED).stream()
        .map(item -> new File(item.getFileId()))
        .filter(File::exists)
        .forEach(this::notifyCreate);
    fileItemRepository.findByStatus(Status.UNPROCESSED_UPDATE).stream()
        .map(item -> new File(item.getFileId()))
        .filter(File::exists)
        .forEach(this::notifyUpdate);
    fileItemRepository.findByStatus(Status.UNPROCESSED_DELETE).stream()
        .map(item -> new File(item.getFileId()))
        .forEach(this::notifyDelete);
    LOGGER.info("retryUnprocessed took {} seconds", (System.currentTimeMillis() - start) / 1000f);
  }

  private void checkForNewFiles() {
    long start = System.currentTimeMillis();
    Map<String, Long> knownFiles = new HashMap<>((int) fileItemRepository.count());
    fileItemRepository
        .findAll()
        .forEach(item -> knownFiles.put(item.getFileId(), item.getModified()));
    try (Stream<Path> fileStream =
        Files.walk(Paths.get(monitoredDirectory), FileVisitOption.FOLLOW_LINKS)) {
      fileStream
          .filter(t -> fileMatchesFilter(t.toFile(), false))
          .forEach(
              path -> {
                File file = path.toFile();
                long modified = knownFiles.getOrDefault(file.getAbsolutePath(), 0L);
                if (modified == 0) {
                  notifyCreate(file);
                } else if (file.lastModified() > modified) {
                  notifyUpdate(file);
                }
              });
    } catch (IOException e) {
      LOGGER.warn("Error encountered while checking for new files.", e);
    }
    LOGGER.info("New file check took {} seconds", (System.currentTimeMillis() - start) / 1000f);
  }

  private void checkForDeletedFiles() {
    long start = System.currentTimeMillis();
    StreamSupport.stream(fileItemRepository.findAll().spliterator(), false)
        .map(item -> new File(item.getFileId()))
        .filter(file -> !file.exists())
        .forEach(this::notifyDelete);
    LOGGER.info(
        "Deleted files check took {} seconds", (System.currentTimeMillis() - start) / 1000f);
  }

  public void destroy() throws Exception {
    LOGGER.info("Shutting down...");
    executorService.shutdownNow();
    monitor.stop(1000);
  }
}
