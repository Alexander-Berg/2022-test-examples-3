package ru.yandex.market.logistics.logistics4shops.jobs;

import java.util.Collection;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.support.GenericApplicationContext;

import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.tms.quartz2.spring.CronTrigger;
import ru.yandex.market.tms.quartz2.spring.MonitoringConfig;

@DisplayName("Тест на мониторинг кварцовых джоб")
public class TmsMonitoringTest extends AbstractIntegrationTest {

    @Autowired
    private GenericApplicationContext context;

    @Test
    @DisplayName("Все кварцовые джобы должны быть аннотированы @MonitoringConfig")
    void testMonitoringConfigAnnotation() {
        ConfigurableListableBeanFactory factory = context.getBeanFactory();
        var executors = factory.getBeansWithAnnotation(CronTrigger.class).keySet();
        var monitoredExecutors = factory.getBeansWithAnnotation(MonitoringConfig.class).keySet();
        Collection<String> nonMonitoredExecutors = CollectionUtils.subtract(executors, monitoredExecutors);
        softly.assertThat(nonMonitoredExecutors).isEmpty();
    }
}
