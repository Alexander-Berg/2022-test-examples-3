package ru.yandex.market.jmf.attributes.test.bool;

import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.params.provider.Arguments.arguments;

public class BooleanDescriptorUnwrapTest extends AbstractBooleanDescriptorTest {

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void unwrap(@SuppressWarnings("unused") String testName, Boolean value, Class<?> toType, Object expected) {
        Object result = descriptor.unwrap(attribute, type, value, toType);
        Assertions.assertEquals(expected, result);
    }

    private static Stream<Arguments> data() {
        return Stream.of(
                arguments("null", null, Object.class, null),
                arguments("booleanTrue", Boolean.TRUE, Boolean.class, Boolean.TRUE),
                arguments("booleanFalse", Boolean.FALSE, Boolean.class, Boolean.FALSE),
                arguments("stringTrue", Boolean.TRUE, String.class, "true"),
                arguments("stringFalse", Boolean.FALSE, String.class, "false"),
                arguments("longFalse", Boolean.FALSE, Long.class, 0L),
                arguments("longTrue", Boolean.TRUE, Long.class, 1L),
                arguments("jsonNodeFalse", Boolean.FALSE, JsonNode.class, BooleanNode.valueOf(false)),
                arguments("jsonNodeTrue", Boolean.TRUE, JsonNode.class, BooleanNode.valueOf(true))
        );
    }
}
