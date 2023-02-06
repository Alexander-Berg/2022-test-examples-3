package ru.yandex.market.checkout.checkouter.pay.returns;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.collections.CollectionUtils;
import ru.yandex.common.util.collections.Pair;
import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.balance.model.NotificationMode;
import ru.yandex.market.checkout.checkouter.balance.model.notifications.TrustRefundNotification;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.event.PagedEvents;
import ru.yandex.market.checkout.checkouter.feature.type.common.IntegerFeatureType;
import ru.yandex.market.checkout.checkouter.order.ControllerUtils;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.StatusAndSubstatus;
import ru.yandex.market.checkout.checkouter.pay.AbstractPaymentTestBase;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.Refund;
import ru.yandex.market.checkout.checkouter.pay.RefundService;
import ru.yandex.market.checkout.checkouter.pay.RefundStatus;
import ru.yandex.market.checkout.checkouter.pay.ReturnService;
import ru.yandex.market.checkout.checkouter.pay.ReturnableItemsService;
import ru.yandex.market.checkout.checkouter.pay.StorageReturnService;
import ru.yandex.market.checkout.checkouter.report.Experiments;
import ru.yandex.market.checkout.checkouter.returns.IllegalReturnStatusException;
import ru.yandex.market.checkout.checkouter.returns.Return;
import ru.yandex.market.checkout.checkouter.returns.ReturnCancelReason;
import ru.yandex.market.checkout.checkouter.returns.ReturnDecision;
import ru.yandex.market.checkout.checkouter.returns.ReturnDecisionType;
import ru.yandex.market.checkout.checkouter.returns.ReturnDelivery;
import ru.yandex.market.checkout.checkouter.returns.ReturnHistory;
import ru.yandex.market.checkout.checkouter.returns.ReturnItem;
import ru.yandex.market.checkout.checkouter.returns.ReturnOptionsResponse;
import ru.yandex.market.checkout.checkouter.returns.ReturnReasonType;
import ru.yandex.market.checkout.checkouter.returns.ReturnStatus;
import ru.yandex.market.checkout.checkouter.trust.service.TrustPaymentService;
import ru.yandex.market.checkout.common.rest.InvalidRequestException;
import ru.yandex.market.checkout.common.rest.Pager;
import ru.yandex.market.checkout.helpers.EventsGetHelper;
import ru.yandex.market.checkout.helpers.OrderHistoryEventsTestHelper;
import ru.yandex.market.checkout.helpers.RefundHelper;
import ru.yandex.market.checkout.helpers.ReturnHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.ReturnProvider;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.common.report.model.MarketReportPlace;
import ru.yandex.market.common.report.model.PickupOption;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.DELIVERY;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PICKUP;
import static ru.yandex.market.checkout.checkouter.pay.compensation.ReturnItemsUtils.collapse;
import static ru.yandex.market.checkout.checkouter.returns.ReturnStatus.TERMINAL_RETURN_STATUSES;
import static ru.yandex.market.checkout.checkouter.viewmodel.NonReturnableReasonType.ALREADY_REFUNDED;
import static ru.yandex.market.checkout.providers.WhiteParametersProvider.dsbsOrderItem;
import static ru.yandex.market.checkout.providers.WhiteParametersProvider.simpleWhiteParameters;
import static ru.yandex.market.checkout.test.providers.ActualDeliveryProvider.DELIVERY_PRICE;
import static ru.yandex.market.checkout.test.providers.ActualDeliveryProvider.PICKUP_PRICE;
import static ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams.buildCheckBasketWithConfirmedRefund;


/**
 * @author : poluektov
 * date: 28.02.18.
 */
public class ReturnServiceTest extends AbstractPaymentTestBase {

    @Autowired
    private ReturnHelper returnHelper;
    @Autowired
    private ReturnService returnService;
    @Autowired
    private ReturnableItemsService returnableItemsService;
    @Autowired
    private RefundService refundService;
    @Autowired
    private EventsGetHelper eventsGetHelper;
    @Autowired
    private TrustPaymentService trustPaymentService;
    @Autowired
    protected OrderHistoryEventsTestHelper eventsTestHelper;
    @Autowired
    private RefundHelper refundHelper;
    private Return ret;

    @BeforeEach
    public void setUp() {
        freezeTimeAt("2018-01-01T00:00:00Z");
        returnHelper.mockShopInfo();
        returnHelper.mockSupplierInfo();
        mockActualDelivery();
    }

    @AfterEach
    public void tearDown() {
        clearFixed();
    }

    @Test
    public void testSimpleCreateReturn() throws Exception {
        order.set(orderServiceTestHelper.createDeliveredBlueOrder());
        addToCurrentDate(1);

        ret = returnHelper.createReturn(order.get().getId(), ReturnProvider.generateReturn(order.get()));

        assertThat(ret.getStatus(), equalTo(ReturnStatus.REFUND_IN_PROGRESS));
        assertThat(ret.hasCompensation(), is(true));
        assertThat(ret.getUserId(), is(order.get().getBuyer().getUid()));
        checkEventsDateFromEqualsCurrent(HistoryEventType.ORDER_RETURN_CREATED);
        Return returnById = returnService.findReturnById(ret.getId(), false, ClientInfo.SYSTEM);
        assertNotNull(returnableItemsService.getReturnableItems(order.get().getId(), ClientInfo.SYSTEM));
    }

    @Test
    public void returnCreatedFromPickupUserReceived() throws Exception {
        Order testOrder = orderServiceTestHelper.createPickupBlueOrder();
        orderStatusHelper.updateOrderStatus(testOrder.getId(), PICKUP, OrderSubstatus.PICKUP_USER_RECEIVED);
        testOrder = orderService.getOrder(testOrder.getId());
        assertThat(testOrder.getStatus(), is(PICKUP));
        assertThat(testOrder.getSubstatus(), is(OrderSubstatus.PICKUP_USER_RECEIVED));
        order.set(testOrder);
        addToCurrentDate(1);
        ret = returnHelper.createReturn(order.get().getId(), ReturnProvider.generateReturn(order.get()));
        assertThat(ret.getStatus(), equalTo(ReturnStatus.REFUND_IN_PROGRESS));
        assertThat(ret.hasCompensation(), is(true));
        checkEventsDateFromEqualsCurrent(HistoryEventType.ORDER_RETURN_CREATED);
    }

