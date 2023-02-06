package ru.yandex.market.delivery.deliveryintegrationtests.tool;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class FileUtil {

    private FileUtil() { }

    /**
     * Получает String из файла с телом запроса.
     * Подставляет значения переданных аргументов вместо плейсхолдеров.
     */
    public static String bodyStringFromFile(String filePath, Object... args) {
        String reqBodyString = readFile(filePath);
        return String.format(reqBodyString, args);
    }

    public static File getFile(String filePath) {
        ClassLoader classLoader = FileUtil.class.getClassLoader();
        return new File(classLoader.getResource(filePath).getFile());
    }

    public static String readFile(String filePath) {
        try {
            return new Scanner(getFile(filePath)).useDelimiter("\\Z").next();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
