package org.commons;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Riccardo Pala (riccardo.pala@open.ac.uk)
 */
class FilenameUtilsTest {

  @Test
  void changeExtentions() {
    String filename = "afilename.old";
    String newExtension = "new";
    String renamedFilename = FilenameUtils
      .changeExtension(filename, newExtension);
    assertEquals("afilename.new", renamedFilename);
  }

  // @todo think to other possibilities of changeExtentions

}