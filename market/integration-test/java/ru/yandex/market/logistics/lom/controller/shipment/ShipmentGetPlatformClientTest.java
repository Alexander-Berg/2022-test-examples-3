package ru.yandex.market.logistics.lom.controller.shipment;

import java.util.Optional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.entity.enums.PlatformClient;
import ru.yandex.market.logistics.lom.service.shipment.ShipmentService;

class ShipmentGetPlatformClientTest extends AbstractContextualTest {

    @Autowired
    ShipmentService shipmentService;

    @Test
    @DatabaseSetup("/controller/shipment/before/shipment_orders_platforms.xml")
    void getPlatformClientOk() {
        softly.assertThat(shipmentService.getPlatformClientByOrdersInShipment(1L))
            .isEqualTo(Optional.of(PlatformClient.YANDEX_DELIVERY));
        softly.assertThat(shipmentService.getPlatformClientByOrdersInShipment(2L))
            .isEqualTo(Optional.of(PlatformClient.BERU));
        softly.assertThat(shipmentService.getPlatformClientByOrdersInShipment(3L))
            .isEqualTo(Optional.empty());
    }
}
