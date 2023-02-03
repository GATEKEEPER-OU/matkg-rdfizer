package org.ou.gatekeeper.adapters.css;

import com.ibm.fhir.model.resource.Bundle;
import com.ibm.fhir.model.resource.Condition;
import com.ibm.fhir.model.resource.Observation;
import com.ibm.fhir.model.resource.Patient;
import com.ibm.fhir.model.type.Boolean;
import com.ibm.fhir.model.type.*;
import com.ibm.fhir.model.type.code.ObservationStatus;
import org.apache.commons.codec.binary.Base64;
import org.commons.DateTimeUtils;
import org.commons.JSONObjectUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.ou.gatekeeper.adapters.BaseBuilder;

import java.lang.String;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedList;

/**
 * @author Riccardo Pala (riccardo.pala@open.ac.uk)
 * @todo description
 */
class CSSBuilder extends BaseBuilder {

  public static final String BASE_URL = "https://www.gatekeeper-project.eu/sid/puglia";

//  public static final String OBSERVATION_CATEGORY_SYSTEM = HL7_SYSTEM + "/observation-category";
//  public static final String HL7_SYSTEM = "http://terminology.hl7.org/CodeSystem/observation-category";
//  public static final String HL7_PATIENT_AGE = "http://hl7.org/fhir/StructureDefinition/observation-patientAge";
//  public static final String PATIENT_AGE = HL7_STRUCTURE + "/observation-patientAge";

  public static DateTime buildDate(String dateTime) {
    return DateTime.builder()
      .value(
        DateTimeUtils.cast(dateTime)
      )
      .build();
  }

  public static Bundle.Entry buildPatient(JSONObject patient) {
    String patientId = JSONObjectUtils.getId(patient, "patient_id");
    String uuid = patientId.replace("@", "-");
    String fullUrl = BASE_URL + "/patient/" + patientId;
    return buildEntry(
      Patient.builder()
        .id(uuid)
        .identifier(
          buildIdentifier(
            BASE_URL + "/identifier",
            patientId
          )
        )
        .build(),
      "Patient",
      "identifier=" + BASE_URL + "/patient|" + patientId,
      buildFullUrl(fullUrl)
    );
  }

  //
  // Observations
  //
  public static Bundle.Entry buildObservationAge(
          JSONObject examination,
          Bundle.Entry patientEntry
  ) {
    String patientId = patientEntry.getResource().getId();
    String date = examination.getString("date");
    BigDecimal age = examination.getBigDecimal("patient_age");
    String uuid = Base64.encodeBase64URLSafeString(
            (patientId + date + age.toString()).getBytes()
    );
    return buildEntry(
            Observation.builder()
                    .id(uuid)
                    .status(ObservationStatus.FINAL)
                    .identifier(
                            buildIdentifier(
                                    BASE_URL + "/identifier",
                                    "Observation/" + uuid
                            )
                    )
                    .code(
                            buildCodeableConcept(buildCoding(
                                    LOCAL_SYSTEM,
                                    "patient_age",
                                    "Patient age"
                            ))
                    )
                    .effective(
                            buildDate(examination.getString("date"))
                    )
                    .value(
                            buildQuantity(
                                    Decimal.of(examination.getBigDecimal("patient_age")),
                                    "year",
                                    UNITSOFM_SYSTEM,
                                    "a_j" // TODO check here https://ucum.org/ucum#para-29
                            )
                    )
                    .subject(
                            buildReference(patientEntry)
                    )
                    .build(),
            FHIR_OBSERVATION_TYPE,
            null,
            buildFullUrl(BASE_URL + "/observation/" + uuid)
    );
  }


  public static Bundle.Entry buildObservationBodyHeight(
    JSONObject examination,
    Bundle.Entry patientEntry,
    Bundle.Entry patientAgeEntry
  ) {
    try {
      return buildObservation(
        JSONObjectUtils.getId(examination, "examination_id") + "-BodyHeight",
        buildCodeableConcept(buildCoding(
          LOINC_SYSTEM,
          "8302-2",
          "Body height"
        )),
        null,
        buildQuantity(
          Decimal.of(examination.getBigDecimal("height")),
          "meter",
          LOINC_SYSTEM,
          examination.getString("height_unit")
        ),
        patientEntry,
        patientAgeEntry,
        examination
      );
    } catch (JSONException e) {
      // @todo just print a warning
      return null;
    }
  }

