package ru.yandex.market.billing.fulfillment.orders;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.common.util.currency.Currency;
import ru.yandex.common.util.exception.ExceptionCollector;
import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.categories.db.DbSupplierCategoryFeeService;
import ru.yandex.market.billing.core.order.model.ValueType;
import ru.yandex.market.billing.fulfillment.OrderType;
import ru.yandex.market.billing.fulfillment.orders.commission.SupplierCategoryFee;
import ru.yandex.market.billing.fulfillment.tariffs.FulfillmentTariff;
import ru.yandex.market.billing.fulfillment.tariffs.PriceAndTariffValue;
import ru.yandex.market.billing.fulfillment.tariffs.PriceAndTariffValueService;
import ru.yandex.market.billing.fulfillment.tariffs.TariffValue;
import ru.yandex.market.billing.fulfillment.tariffs.TariffsIterator;
import ru.yandex.market.billing.fulfillment.tariffs.TariffsService;
import ru.yandex.market.billing.model.billing.BillingServiceType;
import ru.yandex.market.billing.order.model.BillingUnit;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.date.Period;
import ru.yandex.market.core.order.DbOrderDao;
import ru.yandex.market.core.order.OrderTrantimeDao;
import ru.yandex.market.core.order.model.OrderItemForBilling;
import ru.yandex.market.mbi.tariffs.client.model.BillingUnitEnum;
import ru.yandex.market.mbi.tariffs.client.model.CancelledOrderFeeJsonSchema;
import ru.yandex.market.mbi.tariffs.client.model.CommonJsonSchema;
import ru.yandex.market.mbi.tariffs.client.model.Partner;
import ru.yandex.market.mbi.tariffs.client.model.TariffDTO;
import ru.yandex.market.mbi.tariffs.client.model.TariffFindQuery;

import static java.util.Collections.emptySet;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.market.billing.fulfillment.tariffs.TestTariffCreationUtil.createFulfillmentTariff;

@SuppressWarnings("ParameterNumber")
class FulfillmentOrderBillingServiceDbUnitTest extends FunctionalTest {

