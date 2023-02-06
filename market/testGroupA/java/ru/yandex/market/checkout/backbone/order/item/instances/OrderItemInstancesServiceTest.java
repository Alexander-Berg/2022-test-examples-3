package ru.yandex.market.checkout.backbone.order.item.instances;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderItemInstance;
import ru.yandex.market.checkout.checkouter.order.OrderItemInstances;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.util.OrderItemInstancesUtil;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class OrderItemInstancesServiceTest extends AbstractWebTestBase {

    private static final String CIS_VALID = "010465006531553121CtPoNqNB7qOdc";
    private static final String CIS_INVALID = "010465006531553121CtPoNqNB7qOdc_1234567890";

    @Test
    public void validateFbyOrderForEditInstances() throws Exception {
        validateCis(CIS_VALID, status().isOk());
        validateCis(CIS_INVALID, status().isBadRequest());
    }

    private void validateCis(String cis, ResultMatcher resultMatchers) throws Exception {
        // минимум обязательных параметров
        var orderItem = new OrderItem();
        orderItem.setId(1L);
        orderItem.setOrderId(1L);
        orderItem.setCount(1);
        var orderItemInstance = new OrderItemInstance();
        orderItemInstance.setCis(cis);
        var orderItemInstances = new OrderItemInstances();
        orderItemInstances.setItemId(1L);
        orderItemInstances.setInstances(List.of(orderItemInstance));

        //
        var request = new ValidateOrderItemsInstancesRequest(OrderStatus.UNPAID, null, List.of(orderItem),
                List.of(orderItemInstances), true, null, List.of(), false);
        var requestAsString = new ObjectMapper().writeValueAsString(request);
        mockMvc.perform(post("/orders/{orderId}/items/instances/validate", 1L)
                        .content(requestAsString)
                        .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andDo(print())
                .andExpect(resultMatchers);
    }

    @Test
    @DisplayName("Предобработка изменяет balanceOrderId и instances")
    public void preprocessFbyOrderForEditInstances() throws Exception {
        var orderItem = new OrderItem();
        orderItem.setId(1L);
        orderItem.setOrderId(1L);
        orderItem.setCount(2);
        orderItem.setCargoTypes(Set.of(980));
        ArrayNode instances = OrderItemInstancesUtil.putNodes(orderItem.getInstances(),
                OrderItemInstance.InstanceType.BALANCE_ORDER_ID.getName(), List.of("1234567890"));
        orderItem.setInstances(instances);
        var orderItemInstance = new OrderItemInstance();
        orderItemInstance.setCis("010465006531553121CtPoNqNB7qOdc");
        orderItemInstance.setBalanceOrderId("1234567890");
        var orderItemInstances = new OrderItemInstances();
        orderItemInstances.setItemId(1L);
        orderItemInstances.setInstances(List.of(orderItemInstance));

        var delivery = new Delivery();
        delivery.setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        var request = new ValidateOrderItemsInstancesRequest(OrderStatus.UNPAID, null, List.of(orderItem),
                List.of(orderItemInstances), true, delivery, List.of(), false);
        var objectMapper = new ObjectMapper()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        var requestAsString = objectMapper.writeValueAsString(request);
        var responseAsString = mockMvc.perform(post("/orders/{orderId}/items/instances/preprocess", 1L)
                        .content(requestAsString)
                        .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        var response = (List<OrderItem>) objectMapper.readValue(responseAsString,
                new TypeReference<List<OrderItem>>() { });
        Assertions.assertFalse(response.isEmpty());
        var item = response.get(0);
        Assertions.assertNotNull(item.getInstances());
        Assertions.assertTrue(item.getInstances().toString().contains("1-item-1-1"));
        Assertions.assertEquals("1-item-1", item.getBalanceOrderId());
    }
}
