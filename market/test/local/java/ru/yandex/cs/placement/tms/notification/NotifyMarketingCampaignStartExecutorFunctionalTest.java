package ru.yandex.cs.placement.tms.notification;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.Month;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.cs.billing.util.TimeUtil;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendor.AbstractCsPlacementTmsFunctionalTest;

public class NotifyMarketingCampaignStartExecutorFunctionalTest extends AbstractCsPlacementTmsFunctionalTest {

    private final NotifyMarketingCampaignStartExecutor executor;
    private final Clock clock;

    @Autowired
    public NotifyMarketingCampaignStartExecutorFunctionalTest(NotifyMarketingCampaignStartExecutor executor,
                                                              Clock clock) {
        this.executor = executor;
        this.clock = clock;
    }

    @BeforeEach
    public void beforeEach() {
        Mockito.reset(clock);
        Mockito.when(clock.instant()).thenReturn(
                TimeUtil.toInstant(LocalDateTime.of(2020, Month.JULY, 1, 0, 0))
        );
    }

    @DisplayName("Уведомление регистрируется в первый раз, когда еще нет ни одной записи об активном или нет " +
            "уведомлении")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/placement/tms/notification/NotifyMarketingCampaignStartExecutorFunctionalTest/testAddFirstNotification/before.csv",
            after = "/ru/yandex/cs/placement/tms/notification/NotifyMarketingCampaignStartExecutorFunctionalTest/testAddFirstNotification/after.csv",
            dataSource = "vendorDataSource"
    )
    void testAddFirstNotification() {
        executor.executeThrottledJob(null);
    }

    @DisplayName("Два дня подряд есть активные кампании")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/placement/tms/notification/NotifyMarketingCampaignStartExecutorFunctionalTest/testTwoDaysInRowWithActiveCampaign/before.csv",
            after = "/ru/yandex/cs/placement/tms/notification/NotifyMarketingCampaignStartExecutorFunctionalTest/testTwoDaysInRowWithActiveCampaign/after.csv",
            dataSource = "vendorDataSource"
    )
    void testTwoDaysInRowWithActiveCampaign() {
        executor.executeThrottledJob(null);
    }
}
