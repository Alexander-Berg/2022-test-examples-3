package ru.yandex.market.jmf.attributes.test.time;

import java.time.LocalTime;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.crm.util.Randoms;

import static org.junit.jupiter.params.provider.Arguments.arguments;

public class TimeDescriptorUnwrapTest extends AbstractTimeDescriptorTest {

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void unwrap(@SuppressWarnings("unused") String testName, LocalTime value, Class<?> toType, Object expected) {
        Object result = descriptor.unwrap(attribute, type, value, toType);
        Assertions.assertEquals(expected, result);
    }

    private static Stream<Arguments> data() {
        var randomValue = Randoms.time();

        return Stream.of(
                arguments("null", null, Object.class, null),
                arguments("random", randomValue, LocalTime.class, randomValue),
                arguments("localTime", LocalTime.of(7, 13), LocalTime.class, LocalTime.of(7, 13)),
                arguments("string", LocalTime.of(7, 13), String.class, "07:13:00"),
                arguments("jsonNode", LocalTime.of(7, 13), JsonNode.class, new TextNode("07:13:00"))
        );
    }
}
