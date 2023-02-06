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

class ErrorPairInvalidTest {

    @MethodSource("data")
    @ParameterizedTest
    void errorPairWithoutMessageTest(ErrorPair invalidErrorPair, String invalidFieldName) {

        Set<ConstraintViolation<ErrorPair>> constraintViolations = VALIDATOR.validate(invalidErrorPair);

        Assertions.assertEquals(1, constraintViolations.size());
        Assertions.assertTrue(constraintViolations.stream()
            .anyMatch(constraintViolation -> constraintViolation.getPropertyPath().toString().equals(invalidFieldName))
        );
    }

    static Stream<Arguments> data() {
        return Stream.of(
            of(new ErrorPair(ErrorCode.FAILED_TO_PARSE_XML, ""), "message"),
            of(new ErrorPair(ErrorCode.FAILED_TO_PARSE_XML, null), "message"),
            of(new ErrorPair(ErrorCode.FAILED_TO_PARSE_XML, " "), "message"),
            of(new ErrorPair(null, "message"), "code")
        );
    }
}
