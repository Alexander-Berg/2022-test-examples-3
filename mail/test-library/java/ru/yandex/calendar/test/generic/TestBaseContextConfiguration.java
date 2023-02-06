package ru.yandex.calendar.test.generic;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.calendar.boot.CalendarContextConfiguration;
import ru.yandex.calendar.logic.resource.ResourceTestManager;
import ru.yandex.calendar.test.auto.db.util.TestLayerCollLastUpdateChecker;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestStatusChecker;

@Configuration
@Import({
        CalendarTestInitContextConfiguration.class,
        CalendarTestJdbcContextConfiguration.class,
        CalendarContextConfiguration.class,
        TvmClientMockTestConfiguration.class,
        MicroCoreTestConfiguration.class
})
public class TestBaseContextConfiguration {
    @Bean
    public ResourceTestManager resourceTestManager() {
        return new ResourceTestManager();
    }

    @Bean
    public TestManager testManager() {
        return new TestManager();
    }

    @Bean
    public TestLayerCollLastUpdateChecker testLayerCollLastUpdateChecker() {
        return new TestLayerCollLastUpdateChecker();
    }

    @Bean
    public TestStatusChecker testStatusChecker() {
        return new TestStatusChecker();
    }
}
