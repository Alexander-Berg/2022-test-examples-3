package ru.yandex.market.logistics.test.integration.utils;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

public final class QueryParamUtils {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private QueryParamUtils() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    public static MultiValueMap<String, String> toParams(@Nullable Object object) {
        if (object == null) {
            return new LinkedMultiValueMap<>();
        }

        return toMultiValueMap(
            OBJECT_MAPPER.convertValue(object, new TypeReference<Map<String, Object>>() {
            })
        );
    }

    @Nonnull
    private static MultiValueMap<String, String> toMultiValueMap(Map<String, Object> paramMap) {
        MultiValueMap<String, String> paramMultiMap = new LinkedMultiValueMap<>();
        paramMap.forEach(
            (key, value) ->
                (value instanceof Collection ? ((Collection<?>) value).stream() : Stream.of(value))
                    .map(Object::toString)
                    .forEach(v -> paramMultiMap.add(key, v))
        );
        return paramMultiMap;
    }
}