  public static Bundle.Entry buildObservationBodyWeight(
    JSONObject examination,
    Bundle.Entry patientEntry,
    Bundle.Entry patientAgeEntry
  ) {
    try {
      return buildObservation(
        JSONObjectUtils.getId(examination, "examination_id") + "-BodyWeight",
        buildCodeableConcept(buildCoding(
          LOINC_SYSTEM,
          "29463-7",
          "Body weight"
        )),
        null,
        buildQuantity(
          Decimal.of(examination.getInt("weight")),
          "kilogram",
          LOINC_SYSTEM,
          examination.getString("weight_unit")
        ),
        patientEntry,
        patientAgeEntry,
        examination
      );
    } catch (JSONException e) {
      // @todo just print a warning
      return null;
    }
  }

  public static Bundle.Entry buildObservationGlycosilatedEmoglobin(
    JSONObject examination,
    Bundle.Entry patientEntry,
    Bundle.Entry patientAgeEntry
  ) {
    try {
      return buildObservation(
        JSONObjectUtils.getId(examination, "examination_id") + "-GlycosilatedEmoglobin",
        buildCodeableConcept(buildCoding(
          LOINC_SYSTEM,
          "59261-8",
          "Hemoglobin A1c/Hemoglobin.total in Blood by IFCC protocol"
        )),
        null,
        buildQuantity(
          Decimal.of(examination.getBigDecimal("glycosilated_emoglobin")),
          "millimole per mole",
          UNITSOFM_SYSTEM,
          examination.getString("glycosilated_emoglobin_unit")
        ),
        patientEntry,
        patientAgeEntry,
        examination
      );
    } catch (JSONException e) {
      // @todo just print a warning
      return null;
    }
  }

  public static Bundle.Entry buildObservationTotalCholesterol(
    JSONObject examination,
    Bundle.Entry patientEntry,
    Bundle.Entry patientAgeEntry
  ) {
    try {
      return buildObservation(
        JSONObjectUtils.getId(examination, "examination_id") + "-TotalCholesterol",
        buildCodeableConcept(buildCoding(
          LOINC_SYSTEM,
          "2093-3",
          "Cholesterol [Mass/volume] in Serum or Plasma"
        )),
        null,
        buildQuantity(
          Decimal.of(examination.getBigDecimal("total_cholesterol")),
          "milligram per deciliter",
          LOINC_SYSTEM,
          examination.getString("total_cholesterol_unit")
        ),
        patientEntry,
        patientAgeEntry,
        examination
      );
    } catch (JSONException e) {
      // @todo just print a warning
      return null;
    }
  }

  public static Bundle.Entry buildObservationHDL(
    JSONObject examination,
    Bundle.Entry patientEntry,
    Bundle.Entry patientAgeEntry
  ) {
    try {
      return buildObservation(
        JSONObjectUtils.getId(examination, "examination_id") + "-HDL",
        buildCodeableConcept(buildCoding(
          LOINC_SYSTEM,
          "2085-9",
          "Cholesterol in HDL [Mass/volume] in Serum or Plasma"
        )),
        null,
        buildQuantity(
          Decimal.of(examination.getBigDecimal("HDL")),
          "milligram per deciliter",
          LOINC_SYSTEM,
          examination.getString("HDL_unit")
        ),
        patientEntry,
        patientAgeEntry,
        examination
      );
    } catch (JSONException e) {
      // @todo just print a warning
      return null;
    }
  }

  public static Bundle.Entry buildObservationLDL(
    JSONObject examination,
    Bundle.Entry patientEntry,
    Bundle.Entry patientAgeEntry
  ) {
    try {
      return buildObservation(
        JSONObjectUtils.getId(examination, "examination_id") + "-LDL",
        buildCodeableConcept(buildCoding(
          LOINC_SYSTEM,
          "2089-1",
          "Cholesterol in LDL [Mass/volume] in Serum or Plasma"
        )),
        null,
        buildQuantity(
          Decimal.of(examination.getBigDecimal("LDL")),
          "milligram per deciliter",
          LOINC_SYSTEM,
          examination.getString("LDL_unit")
        ),
        patientEntry,
        patientAgeEntry,
        examination
      );
    } catch (JSONException e) {
      // @todo just print a warning
      return null;
    }
  }

