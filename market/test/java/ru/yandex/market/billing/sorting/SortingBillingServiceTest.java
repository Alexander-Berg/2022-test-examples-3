package ru.yandex.market.billing.sorting;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.categories.db.DbCategoryDao;
import ru.yandex.market.billing.categories.db.DbSupplierCategoryFeeService;
import ru.yandex.market.billing.config.OldFirstPartySuppliersIds;
import ru.yandex.market.billing.fulfillment.promo.SupplierPromoTariffDao;
import ru.yandex.market.billing.fulfillment.tariffs.TariffsService;
import ru.yandex.market.billing.imports.logistic.dao.LogisticOrdersDao;
import ru.yandex.market.billing.model.billing.BillingServiceType;
import ru.yandex.market.billing.service.environment.EnvironmentAwareDateValidationService;
import ru.yandex.market.billing.service.environment.EnvironmentService;
import ru.yandex.market.billing.sorting.model.SortingDailyTariff;
import ru.yandex.market.billing.sorting.model.SortingOrderTariff;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.order.OrderBilledAmountsDao;
import ru.yandex.market.core.sorting.model.SortingIntakeType;
import ru.yandex.market.fulfillment.entities.base.DateTimeInterval;

@ExtendWith(MockitoExtension.class)
class SortingBillingServiceTest extends FunctionalTest {

    private static final LocalDate DATE_2020_01_09 = LocalDate.of(2020, 1, 9);
    private static final List<SortingOrderTariff> DEFAULT_ORDER_TARIFFS = List.of(
            createSortingTariff(
                    1500,
                    SortingIntakeType.SELF_DELIVERY
            ),
            createSortingTariff(
                    2500,
                    SortingIntakeType.INTAKE
            )
    );
    private static final List<SortingDailyTariff> DEFAULT_DAILY_TARIFFS = List.of(
            createDailyTariff(
                    100000,
                    null
            ),
            createDailyTariff(
                    2000,
                    3L
            )
    );
    @Autowired
    private EnvironmentAwareDateValidationService environmentAwareDateValidationService;
    @Autowired
    private TransactionTemplate pgTransactionTemplate;
    @Autowired
    private SortingBillingDao sortingBillingDao;
    @Autowired
    private LogisticOrdersDao logisticOrdersDao;
    @Autowired
    private EnvironmentService environmentService;
    @Autowired
    private SupplierPromoTariffDao supplierPromoTariffDao;
    @Autowired
    private TariffsService clientTariffsService;
    @Autowired
    private DbCategoryDao categoryDao;
    @Autowired
    private DbSupplierCategoryFeeService supplierCategoryFeeDao;
    @Autowired
    private OrderBilledAmountsDao orderBilledAmountsDao;
    @Autowired
    private OldFirstPartySuppliersIds oldFirstPartySuppliersIds;
    private SortingBillingService sortingBillingService;
    @Mock
    private SortingOrdersTariffDao sortingOrdersTariffDao;
    @Mock
    private SortingDailyTariffDao sortingDailyTariffDao;

    private static OffsetDateTime createOffsetDateTime(LocalDate date) {
        return ZonedDateTime.of(date, LocalTime.MIDNIGHT, ZoneId.systemDefault()).toOffsetDateTime();
    }

    private static SortingOrderTariff createSortingTariff(
            long value,
            SortingIntakeType intakeType) {
        return new SortingOrderTariff.Builder().setDateTimeInterval(
                        new DateTimeInterval(
                                createOffsetDateTime(LocalDate.of(2020, 1, 1)),
                                createOffsetDateTime(LocalDate.MAX)
                        )
                )
                .setServiceType(BillingServiceType.SORTING)
                .setValue(value)
                .setSupplierId(null)
                .setIntakeType(intakeType)
                .build();
    }

    private static SortingDailyTariff createDailyTariff(
            long value,
            Long supplierId) {
        return new SortingDailyTariff.Builder().setDateTimeInterval(
                        new DateTimeInterval(
                                createOffsetDateTime(LocalDate.of(2020, 1, 1)),
                                createOffsetDateTime(LocalDate.MAX)
                        )
                )
                .setValue(value)
                .setSupplierId(supplierId)
                .build();
    }

    @BeforeEach
    void init() {
        sortingBillingService = new SortingBillingService(
                categoryDao,
                supplierCategoryFeeDao,
                orderBilledAmountsDao,
                environmentAwareDateValidationService,
                pgTransactionTemplate,
                sortingBillingDao,
                sortingOrdersTariffDao,
                sortingDailyTariffDao,
                logisticOrdersDao,
                environmentService,
                supplierPromoTariffDao,
                clientTariffsService,
                oldFirstPartySuppliersIds);
    }

    private void mock(List<SortingOrderTariff> orderTariffs, List<SortingDailyTariff> dailyTariffs) {
        Mockito.when(sortingOrdersTariffDao.getAllSortingTariffs(Mockito.any())).thenReturn(orderTariffs);
        Mockito.when(sortingDailyTariffDao.getAllSortingDailyTariffs(Mockito.any())).thenReturn(dailyTariffs);
    }

    @DbUnitDataSet(
            before = "SortingBillingServiceTestEmpty_before.csv",
            after = "SortingBillingServiceTestEmpty_after.csv"
    )
    @DisplayName("Не должны падать при отсутствии данных.")
    @Test
    void testEmptyTables() {
        sortingBillingService.process(DATE_2020_01_09);
    }

