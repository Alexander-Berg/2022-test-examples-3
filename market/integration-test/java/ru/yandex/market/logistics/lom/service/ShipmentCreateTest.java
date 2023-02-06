package ru.yandex.market.logistics.lom.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.model.dto.ShipmentApplicationDto;
import ru.yandex.market.logistics.lom.model.dto.ShipmentDto;
import ru.yandex.market.logistics.lom.service.shipment.ShipmentService;

public class ShipmentCreateTest extends AbstractContextualTest {
    @Autowired
    private ShipmentService shipmentService;

    @Test
    void createShipmentWithoutAllRequiredFields() {
        ShipmentApplicationDto emptyShipmentApplication = ShipmentApplicationDto.builder()
            .shipment(ShipmentDto.builder().build())
            .build();

        softly.assertThatThrownBy(
            () -> shipmentService.createShipmentApplication(emptyShipmentApplication)
        )
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(
                "Error creating shipment because not all required field are provided: " +
                    "ShipmentApplicationDto(id=null, shipment=ShipmentDto(id=null, marketIdFrom=null, " +
                    "marketIdTo=null, partnerIdTo=null, shipmentType=null, shipmentDate=null, warehouseFrom=null, " +
                    "warehouseTo=null, fake=null), requisiteId=null, externalId=null, " +
                    "interval=null, status=null, " +
                    "korobyteDto=null, courier=null, cost=null, comment=null, balanceContractId=null, " +
                    "balancePersonId=null, locationZoneId=null)"
            );
    }
}
