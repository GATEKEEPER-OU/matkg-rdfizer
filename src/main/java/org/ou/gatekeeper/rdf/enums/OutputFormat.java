package org.ou.gatekeeper.rdf.enums;

/**
 * TODO desc
 * @author Riccardo Pala (riccardo.pala@open.ac.uk)
 */
public enum OutputFormat {

  TURTLE("turtle"),
  NTRIPLES("ntriples"),
  NQUADS("nquads"),
  JSONLD("jsonld"),
  TRIG("trig"),
  TRIX("trix");

  /** @todo */
  private String value;

  /**
   * @todo
   * */
  OutputFormat(String s) {
    value = s;
  }

  /**
   * @todo
   * */
  @Override
  public String toString() {
    return value;
  }

}