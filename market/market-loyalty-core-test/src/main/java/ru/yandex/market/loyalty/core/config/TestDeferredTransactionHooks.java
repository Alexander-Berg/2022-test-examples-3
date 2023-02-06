package ru.yandex.market.loyalty.core.config;

import ru.yandex.market.loyalty.core.service.budgeting.DeferredTransactionHooks;

class TestDeferredTransactionHooks implements DeferredTransactionHooks {
    TestDeferredTransactionHooks() {

    }

    @Override
    public boolean shouldForceDeferredTransactions(boolean isDeferredTransactions, long fromAccountId,
                                                   long toAccountId) {
        return isDeferredTransactions;
    }
}
