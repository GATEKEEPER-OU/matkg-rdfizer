package org.ou.gatekeeper.fhir.adapters.sh;

import com.google.common.base.CaseFormat;
import com.ibm.fhir.model.resource.*;
import com.ibm.fhir.model.type.*;
import com.ibm.fhir.model.type.code.AppointmentStatus;
import com.ibm.fhir.model.type.code.ObservationStatus;
import com.ibm.fhir.model.type.code.ParticipationStatus;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.text.CaseUtils;
import org.commons.JSONObjectUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.ou.gatekeeper.fhir.adapters.FHIRBaseBuilder;

import java.lang.String;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.Date;
import java.util.List;

class SHBuilder extends FHIRBaseBuilder {

  private static String pilotId;

  public static final String BASE_URL = "https://opensource.samsung.com/projects/helifit";
  public static final String SAMSUNG_LIVE_SYSTEM = "http://samsung/live-data";

  //--------------------------------------------------------------------------//
  // Gets
  //--------------------------------------------------------------------------//

  public static boolean hasValue(JSONObject dataElement, String key) {
    JSONArray values = dataElement.getJSONArray("values");
    JSONObject element = values.getJSONObject(0);
    return element.has(key);
  }

  public static String getValue(JSONObject dataElement, String key) {
    JSONArray values = dataElement.getJSONArray("values");
    JSONObject element = values.getJSONObject(0);
    if (element.has(key)) {
      String value = element.getString(key);
      if (StringUtils.isBlank(value)) {
        String message = String.format("Property '%s' is blank", key);
        LOGGER.warn(message);
      }
      return value;
    }
    return ""; // TODO request to remove empty values
  }

  public static String getCountType(String value) {
    switch (value) {
      case "30001": return "stride";
      case "30002": return "stroke";
      case "30003": return "swing";
      case "30004": return "repetition";
      default:
        return "undefined";
    }
  }

  public static Resource buildAppointmentResource(
          JSONObject vEvent,
          String eventType,
          Bundle.Entry patientEntry,
          JSONObject vCalendar,
          Resource parentAppointment
  ) {
    String timezone = vCalendar.getString("X-WR-TIMEZONE");
    String resourceId = UUID.randomUUID().toString();
    Resource resource = Appointment.builder()
            .status(getAppointmentStatus(vEvent.getString("STATUS")))
            .appointmentType(createAppointmentType(eventType))
            .participant(createAppointmentParticipant(patientEntry.getResource().getId(), "Patient"))
            .start(createAppointmentTime(vEvent.getString("DTSTART"), timezone))
            .end(createAppointmentTime(vEvent.getString("DTEND"), timezone))
            .supportingInformation(Reference.builder()
                    .reference("Appointment/" + parentAppointment.getId())
                    .display(parentAppointment.getId())
                    .build())
            .id(resourceId)
            .identifier(Identifier.builder()
                    .system(Uri.builder().value("https://opensource.samsung.com/projects/helifit/identifier").build())
                    .value(resourceId)
                    .build())
            .meta(Meta.builder().lastUpdated(createAppointmentTime(vEvent.getString("LAST-MODIFIED"), timezone)).build())
            .created(DateTime.of(createAppointmentTime(vEvent.getString("LAST-MODIFIED"), timezone)))
            .extension(
                    Extension.builder().value(vEvent.getString("X-GOOGLE-CONFERENCE")).url("X-GOOGLE-CONFERENCE").build(),
                    Extension.builder().value(vEvent.getString("LOCATION")).url("LOCATION").build(),
                    Extension.builder().value(vEvent.getString("SEQUENCE")).url("SEQUENCE").build(),
                    Extension.builder().value(vEvent.getString("SUMMARY")).url("SUMMARY").build(),
                    Extension.builder().value(vEvent.getString("TRANSP")).url("TRANSP").build(),
                    Extension.builder().value(vCalendar.getString("CALSCALE")).url("CALSCALE").build(),
                    Extension.builder().value(vCalendar.getString("METHOD")).url("METHOD").build(),
                    Extension.builder().value(vCalendar.getString("X-WR-CALNAME")).url("X-WR-CALNAME").build(),
                    Extension.builder().value(vCalendar.getString("X-WR-TIMEZONE")).url("X-WR-TIMEZONE").build()
            )
            .description(vEvent.getString("DESCRIPTION"))
            .build();

    return  resource;
  }

