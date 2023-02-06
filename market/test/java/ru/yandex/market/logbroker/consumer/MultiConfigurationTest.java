package ru.yandex.market.logbroker.consumer;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.kikimr.persqueue.LogbrokerClientFactory;
import ru.yandex.kikimr.persqueue.auth.Credentials;
import ru.yandex.market.logbroker.consumer.config.EnableLogbrokerConsumer;
import ru.yandex.market.logbroker.consumer.config.LogbrokerConnectionParams;
import ru.yandex.market.logbroker.consumer.config.LogbrokerConsumerConfiguration;
import ru.yandex.market.logbroker.consumer.config.LogbrokerConsumerParams;
import ru.yandex.market.logbroker.consumer.config.LogbrokerConsumerRegistry;
import ru.yandex.market.logbroker.consumer.config.builder.LogbrokerConsumerBuilder;
import ru.yandex.market.logbroker.consumer.config.builder.LogbrokerConsumers;
import ru.yandex.market.logbroker.consumer.util.LbParser;
import ru.yandex.market.logbroker.consumer.util.LbReaderOffsetDao;
import ru.yandex.market.logbroker.consumer.util.impl.DummyLbReaderOffsetDao;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
class MultiConfigurationTest {

    public static final String ANOTHER_LB_READER_NAME = "anotherLbReader";

    @Autowired
    private List<LogbrokerReader> lbReaders;

    @Autowired
    private LogbrokerClientFactory logbrokerClientFactory;

    @Autowired
    private LogbrokerConsumerRegistry registry;

    @Test
    void testContextStarted() {
        assertThat(lbReaders).hasSize(2);
        assertThat(logbrokerClientFactory).isNotNull();

        assertThat(registry.getLogbrokerReaders()).hasSize(2);
        assertThat(registry.getLogbrokerReaders().keySet())
                .containsExactly(
                        LogbrokerConsumerConfiguration.MAIN_LB_READER_BEAN_NAME,
                        ANOTHER_LB_READER_NAME
                );
    }

    @Configuration
    @Import(BaseTestConfiguration.class)
    @EnableLogbrokerConsumer
    public static class TestConfiguration {

        @Bean
        public LogbrokerConnectionParams logbrokerConnectionParams() {
            LogbrokerConnectionParams params = new LogbrokerConnectionParams();
            params.setPort(222);
            params.setHost("host");
            params.setCredentialsSupplier(() -> Credentials.tvm("123"));
            return params;
        }

        @Bean
        public LbReaderOffsetDao lbReaderOffsetDao() {
            return new DummyLbReaderOffsetDao();
        }

        @Bean
        public Consumer<List<LocalDate>> eventsConsumer() {
            return (events) -> {
            };
        }

        @Bean
        public LbParser<LocalDate> localDateLbParser() {
            return (entity, line) -> LocalDate.now();
        }

        @Bean
        public LogbrokerConsumerBuilder<LocalDate> logbrokerConsumerBuilder(LbParser<LocalDate> parser) {
            return LogbrokerConsumers.single(
                    "entity",
                    "topic",
                    "client/24",
                    parser,
                    eventsConsumer(),
                    null
            );
        }

        @Bean
        public LogbrokerConsumerBuilder<LocalDate> anotherLogbrokerConsumerBuilder(LbParser<LocalDate> parser) {
            return () -> Collections.singletonList(
                    new LogbrokerConsumerParams<>(
                            ANOTHER_LB_READER_NAME,
                            "entity",
                            "topic",
                            "client/24",
                            parser,
                            eventsConsumer(),
                            null
                    )
            );
        }

    }
}
