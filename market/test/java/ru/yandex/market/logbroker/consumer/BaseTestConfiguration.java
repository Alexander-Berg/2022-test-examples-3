package ru.yandex.market.logbroker.consumer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;

/**
 * @author ungomma
 */
@Configuration
public class BaseTestConfiguration {

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPropertyResolver() {
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

}
