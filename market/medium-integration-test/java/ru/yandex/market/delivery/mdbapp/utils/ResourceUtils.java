package ru.yandex.market.delivery.mdbapp.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.springframework.core.io.ClassPathResource;

public class ResourceUtils {

    private ResourceUtils() {
    }

    public static String getFileContent(String filePath) throws IOException {
        File file = new ClassPathResource(filePath).getFile();
        return Files.readString(file.toPath());
    }
}
