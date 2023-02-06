package ru.yandex.market.tpl.common.startrek.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.tpl.common.startrek.domain.TestTicketStateFactory;

@Configuration
public class StartrekListenerTestClassesConfiguration {

    @Bean
    public TestTicketStateFactory testTicketStateFactory() {
        return new TestTicketStateFactory();
    }

}