    @Test
    public void exceptionWhenReturnCreatedFromDefaultPickupOrderState() {
        Order testOrder = orderServiceTestHelper.createPickupBlueOrder();
        assertThat(testOrder.getStatus(), is(PICKUP));
        assertThat(testOrder.getSubstatus(), is(OrderSubstatus.PICKUP_SERVICE_RECEIVED));
        order.set(testOrder);
        addToCurrentDate(1);
        Assertions.assertThrows(InvalidRequestException.class, () -> ret =
                returnHelper.createReturn(order.get().getId(), ReturnProvider.generateReturn(order.get())));
    }

    @Test
    public void testPostpaidOfflineReturnRefundProcessing() throws Exception {
        order.set(orderServiceTestHelper.createDeliveredBluePostPaidOrder());
        Return requestBody = ReturnProvider.generateReturn(order.get());
        requestBody.setDelivery(ReturnProvider.getDefaultReturnDelivery());
        ReturnHelper.addDeliveryItemToRequest(requestBody);

        addToCurrentDate(1);
        //Пользователь всегда создает возврат с флагом false или null
        requestBody.setPayOffline(false);
        ret = returnHelper.initReturn(order.get().getId(), requestBody);
        checkEventsDateFromEqualsCurrent(HistoryEventType.ORDER_RETURN_CREATED);

        addToCurrentDate(1);
        //Оператор выставил payOffline=true
        ret.setPayOffline(true);
        ret = returnHelper.resumeReturn(order.get().getId(), ret.getId(), ret);
        refundHelper.proceedAsyncRefunds(order.get().getId());
        assertThat(ret.getPayOffline(), equalTo(true));
        checkEventsDateFromEqualsCurrent(HistoryEventType.ORDER_RETURN_STATUS_UPDATED);

        addToCurrentDate(1);
        returnService.processReturnPayments(order().getId(), ret.getId(), ClientInfo.SYSTEM);
        checkEventsDateFromEqualsCurrent(HistoryEventType.RECEIPT_GENERATED, HistoryEventType.NEW_COMPENSATION);
        addToCurrentDate(1);

        refundHelper.proceedAsyncRefunds(order.get().getId());
        refundService.getReturnRefunds(ret).forEach(this::notifyRefundReceipts);
        returnService.processReturnPayments(order().getId(), ret.getId(), ClientInfo.SYSTEM);
        refundTestHelper.checkPostpaidRefundsSuccess(ret);
        checkEventsDateFromEqualsCurrent(HistoryEventType.ORDER_RETURN_STATUS_UPDATED);
    }

    @Test
    public void testReturnRefundInDraftStatusWithFailedTrust() throws Exception {
        order.set(orderServiceTestHelper.createDeliveredBlueOrder());

        //Вернет 400 при походе в /refunds траста
        trustMockConfigurer.mockBadRequestRefund();

        Return requestBody = ReturnProvider.generateReturn(order.get());
        requestBody.setDelivery(ReturnProvider.getDefaultReturnDelivery());
        ReturnHelper.addDeliveryItemToRequest(requestBody);

        addToCurrentDate(1);
        //Пользователь всегда создает возврат с флагом false или null
        requestBody.setPayOffline(false);
        ret = returnHelper.initReturn(order.get().getId(), requestBody);
        checkEventsDateFromEqualsCurrent(HistoryEventType.ORDER_RETURN_CREATED);

        addToCurrentDate(1);
        //Оператор выставил payOffline=true
        ret.setPayOffline(true);
        ret = returnHelper.resumeReturn(order.get().getId(), ret.getId(), ret);
        assertThat(ret.getPayOffline(), equalTo(true));
        checkEventsDateFromEqualsCurrent(HistoryEventType.ORDER_RETURN_STATUS_UPDATED);

        addToCurrentDate(1);
        returnService.processReturnPayments(order.get(), ret);

        Optional<Refund> draftRefund = refundService.getRefunds(order.get().getId()).stream().findFirst();
        assertTrue(draftRefund.isPresent());
        assertThat(draftRefund.get().getStatus(), is(RefundStatus.DRAFT));
        assertNull(draftRefund.get().getTrustRefundKey().getTrustRefundId());
        if (refundHelper.isAsyncRefundStrategyEnabled(draftRefund.get())) {
            assertThrows(
                    RuntimeException.class,
                    () -> refundHelper.proceedAsyncRefund(draftRefund.get()),
                    "Refund processing is not finished. Refund status: DRAFT");
        }

        //Включаем создание рефанда в трасте
        trustMockConfigurer.mockCreateRefund(null);

        addToCurrentDate(1);
        returnService.processReturnPayments(order.get(), ret);
        refundHelper.proceedAsyncRefund(draftRefund.get());
        Optional<Refund> actualRefund = refundService.getReturnRefunds(ret).stream().findFirst();
        assertTrue(actualRefund.isPresent());
        assertThat(actualRefund.get().getStatus(), is(RefundStatus.ACCEPTED));
        assertNotNull(actualRefund.get().getTrustRefundKey().getTrustRefundId());
    }

