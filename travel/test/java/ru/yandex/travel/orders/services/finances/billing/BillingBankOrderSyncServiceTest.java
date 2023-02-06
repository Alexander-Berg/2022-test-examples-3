package ru.yandex.travel.orders.services.finances.billing;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.travel.commons.proto.ProtoCurrencyUnit;
import ru.yandex.travel.integration.balance.BillingCsvApiClient;
import ru.yandex.travel.integration.balance.model.csv.BillingCurrency;
import ru.yandex.travel.integration.balance.model.csv.PaymentType;
import ru.yandex.travel.integration.balance.model.csv.TransactionType;
import ru.yandex.travel.orders.entities.finances.BankOrder;
import ru.yandex.travel.orders.entities.finances.BankOrderDetail;
import ru.yandex.travel.orders.entities.finances.BankOrderPayment;
import ru.yandex.travel.orders.entities.finances.BankOrderPaymentDetailsStatus;
import ru.yandex.travel.orders.entities.finances.BankOrderStatus;
import ru.yandex.travel.orders.entities.finances.BillingTransactionPaymentType;
import ru.yandex.travel.orders.entities.finances.BillingTransactionType;
import ru.yandex.travel.orders.entities.finances.OebsPaymentStatus;
import ru.yandex.travel.orders.repository.finances.BankOrderDetailRepository;
import ru.yandex.travel.orders.repository.finances.BankOrderPaymentRepository;
import ru.yandex.travel.orders.repository.finances.BankOrderRepository;
import ru.yandex.travel.testing.time.SettableClock;
import ru.yandex.travel.workflow.single_operation.SingleOperationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.travel.orders.services.finances.billing.RandomObjects.createBankOrder;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
//@TestExecutionListeners(
//        listeners = TruncateDatabaseTestExecutionListener.class,
//        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS
//)
public class BillingBankOrderSyncServiceTest {

    private static final ZoneId TEST_ZONE = ZoneId.of("Europe/Moscow");
    private static final String PAYMENT_BATCH_ID = "100";
    private static final String BANK_ORDER_ID = "200";

    @Autowired
    private BankOrderRepository bankOrderRepository;

    @Autowired
    private BankOrderPaymentRepository bankOrderPaymentRepository;

    @Autowired
    private BankOrderDetailRepository bankOrderDetailRepository;

    @Mock
    private BillingCsvApiClient billingCsvApiClient;

    private SettableClock clock = new SettableClock();

    private BillingBankOrderSyncService bankOrderSyncService;

    private BillingBankOrderSyncProperties billingBankOrderSyncProperties;

    @Mock
    private SingleOperationService singleOperationService;

    private BankOrder bankOrderInDatabase;

