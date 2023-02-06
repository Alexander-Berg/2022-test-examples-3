package ru.yandex.market.logistics.nesu.api.order;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.common.services.auth.blackbox.BlackboxService;
import ru.yandex.market.logistics.lom.model.enums.tags.OrderTag;
import ru.yandex.market.logistics.nesu.api.auth.ApiAuthHolder;
import ru.yandex.market.logistics.nesu.base.order.AbstractSubmitOrderTest;
import ru.yandex.market.logistics.nesu.dto.order.OrdersSubmitRequest;
import ru.yandex.market.mbi.api.client.MbiApiClient;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Оформление заказа в Open API")
@DatabaseSetup("/repository/settings/delivery_type_service_settings.xml")
@DatabaseSetup("/controller/order/submit/submit_data.xml")
class ApiSubmitOrderTest extends AbstractSubmitOrderTest {

    @Autowired
    private BlackboxService blackboxService;
    @Autowired
    private MbiApiClient mbiApiClient;

    private ApiAuthHolder authHolder;

    @BeforeEach
    void setup() {
        authHolder = new ApiAuthHolder(blackboxService, objectMapper);
        authHolder.mockAccess(mbiApiClient, SHOP_ID);
    }

    @Test
    @DisplayName("Оформление заказа, принадлежащего недоступному магазину")
    void unavailableShop() throws Exception {
        long orderId = 102L;

        when(lomClient.getOrder(orderId, Set.of())).thenReturn(Optional.of(order(orderId, 11)));

        submitOrders(List.of(orderId))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/submit/submit_shop_not_available.json"));

        authHolder.verifyCheckAccess(mbiApiClient, 1L);
    }

    @Test
    @DisplayName("Оформление нескольких заказов, принадлежащих одному магазину, один запрос проверки прав на магазин")
    void sameShopSingleCall() throws Exception {
        mockSuccessfulOrder(105, 12);
        mockSuccessfulOrder(106, 13);
        mockGetSortingCenterWarehouse();
        mockGetSenderAvailableDeliveries();

        submitOrders(List.of(105L, 106L))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/submit/submit_success_multiple.json"));

        authHolder.verifyCheckAccess(mbiApiClient, SHOP_ID);
        verifyNoMoreInteractions(mbiApiClient);

        verify(registerOrderCapacityProducer).produceTask(105L);
        verify(registerOrderCapacityProducer).produceTask(106L);
    }

    @Nonnull
    @Override
    protected ResultActions submitOrders(OrdersSubmitRequest request) throws Exception {
        return mockMvc.perform(
            request(HttpMethod.POST, "/api/orders/submit", request)
                .headers(authHolder.authHeaders())
        );
    }

    @Nonnull
    @Override
    protected OrderTag getTag() {
        return OrderTag.COMMITTED_VIA_DAAS_OPEN_API;
    }
}
