package org.ou.gatekeeper.adapters;

import org.ou.gatekeeper.adapters.css.CSSAdapter;
import org.ou.gatekeeper.adapters.fhir.FHIRAdapter;
import org.ou.gatekeeper.adapters.sh.SHAdapter;

/**
 * @author Riccardo Pala (riccardo.pala@open.ac.uk)
 * @todo description
 */
public class DataAdapters {

  /**
   * @todo description
   */
  public static DataAdapter getDataAdapter(String sourceFormat) {
    String name = sourceFormat.toLowerCase();
    switch (name) {
      case "css":
        return CSSAdapter.create();
      case "fhir":
        return FHIRAdapter.create();
      case "sh":
        return SHAdapter.create();
      default:
        throw new IllegalArgumentException("Only 'CSS' / 'FHIR' / 'SH' types allowed");
    }
  }

}