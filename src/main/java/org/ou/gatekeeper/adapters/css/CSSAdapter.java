package org.ou.gatekeeper.adapters.css;

import com.ibm.fhir.model.resource.Bundle;
import org.json.JSONArray;
import org.json.JSONObject;
import org.ou.gatekeeper.adapters.BaseAdapter;
import org.ou.gatekeeper.adapters.DataAdapter;

import java.util.Collection;

import static org.apache.commons.collections.CollectionUtils.addIgnoreNull;
import static org.ou.gatekeeper.adapters.css.CSSBuilder.*;

/**
 * @author Riccardo Pala (riccardo.pala@open.ac.uk)
 * TODO description
 */
public class CSSAdapter extends BaseAdapter
                                implements DataAdapter {

  /**
   * TODO description
   */
  public static DataAdapter create() {
    return new CSSAdapter();
  }

  //--------------------------------------------------------------------------//
  // Class definition
  //--------------------------------------------------------------------------//

  /**
   * TODO description
   */
  protected CSSAdapter() {}

  /**
   * TODO description
   */
  protected void siftData(
    JSONObject json,
    Collection<Bundle.Entry> entries
  ) {
    JSONArray examinations = json.getJSONArray("clinical_examinations");
    for (int i = 0; i < examinations.length(); ++i) {
      JSONObject examination = examinations.getJSONObject(i);

      // Build and collect entries from input JSON
      Bundle.Entry patientEntry = collectPatient(entries, examination);
      Bundle.Entry patientAgeEntry = collectPatientAge(entries, examination, patientEntry);
      collectObservations(entries, examination, patientEntry, patientAgeEntry);
      collectConditions(entries, examination, patientEntry);
    }
  }

  /**
   * TODO description
   */
  private static Bundle.Entry collectPatient(
    Collection<Bundle.Entry> entries,
    JSONObject examination
  ) {
    JSONObject    patientJson = examination.getJSONObject("patient");
    Bundle.Entry patientEntry = buildPatient(patientJson);
    addIgnoreNull(entries, patientEntry);
    return patientEntry;
  }

  private static Bundle.Entry collectPatientAge(
    Collection<Bundle.Entry> entries,
    JSONObject examination,
    Bundle.Entry patientEntry
  ) {
    Bundle.Entry ageEntry = buildObservationAge(examination, patientEntry);
    addIgnoreNull(entries, ageEntry);
    return ageEntry;
  }

  private static void collectObservations(
    Collection<Bundle.Entry> entries,
    JSONObject examination,
    Bundle.Entry patientEntry,
    Bundle.Entry patientAgeEntry
  ) {
    addIgnoreNull(entries,
      buildObservationBodyHeight(examination, patientEntry, patientAgeEntry));
    addIgnoreNull(entries,
      buildObservationBodyWeight(examination, patientEntry, patientAgeEntry));
    addIgnoreNull(entries,
      buildObservationGlycosilatedEmoglobin(examination, patientEntry, patientAgeEntry));
    addIgnoreNull(entries,
      buildObservationTotalCholesterol(examination, patientEntry, patientAgeEntry));
    addIgnoreNull(entries,
      buildObservationHDL(examination, patientEntry, patientAgeEntry));
    addIgnoreNull(entries,
      buildObservationLDL(examination, patientEntry, patientAgeEntry));
    addIgnoreNull(entries,
      buildObservationTriglycerides(examination, patientEntry, patientAgeEntry));
    addIgnoreNull(entries,
      buildObservationTotalCholesterolHDL(examination, patientEntry, patientAgeEntry));
    addIgnoreNull(entries,
      buildObservationSerumCreatinine(examination, patientEntry, patientAgeEntry));
    addIgnoreNull(entries,
      buildObservationAlbuminuriaCreatininuriaRatio(examination, patientEntry, patientAgeEntry));
    addIgnoreNull(entries,
      buildObservationALT(examination, patientEntry, patientAgeEntry));
    addIgnoreNull(entries,
      buildObservationAST(examination, patientEntry, patientAgeEntry));
    addIgnoreNull(entries,
      buildObservationGammaGT(examination, patientEntry, patientAgeEntry));
    addIgnoreNull(entries,
      buildObservationAlkalinePhosphatase(examination, patientEntry, patientAgeEntry));
    addIgnoreNull(entries,
      buildObservationUricAcid(examination, patientEntry, patientAgeEntry));
    addIgnoreNull(entries,
      buildObservationGFR(examination, patientEntry, patientAgeEntry));
    addIgnoreNull(entries,
      buildObservationNitrites(examination, patientEntry, patientAgeEntry));
    addIgnoreNull(entries,
      buildObservationBloodPressure(examination, patientEntry, patientAgeEntry));
    addIgnoreNull(entries,
      buildObservationYearsWithDiabetes(examination, patientEntry, patientAgeEntry));
  }

  private static void collectConditions(
    Collection<Bundle.Entry> entries,
    JSONObject examination,
    Bundle.Entry patientEntry
  ) {
    addIgnoreNull(entries,
      buildConditionHepaticSteatosis(examination, patientEntry));
    addIgnoreNull(entries,
      buildConditionHypertension(examination, patientEntry));
    addIgnoreNull(entries,
      buildConditionHeartFailure(examination, patientEntry));
    addIgnoreNull(entries,
      buildConditionBPCO(examination, patientEntry));
    addIgnoreNull(entries,
      buildConditionChronicKidneyDisease(examination, patientEntry));
    addIgnoreNull(entries,
      buildConditionIschemicHeartDisease(examination, patientEntry));
  }

}