    @Test
    public void testCompensationSumChange() throws Exception {
        order.set(orderServiceTestHelper.createDeliveredBluePostPaidOrder());
        Return requestBody = ReturnProvider.generateReturn(order.get());
        requestBody.setDelivery(ReturnProvider.getDefaultReturnDelivery());
        ReturnHelper.addDeliveryItemToRequest(requestBody);

        addToCurrentDate(1);
        ret = returnHelper.initReturn(order.get().getId(), requestBody);
        checkEventsDateFromEqualsCurrent(HistoryEventType.ORDER_RETURN_CREATED);

        addToCurrentDate(1);
        Return retChanges = new Return();
        retChanges.setItems(ret.getItems());
        // Нельзя завязываться на порядок следования записей без явного указания сортировки!
        ReturnItem firstItem = retChanges.getItems().stream()
                .filter(i -> Objects.nonNull(i.getItemId()))
                .findFirst()
                .orElseThrow(RuntimeException::new);
        firstItem.setSupplierCompensation(new BigDecimal(100.0));

        ret = returnHelper.resumeReturn(order.get().getId(), ret.getId(), retChanges);
        checkEventsDateFromEqualsCurrent(HistoryEventType.ORDER_RETURN_STATUS_UPDATED);

        Optional<ReturnItem> updatedItem = ret.getItems().stream().filter(i -> Objects.equals(i.getItemId(),
                firstItem.getItemId())).findFirst();
        assertTrue(updatedItem.isPresent());
        assertThat(updatedItem.get().getSupplierCompensation(), comparesEqualTo(firstItem.getSupplierCompensation()));
    }

    @Test
    public void testReturnRefundDecisionMadeStatus() throws Exception {
        order.set(orderServiceTestHelper.createDeliveredBluePostPaidOrder());
        Return requestBody = ReturnProvider.generateReturn(order.get());
        requestBody.setDelivery(ReturnProvider.getDefaultReturnDelivery());
        ReturnHelper.addDeliveryItemToRequest(requestBody);

        addToCurrentDate(1);
        ret = returnHelper.initReturn(order.get().getId(), requestBody);
        checkEventsDateFromEqualsCurrent(HistoryEventType.ORDER_RETURN_CREATED);
        returnService.updateReturnItemsDecisions(order.get().getId(), ret.getId(),
                ret.getItems().stream()
                        .map(item -> new ReturnDecision(item.getId(), ReturnDecisionType.REFUND_MONEY, ""))
                        .collect(Collectors.toUnmodifiableList()),
                new ClientInfo(ClientRole.SHOP_USER, 123L));
        ret = returnService.updateReturnStatus(order.get().getId(), ret.getId(),
                ReturnStatus.DECISION_MADE, null, new ClientInfo(ClientRole.SHOP_USER, 123L));

        addToCurrentDate(1);
        ret = returnHelper.resumeReturn(order.get().getId(), ret.getId(), ret);
        refundHelper.proceedAsyncRefunds(order.get().getId());
        checkEventsDateFromEqualsCurrent(HistoryEventType.ORDER_RETURN_STATUS_UPDATED);

        addToCurrentDate(1);
        returnService.processReturnPayments(order().getId(), ret.getId(), ClientInfo.SYSTEM);
        checkEventsDateFromEqualsCurrent(HistoryEventType.RECEIPT_GENERATED, HistoryEventType.NEW_COMPENSATION);
        addToCurrentDate(1);

        refundHelper.proceedAsyncRefunds(order.get().getId());
        refundService.getReturnRefunds(ret).forEach(this::notifyRefundReceipts);
        returnService.processReturnPayments(order().getId(), ret.getId(), ClientInfo.SYSTEM);
        refundTestHelper.checkPostpaidRefundsSuccess(ret);
        checkEventsDateFromEqualsCurrent(HistoryEventType.ORDER_RETURN_STATUS_UPDATED);
    }

    @Test
    public void testPostPaidReturnProcessing() throws Exception {
        checkouterFeatureWriter.writeValue(IntegerFeatureType.ORDER_ID_BEFORE_SPLIT_ITEMS, -1);
        order.set(orderServiceTestHelper.createDeliveredBluePostPaid1POrder());

        Return r = ReturnProvider.generateReturnWithDelivery(order.get(), order().getDelivery().getDeliveryServiceId());
        this.ret = returnHelper.createReturn(order.get().getId(), r);

        trustMockConfigurer.resetRequests();
        returnService.processReturnPayments(order().getId(), this.ret.getId(), ClientInfo.SYSTEM);
        refundHelper.proceedAsyncRefunds(this.ret);
        refundTestHelper.checkRefunds(this.ret);
        returnTestHelper.checkReturnBallanceCalls(this.ret);
    }

    @Test
    public void testPostPaidReturnRefundProcessing() throws Exception {
        order.set(orderServiceTestHelper.createDeliveredBluePostPaidOrder());
        ret = returnHelper.createReturn(order.get().getId(), ReturnProvider.generateReturn(order.get()));
        trustMockConfigurer.resetRequests();
        returnService.createAndDoRefunds(ret, order());
        refundHelper.proceedAsyncRefunds(order.get().getId());
        refundTestHelper.checkRefunds(ret);
        checkEventsDateFromEqualsCurrent(HistoryEventType.CASH_REFUND);
        refundService.getReturnRefunds(ret).forEach(this::notifyRefundReceipts);
        returnService.processReturnPayments(order().getId(), ret.getId(), ClientInfo.SYSTEM);
        refundTestHelper.checkPostpaidRefundsSuccess(ret);
    }

    @Test
    public void testPostPaidReturnNotifications() {
        order.set(orderServiceTestHelper.createDeliveredBluePostPaidOrder());
        ret = returnHelper.createReturn(order.get().getId(), ReturnProvider.generateReturn(order.get()));
        tmsTaskHelper.runProcessReturnPaymentsPartitionTaskV2();
        refundHelper.proceedAsyncRefunds(order.get().getId());
        Collection<Refund> refunds = refundService.getReturnRefunds(ret);
        assertThat(refunds.isEmpty(), equalTo(false));
        assertTrue(refunds.stream().allMatch(r -> r.getStatus() == RefundStatus.ACCEPTED));
        assertThat(refunds.size(), equalTo(1));

        tmsTaskHelper.runProcessReturnPaymentsPartitionTaskV2();
        refunds.forEach(r -> {
            trustMockConfigurer.mockCheckBasket(buildCheckBasketWithConfirmedRefund(r
                    .getTrustRefundId(), r.getAmount()));
            trustMockConfigurer.mockStatusBasket(buildCheckBasketWithConfirmedRefund(r
                    .getTrustRefundId(), r.getAmount()), null);
        });

        tmsTaskHelper.runProcessReturnPaymentsPartitionTaskV2();
        refunds = refundService.getReturnRefunds(ret);
        assertTrue(refunds.stream().allMatch(r -> r.getStatus() == RefundStatus.WAIT_FOR_NOTIFICATION));
        refunds.forEach(this::notifyRefundReceipts);

        refunds = refundService.getReturnRefunds(ret);
        assertTrue(refunds.stream().allMatch(r -> r.getStatus() == RefundStatus.SUCCESS));
    }

