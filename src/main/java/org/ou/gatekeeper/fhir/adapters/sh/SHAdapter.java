package org.ou.gatekeeper.fhir.adapters.sh;

import com.ibm.fhir.model.format.Format;
import com.ibm.fhir.model.generator.FHIRGenerator;
import com.ibm.fhir.model.generator.exception.FHIRGeneratorException;
import com.ibm.fhir.model.resource.Appointment;
import com.ibm.fhir.model.resource.Bundle;
import com.ibm.fhir.model.resource.Observation;
import com.ibm.fhir.model.resource.Resource;
import com.ibm.fhir.model.type.*;
import com.ibm.fhir.model.type.code.AppointmentStatus;
import com.ibm.fhir.model.type.code.BundleType;
import com.ibm.fhir.model.visitor.Visitable;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.commons.JSONObjectUtils;
import org.commons.ResourceUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.ou.gatekeeper.fhir.adapters.FHIRAdapter;
import org.ou.gatekeeper.fhir.adapters.FHIRBaseBuilder;
import org.ou.gatekeeper.fhir.helpers.FHIRNormalizer;

import java.io.*;
import java.lang.String;
import java.nio.file.Files;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import static org.apache.commons.collections.CollectionUtils.addIgnoreNull;
import static org.commons.ResourceUtils.generateUniqueFilename;
import static org.ou.gatekeeper.fhir.adapters.sh.SHBuilder.*;

/**
 * @author Riccardo Pala (riccardo.pala@open.ac.uk)
 * TODO description
 *
 * @link https://developer.samsung.com/health/android/data/api-reference/com/samsung/android/sdk/healthdata/HealthConstants.Exercise.html
 */
public class SHAdapter implements FHIRAdapter {

  /**
   * TODO description
   */
  public static SHAdapter create() {
    return new SHAdapter();
  }

  @Override
  public void transform(File dataset, File output) {
    transform(dataset, output, false);
  }

  @Override
  public void transform(File dataset, File output, boolean normalize) {
    try (
      InputStream datasetInputStream = new FileInputStream(dataset)
    ) {
      // Parse JSON dataset
      JSONTokener tokenizer = new JSONTokener(datasetInputStream);
      JSONObject json = new JSONObject(tokenizer);

      // Collect all entries
      Collection<Bundle.Entry> entries = new LinkedList<>();
      siftData(json, entries);

      // Build FHIR bundle
      Visitable bundle = Bundle.builder()
        .entry(entries)
        .type(BundleType.TRANSACTION)
        .build();

      // Save FHIR bundle in a JSON file
      String tempFilename = generateUniqueFilename("output", "tmp.fhir.json");
      File tempOutputFile = new File(TMP_DIR, tempFilename);
      save(bundle, tempOutputFile);

      // Normalize FHIR bundles, if requested
      if (normalize) {
        FHIRNormalizer.normalize(tempOutputFile, output);
        ResourceUtils.clean(tempOutputFile);
      } else {
        Files.move(tempOutputFile.toPath(), output.toPath());
      }

    } catch (FileNotFoundException e) {
      // TODO Message
      e.printStackTrace();

    } catch (IOException e) {
      // TODO Message
      e.printStackTrace();
    }
  }

  //--------------------------------------------------------------------------//
  // Class definition
  //--------------------------------------------------------------------------//

  private static final File TMP_DIR = FileUtils.getTempDirectory();

  /**
   * TODO description
   */
  protected SHAdapter() {}

  /**
   * @todo description
   */
  private static boolean hasComponents(JSONObject dataElement) {
    String obsType = dataElement.getString("type_id");
    return obsType.equals("weight")
      || obsType.equals("bloodGlucose")
      || obsType.equals("bloodPressure")
      || obsType.equals("oxygenSaturation");
  }

  /**
   * TODO description
   */
  private static void siftData(
    JSONObject json,
    Collection<Bundle.Entry> entries
  ) {
    Bundle.Entry patientEntry = collectPatient(entries, json);
    JSONArray data = json.getJSONArray("data");
    for (int i = 0; i < data.length(); ++i) {
      JSONObject element = data.getJSONObject(i);

      if (element.has("VCALENDAR")) {
        collectCalendarEvent(entries, element, patientEntry);
      } else {
        String obsType = element.getString("type_id");
        if (obsType.equals("sleep")) {
          collectSleepObservations(entries, element, patientEntry, data);
        } else {
          collectObservations(entries, element, patientEntry);
        }
      }
    }
  }

  /**
   * TODO description
   */
  private static Bundle.Entry collectPatient(
    Collection<Bundle.Entry> entries,
    JSONObject json
  ) {
    Bundle.Entry patientEntry = buildPatient(json);
    addIgnoreNull(entries, patientEntry);
    return patientEntry;
  }

  /**
   * @todo description
   */
  private static void collectObservations(
    Collection<Bundle.Entry> entries,
    JSONObject dataElement,
    Bundle.Entry patientEntry
  ) {
    String obsType = dataElement.getString("type_id");

    // NOTE sleep collected in another way
    if ( obsType.equals("sleep")
      || obsType.equals("sleepStage")
    ) return;

    Bundle.Entry mainObservation;
    Quantity value = SHBuilder.getMainValue(dataElement);
    Collection<Observation.Component> components = hasComponents(dataElement)
      ? collectMainComponents(dataElement)
      : new LinkedList<>();
    mainObservation = buildMainObservation(dataElement, components, value, patientEntry);
    entries.add(mainObservation);

    collectCalorie(entries, dataElement, mainObservation, patientEntry);
    collectCount(entries, dataElement, mainObservation, patientEntry);
    collectDistance(entries, dataElement, mainObservation, patientEntry);
    collectSpeed(entries, dataElement, mainObservation, patientEntry);

    collectHeartRateInstantaneous(entries, dataElement, mainObservation, patientEntry);
    collectHeartRateMin(entries, dataElement, mainObservation, patientEntry);
    collectHeartRateMax(entries, dataElement, mainObservation, patientEntry);
    collectHeartBeatCount(entries, dataElement, mainObservation, patientEntry);

    collectCadence(entries, dataElement, mainObservation, patientEntry);
//    collectCount(entries, dataElement, mainObservation, patientEntry);     // NOTE already collected above
//    collectDistance(entries, dataElement, mainObservation, patientEntry);  // NOTE already collected above
//    collectHeartRate(entries, dataElement, mainObservation, patientEntry); // NOTE already collected above
    collectPower(entries, dataElement, mainObservation, patientEntry);
//    collectSpeed(entries, dataElement, mainObservation, patientEntry);     // NOTE already collected above
    collectAltitude(entries, dataElement, mainObservation, patientEntry);
    collectDuration(entries, dataElement, mainObservation, patientEntry);
    collectHeartRate(entries, dataElement, mainObservation, patientEntry);
    collectRpm(entries, dataElement, mainObservation, patientEntry);
    collectVo2Max(entries, dataElement, mainObservation, patientEntry);
    collectLocation(entries, dataElement, mainObservation, patientEntry);
  }

  private static void collectCalendarEvent(
    Collection<Bundle.Entry> entries,
    JSONObject dataElement,
    Bundle.Entry patientEntry
  ) {
    JSONArray vCalendars = dataElement.getJSONArray("VCALENDAR");
    List<String> eventTypes = new LinkedList<>();
    eventTypes.add("VEVENT");
    eventTypes.add("VTODO");
    eventTypes.add("VJOURNAL");
    eventTypes.add("VFREEBUSY");
    eventTypes.add("VTIMEZONE");
//      eventTypes.add("VIANACOMP");
//      eventTypes.add("VXCOMP");
    for (int i = 0; i < vCalendars.length(); i++) {
      JSONObject vCalendar = vCalendars.getJSONObject(i);
      String parentId = UUID.randomUUID().toString();
      Resource parentAppointment = Appointment.builder()
              .appointmentType(createAppointmentType("iCalendar"))
              .status(getAppointmentStatus("noshow"))
              .id(parentId)
              .participant(createAppointmentParticipant(patientEntry.getResource().getId(), "Patient"))
              .identifier(
                      Identifier.builder()
                      .system(Uri.builder().value("https://opensource.samsung.com/projects/helifit/identifier").build())
                      .value(parentId).build()
              )
              .build();
      Bundle.Entry parentEntry = buildEntry(parentAppointment, buildFullUrl(BASE_URL + "/appointment/" + parentAppointment.getId()).getValue());
      entries.add(parentEntry);
      for (int k = 0; k < eventTypes.size(); k++) {
        if (!vCalendar.has(eventTypes.get(k))) continue;
        JSONArray events = vCalendar.getJSONArray(eventTypes.get(k));
        for (int j = 0; j < events.length(); j++) {
          JSONObject vEvent = events.getJSONObject(j);
          String eventType = eventTypes.get(k);
          Resource resource = buildAppointmentResource(vEvent, eventType, patientEntry, vCalendar, parentAppointment);
          String uuid = resource.getId();
          Bundle.Entry entry = buildEntry(resource,buildFullUrl(BASE_URL + "/appointment/" + uuid).getValue());
          entries.add(entry);
        }
      }
    }
  }

