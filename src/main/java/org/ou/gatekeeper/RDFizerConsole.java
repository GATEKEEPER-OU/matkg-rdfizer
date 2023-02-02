package org.ou.gatekeeper;

import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.commons.FilenameUtils;
import org.ou.gatekeeper.adapters.DataAdapter;
import org.ou.gatekeeper.adapters.DataAdapters;
import org.ou.gatekeeper.rdf.enums.OutputFormat;
import org.ou.gatekeeper.rdf.enums.OutputFormats;
import org.ou.gatekeeper.rdf.mappings.HelifitMapping;
import org.ou.gatekeeper.rdf.mappings.RMLMapping;

import java.io.File;
import java.util.Iterator;

/**
 * @author Riccardo Pala (riccardo.pala@open.ac.uk)
 * @todo description
 */
public class RDFizerConsole {

  public static void main(String[] args) {
    try {
      CommandLine cmd = setupArguments(args);
      // TODO double-check input validation
      // @see https://stackoverflow.com/questions/1810962/java-commons-cli-options-with-list-of-possible-values

      if (cmd.hasOption("help")) {
        printHelp();
        return;
      }

      setupOutput(cmd);
      setupAdapter(cmd);
      setupMapping();

      File input = new File(cmd.getOptionValue("input"));
      // NOTE doesn't need to be check, because it a required() option
      if (input.isDirectory()) {
        runBatch(cmd, input);
      } else {
        run(cmd, input);
      }

    } catch (ParseException e) {
      // TODO print error message
      System.err.println("ERROR.  Reason: " + e.getMessage());
      printHelp();
    }
  }

  //--------------------------------------------------------------------------//
  // Class definition
  //--------------------------------------------------------------------------//

  private static DataAdapter adapter;
  private static RMLMapping mapping;

  private static String outputFilename;
  private static File outputDir;
  private static String outputFileExt;

  private static final Options options = new Options();

  /**
   * This class is not instantiable
   */
  private RDFizerConsole() {}

  private static CommandLine setupArguments(String[] args) throws ParseException {
    options.addOption(
      Option.builder("h")
        .longOpt("help")
        .build());
    options.addOption(
      Option.builder("t")
        .longOpt("input-type")
        .hasArg().argName("TYPE")
        .desc("Type of input data. Values allowed: [ fhir, css, sh ].")
        .required()
        .build());
    options.addOption(
      Option.builder("i")
        .longOpt("input")
        .hasArg().argName("FILE or DIR")
        .desc("The input file or directory than contains the dataset.")
        .required()
        .build());
    options.addOption(
      Option.builder("x")
        .longOpt("output-format")
        .hasArg().argName("FORMAT")
        .desc("Type of output data. Values allowed: [ turtle, nt ]. DEFAULT 'nt'.")
        .build());
    options.addOption(
      Option.builder("o")
        .longOpt("output-file")
        .hasArg().argName("FILE")
        .desc("The output file. If not given, it will take the input filename.")
        .build());
    options.addOption(
      Option.builder("O")
        .longOpt("output-dir")
        .hasArg().argName("DIR")
        .desc("The output directory. DEFAULT './output'. It will be created, if not exists.")
        .build());
    // TODO option for the ontology ?
    CommandLineParser parser = new DefaultParser();
    return parser.parse(options, args);
  }

  private static void printHelp() {
    // TODO change follow 'header' and 'footer'
    String header = "Do something useful with an input file\n\n";
    String footer = "\nPlease report issues at https://github.com/GATEKEEPER-OU/rdfizer-java/issues";
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("java -jar rdfizer.jar", header, options, footer, true);
  }

  private static void setupOutput(CommandLine cmd) {
    //
    final String OUTPUT_DIR = "output-dir";
    String outputPath = cmd.hasOption(OUTPUT_DIR)  ? cmd.getOptionValue(OUTPUT_DIR) : "./output";
    outputDir = new File(outputPath);
    if (!outputDir.exists()) {
      outputDir.mkdirs();
    }
    //
    final String OUTPUT_FILE = "output-file";
    outputFilename = cmd.hasOption(OUTPUT_FILE) ? cmd.getOptionValue(OUTPUT_FILE) : null;
    //
    final String OUTPUT_FORMAT = "output-format";
    outputFileExt = cmd.hasOption(OUTPUT_FORMAT) ? cmd.getOptionValue(OUTPUT_FORMAT) : "nt";
  }

  private static void setupAdapter(CommandLine cmd) throws MissingArgumentException {
    final String INPUT_TYPE = "input-type";
    if (!cmd.hasOption(INPUT_TYPE)) {
      String message = String.format("'%s' is missing.", INPUT_TYPE);
      throw new MissingArgumentException(message);
    }
    String inputType = cmd.getOptionValue(INPUT_TYPE);
    adapter = DataAdapters.getDataAdapter(inputType);
  }

  private static void setupMapping() throws MissingArgumentException {
    if (outputFileExt == null) {
      String funcName = "setupOutputFileExt()";
      String message = String.format("'%s' should be call first", funcName);
      throw new IllegalStateException(message);
    }
    OutputFormat outputFormat = OutputFormats.getOutputFormat(outputFileExt);
    mapping = HelifitMapping.create(outputFormat);
  }

  private static void run(CommandLine cmd, File inputFile) {
    if (outputFilename == null) {
      String trimmedDatasetName = FilenameUtils.trim2LvlExtension(inputFile.getName());
      outputFilename = FilenameUtils
        .changeExtension(trimmedDatasetName, outputFileExt);
    }
    File outputFile = new File(outputDir, outputFilename);
    RDFizer.trasform(inputFile, adapter, mapping, outputFile);
  }

  private static void runBatch(CommandLine cmd, File inputDir) {
    String[] exts = {"json"};
    Iterator<File> inputDirFiles = FileUtils.iterateFiles(inputDir, exts, true);
    while (inputDirFiles.hasNext()) {
      outputFilename = null; // FORCE output filename to input filename
      File inputFile = inputDirFiles.next();
      run(cmd, inputFile);
    }
  }

}