  public static Bundle.Entry buildObservationTriglycerides(
    JSONObject examination,
    Bundle.Entry patientEntry,
    Bundle.Entry patientAgeEntry
  ) {
    try {
      return buildObservation(
        JSONObjectUtils.getId(examination, "examination_id") + "-Triglycerides",
        buildCodeableConcept(buildCoding(
          LOINC_SYSTEM,
          "2571-8",
          "Triglyceride [Mass/volume] in Serum or Plasma"
        )),
        null,
        buildQuantity(
          Decimal.of(examination.getBigDecimal("triglycerides")),
          "milligram per deciliter",
          LOINC_SYSTEM,
          examination.getString("triglycerides_unit")
        ),
        patientEntry,
        patientAgeEntry,
        examination
      );
    } catch (JSONException e) {
      // @todo just print a warning
      return null;
    }
  }

  public static Bundle.Entry buildObservationTotalCholesterolHDL(
    JSONObject examination,
    Bundle.Entry patientEntry,
    Bundle.Entry patientAgeEntry
  ) {
    try {
      return buildObservation(
        JSONObjectUtils.getId(examination, "examination_id") + "-TotalCholesterolHDL",
        buildCodeableConcept(buildCoding(
          LOINC_SYSTEM,
          "2095-8",
          "Cholesterol in HDL/Cholesterol.total [Mass Ratio] in Serum or Plasma"
        )),
        null,
        buildQuantity(
          Decimal.of(examination.getBigDecimal("TC_HDL")),
          "milligram per deciliter",
          LOINC_SYSTEM,
          examination.getString("TC_HDL_unit")
        ),
        patientEntry,
        patientAgeEntry,
        examination
      );
    } catch (JSONException e) {
      // @todo just print a warning
      return null;
    }
  }

  public static Bundle.Entry buildObservationSerumCreatinine(
    JSONObject examination,
    Bundle.Entry patientEntry,
    Bundle.Entry patientAgeEntry
  ) {
    try {
      return buildObservation(
        JSONObjectUtils.getId(examination, "examination_id") + "-SerumCreatinine",
        buildCodeableConcept(buildCoding(
          LOINC_SYSTEM,
          "2160-0",
          "Creatinine [Mass/volume] in Serum or Plasma"
        )),
        null,
        buildQuantity(
          Decimal.of(examination.getBigDecimal("serum_creatinine")),
          "milligram per deciliter",
          LOINC_SYSTEM,
          examination.getString("serum_creatinine_unit")
        ),
        patientEntry,
        patientAgeEntry,
        examination
      );
    } catch (JSONException e) {
      // @todo just print a warning
      return null;
    }
  }

  public static Bundle.Entry buildObservationAlbuminuriaCreatininuriaRatio(
    JSONObject examination,
    Bundle.Entry patientEntry,
    Bundle.Entry patientAgeEntry
  ) {
    try {
      return buildObservation(
        JSONObjectUtils.getId(examination, "examination_id") + "-AlbuminuriaCreatininuriaRatio",
        buildCodeableConcept(buildCoding(
          LOINC_SYSTEM,
          "14959-1",
          "Microalbumin/Creatinine [Mass Ratio] in Urine"
        )),
        null,
        buildQuantity(
          Decimal.of(examination.getBigDecimal("albuminuria_creatininuria_ratio")),
          "milligram per gram",
          LOINC_SYSTEM,
          examination.getString("albuminuria_creatininuria_ratio_unit")
        ),
        patientEntry,
        patientAgeEntry,
        examination
      );
    } catch (JSONException e) {
      // @todo just print a warning
      return null;
    }
  }

  public static Bundle.Entry buildObservationALT(
    JSONObject examination,
    Bundle.Entry patientEntry,
    Bundle.Entry patientAgeEntry
  ) {
    try {
      return buildObservation(
        JSONObjectUtils.getId(examination, "examination_id") + "-ALT",
        buildCodeableConcept(buildCoding(
          LOINC_SYSTEM,
          "1742-6",
          "Alanine aminotransferase [Enzymatic activity/volume] in Serum or Plasma"
        )),
        null,
        buildQuantity(
          Decimal.of(examination.getBigDecimal("GPT_ALT")),
          "enzyme unit per liter",
          LOINC_SYSTEM,
          examination.getString("GPT_ALT_unit")
        ),
        patientEntry,
        patientAgeEntry,
        examination
      );
    } catch (JSONException e) {
      // @todo just print a warning
      return null;
    }
  }

