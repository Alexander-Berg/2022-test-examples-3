package ru.yandex.market.checkout.checkouter.pay;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.market.checkout.checkouter.balance.trust.model.BasketLineMarkup;
import ru.yandex.market.checkout.checkouter.balance.trust.model.BasketMarkup;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderUpdateService;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.checkouter.viewmodel.OrderItemViewModel;
import ru.yandex.market.checkout.checkouter.viewmodel.containers.OrderItemsViewModel;
import ru.yandex.market.checkout.helpers.RefundHelper;
import ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.ORDER_CREATE_CASH_PAYMENT;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.ORDER_CREATE_OFFSET_ADVANCE_RECEIPT;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.CREATE_BASKET_STUB;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.CREATE_DELIVERY_RECEIPT_STUB;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.CREATE_REFUND_STUB;
import static ru.yandex.market.checkout.util.balance.checkers.TrustCallsChecker.getRequestBodyAsJson;

public class TrustRequestConverterTest extends AbstractPaymentTestBase {

    public static final int CIS_REQUIRED_CARGOTYPE_CODE = 980;
    public static final int CIS_DISTINCT_CARGOTYPE_CODE = 985;
    public static final int CIS_OPTIONAL_CARGOTYPE_CODE = 990;
    private final String cis1 = "010465006531553121CtPoNqNB7qOdc";
    private final String cis2 = "010465006531553121.tPoNqN;7qOdc";

    @Autowired
    protected OrderUpdateService orderUpdateService;
    @Autowired
    private TestSerializationService serializationService;
    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private RefundHelper refundHelper;
    @Autowired
    private TrustRequestConverter trustRequestConverter;

    @Test
    public void convertBasketMarkup() {
        Order order = createPaidDropshipOrderWithCargotype(3, CIS_REQUIRED_CARGOTYPE_CODE);
        OrderItem item = order.getItems().iterator().next();
        item.setPrice(new BigDecimal("19.5"));
        item.setBuyerPrice(new BigDecimal("19.5"));

        BasketMarkup markup = new BasketMarkup();
        markup
                .addBasketLineMarkup(item.getBalanceOrderId(), new BasketLineMarkup()
                        .addPaymentMethod("card", new BigDecimal("11.3"))
                        .addPaymentMethod("spasibo", new BigDecimal("47.2")));
        BasketMarkup actualMarkup = trustRequestConverter.convertBasketMarkup(Collections.singleton(order), markup);
        BasketMarkup expectedMarkup = new BasketMarkup();
        expectedMarkup
                .addBasketLineMarkup(item.getBalanceOrderId() + "-1", new BasketLineMarkup()
                        .addPaymentMethod("card", new BigDecimal("3.5"))
                        .addPaymentMethod("spasibo", new BigDecimal("16")))
                .addBasketLineMarkup(item.getBalanceOrderId() + "-3", new BasketLineMarkup()
                        .addPaymentMethod("card", new BigDecimal("4.3"))
                        .addPaymentMethod("spasibo", new BigDecimal("15.2")))
                .addBasketLineMarkup(item.getBalanceOrderId() + "-2", new BasketLineMarkup()
                        .addPaymentMethod("card", new BigDecimal("3.5"))
                        .addPaymentMethod("spasibo", new BigDecimal("16")));
        assertThat(actualMarkup.toString(), equalTo(expectedMarkup.toString()));
    }

