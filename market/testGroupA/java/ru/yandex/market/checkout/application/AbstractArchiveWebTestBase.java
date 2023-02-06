package ru.yandex.market.checkout.application;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableRecord;
import org.jooq.impl.TableImpl;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.common.util.application.EnvironmentType;
import ru.yandex.kikimr.persqueue.producer.AsyncProducer;
import ru.yandex.kikimr.persqueue.producer.transport.message.inbound.ProducerInitResponse;
import ru.yandex.kikimr.persqueue.producer.transport.message.inbound.ProducerWriteResponse;
import ru.yandex.market.checkout.checkouter.balance.model.NotificationMode;
import ru.yandex.market.checkout.checkouter.balance.model.notifications.TrustRefundNotification;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.RecipientPerson;
import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBox;
import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBoxItem;
import ru.yandex.market.checkout.checkouter.feature.type.common.ComplexFeatureType;
import ru.yandex.market.checkout.checkouter.order.BasicOrder;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderEditRequest;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.archive.ArchiveContext;
import ru.yandex.market.checkout.checkouter.order.archive.OrderArchiveService;
import ru.yandex.market.checkout.checkouter.order.archive.requests.OrderMovingDirection;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.recipient.RecipientEditRequest;
import ru.yandex.market.checkout.checkouter.order.promo.ReportPromoType;
import ru.yandex.market.checkout.checkouter.pay.AbstractPaymentTestBase;
import ru.yandex.market.checkout.checkouter.pay.PagedPayments;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentGoal;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.Refund;
import ru.yandex.market.checkout.checkouter.pay.RefundService;
import ru.yandex.market.checkout.checkouter.returns.Return;
import ru.yandex.market.checkout.checkouter.storage.ArchiveStorageManager;
import ru.yandex.market.checkout.checkouter.storage.StorageType;
import ru.yandex.market.checkout.checkouter.storage.archive.repository.OrderArchivingDao;
import ru.yandex.market.checkout.checkouter.storage.archive.repository.OrderCopyingDao;
import ru.yandex.market.checkout.checkouter.tasks.eventexport.logbroker.OrderEventsLbkxExportTask;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.checkouter.trust.service.TrustPaymentService;
import ru.yandex.market.checkout.common.tasks.ZooTask;
import ru.yandex.market.checkout.helpers.MultiPaymentHelper;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.ParcelBoxHelper;
import ru.yandex.market.checkout.helpers.ReceiptRepairHelper;
import ru.yandex.market.checkout.helpers.ReturnHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.ReturnProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams;
import ru.yandex.market.checkout.util.fulfillment.FulfillmentConfigurer;
import ru.yandex.market.checkout.util.loyalty.response.OrderItemResponseBuilder;
import ru.yandex.market.checkout.util.report.FoundOfferBuilder;
import ru.yandex.market.checkouter.jooq.Tables;
import ru.yandex.market.common.report.model.PromoDetails;
import ru.yandex.market.loyalty.api.model.ItemPromoResponse;
import ru.yandex.market.loyalty.api.model.PromoType;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.checkout.checkouter.order.Color.BLUE;
import static ru.yandex.market.checkout.checkouter.storage.util.ArchivedTableUtils.TRACKED_ARCHIVING_TABLES;
import static ru.yandex.market.checkout.checkouter.storage.util.ArchivedTableUtils.archivedTable;
import static ru.yandex.market.checkout.checkouter.storage.util.ArchivedTableUtils.archivedTableIdField;
import static ru.yandex.market.checkout.checkouter.storage.util.ArchivedTableUtils.getArchivingTableIdField;
import static ru.yandex.market.checkout.checkouter.storage.util.ArchivedTableUtils.needTrackArchivingTable;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.blueOrderWithDeliveryPromoParameters;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;
import static ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams.buildPostAuth;
import static ru.yandex.market.checkouter.jooq.Tables.DELIVERY_ADDRESS;
import static ru.yandex.market.checkouter.jooq.Tables.DELIVERY_PROMO;
import static ru.yandex.market.checkouter.jooq.Tables.DELIVERY_TRACK;
import static ru.yandex.market.checkouter.jooq.Tables.DELIVERY_TRACK_CHECKPOINT;
import static ru.yandex.market.checkouter.jooq.Tables.DELIVERY_TRACK_CHECKPOINT_HISTORY;
import static ru.yandex.market.checkouter.jooq.Tables.DELIVERY_TRACK_HISTORY;
import static ru.yandex.market.checkouter.jooq.Tables.DIGITAL_CONTENT;
import static ru.yandex.market.checkouter.jooq.Tables.EXTERNAL_CERTIFICATE;
import static ru.yandex.market.checkouter.jooq.Tables.EXTERNAL_CERTIFICATE_EVENT;
import static ru.yandex.market.checkouter.jooq.Tables.EXTERNAL_CERTIFICATE_HISTORY;
import static ru.yandex.market.checkouter.jooq.Tables.EXTERNAL_CERTIFICATE_USER;
import static ru.yandex.market.checkouter.jooq.Tables.ITEM_PROMO;
import static ru.yandex.market.checkouter.jooq.Tables.ITEM_PROMO_HISTORY;
import static ru.yandex.market.checkouter.jooq.Tables.ITEM_SERVICE;
import static ru.yandex.market.checkouter.jooq.Tables.ITEM_SERVICE_HISTORY;
import static ru.yandex.market.checkouter.jooq.Tables.ORDER_BUYER;
import static ru.yandex.market.checkouter.jooq.Tables.ORDER_BUYER_HISTORY;
import static ru.yandex.market.checkouter.jooq.Tables.ORDER_CHANGE_REQUESTS;
import static ru.yandex.market.checkouter.jooq.Tables.ORDER_CHANGE_REQUESTS_HISTORY;
import static ru.yandex.market.checkouter.jooq.Tables.ORDER_DELIVERY;
import static ru.yandex.market.checkouter.jooq.Tables.ORDER_EVENT;
import static ru.yandex.market.checkouter.jooq.Tables.ORDER_HISTORY;
import static ru.yandex.market.checkouter.jooq.Tables.ORDER_ITEM;
import static ru.yandex.market.checkouter.jooq.Tables.ORDER_ITEM_DELIVERY;
import static ru.yandex.market.checkouter.jooq.Tables.ORDER_ITEM_HISTORY;
import static ru.yandex.market.checkouter.jooq.Tables.ORDER_PAYMENT;
import static ru.yandex.market.checkouter.jooq.Tables.ORDER_PRICE;
import static ru.yandex.market.checkouter.jooq.Tables.ORDER_PRICE_HISTORY;
import static ru.yandex.market.checkouter.jooq.Tables.ORDER_PROMO;
import static ru.yandex.market.checkouter.jooq.Tables.ORDER_PROMO_HISTORY;
import static ru.yandex.market.checkouter.jooq.Tables.ORDER_PROPERTY;
import static ru.yandex.market.checkouter.jooq.Tables.PARCEL;
import static ru.yandex.market.checkouter.jooq.Tables.PARCEL_BOX;
import static ru.yandex.market.checkouter.jooq.Tables.PARCEL_BOX_HISTORY;
import static ru.yandex.market.checkouter.jooq.Tables.PARCEL_BOX_ITEM;
import static ru.yandex.market.checkouter.jooq.Tables.PARCEL_BOX_ITEM_HISTORY;
import static ru.yandex.market.checkouter.jooq.Tables.PARCEL_HISTORY;
import static ru.yandex.market.checkouter.jooq.Tables.PARCEL_ITEM;
import static ru.yandex.market.checkouter.jooq.Tables.PARCEL_ITEM_HISTORY;
import static ru.yandex.market.checkouter.jooq.Tables.PAYMENT;
import static ru.yandex.market.checkouter.jooq.Tables.PAYMENT_HISTORY;
import static ru.yandex.market.checkouter.jooq.Tables.RECEIPT;
import static ru.yandex.market.checkouter.jooq.Tables.RECEIPT_ITEM;
import static ru.yandex.market.checkouter.jooq.Tables.REFUND;
import static ru.yandex.market.checkouter.jooq.Tables.REFUND_HISTORY;
import static ru.yandex.market.checkouter.jooq.Tables.RESALE_SPECS;
import static ru.yandex.market.checkouter.jooq.Tables.RETURN;
import static ru.yandex.market.checkouter.jooq.Tables.RETURN_DELIVERY;
import static ru.yandex.market.checkouter.jooq.Tables.RETURN_DELIVERY_HISTORY;
import static ru.yandex.market.checkouter.jooq.Tables.RETURN_HISTORY;
import static ru.yandex.market.checkouter.jooq.Tables.RETURN_ITEM;
import static ru.yandex.market.checkouter.jooq.tables.Orders.ORDERS;

