package ru.yandex.direct.core.testing.repository;


import org.jooq.util.mysql.MySQLDSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ru.yandex.direct.common.jooqmapper.OldJooqMapperWithSupplier;
import ru.yandex.direct.core.entity.client.model.ClientLimits;
import ru.yandex.direct.core.entity.client.repository.ClientLimitsRepository;
import ru.yandex.direct.dbschema.ppc.tables.records.ClientLimitsRecord;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.jooqmapperhelper.InsertHelper;

import static ru.yandex.direct.dbschema.ppc.tables.ClientLimits.CLIENT_LIMITS;

@Repository
public class TestClientLimitsRepository {

    private final OldJooqMapperWithSupplier<ClientLimits> clientLimitMapper;

    private final DslContextProvider dslContextProvider;

    @Autowired
    public TestClientLimitsRepository(DslContextProvider dslContextProvider,
                                      ClientLimitsRepository clientLimitsRepository) {
        this.dslContextProvider = dslContextProvider;
        this.clientLimitMapper = clientLimitsRepository.createClientLimitsMapper();
    }

    public void addClientLimits(int shard, ClientLimits clientLimits) {
        if (clientLimits != null) {
            insertClientLimitsToDb(shard, clientLimits);
        }
    }

    private void insertClientLimitsToDb(int shard, ClientLimits clientLimits) {
        InsertHelper<ClientLimitsRecord> insertHelper =
                new InsertHelper<>(dslContextProvider.ppc(shard), CLIENT_LIMITS);
        insertHelper.add(clientLimitMapper, clientLimits).newRecord();
        if (insertHelper.hasAddedRecords()) {
            insertHelper.onDuplicateKeyUpdate()
                    .set(CLIENT_LIMITS.CAMP_COUNT_LIMIT, MySQLDSL.values(CLIENT_LIMITS.CAMP_COUNT_LIMIT))
                    .set(CLIENT_LIMITS.UNARC_CAMP_COUNT_LIMIT, MySQLDSL.values(CLIENT_LIMITS.UNARC_CAMP_COUNT_LIMIT))
                    .set(CLIENT_LIMITS.BANNER_COUNT_LIMIT, MySQLDSL.values(CLIENT_LIMITS.BANNER_COUNT_LIMIT))
                    .set(CLIENT_LIMITS.KEYWORD_COUNT_LIMIT, MySQLDSL.values(CLIENT_LIMITS.KEYWORD_COUNT_LIMIT))
                    .set(CLIENT_LIMITS.FEED_COUNT_LIMIT, MySQLDSL.values(CLIENT_LIMITS.FEED_COUNT_LIMIT))
                    .set(CLIENT_LIMITS.FEED_MAX_FILE_SIZE, MySQLDSL.values(CLIENT_LIMITS.FEED_MAX_FILE_SIZE))
                    .set(CLIENT_LIMITS.GENERAL_BLACKLIST_SIZE_LIMIT, MySQLDSL.values(CLIENT_LIMITS.GENERAL_BLACKLIST_SIZE_LIMIT))
                    .set(CLIENT_LIMITS.VIDEO_BLACKLIST_SIZE_LIMIT, MySQLDSL.values(CLIENT_LIMITS.VIDEO_BLACKLIST_SIZE_LIMIT));
        }
        insertHelper.execute();
    }

}
