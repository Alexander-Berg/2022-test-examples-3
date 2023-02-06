package ru.yandex.market.checkout.checkouter.orderItem;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBox;
import ru.yandex.market.checkout.checkouter.event.HistoryEventReason;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.order.JsonInstance;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderStatusNotAllowedException;
import ru.yandex.market.checkout.checkouter.order.OrderStatusUpdateResult;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.OrderUpdateService;
import ru.yandex.market.checkout.checkouter.order.StatusAndSubstatus;
import ru.yandex.market.checkout.checkouter.pay.AbstractPaymentTestBase;
import ru.yandex.market.checkout.checkouter.storage.OrderWritingDao;
import ru.yandex.market.checkout.checkouter.storage.item.OrderItemDao;
import ru.yandex.market.checkout.checkouter.viewmodel.OrderItemViewModel;
import ru.yandex.market.checkout.checkouter.viewmodel.OrderViewModel;
import ru.yandex.market.checkout.checkouter.viewmodel.containers.OrderItemsViewModel;
import ru.yandex.market.checkout.helpers.ChangeOrderItemsHelper;
import ru.yandex.market.checkout.helpers.DropshipDeliveryHelper;
import ru.yandex.market.checkout.helpers.OrderHistoryEventsTestHelper;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.OrderStatusHelper;
import ru.yandex.market.checkout.helpers.ParcelBoxHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.util.ClientHelper;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.order.JsonInstance.toNode;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PROCESSING;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;

public class PutOrderItemInstancesTest extends AbstractPaymentTestBase {

    private final String cis1 = "010465006531553121CtPoNqNB7qOdc";
    private final String cis2 = "010465006531553121.tPoNqN;7qOdc";
    private final String cis3 = "010465006531553121.tPoNqN;8qOdc";
    private final String cis4 = "010465006531553121.tPoNqN;9qOdc";
    private final String invalidCis = "invalidCis";
    //    КИЗ с криптохвостом
    private final String validCisFull = "0104601662000016215RNef*\\u001d93B0Ik";
    private final String uit1 = "12345678";
    private final String imei1 = "352099001761481";
    private final String sn1 = "01123456789012342112";
    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private ChangeOrderItemsHelper changeOrderItemsHelper;
    @Autowired
    protected OrderUpdateService orderUpdateService;
    @Autowired
    private ParcelBoxHelper parcelBoxHelper;
    @Autowired
    private TestSerializationService serializationService;
    @Autowired
    private OrderWritingDao orderWritingDao;
    @Autowired
    private OrderItemDao orderItemDao;
    @Autowired
    private OrderHistoryEventsTestHelper eventsTestHelper;
    @Autowired
    private OrderStatusHelper orderStatusHelper;

    private ObjectMapper mapper = new ObjectMapper();

    private String createSingleCis() {
        return "[{\"cis\":\"" + cis1 + "\",\"cisFull\":\"" + invalidCis + "\"}]";
    }

    private String createSingleCisWithoutCryptotail() {
        return "[{\"cis\":\"" + cis1 + "\"}]";
    }

    private String createInvalidCis() {
        return "[{\"cis\":\"" + invalidCis + "\"}]";
    }

    private String createValidCisWithCryptotail() {
        return "[{\"cis\":\"" + validCisFull + "\"}]";
    }

    private String createTwoCises() {
        return "[{\"cis\":\"" + cis1 + "\"}, {\"cis\":\"" + cis2 + "\"}]";
    }

    private String createTwoCisesWithoutSpace() {
        return "[{\"cis\":\"" + cis1 + "\"},{\"cis\":\"" + cis2 + "\"}]";
    }

    private String createPutInstancesItemRequest(long itemId, String cises) {
        return "{\n" +
                "    \"items\": [\n" +
                "        {\n" +
                "            \"id\": " + itemId + ",\n" +
                "            \"instances\": " + cises + "\n" +
                "        }\n" +
                "    ]\n" +
                "}";
    }

    private String createPutInstancesItemRequest(long item1, long item2) {
        return "{\n" +
                "    \"items\": [\n" +
                "        {\n" +
                "            \"id\": " + item1 + ",\n" +
                "            \"instances\": [{\"cis\": \"" + cis1 + "\"}, {\"cis\": \"" + cis2 + "\"}]\n" +
                "        },\n" +
                "        {\n" +
                "            \"id\": " + item2 + ",\n" +
                "            \"instances\": [{\"cis\": \"" + cis3 + "\"}, {\"cis\": \"" + cis4 + "\"}]\n" +
                "        }\n" +
                "    ]\n" +
                "}";
    }

