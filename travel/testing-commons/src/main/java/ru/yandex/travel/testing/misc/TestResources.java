package ru.yandex.travel.testing.misc;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.common.base.Preconditions;

public class TestResources {

    private TestResources() {
    }

    public static String readResource(String relativePath) {
        URL resource = TestResources.class.getClassLoader().getResource(relativePath);
        Preconditions.checkNotNull(resource, "No such resource: %s", relativePath);
        try {
            return readFile(resource.toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static String readFile(URI file) {
        return readFile(Paths.get(file));
    }

    public static String readFile(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read; path: " + path, e);
        }
    }
}