  /**
   * @todo descriptioncollectCalendarEvent
   */
  private static void collectSleepObservations(
    Collection<Bundle.Entry> entries,
    JSONObject dataElement,
    Bundle.Entry patientEntry,
    JSONArray bundle
  ) {
    Quantity value = FHIRBaseBuilder.buildQuantity(
      Decimal.of(
        SHBuilder.getValue(dataElement, "sleep_hours")
      ),
      "hour",
      UNITSOFM_SYSTEM,
      "h"
    );
    Collection<Observation.Component> stages = collectSleepStages(bundle, dataElement);
    Bundle.Entry mainObservation = buildMainObservation(dataElement, stages, value, patientEntry);
    entries.add(mainObservation);
  }

  private static Collection<Observation.Component> collectSleepStages(JSONArray bundle, JSONObject dataElement) {
    Collection<Observation.Component> components = new LinkedList<>();
    String parentId = dataElement.getString("data_uuid");
    for (int i = 0; i < bundle.length(); ++i) {
      JSONObject stageElement = bundle.getJSONObject(i);
      String typeId = stageElement.getString("type_id");
      if (typeId.equals("sleepStage")) {
        String currentParentId = stageElement.getString("parent_data_uuid");
        if (currentParentId.equals(parentId)) {
          String    code = SHBuilder.getValue(stageElement, "stage");
          String display = SHBuilder.getValue(stageElement, "stage_type");
          String  startTime = SHBuilder.getValue(stageElement, "start_time");
          String    endTime = SHBuilder.getValue(stageElement, "end_time");
          String zoneOffset = SHBuilder.getValue(stageElement, "time_offset");

          Observation.Component stage = buildObservationComponent(
            buildCodeableConcept(buildCoding(
              LOCAL_SYSTEM,
              code,
              display.toLowerCase()
            )),
            buildPeriod(startTime, endTime, zoneOffset)
          );
          components.add(stage);
        }
      }
    }
    return components;
  }

  private static Collection<Observation.Component> collectMainComponents(JSONObject dataElement) {
    JSONObject elementValues = dataElement.getJSONArray("values").getJSONObject(0);
    Collection<Observation.Component> components = new LinkedList<>();
    collectWeightComponents(components, elementValues);
    collectBloodPressureComponents(components, elementValues);
    collectBloodGlucoseComponents(components, elementValues);
    collectOxygenSaturationComponents(components, elementValues);
    return components;
  }

  private static void collectWeightComponents(Collection<Observation.Component> components, JSONObject elementValues) {
    //
    // weight // @note We moved this to the main observation
//    String weight = JSONObjectUtils.getElementValue(elementValues, "weight");
//    if (!StringUtils.isBlank(weight)) {
//      Observation.Component height = buildObservationComponent(
//        buildCodeableConcept(buildCoding(
//          LOINC_SYSTEM,
//          "29463-7",
//          "Body weight"
//        )),
//        FHIRBaseBuilder.buildQuantity(
//          Decimal.of(weight),
//          "kilogram",
//          UNITSOFM_SYSTEM,
//          "Kg"
//        )
//      );
//      components.add(height);
//    }
    //
    // body_fat
    String bodyFatValue = JSONObjectUtils.getElementValue(elementValues, "body_fat");
    if (!StringUtils.isBlank(bodyFatValue)) {
      Observation.Component bodyFat = buildObservationComponent(
        buildCodeableConcept(buildCoding(
          LOINC_SYSTEM,
          "41982-0",
          "Body Fat"
        )),
        FHIRBaseBuilder.buildQuantity(
          Decimal.of(bodyFatValue),
          "percent",
          UNITSOFM_SYSTEM,
          "%"
        )
      );
      components.add(bodyFat);
    }
    //
    // body_fat_mass
    String bodyFatMassValue = JSONObjectUtils.getElementValue(elementValues, "body_fat_mass");
    if (!StringUtils.isBlank(bodyFatMassValue)) {
      Observation.Component bodyFatMass = buildObservationComponent(
        buildCodeableConcept(buildCoding(
                LOINC_SYSTEM,
          "73708-0",
          "Body fat mass"
        )),
        FHIRBaseBuilder.buildQuantity(
          Decimal.of(bodyFatMassValue),
          "kilogram",
          UNITSOFM_SYSTEM,
          "Kg"
        )
      );
      components.add(bodyFatMass);
    }
    //
    // muscle_mass
    String muscleMassValue = JSONObjectUtils.getElementValue(elementValues, "muscle_mass");
    if (!StringUtils.isBlank(muscleMassValue)) {
      Observation.Component muscleMass = buildObservationComponent(
        buildCodeableConcept(buildCoding(
                LOINC_SYSTEM,
          "73964-9",
          "Muscle mass"
        )),
        FHIRBaseBuilder.buildQuantity(
          Decimal.of(muscleMassValue),
          "percent",
          UNITSOFM_SYSTEM,
          "%"
        )
      );
      components.add(muscleMass);
    }
    //
    // skeletal_muscle
    String skeletalMuscleValue = JSONObjectUtils.getElementValue(elementValues, "skeletal_muscle");
    if (!StringUtils.isBlank(skeletalMuscleValue)) {
      Observation.Component skeletalMuscle = buildObservationComponent(
        buildCodeableConcept(buildCoding(
          LOCAL_SYSTEM,
          "skeletal_muscle",
          "Skeletal muscle"
        )),
        FHIRBaseBuilder.buildQuantity(
          Decimal.of(skeletalMuscleValue),
          "percent",
          UNITSOFM_SYSTEM,
          "%"
        )
      );
      components.add(skeletalMuscle);
    }
    //
    // skeletal_muscle_mass
    String skeletalMuscleMassValue = JSONObjectUtils.getElementValue(elementValues, "skeletal_muscle_mass");
    if (!StringUtils.isBlank(skeletalMuscleMassValue)) {
      Observation.Component skeletalMuscleMass = buildObservationComponent(
        buildCodeableConcept(buildCoding(
          LOCAL_SYSTEM,
          "skeletal_muscle_mass",
          "Skeletal muscle mass"
        )),
        FHIRBaseBuilder.buildQuantity(
          Decimal.of(skeletalMuscleMassValue),
          "percent",
          UNITSOFM_SYSTEM,
          "%"
        )
      );
      components.add(skeletalMuscleMass);
    }
    //
    // basal_metabolic_rate
    String basalMetabolicRateValue = JSONObjectUtils.getElementValue(elementValues, "basal_metabolic_rate");
    if (!StringUtils.isBlank(basalMetabolicRateValue)) {
      Observation.Component basalMetabolicRate = buildObservationComponent(
        buildCodeableConcept(buildCoding(
                LOINC_SYSTEM,
          "50042-1",
          "Basal metabolic rate"
        )),
        FHIRBaseBuilder.buildQuantity(
          Decimal.of(basalMetabolicRateValue),
          "kilocalorie per day",
          UNITSOFM_SYSTEM,
          "kcal/d"
        )
      );
      components.add(basalMetabolicRate);
    }
    //
    // fat_free
    String fatFreeValue = JSONObjectUtils.getElementValue(elementValues, "fat_free");
    if (!StringUtils.isBlank(fatFreeValue)) {
      Observation.Component fatFree = buildObservationComponent(
        buildCodeableConcept(buildCoding(
          LOCAL_SYSTEM,
          "fat_free",
          "Fat free"
        )),
        FHIRBaseBuilder.buildQuantity(
          Decimal.of(fatFreeValue),
          "percent",
          UNITSOFM_SYSTEM,
          "%"
        )
      );
      components.add(fatFree);
    }
    //
    // fat_free_mass
    String fatFreeMassValue = JSONObjectUtils.getElementValue(elementValues, "fat_free_mass");
    if (!StringUtils.isBlank(fatFreeMassValue)) {
      Observation.Component fatFreeMass = buildObservationComponent(
        buildCodeableConcept(buildCoding(
          LOCAL_SYSTEM,
          "fat_free_mass",
          "Fat free mass"
        )),
        FHIRBaseBuilder.buildQuantity(
          Decimal.of(fatFreeMassValue),
          "kilogram",
          UNITSOFM_SYSTEM,
          "Kg"
        )
      );
      components.add(fatFreeMass);
    }
    //
    // total_body_water
    String totalBodyWaterValue = JSONObjectUtils.getElementValue(elementValues, "total_body_water");
    if (!StringUtils.isBlank(totalBodyWaterValue)) {
      Observation.Component totalBodyWater = buildObservationComponent(
        buildCodeableConcept(buildCoding(
                LOINC_SYSTEM,
          "73706-4",
          "Total body water"
        )),
        FHIRBaseBuilder.buildQuantity(
          Decimal.of(totalBodyWaterValue),
          "liter",
          UNITSOFM_SYSTEM,
          "l"
        )
      );
      components.add(totalBodyWater);
    }
  }

