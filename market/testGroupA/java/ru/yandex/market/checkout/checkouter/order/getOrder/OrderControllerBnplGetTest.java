package ru.yandex.market.checkout.checkouter.order.getOrder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.util.bnpl.BnplMockConfigurer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.CLIENT_ROLE;
import static ru.yandex.market.checkout.providers.BnplTestProvider.defaultBnplParameters;

public class OrderControllerBnplGetTest extends AbstractWebTestBase {

    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private BnplMockConfigurer bnplMockConfigurer;

    @BeforeEach
    public void mockBnpl() {
        checkouterProperties.setEnableBnpl(true);
        bnplMockConfigurer.mockWholeBnpl();
        reportConfigurer.mockDefaultCreditInfo();
    }

    @Test
    public void testBnplInGetOrder() throws Exception {
        Parameters parameters = defaultBnplParameters();
        Order order = orderCreateHelper.createOrder(parameters);
        orderPayHelper.payForOrder(order);

        order = orderService.getOrder(order.getId(), ClientInfo.SYSTEM);

        mockMvc.perform(get("/orders/{orderId}", order.getId())
                .param(CLIENT_ROLE, "USER")
                .param(CheckouterClientParams.CLIENT_ID, String.valueOf(BuyerProvider.UID))
                .param(CheckouterClientParams.UID, String.valueOf(BuyerProvider.UID))
                .param(CheckouterClientParams.SHOW_RETURN_STATUSES, "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].bnpl").value(true))
                .andExpect(jsonPath("$.bnpl").value(true));
    }
}
