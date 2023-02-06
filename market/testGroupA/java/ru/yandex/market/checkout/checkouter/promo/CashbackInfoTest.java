package ru.yandex.market.checkout.checkouter.promo;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cashback.model.OrderCashbackInfoResponse;
import ru.yandex.market.checkout.checkouter.cashback.model.OrdersCashbackInfoResponse;
import ru.yandex.market.checkout.checkouter.cashback.model.details.CashbackDetailsGroupResponse;
import ru.yandex.market.checkout.checkouter.cashback.model.details.CashbackDetailsResponse;
import ru.yandex.market.checkout.checkouter.cashback.model.details.CashbackDetailsSuperGroupResponse;
import ru.yandex.market.checkout.checkouter.cashback.model.details.CashbackPromoAccrualStatusResponse;
import ru.yandex.market.checkout.checkouter.cashback.model.details.OrderCashbackDetailsResponse;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderEditRequest;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestType;
import ru.yandex.market.checkout.checkouter.order.changerequest.itemsremoval.ItemInfo;
import ru.yandex.market.checkout.checkouter.order.changerequest.itemsremoval.MissingItemsNotification;
import ru.yandex.market.checkout.checkouter.order.item.removalrules.OrderTotalItemsRemovalRule;
import ru.yandex.market.checkout.checkouter.order.promo.ItemPromo;
import ru.yandex.market.checkout.checkouter.order.promo.OrderPromo;
import ru.yandex.market.checkout.checkouter.order.promo.PromoDefinition;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentGoal;
import ru.yandex.market.checkout.checkouter.pay.PaymentService;
import ru.yandex.market.checkout.checkouter.pay.Refund;
import ru.yandex.market.checkout.checkouter.pay.RefundReason;
import ru.yandex.market.checkout.checkouter.pay.RefundService;
import ru.yandex.market.checkout.checkouter.pay.RefundableItem;
import ru.yandex.market.checkout.checkouter.pay.RefundableItems;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.helpers.CashbackDetailsHelper;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.RefundHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.CashbackTestProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.loyalty.LoyaltyConfigurer;
import ru.yandex.market.checkout.util.services.OrderServiceHelper;
import ru.yandex.market.loyalty.api.model.CashbackType;
import ru.yandex.market.loyalty.api.model.cashback.details.BnplStatus;
import ru.yandex.market.loyalty.api.model.cashback.details.ExternalItemCashback;
import ru.yandex.market.loyalty.api.model.cashback.details.StructuredCashback;
import ru.yandex.market.loyalty.api.model.cashback.details.StructuredCashbackGroup;
import ru.yandex.market.loyalty.api.model.cashback.details.StructuredCashbackRequest;
import ru.yandex.market.loyalty.api.model.cashback.details.StructuredCashbackRequests;
import ru.yandex.market.loyalty.api.model.cashback.details.StructuredCashbackResponse;
import ru.yandex.market.loyalty.api.model.cashback.details.StructuredCashbackResponses;
import ru.yandex.market.loyalty.api.model.cashback.details.StructuredCashbackSuperGroup;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventReason.USER_REQUESTED_REMOVE;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.USE_REFUNDS_WITHOUT_ITEMS_CHANGED_IN_CASHBACK_INFO;
import static ru.yandex.market.checkout.test.providers.OrderItemProvider.buildOrderItem;
import static ru.yandex.market.loyalty.api.model.cashback.details.StructuredCashbackPromoAccrualStatus.PENDING;
import static ru.yandex.market.loyalty.api.model.cashback.details.StructuredCashbackPromoAccrualStatus.SUCCESS;

public class CashbackInfoTest extends AbstractWebTestBase {

    private static final Gson GSON = new GsonBuilder()
            .setDateFormat("dd-MM-yyyy HH:mm:ss")
            .setPrettyPrinting().create();

