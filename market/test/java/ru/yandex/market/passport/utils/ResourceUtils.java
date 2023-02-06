package ru.yandex.market.passport.utils;

import java.io.IOException;
import java.net.URL;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

/**
 * @author anmalysh
 * @since 10/27/2018
 */
public class ResourceUtils {

    private ResourceUtils() {
    }

    public static String getJsonResourceAsString(String resource) {
        URL url = Resources.getResource(resource);
        try {
            // We want to have resources to be readble, but don't expect serialization to pretty print
            return Resources.toString(url, Charsets.UTF_8)
                .replaceAll("\n\\s*", "\n")
                .replaceAll("\n", "")
                .replaceAll(": ", ":");
        } catch (IOException e) {
            throw new RuntimeException("Failed to get resource " + resource + " as string", e);
        }
    }
}
