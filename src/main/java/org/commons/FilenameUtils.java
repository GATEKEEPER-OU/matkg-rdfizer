package org.commons;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Riccardo Pala (riccardo.pala@open.ac.uk)
 * @todo description
 */
public class FilenameUtils {

  /** Timestamp separator */
  public static final String TS_SEP = "__";

  /** File extension separator */
  public static final String EXT_SEP = ".";

  /**
   *
   * @todo description
   */
  public static String nameWithTimestamp(String prefix, String ext) {
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss-n");
    LocalDateTime now = LocalDateTime.now();
    String timestamp = dateTimeFormatter.format(now);
    return prefix + TS_SEP + timestamp + EXT_SEP + ext;
  }

  /**
   *
   * @todo description
   */
  public static String trimExtension(String filename) {
    String[] parts = filename.split(Pattern.quote(EXT_SEP));
    // TODO test with
    // - filename = null
    // - filename = ""
    // etc.
    return parts[0];
  }

  /**
   *
   * @todo description
   */
  public static String trim2LvlExtension(String filename) {
    String[] parts = filename.split(Pattern.quote(EXT_SEP));
    int nParts = parts.length;
    if(nParts == 2 ) return filename; // @todo review this code
    if (nParts < 2) throw new IllegalArgumentException("The given filename doesn't contain a 2 lvl extension.");
    parts[nParts - 2] = null;
    return Arrays.stream(parts)
      .filter(Objects::nonNull)
      .collect(Collectors.joining(EXT_SEP));
  }

  /**
   * @todo description
   */
  public static String changeExtension(String filename, String newExt) {
    int indexOfExtSep = filename.lastIndexOf(EXT_SEP);
    String nameWithoutExt = filename.substring(0, indexOfExtSep);
    return nameWithoutExt + EXT_SEP + newExt;
  }

  //--------------------------------------------------------------------------//
  // Class definition
  //--------------------------------------------------------------------------//

  /**
   * This class is not instantiable
   */
  private FilenameUtils() {
  }

}