    @Autowired
    private CashbackDetailsHelper cashbackDetailsHelper;
    @Autowired
    private OrderServiceHelper orderServiceHelper;
    @Autowired
    private RefundService refundService;
    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private RefundHelper refundHelper;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private OrderTotalItemsRemovalRule orderTotalItemsRemovalRule;

    @BeforeEach
    void initProperties() {
        checkouterFeatureWriter.writeValue(USE_REFUNDS_WITHOUT_ITEMS_CHANGED_IN_CASHBACK_INFO, true);
    }

    /**
     * Запрос в ручку /users/{userId}/cashback
     * Проверяет тело запроса в ручку лоялти и тело ответа на корректность передаваемых и получаемых данных
     */
    @Test
    public void testOrderCashbackDetails() throws Exception {
        Order order = OrderProvider.getBlueOrder();
        order.setFake(true);
        List<String> expectedOrderUiPromoFlags = List.of("ui1", "ui2");
        order.setProperty(OrderPropertyType.ORDER_UI_PROMO_FLAGS, expectedOrderUiPromoFlags);
        order.setBuyer(BuyerProvider.getBuyer());
        order.setBnpl(true);
        order.setPromos(List.of(
                new OrderPromo(PromoDefinition.cashbackPromo("marketPromoId1", null, null, null)),
                new OrderPromo(PromoDefinition.cashbackPromo("marketPromoId2", null, null, null)),
                new OrderPromo(PromoDefinition.cashbackPromo("marketPromoId3", null, null, null)),
                new OrderPromo(PromoDefinition.cashbackPromo("marketPromoId4", null, null, null))
        ));
        OrderItem firstItem = buildOrderItem("1", 5);
        OrderItem secondItem = buildOrderItem("2", 1);
        firstItem.setPromos(Set.of(
                ItemPromo.cashbackPromo(
                        BigDecimal.TEN,
                        "marketPromoId1",
                        null,
                        null,
                        null,
                        "cms1",
                        "group1",
                        List.of("1"),
                        null,
                        null,
                        null
                ),
                ItemPromo.cashbackPromo(
                        BigDecimal.ONE,
                        "marketPromoId2",
                        null,
                        null,
                        null,
                        "cms2",
                        "group2",
                        List.of("2"),
                        null,
                        null,
                        null
                ),
                ItemPromo.cashbackPromo(BigDecimal.valueOf(2), null, null)
        ));
        secondItem.setPromos(Set.of(
                ItemPromo.cashbackPromo(
                        BigDecimal.valueOf(100),
                        "marketPromoId3",
                        null,
                        null,
                        null,
                        "cms3",
                        "group3",
                        List.of("3"),
                        null,
                        null,
                        null
                ),
                ItemPromo.cashbackPromo(
                        BigDecimal.valueOf(300),
                        "marketPromoId4",
                        null,
                        null,
                        null,
                        "cms4",
                        "group4",
                        List.of("4", "5"),
                        null,
                        null,
                        null
                ),
                ItemPromo.cashbackPromo(BigDecimal.valueOf(500), null, "fakePromoId")
        ));
        //ExternalItemCashback содержит marketPromoId
        // и кол-во баллов, умноженное на количество по каждой товарной позиции(accrualAmount * count)
        Map<String, ExternalItemCashback> expectedItemCashback = Map.of(
                "marketPromoId1", new ExternalItemCashback("marketPromoId1", BigDecimal.valueOf(50), "cms1",
                        "group1", List.of("1"), PENDING),
                "marketPromoId2", new ExternalItemCashback("marketPromoId2", BigDecimal.valueOf(5), "cms2",
                        "group2", List.of("2"), PENDING),
                "marketPromoId3", new ExternalItemCashback("marketPromoId3", BigDecimal.valueOf(100), "cms3",
                        "group3", List.of("3"), PENDING),
                "marketPromoId4", new ExternalItemCashback("marketPromoId4", BigDecimal.valueOf(300), "cms4",
                        "group4", List.of("4", "5"), PENDING)
        );
        order.setItems(List.of(firstItem, secondItem));
        orderServiceHelper.saveOrder(order);
        List<String> uiPromoFlags = List.of("ui1, ui2");
        List<String> uiGroup = List.of("uiGroup");
        List<String> promoKey = List.of("promoKey");
        List<String> superGroupKeys = List.of("superGroupKeys");
        List<String> superGroupUi = List.of("superGroupUi");
        loyaltyConfigurer.mockCashbackDetails(new StructuredCashbackResponses(
                List.of(new StructuredCashbackResponse(1L, null,
                        BigDecimal.TEN,
                        new StructuredCashback(
                                uiPromoFlags,
                                List.of(new StructuredCashbackGroup("groupKey", "groupName", BigDecimal.ONE,
                                        promoKey, uiGroup, "default-cashback", SUCCESS
                                )),
                                List.of(new StructuredCashbackSuperGroup("superGroupKey", "superGroupName",
                                        BigDecimal.ZERO, superGroupKeys, superGroupUi, "desc"
                                ))
                        ), PENDING
                )), null
        ));
        OrderCashbackDetailsResponse cashbackDetails = cashbackDetailsHelper.getOrderCashbackDetails(
                Collections.singletonList(order), order.getBuyer().getUid()
        ).getOrders().get(0);
        List<ServeEvent> serveEvents = loyaltyConfigurer.servedEvents();
        StructuredCashbackRequests structuredCashbackRequests =
                GSON.fromJson(serveEvents.get(0).getRequest().getBodyAsString(), StructuredCashbackRequests.class);
        List<StructuredCashbackRequest> orders = structuredCashbackRequests.getOrders();
        assertThat(orders, hasSize(1));
        StructuredCashbackRequest structuredCashbackRequest = orders.get(0);
        assertThat(structuredCashbackRequest.getUiPromoFlags(), equalTo(expectedOrderUiPromoFlags));
        assertThat(structuredCashbackRequest.getBnplStatus(), equalTo(BnplStatus.ENABLED));
        List<ExternalItemCashback> itemCashback = structuredCashbackRequest.getItemCashback();
        assertThat(itemCashback, hasSize(4));
        for (ExternalItemCashback externalItemCashback : itemCashback) {
            ExternalItemCashback cashback = expectedItemCashback.get(externalItemCashback.getPromoKey());
            assertThat(externalItemCashback.getCmsSemanticId(), is(cashback.getCmsSemanticId()));
            assertThat(externalItemCashback.getDetailsGroupName(), is(cashback.getDetailsGroupName()));
            assertThat(externalItemCashback.getUiPromoFlags(), is(cashback.getUiPromoFlags()));
            assertThat(externalItemCashback.getAmount(), comparesEqualTo(cashback.getAmount()));
            assertThat(externalItemCashback.getStatus(), is(cashback.getStatus()));
        }
        assertThat(cashbackDetails.getOrderId(), is(1L));
        assertThat(cashbackDetails.getAmount(), comparesEqualTo(BigDecimal.TEN));
        CashbackDetailsResponse details = cashbackDetails.getDetails();
        assertThat(details.getUiPromoFlags(), is(uiPromoFlags));
        CashbackDetailsGroupResponse group = details.getGroups().get(0);
        assertThat(group.getAmount(), comparesEqualTo(BigDecimal.ONE));
        assertThat(group.getKey(), is("groupKey"));
        assertThat(group.getName(), is("groupName"));
        assertThat(group.getUiPromoFlags(), is(uiGroup));
        assertThat(group.getCmsSemanticId(), equalTo("default-cashback"));
        assertThat(group.getPromoKeys(), is(promoKey));
        assertThat(group.getStatus(), is(CashbackPromoAccrualStatusResponse.SUCCESS));
        CashbackDetailsSuperGroupResponse detailsSuperGroup = details.getSuperGroups().get(0);
        assertThat(detailsSuperGroup.getKey(), is("superGroupKey"));
        assertThat(detailsSuperGroup.getName(), is("superGroupName"));
        assertThat(detailsSuperGroup.getAmount(), comparesEqualTo(BigDecimal.ZERO));
        assertThat(detailsSuperGroup.getGroupKeys(), is(superGroupKeys));
        assertThat(detailsSuperGroup.getUiPromoFlags(), is(superGroupUi));
        assertThat(detailsSuperGroup.getDescription(), is("desc"));
    }

