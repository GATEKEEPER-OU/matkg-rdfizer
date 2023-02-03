package org.ou.gatekeeper.rdf.enums;

/**
 * @author Riccardo Pala (riccardo.pala@open.ac.uk)
 * @todo description
 */
public class OutputFormats {

  /**
   * @todo description
   */
  public static OutputFormat getOutputFormat(String format) {
    String name = format.toLowerCase();
    switch (name) {
      case "turtle":
        return OutputFormat.TURTLE;
      case "nt":
        return OutputFormat.NTRIPLES;
      default:
        throw new IllegalArgumentException("Only 'CSS' / 'FHIR' / 'SH' types allowed");
    }
  }

}