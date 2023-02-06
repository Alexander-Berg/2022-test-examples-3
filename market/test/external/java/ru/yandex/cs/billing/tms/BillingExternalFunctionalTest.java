package ru.yandex.cs.billing.tms;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.cs.billing.CsBillingCoreConstants;
import ru.yandex.cs.billing.billing.BillingService;
import ru.yandex.cs.billing.tms.multi.MultiServiceExecutor;
import ru.yandex.cs.billing.util.TimeUtil;
import ru.yandex.market.common.test.db.DbUnitDataSet;

public class BillingExternalFunctionalTest extends AbstractCsBillingTmsExternalFunctionalTest {

    private final BillingExecutor billingExecutor;
    private final BillingService billingService;
    private final FinanceCutoffExecutor financeCutoffExecutor;
    private final MultiServiceExecutor updateActualBalanceExecutor;
    private final MultiServiceExecutor sendSpendingsToBalanceExecutor;
    private final NamedParameterJdbcTemplate csBillingNamedParameterJdbcTemplate;
    private final Clock clock;

    @SuppressWarnings("checkstyle:ParameterNumber")

    @Autowired
    public BillingExternalFunctionalTest(BillingExecutor billingExecutor,
                                         BillingService billingService,
                                         FinanceCutoffExecutor financeCutoffExecutor,
                                         MultiServiceExecutor updateActualBalanceExecutor,
                                         MultiServiceExecutor sendSpendingsToBalanceExecutor,
                                         NamedParameterJdbcTemplate csBillingNamedParameterJdbcTemplate,
                                         Clock clock) {
        this.billingExecutor = billingExecutor;
        this.billingService = billingService;
        this.financeCutoffExecutor = financeCutoffExecutor;
        this.updateActualBalanceExecutor = updateActualBalanceExecutor;
        this.sendSpendingsToBalanceExecutor = sendSpendingsToBalanceExecutor;
        this.csBillingNamedParameterJdbcTemplate = csBillingNamedParameterJdbcTemplate;
        this.clock = clock;
    }