    private String createPutInstancesWithTwoCisesRequest(long itemId) {
        return createPutInstancesItemRequest(itemId, createTwoCises());
    }

    private String createPutInstancesWithSingleCisRequest(long itemId) {
        return createPutInstancesItemRequest(itemId, createSingleCis());
    }

    private String createPutInstancesWithInvalidSingleCisRequest(long itemId) {
        return createPutInstancesItemRequest(itemId, createInvalidCis());
    }

    private String createPutInstancesWithValidSingleCisWithCryptotailRequest(long itemId) {
        return createPutInstancesItemRequest(itemId, createValidCisWithCryptotail());
    }

    private String createPutInstancesWithValidSingleCisWithoutCryptotailRequest(long itemId) {
        return createPutInstancesItemRequest(itemId, createSingleCisWithoutCryptotail());
    }

    @Test
    public void putInstances() throws Exception {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.getOrder().getItems().forEach(item -> {
            item.setCargoTypes(Collections.singleton(CIS_REQUIRED_CARGOTYPE_CODE));
        });
        Order order = orderCreateHelper.createOrder(parameters);
        long itemId = order.getItems().iterator().next().getId();
        mockMvc.perform(put("/orders/{orderId}/items/instances", order.getId())
                .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                .param(CheckouterClientParams.CLIENT_ID, "0")
                .content(createPutInstancesWithTwoCisesRequest(itemId))
                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isBadRequest()); //лишние кизы
        mockMvc.perform(put("/orders/{orderId}/items/instances", order.getId())
                .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                .param(CheckouterClientParams.CLIENT_ID, "0")
                .content(createPutInstancesWithSingleCisRequest(itemId))
                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());
        Order changedOrder = orderService.getOrder(order.getId());
        OrderItem orderItem = changedOrder.getItem(itemId);
        Assertions.assertNotNull(orderItem.getInstances());
        Assertions.assertEquals(createSingleCis(), orderItem.getInstances().toString());
        List<OrderHistoryEvent> events = eventsTestHelper.getEventsOfType(order.getId(),
                HistoryEventType.ITEMS_UPDATED);
        assertThat(events.size(), is(1));
        assertThat(events.get(0).getType(), is(HistoryEventType.ITEMS_UPDATED));
        assertThat(events.get(0).getReason(), is(HistoryEventReason.ITEM_INSTANCES_UPDATED));
    }

