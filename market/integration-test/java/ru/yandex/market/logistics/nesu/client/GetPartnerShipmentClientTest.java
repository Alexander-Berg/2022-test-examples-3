package ru.yandex.market.logistics.nesu.client;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.nesu.client.enums.ShipmentType;
import ru.yandex.market.logistics.nesu.client.model.NamedEntity;
import ru.yandex.market.logistics.nesu.client.model.shipment.PartnerShipmentActions;
import ru.yandex.market.logistics.nesu.client.model.shipment.PartnerShipmentDto;
import ru.yandex.market.logistics.nesu.client.model.shipment.PartnerShipmentOrdersCount;
import ru.yandex.market.logistics.nesu.client.model.shipment.PartnerShipmentStatus;
import ru.yandex.market.logistics.nesu.client.model.shipment.PartnerShipmentStatusChange;
import ru.yandex.market.logistics.nesu.client.model.shipment.PartnerShipmentWarehouseDto;
import ru.yandex.market.logistics.nesu.client.model.shipment.WarehouseType;
import ru.yandex.market.logistics.util.client.exception.HttpTemplateException;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

@DisplayName("Получение данных отгрузки магазина в клиенте")
class GetPartnerShipmentClientTest extends AbstractClientTest {

    @Test
    @DisplayName("Успех")
    void success() {
        mock.expect(requestTo(startsWith(uri + "/internal/partner/shipments/300")))
            .andExpect(method(HttpMethod.GET))
            .andExpect(queryParam("userId", "100"))
            .andExpect(queryParam("shopId", "500"))
            .andRespond(
                withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(extractFileContent("response/shipment/get_response.json"))
            );

        softly.assertThat(client.getShipment(100, 500, 300))
            .isEqualTo(getShipmentDto());
    }

    @Test
    @DisplayName("Успех, список shopId")
    void successWithShopIds() {
        mock.expect(requestTo(startsWith(uri + "/internal/partner/shipments/300")))
            .andExpect(method(HttpMethod.GET))
            .andExpect(queryParam("userId", "100"))
            .andExpect(queryParam("shopIds", String.valueOf(100L), String.valueOf(200L), String.valueOf(300L)))
            .andRespond(
                withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(extractFileContent("response/shipment/get_response.json"))
            );

        softly.assertThat(client.getShipment(100, Set.of(100L, 200L, 300L), 300))
            .isEqualTo(getShipmentDto());
    }

    @Test
    @DisplayName("Bad Request")
    void error() {
        mock.expect(requestTo(startsWith(uri + "/internal/partner/shipments/300")))
            .andExpect(method(HttpMethod.GET))
            .andExpect(queryParam("userId", "100"))
            .andRespond(
                withStatus(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
            );
        Assertions.assertThrows(
            HttpTemplateException.class,
            () -> client.getShipment(100, Set.of(), 300));
    }

    @Nonnull
    private PartnerShipmentDto getShipmentDto() {
        return PartnerShipmentDto.builder()
            .id(300L)
            .planIntervalFrom(LocalDate.of(2021, 3, 4).atTime(10, 21))
            .planIntervalTo(LocalDate.of(2021, 4, 5).atTime(11, 22))
            .number("shipment-number")
            .shipmentType(ShipmentType.IMPORT)
            .warehouseFrom(
                PartnerShipmentWarehouseDto.builder()
                    .id(900L)
                    .name("Какой-то склад")
                    .address("Какой-то адрес")
                    .build()
            )
            .warehouseTo(
                PartnerShipmentWarehouseDto.builder()
                    .id(901L)
                    .name("Дропофф")
                    .address("Адрес дропоффа")
                    .type(WarehouseType.DROPOFF)
                    .build()
            )
            .partner(NamedEntity.builder().id(600L).name("Какой-то партнёр").build())
            .currentStatus(
                PartnerShipmentStatusChange.builder()
                    .code(PartnerShipmentStatus.INBOUND_ACCEPTANCE)
                    .description(PartnerShipmentStatus.INBOUND_ACCEPTANCE.getDescription())
                    .datetime(Instant.parse("2021-02-03T09:23:00Z"))
                    .build()
            )
            .ordersCount(
                PartnerShipmentOrdersCount.builder()
                    .draft(23)
                    .planned(22)
                    .fact(21)
                    .build()
            )
            .availableActions(
                PartnerShipmentActions.builder()
                    .confirm(true)
                    .downloadAct(true)
                    .uploadAct(false)
                    .downloadTransportationWaybill(false)
                    .build()
            )
            .orderIds(List.of(100L, 200L))
            .confirmedOrderIds(List.of(200L))
            .build();
    }
}
