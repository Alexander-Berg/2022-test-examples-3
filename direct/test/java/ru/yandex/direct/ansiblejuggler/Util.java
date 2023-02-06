package ru.yandex.direct.ansiblejuggler;

import java.io.IOException;
import java.net.URL;

import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import static com.google.common.io.Resources.getResource;
import static ru.yandex.direct.ansiblejuggler.PlaybookUtils.getMapper;

@ParametersAreNonnullByDefault
public class Util {
    public static String getLocalResource(String resourceName) {
        URL resource = getResource(resourceName);
        try {
            return Resources.toString(resource, Charsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String dumpAsString(Object o) {
        try {
            return getMapper().writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
