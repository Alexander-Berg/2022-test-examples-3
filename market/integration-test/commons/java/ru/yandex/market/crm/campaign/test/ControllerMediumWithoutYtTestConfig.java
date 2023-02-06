package ru.yandex.market.crm.campaign.test;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author apershukov
 */
@Configuration
@Import({
        ServiceMediumWithoutYtTestConfig.class,
        TestWebContextConfig.class
})
class ControllerMediumWithoutYtTestConfig {
}
