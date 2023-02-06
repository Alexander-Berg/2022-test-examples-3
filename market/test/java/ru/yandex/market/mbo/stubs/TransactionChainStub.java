package ru.yandex.market.mbo.stubs;

import ru.yandex.market.mbo.utils.db.TransactionChainCall;

/**
 * @author moskovkin@yandex-team.ru
 * @since 26.10.18
 */
public class TransactionChainStub extends TransactionChainCall {
    @Override
    public boolean doInTransactionChain(Action action) {
        try {
            return action.doAction();
        } catch (Exception e) {
            action.onError(e);
            return false;
        }
    }
}