    /**
     * Запрос в ручку /users/{userId}/cashback
     * Проверяет тело запроса в ручку лоялти после до и после запроса на изменение заказа(удаление 9 айтемов из 10)
     */
    @Test
    void testLoyaltyRequestCashbackDetailsAfterEditRequest() throws Exception {
        orderTotalItemsRemovalRule.setMaxTotalPercentRemovable(BigDecimal.valueOf(99));
        OrderItem item = buildOrderItem("offer-1", 10);
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParametersWithItems(item);
        parameters.getLoyaltyParameters().setSelectedCashbackOption(CashbackType.EMIT);
        loyaltyConfigurer.mockCashbackDetailsEmptyResponse();
        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        cashbackDetailsHelper.getOrderCashbackDetails(Collections.singletonList(order), order.getBuyer().getUid());
        // создаем запрос на изменение заказа
        OrderEditRequest editRequest = new OrderEditRequest();
        editRequest.setMissingItemsNotification(
                new MissingItemsNotification(true, List.of(
                        new ItemInfo(order.getItems().iterator().next().getId(), 1, new HashSet<>())
                ), USER_REQUESTED_REMOVE, true));

        // MDB уведомляет Чекаутер о ненайденных товарах, создается ChangeRequest на удаление товаров из заказа
        notifyMissingItemsAndExpectItemsRemoval(order.getId(), editRequest, ClientRole.SYSTEM);

        cashbackDetailsHelper.getOrderCashbackDetails(Collections.singletonList(order), order.getBuyer().getUid());

        List<ServeEvent> serveEvents = loyaltyConfigurer.servedEvents();
        List<ServeEvent> loyaltyRequests = serveEvents.stream()
                .filter(event -> event.getRequest().getUrl().startsWith(LoyaltyConfigurer.URI_CASHBACK_DETAILS))
                .collect(Collectors.toList());
        assertThat(loyaltyRequests, hasSize(2));
        StructuredCashbackRequests structuredCashbackRequests =
                GSON.fromJson(loyaltyRequests.get(0).getRequest().getBodyAsString(), StructuredCashbackRequests.class);
        List<ExternalItemCashback> itemCashback = structuredCashbackRequests.getOrders().get(0).getItemCashback();
        assertThat(itemCashback, hasSize(1));
        assertThat(itemCashback.get(0).getAmount(), comparesEqualTo(new BigDecimal(100)));

        StructuredCashbackRequests structuredCashbackRequestsAfterEditRequest =
                GSON.fromJson(loyaltyRequests.get(1).getRequest().getBodyAsString(), StructuredCashbackRequests.class);
        List<ExternalItemCashback> itemCashbackAfterEditRequest = structuredCashbackRequestsAfterEditRequest.getOrders()
                .get(0)
                .getItemCashback();
        assertThat(itemCashbackAfterEditRequest, hasSize(1));
        assertThat(itemCashbackAfterEditRequest.get(0).getAmount(), comparesEqualTo(BigDecimal.TEN));
    }


