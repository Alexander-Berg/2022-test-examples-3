package ru.yandex.market.core.sorting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.fulfillment.model.BillingServiceType;
import ru.yandex.market.core.fulfillment.tariff.TariffsIterator;
import ru.yandex.market.core.fulfillment.tariff.TariffsService;
import ru.yandex.market.core.sorting.matchers.SortingDailyTariffMatcher;
import ru.yandex.market.core.sorting.matchers.SortingOrderTariffMatcher;
import ru.yandex.market.core.sorting.model.SortingDailyTariff;
import ru.yandex.market.core.sorting.model.SortingIntakeType;
import ru.yandex.market.core.sorting.model.SortingOrderTariff;
import ru.yandex.market.fulfillment.entities.base.DateTimeInterval;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.mbi.tariffs.client.model.BillingUnitEnum;
import ru.yandex.market.mbi.tariffs.client.model.CommonJsonSchema;
import ru.yandex.market.mbi.tariffs.client.model.MinDailyTariffJsonSchema;
import ru.yandex.market.mbi.tariffs.client.model.OrderStatusEnum;
import ru.yandex.market.mbi.tariffs.client.model.Partner;
import ru.yandex.market.mbi.tariffs.client.model.PartnerType;
import ru.yandex.market.mbi.tariffs.client.model.ServiceTypeEnum;
import ru.yandex.market.mbi.tariffs.client.model.SortingTariffJsonSchema;
import ru.yandex.market.mbi.tariffs.client.model.TariffDTO;
import ru.yandex.market.mbi.tariffs.client.model.TariffFindQuery;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;

class SortingTariffServiceTest extends FunctionalTest {

    private static final LocalDate BILLING_DATE = LocalDate.of(2020, 1, 1);

    @Autowired
    private SortingDailyTariffDao sortingDailyTariffDao;

    @Autowired
    private SortingOrdersTariffDao sortingOrdersTariffDao;

    @Autowired
    private TariffsService tariffsService;

    @Autowired
    private EnvironmentService environmentService;

    private SortingTariffService sortingTariffService;

    private static final List<SortingDailyTariff> DAILY_TARIFF = List.of(
            createDailyTariff(
                    LocalDate.of(2019, 1, 1),
                    LocalDate.of(2020, 1, 1),
                    10000,
                    1L
            ),
            createDailyTariff(
                    LocalDate.of(2020, 1, 1),
                    LocalDate.MAX,
                    100000,
                    1L
            )
    );

    private static final List<SortingOrderTariff> DEFAULT_ORDER = List.of(
            createSortingTariff(
                    LocalDate.of(2019, 1, 1),
                    LocalDate.of(2019, 12, 1),
                    2600,
                    1L,
                    SortingIntakeType.INTAKE
            ),
            createSortingTariff(
                    LocalDate.of(2019, 1, 1),
                    LocalDate.MAX,
                    2500,
                    1L,
                    SortingIntakeType.INTAKE
            ),
            createSortingTariff(
                    LocalDate.of(2019, 1, 1),
                    LocalDate.of(2019, 12, 1),
                    15000,
                    2L,
                    SortingIntakeType.SELF_DELIVERY
            ),
            createSortingTariff(
                    LocalDate.of(2019, 1, 1),
                    LocalDate.MAX,
                    1500,
                    2L,
                    SortingIntakeType.SELF_DELIVERY
            )
    );

    private static OffsetDateTime createOffsetDateTime(LocalDate date) {
        return ZonedDateTime.of(date, LocalTime.MIDNIGHT, ZoneId.systemDefault()).toOffsetDateTime();
    }

    private static SortingOrderTariff createSortingTariff(
            LocalDate from,
            LocalDate to,
            long value,
            Long supplierId,
            SortingIntakeType intakeType) {
        return new SortingOrderTariff.Builder().setDateTimeInterval(
                new DateTimeInterval(createOffsetDateTime(from), createOffsetDateTime(to))
        )
                .setServiceType(BillingServiceType.SORTING)
                .setValue(value)
                .setSupplierId(supplierId)
                .setIntakeType(intakeType)
                .build();
    }

