package ru.yandex.market.psku.postprocessor.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.yandex.market.psku.postprocessor.service.tracker.mock.CategoryTrackerInfoProducerMock;
import ru.yandex.market.psku.postprocessor.service.wait_content.WaitContentProxyDao;

@Configuration
public class TrackerTestConfig {

    @Bean
    WaitContentProxyDao waitContentProxyDao(org.jooq.Configuration jooqConfiguration) {
        return new WaitContentProxyDao(jooqConfiguration, categoryTrackerInfoProducerMock());
    }

    @Bean
    CategoryTrackerInfoProducerMock categoryTrackerInfoProducerMock() {
        return new CategoryTrackerInfoProducerMock();
    }
}
