package org.ou.gatekeeper;

import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.commons.FilenameUtils;
import org.commons.ResourceUtils;
import org.ou.gatekeeper.adapters.DataAdapter;
import org.ou.gatekeeper.adapters.DataAdapters;
import org.ou.gatekeeper.rdf.enums.OutputFormat;
import org.ou.gatekeeper.rdf.enums.OutputFormats;
import org.ou.gatekeeper.rdf.mappings.HelifitMapping;
import org.ou.gatekeeper.rdf.mappings.RMLMapping;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;

/**
 * @author Riccardo Pala (riccardo.pala@open.ac.uk)
 * @todo description
 */
public class RDFizerConsole {

  public static void main(String[] args) throws IOException {
    try {
      CommandLine cmd = setupArguments(args);
      // TODO double-check input validation
      // @see https://stackoverflow.com/questions/1810962/java-commons-cli-options-with-list-of-possible-values

      if (cmd.hasOption("help")) {
        printHelp();
        return;
      }

      if (cmd.hasOption("version")) {
        // TODO see how to suppress SLF4J messages
        final Properties properties = new Properties();
        properties.load(ResourceUtils.getResourceAsStream("project.properties"));
        System.out.println(properties.getProperty("version"));
        return;
      }

      setupInput(cmd);
      setupOutput(cmd);
      setupAdapter(cmd);
      if (!outputFileExt.equals("fhir")) {
        setupMapping();
      }
      run();

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

  private static String inputFilename;
  private static String outputFilename;
  private static File outputDir;
  private static String outputFileExt;

  private static final Options options = new Options();

  /**
   * This class is not instantiable
   */
  private RDFizerConsole() {
  }

  private static void run() {
    File inputFile = new File(inputFilename);
    String outputExt = outputFileExt.equals("fhir") ? "fhir.json" : outputFileExt;
    if (inputFile.isDirectory()) {
      File inputDir = inputFile;
      String[] exts = { "json" };
      Iterator<File> inputDirFiles = FileUtils.iterateFiles(inputDir, exts, true);
      while (inputDirFiles.hasNext()) {
        inputFile = inputDirFiles.next();
        File outputFile = getOutputFile(outputFilename, outputExt, inputFile);
        RDFizer.transform(inputFile, outputFile, adapter, mapping);
      }

    } else {
      File outputFile = getOutputFile(outputFilename, outputExt, inputFile);
      RDFizer.transform(inputFile, outputFile, adapter, mapping);
    }
  }

  private static CommandLine setupArguments(String[] args) throws ParseException {
    options.addOption(
      Option.builder("h")
        .longOpt("help")
        .build());
    options.addOption(
      Option.builder("v")
        .longOpt("version")
        .build());
    // TODO option for the ontology ?
    options.addOption(
      Option.builder()
//      Option.builder("t")
        .longOpt("input-format")
        .hasArg().argName("FORMAT")
        .desc("Format of input data. Values allowed: [ fhir, css, sh ].")
        .build());
    options.addOption(
      Option.builder()
//      Option.builder("i")
        .longOpt("input")
        .hasArg().argName("FILE or DIR")
        .desc("The input file or directory than contains the dataset.")
        .build());
    options.addOption(
      Option.builder()
//      Option.builder("y")
        .longOpt("output-format")
        .hasArg().argName("FORMAT")
        .desc("Type of output data. Values allowed: [ fhir, turtle, nt ]. DEFAULT 'nt'.")
        .build());
    options.addOption(
      Option.builder()
//      Option.builder("o")
        .longOpt("output-file")
        .hasArg().argName("FILE")
        .desc("The output file. If not given, it will take the input filename.")
        .build());
    options.addOption(
      Option.builder()
//      Option.builder("O")
        .longOpt("output-dir")
        .hasArg().argName("DIR")
        .desc("The output directory. DEFAULT './output'. It will be created, if not exists.")
        .build());
    // TODO option to clean temporary files ?
    CommandLineParser parser = new DefaultParser();
    return parser.parse(options, args);
  }

  private static void printHelp() {
    // TODO change follow 'header' and 'footer'
    String header = "Do something useful with an input file\n\n";
    String footer = "\nPlease report issues at https://github.com/GATEKEEPER-OU/mkg-rdfizer/issues";
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("rdfizer", header, options, footer, true);
  }

  private static void setupAdapter(CommandLine cmd) throws MissingArgumentException {
    final String INPUT_FORMAT = "input-format";
    if (!cmd.hasOption(INPUT_FORMAT)) {
      String message = String.format("--%s is missing.", INPUT_FORMAT);
      throw new MissingArgumentException(message);
    }
    String value = cmd.getOptionValue(INPUT_FORMAT);
    adapter = DataAdapters.getDataAdapter(value);
  }

  private static void setupMapping() throws MissingArgumentException {
    if (outputFileExt == null) {
      String funcName = "setupOutputFileExt()";
      String message = String.format("'%s' should be call first", funcName);
      throw new IllegalStateException(message);
    }
    OutputFormat format = OutputFormats.getOutputFormat(outputFileExt);
    mapping = HelifitMapping.create(format);
  }

  private static void setupInput(CommandLine cmd) throws MissingArgumentException {
    final String INPUT = "input";
    if (!cmd.hasOption(INPUT)) {
      String message = String.format("--%s is missing.", INPUT);
      throw new MissingArgumentException(message);
    }
    inputFilename = cmd.getOptionValue(INPUT);
  }

  private static void setupOutput(CommandLine cmd) {
    //
    final String OUTPUT_DIR = "output-dir";
    String outputPath = cmd.hasOption(OUTPUT_DIR) ? cmd.getOptionValue(OUTPUT_DIR) : "./output";
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

  /**
   * Try to open a file named {@code outputFilename}, if {@code null} uses {@code defaultFile.getName()}.
   * @param outputFilename output filename
   * @param outputExt output file extension
   * @param defaultFile file pointer containing the default filename
   * @returns output file pointer.
   * */
  private static File getOutputFile(String outputFilename, String outputExt, File defaultFile) {
    if (outputFilename == null) {
      String inputFilename = defaultFile.getName();
      outputFilename = FilenameUtils.trimExtension(inputFilename);
    }
    String outputFilenameWithExt = FilenameUtils.nameWithTimestamp(outputFilename, outputExt);
    File outputFile = new File(outputDir, outputFilenameWithExt);
    return outputFile;
  }

}