    @Test
    public void putInstancesWithCisFull() throws Exception {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_CIS_FULL_VALIDATION, true);
        Parameters parameters = defaultBlueOrderParameters();
        parameters.getOrder().getItems().forEach(item -> {
            item.setCargoTypes(Collections.singleton(CIS_REQUIRED_CARGOTYPE_CODE));
        });
        Order order = orderCreateHelper.createOrder(parameters);
        long itemId = order.getItems().iterator().next().getId();
        mockMvc.perform(put("/orders/{orderId}/items/instances", order.getId())
                .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                .param(CheckouterClientParams.CLIENT_ID, "0")
                .content(createPutInstancesWithInvalidSingleCisRequest(itemId))
                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isBadRequest()); // cisFull и cis с криптохвостом невалидный
        mockMvc.perform(put("/orders/{orderId}/items/instances", order.getId())
                .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                .param(CheckouterClientParams.CLIENT_ID, "0")
                .content(createPutInstancesWithValidSingleCisWithCryptotailRequest(itemId))
                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());
        String expectedCisValues = "[{\"cis\":\"0104601662000016215RNef*\"," +
                "\"cisFull\":\"0104601662000016215RNef*\\u001D93B0Ik\"}]";
        Order changedOrder = orderService.getOrder(order.getId());
        OrderItem orderItem = changedOrder.getItem(itemId);
        Assertions.assertNotNull(orderItem.getInstances());
        Assertions.assertEquals(expectedCisValues, orderItem.getInstances().toString());
        List<OrderHistoryEvent> events = eventsTestHelper.getEventsOfType(order.getId(),
                HistoryEventType.ITEMS_UPDATED);
        assertThat(events.size(), is(1));
        assertThat(events.get(0).getType(), is(HistoryEventType.ITEMS_UPDATED));
        assertThat(events.get(0).getReason(), is(HistoryEventReason.ITEM_INSTANCES_UPDATED));
    }

    @Test
    public void putInstancesWithCis() throws Exception {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_CIS_FULL_VALIDATION, true);
        Parameters parameters = defaultBlueOrderParameters();
        parameters.getOrder().getItems().forEach(item -> {
            item.setCargoTypes(Collections.singleton(CIS_REQUIRED_CARGOTYPE_CODE));
        });
        Order order = orderCreateHelper.createOrder(parameters);
        long itemId = order.getItems().iterator().next().getId();
        mockMvc.perform(put("/orders/{orderId}/items/instances", order.getId())
                .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                .param(CheckouterClientParams.CLIENT_ID, "0")
                .content(createPutInstancesWithInvalidSingleCisRequest(itemId))
                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/orders/{orderId}/items/instances", order.getId())
                .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                .param(CheckouterClientParams.CLIENT_ID, "0")
                .content(createPutInstancesWithValidSingleCisWithoutCryptotailRequest(itemId))
                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());
        Order changedOrder = orderService.getOrder(order.getId());
        OrderItem orderItem = changedOrder.getItem(itemId);
        Assertions.assertNotNull(orderItem.getInstances());
        String expectedCisValues = "[{\"cis\":\"" + cis1 + "\"}]";
        Assertions.assertEquals(expectedCisValues, orderItem.getInstances().toString());
        List<OrderHistoryEvent> events = eventsTestHelper.getEventsOfType(order.getId(),
                HistoryEventType.ITEMS_UPDATED);
        assertThat(events.size(), is(1));
        assertThat(events.get(0).getType(), is(HistoryEventType.ITEMS_UPDATED));
        assertThat(events.get(0).getReason(), is(HistoryEventReason.ITEM_INSTANCES_UPDATED));
    }


    @Test
    public void putInstancesFromOtherOrder() throws Exception {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.getOrder().getItems().forEach(item -> {
            item.setCargoTypes(Collections.singleton(CIS_REQUIRED_CARGOTYPE_CODE));
        });
        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, PROCESSING);
        long itemId = order.getItems().iterator().next().getId();
        Order order2 = orderCreateHelper.createOrder(parameters);
        orderPayHelper.payForOrder(order2);
        Assertions.assertNotEquals(order.getId(), order2.getId());
        mockMvc.perform(put("/orders/{orderId}/items/instances", order2.getId())
                .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                .param(CheckouterClientParams.CLIENT_ID, "0")
                .content(createPutInstancesWithTwoCisesRequest(itemId))
                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isBadRequest()); //лишние кизы
        mockMvc.perform(put("/orders/{orderId}/items/instances", order2.getId())
                .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                .param(CheckouterClientParams.CLIENT_ID, "0")
                .content(createPutInstancesWithSingleCisRequest(itemId))
                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void putInstancesToItemNotRequiredMarking() throws Exception {
        Parameters parameters = defaultBlueOrderParameters();
        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, PROCESSING);
        long itemId = order.getItems().iterator().next().getId();
        mockMvc.perform(put("/orders/{orderId}/items/instances", order.getId())
                .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                .param(CheckouterClientParams.CLIENT_ID, "0")
                .content(createPutInstancesWithSingleCisRequest(itemId))
                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(ints = {CIS_DISTINCT_CARGOTYPE_CODE, CIS_OPTIONAL_CARGOTYPE_CODE})
    public void allowToPutInstancesWhenItemWithOptionalCargotype(int cargoType) throws Exception {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.getOrder().getItems().forEach(item -> {
            item.setCargoTypes(Collections.singleton(cargoType));
        });
        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, PROCESSING);
        long itemId = order.getItems().iterator().next().getId();
        mockMvc.perform(put("/orders/{orderId}/items/instances", order.getId())
                .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                .param(CheckouterClientParams.CLIENT_ID, "0")
                .content(createPutInstancesWithSingleCisRequest(itemId))
                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());
    }

    @Test
    public void putInstancesToMultiItems() throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParametersWithItems(
                OrderItemProvider.getOrderItem(), OrderItemProvider.getAnotherOrderItem());
        parameters.getOrder().getItems().forEach(item -> {
            item.setCount(2);
            item.setCargoTypes(Collections.singleton(CIS_REQUIRED_CARGOTYPE_CODE));
        });
        Order order = orderCreateHelper.createOrder(parameters);
        Iterator<OrderItem> it = order.getItems().iterator();
        long item1 = it.next().getId();
        long item2 = it.next().getId();
        Assertions.assertEquals(2, order.getItem(item1).getCount());
        Assertions.assertEquals(2, order.getItem(item2).getCount());
        mockMvc.perform(put("/orders/{orderId}/items/instances", order.getId())
                .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                .param(CheckouterClientParams.CLIENT_ID, "0")
                .content(createPutInstancesWithTwoCisesRequest(item1))
                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk()); //не хватает киза 2 айтему
        mockMvc.perform(put("/orders/{orderId}/items/instances", order.getId())
                .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                .param(CheckouterClientParams.CLIENT_ID, "0")
                .content(createPutInstancesItemRequest(item1, item2))
                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());
        Order changedOrder = orderService.getOrder(order.getId());
        OrderItem orderItem = changedOrder.getItem(item1);
        Assertions.assertNotNull(orderItem.getInstances());
        Assertions.assertEquals(toNode(
                new JsonInstance(cis1),
                new JsonInstance(cis2)),
                orderItem.getInstances().toString());
        orderItem = changedOrder.getItem(item2);
        Assertions.assertNotNull(orderItem.getInstances());
        Assertions.assertEquals(toNode(
                new JsonInstance(cis3),
                new JsonInstance(cis4)),
                orderItem.getInstances().toString());
    }

    @Test
    public void putInstancesToMultiItems2() throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParametersWithItems(
                OrderItemProvider.getOrderItem(), OrderItemProvider.getAnotherOrderItem());
        Iterator<Integer> cargoIter = Arrays.asList(CIS_REQUIRED_CARGOTYPE_CODE, CIS_OPTIONAL_CARGOTYPE_CODE)
                .iterator();
        parameters.getOrder().getItems().forEach(item -> {
            item.setCount(2);
            item.setCargoTypes(Collections.singleton(cargoIter.next()));
        });
        Order order = orderCreateHelper.createOrder(parameters);
        Iterator<OrderItem> it = order.getItems().iterator();
        long item1 = it.next().getId();
        long item2 = it.next().getId();
        if (!order.getItem(item1).getCargoTypes().contains(CIS_REQUIRED_CARGOTYPE_CODE)) {
            long temp = item1;
            item1 = item2;
            item2 = temp;
        }
        Assertions.assertEquals(2, order.getItem(item1).getCount());
        Assertions.assertEquals(2, order.getItem(item2).getCount());
        mockMvc.perform(put("/orders/{orderId}/items/instances", order.getId())
                .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                .param(CheckouterClientParams.CLIENT_ID, "0")
                .content(createPutInstancesWithTwoCisesRequest(item1))
                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());
        mockMvc.perform(put("/orders/{orderId}/items/instances", order.getId())
                .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                .param(CheckouterClientParams.CLIENT_ID, "0")
                .content(createPutInstancesItemRequest(item1, item2))
                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());
        Order changedOrder = orderService.getOrder(order.getId());
        OrderItem orderItem = changedOrder.getItem(item1);
        Assertions.assertNotNull(orderItem.getInstances());
        Assertions.assertEquals(toNode(
                new JsonInstance(cis1),
                new JsonInstance(cis2)),
                orderItem.getInstances().toString());
        orderItem = changedOrder.getItem(item2);
        Assertions.assertNotNull(orderItem.getInstances());
        Assertions.assertEquals(toNode(
                new JsonInstance(cis3),
                new JsonInstance(cis4)),
                orderItem.getInstances().toString());
    }

    @Test
    public void putInstancesToMultiItems3() throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParametersWithItems(
                OrderItemProvider.getOrderItem(), OrderItemProvider.getAnotherOrderItem());
        Iterator<Integer> cargoIter = Arrays.asList(CIS_REQUIRED_CARGOTYPE_CODE, 100)
                .iterator();
        parameters.getOrder().getItems().forEach(item -> {
            item.setCount(2);
            item.setCargoTypes(Collections.singleton(cargoIter.next()));
        });
        Order order = orderCreateHelper.createOrder(parameters);
        Iterator<OrderItem> it = order.getItems().iterator();
        long item1 = it.next().getId();
        long item2 = it.next().getId();
        if (!order.getItem(item1).getCargoTypes().contains(CIS_REQUIRED_CARGOTYPE_CODE)) {
            long temp = item1;
            item1 = item2;
            item2 = temp;
        }
        Assertions.assertEquals(2, order.getItem(item1).getCount());
        Assertions.assertEquals(2, order.getItem(item2).getCount());
        mockMvc.perform(put("/orders/{orderId}/items/instances", order.getId())
                .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                .param(CheckouterClientParams.CLIENT_ID, "0")
                .content(createPutInstancesWithTwoCisesRequest(item1))
                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());
        mockMvc.perform(put("/orders/{orderId}/items/instances", order.getId())
                .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                .param(CheckouterClientParams.CLIENT_ID, "0")
                .content(createPutInstancesItemRequest(item1, item2))
                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());
        Order changedOrder = orderService.getOrder(order.getId());
        OrderItem orderItem = changedOrder.getItem(item1);
        Assertions.assertNotNull(orderItem.getInstances());
        Assertions.assertEquals(createTwoCisesWithoutSpace(),
                orderItem.getInstances().toString());
    }

    @Test
    public void changeOrderWithInstances() throws Exception {
        Order order = createPaidDropshipOrderWithCargotype(2, CIS_REQUIRED_CARGOTYPE_CODE);
        long itemId = order.getItems().iterator().next().getId();
        order = orderService.getOrder(order.getId());
        Assertions.assertEquals("[{\"balanceOrderId\":\"" + order.getId()
                        + "-item-" + itemId + "-1\"},{\"balanceOrderId\":\"" + order.getId() + "-item-" + itemId +
                        "-2\"}]",
                order.getItem(itemId).getInstances().toString());
        Collection<OrderItem> newItems = order.getItems();
        newItems.forEach(item -> item.setCount(1));
        ResultActions response = changeOrderItemsHelper.changeOrderItems(newItems, ClientHelper.shopClientFor(order),
                order.getId());
        String stringResponse = response
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        order = orderService.getOrder(order.getId());
        Assertions.assertEquals(1, order.getItem(itemId).getCount());
        Assertions.assertEquals("[{\"balanceOrderId\":\"" + order.getId()
                        + "-item-" + itemId + "-1\"}]",
                order.getItem(itemId).getInstances().toString());
    }

    private void assertOrderStatusUpdateNotAllowed(long orderId, OrderStatus orderStatus,
                                                   OrderSubstatus orderSubstatus, ClientInfo clientInfo) {
        Assertions.assertThrows(OrderStatusNotAllowedException.class, () -> {
            orderUpdateService.validateCisesAndUpdateOrderStatus(orderId, StatusAndSubstatus.of(orderStatus,
                    orderSubstatus),
                    clientInfo, null);
        });
    }

    private void assertOrderStatusUpdateAllowed(long orderId, OrderStatus orderStatus,
                                                OrderSubstatus orderSubstatus, ClientInfo clientInfo) {
        orderUpdateService.updateOrderStatus(orderId, orderStatus, orderSubstatus, clientInfo);
    }

    @Test
    public void changeOrderSubstatus() throws Exception {
        Order order = createPaidDropshipOrderWithCargotype(2, CIS_REQUIRED_CARGOTYPE_CODE);
        ParcelBox parcelBox = parcelBoxHelper.provideOneBoxForOrder(order);
        parcelBoxHelper.putBoxes(
                order.getId(),
                order.getDelivery().getParcels().get(0).getId(),
                Collections.singletonList(parcelBox),
                ClientHelper.shopUserClientFor(order)
        );
        orderUpdateService.validateCisesAndUpdateOrderStatus(order.getId(),
                StatusAndSubstatus.of(PROCESSING, OrderSubstatus.PACKAGING),
                ClientInfo.SYSTEM, null
        );
        ClientInfo clientInfo = ClientInfo.builder(ClientRole.SHOP).withId(order.getShopId()).build();
        assertOrderStatusUpdateNotAllowed(order.getId(), PROCESSING,
                OrderSubstatus.READY_TO_SHIP, clientInfo);
        order = orderService.getOrder(order.getId());
        long itemId = order.getItems().iterator().next().getId();
        mockMvc.perform(put("/orders/{orderId}/items/instances", order.getId())
                .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                .param(CheckouterClientParams.CLIENT_ID, "0")
                .content(createPutInstancesWithTwoCisesRequest(itemId))
                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());
        orderUpdateService.updateOrderStatus(order.getId(), PROCESSING,
                OrderSubstatus.READY_TO_SHIP);
    }

    @ParameterizedTest
    @ValueSource(ints = {CIS_REQUIRED_CARGOTYPE_CODE, CIS_DISTINCT_CARGOTYPE_CODE, CIS_OPTIONAL_CARGOTYPE_CODE})
    public void changeOrderStatus(int cargoType) throws Exception {
        Parameters parameters = DropshipDeliveryHelper.getDropshipPrepaidParameters();
        parameters.getOrders().get(0).getItems().forEach(item -> {
            item.setCount(2);
        });
        Order order = orderCreateHelper.createOrder(parameters);
        orderPayHelper.payForOrder(order);
        ParcelBox parcelBox = parcelBoxHelper.provideOneBoxForOrder(order);
        parcelBoxHelper.putBoxes(
                order.getId(),
                order.getDelivery().getParcels().get(0).getId(),
                Collections.singletonList(parcelBox),
                ClientHelper.shopUserClientFor(order)
        );
        orderUpdateService.updateOrderStatus(order.getId(), PROCESSING,
                OrderSubstatus.SHIPPED);
        parameters.getOrders().get(0).getItems().forEach(item -> {
            item.setCargoTypes(Collections.singleton(cargoType));
        });
        order = orderCreateHelper.createOrder(parameters);
        orderPayHelper.payForOrder(order);
        ClientInfo clientInfo = ClientInfo.builder(ClientRole.SHOP).withId(order.getShopId()).build();
        if (cargoType == CIS_REQUIRED_CARGOTYPE_CODE) {
            assertOrderStatusUpdateNotAllowed(order.getId(), PROCESSING, OrderSubstatus.READY_TO_SHIP, clientInfo);
        } else {
            assertOrderStatusUpdateAllowed(order.getId(), PROCESSING, OrderSubstatus.READY_TO_SHIP, clientInfo);
        }
    }

    @Test
    public void testStatusUpdateBulkCisesNotLoaded() throws Exception {
        Parameters parameters = DropshipDeliveryHelper.getDropshipPrepaidParameters();
        parameters.getOrders().get(0).getItems().forEach(item -> {
            item.setCount(2);
            item.setCargoTypes(Collections.singleton(CIS_REQUIRED_CARGOTYPE_CODE));
        });
        Order order1 = orderCreateHelper.createOrder(parameters);
        orderPayHelper.payForOrder(order1);
        parameters.getOrders().get(0).getItems().forEach(item -> {
            item.setCount(2);
            item.setCargoTypes(Collections.singleton(CIS_REQUIRED_CARGOTYPE_CODE + 1));
        });
        Order order2 = orderCreateHelper.createOrder(parameters);
        orderPayHelper.payForOrder(order2);
        List<Long> orderIds = Arrays.asList(order1.getId(), order2.getId());

        ParcelBox parcelBox1 = parcelBoxHelper.provideOneBoxForOrder(order1);
        parcelBoxHelper.putBoxes(
                order1.getId(),
                order1.getDelivery().getParcels().get(0).getId(),
                Collections.singletonList(parcelBox1),
                ClientHelper.shopUserClientFor(order1)
        );
        ParcelBox parcelBox2 = parcelBoxHelper.provideOneBoxForOrder(order2);
        parcelBoxHelper.putBoxes(
                order2.getId(),
                order2.getDelivery().getParcels().get(0).getId(),
                Collections.singletonList(parcelBox2),
                ClientHelper.shopUserClientFor(order2)
        );
        ClientInfo clientInfo = ClientInfo.builder(ClientRole.SHOP).withShopId(order1.getShopId()).build();
        List<OrderStatusUpdateResult> result = orderUpdateService.updateOrderStatusBulk(
                orderIds,
                StatusAndSubstatus.of(PROCESSING, OrderSubstatus.READY_TO_SHIP),
                clientInfo, true);

        assertThat(result.get(0).isUpdated(), equalTo(false));
        assertThat(result.get(0).getErrorDetails(), notNullValue());

        assertThat(result.get(1).isUpdated(), equalTo(true));
        assertThat(result.get(1).getErrorDetails(), nullValue());
    }

    @Test
    public void instancesInOutput() throws Exception {
        Order order = createPaidDropshipOrderWithCargotype(2, CIS_REQUIRED_CARGOTYPE_CODE);
        long itemId = order.getItems().iterator().next().getId();
        MvcResult result = mockMvc.perform(put("/orders/{orderId}/items/instances", order.getId())
                .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                .param(CheckouterClientParams.CLIENT_ID, "0")
                .content(createPutInstancesWithTwoCisesRequest(itemId))
                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andReturn();
        OrderItemViewModel response = serializationService.deserializeCheckouterObject(
                result.getResponse().getContentAsString(), OrderItemsViewModel.class)
                .getContent()
                .iterator()
                .next();
        JsonNode instances = response.getInstances();
        Assertions.assertEquals("[{\"cis\":\"" + cis1 + "\",\"balanceOrderId\":\"" + order.getId()
                        + "-item-" + itemId + "-1\"},{\"cis\":\"" + cis2 + "\",\"balanceOrderId\":\"" +
                        order.getId() + "-item-" + itemId + "-2\"}]",
                instances.toString());
        result = mockMvc.perform(get("/orders/{orderId}", order.getId())
                .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                .param(CheckouterClientParams.CLIENT_ID, "0")
                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andReturn();
        OrderViewModel orderResponse = serializationService.deserializeCheckouterObject(
                result.getResponse().getContentAsString(), OrderViewModel.class);
        instances = orderResponse
                .getItems()
                .iterator().next()
                .getInstances();
        Assertions.assertEquals("[{\"cis\":\"" + cis1 + "\",\"balanceOrderId\":\"" + order.getId()
                        + "-item-" + itemId + "-1\"},{\"cis\":\"" + cis2 + "\",\"balanceOrderId\":\"" + order.getId()
                        + "-item-" + itemId +
                        "-2\"}]",
                instances.toString());
        Assertions.assertNotNull(orderService.getOrder(order.getId())
                .getItems().iterator().next().getBalanceOrderId());
    }

    @Test
    public void instancesInOutput2() throws Exception {
        Order order = createPaidDropshipOrderWithCargotype(2, CIS_REQUIRED_CARGOTYPE_CODE);
        long itemId = order.getItems().iterator().next().getId();
        MvcResult result = mockMvc.perform(put("/orders/{orderId}/items/{itemId}/instances", order.getId(),
                itemId)
                .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                .param(CheckouterClientParams.CLIENT_ID, "0")
                .content("{\"instances\":" + createTwoCises() + "}")
                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andReturn();
        OrderItemViewModel response = serializationService.deserializeCheckouterObject(
                result.getResponse().getContentAsString(), OrderItemViewModel.class);
        JsonNode instances = response.getInstances();
        Assertions.assertEquals("[{\"cis\":\"" + cis1 + "\",\"balanceOrderId\":\"" + order.getId()
                        + "-item-" + itemId + "-1\"},{\"cis\":\"" + cis2 + "\"," +
                        "\"balanceOrderId\":\"" + order.getId() + "-item-" + itemId + "-2\"}]",
                instances.toString());
        result = mockMvc.perform(get("/orders/{orderId}", order.getId())
                .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                .param(CheckouterClientParams.CLIENT_ID, "0")
                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andReturn();
        OrderViewModel orderResponse = serializationService.deserializeCheckouterObject(
                result.getResponse().getContentAsString(), OrderViewModel.class);
        instances = orderResponse
                .getItems()
                .iterator().next()
                .getInstances();
        Assertions.assertEquals("[{\"cis\":\"" + cis1 + "\",\"balanceOrderId\":\"" + order.getId()
                        + "-item-" + itemId + "-1\"},{\"cis\":\"" + cis2 + "\",\"balanceOrderId\":\"" + order.getId()
                        + "-item-" + itemId +
                        "-2\"}]",
                instances.toString());
    }

    @Test
    @DisplayName("Добавляются три киза - два из них дубли")
    public void putCisesToItemWithDuplicate() throws Exception {
        Order order = createPaidDropshipOrderWithCargotype(2, CIS_REQUIRED_CARGOTYPE_CODE);
        long itemId = order.getItems().iterator().next().getId();
        mockMvc.perform(put("/orders/{orderId}/items/{itemId}/instances", order.getId(),
                itemId)
                .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                .param(CheckouterClientParams.CLIENT_ID, "0")
                .content("{\"instances\":" + toNode(
                        new JsonInstance(cis1),
                        new JsonInstance(cis1),
                        new JsonInstance(cis2))
                        + "}")
                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Добавление кизов вместе с uit, imei и sn")
    public void putCisesWithUit() throws Exception {
        Order order = createPaidDropshipOrderWithCargotype(2, CIS_REQUIRED_CARGOTYPE_CODE);
        long itemId = order.getItems().iterator().next().getId();
        MvcResult result = mockMvc.perform(put("/orders/{orderId}/items/{itemId}/instances", order.getId(),
                itemId)
                .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                .param(CheckouterClientParams.CLIENT_ID, "0")
                .content("{\"instances\":" + toNode(new JsonInstance(cis1, uit1, null, imei1, sn1)) + "}")
                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andReturn();
        OrderItemViewModel response = serializationService.deserializeCheckouterObject(
                result.getResponse().getContentAsString(), OrderItemViewModel.class);
        JsonNode instances = response.getInstances();
        Assertions.assertEquals(toNode(new JsonInstance(cis1, uit1,
                        order.getId() + "-item-" + itemId + "-1", imei1, sn1)),
                instances.toString());
    }

    @Test
    public void instancesInOutput3() throws Exception {
        Order order = createPaidDropshipOrderWithCargotype(2, CIS_REQUIRED_CARGOTYPE_CODE);
        long itemId = order.getItems().iterator().next().getId();
        MvcResult result = mockMvc.perform(put("/orders/{orderId}/items/{itemId}/instances", order.getId(),
                itemId + 100500)
                .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                .param(CheckouterClientParams.CLIENT_ID, "0")
                .content("{\"instances\":" + createTwoCises() + "}")
                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    public void daoTest() throws Exception {
        Order order = createPaidDropshipOrderWithCargotype(2, null);
        long itemId = order.getItems().iterator().next().getId();
        order.getItem(itemId).setBalanceOrderId("11111");
        transactionTemplate.execute(ts -> {
            orderWritingDao.bindItemBalanceOrderIdAndCommissionForItems(order);
            return null;
        });
        List<OrderItem> items = orderItemDao.findOrderItemsById(Collections.singletonList(itemId));
        Assertions.assertNull(items.get(0).getInstances());
        Assertions.assertEquals("11111", items.get(0).getBalanceOrderId());
        order.getItem(itemId).setBalanceOrderId(null);
        String instancesStr = "[{\"cis\": \"010465006531553121CtPoNqNB7qOdc\"}, {\"cis\": \"010465006531553121" +
                ".tPoNqN;7qOdc\"}, {\"cisFull\": \"" + validCisFull + "\"}]";
        ArrayNode instances = (ArrayNode) mapper.readTree(instancesStr);
        order.getItem(itemId).setInstances(instances);
        transactionTemplate.execute(ts -> {
            orderWritingDao.bindItemBalanceOrderIdAndCommissionForItems(order);
            return null;
        });
        items = orderItemDao.findOrderItemsById(Collections.singletonList(itemId));
        Assertions.assertEquals(instances, items.get(0).getInstances());
    }

    @Test
    public void putInstancesToShippedFFOrder() throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.getOrder().getItems().forEach(item -> {
            item.setCargoTypes(Collections.singleton(CIS_REQUIRED_CARGOTYPE_CODE));
        });
        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, PROCESSING);
        orderUpdateService.updateOrderStatus(order.getId(), PROCESSING, OrderSubstatus.SHIPPED);

        long itemId = order.getItems().iterator().next().getId();
        mockMvc.perform(put("/orders/{orderId}/items/instances", order.getId())
                .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                .param(CheckouterClientParams.CLIENT_ID, "0")
                .content(createPutInstancesWithSingleCisRequest(itemId))
                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());
    }

    @Test
    public void balanceForInstancesInPrepaidFFOrder() throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParametersWithItems(
                OrderItemProvider.getOrderItem(), OrderItemProvider.getAnotherOrderItem());
        parameters.getOrder().getItems().forEach(item -> {
            item.setCount(2);
            item.setCargoTypes(Collections.singleton(CIS_REQUIRED_CARGOTYPE_CODE));
        });
        Order order = orderCreateHelper.createOrder(parameters);
        paymentHelper.payForOrder(order);
        long itemId = order.getItems().iterator().next().getId();
        MvcResult result = mockMvc.perform(put("/orders/{orderId}/items/{itemId}/instances", order.getId(),
                itemId)
                .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                .param(CheckouterClientParams.CLIENT_ID, "0")
                .content("{\"instances\":" + createTwoCises() + "}")
                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andReturn();
        OrderItemViewModel response = serializationService.deserializeCheckouterObject(
                result.getResponse().getContentAsString(), OrderItemViewModel.class);
        JsonNode instances = response.getInstances();
        Assertions.assertEquals("[{\"cis\":\"" + cis1 + "\",\"balanceOrderId\":\"" + order.getId()
                        + "-item-" + itemId + "-1\"},{\"cis\":\"" + cis2 + "\",\"balanceOrderId\":\"" +
                        order.getId() + "-item-" + itemId + "-2\"}]",
                instances.toString());
        result = mockMvc.perform(get("/orders/{orderId}", order.getId())
                .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                .param(CheckouterClientParams.CLIENT_ID, "0")
                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andReturn();
        OrderViewModel orderResponse = serializationService.deserializeCheckouterObject(
                result.getResponse().getContentAsString(), OrderViewModel.class);
        instances = orderResponse
                .getItems()
                .iterator().next()
                .getInstances();
        Assertions.assertEquals("[{\"cis\":\"" + cis1 + "\",\"balanceOrderId\":\"" + order.getId()
                        + "-item-" + itemId + "-1\"},{\"cis\":\"" + cis2 + "\",\"balanceOrderId\":\""
                        + order.getId() + "-item-" + itemId +
                        "-2\"}]",
                instances.toString());
    }
}
