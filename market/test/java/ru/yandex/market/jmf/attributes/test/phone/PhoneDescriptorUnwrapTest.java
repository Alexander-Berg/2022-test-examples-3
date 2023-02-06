package ru.yandex.market.jmf.attributes.test.phone;

import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.crm.domain.Phone;
import ru.yandex.market.jmf.attributes.phone.PhoneData;

import static org.junit.jupiter.params.provider.Arguments.arguments;

public class PhoneDescriptorUnwrapTest extends AbstractPhoneDescriptorTest {

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void unwrap(@SuppressWarnings("unused") String testName, Phone value, Class<?> toType, Object expected) {
        Object result = descriptor.unwrap(attribute, type, value, toType);
        Assertions.assertEquals(expected, result);
    }

    private static Stream<Arguments> data() {
        var value = Phone.fromRaw("+71234567890 доб. 123");

        var node = objectMapper.createObjectNode();
        node.put("raw", "+71234567890 доб. 123");
        node.put("normalized", "+71234567890#123");

        return Stream.of(
                arguments("null", null, Object.class, null),
                arguments("phone", value, Phone.class, value),
                arguments("phoneData", value, PhoneData.class, new PhoneData("+71234567890 доб. 123",
                        "+71234567890#123", "+71234567890")),
                arguments("string", value, String.class, "+71234567890#123"),
                arguments("jsonNode", value, JsonNode.class, node)
        );
    }
}
