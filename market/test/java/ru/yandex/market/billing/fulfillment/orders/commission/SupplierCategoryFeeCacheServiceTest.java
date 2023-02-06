package ru.yandex.market.billing.fulfillment.orders.commission;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.categories.db.DbSupplierCategoryFeeService;
import ru.yandex.market.billing.fulfillment.OrderType;
import ru.yandex.market.billing.fulfillment.tariffs.TariffsIterator;
import ru.yandex.market.billing.fulfillment.tariffs.TariffsService;
import ru.yandex.market.billing.model.billing.BillingServiceType;
import ru.yandex.market.billing.service.environment.EnvironmentService;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.date.Period;
import ru.yandex.market.core.util.DateTimes;
import ru.yandex.market.mbi.tariffs.client.model.BillingUnitEnum;
import ru.yandex.market.mbi.tariffs.client.model.CommonJsonSchema;
import ru.yandex.market.mbi.tariffs.client.model.ModelType;
import ru.yandex.market.mbi.tariffs.client.model.OrderStatusEnum;
import ru.yandex.market.mbi.tariffs.client.model.Partner;
import ru.yandex.market.mbi.tariffs.client.model.PartnerType;
import ru.yandex.market.mbi.tariffs.client.model.ServiceTypeEnum;
import ru.yandex.market.mbi.tariffs.client.model.SupplierCategoryFeeTariffJsonSchema;
import ru.yandex.market.mbi.tariffs.client.model.TariffDTO;
import ru.yandex.market.mbi.tariffs.client.model.TariffFindQuery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Тесты для {@link SupplierCategoryFeeCacheService}
 */
class SupplierCategoryFeeCacheServiceTest extends FunctionalTest {

    private static final Period DUMMY_PERIOD = new Period(Instant.now().minusSeconds(1000), Instant.now());

    private static final ImmutableList<SupplierCategoryFee> EXPECTED_FEE = ImmutableList.of(
            new SupplierCategoryFee(1L, 2L, 3, DUMMY_PERIOD, BillingServiceType.FEE),
            new SupplierCategoryFee(10L, null, 30, DUMMY_PERIOD, BillingServiceType.FEE),
            new SupplierCategoryFee(100L, 200L, 300, DUMMY_PERIOD, BillingServiceType.FEE)
    );

    private static final ImmutableList<SupplierCategoryFee> EXPECTED_CASH_ONLY_FEE = ImmutableList.of(
            new SupplierCategoryFee(1L, 2L, 3, DUMMY_PERIOD, BillingServiceType.FEE),
            new SupplierCategoryFee(10L, null, 30, DUMMY_PERIOD, BillingServiceType.FEE),
            new SupplierCategoryFee(100L, 200L, 300, DUMMY_PERIOD, BillingServiceType.FEE)
    );

    private static final List<SupplierCategoryFee> EXPECTED_MIN_FEE = List.of(
            new SupplierCategoryFee(1, null, 10, DUMMY_PERIOD, BillingServiceType.FEE),
            new SupplierCategoryFee(2, 1L, 100, DUMMY_PERIOD, BillingServiceType.FEE)
    );

    private static final List<SupplierCategoryFee> EXPECTED_FBS_FEE = List.of(
            new SupplierCategoryFee(100L, null, 200, DUMMY_PERIOD, BillingServiceType.FEE, true),
            new SupplierCategoryFee(100L, null, 300, DUMMY_PERIOD, BillingServiceType.FEE)
    );

    private static SupplierCategoryFeeCacheService service;
    private static long tariffIdCounter = 0;

    @Autowired
    @Qualifier("testDbSupplierCategoryFeeDao")
    private DbSupplierCategoryFeeService dbSupplierCategoryFeeDao;

    @Autowired
    private EnvironmentService environmentService;

    @BeforeAll
    static void init() {
        DbSupplierCategoryFeeService dao = mock(DbSupplierCategoryFeeService.class);
        doReturn(EXPECTED_FEE).when(dao).getFee(null, OrderType.FULFILLMENT);
        doReturn(EXPECTED_CASH_ONLY_FEE).when(dao).getCashOnly(null, OrderType.FULFILLMENT);
        doReturn(EXPECTED_MIN_FEE).when(dao).getMinFee(null, OrderType.FULFILLMENT);

        service = new SupplierCategoryFeeCacheService(dao, null, OrderType.FULFILLMENT);
    }