    private void notifyRefundReceipts(Refund r) {
        refundService.notifyRefund(new TrustRefundNotification(
                NotificationMode.receipt, r.getTrustRefundId(), "success", false,
                trustPaymentService.getReceiptUrl(r.getTrustRefundId(), r.getTrustRefundId())));
    }

    @Test
    public void testPrepaidReturnCompensationProcessing() throws Exception {
        order.set(orderServiceTestHelper.createDeliveredBlueOrder());
        ret = returnHelper.createReturn(order.get().getId(), ReturnProvider.generateReturn(order.get()));
        trustMockConfigurer.resetRequests();
        returnService.processCompensation(ret, order());
        returnTestHelper.checkCompensations(ret);
    }

    @Test
    public void testProcessReturnPaymentsIds() throws Exception {
        order.set(orderServiceTestHelper.createDeliveredBlueOrder());
        ret = returnHelper.createReturn(order().getId(), ReturnProvider.generateReturn(order.get()));
        trustMockConfigurer.resetRequests();
        returnService.processReturnPayments(order().getId(), ret.getId(), ClientInfo.SYSTEM);
        refundHelper.proceedAsyncRefunds(ret);
        refundTestHelper.checkRefunds(ret);
        returnTestHelper.checkReturnBallanceCalls(ret);
    }

    @Test
    public void testProcessReturnPaymentsEntities() throws Exception {
        order.set(orderServiceTestHelper.createDeliveredBlueOrder());
        ret = returnHelper.createReturn(order().getId(), ReturnProvider.generateReturn(order.get()));
        trustMockConfigurer.resetRequests();
        returnService.processReturnPayments(order(), ret);
        refundHelper.proceedAsyncRefunds(ret);
        refundTestHelper.checkRefunds(ret);
        returnTestHelper.checkReturnBallanceCalls(ret);
    }

    @Test
    public void testPrepaidReturnRefundProcessing() {
        order.set(orderServiceTestHelper.createDeliveredBlueOrder());
        ret = returnHelper.createReturn(order.get().getId(), ReturnProvider.generateReturn(order.get()));
        trustMockConfigurer.resetRequests();
        returnService.createAndDoRefunds(ret, order());
        refundTestHelper.checkRefunds(ret);
    }

    @Test
    public void testRefundAmountWithSimilarReturnItemsGrouping() {
        order.set(orderServiceTestHelper.createDeliveredBlueOrderWithCount(5));
        List<OrderItem> items = new ArrayList<>(order.get().getItems());
        assertTrue(items.get(0).getCount() > 1);
        ret = ReturnProvider.generateReturn(order.get());
        ReturnItem item = ret.getItems().get(0);
        //раскидываем первый товар по двам каунтам для того, чтобы посмотреть как суммируется потом refund
        int count = item.getCount() - 1;
        ReturnItem firstPartOfItems = ReturnItem
                .initReturnOrderItem(item.getItemId(), ReturnReasonType.BAD_QUALITY, count, BigDecimal.valueOf(count));
        firstPartOfItems.setSupplierCompensation(item.getSupplierCompensation());
        firstPartOfItems.setReasonType(ReturnReasonType.BAD_QUALITY);
        firstPartOfItems.setReturnReason("First Reason");
        ReturnItem secondPartOfItems = ReturnItem
                .initReturnOrderItem(item.getItemId(), ReturnReasonType.BAD_QUALITY, 1, BigDecimal.ONE);
        secondPartOfItems.setSupplierCompensation(item.getSupplierCompensation());
        secondPartOfItems.setReasonType(ReturnReasonType.BAD_QUALITY);
        secondPartOfItems.setReturnReason("Second Reason");
        ret.setItems(Arrays.asList(firstPartOfItems, secondPartOfItems));
        ret = returnHelper.createReturn(order.get().getId(), ret);

        BigDecimal expectedRefundAmount =
                items.get(0).getBuyerPrice().multiply(BigDecimal.valueOf(items.get(0).getCount()));

        returnService.createAndDoRefunds(ret, order());
        List<Refund> refunds = new ArrayList<>(refundHelper.proceedAsyncRefunds(ret));

        assertTrue(CollectionUtils.isNonEmpty(refunds));
        assertEquals(1, refunds.size());
        assertEquals(expectedRefundAmount, refunds.get(0).getAmount());
        assertTrue(refunds.stream().allMatch(r -> r.getStatus() == RefundStatus.ACCEPTED));
    }

    @Test
    public void distinctByKeyTest() {
        List<ReturnDelivery> options = new ArrayList<>();
        ReturnDelivery retDelivery1 = new ReturnDelivery();
        retDelivery1.setType(DeliveryType.DELIVERY);
        retDelivery1.setDeliveryServiceId(123L);
        options.add(retDelivery1);
        ReturnDelivery retDelivery2 = new ReturnDelivery();
        retDelivery2.setType(DeliveryType.DELIVERY);
        retDelivery2.setDeliveryServiceId(124L);
        options.add(retDelivery2);
        ReturnDelivery retDelivery3 = new ReturnDelivery();
        retDelivery3.setType(DeliveryType.DELIVERY);
        retDelivery3.setDeliveryServiceId(123L);
        options.add(retDelivery3);
        ReturnDelivery retDelivery4 = new ReturnDelivery();
        retDelivery4.setType(DeliveryType.POST);
        retDelivery4.setDeliveryServiceId(123L);
        options.add(retDelivery4);
        ReturnDelivery retDelivery5 = new ReturnDelivery();
        retDelivery5.setType(DeliveryType.DELIVERY);
        retDelivery5.setDeliveryServiceId(123L);
        options.add(retDelivery5);
        List<ReturnDelivery> result = options.stream()
                .filter(StorageReturnService.distinctByKey(delivery -> Pair.of(
                        delivery.getType(),
                        delivery.getDeliveryServiceId())
                ))
                .collect(toList());
        assertThat(result.size(), equalTo(3));
    }