public abstract class AbstractArchiveWebTestBase extends AbstractPaymentTestBase {

    public static final List<Table<?>> ARCHIVING_TABLES = ImmutableList.of(
            ORDERS,
            ORDER_PROPERTY,
            DELIVERY_ADDRESS,
            ORDER_DELIVERY,
            ORDER_ITEM,
            RETURN,
            RETURN_ITEM,
            RETURN_DELIVERY,
            RETURN_HISTORY,
            RETURN_DELIVERY_HISTORY,
            PAYMENT,
            PAYMENT_HISTORY,
            ORDER_PAYMENT,
            REFUND,
            REFUND_HISTORY,
            RECEIPT,
            DIGITAL_CONTENT,
            RECEIPT_ITEM,
            ORDER_HISTORY,
            ORDER_EVENT,
            ORDER_BUYER,
            ORDER_BUYER_HISTORY,
            ORDER_CHANGE_REQUESTS,
            ORDER_CHANGE_REQUESTS_HISTORY,
            PARCEL,
            PARCEL_HISTORY,
            PARCEL_ITEM,
            PARCEL_ITEM_HISTORY,
            PARCEL_BOX,
            PARCEL_BOX_HISTORY,
            PARCEL_BOX_ITEM,
            PARCEL_BOX_ITEM_HISTORY,
            DELIVERY_TRACK,
            DELIVERY_TRACK_HISTORY,
            DELIVERY_TRACK_CHECKPOINT,
            DELIVERY_TRACK_CHECKPOINT_HISTORY,
            ORDER_ITEM_HISTORY,
            ORDER_ITEM_DELIVERY,
            ITEM_PROMO,
            ITEM_PROMO_HISTORY,
            ORDER_PROMO,
            ORDER_PROMO_HISTORY,
            DELIVERY_PROMO,
            EXTERNAL_CERTIFICATE,
            EXTERNAL_CERTIFICATE_HISTORY,
            EXTERNAL_CERTIFICATE_EVENT,
            EXTERNAL_CERTIFICATE_USER,
            ORDER_PRICE,
            ORDER_PRICE_HISTORY,
            ITEM_SERVICE,
            ITEM_SERVICE_HISTORY,
            RESALE_SPECS
    );

