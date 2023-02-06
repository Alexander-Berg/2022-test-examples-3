package ru.yandex.market.checkout.checkouter.pay.bnpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.OrderStatusHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.util.bnpl.BnplMockConfigurer;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.DELIVERY;
import static ru.yandex.market.checkout.helpers.BnplTestHelper.findRequestByFirstRelevantUrl;
import static ru.yandex.market.checkout.providers.BnplTestProvider.bnplAndCashbackParameters;

@DisplayName("Проверяем работу BnplRestAPI.")
public class BnplRestAPITest extends AbstractWebTestBase {

    @Autowired
    OrderPayHelper orderPayHelper;
    @Autowired
    private OrderStatusHelper orderStatusHelper;
    @Autowired
    private BnplMockConfigurer bnplMockConfigurer;

    @BeforeEach
    public void createOrder() throws Exception {
        bnplMockConfigurer.mockWholeBnpl();
        checkouterProperties.setEnableServicesPrepay(true);
        checkouterProperties.setEnableBnpl(true);
    }

    @Test
    @DisplayName("Проверяем что при смене статуса доставки в запросе к Сплиту присутствует параметр длинного сплита.")
    public void existQueryParametersForPaidSplitInRequest() {
        Parameters parameters = bnplAndCashbackParameters();
        Order order = orderCreateHelper.createOrder(parameters);
        orderPayHelper.payWithRealResponse(order);
        order = orderService.getOrder(order.getId());
        orderStatusHelper.proceedOrderToStatus(order, DELIVERY);
        order = orderService.getOrder(order.getId());
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);

        var events = bnplMockConfigurer.servedEvents();
        String url = "/yandex/order/info?external_id=" + order.getPaymentId() + "&features=paid_split";
        var event = findRequestByFirstRelevantUrl(events, url);
        assertNotNull(event);
    }
}
