package org.ou.gatekeeper.adapters.fhir;

import com.ibm.fhir.model.resource.Bundle;
import org.commons.FHIRUtils;
import org.json.JSONObject;
import org.ou.gatekeeper.adapters.BaseAdapter;
import org.ou.gatekeeper.adapters.DataAdapter;

import java.io.File;
import java.util.Collection;

/**
 * @author Riccardo Pala (riccardo.pala@open.ac.uk)
 * TODO description
 */
public class FHIRAdapter extends BaseAdapter
                      implements DataAdapter {

  /**
   * TODO description
   */
  public static DataAdapter create() {
    return new FHIRAdapter();
  }

  /**
   * TODO description
   */
  public void toFhir(File dataset, File output) {
    throw new UnsupportedOperationException();
  }

  /**
   * TODO description
   */
  public void toExtendedFhir(File dataset, File output) {
    FHIRUtils.normalize(dataset, output);
  }

  //--------------------------------------------------------------------------//
  // Class definition
  //--------------------------------------------------------------------------//

  /**
   * TODO description
   * */
  protected FHIRAdapter() {}

  /**
   * TODO description
   */
  @Override
  protected void siftData(JSONObject json, Collection<Bundle.Entry> entries) {
    throw new UnsupportedOperationException();
  }

}