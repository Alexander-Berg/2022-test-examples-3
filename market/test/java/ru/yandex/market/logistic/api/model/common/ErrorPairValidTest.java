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

class ErrorPairValidTest {

    @MethodSource("data")
    @ParameterizedTest
    void errorPairValidTest(ErrorPair validErrorPair) {
        Set<ConstraintViolation<ErrorPair>> constraintViolation = VALIDATOR.validate(validErrorPair);

        Assertions.assertTrue(constraintViolation.isEmpty());
    }

    private static Stream<Arguments> data() {
        return Stream.of(
            of(new ErrorPair(ErrorCode.FAILED_TO_PARSE_XML, "message")),
            of(new ErrorPair(ErrorCode.ENTITY_NOT_FOUND, "message", "description"))
        );
    }
}
