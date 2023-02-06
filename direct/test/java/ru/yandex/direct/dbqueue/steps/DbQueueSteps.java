package ru.yandex.direct.dbqueue.steps;

import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.ParametersAreNonnullByDefault;

import org.jooq.DSLContext;
import org.jooq.Record1;

import ru.yandex.direct.dbqueue.DbQueueJobType;
import ru.yandex.direct.dbqueue.repository.DbQueueTypeMap;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static ru.yandex.direct.dbschema.ppc.Tables.DBQUEUE_JOBS;
import static ru.yandex.direct.dbschema.ppc.Tables.DBQUEUE_JOB_ARCHIVE;
import static ru.yandex.direct.utils.CommonUtils.ifNotNull;

@ParametersAreNonnullByDefault
public class DbQueueSteps {
    private static final ru.yandex.direct.dbschema.ppcdict.tables.DbqueueJobTypes PPCDICT_DBQUEUE_JOB_TYPES =
            ru.yandex.direct.dbschema.ppcdict.tables.DbqueueJobTypes.DBQUEUE_JOB_TYPES;

    private static final ru.yandex.direct.dbschema.ppc.tables.DbqueueJobTypes PPC_DBQUEUE_JOB_TYPES =
            ru.yandex.direct.dbschema.ppc.tables.DbqueueJobTypes.DBQUEUE_JOB_TYPES;

    private static final AtomicInteger NEXT_JOB_TYPE_ID = new AtomicInteger(1);

    private final ShardHelper shardHelper;
    private final DslContextProvider dslContextProvider;
    private final DbQueueTypeMap typeMap;

    public DbQueueSteps(ShardHelper shardHelper, DslContextProvider dslContextProvider,
                        DbQueueTypeMap typeMap) {
        this.shardHelper = shardHelper;
        this.dslContextProvider = dslContextProvider;
        this.typeMap = typeMap;
    }

    public <A, R> void registerJobType(DbQueueJobType<A, R> jobType) {
        long jobTypeId = nextJobTypeId();

        dslContextProvider.ppcdict()
                .insertInto(PPCDICT_DBQUEUE_JOB_TYPES)
                .set(PPCDICT_DBQUEUE_JOB_TYPES.JOB_TYPE, jobType.getName())
                .set(PPCDICT_DBQUEUE_JOB_TYPES.JOB_TYPE_ID, jobTypeId)
                .onDuplicateKeyIgnore()
                .execute();

        for (int shard : shardHelper.dbShards()) {
            dslContextProvider.ppc(shard)
                    .insertInto(PPC_DBQUEUE_JOB_TYPES)
                    .set(PPC_DBQUEUE_JOB_TYPES.JOB_TYPE, jobType.getName())
                    .set(PPC_DBQUEUE_JOB_TYPES.JOB_TYPE_ID, jobTypeId)
                    .onDuplicateKeyIgnore()
                    .execute();
        }
    }

    public <A, R> void clearQueue(DbQueueJobType<A, R> jobType) {
        for (int shard : shardHelper.dbShards()) {
            Long jobTypeId = typeMap.getJobTypeIdByTypeName(jobType.getName());
            DSLContext dslContext = dslContextProvider.ppc(shard);

            dslContext.deleteFrom(DBQUEUE_JOBS)
                    .where(DBQUEUE_JOBS.JOB_TYPE_ID.eq(jobTypeId))
                    .execute();

            dslContext.deleteFrom(DBQUEUE_JOB_ARCHIVE)
                    .where(DBQUEUE_JOB_ARCHIVE.JOB_TYPE_ID.eq(jobTypeId))
                    .execute();
        }
    }

    public <A, R> Long getLastJobByType(int shard, DbQueueJobType<A, R> jobType) {
        Long jobTypeId = typeMap.getJobTypeIdByTypeName(jobType.getName());
        DSLContext dslContext = dslContextProvider.ppc(shard);

        var result = dslContext.select(DBQUEUE_JOBS.JOB_ID)
                .from(DBQUEUE_JOBS)
                .where(DBQUEUE_JOBS.JOB_TYPE_ID.eq(jobTypeId))
                .orderBy(DBQUEUE_JOBS.JOB_ID.desc())
                .limit(1)
                .fetchOne();
        return ifNotNull(result, Record1::value1);
    }

    private long nextJobTypeId() {
        return NEXT_JOB_TYPE_ID.incrementAndGet();
    }
}
