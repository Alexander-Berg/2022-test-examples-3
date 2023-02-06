package ru.yandex.market.logistics.lom.client;

import java.util.List;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;

import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.enums.OptionalOrderPart;
import ru.yandex.market.logistics.lom.model.page.PageResult;
import ru.yandex.market.logistics.lom.model.search.Pageable;

import static ru.yandex.market.logistics.lom.model.dto.DtoBuilderFactory.orderSearchDto;
import static ru.yandex.market.logistics.lom.model.dto.DtoBuilderFactory.orderSearchFilterBuilder;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.queryParam;

class OrderSearchClientTest extends AbstractClientTest {

    @Autowired
    private LomClient lomClient;

    @Test
    @DisplayName("Поиск заказов")
    void searchOrders() {
        prepareMockRequest(HttpMethod.PUT, "/orders/search", "request/order/search.json", "response/order/search.json")
            .andExpect(queryParam("size", "10"))
            .andExpect(queryParam("page", "0"));
        PageResult<OrderDto> result = lomClient.searchOrders(
            orderSearchFilterBuilder().build(),
            new Pageable(0, 10, null)
        );

        softly.assertThat(result).isEqualToComparingFieldByFieldRecursively(expectedPageResult());
    }

    @Test
    @DisplayName("Поиск заказов с опциональными частями заказа")
    void searchOrdersWithOptionalParts() {
        prepareMockRequest(HttpMethod.PUT, "/orders/search", "request/order/search.json", "response/order/search.json")
            .andExpect(queryParam("size", "10"))
            .andExpect(queryParam("page", "0"))
            .andExpect(queryParam(
                "optionalParts",
                "CHANGE_REQUESTS",
                "CANCELLATION_REQUESTS",
                "UPDATE_RECIPIENT_ENABLED",
                "GLOBAL_STATUSES_HISTORY",
                "RETURNS_IDS"
            ));
        PageResult<OrderDto> result = lomClient.searchOrders(
            orderSearchFilterBuilder().build(),
            OptionalOrderPart.ALL,
            new Pageable(0, 10, null)
        );

        softly.assertThat(result).usingRecursiveComparison().isEqualTo(expectedPageResult());
    }

    @Test
    @DisplayName("Поиск заказов без параметров страниц")
    void searchUnpaged() {
        prepareMockRequest(
            HttpMethod.PUT,
            "/orders/search",
            "request/order/search.json",
            "response/order/search_unpaged.json"
        )
            .andExpect(queryParam("unpaged", "true"));

        PageResult<OrderDto> result = lomClient.searchOrders(
            orderSearchFilterBuilder().build(),
            Pageable.unpaged()
        );

        softly.assertThat(result).isEqualToComparingFieldByFieldRecursively(expectedPageResult().setSize(0));
    }

    @Nonnull
    private PageResult<OrderDto> expectedPageResult() {
        return new PageResult<OrderDto>()
            .setData(List.of(orderSearchDto()))
            .setTotalPages(1)
            .setPageNumber(0)
            .setTotalElements(1)
            .setSize(10);
    }
}
