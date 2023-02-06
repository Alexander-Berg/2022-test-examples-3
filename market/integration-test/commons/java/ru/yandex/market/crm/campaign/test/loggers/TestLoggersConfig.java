package ru.yandex.market.crm.campaign.test.loggers;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.crm.core.test.loggers.CoreTestLoggersConfig;

/**
 * @author apershukov
 */
@Configuration
@Import(CoreTestLoggersConfig.class)
@ComponentScan("ru.yandex.market.crm.campaign.test.loggers")
public class TestLoggersConfig {
}
