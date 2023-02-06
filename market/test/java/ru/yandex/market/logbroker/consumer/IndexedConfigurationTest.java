package ru.yandex.market.logbroker.consumer;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.kikimr.persqueue.LogbrokerClientFactory;
import ru.yandex.kikimr.persqueue.auth.Credentials;
import ru.yandex.market.logbroker.consumer.config.EnableLogbrokerConsumer;
import ru.yandex.market.logbroker.consumer.config.LogbrokerConnectionParams;
import ru.yandex.market.logbroker.consumer.config.LogbrokerConsumerConfiguration;
import ru.yandex.market.logbroker.consumer.config.LogbrokerConsumerRegistry;
import ru.yandex.market.logbroker.consumer.config.builder.ConsumerFactory;
import ru.yandex.market.logbroker.consumer.config.builder.LbParserFactory;
import ru.yandex.market.logbroker.consumer.config.builder.LogbrokerConsumerBuilder;
import ru.yandex.market.logbroker.consumer.config.builder.LogbrokerConsumers;
import ru.yandex.market.logbroker.consumer.util.LbReaderOffsetDao;
import ru.yandex.market.logbroker.consumer.util.impl.DummyLbReaderOffsetDao;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
class IndexedConfigurationTest {

    @Autowired
    private List<LogbrokerReader> lbReaders;

    @Autowired
    private LogbrokerClientFactory logbrokerClientFactory;

    @Autowired
    private LogbrokerConsumerRegistry registry;

    @Autowired
    @Qualifier(LogbrokerConsumerConfiguration.MAIN_LB_READER_BEAN_NAME)
    private LogbrokerReader mainReader;

    @Autowired
    @Qualifier(LogbrokerConsumerConfiguration.MAIN_LB_READER_BEAN_NAME + "_1")
    private LogbrokerReader firstReader;

    @Autowired
    @Qualifier(LogbrokerConsumerConfiguration.MAIN_LB_READER_BEAN_NAME + "_3")
    private LogbrokerReader thirdReader;

    @Test
    void testContextStarted() {
        assertThat(lbReaders).hasSize(3);

        assertThat(mainReader).isNotNull();
        assertThat(firstReader).isNotNull();
        assertThat(thirdReader).isNotNull();

        assertThat(logbrokerClientFactory).isNotNull();

        assertThat(registry.getLogbrokerReaders()).hasSize(3);
        assertThat(registry.getLogbrokerReaders().keySet())
                .containsExactly(
                        LogbrokerConsumerConfiguration.MAIN_LB_READER_BEAN_NAME,
                        LogbrokerConsumerConfiguration.MAIN_LB_READER_BEAN_NAME + "_2",
                        LogbrokerConsumerConfiguration.MAIN_LB_READER_BEAN_NAME + "_3"
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
        public ConsumerFactory<LocalDate> eventsConsumerFactory() {
            return i -> (events) -> {
            };
        }

        @Bean
        public LbParserFactory<LocalDate> parserFactory() {
            return i -> (entity, line) -> LocalDate.now();
        }

        @Bean
        public LogbrokerConsumerBuilder<LocalDate> logbrokerConsumerBuilder(LbParserFactory<LocalDate> parserFactory,
                                                                            ConsumerFactory<LocalDate> consumerFactory) {
            return LogbrokerConsumers.prefixed(
                    Arrays.asList("clients/12", "clients/25", "client/677"),
                    "entity",
                    "topic",
                    parserFactory,
                    consumerFactory
            );
        }

    }
}
