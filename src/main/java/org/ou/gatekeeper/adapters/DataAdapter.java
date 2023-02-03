package org.ou.gatekeeper.adapters;

import java.io.File;

/**
 * @author Riccardo Pala (riccardo.pala@open.ac.uk)
 * TODO description
 */
public interface DataAdapter {

  /**
   * TODO description
   * */
  void toFhir(File dataset, File output);

  /**
   * TODO description
   * */
  void toExtendedFhir(File dataset, File output);

}