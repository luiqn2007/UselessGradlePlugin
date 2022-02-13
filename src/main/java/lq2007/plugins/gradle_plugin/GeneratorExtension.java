package lq2007.plugins.gradle_plugin;

import java.io.File;
import java.util.ArrayList;

/**
 * Config: modSourceGenerator
 */
public class GeneratorExtension {

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
    public String packageName = "";

    /**
     * true if clean compiled class files
     */
    public boolean cleanCompiledFiles = true;

    /**
     * Tasks existed in dependencies
     */
    public Iterable<String> extTasks = new ArrayList<>();
}
