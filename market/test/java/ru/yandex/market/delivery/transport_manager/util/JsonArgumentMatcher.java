package ru.yandex.market.delivery.transport_manager.util;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.mockito.ArgumentMatcher;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;

@Value
@Slf4j
public class JsonArgumentMatcher<T> implements ArgumentMatcher<T> {
    @Nonnull
    String expected;
    ObjectMapper objectMapper = new ObjectMapper();

    @SneakyThrows
    public JsonArgumentMatcher(@Nonnull T expected) {
        if (expected == null) {
            throw new IllegalStateException("Required value should not be null!");
        }
        this.expected = objectMapper.writeValueAsString(expected);
    }

    @SneakyThrows
    @Override
    public boolean matches(T argument) {
        if (argument == null) {
            return false;
        }
        String actual = objectMapper.writeValueAsString(argument);

        JSONCompareResult result = JSONCompare.compareJSON(expected, actual, JSONCompareMode.STRICT);
        if (result.failed()) {
            log.error(result.getMessage());
        }
        return !result.failed();
    }
}
