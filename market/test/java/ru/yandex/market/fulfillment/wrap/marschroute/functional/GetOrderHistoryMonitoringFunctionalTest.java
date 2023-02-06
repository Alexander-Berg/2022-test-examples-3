package ru.yandex.market.fulfillment.wrap.marschroute.functional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentInteraction;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentUrl;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FunctionalTestScenarioBuilder;
import ru.yandex.market.fulfillment.wrap.marschroute.entity.Email;
import ru.yandex.market.fulfillment.wrap.marschroute.entity.OrderInfo;
import ru.yandex.market.fulfillment.wrap.marschroute.functional.configuration.IntegrationTest;
import ru.yandex.market.fulfillment.wrap.marschroute.model.base.DeliveryId;
import ru.yandex.market.fulfillment.wrap.marschroute.monitoring.email.EmailMonitoringProducer;
import ru.yandex.market.fulfillment.wrap.marschroute.service.OrderInfoService;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetOrderHistoryResponse;

import java.util.Arrays;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@MockBean(OrderInfoService.class)
@MockBean(EmailMonitoringProducer.class)
class GetOrderHistoryMonitoringFunctionalTest extends IntegrationTest {

    @Autowired
    private OrderInfoService orderInfoService;

    @Autowired
    private EmailMonitoringProducer producer;

    /**
     * Проверяет, что по итогу исполнения запроса было сгенерировано два эмейл сообщения.
     */
    @Test
    void testMonitoringIsFiredTwice() throws Exception {
        String orderId = "EXT43711873";
        OrderInfo orderInfo = new OrderInfo("1",orderId, DeliveryId.MARKET_DELIVERY);
        given(orderInfoService.getOrCompute(orderInfo.getOrderId())).willReturn(orderInfo);

        String wrapRequestPath = "functional/get_order_history/monitoring/wrapper_request.xml";
        String marschrouteResponsePath = "functional/get_order_history/monitoring/marschroute_response.json";
        String wrapResponsePath = "functional/get_order_history/monitoring/expected_wrapper_response.xml";

        FulfillmentInteraction marschrouteInteraction = FulfillmentInteraction.createMarschrouteInteraction()
                .setFulfillmentUrl(FulfillmentUrl.fulfillmentUrl(Arrays.asList("tracking", orderId), HttpMethod.GET))
                .setResponsePath(marschrouteResponsePath);

        FunctionalTestScenarioBuilder
                .start(GetOrderHistoryResponse.class)
                .sendRequestToWrap(
                        "/fulfillment/get-order-history",
                        HttpMethod.POST,
                        wrapRequestPath)
                .thenMockFulfillmentRequest(marschrouteInteraction)
                .andExpectWrapAnswerToBeEqualTo(wrapResponsePath)
                .build(mockMvc, restTemplate, fulfillmentMapper, apiUrl, apiKey)
                .start();

        verify(producer, times(2)).fire(any(Email.class));
    }
}
