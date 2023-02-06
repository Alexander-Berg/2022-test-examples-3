package ru.yandex.market.logistics.management.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;
import javax.sql.DataSource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import net.javacrumbs.jsonunit.core.Option;
import net.javacrumbs.jsonunit.spring.JsonUnitResultMatchers;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

public class TestUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper()
        .registerModule(new ParameterNamesModule(JsonCreator.Mode.PROPERTIES));

    private TestUtil() {
    }

    public static ObjectMapper getObjectMapper() {
        return MAPPER;
    }

    public static ResultMatcher testJson(String relativePath) {
        return testJson(relativePath, true);
    }

    public static ResultMatcher testJson(String relativePath, boolean ignoreArrayOrder) {
        JsonUnitResultMatchers json = JsonUnitResultMatchers.json();
        if (ignoreArrayOrder) {
            json = json.when(Option.IGNORING_ARRAY_ORDER);
        }
        return json.isEqualTo(pathToJson(relativePath));
    }

    @Nonnull
    public static ResultMatcher testJson(String relativePath, Option... options) {
        JsonUnitResultMatchers json = JsonUnitResultMatchers.json();
        for (Option option : options) {
            json = json.when(option);
        }
        return json.isEqualTo(pathToJson(relativePath));
    }

    @Nonnull
    public static ResultMatcher noContent() {
        return content().string("");
    }

    @Nonnull
    public static ResultMatcher hasResolvedExceptionContainingMessage(String expectedMessage) {
        return result -> {
            Exception exception = result.getResolvedException();
            if (exception == null) {
                throw new AssertionError("Expected resolved exception, but was null");
            }
            String actualMessage = exception.getMessage();
            if (!actualMessage.contains(expectedMessage)) {
                String message = String.format(
                    "Expected exception message to contain '%s', but exception message was '%s'",
                    expectedMessage,
                    actualMessage
                );
                throw new AssertionError(message);
            }
        };
    }

    public static String emptyJson() {
        return "{}";
    }

    public static String emptyJsonList() {
        return "{ \"entities\" : [] }";
    }

    public static String pathToJson(String relativePath) {
        return Optional.ofNullable(getSystemResourceAsStream(relativePath))
            .map(res -> {
                try (InputStream resource = res) {
                    return IOUtils.toString(resource, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new RuntimeException("Error during reading from file " + relativePath, e);
                }
            })
            .orElseThrow(() -> new IllegalArgumentException("File " + relativePath + " not found"));
    }

    public static ResultMatcher jsonContent(String relativePath) {
        return content().json(pathToJson(relativePath), true);
    }

    public static <T> String pojoToString(T object) {
        try {
            return MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Cannot transform object to string " + object, e);
        }
    }

    public static void executeSqlScript(String path, DataSource dataSource) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource(path));
        populator.execute(dataSource);
    }

    public static Callable<Boolean> noActiveAsyncThreads(ThreadPoolTaskExecutor asyncExecutor) {
        return () -> asyncExecutor.getActiveCount() == 0;
    }

    public static <K, V> MultiValueMap<K, V> toMultiValueMap(Map<K, V> singleValueMap) {
        MultiValueMap<K, V> result = new LinkedMultiValueMap<>();
        singleValueMap.forEach((key, value) -> result.put(key, List.of(value)));
        return result;
    }

    @Nonnull
    public static MockHttpServletRequestBuilder request(HttpMethod method, String url, Object body) {
        return MockMvcRequestBuilders.request(method, url)
            .contentType(MediaType.APPLICATION_JSON)
            .content(pojoToString(body));
    }

    @Nonnull
    public static MockHttpServletRequestBuilder request(HttpMethod method, String url, String relativePath) {
        return MockMvcRequestBuilders.request(method, url)
            .contentType(MediaType.APPLICATION_JSON)
            .content(pathToJson(relativePath));
    }

    @Nonnull
    public static MockHttpServletRequestBuilder request(HttpMethod method, String url) {
        return MockMvcRequestBuilders.request(method, url);
    }

    public static String point(double latitude, double longitude) {
        return longitude + " " + latitude;
    }

    @Nonnull
    public static ResultMatcher validationErrorMatcher(
        String objectName,
        String fieldName,
        String errorCode,
        String message
    ) {
        return ResultMatcher.matchAll(
            jsonPath("errors[0].objectName").value(objectName),
            jsonPath("errors[0].field").value(fieldName),
            jsonPath("errors[0].code").value(errorCode),
            jsonPath("errors[0].defaultMessage").value(message)
        );
    }

    @Nonnull
    public static ResultMatcher fileContent(String relativePath) throws IOException {
        return content()
            .bytes(
                new ClassPathResource(relativePath)
                    .getInputStream()
                    .readAllBytes()
            );
    }

}
