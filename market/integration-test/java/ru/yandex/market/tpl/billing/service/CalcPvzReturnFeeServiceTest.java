package ru.yandex.market.tpl.billing.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.common.util.exception.ExceptionCollector;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.tariffs.client.model.BillingUnitEnum;
import ru.yandex.market.mbi.tariffs.client.model.CommonJsonSchema;
import ru.yandex.market.mbi.tariffs.client.model.ModelType;
import ru.yandex.market.mbi.tariffs.client.model.PvzTariffFlatJsonSchema;
import ru.yandex.market.mbi.tariffs.client.model.ServiceTypeEnum;
import ru.yandex.market.mbi.tariffs.client.model.TariffDTO;
import ru.yandex.market.mbi.tariffs.client.model.TariffFindQuery;
import ru.yandex.market.tpl.billing.AbstractFunctionalTest;
import ru.yandex.market.tpl.billing.model.exception.PickupPointNotFoundException;
import ru.yandex.market.tpl.billing.model.exception.PickupPointTariffNotFoundException;
import ru.yandex.market.tpl.billing.service.tariff.TariffService;
import ru.yandex.market.tpl.billing.service.tariff.TariffsIterator;
import ru.yandex.market.tpl.billing.util.DateTimeUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doAnswer;

public class CalcPvzReturnFeeServiceTest extends AbstractFunctionalTest {

    @Autowired
    CalculatePvzReturnFeeService calculatePvzReturnFeeService;

    @Autowired
    TariffService tariffService;

    @Autowired
    TestableClock clock;

    @Test
    @DbUnitDataSet(
            before = {
                    "/database/service/calcpvzorderfeeservice/before/pickup_point_tariff.csv",
                    "/database/service/newcalcpvzreturnfeeservice/before/one_pvz_return.csv"},
            after = "/database/service/newcalcpvzreturnfeeservice/after/one_pvz_return.csv")
    void calcOnePvzOrderTest() {
        clock.setFixed(Instant.parse("2021-08-01T00:00:00Z"), DateTimeUtil.DEFAULT_ZONE_ID);
        LocalDate localDate = LocalDate.of(2021, 8, 1);
        try (ExceptionCollector exceptionCollector = new ExceptionCollector()) {
            calculatePvzReturnFeeService.calcReturnTransactionsForPeriod(localDate, localDate, null, exceptionCollector);
        }
    }

    @Test
    @DbUnitDataSet(
            before = {
                    "/database/service/calcpvzorderfeeservice/before/pickup_point_tariff.csv",
                    "/database/service/newcalcpvzreturnfeeservice/before/three_pvz_return.csv"},
            after = "/database/service/newcalcpvzreturnfeeservice/after/three_pvz_return.csv")
    void calcThreePvzOrderTest() {
        clock.setFixed(Instant.parse("2021-08-01T00:00:00Z"), DateTimeUtil.DEFAULT_ZONE_ID);
        LocalDate localDate = LocalDate.of(2021, 8, 1);
        try (ExceptionCollector exceptionCollector = new ExceptionCollector()) {
            calculatePvzReturnFeeService.calcReturnTransactionsForPeriod(
                    localDate,
                    LocalDate.of(2021, 8, 5),
                    null,
                    exceptionCollector);
        }
    }

    @Test
    @DbUnitDataSet(
            before = {
                    "/database/service/calcpvzorderfeeservice/before/pickup_point_tariff.csv",
                    "/database/service/newcalcpvzreturnfeeservice/before/ignored_pvz_return.csv"
            },
            after = "/database/service/newcalcpvzreturnfeeservice/after/ignored_pvz_return.csv")
    void calcIgnoredPartnerPvzOrderTest() {
        clock.setFixed(Instant.parse("2021-08-01T00:00:00Z"), DateTimeUtil.DEFAULT_ZONE_ID);
        LocalDate localDate = LocalDate.of(2021, 8, 1);
        try (ExceptionCollector exceptionCollector = new ExceptionCollector()) {
            calculatePvzReturnFeeService.calcReturnTransactionsForPeriod(localDate, localDate, null, exceptionCollector);
        }
    }