  public static Bundle.Entry buildObservationAST(
    JSONObject examination,
    Bundle.Entry patientEntry,
    Bundle.Entry patientAgeEntry
  ) {
    try {
      return buildObservation(
        JSONObjectUtils.getId(examination, "examination_id") + "-AST",
        buildCodeableConcept(buildCoding(
          LOINC_SYSTEM,
          "1920-8",
          "Aspartate aminotransferase [Enzymatic activity/volume] in Serum or Plasma"
        )),
        null,
        buildQuantity(
          Decimal.of(examination.getBigDecimal("GOT_AST")),
          "enzyme unit per liter",
          LOINC_SYSTEM,
          examination.getString("GOT_AST_unit")
        ),
        patientEntry,
        patientAgeEntry,
        examination
      );
    } catch (JSONException e) {
      // @todo just print a warning
      return null;
    }
  }

  public static Bundle.Entry buildObservationGammaGT(
    JSONObject examination,
    Bundle.Entry patientEntry,
    Bundle.Entry patientAgeEntry
  ) {
    try {
      return buildObservation(
        JSONObjectUtils.getId(examination, "examination_id") + "-GammaGT",
        buildCodeableConcept(buildCoding(
          LOINC_SYSTEM,
          "2324-2",
          "Gamma glutamyl transferase [Enzymatic activity/volume] in Serum or Plasma"
        )),
        null,
        buildQuantity(
          Decimal.of(examination.getBigDecimal("gammaGT")),
          "international unit per liter",
          LOINC_SYSTEM,
          examination.getString("gammaGT_unit")
        ),
        patientEntry,
        patientAgeEntry,
        examination
      );
    } catch (JSONException e) {
      // @todo just print a warning
      return null;
    }
  }

  public static Bundle.Entry buildObservationAlkalinePhosphatase(
    JSONObject examination,
    Bundle.Entry patientEntry,
    Bundle.Entry patientAgeEntry
  ) {
    try {
      return buildObservation(
        JSONObjectUtils.getId(examination, "examination_id") + "-AlkalinePhosphatase",
        buildCodeableConcept(buildCoding(
          LOINC_SYSTEM,
          "6768-6",
          "Alkaline phosphatase [Enzymatic activity/volume] in Serum or Plasma"
        )),
        null,
        buildQuantity(
          Decimal.of(examination.getBigDecimal("alkaline_phosphatase")),
          "international unit per liter",
          LOINC_SYSTEM,
          examination.getString("alkaline_phosphatase_unit")
        ),
        patientEntry,
        patientAgeEntry,
        examination
      );
    } catch (JSONException e) {
      // @todo just print a warning
      return null;
    }
  }

  public static Bundle.Entry buildObservationUricAcid(
    JSONObject examination,
    Bundle.Entry patientEntry,
    Bundle.Entry patientAgeEntry
  ) {
    try {
      return buildObservation(
        JSONObjectUtils.getId(examination, "examination_id") + "-UricAcid",
        buildCodeableConcept(buildCoding(
          LOINC_SYSTEM,
          "3084-1",
          "Urate [Mass/volume] in Serum or Plasma"
        )),
        null,
        buildQuantity(
          Decimal.of(examination.getBigDecimal("uric_acid")),
          "milligram per deciliter",
          LOINC_SYSTEM,
          examination.getString("uric_acid_unit")
        ),
        patientEntry,
        patientAgeEntry,
        examination
      );
    } catch (JSONException e) {
      // @todo just print a warning
      return null;
    }
  }

  public static Bundle.Entry buildObservationGFR(
    JSONObject examination,
    Bundle.Entry patientEntry,
    Bundle.Entry patientAgeEntry
  ) {
    try {
      return buildObservation(
        JSONObjectUtils.getId(examination, "examination_id") + "-GFR",
        buildCodeableConcept(buildCoding(
          LOINC_SYSTEM,
          "48642-3",
          "Glomerular filtration rate/1.73 sq M.predicted among non-blacks [Volume Rate/Area] in Serum, Plasma or Blood by Creatinine-based formula (MDRD)"
        )),
        null,
        buildQuantity(
          Decimal.of(examination.getBigDecimal("eGFR")),
          examination.getString("eGFR_unit"),
          LOINC_SYSTEM,
          examination.getString("eGFR_unit")
        ),
        patientEntry,
        patientAgeEntry,
        examination
      );
    } catch (JSONException e) {
      // @todo just print a warning
      return null;
    }
  }

