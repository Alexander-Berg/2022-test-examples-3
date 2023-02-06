package ru.yandex.market.tsum.core;

/**
 * @author David Burnazyan <a href="mailto:dburnazyan@yandex-team.ru"></a>
 * @date 26/06/2018
 */
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.net.URL;

public class TestResourceLoader {
    private static final Gson GSON = new GsonBuilder().create();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static <T> T getTestResourceAsObject(String name, Class<T> targetClass, SerializerType serializerType)
        throws IOException {

        URL url = Resources.getResource(name);
        String result = Resources.toString(url, Charsets.UTF_8);

        if (serializerType == SerializerType.JACKSON) {
            return MAPPER.readValue(result.toString(), targetClass);
        } else {
            return GSON.fromJson(result.toString(), targetClass);
        }
    }

    public static String getTestResourceAsString(String name) throws IOException {
        URL url = Resources.getResource(name);
        return Resources.toString(url, Charsets.UTF_8);
    }

    public static String getTestResourceAsString(Class<?> contextClass, String name) throws IOException {
        URL url = Resources.getResource(contextClass, name);
        return Resources.toString(url, Charsets.UTF_8);
    }

    public enum SerializerType {
        JACKSON,
        GSON
    }
}