    private static final LocalDate BILLING_DATE = LocalDate.of(2018, 2, 1);
    private static final LocalDate JUNE_10_2020 = LocalDate.of(2020, 6, 10);
    private static final LocalDate JULY_01_2021 = LocalDate.of(2021, 7, 1);
    private static final LocalDate FEBRUARY_16_2021 = LocalDate.of(2021, 2, 16);
    private static final LocalDate OCTOBER_01_2021 = LocalDate.of(2021, 10, 1);
    private static final LocalDate BILLING_DATE_2020_10_02 = LocalDate.of(2020, 10, 2);
    private static final List<Pair<SupplierCategoryFee, OrderType>> DEFAULT_FEE = List.of(
            createSupplierCategoryFee(
                    90401,
                    50,
                    LocalDate.of(2018, 1, 1),
                    LocalDate.of(2018, 2, 1),
                    OrderType.FULFILLMENT),
            createSupplierCategoryFee(
                    90402,
                    50,
                    LocalDate.of(2018, 1, 1),
                    LocalDate.of(2018, 2, 1),
                    OrderType.FULFILLMENT),
            createSupplierCategoryFee(
                    90403,
                    50,
                    LocalDate.of(2018, 1, 1),
                    LocalDate.of(2018, 2, 1),
                    OrderType.FULFILLMENT),
            createSupplierCategoryFee(
                    90401,
                    30,
                    LocalDate.of(2018, 2, 1),
                    LocalDate.MAX,
                    OrderType.FULFILLMENT),
            createSupplierCategoryFee(
                    90402,
                    30,
                    LocalDate.of(2018, 2, 1),
                    LocalDate.MAX,
                    OrderType.FULFILLMENT),
            createSupplierCategoryFee(
                    90403,
                    30,
                    LocalDate.of(2018, 2, 1),
                    LocalDate.MAX,
                    OrderType.FULFILLMENT),
            createSupplierCategoryFee(
                    90404,
                    100,
                    LocalDate.of(2018, 2, 1),
                    LocalDate.MAX,
                    OrderType.DROP_SHIP)
    );
    private static final List<FulfillmentTariff> DEFAULT_TARIFF = List.of(
            createFulfillmentTariff(
                    LocalDate.of(2018, 2, 1),
                    LocalDate.MAX,
                    BillingServiceType.FF_PROCESSING,
                    null,
                    null,
                    800,
                    OrderType.FULFILLMENT)
    );
    private static final List<Pair<SupplierCategoryFee, OrderType>> BILL_LOYALTY_LIST = List.of(
            createSupplierCategoryFee(
                    90401,
                    50,
                    LocalDate.of(2018, 1, 1),
                    LocalDate.of(2018, 2, 1),
                    OrderType.DROP_SHIP
            ),
            createSupplierCategoryFee(
                    90402,
                    50,
                    LocalDate.of(2018, 1, 1),
                    LocalDate.of(2018, 2, 1),
                    OrderType.DROP_SHIP
            ),
            createSupplierCategoryFee(
                    90403,
                    50,
                    LocalDate.of(2018, 1, 1),
                    LocalDate.of(2018, 2, 1),
                    OrderType.DROP_SHIP
            ),
            createSupplierCategoryFee(
                    90401,
                    30,
                    LocalDate.of(2018, 2, 1),
                    LocalDate.MAX,
                    OrderType.DROP_SHIP
            ),
            createSupplierCategoryFee(
                    90402,
                    30,
                    LocalDate.of(2018, 2, 1),
                    LocalDate.MAX,
                    OrderType.DROP_SHIP
            ),
            createSupplierCategoryFee(
                    90403,
                    30,
                    LocalDate.of(2018, 2, 1),
                    LocalDate.MAX,
                    OrderType.DROP_SHIP
            ),
            createSupplierCategoryFee(
                    90404,
                    100,
                    LocalDate.of(2018, 2, 1),
                    LocalDate.MAX,
                    OrderType.DROP_SHIP
            ),
            createSupplierCategoryFee(
                    90401,
                    null,
                    500,
                    LocalDate.of(2018, 1, 1),
                    LocalDate.of(2018, 2, 1),
                    BillingServiceType.LOYALTY_PARTICIPATION_FEE,
                    OrderType.DROP_SHIP
            ),
            createSupplierCategoryFee(
                    90402,
                    null,
                    500,
                    LocalDate.of(2018, 1, 1),
                    LocalDate.of(2018, 2, 1),
                    BillingServiceType.LOYALTY_PARTICIPATION_FEE,
                    OrderType.DROP_SHIP
            ),
            createSupplierCategoryFee(
                    90403,
                    null,
                    500,
                    LocalDate.of(2018, 1, 1),
                    LocalDate.of(2018, 2, 1),
                    BillingServiceType.LOYALTY_PARTICIPATION_FEE,
                    OrderType.DROP_SHIP
            ),
            createSupplierCategoryFee(
                    90401,
                    null,
                    300,
                    LocalDate.of(2018, 2, 1),
                    LocalDate.MAX,
                    BillingServiceType.LOYALTY_PARTICIPATION_FEE,
                    OrderType.DROP_SHIP
            ),
            createSupplierCategoryFee(
                    90402,
                    null,
                    300,
                    LocalDate.of(2018, 2, 1),
                    LocalDate.MAX,
                    BillingServiceType.LOYALTY_PARTICIPATION_FEE,
                    OrderType.DROP_SHIP
            ),
            createSupplierCategoryFee(
                    90403,
                    null,
                    300,
                    LocalDate.of(2018, 2, 1),
                    LocalDate.MAX,
                    BillingServiceType.LOYALTY_PARTICIPATION_FEE,
                    OrderType.DROP_SHIP
            ),
            createSupplierCategoryFee(
                    90404,
                    null,
                    1000,
                    LocalDate.of(2018, 2, 1),
                    LocalDate.MAX,
                    BillingServiceType.LOYALTY_PARTICIPATION_FEE,
                    OrderType.DROP_SHIP
            )
    );
    private static final List<Pair<SupplierCategoryFee, OrderType>> BILL_LOYALTY_MIN_FEE_LIST = List.of(
            createSupplierCategoryFee(
                    90401,
                    666L,
                    10000000,
                    LocalDate.of(2018, 1, 1),
                    LocalDate.MAX,
                    BillingServiceType.LOYALTY_PARTICIPATION_FEE,
                    OrderType.DROP_SHIP
            )
    );
    @Autowired
    @Qualifier("testFulfillmentOrderBillingService")
    private FulfillmentOrderBillingService fulfillmentOrderBillingService;
    @Autowired
    @Qualifier("clientTariffsService")
    private TariffsService tariffsService;
    @Autowired
    @Qualifier("testDbSupplierCategoryFeeDao")
    private DbSupplierCategoryFeeService dbSupplierCategoryFeeDao;
    @Autowired
    @Qualifier("clientTariffsService")
    private TariffsService fulfillmentTariffDao;
    private ExceptionCollector exceptionCollector;

    @Autowired
    private OrderTrantimeDao orderTrantimeDao;
    @Autowired
    private DbOrderDao dbOrderDao;

    private static Instant createInstant(LocalDate date) {
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant();
    }

    private static Pair<SupplierCategoryFee, OrderType> createSupplierCategoryFee(
            long hyperId,
            Long supplierId,
            int value,
            LocalDate from,
            LocalDate to,
            BillingServiceType serviceType,
            OrderType orderType) {
        return new Pair<>(new SupplierCategoryFee(
                hyperId,
                supplierId,
                value,
                new Period(createInstant(from), createInstant(to)),
                serviceType
        ), orderType);
    }

    private static Pair<SupplierCategoryFee, OrderType> createSupplierCategoryFee(
            long hyperId,
            int value,
            LocalDate from,
            LocalDate to,
            OrderType orderType) {
        return createSupplierCategoryFee(hyperId, null, value, from, to, BillingServiceType.FEE, orderType);
    }

    private static List<Pair<SupplierCategoryFee, OrderType>> createTestList(long hyperId) {
        return List.of(
                createSupplierCategoryFee(
                        hyperId,
                        50,
                        LocalDate.of(2021, 1, 1),
                        LocalDate.of(2021, 2, 1),
                        OrderType.FULFILLMENT
                ),
                createSupplierCategoryFee(
                        hyperId,
                        30,
                        LocalDate.of(2021, 2, 1),
                        LocalDate.MAX,
                        OrderType.FULFILLMENT
                ),
                createSupplierCategoryFee(
                        hyperId,
                        100,
                        LocalDate.of(2021, 2, 1),
                        LocalDate.MAX,
                        OrderType.DROP_SHIP
                ),
                createSupplierCategoryFee(
                        hyperId,
                        12347L,
                        0,
                        LocalDate.of(2021, 2, 1),
                        LocalDate.MAX,
                        BillingServiceType.FEE,
                        OrderType.FULFILLMENT
                ),
                createSupplierCategoryFee(
                        hyperId,
                        10,
                        LocalDate.of(2021, 2, 1),
                        LocalDate.MAX,
                        OrderType.DROP_SHIP_BY_SELLER
                )
        );
    }

