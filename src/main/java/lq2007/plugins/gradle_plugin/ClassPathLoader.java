package lq2007.plugins.gradle_plugin;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

class ClassPathLoader implements FileVisitor<Path> {
    private final List<Class<?>> result = new ArrayList<>();

    private final LinkedList<String> packageList = new LinkedList<>();
    private String currentPackage = "";

    private boolean begin = false;
    private final String beginPackage;

    public ClassPathLoader(String beginPackage) {
        this.beginPackage = beginPackage;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
        String s = dir.getFileName().toString();
        if (begin) {
            packageList.add(s);
        } else if (beginPackage.equals(s)) {
            begin = true;
            packageList.add(s);
        }
        currentPackage = String.join(".", packageList);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        String s = file.getFileName().toString();
        if (s.endsWith(".class")) {
            tryLoad(s.substring(0, s.length() - 6));
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) {
        Utils.log(exc);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
        String s = dir.getFileName().toString();
        if (!s.isEmpty() && !packageList.isEmpty()) {
            packageList.removeLast();
            currentPackage = String.join(".", packageList);
        }
        return FileVisitResult.CONTINUE;
    }

    private void tryLoad(String name) {
        try {
            result.add(getClass().getClassLoader().loadClass(currentPackage + "." + name));
        } catch (ClassNotFoundException e) {
            Utils.log(e);
        }
    }

    public List<Class<?>> getResult() {
        return List.copyOf(result);
    }
}
