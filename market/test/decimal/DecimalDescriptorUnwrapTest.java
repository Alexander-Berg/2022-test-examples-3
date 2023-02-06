package ru.yandex.market.jmf.attributes.test.decimal;

import java.math.BigDecimal;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.DecimalNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.crm.util.Randoms;

import static org.junit.jupiter.params.provider.Arguments.arguments;

public class DecimalDescriptorUnwrapTest extends AbstractDecimalDescriptorTest {

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void unwrap(@SuppressWarnings("unused") String testName, BigDecimal value, Class<?> toType,
                       Object expected) {
        Object result = descriptor.unwrap(attribute, type, value, toType);
        Assertions.assertEquals(expected, result);
    }

    private static Stream<Arguments> data() {
        var randomValue = Randoms.bigDecimal();

        return Stream.of(
                arguments("null", null, Object.class, null),
                arguments("random", randomValue, BigDecimal.class, randomValue),
                arguments("bigDecimal", BigDecimal.valueOf(2), BigDecimal.class, BigDecimal.valueOf(2)),
                arguments("double", BigDecimal.valueOf(3.5), Double.class, 3.5),
                arguments("string", BigDecimal.valueOf(7.11), String.class, "7.11"),
                arguments("jsonNode", BigDecimal.valueOf(13.17), JsonNode.class,
                        new DecimalNode(BigDecimal.valueOf(13.17)))
        );
    }
}