    @Test
    public void duplicateItemsWithDifferentReasonTest() {
        order.set(orderServiceTestHelper.createDeliveredBlueOrderWithCount(10));
        Return r = ReturnProvider.generateReturn(order());
        r.setUserCompensationSum(BigDecimal.ZERO);
        distinctItemsByReason(r);
        ret = returnHelper.createReturn(order.get().getId(), r);
        List<Payment> payments = returnService.processCompensation(ret, order());
        assertThat(payments, hasSize(1));
        assertThat(payments.get(0).getTotalAmount(), comparesEqualTo(BigDecimal.valueOf(200)));
    }

    @Test
    public void returnItemCollapseOk() {
        ReturnItem item = collapse(List.of(
                new ReturnItem(42L, 10, BigDecimal.TEN, false, BigDecimal.TEN),
                new ReturnItem(42L, 10, BigDecimal.TEN, false, null),
                new ReturnItem(42L, 20, BigDecimal.valueOf(20), false, BigDecimal.ONE)
        ));
        assertThat(item.getItemId(), equalTo(42L));
        assertThat(item.getCount(), equalTo(40));
        assertThat(item.getQuantityIfExistsOrCount(), comparesEqualTo(BigDecimal.valueOf(40)));
        assertThat(item.isDeliveryService(), equalTo(false));
        assertThat(item.getSupplierCompensation(), equalTo(BigDecimal.TEN.add(BigDecimal.ONE)));
    }

    @Test
    public void shouldReturnCurrentReturnState() {
        order.set(orderServiceTestHelper.createDeliveredBlueOrder());

        addToCurrentDate(1);
        ret = returnHelper.createReturn(order.get().getId(), ReturnProvider.generateReturn(order.get()));
        trustMockConfigurer.resetRequests();
        addToCurrentDate(8);
        final Date updateDateOnOrderBefore = order.get().getUpdateDate();
        final ReturnStatus status = returnService.processReturnPayments(order.get(), ret);
        assertThat("Expired return should fail!", status, equalTo(ReturnStatus.FAILED));

        // После записи в историю дата должна обновиться!
        final Order updatedOrder = orderService.getOrder(order.get().getId());
        assertNotEquals(updateDateOnOrderBefore, updatedOrder.getUpdateDate());

        PagedEvents events = eventService.getPagedOrderHistoryEvents(
                order().getId(),
                Pager.atPage(1, 10),
                null,
                ControllerUtils.buildCheckpointRequest(false, ClientRole.SYSTEM),
                EnumSet.of(HistoryEventType.ORDER_RETURN_CREATED, HistoryEventType.ORDER_RETURN_STATUS_UPDATED),
                false,
                ClientInfo.SYSTEM,
                null
        );

        long lastOrderReturnHistoryEventId = events.getItems().stream()
                .filter(ohe -> ohe.getType() == HistoryEventType.ORDER_RETURN_STATUS_UPDATED)
                .mapToLong(OrderHistoryEvent::getHistoryId)
                .max()
                .orElse(-1L);

        assertThat("Should have ORDER_RETURN_STATUS_UPDATED event",
                lastOrderReturnHistoryEventId, greaterThan(-1L));

        ReturnHistory returnHistory =
                returnService.getReturnHistoryByOrderHistoryId(lastOrderReturnHistoryEventId);

        Assertions.assertEquals(ReturnStatus.FAILED, returnHistory.getStatus());
        Assertions.assertEquals(ReturnStatus.REFUND_IN_PROGRESS, returnHistory.getPrevReturn().getStatus());
    }

    @Test
    public void testCreateReturnWithUncompletedPaymentQC() {
        Order order = orderServiceTestHelper.createDeliveredBluePostPaidOrderWithoutProcessingPayment();
        returnHelper.createReturn(order.getId(), ReturnProvider.generateReturn(order));
    }

    @Test
    public void testReturnLargeSizeProperty() {
        order.set(orderServiceTestHelper.createDeliveredBlueOrder());
        mockActualDelivery(true);

        ret = returnHelper.createReturn(order.get().getId(), ReturnProvider.generateReturn(order.get()));
        Return foundReturn = returnService.findReturns(
                order.get().getId(),
                null, ClientInfo.SYSTEM,
                Pager.atPage(1, 100)
        ).getItems().iterator().next();
        ReturnOptionsResponse returnOptions = returnService.getReturnOptions(order.get().getId(), 100L,
                true, ClientInfo.SYSTEM,
                ret.getItems(), Experiments.empty(), null, null);

        assertThat(ret.isLargeSize(), is(true));
        assertThat(foundReturn.isLargeSize(), is(true));
        assertThat(returnOptions.isLargeSize(), is(true));
    }

    @Test
    public void testReturnWithStatusFilter() {
        order.set(orderServiceTestHelper.createDeliveredBlueOrder());
        mockActualDelivery(true);

        ret = ReturnProvider.generateReturn(order.get());
        ret = returnHelper.createReturn(order.get().getId(), ret);
        Return foundReturn = returnService.findReturns(
                order.get().getId(),
                new ReturnStatus[]{ReturnStatus.REFUND_IN_PROGRESS}, ClientInfo.SYSTEM,
                Pager.atPage(1, 100)
        ).getItems().iterator().next();
        ReturnOptionsResponse returnOptions = returnService.getReturnOptions(order.get().getId(), 100L,
                true, ClientInfo.SYSTEM,
                ret.getItems(), Experiments.empty(), List.of(), null);

        assertThat(ret.isLargeSize(), is(true));
        assertThat(foundReturn.isLargeSize(), is(true));
        assertThat(returnOptions.isLargeSize(), is(true));
    }

