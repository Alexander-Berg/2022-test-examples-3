package ru.yandex.market.jmf.attributes.test.decimal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.node.DecimalNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.crm.util.Randoms;

import static org.junit.jupiter.params.provider.Arguments.arguments;

public class DecimalDescriptorWrapTest extends AbstractDecimalDescriptorTest {

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void wrap(@SuppressWarnings("unused") String testName, Object rawValue, BigDecimal expected) {
        BigDecimal result = descriptor.wrap(attribute, type, rawValue);
        Assertions.assertEquals(expected, result);
    }

    @Test
    public void wrapDecimalNode() {
        BigDecimal result = descriptor.wrap(attribute, type, new DecimalNode(new BigDecimal(12.34)));
        Assertions.assertEquals(BigDecimal.valueOf(12.34), result.setScale(2, RoundingMode.HALF_UP));
    }

    private static Stream<Arguments> data() {
        var randomValue = Randoms.bigDecimal();

        return Stream.of(
                arguments("null", null, null),
                arguments("random", randomValue, randomValue),
                arguments("long", 7L, BigDecimal.valueOf(7L)),
                arguments("int", 13, BigDecimal.valueOf(13)),
                arguments("double", 17.23, BigDecimal.valueOf(17.23)),
                arguments("string", "19.41", BigDecimal.valueOf(19.41)),
                arguments("textNode_double", new TextNode("19.41"), BigDecimal.valueOf(19.41)),
                arguments("textNode_int", new TextNode("19"), BigDecimal.valueOf(19)),
                arguments("intNode", new IntNode(19), BigDecimal.valueOf(19))
        );
    }
}