    @Before
    public void setUp() {
        bankOrderRepository.deleteAll();
        bankOrderPaymentRepository.deleteAll();
        bankOrderDetailRepository.deleteAll();
        billingBankOrderSyncProperties = BillingBankOrderSyncProperties.builder().bankPaymentCheckAttempts(1)
                .bankPaymentAttemptDelay(Duration.ofMinutes(1))
                .emailsSendingDelay(Duration.ZERO)
                .maxIntervalDays(30)
                .build();
        bankOrderSyncService = new BillingBankOrderSyncService(billingCsvApiClient, bankOrderRepository,
                bankOrderPaymentRepository, billingBankOrderSyncProperties, singleOperationService, clock);

        bankOrderInDatabase = new BankOrder();
        bankOrderInDatabase.setBankOrderId(BANK_ORDER_ID);
        bankOrderInDatabase.setOebsPaymentStatus(OebsPaymentStatus.CREATED);
        bankOrderInDatabase.setStatus(BankOrderStatus.STARTED);
        bankOrderInDatabase.setSum(BigDecimal.TEN);
        bankOrderInDatabase.setServiceId(123);
        bankOrderInDatabase.setEventtime(LocalDate.of(2020, Month.MAY, 30));
        bankOrderInDatabase.setTrantime(LocalDate.of(2020, Month.MAY, 30).atTime(7, 40));

        BankOrderPayment bankOrderPaymentInDatabase = new BankOrderPayment();
        bankOrderPaymentInDatabase.setPaymentBatchId(PAYMENT_BATCH_ID);

        BankOrderDetail bankOrderInDatabaseDetail = new BankOrderDetail();
        bankOrderInDatabaseDetail.setTrustPaymentId("123");
        bankOrderInDatabaseDetail.setSum(BigDecimal.TEN);
        bankOrderInDatabaseDetail.setTransactionType(BillingTransactionType.PAYMENT);
        bankOrderInDatabaseDetail.setPaymentType(BillingTransactionPaymentType.COST);
        bankOrderInDatabaseDetail.setCurrency(ProtoCurrencyUnit.RUB);
        bankOrderInDatabaseDetail.setHandlingTime(LocalDate.of(2020, Month.MAY, 30));
        bankOrderInDatabaseDetail.setPaymentTime(LocalDate.of(2020, Month.MAY, 30));

        final List<BankOrderDetail> details = new ArrayList<>();
        details.add(bankOrderInDatabaseDetail);

        bankOrderInDatabase.setBankOrderPayment(bankOrderPaymentInDatabase);
        bankOrderInDatabase.getBankOrderPayment().setDetails(details);
    }

    private void initDb() {
        bankOrderRepository.save(bankOrderInDatabase);
    }

    @Test
    @Transactional
    public void testSyncFromDefaultDateToTodayWithEmptyTable() {
        clock.setCurrentTime(ZonedDateTime.of(
                LocalDate.of(2019, Month.MARCH, 8),
                LocalTime.of(12, 0),
                TEST_ZONE
        ).toInstant());


        bankOrderSyncService.syncNewBankOrders("ignored");

        assertThat(bankOrderSyncService.getIntervalsForFetching().stream()
                .anyMatch(el ->
                        el.getFrom().compareTo(LocalDate.of(2019, Month.JANUARY, 1)) == 0
                                && el.getTo().compareTo(LocalDate.of(2019, Month.JANUARY, 30)) == 0)).isEqualTo(true);
        assertThat(bankOrderSyncService.getIntervalsForFetching().stream()
                .anyMatch(el ->
                        el.getFrom().compareTo(LocalDate.of(2019, Month.JANUARY, 31)) == 0
                                && el.getTo().compareTo(LocalDate.of(2019, Month.MARCH, 1)) == 0)).isEqualTo(true);
        assertThat(bankOrderSyncService.getIntervalsForFetching().stream()
                .anyMatch(el ->
                        el.getFrom().compareTo(LocalDate.of(2019, Month.MARCH, 2)) == 0
                                && el.getTo().compareTo(LocalDate.of(2019, Month.MARCH, 8)) == 0)).isEqualTo(true);
    }

    @Test
    @Transactional
    public void testSyncFromStartDateToTodayWithEmptyTable() {
        clock.setCurrentTime(ZonedDateTime.of(
                LocalDate.of(2020, Month.MARCH, 8),
                LocalTime.of(12, 0),
                TEST_ZONE
        ).toInstant());

        billingBankOrderSyncProperties.setStartDate(LocalDate.of(2020, Month.JANUARY, 1));

        bankOrderSyncService.syncNewBankOrders("ignored");

        assertThat(bankOrderSyncService.getIntervalsForFetching().stream()
                .anyMatch(el ->
                        el.getFrom().compareTo(LocalDate.of(2020, Month.JANUARY, 1)) == 0
                                && el.getTo().compareTo(LocalDate.of(2020, Month.JANUARY, 30)) == 0)).isEqualTo(true);
        assertThat(bankOrderSyncService.getIntervalsForFetching().stream()
                .anyMatch(el ->
                        el.getFrom().compareTo(LocalDate.of(2020, Month.JANUARY, 31)) == 0
                                && el.getTo().compareTo(LocalDate.of(2020, Month.FEBRUARY, 29)) == 0)).isEqualTo(true);
        assertThat(bankOrderSyncService.getIntervalsForFetching().stream()
                .anyMatch(el ->
                        el.getFrom().compareTo(LocalDate.of(2020, Month.MARCH, 1)) == 0
                                && el.getTo().compareTo(LocalDate.of(2020, Month.MARCH, 8)) == 0)).isEqualTo(true);

        billingBankOrderSyncProperties.setStartDate(null);
    }



