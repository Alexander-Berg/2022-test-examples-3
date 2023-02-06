package ru.yandex.market.jmf.attributes.test.bool;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.params.provider.Arguments.arguments;

public class BooleanDescriptorWrapTest extends AbstractBooleanDescriptorTest {

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void wrap(@SuppressWarnings("unused") String testName, Object rawValue, Boolean expected) {
        Boolean result = descriptor.wrap(attribute, type, rawValue);
        Assertions.assertEquals(expected, result);
    }

    private static Stream<Arguments> data() {
        return Stream.of(
                arguments("null", null, null),
                arguments("booleanTrue", Boolean.TRUE, Boolean.TRUE),
                arguments("booleanFalse", Boolean.FALSE, Boolean.FALSE),
                arguments("stringTrue", true, Boolean.TRUE),
                arguments("stringFalse", false, Boolean.FALSE),
                arguments("stringOther", "other", Boolean.FALSE),
                arguments("intZero", 0, Boolean.FALSE),
                arguments("intOne", 1, Boolean.TRUE),
                arguments("intPositive", 7, Boolean.TRUE),
                arguments("doubleZero", .0, Boolean.FALSE),
                arguments("doublePositive", 3.7, Boolean.TRUE)
        );
    }
}
