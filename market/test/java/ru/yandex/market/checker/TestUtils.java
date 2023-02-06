package ru.yandex.market.checker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.eclipse.jetty.io.RuntimeIOException;

import ru.yandex.market.checker.api.model.PagerResponseInfo;
import ru.yandex.market.common.test.util.JsonTestUtil;


/**
 * Утилитный класс для тестов.
 */
public class TestUtils {
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule())
            .setPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CAMEL_CASE)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);

    private TestUtils() {
        throw new UnsupportedOperationException();
    }

    public static <T> PagerResponseInfo parsePagerResponse(String json, Class<T> clazz) {
        try {
            String resultJson = JsonTestUtil.parseJson(json)
                    .getAsJsonObject()
                    .toString();
            PagerResponseInfo response = objectMapper.readValue(resultJson, PagerResponseInfo.class);

            List<T> parsedItems = response.getItems().stream()
                    .map(obj -> objectMapper.convertValue(obj, clazz))
                    .collect(Collectors.toList());
            response.setItems(new ArrayList<>(parsedItems));
            return response;
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
    }

    public static <T> T parseOneResult(String json, Class<T> clazz) {
        try {
            String resultJson = JsonTestUtil.parseJson(json)
                    .getAsJsonObject()
                    .toString();
            return objectMapper.readValue(resultJson, clazz);
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
    }

    public static <T> List<T> parseListResults(String json, Class<T> clazz) {
        try {
            String resultJson = JsonTestUtil.parseJson(json)
                    .getAsJsonArray()
                    .toString();
            TypeFactory typeFactory = objectMapper.getTypeFactory();
            return objectMapper.readValue(resultJson, typeFactory.constructCollectionType(List.class, clazz));
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
    }

}