    private static List<FulfillmentTariff> createDeliveryTariffs(BillingServiceType serviceType) {
        return List.of(
                createFulfillmentTariff(
                        LocalDate.of(2018, 2, 1),
                        LocalDate.MAX,
                        serviceType,
                        4000000L,
                        1000000L,
                        800,
                        OrderType.FULFILLMENT
                ),
                createFulfillmentTariff(
                        LocalDate.of(2018, 2, 1),
                        LocalDate.MAX,
                        serviceType,
                        null,
                        null,
                        900,
                        OrderType.CROSSDOCK
                ),
                createFulfillmentTariff(
                        LocalDate.of(2018, 2, 1),
                        LocalDate.MAX,
                        serviceType,
                        null,
                        null,
                        1000,
                        OrderType.DROP_SHIP
                )
        );
    }

    private static List<FulfillmentTariff> createCrossregionalDeliveryTariffs() {
        return List.of(
                createFulfillmentTariff(
                        LocalDate.of(2018, 2, 1),
                        LocalDate.MAX,
                        BillingServiceType.CROSSREGIONAL_DELIVERY,
                        4000000L,
                        1000000L,
                        800,
                        OrderType.FULFILLMENT,
                        3L,
                        6L

                ),
                createFulfillmentTariff(
                        LocalDate.of(2018, 2, 1),
                        LocalDate.MAX,
                        BillingServiceType.CROSSREGIONAL_DELIVERY,
                        null,
                        null,
                        900,
                        OrderType.CROSSDOCK,
                        3L,
                        6L
                ),
                createFulfillmentTariff(
                        LocalDate.of(2018, 2, 1),
                        LocalDate.MAX,
                        BillingServiceType.CROSSREGIONAL_DELIVERY,
                        null,
                        null,
                        1000,
                        OrderType.DROP_SHIP,
                        3L,
                        6L
                )
        );
    }

    private void mock(
            List<Pair<SupplierCategoryFee, OrderType>> feeList,
            List<FulfillmentTariff> tariffList,
            List<Pair<SupplierCategoryFee, OrderType>> minFeeList) {
        Mockito.doAnswer(getFees(feeList)).when(dbSupplierCategoryFeeDao).getFee(Mockito.any(), Mockito.any());
        Mockito.when(fulfillmentTariffDao.getOrderedTariffs(Mockito.any())).thenReturn(tariffList);
        Mockito.doAnswer(getFees(minFeeList)).when(dbSupplierCategoryFeeDao).getMinFee(Mockito.any(), Mockito.any());
    }

    private Answer<List<SupplierCategoryFee>> getFees(List<Pair<SupplierCategoryFee, OrderType>> feeList) {
        return invocation -> {
            Instant date = createInstant(invocation.getArgument(0));
            OrderType orderType = invocation.getArgument(1);
            return feeList.stream().filter(fee -> {
                Period period = fee.getFirst().getPeriod();
                Instant from = period.getFrom();
                Instant to = period.getTo();
                return fee.getSecond() == orderType && from.compareTo(date) <= 0 && date.compareTo(to) < 0;
            }).map(fee -> fee.getFirst()).collect(Collectors.toList());
        };
    }

    private void mock(List<Pair<SupplierCategoryFee, OrderType>> feeList, List<FulfillmentTariff> tariffList) {
        mock(feeList, tariffList, List.of());
    }

    @BeforeEach
    void setup() {
        this.exceptionCollector = new ExceptionCollector();
    }

    @AfterEach
    void endTest() {
        assertDoesNotThrow(
                () -> this.exceptionCollector.close()
        );
    }

    @Test
    @DbUnitDataSet(
            before = "FulfillmentOrderBillingServiceDbUnitTest.before.csv",
            after = "FulfillmentOrderBillingServiceDbUnitTest.after.csv"
    )
    void test() {
        mock(DEFAULT_FEE, DEFAULT_TARIFF);
        fulfillmentOrderBillingService.bill(BillingServiceType.FF_PROCESSING, BILLING_DATE, new HashSet<>(),
                exceptionCollector);
        fulfillmentOrderBillingService.bill(BillingServiceType.FEE, BILLING_DATE, new HashSet<>(), exceptionCollector);
    }

    @Test
    @DbUnitDataSet(
            before = "FulfillmentOrderBillingServiceDbUnitTest.testSkipIgnoredOrderItem.before.csv",
            after = "FulfillmentOrderBillingServiceDbUnitTest.testSkipIgnoredOrderItem.after.csv"
    )
    void testSkipIgnoredOrderItem() {
        mock(DEFAULT_FEE, DEFAULT_TARIFF);
        fulfillmentOrderBillingService.process(BILLING_DATE);
    }

