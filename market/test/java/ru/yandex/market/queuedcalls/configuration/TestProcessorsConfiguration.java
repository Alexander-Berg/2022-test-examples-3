package ru.yandex.market.queuedcalls.configuration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.queuedcalls.ExecutionResult;
import ru.yandex.market.queuedcalls.QueuedCallProcessor;
import ru.yandex.market.queuedcalls.QueuedCallType;
import ru.yandex.market.queuedcalls.model.TestQCType;

@Configuration
public class TestProcessorsConfiguration {

    @Bean
    public QueuedCallProcessor firstProcessor() {
        return new DummyProcessor(TestQCType.FIRST);
    }

    @Bean
    public QueuedCallProcessor secondProcessor() {
        return new DummyProcessor(TestQCType.SECOND);
    }

    @Bean
    public QueuedCallProcessor thirdProcessor() {
        return new DummyProcessor(TestQCType.THIRD);
    }


    private static class DummyProcessor implements QueuedCallProcessor {

        private final QueuedCallType type;

        DummyProcessor(QueuedCallType type) {
            this.type = type;
        }

        @Nullable
        @Override
        public ExecutionResult process(QueuedCallProcessor.QueuedCallExecution execution) {
            return null;
        }

        @Nonnull
        @Override
        public QueuedCallType getSupportedType() {
            return type;
        }

        @Override
        public int delayBetweenExecutionsOnHostInSeconds() {
            return -1;
        }

        @Override
        public int batchSize() {
            return 1000;
        }
    }
}
