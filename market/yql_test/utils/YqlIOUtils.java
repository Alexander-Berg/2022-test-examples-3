package ru.yandex.market.yql_test.utils;

import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;

public class YqlIOUtils {

    private YqlIOUtils() {
    }

    public static void write(String data, String filePath) {
        try {
            IOUtils.write(data, new FileOutputStream(filePath));
        } catch (IOException e) {
            throw new IllegalStateException("can't write to file " + filePath, e);
        }
    }
}
