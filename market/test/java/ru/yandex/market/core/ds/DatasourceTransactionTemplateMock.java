package ru.yandex.market.core.ds;

import java.util.function.Function;

import ru.yandex.market.core.protocol.model.ActionContext;
import ru.yandex.market.core.shop.ShopActionContext;

/**
 * @author Vadim Lyalin
 */
public class DatasourceTransactionTemplateMock implements DatasourceTransactionTemplate {
    public static final long MOCK_ACTION_ID = 1;

    @Override
    public <T> T execute(long datasourceId, ActionContext actionContext, Function<ShopActionContext, T> handler) {
        return handler.apply(new ShopActionContext(MOCK_ACTION_ID, datasourceId));
    }
}
