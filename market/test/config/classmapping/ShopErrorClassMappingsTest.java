package config.classmapping;

import org.junit.jupiter.api.Test;
import ru.yandex.market.checkout.pushapi.client.error.ErrorSubCode;
import ru.yandex.market.checkout.pushapi.client.error.ShopErrorException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.checkout.pushapi.client.util.test.XmlTestUtil.sameXmlAs;

public class ShopErrorClassMappingsTest extends BaseClassMappingsTest {

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
        sb.append("</error>");
        final ShopErrorException exception = deserialize(ShopErrorException.class, sb.toString());

        assertEquals(code, exception.getCode());
        assertEquals(message, exception.getMessage());
    }

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
        assertThat(serialize(new ShopErrorException(code, message, false)), is(sameXmlAs(expectedXml)));
    }
}
