package ru.yandex.market.crm.campaign.test;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;


/**
 * @author zloddey
 */
@Configuration
@Import({
        CoreServiceIntTestConfig.class,
        MockYqlConfig.class,
        TestYtConfig.class
})
class ServiceMediumWithoutYtTestConfig {
}
