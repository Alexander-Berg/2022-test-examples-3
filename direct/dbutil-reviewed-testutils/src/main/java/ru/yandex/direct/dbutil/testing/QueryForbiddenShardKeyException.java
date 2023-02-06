package ru.yandex.direct.dbutil.testing;

import ru.yandex.direct.dbutil.sharding.ShardKey;

public class QueryForbiddenShardKeyException extends RuntimeException {
    public static final String ERROR_STRING_TEMPLATE = "Query uses forbidden shard mapping table '%s', " +
            "see TestShardSupport. Fix the code or use @QueryWithForbiddenShardMapping";

    QueryForbiddenShardKeyException(ShardKey shardKey) {
        super(String.format(ERROR_STRING_TEMPLATE, shardKey.getTable().getName()));
    }
}
