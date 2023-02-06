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
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.kikimr.persqueue.consumer.StreamConsumer;
import ru.yandex.kikimr.persqueue.consumer.StreamListener;
import ru.yandex.kikimr.persqueue.consumer.stream.StreamConsumerConfig;
import ru.yandex.market.core.database.MdbMbiConfig;
import ru.yandex.market.core.logbroker.receiver.StoppableTransactionalLogbrokerListener;
import ru.yandex.market.logbroker.consumer.LogbrokerDataProcessor;
import ru.yandex.market.logbroker.db.LogbrokerMonitorExceptionsService;
import ru.yandex.market.logbroker.model.LogbrokerCluster;
import ru.yandex.market.mbi.thread.MbiExecutors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

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
                error -> {
                }
        );
    }

    @Configuration
    public static class ConsumerTestConfiguration {
        private final LogbrokerCluster logbrokerCluster;
        private final LogbrokerDataProcessor logbrokerOrderEventsProcessor;
        private final TransactionTemplate transactionTemplate;
        private final LogbrokerMonitorExceptionsService monitorExceptionsService;

        @SuppressWarnings("checkstyle:lineLength")
        public ConsumerTestConfiguration(
                @Qualifier("lbkxCluster") LogbrokerCluster logbrokerCluster,
                LogbrokerDataProcessor initializedLogbrokerOrdersProcessor,
                TransactionTemplate transactionTemplate,
                @Qualifier("logbrokerMonitorExceptionsService") LogbrokerMonitorExceptionsService monitorExceptionsService) {
            this.logbrokerCluster = logbrokerCluster;
            this.logbrokerOrderEventsProcessor = initializedLogbrokerOrdersProcessor;
            this.transactionTemplate = transactionTemplate;
            this.monitorExceptionsService = monitorExceptionsService;
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
        StreamListener transactionalGetOrderEventsListener() {
            return new StoppableTransactionalLogbrokerListener(
                    logbrokerOrderEventsProcessor,
                    transactionTemplate,
                    monitorExceptionsService,
                    processingRetryTemplate(),
                    false,
                    MdbMbiConfig.DatasourceRouting.FORCE_ORIGINAL
            );
        }
    }
}