    // Поле UPDATED_AT проставляется триггером в базе, сравнивать его некорректно
    public static final Map<TableImpl, TableField> IGNORE_FIELDS = ImmutableMap.of(
            RETURN, RETURN.UPDATED_AT,
            RECEIPT_ITEM, RECEIPT_ITEM.UPDATED_AT,
            RETURN_ITEM, RETURN_ITEM.UPDATED_AT,
            RECEIPT, RECEIPT.UPDATED_AT,
            ITEM_SERVICE, ITEM_SERVICE.UPDATED_AT
    );

    private static final String PROMO_KEY = "some promo";
    private static final String ANAPLAN_ID = "some anaplan id";
    private static final String SHOP_PROMO_KEY = "some promo";

    protected static final long BATCH_PERIOD = 120L;
    protected static final int BATCH_SIZE = 100;
    protected static final long MAX_INTERVAL_BETWEEN_ORDERS_CREATION_IN_MULTI_ORDER = 1L;

    @Autowired
    protected QueuedCallService queuedCallService;
    @Autowired
    protected OrderArchiveService orderArchiveService;
    @Autowired
    protected RefundService refundService;
    @Autowired
    protected OrderCopyingDao orderCopyingDao;
    @Autowired
    protected DSLContext dsl;
    @Autowired
    protected ArchiveStorageManager archiveStorageManager;
    @Autowired
    private ReceiptRepairHelper receiptRepairHelper;
    @Autowired
    private MultiPaymentHelper multiPaymentHelper;
    @Autowired
    private OrderPayHelper payHelper;
    @Autowired
    private ReturnHelper returnHelper;
    @Autowired
    private TrustPaymentService trustPaymentService;
    @Autowired
    private OrderArchivingDao orderArchiveDao;
    @Autowired
    private ParcelBoxHelper parcelBoxHelper;
    @Autowired
    private FulfillmentConfigurer fulfillmentConfigurer;
    @Value("${market.checkout.lbkx.topic.order-event.partition.count}")
    protected int partitionsCount;

