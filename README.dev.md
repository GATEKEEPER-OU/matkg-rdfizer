# RDFizer-java <!-- omit in toc -->

This component aims at to transform EMR (T2D Lab Tests) and PHR (Samsung Health data) to HeLiFit ontology through the use of the RML Mapper.



## Step 1. Download the RDFizer java project from github repository

1. Run git clone https://github.ecodesamsung.com/Health-Innovation/rdfizer.git
2. Open the project IntelliJ IDEA
3. Make sure you are using Java 17.0.2 with IntelliJ IDEA: File --> Project Structure --> Project --> SDK

## Step 2. Configure Ant
1. if Tab Ant is present at left side on the IDEA, then None. 
2. otherwise, right click on the build.xml file, "Add as Ant Build file". 

## Step 3. RDFox configuration for the RDFizer-Data-Test
1. Download RDFox 6.0 from https://www.oxfordsemantic.tech/downloads
2. Create "rdfox" folder under rdfizer/lib
3. Copy the files JRDFox.jar, libRDFox.dll, libRDFox.lib, libRDFox-static.lib under rdfizer/lib/rdfox
4. Copy RDFox licence rdfizer/lib
5. Go to File --> Project Structure --> Libraries --> click on "+" --> Java --> select rdfizer/lib/rdfox/JRDFox.java --> APPLY.
6. Build Project with "Build --> Build Project"


## Step 4. How to crete RDFizer.jar  from IntelliJ IDEA (approach maven)
1. Make sure you have installed maven on your machine, https://maven.apache.org/plugins/maven-install-plugin/ 
2. Select Terminal Tab from IDEA and run: mvn clean compile assembly:single
3. The rdfize-ver-jar-with-dependecies.jar with all the dependecies is Targer folder of the project
4. You are ready to import the rdfize-ver-jar-with-dependecies.jar in MatKG. 

## Step 4. How to crete RDFizer.jar  from IntelliJ IDEA (approach editor - To be tested)
1. Go to File --> Project Structure --> Artefacts --> by clicking on the plus symbol -> JAR --> From Modules All Dependencies --> click on "Copy to the output directory and link via manifest"  --> OK --> APPLY
2. Go to File --> Project Structure --> Artefacts --> Select "rdfizer:java" -> Pre-Processing Tab --> tick the box "Run Ant Target" --> click on ... --> selct "Bundle" --> APPLY
3. Create JAR. "Build --> Build Artefact --> Build or rebuild"

## If the RML mapping are modified/updated (in src/main/resources/mappings.helifit/parts)
1. Select Ant Tab, double click on bundle-helifit (you can see the Ant Building in progress at button right)

## Troubleshooting (Review of the below steps)
5. build Project. Build --> Build Project
6. create JAR. Build --> Build Artefact --> Rebuild
7. rdfizer.jar is created and will appear under the project folder: out --> artifacts --> rdfizer_jar

7. In case of ISSUE (e.g.; in case the rdfizer.jar is not generated, especially when the pom is modified when installing the dependencies)
8. Run clean from ant menu (this deletes the out, target, the aggregated rml file mappings folder)
9. Delete META-INF folder from resources/META-INF
10. Run bundle from ant menu (this creates the out, target, the aggregated rml file mappings)
11. Go to File--> Project Structure --> Artefacts --> delete all the jar  you find in there (by clicking on the minus symbol) then APPLY. 
12. Go to File--> Project Structure --> Artefacts --> by clicking on the plus symbol -> JAR --> From Modules All Dependencies --> Copy to the output directory and link via manifest --> OK --> APPLY
13. Go to File--> Project Structure --> Artefacts --> Select RDFizer.java -> Pre-Processing --> tick the box "Run Ant Target" --> click on ... --> selct "Bundle" --> APPLY
14. Go to Step 4. and execute 4. + 5. + 6.
15. Check the folder out/artifacts/<project_name_jar>/ for the file <project_name>.jar (e.g.; rdfizer.jar)

## if case of extending or editing the RML mappings files, Perform the bundle-helifit operation for merging all the set of RML rules
1. run bundle from ant menu
2. in case of issue, run clean from ant menu

## What is it based on?
1. HeLiFit ontology V1.5.1 - containing all the classes and properties used for transforming EMR (T2D Lab Tests) and PHR (Samsung Health data) into HeLiFit
2. helifit.template.ttl - containing all the RML mapping rules that support the transformation into HeLiFit. You can find the spec of the mapping under rdfizer --> src --> resources --> mappings.helifit


## Some Bookmark

[Ant Tutorial](https://www.javaguicodexample.com/antworksheet3.html)

[Logback documentation](https://logback.qos.ch/documentation.html)  
[Logback configuration](https://logback.qos.ch/manual/configuration.html)

[RMLMapper - Examples](https://github.com/RMLio/rmlmapper-java/tree/master/src/test/java/be/ugent/rml)  
[RMLMapper - Usage example](https://github.com/RMLio/rmlmapper-java/blob/master/src/test/java/be/ugent/rml/readme/ReadmeTest.java)  
[RMLMapper - Logback conf](https://github.com/RMLio/rmlmapper-java/blob/master/src/test/resources/logback.xml)

[IBM - FHIR Model Guide](https://ibm.github.io/FHIR/guides/FHIRModelGuide)  
[IBM - FHIR Model Javadoc](https://ibm.github.io/FHIR/javadocs/latest/overview-summary.html)

[HL7 -FHIR Resource List](https://hl7.org/fhir/2021may/resourcelist.html)  
[GATEKEEPER - FHIR Artifacts Summary](https://build.fhir.org/ig/gatekeeper-project/gk-fhir-ig/artifacts.html)

[RDF Visualizer Online](https://issemantic.net/rdf-visualizer)