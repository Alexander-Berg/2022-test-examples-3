package ru.yandex.direct.core.testing.repository;

import java.util.Collection;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.jooq.Field;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ru.yandex.direct.core.entity.keyword.repository.internal.DbAddedPhrasesCache;
import ru.yandex.direct.dbutil.QueryWithoutIndex;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.jooqmapper.JooqMapperWithSupplier;

import static ru.yandex.direct.core.entity.keyword.repository.KeywordCacheRepository.buildJooqMapper;
import static ru.yandex.direct.dbschema.ppc.Tables.ADDED_PHRASES_CACHE;

@Component
@Lazy
@ParametersAreNonnullByDefault
public class TestKeywordCacheRepository {
    private JooqMapperWithSupplier<DbAddedPhrasesCache> jooqMapper = buildJooqMapper();
    private Collection<Field<?>> allFieldsToRead = jooqMapper.getFieldsToRead();

    private final DslContextProvider dslContextProvider;

    @Autowired
    public TestKeywordCacheRepository(DslContextProvider dslContextProvider) {
        this.dslContextProvider = dslContextProvider;
    }

    @QueryWithoutIndex("Используется только в тестах")
    public List<DbAddedPhrasesCache> getCachedKeywordsByAdGroupIds(int shard, List<Long> adGroupIds) {
        return dslContextProvider.ppc(shard)
                .select(allFieldsToRead)
                .from(ADDED_PHRASES_CACHE)
                .where(ADDED_PHRASES_CACHE.PID.in(adGroupIds))
                .fetch()
                .map(jooqMapper::fromDb);
    }
}
