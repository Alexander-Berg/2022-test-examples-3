package ru.yandex.market.api.partner.controllers.delivery.shipment;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import ru.yandex.market.api.partner.controllers.order.shipment.Shipment;
import ru.yandex.market.api.partner.controllers.order.shipment.ShipmentDocument;
import ru.yandex.market.api.partner.controllers.order.shipment.ShipmentDocumentType;
import ru.yandex.market.api.partner.controllers.serialization.BaseOldSerializationTest;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelStatus;

/**
 * @author stani
 */

class DeliveryShipmentSerializationTest extends BaseOldSerializationTest {

    @Test
    void shouldSerializeShipment() {

        Shipment shipment = new Shipment();
        shipment.setWidth(1L);
        shipment.setHeight(23L);
        shipment.setDepth(1L);
        shipment.setWeight(2000L);
        shipment.setStatus(ParcelStatus.CREATED);

        ShipmentDocument doc = new ShipmentDocument();
        doc.setType(ShipmentDocumentType.LABEL);
        doc.setUrl("https://s3-mds-proxy.market.yandex.net/dsm/label/1393732-order-1495729749.pdf");
        shipment.setDocuments(Collections.singletonList(doc));

        getChecker().testSerialization(shipment,
                "{\n" +
                        "    \"depth\": 1, \n" +
                        "    \"height\": 23, \n" +
                        "    \"status\": \"CREATED\", \n" +
                        "    \"weight\": 2000, \n" +
                        "    \"width\": 1,\n" +
                        "    \"documents\":[{\"type\":\"LABEL\"," +
                        "    \"url\":\"https://s3-mds-proxy.market.yandex.net/dsm/label/1393732-order-1495729749.pdf\"}]" +
                        "}",
                "<shipment weight=\"2000\" height=\"23\" width=\"1\" depth=\"1\" status=\"CREATED\">" +
                        "<documents><document type=\"LABEL\" " +
                        "url=\"https://s3-mds-proxy.market.yandex.net/dsm/label/1393732-order-1495729749.pdf\"/>" +
                        "</documents></shipment>");
    }

}