    /**
     * Тест проверяет выдачу чекаутерного кб по ручке GET users/{userId}/cashbackInfo, а также статусы начислений
     * До рефанда кб по заказу равен 199.00 баллов
     * После рефанда кб по заказу равен 100.00 баллов
     * Рефанд не меняет количество айтемов в заказе
     */
    @Test
    void shouldReturnCorrectCashbackAmountAfterRefund() throws Exception {
        checkouterFeatureWriter.writeValue(USE_REFUNDS_WITHOUT_ITEMS_CHANGED_IN_CASHBACK_INFO, true);
        OrderItem firstItem = OrderItemProvider.getOrderItem();
        OrderItem secondItem = OrderItemProvider.getAnotherOrderItem();
        String offerId = secondItem.getOfferId();
        Parameters parameters = CashbackTestProvider.severalItemsCashbackParameters(firstItem, secondItem);
        parameters.getLoyaltyParameters().setSelectedCashbackOption(CashbackType.EMIT);
        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);

        Long orderId = order.getId();
        BigDecimal cashbackWithoutRefunds = new BigDecimal("199.00");
        BigDecimal cashbackAfterRefund = new BigDecimal("100.00");

        order = orderService.getOrder(orderId);

        OrdersCashbackInfoResponse ordersCashbackInfo =
                cashbackDetailsHelper.getOrdersCashbackInfo(Set.of(orderId), order.getBuyer().getUid());
        OrderCashbackInfoResponse orderCashbackInfo = ordersCashbackInfo.getOrderCashbackResponses().get(0);
        assertEquals(orderCashbackInfo.getOrderId(), orderId);
        assertEquals(orderCashbackInfo.getEmitStatus(), CashbackPromoAccrualStatusResponse.PENDING);
        assertThat(orderCashbackInfo.getEmitAmount(), comparesEqualTo(cashbackWithoutRefunds));

