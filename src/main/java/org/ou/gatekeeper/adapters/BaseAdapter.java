package org.ou.gatekeeper.adapters;

import com.ibm.fhir.model.format.Format;
import com.ibm.fhir.model.generator.FHIRGenerator;
import com.ibm.fhir.model.generator.exception.FHIRGeneratorException;
import com.ibm.fhir.model.resource.Bundle;
import com.ibm.fhir.model.type.code.BundleType;
import com.ibm.fhir.model.visitor.Visitable;
import org.apache.commons.io.FileUtils;
import org.commons.FHIRUtils;
import org.commons.ResourceUtils;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.util.Collection;
import java.util.LinkedList;

import static org.commons.ResourceUtils.generateUniqueFilename;

public abstract class BaseAdapter implements DataAdapter {

  /**
   * TODO description
   */
  @Override
  public void toFhir(File dataset, File output) {
    try (InputStream datasetInputStream = new FileInputStream(dataset)) {
      // Parse JSON dataset
      JSONTokener tokenizer = new JSONTokener(datasetInputStream);
      JSONObject json = new JSONObject(tokenizer);

      // Collect all entries
      Collection<Bundle.Entry> entries = new LinkedList<>();
      siftData(json, entries);

      // Build FHIR bundle
      Visitable bundle = Bundle.builder()
        .entry(entries)
        .type(BundleType.TRANSACTION)
        .build();

      // Save FHIR bundle in a JSON file
      Writer file = new FileWriter(output);
      FHIRGenerator
        .generator(Format.JSON, true)
        .generate(bundle, file);
      file.close();

    } catch (IOException | FHIRGeneratorException e) {
      // TODO how to handle it?
      throw new RuntimeException(e);
    }
  }

  /**
   * TODO description
   */
  @Override
  public void toExtendedFhir(File dataset, File output) {
    String tempFilename = generateUniqueFilename("output", "tmp.fhir.json");
    File tempOutputFile = new File(TMP_DIR, tempFilename);
    toFhir(dataset, tempOutputFile);
    FHIRUtils.normalize(tempOutputFile, output);
    ResourceUtils.clean(tempOutputFile);
  }

  /**
   * TODO description
   */
  protected abstract void siftData(
    JSONObject json,
    Collection<Bundle.Entry> entries
  );

  //--------------------------------------------------------------------------//
  // Private methods
  //--------------------------------------------------------------------------//

  /** Used to store temporary content */
  private static final File TMP_DIR = FileUtils.getTempDirectory();

}