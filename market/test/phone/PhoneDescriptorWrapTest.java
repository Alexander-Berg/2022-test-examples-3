package ru.yandex.market.jmf.attributes.test.phone;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.crm.domain.Phone;
import ru.yandex.market.jmf.attributes.phone.PhoneData;
import ru.yandex.market.jmf.utils.Maps;

import static org.junit.jupiter.params.provider.Arguments.arguments;

public class PhoneDescriptorWrapTest extends AbstractPhoneDescriptorTest {

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void wrap(@SuppressWarnings("unused") String testName, Object rawValue, Phone expected) {
        Phone result = descriptor.wrap(attribute, type, rawValue);
        Assertions.assertEquals(expected, result);
    }

    private static Stream<Arguments> data() {
        var value = Phone.fromRaw("+71234567890 доб. 123");

        var node1 = objectMapper.createObjectNode();
        node1.put("normalized", "+71234567890#123");

        var node2 = objectMapper.createObjectNode();
        node2.put("raw", "+71234567890 доб. 123");

        return Stream.of(
                arguments("null", null, Phone.empty()),
                arguments("phone", value, value),
                arguments("phoneData", new PhoneData("+71234567890 доб. 123", "+71234567890#123", "+71234567890"),
                        value),
                arguments("string", "+71234567890 доб. 123", value),
                arguments("rawMap", Maps.of("raw", "+71234567890 доб. 123"), value),
                arguments("normalizedMap", Maps.of("main", "+71234567890", "ext", "123"),
                        Phone.fromNormalized("+71234567890#123")),
                arguments("normalizedNode", node1, Phone.fromNormalized("+71234567890#123")),
                arguments("rawNode", node2, value)
        );
    }
}
