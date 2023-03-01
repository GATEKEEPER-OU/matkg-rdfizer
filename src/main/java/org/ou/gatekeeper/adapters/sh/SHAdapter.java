package org.ou.gatekeeper.adapters.sh;

import com.ibm.fhir.model.resource.Appointment;
import com.ibm.fhir.model.resource.Bundle;
import com.ibm.fhir.model.resource.Observation;
import com.ibm.fhir.model.resource.Resource;
import com.ibm.fhir.model.type.*;
import com.ibm.fhir.model.type.Decimal;
import com.ibm.fhir.model.type.Quantity;
import org.apache.commons.lang.StringUtils;
import org.commons.JSONObjectUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.ou.gatekeeper.adapters.BaseAdapter;
import org.ou.gatekeeper.adapters.BaseBuilder;
import org.ou.gatekeeper.adapters.DataAdapter;

import java.lang.String;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import static org.apache.commons.collections.CollectionUtils.addIgnoreNull;
import static org.ou.gatekeeper.adapters.sh.SHBuilder.*;

/**
 * @author Riccardo Pala (riccardo.pala@open.ac.uk)
 * TODO description
 *
 * @link <a href="https://developer.samsung.com/health/android/data/api-reference/com/samsung/android/sdk/healthdata/HealthConstants.Exercise.html">...</a>
 */
