package ru.yandex.market.checkout.checkouter.controller.archive;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.checkout.application.AbstractArchiveWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.BasicOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.request.OrderRequest;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCObjectType;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.queuedcalls.QueuedCallType;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ArchiveControllerTest extends AbstractArchiveWebTestBase {

    @DisplayName("Если архивации без проверки условий архивации запрещена, не можем заархивировать заказ")
    @Test
    public void notArchiveOrderWithoutCheckingArchivingConditions() throws Exception {
        Order order = createBlueOrder();
        mockMvc.perform(post("/archive/orders/" + order.getId())
                        .param("check_archiving_conditions", "false"))
                .andExpect(status().isBadRequest());
        checkOrderIsAvailableUnderBasicAPI(order);
    }

    @DisplayName("Если разрешена архивация без проверки условий архивации, можем заархивировать заказ")
    @Test
    public void archiveOrderWithoutCheckingArchivingConditionsIfEnabled() throws Exception {
        checkouterProperties.setArchiveAPIEnabled(true);
        checkouterProperties.setArchiveWithoutCheckingArchivingConditionEnabled(true);
        Order order = createBlueOrder();
        mockMvc.perform(post("/archive/orders/" + order.getId())
                        .param("check_archiving_conditions", "false"))
                .andExpect(status().isOk());
        checkOrderIsAvailableUnderArchiveAPI(order);
    }


    @DisplayName("Не архивировать, если заказ не удовлетворяет условиям архивации")
    @Test
    public void notArchiveOrderIfNotMeetArchivingConditions() throws Exception {
        checkouterProperties.setArchiveAPIEnabled(true);
        Order order = createBlueOrder();
        mockMvc.perform(post("/archive/orders/" + order.getId()))
                .andExpect(status().isOk());
        checkOrderIsAvailableUnderBasicAPI(order);
    }

    @DisplayName("Архивировать мультизаказ без проверки условий архивации")
    @Test
    public void archiveMultiOrder() throws Exception {
        checkouterProperties.setArchiveAPIEnabled(true);
        checkouterProperties.setArchiveWithoutCheckingArchivingConditionEnabled(true);
        List<Order> orders = createMultiOrder();
        Set<Long> orderIds = orders.stream().map(BasicOrder::getId).collect(Collectors.toSet());
        String multiOrderId = masterJdbcTemplate.queryForObject(
                "select text_value from order_property where order_id = "
                        + orderIds.iterator().next() + " and name = 'multiOrderId'", String.class);
        mockMvc.perform(post("/archive/multiOrder/" + multiOrderId)
                        .param("check_archiving_conditions", "false"))
                .andExpect(status().isOk());
        orders.forEach(o -> checkOrderIsAvailableUnderArchiveAPI(o));
    }

    @DisplayName("Архивировать заказы без проверки условий архивации")
    @Test
    public void archiveOrders() throws Exception {
        checkouterProperties.setArchiveAPIEnabled(true);
        checkouterProperties.setArchiveWithoutCheckingArchivingConditionEnabled(true);
        Order firstOrder = createBlueOrder();
        Order secondOrder = createBlueOrder();
        mockMvc.perform(post("/archive/orders")
                        .content(String.format("[\"%s\", \"%s\"]", firstOrder.getId(), secondOrder.getId()))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param("check_archiving_conditions", "false"))
                .andExpect(status().isOk());
        checkOrderIsAvailableUnderArchiveAPI(firstOrder);
        checkOrderIsAvailableUnderArchiveAPI(secondOrder);
    }

    @DisplayName("Поставить QC на разархивацию заказа")
    @Test
    public void unarchiveOrder() throws Exception {
        Order order = createBlueOrder();
        mockMvc.perform(post("/unarchive/orders/" + order.getId()))
                .andExpect(status().isOk());
        Set<QueuedCallType> queuedCallTypes = queuedCallService
                .existingCallsForObjId(CheckouterQCObjectType.ORDER, order.getId());
        assertTrue(queuedCallTypes.contains(CheckouterQCType.UNARCHIVE_ORDER));
    }

    private void checkOrderIsAvailableUnderBasicAPI(Order order) {
        checkOrderIsAvailableUnderAPI(order, false);
    }

    private void checkOrderIsAvailableUnderArchiveAPI(Order order) {
        checkOrderIsAvailableUnderAPI(order, true);
    }

    private void checkOrderIsAvailableUnderAPI(Order order, boolean archived) {
        Order orderViewModel = client.getOrder(
                new RequestClientInfo(ClientRole.SYSTEM, null), OrderRequest.builder(order.getId())
                        .withArchived(archived)
                        .build()
        );
        assertNotNull(orderViewModel);
    }
}
