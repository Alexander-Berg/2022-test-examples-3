package ru.yandex.market.checkout.pushapi.client.xml;

import org.junit.jupiter.api.Test;
import ru.yandex.market.checkout.pushapi.client.error.ErrorSubCode;
import ru.yandex.market.checkout.pushapi.client.error.ShopErrorException;
import ru.yandex.market.checkout.pushapi.client.util.test.XmlTestUtil;

public class ShopErrorExceptionXmlSerializerTest {

    private ShopErrorExceptionXmlSerializer serializer = new ShopErrorExceptionXmlSerializer();

    @Test
    public void testSerialize() throws Exception {
        assertSerialize(ErrorSubCode.CANT_PARSE_RESPONSE);
        assertSerialize(ErrorSubCode.CONNECTION_REFUSED);
        assertSerialize(ErrorSubCode.CONNECTION_TIMED_OUT);
        assertSerialize(ErrorSubCode.HTTP);
        assertSerialize(ErrorSubCode.INVALID_DATA);
        assertSerialize(ErrorSubCode.READ_TIMED_OUT);
        assertSerialize(ErrorSubCode.READ_TIMED_OUT, "Blah");
    }

    private void assertSerialize(ErrorSubCode code) throws Exception {
        assertSerialize(code, "");
    }

    private void assertSerialize(ErrorSubCode code, String message) throws Exception {
        final StringBuilder sb = new StringBuilder();
        sb.append("<error>");
        sb.append("    <code>" + code.toString() + "</code>");
        if (message != null && !message.isEmpty()) {
            sb.append("    <message>" + message + "</message>");
        }
        sb.append("</error>");
        final String expectedXml = sb.toString();
        XmlTestUtil.assertSerializeResultAndString(
                serializer,
                new ShopErrorException(code, message, false),
                expectedXml
        );
    }
}
