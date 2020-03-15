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
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.comparator.NameFileComparator;
import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.apache.commons.io.monitor.FileEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wholesale copy of the FileAlterationObserver class. SafeFileAlterationObserver changes
 * checkAndNotify() method to not fire remove notification for all monitored files when the root
 * directory becomes unavailable. Instead it just simply stops monitoring until the root directory
 * becomes available again.
 */
public class SafeFileAlterationObserver extends FileAlterationObserver {

  private static final long serialVersionUID = 1185122225658782848L;

  private static final Logger LOGGER = LoggerFactory.getLogger(SafeFileAlterationObserver.class);

  static final FileEntry[] EMPTY_ENTRIES = new FileEntry[0];
  private final transient List<FileAlterationListener> listeners = new CopyOnWriteArrayList<>();
  private final FileEntry rootEntry;
  private final transient FileFilter fileFilter;
  private final transient Comparator<File> comparator;

  /**
   * Construct an observer for the specified directory and file filter.
   *
   * @param directory the directory to observe
   * @param fileFilter The file filter or null if none
   */
  public SafeFileAlterationObserver(final File directory, final FileFilter fileFilter) {
    super(directory);
    this.rootEntry = new FileEntry(directory);
    this.fileFilter = fileFilter;
    this.comparator = NameFileComparator.NAME_SYSTEM_COMPARATOR;
  }

  /**
   * Return the directory being observed.
   *
   * @return the directory being observed
   */
  @Override
  public File getDirectory() {
    return rootEntry.getFile();
  }

  /**
   * Return the fileFilter.
   *
   * @return the fileFilter
   * @since 2.1
   */
  @Override
  public FileFilter getFileFilter() {
    return fileFilter;
  }

  /**
   * Add a file system listener.
   *
   * @param listener The file system listener
   */
  @Override
  public void addListener(final FileAlterationListener listener) {
    if (listener != null) {
      listeners.add(listener);
    }
  }

  /**
   * Remove a file system listener.
   *
   * @param listener The file system listener
   */
  @Override
  public void removeListener(final FileAlterationListener listener) {
    if (listener != null) {
      while (listeners.remove(listener)) {}
    }
  }

  /**
   * Returns the set of registered file system listeners.
   *
   * @return The file system listeners
   */
  @Override
  public Iterable<FileAlterationListener> getListeners() {
    return listeners;
  }

  /**
   * Initialize the observer.
   *
   * @throws Exception if an error occurs
   */
  @Override
  public void initialize() throws Exception {
    rootEntry.refresh(rootEntry.getFile());
    final FileEntry[] children = doListFiles(rootEntry.getFile(), rootEntry);
    rootEntry.setChildren(children);
  }

  /** Check whether the file and its children have been created, modified or deleted. */
  @Override
  public void checkAndNotify() {

    /* fire onStart() */
    for (final FileAlterationListener listener : listeners) {
      listener.onStart(this);
    }

    /* fire directory/file events */
    final File rootFile = rootEntry.getFile();
    if (rootFile.exists() && rootFile.canRead()) {
      if (!rootEntry.isExists()) {
        LOGGER.info(
            "Monitored directory [{}] has become available. Resuming monitoring.",
            rootFile.getAbsolutePath());
        rootEntry.setExists(true);
        try {
          initialize();
        } catch (Exception e) {
          LOGGER.error("Could not initialize monitored directory after it became available", e);
        }
      }
      checkAndNotify(rootEntry, rootEntry.getChildren(), listFiles(rootFile));
    } else if (rootEntry.isExists()) {
      LOGGER.warn(
          "Monitored directory [{}] no longer available. Suspending monitoring until directory becomes available",
          rootFile.getAbsolutePath());
      rootEntry.setExists(false);
    }

    /* fire onStop() */
    for (final FileAlterationListener listener : listeners) {
      listener.onStop(this);
    }
  }