  private static void collectBloodPressureComponents(Collection<Observation.Component> components, JSONObject elementValues) {
    //
    // systolic
    String systolicValue = JSONObjectUtils.getElementValue(elementValues, "systolic");
    if (!StringUtils.isBlank(systolicValue)) {
      Observation.Component systolic = buildObservationComponent(
        buildCodeableConcept(buildCoding(
          LOINC_SYSTEM,
          "8480-6",
          "Systolic blood pressure"
        )),
        FHIRBaseBuilder.buildQuantity(
          Decimal.of(systolicValue),
          "millimeter of mercury",
          LOINC_SYSTEM,
          "mmHg"
        )
      );
      components.add(systolic);
    }
    //
    // diastolic
    String diastolicValue = JSONObjectUtils.getElementValue(elementValues, "diastolic");
    if (!StringUtils.isBlank(diastolicValue)) {
      Observation.Component diastolic = buildObservationComponent(
        buildCodeableConcept(buildCoding(
          LOINC_SYSTEM,
          "8462-4",
          "Diastolic blood pressure"
        )),
        FHIRBaseBuilder.buildQuantity(
          Decimal.of(diastolicValue),
          "millimeter of mercury",
          LOINC_SYSTEM,
          "mmHg"
        )
      );
      components.add(diastolic);
    }
    //
    // mean
    String meanValue = JSONObjectUtils.getElementValue(elementValues, "mean");
    if (!StringUtils.isBlank(meanValue)) {
      Observation.Component mean = buildObservationComponent(
        buildCodeableConcept(buildCoding(
          LOCAL_SYSTEM,
          "mean",
          "Mean"
        )),
        FHIRBaseBuilder.buildQuantity(
          Decimal.of(meanValue),
          "millimeter of mercury",
          LOINC_SYSTEM,
          "mmHg"
        )
      );
      components.add(mean);
    }
    //
    // pulse
    String pulseValue = JSONObjectUtils.getElementValue(elementValues, "pulse");
    if (!StringUtils.isBlank(pulseValue)) {
      Observation.Component pulse = buildObservationComponent(
        buildCodeableConcept(buildCoding(
          LOCAL_SYSTEM,
          "pulse",
          "Pulse"
        )),
        FHIRBaseBuilder.buildQuantity(
          Decimal.of(pulseValue),
          "{beats}/min",
          LOINC_SYSTEM,
          "beats/min"
        )
      );
      components.add(pulse);
    }
  }

  private static void collectBloodGlucoseComponents(Collection<Observation.Component> components, JSONObject elementValues) {
    //
    // glucose
    String glucoseValue = JSONObjectUtils.getElementValue(elementValues, "glucose");
    if (!StringUtils.isBlank(glucoseValue)) {
      Observation.Component glucose = buildObservationComponent(
        buildCodeableConcept(buildCoding(
          LOCAL_SYSTEM,
          "glucose",
          "Glucose"
        )),
        FHIRBaseBuilder.buildQuantity(
          Decimal.of(glucoseValue),
          "millimoles per liter",
          UNITSOFM_SYSTEM,
          "mmol/L"
        )
      );
      components.add(glucose);
    }
    //
    // sample_source_type
    String sampleSourceTypeValue = JSONObjectUtils.getElementValue(elementValues, "sample_source_type");
    if (!StringUtils.isBlank(sampleSourceTypeValue)) {
      Observation.Component sampleSourceType = Observation.Component.builder()
      .code(
        buildCodeableConcept(buildCoding(
          LOCAL_SYSTEM,
          "sample_source_type",
          "Sample source type"
        ))
      )
      .value(sampleSourceTypeValue)
      .build();
      components.add(sampleSourceType);
    }
    //
    // measurement_type
    String measurementTypeValue = JSONObjectUtils.getElementValue(elementValues, "measurement_type");
    if (!StringUtils.isBlank(measurementTypeValue)) {
      Observation.Component measurementType = Observation.Component.builder()
        .code(
          buildCodeableConcept(buildCoding(
            LOCAL_SYSTEM,
            "measurement_type",
            "Measurement type"
          ))
        )
        .value(measurementTypeValue)
        .build();
      components.add(measurementType);
    }
    //
    // meal_type
    String mealTypeValue = JSONObjectUtils.getElementValue(elementValues, "meal_type");
    if (!StringUtils.isBlank(mealTypeValue)) {
      Observation.Component mealType = Observation.Component.builder()
        .code(
          buildCodeableConcept(buildCoding(
            LOCAL_SYSTEM,
            "meal_type",
            "Meal type"
          ))
        )
        .value(mealTypeValue)
        .build();
      components.add(mealType);
    }
    //
    // meal_time
    String mealTimeValue = JSONObjectUtils.getElementValue(elementValues, "meal_time");
    String timeOffsetValue = JSONObjectUtils.getElementValue(elementValues, "time_offset");
    if (!StringUtils.isBlank(mealTimeValue) && !StringUtils.isBlank(timeOffsetValue)) {
      Observation.Component mealTime = buildObservationComponent(
        buildCodeableConcept(buildCoding(
          LOCAL_SYSTEM,
          "meal_time",
          "Meal time"
        )),
        buildDateTime(mealTimeValue, timeOffsetValue)
//        buildDateTime(
//          new Timestamp(Long.parseLong(value)),
//          zoneOffset
//        )
      );
      components.add(mealTime);
    }
  }

  private static void collectOxygenSaturationComponents(Collection<Observation.Component> components, JSONObject elementValues) {
    //
    // spo2
    String spo2Value = JSONObjectUtils.getElementValue(elementValues, "spo2");
    if (!StringUtils.isBlank(spo2Value)) {
      Observation.Component spo2 = buildObservationComponent(
        buildCodeableConcept(buildCoding(
          LOCAL_SYSTEM,
          "spo2",
          "Pulse Oximetry"
        )),
        FHIRBaseBuilder.buildQuantity(
          Decimal.of(spo2Value),
          "percent",
          UNITSOFM_SYSTEM,
          "%"
        )
      );
      components.add(spo2);
    }
    //
    // heart_rate
    String heartRateValue = JSONObjectUtils.getElementValue(elementValues, "heart_rate");
    if (!StringUtils.isBlank(heartRateValue)) {
      Observation.Component heartRate = buildObservationComponent(
        buildCodeableConcept(buildCoding(
          LOCAL_SYSTEM,
          "heart_rate",
          "Heart rate"
        )),
        FHIRBaseBuilder.buildQuantity(
          Decimal.of(heartRateValue),
          "{beats}/min",
          LOINC_SYSTEM,
          "beats/min"
        )
      );
      components.add(heartRate);
    }
  }

