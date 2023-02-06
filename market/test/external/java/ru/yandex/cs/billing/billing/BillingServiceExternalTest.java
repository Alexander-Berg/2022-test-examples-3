package ru.yandex.cs.billing.billing;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.cs.billing.AbstractCsBillingCoreExternalFunctionalTest;
import ru.yandex.cs.billing.CsBillingCoreConstants;
import ru.yandex.cs.billing.billing.model.GetAvailableTransfer;
import ru.yandex.cs.billing.err.CSBillingError;
import ru.yandex.cs.billing.err.CampaignNotFoundException;
import ru.yandex.cs.billing.err.InconsistentDataException;
import ru.yandex.cs.billing.util.TimeUtil;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;

public class BillingServiceExternalTest extends AbstractCsBillingCoreExternalFunctionalTest {

    private final BillingService billingService;
    private final Clock clock;

    @Autowired
    public BillingServiceExternalTest(final BillingService billingService,
                                      final Clock clock) {
        this.billingService = billingService;
        this.clock = clock;
    }

    @DisplayName("Нет кампании")
    @Test
    void testNoCampaignWasFound() {
        final CampaignNotFoundException exception = Assertions.assertThrows(
                CampaignNotFoundException.class,
                () -> billingService.billCampaign(CsBillingCoreConstants.VENDOR_SERVICE_ID, 1)
        );

        Assertions.assertEquals(exception.getError(), CSBillingError.CAMPAIGN_NOT_FOUND);
        Assertions.assertEquals(exception.getMessage(), "Campaign 132-1 was not found");
    }

    @DisplayName("Нет баланса")
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/billing/BillingServiceExternalTest/testNoCampaignBalanceWasFound/before.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testNoCampaignBalanceWasFound() {
        final InconsistentDataException exception = Assertions.assertThrows(
                InconsistentDataException.class,
                () -> billingService.billCampaign(CsBillingCoreConstants.VENDOR_SERVICE_ID, 1L)
        );

        Assertions.assertEquals(exception.getError(), CSBillingError.INCONSISTENT_DATA);
        Assertions.assertEquals(exception.getMessage(), "Campaign balance 132-1 was not found");
    }

    @DisplayName("Конец кампании раньше начала")
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/billing/BillingServiceExternalTest/testCampaignEndBeforeStart/before.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testCampaignEndBeforeStart() {
        final InconsistentDataException exception = Assertions.assertThrows(
                InconsistentDataException.class,
                () -> billingService.billCampaign(CsBillingCoreConstants.VENDOR_SERVICE_ID, 1L)
        );

        Assertions.assertEquals(exception.getError(), CSBillingError.INCONSISTENT_DATA);
        Assertions.assertEquals(exception.getMessage(), "Invalid campaign dates range for campaign 132-1");
    }

    @DisplayName("День биллинга раньше даты старта кампании")
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/billing/BillingServiceExternalTest/testCampaignLastBilledAtBeforeStart/before.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testCampaignLastBilledAtBeforeStart() {
        final InconsistentDataException exception = Assertions.assertThrows(
                InconsistentDataException.class,
                () -> billingService.billCampaign(CsBillingCoreConstants.VENDOR_SERVICE_ID, 1L)
        );

        Assertions.assertEquals(exception.getError(), CSBillingError.INCONSISTENT_DATA);
        Assertions.assertEquals(exception.getMessage(), "Last billed date is earlier than campaign start date for campaign 132-1");
    }

    @DisplayName("Кампания закрыта")
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/billing/BillingServiceExternalTest/testCampaignIsClosed/before.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testCampaignIsClosed() {
        final InconsistentDataException exception = Assertions.assertThrows(
                InconsistentDataException.class,
                () -> billingService.billCampaign(CsBillingCoreConstants.VENDOR_SERVICE_ID, 1L)
        );

        Assertions.assertEquals(exception.getError(), CSBillingError.INCONSISTENT_DATA);
        Assertions.assertEquals(exception.getMessage(), "Campaign 132-1 is closed");
    }

