package ru.yandex.market.checkout.checkouter.order.getOrder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderEditRequest;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.edit.OrderEditService;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.order.Color.ALL_COLORS;
import static ru.yandex.market.checkout.checkouter.order.changerequest.CancellationRequest.builder;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;

public class GetOrdersHasCancellationRequestTest extends AbstractWebTestBase {

    @Autowired
    protected YandexMarketDeliveryHelper yandexMarketDeliveryHelper;
    @Autowired
    protected OrderEditService orderEditService;

    @DisplayName("Проверяем поведение флага hasCancellationRequest")
    @Test
    public void hasCancellationRequestTest() throws Exception {
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .buildParameters();
        parameters.getBuyer().setUid(777L);
        parameters.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        Order order = orderCreateHelper.createOrder(parameters);
        orderUpdateService.updateOrderStatus(order.getId(), OrderStatus.PROCESSING, OrderSubstatus.READY_TO_SHIP);
        Long uid = order.getBuyer().getUid();
        String orderStatusName = order.getStatus().name();
        OrderEditRequest orderEditRequest = new OrderEditRequest();
        orderEditRequest.setCancellationRequest(builder()
                .substatus(OrderSubstatus.USER_CHANGED_MIND)
                .notes("")
                .build());
        ClientInfo clientInfo = order.getUserClientInfo();

        MockHttpServletRequestBuilder requestTemplate = get("/orders/by-uid/{uid}/recent", uid)
                .param(CheckouterClientParams.RGB, order.getRgb().name())
                .param(CheckouterClientParams.STATUS, orderStatusName);
        mockMvc.perform(requestTemplate)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[0].id").value(order.getId()));
        mockMvc.perform(
                requestTemplate.param(CheckouterClientParams.HAS_CANCELLATION_REQUEST, Boolean.TRUE.toString())
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[*].id").value(not(contains(order.getId()))));

        orderEditService.editOrder(order.getId(), clientInfo, ALL_COLORS, orderEditRequest);

        requestTemplate = get("/orders/by-uid/{uid}/recent", uid)
                .param(CheckouterClientParams.RGB, order.getRgb().name())
                .param(CheckouterClientParams.STATUS, orderStatusName);
        mockMvc.perform(requestTemplate)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[*].id").value(not(contains(order.getId()))));
        mockMvc.perform(
                requestTemplate.param(CheckouterClientParams.HAS_CANCELLATION_REQUEST, Boolean.TRUE.toString())
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[0].id").value(order.getId()));
    }
}