  private static Bundle.Entry collectCalorie(
    Collection<Bundle.Entry> entries,
    JSONObject dataElement,
    Bundle.Entry parentEntry,
    Bundle.Entry patientEntry
  ) {
    String calorie = getValue(dataElement, "calorie");
    String mean = getValue(dataElement, "mean_caloricburn_rate");
    String max = getValue(dataElement, "max_caloricburn_rate");
    if ( !StringUtils.isBlank(calorie)
      || !StringUtils.isBlank(mean)
      || !StringUtils.isBlank(max)
    ) {
      String uuid = dataElement.getString("data_uuid");

      Collection<Observation.Component> components = new LinkedList<>();
      //
      // Calorie
      if (!StringUtils.isBlank(calorie) ) {
        Observation.Component calorieComponent = buildObservationComponent(
          buildCodeableConcept(buildCoding(
            LOCAL_SYSTEM,
            "calorie",
            "Calorie"
          )),
          FHIRBaseBuilder.buildQuantity(
            Decimal.of(calorie),
            "kcal/s",
            UNITSOFM_SYSTEM,
            "kcal/s"
          )
        );
        components.add(calorieComponent);
      }
      //
      // mean_caloricburn_rate
      if (!StringUtils.isBlank(mean)) {
        Observation.Component meanComponent = buildObservationComponent(
          buildCodeableConcept(buildCoding(
            LOCAL_SYSTEM,
            "mean_caloricburn_rate",
            "Mean caloric burn rate"
          )),
          FHIRBaseBuilder.buildQuantity(
            Decimal.of(mean),
            "kcal/s",
            UNITSOFM_SYSTEM,
            "kcal/s"
          )
        );
        components.add(meanComponent);
      }
      //
      // max_caloricburn_rate
      if (!StringUtils.isBlank(max)) {
        Observation.Component maxComponent = buildObservationComponent(
          buildCodeableConcept(buildCoding(
            LOCAL_SYSTEM,
            "max_caloricburn_rate",
            "Max caloric burn rate"
          )),
          FHIRBaseBuilder.buildQuantity(
            Decimal.of(max),
            "kcal/s",
            UNITSOFM_SYSTEM,
            "kcal/s"
          )
        );
        components.add(maxComponent);
      }

      //
      // aggregated observation
      String aggregatedId = String.format("%s-calorie", uuid);
      Bundle.Entry aggregatedObservation = buildAggregatedObservation(
        aggregatedId,
        dataElement,
        buildCodeableConcept(buildCoding(
          LOCAL_SYSTEM,
          "calorie",
          "Calorie"
        )),
        components,
        null,
        parentEntry,
        patientEntry
      );
      entries.add(aggregatedObservation);

      //
      // collect live data
      JSONArray liveData = getLiveData(dataElement);
      for (int i = 0; i < liveData.length(); ++i) {
        JSONObject liveElement = liveData.getJSONObject(i);
        String value = JSONObjectUtils.getElementValue(liveElement, "calorie");
        if (!StringUtils.isBlank(value)) {
          // Collect live_data values
          String liveId = String.format("%s-calorie-%d", uuid, i);
          Bundle.Entry liveObs = buildLiveObservation(
            liveId,
            liveElement,
            dataElement,
            buildCodeableConcept(buildCoding(
              SAMSUNG_LIVE_SYSTEM,
              "live_data_calorie",
              "Live data calorie"
            )),
            FHIRBaseBuilder.buildQuantity(
              Decimal.of(value),
              "kcal/s",
              UNITSOFM_SYSTEM,
              "kcal/s"
            ),
            aggregatedObservation,
            patientEntry
          );
          entries.add(liveObs);
        }
      }
      return aggregatedObservation;
    }
    return null;
  }

  private static Bundle.Entry collectCount(
    Collection<Bundle.Entry> entries,
    JSONObject dataElement,
    Bundle.Entry parentEntry,
    Bundle.Entry patientEntry
  ) {
    JSONObject values = dataElement.getJSONArray("values").getJSONObject(0);
    String count = getValue(dataElement, "count");
    if (!StringUtils.isBlank(count)) {
      String uuid = dataElement.getString("data_uuid");
      String countType = "..."; // TODO change this to a proper unit
      if (values.has("count_type")) {
        countType = getCountType(values.getString("count_type"));
      } else if (dataElement.has("type_id")) {
        String typeId = dataElement.getString("type_id");
        if (typeId.equals("stepDailyTrend")) {
          countType = "steps";
        }
      }

      Collection<Observation.Component> components = new LinkedList<>();
      //
      // count
      Observation.Component countComponent = buildObservationComponent(
        buildCodeableConcept(buildCoding(
          LOCAL_SYSTEM,
          "count",
          "Count"
        )),
        FHIRBaseBuilder.buildQuantity(
          Decimal.of(count),
          countType,
          LOCAL_SYSTEM,
          countType
        )
      );
      components.add(countComponent);

      //
      // aggregated observation
      String aggregatedId = String.format("%s-count", uuid);
      Bundle.Entry aggregatedObservation = buildAggregatedObservation(
        aggregatedId,
        dataElement,
        buildCodeableConcept(buildCoding(
          LOCAL_SYSTEM,
          "count",
          "Count"
        )),
        components,
        null,
        parentEntry,
        patientEntry
      );
      entries.add(aggregatedObservation);

      //
      // collect live data
      JSONArray liveData = getLiveData(dataElement);
      for (int i = 0; i < liveData.length(); ++i) {
        JSONObject liveElement = liveData.getJSONObject(i);
        String value = JSONObjectUtils.getElementValue(liveElement, "count");
        if (!StringUtils.isBlank(value)) {
          // Collect live_data values
          String liveId = String.format("%s-count-%d", uuid, i);
          Bundle.Entry liveObs = buildLiveObservation(
            liveId,
            liveElement,
            dataElement,
            buildCodeableConcept(buildCoding(
              SAMSUNG_LIVE_SYSTEM,
              "live_data_count",
              "Live data count"
            )),
            FHIRBaseBuilder.buildQuantity(
              Decimal.of(value),
              countType,
              LOCAL_SYSTEM,
              countType
            ),
            aggregatedObservation,
            patientEntry
          );
          entries.add(liveObs);
        }
      }

      return aggregatedObservation;
    }
    return null;
  }

  private static Bundle.Entry collectDistance(
    Collection<Bundle.Entry> entries,
    JSONObject dataElement,
    Bundle.Entry parentEntry,
    Bundle.Entry patientEntry
  ) {
    JSONObject values = dataElement.getJSONArray("values").getJSONObject(0);
    String distance = getValue(dataElement, "distance");
    String inclineDistance = getValue(dataElement, "incline_distance");
    String declineDistance = getValue(dataElement, "decline_distance");
    if ( !StringUtils.isBlank(distance)
            || !StringUtils.isBlank(inclineDistance)
            || !StringUtils.isBlank(declineDistance)
    ) {
      String uuid = dataElement.getString("data_uuid");

      Collection<Observation.Component> components = new LinkedList<>();
      //
      // Distance
      if (!StringUtils.isBlank(distance)) {
        Observation.Component distanceComponent = buildObservationComponent(
          buildCodeableConcept(buildCoding(
            LOCAL_SYSTEM,
            "distance",
            "Distance"
          )),
          FHIRBaseBuilder.buildQuantity(
            Decimal.of(distance),
            "m",
            UNITSOFM_SYSTEM,
            "m"
          )
        );
        components.add(distanceComponent);
      }
      //
      // incline_distance
      if (!StringUtils.isBlank(inclineDistance)) {
        Observation.Component inclineComponent = buildObservationComponent(
          buildCodeableConcept(buildCoding(
            LOCAL_SYSTEM,
            "incline_distance",
            "Incline distance"
          )),
          FHIRBaseBuilder.buildQuantity(
            Decimal.of(inclineDistance),
            "m",
            UNITSOFM_SYSTEM,
            "m"
          )
        );
        components.add(inclineComponent);
      }
      //
      // decline_distance
      if (!StringUtils.isBlank(declineDistance)) {
        Observation.Component inclineComponent = buildObservationComponent(
          buildCodeableConcept(buildCoding(
            LOCAL_SYSTEM,
            "decline_distance",
            "Decline distance"
          )),
          FHIRBaseBuilder.buildQuantity(
            Decimal.of(declineDistance),
            "m",
            UNITSOFM_SYSTEM,
            "m"
          )
        );
        components.add(inclineComponent);
      }

      //
      // aggregated observation
      String aggregatedId = String.format("%s-distance", uuid);
      Bundle.Entry aggregatedObservation = buildAggregatedObservation(
        aggregatedId,
        dataElement,
        buildCodeableConcept(buildCoding(
          LOCAL_SYSTEM,
          "distance",
          "Distance"
        )),
        components,
        null,
        parentEntry,
        patientEntry
      );
      entries.add(aggregatedObservation);

      //
      // collect live data
      JSONArray liveData = getLiveData(dataElement);
      for (int i = 0; i < liveData.length(); ++i) {
        JSONObject liveElement = liveData.getJSONObject(i);
        String value = JSONObjectUtils.getElementValue(liveElement, "distance");
        if (!StringUtils.isBlank(value)) {
          // Collect live_data values
          String liveId = String.format("%s-distance-%d", uuid, i);
          Bundle.Entry liveObs = buildLiveObservation(
            liveId,
            liveElement,
            dataElement,
            buildCodeableConcept(buildCoding(
              SAMSUNG_LIVE_SYSTEM,
              "live_data_distance",
              "Live data distance"
            )),
            FHIRBaseBuilder.buildQuantity(
              Decimal.of(value),
              "m",
              UNITSOFM_SYSTEM,
              "m"
            ),
            aggregatedObservation,
            patientEntry
          );
          entries.add(liveObs);
        }
      }

      return aggregatedObservation;
    }
    return null;
  }

