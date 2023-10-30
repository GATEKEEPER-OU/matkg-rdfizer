package lib.tests.helpers;

import org.apache.commons.io.FileUtils;
import tech.oxfordsemantic.jrdfox.Prefixes;
import tech.oxfordsemantic.jrdfox.client.*;
import tech.oxfordsemantic.jrdfox.exceptions.JRDFoxException;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;

/**
 * @author Riccardo Pala (riccardo.pala@open.ac.uk)
 * */
public class RDFoxUtils {

  static final String LICENSE_FILE = "RDFox.lic";

//  static {
////    System.loadLibrary("RDFox");
////    System.setProperty("java.library.path", "lib/v6.2-mac");
//  }

  /**
   * @todo
   * */
  public static void startLocalServer(
    String firstRoleName, String password
  ) throws JRDFoxException {
    Map<String, String> serverParams = new HashMap<>();
    serverParams.put("license-file", LICENSE_FILE);
    serverParams.put("num-threads", "2");
//    System.setProperty("java.library.path", "lib/v6.2-mac");
//    System.loadLibrary("");
    ConnectionFactory.startLocalServer(serverParams);
    if (ConnectionFactory.getNumberOfLocalServerRoles() == 0) {
      ConnectionFactory.createFirstLocalServerRole(firstRoleName, password);
    }
  }

  public static void createDatastore(
    ServerConnection serverConnection, String datastoreName, String ontology
  ) throws JRDFoxException {
    serverConnection.setNumberOfThreads(2);
    serverConnection.createDataStore(datastoreName, Collections.emptyMap());
    try (DataStoreConnection dataStoreConnection = serverConnection.newDataStoreConnection(datastoreName)) {
      RDFoxUtils.importOntology(dataStoreConnection, ontology);
    }
  }






  /**
   * @todo
   * */
  public static void importOntology(DataStoreConnection dataStoreConnection, String ontologyName) {
    File onotologyFile = null;
    try {
      URL ontologyResource = RDFoxUtils.class.getClassLoader().getResource(ontologyName);
      onotologyFile = new File(ontologyResource.toURI());

    } catch (URISyntaxException e) {
      // @todo Message
      e.printStackTrace(); // DEBUG
    }
    Prefixes prefixes = new Prefixes();
    prefixes.declareStandardPrefixes();
    importData(dataStoreConnection, onotologyFile, prefixes);
  }

  public static File getQueryFile(String queryName) {
    File queryFile  = null;
    try {
      URL query = RDFoxUtils.class.getClassLoader().getResource(queryName);
      queryFile = new File(query.toURI());
    } catch (URISyntaxException e) {
      // @todo Message
      e.printStackTrace(); // DEBUG
    }
    return queryFile;
  }

  /**
   * @todo
   * */
  public static void importData(DataStoreConnection dataStoreConnection, File data) {
    if (data.isDirectory()) {
      File[] files = data.listFiles();
      for (File file: files) {
        importData(dataStoreConnection, file, Prefixes.s_emptyPrefixes);
      }
    } else {
      importData(dataStoreConnection, data, Prefixes.s_emptyPrefixes);
    }
  }

  /**
   * @todo
   * */
  public static void importData(DataStoreConnection dataStoreConnection, File data, Prefixes prefixes) {
    try (InputStream inputStream = new FileInputStream(data)) {
//      dataStoreConnection.importData(UpdateType.ADDITION, prefixes, inputStream); // rdfox-v5.x
      dataStoreConnection.importData(UpdateType.ADDITION, inputStream); // rdfox-v6.0

    } catch (FileNotFoundException e) {
      // @todo Message
      e.printStackTrace(); // DEBUG

    } catch (IOException e) {
      // @todo Message
      e.printStackTrace(); // DEBUG

    } catch (JRDFoxException e) {
      // @todo Message
      e.printStackTrace(); // DEBUG
    }
  }

  /**
   * @todo
   * */
  public static void saveQueryResults(DataStoreConnection dataStoreConnection, String query, File output) {
    Prefixes prefixes = new Prefixes();
    prefixes.declareStandardPrefixes();

    List<String> rows = new ArrayList<String>();

//    try (Cursor cursor = dataStoreConnection.createCursor(null, prefixes, query, Collections.emptyMap())) { // rdfox-v5.x
    try (Cursor cursor = dataStoreConnection.createCursor(query, Collections.emptyMap())) { // rdfox-v6.0
      int arity = cursor.getArity();

      for (long multiplicity = cursor.open(); multiplicity != 0; multiplicity = cursor.advance()) {
        StringBuilder row = new StringBuilder();
        for (int termIndex = 0; termIndex < arity; ++termIndex) {
          row.append(" ");
          ResourceValue resource = cursor.getResourceValue(termIndex);
          row.append(resource.toString(prefixes));
        }
        rows.add(row.toString());
      }
      String content = String.join(".\n", rows);
      FileUtils.writeStringToFile(output, content, Charset.defaultCharset(), false);

    } catch (JRDFoxException e) {
      // @todo Message
      e.printStackTrace(); // DEBUG

    } catch (IOException e) {
      // @todo Message
      e.printStackTrace(); // DEBUG
    }
  }

  /**
   * @todo
   * */
  public static void printQueryResults(DataStoreConnection dataStoreConnection, String query) {
    Prefixes prefixes = new Prefixes();
    prefixes.declareStandardPrefixes();
//    try (Cursor cursor = dataStoreConnection.createCursor(null, prefixes, query, Collections.emptyMap())) { // rdfox-v5.x
    try (Cursor cursor = dataStoreConnection.createCursor(query, Collections.emptyMap())) { // rdfox-v6.0

      int numberOfRows = 0;
      System.out.println();
      System.out.println("=======================================================================================");
      int arity = cursor.getArity();
      // We iterate through the result tuples.
      for (long multiplicity = cursor.open(); multiplicity != 0; multiplicity = cursor.advance()) {
        // We iterate through the terms of each tuple.
        for (int termIndex = 0; termIndex < arity; ++termIndex) {
          if (termIndex != 0)
            System.out.print(" ");
          ResourceValue resource = cursor.getResourceValue(termIndex);
          System.out.print(resource.toString(prefixes));
        }
        System.out.print(" * ");
        System.out.print(multiplicity);
        System.out.println();
        ++numberOfRows;
      }
      System.out.println("---------------------------------------------------------------------------------------");
      System.out.println("  The number of rows returned: " + numberOfRows);
      System.out.println("=======================================================================================");
      System.out.println();

    } catch (JRDFoxException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @todo description
   */
  private RDFoxUtils() {
  }

}