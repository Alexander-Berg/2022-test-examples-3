package ru.yandex.market.tpl.billing.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.tariffs.client.model.BillingUnitEnum;
import ru.yandex.market.mbi.tariffs.client.model.CommonJsonSchema;
import ru.yandex.market.mbi.tariffs.client.model.ModelType;
import ru.yandex.market.mbi.tariffs.client.model.PvzTariffBrandedDecorationJsonShema;
import ru.yandex.market.mbi.tariffs.client.model.PvzTariffZoneEnum;
import ru.yandex.market.mbi.tariffs.client.model.ServiceTypeEnum;
import ru.yandex.market.mbi.tariffs.client.model.TariffDTO;
import ru.yandex.market.mbi.tariffs.client.model.TariffFindQuery;
import ru.yandex.market.tpl.billing.AbstractFunctionalTest;
import ru.yandex.market.tpl.billing.service.tariff.TariffService;
import ru.yandex.market.tpl.billing.service.tariff.TariffsIterator;
import ru.yandex.market.tpl.common.util.exception.TplIllegalStateException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doAnswer;

class PvzOneTimeFeeServiceTest extends AbstractFunctionalTest {

    @Autowired
    private TestableClock clock;

    @Autowired
    private PvzOneTimeFeeService pvzOneTimeFeeService;

    @Autowired
    private TariffService tariffService;

    @BeforeEach
    void setup() {
        clock.setFixed(Instant.parse("2021-06-22T01:00:00Z"), ZoneOffset.ofHours(+4));
    }

    @Test
    @DisplayName("В БД нет ПВЗ которым нужно выплатить за брендирование в текущем месяце")
    @DbUnitDataSet(
            before = "/database/service/pvzonetimefee/before/branded_pvz_need_fee_empty.csv",
            after = "/database/service/pvzonetimefee/after/branded_pvz_need_fee_empty.csv")
    void whenBrandedPvzNeedFeeEmpty() {
        pvzOneTimeFeeService.calcBrandedDecorationFee(LocalDate.now(clock));
    }

    @Test
    @DisplayName("В БД есть ПВЗ которым нужно выплатить за брендирование, в export_transaction не пишем")
    @DbUnitDataSet(
            before = "/database/service/pvzonetimefee/before/all_new_branded_pvz_need_fee_and_export_transaction_disabled.csv",
            after = "/database/service/pvzonetimefee/after/all_new_branded_pvz_need_fee_and_export_transaction_disabled.csv")
    void whenAllNewBrandedPvzNeedFeeAndExportTransactionDisabled() {
        pvzOneTimeFeeService.calcBrandedDecorationFee(LocalDate.now(clock));
    }

    @Test
    @DisplayName("В БД есть ПВЗ которым нужно выплатить за брендирование, в export_transaction пишем")
    @DbUnitDataSet(
            before = "/database/service/pvzonetimefee/before/all_new_branded_pvz_need_fee_and_export_transaction_enabled.csv",
            after = "/database/service/pvzonetimefee/after/all_new_branded_pvz_need_fee_and_export_transaction_enabled.csv")
    void whenAllNewBrandedPvzNeedFeeAndExportTransactionEnabled() {
        pvzOneTimeFeeService.calcBrandedDecorationFee(LocalDate.now(clock));
    }

    @Test
    @DisplayName("В БД есть ПВЗ части уже выплатили за брендирование, части нет, в export_transaction не пишем")
    @DbUnitDataSet(
            before = "/database/service/pvzonetimefee/before/part_new_branded_pvz_need_fee_and_export_transaction_disabled.csv",
            after = "/database/service/pvzonetimefee/after/part_new_branded_pvz_need_fee_and_export_transaction_disabled.csv")
    void whenPartNewBrandedPvzNeedFeeAndExportTransactionDisabled() {
        pvzOneTimeFeeService.calcBrandedDecorationFee(LocalDate.now(clock));
    }

    @Test
    @DisplayName("В БД есть ПВЗ части уже выплатили за брендирование, части нет, в export_transaction пишем")
    @DbUnitDataSet(
            before = "/database/service/pvzonetimefee/before/part_new_branded_pvz_need_fee_and_export_transaction_enabled.csv",
            after = "/database/service/pvzonetimefee/after/part_new_branded_pvz_need_fee_and_export_transaction_enabled.csv")
    void whenPartNewBrandedPvzNeedFeeAndExportTransactionEnabled() {
        pvzOneTimeFeeService.calcBrandedDecorationFee(LocalDate.now(clock));
    }

