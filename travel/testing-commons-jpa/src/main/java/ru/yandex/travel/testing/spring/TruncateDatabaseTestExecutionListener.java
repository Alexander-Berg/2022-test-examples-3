package ru.yandex.travel.testing.spring;

import javax.persistence.EntityManager;

import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.transaction.support.TransactionTemplate;


public class TruncateDatabaseTestExecutionListener implements TestExecutionListener {
    @Override
    public void afterTestClass(TestContext testContext) throws Exception {
        EntityManager entityManager = testContext.getApplicationContext().getBean(EntityManager.class);
        TransactionTemplate transactionTemplate = testContext.getApplicationContext().getBean(TransactionTemplate.class);
        TruncateDatabaseService truncateDatabaseService = new TruncateDatabaseService(transactionTemplate, entityManager);
        truncateDatabaseService.truncate();
    }
}
