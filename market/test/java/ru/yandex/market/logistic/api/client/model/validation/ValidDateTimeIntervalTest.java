package ru.yandex.market.logistic.api.client.model.validation;


import java.util.Set;

import javax.validation.ConstraintViolation;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.model.validation.DateTimeIntervalValidator;
import ru.yandex.market.logistic.api.model.validation.ValidDateTimeInterval;
import ru.yandex.market.logistic.api.utils.DateTimeInterval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.logistic.api.TestUtils.VALIDATOR;

/**
 * Тест проверяет работу аннотации {@link ValidDateTimeInterval} и валидатора {@link DateTimeIntervalValidator}.
 */
class ValidDateTimeIntervalTest {

    class ClassWithIntervalField {

        @ValidDateTimeInterval(completeDays = 7)
        DateTimeInterval dateTimeInterval;

        ClassWithIntervalField(DateTimeInterval dateTimeInterval) {
            this.dateTimeInterval = dateTimeInterval;
        }
    }

    @Test
    void positiveScenarioExact() {

        DateTimeInterval validExact7days =
            DateTimeInterval.fromFormattedValue("2019-08-01T09:00:01+03:00/2019-08-08T09:00:01+03:00");
        ClassWithIntervalField classWithIntervalField = new ClassWithIntervalField(validExact7days);
        Set<ConstraintViolation<ClassWithIntervalField>> constraintViolations =
            VALIDATOR.validate(classWithIntervalField);

        assertTrue(constraintViolations.isEmpty());

    }

    @Test
    void positiveScenario() {
        DateTimeInterval validLessThan7days =
            DateTimeInterval.fromFormattedValue("2019-08-04T09:00:01+03:00/2019-08-08T09:00:01+03:00");
        ClassWithIntervalField classWithIntervalField = new ClassWithIntervalField(validLessThan7days);
        Set<ConstraintViolation<ClassWithIntervalField>> constraintViolations =
            VALIDATOR.validate(classWithIntervalField);

        assertTrue(constraintViolations.isEmpty());
    }

    @Test
    void positiveNullableScenario() {
        ClassWithIntervalField classWithIntervalField = new ClassWithIntervalField(null);
        Set<ConstraintViolation<ClassWithIntervalField>> constraintViolations =
            VALIDATOR.validate(classWithIntervalField);

        assertTrue(constraintViolations.isEmpty());
    }

    @Test
    void negativeScenario() {
        DateTimeInterval invalidMoreThan7days =
            DateTimeInterval.fromFormattedValue("2019-07-01T09:00:01+03:00/2019-08-08T09:00:01+03:00");
        ClassWithIntervalField classWithIntervalField = new ClassWithIntervalField(invalidMoreThan7days);
        Set<ConstraintViolation<ClassWithIntervalField>> constraintViolations =
            VALIDATOR.validate(classWithIntervalField);

        assertFalse(constraintViolations.isEmpty());

        assertEquals(
            1,
            constraintViolations.stream()
                .filter((f) -> f.getMessage()
                    .equals("Datetime interval too large"))
                .count()
        );
    }


}
