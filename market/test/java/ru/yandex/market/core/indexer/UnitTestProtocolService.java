package ru.yandex.market.core.indexer;

import org.springframework.transaction.support.SimpleTransactionStatus;

import ru.yandex.market.core.protocol.ProtocolService;
import ru.yandex.market.core.protocol.ProtocolTransactionCallback;
import ru.yandex.market.core.protocol.model.ActionContext;
import ru.yandex.market.core.protocol.model.ActionInfo;
import ru.yandex.market.core.protocol.model.ProtocolFunction;

/**
 * @author sergey-fed
 */
public class UnitTestProtocolService implements ProtocolService {

    public static final long TEST_ACTION_ID = 777L;

    @Override
    public long createAction(ActionContext actionContext) {
        return TEST_ACTION_ID;
    }

    @Override
    public ActionInfo getActionInfo(long actionId) {
        return null;
    }

    @Override
    public <T> T executeInTransaction(ProtocolTransactionCallback<T> callback) {
        callback.setProtocolService(this);
        return callback.doInTransaction(new SimpleTransactionStatus());
    }

    @Override
    public <T> T actionInTransaction(ActionContext actionContext, ProtocolFunction<T> function) {
        return null;
    }

    @Override
    public long logAction(ActionContext actionContext) {
        return 0;
    }
}