    private static SortingDailyTariff createDailyTariff(
            LocalDate from,
            LocalDate to,
            long value,
            Long supplierId) {
        return new SortingDailyTariff.Builder().setDateTimeInterval(
                new DateTimeInterval(createOffsetDateTime(from), createOffsetDateTime(to))
        )
                .setValue(value)
                .setSupplierId(supplierId)
                .build();
    }

    private void mock(List<SortingOrderTariff> orderTariffs, List<SortingDailyTariff> dailyTariffs) {
        Mockito.doReturn(orderTariffs).when(sortingOrdersTariffDao).getAllSortingTariffs(Mockito.any());
        Mockito.doReturn(dailyTariffs).when(sortingDailyTariffDao).getAllSortingDailyTariffs(Mockito.any());
        sortingTariffService = new SortingTariffService(sortingOrdersTariffDao, sortingDailyTariffDao, BILLING_DATE);
    }

    private void mock() {
        mock(List.of(), List.of());
    }

    @BeforeEach
    void beforeEach() {
        mock();
    }

    @Test
    void testGetAllEmpty() {

        SortingDailyTariff dailyTariff =
                sortingTariffService.getDailyTariff(1L, SortingIntakeType.SELF_DELIVERY);
        assertEquals(0, dailyTariff.getValue());
        assertThrows(RuntimeException.class,
                () -> sortingTariffService.getDailyTariff(1L, SortingIntakeType.INTAKE));
        var tariff = sortingTariffService.getOrderTariff(1L, SortingIntakeType.INTAKE);
        assertThat(tariff, notNullValue());
        assertThat(
                tariff,
                allOf(
                        SortingOrderTariffMatcher.hasSupplierId(null),
                        SortingOrderTariffMatcher.hasServiceType(BillingServiceType.SORTING),
                        SortingOrderTariffMatcher.hasIntakeType(SortingIntakeType.SELF_DELIVERY),
                        SortingOrderTariffMatcher.hasValue(3000L)
                )
        );
    }

    @Test
    void testShouldReturnTariffForCorrectDate() {
        mock(List.of(
                createSortingTariff(
                        LocalDate.of(2019, 1, 1),
                        LocalDate.of(2019, 12, 31),
                        2600,
                        1L,
                        SortingIntakeType.INTAKE
                ),
                createSortingTariff(
                        LocalDate.of(2020, 1, 1),
                        LocalDate.MAX,
                        2500,
                        1L,
                        SortingIntakeType.INTAKE
                ),
                createSortingTariff(
                        LocalDate.of(2019, 1, 1),
                        LocalDate.of(2019, 12, 31),
                        15000,
                        2L,
                        SortingIntakeType.SELF_DELIVERY
                ),
                createSortingTariff(
                        LocalDate.of(2020, 1, 1),
                        LocalDate.MAX,
                        1500,
                        2L,
                        SortingIntakeType.SELF_DELIVERY
                )
        ), List.of(
                createDailyTariff(
                        LocalDate.of(2019, 1, 1),
                        LocalDate.of(2020, 1, 1),
                        10000,
                        1L
                ),
                createDailyTariff(
                        LocalDate.of(2020, 1, 1),
                        LocalDate.MAX,
                        100000,
                        1L
                )
        ));

        SortingDailyTariff dailyTariff = sortingTariffService.getDailyTariff(1L, SortingIntakeType.INTAKE);
        assertThat(
                dailyTariff,
                allOf(
                        SortingDailyTariffMatcher.hasSupplierId(1L),
                        SortingDailyTariffMatcher.hasValue(100000L)
                )
        );

        SortingOrderTariff orderIntakeTariff = sortingTariffService.getOrderTariff(1L, SortingIntakeType.INTAKE);
        assertThat(orderIntakeTariff, notNullValue());
        assertThat(
                orderIntakeTariff,
                allOf(
                        SortingOrderTariffMatcher.hasSupplierId(1L),
                        SortingOrderTariffMatcher.hasServiceType(BillingServiceType.SORTING),
                        SortingOrderTariffMatcher.hasIntakeType(SortingIntakeType.INTAKE),
                        SortingOrderTariffMatcher.hasValue(2500L)
                )
        );
        SortingOrderTariff orderSelfDeliveryTariff = sortingTariffService.getOrderTariff(2L, SortingIntakeType.SELF_DELIVERY);
        assertThat(orderSelfDeliveryTariff, notNullValue());
        assertThat(
                orderSelfDeliveryTariff,
                allOf(
                        SortingOrderTariffMatcher.hasSupplierId(2L),
                        SortingOrderTariffMatcher.hasServiceType(BillingServiceType.SORTING),
                        SortingOrderTariffMatcher.hasIntakeType(SortingIntakeType.SELF_DELIVERY),
                        SortingOrderTariffMatcher.hasValue(1500L)
                )
        );
    }

