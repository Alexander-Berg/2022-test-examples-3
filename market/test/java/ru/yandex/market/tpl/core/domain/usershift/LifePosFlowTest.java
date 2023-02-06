package ru.yandex.market.tpl.core.domain.usershift;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import ru.yandex.market.tpl.api.model.order.OrderChequeRemoteDto;
import ru.yandex.market.tpl.api.model.order.OrderChequeType;
import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.receipt.lifepay.LifePayTransactionFilterDto;
import ru.yandex.market.tpl.api.model.receipt.lifepay.LifePayTransactionMetadata;
import ru.yandex.market.tpl.api.model.receipt.lifepay.LifePayTransactionNotificationDto;
import ru.yandex.market.tpl.api.model.receipt.lifepay.PaymentMethod;
import ru.yandex.market.tpl.api.model.receipt.lifepay.PaymentStatus;
import ru.yandex.market.tpl.api.model.receipt.lifepay.PaymentType;
import ru.yandex.market.tpl.api.model.receipt.report.ReceiptReportXFiltersDto;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointStatus;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskStatus;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.company.CompanyPermissionsProjection;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.receipt.ReceiptReportService;
import ru.yandex.market.tpl.core.domain.receipt.lifepay.LifePayService;
import ru.yandex.market.tpl.core.domain.receipt.lifepay.LifePayTransaction;
import ru.yandex.market.tpl.core.domain.receipt.lifepay.LifePayTransactionRepository;
import ru.yandex.market.tpl.core.domain.receipt.lifepay.partner.PartnerTransactionService;
import ru.yandex.market.tpl.core.domain.receipt.lifepay.partner.TransactionPage;
import ru.yandex.market.tpl.core.domain.receipt.report.PartnerReceiptReportX;
import ru.yandex.market.tpl.core.domain.receipt.report.PartnerReceiptReportXRepository;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.query.usershift.UserShiftQueryService;
import ru.yandex.market.tpl.core.test.TestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.LIFE_POS_RESERVE_FISCALIZATION_ENABLED;

