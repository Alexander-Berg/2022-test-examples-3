package ru.yandex.cs.billing.monitoring.dao;

import java.time.Clock;
import java.time.LocalDate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.cs.billing.AbstractCsBillingTmsFunctionalTest;
import ru.yandex.cs.billing.CsBillingCoreConstants;
import ru.yandex.cs.billing.util.TimeUtil;
import ru.yandex.market.common.test.db.DbUnitDataSet;

public class MonitoringDaoFunctionalTest extends AbstractCsBillingTmsFunctionalTest {
    @Autowired
    private MonitoringDao monitoringDao;
    @Autowired
    private Clock clock;

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/billing/monitoring/dao/MonitoringDaoFunctionalTest/testCountBilledCampaignsAfterDsCutoff/before.csv",
            dataSource = "csBillingDataSource"
    )
    void testCountBilledCampaignsAfterDsCutoff() {
        LocalDate now = LocalDate.of(2022, 2, 1);
        LocalDate poi = TimeUtil.previousDay(now);
        long count = monitoringDao.countBilledCampaignsAfterDsCutoff(CsBillingCoreConstants.VENDOR_SERVICE_ID, poi);
        Assertions.assertEquals(1, count);
    }
}
