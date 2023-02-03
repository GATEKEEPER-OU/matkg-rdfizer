package org.ou.gatekeeper.adapters;

import com.ibm.fhir.model.resource.Bundle;
import com.ibm.fhir.model.resource.Device;
import com.ibm.fhir.model.resource.Resource;
import com.ibm.fhir.model.type.*;
import com.ibm.fhir.model.type.code.HTTPVerb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.String;
import java.sql.Timestamp;
import java.util.UUID;

/**
 * @author Riccardo Pala (riccardo.pala@open.ac.uk)
 * @todo description
 */
public abstract class BaseBuilder {

  public static final String LOCAL_SYSTEM = "http://local-system";

  /**
   * Base URL of Logical Observation Identifiers Names and Codes
   * <a href="https://loinc.org/search">LOINC_SYSTEM</a>
   * */
  public static final String LOINC_SYSTEM = "http://loinc.org";

  /**
   * Base URL of Unified Code for Units of Measure
   * <a href="https://ucum.org/ucum#section-Base-Units">UNITSOFM_SYSTEM</a>
   * */
  public static final String UNITSOFM_SYSTEM = "http://unitsofmeasure.org";

  public static final String DOID_SYSTEM = "http://purl.obolibrary.org/obo/";

  public static final String HL7_SYSTEM = "http://terminology.hl7.org/CodeSystem";
  public static final String HL7_STRUCTURE = "http://hl7.org/fhir/StructureDefinition";

  public static final Logger LOGGER = LoggerFactory.getLogger(BaseBuilder.class);

  /**
   * @todo description
   */
  public static Uri buildFullUrl() {
    return buildFullUrl("urn:uuid:" + UUID.randomUUID());
  }

  /**
   * @todo description
   */
  public static Uri buildFullUrl(String uri) {
    return Uri.uri(uri);
  }

  /**
   * @todo description
   */
  public static Bundle.Entry buildEntry(Resource resource, String requestUrl) {
    return buildEntry(resource, requestUrl, null, buildFullUrl());
  }

  /**
   * @todo description
   */
  public static Bundle.Entry buildEntry(Resource resource, String requestUrl, String requestIdentifier) {
    return buildEntry(resource, requestUrl, requestIdentifier, buildFullUrl());
  }

  /**
   * @todo description
   */
  public static Bundle.Entry buildEntry(
    Resource resource,
    String requestUrl,
    String requestIdentifier,
    Uri fullUrl
  ) {
    Bundle.Entry.Request.Builder request = Bundle.Entry.Request.builder()
      .method(HTTPVerb.POST)
      .url(Uri.uri(requestUrl));

    if (requestIdentifier != null) {
      request = request.ifNoneExist(requestIdentifier);
    }

    return Bundle.Entry.builder()
      .fullUrl(fullUrl)
      .request(request.build())
      .resource(resource)
      .build();
  }

  public static Identifier buildIdentifier(String system, String value) {
    return Identifier.builder()
      .system(Uri.uri(system))
      .value(value)
      .build();
  }

  public static Device buildDevice(Identifier identifier) {
    return Device.builder()
      .identifier(identifier)
      .build();
  }

  // @todo refactory buildCodeableConcept split and optimize implementing buildCoding()

  public static Coding buildCoding(String system, String code, String display) {
    return Coding.builder()
      .system(
        Uri.uri(system)
      )
      .code(Code.code(code))
      .display(display)
      .build();
  }

  /**
   * @todo description
   */
  public static CodeableConcept buildCodeableConcept(Coding coding) {
    return buildCodeableConcept(coding, null);
  }

  /**
   * @todo description
   */
  public static CodeableConcept buildCodeableConcept(Coding coding, String text) {
    CodeableConcept.Builder cc = CodeableConcept.builder();
    if (coding != null) {
      cc = cc.coding(coding);
    }
    if (text != null) {
      cc = cc.text(text);
    }
    return cc.build();
  }

  /**
   * @todo description
   */
  public static Timestamp toTimestamp(String dateTime) {
    Long l = Long.parseLong(dateTime);
    return new Timestamp(l);
  }

  /**
   * @todo description
   */
  public static Quantity buildQuantity(
    Decimal value,
    String unit,
    String system,
    String code
  ) {
    return Quantity.builder()
      .value(value)
      .unit(unit)
      .system(Uri.uri(system))
      .code(Code.code(code))
      .build();
  }

  /**
   * @todo description
   */
  public static Reference buildReference(Identifier identifier) {
    return Reference.builder()
      .identifier(identifier)
      .build();
  }

  /**
   * @todo description
   */
  public static Reference buildReference(Bundle.Entry entry) {
    return Reference.builder()
      .reference(
        entry.getFullUrl().getValue()
      )
      .display(entry.getResource().getId())
      .build();
  }

  /**
   * @todo description
   */
  protected static final String FHIR_OBSERVATION_TYPE = "Observation";

  /**
   * @todo description
   */
  protected static final String FHIR_CONDITION_TYPE = "Condition";

  /**
   * @todo description
   */
  protected BaseBuilder() {
  }

}