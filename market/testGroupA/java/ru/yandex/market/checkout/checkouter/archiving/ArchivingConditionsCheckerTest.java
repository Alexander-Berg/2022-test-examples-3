package ru.yandex.market.checkout.checkouter.archiving;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;

import ru.yandex.market.checkout.application.AbstractServicesTestBase;
import ru.yandex.market.checkout.carter.InMemoryAppender;
import ru.yandex.market.checkout.checkouter.color.ColorConfig;
import ru.yandex.market.checkout.checkouter.color.SingleColorConfig;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.order.BasicOrder;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OrderService;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.archive.OrderArchiveCheckingService;
import ru.yandex.market.checkout.checkouter.order.archive.OrderArchiveRequest;
import ru.yandex.market.checkout.checkouter.order.archive.OrderArchiveService;
import ru.yandex.market.checkout.checkouter.order.archive.StorageOrderArchiveService;
import ru.yandex.market.checkout.checkouter.order.archive.filters.AbstractArchivingConditionChecker;
import ru.yandex.market.checkout.checkouter.pay.PaymentGoal;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentStatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.checkouter.pay.RefundStatus;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptStatus;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptType;
import ru.yandex.market.checkout.checkouter.returns.ReturnStatus;
import ru.yandex.market.checkout.checkouter.storage.OrderHistoryDao;
import ru.yandex.market.checkout.checkouter.storage.archive.dto.DeliveryForArchiving;
import ru.yandex.market.checkout.checkouter.storage.archive.dto.PaymentForArchiving;
import ru.yandex.market.checkout.checkouter.storage.archive.dto.ReceiptForArchiving;
import ru.yandex.market.checkout.checkouter.storage.archive.dto.RefundForArchiving;
import ru.yandex.market.checkout.checkouter.storage.archive.dto.ReturnForArchiving;
import ru.yandex.market.checkout.checkouter.storage.archive.repository.OrderArchivingDao;
import ru.yandex.market.checkout.checkouter.storage.util.MultiLockHelper;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.storage.StorageCallback;
import ru.yandex.market.checkout.storage.impl.LockCallback;
import ru.yandex.market.queuedcalls.QueuedCall;
import ru.yandex.market.queuedcalls.QueuedCallService;
import ru.yandex.market.queuedcalls.QueuedCallType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType.SHOP;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType.YANDEX_MARKET;
import static ru.yandex.market.checkout.checkouter.pay.PaymentGoal.CREDIT;
import static ru.yandex.market.checkout.checkouter.pay.PaymentGoal.EXTERNAL_CERTIFICATE;
import static ru.yandex.market.checkout.checkouter.pay.PaymentGoal.MARKET_COMPENSATION;
import static ru.yandex.market.checkout.checkouter.pay.PaymentGoal.ORDER_POSTPAY;
import static ru.yandex.market.checkout.checkouter.pay.PaymentGoal.ORDER_PREPAY;
import static ru.yandex.market.checkout.checkouter.pay.PaymentGoal.SUBSIDY;
import static ru.yandex.market.checkout.checkouter.pay.PaymentGoal.SUPPLIER_PAYMENT;
import static ru.yandex.market.checkout.checkouter.pay.PaymentGoal.USER_COMPENSATION;
import static ru.yandex.market.checkout.checkouter.pay.PaymentStatus.CANCELLED;
import static ru.yandex.market.checkout.checkouter.pay.PaymentStatus.CLEARED;
import static ru.yandex.market.checkout.checkouter.pay.PaymentStatus.INIT;
import static ru.yandex.market.checkout.checkouter.pay.PaymentStatus.IN_PROGRESS;
import static ru.yandex.market.checkout.checkouter.pay.RefundStatus.ACCEPTED;
import static ru.yandex.market.checkout.checkouter.pay.RefundStatus.RETURNED;
import static ru.yandex.market.checkout.checkouter.pay.RefundStatus.SUCCESS;
import static ru.yandex.market.checkout.checkouter.receipt.ReceiptStatus.GENERATED;
import static ru.yandex.market.checkout.checkouter.receipt.ReceiptStatus.NEW;
import static ru.yandex.market.checkout.checkouter.receipt.ReceiptStatus.PRINTED;
import static ru.yandex.market.checkout.checkouter.receipt.ReceiptType.INCOME;
import static ru.yandex.market.checkout.checkouter.receipt.ReceiptType.INCOME_RETURN;
import static ru.yandex.market.checkout.checkouter.receipt.ReceiptType.OFFSET_ADVANCE_ON_DELIVERED;
import static ru.yandex.market.checkout.checkouter.returns.ReturnStatus.FAILED;
import static ru.yandex.market.checkout.checkouter.returns.ReturnStatus.REFUNDED;
import static ru.yandex.market.checkout.checkouter.returns.ReturnStatus.REFUND_IN_PROGRESS;

@ExtendWith(MockitoExtension.class)
public class ArchivingConditionsCheckerTest extends AbstractServicesTestBase {

    private static final Logger LOGGER = (Logger) LoggerFactory.getLogger(AbstractArchivingConditionChecker.class);
    private static final String SUBSIDY_PAYMENT_CONDITION_FAILED =
            "SubsidyPaymentArchivingConditionChecker check has failed!";
    private static final String INCOME_RETURN_RECEIPT_STATUS_DATE_CONDITION_FAILED =
            "IncomeReturnReceiptStatusDateArchivingConditionChecker check has failed!";
    private static final String CREDIT_PAYMENT_CONDITION_FAILED =
            "CreditPaymentArchivingConditionChecker check has failed!";
    private static final String ORDER_BASE_CONDITION_FAILED =
            "OrderBaseArchivingConditionChecker check has failed!";
    private static final String PAYMENT_STATUS_DATE_CONDITION_FAILED =
            "PaymentStatusDateArchivingConditionChecker check has failed!";
    private static final String INCOME_RECEIPT_STATUS_DATE_CONDITION_FAILED =
            "IncomeReceiptStatusDateArchivingConditionChecker check has failed!";
    private static final String DELIVERY_RECEIPT_STATUS_DATE_CONDITION_FAILED =
            "DeliveryReceiptStatusDateArchivingConditionChecker check has failed!";
    private static final String RETURN_BASIC_CONDITION_FAILED =
            "ReturnBasicArchivingConditionChecker check has failed!";
    private static final String RETURN_DATE_CONDITION_FAILED =
            "ReturnDateArchivingConditionChecker check has failed!";
    private static final String REFUND_RETURN_STATUS_DATE_CONDITION_FAILED =
            "RefundReturnStatusDateArchivingConditionChecker check has failed!";
    private static final String RETURN_REFUND_RECEIPT_CONDITION_FAILED =
            "RefundReturnReceiptArchivingConditionChecker check has failed!";
    private static final String RETURN_REFUND_CREDIT_CONDITION_FAILED =
            "RefundReturnCreditArchivingConditionChecker check has failed!";
    private static final String RETURN_REFUND_SUBSIDY_CONDITION_FAILED =
            "RefundReturnSubsidyArchivingConditionChecker check has failed!";
    private static final String RETURN_COMPENSATION_CONDITION_FAILED =
            "ReturnCompensationArchivingConditionChecker check has failed!";
    private static final String REFUND_CANCELLED_ORDER_CONDITION_FAILED =
            "RefundCancelledOrderArchivingConditionChecker check has failed!";
    private static final String ORDER_QC_CONDITION_FAILED =
            "OrderQueuedCallsArchivingConditionChecker check has failed!";

    private static final QueuedCallType QUEUED_CALL_TYPE = CheckouterQCType.PAYMENT_CALL_BALANCE_UPDATE_PAYMENT;

    @Mock
    private OrderArchivingDao archiveDao;
    @Mock
    private OrderHistoryDao historyDao;
    @Mock
    private QueuedCallService queuedCallService;
    @Mock
    private ColorConfig colorConfig;

    private OrderArchiveService archiveService;
    private OrderArchiveService testingArchiveService;

    private LocalDateTime now;
    private OrderArchiveRequest request;
    private static final Long ORDER_ID = 1L;
    private static final Set<Long> ORDER_IDS = Set.of(ORDER_ID);
    private static final Long ORDER_PAYMENT_ID = 2L;
    private static final Long CREDIT_PAYMENT_ID = 3L;
    private static final Long SUBSIDY_PAYMENT_ID = 4L;
    private static final Long RECEIPT_ID = 5L;
    private static final Long RETURN_ID = 6L;
    private static final Long REFUND_ID = 7L;
    private static final Long FAILED_REFUND_ID = 8L;
    private static final Long CREDIT_REFUND_ID = 9L;
    private static final Long SUBSIDY_REFUND_ID = 10L;
    private static final Long COMPENSATION_PAYMENT_ID = 11L;

