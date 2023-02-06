package ru.yandex.market.delivery.mdbapp;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.params.ParameterizedTest.INDEX_PLACEHOLDER;

@ExtendWith({
    SoftAssertionsExtension.class,
    MockitoExtension.class,
})
public class AbstractTest {
    protected static final String TUPLE_PARAMETERIZED_DISPLAY_NAME = "[" + INDEX_PLACEHOLDER + "] {0}";

    @InjectSoftAssertions
    protected SoftAssertions softly;

    protected ObjectMapper objectMapper = new ObjectMapper()
        .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);
}