    @Test
    @Transactional
    public void testSyncFromLatestUnprocessedRecordDate() {
        clock.setCurrentTime(ZonedDateTime.of(
                LocalDate.of(2019, Month.NOVEMBER, 5),
                LocalTime.of(12, 0),
                TEST_ZONE
        ).toInstant());

        final List<BankOrder> orders = List.of(
                createBankOrder(b -> {
                    b.setBankOrderId("1");
                    b.setOebsPaymentStatus(OebsPaymentStatus.RECONCILED);
                    b.setEventtime(LocalDate.of(2019, Month.NOVEMBER, 1));
                }),
                createBankOrder(b -> {
                    b.setBankOrderId("2");
                    b.setEventtime(LocalDate.of(2019, Month.NOVEMBER, 3));
                    b.setOebsPaymentStatus(OebsPaymentStatus.CREATED);
                }),
                createBankOrder(b -> {
                    b.setBankOrderId("3");
                    b.setEventtime(LocalDate.of(2019, Month.NOVEMBER, 4));
                    b.setOebsPaymentStatus(OebsPaymentStatus.RETURNED);
                })
        );
        bankOrderRepository.saveAll(orders);

        bankOrderSyncService.syncNewBankOrders("ignored");

        assertThat(bankOrderSyncService.getIntervalsForFetching().stream()
                .anyMatch(el ->
                        el.getFrom().compareTo(LocalDate.of(2019, Month.NOVEMBER, 3)) == 0
                                && el.getTo().compareTo(LocalDate.of(2019, Month.NOVEMBER, 5)) == 0)).isEqualTo(true);
    }

    @Test
    @Transactional
    public void testSyncFromLatestProcessedRecordDateIfThereIsNoUnprocessed() {
        clock.setCurrentTime(ZonedDateTime.of(
                LocalDate.of(2019, Month.NOVEMBER, 5),
                LocalTime.of(12, 0),
                TEST_ZONE
        ).toInstant());

        final List<BankOrder> orders = List.of(
                createBankOrder(b -> {
                    b.setBankOrderId("1");
                    b.setOebsPaymentStatus(OebsPaymentStatus.RECONCILED);
                    b.setEventtime(LocalDate.of(2019, Month.NOVEMBER, 1));
                }),
                createBankOrder(b -> {
                    b.setBankOrderId("2");
                    b.setEventtime(LocalDate.of(2019, Month.NOVEMBER, 3));
                    b.setOebsPaymentStatus(OebsPaymentStatus.RECONCILED);
                }),
                createBankOrder(b -> {
                    b.setBankOrderId("3");
                    b.setEventtime(LocalDate.of(2019, Month.NOVEMBER, 4));
                    b.setOebsPaymentStatus(OebsPaymentStatus.RETURNED);
                })
        );
        bankOrderRepository.saveAll(orders);

        bankOrderSyncService.syncNewBankOrders("ignored");

        assertThat(bankOrderSyncService.getIntervalsForFetching().stream()
                .anyMatch(el ->
                        el.getFrom().compareTo(LocalDate.of(2019, Month.NOVEMBER, 4)) == 0
                                && el.getTo().compareTo(LocalDate.of(2019, Month.NOVEMBER, 5)) == 0)).isEqualTo(true);
    }