    @Test
    public void testReturnNullSizeProperty() {
        Parameters parameters = simpleWhiteParameters();
        parameters.getOrder().setItems(Collections.singletonList(dsbsOrderItem().width(null).weight(null)
                .depth(null).height(null).build()));
        Order testOrder = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(testOrder, OrderStatus.PROCESSING);
        orderUpdateService.updateOrderStatus(testOrder.getId(), StatusAndSubstatus.of(DELIVERY,
                OrderSubstatus.DELIVERY_SERVICE_RECEIVED), ClientInfo.SYSTEM);
        orderUpdateService.updateOrderStatus(testOrder.getId(), StatusAndSubstatus.of(OrderStatus.DELIVERED,
                OrderSubstatus.DELIVERED_USER_RECEIVED), ClientInfo.SYSTEM);
        order.set(testOrder);
        ret = ReturnProvider.generateReturn(order.get());
        ret = returnHelper.createReturn(order.get().getId(), ret);
        List<DeliveryType> deliveryTypes = List.of(DeliveryType.POST, DeliveryType.PICKUP, DeliveryType.DELIVERY);
        returnService.getReturnOptions(order.get().getId(), 100L,
                true, ClientInfo.SYSTEM,
                order.get().getItems().stream().map(item -> {
                    ReturnItem returnItem = new ReturnItem();
                    returnItem.setCount(item.getCount());
                    returnItem.setQuantity(item.getQuantityIfExistsOrCount());
                    returnItem.setItemId(item.getId());
                    returnItem.setReasonType(ReturnReasonType.BAD_QUALITY);
                    return returnItem;
                }).collect(Collectors.toUnmodifiableList()), Experiments.empty(), deliveryTypes, null);
        assertReportRequestHasDefaultDimensions();
    }

    @Test
    public void testReturnWithNoReturnsInSuchStatusFilter() {
        order.set(orderServiceTestHelper.createDeliveredBlueOrder());
        mockActualDelivery(true);

        ret = ReturnProvider.generateReturn(order.get());
        ret = returnHelper.createReturn(order.get().getId(), ret);
        Collection<Return> returns = returnService.findReturns(
                order.get().getId(),
                new ReturnStatus[]{ReturnStatus.REFUNDED}, ClientInfo.SYSTEM,
                Pager.atPage(1, 100)
        ).getItems();
        Assertions.assertTrue(returns.isEmpty());
    }

    @Test
    public void testMarketPartnerProperty() {
        order.set(orderServiceTestHelper.createDeliveredBlueOrder());
        ret = returnHelper.createReturn(order.get().getId(), ReturnProvider.generateReturn(order.get()));
        List<DeliveryType> deliveryTypes = List.of(DeliveryType.POST, DeliveryType.PICKUP, DeliveryType.DELIVERY);
        ReturnOptionsResponse returnOptions = returnService.getReturnOptions(order.get().getId(), null,
                true, ClientInfo.SYSTEM,
                ret.getItems(), Experiments.empty(), deliveryTypes, null);
        ReturnDelivery deliveryOption =
                returnOptions.getDeliveryOptions().stream().filter(o -> o.getType().equals(DeliveryType.DELIVERY))
                        .findFirst().orElse(null);
        ReturnDelivery pickupOption =
                returnOptions.getDeliveryOptions().stream().filter(o -> o.getType().equals(DeliveryType.PICKUP))
                        .findFirst().orElse(null);

        assertNotNull(deliveryOption);
        assertFalse(deliveryOption.isMarketPartner(), "Для доставки isMarketPartner должен быть false");
        assertNotNull(pickupOption);
        assertTrue(pickupOption.isMarketPartner(), "Для ПВЗ isMarketPartner должен быть true");
    }

    @Test
    public void shouldReturnPriceProperty() {
        order.set(orderServiceTestHelper.createDeliveredBlueOrder());
        mockActualDelivery(true);

        ret = returnHelper.createReturn(order.get().getId(), ReturnProvider.generateReturn(order.get()));
        ReturnOptionsResponse returnOptions = returnService.getReturnOptions(order.get().getId(), 100L,
                true, ClientInfo.SYSTEM, ret.getItems(), Experiments.empty(), null, null);
        assertThat(returnOptions.getDeliveryOptions(), hasItems(
                hasProperty("price", allOf(
                        hasProperty("value", is(DELIVERY_PRICE)),
                        hasProperty("currency", is(Currency.RUR)))),
                hasProperty("price", allOf(
                        hasProperty("value", is(PICKUP_PRICE)),
                        hasProperty("currency", is(Currency.RUR))))));
    }

    @Test
    @DisplayName("Отмена возврата в процессе рефанда юзером недоступна")
    public void shouldProhibitCancellationForRefunInProgress() {
        order.set(orderServiceTestHelper.createDeliveredBlueOrder());

        ret = returnHelper.createReturn(order.get().getId(), ReturnProvider.generateReturn(order.get()));
        assertEquals(ret.getStatus(), ReturnStatus.REFUND_IN_PROGRESS);
        Assertions.assertThrows(IllegalReturnStatusException.class, () -> returnService.updateReturnStatus(
                order.get().getId(), ret.getId(),
                ReturnStatus.CANCELLED, ReturnCancelReason.USER_CHANGED_MIND,
                ClientInfo.builder(ClientRole.USER).build()));
    }