    @DisplayName("Месячная кампания, с началом в день билинга, с ежедневными списиваниями за клики, не последний день периода тарификации")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/billing/BillingServiceExternalTest/testCampaignMonthTariffDailyChargesNotBillingPeriodLastDayStartsNow/before.csv",
            after = "/ru/yandex/cs/billing/billing/BillingServiceExternalTest/testCampaignMonthTariffDailyChargesNotBillingPeriodLastDayStartsNow/after.csv",
            dataSource = "csBillingDataSource"
    )
    void testCampaignMonthTariffDailyChargesNotBillingPeriodLastDayStartsNow() {
        billingService.billCampaign(CsBillingCoreConstants.VENDOR_SERVICE_ID, 1L);
    }

    @DisplayName("Проверка учета необиленных расходов за день")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/billing/BillingServiceExternalTest/testCalculateNotBilledSpendings/before.csv",
            dataSource = "csBillingDataSource"
    )
    void testCalculateNotBilledSpendings() {
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(LocalDate.of(2020, Month.FEBRUARY, 14)));

        final GetAvailableTransfer getAvailableTransfer =
                billingService.calculateSumAvailableForTransferInCents(CsBillingCoreConstants.VENDOR_SERVICE_ID, 1L);
        Assertions.assertEquals(
                new BigDecimal("74824.00"),
                getAvailableTransfer.getSumInCents()
        );
    }

    @DisplayName("Месячная кампания, с ежедневными списиваниями за клики, последний день периода тарификации, " +
            "0 после биллинга, следующий период с финансовым катоффом")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/billing/BillingServiceExternalTest/testCampaignMonthTariffDailyChargesBillingPeriodLastDay/before.csv",
            after = "/ru/yandex/cs/billing/billing/BillingServiceExternalTest/testCampaignMonthTariffDailyChargesBillingPeriodLastDay/after.csv",
            dataSource = "csBillingDataSource"
    )
    void testCampaignMonthTariffDailyChargesBillingPeriodLastDay() {
        billingService.billCampaign(CsBillingCoreConstants.VENDOR_SERVICE_ID, 1L);
    }

    @DisplayName("Месячная кампания, с ежедневными списиваниями за клики, c гибкой суммой тарифа, последний день периода тарификации")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/billing/BillingServiceExternalTest/testCampaignMonthFlexibleTariffDailyChargesBillingPeriodLastDay/before.csv",
            after = "/ru/yandex/cs/billing/billing/BillingServiceExternalTest/testCampaignMonthFlexibleTariffDailyChargesBillingPeriodLastDay/after.csv",
            dataSource = "csBillingDataSource"
    )
    void testCampaignMonthFlexibleTariffDailyChargesBillingPeriodLastDay() {
        billingService.billCampaign(CsBillingCoreConstants.VENDOR_SERVICE_ID, 1L);
    }

    @DisplayName("Проверить количество доступных средств для перевода с услуги на услугу. Кампания была активна 4 дня, " +
            "резервируем на 12 дней в будущем")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/billing/BillingServiceExternalTest/testCalculateAvailableTransferSumFlexibleTariffMinSum/before.csv",
            dataSource = "csBillingDataSource"
    )
    void testCalculateAvailableTransferSumFlexibleTariffMinSum() {
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(LocalDate.of(2020, Month.FEBRUARY, 18)));

        final GetAvailableTransfer getAvailableTransfer = billingService.calculateSumAvailableForTransferInCents(CsBillingCoreConstants.VENDOR_SERVICE_ID, 1L);
        Assertions.assertEquals(
                new BigDecimal("74724.00"),
                getAvailableTransfer.getSumInCents()
        );
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/billing/BillingServiceExternalTest/testAvailableSumWithoutActivityPeriods/before.csv",
            dataSource = "csBillingDataSource"
    )
    void testAvailableSumWithoutActivityPeriods() {
        Mockito.when(clock.instant())
                .thenReturn(TimeUtil.toInstant(
                    LocalDateTime.of(2021, Month.NOVEMBER, 10, 19, 22, 0))
                );

        final GetAvailableTransfer getAvailableTransfer = billingService.calculateSumAvailableForTransferInCents(
                CsBillingCoreConstants.VENDOR_SERVICE_ID, 36050L);

        Assertions.assertEquals(
                new BigDecimal("666666.00"),
                getAvailableTransfer.getSumInCents()
        );
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/billing/BillingServiceExternalTest/testAvailableSumWithActivityPeriods/before.csv",
            dataSource = "csBillingDataSource"
    )
    void testAvailableSumWithActivityPeriods() {
        Mockito.when(clock.instant())
                .thenReturn(TimeUtil.toInstant(
                        LocalDateTime.of(2021, Month.NOVEMBER, 10, 19, 22, 0))
                );

        final GetAvailableTransfer getAvailableTransfer = billingService.calculateSumAvailableForTransferInCents(
                CsBillingCoreConstants.VENDOR_SERVICE_ID, 35950L);

        Assertions.assertEquals(
                new BigDecimal("0.00"),
                getAvailableTransfer.getSumInCents()
        );
    }
}
