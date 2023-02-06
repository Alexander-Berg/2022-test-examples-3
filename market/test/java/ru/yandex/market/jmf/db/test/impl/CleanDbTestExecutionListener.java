package ru.yandex.market.jmf.db.test.impl;

import org.springframework.stereotype.Component;
import org.springframework.test.context.event.AfterTestClassEvent;
import org.springframework.test.context.event.AfterTestMethodEvent;
import org.springframework.test.context.event.annotation.AfterTestClass;
import org.springframework.test.context.event.annotation.AfterTestMethod;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import ru.yandex.market.jmf.db.test.CleanDb;
import ru.yandex.market.jmf.tx.AfterCompletionSynchronization;

@Component
public class CleanDbTestExecutionListener {

    private final TestDbCleaner testDbCleaner;

    public CleanDbTestExecutionListener(TestDbCleaner testDbCleaner) {
        this.testDbCleaner = testDbCleaner;
    }

    @AfterTestClass
    public void afterTestClass(AfterTestClassEvent event) {
        CleanDb cleanDb = event.getTestContext().getTestClass().getAnnotation(CleanDb.class);
        if (cleanDb != null && cleanDb.classMode() == CleanDb.ClassMode.AFTER_ALL) {
            reinitializeDb();
        }
    }

    @AfterTestMethod
    public void afterTestMethod(AfterTestMethodEvent event) {
        CleanDb methodCleanDb = event.getTestContext().getTestMethod().getAnnotation(CleanDb.class);
        CleanDb classCleanDb = event.getTestContext().getTestClass().getAnnotation(CleanDb.class);
        if (methodCleanDb != null) {
            if (methodCleanDb.methodMode() == CleanDb.MethodMode.AFTER ||
                    methodCleanDb.methodMode() == CleanDb.MethodMode.DEFAULT && classCleanDb != null && classCleanDb.classMode() == CleanDb.ClassMode.AFTER_ALL) {
                reinitializeDb();
            }
            return;
        }
        if (classCleanDb != null && classCleanDb.classMode() == CleanDb.ClassMode.AFTER_EACH_METHOD) {
            reinitializeDb();
        }
    }

    private void reinitializeDb() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            testDbCleaner.reinitializeDb();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                (AfterCompletionSynchronization) status -> testDbCleaner.reinitializeDb()
        );
    }
}
