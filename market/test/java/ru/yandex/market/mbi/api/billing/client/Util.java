package ru.yandex.market.mbi.api.billing.client;

import java.io.File;
import java.net.URL;

/**
 * Утилиты для тестирования
 */
public class Util {

    private Util() {
        throw new AssertionError();
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
