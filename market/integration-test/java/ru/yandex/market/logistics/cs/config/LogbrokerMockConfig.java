package ru.yandex.market.logistics.cs.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import ru.yandex.kikimr.persqueue.consumer.transport.ConsumerTransport;
import ru.yandex.market.checkout.checkouter.consumer.config.EventsConsumerConfiguration;
import ru.yandex.market.logbroker.consumer.config.LogbrokerClientFactoryConfiguration;
import ru.yandex.market.logbroker.consumer.config.LogbrokerConnectionParams;
import ru.yandex.market.logbroker.consumer.config.LogbrokerConsumerConfiguration;
import ru.yandex.market.logistics.cs.checkouter.common.ConsumerTransportMock;
import ru.yandex.market.logistics.cs.checkouter.common.StreamListenerLogbrokerClientFactory;
import ru.yandex.market.logistics.cs.config.dbqueue.LogbrokerCheckouterConsumptionQueueConfiguration;

@Configuration
@Import({
    LogbrokerClientFactoryConfiguration.class,
    EventsConsumerConfiguration.class,
    LogbrokerConsumerConfiguration.class,
    CheckouterLogbrokerConsumerConfig.class,
    LogbrokerCheckouterConsumptionQueueConfiguration.class,
})
@ComponentScan(basePackages = "ru.yandex.market.logistics.cs.logbroker")
public class LogbrokerMockConfig {

    @Primary
    @Bean
    public StreamListenerLogbrokerClientFactory logbrokerClientFactory(ConsumerTransport consumerTransport) {
        return new StreamListenerLogbrokerClientFactory(consumerTransport);
    }

    @Primary
    @Bean
    public LogbrokerConnectionParams logbrokerConnectionParams() {
        LogbrokerConnectionParams params = new LogbrokerConnectionParams();
        params.setHost("localhost");
        params.setPort(1024);
        return params;
    }

    @Bean
    public ConsumerTransportMock consumerTransport() {
        return new ConsumerTransportMock();
    }
}
