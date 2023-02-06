package ru.yandex.market.checkout.helpers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.PagedOrders;
import ru.yandex.market.checkout.common.WebTestHelper;
import ru.yandex.market.checkout.helpers.utils.MockMvcAware;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.BUSINESS_ID;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.CLIENT_ID;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.CLIENT_ROLE;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.SHOP_ID;

@WebTestHelper
public class OrderGetHelper extends MockMvcAware {

    @Autowired
    public OrderGetHelper(WebApplicationContext webApplicationContext,
                          TestSerializationService testSerializationService) {
        super(webApplicationContext, testSerializationService);
    }

    public Order getOrder(long orderId, ClientInfo clientInfo) throws Exception {
        return getOrder(orderId, clientInfo, false);
    }

    public Order getOrder(long orderId, ClientInfo clientInfo, boolean archived) throws Exception {
        MockHttpServletRequestBuilder request = get("/orders/{orderId}", orderId)
                .param(CLIENT_ROLE, clientInfo.getRole().name())
                .param(CheckouterClientParams.RGB, Color.BLUE.name(), Color.WHITE.name())
                .param(CLIENT_ID, clientInfo.getId() == null ? null : clientInfo.getId().toString())
                .param(SHOP_ID, clientInfo.getShopId() == null ? null : clientInfo.getShopId().toString())
                .param(BUSINESS_ID, clientInfo.getBusinessId() == null ? null : clientInfo.getBusinessId().toString())
                .param("archived", String.valueOf(archived));
        return performApiRequest(request, Order.class);
    }

    public void performRequestAndExpectOrderNotFoundException(long orderId, ClientInfo clientInfo, boolean archived)
            throws Exception {
        MockHttpServletRequestBuilder request = get("/orders/{orderId}", orderId)
                .param(CLIENT_ROLE, clientInfo.getRole().name())
                .param(CheckouterClientParams.RGB, Color.BLUE.name(), Color.WHITE.name())
                .param(CLIENT_ID, clientInfo.getId() == null ? null : clientInfo.getId().toString())
                .param(SHOP_ID, clientInfo.getShopId() == null ? null : clientInfo.getShopId().toString())
                .param(BUSINESS_ID, clientInfo.getBusinessId() == null ? null : clientInfo.getBusinessId().toString())
                .param("archived", String.valueOf(archived));
        mockMvc.perform(request)
                .andDo(log())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Order not found: " + orderId));
    }

    public PagedOrders getOrders(ClientInfo clientInfo, String fromDate, String toDate) throws Exception {
        MockHttpServletRequestBuilder request = get("/orders")
                .param(CLIENT_ROLE, clientInfo.getRole().name())
                .param(CheckouterClientParams.RGB, Color.BLUE.name(), Color.WHITE.name())
                .param(CLIENT_ID, clientInfo.getId() == null ? null : clientInfo.getId().toString())
                .param(SHOP_ID, clientInfo.getShopId() == null ? null : clientInfo.getShopId().toString())
                .param(BUSINESS_ID, clientInfo.getBusinessId() == null ? null : clientInfo.getBusinessId().toString())
                .param(CheckouterClientParams.RGB, Color.GREEN.name())
                .param(CheckouterClientParams.RGB, Color.BLUE.name())
                .param(CheckouterClientParams.RGB, Color.RED.name())
                .param(CheckouterClientParams.FROM_DATE, fromDate)
                .param(CheckouterClientParams.TO_DATE, toDate);

        return performApiRequest(request, PagedOrders.class);
    }
}
