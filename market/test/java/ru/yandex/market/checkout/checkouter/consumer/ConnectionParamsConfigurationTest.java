package ru.yandex.market.checkout.checkouter.consumer;


import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
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

import ru.yandex.bolts.collection.Option;
import ru.yandex.inside.passport.tvm2.Tvm2;
import ru.yandex.kikimr.persqueue.auth.Credentials;
import ru.yandex.market.checkout.checkouter.consumer.config.EnableEventsConsumer;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.logbroker.consumer.config.LogbrokerConnectionParams;
import ru.yandex.market.logbroker.consumer.config.builder.ConsumerFactory;
import ru.yandex.market.logbroker.consumer.util.LbReaderOffsetDao;
import ru.yandex.market.logbroker.consumer.util.impl.DummyLbReaderOffsetDao;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@TestPropertySource({"classpath:int-test-no-tvm.properties"})
public class ConnectionParamsConfigurationTest {

    public static final String TVM_TICKET = "32525";

    @Autowired
    private LogbrokerConnectionParams logbrokerConnectionParams;

    /**
     * Провереяем, что при заведенном Tvm2 в контексте, не создается новый инстанс,
     * и этот tvm2 автоматически подтягивается в параметры соединения logbroker.
     */
    @Test
    public void testTvmCredentialsArePresent() {
        Assert.assertThat(
                logbrokerConnectionParams.getCredentialsSupplier().get(),
                is(Credentials.tvm(TVM_TICKET))
        );
    }

    @Configuration
    @EnableEventsConsumer
    @ImportResource("classpath:WEB-INF/checkouter-serialization.xml")
    public static class TestConfiguration {

        @Bean
        public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholdersResolver() {
            return new PropertySourcesPlaceholderConfigurer();
        }

        @Bean
        public Tvm2 tvm2() {
            Tvm2 mock = Mockito.mock(Tvm2.class);
            when(mock.getServiceTicket(anyInt())).thenReturn(Option.of(TVM_TICKET));
            return mock;
        }

        @Bean(name = "eventsConsumerFactory")
        public ConsumerFactory<OrderHistoryEvent> eventsConsumerFactory() {
            return (i) -> (events) -> {
            };
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

    }
}