    @Test
    void testSortingExternalTariffs() {
        mock(DEFAULT_ORDER, DAILY_TARIFF);

        environmentService.setValue("mbi.billing.use.external.tariffs.sorting", "true");
        doAnswer(invocation -> {
            TariffFindQuery findQuery = invocation.getArgument(0);
            Assertions.assertTrue(findQuery.getIsActive(), "Only active tariffs should be available");

            LocalDate targetDate = Objects.requireNonNull(findQuery.getTargetDate());
            return new TariffsIterator((pageNumber, batchSize) -> {
                if (pageNumber != 0) {
                    return List.of();
                }

                return getSortingTariffs()
                        .stream()
                        .filter(tariff -> tariff.getServiceType() == ServiceTypeEnum.SORTING)
                        .filter(tariff -> {
                            LocalDate to = tariff.getDateTo();
                            LocalDate from = tariff.getDateFrom();
                            return targetDate.compareTo(from) >= 0
                                    && (to == null || targetDate.compareTo(to) < 0);
                        })
                        .collect(Collectors.toList());
            });
        }).when(tariffsService).findTariffs(any(TariffFindQuery.class));
        sortingTariffService = new SortingTariffService(sortingOrdersTariffDao, sortingDailyTariffDao, BILLING_DATE);

        SortingOrderTariff orderIntakeTariff = sortingTariffService.getOrderTariff(1L, SortingIntakeType.INTAKE);
        assertThat(orderIntakeTariff, notNullValue());
        assertThat(
                orderIntakeTariff,
                allOf(
                        SortingOrderTariffMatcher.hasSupplierId(1L),
                        SortingOrderTariffMatcher.hasServiceType(BillingServiceType.SORTING),
                        SortingOrderTariffMatcher.hasIntakeType(SortingIntakeType.INTAKE),
                        SortingOrderTariffMatcher.hasValue(2500L)
                )
        );
        SortingOrderTariff orderSelfDeliveryTariff = sortingTariffService.getOrderTariff(2L, SortingIntakeType.SELF_DELIVERY);
        assertThat(orderSelfDeliveryTariff, notNullValue());
        assertThat(
                orderSelfDeliveryTariff,
                allOf(
                        SortingOrderTariffMatcher.hasSupplierId(2L),
                        SortingOrderTariffMatcher.hasServiceType(BillingServiceType.SORTING),
                        SortingOrderTariffMatcher.hasIntakeType(SortingIntakeType.SELF_DELIVERY),
                        SortingOrderTariffMatcher.hasValue(1500L)
                )
        );
    }

