package ru.yandex.market.logistics.lom.client;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.ResponseActions;

import ru.yandex.market.logistics.lom.model.dto.ShipmentSearchDto;
import ru.yandex.market.logistics.lom.model.enums.OptionalOrderPart;
import ru.yandex.market.logistics.lom.model.page.PageResult;
import ru.yandex.market.logistics.lom.model.search.Pageable;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static ru.yandex.market.logistics.lom.model.dto.DtoBuilderFactory.shipmentSearchDtoBuilder;
import static ru.yandex.market.logistics.lom.model.dto.DtoBuilderFactory.shipmentSearchFilterBuilder;

class ShipmentSearchClientTest extends AbstractClientTest {

    @Autowired
    private LomClient lomClient;

    @Test
    @DisplayName("Поиск отгрузок")
    void searchShipment() {
        commonPrepareMock();

        PageResult<ShipmentSearchDto> result = lomClient.searchShipments(
            shipmentSearchFilterBuilder().build(),
            new Pageable(0, 10, null)
        );

        commonAssert(result);
    }

    @Test
    @DisplayName("Поиск заказов с опциональными частями заказа")
    void searchShipmentWithOrderOptionalParts() {
        commonPrepareMock()
            .andExpect(queryParam(
                "optionalOrderParts",
                "CHANGE_REQUESTS", "CANCELLATION_REQUESTS"
            ));

        PageResult<ShipmentSearchDto> result = lomClient.searchShipments(
            shipmentSearchFilterBuilder().build(),
            OptionalOrderPart.ALL,
            new Pageable(0, 10, null)
        );

        commonAssert(result);
    }

    private ResponseActions commonPrepareMock() {
        return prepareMockRequest(HttpMethod.PUT, "/shipments/search", "shipment/search_shipment.json")
            .andExpect(queryParam("size", "10"))
            .andExpect(queryParam("page", "0"));
    }

    private void commonAssert(PageResult<ShipmentSearchDto> result) {
        PageResult<ShipmentSearchDto> expected = new PageResult<ShipmentSearchDto>()
            .setData(List.of(shipmentSearchDtoBuilder().build()))
            .setTotalPages(1)
            .setPageNumber(0)
            .setTotalElements(1)
            .setSize(10);
        softly.assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }
}
