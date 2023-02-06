package ru.yandex.market.tpl.billing.service;

import java.time.LocalDate;
import java.util.Map;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.tpl.billing.AbstractFunctionalTest;
import ru.yandex.market.mbi.tariffs.client.model.ServiceTypeEnum;
import ru.yandex.market.tpl.billing.model.ServiceType;
import ru.yandex.market.tpl.billing.model.exception.CashTariffNotFoundException;
import ru.yandex.market.tpl.billing.model.exception.PickupPointTariffNotFoundException;
import ru.yandex.market.tpl.billing.service.tariff.TariffService;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.tpl.billing.utils.TariffsUtil.mockManyTariffResponses;
import static ru.yandex.market.tpl.billing.utils.TariffsUtil.mockTariffResponse;

public class PvzCalcServiceTest extends AbstractFunctionalTest {

    private static final String REWARD_TARIFF_PATH = "/database/service/pvzcalcservise/tariff/reward_tariff.json";
    private static final String CARD_COMPENSATION = "/database/service/pvzcalcservise/tariff/card_compensation.json";
    private static final String CASH_COMPENSATION = "/database/service/pvzcalcservise/tariff/cash_compensation.json";
    private static final String DROPOFF_TARIFF_PATH = "/database/service/pvzcalcservise/tariff/dropoff_tariff.json";

    @Autowired
    PvzCalcService pvzCalcService;

    @Autowired
    TariffService tariffService;

    @Test
    @DbUnitDataSet(
            before = {
                    "/database/service/calcpvzorderfeeservice/before/pickup_point_tariff.csv",
                    "/database/service/pvzcalcservise/before/three_pvz_return_and_transactions.csv"},
            after = "/database/service/pvzcalcservise/after/two_pvz_return.csv")
    void deleteReturnsTest() {

        LocalDate localDate = LocalDate.of(2021, 8, 1);
        pvzCalcService.calculateDailyReturnTransactions(localDate);
        pvzCalcService.calculatePeriodReturnTransactions(localDate, localDate, null);
    }

    @Test
    @DbUnitDataSet(
            before = {
                    "/database/service/calcpvzorderfeeservice/before/pickup_point_tariff.csv",
                    "/database/service/pvzcalcservise/before/three_pvz_orders_and_dropoff.csv"},
            after = "/database/service/pvzcalcservise/after/three_pvz_orders_and_dropoff.csv")
    void deleteOrderTest() {
        LocalDate localDate = LocalDate.of(2021, 8, 1);
        mockManyTariffResponses(tariffService, Map.of(
                ServiceTypeEnum.PVZ_REWARD, REWARD_TARIFF_PATH,
                ServiceTypeEnum.PVZ_CARD_COMPENSATION, CARD_COMPENSATION,
                ServiceTypeEnum.PVZ_CASH_COMPENSATION, CASH_COMPENSATION));
        pvzCalcService.calculateDailyRewardTransactions(localDate);
        pvzCalcService.calculateDailyCompensationTransactions(localDate);
    }

    @Test
    @DbUnitDataSet(
            before = {
                    "/database/service/calcpvzorderfeeservice/before/pickup_point_tariff.csv",
                    "/database/service/pvzcalcservise/before/failing_pvz_order_should_not_rollback.csv"},
            after = "/database/service/pvzcalcservise/after/failing_pvz_order_should_not_rollback.csv")
    void failingOrderShouldNotRollback() {
        LocalDate localDate = LocalDate.of(2021, 8, 1);
        mockManyTariffResponses(tariffService, Map.of(
                ServiceTypeEnum.PVZ_REWARD, REWARD_TARIFF_PATH,
                ServiceTypeEnum.PVZ_CARD_COMPENSATION, CARD_COMPENSATION,
                ServiceTypeEnum.PVZ_CASH_COMPENSATION, CASH_COMPENSATION));
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> pvzCalcService.calculateDailyRewardTransactions(localDate)
        );
        assertTrue(exception instanceof PickupPointTariffNotFoundException);

        exception = assertThrows(
                RuntimeException.class,
                () -> pvzCalcService.calculateDailyCompensationTransactions(localDate)
        );
        assertTrue(exception instanceof PickupPointTariffNotFoundException);
    }

    @Test
    @DbUnitDataSet(
            before = {
                    "/database/service/calcpvzorderfeeservice/before/pickup_point_tariff.csv",
                    "/database/service/pvzcalcservise/before/dropoff.csv"},
            after = "/database/service/pvzcalcservise/after/dropoff.csv")
    void deleteDropOffTest() {
        mockTariffResponse(tariffService, DROPOFF_TARIFF_PATH);
        LocalDate localDate = LocalDate.of(2021, 8, 1);
        pvzCalcService.calculateDailyDropOffTransactions(localDate);
        pvzCalcService.calculatePeriodDropOffTransactions(localDate, localDate);
    }
}
