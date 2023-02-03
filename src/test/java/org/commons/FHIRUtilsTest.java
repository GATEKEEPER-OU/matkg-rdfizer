package org.commons;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * @author Riccardo Pala (riccardo.pala@open.ac.uk)
 */
public class FHIRUtilsTest {

  @ParameterizedTest
  @CsvSource({
    "7f3e11e4b2d1a852e49b3814ebfdd98f89cc7c543d1d7b531efde27945a8d71f, datasets/saxony/phr/01-dataset-complete.json",
    "a23ea70bec8b24211101d65f88a275be5d5a839f5128c97b7aa066c6be999795, datasets/saxony/phr/04-dataset-complete.json"
  })
  void test_normalize(String expectedDigest, String dataset){
    // TODO
  }

//  @ParameterizedTest
//  @CsvSource({
//    "7f3e11e4b2d1a852e49b3814ebfdd98f89cc7c543d1d7b531efde27945a8d71f, datasets/saxony/phr/01-dataset-complete.json",
//    "a23ea70bec8b24211101d65f88a275be5d5a839f5128c97b7aa066c6be999795, datasets/saxony/phr/04-dataset-complete.json"
//  })
//  void testNormalizeFHIR(String expectedDigest, String dataset) {
//    File datasetFile = TestUtils.loadResource(dataset);
//    File outputFile = TestUtils.createOutputFile("output", "nomalized.fhir.json");
//
//    FHIRNormalizer.normalize(datasetFile, outputFile);
//
//    try {
//      String outputDigest = new DigestUtils(SHA3_256).digestAsHex(outputFile);
//      assertEquals(expectedDigest, outputDigest);
//    } catch (IOException e) {
//    } finally {
////      outputFile.delete();
//    }
//  }

}