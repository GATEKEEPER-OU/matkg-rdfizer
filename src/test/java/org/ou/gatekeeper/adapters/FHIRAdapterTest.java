package org.ou.gatekeeper.adapters;

/**
 * @author Riccardo Pala (riccardo.pala@open.ac.uk)
 */
class FHIRAdapterTest { // TODO

//  @ParameterizedTest
//  @CsvSource({
//    // Patient
//    "xxx, keep, PatientWithAge",
////    "xxx, keep, BodyHeight",
////    "xxx, keep, BodyWeight",
//
//    // Observations
////    "xxx, keep, GlycosilatedEmoglobin",
////    "xxx, keep, TotalCholesterol",
////    "xxx, keep, HighDensityLipoprotein",
////    "xxx, keep, LowDensityLipoprotein",
////    "xxx, keep, Triglycerides",
////    "xxx, keep, TotalCholesterolHDL",
////    "xxx, keep, SerumCreatinine",
////    "xxx, keep, AlbuminuriaCreatininuriaRatio",
////    "xxx, keep, GPTALT",
////    "xxx, keep, GOTAST",
////    "xxx, keep, GammaGT",
////    "xxx, keep, AlkalinePhosphatase",
////    "xxx, keep, UricAcid",
////    "xxx, keep, EstimatedGlomerularFiltrationRate",
////    "xxx, keep, Nitrites",
////    "xxx, keep, BloodPressure",
////    "xxx, keep, YearsWithDiabetes",
//
//    // Conditions
////    "xxx, keep, HepaticSteatosis",
////    "xxx, keep, Hypertension",
////    "xxx, keep, HeartFailure",
////    "xxx, keep, BPCO",
////    "xxx, keep, ChronicKidneyDisease",
////    "xxx, keep, IschemicHeartDisease",
//  })
//  void test_transform_RawToFHIR(String expectedDigest, String policy, String datasetName) {
//    String datasetPath = TestUtils.getDatasetPath("CSS", datasetName);
//    File   datasetFile = TestUtils.loadResource(datasetPath);
//    File    outputFile = TestUtils.createOutputFile("output-"+datasetName, "fhir.json");
//
//    FHIRAdapter converter = CSSAdapter.create();
//    converter.transform(datasetFile, outputFile);
//
////    File outputNormFile = TestUtils.createOutputFile("output-"+datasetName, "norm.json");
////    converter.transform(datasetFile, outputNormFile, true);
//
//    try {
//      // Before evalutate, it must be eliminated outo-generated values (uuids, etc...)
//      // because they will change each run
////      TestUtils.removeAllLinesFromFile(outputFile, "fullUrl");
////      TestUtils.removeAllLinesFromFile(outputFile, "reference");
////      TestUtils.removeAllLinesFromFile(outputFile, "valueString");
//
//      // Evaluation
//      String outputDigest = new DigestUtils(SHA3_256).digestAsHex(outputFile);
//      assertEquals(expectedDigest, outputDigest);
//
//    } catch (IOException e) {
//      e.printStackTrace();
//
//    } finally {
//      if (policy.equals("keep")) {
//        System.out.println("outputFile >>> " + outputFile);
//      } else if (policy.equals("clean")) {
//        ResourceUtils.clean(outputFile);
////        ResourceUtils.clean(outputNormFile);
//      } else {
//        throw new IllegalArgumentException("Only 'keep' or 'clean' policies allowed");
//      }
//    }
//  }

}