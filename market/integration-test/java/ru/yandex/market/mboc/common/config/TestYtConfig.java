package ru.yandex.market.mboc.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
    ScheduledExecutorConfig.class
})
public class TestYtConfig extends YtConfig {
    public TestYtConfig(ScheduledExecutorConfig config) {
        super(config);
    }
}
