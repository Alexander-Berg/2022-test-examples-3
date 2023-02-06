package ru.yandex.direct.core.testing.repository;

import java.util.Collection;
import java.util.Map;

import ru.yandex.direct.core.entity.moderation.model.ModResyncQueueObj;
import ru.yandex.direct.core.entity.moderation.model.ModResyncQueueObjectType;
import ru.yandex.direct.core.entity.moderation.repository.ModResyncQueueRepository;
import ru.yandex.direct.dbschema.ppc.Tables;
import ru.yandex.direct.dbutil.QueryWithoutIndex;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.jooqmapper.JooqMapperWithSupplier;

import static ru.yandex.direct.dbschema.ppc.tables.ModResyncQueue.MOD_RESYNC_QUEUE;

@QueryWithoutIndex("Тестовый репозиторий")
public class TestModResyncQueueRepository {

    private final DslContextProvider dslContextProvider;
    private final JooqMapperWithSupplier<ModResyncQueueObj> mapper;

    public TestModResyncQueueRepository(DslContextProvider dslContextProvider,
                                        ModResyncQueueRepository modResyncQueueRepository) {
        this.dslContextProvider = dslContextProvider;
        this.mapper = modResyncQueueRepository.getMapper();
    }

    public ModResyncQueueObj get(int shard, ModResyncQueueObjectType type, Long id) {
        return dslContextProvider.ppc(shard)
                .select(mapper.getFieldsToRead())
                .from(MOD_RESYNC_QUEUE)
                .where(MOD_RESYNC_QUEUE.OBJECT_TYPE.eq(ModResyncQueueObjectType.toSource(type)))
                .and(MOD_RESYNC_QUEUE.OBJECT_ID.eq(id))
                .fetchOne(mapper::fromDb);
    }

    public Map<Long, ModResyncQueueObj> get(int shard, ModResyncQueueObjectType type, Collection<Long> ids) {
        return dslContextProvider.ppc(shard)
                .select(mapper.getFieldsToRead())
                .from(MOD_RESYNC_QUEUE)
                .where(MOD_RESYNC_QUEUE.OBJECT_TYPE.eq(ModResyncQueueObjectType.toSource(type)))
                .and(MOD_RESYNC_QUEUE.OBJECT_ID.in(ids))
                .fetchMap(MOD_RESYNC_QUEUE.OBJECT_ID, mapper::fromDb);
    }

    public Integer getSize(int shard) {
        return dslContextProvider.ppc(shard)
                .selectCount()
                .from(MOD_RESYNC_QUEUE)
                .fetchOne()
                .value1();
    }

    public void clear(int shard) {
        dslContextProvider.ppc(shard)
                .deleteFrom(Tables.MOD_RESYNC_QUEUE)
                .execute();
    }
}
