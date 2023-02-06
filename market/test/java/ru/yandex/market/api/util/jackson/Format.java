package ru.yandex.market.api.util.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matcher;
import ru.yandex.market.api.controller.jackson.ObjectMapperFactory;
import ru.yandex.market.api.util.ResourceHelpers;

import java.io.IOException;
import java.util.function.Function;

import static org.hamcrest.Matchers.containsString;

/**
 * @author dimkarp93
 */
abstract class Format {
    private final ObjectMapperFactory mapperFactory;
    private final Function<ObjectMapperFactory, ObjectMapper> func;

    Format(ObjectMapperFactory mapperFactory, Function<ObjectMapperFactory, ObjectMapper> func) {
        this.mapperFactory = mapperFactory;
        this.func = func;
    }

    public <T> T read(String path, Class<T> clazz) {
        try {
            return func.apply(mapperFactory).readValue(ResourceHelpers.getResource(path), clazz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> String write(T entity) {
        try {
            return func.apply(mapperFactory).writeValueAsString(entity);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public Matcher<String> match(String fieldName, String param) {
        return containsString(String.format(template(), fieldName, param));
    }

    protected abstract String template();
}
