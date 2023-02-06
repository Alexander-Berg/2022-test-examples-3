package ru.yandex.market.mbi.tariffs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.eclipse.jetty.io.RuntimeIOException;

import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.mbi.tariffs.model.PagerResponseInfo;
import ru.yandex.market.partner.error.info.model.ErrorInfo;

/**
 * Утилитный класс для тестов.
 */
public final class TestUtils {
    /**
     * взято из {@link ru.yandex.market.mbi.tariffs.config.JacksonConfig}
     */
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule())
            .setPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CAMEL_CASE)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);

    private TestUtils() {
        throw new UnsupportedOperationException();
    }

    public static List<ErrorInfo> getErrors(String json) {
        try {
            String errorsJson = JsonTestUtil.parseJson(json)
                    .getAsJsonObject().get("errors")
                    .toString();
            TypeFactory typeFactory = objectMapper.getTypeFactory();
            return objectMapper.readValue(errorsJson, typeFactory.constructCollectionType(List.class, ErrorInfo.class));
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
    }

    public static Map<String, String> getErrorDetails(String json, int indexOfError) {
        try {
            String detailJson = JsonTestUtil.parseJson(json)
                    .getAsJsonObject().get("errors")
                    .getAsJsonArray().get(indexOfError)
                    .getAsJsonObject().get("details")
                    .toString();
            TypeFactory typeFactory = objectMapper.getTypeFactory();
            return objectMapper.readValue(detailJson, typeFactory.constructMapType(Map.class, String.class, String.class));
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
    }

    public static <T> T parseOneResult(String json, Class<T> clazz) {
        try {
            String resultJson = JsonTestUtil.parseJson(json)
                    .getAsJsonObject().get("result")
                    .toString();
            return objectMapper.readValue(resultJson, clazz);
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
    }

    public static <T> PagerResponseInfo parsePagerResponse(String json, Class<T> clazz) {
        try {
            String resultJson = JsonTestUtil.parseJson(json)
                    .getAsJsonObject().get("result")
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

    public static <T> List<T> parseListResults(String json, Class<T> clazz) {
        try {
            String resultJson = JsonTestUtil.parseJson(json)
                    .getAsJsonObject().get("result")
                    .toString();
            TypeFactory typeFactory = objectMapper.getTypeFactory();
            return objectMapper.readValue(resultJson, typeFactory.constructCollectionType(List.class, clazz));
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
    }

    public static <T> List<T> convert(List<Object> list, Class<T> clazz) {
        return list.stream()
                .map(obj -> objectMapper.convertValue(obj, clazz))
                .collect(Collectors.toList());
    }
}
