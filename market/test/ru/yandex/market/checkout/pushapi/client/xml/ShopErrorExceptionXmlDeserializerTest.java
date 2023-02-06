package ru.yandex.market.checkout.pushapi.client.xml;

import org.junit.jupiter.api.Test;
import ru.yandex.market.checkout.pushapi.client.error.ErrorSubCode;
import ru.yandex.market.checkout.pushapi.client.error.ShopErrorException;
import ru.yandex.market.checkout.pushapi.client.util.test.XmlTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ShopErrorExceptionXmlDeserializerTest {

    private ShopErrorExceptionXmlDeserializer deserializer = new ShopErrorExceptionXmlDeserializer();

    @Test
    public void testDeserialize() throws Exception {
        assertDeserialize(ErrorSubCode.CANT_PARSE_RESPONSE);
        assertDeserialize(ErrorSubCode.CONNECTION_REFUSED);
        assertDeserialize(ErrorSubCode.CONNECTION_TIMED_OUT);
        assertDeserialize(ErrorSubCode.HTTP);
        assertDeserialize(ErrorSubCode.INVALID_DATA);
        assertDeserialize(ErrorSubCode.READ_TIMED_OUT);
        assertDeserialize(ErrorSubCode.READ_TIMED_OUT, "Blah");
    }

    private void assertDeserialize(ErrorSubCode code) throws Exception {
        assertDeserialize(code, "");
    }

    private void assertDeserialize(ErrorSubCode code, String message) throws Exception {
        if (message == null) {
            message = "";
        }

        final StringBuilder sb = new StringBuilder();
        sb.append("<error>");
        sb.append("    <code>" + code.toString() + "</code>");
        if (!message.isEmpty()) {
            sb.append("    <message>" + message + "</message>");
        }
        sb.append("<shop-admin>false</shop-admin>");
        sb.append("</error>");
        final ShopErrorException exception = XmlTestUtil.deserialize(
                deserializer,
                sb.toString()
        );

        assertEquals(code, exception.getCode());
        assertEquals(message, exception.getMessage());
    }
}