    @Test
    @DisplayName("Отмена созданного возврата юзером успешна")
    public void shouldCancelCreatedReturn() {
        order.set(orderServiceTestHelper.createDeliveredBlueOrder());

        ret = returnHelper.createReturnForOrder(order.get(), (r, o) -> {
            r.setStatus(ReturnStatus.STARTED_BY_USER);
            r.setDelivery(returnHelper.getDefaultReturnDelivery());
            return r;
        });
        Return aReturn = returnService.updateReturnStatus(order.get().getId(), ret.getId(),
                ReturnStatus.CANCELLED, ReturnCancelReason.USER_CHANGED_MIND,
                ClientInfo.builder(ClientRole.USER).build());
        assertEquals(aReturn.getStatus(), ReturnStatus.CANCELLED);
        assertEquals(ReturnCancelReason.USER_CHANGED_MIND, aReturn.getCancelReason());
        assertThat(eventsTestHelper.getEventsOfType(order.get().getId(),
                        HistoryEventType.ORDER_RETURN_DELIVERY_CANCEL_REQUESTED),
                Matchers.hasSize(1));
    }

    @Test
    @DisplayName("Отмена возврата из STARTED_BY_USER успешна")
    public void shouldCancelReturnInProgressBySystem() {
        order.set(orderServiceTestHelper.createDeliveredBlueOrder());

        ret = returnHelper.initReturn(order.get().getId(), ReturnProvider.generateReturn(order.get()));
        assertEquals(ret.getStatus(), ReturnStatus.STARTED_BY_USER);
        Return aReturn = returnService.updateReturnStatus(order.get().getId(), ret.getId(),
                ReturnStatus.CANCELLED, ReturnCancelReason.USER_CHANGED_MIND,
                ClientInfo.SYSTEM);
        assertEquals(aReturn.getStatus(), ReturnStatus.CANCELLED);
    }

    @Test
    @DisplayName("Отмена возврата доставкой из термиального статуса недоступна")
    public void shouldNotCancelTerminalReturnBySystem() {
        order.set(orderServiceTestHelper.createDeliveredBlueOrder());

        ret = returnHelper.createReturnForOrder(order.get(), (r, o) -> {
            r.setStatus(ReturnStatus.REFUNDED_WITH_BONUSES);
            return r;
        });
        assertEquals(ret.getStatus(), ReturnStatus.REFUNDED_WITH_BONUSES);
        Assertions.assertThrows(IllegalReturnStatusException.class, () -> returnService.updateReturnStatus(
                order.get().getId(), ret.getId(),
                ReturnStatus.CANCELLED, ReturnCancelReason.TOO_MANY_RESCHEDULES,
                ClientInfo.builder(ClientRole.DELIVERY_SERVICE).build()));
    }

    @DisplayName("ReturnItem: count дублируется в quantity")
    @Test
    public void shouldBeTheSameQuantityCount() {
        order.set(orderServiceTestHelper.createDeliveredBlueOrderWithCount(5));
        List<OrderItem> items = new ArrayList<>(order.get().getItems());
        assertTrue(items.get(0).getCount() > 1);
        ret = ReturnProvider.generateReturn(order.get());

        ret = returnHelper.createReturn(order.get().getId(), ret);

        for (ReturnItem item : ret.getItems()) {
            assertEquals(item.getQuantity().intValue(), item.getCount());
        }
    }

    @DisplayName("Вернуть можно только еще не отправленные товары")
    @Test
    public void shouldNotAllowToReturnActiveReturnItems() {
        order.set(orderServiceTestHelper.createDeliveredBlueOrderWithCount(5));
        long userId = order.get().getBuyer().getUid();
        var userInfo = ClientInfo.createFromJson(ClientRole.USER, null, userId, null, null);
        List<OrderItem> items = new ArrayList<>(order.get().getItems());
        assertTrue(items.get(0).getCount() > 1);
        ret = ReturnProvider.generateReturnWithDelivery(order.get(), 12345L);
        ret.getItems().forEach(ri -> {
            ri.setCount(ri.getCount() - 1);
            ri.setQuantity(ri.getQuantity().subtract(BigDecimal.ONE));
        });

        var initialItems = returnableItemsService.getReturnableItems(order.get().getId(),
                userInfo);
        assertEquals(0, initialItems.getNonReturnableItems().size());
        returnHelper.mockActualDelivery(ret, order.get());
        ret = returnHelper.createReturn(order.get().getId(), ret);
        assertFalse(TERMINAL_RETURN_STATUSES.contains(ret.getStatus()));

        var itemsWithActiveReturn = returnableItemsService.getReturnableItems(order.get().getId(),
                userInfo);

        assertNotNull(itemsWithActiveReturn.getReturnableItems());
        assertTrue(itemsWithActiveReturn.getReturnableItems().stream().allMatch(ri -> ri.getCount() == 1));
        assertNotEquals(0, itemsWithActiveReturn.getNonReturnableItems().size());
    }

    @Test
    @DisplayName("Возврат без способа отправки не затрагивает счетчик дублей")
    public void returnWithoutDeliveryDoNotAffectDeduplication() {
        order.set(orderServiceTestHelper.createDeliveredBlueOrderWithCount(5));
        long userId = order.get().getBuyer().getUid();
        var userInfo = ClientInfo.createFromJson(ClientRole.USER, null, userId, null, null);
        ret = ReturnProvider.generateReturn(order.get());
        ret.setDelivery(null);
        ret.getItems().forEach(ri -> {
            ri.setCount(ri.getCount() - 1);
            ri.setQuantity(ri.getQuantity().subtract(BigDecimal.ONE));
        });

        var initialItems = returnableItemsService.getReturnableItems(order.get().getId(),
                userInfo);
        assertEquals(0, initialItems.getNonReturnableItems().size());
        ret = returnHelper.createReturn(order.get().getId(), ret);

        var itemsWithActiveReturn = returnableItemsService.getReturnableItems(order.get().getId(),
                userInfo);

        assertEquals(0, itemsWithActiveReturn.getNonReturnableItems().size());
    }

