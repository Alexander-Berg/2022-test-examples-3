package ru.yandex.market.partner.notification.service.mustache;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.junit.jupiter.api.Test;

import ru.yandex.market.partner.notification.service.mustache.model.TransportType;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParametersXmlReaderTest {

    private final ParametersXmlReader reader = new ParametersXmlReader();

    @Test
    void shouldParseToMap() throws IOException, JDOMException {
        var input = "<data>" +
                "<notification-supplier-info>" +
                "<crossdock>false</crossdock>" +
                "<campaign-id>333444555</campaign-id>" +
                "<name>supplier_name</name>" +
                "<fulfillment>true</fulfillment>" +
                "<id>461951</id>" +
                "<click-and-collect>false</click-and-collect>" +
                "<dropship>false</dropship>" +
                "</notification-supplier-info>" +
                "<shop-name>The Shop</shop-name>" +
                "<mbi-transport-type>5</mbi-transport-type>" +
                "</data>";
        SAXBuilder builder = new SAXBuilder();
        Element body = builder.build(new StringReader(input)).getRootElement();

        var actual = reader.read(body);
        assertEquals(
                Map.of("notification-supplier-info",
                        Map.of("crossdock", "false",
                                "campaign-id", "333444555",
                                "name", "supplier_name",
                                "fulfillment", "true",
                                "id", "461951",
                                "click-and-collect", "false",
                                "dropship", "false"),
                        "shop-name", "The Shop",
                        "mbi-transport-type", "5",
                        // common variables
                        "campaignId", "333444555",
                        "shopName", "supplier_name",
                        "shopId", "461951",
                        "isTelegram", "true"),
                actual
        );
    }

    @Test
    void testTransportTypesVariables() {
        assertEquals(TransportType.values().length, ParametersXmlReader.PARAM_NAME_TO_TRANSPORT.size());
    }

}
