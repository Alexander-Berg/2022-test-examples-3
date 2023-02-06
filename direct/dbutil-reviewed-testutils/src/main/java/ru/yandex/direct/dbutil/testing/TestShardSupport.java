package ru.yandex.direct.dbutil.testing;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.cache.Cache;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;

import ru.yandex.direct.dbutil.QueryWithForbiddenShardMapping;
import ru.yandex.direct.dbutil.sharding.ShardKey;
import ru.yandex.direct.dbutil.sharding.ShardSupport;
import ru.yandex.direct.dbutil.sharding.ShardedValuesGenerator;
import ru.yandex.direct.dbutil.wrapper.DatabaseWrapperProvider;

/**
 * Тестовый ShardSupport, при попытке использования запрещенных таблиц шардинга
 * кидает {@link ru.yandex.direct.dbutil.QueryWithForbiddenShardMapping}
 */
@ParametersAreNonnullByDefault
public class TestShardSupport extends ShardSupport {

    private static final Set<ShardKey> ALLOWED_SHARD_KEYS = Set.of(ShardKey.CLIENT_ID, ShardKey.UID, ShardKey.LOGIN,
            ShardKey.CID, ShardKey.ORDER_ID, ShardKey.PROMOACTION_ID);

    private IgnoreAnnotationHelper ignoreAnnotationHelper =
            new IgnoreAnnotationHelper(QueryWithForbiddenShardMapping.class, getClass());

    public TestShardSupport(DatabaseWrapperProvider databaseWrapperProvider,
                            ShardedValuesGenerator valuesGenerator, int numOfPpcShards) {
        super(databaseWrapperProvider, valuesGenerator, numOfPpcShards);
    }

    @Override
    protected <R extends Record, T1, T2> void runGetValuesQuery(Table<R> table, Field<T1> keyField,
                                                                Field<T2> chainField, List<Object> chunk,
                                                                Cache<Object, Object> cache,
                                                                Map<Object, List<Integer>> indexesByValue,
                                                                ShardKey currentKey, Object[] currentValues) {
        if (!ALLOWED_SHARD_KEYS.contains(currentKey) && !ignoreAnnotationHelper.hasIgnoreAnnotation(false)) {
            throw new QueryForbiddenShardKeyException(currentKey);
        }

        super.runGetValuesQuery(table, keyField, chainField, chunk, cache, indexesByValue, currentKey, currentValues);
    }
}
