package ru.yandex.market.queuedcalls.configuration;

import java.util.Collection;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.queuedcalls.EnvironmentConfig;
import ru.yandex.market.queuedcalls.QueuedCallOrderIdCalculatorService;
import ru.yandex.market.queuedcalls.QueuedCallProcessor;

import static org.mockito.Mockito.mock;
import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW;

@Configuration
public class TestConfiguration implements EnvironmentConfig {

    @Autowired
    private DataSource dataSource;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private List<QueuedCallProcessor> processorsList;

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }


    @Override
    @Bean
    public Collection<QueuedCallProcessor> processors() {
        return processorsList;
    }

    @Override
    @Bean
    public DataSource queuedCallsDataSource() {
        return dataSource;
    }

    @Override
    @Bean
    public TransactionTemplate requiresNewTransactionTemplate() {
        return new TransactionTemplate(transactionManager, new DefaultTransactionDefinition(PROPAGATION_REQUIRES_NEW));
    }

    @Override
    @Bean
    public TestableClock clock() {
        return new TestableClock();
    }

    @Bean
    public QueuedCallOrderIdCalculatorService queuedCallOrderIdCalculatorService() {
        return mock(QueuedCallOrderIdCalculatorService.class);
    }
}
