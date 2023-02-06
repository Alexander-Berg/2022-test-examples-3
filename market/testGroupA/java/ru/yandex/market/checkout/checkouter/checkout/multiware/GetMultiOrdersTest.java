package ru.yandex.market.checkout.checkouter.checkout.multiware;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;
import ru.yandex.market.checkout.checkouter.order.PagedOrders;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

public class GetMultiOrdersTest extends AbstractWebTestBase {

    private MultiOrder multiOrder;

    @BeforeEach
    public void createMultiOrder() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.addOrder(BlueParametersProvider.defaultBlueOrderParameters());
        multiOrder = orderCreateHelper.createMultiOrder(parameters);
        orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        assertThat(multiOrder, notNullValue());
    }

    @Test
    public void checkoutMultiOrder() throws Exception {
        Order order1 = orderService.getOrder(multiOrder.getOrders().get(0).getId());

        mockMvc.perform(get("/orders")
                .param(CheckouterClientParams.MULTI_ORDER_ID, order1.getProperty(OrderPropertyType.MULTI_ORDER_ID))
                .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                .param(CheckouterClientParams.RGB, Color.BLUE.name()))
                .andDo(log())
                .andExpect(jsonPath("$.orders.length()").value(2));
    }

    @Test
    public void getMultiOrderViaClient() {
        Order order1 = orderService.getOrder(multiOrder.getOrders().get(0).getId());

        OrderSearchRequest searchRequest = new OrderSearchRequest();
        searchRequest.setRgbs(Color.values());
        searchRequest.setMultiOrderId(order1.getProperty(OrderPropertyType.MULTI_ORDER_ID));
        PagedOrders orders = client.getOrders(searchRequest, ClientRole.SYSTEM, 1L);
        assertThat(orders.getItems(), hasSize(2));
    }

}
