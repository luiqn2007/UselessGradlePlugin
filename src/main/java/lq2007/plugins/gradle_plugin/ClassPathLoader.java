package lq2007.plugins.gradle_plugin;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ClassPathLoader implements FileVisitor<Path> {
    private final List<Class<?>> result = new ArrayList<>();

    private final LinkedList<String> packageList = new LinkedList<>();
    private String currentPackage = "";

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        System.out.println("Pre: " + packageList + " -> " + dir);
        String s = dir.getFileName().toString();
        if (!s.isEmpty()) {
            packageList.add(s);
            currentPackage = String.join(".", packageList);
        }
        System.out.println("Pre: " + packageList + " <- " + dir);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        System.out.println("Visit: " + packageList + " -> " + file);
        String s = file.getFileName().toString();
        if (s.endsWith(".class")) {
            tryLoad(s.substring(0, s.length() - 6));
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        Utils.log(exc);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        System.out.println("Post: " + packageList + " -> " + dir);
        String s = dir.getFileName().toString();
        if (!s.isEmpty() && !packageList.isEmpty()) {
            packageList.removeLast();
            currentPackage = String.join(".", packageList);
        }
        System.out.println("Post: " + packageList + " <- " + dir);
        return FileVisitResult.CONTINUE;
    }

    private void tryLoad(String name) {
        try {
            System.out.println("  try load " + currentPackage + "." + name);
            result.add(getClass().getClassLoader().loadClass(currentPackage + "." + name));
        } catch (ClassNotFoundException e) {
            Utils.log(e);
        }
    }

    public List<Class<?>> getResult() {
        return List.copyOf(result);
    }
}
