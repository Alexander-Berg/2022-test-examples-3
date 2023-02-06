package ru.yandex.calendar.frontend.mailhook;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import ru.yandex.calendar.monitoring.WebApiMonitoringConfiguration;
import ru.yandex.calendar.unistat.ApiUnistatContextConfiguration;

@Configuration
@Import({
        MailhookContextConfiguration.class,
        ApiUnistatContextConfiguration.class,
        WebApiMonitoringConfiguration.class,
})
public class MailhookContextTestConfiguration {
    @Bean
    @Primary
    public MailhookService mailhookService() {
        return Mockito.mock(MailhookServiceImpl.class);
    }
}
