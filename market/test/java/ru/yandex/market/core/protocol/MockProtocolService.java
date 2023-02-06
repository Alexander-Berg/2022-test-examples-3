package ru.yandex.market.core.protocol;

import java.util.Date;

import ru.yandex.market.core.protocol.model.ActionContext;
import ru.yandex.market.core.protocol.model.ActionInfo;
import ru.yandex.market.core.protocol.model.ProtocolFunction;

/**
 * @author Alexey Shevenkov ashevenkov@yandex-team.ru
 */
public class MockProtocolService implements ProtocolService {

    public static final int TEST_ACTION_ID = 123;

    @Override
    public long createAction(ActionContext actionContext) {
        return TEST_ACTION_ID;
    }

    @Override
    public ActionInfo getActionInfo(long actionId) {
        return new ActionInfo(actionId, 0, 0, 0, new Date(), null);
    }

    @Override
    public Object executeInTransaction(ProtocolTransactionCallback callback) {
        return null;
    }

    @Override
    public <T> T actionInTransaction(ActionContext actionContext, ProtocolFunction<T> function) {
        return function.calculate(null, 1L);
    }

    @Override
    public long logAction(ActionContext actionContext) {
        return 0;
    }

}