    @DbUnitDataSet(
            before = "SortingBillingServiceTestBilledCorrectDay.lom.before.csv",
            after = "SortingBillingServiceTestBilledCorrectDay.only_lom.after.csv"
    )
    @DisplayName("Обилливается только указанная дата.")
    @Test
    void testBillingCorrectDay() {
        mock(DEFAULT_ORDER_TARIFFS, DEFAULT_DAILY_TARIFFS);
        sortingBillingService.process(DATE_2020_01_09);
    }

    @DbUnitDataSet(
            before = "SortingBillingServiceTestWontRewriteOldRecords.only_lom.before.csv",
            after = "SortingBillingServiceTestWontRewriteOldRecords.lom.after.csv"
    )
    @DisplayName("Не должны перезаписывать старые записи тех дней, которые не обилливались в" +
            "текущем контексте запуска сервиса.")
    @Test
    void testShouldNotRewriteOldDates_whenBillNewDate() {
        mock(DEFAULT_ORDER_TARIFFS, DEFAULT_DAILY_TARIFFS);
        sortingBillingService.process(DATE_2020_01_09);
    }

    @DbUnitDataSet(
            before = "SortingBillingServiceTestRewriteDateWhenBillAgain.only_lom.before.csv",
            after = "SortingBillingServiceTestRewriteDateWhenBillAgain.only_lom.after.csv"
    )
    @DisplayName("Пересчитываем предыдущие значения, если они за тот же день, что и обилливаемая дата.")
    @Test
    void testShouldRewriteRecordsForDate_whenBillAgain() {
        mock(DEFAULT_ORDER_TARIFFS, DEFAULT_DAILY_TARIFFS);
        sortingBillingService.process(DATE_2020_01_09);
    }

    @DbUnitDataSet(
            before = "SortingBillingServiceTestBillDsAndScTogether_before.csv",
            after = "SortingBillingServiceTestBill.lom.only.after.csv"
    )
    @DisplayName("Должны обилливать данные только из LOM'а.")
    @Test
    void testShouldBillDsAndScOrdersWhenGivenBoth() {
        mock(DEFAULT_ORDER_TARIFFS, DEFAULT_DAILY_TARIFFS);
        sortingBillingService.process(DATE_2020_01_09);
    }

    @DbUnitDataSet(
            before = "SortingBillingServiceTestBilledCorrectDayPromo.lom.before.csv",
            after = "SortingBillingServiceTestBilledCorrectDayPromo.only_lom.after.csv"
    )
    @DisplayName("Обилливается только указанная дата.")
    @Test
    void testBillingCorrectDayWithPromoTariff() {
        mock(DEFAULT_ORDER_TARIFFS, List.of(
                createDailyTariff(
                        100000,
                        null
                ),
                createDailyTariff(
                        2000,
                        3L
                ),
                createDailyTariff(
                        2000,
                        6L
                ),
                createDailyTariff(
                        2000,
                        7L
                )));
        sortingBillingService.process(DATE_2020_01_09);
    }

    @Test
    @DisplayName("Тест с фильтрацией")
    @DbUnitDataSet(
            before = "SortingBillingServiceTestBilled.filtering.before.csv",
            after = "SortingBillingServiceTestBilled.filtering.lom.after.csv"
    )
    void testWithFiltering() {
        mock(DEFAULT_ORDER_TARIFFS, List.of(
                createDailyTariff(
                        100000,
                        null
                )
        ));
        sortingBillingService.process(LocalDate.of(2021, 5, 19));
        sortingBillingService.process(LocalDate.of(2021, 5, 20));
    }

    @Test
    @DisplayName("Тест игнорирования чекаутера")
    @DbUnitDataSet(
            before = "SortingBillingServiceTestBilled.checkouter.before.csv",
            after = "SortingBillingServiceTestBilled.checkouter.only_lom.after.csv"
    )
    void testIgnoreCheckouter() {
        sortingBillingService.process(DATE_2020_01_09);
    }

    @Test
    @DbUnitDataSet(
            before = "SortingBillingServiceTestBilled.testWithRebillingWithSelfDelivery.before.csv",
            after = "SortingBillingServiceTestBilled.testWithRebillingWithSelfDelivery.after.csv"
    )
    void testWithRebillingWithSelfDelivery() {
        mock(DEFAULT_ORDER_TARIFFS, List.of(
                createDailyTariff(
                        150000,
                        null
                )
        ));
        sortingBillingService.process(LocalDate.of(2021, 7, 14));
    }

    @Test
    @DisplayName("Тест на обиливание конкретных поставщиков")
    @DbUnitDataSet(
            before = "SortingBillingServiceTestBilled.specificPartners.before.csv",
            after = "SortingBillingServiceTestBilled.specificPartners.after.csv"
    )
    void testBillingForSpecificPartners() {
        mock(DEFAULT_ORDER_TARIFFS, DEFAULT_DAILY_TARIFFS);
        sortingBillingService.processForPartners(DATE_2020_01_09, Set.of(1L, 3L));
    }


    @Test
    @DisplayName("Проверяем что зануляются только нужные поставщики.")
    @DbUnitDataSet(
            before = "SortingBillingServiceTestBilled.clearOnlyNecessaryPartners.before.csv",
            after = "SortingBillingServiceTestBilled.clearOnlyNecessaryPartners.after.csv"
    )
    void testClearOnlyNecessaryPartners() {
        mock(DEFAULT_ORDER_TARIFFS, DEFAULT_DAILY_TARIFFS);
        sortingBillingService.processForPartners(DATE_2020_01_09, Set.of(1L, 2L));
    }
}
