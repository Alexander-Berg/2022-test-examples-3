package ru.yandex.market.logistics.tarifficator.util;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

@UtilityClass
@ParametersAreNonnullByDefault
public class TestUtils {
    public static final String PARAMETERIZED_TEST_DEFAULT_NAME = "[{index}] {0}";
    private static final String EMPTY_STRING = "";
    private static final JsonFormat.Printer PROTOBUF_TO_JSON_PRINTER = JsonFormat.printer();

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .registerModule(new JavaTimeModule())
        .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    @Nonnull
    public MultiValueMap<String, String> toParams(@Nullable Object obj) {
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        if (obj == null) {
            return parameters;
        }
        Map<String, Object> maps = OBJECT_MAPPER.convertValue(obj, new TypeReference<Map<String, Object>>() {
        });
        maps.forEach((key, value) -> {
            if (value instanceof Collection) {
                Collection<?> values = (Collection<?>) value;
                if (!values.isEmpty()) {
                    values.forEach(v -> parameters.add(key, String.valueOf(v)));
                } else {
                    parameters.add(key, "");
                }
            } else {
                parameters.add(key, String.valueOf(value));
            }
        });
        return parameters;
    }

    @Nonnull
    public ResultMatcher jsonContent(String path, String... ignoredFields) {
        return r -> {
            String content = r.getResponse().getContentAsString(StandardCharsets.UTF_8);
            JSONAssert.assertEquals(
                extractFileContent(path),
                content,
                new CustomComparator(
                    JSONCompareMode.STRICT,
                    Arrays.stream(ignoredFields)
                        .map(fieldName -> new Customization(fieldName, (o1, o2) -> true))
                        .toArray(Customization[]::new)
                )
            );
        };
    }

    @Nonnull
    public InputStream getFileAsInputStream(String path) {
        return Objects.requireNonNull(getSystemResourceAsStream(path));
    }

    @Nonnull
    public ResultMatcher noContent() {
        return content().string(EMPTY_STRING);
    }

    @Nonnull
    public MockHttpServletRequestBuilder request(HttpMethod method, String url, Object body) throws Exception {
        return MockMvcRequestBuilders.request(method, url)
            .contentType(MediaType.APPLICATION_JSON)
            .content(OBJECT_MAPPER.writeValueAsString(body));
    }

    @SneakyThrows
    public void setFieldValue(Object object, @Nullable Field field, @Nullable Object value) {
        if (field == null) {
            return;
        }
        field.setAccessible(true);
        field.set(object, value);
    }

    @SneakyThrows
    @Nonnull
    public String protoMessageToString(MessageOrBuilder message) {
        return PROTOBUF_TO_JSON_PRINTER.print(message);
    }
}
