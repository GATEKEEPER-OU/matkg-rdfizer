package org.ou.gatekeeper;

import lib.tests.helpers.RDFoxUtils;
import lib.tests.helpers.TestUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.commons.ResourceUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.ou.gatekeeper.adapters.DataAdapter;
import org.ou.gatekeeper.adapters.DataAdapters;
import org.ou.gatekeeper.rdf.enums.OutputFormat;
import org.ou.gatekeeper.rdf.mappings.HelifitMapping;
import org.ou.gatekeeper.rdf.mappings.RMLMapping;
import tech.oxfordsemantic.jrdfox.client.ConnectionFactory;
import tech.oxfordsemantic.jrdfox.client.DataStoreConnection;
import tech.oxfordsemantic.jrdfox.client.ServerConnection;
import tech.oxfordsemantic.jrdfox.exceptions.JRDFoxException;

import java.io.File;
import java.io.IOException;

import static org.apache.commons.codec.digest.MessageDigestAlgorithms.SHA3_256;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Riccardo Pala (riccardo.pala@open.ac.uk)
 * @author Carlo Allocca (c.allocca@samsung.com)
 */
public class KnowledgeGraphTest {

  static final String ONTOLOGY = "ontologies/HeLiFit-OWL-Functional-Syntax.owl";

  static final String RDFOX_HOST = "rdfox:local";
  static final String TEST_ROLE = "aRole";
  static final String TEST_PASSWORD = "aPassword";
  static final String DATASTORE_NAME = "GK-DataStore";
  static ServerConnection serverConnection = null;

  @BeforeAll
  static void setUpClass() throws JRDFoxException {
    RDFoxUtils.startLocalServer(TEST_ROLE, TEST_PASSWORD);
    serverConnection = ConnectionFactory.newServerConnection(RDFOX_HOST, TEST_ROLE, TEST_PASSWORD);
    RDFoxUtils.createDatastore(serverConnection, DATASTORE_NAME, ONTOLOGY);
  }

  @BeforeEach
  void setUp() {
    // delete clean datastore
  }

  @ParameterizedTest
  @CsvSource({
    //
    // CSS
    // -------------------------------------------------------------------------
    "xxx, keep, CSS, GlycosilatedEmoglobin",
//    "xxx, keep, CSS, BloodPressure",
//    "xxx, keep, CSS, BodyWeight",
//    "xxx, keep, CSS, GlycosilatedEmoglobin",
//    "xxx, keep, CSS, TotalCholesterol",
//    "xxx, keep, CSS, HighDensityLipoprotein",
//    "xxx, keep, CSS, LowDensityLipoprotein",
//    "xxx, keep, CSS, Triglycerides",
//    "xxx, keep, CSS, SerumCreatinine",
//    "xxx, keep, CSS, AlbuminuriaCreatininuriaRatio",
//    "xxx, keep, CSS, AlanineAminoTransferase",
//    "xxx, keep, CSS, AspartateAminoTransferase",
//    "xxx, keep, CSS, GlutamylTransferase",
//    "xxx, keep, CSS, AlkalinePhosphatase",
//    "xxx, keep, CSS, UricAcid",
//    "xxx, keep, CSS, EstimatedGlomerularFiltrationRate",
//    "xxx, keep, CSS, Nitrites",
//    "xxx, keep, CSS, HepaticSteatosis",
//    "xxx, keep, CSS, Hypertension",
//    "xxx, keep, CSS, HeartFailure",
//    "xxx, keep, CSS, BPCO",
//    "xxx, keep, CSS, ChronicKidneyDisease",
//   "xxx, keep, CSS, IschemicHeartDisease",
    //
    // Samsung Health
    // -------------------------------------------------------------------------
//    "xxx, keep, SH, FloorsClimbed",
//    "xxx, keep, SH, StepDailyTrend",
//    "xxx, keep, SH, HeartRate",
//    "xxx, keep, SH, Sleep",
//    "xxx, keep, SH, SleepStage",
//    "xxx, keep, SH, Walking",
//    "xxx, keep, SH, Cycling",
//    "xxx, keep, SH, Running",
//    "xxx, keep, SH, Swimming",
//    "xxx, keep, SH, BodyWeight",
//    "xxx, keep, SH, BodyHeight",
//    "xxx, keep, SH, WaterIntake",
//    "xxx, keep, SH, CaffeineIntake",
//    "xxx, keep, SH, BloodGlucose",
//    "xxx, keep, SH, BloodPressure",
//    "xxx, keep, SH, OxygenSaturation",
//    "xxx, keep, SH, CalendarEvent",
//    "xxx, keep, SH, BodyFat",
//    "xxx, keep, SH, BodyMuscle",
//    "xxx, keep, SH, BodyTemperature",
//    "xxx, keep, SH, HipCircumference",
//    "xxx, keep, SH, Homocysteine",
    "xxx, keep, SH, WaistCircumference",
  })
  void test_knowledgeGraph(
    String expectedDigest, String policy, String sourceType, String datasetName
  ) throws JRDFoxException {
    String datasetPath = TestUtils.getDatasetPath(sourceType, datasetName);
    File   datasetFile = TestUtils.loadResource(datasetPath);

    OutputFormat tripleFormat = OutputFormat.TURTLE;
    File triplesFile = TestUtils.createOutputFile("output-"+datasetName+".triples", "turtle");

    String queryPath = TestUtils.getQueryPath(sourceType, datasetName);
    File   queryFile = TestUtils.loadResource(queryPath);
    String queryText = ResourceUtils.readFileToString(queryFile);
    File  outputFile = TestUtils.createOutputFile("output-"+datasetName+".query", "nt");

    DataAdapter converter = DataAdapters.getDataAdapter(sourceType);
    RMLMapping    mapping = HelifitMapping.create(tripleFormat);
    RDFizer.trasform(datasetFile, converter, mapping, triplesFile);

    try (DataStoreConnection dataStoreConnection = serverConnection.newDataStoreConnection(DATASTORE_NAME)) {
      RDFoxUtils.importData(dataStoreConnection, triplesFile);
      RDFoxUtils.saveQueryResults(dataStoreConnection, queryText, outputFile);
    }

    try {
      String outputDigest = new DigestUtils(SHA3_256).digestAsHex(outputFile);
      assertEquals(expectedDigest, outputDigest);

    } catch (IOException e) {
      e.printStackTrace();

    } finally {
      if (policy.equals("keep")) {
        System.out.println("triplesFile >>> " + triplesFile);
        System.out.println("Query Results >>> " + outputFile);
      } else if (policy.equals("clean")) {
        ResourceUtils.clean(triplesFile);
        ResourceUtils.clean(outputFile);
      } else {
        throw new IllegalArgumentException("Only 'keep' or 'clean' policies allowed");
      }
    }
  }

}