package ru.yandex.cs.placement.tms.notification;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.Month;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.cs.billing.util.TimeUtil;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendor.AbstractCsPlacementTmsFunctionalTest;

import static org.mockito.Mockito.when;

public class NotifyMarketingCampaignNotApprovedExecutorFunctionalTest extends AbstractCsPlacementTmsFunctionalTest {
    @Autowired
    private NotifyMarketingCampaignNotApprovedExecutor executor;
    @Autowired
    private Clock clock;

    @BeforeEach
    public void beforeEach() {
        when(clock.instant()).thenReturn(
                TimeUtil.toInstant(LocalDateTime.of(2020, Month.JULY, 1, 0, 0))
        );
    }

    @DisplayName("Уведомление вендору за 7 дней до старта маркетинговой кампании")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/placement/tms/notification/NotifyMarketingCampaignNotApprovedExecutorFunctionalTest/testNotifyVendorIn7DaysBeforeCampaignStarts/before.csv",
            after = "/ru/yandex/cs/placement/tms/notification/NotifyMarketingCampaignNotApprovedExecutorFunctionalTest/testNotifyVendorIn7DaysBeforeCampaignStarts/after.csv",
            dataSource = "vendorDataSource"
    )
    void testNotifyVendorIn7DaysBeforeCampaignStarts() {
        executor.executeThrottledJob(null);
    }

    @DisplayName("Уведомление менеджеру за 7 дней до старта маркетинговой кампании")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/placement/tms/notification/NotifyMarketingCampaignNotApprovedExecutorFunctionalTest/testNotifyManagerIn7DaysBeforeCampaignStarts/before.csv",
            after = "/ru/yandex/cs/placement/tms/notification/NotifyMarketingCampaignNotApprovedExecutorFunctionalTest/testNotifyManagerIn7DaysBeforeCampaignStarts/after.csv",
            dataSource = "vendorDataSource"
    )
    void testNotifyManagerIn7DaysBeforeCampaignStarts() {
        executor.executeThrottledJob(null);
    }

    @DisplayName("Уведомление вендору через 7 дней после создания маркетинговой кампании")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/placement/tms/notification/NotifyMarketingCampaignNotApprovedExecutorFunctionalTest/testNotifyVendorIn7DaysAfterCampaignCreated/before.csv",
            after = "/ru/yandex/cs/placement/tms/notification/NotifyMarketingCampaignNotApprovedExecutorFunctionalTest/testNotifyVendorIn7DaysAfterCampaignCreated/after.csv",
            dataSource = "vendorDataSource"
    )
    void testNotifyVendorIn7DaysAfterCampaignCreated() {
        executor.executeThrottledJob(null);
    }

    @DisplayName("Уведомление менеджеру через 7 дней после создания маркетинговой кампании")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/placement/tms/notification/NotifyMarketingCampaignNotApprovedExecutorFunctionalTest/testNotifyManagerIn7DaysAfterCampaignCreated/before.csv",
            after = "/ru/yandex/cs/placement/tms/notification/NotifyMarketingCampaignNotApprovedExecutorFunctionalTest/testNotifyManagerIn7DaysAfterCampaignCreated/after.csv",
            dataSource = "vendorDataSource"
    )
    void testNotifyManagerIn7DaysAfterCampaignCreated() {
        executor.executeThrottledJob(null);
    }
}