    @Test
    @DbUnitDataSet(
            before = {
                    "/database/service/calcpvzorderfeeservice/before/pickup_point_tariff.csv",
                    "/database/service/calcpvzorderfeeservice/before/tariff_service_on.csv",
                    "/database/service/newcalcpvzreturnfeeservice/before/one_pvz_return.csv"},
            after = "/database/service/newcalcpvzreturnfeeservice/after/one_pvz_return.csv")
    void calcOnePvzOrderWithPvzTariffServiceTest() {
        setupDoAnswer();
        clock.setFixed(Instant.parse("2021-08-01T00:00:00Z"), DateTimeUtil.DEFAULT_ZONE_ID);
        LocalDate localDate = LocalDate.of(2021, 8, 1);
        try (ExceptionCollector exceptionCollector = new ExceptionCollector()) {
            calculatePvzReturnFeeService.calcReturnTransactionsForPeriod(localDate, localDate, null, exceptionCollector);
        }
    }

    @Test
    @DbUnitDataSet(
            before = {
                    "/database/service/calcpvzorderfeeservice/before/pickup_point_tariff.csv",
                    "/database/service/calcpvzorderfeeservice/before/tariff_service_on.csv",
                    "/database/service/newcalcpvzreturnfeeservice/before/three_pvz_return.csv"},
            after = "/database/service/newcalcpvzreturnfeeservice/after/three_pvz_return.csv")
        void calcThreePvzOrderWithPvzTariffServiceTest() {
        setupDoAnswer();
        clock.setFixed(Instant.parse("2021-08-01T00:00:00Z"), DateTimeUtil.DEFAULT_ZONE_ID);
        LocalDate localDate = LocalDate.of(2021, 8, 1);
        try (ExceptionCollector exceptionCollector = new ExceptionCollector()) {
            calculatePvzReturnFeeService.calcReturnTransactionsForPeriod(
                    localDate,
                    LocalDate.of(2021, 8, 5),
                    null,
                    exceptionCollector);
        }
    }

    @Test
    @DbUnitDataSet(
            before = {
                    "/database/service/calcpvzorderfeeservice/before/pickup_point_tariff.csv",
                    "/database/service/calcpvzorderfeeservice/before/tariff_service_on.csv",
                    "/database/service/newcalcpvzreturnfeeservice/before/one_pvz_return.csv"})
    void calcWithNoTariffs() {
        clock.setFixed(Instant.parse("2021-08-01T00:00:00Z"), DateTimeUtil.DEFAULT_ZONE_ID);
        LocalDate localDate = LocalDate.of(2021, 8, 1);
        setupDoAnswerNoTariffs();
        ExceptionCollector exceptionCollector = new ExceptionCollector();
        calculatePvzReturnFeeService.calcReturnTransactionsForPeriod(
                localDate,
                LocalDate.of(2021, 8, 5),
                null,
                exceptionCollector);
        PickupPointTariffNotFoundException exc = assertThrows(
                PickupPointTariffNotFoundException.class,
                exceptionCollector::close
        );
        assertEquals("Pickup point tariff not found for order 1. Ignoring order", exc.getMessage());
    }

    @Test
    @DbUnitDataSet(
            before = {
                    "/database/service/calcpvzorderfeeservice/before/pickup_point_tariff.csv",
                    "/database/service/calcpvzorderfeeservice/before/tariff_service_on.csv",
                    "/database/service/newcalcpvzreturnfeeservice/before/ignored_pvz_return.csv"},
            after = "/database/service/newcalcpvzreturnfeeservice/after/ignored_pvz_return.csv")
    void calcIgnoredPartnerPvzOrderWithPvzTariffServiceTest() {
        clock.setFixed(Instant.parse("2021-08-01T00:00:00Z"), DateTimeUtil.DEFAULT_ZONE_ID);
        LocalDate localDate = LocalDate.of(2021, 8, 1);
        setupDoAnswer();
        try (ExceptionCollector exceptionCollector = new ExceptionCollector()) {
            calculatePvzReturnFeeService.calcReturnTransactionsForPeriod(localDate, localDate, null, exceptionCollector);
        }
        }