  private static Bundle.Entry collectSpeed(
    Collection<Bundle.Entry> entries,
    JSONObject dataElement,
    Bundle.Entry parentEntry,
    Bundle.Entry patientEntry
  ) {
    JSONObject values = dataElement.getJSONArray("values").getJSONObject(0);
    String speed = getValue(dataElement, "speed");
    String meanSpeed = getValue(dataElement, "mean_speed");
    String maxSpeed = getValue(dataElement, "max_speed");
    if ( !StringUtils.isBlank(speed)
      || !StringUtils.isBlank(meanSpeed)
      || !StringUtils.isBlank(maxSpeed)
    ) {
      String uuid = dataElement.getString("data_uuid");

      Collection<Observation.Component> components = new LinkedList<>();

      //
      // Speed
      if (!StringUtils.isBlank(speed)) {
        Observation.Component speedComponent = buildObservationComponent(
          buildCodeableConcept(buildCoding(
            LOCAL_SYSTEM,
            "speed",
            "Speed"
          )),
          FHIRBaseBuilder.buildQuantity(
            Decimal.of(speed),
            "m/s",
            UNITSOFM_SYSTEM,
            "m/s"
          )
        );
        components.add(speedComponent);
      }

      //
      // mean_speed
      if (!StringUtils.isBlank(meanSpeed)) {
        Observation.Component meanComponent = buildObservationComponent(
          buildCodeableConcept(buildCoding(
            LOCAL_SYSTEM,
            "mean_speed",
            "Mean speed"
          )),
          FHIRBaseBuilder.buildQuantity(
            Decimal.of(meanSpeed),
            "m/s",
            UNITSOFM_SYSTEM,
            "m/s"
          )
        );
        components.add(meanComponent);
      }

      //
      // max_speed
      if (!StringUtils.isBlank(maxSpeed)) {
        Observation.Component maxComponent = buildObservationComponent(
          buildCodeableConcept(buildCoding(
            LOCAL_SYSTEM,
            "max_speed",
            "Max speed"
          )),
          FHIRBaseBuilder.buildQuantity(
            Decimal.of(maxSpeed),
            "m/s",
            UNITSOFM_SYSTEM,
            "m/s"
          )
        );
        components.add(maxComponent);
      }

      //
      // aggregated observation
      String aggregatedId = String.format("%s-speed", uuid);
      Bundle.Entry aggregatedObservation = buildAggregatedObservation(
        aggregatedId,
        dataElement,
        buildCodeableConcept(buildCoding(
          LOCAL_SYSTEM,
          "speed",
          "Speed"
        )),
        components,
        null,
        parentEntry,
        patientEntry
      );
      entries.add(aggregatedObservation);

      //
      // collect live data
      JSONArray liveData = getLiveData(dataElement);
      for (int i = 0; i < liveData.length(); ++i) {
        JSONObject liveElement = liveData.getJSONObject(i);
        String value = JSONObjectUtils.getElementValue(liveElement, "speed\")");
        if (!StringUtils.isBlank(value)) {
          // Collect live_data values
          String liveId = String.format("%s-speed-%d", uuid, i);
          Bundle.Entry liveObs = buildLiveObservation(
            liveId,
            liveElement,
            dataElement,
            buildCodeableConcept(buildCoding(
              SAMSUNG_LIVE_SYSTEM,
              "live_data_speed",
              "Live data speed"
            )),
            FHIRBaseBuilder.buildQuantity(
              Decimal.of(value),
              "m/s",
              UNITSOFM_SYSTEM,
              "m/s"
            ),
            aggregatedObservation,
            patientEntry
          );
          entries.add(liveObs);
        }
      }

      return aggregatedObservation;
    }
    return null;
  }

  private static Bundle.Entry collectHeartRateInstantaneous(
    Collection<Bundle.Entry> entries,
    JSONObject dataElement,
    Bundle.Entry parentEntry,
    Bundle.Entry patientEntry
  ) {
    JSONObject values = dataElement.getJSONArray("values").getJSONObject(0);
    String heartRate = getValue(dataElement, "heart_rate");
    if (!StringUtils.isBlank(heartRate)) {
      String uuid = dataElement.getString("data_uuid");

      Collection<Observation.Component> components = new LinkedList<>();

      //
      // aggregated observation
      String aggregatedId = String.format("%s-heart-rate", uuid);
      Bundle.Entry aggregatedObservation = buildAggregatedObservation(
        aggregatedId,
        dataElement,
        buildCodeableConcept(buildCoding(
          LOCAL_SYSTEM,
          "heart_rate_instantaneous",
          "Heart Rate Instantaneous"
        )),
        components,
        FHIRBaseBuilder.buildQuantity(
          Decimal.of(heartRate),
          "beats/min",
          UNITSOFM_SYSTEM,
          "{Beats}/min"
        ),
        parentEntry,
        patientEntry
      );
      entries.add(aggregatedObservation);

      //
      // collect live data
      JSONArray liveData = getLiveData(dataElement);
      for (int i = 0; i < liveData.length(); ++i) {
        JSONObject liveElement = liveData.getJSONObject(i);
        String value = JSONObjectUtils.getElementValue(liveElement, "heart_rate");
        if (liveElement.has("heart_rate")) {
          // Collect live_data values
          String liveId = String.format("%s-heart-rate-%d", uuid, i);
          Bundle.Entry liveObs = buildLiveObservation(
            liveId,
            liveElement,
            dataElement,
            buildCodeableConcept(buildCoding(
              SAMSUNG_LIVE_SYSTEM,
              "live_data_heart_rate_instantaneous",
              "Live data heart rate instantaneous"
            )),
            FHIRBaseBuilder.buildQuantity(
              Decimal.of(value),
              "beats/min",
              UNITSOFM_SYSTEM,
              "{Beats}/min"
            ),
            aggregatedObservation,
            patientEntry
          );
          entries.add(liveObs);
        }
      }

      return aggregatedObservation;
    }
    return null;
  }

  private static Bundle.Entry collectHeartRateMin(
    Collection<Bundle.Entry> entries,
    JSONObject dataElement,
    Bundle.Entry parentEntry,
    Bundle.Entry patientEntry
  ) {
    JSONObject values = dataElement.getJSONArray("values").getJSONObject(0);
    String heartRateMin = getValue(dataElement, "heart_rate_min");
    if (!StringUtils.isBlank(heartRateMin)) {
      String uuid = dataElement.getString("data_uuid");

      Collection<Observation.Component> components = new LinkedList<>();

      //
      // aggregated observation
      String aggregatedId = String.format("%s-heart-rate-min", uuid);
      Bundle.Entry aggregatedObservation = buildAggregatedObservation(
        aggregatedId,
        dataElement,
        buildCodeableConcept(buildCoding(
          LOCAL_SYSTEM,
          "heart_rate_min",
          "Min Heart Rate"
        )),
        components,
        FHIRBaseBuilder.buildQuantity(
          Decimal.of(heartRateMin),
          "beats/min",
          UNITSOFM_SYSTEM,
          "{Beats}/min"
        ),
        parentEntry,
        patientEntry
      );
      entries.add(aggregatedObservation);

      //
      // collect live data
      JSONArray liveData = getLiveData(dataElement);
      for (int i = 0; i < liveData.length(); ++i) {
        JSONObject liveElement = liveData.getJSONObject(i);
        String value = JSONObjectUtils.getElementValue(liveElement, "heart_rate_min");
        if (!StringUtils.isBlank(value)) {
          // Collect live_data values
          String liveId = String.format("%s-heart-rate-min-%d", uuid, i);
          Bundle.Entry liveObs = buildLiveObservation(
            liveId,
            liveElement,
            dataElement,
            buildCodeableConcept(buildCoding(
              SAMSUNG_LIVE_SYSTEM,
              "live_data_heart_rate_min",
              "Live data min heart rate"
            )),
            FHIRBaseBuilder.buildQuantity(
              Decimal.of(value),
              "beats/min",
              UNITSOFM_SYSTEM,
              "{Beats}/min"
            ),
            aggregatedObservation,
            patientEntry
          );
          entries.add(liveObs);
        }
      }

      return aggregatedObservation;
    }
    return null;
  }

