package ru.yandex.market.checkout.checkouter.order;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.request.BasicOrderRequest;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.checkout.checkouter.viewmodel.OrderChangesViewModel;
import ru.yandex.market.checkout.helpers.OrderChangesHelper;
import ru.yandex.market.checkout.providers.BlueParametersProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderChangesTest extends AbstractWebTestBase {

    @Autowired
    private OrderChangesHelper orderChangesHelper;
    private RequestClientInfo clientInfo;
    private Order order;

    @BeforeEach
    void setUp() {
        order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        clientInfo = new RequestClientInfo(ClientRole.SYSTEM, null);
    }

    @Test
    void shouldReturnEmptyChanges() throws Exception {
        OrderChangesViewModel orderChanges = orderChangesHelper.getOrderChanges(order.getId(), ClientInfo.SYSTEM);

        assertNotNull(orderChanges.getChangedTotal());
        assertThat(orderChanges.getItemsChanged(), empty());
    }

    @Test
    void shouldReturnEmptyChangesFromClient() {
        BasicOrderRequest request = BasicOrderRequest.builder(order.getId()).build();
        OrderChangesViewModel orderChanges = client.getOrderChanges(clientInfo, request);

        assertNotNull(orderChanges.getChangedTotal());
        assertThat(orderChanges.getItemsChanged(), empty());
    }

    @Test
    void shouldReturnEmptyChangesFromClient_bulkCase() {
        BasicOrdersRequest request = BasicOrdersRequest.builder(List.of(order.getId())).build();
        List<OrderChangesViewModel> ordersChanges = client.bulkGetOrderChanges(clientInfo, request);

        assertEquals(1, ordersChanges.size());
        assertThat(ordersChanges.get(0).getItemsChanged(), empty());
    }

    @Test
    void shouldReturnEmptyChangesForUser() throws Exception {
        OrderChangesViewModel orderChanges = orderChangesHelper.getOrderChanges(order.getId(),
                new ClientInfo(ClientRole.USER, order.getBuyer().getUid()));

        assertNotNull(orderChanges.getChangedTotal());
        assertThat(orderChanges.getItemsChanged(), empty());
    }

    @Test
    void shouldReturn404ForInvalidUser() throws Exception {
        orderChangesHelper.getOrderChangesForActions(order.getId(), new ClientInfo(ClientRole.USER,
                order.getBuyer().getUid() + 1L))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnEmptyResponseForInvalidUser_bulkCase() {
        clientInfo = new RequestClientInfo(ClientRole.USER, order.getBuyer().getUid() + 1L);
        BasicOrdersRequest request = BasicOrdersRequest.builder(List.of(order.getId())).build();
        List<OrderChangesViewModel> ordersChanges = client.bulkGetOrderChanges(clientInfo, request);

        assertEquals(0, ordersChanges.size());
    }

}
