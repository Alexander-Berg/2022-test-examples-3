package ru.yandex.direct.dbutil.testing;

import java.util.HashMap;
import java.util.Map;

import ru.yandex.direct.dbutil.ShardByClient;
import ru.yandex.direct.dbutil.model.ClientId;

public class FakeShardByClient implements ShardByClient {
    private Map<ClientId, Integer> shardByClientId = new HashMap<>();

    public FakeShardByClient() {
    }

    public FakeShardByClient(Map<ClientId, Integer> shardByClientId) {
        this.shardByClientId = shardByClientId;
    }

    @Override
    public int getShardByClientId(ClientId clientId) {
        return shardByClientId.getOrDefault(clientId, 0);
    }
}
