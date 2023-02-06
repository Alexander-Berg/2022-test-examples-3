package ru.yandex.market.billing.checkout.logbroker;

import java.util.Collections;
import java.util.concurrent.ExecutorService;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.NoBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import ru.yandex.kikimr.persqueue.consumer.StreamConsumer;
import ru.yandex.kikimr.persqueue.consumer.StreamListener;
import ru.yandex.kikimr.persqueue.consumer.stream.StreamConsumerConfig;
import ru.yandex.market.billing.util.logbroker.LogbrokerStreamListener;
import ru.yandex.market.logbroker.consumer.LogbrokerDataProcessor;
import ru.yandex.market.logbroker.model.LogbrokerCluster;
import ru.yandex.market.mbi.thread.MbiExecutors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.billing.util.logbroker.RetryingStreamListenerDecorator.wrapWithRetry;
import static ru.yandex.market.billing.util.logbroker.StoppableStreamListenerDecorator.wrapWithStoppable;

@Configuration
@Import(LogbrokerCheckouterConsumerTestConfig.ConsumerTestConfiguration.class)
public class LogbrokerCheckouterConsumerTestConfig {

    @Bean
    public LogbrokerDataProcessor initializedLogbrokerOrdersProcessor() {
        var mockedProcessor = Mockito.mock(LogbrokerOrderEventsProcessor.class);
        doNothing().doThrow(new IllegalArgumentException()).when(mockedProcessor).process(any());
        return mockedProcessor;
    }

    @Bean(name = "orderEventsLbkxExecutorService")
    public ExecutorService logbrokerEventProcessingExecutorService() {
        return MbiExecutors.newSingleThreadErrorHandlingExecutor(
                "checkouter-order-consumer",
                error -> { }
        );
    }

    @Bean
    public LogbrokerCluster lbkxCluster() {
        return mock(LogbrokerCluster.class);
    }

    @Configuration
    public static class ConsumerTestConfiguration {
        private final LogbrokerCluster logbrokerCluster;
        private final LogbrokerDataProcessor logbrokerOrderEventsProcessor;

        @SuppressWarnings("checkstyle:lineLength")
        public ConsumerTestConfiguration(
                @Qualifier("lbkxCluster") LogbrokerCluster logbrokerCluster,
                LogbrokerDataProcessor initializedLogbrokerOrdersProcessor) {
            this.logbrokerCluster = logbrokerCluster;
            this.logbrokerOrderEventsProcessor = initializedLogbrokerOrdersProcessor;
        }

        @Bean
        StreamConsumer testGetOrdersConsumer() {
            return logbrokerCluster.createStreamConsumer(
                    StreamConsumerConfig.builder(Collections.singleton("test-topic"), "test-consumer")
                            .build()
            );
        }

        @Bean
        RetryTemplate processingRetryTemplate() {
            RetryTemplate retryTemplate = new RetryTemplate();
            retryTemplate.setBackOffPolicy(new NoBackOffPolicy());
            RetryPolicy retryPolicy = new SimpleRetryPolicy(1);
            retryTemplate.setRetryPolicy(retryPolicy);
            return retryTemplate;
        }

        @Bean(name = "stoppableTransactionalLogbrokerListener")
        StreamListener orderEventsLogbrokerListener() {
            StreamListener listener = new LogbrokerStreamListener(logbrokerOrderEventsProcessor);
            StreamListener retryingListener = wrapWithRetry(listener, processingRetryTemplate());
            return wrapWithStoppable(retryingListener);
        }
    }
}