@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class LifePosFlowTest {
    private static final String TERMINAL_SERIAL = "42";
    private static final String REQUEST_ID = "request_id";

    private final UserShiftCommandService commandService;
    private final TestDataFactory testDataFactory;
    private final TestUserHelper testUserHelper;
    private final LifePayService lifePayService;
    private final LifePayTransactionRepository lifePayTransactionRepository;
    private final DbQueueTestUtil dbQueueTestUtil;
    private final UserShiftRepository userShiftRepository;
    private final UserShiftCommandDataHelper helper;
    private final OrderRepository orderRepository;
    private final Clock clock;
    private final PartnerReceiptReportXRepository partnerReceiptReportXRepository;
    private final ReceiptReportService receiptReportService;
    private final PartnerTransactionService partnerTransactionService;
    private final UserShiftQueryService queryService;

    private User user;
    private UserShift userShift;
    private RoutePoint currentRoutePoint;
    private OrderDeliveryTask currentTask;
    private Order currentOrder;

    @MockBean
    private ConfigurationProviderAdapter configurationProviderAdapter;

    @BeforeEach
    void init() {
        initUserShift();
        when(configurationProviderAdapter.isBooleanEnabled(LIFE_POS_RESERVE_FISCALIZATION_ENABLED)).thenReturn(true);
    }

    @Test
    void transactionProcessingAfterPayAndRegisterCheque() {
        payOrder();

        assertOrderPaid();

        printCheque();

        assertRoutePointFinished();
        assertChequePrinted();
        assertTaskDelivered();

        receiveAndProcessPayTransaction();

        assertRoutePointFinished();
        assertPrintedChequesCountIsEqualTo(1);
        assertMetadataPresent();
    }

    @Test
    void transactionProcessingBeforePayAndRegisterCheque() {
        receiveAndProcessPayTransaction();

        assertOrderPaid();
        assertChequePrinted();
        assertRoutePointInProgress();
        assertTaskNotDelivered();

        payOrder();
        printCheque();

        assertTaskDelivered();
        assertRoutePointFinished();
        assertPrintedChequesCountIsEqualTo(1);

        assertMetadataPresent();
    }

    @Test
    void transactionProcessingBetweenPayAndRegisterCheque() {
        payOrder();

        assertOrderPaid();

        receiveAndProcessPayTransaction();

        assertRoutePointInProgress();
        assertChequePrinted();
        assertTaskNotDelivered();

        assertMetadataPresent();

        printCheque();

        assertTaskDelivered();
        assertRoutePointFinished();
        assertPrintedChequesCountIsEqualTo(1);
    }

    @Test
    void revertChequeTransactionProcessingBeforeRegisterCheque() {
        payOrder();
        printCheque();

        assertTaskDelivered();
        assertRoutePointFinished();
        assertPrintedChequesCountIsEqualTo(1);

        receiveAndProcessPayTransaction();
        receiveAndProcessRefundTransaction();
        assertTaskDelivered();
        assertRoutePointFinished();
        assertPrintedChequesCountIsEqualTo(2);
        assertMetadataPresent();

        revertCheque();

        assertTaskNotDelivered();
        assertRoutePointUnfinished();
        assertPrintedChequesCountIsEqualTo(2);
        assertMetadataPresent();
    }

    @Test
    void transactionProcessingAfterPrintAndRevertCheque() {
        payOrder();
        printCheque();
        revertCheque();

        receiveAndProcessRefundTransaction();
        receiveAndProcessPayTransaction();

        assertTaskNotDelivered();
        assertRoutePointUnfinished();
        assertPrintedChequesCountIsEqualTo(2);
        assertMetadataPresent();
    }

    @Test
    void chequesCreatedWithReserveFiscalizeAttribute() {
        receiveAndProcessPayTransaction();
        assertOrderPaid();
        assertTaskNotDelivered();
        assertRoutePointInProgress();
        assertPrintedChequesCountIsEqualTo(1);

        payOrder();
        printCheque();
        receiveAndProcessRefundTransaction();
        revertCheque();

        assertTaskNotDelivered();
        assertRoutePointUnfinished();
        assertPrintedChequesCountIsEqualTo(2);
        assertPrintedChequesReserveFiscalized();
        assertMetadataPresent();
    }

    @Test
    void revertTransactionProcessingAfterRegisterCheque() {
        payOrder();
        printCheque();

        assertTaskDelivered();
        assertRoutePointFinished();
        assertPrintedChequesCountIsEqualTo(1);

        revertCheque();
        assertTaskNotDelivered();
        assertRoutePointUnfinished();
        assertPrintedChequesCountIsEqualTo(2);

        receiveAndProcessRefundTransaction();

        assertTaskNotDelivered();
        assertRoutePointUnfinished();
        assertPrintedChequesCountIsEqualTo(2);
        assertMetadataPresent();
    }

    @Test
    void testXReportIsCorrectWithLifePosTransactions() {
        payOrder();
        printCheque();
        receiveAndProcessPayTransaction();
        revertCheque();
        receiveAndProcessRefundTransaction();

        var xReport = getUserShiftXReport();

        assertThat(xReport.getIncomeLifePos().compareTo(currentOrder.getTotalPrice())).isEqualTo(0);
        assertThat(xReport.getReturnLifePos().compareTo(currentOrder.getTotalPrice())).isEqualTo(0);
        assertThat(xReport.getIncomeCard().compareTo(currentOrder.getTotalPrice())).isEqualTo(0);
        assertThat(xReport.getReturnIncomeCard().compareTo(currentOrder.getTotalPrice())).isEqualTo(0);
        assertThat(xReport.getTerminalSerial()).isEqualTo(TERMINAL_SERIAL);
    }

    @Test
    void testXReportIsCorrectWithoutLifePosTransactions() {
        payOrder();
        printCheque();
        revertCheque();

        var xReport = getUserShiftXReport();

        assertThat(xReport.getIncomeLifePos().compareTo(BigDecimal.ZERO)).isEqualTo(0);
        assertThat(xReport.getReturnLifePos().compareTo(BigDecimal.ZERO)).isEqualTo(0);
        assertThat(xReport.getIncomeCard().compareTo(currentOrder.getTotalPrice())).isEqualTo(0);
        assertThat(xReport.getReturnIncomeCard().compareTo(currentOrder.getTotalPrice())).isEqualTo(0);
        assertThat(xReport.getTerminalSerial()).isNull();
    }

    @Test
    void testGetTransactionsForUserShift() {
        payOrder();
        printCheque();
        receiveAndProcessPayTransaction();
        revertCheque();
        receiveAndProcessRefundTransaction();

        var transactions = getUserShiftTransactions();

        assertThat(transactions).hasSize(2);
        var refundTransaction = transactions.getContent().get(0);
        assertThat(refundTransaction.getPaymentType()).isEqualTo(PaymentType.REFUND);
        assertThat(refundTransaction.getOrderIds()).containsExactly(currentOrder.getExternalOrderId());
        assertThat(refundTransaction.getCourierFullName()).isEqualTo(user.getFullName());
        assertThat(refundTransaction.getTerminalSerial()).isEqualTo(TERMINAL_SERIAL);
        assertThat(refundTransaction.getAmount().compareTo(currentOrder.getTotalPrice())).isEqualTo(0);

        var paymentTransaction = transactions.getContent().get(1);
        assertThat(paymentTransaction.getPaymentType()).isEqualTo(PaymentType.PAYMENT);
        assertThat(paymentTransaction.getOrderIds()).containsExactly(currentOrder.getExternalOrderId());
        assertThat(paymentTransaction.getCourierFullName()).isEqualTo(user.getFullName());
        assertThat(paymentTransaction.getTerminalSerial()).isEqualTo(TERMINAL_SERIAL);
        assertThat(paymentTransaction.getAmount().compareTo(currentOrder.getTotalPrice())).isEqualTo(0);
    }

    @Test
    void testGetTransactionsTotalSum() {
        payOrder();
        printCheque();
        receiveAndProcessPayTransaction();

        revertCheque();
        receiveAndProcessRefundTransaction();

        payOrder();
        printCheque();
        receiveAndProcessPayTransaction();

        var transactions = getUserShiftTransactions();

        assertThat(transactions).hasSize(3);
        assertThat(transactions.getSumTotal().compareTo(currentOrder.getTotalPrice())).isEqualTo(0);
    }

    @Test
    void processPayInDemoMode() {
        LocalDate date = LocalDate.now();
        user = testUserHelper.findOrCreateUser(1, date);
        Shift shift = testUserHelper.findOrCreateOpenShiftForSc(date, SortingCenter.DEMO_SC_ID);

        long userShiftId = testDataFactory.createEmptyShift(shift.getId(), user);
        long routePointId = testDataFactory.createEmptyRoutePoint(user, userShiftId).getId();
        testDataFactory.addDeliveryTaskManual(user, userShiftId, routePointId,
                OrderGenerateService.OrderGenerateParam.builder()
                        .paymentStatus(OrderPaymentStatus.UNPAID)
                        .paymentType(OrderPaymentType.CARD)
                        .build());

        commandService.checkin(user, new UserShiftCommand.CheckIn(userShiftId));
        commandService.startShift(user, new UserShiftCommand.Start(userShiftId));

        userShift = userShiftRepository.findById(userShiftId).orElseThrow();
        testUserHelper.finishPickupAtStartOfTheDay(userShift);

        commandService.arriveAtRoutePoint(user,
                new UserShiftCommand.ArriveAtRoutePoint(userShiftId, routePointId,
                        helper.getLocationDto(userShiftId)));

        currentRoutePoint = userShift.getCurrentRoutePoint();
        currentTask = currentRoutePoint.streamOrderDeliveryTasks()
                .findAny()
                .orElseThrow();
        currentOrder = orderRepository.findByIdOrThrow(currentTask.getOrderId());

        payOrder();
        assertOrderPaid();

        printCheque();

        assertRoutePointFinished();
        assertThat(currentOrder.getCheques()).hasSize(1);

        assertTaskDelivered();
    }

    private void initUserShift() {
        LocalDate date = LocalDate.now();
        user = testUserHelper.findOrCreateUser(3217L, date);
        Shift shift = testUserHelper.findOrCreateOpenShift(date);

        long userShiftId = testDataFactory.createEmptyShift(shift.getId(), user);
        long routePointId = testDataFactory.createEmptyRoutePoint(user, userShiftId).getId();
        testDataFactory.addDeliveryTaskManual(user, userShiftId, routePointId,
                OrderGenerateService.OrderGenerateParam.builder()
                        .paymentStatus(OrderPaymentStatus.UNPAID)
                        .paymentType(OrderPaymentType.CARD)
                        .build());

        commandService.checkin(user, new UserShiftCommand.CheckIn(userShiftId));
        commandService.startShift(user, new UserShiftCommand.Start(userShiftId));

        userShift = userShiftRepository.findById(userShiftId).orElseThrow();
        testUserHelper.finishPickupAtStartOfTheDay(userShift);

        commandService.arriveAtRoutePoint(user,
                new UserShiftCommand.ArriveAtRoutePoint(userShiftId, routePointId,
                        helper.getLocationDto(userShiftId)));

        currentRoutePoint = userShift.getCurrentRoutePoint();
        currentTask = currentRoutePoint.streamOrderDeliveryTasks()
                .findAny()
                .orElseThrow();
        currentOrder = orderRepository.findByIdOrThrow(currentTask.getOrderId());
    }

    private void payOrder() {
        commandService.payOrder(user,
                new UserShiftCommand.PayOrder(
                        userShift.getId(),
                        currentRoutePoint.getId(),
                        currentTask.getId(),
                        OrderPaymentType.CARD,
                        null
                )
        );
    }

    private void printCheque() {
        commandService.printCheque(user,
                new UserShiftCommand.PrintOrReturnCheque(
                        userShift.getId(),
                        currentRoutePoint.getId(),
                        currentTask.getId(),
                        new OrderChequeRemoteDto(OrderPaymentType.CARD, OrderChequeType.SELL),
                        Instant.now(clock),
                        false,
                        null,
                        Optional.empty()
                )
        );
    }

    private void revertCheque() {
        commandService.returnCheque(user,
                new UserShiftCommand.PrintOrReturnCheque(
                        userShift.getId(),
                        currentRoutePoint.getId(),
                        currentTask.getId(),
                        new OrderChequeRemoteDto(OrderPaymentType.CARD, OrderChequeType.RETURN),
                        Instant.now(clock),
                        false,
                        null,
                        Optional.empty()
                )
        );
    }

    private void receiveAndProcessPayTransaction() {
        receiveAndProcessTransaction(PaymentType.PAYMENT);
    }

    private void receiveAndProcessRefundTransaction() {
        receiveAndProcessTransaction(PaymentType.REFUND);
    }

    private void receiveAndProcessTransaction(PaymentType paymentType) {
        receiveTransactionNotification(
                List.of(currentTask.getId()),
                PaymentStatus.SUCCESS,
                PaymentMethod.CARD,
                paymentType
        );
        dbQueueTestUtil.executeAllQueueItems(QueueType.PROCESS_LIFE_POS_TRANSACTION);
    }

    private LifePayTransaction receiveTransactionNotification(
            List<Long> taskIds,
            PaymentStatus paymentStatus,
            PaymentMethod paymentMethod,
            PaymentType paymentType
    ) {
        var number = UUID.randomUUID().toString();
        var transaction = new LifePayTransactionNotificationDto();

        transaction.setNumber(number);

        transaction.setStatus(paymentStatus);
        transaction.setMethod(paymentMethod);
        transaction.setType(paymentType);
        transaction.setAmount(currentOrder.getTotalPrice().toString());
        transaction.setTerminalSerial(TERMINAL_SERIAL);
        transaction.setCreated(Instant.now());

        transaction.setMetadata(new LifePayTransactionMetadata(taskIds.stream()
                .map(Object::toString)
                .collect(Collectors.joining(",")),
                REQUEST_ID
        ));
        lifePayService.saveTransaction(transaction);

        return lifePayTransactionRepository.findByNumber(number).orElseThrow();
    }

    private void assertRoutePointFinished() {
        assertRoutePointHasStatus(RoutePointStatus.FINISHED);
        assertThat(userShift.getCurrentRoutePoint()).isNotEqualTo(currentRoutePoint);
    }

    private void assertRoutePointUnfinished() {
        assertRoutePointHasStatus(RoutePointStatus.UNFINISHED);
        assertThat(userShift.getCurrentRoutePoint()).isEqualTo(currentRoutePoint);
    }

    private void assertRoutePointInProgress() {
        assertRoutePointHasStatus(RoutePointStatus.IN_PROGRESS);
        assertThat(userShift.getCurrentRoutePoint()).isEqualTo(currentRoutePoint);
    }

    private void assertRoutePointHasStatus(RoutePointStatus routePointStatus) {
        assertThat(currentRoutePoint.getStatus()).isEqualTo(routePointStatus);
    }

    private void assertTaskDelivered() {
        assertTaskHasStatus(OrderDeliveryTaskStatus.DELIVERED);
    }

    private void assertTaskNotDelivered() {
        assertTaskHasStatus(OrderDeliveryTaskStatus.NOT_DELIVERED);
    }

    private void assertTaskHasStatus(OrderDeliveryTaskStatus status) {
        assertThat(currentTask.getStatus()).isEqualTo(status);
    }

    private void assertOrderPaid() {
        assertThat(currentOrder.getPaymentStatus()).isEqualTo(OrderPaymentStatus.PAID);
    }

    private void assertChequePrinted() {
        assertThat(currentOrder.getCheques()).hasSize(1);
        dbQueueTestUtil.assertQueueHasSize(QueueType.RECEIPT_FISCALIZE, 1);
    }

    private void assertPrintedChequesCountIsEqualTo(int count) {
        assertThat(currentOrder.getCheques()).hasSize(count);
        dbQueueTestUtil.assertQueueHasSize(QueueType.RECEIPT_FISCALIZE, count);
    }

    private PartnerReceiptReportX getUserShiftXReport() {
        var filters = new ReceiptReportXFiltersDto();
        filters.setUserShiftId(userShift.getId());
        var specification = receiptReportService.<PartnerReceiptReportX>specification(filters,
                CompanyPermissionsProjection.builder().isSuperCompany(true).build());
        var result = partnerReceiptReportXRepository.findAll(specification, Pageable.unpaged());
        var content = result.getContent();
        assertThat(content).hasSize(1);
        return content.iterator().next();
    }

    private TransactionPage getUserShiftTransactions() {
        var filters = new LifePayTransactionFilterDto();
        filters.setUserShiftId(userShift.getId());
        var pageable = PageRequest.of(0, 100, Sort.Direction.DESC, "transactionTime");
        return partnerTransactionService.getTransactions(filters, pageable,
                CompanyPermissionsProjection.builder().isSuperCompany(true).build());
    }

    private void assertPrintedChequesReserveFiscalized() {
        currentOrder.getCheques()
                .forEach(cheque -> assertThat(cheque.getReserveFiscalized()).isTrue());
    }

    private void assertMetadataPresent() {
        var orderDeliveryTaskDto = queryService.getDeliveryTaskInfo(
                user, currentRoutePoint.getId(), currentTask.getId());
        var metadata = orderDeliveryTaskDto.getMetadata();
        assertThat(metadata).isNotNull();
        assertThat(metadata.getRequestId()).isEqualTo(REQUEST_ID);
        assertThat(metadata.getTaskIds()).isEqualTo(currentTask.getId().toString());
    }
}
