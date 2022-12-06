package org.ou.gatekeeper.fhir.adapters;

import org.apache.commons.codec.digest.DigestUtils;
import org.commons.ResourceUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import lib.tests.helpers.TestUtils;
import org.ou.gatekeeper.fhir.adapters.sh.SHAdapter;

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
    "5af918e5d5c9289c82c513856e59e70af3111d648d2c57821942aaa87ca0c4e7, keep, Patient",
    // Observations
    "xxx, clean, Height",
    "xxx, clean, Weight",
    "xxx, clean, WaterIntake",
    "xxx, clean, CaffeineIntake",
    "xxx, clean, FloorsClimbed",
    "xxx, clean, BloodGlucose",
    "xxx, clean, BloodPressure",
    "xxx, clean, FloorsClimbed",
    "xxx, clean, StepDailyTrend",
    "xxx, clean, HeartRate",
    "xxx, clean, Walking",
    "xxx, clean, Cycling",
    "xxx, clean, Swimming.txt",
    "xxx, clean, Sleep",
  })
  void test_transform_RawToFHIR(String expectedDigest, String policy, String datasetName) {
    String datasetPath = TestUtils.getDatasetPath("SH", datasetName);
    File datasetFile = TestUtils.loadResource(datasetPath);
    File  outputFile = TestUtils.createOutputFile("output-"+datasetName, "fhir.json");

    FHIRAdapter converter = SHAdapter.create();
    converter.transform(datasetFile, outputFile);

    File outputNormFile = TestUtils.createOutputFile("output-"+datasetName, "norm.json");
    converter.transform(datasetFile, outputNormFile, true);

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
        ResourceUtils.clean(outputNormFile);
      } else {
        throw new IllegalArgumentException("Only 'keep' or 'clean' policies allowed");
      }
    }
  }

}