  public static Bundle.Entry buildObservationNitrites(
    JSONObject examination,
    Bundle.Entry patientEntry,
    Bundle.Entry patientAgeEntry
  ) {
    try {
      return buildObservation(
        JSONObjectUtils.getId(examination, "examination_id") + "-Nitrites",
        buildCodeableConcept(buildCoding(
          LOINC_SYSTEM,
          "5802-4",
          "Nitrite [Presence] in Urine by Test strip"
        )),
        null,
        Boolean.builder()
          .value(examination.getBoolean("nitrites"))
          .build(),
        patientEntry,
        patientAgeEntry,
        examination
      );
    } catch (JSONException e) {
      // @todo just print a warning
      return null;
    }
  }

  public static Bundle.Entry buildObservationBloodPressure(
    JSONObject examination,
    Bundle.Entry patientEntry,
    Bundle.Entry patientAgeEntry
  ) {
    if ( !examination.has("systolic_pressure")
      && !examination.has("diastolic_pressure")
    ) return null;
    try {
      Collection<Observation.Component> components = new LinkedList<>();
      //
      // systolic_pressure
      if (examination.has("systolic_pressure")) {
        Observation.Component systolicPressure = buildObservationComponent(
          buildCodeableConcept(buildCoding(
            LOINC_SYSTEM,
            "8480-6",
            "Systolic blood pressure"
          )),
          buildQuantity(
            Decimal.of(examination.getInt("systolic_pressure")),
            "millimeter of mercury",
            LOINC_SYSTEM,
            examination.getString("systolic_pressure_unit")
          )
        );
        components.add(systolicPressure);
      }
      //
      // systolic_pressure
      if (examination.has("diastolic_pressure")) {
        Observation.Component diastolicPressure = buildObservationComponent(
          buildCodeableConcept(buildCoding(
            LOINC_SYSTEM,
            "8462-4",
            "Diastolic blood pressure"
          )),
          buildQuantity(
            Decimal.of(examination.getInt("diastolic_pressure")),
            "millimeter of mercury",
            LOINC_SYSTEM,
            examination.getString("diastolic_pressure_unit")
          )
        );
        components.add(diastolicPressure);
      }

      return buildObservation(
        JSONObjectUtils.getId(examination, "examination_id") + "-BloodPressure",
        buildCodeableConcept(buildCoding(
          LOINC_SYSTEM,
          "85354-9",
          "Blood pressure panel with all children optional"
        )),
        components,
        null,
        patientEntry,
        patientAgeEntry,
        examination
      );
    } catch (JSONException e) {
      // @todo just print a warning
      return null;
    }
  }

  public static Bundle.Entry buildObservationYearsWithDiabetes(
    JSONObject examination,
    Bundle.Entry patientEntry,
    Bundle.Entry patientAgeEntry
  ) {
    try {
      return buildObservation(
        JSONObjectUtils.getId(examination, "examination_id") + "-YearsWithDiabetes",
        buildCodeableConcept(buildCoding(
          "https://www.phenxtoolkit.org/",
          "PX070801190200",
          "PX070801 Diabetes Mellitus Year"
        )),
        null,
        buildQuantity(
          Decimal.of(examination.getInt("years_with_diabetes")),
          "year",
          UNITSOFM_SYSTEM,
          "a_j" // TODO check here https://ucum.org/ucum#para-29
        ),
        patientEntry,
        patientAgeEntry,
        examination
      );
    } catch (JSONException e) {
      // @todo just print a warning
      return null;
    }
  }

  //
  // Conditions
  //

  public static Bundle.Entry buildConditionHepaticSteatosis(
    JSONObject examination,
    Bundle.Entry patientEntry
  ) {
    try {
      return buildCondition(
        JSONObjectUtils.getId(examination, "examination_id") + "-HepaticSteatosis",
        buildCodeableConcept(
          buildCoding(
            DOID_SYSTEM,
            "DOID_9452",
            "Steatosis of liver (disorder)"
          ),
          String.valueOf(
            examination.getBoolean("hepatic_steatosis")
          )
        ),
        patientEntry,
        examination
      );
    } catch (JSONException e) {
      // @todo just print a warning
      return null;
    }
  }

  public static Bundle.Entry buildConditionHypertension(
    JSONObject examination,
    Bundle.Entry patientEntry
  ) {
    try {
      return buildCondition(
        JSONObjectUtils.getId(examination, "examination_id") + "-Hypertension",
        buildCodeableConcept(
          buildCoding(
            DOID_SYSTEM,
            "DOID_10825",
            "Hypertensive disorder, systemic arterial (disorder)"
          ),
          String.valueOf(
            examination.getBoolean("hypertension")
          )
        ),
        patientEntry,
        examination
      );
    } catch (JSONException e) {
      // @todo just print a warning
      return null;
    }
  }

