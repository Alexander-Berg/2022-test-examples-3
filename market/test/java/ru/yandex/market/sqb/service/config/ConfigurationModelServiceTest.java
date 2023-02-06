package ru.yandex.market.sqb.service.config;

import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.sqb.exception.SqbConfigurationException;
import ru.yandex.market.sqb.exception.SqbValidationException;
import ru.yandex.market.sqb.model.conf.QueryModel;
import ru.yandex.market.sqb.test.ConfigurationReaderUtils;
import ru.yandex.market.sqb.test.ObjectGenerationUtils;
import ru.yandex.market.sqb.util.SqbGenerationUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.fail;
import static ru.yandex.market.sqb.test.ConfigurationReaderUtils.createReader;

/**
 * Unit-тесты для {@link ConfigurationReaderService}.
 *
 * @author Vladislav Bauer
 */
public class ConfigurationModelServiceTest {

    public static final String CONFIG_CORRECT = "positive-test";
    public static final String CONFIG_INSUFFICIENT_PARAMETER = "insufficient-parameter";
    public static final String CONFIG_DIFFERENT_NAME = "different-parameter-name";
    public static final String CONFIG_INVALID_SQL = "negative-invalid-sql";
    public static final String CONFIG_NOT_EXISTED = "-not-existed-file";


    @ParameterizedTest
    @MethodSource("data")
    void test(
            @Nonnull String name,
            @Nullable Class<? extends Throwable> exceptionClass
    ) {
        if (name.equals(CONFIG_NOT_EXISTED)) {
            // значения параметров должны быть стабильными, иначе testenv считает тест мигающим
            name = ObjectGenerationUtils.createName() + name;
        }
        final String fileName = ConfigurationReaderUtils.xmlFile(name);

        try {
            final QueryModel model = SqbGenerationUtils.readQueryModel(createReader(fileName));

            assertThat(model, notNullValue());

            if (exceptionClass != null) {
                fail(String.format("This test must fail with %s (file: %s)", exceptionClass, fileName));
            }
        } catch (final Exception ex) {
            if (exceptionClass == null || exceptionClass != ex.getClass()) {
                throw ex;
            }
        }
    }

    private static Stream<Arguments> data() {
        return Stream.of(
                // Валидные файлы конфигурации
                Arguments.of(CONFIG_CORRECT, null),
                Arguments.of(CONFIG_INVALID_SQL, null),

                // Невалидные файл конфигурации
                Arguments.of(CONFIG_NOT_EXISTED, SqbConfigurationException.class),
                Arguments.of("negative-alias-bad-name-1", SqbValidationException.class),
                Arguments.of("negative-alias-bad-name-2", SqbValidationException.class),
                Arguments.of("negative-alias-bad-name-3", SqbValidationException.class),
                Arguments.of("negative-alias-duplicated", SqbValidationException.class),
                Arguments.of("negative-alias-wo-name", SqbConfigurationException.class),
                Arguments.of("negative-argument-bad-name", SqbValidationException.class),
                Arguments.of("negative-argument-duplicated", SqbValidationException.class),
                Arguments.of("negative-order-bad-parameter", SqbValidationException.class),
                Arguments.of("negative-order-bad-type", SqbValidationException.class),
                Arguments.of("negative-order-wo-parameter", SqbConfigurationException.class),
                Arguments.of("negative-parameter-bad-name", SqbValidationException.class),
                Arguments.of("negative-parameter-duplicated", SqbValidationException.class),
                Arguments.of("negative-parameter-template-condition", SqbValidationException.class),
                Arguments.of("negative-parameter-template-condition-sql", SqbValidationException.class),
                Arguments.of("negative-parameter-template-sql", SqbValidationException.class),
                Arguments.of("negative-parameter-two-descriptions", SqbValidationException.class),
                Arguments.of("negative-parameter-two-values", SqbValidationException.class),
                Arguments.of("negative-parameter-wo-condition", SqbValidationException.class),
                Arguments.of("negative-parameter-wo-description", SqbValidationException.class),
                Arguments.of("negative-parameter-wo-name", SqbConfigurationException.class),
                Arguments.of("negative-parameter-wo-sql", SqbValidationException.class),
                Arguments.of("negative-parameter-wo-value", SqbValidationException.class),
                Arguments.of("negative-parameter-wrong-template", SqbValidationException.class),
                Arguments.of("negative-query-bad-root", SqbConfigurationException.class),
                Arguments.of("negative-query-wo-base", SqbConfigurationException.class),
                Arguments.of("negative-query-wo-description", SqbConfigurationException.class),
                Arguments.of("negative-query-unknown-attribute", SqbConfigurationException.class),
                Arguments.of("negative-query-unknown-element", SqbConfigurationException.class),
                Arguments.of("negative-template-bad-name", SqbValidationException.class),
                Arguments.of("negative-template-duplicated", SqbValidationException.class),
                Arguments.of("negative-template-wo-condition", SqbValidationException.class),
                Arguments.of("negative-template-wo-name", SqbConfigurationException.class),
                Arguments.of("negative-template-wo-sql", SqbValidationException.class)
        );
    }

}