    @Test
    @Transactional
    public void testServiceSaveBankOrder() {
        when(billingCsvApiClient.getBankOrders(any(), any(), any())).thenReturn(List.of(
                ru.yandex.travel.integration.balance.model.csv.BankOrder.builder()
                        .paymentBatchId(PAYMENT_BATCH_ID)
                        .bankOrderId(BANK_ORDER_ID)
                        .status(ru.yandex.travel.integration.balance.model.csv.BankOrderStatus.DONE)
                        .oebsPaymentStatus(ru.yandex.travel.integration.balance.model.csv.OebsPaymentStatus.RECONCILED)
                        .sum(BigDecimal.TEN)
                        .serviceId(123)
                        .eventtime(LocalDate.of(2020, Month.MAY, 9))
                        .trantime(LocalDate.of(2020, Month.MAY, 10).atStartOfDay())
                        .description("description 1")
                        .build()
        ));
        when(billingCsvApiClient.getBankOrderDetails(any())).thenReturn(List.of(
                ru.yandex.travel.integration.balance.model.csv.BankOrderDetail.builder()
                        .paymentBatchId(PAYMENT_BATCH_ID)
                        .trustRefundId("123")
                        .paymentTime(LocalDate.of(2020, Month.MAY, 9))
                        .handlingTime(LocalDate.of(2020, Month.MAY, 10))
                        .sum(BigDecimal.TEN)
                        .currency(BillingCurrency.RUR)
                        .transactionType(TransactionType.PAYMENT)
                        .paymentType(PaymentType.COST)
                        .build()
        ));

        BillingBankOrderSyncService.DateInterval intervalForFetching =  new BillingBankOrderSyncService.DateInterval(
                LocalDate.of(2020, Month.MAY, 1),
                LocalDate.of(2022, Month.MAY, 30)
        );
        bankOrderSyncService.fetchBatchBankOrders(intervalForFetching);

        final Optional<BankOrder> found =
                Optional.ofNullable(bankOrderRepository.findByPaymentBatchIdAndBankOrderId(
                        PAYMENT_BATCH_ID, BANK_ORDER_ID));
        final BankOrder bankOrder = found.orElseThrow();

        assertThat(PAYMENT_BATCH_ID).isEqualTo(bankOrder.getBankOrderPayment().getPaymentBatchId());
        assertThat(BANK_ORDER_ID).isEqualTo(bankOrder.getBankOrderId());
        assertThat("description 1").isEqualTo(bankOrder.getDescription());
        assertThat(BigDecimal.TEN).isEqualTo(bankOrder.getSum());
        assertThat(bankOrder.getBankOrderPayment().getStatus()).isEqualTo(BankOrderPaymentDetailsStatus.NEW);
        assertThat(bankOrder.getBankOrderPayment().getDetails()).hasSize(0);

        bankOrderSyncService.syncBankOrderPaymentDetails(bankOrder.getBankOrderPayment().getPaymentBatchId());

        final Optional<BankOrder> refreshedFound =
                Optional.ofNullable(bankOrderRepository.findByPaymentBatchIdAndBankOrderId(
                        PAYMENT_BATCH_ID, BANK_ORDER_ID));
        final BankOrder refreshedBankOrder = refreshedFound.orElseThrow();

        assertThat(1).isEqualTo(refreshedBankOrder.getBankOrderPayment().getDetails().size());
        final BankOrderDetail bankOrderDetail = refreshedBankOrder.getBankOrderPayment().getDetails().get(0);
        assertThat(LocalDate.of(2020, Month.MAY, 9)).isEqualTo(bankOrderDetail.getPaymentTime());
    }

