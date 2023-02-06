package ru.yandex.market.billing.distribution.share;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.billing.distribution.imports.dao.DistributionPartnerDao;
import ru.yandex.market.billing.pg.dao.PgCategoryDao;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.billing.distribution.share.DistributionShareDao;
import ru.yandex.market.core.order.returns.ReturnOrdersDao;

/**
 * Тесты для {@link DistributionShareCalculationService}.
 *
 * @author vbudnev
 */
@DbUnitDataSet(before = {
        "db/categories.before.csv",
        "db/categories.rates.before.csv",
        "db/categories.rates.white.before.csv"
})
class DistributionShareCalculationServiceTest extends FunctionalTest {
    private static final LocalDate DATE_2019_01_05 = LocalDate.of(2019, 1, 5);
    private static final LocalDate DATE_2019_01_20 = LocalDate.of(2019, 1, 20);
    private static final LocalDate DATE_2019_12_01 = LocalDate.of(2019, 12, 1);
    private static final LocalDate DATE_2019_12_02 = LocalDate.of(2019, 12, 2);
    private static final LocalDate DATE_2019_12_03 = LocalDate.of(2019, 12, 3);
    private static final LocalDate DATE_2019_12_05 = LocalDate.of(2019, 12, 5);
    private static final LocalDate DATE_2019_12_08 = LocalDate.of(2019, 12, 8);
    private static final LocalDate DATE_2019_12_22 = LocalDate.of(2019, 12, 22);
    private static final LocalDate DATE_2019_12_25 = LocalDate.of(2019, 12, 25);
    private static final LocalDate DATE_2019_12_21 = LocalDate.of(2019, 12, 21);
    private static final LocalDate DATE_2019_12_30 = LocalDate.of(2019, 12, 30);
    private static final LocalDate DATE_2020_05_15 = LocalDate.of(2020, 5, 15);
    private static final LocalDate DATE_2020_06_09 = LocalDate.of(2020, 6, 9);
    private static final LocalDate DATE_2020_06_22 = LocalDate.of(2020, 6, 22);
    private static final LocalDate DATE_2020_09_30 = LocalDate.of(2020, 9, 30);
    private static final LocalDate DATE_2020_10_10 = LocalDate.of(2020, 10, 10);
    private static final LocalDate DATE_2021_07_14 = LocalDate.of(2021, 7, 14);
    private static final LocalDate DATE_2021_07_16 = LocalDate.of(2021, 7, 16);
    private static final LocalDateTime DATETIME_2021_09_15 = LocalDateTime.of(2021,9,15,0,0);

    @Autowired
    private DistributionShareDao distributionShareDao;

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    private PgCategoryDao pgCategoryDao;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private DistributionShareCalculationService partialService;

    @Autowired
    private DistributionShareCalculationService distributionShareCalculationService;

    @Autowired
    private DistributionCategoryTariffDao distributionCategoryTariffDao;

    @Autowired
    private ReturnOrdersDao returnOrdersDao;

    @Autowired
    private DistributionPartnerDao distributionPartnerDao;

    @BeforeEach
    void beforeEach() {
        //конфигурация сервиса, кторая смотрит на фейковую табличку, имитрующую выдачу вьюхи - для удобства
        // подготовки данных
        partialService = new DistributionShareCalculationService(
                pgCategoryDao,
                distributionShareDao,
                transactionTemplate,
                new DistributionClidDao(
                        namedParameterJdbcTemplate,
                        "MARKET_BILLING.V_DISTRIBUTION_ORDERS"
                ),
                distributionCategoryTariffDao,
                returnOrdersDao,
                distributionPartnerDao
        );
    }

    /**
     * 100[копеек] * 50[штук] * 0.05[коэффициент от количества] / (1+0.18[ндс])
     */
    @DisplayName("Smoke test для одной товарной позиции")
    @DbUnitDataSet(
            before = {"db/DistributionShareCalculationServiceTest.1rec.before.csv"},
            after = "db/DistributionShareCalculationServiceTest.1rec.after.csv"
    )
    @Test
    void test_1recTest() {
        partialService.recalculateShare(DATE_2019_12_02);
    }

    @DisplayName("Проверка фильтра по дате")
    @DbUnitDataSet(
            before = {"db/DistributionShareCalculationServiceTest.date_range.before.csv"},
            after = "db/DistributionShareCalculationServiceTest.date_range.after.csv"
    )
    @Test
    void test_dateRange() {
        partialService.recalculateShare(DATE_2019_12_02);
    }