        createRefundOfItemByItemId(order, offerId);
        Collection<Refund> refunds = refundService.getRefunds(orderId);
        assertThat(refunds, hasSize(1));
        assertFalse(refunds.iterator().next().wereOrderItemsChanged());
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        order = orderService.getOrder(orderId);
        assertThat(order.getStatus(), equalTo(OrderStatus.DELIVERED));
        assertTrue(queuedCallService.existsQueuedCall(CheckouterQCType.ORDER_EMIT_CASHBACK, orderId));
        queuedCallService.executeQueuedCallSynchronously(CheckouterQCType.ORDER_EMIT_CASHBACK, orderId);


        OrdersCashbackInfoResponse ordersCashbackInfoAfterRefund =
                cashbackDetailsHelper.getOrdersCashbackInfo(Set.of(orderId), order.getBuyer().getUid());
        OrderCashbackInfoResponse orderCashbackInfoAfterRefund =
                ordersCashbackInfoAfterRefund.getOrderCashbackResponses().get(0);
        assertEquals(orderCashbackInfoAfterRefund.getOrderId(), orderId);
        assertEquals(orderCashbackInfoAfterRefund.getEmitStatus(), CashbackPromoAccrualStatusResponse.PENDING);
        assertThat(orderCashbackInfoAfterRefund.getEmitAmount(), comparesEqualTo(cashbackAfterRefund));

        var payments = paymentService.getPayments(Collections.singletonList(order.getId()), ClientInfo.SYSTEM,
                PaymentGoal.CASHBACK_EMIT);
        assertEquals(1, payments.size());