    @Test
    @DbUnitDataSet(
            before = "FulfillmentOrderBillingServiceDbUnitTest.testMinimumTariff.before.csv",
            after = "FulfillmentOrderBillingServiceDbUnitTest.testMinimumTariff.after.csv"
    )
    void testMinimumTariff() {
        mock(DEFAULT_FEE, DEFAULT_TARIFF, List.of(
                createSupplierCategoryFee(
                        90401,
                        100000000,
                        LocalDate.of(2018, 2, 1),
                        LocalDate.MAX,
                        OrderType.FULFILLMENT
                ),
                createSupplierCategoryFee(
                        90404,
                        200000000,
                        LocalDate.of(2018, 2, 1),
                        LocalDate.MAX,
                        OrderType.DROP_SHIP
                )
        ));
        fulfillmentOrderBillingService.bill(BillingServiceType.FEE, BILLING_DATE, new HashSet<>(), exceptionCollector);
    }

    @DisplayName("Биллинг ff_processing для crossdock")
    @Test
    @DbUnitDataSet(
            before = "FulfillmentOrderBillingServiceDbUnitTest.crossdock_isNotFree.before.csv",
            after = "FulfillmentOrderBillingServiceDbUnitTest.crossdock_isNotFree.after.csv"
    )
    void testBillingFfCrossdockIsNotFree() {
        mock(List.of(), List.of(
                createFulfillmentTariff(
                        LocalDate.of(2018, 2, 1),
                        LocalDate.MAX,
                        BillingServiceType.FF_PROCESSING,
                        null,
                        null,
                        800,
                        OrderType.FULFILLMENT
                ),
                createFulfillmentTariff(
                        LocalDate.of(2018, 2, 1),
                        LocalDate.MAX,
                        BillingServiceType.FF_PROCESSING,
                        null,
                        null,
                        900,
                        OrderType.CROSSDOCK
                )
        ));
        fulfillmentOrderBillingService.bill(BillingServiceType.FF_PROCESSING, BILLING_DATE, new HashSet<>(),
                exceptionCollector);
    }

    @DisplayName("Биллинг ff_processing для космических весогабаритов")
    @Test
    @DbUnitDataSet(
            before = "FulfillmentOrderBillingServiceDbUnitTest.huge_lwhw.before.csv",
            after = "FulfillmentOrderBillingServiceDbUnitTest.huge_lwhw.after.csv"
    )
    void testBillingFfHugeLwhw() {
        mock(List.of(), List.of(
                createFulfillmentTariff(
                        LocalDate.of(2018, 2, 1),
                        LocalDate.MAX,
                        BillingServiceType.FF_PROCESSING,
                        4000000L,
                        1000000L,
                        400,
                        OrderType.FULFILLMENT
                ),
                createFulfillmentTariff(
                        LocalDate.of(2018, 2, 1),
                        LocalDate.MAX,
                        BillingServiceType.FF_PROCESSING,
                        null,
                        null,
                        800,
                        OrderType.FULFILLMENT
                )
        ));
        fulfillmentOrderBillingService.bill(BillingServiceType.FF_PROCESSING, BILLING_DATE, new HashSet<>(),
                exceptionCollector);
    }

    @Test
    @DbUnitDataSet(
            before = "FulfillmentOrderBillingServiceDbUnitTest.testFeeCancellationInCurrentMonth.before.csv",
            after = "FulfillmentOrderBillingServiceDbUnitTest.testFeeCancellationInCurrentMonth.after.csv"
    )
    void testFeeCancellationInCurrentMonth() {
        fulfillmentOrderBillingService.bill(BillingServiceType.FEE_CANCELLATION, LocalDate.of(2018, 2, 5),
                new HashSet<>(), exceptionCollector);
    }

    @Test
    @DbUnitDataSet(
            before = "FulfillmentOrderBillingServiceDbUnitTest.testloyaltyParticipationFeeCancellationInCurrentMonth" +
                    ".before.csv",
            after = "FulfillmentOrderBillingServiceDbUnitTest.testloyaltyParticipationFeeCancellationInCurrentMonth" +
                    ".after.csv"
    )
    void testLoyaltyParticipationFeeCancellationInCurrentMonth() {
        fulfillmentOrderBillingService.bill(BillingServiceType.LOYALTY_PARTICIPATION_FEE_CANCELLATION,
                LocalDate.of(2018, 2, 5), new HashSet<>(), exceptionCollector);
    }

    @Test
    @DbUnitDataSet(
            before = "FulfillmentOrderBillingServiceDbUnitTest.testFeeCancellationInNextMonth.before.csv",
            after = "FulfillmentOrderBillingServiceDbUnitTest.testFeeCancellationInNextMonth.after.csv"
    )
    void testFeeCancellationInNextMonth() {
        fulfillmentOrderBillingService.bill(BillingServiceType.FEE_CANCELLATION, LocalDate.of(2018, 3, 5),
                new HashSet<>(), exceptionCollector);
    }

    @Test
    @DbUnitDataSet(
            before = "FulfillmentOrderBillingServiceDbUnitTest.testLoyaltyParticipationFeeCancellationInNextMonth" +
                    ".before.csv",
            after = "FulfillmentOrderBillingServiceDbUnitTest.testLoyaltyParticipationFeeCancellationInNextMonth" +
                    ".after.csv"
    )
    void testLoyaltyParticipationFeeCancellationInNextMonth() {
        fulfillmentOrderBillingService.bill(BillingServiceType.LOYALTY_PARTICIPATION_FEE_CANCELLATION,
                LocalDate.of(2018, 3, 5), new HashSet<>(), exceptionCollector);
    }