  private static Bundle.Entry collectHeartRateMax(
    Collection<Bundle.Entry> entries,
    JSONObject dataElement,
    Bundle.Entry parentEntry,
    Bundle.Entry patientEntry
  ) {
    JSONObject values = dataElement.getJSONArray("values").getJSONObject(0);
    String heartRateMax = getValue(dataElement, "heart_rate_max");
    if (!StringUtils.isBlank(heartRateMax)) {
      String uuid = dataElement.getString("data_uuid");

      Collection<Observation.Component> components = new LinkedList<>();

      //
      // aggregated observation
      String aggregatedId = String.format("%s-heart-rate-max", uuid);
      Bundle.Entry aggregatedObservation = buildAggregatedObservation(
        aggregatedId,
        dataElement,
        buildCodeableConcept(buildCoding(
          LOCAL_SYSTEM,
          "heart_rate_max",
          "Max Heart Rate"
        )),
        components,
        FHIRBaseBuilder.buildQuantity(
          Decimal.of(heartRateMax),
          "beats/min",
          UNITSOFM_SYSTEM,
          "{Beats}/min"
        ),
        parentEntry,
        patientEntry
      );
      entries.add(aggregatedObservation);

      //
      // collect live data
      JSONArray liveData = getLiveData(dataElement);
      for (int i = 0; i < liveData.length(); ++i) {
        JSONObject liveElement = liveData.getJSONObject(i);
        String value = JSONObjectUtils.getElementValue(liveElement, "heart_rate_max");
        if (!StringUtils.isBlank(value)) {
          // Collect live_data values
          String liveId = String.format("%s-heart-rate-max-%d", uuid, i);
          Bundle.Entry liveObs = buildLiveObservation(
            liveId,
            liveElement,
            dataElement,
            buildCodeableConcept(buildCoding(
              SAMSUNG_LIVE_SYSTEM,
              "live_data_heart_rate_max",
              "Live data max heart rate"
            )),
            FHIRBaseBuilder.buildQuantity(
              Decimal.of(value),
              "beats/min",
              UNITSOFM_SYSTEM,
              "{Beats}/min"
            ),
            aggregatedObservation,
            patientEntry
          );
          entries.add(liveObs);
        }
      }

      return aggregatedObservation;
    }
    return null;
  }

  private static Bundle.Entry collectHeartBeatCount(
    Collection<Bundle.Entry> entries,
    JSONObject dataElement,
    Bundle.Entry parentEntry,
    Bundle.Entry patientEntry
  ) {
    JSONObject values = dataElement.getJSONArray("values").getJSONObject(0);
    String heartBeatCount = getValue(dataElement, "heart_beat_count");
    if (!StringUtils.isBlank(heartBeatCount)) {
      String uuid = dataElement.getString("data_uuid");

      Collection<Observation.Component> components = new LinkedList<>();

      //
      // aggregated observation
      String aggregatedId = String.format("%s-heart-beat-count", uuid);
      Bundle.Entry aggregatedObservation = buildAggregatedObservation(
        aggregatedId,
        dataElement,
        buildCodeableConcept(buildCoding(
          LOCAL_SYSTEM,
          "heart_beat_count",
          "Heart Beat Count"
        )),
        components,
        FHIRBaseBuilder.buildQuantity(
          Decimal.of(heartBeatCount),
          "beats/min",
          UNITSOFM_SYSTEM,
          "beats/min"
        ),
        parentEntry,
        patientEntry
      );
      entries.add(aggregatedObservation);
      return aggregatedObservation;
    }
    return null;
  }

  private static Bundle.Entry collectCadence(
    Collection<Bundle.Entry> entries,
    JSONObject dataElement,
    Bundle.Entry parentEntry,
    Bundle.Entry patientEntry
  ) {
    JSONObject values = dataElement.getJSONArray("values").getJSONObject(0);
    String meanCadence = getValue(dataElement, "mean_cadence");
    String maxCadence = getValue(dataElement, "max_cadence");
    if ( !StringUtils.isBlank(meanCadence)
      && !StringUtils.isBlank(maxCadence)
    ) {
      String uuid = dataElement.getString("data_uuid");

      Collection<Observation.Component> components = new LinkedList<>();
      //
      // mean_cadence
      Observation.Component meanComponent = buildObservationComponent(
        buildCodeableConcept(buildCoding(
          LOCAL_SYSTEM,
          "mean_cadence",
          "Mean Cadence"
        )),
        FHIRBaseBuilder.buildQuantity(
          Decimal.of(meanCadence),
          "m/s",
          UNITSOFM_SYSTEM,
          "m/s"
        )
      );
      components.add(meanComponent);

      //
      // max_cadence
      Observation.Component maxComponent = buildObservationComponent(
        buildCodeableConcept(buildCoding(
          LOCAL_SYSTEM,
          "max_cadence",
          "Max Cadence"
        )),
        FHIRBaseBuilder.buildQuantity(
          Decimal.of(maxCadence),
          "m/s",
          UNITSOFM_SYSTEM,
          "m/s"
        )
      );
      components.add(maxComponent);

      //
      // aggregated observation
      String aggregatedId = String.format("%s-cadence", uuid);
      Bundle.Entry aggregatedObservation = buildAggregatedObservation(
        aggregatedId,
        dataElement,
        buildCodeableConcept(buildCoding(
          LOCAL_SYSTEM,
          "cadence",
          "Cadence"
        )),
        components,
        null,
        parentEntry,
        patientEntry
      );
      entries.add(aggregatedObservation);

      //
      // collect live data
      JSONArray liveData = getLiveData(dataElement);
      for (int i = 0; i < liveData.length(); ++i) {
        JSONObject liveElement = liveData.getJSONObject(i);
        String value = JSONObjectUtils.getElementValue(liveElement, "cadence");
        if (!StringUtils.isBlank(value)) {
          // Collect live_data values
          String liveId = String.format("%s-cadence-%d", uuid, i);
          Bundle.Entry liveObs = buildLiveObservation(
            liveId,
            liveElement,
            dataElement,
            buildCodeableConcept(buildCoding(
              SAMSUNG_LIVE_SYSTEM,
              "live_data_cadence",
              "Live data cadence"
            )),
            FHIRBaseBuilder.buildQuantity(
              Decimal.of(value),
              "m/s",
              UNITSOFM_SYSTEM,
              "m/s"
            ),
            aggregatedObservation,
            patientEntry
          );
          entries.add(liveObs);
        }
      }

      return aggregatedObservation;
    }
    return null;
  }

  private static Bundle.Entry collectPower(
    Collection<Bundle.Entry> entries,
    JSONObject dataElement,
    Bundle.Entry parentEntry,
    Bundle.Entry patientEntry
  ) {
    JSONObject values = dataElement.getJSONArray("values").getJSONObject(0);
    String meanPower = getValue(dataElement, "mean_power");
    String maxPower = getValue(dataElement, "max_power");
    if ( !StringUtils.isBlank(meanPower)
      && !StringUtils.isBlank(maxPower)
    ) {
      String uuid = dataElement.getString("data_uuid");

      Collection<Observation.Component> components = new LinkedList<>();
      //
      // mean_power
      Observation.Component meanComponent = buildObservationComponent(
        buildCodeableConcept(buildCoding(
          LOCAL_SYSTEM,
          "mean_power",
          "Mean Power"
        )),
        FHIRBaseBuilder.buildQuantity(
          Decimal.of(meanPower),
          "watt",
          UNITSOFM_SYSTEM,
          "W"
        )
      );
      components.add(meanComponent);

      //
      // max_power
      Observation.Component maxComponent = buildObservationComponent(
        buildCodeableConcept(buildCoding(
          LOCAL_SYSTEM,
          "max_power",
          "Max Power"
        )),
        FHIRBaseBuilder.buildQuantity(
          Decimal.of(maxPower),
          "watt",
          UNITSOFM_SYSTEM,
          "W"
        )
      );
      components.add(maxComponent);

      //
      // aggregated observation
      String aggregatedId = String.format("%s-power", uuid);
      Bundle.Entry aggregatedObservation = buildAggregatedObservation(
        aggregatedId,
        dataElement,
        buildCodeableConcept(buildCoding(
          LOCAL_SYSTEM,
          "power",
          "Power"
        )),
        components,
        null,
        parentEntry,
        patientEntry
      );
      entries.add(aggregatedObservation);

      //
      // collect live data
      JSONArray liveData = getLiveData(dataElement);
      for (int i = 0; i < liveData.length(); ++i) {
        JSONObject liveElement = liveData.getJSONObject(i);
        String value = JSONObjectUtils.getElementValue(liveElement, "power");
        if (!StringUtils.isBlank(value)) {
          // Collect live_data values
          String liveId = String.format("%s-power-%d", uuid, i);
          Bundle.Entry liveObs = buildLiveObservation(
            liveId,
            liveElement,
            dataElement,
            buildCodeableConcept(buildCoding(
              SAMSUNG_LIVE_SYSTEM,
              "live_data_power",
              "Live data power"
            )),
            FHIRBaseBuilder.buildQuantity(
              Decimal.of(value),
              "watt",
              UNITSOFM_SYSTEM,
              "W"
            ),
            aggregatedObservation,
            patientEntry
          );
          entries.add(liveObs);
        }
      }

      return aggregatedObservation;
    }
    return null;
  }

