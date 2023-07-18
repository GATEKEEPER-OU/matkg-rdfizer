package org.ou.gatekeeper;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.commons.ResourceUtils;
import org.ou.gatekeeper.adapters.DataAdapter;
import org.ou.gatekeeper.rdf.RDFMapper;
import org.ou.gatekeeper.rdf.mappings.RMLMapping;

import java.io.File;

import static org.commons.ResourceUtils.generateUniqueFilename;

/**
 * @author Riccardo Pala (riccardo.pala@open.ac.uk)
 * TODO description
 */
public class RDFizer {

  /**
   * Reads dataset from file and writes the result on a file in FHIR format.
   * @param input file to convert
   * @param output RDF output file
   * @param adapter the adapter that understands the input dataset
   * */
  public static void transform(
    File input,
    File output,
    DataAdapter adapter
  ) {
    transform(input, output, adapter, null);
  }

  // TODO update the description for the FHIR STEP
  // Reads dataset from file and writes the result on a file in FHIR format.
  /**
   * Reads dataset from file and writes the result on a file in RDF format.
   * @param input file to convert
   * @param output RDF output file
   * @param adapter the adapter that understands the input dataset
   * @param mapping RML mapping file which contains mapping rules
   * */
  public static void transform(
    File input,
    File output,
    DataAdapter adapter,
    RMLMapping mapping
  ) {
    transform(input, output, adapter, mapping, true);
  }

  /**
   * Reads dataset from file and writes the result on a file in RDF format.
   * @param input file to convert
   * @param output RDF output file
   * @param adapter the adapter that understands the input dataset
   * @param mapping RML mapping file which contains mapping rules
   * @param clean if {@code false} all intermediate results will be kept
   * */
  public static void transform(
    File input,
    File output,
    DataAdapter adapter,
    RMLMapping mapping,
    boolean clean
  ) {
    if (mapping == null) {
      adapter.toFhir(input, output);

    } else {
      String inputName = FilenameUtils.removeExtension(input.getName());
      String fhirFilename = generateUniqueFilename("output-"+inputName, "json");
//    String fhirFilename = generateUniqueFilename("output", "fhir.json");
      File tempFhirFile = new File(TMP_DIR, fhirFilename);
      adapter.toExtendedFhir(input, tempFhirFile);
      mapping.setLocalSource(tempFhirFile.getAbsolutePath());
      RDFMapper.map(mapping, output);
      if (clean) {
        ResourceUtils.clean(tempFhirFile);
      }
    }
  }

  //--------------------------------------------------------------------------//
  // Class definition
  //--------------------------------------------------------------------------//

  private static final File TMP_DIR = FileUtils.getTempDirectory();
//  private static final Logger LOGGER = LoggerFactory.getLogger(RDFizer.class); // @todo

  /**
   * This class is not instantiable
   */
  private RDFizer() {
  }

}