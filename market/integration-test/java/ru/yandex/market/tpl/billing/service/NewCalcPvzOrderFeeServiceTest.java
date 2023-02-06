package ru.yandex.market.tpl.billing.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.common.util.exception.ExceptionCollector;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.tariffs.client.model.BillingUnitEnum;
import ru.yandex.market.mbi.tariffs.client.model.CommonJsonSchema;
import ru.yandex.market.mbi.tariffs.client.model.ModelType;
import ru.yandex.market.mbi.tariffs.client.model.PvzBrandingTypeEnum;
import ru.yandex.market.mbi.tariffs.client.model.PvzTariffFlatJsonSchema;
import ru.yandex.market.mbi.tariffs.client.model.PvzTariffRewardJsonSchema;
import ru.yandex.market.mbi.tariffs.client.model.ServiceTypeEnum;
import ru.yandex.market.mbi.tariffs.client.model.TariffDTO;
import ru.yandex.market.mbi.tariffs.client.model.TariffFindQuery;
import ru.yandex.market.tpl.billing.AbstractFunctionalTest;
import ru.yandex.market.tpl.billing.model.exception.PickupPointNotFoundException;
import ru.yandex.market.tpl.billing.model.exception.PickupPointTariffNotFoundException;
import ru.yandex.market.tpl.billing.service.pvz.CalcBrandedPvzGmvService;
import ru.yandex.market.tpl.billing.service.pvz.PvzDropoffMovementCalcService;
import ru.yandex.market.tpl.billing.service.tariff.TariffService;
import ru.yandex.market.tpl.billing.service.tariff.TariffsIterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doAnswer;

public class NewCalcPvzOrderFeeServiceTest extends AbstractFunctionalTest {

    @Autowired
    NewCalcPvzOrderFeeService newCalcPvzOrderFeeService;

    @Autowired
    TariffService tariffService;

    @Autowired
    CalcBrandedPvzGmvService calcBrandedPvzGmvService;

    @Autowired
    TestableClock clock;

    @Test
    @DbUnitDataSet(
            before = "/database/service/calcpvzorderfeeservice/before/testRecalcMonth.csv",
            after = "/database/service/calcpvzorderfeeservice/after/testRecalcMonth.csv")
    void testRecalcMonth() {
        newCalcPvzOrderFeeService.recalcMonthRewards(YearMonth.of(2021, Month.SEPTEMBER));
    }

    @Test
    @DbUnitDataSet(
            before = {
                    "/database/service/calcpvzorderfeeservice/before/pickup_point_tariff.csv",
                    "/database/service/calcpvzorderfeeservice/before/pvz_dbs_movement.csv"},
            after = "/database/service/calcpvzorderfeeservice/after/pvz_dbs_movement.csv")
    void calcPvzDbsMovement() {
        ExceptionCollector exceptionCollector = new ExceptionCollector();
        newCalcPvzOrderFeeService.calcDbsMovementTransactions(LocalDate.of(2021, 11, 14), exceptionCollector);
        newCalcPvzOrderFeeService.calcDbsMovementTransactions(LocalDate.of(2021, 11, 15), exceptionCollector);

        PickupPointTariffNotFoundException exception = assertThrows(
                PickupPointTariffNotFoundException.class,
                exceptionCollector::close
        );
        assertEquals("Pickup point tariff not found for order 1. Ignoring order", exception.getMessage());
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/calcpvzorderfeeservice/before/calc_pvz_dbs_movement_with_tariff_service.csv",
            after = "/database/service/calcpvzorderfeeservice/after/calc_pvz_dbs_movement_with_tariff_service.csv")
    void calcPvzDbsMovementWithTariffService() {
        doAnswer(
                invocation -> new TariffsIterator(((pageNumber, batchSize) -> {
                    if (pageNumber != 0) {
                        return List.of();
                    }

                    TariffFindQuery findQuery = invocation.getArgument(0);
                    ServiceTypeEnum serviceType = findQuery.getServiceType();

                    long tariffId;
                    BigDecimal tariffAmount;

                    if (serviceType == ServiceTypeEnum.PVZ_DBS_INCOME) {
                        tariffId = 1L;
                        tariffAmount = new BigDecimal(30);
                    } else {
                        tariffId = 2L;
                        tariffAmount = new BigDecimal(60);
                    }

                    PvzTariffFlatJsonSchema flatJsonSchema = buildFlatJsonSchema(tariffAmount);
                    return List.of(buildTariffDto(tariffId, serviceType, List.of(flatJsonSchema)));
                }))
        ).when(tariffService).findTariffs(ArgumentMatchers.any(TariffFindQuery.class));

        ExceptionCollector exceptionCollector = new ExceptionCollector();

        newCalcPvzOrderFeeService.calcDbsMovementTransactions(LocalDate.of(2022, 2, 10), exceptionCollector);
        newCalcPvzOrderFeeService.calcDbsMovementTransactions(LocalDate.of(2022, 2, 11), exceptionCollector);

        PickupPointNotFoundException pickupPointNotFoundEx = assertThrows(
                PickupPointNotFoundException.class,
                exceptionCollector::close
        );

        assertEquals("Pickup point with id 2 not found for order 3. Ignoring order", pickupPointNotFoundEx.getMessage());
    }

    private TariffDTO buildTariffDto(
            Long tariffId,
            ServiceTypeEnum serviceType,
            List<Object> meta
    ) {
        TariffDTO tariffDTO = new TariffDTO();
        tariffDTO.setId(tariffId);
        tariffDTO.setIsActive(true);
        tariffDTO.setDateFrom(LocalDate.MIN);
        tariffDTO.setModelType(ModelType.THIRD_PARTY_LOGISTICS_PVZ);
        tariffDTO.setServiceType(serviceType);
        tariffDTO.setMeta(meta);

        return tariffDTO;
    }

    private PvzTariffFlatJsonSchema buildFlatJsonSchema(BigDecimal tariffAmount) {
        return (PvzTariffFlatJsonSchema) new PvzTariffFlatJsonSchema()
                .amount(tariffAmount)
                .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                .currency("RUB")
                .billingUnit(BillingUnitEnum.ORDER);
    }
}
