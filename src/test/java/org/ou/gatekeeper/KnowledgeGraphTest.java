package org.ou.gatekeeper;

import lib.tests.helpers.RDFoxUtils;
import lib.tests.helpers.TestUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.commons.ResourceUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.ou.gatekeeper.fhir.adapters.FHIRAdapter;
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
//    "xxx, keep, CSS, GlycosilatedEmoglobin",

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
    "xxx, keep, SH, OxygenSaturation",
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

    FHIRAdapter converter = TestUtils.getFHIRAdapter(sourceType);
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