    @Test
    void testMinDailyExternalTariffs() {
        mock(DEFAULT_ORDER, DAILY_TARIFF);

        environmentService.setValue("mbi.billing.use.external.tariffs.min_daily", "true");
        doAnswer(invocation -> {
            TariffFindQuery findQuery = invocation.getArgument(0);
            Assertions.assertTrue(findQuery.getIsActive(), "Only active tariffs should be available");

            LocalDate targetDate = Objects.requireNonNull(findQuery.getTargetDate());
            return new TariffsIterator((pageNumber, batchSize) -> {
                if (pageNumber != 0) {
                    return List.of();
                }

                return getMinDailyTariffs()
                        .stream()
                        .filter(tariff -> tariff.getServiceType() == ServiceTypeEnum.MIN_DAILY)
                        .filter(tariff -> {
                            LocalDate to = tariff.getDateTo();
                            LocalDate from = tariff.getDateFrom();
                            return targetDate.compareTo(from) >= 0
                                    && (to == null || targetDate.compareTo(to) < 0);
                        })
                        .collect(Collectors.toList());
            });
        }).when(tariffsService).findTariffs(any(TariffFindQuery.class));
        sortingTariffService = new SortingTariffService(sortingOrdersTariffDao, sortingDailyTariffDao, BILLING_DATE);
        SortingDailyTariff dailyTariff = sortingTariffService.getDailyTariff(1L, SortingIntakeType.INTAKE);
        assertThat(
                dailyTariff,
                allOf(
                        SortingDailyTariffMatcher.hasSupplierId(1L),
                        SortingDailyTariffMatcher.hasValue(100000L)
                )
        );
    }

    private static List<TariffDTO> getMinDailyTariffs() {
        return List.of(
                createTariff(1L, 1L, LocalDate.of(2019, 1, 1), LocalDate.of(2020,1,1), ServiceTypeEnum.MIN_DAILY, List.of(
                    createMinDailyMeta(new BigDecimal("100.00"))
                )),
                createTariff(2L, 1L, LocalDate.of(2020, 1, 1), null, ServiceTypeEnum.MIN_DAILY, List.of(
                        createMinDailyMeta(new BigDecimal("1000.00"))
                ))
        );
    }

    private static List<TariffDTO> getSortingTariffs() {
        return List.of(
                createTariff(1L, 1L, LocalDate.of(2019,1,1), LocalDate.of(2019, 12,31), ServiceTypeEnum.SORTING, List.of(
                        createSortingMeta(new BigDecimal("26.00"), SortingTariffJsonSchema.IntakeTypeEnum.INTAKE)
                )),
                createTariff(2L, 1L, LocalDate.of(2019,1,1), null, ServiceTypeEnum.SORTING, List.of(
                        createSortingMeta(new BigDecimal("25.00"), SortingTariffJsonSchema.IntakeTypeEnum.INTAKE)
                )),
                createTariff(3L, 2L, LocalDate.of(2019,1,1), LocalDate.of(2019, 12,31), ServiceTypeEnum.SORTING, List.of(
                        createSortingMeta(new BigDecimal("150.00"), SortingTariffJsonSchema.IntakeTypeEnum.SELF_DELIVERY)
                )),
                createTariff(4L, 2L, LocalDate.of(2019,1,1), null, ServiceTypeEnum.SORTING, List.of(
                        createSortingMeta(new BigDecimal("15.00"), SortingTariffJsonSchema.IntakeTypeEnum.SELF_DELIVERY)
                ))
        );
    }

    private static TariffDTO createTariff(long id, long supplierId, LocalDate from, LocalDate to, ServiceTypeEnum serviceType, List<Object> meta) {
        TariffDTO tariff = new TariffDTO();
        tariff.setId(id);
        tariff.setIsActive(true);
        tariff.setServiceType(serviceType);
        tariff.setDateFrom(from);
        tariff.setDateTo(to);
        tariff.setPartner(new Partner().id(supplierId).type(PartnerType.SUPPLIER));
        tariff.setMeta(meta);
        return tariff;
    }

    private static CommonJsonSchema createSortingMeta(BigDecimal amount, SortingTariffJsonSchema.IntakeTypeEnum intakeType) {
        return new SortingTariffJsonSchema()
                .intakeType(intakeType)
                .orderStatus(OrderStatusEnum.DELIVERED)
                .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                .amount(amount)
                .currency("RUB")
                .billingUnit(BillingUnitEnum.ORDER);
    }

    private static CommonJsonSchema createMinDailyMeta(BigDecimal amount) {
        return new MinDailyTariffJsonSchema()
                .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                .amount(amount)
                .currency("RUB")
                .billingUnit(BillingUnitEnum.DAILY);
    }
}
