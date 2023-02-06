package ru.yandex.direct.grid.processing.util;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.module.kotlin.KotlinModule;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;

public class GraphQlJsonUtils {

    private static final ObjectMapper GRAPHQL_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule()
                    .addSerializer(LocalDate.class, new LocalDateSerializer(ISO_LOCAL_DATE))
                    .addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(ISO_LOCAL_DATE_TIME))
            )
            .registerModule(new SimpleModule()
                    .addSerializer(Enum.class, new GraphQlEnumSerializer()))
            .registerModule(new Jdk8Module())
            .registerModule(new KotlinModule())
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .configure(JsonGenerator.Feature.QUOTE_FIELD_NAMES, false);

    public static String graphQlSerialize(Object obj) {
        try {
            return GRAPHQL_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("can not serialize object to json", e);
        }
    }

    public static <T> T convertValue(Object fromValue, Class<T> toValueType) {
        return GRAPHQL_MAPPER.convertValue(fromValue, toValueType);
    }

    /**
     * Конвертация для классов содержащих generic поля
     */
    public static <T> T convertValue(Object fromValue, TypeReference<T> typeReference) {
        return GRAPHQL_MAPPER.convertValue(fromValue, typeReference);
    }

    private static class GraphQlEnumSerializer extends JsonSerializer<Enum> {
        @Override
        public void serialize(Enum value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeRawValue(value.name());
        }

        @Override
        public Class<Enum> handledType() {
            return Enum.class;
        }
    }
}