    @ParameterizedTest
    @ValueSource(ints = {CIS_REQUIRED_CARGOTYPE_CODE, CIS_DISTINCT_CARGOTYPE_CODE, CIS_OPTIONAL_CARGOTYPE_CODE})
    public void incomeAndDeliveredReceiptRequests(int cargoType) throws Exception {
        Order order = createPaidDropshipOrderWithCargotype(2, cargoType);
        long itemId = order.getItems().iterator().next().getId();
        List<ServeEvent> createBasketEvents = trustMockConfigurer.servedEvents().stream()
                .filter(event -> event.getStubMapping().getName().equals(CREATE_BASKET_STUB))
                .collect(Collectors.toList());
        assertThat(createBasketEvents, hasSize(1));
        ServeEvent createBasketEvent = Iterables.getOnlyElement(createBasketEvents);
        JsonObject body = getRequestBodyAsJson(createBasketEvent);

        assertThat(body.get("orders").getAsJsonArray().size(), equalTo(3));
        AtomicInteger index = new AtomicInteger(1);
        Streams.stream(body.get("orders").getAsJsonArray().iterator())
                .limit(2)
                .forEach(order1 -> {
                    Assertions.assertEquals(((JsonObject) order1).get("order_id").toString(), "\"" +
                            order.getId() + "-item-" + itemId + "-" + index.getAndIncrement() + "\"");
                    Assertions.assertEquals(((JsonObject) order1).get("qty").toString(), "\"1\"");
                });

        MvcResult result = mockMvc.perform(put("/orders/{orderId}/items/instances", order.getId())
                .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                .param(CheckouterClientParams.CLIENT_ID, "0")
                .content("{\n" +
                        "    \"items\": [\n" +
                        "        {\n" +
                        "            \"id\": " + itemId + ",\n" +
                        "            \"instances\": [{\"cis\": \"" + cis1 + "\"}, {\"cis\": \"" + cis2 + "\"}]\n" +
                        "        }\n" +
                        "    ]\n" +
                        "}")
                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andReturn();
        OrderItemViewModel response = serializationService.deserializeCheckouterObject(
                result.getResponse().getContentAsString(), OrderItemsViewModel.class)
                .getContent()
                .iterator()
                .next();
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        assertTrue(queuedCallService.existsQueuedCall(ORDER_CREATE_OFFSET_ADVANCE_RECEIPT, order.getId()));

        trustMockConfigurer.resetRequests();
        queuedCallService.executeQueuedCallBatch(ORDER_CREATE_OFFSET_ADVANCE_RECEIPT);

        List<ServeEvent> createDeliveryReceipts = trustMockConfigurer.servedEvents().stream()
                .filter(event -> event.getStubMapping().getName().equals(CREATE_DELIVERY_RECEIPT_STUB))
                .collect(Collectors.toList());
        assertThat(createDeliveryReceipts, hasSize(1));
        ServeEvent createDeliveryReceipt = Iterables.getOnlyElement(createDeliveryReceipts);
        JsonObject deliveryBody = getRequestBodyAsJson(createDeliveryReceipt);
        assertThat(deliveryBody.get("orders").getAsJsonArray().size(), equalTo(3));
        AtomicInteger index1 = new AtomicInteger(1);
        Streams.stream(deliveryBody.get("orders").getAsJsonArray().iterator())
                .forEach(order1 -> {
                    if (!((JsonObject) order1).get("order_id").toString().contains("-delivery")) {
                        Assertions.assertEquals(((JsonObject) order1).get("order_id").toString(), "\"" +
                                order.getId() + "-item-" + itemId + "-" + index1.getAndIncrement() + "\"");
                        String expectedCis = index1.get() == 2 ? cis1 : cis2;
                        Assertions.assertEquals(((JsonObject) order1).get("fiscal_item_code").toString(),
                                "\"" + expectedCis + "\"");
                    }
                });
    }

    @ParameterizedTest
    @ValueSource(ints = {CIS_REQUIRED_CARGOTYPE_CODE, CIS_DISTINCT_CARGOTYPE_CODE, CIS_OPTIONAL_CARGOTYPE_CODE})
    public void incomeAndDeliveredReceiptRequestsWithCisFull(int cargoType) throws Exception {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_CIS_FULL_VALIDATION, true);

        Order order = createPaidDropshipOrderWithCargotype(2, cargoType);

        long itemId = order.getItems().iterator().next().getId();
        String cisFull1 = "0104601662000016215RNef*\\u001d93B0Ik";
        String cisFull2 = "0104650194496408215'4iRsB_JcDQ-\\u001d91EE07\\u001d" +
                "929CmJoU45/CPoiJh7p7ajYJWdze5wgGIQJsxmw9fqurc=";
        MvcResult result = mockMvc.perform(put("/orders/{orderId}/items/instances", order.getId())
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                        .param(CheckouterClientParams.CLIENT_ID, "0")
                        .content("{\n" +
                                "    \"items\": [\n" +
                        "        {\n" +
                        "            \"id\": " + itemId + ",\n" +
                        "            \"instances\": [{\"cis\": \"" + cisFull1 + "\"}," +
                        " {\"cis\": \"" + cisFull2 + "\"}]\n" +
                        "        }\n" +
                        "    ]\n" +
                        "}")
                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andReturn();
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        assertTrue(queuedCallService.existsQueuedCall(ORDER_CREATE_OFFSET_ADVANCE_RECEIPT, order.getId()));

        trustMockConfigurer.resetRequests();
        queuedCallService.executeQueuedCallBatch(ORDER_CREATE_OFFSET_ADVANCE_RECEIPT);

        List<ServeEvent> createDeliveryReceipts = trustMockConfigurer.servedEvents().stream()
                .filter(event -> event.getStubMapping().getName().equals(CREATE_DELIVERY_RECEIPT_STUB))
                .collect(Collectors.toList());
        assertThat(createDeliveryReceipts, hasSize(1));
        ServeEvent createDeliveryReceipt = Iterables.getOnlyElement(createDeliveryReceipts);
        JsonObject deliveryBody = getRequestBodyAsJson(createDeliveryReceipt);
        assertThat(deliveryBody.get("orders").getAsJsonArray().size(), equalTo(3));
        AtomicInteger index1 = new AtomicInteger(1);
        Streams.stream(deliveryBody.get("orders").getAsJsonArray().iterator())
                .forEach(order1 -> {
                    if (!((JsonObject) order1).get("order_id").toString().contains("-delivery")) {
                        Assertions.assertEquals(((JsonObject) order1).get("order_id").toString(), "\"" +
                                order.getId() + "-item-" + itemId + "-" + index1.getAndIncrement() + "\"");
                        String expectedCis = index1.get() == 2 ? cisFull1 : cisFull2;
                        Assertions.assertEquals(((JsonObject) order1).get("fiscal_item_code").toString(),
                                "\"" + expectedCis + "\"");
                    }
                });
    }

    @ParameterizedTest
    @ValueSource(ints = {CIS_REQUIRED_CARGOTYPE_CODE, CIS_DISTINCT_CARGOTYPE_CODE, CIS_OPTIONAL_CARGOTYPE_CODE})
    public void createCashPaymentRequest(int cargoType) throws Exception {
        Order order = createDropshipPostpaidOrderWithCargotype(2, cargoType);
        long itemId = order.getItems().iterator().next().getId();

        MvcResult result = mockMvc.perform(put("/orders/{orderId}/items/instances", order.getId())
                .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                .param(CheckouterClientParams.CLIENT_ID, "0")
                .content("{\n" +
                        "    \"items\": [\n" +
                        "        {\n" +
                        "            \"id\": " + itemId + ",\n" +
                        "            \"instances\": [{\"cis\": \"" + cis1 + "\"}, {\"cis\": \"" + cis2 + "\"}]\n" +
                        "        }\n" +
                        "    ]\n" +
                        "}")
                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andReturn();
        OrderItemViewModel response = serializationService.deserializeCheckouterObject(
                result.getResponse().getContentAsString(), OrderItemsViewModel.class)
                .getContent()
                .iterator()
                .next();
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        assertTrue(queuedCallService.existsQueuedCall(ORDER_CREATE_CASH_PAYMENT, order.getId()));

        trustMockConfigurer.resetRequests();
        queuedCallService.executeQueuedCallBatch(ORDER_CREATE_CASH_PAYMENT);

        List<ServeEvent> createBasketEvents = trustMockConfigurer.servedEvents().stream()
                .filter(event -> event.getStubMapping().getName().equals(CREATE_BASKET_STUB))
                .collect(Collectors.toList());
        assertThat(createBasketEvents, hasSize(1));
        ServeEvent createBasketEvent = Iterables.getOnlyElement(createBasketEvents);
        JsonObject body = getRequestBodyAsJson(createBasketEvent);

        assertThat(body.get("orders").getAsJsonArray().size(), equalTo(3));
        AtomicInteger index = new AtomicInteger(1);
        Streams.stream(body.get("orders").getAsJsonArray().iterator())
                .forEach(order1 -> {
                    if (!((JsonObject) order1).get("order_id").toString().contains("-delivery")) {
                        Assertions.assertEquals(((JsonObject) order1).get("order_id").toString(), "\"" +
                                order.getId() + "-item-" + itemId + "-" + index.getAndIncrement() + "\"");
                        Assertions.assertEquals(((JsonObject) order1).get("qty").toString(), "\"1\"");
                    }
                });
    }

    @ParameterizedTest
    @ValueSource(ints = {CIS_REQUIRED_CARGOTYPE_CODE, CIS_DISTINCT_CARGOTYPE_CODE, CIS_OPTIONAL_CARGOTYPE_CODE})
    public void createRefundRequest(int cargoType) throws Exception {
        Order order = createPaidDropshipOrderWithCargotype(2, cargoType);
        long itemId = order.getItems().iterator().next().getId();

        MvcResult result = mockMvc.perform(put("/orders/{orderId}/items/instances", order.getId())
                .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                .param(CheckouterClientParams.CLIENT_ID, "0")
                .content("{\n" +
                        "    \"items\": [\n" +
                        "        {\n" +
                        "            \"id\": " + itemId + ",\n" +
                        "            \"instances\": [{\"cis\": \"" + cis1 + "\"}, {\"cis\": \"" + cis2 + "\"}]\n" +
                        "        }\n" +
                        "    ]\n" +
                        "}")
                        .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andReturn();
        OrderItemViewModel response = serializationService.deserializeCheckouterObject(
                        result.getResponse().getContentAsString(), OrderItemsViewModel.class)
                .getContent()
                .iterator()
                .next();
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.CANCELLED);

        CheckBasketParams config = CheckBasketParams.buildDividedItems(order);
        trustMockConfigurer.mockCheckBasket(config);
        trustMockConfigurer.mockStatusBasket(config, null);

        assertTrue(queuedCallService.existsQueuedCall(CheckouterQCType.ORDER_REFUND, order.getId()));
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_CREATE_SUBSIDY_PAYMENT);
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_REFUND);

        refundHelper.proceedAsyncRefunds(order.getId());

        List<ServeEvent> createRefund = trustMockConfigurer.servedEvents().stream()
                .filter(event -> event.getStubMapping().getName().equals(CREATE_REFUND_STUB))
                .collect(Collectors.toList());
        assertThat(createRefund, hasSize(1));
        ServeEvent createRefundEvent = Iterables.getOnlyElement(createRefund);
        JsonObject refundBody = getRequestBodyAsJson(createRefundEvent);
        assertThat(refundBody.get("orders").getAsJsonArray().size(), equalTo(3));
        AtomicInteger index = new AtomicInteger(1);
        Streams.stream(refundBody.get("orders").getAsJsonArray().iterator())
                .forEach(order1 -> {
                    if (!((JsonObject) order1).get("order_id").toString().contains("-delivery")) {
                        Assertions.assertEquals(((JsonObject) order1).get("order_id").toString(), "\"" +
                                order.getId() + "-item-" + itemId + "-" + index.getAndIncrement() + "\"");
                        Assertions.assertEquals(((JsonObject) order1).get("delta_qty").toString(), "\"1\"");
                        Assertions.assertEquals(((JsonObject) order1).get("delta_amount").toString(),
                                "\"" + order.getItem(itemId).getBuyerPrice() + "\"");
                    }
                });
    }
}
