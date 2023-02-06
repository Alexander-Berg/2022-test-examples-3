package ru.yandex.market.mbo.core.utils;

import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

public class TransactionTemplateMock extends TransactionTemplate {
    public TransactionTemplateMock() {
    }

    @Override
    public <T> T execute(TransactionCallback<T> action) throws TransactionException {
        TransactionStatus status = new SimpleTransactionStatus();
        return action.doInTransaction(status);
    }
}
