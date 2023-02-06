package ru.yandex.market.checkout.checkouter.controller;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.backbone.validation.order.item.instances.CisItemInstancesValidator;
import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.json.RemovingCisRequirementRequest;
import ru.yandex.market.checkout.checkouter.json.RemovingCisRequirementResponse;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestType;
import ru.yandex.market.checkout.checkouter.order.changerequest.MethodOfChange;
import ru.yandex.market.checkout.checkouter.order.edit.PossibleOrderChange;
import ru.yandex.market.checkout.helpers.OrderCreateHelper;
import ru.yandex.market.checkout.helpers.OrderStatusHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SupportControllerTest extends AbstractWebTestBase {

    private static final String FIRE_TASK_URL = "/fireZkTask/{taskName}";
    @Autowired
    private CheckouterAPI client;
    @Autowired
    private OrderCreateHelper orderCreateHelper;
    @Autowired
    private OrderStatusHelper orderStatusHelper;
    @Autowired
    @Qualifier("checkouterAnnotationObjectMapper")
    private ObjectMapper mapper;

    @Test
    void orderEventsLogbrokerExportTaskPositiveTest() throws Exception {
        mockMvc.perform(post(FIRE_TASK_URL, "OrderEventsLbkxExportTask_0"))
                .andExpect(status().isOk());
    }

    @Test
    public void propertiesSerializationTest() throws Exception {
        mockMvc.perform(get("/properties"))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    void possibleOrderChangeSerializationTest() throws Exception {
        PossibleOrderChange change = new PossibleOrderChange(ChangeRequestType.DELIVERY_DATES,
                MethodOfChange.PARTNER_API, 50, 60);
        assertThat(mapper.writeValueAsString(List.of(change))).isNotEmpty();
    }

    @Test
    void shouldRemoveCisRequirementFromItem() {
        OrderItem itemTemplate = OrderItemProvider.buildOrderItem("requiredCis");
        itemTemplate.setCargoTypes(CisItemInstancesValidator.CIS_REQUIRED_CARGOTYPE_CODES);
        Parameters params = BlueParametersProvider.defaultBlueOrderParametersWithItems(itemTemplate);
        Order order = orderCreateHelper.createOrder(params);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        OrderItem item = findActualItem(order, itemTemplate);
        RemovingCisRequirementRequest request = new RemovingCisRequirementRequest(false, List.of(item.getMsku()));

        RemovingCisRequirementResponse response = client.removeCisRequirement(request);

        assertThat(response.getEditedOrderIds()).containsOnly(order.getId());
    }

    @Test
    void skipRemovingCisRequirementFromItemBecauseMissingStatus() {
        OrderItem itemTemplate = OrderItemProvider.buildOrderItem("requiredCis");
        itemTemplate.setCargoTypes(CisItemInstancesValidator.CIS_REQUIRED_CARGOTYPE_CODES);
        Parameters params = BlueParametersProvider.defaultBlueOrderParametersWithItems(itemTemplate);
        Order order = orderCreateHelper.createOrder(params);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        OrderItem item = findActualItem(order, itemTemplate);
        RemovingCisRequirementRequest request = new RemovingCisRequirementRequest(false, List.of(item.getMsku()));

        RemovingCisRequirementResponse response = client.removeCisRequirement(request);

        assertThat(response.getEditedOrderIds()).isEmpty();
    }

    @Test
    void skipRemovingCisRequirementFromItemBecauseMissingMsku() {
        OrderItem itemTemplate = OrderItemProvider.buildOrderItem("requiredCis");
        itemTemplate.setCargoTypes(CisItemInstancesValidator.CIS_REQUIRED_CARGOTYPE_CODES);
        Parameters params = BlueParametersProvider.defaultBlueOrderParametersWithItems(itemTemplate);
        Order order = orderCreateHelper.createOrder(params);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        OrderItem item = findActualItem(order, itemTemplate);
        RemovingCisRequirementRequest request = new RemovingCisRequirementRequest(false, List.of(item.getMsku() + 1));

        RemovingCisRequirementResponse response = client.removeCisRequirement(request);

        assertThat(response.getEditedOrderIds()).isEmpty();
    }

    private OrderItem findActualItem(Order order, OrderItem itemTemplate) {
        return order.getItems().stream()
                .filter(item -> item.getOfferId().equals(itemTemplate.getOfferId()))
                .findAny()
                .orElseThrow();
    }
}
