package ru.yandex.market.abo.bpmn.client;

import java.io.File;
import java.net.URL;

public final class Util {
    private Util() {
        throw new UnsupportedOperationException();
    }

    /**
     * Получение файла из classpath
     * @param path путь к файлу
     */
    public static File getClassPathFile(String path) {
        ClassLoader classLoader = Util.class.getClassLoader();
        URL url = classLoader.getResource(path);
        if (url == null) {
            return null;
        } else {
            return new File(url.getFile());
        }

    }
}
