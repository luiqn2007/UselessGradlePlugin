package lq2007.plugins.gradle_plugin;

import lq2007.plugins.gradle_plugin.support.ISourcePlugin;
import lq2007.plugins.gradle_plugin.support.PluginContext;
import lq2007.plugins.gradle_plugin.support.PluginExceptions;
import lq2007.plugins.gradle_plugin.support.PluginHelper;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Task: run genModSourceTask or build
 */
public class SourceGenerator extends DefaultTask {

    private GeneratorExtension ext;
    private PluginHelper helper;
    private Path srcPath;
    private Path logPath;
    private Path classesPath;
    private String packageName;
    private URLClassLoader classLoader;
    private Method addURL;

    /**
     * Run tasks like annotations
     */
    @TaskAction
    public void main() {
        ext = (GeneratorExtension) getProject().getExtensions().getByName("sourceTasks");
        try {
            initialize();

            List<ISourcePlugin> plugins = new ArrayList<>();
            List<Class<?>> result = compileClasses();
            System.out.println("  CompiledFiles " + result.size());

            for (String name: ext.extTasks) {
                Class<?> c = toClass(name);
                if (c != null) {
                    newPlugins(c).forEach(plugins::add);
                }
            }
            for (Class<?> c : result) {
                if (c != null) {
                    newPlugins(c).forEach(plugins::add);
                }
            }
            plugins.forEach(p -> System.out.println("  Plugin " + p));

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
                        log(e);
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
            log(e);
        } finally {
            if (ext.cleanCompiledFiles) {
                try {
                    deleteDir(classesPath);
                } catch (IOException e) {
                    log(e);
                }
            }
        }
    }

    private void initialize() throws IOException, NoSuchMethodException {
        // paths
        Path projectPath = getProject().getRootDir().getAbsoluteFile().toPath();
        srcPath = projectPath.resolve("src").resolve(ext.sourceSet).resolve("java");
        Path resPath = projectPath.resolve("src").resolve(ext.sourceSet).resolve("resources/assets");
        packageName = ext.packageName;
        if (logPath == null) {
            if (ext.log == null) {
                logPath = srcPath.resolve("gen/log.txt");
            } else if (ext.log.isDirectory()) {
                logPath = ext.log.toPath().resolve("log.txt");
            } else {
                logPath = ext.log.toPath();
            }
            Files.createDirectories(logPath.getParent());
            Files.deleteIfExists(logPath);
            Files.createFile(logPath);
        }
        if (ext.output == null || !ext.output.isDirectory()) {
            classesPath = projectPath.resolve("/build/gen_task/classes");
        } else {
            classesPath = ext.output.toPath();
        }
        deleteDir(classesPath);
        Files.createDirectories(classesPath);
        // loader
        classLoader = (URLClassLoader) SourceGenerator.class.getClassLoader();
        addURL = classLoader.getClass().getDeclaredMethod("addURL", URL.class);
        addURL.setAccessible(true);
        // helper
        helper = new PluginHelper(srcPath, resPath, projectPath, logPath, classesPath);
        // result
        System.out.println("Tasks initialized, pkg=" + packageName);
    }

    private void deleteDir(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            Files.walk(path, 1).forEach(p -> {
                try {
                    if (Files.isRegularFile(p)) {
                        Files.delete(p);
                    } else if (!p.equals(path)) {
                        deleteDir(p);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            Files.delete(path);
        }
    }

    private List<Class<?>> compileClasses() throws IOException, URISyntaxException, InvocationTargetException, IllegalAccessException {
        if (packageName == null || packageName.isEmpty()) {
            return Collections.emptyList();
        }
        List<File> sources = Files.walk(srcPath.resolve(packageName.replace(".", "/")))
                .filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().endsWith(".java"))
                .map(Path::toFile)
                .toList();
        List<File> classpath = Stream.concat(Arrays.stream(System.getProperty("java.library.path").split(";")),
                        Arrays.stream(System.getProperty("java.class.path").split(";")))
                .map(File::new)
                .filter(File::exists)
                .distinct()
                .collect(Collectors.toList());
        for (URL url : classLoader.getURLs()) {
            classpath.add(new File(url.toURI()));
        }
        List<File> source = List.of(srcPath.toFile());
        List<File> output = List.of(classesPath.toFile());

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> listener = new DiagnosticCollector<>();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(listener, Locale.ENGLISH, StandardCharsets.UTF_8);
        fileManager.setLocation(StandardLocation.CLASS_OUTPUT, output);
        fileManager.setLocation(StandardLocation.SOURCE_PATH, source);
        fileManager.setLocation(StandardLocation.CLASS_PATH, classpath);
        Iterable<? extends JavaFileObject> units = fileManager.getJavaFileObjectsFromFiles(sources);
        JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, listener, null, null, units);
        task.call();

        addURL.invoke(classLoader, classesPath.toFile().toURI().toURL());

        if (!listener.getDiagnostics().isEmpty()) {
            for (Diagnostic<? extends JavaFileObject> diagnostic : listener.getDiagnostics()) {
                log(diagnostic.getKind() + diagnostic.getMessage(Locale.ENGLISH));
                log(diagnostic.getSource().toString());
            }
        }

        System.out.println(classesPath);
        ClassPathLoader visitor = new ClassPathLoader(this, packageName.split("\\.")[0]);
        Files.walkFileTree(classesPath, visitor);
        return visitor.getResult();
    }

    private Stream<ISourcePlugin> newPlugins(Class<?> c) {
        if (ISourcePlugin.class.isAssignableFrom(c) && !c.isInterface() && !Modifier.isAbstract(c.getModifiers())) {
            if (c.isEnum()) {
                return Arrays.stream(c.getEnumConstants()).map(o -> (ISourcePlugin) o);
            } else {
                try {
                    Object o = c.getConstructor().newInstance();
                    return Stream.of((ISourcePlugin) o);
                } catch (Exception e) {
                    log(e);
                }
            }
        }
        return Stream.empty();
    }

    private Class<?> toClass(String name) {
        try {
            return classLoader.loadClass(name);
        } catch (ClassNotFoundException e) {
            log(e);
            return null;
        }
    }

    void log(String message) {
        try {
            Files.writeString(logPath, message + "\n", StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new RuntimeException("message: " + message, e);
        }
    }

    void log(Exception e) {
        StringWriter writer = new StringWriter();
        e.printStackTrace(new PrintWriter(writer));
        log(writer.toString());
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
