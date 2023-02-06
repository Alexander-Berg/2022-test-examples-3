package ru.yandex.market.logistics.validation;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.validation.ConstraintViolation;

import one.util.streamex.StreamEx;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Хотя бы одно поле не blank")
public class AtLeastOneFieldNotBlankValidatorTest extends AbstractTest {
    private static final String BLANK_STRING = " \n\t ";
    private static final String NOT_BLANK_STRING = "some value";
    private static final List<String> STRING_VARIANTS
        = Arrays.asList(null, StringUtils.EMPTY, BLANK_STRING, NOT_BLANK_STRING);

    @ParameterizedTest
    @MethodSource
    @DisplayName("Проверки единственной аннотации")
    void success(SingleAnnotatedClass value, boolean passed) {
        var validationResult = validator.validate(value);

        if (passed) {
            softly.assertThat(validationResult).isEmpty();
        } else {
            softly.assertThat(validationResult)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("at least one field must not be blank: [field1, field2]");
        }
    }

    @Nonnull
    static Stream<Arguments> success() {
        return StreamEx.cartesianPower(3, STRING_VARIANTS).map(fields -> {
            String field1 = fields.get(0);
            String field2 = fields.get(1);

            return Arguments.of(
                new SingleAnnotatedClass(field1, field2, fields.get(2)),
                field1 == NOT_BLANK_STRING || field2 == NOT_BLANK_STRING
            );
        });
    }

    @ParameterizedTest
    @MethodSource
    @DisplayName("Поле не является CharSequence")
    void incorrectField(IncorrectAnnotatedClass value, boolean passed) {
        var validationResult = validator.validate(value);

        if (passed) {
            softly.assertThat(validationResult).isEmpty();
        } else {
            softly.assertThat(validationResult)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("at least one field must not be blank: [field1, field2]");
        }
    }

    @Nonnull
    static Stream<Arguments> incorrectField() {
        return StreamEx.of(STRING_VARIANTS)
            .cross(null, 0L)
            .mapKeyValue(
                (str, number) -> Arguments.of(new IncorrectAnnotatedClass(str, number), str == NOT_BLANK_STRING)
            );
    }

    @AtLeastOneFieldNotBlank({"field1", "field2"})
    static class SingleAnnotatedClass {
        String field1;
        String field2;
        String field3;

        SingleAnnotatedClass(String field1, String field2, String field3) {
            this.field1 = field1;
            this.field2 = field2;
            this.field3 = field3;
        }

        @Override
        public String toString() {
            return String.format("%s, %s, %s", describe(field1), describe(field2), describe(field3));
        }
    }

    @AtLeastOneFieldNotBlank({"field1", "field2"})
    static class IncorrectAnnotatedClass {
        String field1;
        Long field2;

        IncorrectAnnotatedClass(String field1, Long field2) {
            this.field1 = field1;
            this.field2 = field2;
        }

        @Override
        public String toString() {
            return String.format("%s, %s", describe(field1), field2);
        }
    }

    @Nonnull
    private static String describe(String str) {
        if (str == null) {
            return "<null>";
        }
        if (str.isEmpty()) {
            return "<empty>";
        }
        if (StringUtils.isBlank(str)) {
            return "<blank>";
        }
        return str;
    }
}