    @DisplayName("Рефанд бонусами корретно отсчитывается")
    @Test
    public void shouldCorrectlyCountAlternativelyRefundedItems() {
        order.set(orderServiceTestHelper.createDeliveredBlueOrderWithCount(5));
        long userId = order.get().getBuyer().getUid();
        var userInfo = ClientInfo.createFromJson(ClientRole.USER, null, userId, null, null);
        List<OrderItem> items = new ArrayList<>(order.get().getItems());
        assertTrue(items.get(0).getCount() > 1);
        ret = ReturnProvider.generateReturn(order.get());
        ret.getItems().forEach(ri -> {
            ri.setCount(ri.getCount() - 1);
            ri.setQuantity(ri.getQuantity().subtract(BigDecimal.ONE));
        });

        var initialItems = returnableItemsService.getReturnableItems(order.get().getId(),
                userInfo);
        assertEquals(0, initialItems.getNonReturnableItems().size());
        ret = returnHelper.createReturn(order.get().getId(), ret);
        returnService.updateReturnStatus(ret.getId(), ReturnStatus.REFUNDED_WITH_BONUSES,
                order.get(), null, ClientInfo.SYSTEM);

        var itemsWithActiveReturn = returnableItemsService.getReturnableItems(order.get().getId(),
                userInfo);

        assertNotNull(itemsWithActiveReturn.getReturnableItems());
        assertTrue(itemsWithActiveReturn.getReturnableItems().stream().allMatch(ri -> ri.getCount() == 1));
        assertNotEquals(0, itemsWithActiveReturn.getNonReturnableItems().size());
        assertThat(itemsWithActiveReturn.getNonReturnableItems(), hasItems(
                hasProperty("nonReturnableReason", is(ALREADY_REFUNDED))));
        assertEquals(4, itemsWithActiveReturn.getNonReturnableItems().stream()
                .filter(m -> m.getNonReturnableReason() == ALREADY_REFUNDED).mapToInt(r -> r.getItem().getCount())
                .findFirst().orElseThrow());

    }

    private void distinctItemsByReason(Return r) {
        ReturnItem duplicateItem = makeReturnItemDuplicate(order());
        ReturnItem originalItem = findItemByItemId(r, duplicateItem.getItemId());
        int count = originalItem.getCount() - 1;
        originalItem.setCount(count);
        originalItem.setQuantity(BigDecimal.valueOf(count));
        duplicateItem.setCount(1);
        duplicateItem.setQuantity(BigDecimal.ONE);
        originalItem.setSupplierCompensation(BigDecimal.valueOf(100));
        duplicateItem.setSupplierCompensation(BigDecimal.valueOf(100));
        duplicateItem.setReasonType(ReturnReasonType.WRONG_ITEM);
        r.getItems().add(duplicateItem);
    }

    private ReturnItem makeReturnItemDuplicate(Order order) {
        return ReturnProvider.makeReturnItemFromOrderItem(order.getItems()
                .stream()
                .filter(i -> i.getCount() > 1)
                .findAny()
                .orElseThrow(() -> new RuntimeException("No items with count > 1 in order")));
    }

    @Nonnull
    private ReturnItem findItemByItemId(Return r, Long itemId) {
        return r.getItems().stream()
                .filter(i -> Objects.equals(itemId, i.getItemId()))
                .findAny()
                .orElseThrow(() -> new RuntimeException("Unknown item"));
    }

    //Костыль: в тесте не пользуется OrderCreateHelper, как следствие нет мока на ActualDelivery из коробки.
    private void mockActualDelivery() {
        mockActualDelivery(false);
    }

    private void mockActualDelivery(boolean largeSize) {
        Parameters parameters = new Parameters();
        PickupOption pickupOption = new PickupOption();
        pickupOption.setDeliveryServiceId(321L);
        pickupOption.setMarketPartner(true);
        pickupOption.setPrice(PICKUP_PRICE);
        pickupOption.setCurrency(Currency.RUR);
        parameters.getReportParameters().setActualDelivery(
                ActualDeliveryProvider.builder()
                        .addPickup(pickupOption)
                        .addDelivery(123L)
                        .addLargeSize(largeSize)
                        .build());
        reportConfigurer.mockReportPlace(MarketReportPlace.ACTUAL_DELIVERY, parameters.getReportParameters());
    }

    private void checkEventsDateFromEqualsCurrent(HistoryEventType... eventTypes) throws Exception {
        PagedEvents orderHistoryEvents = eventsGetHelper.getOrderHistoryEvents(order.get().getId(), Integer.MAX_VALUE);
        ZonedDateTime currentDate = ZonedDateTime.ofInstant(getClock().instant(), getClock().getZone());
        for (HistoryEventType type : eventTypes) {
            assertThat("There is no eventTypes " + type + " with from_dt = " + currentDate, orderHistoryEvents
                    .getItems().stream().filter(e -> type == e.getType()).
                    anyMatch(e -> ZonedDateTime.ofInstant(e.getFromDate().toInstant(), getClock().getZone())
                            .isEqual(currentDate))
            );
        }
    }

    private void addToCurrentDate(int days) {
        jumpToFuture(days, ChronoUnit.DAYS);
    }

    private void assertReportRequestHasDefaultDimensions() {
        List<String> offersAsList = reportMock.getServeEvents().getServeEvents()
                .stream()
                .filter(se -> se.getRequest().queryParameter("offers-list").isPresent())
                .flatMap(se -> se.getRequest().queryParameter("offers-list").values().stream())
                .collect(Collectors.toList());
        assertThat("More than one offer should have a dimension", offersAsList.size() > 1);
        List<String> offerParameters = offersAsList.stream()
                .flatMap(offer -> Stream.of(offer.split(";")))
                .collect(Collectors.toList());
        List<String> dimensions = new ArrayList<>();
        offerParameters.forEach(
                param -> {
                    String[] kvArray = param.split(":");
                    if (kvArray[0].equals("d") || kvArray[0].equals("w")) {
                        dimensions.add(kvArray[1]);
                    }
                }
        );
        assertTrue(dimensions.size() > 0);
        assertTrue(dimensions.contains("10x10x10"));
        assertTrue(dimensions.contains("0.100"));
    }
}
