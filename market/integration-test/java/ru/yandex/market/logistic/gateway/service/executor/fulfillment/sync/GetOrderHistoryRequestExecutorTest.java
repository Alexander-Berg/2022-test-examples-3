package ru.yandex.market.logistic.gateway.service.executor.fulfillment.sync;

import java.util.Collections;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import ru.yandex.market.logistic.api.client.FulfillmentClient;
import ru.yandex.market.logistic.api.model.common.OrderStatusType;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetOrderHistoryResponse;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.OrderStatus;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.OrderStatusHistory;
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
    private FulfillmentClient fulfillmentClient;

    @Test
    public void executeSuccess() throws Exception {
        when(fulfillmentClient.getOrderHistory(any(ResourceId.class), any(PartnerProperties.class)))
            .thenReturn(getApiResponse());

        mockMvc.perform(post("/fulfillment/getOrderHistory")
            .contentType(MediaType.APPLICATION_JSON)
            .content(getFileContent("fixtures/request/fulfillment/get_order_history/fulfillment_get_order_history.json")))
            .andExpect(status().is2xxSuccessful())
            .andExpect(content().json(getFileContent("fixtures/response/fulfillment/get_order_history/fulfillment_get_order_history.json")));

        verify(fulfillmentClient).getOrderHistory(any(ResourceId.class), any(PartnerProperties.class));
    }


    @Test
    public void executeSuccessWithEmptyHistory() throws Exception {
        GetOrderHistoryResponse apiResponseWithEmptyHistory = getApiResponse(Collections.emptyList());

        when(fulfillmentClient.getOrderHistory(any(ResourceId.class), any(PartnerProperties.class)))
            .thenReturn(apiResponseWithEmptyHistory);

        mockMvc.perform(post("/fulfillment/getOrderHistory")
            .contentType(MediaType.APPLICATION_JSON)
            .content(getFileContent("fixtures/request/fulfillment/get_order_history/fulfillment_get_order_history.json")))
            .andExpect(status().is2xxSuccessful())
            .andExpect(content().json(getFileContent("fixtures/response/fulfillment/get_order_history/fulfillment_get_order_history_empty_history.json")));

        verify(fulfillmentClient).getOrderHistory(any(ResourceId.class), any(PartnerProperties.class));
    }

    @Test
    public void executeResponseException() throws Exception {
        when(fulfillmentClient.getOrderHistory(any(ResourceId.class), any(PartnerProperties.class)))
            .thenReturn(null);

        assertThatThrownBy(() -> mockMvc.perform(post("/fulfillment/getOrderHistory")
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent("fixtures/request/fulfillment/get_order_history/fulfillment_get_order_history.json"))))
            .hasCause(new ServiceInteractionResponseFormatException("Failed to get GetOrderHistoryResponse"));

        verify(fulfillmentClient).getOrderHistory(any(ResourceId.class), any(PartnerProperties.class));
    }

    private GetOrderHistoryResponse getApiResponse() {
        return getApiResponse(Collections.singletonList(new OrderStatus(
            OrderStatusType.ORDER_CREATED_BUT_NOT_APPROVED_FF,
            new DateTime("2017-09-09T10:16:00+03:00"),
            "MyMessage")));
    }

    private GetOrderHistoryResponse getApiResponse(List<OrderStatus> orderStatusList) {
        return new GetOrderHistoryResponse(new OrderStatusHistory(
            orderStatusList,
            new ResourceId("12345", "ABC12345"))
        );
    }
}
