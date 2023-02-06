package ru.yandex.market.logistics.lom;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.params.ParameterizedTest.INDEX_PLACEHOLDER;

@ExtendWith(SoftAssertionsExtension.class)
public class AbstractTest {
    protected static final String TUPLE_PARAMETERIZED_DISPLAY_NAME = "[" + INDEX_PLACEHOLDER + "] {0}";
    @InjectSoftAssertions
    protected SoftAssertions softly;

    protected ObjectMapper objectMapper = new ObjectMapper()
        .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
        .registerModule(new JavaTimeModule());
}
