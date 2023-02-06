package ru.yandex.market.yql_test.utils;

import java.io.File;
import java.net.URL;

public class YqlTestUtils {

    private YqlTestUtils() {
    }

    /**
     * Возвращает абсолютный путь до файла из реесурсов.
     * Абсолютный путь нужен, чтобы приложение могло перезаписать кеш-файл.
     */
    public static String getResourcePathInVCS(Class<?> testClass,
                                              String testPathInArcadia,
                                              String runtimeResourcePath) {
        String arcadiaRoot = System.getenv("ARCADIA_ROOT");
        if (arcadiaRoot == null) {
            return null;
        }
        URL resource = testClass.getResource(runtimeResourcePath);
        if (resource == null) {
            throw new RuntimeException(
                    String.format("Mock file doesn't exist, create file %s with content: '[]'",  runtimeResourcePath)
            );
        }
        String absoluteResourcePath = resource.getFile();
        return arcadiaRoot + File.separator
                + testPathInArcadia + File.separator
                + extractRelativePath(absoluteResourcePath, new File(testPathInArcadia).getName());
    }

    private static String extractRelativePath(String path, String baseDirName) {
        File file = new File(path);
        StringBuilder sb = new StringBuilder(file.getName());
        file = file.getParentFile();
        while (!baseDirName.equals(file.getName())) {
            sb.insert(0, File.separator);
            sb.insert(0, file.getName());
            file = file.getParentFile();
        }
        return sb.toString();
    }
}
