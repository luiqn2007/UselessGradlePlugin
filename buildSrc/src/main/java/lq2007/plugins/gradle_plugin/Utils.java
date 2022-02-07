package lq2007.plugins.gradle_plugin;

import org.gradle.api.Task;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;
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

public class Utils {

    public static Path srcPath, resPath, projectPath;

    public static Path logPath, classesPath;

    public static void initialize(GeneratorExtension ext, Task task) throws IOException {
        projectPath = task.getProject().getRootDir().toPath();
        srcPath = projectPath.resolve("src/main/java");
        resPath = projectPath.resolve("src/main/resources/assets");
        if (logPath == null) {
            if (ext.log == null) {
                logPath = resPath.resolve("gen/log.txt");
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
    }

    public static List<Class<?>> compileClasses(Iterable<File> classpaths) throws IOException, URISyntaxException, IllegalAccessException {
        List<String> classes = Files.walk(srcPath.resolve("lq2007/plugins/gradle_plugin"), 1)
                .filter(Files::isRegularFile)
                .map(p -> p.getFileName().toString())
                .filter(p -> p.endsWith(".java"))
                .map(p -> "lq2007.plugins.gradle_plugin." + p.substring(0, p.length() - 5))
                .toList();

        if (classes.isEmpty()) {
            return List.of();
        }

        List<File> sources = classes.stream()
                .map(s -> s.replace(".", "/") + ".java")
                .map(srcPath::resolve)
                .map(Path::toFile)
                .toList();
        List<File> classpath = Stream.concat(Arrays.stream(System.getProperty("java.library.path").split(";")),
                        Arrays.stream(System.getProperty("java.class.path").split(";")))
                .map(File::new)
                .filter(File::exists)
                .distinct()
                .collect(Collectors.toList());
        for (URL url : ((URLClassLoader) Utils.class.getClassLoader()).getURLs()) {
            classpath.add(new File(url.toURI()));
        }
        classpaths.forEach(classpath::add);
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
        MethodHandles.Lookup lookup = MethodHandles.lookup();

        Files.walk(classesPath)
                .filter(Files::isRegularFile)
                .filter(f -> f.getFileName().toString().endsWith(".class"))
                .forEach(f -> {
                    try {
                        byte[] bytes = Files.readAllBytes(f);
                        lookup.defineClass(bytes);
                    } catch (IOException | IllegalAccessException e) {
                        Utils.log(e);
                    }
                });

        if (!listener.getDiagnostics().isEmpty()) {
            for (Diagnostic<? extends JavaFileObject> diagnostic : listener.getDiagnostics()) {
                log(diagnostic.getKind() + diagnostic.getMessage(Locale.ENGLISH));
                log(diagnostic.getSource().toString());
            }
        }

        List<Class<?>> c = new ArrayList<>();
        for (String s : classes) {
            try {
                c.add(lookup.findClass(s));
            } catch (ClassNotFoundException e) {
                Utils.log(e);
            }
        }

        return Collections.unmodifiableList(c);
    }

    public static void log(String message) {
        try {
            Files.writeString(logPath, message + "\n", StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new RuntimeException("message: " + message, e);
        }
    }

    public static void log(Exception e) {
        StringWriter writer = new StringWriter();
        e.printStackTrace(new PrintWriter(writer));
        log(writer.toString());
    }

    public static void deleteDir(Path path) throws IOException {
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
}