public class SHAdapter extends BaseAdapter
                    implements DataAdapter {

  /**
   * TODO description
   */
  public static DataAdapter create() {
    return new SHAdapter();
  }

  //--------------------------------------------------------------------------//
  // Class definition
  //--------------------------------------------------------------------------//

  /**
   * TODO description
   */
  protected SHAdapter() {}

  /**
   * TODO description
   */
  @Override
  protected void siftData(
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
  private static Bundle.Entry collectPatient(
    Collection<Bundle.Entry> entries,
    JSONObject json
  ) {
    Bundle.Entry patientEntry = SHBuilder.buildPatient(json);
    addIgnoreNull(entries, patientEntry);
    return patientEntry;
  }

  /**
   * TODO description
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
    mainObservation = SHBuilder.buildMainObservation(dataElement, components, value, patientEntry);
    entries.add(mainObservation);

    if (obsType.equals("nutrition")) {
      collectNutritionItem(entries, dataElement, mainObservation, patientEntry, "total_fat", "total_fat", "grams", "gr");
      collectNutritionItem(entries, dataElement, mainObservation, patientEntry, "calorie", "calorie", "kilocalories", "kcal");
      collectNutritionItem(entries, dataElement, mainObservation, patientEntry, "sugar", "sugar", "grams", "gr");
      collectNutritionItem(entries, dataElement, mainObservation, patientEntry, "saturated_fat", "saturated_fat", "grams", "gr");
      collectNutritionItem(entries, dataElement, mainObservation, patientEntry, "polysaturated_fat", "polysaturated_fat", "grams", "gr");
      collectNutritionItem(entries, dataElement, mainObservation, patientEntry, "monosaturated_fat", "monosaturated_fat", "grams", "gr");
      collectNutritionItem(entries, dataElement, mainObservation, patientEntry, "carbohydrate", "carbohydrate", "grams", "gr");
      collectNutritionItem(entries, dataElement, mainObservation, patientEntry, "dietary_fiber", "dietary_fiber", "grams", "gr");
      collectNutritionItem(entries, dataElement, mainObservation, patientEntry, "protein", "protein", "grams", "gr");
      collectNutritionItem(entries, dataElement, mainObservation, patientEntry, "cholesterol", "cholesterol", "milligrams", "mg");
      collectNutritionItem(entries, dataElement, mainObservation, patientEntry, "sodium", "sodium", "milligrams", "mg");
      collectNutritionItem(entries, dataElement, mainObservation, patientEntry, "potassium", "potassium", "milligrams", "mg");
      collectNutritionItem(entries, dataElement, mainObservation, patientEntry, "vitamin_a", "vitamin_a", "%", "%");
      collectNutritionItem(entries, dataElement, mainObservation, patientEntry, "vitamin_c", "vitamin_c", "%", "%");
      collectNutritionItem(entries, dataElement, mainObservation, patientEntry, "calcium", "calcium", "%", "%");
      collectNutritionItem(entries, dataElement, mainObservation, patientEntry, "iron", "iron", "%", "%");
      collectNutritionItem(entries, dataElement, mainObservation, patientEntry, "trans_fat", "trans_fat", "grams", "gr");
    } else {
      collectCalorie(entries, dataElement, mainObservation, patientEntry);
    }
    collectCount(entries, dataElement, mainObservation, patientEntry);
    collectDistance(entries, dataElement, mainObservation, patientEntry);
    collectSpeed(entries, dataElement, mainObservation, patientEntry);

    collectHeartRateInstantaneous(entries, dataElement, mainObservation, patientEntry);
    collectHeartRateMin(entries, dataElement, mainObservation, patientEntry);
    collectHeartRateMax(entries, dataElement, mainObservation, patientEntry);
    collectHeartBeatCount(entries, dataElement, mainObservation, patientEntry);

    collectCadence(entries, dataElement, mainObservation, patientEntry);
    // Count     // NOTE collected above
    // Distance  // NOTE collected above
    // HeartRate // NOTE collected above
    collectPower(entries, dataElement, mainObservation, patientEntry);
    // Speed     // NOTE collected above
    collectAltitude(entries, dataElement, mainObservation, patientEntry);
    collectDuration(entries, dataElement, mainObservation, patientEntry);
    collectHeartRate(entries, dataElement, mainObservation, patientEntry);
    collectRpm(entries, dataElement, mainObservation, patientEntry);
    collectVo2Max(entries, dataElement, mainObservation, patientEntry);
    collectLocation(entries, dataElement, mainObservation, patientEntry);
  }

  private static void collectNutritionItem(
          Collection<Bundle.Entry> entries,
          JSONObject dataElement,
          Bundle.Entry parentEntry,
          Bundle.Entry patientEntry,
          String itemName,
          String itemCode,
          String itemMeasureUnit,
          String itemMeasureUnitCode
  ) {
    String entryValue = SHBuilder.getValue(dataElement, itemName);
    if (!StringUtils.isBlank(entryValue)) {
      String uuid = dataElement.getString("data_uuid");

      Collection<Observation.Component> components = new LinkedList<>();

      //
      // aggregated observation
      String aggregatedId = String.format("%s-" + itemName.replaceAll("_", "-"), uuid);
      Bundle.Entry aggregatedObservation = SHBuilder.buildAggregatedObservation(
              aggregatedId,
              dataElement,
              buildCodeableConcept(buildCoding(
                      LOCAL_SYSTEM,
                      itemCode,
                      itemName
              )),
              components,
              BaseBuilder.buildQuantity(
                      Decimal.of(entryValue),
                      itemMeasureUnit,
                      UNITSOFM_SYSTEM,
                      itemMeasureUnitCode
              ),
              parentEntry,
              patientEntry
      );
      entries.add(aggregatedObservation);
    }
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
<<<<<<< HEAD:src/main/java/org/ou/gatekeeper/fhir/adapters/sh/SHAdapter.java
   * @todo descriptioncollectCalendarEvent
=======
   * TODO description
>>>>>>> 87ed271a3b3588d5ffcea0e2f907d788749be91d:src/main/java/org/ou/gatekeeper/adapters/sh/SHAdapter.java
   */
  private static void collectSleepObservations(
    Collection<Bundle.Entry> entries,
    JSONObject dataElement,
    Bundle.Entry patientEntry,
    JSONArray bundle
  ) {
    Quantity value = BaseBuilder.buildQuantity(
      Decimal.of(
        SHBuilder.getValue(dataElement, "sleep_hours")
      ),
      "hour",
      UNITSOFM_SYSTEM,
      "h"
    );
    Collection<Observation.Component> stages = collectSleepStages(bundle, dataElement);
    Bundle.Entry mainObservation = SHBuilder.buildMainObservation(dataElement, stages, value, patientEntry);
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
          String       code = SHBuilder.getValue(stageElement, "stage");
          String    display = SHBuilder.getValue(stageElement, "stage_type");
          String  startTime = SHBuilder.getValue(stageElement, "start_time");
          String    endTime = SHBuilder.getValue(stageElement, "end_time");
          String zoneOffset = SHBuilder.getValue(stageElement, "time_offset");

          Observation.Component stage = SHBuilder.buildObservationComponent(
            buildCodeableConcept(buildCoding(
              LOCAL_SYSTEM,
              code,
              display.toLowerCase()
            )),
            SHBuilder.buildPeriod(startTime, endTime, zoneOffset)
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
    // weight // NOTE We moved this to the main observation

    //
    // body_fat
    String bodyFatValue = JSONObjectUtils.getElementValue(elementValues, "body_fat");
    if (!StringUtils.isBlank(bodyFatValue)) {
      Observation.Component bodyFat = SHBuilder.buildObservationComponent(
        buildCodeableConcept(buildCoding(
          LOINC_SYSTEM,
          "41982-0",
          "Body Fat"
        )),
        BaseBuilder.buildQuantity(
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
      Observation.Component bodyFatMass = SHBuilder.buildObservationComponent(
        buildCodeableConcept(buildCoding(
                LOINC_SYSTEM,
          "73708-0",
          "Body fat mass"
        )),
        BaseBuilder.buildQuantity(
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
      Observation.Component muscleMass = SHBuilder.buildObservationComponent(
        buildCodeableConcept(buildCoding(
                LOINC_SYSTEM,
          "73964-9",
          "Muscle mass"
        )),
        BaseBuilder.buildQuantity(
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
      Observation.Component skeletalMuscle = SHBuilder.buildObservationComponent(
        buildCodeableConcept(buildCoding(
          LOCAL_SYSTEM,
          "skeletal_muscle",
          "Skeletal muscle"
        )),
        BaseBuilder.buildQuantity(
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
      Observation.Component skeletalMuscleMass = SHBuilder.buildObservationComponent(
        buildCodeableConcept(buildCoding(
          LOCAL_SYSTEM,
          "skeletal_muscle_mass",
          "Skeletal muscle mass"
        )),
        BaseBuilder.buildQuantity(
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
      Observation.Component basalMetabolicRate = SHBuilder.buildObservationComponent(
        buildCodeableConcept(buildCoding(
                LOINC_SYSTEM,
          "50042-1",
          "Basal metabolic rate"
        )),
        BaseBuilder.buildQuantity(
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
      Observation.Component fatFree = SHBuilder.buildObservationComponent(
        buildCodeableConcept(buildCoding(
          LOCAL_SYSTEM,
          "fat_free",
          "Fat free"
        )),
        BaseBuilder.buildQuantity(
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
      Observation.Component fatFreeMass = SHBuilder.buildObservationComponent(
        buildCodeableConcept(buildCoding(
          LOCAL_SYSTEM,
          "fat_free_mass",
          "Fat free mass"
        )),
        BaseBuilder.buildQuantity(
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
      Observation.Component totalBodyWater = SHBuilder.buildObservationComponent(
        buildCodeableConcept(buildCoding(
                LOINC_SYSTEM,
          "73706-4",
          "Total body water"
        )),
        BaseBuilder.buildQuantity(
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
      Observation.Component systolic = SHBuilder.buildObservationComponent(
        buildCodeableConcept(buildCoding(
          LOINC_SYSTEM,
          "8480-6",
          "Systolic blood pressure"
        )),
        BaseBuilder.buildQuantity(
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
      Observation.Component diastolic = SHBuilder.buildObservationComponent(
        buildCodeableConcept(buildCoding(
          LOINC_SYSTEM,
          "8462-4",
          "Diastolic blood pressure"
        )),
        BaseBuilder.buildQuantity(
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
      Observation.Component mean = SHBuilder.buildObservationComponent(
        buildCodeableConcept(buildCoding(
          LOCAL_SYSTEM,
          "mean",
          "Mean"
        )),
        BaseBuilder.buildQuantity(
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
      Observation.Component pulse = SHBuilder.buildObservationComponent(
        buildCodeableConcept(buildCoding(
          LOCAL_SYSTEM,
          "pulse",
          "Pulse"
        )),
        BaseBuilder.buildQuantity(
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
      Observation.Component glucose = SHBuilder.buildObservationComponent(
        buildCodeableConcept(buildCoding(
          LOCAL_SYSTEM,
          "glucose",
          "Glucose"
        )),
        BaseBuilder.buildQuantity(
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
    if (!StringUtils.isBlank(mealTimeValue) && !mealTimeValue.equals("NaN") && !StringUtils.isBlank(timeOffsetValue)) {
      Observation.Component mealTime = SHBuilder.buildObservationComponent(
        buildCodeableConcept(buildCoding(
          LOCAL_SYSTEM,
          "meal_time",
          "Meal time"
        )),
        SHBuilder.buildDateTime(mealTimeValue, timeOffsetValue)
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
      Observation.Component spo2 = SHBuilder.buildObservationComponent(
        buildCodeableConcept(buildCoding(
          LOCAL_SYSTEM,
          "spo2",
          "Pulse Oximetry"
        )),
        BaseBuilder.buildQuantity(
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
      Observation.Component heartRate = SHBuilder.buildObservationComponent(
        buildCodeableConcept(buildCoding(
          LOCAL_SYSTEM,
          "heart_rate",
          "Heart rate"
        )),
        BaseBuilder.buildQuantity(
          Decimal.of(heartRateValue),
          "{beats}/min",
          LOINC_SYSTEM,
          "beats/min"
        )
      );
      components.add(heartRate);
    }
  }

  private static void collectCalorie(
    Collection<Bundle.Entry> entries,
    JSONObject dataElement,
    Bundle.Entry parentEntry,
    Bundle.Entry patientEntry
  ) {
    String calorie = SHBuilder.getValue(dataElement, "calorie");
    String    mean = SHBuilder.getValue(dataElement, "mean_caloricburn_rate");
    String     max = SHBuilder.getValue(dataElement, "max_caloricburn_rate");
    if ( !StringUtils.isBlank(calorie)
      || !StringUtils.isBlank(mean)
      || !StringUtils.isBlank(max)
    ) {
      String uuid = dataElement.getString("data_uuid");

      Collection<Observation.Component> components = new LinkedList<>();
      //
      // Calorie
      if (!StringUtils.isBlank(calorie) ) {
        Observation.Component calorieComponent = SHBuilder.buildObservationComponent(
          buildCodeableConcept(buildCoding(
            LOCAL_SYSTEM,
            "calorie",
            "Calorie"
          )),
          BaseBuilder.buildQuantity(
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
        Observation.Component meanComponent = SHBuilder.buildObservationComponent(
          buildCodeableConcept(buildCoding(
            LOCAL_SYSTEM,
            "mean_caloricburn_rate",
            "Mean caloric burn rate"
          )),
          BaseBuilder.buildQuantity(
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
        Observation.Component maxComponent = SHBuilder.buildObservationComponent(
          buildCodeableConcept(buildCoding(
            LOCAL_SYSTEM,
            "max_caloricburn_rate",
            "Max caloric burn rate"
          )),
          BaseBuilder.buildQuantity(
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
      Bundle.Entry aggregatedObservation = SHBuilder.buildAggregatedObservation(
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
          Bundle.Entry liveObs = SHBuilder.buildLiveObservation(
            liveId,
            liveElement,
            dataElement,
            buildCodeableConcept(buildCoding(
              SHBuilder.SAMSUNG_LIVE_SYSTEM,
              "live_data_calorie",
              "Live data calorie"
            )),
            BaseBuilder.buildQuantity(
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
    }
  }

  private static void collectCount(
    Collection<Bundle.Entry> entries,
    JSONObject dataElement,
    Bundle.Entry parentEntry,
    Bundle.Entry patientEntry
  ) {
    JSONObject values = dataElement.getJSONArray("values").getJSONObject(0);
    String count = SHBuilder.getValue(dataElement, "count");
    if (!StringUtils.isBlank(count)) {
      String uuid = dataElement.getString("data_uuid");
      String countType = "..."; // TODO change this to a proper unit
      if (values.has("count_type")) {
        countType = SHBuilder.getCountType(values.getString("count_type"));
      } else if (dataElement.has("type_id")) {
        String typeId = dataElement.getString("type_id");
        if (typeId.equals("stepDailyTrend")) {
          countType = "steps";
        }
      }

      Collection<Observation.Component> components = new LinkedList<>();
      //
      // count
      Observation.Component countComponent = SHBuilder.buildObservationComponent(
        buildCodeableConcept(buildCoding(
          LOCAL_SYSTEM,
          "count",
          "Count"
        )),
        BaseBuilder.buildQuantity(
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
      Bundle.Entry aggregatedObservation = SHBuilder.buildAggregatedObservation(
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
          Bundle.Entry liveObs = SHBuilder.buildLiveObservation(
            liveId,
            liveElement,
            dataElement,
            buildCodeableConcept(buildCoding(
              SHBuilder.SAMSUNG_LIVE_SYSTEM,
              "live_data_count",
              "Live data count"
            )),
            BaseBuilder.buildQuantity(
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
    }
  }

  private static void collectDistance(
    Collection<Bundle.Entry> entries,
    JSONObject dataElement,
    Bundle.Entry parentEntry,
    Bundle.Entry patientEntry
  ) {
    String        distance = SHBuilder.getValue(dataElement, "distance");
    String inclineDistance = SHBuilder.getValue(dataElement, "incline_distance");
    String declineDistance = SHBuilder.getValue(dataElement, "decline_distance");
    if ( !StringUtils.isBlank(distance)
            || !StringUtils.isBlank(inclineDistance)
            || !StringUtils.isBlank(declineDistance)
    ) {
      String uuid = dataElement.getString("data_uuid");

      Collection<Observation.Component> components = new LinkedList<>();
      //
      // Distance
      if (!StringUtils.isBlank(distance)) {
        Observation.Component distanceComponent = SHBuilder.buildObservationComponent(
          buildCodeableConcept(buildCoding(
            LOCAL_SYSTEM,
            "distance",
            "Distance"
          )),
          BaseBuilder.buildQuantity(
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
        Observation.Component inclineComponent = SHBuilder.buildObservationComponent(
          buildCodeableConcept(buildCoding(
            LOCAL_SYSTEM,
            "incline_distance",
            "Incline distance"
          )),
          BaseBuilder.buildQuantity(
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
        Observation.Component inclineComponent = SHBuilder.buildObservationComponent(
          buildCodeableConcept(buildCoding(
            LOCAL_SYSTEM,
            "decline_distance",
            "Decline distance"
          )),
          BaseBuilder.buildQuantity(
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
      Bundle.Entry aggregatedObservation = SHBuilder.buildAggregatedObservation(
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
          Bundle.Entry liveObs = SHBuilder.buildLiveObservation(
            liveId,
            liveElement,
            dataElement,
            buildCodeableConcept(buildCoding(
              SHBuilder.SAMSUNG_LIVE_SYSTEM,
              "live_data_distance",
              "Live data distance"
            )),
            BaseBuilder.buildQuantity(
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
    }
  }

  private static void collectSpeed(
    Collection<Bundle.Entry> entries,
    JSONObject dataElement,
    Bundle.Entry parentEntry,
    Bundle.Entry patientEntry
  ) {
    String     speed = SHBuilder.getValue(dataElement, "speed");
    String meanSpeed = SHBuilder.getValue(dataElement, "mean_speed");
    String  maxSpeed = SHBuilder.getValue(dataElement, "max_speed");
    if ( !StringUtils.isBlank(speed)
      || !StringUtils.isBlank(meanSpeed)
      || !StringUtils.isBlank(maxSpeed)
    ) {
      String uuid = dataElement.getString("data_uuid");

      Collection<Observation.Component> components = new LinkedList<>();

      //
      // Speed
      if (!StringUtils.isBlank(speed)) {
        Observation.Component speedComponent = SHBuilder.buildObservationComponent(
          buildCodeableConcept(buildCoding(
            LOCAL_SYSTEM,
            "speed",
            "Speed"
          )),
          BaseBuilder.buildQuantity(
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
        Observation.Component meanComponent = SHBuilder.buildObservationComponent(
          buildCodeableConcept(buildCoding(
            LOCAL_SYSTEM,
            "mean_speed",
            "Mean speed"
          )),
          BaseBuilder.buildQuantity(
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
        Observation.Component maxComponent = SHBuilder.buildObservationComponent(
          buildCodeableConcept(buildCoding(
            LOCAL_SYSTEM,
            "max_speed",
            "Max speed"
          )),
          BaseBuilder.buildQuantity(
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
      Bundle.Entry aggregatedObservation = SHBuilder.buildAggregatedObservation(
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
          Bundle.Entry liveObs = SHBuilder.buildLiveObservation(
            liveId,
            liveElement,
            dataElement,
            buildCodeableConcept(buildCoding(
              SHBuilder.SAMSUNG_LIVE_SYSTEM,
              "live_data_speed",
              "Live data speed"
            )),
            BaseBuilder.buildQuantity(
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
    }
  }

  private static void collectHeartRateInstantaneous(
    Collection<Bundle.Entry> entries,
    JSONObject dataElement,
    Bundle.Entry parentEntry,
    Bundle.Entry patientEntry
  ) {
    String heartRate = SHBuilder.getValue(dataElement, "heart_rate");
    if (!StringUtils.isBlank(heartRate)) {
      String uuid = dataElement.getString("data_uuid");

      Collection<Observation.Component> components = new LinkedList<>();

      //
      // aggregated observation
      String aggregatedId = String.format("%s-heart-rate", uuid);
      Bundle.Entry aggregatedObservation = SHBuilder.buildAggregatedObservation(
        aggregatedId,
        dataElement,
        buildCodeableConcept(buildCoding(
          LOCAL_SYSTEM,
          "heart_rate_instantaneous",
          "Heart Rate Instantaneous"
        )),
        components,
        BaseBuilder.buildQuantity(
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
          Bundle.Entry liveObs = SHBuilder.buildLiveObservation(
            liveId,
            liveElement,
            dataElement,
            buildCodeableConcept(buildCoding(
              SHBuilder.SAMSUNG_LIVE_SYSTEM,
              "live_data_heart_rate_instantaneous",
              "Live data heart rate instantaneous"
            )),
            BaseBuilder.buildQuantity(
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
    }
  }

  private static void collectHeartRateMin(
    Collection<Bundle.Entry> entries,
    JSONObject dataElement,
    Bundle.Entry parentEntry,
    Bundle.Entry patientEntry
  ) {
    String heartRateMin = SHBuilder.getValue(dataElement, "heart_rate_min");
    if (!StringUtils.isBlank(heartRateMin)) {
      String uuid = dataElement.getString("data_uuid");

      Collection<Observation.Component> components = new LinkedList<>();

      //
      // aggregated observation
      String aggregatedId = String.format("%s-heart-rate-min", uuid);
      Bundle.Entry aggregatedObservation = SHBuilder.buildAggregatedObservation(
        aggregatedId,
        dataElement,
        buildCodeableConcept(buildCoding(
          LOCAL_SYSTEM,
          "heart_rate_min",
          "Min Heart Rate"
        )),
        components,
        BaseBuilder.buildQuantity(
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
          Bundle.Entry liveObs = SHBuilder.buildLiveObservation(
            liveId,
            liveElement,
            dataElement,
            buildCodeableConcept(buildCoding(
              SHBuilder.SAMSUNG_LIVE_SYSTEM,
              "live_data_heart_rate_min",
              "Live data min heart rate"
            )),
            BaseBuilder.buildQuantity(
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
    }
  }

  private static void collectHeartRateMax(
    Collection<Bundle.Entry> entries,
    JSONObject dataElement,
    Bundle.Entry parentEntry,
    Bundle.Entry patientEntry
  ) {
    String heartRateMax = SHBuilder.getValue(dataElement, "heart_rate_max");
    if (!StringUtils.isBlank(heartRateMax)) {
      String uuid = dataElement.getString("data_uuid");

      Collection<Observation.Component> components = new LinkedList<>();

      //
      // aggregated observation
      String aggregatedId = String.format("%s-heart-rate-max", uuid);
      Bundle.Entry aggregatedObservation = SHBuilder.buildAggregatedObservation(
        aggregatedId,
        dataElement,
        buildCodeableConcept(buildCoding(
          LOCAL_SYSTEM,
          "heart_rate_max",
          "Max Heart Rate"
        )),
        components,
        BaseBuilder.buildQuantity(
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
          Bundle.Entry liveObs = SHBuilder.buildLiveObservation(
            liveId,
            liveElement,
            dataElement,
            buildCodeableConcept(buildCoding(
              SHBuilder.SAMSUNG_LIVE_SYSTEM,
              "live_data_heart_rate_max",
              "Live data max heart rate"
            )),
            BaseBuilder.buildQuantity(
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
    }
  }

  private static void collectHeartBeatCount(
    Collection<Bundle.Entry> entries,
    JSONObject dataElement,
    Bundle.Entry parentEntry,
    Bundle.Entry patientEntry
  ) {
    String heartBeatCount = SHBuilder.getValue(dataElement, "heart_beat_count");
    if (!StringUtils.isBlank(heartBeatCount)) {
      String uuid = dataElement.getString("data_uuid");

      Collection<Observation.Component> components = new LinkedList<>();

      //
      // aggregated observation
      String aggregatedId = String.format("%s-heart-beat-count", uuid);
      Bundle.Entry aggregatedObservation = SHBuilder.buildAggregatedObservation(
        aggregatedId,
        dataElement,
        buildCodeableConcept(buildCoding(
          LOCAL_SYSTEM,
          "heart_beat_count",
          "Heart Beat Count"
        )),
        components,
        BaseBuilder.buildQuantity(
          Decimal.of(heartBeatCount),
          "beats/min",
          UNITSOFM_SYSTEM,
          "beats/min"
        ),
        parentEntry,
        patientEntry
      );
      entries.add(aggregatedObservation);
    }
  }

  private static void collectCadence(
    Collection<Bundle.Entry> entries,
    JSONObject dataElement,
    Bundle.Entry parentEntry,
    Bundle.Entry patientEntry
  ) {
    String meanCadence = SHBuilder.getValue(dataElement, "mean_cadence");
    String  maxCadence = SHBuilder.getValue(dataElement, "max_cadence");
    if ( !StringUtils.isBlank(meanCadence)
      && !StringUtils.isBlank(maxCadence)
    ) {
      String uuid = dataElement.getString("data_uuid");

      Collection<Observation.Component> components = new LinkedList<>();
      //
      // mean_cadence
      Observation.Component meanComponent = SHBuilder.buildObservationComponent(
        buildCodeableConcept(buildCoding(
          LOCAL_SYSTEM,
          "mean_cadence",
          "Mean Cadence"
        )),
        BaseBuilder.buildQuantity(
          Decimal.of(meanCadence),
          "m/s",
          UNITSOFM_SYSTEM,
          "m/s"
        )
      );
      components.add(meanComponent);

      //
      // max_cadence
      Observation.Component maxComponent = SHBuilder.buildObservationComponent(
        buildCodeableConcept(buildCoding(
          LOCAL_SYSTEM,
          "max_cadence",
          "Max Cadence"
        )),
        BaseBuilder.buildQuantity(
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
      Bundle.Entry aggregatedObservation = SHBuilder.buildAggregatedObservation(
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
          Bundle.Entry liveObs = SHBuilder.buildLiveObservation(
            liveId,
            liveElement,
            dataElement,
            buildCodeableConcept(buildCoding(
              SHBuilder.SAMSUNG_LIVE_SYSTEM,
              "live_data_cadence",
              "Live data cadence"
            )),
            BaseBuilder.buildQuantity(
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
    }
  }

  private static void collectPower(
    Collection<Bundle.Entry> entries,
    JSONObject dataElement,
    Bundle.Entry parentEntry,
    Bundle.Entry patientEntry
  ) {
    String meanPower = SHBuilder.getValue(dataElement, "mean_power");
    String  maxPower = SHBuilder.getValue(dataElement, "max_power");
    if ( !StringUtils.isBlank(meanPower)
      && !StringUtils.isBlank(maxPower)
    ) {
      String uuid = dataElement.getString("data_uuid");

      Collection<Observation.Component> components = new LinkedList<>();
      //
      // mean_power
      Observation.Component meanComponent = SHBuilder.buildObservationComponent(
        buildCodeableConcept(buildCoding(
          LOCAL_SYSTEM,
          "mean_power",
          "Mean Power"
        )),
        BaseBuilder.buildQuantity(
          Decimal.of(meanPower),
          "watt",
          UNITSOFM_SYSTEM,
          "W"
        )
      );
      components.add(meanComponent);

      //
      // max_power
      Observation.Component maxComponent = SHBuilder.buildObservationComponent(
        buildCodeableConcept(buildCoding(
          LOCAL_SYSTEM,
          "max_power",
          "Max Power"
        )),
        BaseBuilder.buildQuantity(
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
      Bundle.Entry aggregatedObservation = SHBuilder.buildAggregatedObservation(
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
          Bundle.Entry liveObs = SHBuilder.buildLiveObservation(
            liveId,
            liveElement,
            dataElement,
            buildCodeableConcept(buildCoding(
              SHBuilder.SAMSUNG_LIVE_SYSTEM,
              "live_data_power",
              "Live data power"
            )),
            BaseBuilder.buildQuantity(
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
    }
  }

  private static void collectAltitude(
    Collection<Bundle.Entry> entries,
    JSONObject dataElement,
    Bundle.Entry parentEntry,
    Bundle.Entry patientEntry
  ) {
    String altitudeGain = SHBuilder.getValue(dataElement, "altitude_gain");
    String altitudeLoss = SHBuilder.getValue(dataElement, "altitude_loss");
    String  minAltitude = SHBuilder.getValue(dataElement, "min_altitude");
    String  maxAltitude = SHBuilder.getValue(dataElement, "max_altitude");
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
        Observation.Component gainComponent = SHBuilder.buildObservationComponent(
          buildCodeableConcept(buildCoding(
            LOCAL_SYSTEM,
            "altitude_gain",
            "Altitude gain"
          )),
          BaseBuilder.buildQuantity(
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
        Observation.Component lossComponent = SHBuilder.buildObservationComponent(
          buildCodeableConcept(buildCoding(
            LOCAL_SYSTEM,
            "altitude_loss",
            "Altitude loss"
          )),
          BaseBuilder.buildQuantity(
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
        Observation.Component minComponent = SHBuilder.buildObservationComponent(
          buildCodeableConcept(buildCoding(
            LOCAL_SYSTEM,
            "min_altitude",
            "Min altitude"
          )),
          BaseBuilder.buildQuantity(
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
        Observation.Component maxComponent = SHBuilder.buildObservationComponent(
          buildCodeableConcept(buildCoding(
            LOCAL_SYSTEM,
            "max_altitude",
            "Max Altitude"
          )),
          BaseBuilder.buildQuantity(
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
      Bundle.Entry aggregatedObservation = SHBuilder.buildAggregatedObservation(
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
    }
  }

  private static void collectDuration(
    Collection<Bundle.Entry> entries,
    JSONObject dataElement,
    Bundle.Entry parentEntry,
    Bundle.Entry patientEntry
  ) {
    String duration = SHBuilder.getValue(dataElement, "duration");
    if ( !StringUtils.isBlank(duration)) {
      String uuid = dataElement.getString("data_uuid");
//      double d = Float.parseFloat(duration);
//      String dd = String.valueOf(d * 1000);
      Collection<Observation.Component> components = new LinkedList<>();

      //
      // aggregated observation
      String aggregatedId = String.format("%s-duration", uuid);
      Bundle.Entry aggregatedObservation = SHBuilder.buildAggregatedObservation(
        aggregatedId,
        dataElement,
        buildCodeableConcept(buildCoding(
          LOCAL_SYSTEM,
          "duration",
          "Duration"
        )),
        components,
        BaseBuilder.buildQuantity(
          Decimal.of(duration),
          "ms",
          UNITSOFM_SYSTEM,
          "ms"
        ),
        parentEntry,
        patientEntry
      );
      entries.add(aggregatedObservation);
    }
  }

  private static void collectHeartRate(
    Collection<Bundle.Entry> entries,
    JSONObject dataElement,
    Bundle.Entry parentEntry,
    Bundle.Entry patientEntry
  ) {
    String  minHeartRate = SHBuilder.getValue(dataElement, "min_heart_rate");
    String meanHeartRate = SHBuilder.getValue(dataElement, "mean_heart_rate");
    String  maxHeartRate = SHBuilder.getValue(dataElement, "max_heart_rate");
    if ( !StringUtils.isBlank(minHeartRate)
      || !StringUtils.isBlank(meanHeartRate)
      || !StringUtils.isBlank(maxHeartRate)
    ) {
      String uuid = dataElement.getString("data_uuid");

      Collection<Observation.Component> components = new LinkedList<>();

      //
      // Heart Rate Min
      if (!StringUtils.isBlank(minHeartRate)) {
        Observation.Component hrMinComponent = SHBuilder.buildObservationComponent(
          buildCodeableConcept(buildCoding(
            LOCAL_SYSTEM,
            "min_heart_rate",
            "Heart Rate Min"
          )),
          BaseBuilder.buildQuantity(
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
        Observation.Component hrMeanComponent = SHBuilder.buildObservationComponent(
          buildCodeableConcept(buildCoding(
            LOCAL_SYSTEM,
            "mean_heart_rate",
            "Heart Rate Mean"
          )),
          BaseBuilder.buildQuantity(
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
        Observation.Component hrMaxComponent = SHBuilder.buildObservationComponent(
          buildCodeableConcept(buildCoding(
            LOCAL_SYSTEM,
            "max_heart_rate",
            "Heart Rate Max"
          )),
          BaseBuilder.buildQuantity(
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
      Bundle.Entry aggregatedObservation = SHBuilder.buildAggregatedObservation(
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
          Bundle.Entry liveObs = SHBuilder.buildLiveObservation(
            liveId,
            liveElement,
            dataElement,
            buildCodeableConcept(buildCoding(
              SHBuilder.SAMSUNG_LIVE_SYSTEM,
              "live_data_heart_rate",
              "Live data heart rate"
            )),
            BaseBuilder.buildQuantity(
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
    }
  }

  private static void collectRpm(
    Collection<Bundle.Entry> entries,
    JSONObject dataElement,
    Bundle.Entry parentEntry,
    Bundle.Entry patientEntry
  ) {
    String meanRpm = SHBuilder.getValue(dataElement, "mean_rpm");
    String  maxRpm = SHBuilder.getValue(dataElement, "max_rpm");
    if ( !StringUtils.isBlank(meanRpm)
      && !StringUtils.isBlank(maxRpm)
    ) {
      String uuid = dataElement.getString("data_uuid");

      Collection<Observation.Component> components = new LinkedList<>();

      //
      // mean_rpm
      if (!StringUtils.isBlank(meanRpm)) {
        Observation.Component meanComponent = SHBuilder.buildObservationComponent(
          buildCodeableConcept(buildCoding(
            LOCAL_SYSTEM,
            "mean_rpm",
            "Mean rpm"
          )),
          BaseBuilder.buildQuantity(
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
        Observation.Component maxComponent = SHBuilder.buildObservationComponent(
          buildCodeableConcept(buildCoding(
            LOCAL_SYSTEM,
            "max_rpm",
            "Max rpm"
          )),
          BaseBuilder.buildQuantity(
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
      Bundle.Entry aggregatedObservation = SHBuilder.buildAggregatedObservation(
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
    }
  }

  private static void collectVo2Max(
    Collection<Bundle.Entry> entries,
    JSONObject dataElement,
    Bundle.Entry parentEntry,
    Bundle.Entry patientEntry
  ) {
    String vo2Max = SHBuilder.getValue(dataElement, "vo2_max");
    if (!StringUtils.isBlank(vo2Max)) {
      String uuid = dataElement.getString("data_uuid");

      Collection<Observation.Component> components = new LinkedList<>();

      //
      // aggregated observation
      String aggregatedId = String.format("%s-vo2-max", uuid);
      Bundle.Entry aggregatedObservation = SHBuilder.buildAggregatedObservation(
        aggregatedId,
        dataElement,
        buildCodeableConcept(buildCoding(
          LOCAL_SYSTEM,
          "vo2_max",
          "Vo2_max"
        )),
        components,
        BaseBuilder.buildQuantity(
          Decimal.of(vo2Max),
          "mL/kg/min",
          UNITSOFM_SYSTEM,
          "mL/kg/min"
        ),
        parentEntry,
        patientEntry
      );
      entries.add(aggregatedObservation);
    }
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
          Observation.Component latitudeComponent = SHBuilder.buildObservationComponent(
            buildCodeableConcept(buildCoding(
              LOCAL_SYSTEM,
              "latitude",
              "Latitude"
            )),
            BaseBuilder.buildQuantity(
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
          Observation.Component longitudeComponent = SHBuilder.buildObservationComponent(
            buildCodeableConcept(buildCoding(
              LOCAL_SYSTEM,
              "longitude",
              "Longitude"
            )),
            BaseBuilder.buildQuantity(
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
          Observation.Component altitudeComponent = SHBuilder.buildObservationComponent(
            buildCodeableConcept(buildCoding(
              LOCAL_SYSTEM,
              "altitude",
              "Altitude"
            )),
            BaseBuilder.buildQuantity(
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
          Observation.Component accuracyComponent = SHBuilder.buildObservationComponent(
            buildCodeableConcept(buildCoding(
              LOCAL_SYSTEM,
              "accuracy",
              "Accuracy"
            )),
            BaseBuilder.buildQuantity(
              // @link https://developer.samsung.com/health/android/data/api-reference/com/samsung/android/sdk/healthdata/HealthConstants.UvExposure.html#ACCURACY
              Decimal.of(locationElement.getString("accuracy")),
              "percent",
              UNITSOFM_SYSTEM,
              "%"
            )
          );
          components.add(accuracyComponent);
        }

        Bundle.Entry location = SHBuilder.buildLocation(
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

}