    @DisplayName("Разные цены и ндс")
    @DbUnitDataSet(
            before = {"db/DistributionShareCalculationServiceTest.price_and_vat.before.csv"},
            after = "db/DistributionShareCalculationServiceTest.price_and_vat.after.csv"
    )
    @Test
    void test_pricesAndVats() {
        partialService.recalculateShare(DATE_2019_12_02);
    }

    /**
     * 100[копеек] * 50[штук] * 0.02[коэффициент от категории] / (1+0.18[ндс])
     * 100[копеек] * 150[штук] * 0.02[коэффициент от категории] / (1+0.18[ндс])
     */
    @DisplayName("Категорийный коэффициент имеет приоритет над количественным")
    @DbUnitDataSet(
            before = "db/DistributionShareCalculationServiceTest.category_tariff_priority.before.csv",
            after =  "db/DistributionShareCalculationServiceTest.category_tariff_priority.after.csv"
    )
    @Test
    void test_categoryPriority() {
        partialService.recalculateShare(DATE_2019_12_02);
    }


    @DisplayName("В рамках заказа тарифный коэфициент один для всех позиций")
    @DbUnitDataSet(
            before = {

                    "db/DistributionShareCalculationServiceTest.same_rate_in_order.before.csv"},
            after = "db/DistributionShareCalculationServiceTest.same_rate_in_order.after.csv"
    )
    @Test
    void test_sameRateForItems() {
        partialService.recalculateShare(DATE_2019_12_02);
    }

    @DisplayName("Группировка по clid и vid null + vid string")
    @DbUnitDataSet(
            before = {"db/DistributionShareCalculationServiceTest.vid.before.csv"},
            after = "db/DistributionShareCalculationServiceTest.vid.after.csv"
    )
    @Test
    void test_vidMutations() {
        partialService.recalculateShare(DATE_2019_12_02);
    }

    @DisplayName("Проверка предсортировки и корректного трейса количества в рамках clid+vid на большом количестве")
    @DbUnitDataSet(
            before = {"db/DistributionShareCalculationServiceTest.mixed_unordered.before.csv"},
            after = "db/DistributionShareCalculationServiceTest.mixed_unordered.after.csv"
    )
    @Test
    void test_mixedUnordered() {
        partialService.recalculateShare(DATE_2019_12_01);
        partialService.recalculateShare(DATE_2019_12_03);
        partialService.recalculateShare(DATE_2019_12_05);
        partialService.recalculateShare(DATE_2019_12_08);
    }

    @DisplayName("Тест с продовым вариантом view")
    @DbUnitDataSet(
            before = "db/DistributionShareCalculationServiceTest.fair.old.before.csv",
            after = "db/DistributionShareCalculationServiceTest.fair.old.after.csv"
    )
    @Test
    void test_fairView() {
        distributionShareCalculationService.recalculateShare(DATE_2019_01_05);
    }

    @DisplayName("Тест с продовым вариантом view на новую атрибуцию всего заказа")
    @DbUnitDataSet(
            before = "db/DistributionShareCalculationServiceTest.fair.whole_order.before.csv",
            after = "db/DistributionShareCalculationServiceTest.fair.whole_order.after.csv"
    )
    @Test
    void test_fairWholeOrderView() {
        distributionShareCalculationService.recalculateShare(DATE_2019_01_05);
        distributionShareCalculationService.recalculateShare(DATE_2019_01_20);
    }

