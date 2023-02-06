package ru.yandex.market.jmf.attributes.test.hyperlink;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.jmf.attributes.hyperlink.Hyperlink;
import ru.yandex.market.jmf.utils.Maps;

import static org.junit.jupiter.params.provider.Arguments.arguments;

public class HyperlinkDescriptorWrapTest extends AbstractHyperlinkDescriptorTest {

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void wrap(@SuppressWarnings("unused") String testName, Object rawValue, Hyperlink expected) {
        Hyperlink result = descriptor.wrap(attribute, type, rawValue);
        Assertions.assertEquals(expected, result);
    }

    private static Stream<Arguments> data() {
        var value = new Hyperlink("hrefA", "valueB");

        var node = objectMapper.createObjectNode();
        node.put("href", "hrefA");
        node.put("value", "valueB");

        return Stream.of(
                arguments("null", null, null),
                arguments("hyperLink", value, value),
                arguments("node", node, value),
                arguments("map", Maps.of("href", "hrefA", "value", "valueB"), value)
        );
    }
}
