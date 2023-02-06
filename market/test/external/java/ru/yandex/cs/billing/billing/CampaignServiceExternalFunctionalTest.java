package ru.yandex.cs.billing.billing;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.cs.billing.AbstractCsBillingCoreExternalFunctionalTest;
import ru.yandex.cs.billing.CsBillingCoreConstants;
import ru.yandex.cs.billing.api.enums.CampaignBalanceDaysLeftForecastSource;
import ru.yandex.cs.billing.billing.dto.BillingInfoDto;
import ru.yandex.cs.billing.campaign.CampaignService;
import ru.yandex.cs.billing.campaign.dao.CampaignTariffDao;
import ru.yandex.cs.billing.campaign.model.CampaignBalanceDaysLeftForecast;
import ru.yandex.cs.billing.campaign.model.CampaignChargeLimit;
import ru.yandex.cs.billing.err.InconsistentDataException;
import ru.yandex.cs.billing.util.PeriodType;
import ru.yandex.cs.billing.util.TimeUtil;
import ru.yandex.market.common.test.db.DbUnitDataSet;

public class CampaignServiceExternalFunctionalTest extends AbstractCsBillingCoreExternalFunctionalTest {

    private final Clock clock;
    private final CampaignService campaignService;
    private final CampaignTariffDao campaignTariffDao;
    private final RecommendedAvgSpendingCalculator recommendedAvgSpendingCalculator;
    private final NamedParameterJdbcTemplate csBillingNamedParameterJdbcTemplate;

    @Autowired
    public CampaignServiceExternalFunctionalTest(Clock clock,
                                                 CampaignService campaignService,
                                                 RecommendedAvgSpendingCalculator recommendedAvgSpendingCalculator,
                                                 NamedParameterJdbcTemplate csBillingNamedParameterJdbcTemplate,
                                                 CampaignTariffDao campaignTariffDao) {
        this.clock = clock;
        this.campaignService = campaignService;
        this.recommendedAvgSpendingCalculator = recommendedAvgSpendingCalculator;
        this.csBillingNamedParameterJdbcTemplate = csBillingNamedParameterJdbcTemplate;
        this.campaignTariffDao = campaignTariffDao;
    }

    @DisplayName("Получаем лимит на расход для кампании в 23.03.2020 10:23")
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/billing/CampaignServiceExternalFunctionalTest/testGetCampaignChargeLimit/before.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testGetCampaignChargeLimit() {
        final LocalDateTime now = LocalDateTime.of(2020, Month.MARCH, 23, 10, 23);

        final CampaignChargeLimit expected = new CampaignChargeLimit(CsBillingCoreConstants.VENDOR_SERVICE_ID, 1L);
        expected.setStartFrom(LocalDateTime.of(2020, Month.MARCH, 14, 20, 12));
        expected.setAmount(Money.valueOf(100L));

        Assertions.assertEquals(
                expected,
                campaignService.getCampaignChargeLimitAtPoi(CsBillingCoreConstants.VENDOR_SERVICE_ID, 1L, now).get()
        );
    }

    @DisplayName("Получаем, лимит не задан")
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/billing/CampaignServiceExternalFunctionalTest/testGetCampaignChargeLimitNotExists/before.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testGetCampaignChargeLimitNotExists() {
        final LocalDateTime now = LocalDateTime.of(2020, Month.MARCH, 23, 10, 23);

        Assertions.assertNull(
                campaignService.getCampaignChargeLimitAtPoi(CsBillingCoreConstants.VENDOR_SERVICE_ID, 1L, now)
                        .orElse(null)
        );
    }

