package ru.yandex.market.tpl.billing.service.pvz;

import java.time.LocalDate;
import java.time.Month;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.exception.ExceptionCollector;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.tariffs.client.model.ServiceTypeEnum;
import ru.yandex.market.tpl.billing.AbstractFunctionalTest;
import ru.yandex.market.tpl.billing.service.tariff.TariffService;
import ru.yandex.market.tpl.billing.utils.TariffsUtil;

public class PvzDropoffMovementCalcServiceTest extends AbstractFunctionalTest {

    @Autowired
    PvzDropoffMovementCalcService pvzDropoffCalcService;

    @Autowired
    TariffService tariffService;

    private static final String DROPOFF_TARIFF = "/database/service/pvz/PvzDropoffCalcService/tariffs/dropoff.json";
    private static final String DROPOFF_RETURN = "/database/service/pvz/PvzDropoffCalcService/tariffs/dropoff_return.json";

    @BeforeEach
    void setupTariffs() {
        TariffsUtil.mockManyTariffResponses(tariffService, Map.of(
                ServiceTypeEnum.PVZ_DROPOFF, DROPOFF_TARIFF,
                ServiceTypeEnum.PVZ_DROPOFF_RETURN, DROPOFF_RETURN
        ));
    }

    @Test
    @DisplayName("Тест корректности расчетов дропоффов")
    @DbUnitDataSet(
            before = "/database/service/pvz/PvzDropoffCalcService/before/calc_dropoff_test.csv",
            after = "/database/service/pvz/PvzDropoffCalcService/after/calc_dropoff_test.csv"
    )
    void calcDropOffTest() {
        LocalDate billingDate = LocalDate.of(2022, Month.FEBRUARY, 3);
        try (ExceptionCollector exceptionCollector = new ExceptionCollector()) {
            pvzDropoffCalcService.calcOneDayDropOffTransactions(billingDate, exceptionCollector);
        }
    }

    @Test
    @DisplayName("Тест игнорирования некоторых партнеров при расчетах")
    @DbUnitDataSet(
            before = "/database/service/pvz/PvzDropoffCalcService/before/calc_ignored_dropoffs.csv",
            after = "/database/service/pvz/PvzDropoffCalcService/after/calc_ignored_dropoffs.csv"
    )
    void calcIgnoredDropOffs() {
        try (ExceptionCollector exceptionCollector = new ExceptionCollector()) {
            pvzDropoffCalcService.calcPeriodDropOffTransactions(
                    LocalDate.of(2021, 8, 1),
                    LocalDate.of(2021, 8, 7),
                    exceptionCollector
            );
        }
    }

    @Test
    @DisplayName("Тест автоматического изменения Id пвз и партнера после переоформления")
    @DbUnitDataSet(
            before = "/database/service/pvz/PvzDropoffCalcService/before/test_automapping.csv",
            after = "/database/service/pvz/PvzDropoffCalcService/after/test_automapping.csv"
    )
    void testAutoMapping() {
        try (ExceptionCollector exceptionCollector = new ExceptionCollector()) {
            pvzDropoffCalcService.calcPeriodDropOffTransactions(
                    LocalDate.of(2022, 6, 1),
                    LocalDate.of(2022, 6, 5),
                    exceptionCollector
            );
        }
    }
}
