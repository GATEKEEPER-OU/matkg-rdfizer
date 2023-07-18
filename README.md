# RDFizer <!-- omit in toc -->

This component aims at to transform EMR (T2D Lab Tests) and PHR (Samsung Health data) to HeLiFit ontology through the use of the RML Mapper.


## Build and Create RDFizer.jar with IntelliJ IDEA as development environment

1. git clone https://github.ecodesamsung.com/Health-Innovation/rdfizer.git
2. open the project IntelliJ IDEA
3. make sure you are using Java 17.0.2 (amazon corretto-17) with IntelliJ IDEA: File --> Project Structure --> Project --> SDK

## RDFox configuration for the RDFizer-Data-Test
1. Download RDFox 6.0 from https://www.oxfordsemantic.tech/downloads
2. Create "rdfox" folder under rdfizer/lib
3. Copy the files JRDFox.jar, libRDFox.dll, libRDFox.lib, libRDFox-static.lib under rdfizer/lib/rdfox
4. Copy RDFox licence rdfizer/lib
5. Go to File --> Project Structure --> Libraries --> click on "+" --> Java --> select rdfizer/lib/rdfox/JRDFox.java --> APPLY.
6. Build Project with "Build --> Build Project"


## How to crete RDFizer.jar  from IntelliJ IDEA
1. Go to File --> Project Structure --> Artefacts --> by clicking on the plus symbol -> JAR --> From Modules All Dependencies --> click on "Copy to the output directory and link via manifest"  --> OK --> APPLY
2. Go to File --> Project Structure --> Artefacts --> Select "rdfizer:java" -> Pre-Processing Tab --> tick the box "Run Ant Target" --> click on ... --> selct "Bundle" --> APPLY
3. Create JAR. "Build --> Build Artefact --> Build or rebuild"

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

### Add RDFox java to maven local storage 
Run this command in the command-line from the working directory

```
mvn install:install-file \
   -Dfile=JRDFox.jar \
   -DgroupId=tech.oxfordsemantic \
   -DartifactId=jrdfox\
   -Dversion=6.0 \
   -Dpackaging=jar
```

## if case of extending or editing the RML mappings files, Perform the bundle-helifit operation for merging all the set of RML rules
1. run bundle from ant menu
2. in case of issue, run clean from ant menu

## What is it based on?
1. HeLiFit ontology V1.5.1 - containing all the classes and properties used for transforming EMR (T2D Lab Tests) and PHR (Samsung Health data) into HeLiFit
2. helifit.template.ttl - containing all the RML mapping rules that support the transformation into HeLiFit. You can find the spec of the mapping under rdfizer --> src --> resources --> mappings.helifit


## Some Bookmark

[Install Java Amazon corretto-17](https://docs.aws.amazon.com/corretto/latest/corretto-17-ug/generic-linux-install.html)

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