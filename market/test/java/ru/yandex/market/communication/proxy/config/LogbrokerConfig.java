package ru.yandex.market.communication.proxy.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.checkout.checkouter.config.CheckouterAnnotationJsonConfig;
import ru.yandex.market.communication.proxy.logbroker.LogbrokerOrderEventsProcessor;
import ru.yandex.market.communication.proxy.service.LogbrokerOrderEventService;

@Configuration
@Import({CheckouterAnnotationJsonConfig.class, JSONSerializationConfig.class})
public class LogbrokerConfig {

    @Bean
    public LogbrokerOrderEventService logbrokerOrderEventService() {
        return new LogbrokerOrderEventService();
    }

    @Bean
    public LogbrokerOrderEventsProcessor logbrokerOrderEventsProcessor(ObjectMapper objectMapper,
                                                                       LogbrokerOrderEventService logbrokerOrderEventService) {
        return new LogbrokerOrderEventsProcessor(objectMapper, logbrokerOrderEventService);
    }
}
