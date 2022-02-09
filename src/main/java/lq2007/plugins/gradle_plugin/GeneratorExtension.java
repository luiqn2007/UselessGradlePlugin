package lq2007.plugins.gradle_plugin;

import java.io.File;
import java.util.ArrayList;

/**
 * Config: modSourceGenerator
 */
public class GeneratorExtension {

    /**
     * jars directory to depends while compiling
     */
    public Iterable<File> classpaths = new ArrayList<>();

    /**
     * classes output directory
     */
    public File output;

    /**
     * log file
     */
    public File log;

    /**
     * custom plugin package
     */
    public String packageName = "lq2007.plugins.gradle_plugin";

    /**
     * true if clean compiled class files
     */
    public boolean cleanCompiledFiles = true;
}