  /**
   * Compare two file lists for files which have been created, modified or deleted.
   *
   * @param parent The parent entry
   * @param previous The original list of files
   * @param files The current list of files
   */
  private void checkAndNotify(
      final FileEntry parent, final FileEntry[] previous, final File[] files) {
    int c = 0;
    final FileEntry[] current = files.length > 0 ? new FileEntry[files.length] : EMPTY_ENTRIES;
    for (final FileEntry entry : previous) {
      while (c < files.length && comparator.compare(entry.getFile(), files[c]) > 0) {
        current[c] = createFileEntry(parent, files[c]);
        doCreate(current[c]);
        c++;
      }
      if (c < files.length && comparator.compare(entry.getFile(), files[c]) == 0) {
        doMatch(entry, files[c]);
        checkAndNotify(entry, entry.getChildren(), listFiles(files[c]));
        current[c] = entry;
        c++;
      } else {
        checkAndNotify(entry, entry.getChildren(), FileUtils.EMPTY_FILE_ARRAY);
        doDelete(entry);
      }
    }
    for (; c < files.length; c++) {
      current[c] = createFileEntry(parent, files[c]);
      doCreate(current[c]);
    }
    parent.setChildren(current);
  }

  /**
   * Create a new file entry for the specified file.
   *
   * @param parent The parent file entry
   * @param file The file to create an entry for
   * @return A new file entry
   */
  private FileEntry createFileEntry(final FileEntry parent, final File file) {
    final FileEntry entry = parent.newChildInstance(file);
    entry.refresh(file);
    final FileEntry[] children = doListFiles(file, entry);
    entry.setChildren(children);
    return entry;
  }

  /**
   * List the files
   *
   * @param file The file to list files for
   * @param entry the parent entry
   * @return The child files
   */
  private FileEntry[] doListFiles(final File file, final FileEntry entry) {
    final File[] files = listFiles(file);
    final FileEntry[] children = files.length > 0 ? new FileEntry[files.length] : EMPTY_ENTRIES;
    for (int i = 0; i < files.length; i++) {
      children[i] = createFileEntry(entry, files[i]);
    }
    return children;
  }

  /**
   * Fire directory/file created events to the registered listeners.
   *
   * @param entry The file entry
   */
  private void doCreate(final FileEntry entry) {
    for (final FileAlterationListener listener : listeners) {
      if (entry.isDirectory()) {
        listener.onDirectoryCreate(entry.getFile());
      } else {
        listener.onFileCreate(entry.getFile());
      }
    }
    final FileEntry[] children = entry.getChildren();
    for (final FileEntry aChildren : children) {
      doCreate(aChildren);
    }
  }

  /**
   * Fire directory/file change events to the registered listeners.
   *
   * @param entry The previous file system entry
   * @param file The current file
   */
  private void doMatch(final FileEntry entry, final File file) {
    if (entry.refresh(file)) {
      for (final FileAlterationListener listener : listeners) {
        if (entry.isDirectory()) {
          listener.onDirectoryChange(file);
        } else {
          listener.onFileChange(file);
        }
      }
    }
  }

  /**
   * Fire directory/file delete events to the registered listeners.
   *
   * @param entry The file entry
   */
  private void doDelete(final FileEntry entry) {
    for (final FileAlterationListener listener : listeners) {
      if (entry.isDirectory()) {
        listener.onDirectoryDelete(entry.getFile());
      } else {
        listener.onFileDelete(entry.getFile());
      }
    }
  }

  /**
   * List the contents of a directory
   *
   * @param file The file to list the contents of
   * @return the directory contents or a zero length array if the empty or the file is not a
   *     directory
   */
  private File[] listFiles(final File file) {
    File[] children = null;
    if (file.isDirectory()) {
      children = fileFilter == null ? file.listFiles() : file.listFiles(fileFilter);
    }
    if (children == null) {
      children = FileUtils.EMPTY_FILE_ARRAY;
    }
    if (comparator != null && children.length > 1) {
      Arrays.sort(children, comparator);
    }
    return children;
  }

  /**
   * Provide a String representation of this observer.
   *
   * @return a String representation of this observer
   */
  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder();
    builder.append(getClass().getSimpleName());
    builder.append("[file='");
    builder.append(getDirectory().getPath());
    builder.append('\'');
    if (fileFilter != null) {
      builder.append(", ");
      builder.append(fileFilter.toString());
    }
    builder.append(", listeners=");
    builder.append(listeners.size());
    builder.append("]");
    return builder.toString();
  }
}
