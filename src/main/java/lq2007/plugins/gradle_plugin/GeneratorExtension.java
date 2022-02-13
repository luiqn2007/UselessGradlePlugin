package lq2007.plugins.gradle_plugin;

import java.io.File;
import java.util.ArrayList;

/**
 * Config: modSourceGenerator
 */
public class GeneratorExtension {

    /**
     * Classes output directory
     */
    public File output;

    /**
     * Log file
     */
    public File log;

    /**
     * Custom plugin package
     */
    public String packageName = "";

    /**
     * Set source set to find task package
     */
    public String sourceSet = "main";

    /**
     * True if clean compiled class files
     */
    public boolean cleanCompiledFiles = true;

    /**
     * Tasks existed in dependencies
     */
    public Iterable<String> extTasks = new ArrayList<>();
}
