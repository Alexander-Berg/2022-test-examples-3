package ru.yandex.autotests.direct.cmd.util;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.autotests.direct.cmd.steps.base.DirectCmdStepsException;

public class FileUtils {
    private static final Logger logger = LoggerFactory.getLogger(FileUtils.class);

    public static String getFilePath(final String fileName) {
        if (getResourceFromClasspath(fileName) != null) {
            return getPathForResource(fileName);
        }
        return getPathForSystemFile(fileName);
    }

    private static String getPathForResource(final String fileName) {
        return getResourceFromClasspath(fileName).getPath();
    }

    public static URL getResourceFromClasspath(final String fileName) {
        return Thread.currentThread().getContextClassLoader().getResource(fileName);
    }

    private static String getPathForSystemFile(final String fileName) {
        File file = new File(fileName);
        return file.getPath();
    }

    public static File downloadToTempFile(String fileUrl) {
        Throwable cause;
        String name = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
        try {
            URL url = new URL(fileUrl);
            File tmp = File.createTempFile("FileUtils-", name);
            tmp.deleteOnExit();
            org.apache.commons.io.FileUtils.copyURLToFile(url, tmp);
            return tmp;
        } catch (MalformedURLException e) {
            logger.warn("Некорректный url", e);
            cause = e;
        } catch (IOException e) {
            logger.warn("Ошибка получения файла", e);
            cause = e;
        }
        throw new DirectCmdStepsException("Ошибка получения файла", cause);
    }
}