    @Test
    @DisplayName("Для одного из регионов не назначена разовая выплата за брендированность")
    @DbUnitDataSet(before = "/database/service/pvzonetimefee/before/decoration_fee_null.csv")
    void whenDecorationFeeNullThenException() {
        Exception exception = assertThrows(TplIllegalStateException.class, () -> {
            pvzOneTimeFeeService.calcBrandedDecorationFee(LocalDate.now(clock));
        });
        assertNotNull(exception.getMessage());
    }

    @Test
    @DisplayName("У партнера пустой offer_date")
    @DbUnitDataSet(
            before = "/database/service/pvzonetimefee/before/partner_offer_date_null.csv",
            after = "/database/service/pvzonetimefee/after/partner_offer_date_null.csv")
    void whenPartnerOfferDateNullThenDecorationFeeEmpty() {
        pvzOneTimeFeeService.calcBrandedDecorationFee(LocalDate.now(clock));
    }

    @Test
    @DisplayName("Расчет с использованием тарифницы")
    @DbUnitDataSet(
            before = "/database/service/pvzonetimefee/before/calc_with_new_tariff_service.csv",
            after = "/database/service/pvzonetimefee/after/calc_with_new_tariff_service.csv")
    void whenCalcWithTariffService() {
        setupTariffService(
                Map.of(PvzTariffZoneEnum.MOSCOW, 99900L,
                        PvzTariffZoneEnum.MOSCOW_NEAR, 88800L,
                        PvzTariffZoneEnum.SPB, 77700L,
                        PvzTariffZoneEnum.SPB_NEAR, 55500L,
                        PvzTariffZoneEnum.OTHER, 44400L
                ));

        pvzOneTimeFeeService.calcBrandedDecorationFee(LocalDate.of(2022, 3, 10));
    }

    @Test
    @DisplayName("Не нашли подходящий тариф в тарифнице")
    @DbUnitDataSet(before = "/database/service/pvzonetimefee/before/calc_with_new_tariff_service.csv")
    void whenTariffFromTariffServiceNotFoundForPvz() {
        setupTariffService(
                Map.of(PvzTariffZoneEnum.MOSCOW_NEAR, 888L,
                        PvzTariffZoneEnum.SPB, 777L,
                        PvzTariffZoneEnum.OTHER, 444L
                ));

        Exception ex = assertThrows(TplIllegalStateException.class, () -> {
            pvzOneTimeFeeService.calcBrandedDecorationFee(LocalDate.of(2022, 3, 10));
        });

        assertEquals(
                "Для одного из регионов не назначена разовая выплата за брендированность ПВЗ",
                ex.getMessage()
        );
    }

    public void setupTariffService(Map<PvzTariffZoneEnum, Long> metaMap) {
        List<Object> meta = metaMap.entrySet().stream().map(e ->
                new PvzTariffBrandedDecorationJsonShema()
                        .pvzTariffZone(e.getKey())
                        .amount(new BigDecimal(e.getValue()))
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .billingUnit(BillingUnitEnum.ORDER)
        ).collect(Collectors.toList());

        doAnswer(invocation -> new TariffsIterator(((pageNumber, batchSize) -> {
            if (pageNumber != 0) {
                return List.of();
            }

            TariffFindQuery findQuery = invocation.getArgument(0);
            ServiceTypeEnum serviceTypeEnum = findQuery.getServiceType();

            TariffDTO tariffDTO = new TariffDTO();
            tariffDTO.setId(1L);
            tariffDTO.setIsActive(true);
            tariffDTO.setDateFrom(LocalDate.MIN);
            tariffDTO.setModelType(ModelType.THIRD_PARTY_LOGISTICS_PVZ);
            tariffDTO.setServiceType(serviceTypeEnum);
            tariffDTO.setMeta(meta);
            return List.of(tariffDTO);

        }))).when(tariffService).findTariffs(ArgumentMatchers.any(TariffFindQuery.class));
    }

}
