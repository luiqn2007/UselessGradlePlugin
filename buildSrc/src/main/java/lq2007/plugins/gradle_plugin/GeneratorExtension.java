package lq2007.plugins.gradle_plugin;

import java.io.File;
import java.util.ArrayList;

public class GeneratorExtension {

    public Iterable<File> classpaths = new ArrayList<>();
    public File output;
    public File log;
}
