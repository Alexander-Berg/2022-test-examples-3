package ru.yandex.market.crm.campaign.test.tms;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author apershukov
 */
@Configuration
public class MockTmsConfig {

    @Bean
    public TestScheduler scheduler() {
        return new TestScheduler();
    }
}
