package toolkit;

import java.io.IOException;
import java.text.SimpleDateFormat;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import org.slf4j.Logger;

@Slf4j
public class Mapper {

    private static final ObjectMapper MAPPER = buildDefaultMapper();
    private static final ObjectMapper LMS_MAPPER = buildDefaultMapper()
        .registerModule(new ParameterNamesModule(JsonCreator.Mode.PROPERTIES));

    private Mapper() {
    }

    @Nonnull
    private static ObjectMapper buildDefaultMapper() {
        return new ObjectMapper()
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
            .registerModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true)
            .enable(SerializationFeature.INDENT_OUTPUT)
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
    }

    @Nonnull
    public static ObjectMapper getDefaultMapper() {
        return MAPPER;
    }

    @Nonnull
    public static ObjectMapper getLmsMapper() {
        return LMS_MAPPER;
    }

    public static String writeValueAsString(Object objectToWrite) {
        try {
            return MAPPER.writeValueAsString(objectToWrite);
        } catch (JsonProcessingException e) {
            log.error(Logger.ROOT_LOGGER_NAME, e);
        }
        return null;
    }


    public static <T> T mapResponse(String response, TypeReference<T> typeReference) {
        T result;
        try {
            result = MAPPER.readValue(response, typeReference);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при десериализации ответа " + response);
        }
        return result;
    }


    public static <T> T mapResponse(Response response, TypeReference<T> typeReference) {
        return mapResponse(OkClient.getResponseText(response), typeReference);
    }

    public static <T> T mapResponse(String response, Class<T> clazz) {
        return mapResponse(response, clazz, MAPPER);
    }

    public static <T> T mapLmsResponse(String response, Class<T> clazz) {
        return mapResponse(response, clazz, LMS_MAPPER);
    }

    public static <T> T mapResponse(String response, Class<T> clazz, ObjectMapper mapper) {
        T result;
        try {
            result = mapper.readValue(response, clazz);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при десериализации ответа " + response);
        }
        return result;
    }
}
