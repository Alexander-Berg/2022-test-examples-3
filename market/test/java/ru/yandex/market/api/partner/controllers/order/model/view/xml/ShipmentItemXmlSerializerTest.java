package ru.yandex.market.api.partner.controllers.order.model.view.xml;

import java.io.StringWriter;

import org.custommonkey.xmlunit.XMLAssert;
import org.junit.jupiter.api.Test;

import ru.yandex.market.api.partner.controllers.order.shipment.ShipmentItem;
import ru.yandex.market.checkout.common.xml.SimpleXmlWriter;

/**
 * @author apershukov
 */
class ShipmentItemXmlSerializerTest {

    @Test
    void testSerialize() throws Exception {
        StringWriter writer = new StringWriter();
        new ShipmentItemXmlSerializer().serializeXml(
                new ShipmentItem(10, 20),
                new SimpleXmlWriter(writer)
        );

        XMLAssert.assertXMLEqual(
                "<item id=\"10\" count=\"20\"/>",
                writer.toString()
        );
    }
}
