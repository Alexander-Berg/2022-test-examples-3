package ru.yandex.market.core.billing.commission;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.date.Period;
import ru.yandex.market.core.fulfillment.model.BillingServiceType;
import ru.yandex.market.core.fulfillment.tariff.TariffsIterator;
import ru.yandex.market.core.fulfillment.tariff.TariffsService;
import ru.yandex.market.core.order.model.MbiBlueOrderType;
import ru.yandex.market.core.util.DateTimes;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.mbi.tariffs.client.model.BillingUnitEnum;
import ru.yandex.market.mbi.tariffs.client.model.CommonJsonSchema;
import ru.yandex.market.mbi.tariffs.client.model.MinFeeJsonSchema;
import ru.yandex.market.mbi.tariffs.client.model.ModelType;
import ru.yandex.market.mbi.tariffs.client.model.Partner;
import ru.yandex.market.mbi.tariffs.client.model.PartnerType;
import ru.yandex.market.mbi.tariffs.client.model.ServiceTypeEnum;
import ru.yandex.market.mbi.tariffs.client.model.SupplierCategoryFeeTariffJsonSchema;
import ru.yandex.market.mbi.tariffs.client.model.TariffDTO;
import ru.yandex.market.mbi.tariffs.client.model.TariffFindQuery;

import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doAnswer;

class DbSupplierCategoryFeeDaoTest extends FunctionalTest {
    private static final Period EXPECTED_PERIOD = new Period(
            DateTimes.toInstantAtDefaultTz(2018, 1, 1),
            Instant.MAX
    );

    private static final ImmutableList<SupplierCategoryFee> EXPECTED_FEE = ImmutableList.of(
            new SupplierCategoryFee(1L, 2L, 3, EXPECTED_PERIOD, BillingServiceType.FEE),
            new SupplierCategoryFee(10L, null, 30, EXPECTED_PERIOD, BillingServiceType.FEE),
            new SupplierCategoryFee(100L, 200L, 300, EXPECTED_PERIOD, BillingServiceType.FEE)
    );
    private static final List<SupplierCategoryFee> EXPECTED_MIN_FEE = List.of(
            new SupplierCategoryFee(1, null, 10, EXPECTED_PERIOD, BillingServiceType.FEE),
            new SupplierCategoryFee(2, 2L, 100, EXPECTED_PERIOD, BillingServiceType.FEE)
    );
    private static final List<SupplierCategoryFee> EXPECTED_FEE_AND_LOYALTY_PARTICIPATION_FEE = List.of(
            new SupplierCategoryFee(1L, 2L, 3, EXPECTED_PERIOD, BillingServiceType.FEE),
            new SupplierCategoryFee(10L, null, 30, EXPECTED_PERIOD, BillingServiceType.FEE),
            new SupplierCategoryFee(100L, 200L, 300, EXPECTED_PERIOD, BillingServiceType.FEE),
            new SupplierCategoryFee(2, 2L, 100, EXPECTED_PERIOD, BillingServiceType.LOYALTY_PARTICIPATION_FEE)
    );
    private static final List<SupplierCategoryFee> EXPECTED_FEE_FBS = List.of(
            new SupplierCategoryFee(100L, null, 200, EXPECTED_PERIOD, BillingServiceType.FEE, true)
    );

    private static final LocalDate DATE = LocalDate.of(2018, 4, 19);

    private DbSupplierCategoryFeeDao dbSupplierCategoryFeeDao = Mockito.mock(DbSupplierCategoryFeeDao.class);

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private TariffsService tariffsService;

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private Instant createInstant(LocalDate date) {
        if (date == LocalDate.MAX)
            return Instant.MAX;
        return DateTimes.toInstantAtDefaultTz(date.getYear(), date.getMonthValue(), date.getDayOfMonth());
    }

    private Pair<SupplierCategoryFee, MbiBlueOrderType> createSupplierCategoryFee(
            long hyperId,
            Long supplierId,
            int value) {
        return new Pair<>(new SupplierCategoryFee(
                hyperId,
                supplierId,
                value,
                new Period(createInstant(
                        LocalDate.of(2018, 1, 1)),
                        createInstant(LocalDate.MAX)
                ),
                BillingServiceType.FEE
        ), MbiBlueOrderType.FULFILLMENT);
    }