  private static Bundle.Entry collectAltitude(
    Collection<Bundle.Entry> entries,
    JSONObject dataElement,
    Bundle.Entry parentEntry,
    Bundle.Entry patientEntry
  ) {
    JSONObject values = dataElement.getJSONArray("values").getJSONObject(0);
    String altitudeGain = getValue(dataElement, "altitude_gain");
    String altitudeLoss = getValue(dataElement, "altitude_loss");
    String minAltitude = getValue(dataElement, "min_altitude");
    String maxAltitude = getValue(dataElement, "max_altitude");
    if ( !StringUtils.isBlank(altitudeGain)
      && !StringUtils.isBlank(altitudeLoss)
      && !StringUtils.isBlank(minAltitude)
      && !StringUtils.isBlank(maxAltitude)
    ) {
      String uuid = dataElement.getString("data_uuid");

      Collection<Observation.Component> components = new LinkedList<>();

      //
      // altitude_gain
      if (!StringUtils.isBlank(altitudeGain)) {
        Observation.Component gainComponent = buildObservationComponent(
          buildCodeableConcept(buildCoding(
            LOCAL_SYSTEM,
            "altitude_gain",
            "Altitude gain"
          )),
          FHIRBaseBuilder.buildQuantity(
            Decimal.of(altitudeGain),
            "m",
            UNITSOFM_SYSTEM,
            "m"
          )
        );
        components.add(gainComponent);
      }

      //
      // altitude_loss
      if (!StringUtils.isBlank(altitudeLoss)) {
        Observation.Component lossComponent = buildObservationComponent(
          buildCodeableConcept(buildCoding(
            LOCAL_SYSTEM,
            "altitude_loss",
            "Altitude loss"
          )),
          FHIRBaseBuilder.buildQuantity(
            Decimal.of(altitudeLoss),
            "m",
            UNITSOFM_SYSTEM,
            "m"
          )
        );
        components.add(lossComponent);
      }

      //
      // min_altitude
      if (!StringUtils.isBlank(minAltitude)) {
        Observation.Component minComponent = buildObservationComponent(
          buildCodeableConcept(buildCoding(
            LOCAL_SYSTEM,
            "min_altitude",
            "Min altitude"
          )),
          FHIRBaseBuilder.buildQuantity(
            Decimal.of(minAltitude),
            "m",
            UNITSOFM_SYSTEM,
            "m"
          )
        );
        components.add(minComponent);
      }

      //
      // max_altitude
      if (!StringUtils.isBlank(maxAltitude)) {
        Observation.Component maxComponent = buildObservationComponent(
          buildCodeableConcept(buildCoding(
            LOCAL_SYSTEM,
            "max_altitude",
            "Max Altitude"
          )),
          FHIRBaseBuilder.buildQuantity(
            Decimal.of(maxAltitude),
            "m",
            UNITSOFM_SYSTEM,
            "m"
          )
        );
        components.add(maxComponent);
      }

      //
      // aggregated observation
      String aggregatedId = String.format("%s-altitude", uuid);
      Bundle.Entry aggregatedObservation = buildAggregatedObservation(
        aggregatedId,
        dataElement,
        buildCodeableConcept(buildCoding(
          LOCAL_SYSTEM,
          "altitude",
          "Altitude"
        )),
        components,
        null,
        parentEntry,
        patientEntry
      );
      entries.add(aggregatedObservation);

      return aggregatedObservation;
    }
    return null;
  }

  private static Bundle.Entry collectDuration(
    Collection<Bundle.Entry> entries,
    JSONObject dataElement,
    Bundle.Entry parentEntry,
    Bundle.Entry patientEntry
  ) {
    JSONObject values = dataElement.getJSONArray("values").getJSONObject(0);
    String duration = getValue(dataElement, "duration");
    if ( !StringUtils.isBlank(duration)) {
      String uuid = dataElement.getString("data_uuid");
      double d = Float.parseFloat(duration);
      String dd = String.valueOf(d / 1000);
      Collection<Observation.Component> components = new LinkedList<>();

      //
      // aggregated observation
      String aggregatedId = String.format("%s-duration", uuid);
      Bundle.Entry aggregatedObservation = buildAggregatedObservation(
        aggregatedId,
        dataElement,
        buildCodeableConcept(buildCoding(
          LOCAL_SYSTEM,
          "duration",
          "Duration"
        )),
        components,
        FHIRBaseBuilder.buildQuantity(
          Decimal.of(dd),
          "ms",
          UNITSOFM_SYSTEM,
          ",s"
        ),
        parentEntry,
        patientEntry
      );
      entries.add(aggregatedObservation);

      return aggregatedObservation;
    }
    return null;
  }

  private static Bundle.Entry collectHeartRate(
    Collection<Bundle.Entry> entries,
    JSONObject dataElement,
    Bundle.Entry parentEntry,
    Bundle.Entry patientEntry
  ) {
    JSONObject values = dataElement.getJSONArray("values").getJSONObject(0);
    String minHeartRate = getValue(dataElement, "min_heart_rate");
    String meanHeartRate = getValue(dataElement, "mean_heart_rate");
    String maxHeartRate = getValue(dataElement, "max_heart_rate");
    if ( !StringUtils.isBlank(minHeartRate)
      || !StringUtils.isBlank(meanHeartRate)
      || !StringUtils.isBlank(maxHeartRate)
    ) {
      String uuid = dataElement.getString("data_uuid");

      Collection<Observation.Component> components = new LinkedList<>();

      //
      // Heart Rate Min
      if (!StringUtils.isBlank(minHeartRate)) {
        Observation.Component hrMinComponent = buildObservationComponent(
          buildCodeableConcept(buildCoding(
            LOCAL_SYSTEM,
            "min_heart_rate",
            "Heart Rate Min"
          )),
          FHIRBaseBuilder.buildQuantity(
            Decimal.of(minHeartRate),
            "beats/min",
            UNITSOFM_SYSTEM,
            "{Beats}/min"
          )
        );
        components.add(hrMinComponent);
      }

      //
      // Heart Rate Mean
      if (!StringUtils.isBlank(meanHeartRate)) {
        Observation.Component hrMeanComponent = buildObservationComponent(
          buildCodeableConcept(buildCoding(
            LOCAL_SYSTEM,
            "mean_heart_rate",
            "Heart Rate Mean"
          )),
          FHIRBaseBuilder.buildQuantity(
            Decimal.of(meanHeartRate),
            "beats/min",
            UNITSOFM_SYSTEM,
            "{Beats}/min"
          )
        );
        components.add(hrMeanComponent);
      }

      //
      // Heart Rate Max
      if (!StringUtils.isBlank(maxHeartRate)) {
        Observation.Component hrMaxComponent = buildObservationComponent(
          buildCodeableConcept(buildCoding(
            LOCAL_SYSTEM,
            "max_heart_rate",
            "Heart Rate Max"
          )),
          FHIRBaseBuilder.buildQuantity(
            Decimal.of(maxHeartRate),
            "beats/min",
            UNITSOFM_SYSTEM,
            "{Beats}/min"
          )
        );
        components.add(hrMaxComponent);
      }

      //
      // aggregated observation
      String aggregatedId = String.format("%s-heart-rate", uuid);
      Bundle.Entry aggregatedObservation = buildAggregatedObservation(
        aggregatedId,
        dataElement,
        buildCodeableConcept(buildCoding(
          LOCAL_SYSTEM,
          "heart_rate",
          "Heart Rate"
        )),
        components,
        null,
        parentEntry,
        patientEntry
      );
      entries.add(aggregatedObservation);

      //
      // collect live data
      JSONArray liveData = getLiveData(dataElement);
      for (int i = 0; i < liveData.length(); ++i) {
        JSONObject liveElement = liveData.getJSONObject(i);
        String value = JSONObjectUtils.getElementValue(liveElement, "heart_rate");
        if (!StringUtils.isBlank(value)) {
          // Collect live_data values
          String liveId = String.format("%s-heart-rate-%d", uuid, i);
          Bundle.Entry liveObs = buildLiveObservation(
            liveId,
            liveElement,
            dataElement,
            buildCodeableConcept(buildCoding(
              SAMSUNG_LIVE_SYSTEM,
              "live_data_heart_rate",
              "Live data heart rate"
            )),
            FHIRBaseBuilder.buildQuantity(
              Decimal.of(value),
              "beats/min",
              UNITSOFM_SYSTEM,
              "{Beats}/min"
            ),
            aggregatedObservation,
            patientEntry
          );
          entries.add(liveObs);
        }
      }

      return aggregatedObservation;
    }
    return null;
  }

