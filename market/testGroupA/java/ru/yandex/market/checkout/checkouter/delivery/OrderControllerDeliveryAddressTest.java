package ru.yandex.market.checkout.checkouter.delivery;

import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.test.providers.AddressProvider;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;
import ru.yandex.market.checkout.util.services.OrderServiceHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


public class OrderControllerDeliveryAddressTest extends AbstractWebTestBase {

    @Autowired
    private OrderServiceHelper orderServiceHelper;
    @Autowired
    private TestSerializationService testSerializationService;

    public static Stream<Arguments> parameterizedTestData() {

        return Arrays.asList(
                new Object[]{new ClientInfo(ClientRole.SHOP, OrderServiceHelper.DEFAULT_SHOP_ID)},
                new Object[]{new ClientInfo(ClientRole.BUSINESS, OrderServiceHelper.DEFAULT_BUSINESS_ID)},
                new Object[]{new ClientInfo(ClientRole.CALL_CENTER_OPERATOR, 123213L)},
                new Object[]{new ClientInfo(ClientRole.CRM_ROBOT, 0L)},
                new Object[]{ClientInfo.SYSTEM}
        ).stream().map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void shouldAllowToEditOrderDelivery(ClientInfo clientInfo) throws Exception {
        Order order = orderServiceHelper.createOrder(Color.BLUE);
        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        assertEquals(OrderStatus.PROCESSING, order.getStatus());

        Delivery newDelivery = new Delivery();
        newDelivery.setShopAddress(AddressProvider.getAnotherAddress());

        mockMvc.perform(post("/orders/{orderId}/delivery", order.getId())
                .param(CheckouterClientParams.CLIENT_ROLE, clientInfo.getRole().name())
                .param(CheckouterClientParams.CLIENT_ID, String.valueOf(clientInfo.getId()))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(testSerializationService.serializeCheckouterObject(newDelivery)))
                .andExpect(status().isOk());
    }
}
