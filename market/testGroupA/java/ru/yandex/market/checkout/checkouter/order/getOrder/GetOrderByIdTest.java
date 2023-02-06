package ru.yandex.market.checkout.checkouter.order.getOrder;

import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.controllers.oms.OrderController;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.NotifyTracksHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.DeliveryTrackCheckpointProvider;
import ru.yandex.market.checkout.providers.DeliveryTrackProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.services.OrderServiceHelper;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.helpers.utils.GetOrdersUtils.resultMatcherOrderNotFound;

/**
 * Tests for controller method
 * {@link OrderController#getOrder(long, ru.yandex.market.checkout.checkouter.client.ClientRole,
 * java.lang.Long, java.lang.Long, java.lang.Boolean)}
 *
 * @see <a href="https://wiki.yandex-team.ru/market/marketplace/dev/checkouter/api/get/orders/order-id/"/>
 * @see <a href="https://testpalm.yandex-team.ru/testcase/checkouter-151"/>
 */
public class GetOrderByIdTest extends AbstractWebTestBase {

    private final String urlTemplate = "/orders/{order-id}";
    @Autowired
    private OrderServiceHelper orderServiceHelper;
    @Autowired
    private NotifyTracksHelper notifyTracksHelper;

    @Epic(Epics.GET_ORDER)
    @Story(Stories.ORDERS_ORDER_ID)
    @DisplayName("RE: GET /orders/{orderId}")
    @Test
    public void testId() throws Exception {
        Parameters parameters = new Parameters();
        Order order = orderCreateHelper.createOrder(parameters);

        mockMvc.perform(
                MockMvcRequestBuilders.get(urlTemplate, order.getId())
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(order.getId().intValue()));

        long nonExistingId = Long.MAX_VALUE;
        mockMvc.perform(
                MockMvcRequestBuilders.get(urlTemplate, nonExistingId)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherOrderNotFound(nonExistingId));
    }

    @Test
    public void testClientRole() throws Exception {
        final Long userId = 83559L;
        final Long shopId = 92538L;
        final Long clientId = 13L;
        Parameters parameters = new Parameters();
        parameters.getBuyer().setUid(userId);
        parameters.setShopId(shopId);
        Order order = orderCreateHelper.createOrder(parameters);

        //ClientRole.SYSTEM
        mockMvc.perform(
                MockMvcRequestBuilders.get(urlTemplate, order.getId())
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(order.getId().intValue()));

        //ClientRole.REFEREE
        mockMvc.perform(
                MockMvcRequestBuilders.get(urlTemplate, order.getId())
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.REFEREE.name())
                        .param(CheckouterClientParams.CLIENT_ID, clientId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(order.getId().intValue()));

        //ClientRole.USER
        mockMvc.perform(
                MockMvcRequestBuilders.get(urlTemplate, order.getId())
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.USER.name())
                        .param(CheckouterClientParams.CLIENT_ID, userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(order.getId().intValue()));

        mockMvc.perform(
                MockMvcRequestBuilders.get(urlTemplate, order.getId())
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.USER.name())
                        .param(CheckouterClientParams.CLIENT_ID, clientId.toString()))
                .andExpect(resultMatcherOrderNotFound(order.getId()));

        //ClientRole.SHOP
        mockMvc.perform(
                MockMvcRequestBuilders.get(urlTemplate, order.getId())
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SHOP.name())
                        .param(CheckouterClientParams.CLIENT_ID, shopId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(order.getId().intValue()));

        mockMvc.perform(
                MockMvcRequestBuilders.get(urlTemplate, order.getId())
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SHOP.name())
                        .param(CheckouterClientParams.CLIENT_ID, clientId.toString()))
                .andExpect(resultMatcherOrderNotFound(order.getId()));

        //ClientRole.SHOP_USER
        mockMvc.perform(
                MockMvcRequestBuilders.get(urlTemplate, order.getId())
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SHOP_USER.name())
                        .param(CheckouterClientParams.CLIENT_ID, clientId.toString())
                        .param(CheckouterClientParams.SHOP_ID, shopId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(order.getId().intValue()));

        mockMvc.perform(
                MockMvcRequestBuilders.get(urlTemplate, order.getId())
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SHOP_USER.name())
                        .param(CheckouterClientParams.CLIENT_ID, userId.toString())
                        .param(CheckouterClientParams.SHOP_ID, clientId.toString()))
                .andExpect(resultMatcherOrderNotFound(order.getId()));

    }

    @Test
    public void testShowReturnStatuses() throws Exception {
        Order order = OrderProvider.getOrderWithTracking();
        order = orderServiceHelper.saveOrder(order);
        //push 2 checkpoints: one normal and one return-type
        notifyTracksHelper.notifyTracks(DeliveryTrackProvider.getDeliveryTrack(
                String.valueOf(order.getId()),
                DeliveryTrackCheckpointProvider.deliveryTrackCheckpoint(1),
                DeliveryTrackCheckpointProvider.deliveryTrackCheckpoint(112213, 60)
        ));

        //request order/{id} with SHOW_RETURN_STATUSES=true, should return one normal, one return checkpoint
        mockMvc.perform(
                MockMvcRequestBuilders.get(urlTemplate, order.getId())
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                        .param(CheckouterClientParams.SHOW_RETURN_STATUSES, Boolean.TRUE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.delivery.shipments[*].tracks[*].checkpoints[?(@.id>0)]")
                        .value(Matchers.hasSize(2)))
                .andExpect(jsonPath("$.delivery.shipments[*].tracks[*].checkpoints[?(@.deliveryStatus=='%d')]", 60)
                        .value(Matchers.hasSize(1)));

        //request order/{id} with SHOW_RETURN_STATUSES=false, should return only one normal checkpoint
        mockMvc.perform(
                MockMvcRequestBuilders.get(urlTemplate, order.getId())
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                        .param(CheckouterClientParams.SHOW_RETURN_STATUSES, Boolean.FALSE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.delivery.shipments[*].tracks[*].checkpoints[?(@.id>0)]")
                        .value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.delivery.shipments[*].tracks[*].checkpoints[?(@.deliveryStatus=='%d')]", 60)
                        .value(Matchers.hasSize(0)));
    }


    @DisplayName("Estimated: получить в истории информацию, что доставка является неточной")
    @Test
    public void orderDeliveryIsEstimatedTest() throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();

        parameters.getReportParameters()
                .getActualDelivery()
                .getResults()
                .get(0)
                .getDelivery()
                .forEach(p -> p.setEstimated(true));

        Order order = orderCreateHelper.createOrder(parameters);

        mockMvc.perform(
                        MockMvcRequestBuilders.get(urlTemplate, order.getId())
                                .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                                .param(CheckouterClientParams.SHOW_RETURN_STATUSES, Boolean.TRUE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.delivery.estimated").value(true));
    }
}
