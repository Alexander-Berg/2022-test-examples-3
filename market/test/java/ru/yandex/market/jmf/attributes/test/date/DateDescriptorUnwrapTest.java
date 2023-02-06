package ru.yandex.market.jmf.attributes.test.date;

import java.time.LocalDate;
import java.util.Date;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.crm.util.Randoms;

import static org.junit.jupiter.params.provider.Arguments.arguments;

public class DateDescriptorUnwrapTest extends AbstractDateDescriptorTest {

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void unwrap(@SuppressWarnings("unused") String testName, LocalDate value, Class<?> toType, Object expected) {
        Object result = descriptor.unwrap(attribute, type, value, toType);
        Assertions.assertEquals(expected, result);
    }

    private static Stream<Arguments> data() {
        var randomValue = Randoms.date();

        return Stream.of(
                arguments("null", null, Object.class, null),
                arguments("random", randomValue, LocalDate.class, randomValue),
                arguments("date", LocalDate.parse("2020-02-22"), Date.class, new Date(1582318800000L)),
                arguments("long", LocalDate.parse("2020-02-22"), Long.class, 1582318800000L),
                arguments("string", LocalDate.parse("2020-02-22"), String.class, "2020-02-22"),
                arguments("jsonNode", LocalDate.parse("2020-02-22"), JsonNode.class, new TextNode("2020-02-22"))
        );
    }
}
