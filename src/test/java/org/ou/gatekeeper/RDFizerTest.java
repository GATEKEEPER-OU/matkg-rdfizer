package org.ou.gatekeeper;

import lib.tests.helpers.TestUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.commons.ResourceUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.ou.gatekeeper.fhir.adapters.FHIRAdapter;
import org.ou.gatekeeper.rdf.enums.OutputFormat;
import org.ou.gatekeeper.rdf.mappings.HelifitMapping;
import org.ou.gatekeeper.rdf.mappings.RMLMapping;

import java.io.File;
import java.io.IOException;

import static org.apache.commons.codec.digest.MessageDigestAlgorithms.SHA3_256;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Riccardo Pala (riccardo.pala@open.ac.uk)
 * @author Carlo Allocca (c.allocca@samsung.com)
 */


class RDFizerTest {

  @ParameterizedTest
  @CsvSource({
    //
    // CSS
    // -------------------------------------------------------------------------

    // Patient
//    "xxx, keep, CSS, Patient",

    // Observations
//    "xxx, keep, CSS, .., all",
//    "xxx, keep, CSS, GlycosilatedEmoglobin",
//    "xxx, keep, CSS, ...",

    //
    // Samsung Health
    // -------------------------------------------------------------------------

    // Patient
//    "xxx, keep, SH, Patient",

    // Observations
//    "xxx, keep, SH, FloorsClimbed",
//    "xxx, keep, SH, StepDailyTrend",
//    "xxx, keep, SH, HeartRate",
//    "xxx, keep, SH, Walking",
//    "xxx, keep, SH, Cycling",
//    "xxx, keep, SH, Running",
//    "xxx, keep, SH, Swimming",
//    "xxx, keep, SH, Sleep",
//    "xxx, keep, SH, SleepStage",
//    "xxx, keep, SH, BodyWeight",
//    "xxx, keep, SH, BodyHeight",
//    "xxx, keep, SH, WaterIntake",
//    "xxx, keep, SH, CaffeineIntake",
//    "xxx, keep, SH, BloodGlucose",
//    "xxx, keep, SH, BloodPressure",
//    "xxx, keep, SH, OxygenSaturation",
  })
  @Disabled
  void test_transform_RawToFHIR(String expectedDigest, String policy, String sourceType, String datasetName) {
    String datasetPath = TestUtils.getDatasetPath(sourceType, datasetName);
    File datasetFile = TestUtils.loadResource(datasetPath);
    File outputFile = TestUtils.createOutputFile("output-"+datasetName, "fhir.json");

    FHIRAdapter converter = TestUtils.getFHIRAdapter(sourceType);
    RDFizer.trasform(datasetFile, converter, outputFile);

    try {
      String outputDigest = new DigestUtils(SHA3_256).digestAsHex(outputFile);
      assertEquals(expectedDigest, outputDigest);

    } catch (IOException e) {
      e.printStackTrace();

    } finally {
      if (policy.equals("keep")) {
        System.out.println("outputFile >>> " + outputFile);
      } else if (policy.equals("clean")) {
        ResourceUtils.clean(outputFile);
      } else {
        throw new IllegalArgumentException("Only 'keep' or 'clean' policies allowed");
      }
    }
  }

