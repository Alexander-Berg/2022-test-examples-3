package ru.yandex.direct.core.testing.fake;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ru.yandex.direct.dbschema.ppc.tables.records.BidsDynamicRecord;
import ru.yandex.direct.dbschema.ppc.tables.records.DynamicConditionsRecord;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static ru.yandex.direct.dbschema.ppc.Tables.BIDS_DYNAMIC;
import static ru.yandex.direct.dbschema.ppc.Tables.DYNAMIC_CONDITIONS;

@Repository
public class DynamicConditionsRepository {

    @Autowired
    private DslContextProvider dslContextProvider;

    public BidsDynamicRecord addBidsDynamicRecord(int shard, BidsDynamicRecord record) {
        dslContextProvider.ppc(shard)
                .insertInto(BIDS_DYNAMIC)
                .set(record)
                .execute();
        return getBidsDynamicRecord(shard, record.getDynId());
    }

    public BidsDynamicRecord getBidsDynamicRecord(int shard, Long id) {
        return dslContextProvider.ppc(shard)
                .select(
                        BIDS_DYNAMIC.DYN_ID, BIDS_DYNAMIC.DYN_COND_ID, BIDS_DYNAMIC.PID, BIDS_DYNAMIC.PRICE,
                        BIDS_DYNAMIC.PRICE_CONTEXT, BIDS_DYNAMIC.AUTOBUDGET_PRIORITY, BIDS_DYNAMIC.STATUS_BS_SYNCED,
                        BIDS_DYNAMIC.OPTS)
                .from(BIDS_DYNAMIC)
                .where(BIDS_DYNAMIC.DYN_ID.eq(id))
                .fetchOne()
                .into(BIDS_DYNAMIC);
    }

    public DynamicConditionsRecord addDynamicConditionRecord(int shard, DynamicConditionsRecord record) {
        dslContextProvider.ppc(shard)
                .insertInto(DYNAMIC_CONDITIONS)
                .set(record)
                .execute();
        return getDynamicConditionsRecord(shard, record.getDynCondId());
    }

    public DynamicConditionsRecord getDynamicConditionsRecord(int shard, Long id) {
        return dslContextProvider.ppc(shard)
                .select(DYNAMIC_CONDITIONS.DYN_COND_ID, DYNAMIC_CONDITIONS.PID, DYNAMIC_CONDITIONS.CONDITION_NAME,
                        DYNAMIC_CONDITIONS.CONDITION_HASH, DYNAMIC_CONDITIONS.CONDITION_JSON)
                .from(DYNAMIC_CONDITIONS)
                .where(DYNAMIC_CONDITIONS.DYN_COND_ID.eq(id))
                .fetchOne()
                .into(DYNAMIC_CONDITIONS);
    }


}
