package ru.yandex.market.crm.core.test.utils;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.crm.core.services.control.GlobalControlConfig;

/**
 * @author apershukov
 */
@Configuration
@ComponentScan("ru.yandex.market.crm.core.test.utils")
@Import(GlobalControlConfig.class)
public class CoreTestUtilsConfig {
}
