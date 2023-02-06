package ru.yandex.cs.billing.billing;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.cs.billing.AbstractCsBillingCoreExternalFunctionalTest;
import ru.yandex.cs.billing.CsBillingCoreConstants;
import ru.yandex.cs.billing.billing.dto.BillingInfoDto;
import ru.yandex.cs.billing.billing.dto.CampaignChargeDto;
import ru.yandex.market.common.test.db.DbUnitDataSet;

@DbUnitDataSet(
        before = "/ru/yandex/cs/billing/billing/FlexibleCampaignExtraChargeTariffProcessorFunctionalTest/before.csv",
        dataSource = "csBillingDataSource"
)
class FlexibleCampaignExtraChargeTariffProcessorFunctionalTest extends AbstractCsBillingCoreExternalFunctionalTest {
    private static final int TARIFF_SERVICE_TYPE_ID = 37;
    private static final long FIFTY_THOUSAND_TARIFF_ID = 37L;
    private static final LocalDateTime LAST_DAY_OF_MARCH = LocalDate.of(2020, Month.MARCH, 31).atStartOfDay();
    private static final LocalDateTime LAST_DAY_OF_MARCH_TARIFF_POI
            = LocalDateTime.of(2020, Month.MARCH, 31, 23, 59, 59);

    private final FlexibleCampaignExtraChargeTariffProcessor flexibleMinSumTariffProcessor;

    @Autowired
    FlexibleCampaignExtraChargeTariffProcessorFunctionalTest(final FlexibleCampaignExtraChargeTariffProcessor flexibleCampaignExtraChargeTariffProcessor) {
        this.flexibleMinSumTariffProcessor = flexibleCampaignExtraChargeTariffProcessor;
    }

    @DisplayName("Месячный период тарификации, биллим последний день периода, у кампании нет активных дней")
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/billing/FlexibleCampaignExtraChargeTariffProcessorFunctionalTest/testMonthBillingPeriodLastDayHasNoActivePeriods/before.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testMonthBillingPeriodLastDayHasNoActivePeriods() {
        final BillingInfoDto billingInfoDto = BillingInfoDto.create(CsBillingCoreConstants.VENDOR_SERVICE_ID, 1L, 1L)
                .withTariffId(FIFTY_THOUSAND_TARIFF_ID)
                .withBilledAt(LAST_DAY_OF_MARCH)
                .withTariffPoi(LAST_DAY_OF_MARCH_TARIFF_POI)
                .withSumCharges(Money.ZERO)
                .withTariffServiceTypeId(TARIFF_SERVICE_TYPE_ID);
        Assertions.assertEquals(
                Collections.emptyList(),
                flexibleMinSumTariffProcessor.process(billingInfoDto)
        );
    }

    @DisplayName("Месячный период тарификации, биллим последний день периода, у кампании есть активный день: 24 числа с 00:00 по 13:42")
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/billing/FlexibleCampaignExtraChargeTariffProcessorFunctionalTest/testMonthBillingPeriodLastDayHasActivePeriod/before.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testMonthBillingPeriodLastDayHasActivePeriod() {
        final BillingInfoDto billingInfoDto = BillingInfoDto.create(CsBillingCoreConstants.VENDOR_SERVICE_ID, 1L, 1L)
                .withTariffId(FIFTY_THOUSAND_TARIFF_ID)
                .withBilledAt(LAST_DAY_OF_MARCH)
                .withTariffPoi(LAST_DAY_OF_MARCH_TARIFF_POI)
                .withSumCharges(Money.ZERO)
                .withTariffServiceTypeId(TARIFF_SERVICE_TYPE_ID);
        Assertions.assertEquals(
                Collections.singletonList(new CampaignChargeDto(
                                TARIFF_SERVICE_TYPE_ID,
                                Money.valueOf("5376"),
                                1,
                                "Flexible min sum tariff cost for 1 active days and one day cost exact = 5376"
                        )
                ),
                flexibleMinSumTariffProcessor.process(billingInfoDto)
        );
    }

