package lq2007.plugins.gradle_plugin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * Plugin
 */
public class PluginMain implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getExtensions().create("sourceTasks", GeneratorExtension.class);
        project.getTasks().register("runSourceTask", SourceGenerator.class);
    }
}
