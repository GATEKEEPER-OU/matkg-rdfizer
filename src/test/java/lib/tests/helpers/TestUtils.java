package lib.tests.helpers;

import org.apache.commons.io.FileUtils;
import org.ou.gatekeeper.adapters.DataAdapter;
import org.ou.gatekeeper.adapters.css.CSSAdapter;
import org.ou.gatekeeper.adapters.fhir.FHIRAdapter;
import org.ou.gatekeeper.adapters.sh.SHAdapter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * TODO desc
 * @author Riccardo Pala (riccardo.pala@open.ac.uk)
 */
public class TestUtils {

  public static final String DATASET_PATH_TEMPLATE = "datasets/%s/raw/%s.json";
  public static final String QUERY_PATH_TEMPLATE = "queries/%s/%s.txt";

  /**
   * @todo description
   */
  public static String getDatasetPath(String sourceType, String datasetName) {
    return String.format(DATASET_PATH_TEMPLATE, sourceType, datasetName);
  }

  /**
   * @todo description
   */
  public static String getQueryPath(String sourceType, String queryFilename) {
    return String.format(QUERY_PATH_TEMPLATE, sourceType, queryFilename);
  }

  /**
   * @todo description
   */
  public static DataAdapter getFHIRAdapter(String sourceType) {
    switch (sourceType) {
      case "CSS":
        return CSSAdapter.create();
      case "SH":
      case "CALENDAR":
        return SHAdapter.create();
      default:
        throw new IllegalArgumentException("Only 'CSS' or 'SH' types allowed");
    }
  }

  /**
   * @todo description
   */
  public static File loadResource(String filename) {
    String pathname = classLoader.getResource(filename).getFile();
//    System.out.println("-----> loadResource().pathname: " + pathname); // DEBUG
    return new File(pathname);
  }

  /**
   * @todo description
   */
  public static File createOutputFile(String prepend, String ext) {
    long timestamp = Instant.now().toEpochMilli();
    String outputFilename = prepend + "-" + timestamp + "." + ext;
//    System.out.println("-----> outputFilename " + RDFizer.TMP_DIR +"/"+ outputFilename); // DEBUG
    return new File(TMP_DIR, outputFilename);
  }

  /**
   * @todo description
   */
  public static void removeAllLinesFromFile(File file, String needle) throws IOException {
    List<String> out = Files.lines(file.toPath())
      .filter(line -> !line.contains(needle))
      .collect(Collectors.toList());
    Files.write(file.toPath(), out, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
  }

  //--------------------------------------------------------------------------//
  // Class definition
  //--------------------------------------------------------------------------//\

  private static ClassLoader classLoader = TestUtils.class.getClassLoader();
  private static final File TMP_DIR = FileUtils.getTempDirectory();

  /**
   * This class is not instantiable
   */
  private TestUtils(){
  }

}