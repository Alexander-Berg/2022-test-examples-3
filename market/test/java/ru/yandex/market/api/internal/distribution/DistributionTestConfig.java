package ru.yandex.market.api.internal.distribution;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DistributionTestConfig {

    @Bean
    public DistributionReportClient distributionClientMock() {
        return DistributionConfig.distributionClientMock();
    }
}