  @ParameterizedTest
  @CsvSource({
    //
    // CSS
    // -------------------------------------------------------------------------

    // Patient
//    "xxx, keep, CSS, Patient, '0000,0001,0002,0003,0044'",
//      "xxx, keep, CSS, PatientWithAge, '0000,0001,0002,0003,0044'",
    // Observations
//   "xxx, keep, CSS, .., all",
//      "xxx, keep, CSS, BodyHeight, '0000,0001,0002,0003,0040,0041,0044,1001'",
//    "xxx, keep, CSS, Weight, '0000,0001,0002,0003,0040,0041,0044,1002'",
//    "xxx, keep, CSS, GlycosilatedEmoglobin, '0000,0001,0002,0003,0040,0041,0044,4001'",
//          "xxx, keep, CSS, TotalCholesterol, '0000,0001,0002,0003,0040,0041,0044,4002'",
//          "xxx, keep, CSS, HighDensityLipoprotein, '0000,0001,0002,0003,0040,0041,0044,4003'",
//          "xxx, keep, CSS, LowDensityLipoprotein, '0000,0001,0002,0003,0040,0041,0044,4004'",
//          "xxx, keep, CSS, Triglycerides, '0000,0001,0002,0003,0040,0041,0044,4005'",
//          "xxx, keep, CSS, SerumCreatinine, '0000,0001,0002,0003,0040,0041,0044,4006'",
//          "xxx, keep, CSS, AlbuminuriaCreatininuriaRatio, '0000,0001,0002,0003,0040,0041,0044,4007'",
//          "xxx, keep, CSS, AlanineAminoTransferase, '0000,0001,0002,0003,0040,0041,0044,4008'",
//          "xxx, keep, CSS, AspartateAminoTransferase, '0000,0001,0002,0003,0040,0041,0044,4009'",
//          "xxx, keep, CSS, GlutamylTransferase, '0000,0001,0002,0003,0040,0041,0044,4010'",
//          "xxx, keep, CSS, AlkalinePhosphatase, '0000,0001,0002,0003,0040,0041,0044,4011'",
//          "xxx, keep, CSS, UricAcid, '0000,0001,0002,0003,0040,0041,0044,4012'",
//          "xxx, keep, CSS, EstimatedGlomerularFiltrationRate, '0000,0001,0002,0003,0040,0041,0044,4013'",
//          "xxx, keep, CSS, Nitrites, '0000,0001,0002,0003,0040,0041,0044,4014'",

//          "xxx, keep, CSS, BloodPressure, '0000,0001,0002,0003,0010,0040,0041,0044,1006'",

//          "xxx, keep, CSS, HepaticSteatosis, '0000,0001,0002,0003,0040,0042,0044,5001'",
//          "xxx, keep, CSS, Hypertension, '0000,0001,0002,0003,0040,0042,0044,5002'",
//          "xxx, keep, CSS, HeartFailure, '0000,0001,0002,0003,0040,0042,0044,5003'",
//          "xxx, keep, CSS, BPCO, '0000,0001,0002,0003,0040,0042,0044,5004'",
//          "xxx, keep, CSS, ChronicKidneyDisease, '0000,0001,0002,0003,0040,0042,0044,5005'",
//          "xxx, keep, CSS, IschemicHeartDisease, '0000,0001,0002,0003,0040,0042,0044,5006'",





          //
    // Samsung Health
    // -------------------------------------------------------------------------

    // Patient
//    "xxx, keep, SH, Patient, '0000,0001,0002,0003,0044'",
//    "xxx, keep, SH, BodyHeight, '0000,0001,0002,0003,0040,0041,0044,1001'",
//    "xxx, keep, SH, BodyWeight, '0000,0001,0002,0003,0010,0040, 0041,0044,1002'",
//    "xxx, keep, SH, BloodPressure, '0000,0001,0002,0003,0010,0040,0041,0044,1006'",

    // Observations
//    "xxx, keep, SH, FloorsClimbed,  '0000,0001,0002,0003,0020,0021,0040,0041,2101'",
//    "xxx, keep, SH, StepDailyTrend, '0000,0001,0002,0003,0020,0021,0040,0041,2102,0010,2001,2002,2003,2004,2005,2006,2007'",
//    "xxx, keep, SH, HeartRate,      '0000,0001,0002,0003,0020,0021,0040,0041,2103,3003,3008,3009'",
    "xxx, keep, SH, Walking,        '0000,0001,0002,0003,0020,0021,0040,0041,0010,3101,3011,3001,3003,3005,3007,2004,2005,2007,2006,2003,2002'",
//    "xxx, keep, SH, UnknownExercise,  '0000,0001,0002,0003,0020,0021,0040,0041,0010,3105,3011,3001,3003,3005,3007,2004,2005,2007,2006,2003,2002'",
//    "xxx, keep, SH, Cycling,        '0000,0001,0002,0003,0020,0021,0040,0041,0010,3102,3011,3001,3003,3005,3007,2004,2005,2007,2006,2003,2002'",
//    "xxx, keep, SH, Running,        '0000,0001,0002,0003,0020,0021,0040,0041,0010,3103,3011,3001,3003,3005,3007,2004,2005,2007,2006,2003,2002'",
//    "xxx, keep, SH, Swimming,       '0000,0001,0002,0003,0020,0021,0040,0041,0010,3104,3011,3001,3003,3005,3007,2004,2005,2007,2006,2003,2002'",
//    "xxx, keep, SH, Sleep,     '0000,0001,0002,0003,0020,0021,0040,0041,2104'",
//    "xxx, keep, SH, SleepStage,     '0000,0001,0002,0003,0020,0021,0040,0041,0043,2105'",
//          "xxx, keep, CALENDAR, CalendarEvent, '0000,0001,0002,0003,6001,6002,6003'",
  })
  /*
   * NOTES
   *   @param modules can be a list of prefixes separated by commas, or the label 'all'
   *   RECOMMENDATION:
   *   - use 'all' label is discoraged into selective tests, because it will produce
   *     many false-positive warnings, make it hard to debug
   * */
  /*
  use this method to test if each rml rule is correctly written w.r.t HeLiFit ontology.
   */
  void test_transform_RawToRDF(String expectedDigest, String policy, String sourceType, String datasetName, String modules) {
    String datasetPath = TestUtils.getDatasetPath(sourceType, datasetName);
//    System.out.println("---------->>>>> " +datasetPath);
    File datasetFile = TestUtils.loadResource(datasetPath);
//    OutputFormat outputFormat = OutputFormat.NTRIPLES;
//    File outputFile = TestUtils.createOutputFile("output-"+datasetName, "nt");
    OutputFormat outputFormat = OutputFormat.TURTLE;
    File outputFile = TestUtils.createOutputFile("output-"+datasetName, "turtle");

    FHIRAdapter converter = TestUtils.getFHIRAdapter(sourceType);
    String[] partsToInclude = !modules.equals("all") ? modules.split(",") : null;
    RMLMapping mapping = HelifitMapping.create(outputFormat, partsToInclude, true);
    RDFizer.trasform(datasetFile, converter, mapping, outputFile, false);

    try {
      String outputDigest = new DigestUtils(SHA3_256).digestAsHex(outputFile);
      assertEquals(expectedDigest, outputDigest);

    } catch (IOException e) {
      e.printStackTrace();

    } finally {
      if (policy.equals("keep")) {
        System.out.println("outputFile >>> " + outputFile);
      } else if (policy.equals("clean")) {
        ResourceUtils.clean(outputFile);
      } else {
        throw new IllegalArgumentException("Only 'keep' or 'clean' policies allowed");
      }
    }
  }

}