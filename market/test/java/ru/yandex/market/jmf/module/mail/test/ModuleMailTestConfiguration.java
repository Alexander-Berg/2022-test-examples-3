package ru.yandex.market.jmf.module.mail.test;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

import ru.yandex.market.jmf.module.automation.test.ModuleAutomationRuleTestConfiguration;
import ru.yandex.market.jmf.module.mail.ModuleMailConfiguration;
import ru.yandex.market.jmf.module.mail.SendMailService;

@Configuration
@Import({
        ModuleMailConfiguration.class,
        ModuleAutomationRuleTestConfiguration.class
})
@ComponentScan("ru.yandex.market.jmf.module.mail.test.impl")
@PropertySource(name = "testMailProperties", value = "classpath:conf/mail.properties")
public class ModuleMailTestConfiguration {
    @Bean
    public SendMailService mockSendMailService() {
        return Mockito.mock(SendMailService.class);
    }
}
