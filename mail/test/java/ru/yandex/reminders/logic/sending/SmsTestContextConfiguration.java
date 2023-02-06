package ru.yandex.reminders.logic.sending;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.yandex.inside.passport.sms.PassportSmsService;

@Configuration
public class SmsTestContextConfiguration {
    @Bean
    public SmsSender getSender() {
        return new SmsSender();
    }

    @Bean
    public PassportSmsService getService() {
        return new PassportSmsService();
    }
}