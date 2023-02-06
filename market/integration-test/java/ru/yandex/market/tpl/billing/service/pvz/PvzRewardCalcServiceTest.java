package ru.yandex.market.tpl.billing.service.pvz;

import java.time.LocalDate;
import java.time.Month;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.common.util.exception.ExceptionCollector;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.tariffs.client.model.ServiceTypeEnum;
import ru.yandex.market.tpl.billing.AbstractFunctionalTest;
import ru.yandex.market.tpl.billing.model.ServiceType;
import ru.yandex.market.tpl.billing.service.NewCalcPvzOrderFeeService;
import ru.yandex.market.tpl.billing.service.pvz.CalcBrandedPvzGmvService;
import ru.yandex.market.tpl.billing.service.pvz.PvzRewardCalcService;
import ru.yandex.market.tpl.billing.service.tariff.TariffService;

import static ru.yandex.market.tpl.billing.utils.TariffsUtil.mockManyTariffResponses;

public class PvzRewardCalcServiceTest extends AbstractFunctionalTest {

    @Autowired
    PvzRewardCalcService pvzRewardCalcService;

    @Autowired
    CalcBrandedPvzGmvService calcBrandedPvzGmvService;

    @Autowired
    NewCalcPvzOrderFeeService newCalcPvzOrderFeeService;

    @Autowired
    TariffService tariffService;

    @Autowired
    TestableClock testableClock;

    private static final Set<String> REWARD_SERVICE_TYPES = Set.of(
            ServiceType.PVZ_REWARD.name(),
            ServiceType.PVZ_REWARD_YADO.name(),
            ServiceType.PVZ_REWARD_DBS.name()
    );

    private static final String REWARD_TARIFF_PATH = "/database/service/pvz/PvzRewardCalcService/tariffs/reward.json";
    private static final String YADO_TARIFF_PATH = "/database/service/pvz/PvzRewardCalcService/tariffs/reward_yado.json";
    private static final String DBS_TARIFF_PATH = "/database/service/pvz/PvzRewardCalcService/tariffs/reward_dbs.json";

    @BeforeEach
    void setupTariffs() {
        mockManyTariffResponses(tariffService, Map.of(
                ServiceTypeEnum.PVZ_REWARD, REWARD_TARIFF_PATH,
                ServiceTypeEnum.PVZ_REWARD_DBS, DBS_TARIFF_PATH,
                ServiceTypeEnum.PVZ_REWARD_YADO, YADO_TARIFF_PATH
        ));
    }

    @Test
    @DisplayName("Тест обиливания услуги PVZ_REWARD")
    @DbUnitDataSet(
            before = "/database/service/pvz/PvzRewardCalcService/before/calc_pvz_reward.csv",
            after = "/database/service/pvz/PvzRewardCalcService/after/calc_pvz_reward.csv"
    )
    void testCalcPvzReward() {
        LocalDate billingDate = LocalDate.of(2022, Month.JANUARY, 20);

        try (ExceptionCollector exceptionCollector = new ExceptionCollector()) {
            pvzRewardCalcService.calcDailyRewardTransactions(billingDate, exceptionCollector);
        }
    }

    @Test
    @DisplayName("Тест биллинга БПВЗ по плоскому тарифу до даты брендирования")
    @DbUnitDataSet(
            before = "/database/service/pvz/PvzRewardCalcService/before/calc_reward_by_flat_tariff_before_branding_date.csv",
            after = "/database/service/pvz/PvzRewardCalcService/after/calc_reward_by_flat_tariff_before_branding_date.csv"
    )
    void testCalculateRewardByFlatTariffBeforeBranding() {
        LocalDate from = LocalDate.of(2022, 3, 5);
        LocalDate to = LocalDate.of(2022, 3, 12);

        try (ExceptionCollector exceptionCollector = new ExceptionCollector()) {
            pvzRewardCalcService.calcPeriodRewardTransactions(from, to, exceptionCollector);
            calcBrandedPvzGmvService.calcGmvRewardAtMonth(from, exceptionCollector);
        }
    }

    @Test
    @DisplayName("Тест обиливания pvz reward, который учитывает, что уже включено (по дате) обиливание по GMV для БПВЗ")
    @DbUnitDataSet(
            before = "/database/service/pvz/PvzRewardCalcService/before/pvz_reward_with_gmv_enabled.csv",
            after = "/database/service/pvz/PvzRewardCalcService/after/pvz_reward_with_gmv_enabled.csv"
    )
    void testCalculatePvzOrdersAfterBillingByGmvInsteadOfBillingByOrders() {
        LocalDate billingDate = LocalDate.of(2022, Month.FEBRUARY, 1);

        try (ExceptionCollector exceptionCollector = new ExceptionCollector()) {
            newCalcPvzOrderFeeService.deletePeriodOrderTransactionByServiceType(
                    billingDate.minusDays(1),
                    billingDate,
                    REWARD_SERVICE_TYPES
            );
            pvzRewardCalcService.calcPeriodRewardTransactions(
                    billingDate.minusDays(1),
                    billingDate,
                    exceptionCollector
            );
        }
    }

    @Test
    @DisplayName("Тест, что при падении расчета заказа продолжится расчет")
    @DbUnitDataSet(
            before = "/database/service/pvz/PvzRewardCalcService/before/failing_order_should_continue.csv",
            after = "/database/service/pvz/PvzRewardCalcService/after/failing_order_should_continue.csv"
    )
    void testFailingOrderShouldContinue() {
        LocalDate billingDate = LocalDate.of(2021, 8, 1);

        pvzRewardCalcService.calcDailyRewardTransactions(billingDate, new ExceptionCollector());
    }

    @Test
    @DisplayName("Тест обиливания услуги PVZ_REWARD_YADO")
    @DbUnitDataSet(
            before = "/database/service/pvz/PvzRewardCalcService/before/yandex_dostavka_orders.csv",
            after = "/database/service/pvz/PvzRewardCalcService/after/yandex_dostavka_orders.csv"
    )
    void testYandexDostavkaOrdersBilling() {
        LocalDate from = LocalDate.of(2021, 8, 1);
        LocalDate to = LocalDate.of(2021, 8, 6);

        pvzRewardCalcService.calcPeriodRewardTransactions(from, to, new ExceptionCollector());
    }

    @Test
    @DisplayName("Тест игнорирования некоторых партнеров при биллинге")
    @DbUnitDataSet(
            before = "/database/service/pvz/PvzRewardCalcService/before/calc_ignored_orders.csv",
            after = "/database/service/pvz/PvzRewardCalcService/after/calc_ignored_orders.csv"
    )
    void calcIgnoredPvzOrder() {
        LocalDate from = LocalDate.of(2021, 8, 1);
        LocalDate to = LocalDate.of(2021, 8, 7);

        pvzRewardCalcService.calcPeriodRewardTransactions(from, to, new ExceptionCollector());
    }

    @Test
    @DisplayName("Тест обиливания услуги PVZ_REWARD_DBS")
    @DbUnitDataSet(
            before = "/database/service/pvz/PvzRewardCalcService/before/pvz_dbs_reward.csv",
            after = "/database/service/pvz/PvzRewardCalcService/after/pvz_dbs_reward.csv"
    )
    void testCalcPvzDbsReward() {
        LocalDate from = LocalDate.of(2021, 11, 14);
        LocalDate to = LocalDate.of(2021, 11, 16);
        ExceptionCollector exceptionCollector = new ExceptionCollector();

        pvzRewardCalcService.calcPeriodRewardTransactions(from, to, exceptionCollector);
    }
}
