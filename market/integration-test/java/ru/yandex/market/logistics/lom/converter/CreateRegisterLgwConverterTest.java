package ru.yandex.market.logistics.lom.converter;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.gateway.common.model.delivery.Register;
import ru.yandex.market.logistic.gateway.common.model.delivery.ResourceId;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.converter.lgw.CreateRegisterLgwConverter;
import ru.yandex.market.logistics.lom.converter.lgw.fulfillment.CreateRegisterLgwFulfillmentConverter;
import ru.yandex.market.logistics.lom.entity.Order;
import ru.yandex.market.logistics.lom.entity.Registry;
import ru.yandex.market.logistics.lom.entity.Shipment;
import ru.yandex.market.logistics.lom.entity.ShipmentApplication;
import ru.yandex.market.logistics.lom.entity.WaybillSegment;
import ru.yandex.market.logistics.lom.entity.embedded.Sender;
import ru.yandex.market.logistics.lom.entity.enums.ShipmentType;

@DisplayName("Конвертация реестров")
public class CreateRegisterLgwConverterTest extends AbstractContextualTest {

    @Autowired
    CreateRegisterLgwConverter createRegisterLgwConverter;

    @Autowired
    CreateRegisterLgwFulfillmentConverter createRegisterLgwFulfillmentConverter;

    @Autowired
    ShipmentConverter shipmentConverter;

    private List<Order> orders;
    private ShipmentApplication shipmentApplication;

    @BeforeEach
    void createApplication() {
        orders = List.of(
            new Order().setBarcode("LO1")
                .setSender(new Sender().setId(1L))
                .setWaybill(List.of(new WaybillSegment().setPartnerId(3L).setExternalId("10"))),
            new Order().setBarcode("LO2")
                .setSender(new Sender().setId(1L))
                .setWaybill(List.of(new WaybillSegment().setPartnerId(3L).setExternalId("20")))
        );

        shipmentApplication = new ShipmentApplication()
            .setShipment(
                new Shipment()
                    .setId(10L)
                    .setPartnerIdTo(3L)
                    .setShipmentType(ShipmentType.WITHDRAW)
                    .setShipmentDate(LocalDate.now())
                    .setRegistry(new Registry().setId(11L)))
            .setId(15L);
    }

    @Test
    @DisplayName("Конвертер реестров для СД")
    void toExternalTest() {
        Register register = createRegisterLgwConverter.toExternalDs(shipmentApplication, orders);
        softly.assertThat(register.getOrdersId()).containsExactlyInAnyOrder(
            ResourceId.builder().setYandexId("LO1").setPartnerId("10").build(),
            ResourceId.builder().setYandexId("LO2").setPartnerId("20").build()
        );

        softly.assertThat(register.getShipmentId().getYandexId()).isEqualTo(shipmentApplication.getId().toString());
    }

    @Test
    @DisplayName("Конвертер реестров для СЦ")
    void toExternalFFTest() {
        ru.yandex.market.logistic.gateway.common.model.fulfillment.Register register =
            createRegisterLgwFulfillmentConverter.toExternal(shipmentApplication, orders);

        softly.assertThat(register.getOrdersId()).containsExactlyInAnyOrder(
            ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId.builder()
                .setYandexId("LO1").setPartnerId("10").build(),
            ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId.builder()
                .setYandexId("LO2").setPartnerId("20").build()
        );

        softly.assertThat(register.getShipmentId().getYandexId()).isEqualTo(shipmentApplication.getId().toString());
    }
}
