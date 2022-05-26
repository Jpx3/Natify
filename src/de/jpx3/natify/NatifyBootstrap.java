package de.jpx3.natify;

import de.jpx3.natify.relink.NatifyRelinker;
import de.jpx3.natify.shared.TranslationConfiguration;
import de.jpx3.natify.translation.NatifyTranslator;

import java.io.File;

public final class NatifyBootstrap {
  public static void main(String[] args) {
    NatifyLogger.printFancyHeader();

    if (args.length == 0) {
      System.out.println("Use java -jar input.jar <translate/relink>");
      System.exit(1);
      return;
    }

    // translation
    if (args[0].equals("translate")) {
      // inputfile outputdir (configfile)
      if (args.length == 4) {
        File input = new File(args[1]);
        File output = new File(args[2]);

        output.mkdirs();
        TranslationConfiguration configuration = TranslationConfiguration.defaultConfiguration();
        NatifyTranslator.process(input, output, configuration);
      } else {
        System.out.println("Use java -jar input.jar translate <inputfile> <outputdir> <configfile>");
        System.exit(1);
      }
    } else if (args[0].equals("relink")) {
      // inputfile inputdir outputdir (configfile)
      if (args.length == 5) {
        File input = new File(args[1]);
        File inputDirectory = new File(args[2]);
        File output = new File(args[3]);

        output.getParentFile().mkdirs();
        output.delete();
        TranslationConfiguration configuration = TranslationConfiguration.defaultConfiguration();
        NatifyRelinker.process(input, inputDirectory, output, configuration);
      } else {
        System.out.println("Use java -jar input.jar relink <inputfile> <inputdir> <outputdir> <configfile>");
        System.exit(1);
      }
    }
  }
}