    private static TariffDTO createTariff(long partnerId, BigDecimal amount) {
        TariffDTO tariff = new TariffDTO();
        tariff.setId(tariffIdCounter++);
        tariff.setServiceType(ServiceTypeEnum.FEE);
        tariff.setModelType(ModelType.DELIVERY_BY_SELLER);
        tariff.setDateFrom(LocalDate.of(2021, 1, 15));
        tariff.setPartner(new Partner().id(partnerId).type(PartnerType.SHOP));
        tariff.setDateTo(null);
        tariff.setMeta(List.of(new SupplierCategoryFeeTariffJsonSchema()
                .categoryId(90401L)
                .orderStatus(OrderStatusEnum.DELIVERED)
                .billingUnit(BillingUnitEnum.ITEM)
                .type(CommonJsonSchema.TypeEnum.RELATIVE)
                .currency("RUB")
                .amount(amount)
        ));
        return tariff;
    }

    private Instant createInstant(LocalDate date) {
        if (date == LocalDate.MAX) {
            return Instant.MAX;
        }
        return DateTimes.toInstantAtDefaultTz(date.getYear(), date.getMonthValue(), date.getDayOfMonth());
    }

    private Pair<SupplierCategoryFee, OrderType> createSupplierCategoryFee(
            int value,
            LocalDate from) {
        return new Pair<>(new SupplierCategoryFee(
                90401,
                1L,
                value,
                new Period(createInstant(from), createInstant(LocalDate.of(2020, 3, 1))),
                BillingServiceType.FEE
        ), OrderType.FULFILLMENT);
    }

    @Test
    void testGetFee() {
        Optional<Integer> fee = service.getFee(1L, 2L, BillingServiceType.FEE);
        assertThat(fee).contains(3);

        fee = service.getFee(100L, 200L, BillingServiceType.FEE);
        assertThat(fee).contains(300);
    }

    @Test
    @DisplayName("Получение кэш-онли-фи из кешСервиса")
    void testGetCashOnlyFee() {
        Optional<Integer> cashOnlyFee = service.getCashOnlyFee(1L, 2L, BillingServiceType.FEE);
        assertThat(cashOnlyFee).contains(3);

        cashOnlyFee = service.getCashOnlyFee(10L, null, BillingServiceType.FEE);
        assertThat(cashOnlyFee).contains(30);

        cashOnlyFee = service.getCashOnlyFee(100L, 200L, BillingServiceType.FEE);
        assertThat(cashOnlyFee).contains(300);
    }

    @Test
    void testGetExpressFee() {
        DbSupplierCategoryFeeService dao = mock(DbSupplierCategoryFeeService.class);
        doReturn(EXPECTED_FBS_FEE).when(dao).getFee(null, OrderType.DROP_SHIP);
        var fbsFeeSupplierService = new SupplierCategoryFeeCacheService(dao, null, OrderType.DROP_SHIP);
        Optional<Integer> fee = fbsFeeSupplierService.getFee(100, null, BillingServiceType.FEE, true);
        assertThat(fee).contains(200);

    }

    @Test
    void testGetMinFee() {
        Optional<Integer> fee = service.getMinFee(2L, 1L, BillingServiceType.FEE);
        assertThat(fee).contains(100);
    }

    @Test
    void testGetFeeNullSupplier() {
        Optional<Integer> fee = service.getFee(10L, null, BillingServiceType.FEE);
        assertThat(fee).contains(30);
    }

    @Test
    void testGetMinFeeNullSupplier() {
        Optional<Integer> fee = service.getMinFee(1L, null, BillingServiceType.FEE);
        assertThat(fee).contains(10);
    }

    @Test
    void testGetFeeNothing() {
        Optional<Integer> fee = service.getFee(50L, 50L, BillingServiceType.FEE);
        assertThat(fee).isNotPresent();
    }

    @Test
    void testGetMinFeeNothing() {
        Optional<Integer> fee = service.getMinFee(50L, 50L, BillingServiceType.FEE);
        assertThat(fee).isNotPresent();
    }

