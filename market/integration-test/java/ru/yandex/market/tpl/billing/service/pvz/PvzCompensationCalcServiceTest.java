package ru.yandex.market.tpl.billing.service.pvz;


import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.exception.ExceptionCollector;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.tariffs.client.model.ServiceTypeEnum;
import ru.yandex.market.tpl.billing.AbstractFunctionalTest;
import ru.yandex.market.tpl.billing.model.exception.PickupPointTariffNotFoundException;
import ru.yandex.market.tpl.billing.service.tariff.TariffService;
import ru.yandex.market.tpl.billing.utils.TariffsUtil;

public class PvzCompensationCalcServiceTest extends AbstractFunctionalTest {

    private final static String CARD_TARIFF_PATH = "/database/service/pvz/PvzCompensationCalcService/tariffs/card_compensation.json";
    private final static String CASH_TARIFF_PATH = "/database/service/pvz/PvzCompensationCalcService/tariffs/cash_compensation.json";

    @Autowired
    TariffService tariffService;

    @Autowired
    PvzCompensationCalcService pvzCompensationCalcService;

    @BeforeEach
    void setupTariffs() {
        TariffsUtil.mockManyTariffResponses(
                tariffService, Map.of(
                        ServiceTypeEnum.PVZ_CARD_COMPENSATION, CARD_TARIFF_PATH,
                        ServiceTypeEnum.PVZ_CASH_COMPENSATION, CASH_TARIFF_PATH
                )
        );
    }

    @Test
    @DisplayName("Тест корректного расчета по тарифам из тарифницы")
    @DbUnitDataSet(
            before = "/database/service/pvz/PvzCompensationCalcService/before/test_calc_pvz_card_cash_compensation.csv",
            after = "/database/service/pvz/PvzCompensationCalcService/after/test_calc_pvz_card_cash_compensation.csv"
    )
    void testCalcPvzCardCashCompensation() {
        try (ExceptionCollector exceptionCollector = new ExceptionCollector()) {
            pvzCompensationCalcService.calcDailyCompensationTransactions(
                    LocalDate.of(2022, 1, 20),
                    exceptionCollector
            );
        }
    }

    @Test
    @DisplayName("Тест, что при возникновении ошибки расчеты продолжатся")
    @DbUnitDataSet(
            before = "/database/service/pvz/PvzCompensationCalcService/before/failing_order_should_continue.csv",
            after = "/database/service/pvz/PvzCompensationCalcService/after/failing_order_should_continue.csv"
    )
    void failingOrderShouldContinue() {
        ExceptionCollector exceptionCollector = new ExceptionCollector();
        pvzCompensationCalcService.calcPeriodCompensationTransaction(
                LocalDate.of(2021, 8, 1),
                LocalDate.of(2021, 8, 6),
                exceptionCollector
        );

        Exception e = Assertions.assertThrows(PickupPointTariffNotFoundException.class, exceptionCollector::close);
        Assertions.assertEquals("Pickup point tariff not found for order 1. Ignoring order", e.getMessage());
    }

    @Test
    @DisplayName("Тест игнорирования предоплаченных заказов")
    @DbUnitDataSet(
            before = "/database/service/pvz/PvzCompensationCalcService/before/test_ignore_prepaid_orders.csv",
            after = "/database/service/pvz/PvzCompensationCalcService/after/test_ignore_prepaid_orders.csv"
    )
    void testIgnorePrepaidOrders() {
        pvzCompensationCalcService.calcPeriodCompensationTransaction(
                LocalDate.of(2021, 8, 1),
                LocalDate.of(2021, 8, 3),
                new ExceptionCollector()
        );
    }
}
