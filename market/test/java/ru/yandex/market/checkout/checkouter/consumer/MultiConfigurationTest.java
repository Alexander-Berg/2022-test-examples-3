package ru.yandex.market.checkout.checkouter.consumer;


import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;

import ru.yandex.market.checkout.checkouter.consumer.config.EnableEventsConsumer;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.logbroker.consumer.LogbrokerReader;
import ru.yandex.market.logbroker.consumer.config.LogbrokerConsumerRegistry;
import ru.yandex.market.logbroker.consumer.config.builder.ConsumerFactory;
import ru.yandex.market.logbroker.consumer.util.LbReaderOffsetDao;
import ru.yandex.market.logbroker.consumer.util.impl.DummyLbReaderOffsetDao;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;

@RunWith(SpringRunner.class)
@TestPropertySource({"classpath:int-test-connection.properties", "classpath:int-test-multi.properties"})
public class MultiConfigurationTest {

    @Autowired
    private List<LogbrokerReader> readers;

    @Test
    public void testContextStarted() {
        Assert.assertThat(readers.size(), is(3));
    }

    @EnableEventsConsumer(consumerCount = 3)
    @Configuration
    @ImportResource("classpath:WEB-INF/checkouter-serialization.xml")
    public static class TestConfiguration {

        @Bean
        public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholdersResolver() {
            return new PropertySourcesPlaceholderConfigurer();
        }

        @Bean(name = "transactionTemplate")
        public TransactionOperations transactionOperations() {
            return new TransactionOperations() {
                @Override
                public <T> T execute(TransactionCallback<T> action) throws TransactionException {
                    action.doInTransaction(null);
                    return null;
                }
            };
        }

        @Bean
        public LbReaderOffsetDao lbReaderOffsetDao() {
            return new DummyLbReaderOffsetDao();
        }

        @Bean(name = "eventsConsumerFactory")
        public ConsumerFactory<OrderHistoryEvent> eventsConsumer() {
            return (i) -> (events) -> {
            };
        }

        @Bean()
        public String logbrokerReaderUser(LogbrokerConsumerRegistry logbrokerConsumerRegistry) {
            Assert.assertThat(logbrokerConsumerRegistry.getLogbrokerReaders().size(), equalTo(3));
            return "";
        }
    }
}
