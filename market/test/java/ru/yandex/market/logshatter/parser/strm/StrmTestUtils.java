package ru.yandex.market.logshatter.parser.strm;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.apache.commons.io.FileUtils;

public class StrmTestUtils {
    private StrmTestUtils() {
    }

    public static List<String> readTestLines(String resourceName) {
        URL resource = StrmTestUtils.class.getClassLoader().getResource(resourceName);

        assert resource != null : "Cannot open test resource: " + resourceName;

        try {
            return FileUtils.readLines(new File(resource.toURI()));
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException("Error while reading lines from resource: " + e.toString());
        }
    }

    public static String readTestLine(String resourceName) {
        return readTestLine(resourceName, 0);
    }

    public static String readTestLine(String resourceName, int lineNo) {
        return readTestLines(resourceName).get(lineNo);
    }
}