  private static Bundle.Entry collectRpm(
    Collection<Bundle.Entry> entries,
    JSONObject dataElement,
    Bundle.Entry parentEntry,
    Bundle.Entry patientEntry
  ) {
    JSONObject values = dataElement.getJSONArray("values").getJSONObject(0);
    String meanRpm = getValue(dataElement, "mean_rpm");
    String maxRpm = getValue(dataElement, "max_rpm");
    if ( !StringUtils.isBlank(meanRpm)
      && !StringUtils.isBlank(maxRpm)
    ) {
      String uuid = dataElement.getString("data_uuid");

      Collection<Observation.Component> components = new LinkedList<>();

      //
      // mean_rpm
      if (!StringUtils.isBlank(meanRpm)) {
        Observation.Component meanComponent = buildObservationComponent(
          buildCodeableConcept(buildCoding(
            LOCAL_SYSTEM,
            "mean_rpm",
            "Mean rpm"
          )),
          FHIRBaseBuilder.buildQuantity(
            Decimal.of(meanRpm),
            "Hz",
            UNITSOFM_SYSTEM,
            "hZ"
          )
        );
        components.add(meanComponent);
      }

      //
      // max_rpm
      if (!StringUtils.isBlank(maxRpm)) {
        Observation.Component maxComponent = buildObservationComponent(
          buildCodeableConcept(buildCoding(
            LOCAL_SYSTEM,
            "max_rpm",
            "Max rpm"
          )),
          FHIRBaseBuilder.buildQuantity(
            Decimal.of(maxRpm),
            "Hz",
            UNITSOFM_SYSTEM,
            "hZ"
          )
        );
        components.add(maxComponent);
      }

      //
      // aggregated observation
      String aggregatedId = String.format("%s-rpm", uuid);
      Bundle.Entry aggregatedObservation = buildAggregatedObservation(
        aggregatedId,
        dataElement,
        buildCodeableConcept(buildCoding(
          LOCAL_SYSTEM,
          "rpm",
          "rpm"
        )),
        components,
        null,
        parentEntry,
        patientEntry
      );
      entries.add(aggregatedObservation);

      return aggregatedObservation;
    }
    return null;
  }

  private static Bundle.Entry collectVo2Max(
    Collection<Bundle.Entry> entries,
    JSONObject dataElement,
    Bundle.Entry parentEntry,
    Bundle.Entry patientEntry
  ) {
    JSONObject values = dataElement.getJSONArray("values").getJSONObject(0);
    String vo2Max = getValue(dataElement, "vo2_max");
    if (!StringUtils.isBlank(vo2Max)) {
      String uuid = dataElement.getString("data_uuid");

      Collection<Observation.Component> components = new LinkedList<>();

      //
      // aggregated observation
      String aggregatedId = String.format("%s-vo2-max", uuid);
      Bundle.Entry aggregatedObservation = buildAggregatedObservation(
        aggregatedId,
        dataElement,
        buildCodeableConcept(buildCoding(
          LOCAL_SYSTEM,
          "vo2_max",
          "Vo2_max"
        )),
        components,
        FHIRBaseBuilder.buildQuantity(
          Decimal.of(vo2Max),
          "mL/kg/min",
          UNITSOFM_SYSTEM,
          "mL/kg/min"
        ),
        parentEntry,
        patientEntry
      );
      entries.add(aggregatedObservation);

      return aggregatedObservation;
    }
    return null;
  }

  private static void collectLocation(
    Collection<Bundle.Entry> entries,
    JSONObject dataElement,
    Bundle.Entry parentEntry,
    Bundle.Entry patientEntry
  ) {
    if (dataElement.has("location_data")) {
      String uuid = dataElement.getString("data_uuid");

      JSONArray locationData = dataElement.getJSONArray("location_data");
      for (int i = 0; i < locationData.length(); ++i) {
        JSONObject locationElement = locationData.getJSONObject(i);
        String locationId = String.format("%s-location-%d", uuid, i);

        Collection<Observation.Component> components = new LinkedList<>();

        //
        // Latitude
        if (locationElement.has("latitude")) {
          Observation.Component latitudeComponent = buildObservationComponent(
            buildCodeableConcept(buildCoding(
              LOCAL_SYSTEM,
              "latitude",
              "Latitude"
            )),
            FHIRBaseBuilder.buildQuantity(
              // @link https://developer.samsung.com/health/android/data/api-reference/com/samsung/android/sdk/healthdata/HealthConstants.UvExposure.html#LATITUDE
              Decimal.of(locationElement.getString("latitude")),
              "degree",
              UNITSOFM_SYSTEM,
              "[pi].rad/360"
            )
          );
          components.add(latitudeComponent);
        }

        //
        // Longitude
        if (locationElement.has("longitude")) {
          Observation.Component longitudeComponent = buildObservationComponent(
            buildCodeableConcept(buildCoding(
              LOCAL_SYSTEM,
              "longitude",
              "Longitude"
            )),
            FHIRBaseBuilder.buildQuantity(
              // @link https://developer.samsung.com/health/android/data/api-reference/com/samsung/android/sdk/healthdata/HealthConstants.UvExposure.html#LONGITUDE
              Decimal.of(locationElement.getString("longitude")),
              "degree",
              UNITSOFM_SYSTEM,
              "[pi].rad/360"
            )
          );
          components.add(longitudeComponent);
        }

        //
        // Altitude
        if (locationElement.has("altitude")) {
          Observation.Component altitudeComponent = buildObservationComponent(
            buildCodeableConcept(buildCoding(
              LOCAL_SYSTEM,
              "altitude",
              "Altitude"
            )),
            FHIRBaseBuilder.buildQuantity(
              Decimal.of(locationElement.getString("altitude")),
              "m",
              UNITSOFM_SYSTEM,
              "meter"
            )
          );
          components.add(altitudeComponent);
        }

        //
        // Accuracy
        if (locationElement.has("accuracy")) {
          Observation.Component accuracyComponent = buildObservationComponent(
            buildCodeableConcept(buildCoding(
              LOCAL_SYSTEM,
              "accuracy",
              "Accuracy"
            )),
            FHIRBaseBuilder.buildQuantity(
              // @link https://developer.samsung.com/health/android/data/api-reference/com/samsung/android/sdk/healthdata/HealthConstants.UvExposure.html#ACCURACY
              Decimal.of(locationElement.getString("accuracy")),
              "percent",
              UNITSOFM_SYSTEM,
              "%"
            )
          );
          components.add(accuracyComponent);
        }

        Bundle.Entry location = buildLocation(
          locationId,
          locationElement,
          dataElement,
          components,
          parentEntry,
          patientEntry
        );
        entries.add(location);
      }
    }
  }



  private static JSONArray getLiveData(JSONObject dataElement) {
    if (dataElement.has("live_data")) {
      return dataElement.getJSONArray("live_data");
    }
    if (dataElement.has("binning_data")) {
      return dataElement.getJSONArray("binning_data");
    }
    return new JSONArray();
  }


  /**
   * TODO description
   */
  private static void save(Visitable bundle, File output) {
    try (
      Writer file = new FileWriter(output)
    ) {
      FHIRGenerator
        .generator(Format.JSON, true)
        .generate(bundle, file);

    } catch (IOException e) {
      // TODO Message
      e.printStackTrace();

    } catch (FHIRGeneratorException e) {
      // TODO Message
      e.printStackTrace();
    }
  }

}