package ru.yandex.market.logistic.gateway.service.executor.delivery.sync;

import java.util.Collections;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import ru.yandex.market.logistic.api.client.DeliveryServiceClient;
import ru.yandex.market.logistic.api.exceptions.RequestStateErrorException;
import ru.yandex.market.logistic.api.model.delivery.ResourceId;
import ru.yandex.market.logistic.api.model.delivery.response.GetOrdersDeliveryDateResponse;
import ru.yandex.market.logistic.api.model.delivery.response.entities.OrderDeliveryDate;
import ru.yandex.market.logistic.api.model.properties.PartnerProperties;
import ru.yandex.market.logistic.api.utils.DateTime;
import ru.yandex.market.logistic.api.utils.TimeInterval;
import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.exceptions.ServiceInteractionResponseFormatException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Интеграционный тест для {@link GetOrdersDeliveryDateRequestExecutor}.
 */
@DatabaseSetup("classpath:repository/state/partners_properties.xml")
public class GetOrdersDeliveryDateExecutorTest extends AbstractIntegrationTest {

    @MockBean
    private DeliveryServiceClient deliveryServiceClient;

    @Test
    public void executeSuccess() throws Exception {
        when(deliveryServiceClient.getOrdersDeliveryDate(anyListOf(ResourceId.class), any(PartnerProperties.class)))
            .thenReturn(getApiResponse());

        mockMvc.perform(post("/delivery/getOrdersDeliveryDate")
            .contentType(MediaType.APPLICATION_JSON)
            .content(getFileContent("fixtures/request/delivery/get_orders_delivery_date/delivery_get_orders_delivery_date.json")))
            .andExpect(status().is2xxSuccessful())
            .andExpect(content().json(getFileContent("fixtures/response/delivery/get_orders_delivery_date/delivery_get_orders_delivery_date.json")));

        verify(deliveryServiceClient).getOrdersDeliveryDate(anyListOf(ResourceId.class), any(PartnerProperties.class));
    }

    @Test
    public void executeResponseException() {
        when(deliveryServiceClient.getOrdersDeliveryDate(anyListOf(ResourceId.class), any(PartnerProperties.class)))
            .thenReturn(null);

        assertThatThrownBy(() -> mockMvc.perform(post("/delivery/getOrdersDeliveryDate")
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent("fixtures/request/delivery/get_orders_delivery_date/delivery_get_orders_delivery_date.json"))))
            .hasCause(new ServiceInteractionResponseFormatException("Failed to get GetOrdersDeliveryDateResponse"));

        verify(deliveryServiceClient).getOrdersDeliveryDate(anyListOf(ResourceId.class), any(PartnerProperties.class));
    }

    @Test
    public void executeResponseError() throws Exception {
        when(deliveryServiceClient.getOrdersDeliveryDate(anyListOf(ResourceId.class), any(PartnerProperties.class)))
            .thenThrow(new RequestStateErrorException("RequestStateErrorException"));

        mockMvc.perform(post("/delivery/getOrdersDeliveryDate")
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent("fixtures/request/delivery/get_orders_delivery_date/delivery_get_orders_delivery_date.json")))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("RequestStateErrorException"));

        verify(deliveryServiceClient).getOrdersDeliveryDate(anyListOf(ResourceId.class), any(PartnerProperties.class));
    }

    private GetOrdersDeliveryDateResponse getApiResponse() {
        return new GetOrdersDeliveryDateResponse(Collections.singletonList(new OrderDeliveryDate(
            new ResourceId.ResourceIdBuilder().setYandexId("12345").setPartnerId("ABC12345").build(),
            new DateTime("2017-09-09T10:16:00+03:00"),
            new TimeInterval("03:00:00+03:00/03:59:00+03:00"),
            "Message")));
    }
}
