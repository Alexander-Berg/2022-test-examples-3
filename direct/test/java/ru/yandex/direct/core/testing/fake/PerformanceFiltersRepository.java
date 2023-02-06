package ru.yandex.direct.core.testing.fake;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ru.yandex.direct.dbschema.ppc.tables.records.BidsPerformanceRecord;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static ru.yandex.direct.dbschema.ppc.Tables.BIDS_PERFORMANCE;

@Repository
public class PerformanceFiltersRepository {

    @Autowired
    private DslContextProvider dslContextProvider;

    public BidsPerformanceRecord addBidsPerformanceRecord(int shard, BidsPerformanceRecord record) {
        return dslContextProvider.ppc(shard)
                .insertInto(BIDS_PERFORMANCE)
                .set(record)
                .returning(BIDS_PERFORMANCE.fields())
                .fetchOne();
    }

}