    @Test
    @DisplayName("Биллинг отмены для квантовых товаров")
    @DbUnitDataSet(
            before = "FulfillmentOrderBillingServiceDbUnitTest.testBillingCancellation_quantum.before.csv",
            after = "FulfillmentOrderBillingServiceDbUnitTest.testBillingCancellation_quantum.after.csv"
    )
    void testBillingCancellationOfQuantumItems() {
        var billingDate = LocalDate.of(2022, 5, 7);

        List.of(
                BillingServiceType.FEE_CANCELLATION,
                BillingServiceType.DELIVERY_TO_CUSTOMER_CANCELLATION,
                BillingServiceType.CROSSREGIONAL_DELIVERY_CANCELLATION
        ).forEach(serviceType ->
                fulfillmentOrderBillingService.bill(serviceType, billingDate, emptySet(), exceptionCollector)
        );
    }

    void setMock() {
        doAnswer(invocation -> {
            TariffFindQuery findQuery = invocation.getArgument(0);
            Assertions.assertTrue(findQuery.getIsActive(), "Only active tariffs should be available");

            LocalDate targetDate = Objects.requireNonNull(findQuery.getTargetDate());
            return new TariffsIterator((pageNumber, batchSize) -> {
                if (pageNumber != 0) {
                    return List.of();
                }

                return getCancelledOrderTariffs()
                        .stream()
                        .filter(tariff -> {
                            LocalDate to = tariff.getDateTo();
                            LocalDate from = tariff.getDateFrom();
                            return targetDate.compareTo(from) >= 0
                                    && (to == null || targetDate.compareTo(to) < 0);
                        })
                        .collect(Collectors.toList());
            });
        }).when(tariffsService).findTariffs(any(TariffFindQuery.class));
    }

    private List<TariffDTO> getCancelledOrderTariffs() {
        return List.of(
                createTariff(1L, BILLING_DATE, null, null,
                        List.of(createMeta(new BigDecimal(75)))
                )
        );
    }

    private TariffDTO createTariff(long id, LocalDate from, LocalDate to, Partner partner, List<Object> meta) {
        TariffDTO tariff = new TariffDTO();
        tariff.setId(id);
        tariff.setMeta(meta);
        tariff.setDateFrom(from);
        tariff.setDateTo(to);
        tariff.setPartner(partner);
        return tariff;
    }

    private CommonJsonSchema createMeta(BigDecimal amount) {
        return new CancelledOrderFeeJsonSchema()
                .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                .amount(amount)
                .currency("RUB")
                .billingUnit(BillingUnitEnum.ITEM);
    }

    @Test
    @DbUnitDataSet(
            before = "FulfillmentOrderBillingServiceDbUnitTest.testCancelledOrderFee.before.csv",
            after = "FulfillmentOrderBillingServiceDbUnitTest.testCancelledOrderFee.after.csv"
    )
    void testCancelledOrderFee() {
        setMock();
        fulfillmentOrderBillingService.process(JULY_01_2021);
        fulfillmentOrderBillingService.process(JUNE_10_2020);
    }

    @Test
    @DbUnitDataSet(
            before = "FulfillmentOrderBillingServiceDbUnitTest.testDeliveryToCustomer.before.csv",
            after = "FulfillmentOrderBillingServiceDbUnitTest.testDeliveryToCustomer.after.csv"
    )
    void testDeliveryToCustomer() {
        mock(List.of(), createDeliveryTariffs(BillingServiceType.DELIVERY_TO_CUSTOMER));
        fulfillmentOrderBillingService.bill(BillingServiceType.DELIVERY_TO_CUSTOMER, BILLING_DATE, new HashSet<>(),
                exceptionCollector);
    }

    @Test
    @DbUnitDataSet(
            before = "FulfillmentOrderBillingServiceDbUnitTest.testCrossregionalDelivery.before.csv",
            after = "FulfillmentOrderBillingServiceDbUnitTest.testCrossregionalDelivery.after.csv"
    )
    void testCrossregionalDelivery() {
        mock(List.of(), createCrossregionalDeliveryTariffs());
        fulfillmentOrderBillingService.bill(BillingServiceType.CROSSREGIONAL_DELIVERY, BILLING_DATE, new HashSet<>(),
                exceptionCollector);
    }

    @Test
    @DbUnitDataSet(
            before = "FulfillmentOrderBillingServiceDbUnitTest.testDeliveryToCustomerReturn.before.csv",
            after = "FulfillmentOrderBillingServiceDbUnitTest.testDeliveryToCustomerReturn.after.csv"
    )
    void testDeliveryToCustomerReturn() {
        mock(List.of(), createDeliveryTariffs(BillingServiceType.DELIVERY_TO_CUSTOMER_RETURN));
        fulfillmentOrderBillingService.bill(BillingServiceType.DELIVERY_TO_CUSTOMER_RETURN, BILLING_DATE,
                new HashSet<>(), exceptionCollector);
    }

    @Test
    @DbUnitDataSet(
            before = "FulfillmentOrderBillingServiceDbUnitTest.testDeliveryToCustomerCancellation.before.csv",
            after = "FulfillmentOrderBillingServiceDbUnitTest.testDeliveryToCustomerCancellation.after.csv"
    )
    void testDeliveryToCustomerCancellation() {
        fulfillmentOrderBillingService.bill(BillingServiceType.DELIVERY_TO_CUSTOMER_CANCELLATION,
                LocalDate.of(2018, 2, 2), new HashSet<>(), exceptionCollector);
    }