    @DisplayName("Тест на неправильную валюту")
    @Test
    @DbUnitDataSet(
            before = "db/DistributionShareCalculationServiceTest.unexpected.currency.before.csv"
    )
    void test_invalidBlueCategory() {
        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> distributionShareCalculationService.recalculateShare(DATE_2019_01_05));
        Assertions.assertEquals("Unexpected currency USD. Expected RUR", exception.getMessage());
    }

    @DisplayName("Тест на партнерские возвраты")
    @DbUnitDataSet(
            before = {"db/DistributionShareCalculationServiceTest.return_partner.before.csv"},
            after = "db/DistributionShareCalculationServiceTest.return_partner.after.csv"
    )
    @Test
    void test_withPartnerReturns() {
        partialService.recalculateShare(DATE_2019_12_02);
    }

    @DisplayName("Тест с продовым вариантом view на разделение первичный/вторичный заказ")
    @DbUnitDataSet(
            before = {"db/categories.rates.different.rate.first.order.before.csv",
                    "db/DistributionShareCalculationServiceTest.fair.whole_order.firstOrder.before.csv"},
            after = "db/DistributionShareCalculationServiceTest.fair.whole_order.firstOrder.after.csv"
    )
    @Test
    void test_fairWholeOrderFirstOrderDifferent() {
        distributionShareCalculationService.recalculateShare(DATE_2019_12_21);
    }

    @DisplayName("Тест с продовым вариантом view на фикс тариф для станций [07.12, 25.12]")
    @DbUnitDataSet(
            before = "db/DistributionShareCalculationServiceTest.fair.yndx_station.before.csv",
            after = "db/DistributionShareCalculationServiceTest.fair.yndx_station.after.csv"
    )
    @Test
    void test_fairYndxStation() {
        distributionShareCalculationService.recalculateShare(DATE_2019_12_30);
    }

    @DisplayName("Тест с продовым вариантом view на бесплатный тариф для стиков сигаретных [12.20, +inf)")
    @DbUnitDataSet(
            before = "db/DistributionShareCalculationServiceTest.fair.cig_sticks.before.csv",
            after = "db/DistributionShareCalculationServiceTest.fair.cig_sticks.after.csv"
    )
    @Test
    void test_fairCigSticks() {
        distributionShareCalculationService.recalculateShare(DATE_2019_12_22);
        distributionShareCalculationService.recalculateShare(DATE_2019_12_25);
    }

    @DisplayName("Проверяем что для фрода выставляется нулевой тариф рейт")
    @DbUnitDataSet(
            before = "db/DistributionShareCalculationServiceTest.fraud.before.csv",
            after = "db/DistributionShareCalculationServiceTest.fraud.after.csv")
    @Test
    void test_shouldReturnZeroTariffRateWhenGivenFraudOrder() {
        distributionShareCalculationService.recalculateShare(DATE_2019_12_21);
    }

    @DisplayName("Проверяем что для over_limit выставляется нулевой тариф рейт")
    @DbUnitDataSet(
            before = "db/DistributionShareCalculationServiceTest.over_limit.before.csv",
            after = "db/DistributionShareCalculationServiceTest.over_limit.after.csv")
    @Test
    void test_shouldReturnZeroTariffRateWhenGivenOverLimit() {
        distributionShareCalculationService.recalculateShare(DATE_2019_12_21);
    }

    @DisplayName("Проверка на учитываение мультизаказов в дистрибуции")
    @DbUnitDataSet(
            before = "db/DistributionShareCalculationServiceTest.multiorder.before.csv",
            after = "db/DistributionShareCalculationServiceTest.multiorder.after.csv"
    )
    @Test
    void test_multiOrderTariffRateWhenGivenUnderLimitForOrder() {
        distributionShareCalculationService.recalculateShare(DATE_2020_05_15);
    }

    @DisplayName("Тест на то, что правильно проставляются информация о возвращении всех товаров заказа")
    @DbUnitDataSet(
            before = "db/DistributionShareCalculationServiceTest.all_items_returned.before.csv",
            after = "db/DistributionShareCalculationServiceTest.all_items_returned.after.csv"
    )
    @Test
    void test_allItemsReturned() {
        distributionShareCalculationService.recalculateShare(DATE_2020_05_15);
    }

    @Test
    @DisplayName("" +
            "Тест на то, что имя тарифа ( как и сам рейт) выбирается в зависимости от даты создания заказа," +
            "а не от даты обиливания заказа"
    )
    @DbUnitDataSet(
            before = "db/DistributionShareCalculationServiceTest.date_depend.before.csv",
            after = "db/DistributionShareCalculationServiceTest.date_depend.after.csv"
    )
    void test_getTariffNameDependOnCreationTime() {
        distributionShareCalculationService.recalculateShare(DATE_2020_06_22);
    }

    @Test
    @DisplayName("Тест на то, что правильно высчитывается мультизаказ, части которого обилены в разные дни")
    @DbUnitDataSet(
            before = "db/DistributionShareCalculationServiceTest.multi_order_different_days.before.csv",
            after = "db/DistributionShareCalculationServiceTest.multi_order_different_days.after.csv"
    )
    void test_multiOrderNormal() {
        distributionShareCalculationService.recalculateShare(DATE_2021_07_14);
        distributionShareCalculationService.recalculateShare(DATE_2021_07_16);
    }

    @Test
    @DisplayName("Тест на белые (DSBS) заказы")
    @DbUnitDataSet(
            before = "db/DistributionShareCalculationServiceTest.whiteOrder.before.csv",
            after = "db/DistributionShareCalculationServiceTest.whiteOrder.after.csv"
    )
    void test_whiteOrder() {
        distributionShareCalculationService.recalculateShare(DATE_2020_09_30);
    }

    @Test
    @DisplayName("Тест на белые (DSBS) заказы проверка по статусу")
    @DbUnitDataSet(
            before = "db/DistributionShareCalculationServiceTest.whiteOrder.statusFilter.before.csv",
            after = "db/DistributionShareCalculationServiceTest.whiteOrder.statusFilter.after.csv"
    )
    void test_whiteOrderStatusFilter() {
        distributionShareCalculationService.recalculateShare(DATE_2020_10_10);
    }

    @Test
    @DisplayName("Тест на белые (DSBS) заказы проверка по дате")
    @DbUnitDataSet(
            before = "db/DistributionShareCalculationServiceTest.whiteOrder.dateFilter.before.csv",
            after = "db/DistributionShareCalculationServiceTest.whiteOrder.dateFilter.after.csv"
    )
    void test_whiteOrderDateFilter() {
        distributionShareCalculationService.recalculateShare(DATE_2020_10_10);
    }

    @Test
    @DisplayName("Тест на игнорирование товаров с item_count = 0")
    @DbUnitDataSet(
            before = "db/DistributionShareCalculationServiceTest.ignoreEmptyItem.before.csv",
            after = "db/DistributionShareCalculationServiceTest.ignoreEmptyItem.after.csv"
    )
    void test_ignoreEmptyItem() {
        distributionShareCalculationService.recalculateShare(DATE_2020_10_10);
    }

    @Test
    @DisplayName("Тест на расчет вознаграждения при наличии промокода клиента партнерской сети")
    @DbUnitDataSet(
            before = "db/DistributionShareCalculationServiceTest.distributionPromo.before.csv",
            after = "db/DistributionShareCalculationServiceTest.distributionPromo.after.csv"
    )
    void test_distributionPromo() {
        distributionShareCalculationService.recalculateShare(DATE_2020_10_10);
    }

    @Test
    @DisplayName("Тест на отключение under_limit/over_limit для заказов, созданных с 19 мая 2021")
    @DbUnitDataSet(
            before = "db/DistributionShareCalculationServiceTest.turnoffUnderOverLimits.before.csv",
            after = "db/DistributionShareCalculationServiceTest.turnoffUnderOverLimits.after.csv"
    )
    void test_turnoffUnderOverLimits() {
        distributionShareCalculationService.recalculateShare(LocalDate.of(2021, Month.MAY, 18));
        distributionShareCalculationService.recalculateShare(LocalDate.of(2021, Month.MAY, 19));
    }

    @Test
    @DisplayName("Тест на отключение FULL_CART_COUPON для заказов, созданных от 7 июня 2021")
    @DbUnitDataSet(
            before = "db/DistributionShareCalculationServiceTest.turnoffFullCartCoupon.before.csv",
            after = "db/DistributionShareCalculationServiceTest.turnoffFullCartCoupon.after.csv"
    )
    void test_turnoffFullCartCoupon() {
        distributionShareCalculationService.recalculateShare(LocalDate.of(2021, Month.JUNE, 6));
        distributionShareCalculationService.recalculateShare(LocalDate.of(2021, Month.JUNE, 7));
    }

    @Test
    @DisplayName("Не переобрабатывать в recalculateShareForOrders выгруженный в distributionShare заказ")
    @DbUnitDataSet(
            before = "db/DistributionShareCalculationServiceTest.calculateByOrderIds.dontRecalculate.before.csv",
            after = "db/DistributionShareCalculationServiceTest.calculateByOrderIds.dontRecalculate.after.csv"
    )
    void test_recalculateByOrderIds_dontRecalculate(){
        distributionShareCalculationService.recalculateShareForOrders(List.of(1L, 2L), DATETIME_2021_09_15);
    }

    @Test
    @DisplayName("recalculateShareForOrders обрабатывает возвраты")
    @DbUnitDataSet(
            before = "db/DistributionShareCalculationServiceTest.calculateByOrderIds.calculateWithReturns.before.csv",
            after = "db/DistributionShareCalculationServiceTest.calculateByOrderIds.calculateWithReturns.after.csv"
    )
    void test_recalculateByOrderIds_calculateWithReturns(){
        distributionShareCalculationService.recalculateShareForOrders(List.of(1L, 2L), DATETIME_2021_09_15);
    }
}