        orderPayHelper.notifyPaymentClear(payments.get(0));
        OrderCashbackInfoResponse ordersCashbackInfoAfterNotifyPaymentClear =
                cashbackDetailsHelper.getOrdersCashbackInfo(Set.of(orderId), order.getBuyer().getUid())
                        .getOrderCashbackResponses().get(0);
        assertEquals(ordersCashbackInfoAfterNotifyPaymentClear.getOrderId(), orderId);
        assertEquals(ordersCashbackInfoAfterNotifyPaymentClear.getEmitStatus(),
                CashbackPromoAccrualStatusResponse.SUCCESS);
        assertThat(ordersCashbackInfoAfterNotifyPaymentClear.getEmitAmount(), comparesEqualTo(cashbackAfterRefund));
    }


    /**
     * Тест проверяет выдачу суммы чекаутерного кб по ручке GET users/{userId}/cashbackInfo
     * До запроса на изменение заказа кб равен 100.00 баллов
     * После запроса на изменение заказа(удаление 9 айтемов из 10) кб равен 10.00 баллов
     * Рефанд меняет количество айтемов в заказе
     */
    @Test
    void shouldReturnCorrectCashbackAmountAfterEditRequest() throws Exception {
        orderTotalItemsRemovalRule.setMaxTotalPercentRemovable(BigDecimal.valueOf(99));
        OrderItem item = buildOrderItem("offer-1", 10);
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParametersWithItems(item);
        parameters.getLoyaltyParameters().setSelectedCashbackOption(CashbackType.EMIT);
        Order order = orderCreateHelper.createOrder(parameters);
        assertThat(order.getItems().iterator().next().getQuantity(), comparesEqualTo(BigDecimal.TEN));
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        OrderCashbackInfoResponse cashbackInfoResponse = cashbackDetailsHelper.getOrdersCashbackInfo(
                Set.of(order.getId()), order.getBuyer().getUid()
        ).getOrderCashbackResponses().get(0);
        assertThat(cashbackInfoResponse.getEmitAmount(), comparesEqualTo(new BigDecimal(100)));
        // создаем запрос на изменение заказа
        OrderEditRequest editRequest = new OrderEditRequest();
        editRequest.setMissingItemsNotification(
                new MissingItemsNotification(true, List.of(
                        new ItemInfo(order.getItems().iterator().next().getId(), 1, new HashSet<>())
                ), USER_REQUESTED_REMOVE, true));

        // MDB уведомляет Чекаутер о ненайденных товарах, создается ChangeRequest на удаление товаров из заказа
        notifyMissingItemsAndExpectItemsRemoval(order.getId(), editRequest, ClientRole.SYSTEM);
        Order orderAfterEditRequest = orderService.getOrder(order.getId());
        Collection<Refund> refunds = refundService.getRefunds(order.getId());
        assertThat(refunds, hasSize(1));
        assertTrue(refunds.iterator().next().wereOrderItemsChanged());
        assertThat(orderAfterEditRequest.getItems().iterator().next().getQuantity(), comparesEqualTo(BigDecimal.ONE));
        OrderCashbackInfoResponse cashbackInfoResponseAfterEditRequest = cashbackDetailsHelper.getOrdersCashbackInfo(
                Set.of(order.getId()), order.getBuyer().getUid()
        ).getOrderCashbackResponses().get(0);
        assertThat(cashbackInfoResponseAfterEditRequest.getEmitAmount(), comparesEqualTo(BigDecimal.TEN));
    }

    private void createRefundOfItemByItemId(Order order, String offerId) {
        final RefundableItems refundableItems = refundService.getRefundableItems(order);
        RefundableItem refundableItem = refundableItems.getItems().stream()
                .filter(item -> offerId.equals(item.getOfferId()))
                .findFirst()
                .get();
        RefundableItems itemsToRefund = refundableItems.withItems(Collections.singletonList(refundableItem));
        createRefund(order, order.getPayment(), itemsToRefund);
    }

    private void createRefund(Order order, Payment payment, RefundableItems items) {
        try {
            var refunds = refundService.createRefund(order.getId(), order.getBuyerTotal(), "Just Test",
                    ClientInfo.SYSTEM, RefundReason.ORDER_CANCELLED, payment.getType(), false, items.toRefundItems(),
                    false, null, false);
            refundHelper.proceedAsyncRefunds(refunds);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ChangeRequest notifyMissingItemsAndExpectItemsRemoval(long orderId,
                                                                  @Nonnull OrderEditRequest editRequest,
                                                                  ClientRole role) {
        List<ChangeRequest> changeRequests = client.editOrder(
                orderId, role, null, List.of(Color.BLUE), editRequest
        );

        assertEquals(1, changeRequests.size());
        ChangeRequest changeRequest = changeRequests.get(0);
        assertEquals(ChangeRequestType.ITEMS_REMOVAL, changeRequest.getType());
        return changeRequest;
    }
}