    @Disabled
    @DisplayName("1. Рекомендованные магазины. Простое списание по ежемесячному тарифу")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedCampaignMonthlyBilledPeriodLastDay/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedCampaignMonthlyBilledPeriodLastDay/after.csv",
            dataSource = "csBillingDataSource"
    )
    void testRecommendedCampaignMonthlyBilledPeriodLastDay() {
        final LocalDateTime billingHappensAt = LocalDateTime.of(2020, Month.MAY, 1, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(billingHappensAt));

        billingExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
        updateActualBalanceExecutor.doJob(mockContext());
    }

    @Disabled
    @DisplayName("Рекомендованные магазины. Простое списание по ежемесячному тарифу с ручной корректировкой")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedCampaignMonthlyBilledPeriodLastDayWithManualCorrection/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedCampaignMonthlyBilledPeriodLastDayWithManualCorrection/after.csv",
            dataSource = "csBillingDataSource"
    )
    void testRecommendedCampaignMonthlyBilledPeriodLastDayWithManualCorrection() {
        final LocalDateTime billingHappensAt = LocalDateTime.of(2020, Month.MAY, 1, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(billingHappensAt));

        billingExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
        updateActualBalanceExecutor.doJob(mockContext());
    }

    @Disabled
    @DisplayName("2. Рекомендованные магазины. Простое списание по ежедневному тарифу")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedCampaignDailyBilledPeriodLastDay/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedCampaignDailyBilledPeriodLastDay/after.csv",
            dataSource = "csBillingDataSource"
    )
    void testRecommendedCampaignDailyBilledPeriodLastDay() {
        final LocalDateTime billingHappensAt = LocalDateTime.of(2020, Month.APRIL, 30, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(billingHappensAt));

        billingExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
        updateActualBalanceExecutor.doJob(mockContext());
    }

    @Disabled
    @DisplayName("3. Рекомендованные магазины. Неактивная кампания (нет списаний)")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testBillingNotActiveRecommendedCampaign/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testBillingNotActiveRecommendedCampaign/after.csv",
            dataSource = "csBillingDataSource"
    )
    void testBillingNotActiveRecommendedCampaign() {
        final LocalDateTime billingHappensAt = LocalDateTime.of(2020, Month.MAY, 1, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(billingHappensAt));

        billingExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
        updateActualBalanceExecutor.doJob(mockContext());
    }

    @Disabled
    @DisplayName("4. Рекомендованные магазины. Месячный тариф. Платных кликов меньше, чем сумма тарифа. В конце месяца добивание до тарифа")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedCampaignMonthlyBilledPaidClicksChargesLessThenTariffWithExtraSum/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedCampaignMonthlyBilledPaidClicksChargesLessThenTariffWithExtraSum/after.csv",
            dataSource = "csBillingDataSource"
    )
    void testRecommendedCampaignMonthlyBilledPaidClicksChargesLessThenTariffWithExtraSum() {
        final LocalDateTime billingHappensAt = LocalDateTime.of(2020, Month.MAY, 1, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(billingHappensAt));

        billingExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
        updateActualBalanceExecutor.doJob(mockContext());
    }

    @Disabled
    @DisplayName("5. Рекомендованные магазины. Месячный тариф. Платных кликов больше, чем сумма тарифа. В конце месяца нет добивания до тарифа")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedCampaignMonthlyBilledChargesGreaterThenTariffNoExtraSum/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedCampaignMonthlyBilledChargesGreaterThenTariffNoExtraSum/after.csv",
            dataSource = "csBillingDataSource"
    )
    void testRecommendedCampaignMonthlyBilledChargesGreaterThenTariffNoExtraSum() {
        final LocalDateTime billingHappensAt = LocalDateTime.of(2020, Month.MAY, 1, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(billingHappensAt));

        billingExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
        updateActualBalanceExecutor.doJob(mockContext());
    }

    @Disabled
    @DisplayName("6. Рекомендованные магазины. Месячный тариф. Кампания неактивна на конец периода, но была активной в этом периоде (списания должны быть)")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedCampaignMonthlyBilledWasActiveExtraSum/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedCampaignMonthlyBilledWasActiveExtraSum/after.csv",
            dataSource = "csBillingDataSource"
    )
    void testRecommendedCampaignMonthlyBilledWasActiveExtraSum() {
        final LocalDateTime billingHappensAt = LocalDateTime.of(2020, Month.MAY, 1, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(billingHappensAt));

        billingExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
        updateActualBalanceExecutor.doJob(mockContext());
    }

    @Disabled
    @DisplayName("7. Рекомендованные магазины. Ежедневный тариф. Кампания неактивна на конец периода, но была активной в этом периоде (списания должны быть)")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedCampaignDailyBilledWasActiveExtraSum/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedCampaignDailyBilledWasActiveExtraSum/after.csv",
            dataSource = "csBillingDataSource"
    )
    void testRecommendedCampaignDailyBilledWasActiveExtraSum() {
        final LocalDateTime billingHappensAt = LocalDateTime.of(2020, Month.APRIL, 30, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(billingHappensAt));

        billingExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
        updateActualBalanceExecutor.doJob(mockContext());
    }

    @Disabled
    @DisplayName("8. Рекомендованные магазины. Ежемесячная тарификация. Смена тарифа. на следующий не хватает => появляется финансовый катофф")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedCampaignMonthlyBilledChangeTariffNotEnoughBalanceFinanceCutoff/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedCampaignMonthlyBilledChangeTariffNotEnoughBalanceFinanceCutoff/after.csv",
            dataSource = "csBillingDataSource"
    )
    void testRecommendedCampaignMonthlyBilledChangeTariffNotEnoughBalanceFinanceCutoff() {
        final LocalDateTime billingHappensAt = LocalDateTime.of(2020, Month.MAY, 1, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(billingHappensAt));

        billingExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
        updateActualBalanceExecutor.doJob(mockContext());
    }

    @Disabled
    @DisplayName("9. Рекомендованные магазины. Ежедневная тарификация. Смена тарифа. на следующий не хватает => появляется финансовый катофф")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedCampaignDailyBilledChangeTariffNotEnoughBalanceFinanceCutoff/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedCampaignDailyBilledChangeTariffNotEnoughBalanceFinanceCutoff/after.csv",
            dataSource = "csBillingDataSource"
    )
    void testRecommendedCampaignDailyBilledChangeTariffNotEnoughBalanceFinanceCutoff() {
        final LocalDateTime billingHappensAt = LocalDateTime.of(2020, Month.MAY, 1, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(billingHappensAt));

        billingExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
        updateActualBalanceExecutor.doJob(mockContext());
    }

    @Disabled
    @DisplayName("19. Рекомендованные магазины. Ежемесячная тарификация. Денег на тариф не хватает, появление финансового катоффа")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedCampaignMonthlyBilledNoChangesNotEnoughBalanceFinanceCutoff/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedCampaignMonthlyBilledNoChangesNotEnoughBalanceFinanceCutoff/after.csv",
            dataSource = "csBillingDataSource"
    )
    void testRecommendedCampaignMonthlyBilledNoChangesNotEnoughBalanceFinanceCutoff() {
        final LocalDateTime billingHappensAt = LocalDateTime.of(2020, Month.JUNE, 1, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(billingHappensAt));

        billingExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
        updateActualBalanceExecutor.doJob(mockContext());
    }

    @Disabled
    @DisplayName("20. Рекомендованные магазины. Ежемесячная тарификация. Денег на тариф хватает, отсутствие финансового катоффа")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedCampaignMonthlyBilledNoChangesEnoughBalance/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedCampaignMonthlyBilledNoChangesEnoughBalance/after.csv",
            dataSource = "csBillingDataSource"
    )
    void testRecommendedCampaignMonthlyBilledNoChangesEnoughBalance() {
        final LocalDateTime billingHappensAt = LocalDateTime.of(2020, Month.JUNE, 1, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(billingHappensAt));

        billingExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
        updateActualBalanceExecutor.doJob(mockContext());
    }

    @Disabled
    @DisplayName("21. Рекомендованные магазины. Ежемесячная тарификация. Смена тарифа на больший. Денег на тариф хватает, отсутствие финансового катоффа")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedCampaignMonthlyBilledChangeToLargerTariffEnoughBalance/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedCampaignMonthlyBilledChangeToLargerTariffEnoughBalance/after.csv",
            dataSource = "csBillingDataSource"
    )
    void testRecommendedCampaignMonthlyBilledChangeToLargerTariffEnoughBalance() {
        final LocalDateTime billingHappensAt = LocalDateTime.of(2020, Month.JUNE, 1, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(billingHappensAt));

        billingExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
        updateActualBalanceExecutor.doJob(mockContext());
    }

    @Disabled
    @DisplayName("22. Рекомендованные магазины. Ежемесячная тарификация. Смена тарифа на меньший. Денег на тариф не хватает, появление финансового катоффа")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedCampaignMonthlyBilledChangeToLesserTariffNotEnoughBalanceFinanceCutoff/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedCampaignMonthlyBilledChangeToLesserTariffNotEnoughBalanceFinanceCutoff/after.csv",
            dataSource = "csBillingDataSource"
    )
    void testRecommendedCampaignMonthlyBilledChangeToLesserTariffNotEnoughBalanceFinanceCutoff() {
        final LocalDateTime billingHappensAt = LocalDateTime.of(2020, Month.JUNE, 1, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(billingHappensAt));

        billingExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
        updateActualBalanceExecutor.doJob(mockContext());
    }

    @Disabled
    @DisplayName("23. Рекомендованные магазины. Ежемесячная тарификация. Смена тарифа на меньший. Денег на тариф хватает, отсутствие финансового катоффа")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedCampaignMonthlyBilledChangeToLesserTariffEnoughBalance/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedCampaignMonthlyBilledChangeToLesserTariffEnoughBalance/after.csv",
            dataSource = "csBillingDataSource"
    )
    void testRecommendedCampaignMonthlyBilledChangeToLesserTariffEnoughBalance() {
        final LocalDateTime billingHappensAt = LocalDateTime.of(2020, Month.JUNE, 1, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(billingHappensAt));

        billingExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
        updateActualBalanceExecutor.doJob(mockContext());
    }

    @Disabled
    @DisplayName("24. Рекомендованные магазины. Ежемесячная тарификация. Компенсация перерасхода")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedCampaignMonthlyBilledCompensateOverchange/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedCampaignMonthlyBilledCompensateOverchange/after.csv",
            dataSource = "csBillingDataSource"
    )
    void testRecommendedCampaignMonthlyBilledCompensateOverchange() {
        final LocalDateTime billingHappensAt = LocalDateTime.of(2020, Month.MAY, 31, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(billingHappensAt));

        billingExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
        updateActualBalanceExecutor.doJob(mockContext());
    }

    @Disabled
    @DisplayName("25. Рекомендованные магазины. Снятие финансового катоффа, если денег на тариф хватает")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedCampaignMonthlyBilledCloseFinanceCutoff/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedCampaignMonthlyBilledCloseFinanceCutoff/after.csv",
            dataSource = "csBillingDataSource"
    )
    void testRecommendedCampaignMonthlyBilledCloseFinanceCutoff() {
        LocalDateTime billingHappensAt = LocalDateTime.of(2020, Month.MAY, 25, 9, 18, 17);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(billingHappensAt));

        financeCutoffExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
    }

    @Disabled
    @DisplayName("26. Рекомендованные магазины. Отсутствие снятия финансового катоффа, если денег на тариф не хватает")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedCampaignMonthlyBilledDontCloseFinanceCutoff/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedCampaignMonthlyBilledDontCloseFinanceCutoff/after.csv",
            dataSource = "csBillingDataSource"
    )
    void testRecommendedCampaignMonthlyBilledDontCloseFinanceCutoff() {
        LocalDateTime billingHappensAt = LocalDateTime.of(2020, Month.MAY, 25, 9, 18, 17);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(billingHappensAt));

        financeCutoffExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
    }

    @Disabled
    @DisplayName("27. Рекомендованные магазины. Отправка открутки в биллинг")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedCampaignMonthlyBilledSendSpendingToBalance/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedCampaignMonthlyBilledSendSpendingToBalance/after.csv",
            dataSource = "csBillingDataSource"
    )
    void testRecommendedCampaignMonthlyBilledSendSpendingToBalance() {
        final LocalDateTime billingHappensAt = LocalDateTime.of(2020, Month.MAY, 26, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(billingHappensAt));

        sendSpendingsToBalanceExecutor.doJob(mockContext());
    }

    @Disabled
    @DisplayName("28. Рекомендованные магазины. Приход клика, изменение актуального баланса")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedCampaignMonthlyBilledUpdateActualBalance/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedCampaignMonthlyBilledUpdateActualBalance/after.csv",
            dataSource = "csBillingDataSource"
    )
    void testRecommendedCampaignMonthlyBilledUpdateActualBalance() {
        updateActualBalanceExecutor.doJob(mockContext());
    }

    @Disabled
    @DisplayName("29. Ставки на модель. Активная кампания, после прихода кликов")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testModelbidsCampaignActiveChargeClicks/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testModelbidsCampaignActiveChargeClicks/after.csv",
            dataSource = "csBillingDataSource"
    )
    void testModelbidsCampaignActiveChargeClicks() {
        final LocalDateTime billingHappensAt = LocalDateTime.of(2020, Month.MAY, 26, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(billingHappensAt));

        billingExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
        updateActualBalanceExecutor.doJob(mockContext());
    }

    @DisplayName("30. Ставки на модель. Выключение кампании")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testModebidsCampaignFinanceCutoff/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testModebidsCampaignFinanceCutoff/after.csv",
            dataSource = "csBillingDataSource"
    )
    void testModebidsCampaignFinanceCutoff() {
        final LocalDateTime billingHappensAt = LocalDateTime.of(2020, Month.MAY, 26, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(billingHappensAt));

        billingExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
        updateActualBalanceExecutor.doJob(mockContext());
        financeCutoffExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
    }

    @DisplayName("31. Ставки на модель. Компенсация перерасхода")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testModebidsCampaignCompensateOvercharge/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testModebidsCampaignCompensateOvercharge/after.csv",
            dataSource = "csBillingDataSource"
    )
    void testModebidsCampaignCompensateOvercharge() {
        final LocalDateTime billingHappensAt = LocalDateTime.of(2020, Month.JUNE, 1, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(billingHappensAt));

        billingExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
        updateActualBalanceExecutor.doJob(mockContext());
    }

    @DisplayName("Ставки на модель. Компенсация перерасхода по лимиту")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testModebidsCampaignCompensateOverchargeWithLimit/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testModebidsCampaignCompensateOverchargeWithLimit/after.csv",
            dataSource = "csBillingDataSource"
    )
    void testModebidsCampaignCompensateOverchargeWithLimit() {
        final LocalDateTime billingHappensAt = LocalDateTime.of(2020, Month.JUNE, 1, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(billingHappensAt));

        billingExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
        updateActualBalanceExecutor.doJob(mockContext());
    }

    @DisplayName("Ставки на модель. Компенсация перерасхода, отрицательный баланс")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testModebidsCampaignCompensateOverchargeNegativeBalance/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testModebidsCampaignCompensateOverchargeNegativeBalance/after.csv",
            dataSource = "csBillingDataSource"
    )
    void testModebidsCampaignCompensateOverchargeNegativeBalance() {
        final LocalDateTime billingHappensAt = LocalDateTime.of(2020, Month.JUNE, 1, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(billingHappensAt));

        billingExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
        updateActualBalanceExecutor.doJob(mockContext());
    }

    @DisplayName("32. Бренд-зона. Списание по тарифу 60")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testBrandzoneCharge60Tariff/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testBrandzoneCharge60Tariff/after.csv",
            dataSource = "csBillingDataSource"
    )
    void testBrandzoneCharge60Tariff() {
        final LocalDateTime billingHappensAt = LocalDateTime.of(2020, Month.JUNE, 1, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(billingHappensAt));

        billingExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
        updateActualBalanceExecutor.doJob(mockContext());
    }

    @DisplayName("33. Бренд-зона. Списание по тарифу 150")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testBrandzoneCharge150Tariff/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testBrandzoneCharge150Tariff/after.csv",
            dataSource = "csBillingDataSource"
    )
    void testBrandzoneCharge150Tariff() {
        final LocalDateTime billingHappensAt = LocalDateTime.of(2020, Month.JUNE, 1, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(billingHappensAt));

        billingExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
        updateActualBalanceExecutor.doJob(mockContext());
    }

    @DisplayName("34. Бренд-зона. Смена тарифа на больший. Средств хватает, катофф не выставляется")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testBrandzoneChangeToLargerTariffEnoughBalance/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testBrandzoneChangeToLargerTariffEnoughBalance/after.csv",
            dataSource = "csBillingDataSource"
    )
    void testBrandzoneChangeToLargerTariffEnoughBalance() {
        final LocalDateTime billingHappensAt = LocalDateTime.of(2020, Month.JUNE, 1, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(billingHappensAt));

        billingExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
        updateActualBalanceExecutor.doJob(mockContext());
    }

    @DisplayName("35. Бренд-зона. Смена тарифа на больший. Средств не хватает, катофф выставляется")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testBrandzoneChangeToLargerTariffNotEnoughBalanceFinanceCutoff/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testBrandzoneChangeToLargerTariffNotEnoughBalanceFinanceCutoff/after.csv",
            dataSource = "csBillingDataSource"
    )
    void testBrandzoneChangeToLargerTariffNotEnoughBalanceFinanceCutoff() {
        final LocalDateTime billingHappensAt = LocalDateTime.of(2020, Month.JUNE, 1, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(billingHappensAt));

        billingExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
        updateActualBalanceExecutor.doJob(mockContext());
    }

    @DisplayName("36. Бренд-зона. Смена тарифа на меньший. Средств хватает, катофф не выставляется")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testBrandzoneChangeToLesserTariffEnoughBalance/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testBrandzoneChangeToLesserTariffEnoughBalance/after.csv",
            dataSource = "csBillingDataSource"
    )
    void testBrandzoneChangeToLesserTariffEnoughBalance() {
        final LocalDateTime billingHappensAt = LocalDateTime.of(2020, Month.JUNE, 1, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(billingHappensAt));

        billingExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
        updateActualBalanceExecutor.doJob(mockContext());
    }

    @DisplayName("37. Бренд-зона. Смена тарифа на меньший. Средств не хватает, катофф выставляется")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testBrandzoneChangeToLesserTariffNotEnoughBalanceFinanceCutoff/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testBrandzoneChangeToLesserTariffNotEnoughBalanceFinanceCutoff/after.csv",
            dataSource = "csBillingDataSource"
    )
    void testBrandzoneChangeToLesserTariffNotEnoughBalanceFinanceCutoff() {
        final LocalDateTime billingHappensAt = LocalDateTime.of(2020, Month.JUNE, 1, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(billingHappensAt));

        billingExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
        updateActualBalanceExecutor.doJob(mockContext());
    }

    @DisplayName("38. Бренд-зона. Неактивная кампания, списаний средств нет")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testBrandzoneNotActiveNoCharges/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testBrandzoneNotActiveNoCharges/after.csv",
            dataSource = "csBillingDataSource"
    )
    void testBrandzoneNotActiveNoCharges() {
        final LocalDateTime billingHappensAt = LocalDateTime.of(2020, Month.JUNE, 1, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(billingHappensAt));

        billingExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
        updateActualBalanceExecutor.doJob(mockContext());
    }

    @DisplayName("39. Аналитика. Неактивная кампания, списаний средств нет")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/analyticsCampaignMonthlyBilledWasNotActive/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/analyticsCampaignMonthlyBilledWasNotActive/after.csv",
            dataSource = "csBillingDataSource"
    )
    void analyticsCampaignMonthlyBilledWasNotActive() {
        final LocalDateTime billingHappensAt = LocalDateTime.of(2020, Month.JUNE, 1, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(billingHappensAt));

        billingExecutor.doJob(CsBillingCoreConstants.ANALYTICS_SERVICE_ID, null);
        updateActualBalanceExecutor.doJob(mockContext());
    }

    @DisplayName("40. Аналитика. Активная кампания. Списания по тарифу")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/analyticsCampaignMonthlyBilledNotEnoughNextPeriodFinanceCutoff/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/analyticsCampaignMonthlyBilledNotEnoughNextPeriodFinanceCutoff/after.csv",
            dataSource = "csBillingDataSource"
    )
    void analyticsCampaignMonthlyBilledNotEnoughNextPeriodFinanceCutoff() {
        final LocalDateTime billingHappensAt = LocalDateTime.of(2020, Month.JUNE, 1, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(billingHappensAt));

        billingExecutor.doJob(CsBillingCoreConstants.ANALYTICS_SERVICE_ID, null);
        updateActualBalanceExecutor.doJob(mockContext());
    }

    @DisplayName("41. Аналитика. Смена тарифа на больший, денег хватает, катофф не выставляется")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/analyticsCampaignMonthlyBilledChangeToLargerTariffEnoughNextPeriod/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/analyticsCampaignMonthlyBilledChangeToLargerTariffEnoughNextPeriod/after.csv",
            dataSource = "csBillingDataSource"
    )
    void analyticsCampaignMonthlyBilledChangeToLargerTariffEnoughNextPeriod() {
        final LocalDateTime billingHappensAt = LocalDateTime.of(2020, Month.JUNE, 1, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(billingHappensAt));

        billingExecutor.doJob(CsBillingCoreConstants.ANALYTICS_SERVICE_ID, null);
        updateActualBalanceExecutor.doJob(mockContext());
    }

    @DisplayName("42. Аналитика. Смена тарифа на больший, денег не хватает, катофф выставляется")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/analyticsCampaignMonthlyBilledChangeToLargerTariffNotEnoughNextPeriodFinanceCutoff/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/analyticsCampaignMonthlyBilledChangeToLargerTariffNotEnoughNextPeriodFinanceCutoff/after.csv",
            dataSource = "csBillingDataSource"
    )
    void analyticsCampaignMonthlyBilledChangeToLargerTariffNotEnoughNextPeriodFinanceCutoff() {
        final LocalDateTime billingHappensAt = LocalDateTime.of(2020, Month.JUNE, 1, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(billingHappensAt));

        billingExecutor.doJob(CsBillingCoreConstants.ANALYTICS_SERVICE_ID, null);
        updateActualBalanceExecutor.doJob(mockContext());
    }

    @DisplayName("43. Аналитика. Смена тарифа на меньший. Денег хватает, катофф не выставляется")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/analyticsCampaignMonthlyBilledChangeToLesserTariffEnoughNextPeriod/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/analyticsCampaignMonthlyBilledChangeToLesserTariffEnoughNextPeriod/after.csv",
            dataSource = "csBillingDataSource"
    )
    void analyticsCampaignMonthlyBilledChangeToLesserTariffEnoughNextPeriod() {
        final LocalDateTime billingHappensAt = LocalDateTime.of(2020, Month.JUNE, 1, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(billingHappensAt));

        billingExecutor.doJob(CsBillingCoreConstants.ANALYTICS_SERVICE_ID, null);
        updateActualBalanceExecutor.doJob(mockContext());
    }

    @DisplayName("44. Аналитика. Смена тарифа на меньший. Денег не хватает, катофф выставляется")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/analyticsCampaignMonthlyBilledChangeToLesserTariffNotEnoughNextPeriodFinanceCutoff/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/analyticsCampaignMonthlyBilledChangeToLesserTariffNotEnoughNextPeriodFinanceCutoff/after.csv",
            dataSource = "csBillingDataSource"
    )
    void analyticsCampaignMonthlyBilledChangeToLesserTariffNotEnoughNextPeriodFinanceCutoff() {
        final LocalDateTime billingHappensAt = LocalDateTime.of(2020, Month.JUNE, 1, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(billingHappensAt));

        billingExecutor.doJob(CsBillingCoreConstants.ANALYTICS_SERVICE_ID, null);
        updateActualBalanceExecutor.doJob(mockContext());
    }

    @Disabled
    @DisplayName("47. Рекомендованные. Ежедневное. Списание по активной кампании")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedDailyBilledIsActive/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedDailyBilledIsActive/after.csv",
            dataSource = "csBillingDataSource"
    )
    void testRecommendedDailyBilledIsActive() {
        LocalDateTime billingHappensAt = LocalDateTime.of(2020, Month.MAY, 26, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(billingHappensAt));

        billingExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
        updateActualBalanceExecutor.doJob(mockContext());
    }

    @Disabled
    @DisplayName("48. Рекомендованные. Ежедневное. Списание по активной кампании, наложение финансового катоффа")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedDailyBilledIsActiveFinanceCutoff/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedDailyBilledIsActiveFinanceCutoff/after.csv",
            dataSource = "csBillingDataSource"
    )
    void testRecommendedDailyBilledIsActiveFinanceCutoff() {
        LocalDateTime billingHappensAt = LocalDateTime.of(2020, Month.MAY, 26, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(billingHappensAt));

        billingExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
        updateActualBalanceExecutor.doJob(mockContext());
    }

    @Disabled
    @DisplayName("49. Рекомендованные. Ежедневное. Списаний больше, чем сумма тарифа. Нет добивания")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedDailyBilledIsActiveNoExtraCharge/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedDailyBilledIsActiveNoExtraCharge/after.csv",
            dataSource = "csBillingDataSource"
    )
    void testRecommendedDailyBilledIsActiveNoExtraCharge() {
        LocalDateTime billingHappensAt = LocalDateTime.of(2020, Month.MAY, 26, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(billingHappensAt));

        billingExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
        updateActualBalanceExecutor.doJob(mockContext());
    }

    @Disabled
    @DisplayName("50. Рекомендованные. Ежедневное. Нет списаний с неактивной")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedDailyBilledWasNotActive/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedDailyBilledWasNotActive/after.csv",
            dataSource = "csBillingDataSource"
    )
    void testRecommendedDailyBilledWasNotActive() {
        LocalDateTime billingHappensAt = LocalDateTime.of(2020, Month.MAY, 26, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(billingHappensAt));

        billingExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
        updateActualBalanceExecutor.doJob(mockContext());
    }

    @Disabled
    @DisplayName("51. Рекомендованные. Ежедневное. Списания в конце дня будут, если кампания была выключена в середине дня")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedDailyWasActive/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedDailyWasActive/after.csv",
            dataSource = "csBillingDataSource"
    )
    void testRecommendedDailyWasActive() {
        LocalDateTime billingHappensAt = LocalDateTime.of(2020, Month.MAY, 26, 0, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(billingHappensAt));

        billingExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
        updateActualBalanceExecutor.doJob(mockContext());
    }

    @Disabled
    @DisplayName("52. Рекомендованные. Ежедневное. Наложение финансового катоффа при достижении лимита по расходам")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedDailyIsActiveOpenChargeLimitFinanceCutoff/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedDailyIsActiveOpenChargeLimitFinanceCutoff/after.csv",
            dataSource = "csBillingDataSource"
    )
    void testRecommendedDailyIsActiveOpenChargeLimitFinanceCutoff() {
        LocalDateTime billingHappensAt = LocalDateTime.of(2020, Month.MAY, 25, 15, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(billingHappensAt));
        financeCutoffExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
    }

    @Disabled
    @DisplayName("53. Рекомендованные. Ежедневное. Снятие финансового катофа по достижению лимита на следующий день после наложения")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedDailyCloseChargeLimitFinanceCutoff/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedDailyCloseChargeLimitFinanceCutoff/after.csv",
            dataSource = "csBillingDataSource"
    )
    void testRecommendedDailyCloseChargeLimitFinanceCutoff() {
        LocalDateTime billingHappensAt = LocalDateTime.of(2020, Month.MAY, 25, 22, 5);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(billingHappensAt));

        financeCutoffExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
    }

    @Disabled
    @DisplayName("Рекомендованные. Ежедневное. Снятие финансового катофа после изменения на больший")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedDailyCloseChargeLimitFinanceCutoffAfterChange/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedDailyCloseChargeLimitFinanceCutoffAfterChange/after.csv",
            dataSource = "csBillingDataSource"
    )
    void testRecommendedDailyCloseChargeLimitFinanceCutoffAfterChange() {
        LocalDateTime billingHappensAt = LocalDateTime.of(2020, Month.MAY, 24, 22, 5);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(billingHappensAt));

        financeCutoffExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
    }

    @Disabled
    @DisplayName("54. Рекомендованные. Ежедневное. Автопрощение при списании больше суммы тарифа и при получении финансового катоффа в этот день")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedDailyCompensateOverchargeFinanceCutoff/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedDailyCompensateOverchargeFinanceCutoff/after.csv",
            dataSource = "csBillingDataSource"
    )
    void testRecommendedDailyCompensateOverchargeFinanceCutoff() {
        LocalDateTime cutoffHappensAt = LocalDateTime.of(2020, Month.MAY, 25, 22, 5);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(cutoffHappensAt));
        financeCutoffExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);


        LocalDateTime billingHappensAt = LocalDateTime.of(2020, Month.MAY, 26, 0, 5);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(billingHappensAt));
        billingExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
        updateActualBalanceExecutor.doJob(mockContext());
    }

    @Disabled
    @DisplayName("55. Рекомендованные. Ежедневное. Автопрощение при списаниях больше суммы лимита в этот день")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedDailyCompensateOvercharge/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedDailyCompensateOvercharge/after.csv",
            dataSource = "csBillingDataSource"
    )
    void testRecommendedDailyCompensateOvercharge() {
        LocalDateTime billingHappensAt = LocalDateTime.of(2020, Month.MAY, 25, 23, 50);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(billingHappensAt));
        financeCutoffExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);

        billingHappensAt = LocalDateTime.of(2020, Month.MAY, 26, 0, 5);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(billingHappensAt));
        billingExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
        updateActualBalanceExecutor.doJob(mockContext());
    }

    @Disabled
    @DisplayName("Рекомендованные. Ежедневное. Создали кампанию и сменили тариф")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedDailyCreatedChangeTariff/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedDailyCreatedChangeTariff/after.csv",
            dataSource = "csBillingDataSource"
    )
    void testRecommendedDailyCreatedChangeTariff() {
        LocalDateTime billingHappensAt = LocalDateTime.of(2020, Month.MAY, 25, 0, 5);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(billingHappensAt));
        financeCutoffExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
        billingExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
        updateActualBalanceExecutor.doJob(mockContext());
    }

    @Disabled
    @DisplayName("Рекомендованные. Ежедневное. Автопрощение при списании больше суммы тарифа и при получении финансового катоффа в этот день")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedDailyChargeLimitCompensateOverchargeFinanceCutoff/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedDailyChargeLimitCompensateOverchargeFinanceCutoff/after.csv",
            dataSource = "csBillingDataSource"
    )
    void testRecommendedDailyChargeLimitCompensateOverchargeFinanceCutoff() {
        LocalDateTime cutoffHappensAt = LocalDateTime.of(2020, Month.MAY, 25, 22, 5);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(cutoffHappensAt));
        financeCutoffExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);

        LocalDateTime billingHappensAt = LocalDateTime.of(2020, Month.MAY, 26, 0, 5);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(billingHappensAt));
        billingExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
        updateActualBalanceExecutor.doJob(mockContext());
    }

    @Disabled
    @DisplayName("Рекомендованные. Ежедневное. Автопрощение при списании больше суммы тарифа")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedDailyChargeLimitCompensateOvercharge/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedDailyChargeLimitCompensateOvercharge/after.csv",
            dataSource = "csBillingDataSource"
    )
    void testRecommendedDailyChargeLimitCompensateOvercharge() {
        LocalDateTime billingHappensAt = LocalDateTime.of(2020, Month.MAY, 26, 0, 5);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(billingHappensAt));
        billingExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
        updateActualBalanceExecutor.doJob(mockContext());
    }

    @Disabled
    @DisplayName("Рекомендованные. Ежедневное. Обновление актуального баланса для изменённого тарифа в текущий день")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedDailyUpdateActualBalanceTariffChange/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedDailyUpdateActualBalanceTariffChange/after.csv",
            dataSource = "csBillingDataSource"
    )
    void testRecommendedDailyUpdateActualBalanceTariffChange() {
        updateActualBalanceExecutor.doJob(mockContext());
    }

    @Disabled
    @DisplayName("Рекомендованные. Ежедневное. Наложение финансового катоффа при достижении лимита по расходам и его снятие")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedDailyIsActiveOpenChargeLimitOpenAndClosedFinanceCutoff/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testRecommendedDailyIsActiveOpenChargeLimitOpenAndClosedFinanceCutoff/after.csv",
            dataSource = "csBillingDataSource"
    )
    void testRecommendedDailyIsActiveOpenChargeLimitOpenAndClosedFinanceCutoff() {
        LocalDateTime billingHappensAt = LocalDateTime.of(2020, Month.JULY, 31, 23, 0);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(billingHappensAt));
        financeCutoffExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);

        csBillingNamedParameterJdbcTemplate.update("" +
                "INSERT INTO CS_BILLING.CLICKS_TRT(CS_ID,CAMPAIGN_ID,TRANTIME,PRICE,PP,COUNT) " +
                "VALUES (:serviceId, :campaignId, :trantime, :price, :pp, :count)",
                new MapSqlParameterSource()
                        .addValue("serviceId", CsBillingCoreConstants.VENDOR_SERVICE_ID)
                        .addValue("campaignId", 4648)
                        .addValue("trantime", TimeUtil.toDate(LocalDate.of(2020, 7, 31)))
                        .addValue("price", 20)
                        .addValue("pp", 36)
                        .addValue("count", 100)
                );
        LocalDateTime billingHappensAt1 = LocalDateTime.of(2020, Month.JULY, 31, 23, 5);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(billingHappensAt1));
        financeCutoffExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);

        csBillingNamedParameterJdbcTemplate.update("" +
                        "INSERT INTO CS_BILLING.CLICKS_TRT(CS_ID,CAMPAIGN_ID,TRANTIME,PRICE,PP,COUNT) " +
                        "VALUES (:serviceId, :campaignId, :trantime, :price, :pp, :count)",
                new MapSqlParameterSource()
                        .addValue("serviceId", CsBillingCoreConstants.VENDOR_SERVICE_ID)
                        .addValue("campaignId", 4648)
                        .addValue("trantime", TimeUtil.toDate(LocalDate.of(2020, 7, 31)))
                        .addValue("price", 20)
                        .addValue("pp", 36)
                        .addValue("count", 100)
        );
        LocalDateTime billingHappensAt2 = LocalDateTime.of(2020, Month.JULY, 31, 23, 10);
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(billingHappensAt2));
        financeCutoffExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, null);
    }

    @DisplayName("Платные отзывы. Закрытая старая кампания и открыта новая в следующем дне, пополнение сегодня")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testCampaignStartsNextDayHasPaymentsToday/before.csv",
            after = "/ru/yandex/cs/billing/tms/BillingExternalFunctionalTest/testCampaignStartsNextDayHasPaymentsToday/after.csv",
            dataSource = "csBillingDataSource"
    )
    void testCampaignStartsNextDayHasPaymentsToday() {
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(LocalDate.now().plusDays(2).atTime(1, 0)));

        billingService.registerPayment(CsBillingCoreConstants.VENDOR_SERVICE_ID, 4649L, BigDecimal.valueOf(100L), BigInteger.ONE);
        JobExecutionContext context = mockContext();
        updateActualBalanceExecutor.doJob(context);
        billingExecutor.doJob(CsBillingCoreConstants.VENDOR_SERVICE_ID, context);
    }

    private JobExecutionContext mockContext() {
        final JobExecutionContext mockContext = Mockito.mock(JobExecutionContext.class);
        final JobDetail mockJobDetail = Mockito.mock(JobDetail.class);
        Mockito.when(mockContext.getJobDetail()).thenReturn(mockJobDetail);
        Mockito.when(mockJobDetail.getKey()).thenReturn(new JobKey("test"));
        return mockContext;
    }
}
