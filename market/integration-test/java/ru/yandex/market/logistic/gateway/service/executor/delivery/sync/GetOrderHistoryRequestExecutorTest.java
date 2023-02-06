package ru.yandex.market.logistic.gateway.service.executor.delivery.sync;

import java.util.Collections;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import ru.yandex.market.logistic.api.client.DeliveryServiceClient;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.model.common.OrderStatusType;
import ru.yandex.market.logistic.api.model.delivery.ResourceId;
import ru.yandex.market.logistic.api.model.delivery.response.GetOrderHistoryResponse;
import ru.yandex.market.logistic.api.model.delivery.response.entities.OrderStatus;
import ru.yandex.market.logistic.api.model.delivery.response.entities.OrderStatusHistory;
import ru.yandex.market.logistic.api.model.properties.PartnerProperties;
import ru.yandex.market.logistic.api.utils.DateTime;
import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.exceptions.ServiceInteractionResponseFormatException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Интеграционный тест для {@link GetOrderHistoryRequestExecutor}.
 */
@DatabaseSetup("classpath:repository/state/partners_properties.xml")
public class GetOrderHistoryRequestExecutorTest extends AbstractIntegrationTest {

    @MockBean
    private DeliveryServiceClient deliveryServiceClient;

    @Test
    public void executeSuccess() throws Exception {
        when(deliveryServiceClient.getOrderHistory(any(ResourceId.class), any(PartnerProperties.class)))
            .thenReturn(getApiResponse());

        mockMvc.perform(post("/delivery/getOrderHistory")
            .contentType(MediaType.APPLICATION_JSON)
            .content(getFileContent("fixtures/request/delivery/get_order_history/delivery_get_order_history.json")))
            .andExpect(status().is2xxSuccessful())
            .andExpect(content().json(getFileContent("fixtures/response/delivery/get_order_history/delivery_get_order_history.json")));

        verify(deliveryServiceClient).getOrderHistory(any(ResourceId.class), any(PartnerProperties.class));
    }

    @Test
    public void executeSuccessWithEmptyHistory() throws Exception {
        GetOrderHistoryResponse apiResponseWithEmptyHistory = getApiResponse(Collections.emptyList());

        when(deliveryServiceClient.getOrderHistory(any(ResourceId.class), any(PartnerProperties.class)))
            .thenReturn(apiResponseWithEmptyHistory);

        mockMvc.perform(post("/delivery/getOrderHistory")
            .contentType(MediaType.APPLICATION_JSON)
            .content(getFileContent("fixtures/request/delivery/get_order_history/delivery_get_order_history.json")))
            .andExpect(status().is2xxSuccessful())
            .andExpect(content().json(getFileContent("fixtures/response/delivery/get_order_history/delivery_get_order_history_empty_history.json")));

        verify(deliveryServiceClient).getOrderHistory(any(ResourceId.class), any(PartnerProperties.class));
    }

    @Test
    public void executeResponseException() throws Exception {
        when(deliveryServiceClient.getOrderHistory(any(ResourceId.class), any(PartnerProperties.class)))
            .thenReturn(null);

        assertThatThrownBy(() -> mockMvc.perform(post("/delivery/getOrderHistory")
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent("fixtures/request/delivery/get_order_history/delivery_get_order_history.json"))))
            .hasCause(new ServiceInteractionResponseFormatException("Failed to get GetOrderHistoryResponse"));

        verify(deliveryServiceClient).getOrderHistory(any(ResourceId.class), any(PartnerProperties.class));
    }

    @Test
    public void executeResponseError() throws Exception {
        when(deliveryServiceClient.getOrderHistory(any(ResourceId.class), any(PartnerProperties.class)))
            .thenThrow(new RequestStateErrorException("RequestStateErrorException"));

        mockMvc.perform(post("/delivery/getOrderHistory")
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent("fixtures/request/delivery/get_order_history/delivery_get_order_history.json")))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("RequestStateErrorException"));

        verify(deliveryServiceClient).getOrderHistory(any(ResourceId.class), any(PartnerProperties.class));
    }

    private GetOrderHistoryResponse getApiResponse() {
        return getApiResponse(Collections.singletonList(new OrderStatus(
            OrderStatusType.ORDER_ARRIVED_TO_DELIVERY_SERVICE_WAREHOUSE_DS,
            new DateTime("2017-09-09T10:16:00+03:00"),
            "MyMessage",
            null)));
    }

    private GetOrderHistoryResponse getApiResponse(List<OrderStatus> orderStatusList) {
        return new GetOrderHistoryResponse(new OrderStatusHistory(orderStatusList,
            new ResourceId.ResourceIdBuilder().setYandexId("12345").setPartnerId("ABC12345").build()));
    }
}
