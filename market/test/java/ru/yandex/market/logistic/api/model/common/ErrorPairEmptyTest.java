package ru.yandex.market.logistic.api.model.common;

import java.util.Set;
import java.util.stream.Stream;

import javax.validation.ConstraintViolation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.params.provider.Arguments.of;
import static ru.yandex.market.logistic.api.TestUtils.VALIDATOR;

class ErrorPairEmptyTest {

    @MethodSource("data")
    @ParameterizedTest
    void errorPairWithNullCodeAndMessageTest(ErrorPair emptyErrorPair) {

        Set<ConstraintViolation<ErrorPair>> constraintViolations = VALIDATOR.validate(emptyErrorPair);

        Assertions.assertEquals(constraintViolations.size(), 2);
        Assertions.assertTrue(constraintViolations.stream()
            .allMatch(
                constraintViolation -> constraintViolation.getPropertyPath().toString().equals("message") ||
                    constraintViolation.getPropertyPath().toString().equals("code")
            )
        );
    }

    private static Stream<Arguments> data() {
        return Stream.of(
            of(new ErrorPair(null, "")),
            of(new ErrorPair(null, " ")),
            of(new ErrorPair(null, null)),
            of(new ErrorPair())
        );
    }
}