    private InMemoryAppender inMemoryAppender;
    private Level oldLevel;

    @Bean
    public MultiLockHelper multiLockHelper() {
        return new MultiLockHelper() {

            @Override
            public <T> T updateWithOrderLocks(Collection<Long> orderIds, StorageCallback<T> callback) {
                return callback.doQuery();
            }

            @Override
            public <T> T doLockedWithoutTransaction(Collection<Long> orderIds, LockCallback<T> callback) {
                return callback.doLocked(null);
            }
        };
    }

    @BeforeEach
    public void setUp() {
        archiveService = new StorageOrderArchiveService(
                transactionTemplate,
                transactionTemplate,
                archiveDao,
                historyDao,
                new OrderArchiveCheckingService(colorConfig, getClock(), queuedCallService, "production",
                        LocalDateTime.parse("2019-06-01T00:00:00")),
                Mockito.mock(OrderService.class),
                queuedCallService,
                multiLockHelper(),
                getClock(),
                20,
                2);
        testingArchiveService = new StorageOrderArchiveService(
                transactionTemplate,
                transactionTemplate,
                archiveDao,
                historyDao,
                new OrderArchiveCheckingService(colorConfig, getClock(), queuedCallService, "testing",
                        LocalDateTime.parse("2019-06-01T00:00:00")),
                Mockito.mock(OrderService.class),
                queuedCallService,
                multiLockHelper(),
                getClock(),
                20,
                2);

        inMemoryAppender = new InMemoryAppender();
        LOGGER.addAppender(inMemoryAppender);
        inMemoryAppender.clear();
        inMemoryAppender.start();

        now = LocalDateTime.now();
        request = new OrderArchiveRequest(now, 100, 60,
                100, now.plusDays(3), now.plusDays(1), Set.of());
        when(archiveDao.findOrdersDelivery(any())).thenReturn(Map.of(ORDER_ID, createOrderDelivery(YANDEX_MARKET)));

        SingleColorConfig blueConfig = mock(SingleColorConfig.class);
        lenient().when(blueConfig.isOrderArchivingEnabled()).thenReturn(true);
        lenient().when(colorConfig.getFor(Color.BLUE)).thenReturn(blueConfig);

        SingleColorConfig whiteConfig = mock(SingleColorConfig.class);
        lenient().when(whiteConfig.isOrderArchivingEnabled()).thenReturn(true);
        lenient().when(colorConfig.getFor(Color.WHITE)).thenReturn(whiteConfig);

        oldLevel = LOGGER.getLevel();
        LOGGER.setLevel(Level.INFO);
    }

