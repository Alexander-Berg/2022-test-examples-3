package ru.yandex.market.mboc.common.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;

/**
 * @author s-ermakov
 */
public class JavaResourceUtils {
    private JavaResourceUtils() {
    }

    public static void copyFromResources(String resourceName, File copyPath) {
        copyFromResources(resourceName, copyPath.toString());
    }

    public static void copyFromResources(String resourceName, String copyPath) {
        try (InputStream in = JavaResourceUtils.class.getClassLoader().getResourceAsStream(resourceName);
             OutputStream out = new FileOutputStream(copyPath)
        ) {
            IOUtils.copy(in, out);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