    @Test
    void testGetFeeNullSupplierNothing() {
        Optional<Integer> fee = service.getFee(50L, null, BillingServiceType.FEE);
        assertThat(fee).isNotPresent();
    }

    @Test
    void testGetMinFeeNullSupplierNothing() {
        Optional<Integer> fee = service.getMinFee(50L, null, BillingServiceType.FEE);
        assertThat(fee).isNotPresent();
    }

    @Test
    @DbUnitDataSet(
            before = "SupplierCategoryFeeCacheServiceTest.testIntersectedPeriods.before.csv"
    )
    @DisplayName("Тесты на получение ошибки, если есть хотя бы 2 пересекающихся периода в разрезе FeeKey")
    void testThrowsIfHasIntersectedPeriods() {
        dbSupplierCategoryFeeDao = mock(DbSupplierCategoryFeeService.class);
        doAnswer(invocation -> {
            Instant date = createInstant(invocation.getArgument(0));
            OrderType orderType = invocation.getArgument(1);
            return Stream.of(
                    createSupplierCategoryFee(
                            0,
                            LocalDate.of(2020, 1, 1)
                    ),
                    createSupplierCategoryFee(
                            300,
                            LocalDate.of(2020, 2, 1)
                    )
            ).filter(fee -> {
                Period period = fee.getFirst().getPeriod();
                Instant from = period.getFrom();
                Instant to = period.getTo();
                return fee.getSecond() == orderType && from.compareTo(date) <= 0 && date.compareTo(to) < 0;
            }).map(Pair::getFirst).collect(Collectors.toList());
        }).when(dbSupplierCategoryFeeDao).getFee(any(), any());

        Instant leftFrom = DateTimes.toInstantAtDefaultTz(2020, 1, 1);
        Instant rightFrom = DateTimes.toInstantAtDefaultTz(2020, 2, 1);
        Instant end = DateTimes.toInstantAtDefaultTz(2020, 3, 1);
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new SupplierCategoryFeeCacheService(
                        dbSupplierCategoryFeeDao,
                        LocalDate.of(2020, Month.FEBRUARY, 20),
                        OrderType.FULFILLMENT
                ))
                .withMessage(String.format(
                        "FeeKey{hyperId=90401, supplierId=1, serviceType=FEE, isExpress=false} has 2 intersection " +
                                "periods : " +
                                "[%s, %s) and [%s, %s)",
                        leftFrom, end, rightFrom, end
                ));
    }

    @Test
    void testWithDiffPartners() {
        TariffsService tariffService = mock(TariffsService.class);
        doAnswer(invocation -> new TariffsIterator((pageNumber, batchSize) -> {
            TariffFindQuery findQuery = invocation.getArgument(0);
            assertThat(findQuery.getIsActive()).as("Only active tariffs should be available").isTrue();

            if (findQuery.getServiceTypes().contains(ServiceTypeEnum.MIN_FEE)) {
                return List.of(); // для min_fee ничего не проверяем
            }
            if (findQuery.getServiceTypes().contains(ServiceTypeEnum.CASH_ONLY_ORDER)) {
                return List.of();
            }

            return pageNumber != 0
                    ? List.of()
                    : List.of(
                    createTariff(123L, new BigDecimal("3")),
                    createTariff(456L, new BigDecimal("20"))
            );
        })).when(tariffService).findTariffs(any(TariffFindQuery.class));

        environmentService.setValue("mbi.billing.use.external.tariffs.fee", "true");
        SupplierCategoryFeeCacheService supplierCategoryFeeCacheService = new SupplierCategoryFeeCacheService(
                new DbSupplierCategoryFeeService(tariffService),
                LocalDate.of(2021, Month.FEBRUARY, 20),
                OrderType.DROP_SHIP_BY_SELLER
        );

        Optional<Integer> feeFor123 = supplierCategoryFeeCacheService.getFee(90401L, 123L, BillingServiceType.FEE);
        assertThat(feeFor123).contains(300);

        Optional<Integer> feeFor456 = supplierCategoryFeeCacheService.getFee(90401L, 456L, BillingServiceType.FEE);
        assertThat(feeFor456).contains(2000);
    }


}
