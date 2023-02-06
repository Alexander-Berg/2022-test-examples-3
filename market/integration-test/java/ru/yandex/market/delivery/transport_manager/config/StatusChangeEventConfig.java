package ru.yandex.market.delivery.transport_manager.config;

import lombok.Data;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;

import ru.yandex.market.delivery.transport_manager.event.status_change.TransportationStatusChangeEvent;

public class StatusChangeEventConfig {
    @Bean
    public ApplicationListener<TransportationStatusChangeEvent> sampleTransportationStatusListener() {
        return new TestTransportationStatusChangeEventApplicationListener();
    }

    @Data
    public static class TestTransportationStatusChangeEventApplicationListener
        implements ApplicationListener<TransportationStatusChangeEvent> {
        private TransportationStatusChangeEvent event;
        private boolean shouldFail = false;

        @Override
        public void onApplicationEvent(TransportationStatusChangeEvent e) {
            event = e;
            if (shouldFail) {
                throw new RuntimeException();
            }
        }
    }
}
