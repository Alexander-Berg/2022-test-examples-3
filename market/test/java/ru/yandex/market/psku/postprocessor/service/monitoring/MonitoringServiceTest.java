package ru.yandex.market.psku.postprocessor.service.monitoring;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.config.BazingaTestConfig;
import ru.yandex.market.psku.postprocessor.config.MonitoringConfig;

@ContextConfiguration(classes = {
    BazingaTestConfig.class,
    MonitoringConfig.class
})
public class MonitoringServiceTest extends BaseDBTest {

    @Autowired
    MonitoringService monitoringService;

    @Test
    public void eachMonitoringTypeShouldMatchExactlyOneMonitorableBean() {
        //проверяем, что поднялся спринг-контекст
        Assertions.assertThat(monitoringService).isNotNull();
    }
}