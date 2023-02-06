package ru.yandex.calendar.logic.notification.xiva;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class NotificationTestConfiguration {
    @Bean
    public EventTestConverter eventTestConverter() {
        return new EventTestConverter();
    }

    @Bean
    public TestEventManager testEventManager() {
        return new TestEventManager();
    }
}
