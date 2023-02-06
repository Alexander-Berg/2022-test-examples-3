package ru.yandex.direct.core.testing.steps;


import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.jooq.types.ULong;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.testing.fake.DynamicConditionsRepository;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.dbschema.ppc.tables.records.BidsDynamicRecord;
import ru.yandex.direct.dbschema.ppc.tables.records.DynamicConditionsRecord;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static ru.yandex.direct.dbschema.ppc.Tables.BIDS_DYNAMIC;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS;
import static ru.yandex.direct.dbschema.ppc.Tables.PHRASES;

@ParametersAreNonnullByDefault
public class DynamicsSteps {

    @Autowired
    DynamicConditionsRepository dynamicConditionsRepository;

    @Autowired
    private ShardHelper shardHelper;

    @Autowired
    private DslContextProvider dslContextProvider;

    public BidsDynamicRecord addDefaultBidsDynamic(AdGroupInfo adGroupInfo) {
        return addDefaultBidsDynamic(adGroupInfo, null);
    }

    public BidsDynamicRecord addDefaultBidsDynamic(AdGroupInfo adGroupInfo, @Nullable Long dynamicConditionId) {
        BidsDynamicRecord record = new BidsDynamicRecord();
        record.setDynCondId(dynamicConditionId);

        return addBidsDynamic(adGroupInfo, record);
    }

    public BidsDynamicRecord addBidsDynamic(AdGroupInfo adGroupInfo, BidsDynamicRecord record) {
        Long newId = shardHelper.generateDynamicIds(1).get(0);
        record.setDynId(newId);
        record.setPid(adGroupInfo.getAdGroupId());

        if (record.getDynCondId() == null) {
            long dynamicConditionId = addDefaultDynamicCondition(adGroupInfo).getDynCondId();
            record.setDynCondId(dynamicConditionId);
        }

        return dynamicConditionsRepository.addBidsDynamicRecord(adGroupInfo.getShard(), record);
    }

    public DynamicConditionsRecord addDefaultDynamicCondition(AdGroupInfo adGroupInfo) {
        Long newId = shardHelper.generateDynamicConditionIds(1).get(0);

        DynamicConditionsRecord record = new DynamicConditionsRecord();
        record.setDynCondId(newId);
        record.setPid(adGroupInfo.getAdGroupId());
        record.setConditionName("Test condition " + newId);
        record.setConditionJson("[{\"type\":\"any\"}]");
        record.setConditionHash(ULong.valueOf(record.getConditionName().hashCode()));

        return dynamicConditionsRepository.addDynamicConditionRecord(adGroupInfo.getShard(), record);
    }

    public List<BidsDynamicRecord> getBidsDynamicRecordsByCampaignIds(int shard, Collection<Long> campaignIds) {
        return dslContextProvider.ppc(shard)
                .select(BIDS_DYNAMIC.DYN_ID, BIDS_DYNAMIC.PRICE, BIDS_DYNAMIC.PRICE_CONTEXT,
                        BIDS_DYNAMIC.AUTOBUDGET_PRIORITY, BIDS_DYNAMIC.STATUS_BS_SYNCED)
                .from(BIDS_DYNAMIC
                        .join(PHRASES).on(PHRASES.PID.eq(BIDS_DYNAMIC.PID))
                        .join(CAMPAIGNS).on(CAMPAIGNS.CID.eq(PHRASES.CID)))
                .where(CAMPAIGNS.CID.in(campaignIds))
                .fetch()
                .into(BIDS_DYNAMIC);
    }
}
