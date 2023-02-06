package ru.yandex.market.jmf.attributes.test.hyperlink;

import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.jmf.attributes.hyperlink.Hyperlink;

import static org.junit.jupiter.params.provider.Arguments.arguments;

public class HyperlinkDescriptorUnwrapTest extends AbstractHyperlinkDescriptorTest {

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void unwrap(@SuppressWarnings("unused") String testName, Hyperlink value, Class<?> toType, Object expected) {
        Object result = descriptor.unwrap(attribute, type, value, toType);
        Assertions.assertEquals(expected, result);
    }

    private static Stream<Arguments> data() {
        var value = new Hyperlink("hrefA", "valueB");

        var node = objectMapper.createObjectNode();
        node.put("href", "hrefA");
        node.put("value", "valueB");

        return Stream.of(
                arguments("null", null, Object.class, null),
                arguments("hyperlink", value, Hyperlink.class, value),
                arguments("string", value, String.class, "((valueB hrefA))"),
                arguments("jsonNode", value, JsonNode.class, node)
        );
    }
}
