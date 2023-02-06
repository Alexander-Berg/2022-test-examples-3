package ru.yandex.direct.core.testing.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static ru.yandex.direct.dbschema.ppc.Tables.EVENTLOG;

@Repository
public class TestEventLogRepository {

    @Autowired
    private DslContextProvider dslContextProvider;

    public void deleteCampaignEvents(int shard, ClientId clientId, Long campaignId) {
        dslContextProvider.ppc(shard)
                .deleteFrom(EVENTLOG)
                .where(EVENTLOG.CLIENT_ID.eq(clientId.asLong())
                        .and(EVENTLOG.CID.eq(campaignId)))
                .execute();
    }
}