    @Test
    @DbUnitDataSet(before = "DbSupplierCategoryFeeDaoTest.before.csv")
    void testGetFee() {
        Mockito.doAnswer(invocation -> {
            Instant date = createInstant(invocation.getArgument(0));
            MbiBlueOrderType orderType = invocation.getArgument(1);
            return List.of(
                    createSupplierCategoryFee(1,
                            2L,
                            3
                    ),
                    createSupplierCategoryFee(10,
                            null,
                            30
                    ),
                    createSupplierCategoryFee(100,
                            200L,
                            300
                    )
            ).stream().filter(fee -> {
                Period period = fee.getFirst().getPeriod();
                Instant from = period.getFrom();
                Instant to = period.getTo();
                return fee.getSecond() == orderType && from.compareTo(date) <= 0 && date.compareTo(to) < 0;
            }).map(fee -> fee.getFirst()).collect(Collectors.toList());
        }).when(dbSupplierCategoryFeeDao).getFee(Mockito.any(), Mockito.any());

        List<SupplierCategoryFee> actual = dbSupplierCategoryFeeDao.getFee(DATE, MbiBlueOrderType.FULFILLMENT);

        assertThat(actual, containsInAnyOrder(
                EXPECTED_FEE.toArray(new SupplierCategoryFee[EXPECTED_FEE.size()])
        ));
    }

    @Test
    @DbUnitDataSet(before = "DbSupplierCategoryFeeDaoTest.before.csv")
    void testGetMinFee() {
        Mockito.doAnswer(invocation -> {
            Instant date = createInstant(invocation.getArgument(0));
            MbiBlueOrderType orderType = invocation.getArgument(1);
            return List.of(
                    createSupplierCategoryFee(1,
                            null,
                            10
                    ),
                    createSupplierCategoryFee(2,
                            2L,
                            100
                    )
            ).stream().filter(fee -> {
                Period period = fee.getFirst().getPeriod();
                Instant from = period.getFrom();
                Instant to = period.getTo();
                return fee.getSecond() == orderType && from.compareTo(date) <= 0 && date.compareTo(to) < 0;
            }).map(fee -> fee.getFirst()).collect(Collectors.toList());
        }).when(dbSupplierCategoryFeeDao).getMinFee(Mockito.any(), Mockito.any());

        List<SupplierCategoryFee> actual = dbSupplierCategoryFeeDao.getMinFee(DATE, MbiBlueOrderType.FULFILLMENT);

        assertThat(actual, containsInAnyOrder(
                EXPECTED_MIN_FEE.toArray(SupplierCategoryFee[]::new)
        ));
    }

    @Test
    void testFeeAndLoyaltyParticipationFeeExternalTariffs() {
        environmentService.setValue("mbi.billing.use.external.tariffs.fee", "true");
        doAnswer(invocation -> {
            TariffFindQuery findQuery = invocation.getArgument(0);
            Assertions.assertTrue(findQuery.getIsActive(), "Only active tariffs should be available");

            return new TariffsIterator((pageNumber, batchSize) -> {
                if (pageNumber != 0) {
                    return List.of();
                }
                return getTariffs()
                        .stream()
                        .filter(tariff -> findQuery.getServiceTypes().contains(tariff.getServiceType()))
                        .filter(tariff -> findQuery.getModelType() == tariff.getModelType())
                        .collect(Collectors.toList());
            });
        }).when(tariffsService).findTariffs(Mockito.any(TariffFindQuery.class));

        DbSupplierCategoryFeeDao dbSupplierCategoryFeeDao = new DbSupplierCategoryFeeDao(tariffsService);

        List<SupplierCategoryFee> actual = dbSupplierCategoryFeeDao.getFee(DATE, MbiBlueOrderType.FULFILLMENT);

        assertThat(actual, containsInAnyOrder(
                EXPECTED_FEE_AND_LOYALTY_PARTICIPATION_FEE.toArray(SupplierCategoryFee[]::new)
        ));

       actual = dbSupplierCategoryFeeDao.getFee(DATE, MbiBlueOrderType.DROP_SHIP);

       assertThat(actual, containsInAnyOrder(
                EXPECTED_FEE_FBS.toArray(SupplierCategoryFee[]::new)
       ));
    }

