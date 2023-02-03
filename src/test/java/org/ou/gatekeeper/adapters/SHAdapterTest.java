package org.ou.gatekeeper.adapters;

import lib.tests.helpers.TestUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.commons.ResourceUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.ou.gatekeeper.adapters.sh.SHAdapter;

import java.io.File;
import java.io.IOException;

import static org.apache.commons.codec.digest.MessageDigestAlgorithms.SHA3_256;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Riccardo Pala (riccardo.pala@open.ac.uk)
 */
class SHAdapterTest {

  @ParameterizedTest
  @CsvSource({
    // Patient
    "xxx, keep, Patient",
    // Observations
//    "xxx, keep, Height",
//    "xxx, keep, Weight",
//    "xxx, keep, WaterIntake",
//    "xxx, keep, CaffeineIntake",
//    "xxx, keep, FloorsClimbed",
//    "xxx, keep, BloodGlucose",
//    "xxx, keep, BloodPressure",
//    "xxx, keep, FloorsClimbed",
//    "xxx, keep, StepDailyTrend",
//    "xxx, keep, HeartRate",
//    "xxx, keep, Walking",
//    "xxx, keep, Cycling",
//    "xxx, keep, Swimming.txt",
//    "xxx, keep, Sleep",
  })
  void test_transform_RawToFHIR(String expectedDigest, String policy, String datasetName) {
    String datasetPath = TestUtils.getDatasetPath("SH", datasetName);
    File datasetFile = TestUtils.loadResource(datasetPath);
    File  outputFile = TestUtils.createOutputFile("output-"+datasetName, "fhir.json");

    DataAdapter converter = SHAdapter.create();
    converter.toFhir(datasetFile, outputFile);

//    File outputNormFile = TestUtils.createOutputFile("output-"+datasetName, "norm.json");
//    converter.transform(datasetFile, outputNormFile, true);

    try {
      // Before evalutate, it must be eliminated outo-generated values (uuids, etc...)
      // because they will change each run
      // TODO resource_id
//      TestUtils.removeAllLinesFromFile(outputFile, "fullUrl");
//      TestUtils.removeAllLinesFromFile(outputFile, "reference");
//      TestUtils.removeAllLinesFromFile(outputFile, "valueString");

      // Evaluation
      String outputDigest = new DigestUtils(SHA3_256).digestAsHex(outputFile);
      assertEquals(expectedDigest, outputDigest);

    } catch (IOException e) {
      e.printStackTrace();

    } finally {
      if (policy.equals("keep")) {
        System.out.println("outputFile >>> " + outputFile);
      } else if (policy.equals("clean")) {
        ResourceUtils.clean(outputFile);
//        ResourceUtils.clean(outputNormFile);
      } else {
        throw new IllegalArgumentException("Only 'keep' or 'clean' policies allowed");
      }
    }
  }

}