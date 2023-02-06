package ru.yandex.market.logistics.nesu.client;

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
import ru.yandex.market.logistics.nesu.client.model.page.Page;
import ru.yandex.market.logistics.nesu.client.model.page.PageRequest;
import ru.yandex.market.logistics.nesu.client.model.shipment.PartnerShipmentFilter;
import ru.yandex.market.logistics.nesu.client.model.shipment.PartnerShipmentOrdersCount;
import ru.yandex.market.logistics.nesu.client.model.shipment.PartnerShipmentSearchDto;
import ru.yandex.market.logistics.nesu.client.model.shipment.PartnerShipmentStatus;
import ru.yandex.market.logistics.util.client.exception.HttpTemplateException;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonRequestContent;

@DisplayName("Поиск отгрузок магазина в клиенте")
class SearchPartnerShipmentsClientTest extends AbstractClientTest {

    @Test
    @DisplayName("Успех")
    void success() {
        mock.expect(requestTo(startsWith(uri + "/internal/partner/shipments/search")))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(queryParam("userId", "100"))
            .andExpect(queryParam("shopId", "500"))
            .andExpect(queryParam("page", "0"))
            .andExpect(queryParam("size", "10"))
            .andExpect(jsonRequestContent("request/shipments/search.json"))
            .andRespond(
                withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(extractFileContent("response/shipment/search_response.json"))
            );

        Page<PartnerShipmentSearchDto> result = client.searchPartnerShipments(
            100,
            500,
            PartnerShipmentFilter.builder()
                .dateFrom(LocalDate.of(2021, 2, 21))
                .dateTo(LocalDate.of(2021, 3, 21))
                .statuses(List.of(
                    PartnerShipmentStatus.INBOUND_SHIPPED,
                    PartnerShipmentStatus.OUTBOUND_CREATED
                ))
                .orderIds(List.of(3000L, 2000L, 1000L))
                .number("654")
                .warehousesFrom(List.of(200L, 210L))
                .warehousesTo(List.of(300L, 310L))
                .withOrders(true)
                .sortedByStatus(true)
                .build(),
            new PageRequest(0, 10)
        );
        softly.assertThat(result.getData())
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactly(shipmentSearchDto());
        softly.assertThat(result.getPage()).isEqualTo(0);
        softly.assertThat(result.getSize()).isEqualTo(10);
        softly.assertThat(result.getTotalElements()).isEqualTo(100);
        softly.assertThat(result.getTotalPages()).isEqualTo(10);
    }

    @Test
    @DisplayName("Успех, список shopId")
    void successWithShopIds() {
        mock.expect(requestTo(startsWith(uri + "/internal/partner/shipments/search")))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(queryParam("userId", "100"))
            .andExpect(queryParam("shopIds",  String.valueOf(100L), String.valueOf(200L), String.valueOf(300L)))
            .andExpect(queryParam("page", "0"))
            .andExpect(queryParam("size", "10"))
            .andExpect(jsonRequestContent("request/shipments/search.json"))
            .andRespond(
                withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(extractFileContent("response/shipment/search_response.json"))
            );

        Page<PartnerShipmentSearchDto> result = client.searchPartnerShipments(
            100,
            Set.of(100L, 200L, 300L),
            PartnerShipmentFilter.builder()
                .dateFrom(LocalDate.of(2021, 2, 21))
                .dateTo(LocalDate.of(2021, 3, 21))
                .statuses(List.of(
                    PartnerShipmentStatus.INBOUND_SHIPPED,
                    PartnerShipmentStatus.OUTBOUND_CREATED
                ))
                .orderIds(List.of(3000L, 2000L, 1000L))
                .number("654")
                .warehousesFrom(List.of(200L, 210L))
                .warehousesTo(List.of(300L, 310L))
                .withOrders(true)
                .sortedByStatus(true)
                .build(),
            new PageRequest(0, 10)
        );
        softly.assertThat(result.getData())
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactly(shipmentSearchDto());
        softly.assertThat(result.getPage()).isEqualTo(0);
        softly.assertThat(result.getSize()).isEqualTo(10);
        softly.assertThat(result.getTotalElements()).isEqualTo(100);
        softly.assertThat(result.getTotalPages()).isEqualTo(10);
    }

    @Test
    @DisplayName("Bad Request")
    void error() {
        mock.expect(requestTo(startsWith(uri + "/internal/partner/shipments/search")))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(queryParam("userId", "100"))
            .andRespond(
                withStatus(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
            );
        Assertions.assertThrows(
            HttpTemplateException.class,
            () -> client.searchPartnerShipments(
                100,
                Set.of(),
                PartnerShipmentFilter.builder()
                    .dateFrom(LocalDate.of(2021, 2, 21))
                    .dateTo(LocalDate.of(2021, 3, 21))
                    .statuses(List.of(
                        PartnerShipmentStatus.INBOUND_SHIPPED,
                        PartnerShipmentStatus.OUTBOUND_CREATED
                    ))
                    .orderIds(List.of(3000L, 2000L, 1000L))
                    .number("654")
                    .warehousesFrom(List.of(200L, 210L))
                    .warehousesTo(List.of(300L, 310L))
                    .build(),
                new PageRequest(0, 10)
            ));
    }

    @Nonnull
    private PartnerShipmentSearchDto shipmentSearchDto() {
        return PartnerShipmentSearchDto.builder()
            .id(5000L)
            .planIntervalFrom(LocalDate.of(2021, 2, 25).atTime(10, 20))
            .planIntervalTo(LocalDate.of(2021, 2, 26).atTime(11, 22))
            .number("shipment-number")
            .shipmentType(ShipmentType.WITHDRAW)
            .status(PartnerShipmentStatus.OUTBOUND_CREATED)
            .statusDescription("status-description")
            .partner(NamedEntity.builder().id(200L).name("shipment-partner").build())
            .ordersCount(
                PartnerShipmentOrdersCount.builder()
                    .draft(100)
                    .planned(200)
                    .fact(300)
                    .build()
            )
            .build();
    }
}
