package ru.yandex.market.crm.platform.reader.test;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author apershukov
 */
@Configuration
@Import({
        TestWebContextConfig.class,
        ServicesTestConfig.class
})
public class ControllerTestConfig {}