    @Test
    @DbUnitDataSet(
            before = {
                    "/database/service/calcpvzorderfeeservice/before/pickup_point_tariff.csv",
                    "/database/service/calcpvzorderfeeservice/before/tariff_service_on.csv",
                    "/database/service/newcalcpvzreturnfeeservice/before/three_pvz_return.csv"},
            after = "/database/service/newcalcpvzreturnfeeservice/after/one_pvz_return.csv")
    void calcForPvzWithPvzTariffServiceTest() {
        clock.setFixed(Instant.parse("2021-08-01T00:00:00Z"), DateTimeUtil.DEFAULT_ZONE_ID);
        LocalDate localDate = LocalDate.of(2021, 8, 1);
        setupDoAnswer();
        try (ExceptionCollector exceptionCollector = new ExceptionCollector()) {
        calculatePvzReturnFeeService.calcReturnTransactionsForPeriod(localDate, localDate, 1L, exceptionCollector);
        }
    }

    @Test
    @DbUnitDataSet(before = "/database/service/calcpvzorderfeeservice/before/tariff_service_on.csv")
    void pickupPointNotFoundTest() {
        clock.setFixed(Instant.parse("2021-08-01T00:00:00Z"), DateTimeUtil.DEFAULT_ZONE_ID);
        LocalDate localDate = LocalDate.of(2021, 8, 1);
        setupDoAnswer();
        assertThrows(
                PickupPointNotFoundException.class,
                () -> calculatePvzReturnFeeService.calcReturnTransactionsForPeriod(localDate, localDate, 1L, new ExceptionCollector())
        );
    }

    @Test
    @DbUnitDataSet(
            before = {
                    "/database/service/calcpvzorderfeeservice/before/pickup_point_tariff.csv",
                    "/database/service/newcalcpvzreturnfeeservice/before/three_pvz_return_and_transactions.csv"},
            after = "/database/service/newcalcpvzreturnfeeservice/after/two_pvz_return.csv")
    void deleteReturnsTest() {
        clock.setFixed(Instant.parse("2021-08-01T00:00:00Z"), DateTimeUtil.DEFAULT_ZONE_ID);
        LocalDate localDate = LocalDate.of(2021, 8, 1);
        calculatePvzReturnFeeService.deleteDailyReturnTransactions(localDate);
    }

    private void setupDoAnswerNoTariffs() {
        doAnswer(
                invocation -> new TariffsIterator(((pageNumber, batchSize) -> List.of()))
        ).when(tariffService).findTariffs(ArgumentMatchers.any(TariffFindQuery.class));
    }

        private void setupDoAnswer() {
        doAnswer(
                invocation -> new TariffsIterator(((pageNumber, batchSize) -> {
                    if (pageNumber != 0) {
                        return List.of();
                    }

                    TariffFindQuery findQuery = invocation.getArgument(0);
                    ServiceTypeEnum serviceType = findQuery.getServiceType();

                    long tariffId;
                    BigDecimal tariffAmount;

                    tariffId = 16;
                    tariffAmount = new BigDecimal(15);
                    TariffDTO tariffDTO = new TariffDTO();
                    tariffDTO.setId(tariffId);
                    tariffDTO.setIsActive(true);
                    tariffDTO.setDateFrom(LocalDate.MIN);
                    tariffDTO.setModelType(ModelType.THIRD_PARTY_LOGISTICS_PVZ);
                    tariffDTO.setServiceType(serviceType);
                    tariffDTO.setMeta(List.of(
                            new PvzTariffFlatJsonSchema()
                                    .amount(tariffAmount)
                                    .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                                    .currency("RUB")
                                    .billingUnit(BillingUnitEnum.ORDER)
                    ));
                    return List.of(tariffDTO);
                }))
        ).when(tariffService).findTariffs(ArgumentMatchers.any(TariffFindQuery.class));
    }
}
