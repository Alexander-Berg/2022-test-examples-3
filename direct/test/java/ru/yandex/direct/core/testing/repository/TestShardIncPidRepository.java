package ru.yandex.direct.core.testing.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static ru.yandex.direct.dbschema.ppcdict.tables.ShardIncPid.SHARD_INC_PID;

@Repository
public class TestShardIncPidRepository {

    @Autowired
    private DslContextProvider dslContextProvider;

    public Long getClientId(Long adGroupId) {
        return dslContextProvider.ppcdict()
                .select(SHARD_INC_PID.CLIENT_ID)
                .from(SHARD_INC_PID)
                .where(SHARD_INC_PID.PID.eq(adGroupId))
                .fetchOne(SHARD_INC_PID.CLIENT_ID);
    }

    public void updateClientId(Long adGroupId, Long newClientId) {
        dslContextProvider.ppcdict()
                .update(SHARD_INC_PID)
                .set(SHARD_INC_PID.CLIENT_ID, newClientId)
                .where(SHARD_INC_PID.PID.eq(adGroupId))
                .execute();
    }

    public void delete(Long adGroupId) {
        dslContextProvider.ppcdict()
                .deleteFrom(SHARD_INC_PID)
                .where(SHARD_INC_PID.PID.eq(adGroupId))
                .execute();
    }

}
