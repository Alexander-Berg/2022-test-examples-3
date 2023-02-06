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
import ru.yandex.market.logistic.api.model.delivery.ExternalResourceId;
import ru.yandex.market.logistic.api.model.delivery.response.GetExternalOrderHistoryResponse;
import ru.yandex.market.logistic.api.model.delivery.response.entities.ExternalOrderStatus;
import ru.yandex.market.logistic.api.model.delivery.response.entities.ExternalOrderStatusHistory;
import ru.yandex.market.logistic.api.model.properties.PartnerProperties;
import ru.yandex.market.logistic.api.utils.DateTime;
import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.exceptions.ServiceInteractionResponseFormatException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Интеграционный тест для {@link GetExternalOrderHistoryRequestExecutor}.
 */
@DatabaseSetup("classpath:repository/state/partners_properties.xml")
public class GetExternalOrderHistoryRequestExecutorTest extends AbstractIntegrationTest {
    @MockBean
    private DeliveryServiceClient deliveryServiceClient;

    @Test
    public void executeSuccess() throws Exception {
        when(deliveryServiceClient.getExternalOrderHistory(any(ExternalResourceId.class), any(PartnerProperties.class)))
            .thenReturn(getApiResponse());

        mockMvc.perform(post("/delivery/getExternalOrderHistory")
            .contentType(MediaType.APPLICATION_JSON)
            .content(getFileContent("fixtures/request/delivery/get_external_order_history/delivery_get_external_order_history.json")))
            .andExpect(status().is2xxSuccessful())
            .andExpect(content().json(getFileContent("fixtures/response/delivery/get_external_order_history/delivery_get_external_order_history.json")));

        verify(deliveryServiceClient).getExternalOrderHistory(any(ExternalResourceId.class), any(PartnerProperties.class));
    }

    @Test
    public void executeSuccessWithEmptyHistory() throws Exception {
        GetExternalOrderHistoryResponse apiResponseWithEmptyHistory = getApiResponse(Collections.emptyList());

        when(deliveryServiceClient.getExternalOrderHistory(any(ExternalResourceId.class), any(PartnerProperties.class)))
            .thenReturn(apiResponseWithEmptyHistory);

        mockMvc.perform(post("/delivery/getExternalOrderHistory")
            .contentType(MediaType.APPLICATION_JSON)
            .content(getFileContent("fixtures/request/delivery/get_external_order_history/delivery_get_external_order_history.json")))
            .andExpect(status().is2xxSuccessful())
            .andExpect(content().json(getFileContent("fixtures/response/delivery/get_external_order_history/delivery_get_external_order_history_empty_history.json")));

        verify(deliveryServiceClient).getExternalOrderHistory(any(ExternalResourceId.class), any(PartnerProperties.class));
    }

    @Test
    public void executeResponseException() throws Exception {
        when(deliveryServiceClient.getExternalOrderHistory(any(ExternalResourceId.class), any(PartnerProperties.class)))
            .thenReturn(null);

        assertThatThrownBy(() -> mockMvc.perform(post("/delivery/getExternalOrderHistory")
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent("fixtures/request/delivery/get_external_order_history/delivery_get_external_order_history.json"))))
            .hasCause(new ServiceInteractionResponseFormatException("Failed to get GetExternalOrderHistoryResponse"));

        verify(deliveryServiceClient).getExternalOrderHistory(any(ExternalResourceId.class), any(PartnerProperties.class));
    }

    @Test
    public void executeResponseError() throws Exception {
        when(deliveryServiceClient.getExternalOrderHistory(any(ExternalResourceId.class), any(PartnerProperties.class)))
            .thenThrow(new RequestStateErrorException("RequestStateErrorException"));

        mockMvc.perform(post("/delivery/getExternalOrderHistory")
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent("fixtures/request/delivery/get_external_order_history/delivery_get_external_order_history.json")))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("RequestStateErrorException"));

        verify(deliveryServiceClient).getExternalOrderHistory(any(ExternalResourceId.class), any(PartnerProperties.class));
    }

    private GetExternalOrderHistoryResponse getApiResponse() {
        return getApiResponse(Collections.singletonList(new ExternalOrderStatus(
            OrderStatusType.ORDER_ARRIVED_TO_DELIVERY_SERVICE_WAREHOUSE_DS,
            new DateTime("2017-09-09T10:16:00+03:00"),
            "MyMessage"
            ))
        );
    }

    private GetExternalOrderHistoryResponse getApiResponse(List<ExternalOrderStatus> orderStatusList) {
        return new GetExternalOrderHistoryResponse(new ExternalOrderStatusHistory(orderStatusList,
            new ExternalResourceId("12345", "ABC12345", "DSID12345")
        ));
    }
}