    private AsyncProducer logbrokerAsyncProducer;

    @BeforeEach
    public void setUp() throws InterruptedException {
        checkouterFeatureWriter.writeValue(ComplexFeatureType.ORDER_ARCHIVING_START_PERIOD, null);

        logbrokerAsyncProducer = Mockito.mock(AsyncProducer.class);

        Mockito.doReturn(logbrokerAsyncProducer).when(lbkxClientFactory).asyncProducer(Mockito.any());

        ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
        CompletableFuture future = CompletableFuture.completedFuture(new ProducerWriteResponse(1, 1, false));
        Mockito.doReturn(future)
                .when(logbrokerAsyncProducer).write(captor.capture(), Mockito.anyLong());
        CompletableFuture<ProducerInitResponse> initFuture
                = CompletableFuture.completedFuture(
                new ProducerInitResponse(Long.MAX_VALUE, "1", 1, "1")
        );
        Mockito.doReturn(initFuture)
                .when(logbrokerAsyncProducer).init();
    }

    protected void archiveOrders(Set<Long> orderIds) {
        assertThat(orderArchiveService.archiveOrders(orderIds, false), hasSize(orderIds.size()));
    }

    protected void moveArchivedOrders() {
        exportEvents();
        runArchiveOrderTasks();
    }

    protected void exportEvents() {
        taskMap.values().stream()
                .filter(this::isExportEventTask)
                .peek(this::enableTask)
                .forEach(ZooTask::runOnce);
    }

    protected void runArchiveOrderTasks() {
        tmsTaskHelper.runArchiveOrderTasks();
    }

    private boolean isExportEventTask(ZooTask task) {
        return task.getName().contains(OrderEventsLbkxExportTask.class.getSimpleName());
    }

    @SneakyThrows
    private void enableTask(ZooTask zooTask) {
        ZooTask.asEnableAwareTask(zooTask)
                .ifPresent(task -> {
                    task.enable();
                    task.setPermittedEnvironmentTypes(EnumSet.of(EnvironmentType.getActive()));
                });
    }

    protected Order createOrderWithItemPromo() {
        Parameters parameters = defaultBlueOrderParameters();

        OrderItem item = parameters.getOrder().getItems().iterator().next();
        item.setBuyerPrice(BigDecimal.valueOf(500));
        item.setQuantPrice(BigDecimal.valueOf(500));

        final BigDecimal promoFixedPrice = BigDecimal.valueOf(300);
        final PromoDetails promoDetails = PromoDetails.builder()
                .promoKey(PROMO_KEY)
                .promoType(ReportPromoType.BLUE_FLASH.getCode())
                .promoFixedPrice(promoFixedPrice)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusHours(2))
                .build();

        final FoundOfferBuilder offerBuilder = FoundOfferBuilder.createFrom(item);

        offerBuilder.promoKey(promoDetails.getPromoKey())
                .promoType(promoDetails.getPromoType())
                .promoDetails(promoDetails)
                .price(promoFixedPrice)
                .oldMin(item.getBuyerPrice());

        parameters.getReportParameters().setOffers(List.of(offerBuilder.build()));
        item.setBuyerPrice(promoFixedPrice);
        item.setQuantPrice(promoFixedPrice);

        parameters.getLoyaltyParameters().expectResponseItem(
                OrderItemResponseBuilder.createFrom(item)
                        .promo(new ItemPromoResponse(
                                BigDecimal.valueOf(200),
                                PromoType.BLUE_FLASH,
                                UUID.randomUUID().toString(),
                                promoDetails.getPromoKey(),
                                SHOP_PROMO_KEY,
                                null,
                                ANAPLAN_ID,
                                null,
                                false,
                                null,
                                null,
                                null
                        ))
        );