    @DisplayName("Месячный период тарификации, биллим последний день периода, у кампании есть 16 активных дней: " +
            "1 числа с 01:23 до 16 числа 00:00, 24 числа с 01:00 по 13:42, 24 числа с 18:23 до 18:36")
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/billing/FlexibleCampaignExtraChargeTariffProcessorFunctionalTest/testMonthBillingPeriodLastDayHasActivePeriodsInsideMonth/before.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testMonthBillingPeriodLastDayHasActivePeriodsInsideMonth() {
        final BillingInfoDto billingInfoDto = BillingInfoDto.create(CsBillingCoreConstants.VENDOR_SERVICE_ID, 1L, 1L)
                .withTariffId(FIFTY_THOUSAND_TARIFF_ID)
                .withBilledAt(LAST_DAY_OF_MARCH)
                .withTariffPoi(LAST_DAY_OF_MARCH_TARIFF_POI)
                .withSumCharges(Money.ZERO)
                .withTariffServiceTypeId(TARIFF_SERVICE_TYPE_ID);
        Assertions.assertEquals(
                Collections.singletonList(new CampaignChargeDto(
                                TARIFF_SERVICE_TYPE_ID,
                                Money.valueOf("86016"),
                                1,
                                "Flexible min sum tariff cost for 16 active days and one day cost exact = 5376"
                        )
                ),
                flexibleMinSumTariffProcessor.process(billingInfoDto)
        );
    }

    @DisplayName("Месячный период тарификации, биллим последний день периода, кампания была активна весь месяц")
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/billing/FlexibleCampaignExtraChargeTariffProcessorFunctionalTest/testMonthBillingPeriodLastDayWasActiveAllBillingPeriod/before.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testMonthBillingPeriodLastDayWasActiveAllBillingPeriod() {
        final BillingInfoDto billingInfoDto = BillingInfoDto.create(CsBillingCoreConstants.VENDOR_SERVICE_ID, 1L, 1L)
                .withTariffId(FIFTY_THOUSAND_TARIFF_ID)
                .withBilledAt(LAST_DAY_OF_MARCH)
                .withTariffPoi(LAST_DAY_OF_MARCH_TARIFF_POI)
                .withSumCharges(Money.ZERO)
                .withTariffServiceTypeId(TARIFF_SERVICE_TYPE_ID);
        Assertions.assertEquals(
                Collections.singletonList(new CampaignChargeDto(
                                TARIFF_SERVICE_TYPE_ID,
                                Money.valueOf("166656"),
                                1,
                                "Flexible min sum tariff cost for 31 active days and one day cost exact = 5376"
                        )
                ),
                flexibleMinSumTariffProcessor.process(billingInfoDto)
        );
    }

    @DisplayName("Месячный период тарификации, биллим последний день периода, кампания была активна 1 день в периоде, " +
            "начало за пределом текущего периода тарификации")
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/billing/FlexibleCampaignExtraChargeTariffProcessorFunctionalTest/testMonthBillingPeriodLastDayHasOneActiveDayCampaignWasActiveBeforeBillingPeriod/before.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testMonthBillingPeriodLastDayHasOneActiveDayCampaignWasActiveBeforeBillingPeriod() {
        final BillingInfoDto billingInfoDto = BillingInfoDto.create(CsBillingCoreConstants.VENDOR_SERVICE_ID, 1L, 1L)
                .withTariffId(FIFTY_THOUSAND_TARIFF_ID)
                .withBilledAt(LAST_DAY_OF_MARCH)
                .withTariffPoi(LAST_DAY_OF_MARCH_TARIFF_POI)
                .withSumCharges(Money.ZERO)
                .withTariffServiceTypeId(TARIFF_SERVICE_TYPE_ID);
        Assertions.assertEquals(
                Collections.singletonList(new CampaignChargeDto(
                                TARIFF_SERVICE_TYPE_ID,
                                Money.valueOf("5376"),
                                1,
                                "Flexible min sum tariff cost for 1 active days and one day cost exact = 5376"
                        )
                ),
                flexibleMinSumTariffProcessor.process(billingInfoDto)
        );
    }

