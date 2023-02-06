package ru.yandex.market.apt;

import java.util.Collections;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static ru.yandex.market.apt.utils.ConfigurationFactory.defaultConfiguration;

class ConfigurationValidatorTest extends AbstractTest {
    private final ConfigurationValidator configurationValidator = new ConfigurationValidator();

    @Test
    void validateSucceeded() {
        softly.assertThatCode(() -> configurationValidator.validate(defaultConfiguration())).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @MethodSource("arguments")
    void validateFailed(Configuration configuration, String message) {
        softly.assertThatThrownBy(() -> configurationValidator.validate(configuration))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(message);
    }

    private static Stream<Arguments> arguments() {
        return Stream.of(
            Arguments.of(defaultConfiguration().setInputDirectories(null), "Input directories not provided"),
            Arguments.of(
                defaultConfiguration().setInputDirectories(Collections.emptyList()),
                "Input directories not provided"
            ),
            Arguments.of(defaultConfiguration().setOutputDirectory(null), "Output directory not provided"),
            Arguments.of(defaultConfiguration().setProcessorClassName(null), "Processor class name not provided")
        );
    }


}