    @Test
    void testMinFeeExternalTariffs() {
        environmentService.setValue("mbi.billing.use.external.tariffs.fee", "true");
        doAnswer(invocation -> {
            TariffFindQuery findQuery = invocation.getArgument(0);
            Assertions.assertTrue(findQuery.getIsActive(), "Only active tariffs should be available");

            return new TariffsIterator((pageNumber, batchSize) -> {
                if (pageNumber != 0) {
                    return List.of();
                }
                return getTariffs()
                        .stream()
                        .filter(tariff -> tariff.getServiceType() == ServiceTypeEnum.MIN_FEE)
                        .collect(Collectors.toList());
            });
        }).when(tariffsService).findTariffs(Mockito.any(TariffFindQuery.class));

        DbSupplierCategoryFeeDao dbSupplierCategoryFeeDao = new DbSupplierCategoryFeeDao(tariffsService);

        List<SupplierCategoryFee> actual = dbSupplierCategoryFeeDao.getMinFee(DATE, MbiBlueOrderType.FULFILLMENT);

        assertThat(actual, containsInAnyOrder(
                EXPECTED_MIN_FEE.toArray(SupplierCategoryFee[]::new)
        ));
    }

    private static List<TariffDTO> getTariffs() {
        return List.of(
                createTariff(1L, 2L, LocalDate.of(2018, 1, 1), null, ServiceTypeEnum.FEE, List.of(
                        createMeta(1L, new BigDecimal("0.03"))
                )),
                createTariff(2L, null, LocalDate.of(2018, 1, 1), null, ServiceTypeEnum.FEE, List.of(
                        createMeta(10L, new BigDecimal("0.30"))
                )),
                createTariff(3L, 200L, LocalDate.of(2018, 1, 1), null, ServiceTypeEnum.FEE, List.of(
                        createMeta(100L, new BigDecimal("3.00"))
                )),
                createTariff(4L, null, LocalDate.of(2018, 1, 1), null, ServiceTypeEnum.MIN_FEE, List.of(
                        createMinFeeMeta(1L, new BigDecimal("0.10"))
                )),
                createTariff(5L, 2L, LocalDate.of(2018, 1, 1), null, ServiceTypeEnum.MIN_FEE, List.of(
                        createMinFeeMeta(2L, new BigDecimal("1.00"))
                )),
                createTariff(6L, 2L, LocalDate.of(2018, 1, 1), null, ServiceTypeEnum.LOYALTY_PARTICIPATION_FEE, List.of(
                        createMeta(2L, new BigDecimal("1.00"))
                )),
                createTariff(7L, null, LocalDate.of(2018, 1, 1), null, ServiceTypeEnum.FEE, List.of(
                        createMeta(100L, new BigDecimal("2.00"), true)), ModelType.FULFILLMENT_BY_SELLER
                ));
    }

    private static TariffDTO createTariff(long id, Long supplierId, LocalDate from, LocalDate to,
                                          ServiceTypeEnum serviceType, List<Object> meta, ModelType modelType) {
        TariffDTO tariff = new TariffDTO();
        tariff.setId(id);
        tariff.setIsActive(true);
        tariff.setServiceType(serviceType);
        tariff.setDateFrom(from);
        tariff.setDateTo(to);
        tariff.setPartner(new Partner().id(supplierId).type(PartnerType.SUPPLIER));
        tariff.setMeta(meta);
        tariff.setModelType(modelType);
        return tariff;
    }

    private static TariffDTO createTariff(
            long id, Long supplierId, LocalDate from, LocalDate to,
            ServiceTypeEnum serviceType, List<Object> meta
    ) {
        return createTariff(id, supplierId, from, to, serviceType, meta, ModelType.FULFILLMENT_BY_YANDEX);
    }

    private static CommonJsonSchema createMeta(long categoryId, BigDecimal amount) {
        return createMeta(categoryId, amount, false);
    }

    private static CommonJsonSchema createMeta(long categoryId, BigDecimal amount, boolean isExpress) {
        return new SupplierCategoryFeeTariffJsonSchema()
                .categoryId(categoryId)
                .isExpress(isExpress)
                .type(CommonJsonSchema.TypeEnum.RELATIVE)
                .amount(amount)
                .currency("RUB")
                .billingUnit(BillingUnitEnum.ITEM);
    }

    private static CommonJsonSchema createMinFeeMeta(long categoryId, BigDecimal amount) {
        return new MinFeeJsonSchema()
                .categoryId(categoryId)
                .type(CommonJsonSchema.TypeEnum.RELATIVE)
                .amount(amount)
                .currency("RUB")
                .billingUnit(BillingUnitEnum.ITEM);
    }
}