  public static CodeableConcept createAppointmentType(String type) {
    String appointmentType = "";
    switch (type) {
      case "VEVENT":
        appointmentType = "eventc";
        break;
      case "VTODO":
        appointmentType = "todoc";
        break;
      case "VJOURNAL":
        appointmentType = "journalc";
        break;
      case "VFREEBUSY":
        appointmentType = "freebusyc";
        break;
      case "VTIMEZONE":
        appointmentType = "timezonec";
        break;
      default:
        appointmentType = type;
        break;
    }
    return  CodeableConcept
            .builder()
            .coding(Coding.builder().code(Code.builder().value(appointmentType).build()).display(appointmentType).system(Uri.uri(LOCAL_SYSTEM)).build())
            .build();
  }

  public static ZonedDateTime createAppointmentTime(String dateTime, String tz) {
    try {
      String fixedDateStr = dateTime.substring(0, 4)
              + "/"
              + dateTime.substring(4, 6)
              + "/"
              + dateTime.substring(6, 8)
              + " "
              + dateTime.substring(9, 15);
      TimeZone timeZone = TimeZone.getTimeZone(tz);
      SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd hhmmss");
      format.setTimeZone(timeZone);
      Date c = format.parse(fixedDateStr);
      String pattern = DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.getPattern();
      String isoDateTime = DateFormatUtils.format(c.getTime(), pattern, timeZone);
      return ZonedDateTime.from(DateTime.builder().value(isoDateTime).build().getValue());
    } catch (ParseException ex) {
      return null;
    }
  }

  public static Appointment.Participant createAppointmentParticipant(String patientId, String resourceType) {
    return Appointment.Participant
            .builder()
            .actor(Reference.builder().reference(resourceType + "/" + patientId).display(patientId).build())
            .status(ParticipationStatus.ACCEPTED)
            .build();
  }

  public static AppointmentStatus getAppointmentStatus(String currentStatus) {
    switch (currentStatus) {
      case "CONFIRMED":
        return AppointmentStatus.builder().value(AppointmentStatus.BOOKED.getValue()).build();
      default:
        return AppointmentStatus.builder().value(AppointmentStatus.PROPOSED.getValue()).build();
    }
  }