    @AfterEach
    public void tearDown() {
        LOGGER.detachAppender(inMemoryAppender);
        LOGGER.setLevel(oldLevel);
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldFailIfOrderAlreadyArchived(String color) {
        List<BasicOrder> orders = createPrepaidDeliveredOrder(Color.valueOf(color));
        orders.forEach(o -> o.setArchived(true));
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(orders);
        checkOrderNotArchivedWithErrorMessage(ORDER_BASE_CONDITION_FAILED);
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldFailIfNoPayments(String color) {
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(createPrepaidDeliveredOrder(Color.valueOf(color)));
        checkOrderNotArchivedWithErrorMessage(PAYMENT_STATUS_DATE_CONDITION_FAILED);
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldOkIfNoPayments(String color) {
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(createPrepaidDeliveredOrder(Color.valueOf(color)));
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS)).thenReturn(Map.of());
        checkOrderArchivedInTest();
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldFailIfPaymentNotCleared(String color) {
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(createPrepaidDeliveredOrder(Color.valueOf(color)));
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildOrderPayment(ORDER_PREPAY, CANCELLED, now, Set.of()));
        checkOrderNotArchivedWithErrorMessage(PAYMENT_STATUS_DATE_CONDITION_FAILED);
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldFailIfPaidByExternalCertificate(String color) {
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(createPrepaidDeliveredOrder(Color.valueOf(color)));
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildOrderPayment(EXTERNAL_CERTIFICATE, CLEARED, now, Set.of()));
        checkOrderNotArchivedWithErrorMessage(PAYMENT_STATUS_DATE_CONDITION_FAILED);
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldFailIfPaidRecently(String color) {
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(createPrepaidDeliveredOrder(Color.valueOf(color)));
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildOrderPayment(ORDER_PREPAY, CLEARED, now.plusDays(2), Set.of()));
        checkOrderNotArchivedWithErrorMessage(PAYMENT_STATUS_DATE_CONDITION_FAILED);
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldFailIfNoReceipts(String color) {
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(createPrepaidDeliveredOrder(Color.valueOf(color)));
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildOrderPayment(ORDER_PREPAY, CLEARED, now, Set.of()));
        checkOrderNotArchivedWithErrorMessage(INCOME_RECEIPT_STATUS_DATE_CONDITION_FAILED);
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldFailIfReceiptNotPrinted(String color) {
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(createPrepaidDeliveredOrder(Color.valueOf(color)));
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildOrderPayment(ORDER_PREPAY, CLEARED, now, Set.of(createReceipt(NEW, INCOME, now))));
        checkOrderNotArchivedWithErrorMessage(INCOME_RECEIPT_STATUS_DATE_CONDITION_FAILED);
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldFailIfReceiptPrintedRecently(String color) {
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(createPrepaidDeliveredOrder(Color.valueOf(color)));
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildOrderPayment(ORDER_PREPAY, CLEARED, now,
                        Set.of(createReceipt(PRINTED, INCOME, now.plusDays(2)))));
        checkOrderNotArchivedWithErrorMessage(INCOME_RECEIPT_STATUS_DATE_CONDITION_FAILED);
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldFailIfReceiptGeneratedRecently(String color) {
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(createPrepaidDeliveredOrder(Color.valueOf(color)));
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildOrderPayment(ORDER_PREPAY, CLEARED, now,
                        Set.of(createReceipt(GENERATED, INCOME, now.plusDays(2)))));
        checkOrderNotArchivedWithErrorMessage(INCOME_RECEIPT_STATUS_DATE_CONDITION_FAILED);
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldFailIfNoDeliveredReceipts(String color) {
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(createPrepaidDeliveredOrder(Color.valueOf(color)));
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildOrderPayment(ORDER_PREPAY, CLEARED, now, buildPrintedReceipt()));
        checkOrderNotArchivedWithErrorMessage(DELIVERY_RECEIPT_STATUS_DATE_CONDITION_FAILED);
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldOkIfPrepaidOrderWithBothReceipts(String color) {
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(createPrepaidDeliveredOrder(Color.valueOf(color)));
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildOrderPayment(ORDER_PREPAY, CLEARED, now, buildOrderPaymentReceipts()));
        checkOrderArchived();
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    @DisplayName("NEGATIVE: Order has not finished queued calls")
    void orderQCNegativeTest(String color) {
        when(queuedCallService.findQueuedCallsByOrderId(anyLong())).thenReturn(
                List.of(new QueuedCall(1,
                        QUEUED_CALL_TYPE,
                        null,
                        null,
                        null,
                        1,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null))
        );
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(createPrepaidDeliveredOrder(Color.valueOf(color)));
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildOrderPayment(ORDER_POSTPAY, CLEARED, now, buildPrintedReceipt()));
        checkOrderNotArchivedWithErrorMessage(ORDER_QC_CONDITION_FAILED);
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    @DisplayName("POSITIVE: DeliveryPartnerType == SHOP")
    void shopDeliveryPositiveTest(String color) {
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(createPrepaidDeliveredOrder(Color.valueOf(color)));
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS)).thenReturn(Map.of());
        when(archiveDao.findOrdersDelivery(any())).thenReturn(Map.of(ORDER_ID, createOrderDelivery(SHOP)));
        checkOrderArchived();
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    @DisplayName("NEGATIVE: У заказа есть SUBSIDY платеж в конечном статусе, но нет обычного")
    void subsidyPaymentNegativeTest(String color) {
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(createPrepaidDeliveredOrder(Color.valueOf(color)));
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildOrderPayment(SUBSIDY, CLEARED, now, buildOrderPaymentReceipts()));
        checkOrderNotArchivedWithErrorMessage(PAYMENT_STATUS_DATE_CONDITION_FAILED);
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    @DisplayName("POSITIVE: У заказа есть платеж в конечном статусе и отмененный платеж")
    void unsuccessfulPaymentsPositiveTest(String color) {
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(createPrepaidDeliveredOrder(Color.valueOf(color)));
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS)).thenReturn(
                Map.of(
                        ORDER_ID,
                        Map.of(
                                ORDER_PAYMENT_ID,
                                createPayment(ORDER_PAYMENT_ID, ORDER_PREPAY, CLEARED, now,
                                        buildOrderPaymentReceipts()),
                                ORDER_PAYMENT_ID + 1,
                                createPayment(ORDER_PAYMENT_ID + 1, ORDER_PREPAY, CANCELLED, now,
                                        buildOrderPaymentReceipts())
                        )
                )

        );
        checkOrderArchived();
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    @DisplayName("POSITIVE: У платежа есть чек в конечном статусе на уменьшение заказа, созданный более 7 дней назад")
    void incomeReturnPaymentReceiptPositiveTest(String color) {
        Set<ReceiptForArchiving> receipts = new HashSet<>(buildOrderPaymentReceipts());
        receipts.add(createReceipt(PRINTED, INCOME_RETURN, now.minusDays(8)));

        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(createPrepaidDeliveredOrder(Color.valueOf(color)));
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildOrderPayment(ORDER_PREPAY, CLEARED, now, receipts));
        checkOrderArchived();
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    @DisplayName("NEGATIVE: У платежа есть чек в конечном статусе на уменьшение заказа, созданный менее 7 дней назад")
    void incomeReturnPaymentReceiptNegativeTest(String color) {
        Set<ReceiptForArchiving> receipts = new HashSet<>(buildOrderPaymentReceipts());
        receipts.add(createReceipt(PRINTED, INCOME_RETURN, now.plusDays(6)));

        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(createPrepaidDeliveredOrder(Color.valueOf(color)));
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildOrderPayment(ORDER_PREPAY, CLEARED, now, receipts));
        checkOrderNotArchivedWithErrorMessage(INCOME_RETURN_RECEIPT_STATUS_DATE_CONDITION_FAILED);
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    @DisplayName("NEGATIVE: У платежа есть чек не в конечном статусе на уменьшение заказа," +
            " созданный более 7 дней назад")
    void incomeReturnPaymentReceiptWrongStatusNegativeTest(String color) {
        Set<ReceiptForArchiving> receipts = new HashSet<>(buildOrderPaymentReceipts());
        receipts.add(createReceipt(NEW, INCOME_RETURN, now.minusDays(8)));

        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(createPrepaidDeliveredOrder(Color.valueOf(color)));
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildOrderPayment(ORDER_PREPAY, CLEARED, now, receipts));
        checkOrderNotArchivedWithErrorMessage(INCOME_RETURN_RECEIPT_STATUS_DATE_CONDITION_FAILED);
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldOkIfPostpaidOrderWithOneReceipt(String color) {
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(createPostpaidDeliveredOrder());
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildOrderPayment(ORDER_POSTPAY, CLEARED, now, buildPrintedReceipt()));
        checkOrderArchived();
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldOkIfOldPrepaidOrderWithOneReceipt(String color) {
        List<BasicOrder> orders = createPostpaidDeliveredOrder();
        LocalDateTime updatedAt = LocalDateTime.parse(
                "2018-01-01 18:37:18.366", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
        Date updatedDate = Date.from(updatedAt.atZone(getClock().getZone()).toInstant());
        orders.forEach(o -> o.setStatusUpdateDate(updatedDate));
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(orders);
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildOrderPayment(ORDER_PREPAY, CLEARED, now, buildPrintedReceipt()));
        checkOrderArchived();
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldFailIfNoApprovedCredit(String color) {
        List<BasicOrder> orders = createPrepaidDeliveredOrder(Color.valueOf(color));
        orders.forEach(o -> o.setPaymentMethod(PaymentMethod.CREDIT));
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(orders);
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildOrderPayment(SUPPLIER_PAYMENT, CLEARED, now, buildOrderPaymentReceipts()));
        checkOrderNotArchivedWithErrorMessage(CREDIT_PAYMENT_CONDITION_FAILED);
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldFailIfCreditPaymentNotCleared(String color) {
        List<BasicOrder> orders = createPrepaidDeliveredOrder(Color.valueOf(color));
        orders.forEach(o -> o.setPaymentMethod(PaymentMethod.CREDIT));
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(orders);
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS)).thenReturn(buildCreditPayments(IN_PROGRESS, now, Set.of()));
        checkOrderNotArchivedWithErrorMessage(CREDIT_PAYMENT_CONDITION_FAILED);
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldFailIfCreditPaymentClearedRecently(String color) {
        List<BasicOrder> orders = createPrepaidDeliveredOrder(Color.valueOf(color));
        orders.forEach(o -> o.setPaymentMethod(PaymentMethod.CREDIT));
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(orders);
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildCreditPayments(CLEARED, now.plusDays(2), Set.of()));
        checkOrderNotArchivedWithErrorMessage(CREDIT_PAYMENT_CONDITION_FAILED);
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldFailIfCreditPaymentHasNoReceipts(String color) {
        List<BasicOrder> orders = createPrepaidDeliveredOrder(Color.valueOf(color));
        orders.forEach(o -> o.setPaymentMethod(PaymentMethod.CREDIT));
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(orders);
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildCreditPayments(CLEARED, now, Set.of()));
        checkOrderNotArchivedWithErrorMessage(CREDIT_PAYMENT_CONDITION_FAILED);
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldFailIfCreditPaymentReceiptNotPrinted(String color) {
        List<BasicOrder> orders = createPrepaidDeliveredOrder(Color.valueOf(color));
        orders.forEach(o -> o.setPaymentMethod(PaymentMethod.CREDIT));
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(orders);
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildCreditPayments(CLEARED, now, Set.of(createReceipt(NEW, INCOME, now))));
        checkOrderNotArchivedWithErrorMessage(CREDIT_PAYMENT_CONDITION_FAILED);
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldFailIfCreditPaymentReceiptPrintedRecently(String color) {
        List<BasicOrder> orders = createPrepaidDeliveredOrder(Color.valueOf(color));
        orders.forEach(o -> o.setPaymentMethod(PaymentMethod.CREDIT));
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(orders);
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildCreditPayments(CLEARED, now, Set.of(createReceipt(PRINTED, INCOME, now.plusDays(2)))));
        checkOrderNotArchivedWithErrorMessage(CREDIT_PAYMENT_CONDITION_FAILED);
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldOkIfCreditPaymentWithReceipt(String color) {
        List<BasicOrder> orders = createPrepaidDeliveredOrder(Color.valueOf(color));
        orders.forEach(o -> o.setPaymentMethod(PaymentMethod.CREDIT));
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(orders);
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildCreditPayments(CLEARED, now, buildPrintedReceipt()));
        checkOrderArchived();
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldOkIfZeroSubsidy(String color) {
        List<BasicOrder> orders = createPrepaidDeliveredOrder(Color.valueOf(color));
        orders.forEach(o -> o.getPromoPrices().setSubsidyTotal(BigDecimal.ZERO));
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(orders);
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildOrderPayment(ORDER_PREPAY, CLEARED, now, buildOrderPaymentReceipts()));
        checkOrderArchived();
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldFailIfNoSubsidy(String color) {
        List<BasicOrder> orders = createPrepaidDeliveredOrder(Color.valueOf(color));
        orders.forEach(o -> o.getPromoPrices().setSubsidyTotal(BigDecimal.TEN));
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(orders);
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildOrderPayment(ORDER_PREPAY, CLEARED, now, buildOrderPaymentReceipts()));
        checkOrderNotArchivedWithErrorMessage(SUBSIDY_PAYMENT_CONDITION_FAILED);
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldFailIfSubsidyNotCleared(String color) {
        List<BasicOrder> orders = createPrepaidDeliveredOrder(Color.valueOf(color));
        orders.forEach(o -> o.getPromoPrices().setSubsidyTotal(BigDecimal.TEN));
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(orders);
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS)).thenReturn(buildOrderAndSubsidyPayments(INIT, now,
                Set.of()));
        checkOrderNotArchivedWithErrorMessage(SUBSIDY_PAYMENT_CONDITION_FAILED);
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldFailIfSubsidyClearedRecently(String color) {
        List<BasicOrder> orders = createPrepaidDeliveredOrder(Color.valueOf(color));
        orders.forEach(o -> o.getPromoPrices().setSubsidyTotal(BigDecimal.TEN));
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(orders);
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildOrderAndSubsidyPayments(CLEARED, now.plusDays(2), Set.of()));
        checkOrderNotArchivedWithErrorMessage(SUBSIDY_PAYMENT_CONDITION_FAILED);
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldFailIfSubsidyHasNoReceipts(String color) {
        List<BasicOrder> orders = createPrepaidDeliveredOrder(Color.valueOf(color));
        orders.forEach(o -> o.getPromoPrices().setSubsidyTotal(BigDecimal.TEN));
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(orders);
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildOrderAndSubsidyPayments(CLEARED, now, Set.of()));
        checkOrderNotArchivedWithErrorMessage(SUBSIDY_PAYMENT_CONDITION_FAILED);
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldFailIfSubsidyReceiptNotPrinted(String color) {
        List<BasicOrder> orders = createPrepaidDeliveredOrder(Color.valueOf(color));
        orders.forEach(o -> o.getPromoPrices().setSubsidyTotal(BigDecimal.TEN));
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(orders);
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildOrderAndSubsidyPayments(CLEARED, now, Set.of(createReceipt(NEW, INCOME, now))));
        checkOrderNotArchivedWithErrorMessage(SUBSIDY_PAYMENT_CONDITION_FAILED);
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldFailIfSubsidyReceiptPrintedRecently(String color) {
        List<BasicOrder> orders = createPrepaidDeliveredOrder(Color.valueOf(color));
        orders.forEach(o -> o.getPromoPrices().setSubsidyTotal(BigDecimal.TEN));
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(orders);
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS)).thenReturn(
                buildOrderAndSubsidyPayments(CLEARED, now, Set.of(createReceipt(PRINTED, INCOME, now.plusDays(2)))));
        checkOrderNotArchivedWithErrorMessage(SUBSIDY_PAYMENT_CONDITION_FAILED);
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldOkIfSubsidyWithReceipt(String color) {
        List<BasicOrder> orders = createPrepaidDeliveredOrder(Color.valueOf(color));
        orders.forEach(o -> o.getPromoPrices().setSubsidyTotal(BigDecimal.TEN));
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(orders);
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildOrderAndSubsidyPayments(CLEARED, now, buildPrintedReceipt()));
        checkOrderArchived();
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldFailIfReturnHasNoRefunds(String color) {
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(createPrepaidDeliveredOrder(Color.valueOf(color)));
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildOrderPayment(ORDER_PREPAY, CLEARED, now, buildOrderPaymentReceipts()));
        when(archiveDao.findReturnsByOrderIds(ORDER_IDS)).thenReturn(buildReturn(REFUNDED, now));
        checkOrderNotArchivedWithErrorMessage(RETURN_BASIC_CONDITION_FAILED);
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldFailIfReturnNotCompleted(String color) {
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(createPrepaidDeliveredOrder(Color.valueOf(color)));
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildOrderPayment(ORDER_PREPAY, CLEARED, now, buildOrderPaymentReceipts()));
        when(archiveDao.findReturnsByOrderIds(ORDER_IDS)).thenReturn(buildReturn(REFUND_IN_PROGRESS, now));
        when(archiveDao.findRefundsByOrderIds(ORDER_IDS)).thenReturn(buildRefund(SUCCESS, now, Set.of()));
        checkOrderNotArchivedWithErrorMessage(RETURN_DATE_CONDITION_FAILED);
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldFailIfReturnableRefundNotCleared(String color) {
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(createPrepaidDeliveredOrder(Color.valueOf(color)));
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildOrderPayment(ORDER_PREPAY, CLEARED, now, buildOrderPaymentReceipts()));
        when(archiveDao.findReturnsByOrderIds(ORDER_IDS)).thenReturn(buildReturn(FAILED, now));
        when(archiveDao.findRefundsByOrderIds(ORDER_IDS)).thenReturn(buildRefund(ACCEPTED, now,
                Set.of(createReceipt(PRINTED, INCOME_RETURN, now))));
        checkOrderNotArchivedWithErrorMessage(REFUND_RETURN_STATUS_DATE_CONDITION_FAILED);
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldFailIfReturnableRefundClearedRecently(String color) {
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(createPrepaidDeliveredOrder(Color.valueOf(color)));
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildOrderPayment(ORDER_PREPAY, CLEARED, now, buildOrderPaymentReceipts()));
        when(archiveDao.findReturnsByOrderIds(ORDER_IDS)).thenReturn(buildReturn(FAILED, now));
        when(archiveDao.findRefundsByOrderIds(ORDER_IDS)).thenReturn(buildRefund(SUCCESS, now.plusDays(2),
                Set.of(createReceipt(PRINTED, INCOME_RETURN, now))));
        checkOrderNotArchivedWithErrorMessage(REFUND_RETURN_STATUS_DATE_CONDITION_FAILED);
    }


    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldFailIfReturnableRefundHasNoReceipts(String color) {
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(createPrepaidDeliveredOrder(Color.valueOf(color)));
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildOrderPayment(ORDER_PREPAY, CLEARED, now, buildOrderPaymentReceipts()));
        when(archiveDao.findReturnsByOrderIds(ORDER_IDS)).thenReturn(buildReturn(FAILED, now));
        when(archiveDao.findRefundsByOrderIds(ORDER_IDS)).thenReturn(buildRefund(SUCCESS, now, Set.of()));
        checkOrderNotArchivedWithErrorMessage(RETURN_REFUND_RECEIPT_CONDITION_FAILED);
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldFailIfReturnableRefundReceiptNotPrinted(String color) {
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(createPrepaidDeliveredOrder(Color.valueOf(color)));
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildOrderPayment(ORDER_PREPAY, CLEARED, now, buildOrderPaymentReceipts()));
        when(archiveDao.findReturnsByOrderIds(ORDER_IDS)).thenReturn(buildReturn(FAILED, now));
        when(archiveDao.findRefundsByOrderIds(ORDER_IDS)).thenReturn(buildRefund(SUCCESS, now,
                Set.of(createReceipt(NEW, INCOME_RETURN, now))));
        checkOrderNotArchivedWithErrorMessage(RETURN_REFUND_RECEIPT_CONDITION_FAILED);
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldFailIfReturnableRefundReceiptPrintedRecently(String color) {
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(createPrepaidDeliveredOrder(Color.valueOf(color)));
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildOrderPayment(ORDER_PREPAY, CLEARED, now, buildOrderPaymentReceipts()));
        when(archiveDao.findReturnsByOrderIds(ORDER_IDS)).thenReturn(buildReturn(FAILED, now));
        when(archiveDao.findRefundsByOrderIds(ORDER_IDS)).thenReturn(buildRefund(SUCCESS, now,
                Set.of(createReceipt(PRINTED, INCOME_RETURN, now.plusDays(2)))));
        checkOrderNotArchivedWithErrorMessage(RETURN_REFUND_RECEIPT_CONDITION_FAILED);
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldOkIfWhenReturnRefundedWithFailedStatus(String color) {
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(createPrepaidDeliveredOrder(Color.valueOf(color)));
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildOrderPayment(ORDER_PREPAY, CLEARED, now, buildOrderPaymentReceipts()));
        when(archiveDao.findReturnsByOrderIds(ORDER_IDS)).thenReturn(buildReturn(FAILED, now));
        when(archiveDao.findRefundsByOrderIds(ORDER_IDS)).thenReturn(buildRefund(SUCCESS, now,
                Set.of(createReceipt(PRINTED, INCOME_RETURN, now))));
        checkOrderArchived();
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldOkReturnableRefundWithFailedRetry(String color) {
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(createPrepaidDeliveredOrder(Color.valueOf(color)));
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildOrderPayment(ORDER_PREPAY, CLEARED, now, buildOrderPaymentReceipts()));
        when(archiveDao.findReturnsByOrderIds(ORDER_IDS)).thenReturn(buildReturn(REFUNDED, now));
        when(archiveDao.findRefundsByOrderIds(ORDER_IDS)).thenReturn(buildRefundWithFailedRetry());
        checkOrderArchived();
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldFailIfRefundedOnlyCredit(String color) {
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(createPrepaidDeliveredOrder(Color.valueOf(color)));
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildCreditPayments(CLEARED, now, buildPrintedReceipt()));
        when(archiveDao.findReturnsByOrderIds(ORDER_IDS)).thenReturn(buildReturn(REFUNDED, now));
        when(archiveDao.findRefundsByOrderIds(ORDER_IDS)).thenReturn(
                Map.of(ORDER_ID, Map.of(CREDIT_REFUND_ID, createCreditRefund(SUCCESS, now, buildPrintedReceipt()))));
        checkOrderNotArchivedWithErrorMessage(RETURN_REFUND_CREDIT_CONDITION_FAILED);
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldFailIfRefundedOnlySupplierPayment(String color) {
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(createPrepaidDeliveredOrder(Color.valueOf(color)));
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildCreditPayments(CLEARED, now, buildPrintedReceipt()));
        when(archiveDao.findReturnsByOrderIds(ORDER_IDS)).thenReturn(buildReturn(REFUNDED, now));
        when(archiveDao.findRefundsByOrderIds(ORDER_IDS)).thenReturn(
                Map.of(ORDER_ID, Map.of(ORDER_PAYMENT_ID, createRefund(SUCCESS, now, buildPrintedReceipt()))));
        checkOrderNotArchivedWithErrorMessage(RETURN_REFUND_CREDIT_CONDITION_FAILED);
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldFailIfReturnableCreditRefundNotCleared(String color) {
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(createPrepaidDeliveredOrder(Color.valueOf(color)));
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildCreditPayments(CLEARED, now, buildPrintedReceipt()));
        when(archiveDao.findReturnsByOrderIds(ORDER_IDS)).thenReturn(buildReturn(REFUNDED, now));
        when(archiveDao.findRefundsByOrderIds(ORDER_IDS)).thenReturn(buildOrderAndCreditRefund(RETURNED, now,
                Set.of()));
        checkOrderNotArchivedWithErrorMessage(REFUND_RETURN_STATUS_DATE_CONDITION_FAILED);
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldFailIfReturnableCreditRefundClearedRecently(String color) {
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(createPrepaidDeliveredOrder(Color.valueOf(color)));
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildCreditPayments(CLEARED, now, buildPrintedReceipt()));
        when(archiveDao.findReturnsByOrderIds(ORDER_IDS)).thenReturn(buildReturn(REFUNDED, now));
        when(archiveDao.findRefundsByOrderIds(ORDER_IDS))
                .thenReturn(buildOrderAndCreditRefund(SUCCESS, now.plusDays(2), Set.of()));
        checkOrderNotArchivedWithErrorMessage(REFUND_RETURN_STATUS_DATE_CONDITION_FAILED);
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldFailIfReturnableCreditRefundHasNoReceipts(String color) {
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(createPrepaidDeliveredOrder(Color.valueOf(color)));
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildCreditPayments(CLEARED, now, buildPrintedReceipt()));
        when(archiveDao.findReturnsByOrderIds(ORDER_IDS)).thenReturn(buildReturn(REFUNDED, now));
        when(archiveDao.findRefundsByOrderIds(ORDER_IDS)).thenReturn(buildOrderAndCreditRefund(SUCCESS, now, Set.of()));
        checkOrderNotArchivedWithErrorMessage(RETURN_REFUND_RECEIPT_CONDITION_FAILED);
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldFailIfReturnableCreditRefundReceiptNotPrinted(String color) {
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(createPrepaidDeliveredOrder(Color.valueOf(color)));
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildCreditPayments(CLEARED, now, buildPrintedReceipt()));
        when(archiveDao.findReturnsByOrderIds(ORDER_IDS)).thenReturn(buildReturn(REFUNDED, now));
        when(archiveDao.findRefundsByOrderIds(ORDER_IDS)).thenReturn(
                buildOrderAndCreditRefund(SUCCESS, now, Set.of(createReceipt(NEW, INCOME_RETURN, now))));
        checkOrderNotArchivedWithErrorMessage(RETURN_REFUND_RECEIPT_CONDITION_FAILED);
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldFailIfReturnableCreditRefundReceiptPrintedRecently(String color) {
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(createPrepaidDeliveredOrder(Color.valueOf(color)));
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildCreditPayments(CLEARED, now, buildPrintedReceipt()));
        when(archiveDao.findReturnsByOrderIds(ORDER_IDS)).thenReturn(buildReturn(REFUNDED, now));
        when(archiveDao.findRefundsByOrderIds(ORDER_IDS)).thenReturn(
                buildOrderAndCreditRefund(SUCCESS, now,
                        Set.of(createReceipt(PRINTED, INCOME_RETURN, now.plusDays(2)))));
        checkOrderNotArchivedWithErrorMessage(RETURN_REFUND_RECEIPT_CONDITION_FAILED);
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldOkIfCreditAndSupplierPaymentsRefunded(String color) {
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(createPrepaidDeliveredOrder(Color.valueOf(color)));
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildCreditPayments(CLEARED, now, buildPrintedReceipt()));
        when(archiveDao.findReturnsByOrderIds(ORDER_IDS)).thenReturn(buildReturn(REFUNDED, now));
        when(archiveDao.findRefundsByOrderIds(ORDER_IDS))
                .thenReturn(buildOrderAndCreditRefund(SUCCESS, now, buildPrintedReceipt()));
        checkOrderArchived();
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldFailIfReturnableSubsidyNotRefunded(String color) {
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(createPrepaidDeliveredOrder(Color.valueOf(color)));
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildOrderPayment(ORDER_PREPAY, CLEARED, now, buildOrderPaymentReceipts()));
        when(archiveDao.findReturnsByOrderIds(ORDER_IDS)).thenReturn(buildReturnWithSubsidy());
        when(archiveDao.findRefundsByOrderIds(ORDER_IDS)).thenReturn(buildRefund(SUCCESS, now,
                Set.of(createReceipt(PRINTED, INCOME_RETURN, now))));
        checkOrderNotArchivedWithErrorMessage(RETURN_REFUND_SUBSIDY_CONDITION_FAILED);
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldFailIfReturnableSubsidyRefundNotCleared(String color) {
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(createPrepaidDeliveredOrder(Color.valueOf(color)));
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildOrderAndSubsidyPayments(CLEARED, now, buildPrintedReceipt()));
        when(archiveDao.findReturnsByOrderIds(ORDER_IDS)).thenReturn(buildReturnWithSubsidy());
        when(archiveDao.findRefundsByOrderIds(ORDER_IDS)).thenReturn(buildOrderAndSubsidyRefund(ACCEPTED, now,
                Set.of(createReceipt(PRINTED, INCOME_RETURN, now))));
        checkOrderNotArchivedWithErrorMessage(REFUND_RETURN_STATUS_DATE_CONDITION_FAILED);
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldFailIfReturnableSubsidyRefundClearedRecently(String color) {
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(createPrepaidDeliveredOrder(Color.valueOf(color)));
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildOrderAndSubsidyPayments(CLEARED, now, buildPrintedReceipt()));
        when(archiveDao.findReturnsByOrderIds(ORDER_IDS)).thenReturn(buildReturnWithSubsidy());
        when(archiveDao.findRefundsByOrderIds(ORDER_IDS)).thenReturn(buildOrderAndSubsidyRefund(SUCCESS,
                now.plusDays(2),
                Set.of(createReceipt(PRINTED, INCOME_RETURN, now))));
        checkOrderNotArchivedWithErrorMessage(REFUND_RETURN_STATUS_DATE_CONDITION_FAILED);
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldFailIfReturnableSubsidyRefundHasNoReceipts(String color) {
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(createPrepaidDeliveredOrder(Color.valueOf(color)));
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildOrderAndSubsidyPayments(CLEARED, now, buildPrintedReceipt()));
        when(archiveDao.findReturnsByOrderIds(ORDER_IDS)).thenReturn(buildReturnWithSubsidy());
        when(archiveDao.findRefundsByOrderIds(ORDER_IDS)).thenReturn(buildOrderAndSubsidyRefund(SUCCESS, now,
                Set.of()));
        checkOrderNotArchivedWithErrorMessage(RETURN_REFUND_RECEIPT_CONDITION_FAILED);
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldFailIfReturnableSubsidyRefundReceiptNotPrinted(String color) {
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(createPrepaidDeliveredOrder(Color.valueOf(color)));
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildOrderAndSubsidyPayments(CLEARED, now, buildPrintedReceipt()));
        when(archiveDao.findReturnsByOrderIds(ORDER_IDS)).thenReturn(buildReturnWithSubsidy());
        when(archiveDao.findRefundsByOrderIds(ORDER_IDS)).thenReturn(buildOrderAndSubsidyRefund(SUCCESS, now,
                Set.of(createReceipt(NEW, INCOME_RETURN, now))));
        checkOrderNotArchivedWithErrorMessage(RETURN_REFUND_RECEIPT_CONDITION_FAILED);
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldFailIfReturnableSubsidyRefundReceiptPrintedRecently(String color) {
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(createPrepaidDeliveredOrder(Color.valueOf(color)));
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildOrderAndSubsidyPayments(CLEARED, now, buildPrintedReceipt()));
        when(archiveDao.findReturnsByOrderIds(ORDER_IDS)).thenReturn(buildReturnWithSubsidy());
        when(archiveDao.findRefundsByOrderIds(ORDER_IDS)).thenReturn(buildOrderAndSubsidyRefund(SUCCESS, now,
                Set.of(createReceipt(GENERATED, INCOME_RETURN, now.plusDays(2)))));
        checkOrderNotArchivedWithErrorMessage(RETURN_REFUND_RECEIPT_CONDITION_FAILED);
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldOkIfReturnableSubsidyRefundWithReceipt(String color) {
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(createPrepaidDeliveredOrder(Color.valueOf(color)));
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildOrderAndSubsidyPayments(CLEARED, now, buildPrintedReceipt()));
        when(archiveDao.findReturnsByOrderIds(ORDER_IDS)).thenReturn(buildReturnWithSubsidy());
        when(archiveDao.findRefundsByOrderIds(ORDER_IDS)).thenReturn(buildOrderAndSubsidyRefund(SUCCESS, now,
                Set.of(createReceipt(GENERATED, INCOME_RETURN, now))));
        checkOrderArchived();
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldFailIfNoReturnableMarketCompensation(String color) {
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(createPrepaidDeliveredOrder(Color.valueOf(color)));
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildOrderPayment(ORDER_PREPAY, CLEARED, now, buildOrderPaymentReceipts()));
        when(archiveDao.findReturnsByOrderIds(ORDER_IDS)).thenReturn(buildReturnWithMarketCompensation());
        when(archiveDao.findRefundsByOrderIds(ORDER_IDS)).thenReturn(buildRefundWithFailedRetry());
        checkOrderNotArchivedWithErrorMessage(RETURN_COMPENSATION_CONDITION_FAILED);
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldFailIfReturnableMarketCompensationNotCleared(String color) {
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(createPrepaidDeliveredOrder(Color.valueOf(color)));
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildOrderAndMarketCompensationPayments(IN_PROGRESS, now, buildPrintedReceipt()));
        when(archiveDao.findReturnsByOrderIds(ORDER_IDS)).thenReturn(buildReturnWithMarketCompensation());
        when(archiveDao.findRefundsByOrderIds(ORDER_IDS)).thenReturn(buildRefundWithFailedRetry());
        checkOrderNotArchivedWithErrorMessage(RETURN_COMPENSATION_CONDITION_FAILED);
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldFailIfReturnableMarketCompensationClearedRecently(String color) {
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(createPrepaidDeliveredOrder(Color.valueOf(color)));
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildOrderAndMarketCompensationPayments(CLEARED, now.plusDays(2), buildPrintedReceipt()));
        when(archiveDao.findReturnsByOrderIds(ORDER_IDS)).thenReturn(buildReturnWithMarketCompensation());
        when(archiveDao.findRefundsByOrderIds(ORDER_IDS)).thenReturn(buildRefundWithFailedRetry());
        checkOrderNotArchivedWithErrorMessage(RETURN_COMPENSATION_CONDITION_FAILED);
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldFailIfReturnableMarketCompensationHasNoReceipts(String color) {
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(createPrepaidDeliveredOrder(Color.valueOf(color)));
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildOrderAndMarketCompensationPayments(CLEARED, now, Set.of()));
        when(archiveDao.findReturnsByOrderIds(ORDER_IDS)).thenReturn(buildReturnWithMarketCompensation());
        when(archiveDao.findRefundsByOrderIds(ORDER_IDS)).thenReturn(buildRefundWithFailedRetry());
        checkOrderNotArchivedWithErrorMessage(RETURN_COMPENSATION_CONDITION_FAILED);
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldFailIfReturnableMarketCompensationReceiptNotPrinted(String color) {
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(createPrepaidDeliveredOrder(Color.valueOf(color)));
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildOrderAndMarketCompensationPayments(CLEARED, now, Set.of(createReceipt(NEW, INCOME,
                        now))));
        when(archiveDao.findReturnsByOrderIds(ORDER_IDS)).thenReturn(buildReturnWithMarketCompensation());
        when(archiveDao.findRefundsByOrderIds(ORDER_IDS)).thenReturn(buildRefundWithFailedRetry());
        checkOrderNotArchivedWithErrorMessage(RETURN_COMPENSATION_CONDITION_FAILED);
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldFailIfReturnableMarketCompensationReceiptPrintedRecently(String color) {
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(createPrepaidDeliveredOrder(Color.valueOf(color)));
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildOrderAndMarketCompensationPayments(
                        CLEARED, now, Set.of(createReceipt(PRINTED, INCOME, now.plusDays(2)))));
        when(archiveDao.findReturnsByOrderIds(ORDER_IDS)).thenReturn(buildReturnWithMarketCompensation());
        when(archiveDao.findRefundsByOrderIds(ORDER_IDS)).thenReturn(buildRefundWithFailedRetry());
        checkOrderNotArchivedWithErrorMessage(RETURN_COMPENSATION_CONDITION_FAILED);
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldOkIfReturnableMarketCompensationWithReceipt(String color) {
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(createPrepaidDeliveredOrder(Color.valueOf(color)));
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS)).thenReturn(
                buildOrderAndMarketCompensationPayments(CLEARED, now, Set.of(createReceipt(PRINTED, INCOME, now))));
        when(archiveDao.findReturnsByOrderIds(ORDER_IDS)).thenReturn(buildReturnWithMarketCompensation());
        when(archiveDao.findRefundsByOrderIds(ORDER_IDS)).thenReturn(buildRefundWithFailedRetry());
        checkOrderArchived();
    }


    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldFailIfNoReturnableUserCompensation(String color) {
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(createPrepaidDeliveredOrder(Color.valueOf(color)));
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildOrderPayment(ORDER_PREPAY, CLEARED, now, buildOrderPaymentReceipts()));
        when(archiveDao.findReturnsByOrderIds(ORDER_IDS)).thenReturn(buildReturnWithUserCompensation());
        when(archiveDao.findRefundsByOrderIds(ORDER_IDS)).thenReturn(buildRefundWithFailedRetry());
        checkOrderNotArchivedWithErrorMessage(RETURN_COMPENSATION_CONDITION_FAILED);
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldFailIfReturnableUserCompensationNotCleared(String color) {
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(createPrepaidDeliveredOrder(Color.valueOf(color)));
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildOrderAndUserCompensationPayments(IN_PROGRESS, now, buildPrintedReceipt()));
        when(archiveDao.findReturnsByOrderIds(ORDER_IDS)).thenReturn(buildReturnWithUserCompensation());
        when(archiveDao.findRefundsByOrderIds(ORDER_IDS)).thenReturn(buildRefundWithFailedRetry());
        checkOrderNotArchivedWithErrorMessage(RETURN_COMPENSATION_CONDITION_FAILED);
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldFailIfReturnableUserCompensationClearedRecently(String color) {
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(createPrepaidDeliveredOrder(Color.valueOf(color)));
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildOrderAndUserCompensationPayments(CLEARED, now.plusDays(2), buildPrintedReceipt()));
        when(archiveDao.findReturnsByOrderIds(ORDER_IDS)).thenReturn(buildReturnWithUserCompensation());
        when(archiveDao.findRefundsByOrderIds(ORDER_IDS)).thenReturn(buildRefundWithFailedRetry());
        checkOrderNotArchivedWithErrorMessage(RETURN_COMPENSATION_CONDITION_FAILED);
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldFailIfReturnableUserCompensationHasNoReceipts(String color) {
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(createPrepaidDeliveredOrder(Color.valueOf(color)));
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildOrderAndUserCompensationPayments(CLEARED, now, Set.of()));
        when(archiveDao.findReturnsByOrderIds(ORDER_IDS)).thenReturn(buildReturnWithUserCompensation());
        when(archiveDao.findRefundsByOrderIds(ORDER_IDS)).thenReturn(buildRefundWithFailedRetry());
        checkOrderNotArchivedWithErrorMessage(RETURN_COMPENSATION_CONDITION_FAILED);
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldFailIfReturnableUserCompensationReceiptNotPrinted(String color) {
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(createPrepaidDeliveredOrder(Color.valueOf(color)));
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS)).thenReturn(
                buildOrderAndUserCompensationPayments(CLEARED, now, Set.of(createReceipt(NEW, INCOME, now))));
        when(archiveDao.findReturnsByOrderIds(ORDER_IDS)).thenReturn(buildReturnWithUserCompensation());
        when(archiveDao.findRefundsByOrderIds(ORDER_IDS)).thenReturn(buildRefundWithFailedRetry());
        checkOrderNotArchivedWithErrorMessage(RETURN_COMPENSATION_CONDITION_FAILED);
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldFailIfReturnableUserCompensationReceiptPrintedRecently(String color) {
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(createPrepaidDeliveredOrder(Color.valueOf(color)));
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildOrderAndUserCompensationPayments(
                        CLEARED, now, Set.of(createReceipt(PRINTED, INCOME, now.plusDays(2)))));
        when(archiveDao.findReturnsByOrderIds(ORDER_IDS)).thenReturn(buildReturnWithUserCompensation());
        when(archiveDao.findRefundsByOrderIds(ORDER_IDS)).thenReturn(buildRefundWithFailedRetry());
        checkOrderNotArchivedWithErrorMessage(RETURN_COMPENSATION_CONDITION_FAILED);
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    @DisplayName("NEGATIVE: Есть заказ с неуспешным возмещением без возврата")
    void shouldFailIfRefundWithoutReturnFail(String color) {
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(createPrepaidDeliveredOrder(Color.valueOf(color)));
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS))
                .thenReturn(buildOrderAndUserCompensationPayments(
                        CLEARED, now, Set.of(createReceipt(PRINTED, INCOME, now.plusDays(2)))));
        RefundForArchiving refund = createRefund(RefundStatus.FAILED, now.minusHours(1),
                Set.of(createReceipt(ReceiptStatus.FAILED, INCOME_RETURN, now.minusHours(1))));
        when(archiveDao.findRefundsByOrderIds(ORDER_IDS)).thenReturn(Map.of(ORDER_ID, Map.of(REFUND_ID, refund)));
        checkOrderNotArchivedWithErrorMessage(REFUND_RETURN_STATUS_DATE_CONDITION_FAILED);
    }

    // Disabled until cancelled orders will be added to archiving
    @Disabled
    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    @DisplayName("NEGATIVE: Есть оплаченный и отмененный заказ до доставки, но нет возмещения")
    void shouldFailIfCancelledPayedOrderWithoutRefund(String color) {
        when(archiveDao.findOrders(Set.of(100L))).thenReturn(List.of(createOrder(OrderStatus.CANCELLED, 100L)));
        when(archiveDao.findOrderPreviousStatus(Set.of(100L))).thenReturn(Map.of(100L, OrderStatus.PROCESSING));
        when(archiveDao.findPaymentsByOrderIds(Set.of(100L)))
                .thenReturn(
                        Map.of(100L, Map.of(
                                ORDER_PAYMENT_ID,
                                createPayment(ORDER_PAYMENT_ID, ORDER_PREPAY, CLEARED, now, buildOrderPaymentReceipts())
                        )));
        checkOrderNotArchivedWithErrorMessage(REFUND_CANCELLED_ORDER_CONDITION_FAILED, Set.of(100L));
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldOkIfReturnableUserCompensationWithReceipt(String color) {
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(createPrepaidDeliveredOrder(Color.valueOf(color)));
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS)).thenReturn(
                buildOrderAndUserCompensationPayments(CLEARED, now, Set.of(createReceipt(PRINTED, INCOME, now))));
        when(archiveDao.findReturnsByOrderIds(ORDER_IDS)).thenReturn(buildReturnWithUserCompensation());
        when(archiveDao.findRefundsByOrderIds(ORDER_IDS)).thenReturn(buildRefundWithFailedRetry());
        checkOrderArchived();
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldOkIfNoPaymentsBlueOrder(String color) {
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(createPrepaidDeliveredOrder(Color.valueOf(color)));
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS)).thenReturn(Map.of());
        checkOrderArchivedInTest();
    }

    @ParameterizedTest
    @CsvSource({
            "BLUE",
            "WHITE"
    })
    void shouldOkIfNoPaymentsWhiteOrder(String color) {
        BasicOrder order = createDeliveredOrder();
        order.setRgb(Color.WHITE);
        when(archiveDao.findOrders(ORDER_IDS)).thenReturn(List.of(order));
        when(archiveDao.findPaymentsByOrderIds(ORDER_IDS)).thenReturn(Map.of());
        checkOrderArchivedInTest();
    }

    private void checkOrderNotArchivedWithErrorMessage(String message, Set<Long> ids) {
        Set<Long> archivedOrderIds = archiveService.archiveOrders(ids, request);
        assertFalse(inMemoryAppender.getRaw().isEmpty());
        assertThat(inMemoryAppender.getRaw().get(0).getFormattedMessage(), containsString(message));
        assertThat(archivedOrderIds, empty());
    }

    private void checkOrderNotArchivedWithErrorMessage(String message) {
        checkOrderNotArchivedWithErrorMessage(message, ORDER_IDS);
    }

    private void checkOrderArchived() {
        assertEquals(1, archiveService.archiveOrders(ORDER_IDS, request).size());
    }

    private void checkOrderArchivedInTest() {
        assertEquals(1, testingArchiveService.archiveOrders(ORDER_IDS, request).size());
    }

    private List<BasicOrder> createPrepaidDeliveredOrder() {
        BasicOrder order = createDeliveredOrder();
        order.setPaymentType(PaymentType.PREPAID);
        return List.of(order);
    }

    private List<BasicOrder> createPrepaidDeliveredOrder(Color rgb) {
        BasicOrder order = createDeliveredOrder();
        order.setRgb(rgb);
        order.setPaymentType(PaymentType.PREPAID);
        return List.of(order);
    }

    private List<BasicOrder> createPostpaidDeliveredOrder() {
        BasicOrder order = createDeliveredOrder();
        order.setPaymentType(PaymentType.POSTPAID);
        return List.of(order);
    }

    private BasicOrder createDeliveredOrder() {
        return createOrder(OrderStatus.DELIVERED, ORDER_ID);
    }

    @Nonnull
    private BasicOrder createOrder(OrderStatus status, Long id) {
        BasicOrder order = new BasicOrder();
        order.setId(id);
        order.setRgb(Color.BLUE);
        order.setFake(false);
        order.setArchived(false);
        order.setStatus(status);
        order.setStatusUpdateDate(Date.from(now.atZone(ZoneId.systemDefault()).toInstant()));
        return order;
    }

    private Map<Long, Map<Long, PaymentForArchiving>> buildOrderPayment(PaymentGoal goal,
                                                                        PaymentStatus status,
                                                                        LocalDateTime updatedAt,
                                                                        Set<ReceiptForArchiving> receipts) {
        return Map.of(ORDER_ID, Map.of(ORDER_PAYMENT_ID, createPayment(ORDER_PAYMENT_ID, goal, status, updatedAt,
                receipts)));
    }

    private Map<Long, Map<Long, PaymentForArchiving>> buildCreditPayments(PaymentStatus status,
                                                                          LocalDateTime updatedAt,
                                                                          Set<ReceiptForArchiving> receipts
    ) {
        return Map.of(ORDER_ID, Map.of(
                ORDER_PAYMENT_ID, createPayment(ORDER_PAYMENT_ID, SUPPLIER_PAYMENT, CLEARED, now,
                        buildOrderPaymentReceipts()),
                CREDIT_PAYMENT_ID, createPayment(CREDIT_PAYMENT_ID, CREDIT, status, updatedAt, receipts)
        ));
    }

    private Map<Long, Map<Long, PaymentForArchiving>> buildOrderAndSubsidyPayments(PaymentStatus status,
                                                                                   LocalDateTime updatedAt,
                                                                                   Set<ReceiptForArchiving> receipts
    ) {
        return Map.of(ORDER_ID, Map.of(
                ORDER_PAYMENT_ID, createPayment(ORDER_PAYMENT_ID, ORDER_PREPAY, CLEARED, now,
                        buildOrderPaymentReceipts()),
                SUBSIDY_PAYMENT_ID, createPayment(SUBSIDY_PAYMENT_ID, SUBSIDY, status, updatedAt, receipts)
        ));
    }

    private PaymentForArchiving createPayment(
            long paymentId, PaymentGoal goal,
            PaymentStatus status,
            LocalDateTime updatedAt,
            Set<ReceiptForArchiving> receipts
    ) {
        PaymentForArchiving payment = new PaymentForArchiving();
        payment.setId(paymentId);
        payment.setGoal(goal);
        payment.setStatus(status);
        payment.setFinalBalanceStatus(status);
        payment.setUpdatedAt(updatedAt);
        payment.setReceipts(receipts);
        return payment;
    }

    private ReceiptForArchiving createReceipt(ReceiptStatus status, ReceiptType type, LocalDateTime updatedAt) {
        ReceiptForArchiving receipt = new ReceiptForArchiving();
        receipt.setId(RECEIPT_ID);
        receipt.setStatus(status);
        receipt.setType(type);
        receipt.setUpdatedAt(updatedAt);
        return receipt;
    }

    private Set<ReceiptForArchiving> buildPrintedReceipt() {
        return Set.of(createReceipt(PRINTED, INCOME, now));
    }

    private Set<ReceiptForArchiving> buildOrderPaymentReceipts() {
        return Set.of(createReceipt(PRINTED, INCOME, now), createReceipt(GENERATED, OFFSET_ADVANCE_ON_DELIVERED, now));
    }

    private Map<Long, Map<Long, ReturnForArchiving>> buildReturn(ReturnStatus status, LocalDateTime updatedAt) {
        return Map.of(ORDER_ID, Map.of(RETURN_ID, createReturn(status, updatedAt)));
    }

    private ReturnForArchiving createReturn(ReturnStatus status, LocalDateTime updatedAt) {
        ReturnForArchiving returnForArchiving = new ReturnForArchiving();
        returnForArchiving.setId(RETURN_ID);
        returnForArchiving.setStatus(status);
        returnForArchiving.setUpdatedAt(updatedAt);
        return returnForArchiving;
    }

    private Map<Long, Map<Long, RefundForArchiving>> buildRefund(RefundStatus status,
                                                                 LocalDateTime updatedAt,
                                                                 Set<ReceiptForArchiving> receipts) {
        return Map.of(ORDER_ID, Map.of(REFUND_ID, createRefund(status, updatedAt, receipts)));
    }

    private RefundForArchiving createRefund(RefundStatus status,
                                            LocalDateTime updatedAt,
                                            Set<ReceiptForArchiving> receipts) {
        RefundForArchiving refund = new RefundForArchiving();
        refund.setId(REFUND_ID);
        refund.setReturnId(RETURN_ID);
        refund.setPaymentId(ORDER_PAYMENT_ID);
        refund.setStatus(status);
        refund.setFinalBalanceStatus(status);
        refund.setUpdatedAt(updatedAt);
        refund.setReceipts(receipts);
        return refund;
    }

    private Map<Long, Map<Long, RefundForArchiving>> buildRefundWithFailedRetry() {
        RefundForArchiving failedRefund = createRefund(RefundStatus.FAILED, now.minusHours(1),
                Set.of(createReceipt(ReceiptStatus.FAILED, INCOME_RETURN, now.minusHours(1))));
        RefundForArchiving refund = createRefund(SUCCESS, now, Set.of(createReceipt(PRINTED, INCOME_RETURN, now)));
        failedRefund.setRetryId(refund.getId());
        return Map.of(ORDER_ID, Map.of(FAILED_REFUND_ID, failedRefund, REFUND_ID, refund));
    }

    private Map<Long, Map<Long, RefundForArchiving>> buildOrderAndCreditRefund(RefundStatus status,
                                                                               LocalDateTime updatedAt,
                                                                               Set<ReceiptForArchiving> receipts) {
        RefundForArchiving refund = createRefund(SUCCESS, now, buildPrintedReceipt());
        return Map.of(ORDER_ID, Map.of(
                REFUND_ID, refund,
                CREDIT_REFUND_ID, createCreditRefund(status, updatedAt, receipts)
        ));
    }

    private RefundForArchiving createCreditRefund(RefundStatus status,
                                                  LocalDateTime updatedAt,
                                                  Set<ReceiptForArchiving> receipts) {
        RefundForArchiving refund = new RefundForArchiving();
        refund.setId(CREDIT_REFUND_ID);
        refund.setReturnId(RETURN_ID);
        refund.setPaymentId(CREDIT_PAYMENT_ID);
        refund.setStatus(status);
        refund.setFinalBalanceStatus(null);
        refund.setUpdatedAt(updatedAt);
        refund.setReceipts(receipts);
        return refund;
    }

    private Map<Long, Map<Long, ReturnForArchiving>> buildReturnWithSubsidy() {
        return Map.of(ORDER_ID, Map.of(RETURN_ID, createReturnWithSubsidy()));
    }

    private ReturnForArchiving createReturnWithSubsidy() {
        ReturnForArchiving returnForArchiving = createReturn(REFUNDED, now);
        returnForArchiving.setSubsidyRefundNeeded(true);
        return returnForArchiving;
    }

    private Map<Long, Map<Long, RefundForArchiving>> buildOrderAndSubsidyRefund(
            RefundStatus status, LocalDateTime updatedAt, Set<ReceiptForArchiving> receipts) {
        RefundForArchiving orderRefund = createRefund(SUCCESS, now, Set.of(createReceipt(PRINTED, INCOME_RETURN, now)));
        orderRefund.setId(REFUND_ID);
        orderRefund.setPaymentId(ORDER_PAYMENT_ID);

        RefundForArchiving subsidyRefund = createRefund(status, updatedAt, receipts);
        subsidyRefund.setId(SUBSIDY_REFUND_ID);
        subsidyRefund.setPaymentId(SUBSIDY_PAYMENT_ID);
        return Map.of(ORDER_ID, Map.of(REFUND_ID, orderRefund, SUBSIDY_REFUND_ID, subsidyRefund));
    }

    private Map<Long, Map<Long, ReturnForArchiving>> buildReturnWithMarketCompensation() {
        return Map.of(ORDER_ID, Map.of(RETURN_ID, createReturnWithMarketCompensation()));
    }

    private ReturnForArchiving createReturnWithMarketCompensation() {
        ReturnForArchiving returnForArchiving = createReturn(REFUNDED, now);
        returnForArchiving.setMarketCompensationNeeded(true);
        return returnForArchiving;
    }

    private Map<Long, Map<Long, PaymentForArchiving>> buildOrderAndMarketCompensationPayments(
            PaymentStatus status, LocalDateTime updatedAt, Set<ReceiptForArchiving> receipts
    ) {
        return Map.of(ORDER_ID, Map.of(
                ORDER_PAYMENT_ID, createPayment(ORDER_PAYMENT_ID, ORDER_PREPAY, CLEARED, now,
                        buildOrderPaymentReceipts()),
                COMPENSATION_PAYMENT_ID, createPayment(COMPENSATION_PAYMENT_ID, MARKET_COMPENSATION, status, updatedAt,
                        receipts)
        ));
    }

    private Map<Long, Map<Long, ReturnForArchiving>> buildReturnWithUserCompensation() {
        return Map.of(ORDER_ID, Map.of(RETURN_ID, createReturnWithUserCompensation()));
    }

    private ReturnForArchiving createReturnWithUserCompensation() {
        ReturnForArchiving returnForArchiving = createReturn(REFUNDED, now);
        returnForArchiving.setUserCompensationNeeded(true);
        return returnForArchiving;
    }

    private Map<Long, Map<Long, PaymentForArchiving>> buildOrderAndUserCompensationPayments(
            PaymentStatus status, LocalDateTime updatedAt, Set<ReceiptForArchiving> receipts
    ) {
        return Map.of(ORDER_ID, Map.of(
                ORDER_PAYMENT_ID, createPayment(ORDER_PAYMENT_ID, ORDER_PREPAY, CLEARED, now,
                        buildOrderPaymentReceipts()),
                COMPENSATION_PAYMENT_ID, createPayment(
                        COMPENSATION_PAYMENT_ID,
                        USER_COMPENSATION,
                        status, updatedAt, receipts
                )));
    }

    private DeliveryForArchiving createOrderDelivery(DeliveryPartnerType type) {
        return new DeliveryForArchiving(type);
    }
}