        return orderCreateHelper.createOrder(parameters);
    }

    protected Order createOrderWithDeliveryPromo() {
        Parameters parameters = blueOrderWithDeliveryPromoParameters();
        fulfillmentConfigurer.configure(parameters, true);

        return orderCreateHelper.createOrder(parameters);
    }

    protected List<ChangeRequest> createChangeRequest(Order order) {
        RecipientPerson expectedRecipientPerson = new RecipientPerson("Ivan", null, "Ivanov");
        RecipientEditRequest recipientEditRequest = new RecipientEditRequest();
        recipientEditRequest.setPerson(expectedRecipientPerson);
        recipientEditRequest.setPhone("+ 7-999-999-99-99");
        OrderEditRequest orderEditRequest = new OrderEditRequest();
        orderEditRequest.setRecipientEditRequest(recipientEditRequest);
        return client.editOrder(
                order.getId(), ClientRole.SYSTEM, null, singletonList(BLUE), orderEditRequest);
    }

    protected void addBoxesWithItemsToOrder(final Order order) {
        final long parcelId = order.getDelivery().getParcels().get(0).getId();
        final ParcelBox parcelBox = prepareBox();
        final ParcelBoxItem parcelBoxItem = prepareBoxItem(order);
        List<ParcelBox> saved;
        try {
            saved = parcelBoxHelper.putBoxes(
                    order.getId(),
                    parcelId,
                    Collections.singletonList(parcelBox),
                    ClientInfo.SYSTEM
            );
            final Long boxId = Iterables.getOnlyElement(saved).getId();
            parcelBoxHelper.putItems(order.getId(), parcelId, boxId,
                    Collections.singletonList(parcelBoxItem), ClientInfo.SYSTEM);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected List<Order> createMultiOrder() throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.addOrder(BlueParametersProvider.defaultBlueOrderParametersWithItems(
                OrderItemProvider.getAnotherWarehouseOrderItem()));
        parameters.setPaymentMethod(PaymentMethod.YANDEX);

        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters);
        Order order1 = orderService.getOrder(multiOrder.getOrders().get(0).getId());
        Order order2 = orderService.getOrder(multiOrder.getOrders().get(1).getId());

        completeMultiOrder(order1, order2);

        return List.of(order1, order2);
    }

    protected Order completeOrder() {
        Order order = completeOrderWithoutOffsetAdvancedReceipt();
        createOffsetAdvancedReceiptAndUpdateStatusToPrinted(order);
        return order;
    }

    protected Order completeOrderWithoutOffsetAdvancedReceipt() {
        Order order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());

        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        order = orderService.getOrder(order.getId());

        clearPaymentAndUpdateIncomeReceiptToPrintedStatus();
        return order;
    }

    protected Order completePostpaidOrder() throws Exception {
        Order order = orderCreateHelper.createOrder(BlueParametersProvider.postpaidBlueOrderParameters());
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_CREATE_CASH_PAYMENT);
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_CREATE_SUBSIDY_PAYMENT);

        trustMockConfigurer.mockCheckBasket(buildPostAuth());
        trustMockConfigurer.mockStatusBasket(buildPostAuth(), null);
        PagedPayments pagedPayments = paymentTestHelper.getPagedPayments(order.getId(), PaymentGoal.ORDER_POSTPAY);
        Payment payment = pagedPayments.getItems().iterator().next();
        payHelper.notifyPaymentClear(payment);
        tmsTaskHelper.runCheckPaymentStatusTaskV2();

        order = orderService.getOrder(order.getId());
        return order;
    }

    protected Order completePostpaidOrderWithShopDelivery() {
        Order order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueNonFulfilmentOrderParameters());
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);

        order = orderService.getOrder(order.getId());
        return order;
    }


    protected void completeMultiOrder(Order order1, Order order2) throws Exception {
        clearMultiPaymentAndUpdateIncomeReceiptToPrintedStatus(order1, order2);

        order1 = orderService.getOrder(order1.getId());
        order2 = orderService.getOrder(order2.getId());

        orderStatusHelper.proceedOrderToStatus(order1, OrderStatus.DELIVERED);
        orderStatusHelper.proceedOrderToStatus(order2, OrderStatus.DELIVERED);

        order1 = orderService.getOrder(order1.getId());
        order2 = orderService.getOrder(order2.getId());

        createOffsetAdvancedReceiptAndUpdateStatusToPrinted(order1, order2);
    }

    protected Order completeCreditOrder() throws IOException {
        createPaidCreditOrder();
        payHelper.doStuffForSupplierPayment(List.of(order.get()));
        tmsTaskHelper.runCheckPaymentStatusTaskV2();

        Order order = orderService.getOrder(order().getId());
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        order = orderService.getOrder(order().getId());

        createOffsetAdvancedReceiptAndUpdateStatusToPrinted(order);
        return order;
    }

    protected void clearPaymentAndUpdateIncomeReceiptToPrintedStatus() {
        trustMockConfigurer.mockCheckBasket(buildPostAuth());
        trustMockConfigurer.mockStatusBasket(buildPostAuth(), null);
        tmsTaskHelper.runProcessHeldPaymentsTaskV2();
        tmsTaskHelper.runCheckPaymentStatusTaskV2();
    }

    protected void clearMultiPaymentAndUpdateIncomeReceiptToPrintedStatus(Order order1, Order order2)
            throws Exception {
        trustMockConfigurer.mockWholeTrust();
        Payment payment = multiPaymentHelper.ordersPay(
                order1.getBuyer().getUid(),
                Arrays.asList(order1.getId(), order2.getId()),
                null
        );
        trustMockConfigurer.mockCheckBasket(buildPostAuth());
        trustMockConfigurer.mockStatusBasket(buildPostAuth(), null);
        multiPaymentHelper.notifyMultiPayment(payment);
        tmsTaskHelper.runProcessHeldPaymentsTaskV2();
        tmsTaskHelper.runCheckPaymentStatusTaskV2();
    }

    protected void createOffsetAdvancedReceiptAndUpdateStatusToPrinted(Order... orders) {
        trustMockConfigurer.mockCheckBasket(buildPostAuth());
        trustMockConfigurer.mockStatusBasket(buildPostAuth(), null);
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_CREATE_OFFSET_ADVANCE_RECEIPT);
        Map<String, String> balanceOrderIdToDeliveryReceiptId = Stream.of(orders)
                .flatMap(order -> order.getItems().stream())
                .collect(Collectors.toMap(item -> item.getBalanceOrderId(), item -> ""));

        trustMockConfigurer.mockCheckBasket(
                CheckBasketParams.buildOffsetAdvanceCheckBasket(balanceOrderIdToDeliveryReceiptId));
        trustMockConfigurer.mockStatusBasket(
                CheckBasketParams.buildOffsetAdvanceCheckBasket(balanceOrderIdToDeliveryReceiptId), null);
        receiptRepairHelper.repairReceipts();
    }

    protected void moveSecondOrderInMultiOrderCreatedAtByDelta(List<Order> ordersInMultiOrder,
                                                               long timestamp,
                                                               long delta) {
        Long orderId = ordersInMultiOrder.get(1).getId();
        LocalDateTime createdAt = Instant.ofEpochMilli(timestamp + delta)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
        transactionTemplate.execute(ts ->
                masterJdbcTemplate.update("update orders set created_at = '" + createdAt + "' where id = "
                        + orderId));
    }

    protected void createReturn(Order order, Instant now) {
        Return ret = ReturnProvider
                .generateReturnWithDelivery(order, order.getDelivery().getDeliveryServiceId());
        returnHelper.createReturn(order.getId(), ret);
        returnHelper.processReturnPayments(order, ret);

        //клирим рефанд
        now = now.plus(3, ChronoUnit.DAYS);
        setFixedTime(now);
        Collection<Refund> refunds = refundService.getRefunds(order.getPayment());
        refunds.forEach(this::notifyRefundReceipts);
        now = now.plus(2, ChronoUnit.HOURS);
        setFixedTime(now);
        refunds.forEach(r -> {
            trustMockConfigurer.mockCheckBasket(CheckBasketParams.buildCheckBasketWithConfirmedRefund(
                    r.getTrustRefundId(), r.getAmount()));
            trustMockConfigurer.mockStatusBasket(CheckBasketParams.buildCheckBasketWithConfirmedRefund(
                    r.getTrustRefundId(), r.getAmount()), null);
        });
        tmsTaskHelper.runSyncRefundWithBillingPartitionTaskV2();

        //клирим компенсационные платежи
        clearPaymentAndUpdateIncomeReceiptToPrintedStatus();
        returnHelper.processReturnPayments(order, ret);
    }

    protected void notifyRefundReceipts(Refund r) {
        refundService.notifyRefund(new TrustRefundNotification(
                NotificationMode.receipt, r.getTrustRefundId(), "success", false,
                trustPaymentService.getReceiptUrl(r.getTrustRefundId(), r.getTrustRefundId())));
    }

    protected Order createBlueOrder() throws IOException {
        Order order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        //генерим гарантийный талон
        client.getWarrantyPdf(order.getId(), ClientRole.SYSTEM, 0L);
        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        return order;
    }

    protected Order archiveOrder(Order order) {
        transactionTemplate.execute(ts -> {
            order.setArchived(true);
            orderArchiveDao.updateArchived(order);
            return null;
        });
        return order;
    }

    protected Map<Table<?>, List<Record>> loadArchivingData(StorageType storageType) {
        return loadArchivingData(storageType, 0);
    }

    protected Map<Table<?>, List<Record>> loadArchivingData(StorageType storageType, int storageIndex) {
        return archiveStorageManager.doWithArchiveContext(storageIndex, () ->
                archiveStorageManager.executeReadOnlyTransaction(storageType, ts -> {
                            Map<Table<?>, List<Record>> data = ARCHIVING_TABLES.stream()
                                    .collect(Collectors.toMap(Function.identity(),
                                            table -> selectRecords(getDsl(storageType), table)
                                    ));
                            IGNORE_FIELDS.forEach((table, field) -> {
                                data.get(table).forEach(record -> record.set(field, null));
                            });
                            return data;
                        }
                )
        );
    }

    protected void saveArchivingData(StorageType storageType, Map<Table<?>, List<Record>> data) {
        saveArchivingData(storageType, 0, data);
    }

    protected void saveArchivingData(StorageType storageType, int storageIndex, Map<Table<?>, List<Record>> data) {
        archiveStorageManager.doWithArchiveContext(storageIndex, () ->
                archiveStorageManager.executeTransaction(storageType, ts -> {
                    ARCHIVING_TABLES.forEach(table -> insertRecords(getDsl(storageType), table, data.get(table)));
                    return null;
                })
        );
    }

    protected void deleteArchivingData(StorageType storageType, int storageIndex) {
        archiveStorageManager.doWithArchiveContext(storageIndex, () ->
                archiveStorageManager.executeTransaction(storageType, ts -> {
                    Lists.reverse(ARCHIVING_TABLES).forEach(table -> getDsl(storageType).deleteFrom(table).execute());
                    return null;
                })
        );
    }

    protected void assertArchivingDataEquals(Map<Table<?>, List<Record>> expected,
                                             Map<Table<?>, List<Record>> actual) {
        ARCHIVING_TABLES.forEach(
                table -> {
                    List<Record> expectedRecords = expected.getOrDefault(table, Collections.emptyList());
                    List<Record> actualRecords = actual.getOrDefault(table, Collections.emptyList());
                    assertRecordsEquals(expectedRecords, actualRecords, table.getName());
                }
        );
    }

    protected void copyArchivingData(OrderMovingDirection direction) {
        copyArchivingData(direction, 0);
    }

    protected void copyArchivingData(OrderMovingDirection direction, int archiveStorageIndex) {
        var data = loadArchivingData(direction.getSourceStorageType(), archiveStorageIndex);
        saveArchivingData(direction.getDestinationStorageType(), archiveStorageIndex, data);
    }

    protected void moveArchivingData(OrderMovingDirection direction) {
        moveArchivingData(direction, 0);
    }

    protected void moveArchivingData(OrderMovingDirection direction, int archiveStorageIndex) {
        copyArchivingData(direction, archiveStorageIndex);
        deleteArchivingData(direction.getSourceStorageType(), archiveStorageIndex);
    }

    protected void checkOrderRecordsExistence(StorageType storageType, Long orderId, boolean exist) {
        checkOrderRecordsExistence(storageType, 0, orderId, exist);
    }

    protected void checkOrderRecordsExistence(StorageType storageType, int storageIndex, Long orderId, boolean exist) {
        List<Record> records = archiveStorageManager.doWithArchiveContext(storageIndex, () ->
                selectRecords(getDsl(storageType), Tables.ORDERS, Tables.ORDERS.ID.eq(orderId))
        );
        assertEquals(exist, !records.isEmpty());
    }

    protected Map<Table<?>, List<Object>> convertToArchivedTableIds(Map<Table<?>, List<Record>> archivingData) {
        return archivingData.entrySet().stream()
                .filter(entry -> needTrackArchivingTable(entry.getKey()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .map(getArchivingTableIdField(entry.getKey())::get)
                                .collect(Collectors.toList())
                ));
    }

    protected Map<Table<?>, List<Object>> loadArchivedTableIds() {
        return TRACKED_ARCHIVING_TABLES.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        table -> dsl.selectFrom(archivedTable(table)).fetch(archivedTableIdField(table))
                ));
    }

    protected void assertArchivedTableIdsEquals(Map<Table<?>, List<Object>> expectedIdsData,
                                                Map<Table<?>, List<Object>> actualIdsData) {
        TRACKED_ARCHIVING_TABLES.forEach(
                table -> {
                    List<Object> expectedIds = expectedIdsData.getOrDefault(table, Collections.emptyList());
                    List<Object> actualIds = actualIdsData.getOrDefault(table, Collections.emptyList());
                    Assertions.assertThat(actualIds).describedAs("archived_%s table content", table.getName())
                            .containsExactlyInAnyOrderElementsOf(expectedIds);
                }
        );
    }

    protected DSLContext getDsl(StorageType storageType) {
        return storageType == StorageType.BASIC ? this.dsl : ArchiveContext.dsl().orElseThrow();
    }

    @Nonnull
    private List<Record> selectRecords(DSLContext dsl, Table<?> table, Condition... conditions) {
        return dsl.select(table.fields()).from(table).where(conditions).fetch();
    }

    private void insertRecords(DSLContext dsl, Table<?> table, List<Record> records) {
        if (!records.isEmpty()) {
            dsl.batchInsert(records.stream()
                            .map(record -> {
                                TableRecord<?> tableRecord = (TableRecord<?>) record.into(table);
                                tableRecord.changed(true);
                                return tableRecord;
                            })
                            .collect(Collectors.toList()))
                    .execute();
        }
    }

    protected List<Long> insertMultiOrder() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.addOrder(BlueParametersProvider.defaultBlueOrderParametersWithItems(
                OrderItemProvider.getAnotherWarehouseOrderItem()));
        parameters.setPaymentMethod(PaymentMethod.YANDEX);

        return orderCreateHelper.createMultiOrder(parameters).getOrders().stream()
                .map(BasicOrder::getId)
                .sorted()
                .collect(Collectors.toList());
    }

    @Nonnull
    protected Order createArchivedOrder() {
        Order order = completePostpaidOrderWithShopDelivery();

        var events = eventService.getOrdersHistoryEventsByOrders(Set.of(order.getId()),
                null, null, true, false,
                false, null,
                new ClientInfo(ClientRole.SYSTEM, null), null);

        assertThat(events, not(empty()));

        archiveOrders(Set.of(order.getId()));

        // Для того, чтобы во всех тасках сместился lastEventId
        for (int i = 0; i < partitionsCount; i++) {
            orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        }

        return order;
    }

    protected List<Order> createArchivedMultiOrder(Function<List<Order>, Set<Long>> orderIdSelector) throws Exception {
        var orders = createMultiOrder();

        archiveOrders(orderIdSelector.apply(orders));

        // Для того, чтобы во всех тасках сместился lastEventId
        for (int i = 0; i < partitionsCount; i++) {
            orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        }

        return orders;
    }

    private void assertRecordsEquals(List<Record> expectedRecords, List<Record> actualRecords, String tableName) {
        assertThat(tableName + " records", actualRecords, hasSize(expectedRecords.size()));
        assertThat(tableName + " content", actualRecords, containsInAnyOrder(expectedRecords.toArray()));
    }

    private ParcelBoxItem prepareBoxItem(final Order order) {
        final Long itemId = order.getItems().iterator().next().getId();
        ParcelBoxItem parcelBoxItem = new ParcelBoxItem();
        parcelBoxItem.setItemId(itemId);
        parcelBoxItem.setCount(1);
        return parcelBoxItem;
    }

    private ParcelBox prepareBox() {
        ParcelBox parcelBox = new ParcelBox();
        parcelBox.setFulfilmentId("ffId");
        parcelBox.setWeight(1L);
        parcelBox.setWidth(2L);
        parcelBox.setHeight(3L);
        parcelBox.setDepth(4L);
        return parcelBox;
    }
}
