package ru.yandex.yt.yqltestable;

import java.io.InputStream;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 06.09.2021
 */
public class YqlTestable {
    private static final String HEADER = readFile("/yqltestable/yqltestable_header.sql");

    // helps to insert "with inline" into request, which makes it over 9000% faster
    public static final String INLINE_KEY = "--_INLINE_";

    private YqlTestable() {
    }

    public static String withHeader(String yql) {
        return HEADER + "\n\n" + yql;
    }

    public static String withSelect(String yql, String param) {
        return yql + "\n\n" + "select * from $" + param + ";";
    }

    public static String withAll(String yql, String param) {
        return withSelect(withHeader(yql), param);
    }

    public static String readFile(String resourcePath) {
        try (InputStream stream = YqlTestable.class.getResourceAsStream(resourcePath)) {
            return new String(stream.readAllBytes());
        } catch (Exception e) {
            throw new RuntimeException("Failed to read file: " + resourcePath, e);
        }
    }
}
