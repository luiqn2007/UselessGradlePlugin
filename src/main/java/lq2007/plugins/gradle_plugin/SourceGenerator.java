package lq2007.plugins.gradle_plugin;

import lq2007.plugins.gradle_plugin.support.*;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Task: run genModSourceTask or build
 */
public class SourceGenerator extends DefaultTask {

    /**
     * Run tasks like annotations
     */
    @TaskAction
    public void main() {
        GeneratorExtension ext = (GeneratorExtension) getProject().getExtensions().getByName("runSourceTask");
        try {
            Utils.initialize(ext, this);
            PluginHelper helper = new PluginHelper(Utils.srcPath, Utils.resPath, Utils.projectPath, Utils.logPath, Utils.classesPath);

            System.out.println("Tasks");
            System.out.println("  Project=" + Utils.projectPath);
            System.out.println("  Src=" + Utils.srcPath);
            System.out.println("  Assets=" + Utils.resPath);
            System.out.println("  Output=" + ext.output);
            System.out.println("  ClassPaths=" + ext.classpaths);
            System.out.println("  LogFile=" + Utils.logPath);

            List<ISourcePlugin> plugins = Utils.compileClasses(ext.classpaths).stream()
                    .filter(ISourcePlugin.class::isAssignableFrom)
                    .filter(c -> !c.isInterface() && !c.isEnum() && !Modifier.isAbstract(c.getModifiers()))
                    .map(c -> {
                        try {
                            return (ISourcePlugin) c.getConstructor().newInstance();
                        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                            e.printStackTrace();
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList();
            System.out.println("  Plugins=" + plugins.size());
            plugins.forEach(p -> System.out.println("    " + p.getClass().getSimpleName() + "=" + p));
            System.out.println();

            List<Entry> entries = plugins.stream()
                    .map(Entry::new)
                    .collect(Collectors.toList());
            boolean stopAll = false;
            while (!entries.isEmpty()) {
                Iterator<Entry> iterator = entries.iterator();
                while (iterator.hasNext()) {
                    Entry next = iterator.next().next(helper);
                    PluginContext context = next.context;
                    Path root = context.root();
                    ISourcePlugin plugin = next.plugin;

                    try {
                        plugin.begin(context, helper);
                    } catch (Exception e) {
                        context.exceptions().setExceptionAtBegin(e);
                    }

                    if (Files.isRegularFile(root)) {
                        handleFile(root, plugin, context, helper);
                    } else if (Files.isDirectory(root)) {
                        Files.walk(root)
                                .filter(Files::isRegularFile)
                                .forEach(f -> handleFile(f, plugin, context, helper));
                    } else {
                        context.exceptions().put(root, new IOException("File not found: " + root));
                    }


                    try {
                        switch (plugin.finished(context, helper)) {
                            case FINISHED -> iterator.remove();
                            case STOP_ALL -> stopAll = true;
                        }
                    } catch (Exception e) {
                        iterator.remove();
                        Utils.log(e);
                    }

                    if (stopAll) {
                        break;
                    }
                }
                if (stopAll) {
                    break;
                }
            }
        } catch (Exception e) {
            getProject().getLogger().error(e.getMessage());
            Utils.log(e);
        }
    }

    private void handleFile(Path path, ISourcePlugin plugin, PluginContext context, PluginHelper helper) {
        try {
            plugin.each(path, context, helper);
        } catch (Exception e) {
            context.exceptions().put(path, e);
        }
    }

    private static class Entry {
        ISourcePlugin plugin;
        PluginContext context;
        int loop = 0;

        public Entry(ISourcePlugin plugin) {
            this.plugin = plugin;
        }

        public Entry next(PluginHelper helper) {
            context = new PluginContext(loop++, plugin.getLoopRoot(helper), new PluginExceptions());
            return this;
        }
    }
}