    @Test
    @Transactional
    public void testServiceUpdatesInfo() {
        initDb();

        when(billingCsvApiClient.getBankOrders(any(), any(), any())).thenReturn(List.of(
                ru.yandex.travel.integration.balance.model.csv.BankOrder.builder()
                        .paymentBatchId(PAYMENT_BATCH_ID)
                        .bankOrderId(BANK_ORDER_ID)
                        .status(ru.yandex.travel.integration.balance.model.csv.BankOrderStatus.DONE)
                        .oebsPaymentStatus(ru.yandex.travel.integration.balance.model.csv.OebsPaymentStatus.RECONCILED)
                        .sum(BigDecimal.TEN)
                        .serviceId(123)
                        .eventtime(LocalDate.of(2020, Month.MAY, 30))
                        .trantime(LocalDate.of(2020, Month.MAY, 31).atTime(4, 20))
                        .build()
        ));

        BillingBankOrderSyncService.DateInterval intervalForFetching =  new BillingBankOrderSyncService.DateInterval(
                LocalDate.of(2020, Month.MAY, 20),
                LocalDate.of(2022, Month.JUNE, 10)
        );
        bankOrderSyncService.fetchBatchBankOrders(intervalForFetching);

        final Optional<BankOrder> found =
                Optional.ofNullable(bankOrderRepository.findByPaymentBatchIdAndBankOrderId(
                        PAYMENT_BATCH_ID, BANK_ORDER_ID));
        final BankOrder bankOrder = found.orElseThrow();

        assertThat(PAYMENT_BATCH_ID).isEqualTo(bankOrder.getBankOrderPayment().getPaymentBatchId());
        assertThat(BANK_ORDER_ID).isEqualTo(bankOrder.getBankOrderId());
        assertThat(BigDecimal.TEN).isEqualTo(bankOrder.getSum());
        assertThat(LocalDate.of(2020, Month.MAY, 30)).isEqualTo(bankOrder.getEventtime());
        assertThat(LocalDate.of(2020, Month.MAY, 31).atTime(4, 20)).isEqualTo(bankOrder.getTrantime());
        assertThat(BankOrderStatus.DONE).isEqualTo(bankOrder.getStatus());
        assertThat(OebsPaymentStatus.RECONCILED).isEqualTo(bankOrder.getOebsPaymentStatus());
        assertThat(1).isEqualTo(bankOrder.getBankOrderPayment().getDetails().size());

        final BankOrderDetail bankOrderDetail = bankOrder.getBankOrderPayment().getDetails().get(0);
        assertThat(LocalDate.of(2020, Month.MAY, 30)).isEqualTo(bankOrderDetail.getPaymentTime());

        verify(billingCsvApiClient, never()).getBankOrderDetails(any());
    }

    @Test
    @Transactional
    public void testDeduplicationOnUpdate() {
        bankOrderInDatabase.setBankOrderId(null);

        initDb();

        assertThat(bankOrderRepository.findByPaymentBatchIdAndBankOrderId(PAYMENT_BATCH_ID, BANK_ORDER_ID)).isNull();

        when(billingCsvApiClient.getBankOrders(any(), any(), any())).thenReturn(List.of(
                ru.yandex.travel.integration.balance.model.csv.BankOrder.builder()
                        .paymentBatchId(PAYMENT_BATCH_ID)
                        .bankOrderId(BANK_ORDER_ID)
                        .status(ru.yandex.travel.integration.balance.model.csv.BankOrderStatus.DONE)
                        .oebsPaymentStatus(ru.yandex.travel.integration.balance.model.csv.OebsPaymentStatus.RECONCILED)
                        .sum(BigDecimal.TEN)
                        .serviceId(123)
                        .eventtime(LocalDate.of(2020, Month.MAY, 30))
                        .trantime(LocalDate.of(2020, Month.MAY, 31).atTime(4, 20))
                        .build()
        ));
        BillingBankOrderSyncService.DateInterval intervalForFetching =  new BillingBankOrderSyncService.DateInterval(
                LocalDate.of(2020, Month.MAY, 20),
                LocalDate.of(2022, Month.JUNE, 10)
        );
        bankOrderSyncService.fetchBatchBankOrders(intervalForFetching);


        assertThat(bankOrderRepository.findByPaymentBatchIdAndBankOrderId(PAYMENT_BATCH_ID, BANK_ORDER_ID)).isNotNull();
        assertThat(bankOrderRepository.count()).isEqualTo(1);
    }
}
