package ru.yandex.canvas.steps;

import java.io.IOException;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.io.IOUtils;

import static java.nio.charset.StandardCharsets.UTF_8;

@ParametersAreNonnullByDefault
public class ResourceHelpers {
    public static String getResource(String resourceName) throws IOException {
        return IOUtils.toString(ResourceHelpers.class.getResourceAsStream(resourceName), UTF_8);
    }
}