    @DisplayName("Месячный период тарификации, биллим последний день периода, кампания была активна 2 дня в периоде: " +
            "1 сек для первый и последнего дней, начало в текущем периоде, кампания продолжает быть активной")
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/billing/FlexibleCampaignExtraChargeTariffProcessorFunctionalTest/testMonthBillingPeriodLastDayHasOneActiveLastDayCampaignIsActive/before.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testMonthBillingPeriodLastDayHasActiveFirstAndLastDays() {
        final BillingInfoDto billingInfoDto = BillingInfoDto.create(CsBillingCoreConstants.VENDOR_SERVICE_ID, 1L, 1L)
                .withTariffId(FIFTY_THOUSAND_TARIFF_ID)
                .withBilledAt(LAST_DAY_OF_MARCH)
                .withTariffPoi(LAST_DAY_OF_MARCH_TARIFF_POI)
                .withSumCharges(Money.ZERO)
                .withTariffServiceTypeId(TARIFF_SERVICE_TYPE_ID);
        Assertions.assertEquals(
                Collections.singletonList(new CampaignChargeDto(
                                TARIFF_SERVICE_TYPE_ID,
                                Money.valueOf("10752"),
                                1,
                                "Flexible min sum tariff cost for 2 active days and one day cost exact = 5376"
                        )
                ),
                flexibleMinSumTariffProcessor.process(billingInfoDto)
        );
    }

    @DisplayName("Месячный период тарификации, биллим последний день периода, кампания была активна 4 дня, " +
            "сумма расходов меньше гибкой минимальной сумме тарифа")
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/billing/FlexibleCampaignExtraChargeTariffProcessorFunctionalTest/testMonthBillingPeriodLastDayWasActiveHasChargesLessThanMinSum/before.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testMonthBillingPeriodLastDayWasActiveHasChargesLessThanMinSum() {
        final BillingInfoDto billingInfoDto = BillingInfoDto.create(CsBillingCoreConstants.VENDOR_SERVICE_ID, 1L, 1L)
                .withTariffId(FIFTY_THOUSAND_TARIFF_ID)
                .withBilledAt(LAST_DAY_OF_MARCH)
                .withTariffPoi(LAST_DAY_OF_MARCH_TARIFF_POI)
                .withSumCharges(Money.ZERO)
                .withTariffServiceTypeId(TARIFF_SERVICE_TYPE_ID);
        Assertions.assertEquals(
                Collections.singletonList(new CampaignChargeDto(
                                TARIFF_SERVICE_TYPE_ID,
                                Money.valueOf("21464"),
                                1,
                                "Flexible min sum tariff cost for 4 active days and one day cost exact = 5376"
                        )
                ),
                flexibleMinSumTariffProcessor.process(billingInfoDto)
        );
    }

    @DisplayName("Месячный период тарификации, биллим последний день периода, кампания была активна 4 дня, " +
            "сумма расходов равна гибкой минимальной сумме тарифа")
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/billing/FlexibleCampaignExtraChargeTariffProcessorFunctionalTest/testMonthBillingPeriodLastDayWasActiveHasChargesEqualToMinSum/before.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testMonthBillingPeriodLastDayWasActiveHasChargesEqualToMinSum() {
        final BillingInfoDto billingInfoDto = BillingInfoDto.create(CsBillingCoreConstants.VENDOR_SERVICE_ID, 1L, 1L)
                .withTariffId(FIFTY_THOUSAND_TARIFF_ID)
                .withBilledAt(LAST_DAY_OF_MARCH)
                .withTariffPoi(LAST_DAY_OF_MARCH_TARIFF_POI)
                .withSumCharges(Money.ZERO)
                .withTariffServiceTypeId(TARIFF_SERVICE_TYPE_ID);
        Assertions.assertEquals(
                Collections.emptyList(),
                flexibleMinSumTariffProcessor.process(billingInfoDto)
        );
    }

    @DisplayName("Месячный период тарификации, биллим последний день периода, кампания была активна 4 дня, " +
            "сумма расходов больше гибкой минимальной сумме тарифа")
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/billing/FlexibleCampaignExtraChargeTariffProcessorFunctionalTest/testMonthBillingPeriodLastDayWasActiveHasChargesGreaterThanMinSum/before.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testMonthBillingPeriodLastDayWasActiveHasChargesGreaterThanMinSum() {
        final BillingInfoDto billingInfoDto = BillingInfoDto.create(CsBillingCoreConstants.VENDOR_SERVICE_ID, 1L, 1L)
                .withTariffId(FIFTY_THOUSAND_TARIFF_ID)
                .withBilledAt(LAST_DAY_OF_MARCH)
                .withTariffPoi(LAST_DAY_OF_MARCH_TARIFF_POI)
                .withSumCharges(Money.ZERO)
                .withTariffServiceTypeId(TARIFF_SERVICE_TYPE_ID);
        Assertions.assertEquals(
                Collections.emptyList(),
                flexibleMinSumTariffProcessor.process(billingInfoDto)
        );
    }
}
