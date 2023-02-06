package ru.yandex.direct.core.testing.repository;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static ru.yandex.direct.dbschema.ppcdict.tables.ShardOrderId.SHARD_ORDER_ID;

/**
 * Работа со связкой шард - ID заказа в тестах
 */
public class TestShardOrderIdRepository {
    private DslContextProvider dslContextProvider;

    @Autowired
    public TestShardOrderIdRepository(DslContextProvider dslContextProvider) {
        this.dslContextProvider = dslContextProvider;
    }

    /**
     * Добавляев связку OrderId c ID клиента
     *
     * @param clientId ID клиента
     * @param orderId  ID кампании в БК
     */
    public void insertOrderIdClientId(ClientId clientId, Long orderId) {
        dslContextProvider.ppcdict()
                .insertInto(SHARD_ORDER_ID)
                .set(SHARD_ORDER_ID.ORDER_ID, orderId)
                .set(SHARD_ORDER_ID.CLIENT_ID, clientId.asLong())
                .execute();
    }

}
