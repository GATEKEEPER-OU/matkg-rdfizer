package org.ou.gatekeeper.rdf.strategies;

import java.io.File;

public class FileSaver implements OutputSaver {

  public FileSaver(File output) {
    this.output = output;
  }

  @Override
  public void save(File content) {
    content.renameTo(output);
  }

  @Override
  public void close() {
    // DO NOTHING
  }

  private final File output;

}