    @Test
    @DbUnitDataSet(
            before = "FulfillmentOrderBillingServiceDbUnitTest.testCrossregionalDeliveryCancellation.before.csv",
            after = "FulfillmentOrderBillingServiceDbUnitTest.testCrossregionalDeliveryCancellation.after.csv"
    )
    void testCrossregionalDeliveryCancellation() {
        fulfillmentOrderBillingService.bill(BillingServiceType.CROSSREGIONAL_DELIVERY_CANCELLATION,
                LocalDate.of(2018, 2, 2), new HashSet<>(), exceptionCollector);
    }

    @Test
    @DbUnitDataSet(
            before = "FulfillmentOrderBillingServiceDbUnitTest.testDSBSOrders.before.csv",
            after = "FulfillmentOrderBillingServiceDbUnitTest.testDSBSOrders.after.csv"
    )
    void testDSBSOrders() {
        mock(List.of(
                createSupplierCategoryFee(
                        90401,
                        50,
                        LocalDate.of(2018, 2, 1),
                        LocalDate.MAX,
                        OrderType.FULFILLMENT
                ),
                createSupplierCategoryFee(
                        90401,
                        30,
                        LocalDate.of(2018, 2, 1),
                        LocalDate.MAX,
                        OrderType.DROP_SHIP
                ),
                createSupplierCategoryFee(
                        90401,
                        30,
                        LocalDate.of(2018, 2, 1),
                        LocalDate.MAX,
                        OrderType.DROP_SHIP_BY_SELLER
                )
        ), createDeliveryTariffs(BillingServiceType.FF_PROCESSING));
        fulfillmentOrderBillingService.bill(BillingServiceType.FEE, BILLING_DATE_2020_10_02, new HashSet<>(),
                exceptionCollector);
    }

    @Test
    @DbUnitDataSet(
            before = "FulfillmentOrderBillingServiceDbUnitTest.testBillLoyaltyParticipationFee.before.csv",
            after = "FulfillmentOrderBillingServiceDbUnitTest.testBillLoyaltyParticipationFee.after.csv"
    )
    void testBillLoyaltyParticipationFee() {
        mock(BILL_LOYALTY_LIST, DEFAULT_TARIFF, BILL_LOYALTY_MIN_FEE_LIST);
        fulfillmentOrderBillingService.bill(BillingServiceType.LOYALTY_PARTICIPATION_FEE, BILLING_DATE,
                new HashSet<>(), exceptionCollector);
    }

    @Test
    @DbUnitDataSet(
            before = "FulfillmentOrderBillingServiceDbUnitTest" +
                    ".testBillLoyaltyParticipationFeeUsingPriceAfterFebruary16.before.csv",
            after = "FulfillmentOrderBillingServiceDbUnitTest" +
                    ".testBillLoyaltyParticipationFeeUsingPriceAfterFebruary16.after.csv"
    )
    void testBillLoyaltyParticipationFeeUsingPriceAfterFebruary16() {
        mock(BILL_LOYALTY_LIST, DEFAULT_TARIFF, BILL_LOYALTY_MIN_FEE_LIST);
        fulfillmentOrderBillingService.bill(BillingServiceType.LOYALTY_PARTICIPATION_FEE, FEBRUARY_16_2021,
                new HashSet<>(), exceptionCollector);
    }

    @Test
    @DbUnitDataSet(
            before = "FulfillmentOrderBillingServiceDbUnitTest" +
                    ".testBillLoyaltyParticipationFeeUsingBillingPriceAfterFebruary16ButCreatedBeforeFebruary16" +
                    ".before.csv",
            after = "FulfillmentOrderBillingServiceDbUnitTest" +
                    ".testBillLoyaltyParticipationFeeUsingBillingPriceAfterFebruary16ButCreatedBeforeFebruary16.after" +
                    ".csv"
    )
    void testBillLoyaltyParticipationFeeDropshipOrdersUsingBillingPriceAfterFebruary16ButCreatedBeforeFebruary16() {
        mock(BILL_LOYALTY_LIST, DEFAULT_TARIFF, BILL_LOYALTY_MIN_FEE_LIST);
        fulfillmentOrderBillingService.bill(BillingServiceType.LOYALTY_PARTICIPATION_FEE, FEBRUARY_16_2021,
                new HashSet<>(), exceptionCollector);
    }

    @Test
    @DbUnitDataSet(
            before = "FulfillmentOrderBillingServiceDbUnitTest" +
                    ".testBillFreeLoyaltyParticipationFeeFulfillmentOrdersCreatedBeforeFebruary16.before.csv",
            after = "FulfillmentOrderBillingServiceDbUnitTest" +
                    ".testBillFreeLoyaltyParticipationFeeFulfillmentOrdersCreatedBeforeFebruary16.after.csv"
    )
    void testBillFreeLoyaltyParticipationFeeFulfillmentOrdersCreatedBeforeFebruary16() {
        fulfillmentOrderBillingService.bill(BillingServiceType.LOYALTY_PARTICIPATION_FEE, FEBRUARY_16_2021,
                new HashSet<>(), exceptionCollector);
    }

