package ru.yandex.market.clickphite.utils;

import java.io.File;

public class ResourceUtils {
    private ResourceUtils() {
    }

    public static File getResourceFile(String path) {
        return new File(getResourcePath(path));
    }

    public static String getResourcePath(String path) {
        return ClassLoader.getSystemResource(path).getPath();
    }
}