  public static Bundle.Entry buildConditionHeartFailure(
    JSONObject examination,
    Bundle.Entry patientEntry
  ) {
    try {
      return buildCondition(
        JSONObjectUtils.getId(examination, "examination_id") + "-HeartFailure",
        buildCodeableConcept(
          buildCoding(
            DOID_SYSTEM,
            "DOID_6000",
            "Heart failure (disorder)"
          ),
          String.valueOf(
            examination.getBoolean("heart_failure")
          )
        ),
        patientEntry,
        examination
      );
    } catch (JSONException e) {
      // @todo just print a warning
      return null;
    }
  }

  public static Bundle.Entry buildConditionBPCO(
    JSONObject examination,
    Bundle.Entry patientEntry
  ) {
    try {
      return buildCondition(
        JSONObjectUtils.getId(examination, "examination_id") + "-BPCO",
        buildCodeableConcept(
          buildCoding(
            DOID_SYSTEM,
            "DOID_3083",
            "Chronic obstructive lung disease (disorder)"
          ),
          String.valueOf(
            examination.getBoolean("bpco")
          )
        ),
        patientEntry,
        examination
      );
    } catch (JSONException e) {
      // @todo just print a warning
      return null;
    }
  }

  public static Bundle.Entry buildConditionChronicKidneyDisease(
    JSONObject examination,
    Bundle.Entry patientEntry
  ) {
    try {
      return buildCondition(
        JSONObjectUtils.getId(examination, "examination_id") + "-ChronicKidneyDisease",
        buildCodeableConcept(
          buildCoding(
            DOID_SYSTEM,
            "DOID_784",
            "Chronic kidney disease (disorder)"
          ),
          String.valueOf(
            examination.getBoolean("chronic_kidney_disease")
          )
        ),
        patientEntry,
        examination
      );
    } catch (JSONException e) {
      // @todo just print a warning
      return null;
    }
  }

  public static Bundle.Entry buildConditionIschemicHeartDisease(
    JSONObject examination,
    Bundle.Entry patientEntry
  ) {
    try {
      return buildCondition(
        JSONObjectUtils.getId(examination, "examination_id") + "-IschemicHeartDisease",
        buildCodeableConcept(
          buildCoding(
            DOID_SYSTEM,
            "DOID_3393",
            "Ischemic heart disease (disorder)"
          ),
          String.valueOf(
            examination.getBoolean("ischemic_heart_disease")
          )
        ),
        patientEntry,
        examination
      );
    } catch (JSONException e) {
      // @todo just print a warning
      return null;
    }
  }

  //--------------------------------------------------------------------------//
  // Class definition
  //--------------------------------------------------------------------------//

  /**
   * @todo description
   */
  private CSSBuilder() {
    super();
  }

  public static Bundle.Entry buildObservation(
    String uuid,
    CodeableConcept code,
    Collection<Observation.Component> components,
    Element value,
    Bundle.Entry subject,
    Bundle.Entry member,
    JSONObject examination
  ) {
    if (components == null) {
      components = new LinkedList<>();
    }
    return buildEntry(
      Observation.builder()
        .id(uuid)
        .status(ObservationStatus.FINAL)
        .identifier(
          buildIdentifier(
            BASE_URL + "/identifier",
            "Observation/" + uuid
          )
        )
        .code(code)
        .effective(
          buildDate(examination.getString("date"))
        )
        .component(components)
        .value(value)
        .hasMember(
          buildReference(member)
        )
        .subject(
          buildReference(subject)
        )
        .build(),
      FHIR_OBSERVATION_TYPE,
      null,
      buildFullUrl(BASE_URL + "/observation/" + uuid)
    );
  }

  public static Observation.Component buildObservationComponent(
    CodeableConcept code,
    Element value
  ) {
    return Observation.Component.builder()
      .code(code)
      .value(value)
      .build();
  }

  public static Bundle.Entry buildCondition(
    String uuid,
    CodeableConcept code,
    Bundle.Entry subject,
    JSONObject examination
  ) {
    return buildEntry(
      Condition.builder()
        .id(uuid)
        .code(code)
        .recordedDate(
          buildDate(examination.getString("date"))
        )
        .subject(
          buildReference(subject)
        )
        .build(),
      FHIR_CONDITION_TYPE
    );
  }

}