    @Test
    @DbUnitDataSet(
            before = "FulfillmentOrderBillingServiceDbUnitTest.testBillLoyaltyParticipationFeeByPromoInfo.before.csv",
            after = "FulfillmentOrderBillingServiceDbUnitTest.testBillLoyaltyParticipationFeeByPromoInfo.after.csv"
    )
    void testBillLoyaltyParticipationFeeByPromoInfo() {
        mock(BILL_LOYALTY_LIST, DEFAULT_TARIFF, BILL_LOYALTY_MIN_FEE_LIST);

        fulfillmentOrderBillingService.bill(BillingServiceType.LOYALTY_PARTICIPATION_FEE, OCTOBER_01_2021,
                new HashSet<>(), exceptionCollector);
    }

    @Test
    @DbUnitDataSet(
            before = "FulfillmentOrderBillingServiceDbUnitTest.testBillFreeLoyaltyParticipationFee.before.csv",
            after = "FulfillmentOrderBillingServiceDbUnitTest.testBillFreeLoyaltyParticipationFee.after.csv"
    )
    void testBillFreeLoyaltyParticipationFee() {
        fulfillmentOrderBillingService.bill(BillingServiceType.LOYALTY_PARTICIPATION_FEE, BILLING_DATE,
                new HashSet<>(), exceptionCollector);
    }

    @Test
    @DbUnitDataSet(
            before = "FulfillmentOrderBillingServiceDbUnitTest.testBillFreeLoyaltyParticipationFeeCancellation.before" +
                    ".csv",
            after = "FulfillmentOrderBillingServiceDbUnitTest.testBillFreeLoyaltyParticipationFeeCancellation.after.csv"
    )
    void testBillFreeLoyaltyParticipationFeeCancellation() {
        fulfillmentOrderBillingService.bill(BillingServiceType.LOYALTY_PARTICIPATION_FEE_CANCELLATION, BILLING_DATE,
                new HashSet<>(), exceptionCollector);
    }

    @Test
    @DbUnitDataSet(
            before = "FulfillmentOrderBillingServiceDbUnitTest.promo.before.csv",
            after = "FulfillmentOrderBillingServiceDbUnitTest.promo.after.csv"
    )
    void testPromo() {
        mock(DEFAULT_FEE, DEFAULT_TARIFF);
        fulfillmentOrderBillingService.bill(BillingServiceType.FF_PROCESSING, BILLING_DATE, new HashSet<>(),
                exceptionCollector);
        fulfillmentOrderBillingService.bill(BillingServiceType.FEE, BILLING_DATE, new HashSet<>(), exceptionCollector);
    }

    @Test
    @DbUnitDataSet(
            before = "FulfillmentOrderBillingServiceDbUnitTest.billForPartners.before.csv",
            after = "FulfillmentOrderBillingServiceDbUnitTest.billForPartners.after.csv"
    )
    void testBillForPartners() {
        mock(DEFAULT_FEE, DEFAULT_TARIFF);
        fulfillmentOrderBillingService.billForPartners(BillingServiceType.FF_PROCESSING, BILLING_DATE, Set.of(12346L,
                2L), exceptionCollector);
        fulfillmentOrderBillingService.billForPartners(BillingServiceType.FEE, BILLING_DATE, Set.of(12346L, 2L),
                exceptionCollector);
    }

    @DbUnitDataSet(
            before = "FulfillmentOrderBillingServiceDbUnitTest.filterDeliveryForExpress.before.csv",
            after = "FulfillmentOrderBillingServiceDbUnitTest.filterDeliveryForExpress.after.csv"
    )
    @Test
    void testForFilterDeliveryForExpress() {
        mock(
                List.of(
                        createSupplierCategoryFee(
                                90401,
                                50,
                                LocalDate.of(2018, 2, 1),
                                LocalDate.MAX,
                                OrderType.DROP_SHIP
                        )
                ),
                List.of(
                        createFulfillmentTariff(
                                LocalDate.of(2018, 2, 1),
                                LocalDate.MAX,
                                BillingServiceType.DELIVERY_TO_CUSTOMER,
                                null,
                                null,
                                1000,
                                OrderType.DROP_SHIP
                        ),
                        createFulfillmentTariff(
                                LocalDate.of(2018, 2, 1),
                                LocalDate.MAX,
                                BillingServiceType.DELIVERY_TO_CUSTOMER_RETURN,
                                null,
                                null,
                                1000,
                                OrderType.DROP_SHIP
                        ),
                        createFulfillmentTariff(
                                LocalDate.of(2018, 2, 1),
                                LocalDate.MAX,
                                BillingServiceType.CROSSREGIONAL_DELIVERY,
                                null,
                                null,
                                1000,
                                OrderType.DROP_SHIP,
                                3L,
                                6L
                        )
                )
        );
        fulfillmentOrderBillingService.process(LocalDate.of(2021, Month.JUNE, 28));
        fulfillmentOrderBillingService.process(LocalDate.of(2021, Month.JUNE, 29));
    }

