package ru.yandex.market.delivery.partnerapimock.util;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUtils {

    private FileUtils() {
    }

    public static String readFile(String filePath) throws URISyntaxException, IOException {
        Path path = Paths.get(FileUtils.class.getResource(filePath).toURI());
        return String.join("\n", Files.readAllLines(path));
    }
}
