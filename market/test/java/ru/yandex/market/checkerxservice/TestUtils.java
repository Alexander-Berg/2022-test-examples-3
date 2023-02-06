package ru.yandex.market.checkerxservice;

import java.io.InputStream;

import ru.yandex.market.checkerxservice.utils.ResourceFileReader;

public class TestUtils {
    private static ResourceFileReader resourceReader = new ResourceFileReader();

    public static String readFileToStrong(String fileName) {
        return resourceReader.safeReadFileToString(fileName);
    }

    public static InputStream getResourceStream(String fileName) {
        try {
            return resourceReader.getFileStream(fileName);
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
        return null;
    }
}
