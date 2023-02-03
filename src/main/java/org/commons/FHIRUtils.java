package org.commons;

import org.apache.commons.lang.ArrayUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.util.UUID;

/**
 * @author Riccardo Pala (riccardo.pala@open.ac.uk)
 * TODO description
 */
public class FHIRUtils {

  /**
   * TODO description
   * explain with a json example, the result with the resourceId
   * @param dataset dataset to normalize
   */
  public static void normalize(File dataset, File output) {
    try (InputStream datasetInputStream = new FileInputStream(dataset)) {
      JSONTokener tokener = new JSONTokener(datasetInputStream);
      JSONObject json = new JSONObject(tokener);
      JSONArray entries = json.getJSONArray("entry");
      siftEntries(entries);
      FileWriter file = new FileWriter(output);
      file.write(json.toString());
      file.close();

    } catch (IOException e) {
      // TODO how to handle it?
      throw new RuntimeException(e);
    }
  }

  /**
   * Loop a
   * ...TODO put the reference to FHIR entries array definition
   * array looking for resources that contain component.
   * @param entries to sift
   */
  private static void siftEntries(JSONArray entries) {
    for (int i = 0; i < entries.length(); ++i) {
      JSONObject entry = entries.getJSONObject(i);
      JSONObject resource = entry.getJSONObject("resource");

      //
      // add systemDomainName to identifier.system or code.system
      // --- fix Patient
      if (resource.getString("resourceType").equals("Patient")) {
        JSONArray identifiers = resource.getJSONArray("identifier");
        appendDomainName(identifiers);
      }
      // --- fix resource
      if (resource.has("code")) {
        JSONArray codes = resource
          .getJSONObject("code")
          .getJSONArray("coding");
        if (codes.length() > 1) {
          String[] codesToRemove = { // TODO refactory this: put in a constant
            "LA11834-1", // Exercise
            "73985-4"  // Exercise activity
            // @note add HERE codes to remove
          };
          removeGenericCodes(codes, codesToRemove);
        }
        appendDomainName(codes);
      }
      // --- fix resource components
      if (resource.has("component")) {
        JSONArray components = resource.getJSONArray("component");
        for (int j=0; j < components.length(); j++) {
          JSONArray codes = components.getJSONObject(j)
            .getJSONObject("code")
            .getJSONArray("coding");
          appendDomainName(codes);
        }
      }
      // ---
      //

      // connect resource to components
      if (resource.getString("resourceType").equals("Observation")) {
//        String resourceId = entry.getString("fullUrl");
        String resourceId = resource.getString("id");
        if (resource.has("component")) {
          JSONArray components = resource.getJSONArray("component");
          connectResourceToComponents(components, resourceId);
        }
      }
      // ---
    }
  }

  /**
   * TODO description
   * */
  private static void removeGenericCodes(JSONArray collection, String[] toRemove) {
    for (int i = 0; i < collection.length(); ++i) {
      JSONObject item = collection.getJSONObject(i);
      String code = item.getString("code");
      if (ArrayUtils.contains(toRemove, code)) {
        collection.remove(i);
      } else {
        // TODO use logger
        System.out.printf("(FHIRNormalizer) WARNING: %s not handled yet. It should be removed\n", code);
      }
    }
  }

  /**
   * TODO description
   * @param collection of identifiers or codes
   */
  private static void appendDomainName(JSONArray collection) {
    for (int i = 0; i < collection.length(); ++i) {
      JSONObject item = collection.getJSONObject(i);
      String system = item.getString("system");
      // --- workround
      if (system.startsWith("identifier=")) {
        system = system.replace("identifier=", "");
      }
      // ---
      String domainName = UrlUtils
        .getHost(system)
        .replace(".", "_");
      item.put("systemDomainName", domainName);
    }
  }

  /**
   * TODO description
   */
  private static void connectResourceToComponents(JSONArray components, String resourceId) {
    // TODO design again this method (better if whole class)
    for (int i = 0; i < components.length(); ++i) {
      JSONObject component = components.getJSONObject(i);
      // TODO rename method needed
      component.put("componentId", UUID.randomUUID()); // append own uuid
      component.put("resourceId", resourceId);                   // append parent resourceId
    }
  }

}