    @DisplayName("Создаем лимит на расход для кампании")
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/billing/CampaignServiceExternalFunctionalTest/testCreateCampaignChargeLimit/before.csv",
            after = "/ru/yandex/cs/billing/billing/CampaignServiceExternalFunctionalTest/testCreateCampaignChargeLimit/after.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testCreateCampaignChargeLimit() {
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(LocalDateTime.of(2020, Month.MARCH, 23, 10, 23)));
        campaignService.updateCampaignChargeLimit(CsBillingCoreConstants.VENDOR_SERVICE_ID, 1L, PeriodType.DAY, Money.valueOf(166666L));
    }

    @DisplayName("Изменяем лимит на расход для кампании, меньше чем минимальное значение")
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/billing/CampaignServiceExternalFunctionalTest/testCreateCampaignChargeLimitLessThanFlexibleMinLimitCampaignNotActive/before.csv",
            after = "/ru/yandex/cs/billing/billing/CampaignServiceExternalFunctionalTest/testCreateCampaignChargeLimitLessThanFlexibleMinLimitCampaignNotActive/after.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testCreateCampaignChargeLimitLessThanFlexibleMinLimitCampaignNotActive() {
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(LocalDateTime.of(2020, Month.MARCH, 23, 10, 23)));
        campaignService.updateCampaignChargeLimit(CsBillingCoreConstants.VENDOR_SERVICE_ID, 1L, PeriodType.DAY, Money.valueOf(10L));
    }

    @DisplayName("Создаем первый лимит на расход для кампании, кампания не активна, меньше чем минимальное значение")
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/billing/CampaignServiceExternalFunctionalTest/testCreateCampaignChargeLimitLessThanFlexibleMinLimit/before.csv",
            after = "/ru/yandex/cs/billing/billing/CampaignServiceExternalFunctionalTest/testCreateCampaignChargeLimitLessThanFlexibleMinLimit/after.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testCreateCampaignChargeLimitLessThanFlexibleMinLimit() {
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(LocalDateTime.of(2020, Month.MARCH, 23, 10, 23)));
        campaignService.updateCampaignChargeLimit(CsBillingCoreConstants.VENDOR_SERVICE_ID, 1L, PeriodType.DAY, Money.valueOf(10L));
    }

    @DisplayName("Создаём лимит расходов, равный минимальной сумме тарифа")
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/billing/CampaignServiceExternalFunctionalTest/testCreateCampaignChargeLimitEqualsToFlexibleTariffSum/before.csv",
            after = "/ru/yandex/cs/billing/billing/CampaignServiceExternalFunctionalTest/testCreateCampaignChargeLimitEqualsToFlexibleTariffSum/after.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testCreateCampaignChargeLimitEqualsToFlexibleTariffSum() {
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(LocalDateTime.of(2020, Month.MARCH, 23, 10, 23)));
        campaignService.updateCampaignChargeLimit(CsBillingCoreConstants.VENDOR_SERVICE_ID, 1L, PeriodType.DAY, Money.valueOf(96768L));
    }

    @DisplayName("Закрываем текущий лимит на расход для кампании")
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/billing/CampaignServiceExternalFunctionalTest/testCloseCurrentCampaignChargeLimit/before.csv",
            after = "/ru/yandex/cs/billing/billing/CampaignServiceExternalFunctionalTest/testCloseCurrentCampaignChargeLimit/after.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testCloseCurrentCampaignChargeLimit() {
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(LocalDateTime.of(2020, Month.MARCH, 23, 10, 23)));
        campaignService.deleteCampaignLimit(CsBillingCoreConstants.VENDOR_SERVICE_ID, 1L, PeriodType.DAY);
    }

    @DisplayName("Закрываем текущий лимит на расход для кампании, не существует")
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/billing/CampaignServiceExternalFunctionalTest/testCloseCurrentCampaignChargeLimitNoLimitExists/before.csv",
            after = "/ru/yandex/cs/billing/billing/CampaignServiceExternalFunctionalTest/testCloseCurrentCampaignChargeLimitNoLimitExists/after.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testCloseCurrentCampaignChargeLimitNoLimitExists() {
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(LocalDateTime.of(2020, Month.MARCH, 23, 10, 23)));
        campaignService.deleteCampaignLimit(CsBillingCoreConstants.VENDOR_SERVICE_ID, 1L, PeriodType.DAY);
    }

    @DisplayName("Закрываем текущий лимит на расход для кампании, уже закрыт")
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/billing/CampaignServiceExternalFunctionalTest/testCloseCurrentCampaignChargeLimitAlreadyClosed/before.csv",
            after = "/ru/yandex/cs/billing/billing/CampaignServiceExternalFunctionalTest/testCloseCurrentCampaignChargeLimitAlreadyClosed/after.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testCloseCurrentCampaignChargeLimitAlreadyClosed() {
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(LocalDateTime.of(2020, Month.MARCH, 14, 23, 23)));
        campaignService.deleteCampaignLimit(CsBillingCoreConstants.VENDOR_SERVICE_ID, 1L, PeriodType.DAY);
    }

    @DisplayName("Вычисляем кол-во оставшихся дней для текущего баланса, с лимитом, есть расходы в текущем периоде")
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/billing/CampaignServiceExternalFunctionalTest/testCalculateBalanceDaysLeftMonthlyBilledWithChargeLimitHasCharges/before.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testCalculateBalanceDaysLeftMonthlyBilledWithChargeLimitHasCharges() {
        LocalDateTime now = LocalDateTime.of(2020, Month.MARCH, 16, 10, 23);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(now));
        BillingInfoDto billingInfo = BillingInfoDto.create(CsBillingCoreConstants.VENDOR_SERVICE_ID, 1, 1);
        updateAvgSpending(billingInfo);
        var forecast = campaignService.calculateCampaignBalanceDaysLeft(
                billingInfo.getServiceId(),
                billingInfo.getCampaignId(),
                PeriodType.MONTH,
                now
        );

        Assertions.assertEquals(
                4,
                forecast.map(CampaignBalanceDaysLeftForecast::getDaysLeft).orElse(null)
        );
        Assertions.assertEquals(
                CampaignBalanceDaysLeftForecastSource.CHARGE_LIMIT,
                forecast.map(CampaignBalanceDaysLeftForecast::getCalculationBase).orElse(null)
        );
    }

    @DisplayName("Вычисляем кол-во оставшихся дней для текущего баланса, с лимитом, есть расходы в текущем периоде и компенсация")
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/billing/CampaignServiceExternalFunctionalTest/testCalculateBalanceDaysLeftMonthlyBilledWithChargeLimitHasChargesAndCompensation/before.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testCalculateBalanceDaysLeftMonthlyBilledWithChargeLimitHasChargesAndCompensation() {
        LocalDateTime now = LocalDateTime.of(2020, Month.MARCH, 16, 10, 23);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(now));
        BillingInfoDto billingInfo = BillingInfoDto.create(CsBillingCoreConstants.VENDOR_SERVICE_ID, 1, 1);
        updateAvgSpending(billingInfo);
        var forecast = campaignService.calculateCampaignBalanceDaysLeft(
                billingInfo.getServiceId(),
                billingInfo.getCampaignId(),
                PeriodType.MONTH,
                now
        );

        Assertions.assertEquals(
                16,
                forecast.map(CampaignBalanceDaysLeftForecast::getDaysLeft).orElse(null)
        );
        Assertions.assertEquals(
                CampaignBalanceDaysLeftForecastSource.CHARGE_LIMIT,
                forecast.map(CampaignBalanceDaysLeftForecast::getCalculationBase).orElse(null)
        );
    }

    @DisplayName("Вычисляем кол-во оставшихся дней для текущего баланса, с лимитом, есть расходы в текущем периоде, " +
            "есть добивание до тарифа в предыдущем периоде, смена тарифа на тот же в следующем периоде, " +
            "добивание в тарифа в текущем периоде и в следующем")
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/billing/CampaignServiceExternalFunctionalTest/testCalculateBalanceDaysLeftMonthlyBilledWithChargeLimitHasChargesExtraSumPreviousPeriod/before.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testCalculateBalanceDaysLeftMonthlyBilledWithChargeLimitHasChargesExtraSumPreviousPeriod() {
        LocalDateTime now = LocalDateTime.of(2020, Month.MARCH, 16, 10, 23);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(now));
        BillingInfoDto billingInfo = BillingInfoDto.create(CsBillingCoreConstants.VENDOR_SERVICE_ID, 1, 1);
        updateAvgSpending(billingInfo);
        var forecast = campaignService.calculateCampaignBalanceDaysLeft(
                billingInfo.getServiceId(),
                billingInfo.getCampaignId(),
                PeriodType.MONTH,
                now
        );

        Assertions.assertEquals(
                46,
                forecast.map(CampaignBalanceDaysLeftForecast::getDaysLeft).orElse(null)
        );
        Assertions.assertEquals(
                CampaignBalanceDaysLeftForecastSource.BALANCE,
                forecast.map(CampaignBalanceDaysLeftForecast::getCalculationBase).orElse(null)
        );
    }

    @DisplayName("Вычисляем кол-во оставшихся дней для текущего баланса, с лимитом, нет расходов в текущем периоде")
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/billing/CampaignServiceExternalFunctionalTest/testCalculateBalanceDaysLeftMonthlyBilledWithChargeLimitHasNoCharges/before.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testCalculateBalanceDaysLeftMonthlyBilledWithChargeLimitHasNoCharges() {
        final LocalDateTime now = LocalDateTime.of(2020, Month.MARCH, 16, 10, 23);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(now));
        BillingInfoDto billingInfo = BillingInfoDto.create(CsBillingCoreConstants.VENDOR_SERVICE_ID, 1, 1);
        updateAvgSpending(billingInfo);
        var forecast = campaignService.calculateCampaignBalanceDaysLeft(
                billingInfo.getServiceId(),
                billingInfo.getCampaignId(),
                PeriodType.MONTH,
                now
        );

        Assertions.assertTrue(forecast.isEmpty());
    }

    @DisplayName("Вычисляем кол-во оставшихся дней для текущего баланса, без лимита, есть расходы в текущем периоде")
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/billing/CampaignServiceExternalFunctionalTest/testCalculateBalanceDaysLeftMonthlyBilledWithoutChargeLimitHasCharges/before.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testCalculateBalanceDaysLeftMonthlyBilledWithoutChargeLimitHasCharges() {
        final LocalDateTime now = LocalDateTime.of(2020, Month.MARCH, 16, 10, 23);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(now));
        BillingInfoDto billingInfo = BillingInfoDto.create(CsBillingCoreConstants.VENDOR_SERVICE_ID, 1, 1);
        updateAvgSpending(billingInfo);
        var forecast = campaignService.calculateCampaignBalanceDaysLeft(
                billingInfo.getServiceId(),
                billingInfo.getCampaignId(),
                PeriodType.MONTH,
                now
        );

        Assertions.assertEquals(
                6,
                forecast.map(CampaignBalanceDaysLeftForecast::getDaysLeft).orElse(null)
        );
        Assertions.assertEquals(
                CampaignBalanceDaysLeftForecastSource.BALANCE,
                forecast.map(CampaignBalanceDaysLeftForecast::getCalculationBase).orElse(null)
        );
    }

    @DisplayName("Вычисляем кол-во оставшихся дней для текущего баланса, без лимита, есть расходы в текущем периоде")
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/billing/CampaignServiceExternalFunctionalTest/testCalculateBalanceDaysLeftDailyBilledHasCharges/before.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testCalculateBalanceDaysLeftDailyBilledHasCharges() {
        LocalDateTime now = LocalDateTime.of(2020, Month.MARCH, 16, 10, 23);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(now));
        BillingInfoDto billingInfo = BillingInfoDto.create(CsBillingCoreConstants.VENDOR_SERVICE_ID, 1, 1);
        updateAvgSpending(billingInfo);
        var forecast = campaignService.calculateCampaignBalanceDaysLeft(
                billingInfo.getServiceId(),
                billingInfo.getCampaignId(),
                PeriodType.DAY.DAY,
                now
        );

        Assertions.assertEquals(
                7,
                forecast.map(CampaignBalanceDaysLeftForecast::getDaysLeft).orElse(null)
        );
        Assertions.assertEquals(
                CampaignBalanceDaysLeftForecastSource.BALANCE,
                forecast.map(CampaignBalanceDaysLeftForecast::getCalculationBase).orElse(null)
        );
    }

    @DisplayName("Вычисляем кол-во оставшихся дней для текущего баланса, без лимита, нет расходов в текущем периоде")
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/billing/CampaignServiceExternalFunctionalTest/testCalculateBalanceDaysLeftMonthlyBilledWithoutChargeLimitHasNoCharges/before.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testCalculateBalanceDaysLeftMonthlyBilledWithoutChargeLimitHasNoCharges() {
        final LocalDateTime now = LocalDateTime.of(2020, Month.MARCH, 16, 10, 23);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(now));
        BillingInfoDto billingInfo = BillingInfoDto.create(CsBillingCoreConstants.VENDOR_SERVICE_ID, 1, 1);
        updateAvgSpending(billingInfo);
        var forecast = campaignService.calculateCampaignBalanceDaysLeft(
                billingInfo.getServiceId(),
                billingInfo.getCampaignId(),
                PeriodType.MONTH,
                now
        );

        Assertions.assertTrue(forecast.isEmpty());
    }

    @DisplayName("Вычисляем стоимость гибкой минимальной суммы тарифа для последнего дня периода и нет активных дней")
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/billing/CampaignServiceExternalFunctionalTest/testCalculateMonthlyBilledFlexibleTariffMinSumLastDayNoActiveDays/before.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testCalculateMonthlyBilledFlexibleTariffMinSumLastDayNoActiveDays() {
        final LocalDateTime now = LocalDateTime.of(2020, Month.MARCH, 31, 10, 23);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(now));
        final Money flexibleTariffMinSum = campaignService.calculateFlexibleTariffMinSum(
                CsBillingCoreConstants.VENDOR_SERVICE_ID,
                1L,
                Collections.emptySet(),
                PeriodType.MONTH,
                now
        );
        Assertions.assertEquals(
                Money.valueOf("5376"),
                flexibleTariffMinSum
        );
    }

    @DisplayName("Изменить стоимость динамического тарифа, закрыв старый")
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/billing/CampaignServiceExternalFunctionalTest/testChangeCampaignDynamicTariffCloseOld/before.csv",
            after = "/ru/yandex/cs/billing/billing/CampaignServiceExternalFunctionalTest/testChangeCampaignDynamicTariffCloseOld/after.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testChangeCampaignDynamicTariffCloseOld() {
        LocalDateTime today = LocalDateTime.of(2020, Month.MARCH, 19, 0, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(today));
        campaignService.changeDynamicTariff(
                CsBillingCoreConstants.ANALYTICS_SERVICE_ID,
                4L,
                today,
                Money.valueOf(100L)
        );
    }


    @DisplayName("Изменить стоимость текущего активного динамического тарифа")
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/billing/CampaignServiceExternalFunctionalTest/testChangeCampaignDynamicTariffCurrentActive/before.csv",
            after = "/ru/yandex/cs/billing/billing/CampaignServiceExternalFunctionalTest/testChangeCampaignDynamicTariffCurrentActive/after.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testChangeCampaignDynamicTariffCurrentActive() {
        LocalDateTime today = LocalDateTime.of(2020, Month.MARCH, 15, 0, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(today));
        campaignService.changeDynamicTariff(
                CsBillingCoreConstants.ANALYTICS_SERVICE_ID,
                4L,
                today,
                Money.valueOf(105L)
        );
    }

    @DisplayName("Попытка изменить тариф для двух сущностей с одинаковым Id тарифа и одинаковыми версиями")
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/billing/CampaignServiceExternalFunctionalTest/testChangeCampaignDynamicTariffCurrentActive/before.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testDoubleApprove() {
        LocalDateTime today = LocalDateTime.of(2020, Month.MARCH, 15, 0, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(today));

        Assertions.assertThrows(InconsistentDataException.class,
                () -> campaignTariffDao.findAtPoi(206, 4, today)
                        .ifPresent(tariff -> {
                            tariff.setDynamicCost(Money.valueOf(100L));
                            campaignTariffDao.updateDynamicTariffAtPoi(tariff, today);
                            tariff.setDynamicCost(Money.valueOf(200L));
                            campaignTariffDao.updateDynamicTariffAtPoi(tariff, today);
                        })
        );
    }

    @DisplayName("Получить расходы за текущий день")
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/billing/CampaignServiceExternalFunctionalTest/testGetDayExpenses/before.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testGetDayExpenses() {
        LocalDateTime today = LocalDateTime.of(2020, Month.FEBRUARY, 14, 0, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(today));
        Optional<Money> dayExpenses = campaignService.getDayExpenses(CsBillingCoreConstants.VENDOR_SERVICE_ID, 1L, 37L);
        Assertions.assertEquals(Money.valueOf(10L), dayExpenses.orElse(null));
    }

    @DisplayName("Создаем первый лимит на расход для кампании, меньше чем минимальное значение расходов за текущий период")
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/billing/CampaignServiceExternalFunctionalTest/testCreateCampaignChargeLimitLessThanCurrentSpending/before.csv",
            after = "/ru/yandex/cs/billing/billing/CampaignServiceExternalFunctionalTest/testCreateCampaignChargeLimitLessThanCurrentSpending/after.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testCreateCampaignChargeLimitLessThanCurrentSpending() {
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(LocalDateTime.of(2020, Month.MARCH, 23, 10, 23)));
        campaignService.updateCampaignChargeLimit(CsBillingCoreConstants.VENDOR_SERVICE_ID, 1L, PeriodType.DAY, Money.valueOf(309900L));
    }

    private void updateAvgSpending(BillingInfoDto billingInfo) {
        Money avgSpending = recommendedAvgSpendingCalculator.calculateAvgSpending(billingInfo);
        csBillingNamedParameterJdbcTemplate.update(
                "UPDATE CS_BILLING.CAMPAIGN_BALANCE " +
                        "SET AVG_SPENDING = :avgSpending " +
                        "WHERE CS_ID = :serviceId AND CAMPAIGN_ID = :campaignId",
                new MapSqlParameterSource()
                        .addValue("serviceId", billingInfo.getServiceId())
                        .addValue("campaignId", billingInfo.getCampaignId())
                        .addValue("avgSpending", avgSpending.toBigDecimal())
        );
    }

}
