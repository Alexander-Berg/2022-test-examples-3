package ru.yandex.market.billing.checkout.logbroker;

import java.util.concurrent.TimeUnit;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.kikimr.persqueue.consumer.StreamConsumer;
import ru.yandex.kikimr.persqueue.consumer.StreamListener;
import ru.yandex.kikimr.persqueue.consumer.StreamListener.ReadResponder;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.ConsumerReadResponse;
import ru.yandex.market.core.logbroker.receiver.ReceiveConfig;
import ru.yandex.market.core.logbroker.receiver.StreamConsumerFactory;

@Configuration
@Import(LogbrokerGetOrdersTestConfig.Basic.class)
public class LogbrokerGetOrdersTestConfig {

    @Autowired
    private ConsumerReadResponse consumerReadResponseMock;
    @Autowired
    private ReadResponder readResponderMock;

    private StreamListener listener;

    @Bean
    public StreamConsumerFactory orderEventsConsumerFactory() {
        var streamConsumerMock = Mockito.mock(StreamConsumer.class);

        Mockito.doAnswer(invocation -> {
            this.listener = invocation.getArgument(0);
            this.listener.onRead(consumerReadResponseMock, readResponderMock);
            return null;
        }).when(streamConsumerMock)
                .startConsume(Mockito.any());

        Mockito.doAnswer(invocation -> {
            this.listener.onClose();
            return null;
        }).when(streamConsumerMock)
                .stopConsume();

        return () -> streamConsumerMock;
    }

    @Configuration
    public static class Basic {
        @Bean
        public ReceiveConfig orderEventsReceiveConfig() {
            return ReceiveConfig.builder()
                    .setTopicName("some-test-topic")
                    .setNumberOfReaders(1)
                    // 3s for execution
                    .setExecutionTimeLimit(3L)
                    .setExecutionTimeUnit(TimeUnit.SECONDS)
                    // 1s for shutdown waiting
                    .setShutdownWaitingTime(1L)
                    .setShutdownWaitingTimeUnit(TimeUnit.SECONDS)
                    // 1ms for receiving
                    .setReceiverSleepTimeLimit(1L)
                    .setReceiverSleepTimeUnit(TimeUnit.MILLISECONDS)
                    .build();
        }

        @Bean
        public ConsumerReadResponse consumerReadResponseMock() {
            return Mockito.mock(ConsumerReadResponse.class);
        }

        @Bean
        public ReadResponder readResponderMock() {
            return Mockito.mock(ReadResponder.class);
        }
    }
}
