package ru.yandex.market.checkout.checkouter.events;

import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.order.getOrder.GetOrderWithBuyerPhonePartialTestBase;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

public class GetOrderHistoryWithBuyerPhoneTest extends GetOrderWithBuyerPhonePartialTestBase {

    @Override
    protected MockHttpServletRequestBuilder requestBuilder() {
        return get("/orders/{orderId}/events", order.getId());
    }

    @Override
    protected String getOrderRoot() {
        return "$.events[0].orderAfter";
    }
}

class GetOrderHistoryWithBuyerPhoneTestByOrderId extends GetOrderWithBuyerPhonePartialTestBase {

    @Override
    protected MockHttpServletRequestBuilder requestBuilder() {
        return get("/orders/events/by-order-id", order.getId())
                .param(CheckouterClientParams.ORDER_ID, order.getId().toString());
    }

    @Override
    protected String getOrderRoot() {
        return "$.events[0].orderAfter";
    }
}
