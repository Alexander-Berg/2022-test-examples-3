package ru.yandex.chemodan.app.psbilling.core.utils;

import java.util.List;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

public class ClassFinder {
    @SuppressWarnings("unchecked")
    public static <TClass extends Class> List<TClass> findTaskClasses(String packageName, TClass clazz) {
        try (ScanResult scanResult = new ClassGraph().whitelistPackages(packageName)
                .enableClassInfo().scan()) {
            return scanResult
                    .getSubclasses(clazz.getName())
                    .loadClasses(clazz);
        }
    }
}
