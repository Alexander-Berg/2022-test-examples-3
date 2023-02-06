package ru.yandex.market.checkout.checkouter.pay;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.common.web.CheckoutHttpParameters;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.util.bnpl.BnplMockConfigurer;

import static ru.yandex.market.checkout.providers.BnplTestProvider.defaultBnplParameters;

/**
 * @author : poluektov
 * date: 2021-07-21.
 */
public class BnplTvmTest extends AbstractWebTestBase {

    @Autowired
    private BnplMockConfigurer bnplMockConfigurer;
    @Autowired
    private OrderPayHelper orderPayHelper;

    @BeforeEach
    public void mockBnpl() {
        checkouterProperties.setEnableBnpl(true);
        bnplMockConfigurer.mockWholeBnpl();
    }

    @Test
    public void checkTvmHeader() {
        Parameters parameters = defaultBnplParameters();
        Order order = orderCreateHelper.createOrder(parameters);

        orderPayHelper.payForOrder(order);
        bnplMockConfigurer.servedEvents().forEach(e ->
                Assertions.assertNotNull(e.getRequest().getHeader(CheckoutHttpParameters.SERVICE_TICKET_HEADER)));
    }
}
