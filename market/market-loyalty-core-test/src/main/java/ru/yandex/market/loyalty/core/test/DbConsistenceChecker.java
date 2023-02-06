package ru.yandex.market.loyalty.core.test;

import org.springframework.stereotype.Component;

import ru.yandex.market.loyalty.core.service.SqlMonitorService;
import ru.yandex.market.monitoring.MonitoringStatus;

import static ru.yandex.market.loyalty.core.utils.MonitorHelper.assertMonitor;

@Component
public class DbConsistenceChecker {
    private final SqlMonitorService sqlMonitorService;

    public DbConsistenceChecker(SqlMonitorService sqlMonitorService) {
        this.sqlMonitorService = sqlMonitorService;
    }

    public void checkMonitor() {
        assertMonitor(MonitoringStatus.OK, sqlMonitorService.checkDbState());
    }
}