  public static CodeableConcept getCodes(JSONObject dataElement) {
    String typeId = dataElement.getString("type_id");
    switch (typeId) {
      case "height":
        return CodeableConcept.builder()
          .coding(
            buildCoding(
              LOINC_SYSTEM,
              "8302-2",
              "Body height"
            )
          )
          .build();
      case "weight":
        return CodeableConcept.builder()
          .coding(
            buildCoding(
              LOINC_SYSTEM,
              "29463-7",
              "Body weight"
            )
          )
          .build();
      case "bloodPressure":
        return CodeableConcept.builder()
          .coding(
            buildCoding(
              LOINC_SYSTEM,
              "85354-9",
              "Blood pressure"
            )
          )
          .build();
      case "heartRate":
        return CodeableConcept.builder()
          .coding(
            buildCoding(
              LOCAL_SYSTEM,
              "heart_rate_sampling",
              "Heart Rate Sampling"
            )
          )
          .build();
      case "exercise":
        return CodeableConcept.builder()
          .coding(
            buildCoding(
              LOINC_SYSTEM,
              "LA11834-1",
              "Exercise"
            ),
            getExerciseCode(dataElement)
          )
          .build();
      case "sleep":
        return CodeableConcept.builder()
          .coding(
            buildCoding(
              LOINC_SYSTEM,
              "93832-4",
              "Sleep"
            )
          )
          .build();
      default:
        String display = CaseUtils.toCamelCase(typeId, true);
        String code = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, display);
        return CodeableConcept.builder()
          .coding(
            buildCoding(
              LOCAL_SYSTEM,
              code,
              display
            )
          )
          .build();
    }
  }

  public static Coding getExerciseCode(JSONObject dataElement) {
    String typeId = getValue(dataElement, "exercise_description");
    switch (typeId) {
      case "Walking":
        return buildCoding(
          LOINC_SYSTEM,
          "82948-1",
          typeId
        );
      default:
        String display = CaseUtils.toCamelCase(typeId, true);
        String code = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, display);
        return buildCoding(
          LOCAL_SYSTEM,
          code,
          display
        );
    }
  }

  public static Quantity getMainValue(JSONObject dataElement) {
    String typeId = dataElement.getString("type_id");
    switch (typeId) {
      case "height":
        return buildQuantity(
          Decimal.of(
            SHBuilder.getValue(dataElement, "height")
          ),
          "centimeter",
          UNITSOFM_SYSTEM,
          "cm"
        );
      case "weight":
        return buildQuantity(
          Decimal.of(
            SHBuilder.getValue(dataElement, "weight")
          ),
          "kilogram",
          UNITSOFM_SYSTEM,
          "Kg"
        );
      case "waterIntake":
      case "caffeineIntake":
        return buildQuantity(
          Decimal.of(
            SHBuilder.getValue(dataElement, "amount")
          ),
          "milliliters",
          UNITSOFM_SYSTEM,
          "mL"
        );
      case "floorsClimbed":
        return buildQuantity(
          Decimal.of(
            SHBuilder.getValue(dataElement, "floor")
          ),
          "floor",
          LOCAL_SYSTEM,
          "floor"
        );
      default:
        return null;
    }
  }

  private static String getTimeOffset(JSONObject dataElement) {
    String key = "time_offset";
    JSONArray values = dataElement.getJSONArray("values");
    JSONObject element = values.getJSONObject(0);
    return element.has(key) ? element.getString(key) : "UTC+0000";
  }

  //--------------------------------------------------------------------------//
  // Builders
  //--------------------------------------------------------------------------//

  /**
   * @todo description
   */
  public static Bundle.Entry buildPatient(JSONObject patient) {
//    String patientId = JSONObjectUtils.getId(patient, "user_uuid");
    String patientEmail = patient.getString("user_id");
    String patientId = patientEmail.replace("@", "-");
    String fullUrl = BASE_URL + "/patient/" + patientId;
//    String fullUrl = BASE_URL + "/patient/" + patientEmail;
    return buildEntry(
      Patient.builder()
        .id(patientId)
        .identifier(
//          buildIdentifier(
//            BASE_URL + "/identifier",
//            patientId
//          ),
          buildIdentifier(
            BASE_URL + "/identifier",
            patientEmail
          )
        )
        .build(),
      "Patient",
      "identifier=" + BASE_URL + "/patient|" + patientId,
      buildFullUrl(fullUrl)
    );
  }

  /**
   * @todo description
   */
  public static Bundle.Entry buildMainObservation(
    JSONObject dataElement,
    Collection<Observation.Component> components,
    Quantity value,
    Bundle.Entry patientEntry
  ) {
    pilotId = dataElement.getString("pilot_id"); // WORKAROUND
    String       uuid = dataElement.getString("data_uuid");
    String   deviceId = dataElement.getString("device_id");
    String zoneOffset = getTimeOffset(dataElement); // TODO re-think again in the future
    String  startTime = getValue(dataElement, "start_time");
    String endTime = hasValue(dataElement, "end_time")
      ? getValue(dataElement, "end_time")
      : null;

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
          getCodes(dataElement)
        )
        .effective(
          endTime != null
            ? buildPeriod(startTime, endTime, zoneOffset)
            : buildDateTime(startTime, zoneOffset)
        )
        .component(components)
        .value(value)
        .device(
          buildReference(buildIdentifier(
            BASE_URL + "/device", deviceId
          ))
        )
        .subject(
          buildReference(patientEntry)
        )
        .build(),
      "Observation",
      null,
      buildFullUrl(BASE_URL + "/observation/" + uuid)
    );
  }

  public static Bundle.Entry buildAggregatedObservation(
    String id,
    JSONObject dataElement,
    CodeableConcept codes,
    Collection<Observation.Component> components,
    Quantity quantity,
    Bundle.Entry parentEntry,
    Bundle.Entry patientEntry
  ) {
    String   deviceId = dataElement.getString("device_id");
    String  startTime = getValue(dataElement, "start_time");
    String endTime = hasValue(dataElement, "end_time")
            ? getValue(dataElement, "end_time")
            : null;
    String zoneOffset = getTimeOffset(dataElement); // TODO re-think again in the future
    return buildEntry(
      Observation.builder()
        .id(id)
        .status(ObservationStatus.FINAL)
        .identifier(buildIdentifier(
          BASE_URL + "/identifier", id
        ))
        .code(codes)
        .component(components)
        .value(quantity)
        .effective(
                endTime != null
                        ? buildPeriod(startTime, endTime, zoneOffset)
                        : buildDateTime(startTime, zoneOffset)
        )
        .device(
          buildReference(buildIdentifier(
            BASE_URL + "/device", deviceId
          ))
        )
        .derivedFrom(
          buildReference(parentEntry)
        )
        .subject(
          buildReference(patientEntry)
        )
        .build(),
      "Observation",
      null,
      buildFullUrl(BASE_URL + "/observation/" + id)
    );
  }

  public static Bundle.Entry buildLiveObservation(
    String id,
    JSONObject liveElement,
    JSONObject dataElement,
    CodeableConcept codes,
    Quantity quantity,
    Bundle.Entry parentEntry,
    Bundle.Entry patientEntry
  ) {
    String   deviceId = dataElement.getString("device_id");
    String  startTime = liveElement.getString("start_time");
    String    endTime = liveElement.getString("end_time");
    String zoneOffset = getTimeOffset(dataElement); // TODO re-think again in the future
    return buildEntry(
      Observation.builder()
        .id(id)
        .status(ObservationStatus.FINAL)
        .identifier(buildIdentifier(
          BASE_URL + "/identifier", id
        ))
        .code(codes)
        .value(quantity)
        .effective(
          buildPeriod(startTime, endTime, zoneOffset)
        )
        .device(
          buildReference(buildIdentifier(
            BASE_URL + "/device", deviceId
          ))
        )
        .derivedFrom(
          buildReference(parentEntry)
        )
        .subject(
          buildReference(patientEntry)
        )
        .build(),
      "Observation",
      null,
      buildFullUrl(BASE_URL + "/observation/" + id)
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

  public static Bundle.Entry buildLocation(
    String id,
    JSONObject locationElement,
    JSONObject dataElement,
    Collection<Observation.Component> components,
    Bundle.Entry parentEntry,
    Bundle.Entry patientEntry
  ) {
    String   deviceId = dataElement.getString("device_id");
    String  startTime = locationElement.getString("start_time");
    String    endTime = locationElement.getString("end_time");
    String zoneOffset = getTimeOffset(dataElement); // TODO re-think again in the future
    return buildEntry(
      Observation.builder()
        .id(id)
        .status(ObservationStatus.FINAL)
        .identifier(buildIdentifier(
          BASE_URL + "/identifier", id
        ))
        .code(buildCodeableConcept(buildCoding(
          LOCAL_SYSTEM,
          "location",
          "Location"
        )))
        .component(components)
        .effective(
          buildPeriod(startTime, endTime, zoneOffset)
        )
        .device(
          buildReference(buildIdentifier(
            BASE_URL + "/device", deviceId
          ))
        )
        .derivedFrom(
          buildReference(parentEntry)
        )
        .subject(
          buildReference(patientEntry)
        )
        .build(),
      "Observation",
      null,
      buildFullUrl(BASE_URL + "/observation/" + id)
    );
  }

  /**
   * @todo description
   */
  public static DateTime buildDateTime(String dateTime, String zoneOffset) {
    String stdDateTime = dateTimeTranslator(dateTime, zoneOffset);
    return DateTime.builder()
      .value(stdDateTime)
      .build();
  }

  /**
   * @todo description
   */
  public static Period buildPeriod(
    String start,
    String end,
    String zoneOffset
  ) {
    return Period.builder()
      .start(DateTime.of(dateTimeTranslator(start, zoneOffset)))
      .end(DateTime.of(dateTimeTranslator(end, zoneOffset)))
      .build();
  }


  //--------------------------------------------------------------------------//
  // Class definition
  //--------------------------------------------------------------------------//

  /**
   * @todo description
   */
  private SHBuilder() {
    super();
  }

  private static String dateTimeTranslator(String timestamp, String zoneOffset){
    TimeZone timeZone;
    // @see https://www.timeanddate.com/time/map/
//    switch (zoneOffset) {
//      case "UTC+0100":
//        timeZone = TimeZone.getTimeZone("Europe/Rome");
//        break;
//      case "UTC+0200":
//        timeZone = TimeZone.getTimeZone("Europe/Athens");
//        break;
//      default:
//        timeZone = TimeZone.getTimeZone("UTC");
//    }

    // @see https://en.wikipedia.org/wiki/List_of_tz_database_time_zones
    switch (pilotId) { // WORKAROUND
      case "UK":
        timeZone = TimeZone.getTimeZone("Europe/London");
        break;
      case "Spain":
      case "SpainBC":
        timeZone = TimeZone.getTimeZone("Europe/Madrid");
        break;
      case "Puglia":
        timeZone = TimeZone.getTimeZone("Europe/Rome");
        break;
      case "Saxony":
        timeZone = TimeZone.getTimeZone("Europe/Berlin");
        break;
      case "Greece":
        timeZone = TimeZone.getTimeZone("Europe/Athens");
        break;
      default:
        throw new IllegalArgumentException("Timezone not allowed");
//        timeZone = TimeZone.getTimeZone("UTC");
    }
    Long lTimestamp = Long.parseLong(timestamp);
    String pattern = DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.getPattern();
    String isoDateTime = DateFormatUtils.format(lTimestamp, pattern, timeZone);
//    System.out.println("zoneOffset>>> " + zoneOffset); // DEBUG
//    System.out.println("isoDateTime>>> " + isoDateTime); // DEBUGi wa
    return isoDateTime;
  }

}