    @Test
    @DbUnitDataSet(
            before = "FulfillmentOrderBillingServiceDbUnitTest.testDirectShopInShop.before.csv",
            after = "FulfillmentOrderBillingServiceDbUnitTest.testDirectShopInShop.after.csv"
    )
    void testDirectShopInShop() {
        mock(List.of(
                createSupplierCategoryFee(
                        90401,
                        30,
                        LocalDate.of(2018, 2, 1),
                        LocalDate.MAX,
                        OrderType.FULFILLMENT
                ),
                createSupplierCategoryFee(
                        90402,
                        30,
                        LocalDate.of(2018, 2, 1),
                        LocalDate.MAX,
                        OrderType.FULFILLMENT
                ),
                createSupplierCategoryFee(
                        90403,
                        30,
                        LocalDate.of(2018, 2, 1),
                        LocalDate.MAX,
                        OrderType.FULFILLMENT
                ),
                createSupplierCategoryFee(
                        90404,
                        100,
                        LocalDate.of(2018, 2, 1),
                        LocalDate.MAX,
                        OrderType.DROP_SHIP
                )
        ), DEFAULT_TARIFF);
        fulfillmentOrderBillingService.bill(BillingServiceType.FF_PROCESSING, BILLING_DATE, new HashSet<>(),
                exceptionCollector);
        fulfillmentOrderBillingService.bill(BillingServiceType.FEE, BILLING_DATE, new HashSet<>(), exceptionCollector);
    }

    @DisplayName("Проверка расчета обиленного значения по батчам")
    @ParameterizedTest(name = "[{index}] {4}")
    @MethodSource("testGetAmountOnBatchedItemData")
    void testGetAmountOnBatchedItem(
            OrderItemForBilling item,
            BillingServiceType serviceType,
            LocalDate billingDate,
            Long expectedAmount,
            String description
    ) {
        PriceAndTariffValueService priceAndTariffValueService = Mockito.mock(PriceAndTariffValueService.class);
        doAnswer(invocation -> true).when(priceAndTariffValueService).isBillingByBatchIsEnabled();
        doCallRealMethod().when(priceAndTariffValueService).getBillingPrice(any(), any(), any());
        doCallRealMethod().when(priceAndTariffValueService).isBillingByBatch(any(), any(), any());

        BigDecimal billingPrice = priceAndTariffValueService.getBillingPrice(
                item,
                serviceType,
                billingDate
        );

        PriceAndTariffValue priceAndTariffValue = new PriceAndTariffValue(
                billingPrice,
                billingPrice,
                Currency.RUR,
                new TariffValue(100, ValueType.RELATIVE, BillingUnit.ORDER)
        );

        Long actualAmount = fulfillmentOrderBillingService.getAmount(
                priceAndTariffValueService,
                serviceType,
                item,
                priceAndTariffValue,
                billingDate,
                false
        );
        assertEquals(expectedAmount, actualAmount);
    }

    private static Stream<Arguments> testGetAmountOnBatchedItemData() {
        return Stream.of(
                Arguments.of(
                        createItemMocked(12, 1, 10),
                        BillingServiceType.FEE,
                        LocalDate.of(2022, Month.MARCH, 15),
                        120L,
                        "120 копеек комиссия, т.к. биллим 12 батчей, каждый из которых по 10 копеек"
                ),
                Arguments.of(
                        createItemMocked(12, 1, 10),
                        BillingServiceType.FF_PROCESSING,
                        LocalDate.of(2022, Month.MARCH, 15),
                        120L,
                        "тоже самое, что и тест выше, только для другой услуги (валидной)"
                ),
                Arguments.of(
                        createItemMocked(12, 12, 10),
                        BillingServiceType.FEE,
                        LocalDate.of(2022, Month.MARCH, 15),
                        120L,
                        "120 копеек комиссия, т.к. биллим 1 батч товаров по цене 120"
                ),
                Arguments.of(
                        createItemMocked(6, 12, 10),
                        BillingServiceType.FEE,
                        LocalDate.of(2022, Month.MARCH, 15),
                        60L,
                        "60 копеек комиссия (потому что количество товаров меньше батч сайза), " +
                                "биллим 1 батч товаров по цене 10"
                ),
                Arguments.of(
                        createItemMocked(20, 12, 10),
                        BillingServiceType.FEE,
                        LocalDate.of(2022, Month.MARCH, 15),
                        240L,
                        "240 копеек комиссия, биллим 2 батча (20 / 12 = 2 в большую сторону) товаров по цене 120"
                ),
                Arguments.of(
                        createItemMocked(11, 12, 10),
                        BillingServiceType.FEE,
                        LocalDate.of(2022, Month.MARCH, 15),
                        110L,
                        "110 копеек комиссия (потому что количество товаров меньше батч сайза), " +
                                "биллим 1 батч товаров по цене 10"
                ),
                Arguments.of(
                        createItemMocked(12, 12, 10),
                        BillingServiceType.DELIVERY_TO_CUSTOMER_RETURN,
                        LocalDate.of(2022, Month.MARCH, 15),
                        120L,
                        "невалидная услуга"
                ),
                Arguments.of(
                        createItemMocked(12, 12, 10),
                        BillingServiceType.FEE,
                        LocalDate.of(2022, Month.MARCH, 14),
                        120L,
                        "невалидная дата"
                ),
                Arguments.of(
                        createItemMocked(12, null, 10),
                        BillingServiceType.FEE,
                        LocalDate.of(2022, Month.MARCH, 15),
                        120L,
                        "нет батч сайза"
                )
        );
    }

    static OrderItemForBilling createItemMocked(int count, Integer batchSize, int price) {
        OrderItemForBilling item = Mockito.mock(OrderItemForBilling.class);
        doReturn(count).when(item).getCount();
        doReturn(batchSize).when(item).getBatchSize();
        doReturn(BigDecimal.valueOf(price)).when(item).getBillingPrice();
        return item;
    }
}
