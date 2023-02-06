package ru.yandex.crypta.graph2.matching.human.helper;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class TestDataHelper {

    public static void copyTestResourceToYtWorkdir(Path workdir, String from, String to) throws IOException {
        URL classPathResource = TestDataHelper.class.getClassLoader().getResource(from);
        if (classPathResource == null) {
            throw new IllegalArgumentException("Resource not found: " + from);
        }

        Path path = Paths.get(classPathResource.getFile());

        Path targetPath = workdir.resolve(to);
        Files.createDirectories(targetPath.getParent());

        Files.copy(path, targetPath, StandardCopyOption.REPLACE_EXISTING);
    }
}
