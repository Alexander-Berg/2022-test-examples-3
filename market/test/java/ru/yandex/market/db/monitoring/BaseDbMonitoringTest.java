package ru.yandex.market.db.monitoring;

import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.db.monitoring.config.DbMonitoringConfig;
import ru.yandex.market.db.monitoring.config.DbMonitoringPgInitializer;
import ru.yandex.market.db.monitoring.config.DbMonitoringTestConfig;

@RunWith(SpringRunner.class)
@ContextConfiguration(
    initializers = DbMonitoringPgInitializer.class,
    classes = {DbMonitoringConfig.class, DbMonitoringTestConfig.class}
)
@Transactional
public abstract class BaseDbMonitoringTest {
}
