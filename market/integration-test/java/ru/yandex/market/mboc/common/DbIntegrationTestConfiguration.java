package ru.yandex.market.mboc.common;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;


/**
 * @author yuramalinov
 * @created 11.10.18
 */
@TestConfiguration
public class DbIntegrationTestConfiguration {

    @Bean
    @Primary
    public TransactionHelper commonTransactionHelper(PlatformTransactionManager transactionManager) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        return transactionTemplate::execute;
    }

    /**
     * Нужен для ситуаций, когда надо гарантировать новую транзакцию.
     */
    @Qualifier("newTransaction")
    @Bean
    public TransactionHelper newTransactionHelper(PlatformTransactionManager transactionManager) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transactionTemplate::execute;
    }
}
