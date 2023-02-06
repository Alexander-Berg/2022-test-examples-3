package ru.yandex.market.crm.triggers.test.helpers;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.crm.core.test.utils.CoreTestUtilsConfig;

/**
 * @author apershukov
 */
@Configuration
@Import(CoreTestUtilsConfig.class)
@ComponentScan("ru.yandex.market.crm.triggers.test.helpers")
public class TestHelpersConfig {
}
